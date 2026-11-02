package cocxanhcoder.viva.exam.system.speech.provider;

import cocxanhcoder.viva.exam.system.speech.config.SpeechProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.RoundingMode;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Deepgram Listen v1 streaming. Uses Java 21 HTTP/WebSocket; no native library or SDK needed. */
@Component
public class DeepgramSpeechClient implements SpeechToTextClient {
    private final SpeechProperties properties;
    private final HttpClient http;
    private final ObjectMapper json = new ObjectMapper();
    private final Set<Session> sessions = ConcurrentHashMap.newKeySet();
    private final ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "deepgram-keepalive"); t.setDaemon(true); return t;
    });

    @Autowired
    public DeepgramSpeechClient(SpeechProperties properties) {
        this(properties, HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(properties.getProviderTimeoutSeconds())).build());
    }

    DeepgramSpeechClient(SpeechProperties properties, HttpClient http) {
        this.properties = properties;
        this.http = http;
    }

    @Override public RecognitionSession open(Listener listener) throws Exception {
        properties.requireConfigured();
        Session session = new Session(listener);
        sessions.add(session);
        try {
            session.connecting = http.newWebSocketBuilder()
                    .connectTimeout(Duration.ofSeconds(properties.getProviderTimeoutSeconds()))
                    .header("Authorization", "Token " + properties.getDeepgramApiKey())
                    .buildAsync(listenUri(), session);
            session.socket = session.connecting.get(properties.getProviderTimeoutSeconds(), TimeUnit.SECONDS);
            session.heartbeat = timer.scheduleWithFixedDelay(session::keepAlive, 3, 3, TimeUnit.SECONDS);
            return session;
        } catch (Exception e) {
            session.close();
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            // Do not propagate provider response bodies, credentials or transport URLs.
            throw new IllegalStateException("Không thể kết nối Deepgram STT");
        }
    }

    private URI listenUri() {
        return URI.create("wss://api.deepgram.com/v1/listen?model=" + encode(properties.getDeepgramModel())
                + "&language=" + encode(properties.getDeepgramLanguage())
                + "&encoding=linear16&sample_rate=16000&channels=1"
                + "&interim_results=true&punctuate=true&smart_format=true&endpointing=300");
    }

    private String encode(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }

    @PreDestroy public void shutdown() {
        timer.shutdownNow();
        for (Session session : Set.copyOf(sessions)) session.close();
        http.shutdownNow();
    }

    private class Session implements RecognitionSession, WebSocket.Listener {
        private final SpeechToTextClient.Listener listener;
        private final StringBuilder fragments = new StringBuilder();
        private final CompletableFuture<Void> drained = new CompletableFuture<>();
        private final AtomicBoolean closed = new AtomicBoolean();
        private final AtomicBoolean failed = new AtomicBoolean();
        private volatile WebSocket socket;
        private volatile CompletableFuture<WebSocket> connecting;
        private volatile ScheduledFuture<?> heartbeat;
        private volatile boolean finishing, summaryReceived;

        Session(SpeechToTextClient.Listener listener) { this.listener = listener; }

        @Override public void onOpen(WebSocket webSocket) {
            socket = webSocket;
            if (closed.get()) webSocket.abort(); else webSocket.request(1);
        }

        @Override public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            if (closed.get()) return null;
            try {
                if (fragments.length() + data.length() > 262144) throw new IllegalStateException("Message too large");
                fragments.append(data);
                if (last) {
                    JsonNode event = json.readTree(fragments.toString());
                    fragments.setLength(0);
                    switch (event.path("type").asText()) {
                        case "Results" -> {
                            JsonNode start = event.get("start");
                            if (start == null || !start.isNumber() || start.decimalValue().signum() < 0)
                                throw new IllegalStateException("Invalid segment offset");
                            // Preserve the numeric segment-id protocol used by TranscriptAccumulator.
                            String id = start.decimalValue().movePointRight(7).setScale(0, RoundingMode.HALF_UP).toPlainString();
                            String text = event.path("channel").path("alternatives").path(0).path("transcript").asText();
                            if (!text.isBlank()) {
                                if (event.path("is_final").asBoolean()) listener.recognized(id, text);
                                else listener.partial(id, text);
                            }
                            // speech_final marks an utterance boundary, not answer submission.
                        }
                        case "Metadata" -> {
                            if (finishing && event.has("duration")) summaryReceived = true;
                        }
                        case "Error" -> fail();
                        default -> { /* Ignore unrelated control messages. */ }
                    }
                }
            } catch (Exception e) { fail(); }
            webSocket.request(1);
            return null;
        }

        @Override public CompletionStage<?> onPing(WebSocket webSocket, ByteBuffer message) {
            webSocket.request(1);
            return webSocket.sendPong(message);
        }

        @Override public CompletionStage<?> onPong(WebSocket webSocket, ByteBuffer message) {
            webSocket.request(1); return null;
        }

        @Override public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            if (!closed.get() && finishing && statusCode == WebSocket.NORMAL_CLOSURE && summaryReceived && !failed.get())
                drained.complete(null);
            else if (!closed.get()) fail();
            return null;
        }

        @Override public void onError(WebSocket webSocket, Throwable error) { if (!closed.get()) fail(); }

        private void fail() {
            drained.completeExceptionally(new IllegalStateException("Deepgram stream bị gián đoạn"));
            if (!closed.get() && failed.compareAndSet(false, true)) listener.failed();
        }

        @Override public synchronized void write(byte[] pcm) {
            if (closed.get() || finishing || failed.get()) throw new IllegalStateException("STT stream đã đóng");
            try {
                socket.sendBinary(ByteBuffer.wrap(pcm), true).get(properties.getProviderTimeoutSeconds(), TimeUnit.SECONDS);
            } catch (Exception e) {
                if (e instanceof InterruptedException) Thread.currentThread().interrupt();
                fail();
                throw new IllegalStateException("Không thể gửi audio đến Deepgram");
            }
        }

        private synchronized void keepAlive() {
            if (closed.get() || finishing || failed.get()) return;
            try {
                socket.sendText("{\"type\":\"KeepAlive\"}", true)
                        .get(properties.getProviderTimeoutSeconds(), TimeUnit.SECONDS);
            } catch (Exception e) {
                if (e instanceof InterruptedException) Thread.currentThread().interrupt();
                fail();
            }
        }

        @Override public void finish() throws Exception {
            synchronized (this) {
                if (closed.get() || failed.get()) throw new IllegalStateException("STT stream đã đóng");
                finishing = true;
                if (heartbeat != null) heartbeat.cancel(false);
                socket.sendText("{\"type\":\"CloseStream\"}", true)
                        .get(properties.getProviderTimeoutSeconds(), TimeUnit.SECONDS);
            }
            // CloseStream drains audio, returns final Results + Metadata, then closes normally.
            drained.get(properties.getFinalizeTimeoutSeconds(), TimeUnit.SECONDS);
        }

        @Override public void close() {
            if (!closed.compareAndSet(false, true)) return;
            if (heartbeat != null) heartbeat.cancel(false);
            if (connecting != null && !connecting.isDone()) connecting.cancel(true);
            if (socket != null) socket.abort();
            drained.completeExceptionally(new CancellationException("STT stream closed"));
            sessions.remove(this);
        }
    }
}
