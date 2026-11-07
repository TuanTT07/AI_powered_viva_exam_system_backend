package cocxanhcoder.viva.exam.system.speech.service;

import cocxanhcoder.viva.exam.system.interview.dto.AnswerReceipt;
import cocxanhcoder.viva.exam.system.interview.service.InterviewAnswers;
import cocxanhcoder.viva.exam.system.interview.service.InterviewAccess;
import cocxanhcoder.viva.exam.system.common.exception.BusinessException;
import cocxanhcoder.viva.exam.system.speech.config.SpeechProperties;
import cocxanhcoder.viva.exam.system.speech.dto.SpeechProtocol;
import cocxanhcoder.viva.exam.system.speech.dto.SpeechProtocol.*;
import cocxanhcoder.viva.exam.system.speech.provider.SpeechToTextClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

@Service
public class SpeechSessionService {
    private static final Logger log = LoggerFactory.getLogger(SpeechSessionService.class);
    private final SpeechProperties properties;
    private final InterviewAccess access;
    private final InterviewAnswers transcripts;
    private final SpeechToTextClient stt;
    private final ObjectMapper json = new ObjectMapper();
    private final Map<String, Connection> connections = new ConcurrentHashMap<>();
    private final Map<UUID, String> attempts = new ConcurrentHashMap<>();
    private final Semaphore capacity;
    private final ExecutorService workers = Executors.newVirtualThreadPerTaskExecutor();
    private final ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "speech-timeouts"); t.setDaemon(true); return t;
    });

    public SpeechSessionService(SpeechProperties properties, InterviewAccess access,
                                InterviewAnswers transcripts, SpeechToTextClient stt) {
        this.properties = properties;
        this.access = access;
        this.transcripts = transcripts;
        this.stt = stt;
        this.capacity = new Semaphore(properties.getMaxConcurrentStt());
        timer.scheduleWithFixedDelay(this::sweep, 1, 1, TimeUnit.SECONDS);
    }

    public void connect(WebSocketSession socket, SpeechTicketService.Grant grant) throws Exception {
        if (grant == null || !grant.jwtExpiresAt().isAfter(Instant.now())) {
            socket.close(CloseStatus.POLICY_VIOLATION); return;
        }
        if (connections.size() >= 100 || attempts.putIfAbsent(grant.attemptId(), socket.getId()) != null) {
            socket.close(new CloseStatus(1008, "Speech connection already active or server busy")); return;
        }
        Connection c = new Connection(new ConcurrentWebSocketSessionDecorator(socket, 5000, 65536), grant);
        connections.put(socket.getId(), c);
        send(c, "session.ready", null, null, Map.of("encoding", "PCM_S16LE", "sampleRate", 16000,
                "channels", 1, "maxFrameBytes", 32768, "maxAnswerSeconds", properties.getMaxAnswerSeconds(),
                "sttProvider", properties.getSttProvider(), "ttsProvider", "deepgram", "ttsLanguage", properties.getLanguage()));
    }

    public void command(String socketId, ClientEvent event) {
        Connection c = connections.get(socketId);
        if (c == null) return;
        synchronized (c) {
            if (!validConnection(c)) return;
            c.lastActivity = Instant.now();
            try {
                switch (event.type()) {
                    case "ping" -> send(c, "pong", event.requestId(), null, Map.of());
                    case "answer.start" -> start(c, event);
                    case "answer.end" -> {
                        if (c.answer == null || !c.answer.equals(event.answerId())) {
                            error(c, event.requestId(), "INVALID_ANSWER", "Sai answerId", false); return;
                        }
                        if (c.state == State.COMPLETED) {
                            send(c, "answer.finalized", event.requestId(), c.answer, c.saved); return;
                        }
                        if (c.state == State.FINALIZING) return;
                        if (c.state != State.STREAMING) {
                            error(c, event.requestId(), "INVALID_STATE", "Không có câu trả lời đang nhận audio", false); return;
                        }
                        finalizeAnswer(c, event.requestId());
                    }
                    default -> error(c, event.requestId(), "INVALID_MESSAGE", "Loại sự kiện không hợp lệ", false);
                }
            } catch (BusinessException e) {
                error(c, event.requestId(), "SPEECH_REJECTED", e.getMessage(), false);
            } catch (Exception | LinkageError e) {
                fail(c, "STT_UNAVAILABLE", "Không thể mở dịch vụ nhận dạng giọng nói", true);
            }
        }
    }

    private void start(Connection c, ClientEvent event) throws Exception {
        if (event.requestId().equals(c.startRequest) && c.answer != null) {
            if (!Objects.equals(event.questionTurnId(), c.prompt)) {
                error(c, event.requestId(), "REQUEST_ID_REUSED", "requestId đã gắn với một lượt hỏi khác", false); return;
            }
            if (c.state == State.COMPLETED) send(c, "answer.finalized", event.requestId(), c.answer, c.saved);
            else if (c.state == State.STREAMING)
                send(c, "answer.started", event.requestId(), c.answer, Map.of("questionTurnId", c.prompt,
                        "deadline", c.deadline.toString()));
            else if (c.state == State.FINALIZING)
                send(c, "answer.finalizing", event.requestId(), c.answer, Map.of());
            else error(c, event.requestId(), "ANSWER_FAILED", "Lượt trả lời đã lỗi; dùng requestId mới nếu được phép thu lại", true);
            return;
        }
        if (c.state == State.STREAMING || c.state == State.FINALIZING || c.state == State.STARTING) {
            error(c, event.requestId(), "ANSWER_IN_PROGRESS", "Một câu trả lời đang được xử lý", false); return;
        }
        AudioFormat audio = event.audio();
        if (event.questionTurnId() == null || audio == null || !"PCM_S16LE".equals(audio.encoding())
                || audio.sampleRate() != 16000 || audio.channels() != 1) {
            error(c, event.requestId(), "INVALID_AUDIO_FORMAT", "Cần PCM 16-bit little-endian, 16 kHz, mono", false); return;
        }
        properties.requireConfigured();
        var prompt = access.requirePrompt(c.grant.studentId(), c.grant.attemptId(), event.questionTurnId());
        if (prompt.deadlineAt() != null && !Instant.now().isBefore(prompt.deadlineAt())) {
            error(c, event.requestId(), "ANSWER_DEADLINE_REACHED", "Lượt hỏi đã hết thời gian", false);
            return;
        }
        if (!capacity.tryAcquire()) {
            error(c, event.requestId(), "STT_BUSY", "Dịch vụ STT đang bận, vui lòng thử lại", true); return;
        }
        c.state = State.STARTING;
        c.startRequest = event.requestId();
        c.prompt = prompt.questionTurnId();
        c.questionAttempt = prompt.questionAttemptId();
        c.answer = UUID.randomUUID();
        c.accumulator = new TranscriptAccumulator();
        TranscriptAccumulator accumulator = c.accumulator;
        AtomicLong revision = new AtomicLong();
        c.limitSeconds = Math.min(properties.getMaxAnswerSeconds(), prompt.timeLimitSeconds());
        c.bytes = 0;
        c.saved = null;
        UUID answer = c.answer;
        // Lease is assigned before opening: even a synchronous SDK callback cannot leak capacity.
        Lease lease = new Lease();
        c.lease = lease;
        try {
            lease.session = stt.open(new SpeechToTextClient.Listener() {
                private boolean current() {
                    return answer.equals(c.answer) && !c.closed
                            && (c.state == State.STARTING || c.state == State.STREAMING || c.state == State.FINALIZING);
                }
                @Override public void partial(String segment, String text) {
                    if (current()) send(c, "transcript.partial", null, answer,
                            new Transcript(segment, revision.incrementAndGet(), text));
                }
                @Override public void recognized(String segment, String text) {
                    if (!current()) return;
                    try {
                        accumulator.accept(segment, text);
                        send(c, "transcript.final", null, answer,
                                new Transcript(segment, revision.incrementAndGet(), text));
                    } catch (Exception e) { failed(); }
                }
                @Override public void failed() {
                    // Never dispose native SDK resources on its callback thread.
                    workers.execute(() -> {
                        synchronized (c) {
                            if (current()) fail(c, "STT_FAILED", "Nhận dạng giọng nói bị gián đoạn; câu trả lời chưa được lưu", true);
                        }
                    });
                }
            });
            c.started = Instant.now();
            c.deadline = c.started.plusSeconds(c.limitSeconds);
            if (prompt.deadlineAt() != null && prompt.deadlineAt().isBefore(c.deadline))
                c.deadline = prompt.deadlineAt();
            c.lastActivity = c.started;
            c.state = State.STREAMING;
            send(c, "answer.started", event.requestId(), answer, Map.of("questionTurnId", c.prompt,
                    "deadline", c.deadline.toString()));
        } catch (Exception | LinkageError e) {
            lease.close();
            c.lease = null;
            c.state = State.ERROR;
            throw e;
        }
    }

    public void audio(String socketId, byte[] pcm) {
        Connection c = connections.get(socketId);
        if (c == null) return;
        synchronized (c) {
            if (!validConnection(c)) return;
            if (c.state != State.STREAMING) {
                error(c, null, "INVALID_STATE", "Audio chỉ được gửi sau answer.started và trước answer.end", false); return;
            }
            if (pcm.length == 0 || pcm.length > 32768 || pcm.length % 2 != 0) {
                fail(c, "INVALID_AUDIO", "Kích thước audio không hợp lệ", false); return;
            }
            if (!Instant.now().isBefore(c.deadline)) {
                finalizeAnswer(c, null); return;
            }
            long elapsedBytes = (Duration.between(c.started, Instant.now()).toMillis() + 2000) * 32;
            // PCM consumes 32KB/s. Allow at most 2 seconds of audio ahead of wall clock.
            if (c.bytes + pcm.length > elapsedBytes
                    || c.bytes + pcm.length > c.limitSeconds * 32000L) {
                fail(c, "AUDIO_LIMIT", "Audio gửi quá nhanh hoặc vượt thời lượng cho phép", false); return;
            }
            try {
                c.lease.session.write(pcm);
                c.bytes += pcm.length;
                c.lastActivity = Instant.now();
            } catch (Exception e) {
                fail(c, "STT_FAILED", "Không thể gửi audio đến STT", true);
            }
        }
    }

    private void finalizeAnswer(Connection c, UUID request) {
        c.state = State.FINALIZING;
        Instant ended = Instant.now();
        Lease lease = c.lease;
        UUID answer = c.answer;
        send(c, "answer.finalizing", request, answer, Map.of());
        workers.execute(() -> {
            long start = System.nanoTime();
            try {
                lease.session.finish();
                synchronized (c) {
                    if (c.closed || c.state != State.FINALIZING || !answer.equals(c.answer)) return;
                    if (!validConnection(c)) return;
                    String text = c.accumulator.text();
                    if (text.isBlank()) {
                        fail(c, "NO_SPEECH", "Không nhận dạng được lời nói; câu trả lời chưa được lưu", true); return;
                    }
                    c.saved = transcripts.complete(c.grant.studentId(), c.grant.attemptId(), c.prompt,
                            c.questionAttempt, answer, text, c.started, ended);
                    c.state = State.COMPLETED;
                    c.lastActivity = Instant.now();
                    send(c, "answer.finalized", request, answer, c.saved);
                    log.info("Speech finalized answerId={} finalizeMs={} audioBytes={}", answer,
                            TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start), c.bytes);
                }
            } catch (Exception | LinkageError e) {
                synchronized (c) {
                    if (!c.closed && c.state == State.FINALIZING && answer.equals(c.answer))
                        fail(c, "FINALIZE_FAILED", "Chưa xác nhận lưu câu trả lời; kiểm tra API tra cứu answerId trước khi thử lại", true);
                }
            } finally {
                lease.close();
                synchronized (c) { if (c.lease == lease) c.lease = null; }
            }
        });
    }

    /** Called by BE1 when its authoritative answer timer expires. */
    public void finishCurrentAnswer(UUID attemptId, UUID questionTurnId) {
        String id = attempts.get(attemptId);
        Connection c = id == null ? null : connections.get(id);
        if (c != null) synchronized (c) {
            if (!c.closed && c.state == State.STREAMING && questionTurnId.equals(c.prompt)) finalizeAnswer(c, null);
        }
    }

    public void protocolError(String id) {
        Connection c = connections.get(id);
        if (c != null) error(c, null, "INVALID_MESSAGE", "Sự kiện JSON không hợp lệ", false);
    }

    private boolean validConnection(Connection c) {
        if (c.closed) return false;
        Instant now = Instant.now();
        if (!c.grant.jwtExpiresAt().isAfter(now)
                || c.connected.plusSeconds(properties.getMaxConnectionMinutes() * 60L).isBefore(now)) {
            terminate(c, "SESSION_EXPIRED", "Kết nối hết hạn; đăng nhập lại và xin ticket mới");
            return false;
        }
        return true;
    }

    private void sweep() {
        for (Connection c : connections.values()) {
            // Do not let one provider startup / database lock delay other connections' timers.
            if (!c.checking.compareAndSet(false, true)) continue;
            workers.execute(() -> {
                try {
                    synchronized (c) {
                        if (!validConnection(c)) return;
                        Instant now = Instant.now();
                        if (Duration.between(c.lastCheck, now).getSeconds() >= 5) {
                            access.requireActiveAttempt(c.grant.studentId(), c.grant.attemptId());
                            c.lastCheck = now;
                        }
                        if (c.state == State.STREAMING
                                && !now.isBefore(c.deadline))
                            finalizeAnswer(c, null);
                        else if (c.state != State.FINALIZING
                                && Duration.between(c.lastActivity, now).getSeconds() >= properties.getIdleTimeoutSeconds())
                            terminate(c, "IDLE_TIMEOUT", "Kết nối không hoạt động quá lâu");
                    }
                } catch (Exception e) {
                    synchronized (c) { terminate(c, "ATTEMPT_UNAVAILABLE", "Lượt thi không còn hoạt động hoặc không thể kiểm tra quyền"); }
                } finally { c.checking.set(false); }
            });
        }
    }

    private void error(Connection c, UUID request, String code, String message, boolean retry) {
        send(c, "speech.error", request, c.answer, new SpeechProtocol.Error(code, message, retry));
    }

    private void fail(Connection c, String code, String message, boolean retry) {
        c.state = State.ERROR;
        error(c, null, code, message, retry);
        Lease lease = c.lease;
        c.lease = null;
        if (lease != null) workers.execute(lease::close);
    }

    private void send(Connection c, String type, UUID request, UUID answer, Object data) {
        if (c.closed) return;
        try {
            c.socket.sendMessage(new TextMessage(json.writeValueAsString(new ServerEvent(type, request, answer, data))));
        } catch (Exception e) {
            workers.execute(() -> {
                disconnect(c.socket.getId());
                try { c.socket.close(CloseStatus.SERVER_ERROR); } catch (Exception ignored) {}
            });
        }
    }

    private void terminate(Connection c, String code, String message) {
        error(c, null, code, message, false);
        disconnect(c.socket.getId());
        try { c.socket.close(CloseStatus.POLICY_VIOLATION); } catch (Exception ignored) {}
    }

    public void disconnect(String id) {
        Connection c = connections.remove(id);
        if (c == null) return;
        synchronized (c) {
            c.closed = true;
            attempts.remove(c.grant.attemptId(), id);
            Lease lease = c.lease;
            c.lease = null;
            if (lease != null) workers.execute(lease::close);
        }
    }

    @PreDestroy public void shutdown() {
        timer.shutdownNow();
        for (String id : List.copyOf(connections.keySet())) disconnect(id);
        workers.shutdown();
    }

    private class Lease {
        volatile SpeechToTextClient.RecognitionSession session;
        private final AtomicBoolean closed = new AtomicBoolean();
        void close() {
            if (!closed.compareAndSet(false, true)) return;
            try { if (session != null) session.close(); }
            catch (Exception | LinkageError e) { log.warn("Speech provider cleanup failed"); }
            finally { capacity.release(); }
        }
    }

    private enum State { READY, STARTING, STREAMING, FINALIZING, COMPLETED, ERROR }
    private static class Connection {
        final WebSocketSession socket;
        final SpeechTicketService.Grant grant;
        final Instant connected = Instant.now();
        final AtomicBoolean checking = new AtomicBoolean();
        volatile boolean closed;
        volatile State state = State.READY;
        volatile UUID answer;
        volatile TranscriptAccumulator accumulator;
        UUID startRequest, prompt, questionAttempt;
        Instant started, lastActivity = Instant.now(), lastCheck = Instant.now();
        long bytes;
        int limitSeconds;
        Instant deadline;
        Lease lease;
        AnswerReceipt saved;
        Connection(WebSocketSession socket, SpeechTicketService.Grant grant) {
            this.socket = socket; this.grant = grant;
        }
    }
}
