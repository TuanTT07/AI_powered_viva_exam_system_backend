package cocxanhcoder.viva.exam.system.speech.provider;

import cocxanhcoder.viva.exam.system.speech.config.SpeechProperties;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;

import java.net.URI;
import java.net.http.*;
import java.nio.ByteBuffer;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DeepgramSpeechClientTest {
    private final HttpClient http = mock(HttpClient.class);
    private final WebSocket.Builder builder = mock(WebSocket.Builder.class);
    private final WebSocket socket = mock(WebSocket.class);
    private final SpeechToTextClient.Listener results = mock(SpeechToTextClient.Listener.class);
    private final SpeechProperties properties = new SpeechProperties();
    private DeepgramSpeechClient client;
    private WebSocket.Listener wire;

    @BeforeEach void setup() {
        properties.setEnabled(true); properties.setDeepgramApiKey("fake-key");
        when(http.newWebSocketBuilder()).thenReturn(builder);
        when(builder.connectTimeout(any())).thenReturn(builder);
        when(builder.header(anyString(), anyString())).thenReturn(builder);
        when(builder.buildAsync(any(), any())).thenAnswer(i -> {
            wire = i.getArgument(1); wire.onOpen(socket);
            return CompletableFuture.completedFuture(socket);
        });
        when(socket.sendBinary(any(), eq(true))).thenReturn(CompletableFuture.completedFuture(socket));
        when(socket.sendText(anyString(), eq(true))).thenReturn(CompletableFuture.completedFuture(socket));
        client = new DeepgramSpeechClient(properties, http);
    }

    @AfterEach void close() { client.shutdown(); }

    private String result(String start, boolean isFinal, String text) {
        return "{\"type\":\"Results\",\"start\":" + start + ",\"is_final\":" + isFinal
                + ",\"speech_final\":true,\"channel\":{\"alternatives\":[{\"transcript\":\"" + text + "\"}]}}";
    }

    @Test void connectsWithPrivateKeyVietnameseAndCorrectPcmParameters() throws Exception {
        var session = client.open(results);
        verify(builder).header("Authorization", "Token fake-key");
        var uri = ArgumentCaptor.forClass(URI.class);
        verify(builder).buildAsync(uri.capture(), any());
        assertEquals("wss", uri.getValue().getScheme());
        assertEquals("api.deepgram.com", uri.getValue().getHost());
        assertTrue(uri.getValue().getQuery().contains("model=nova-3&language=vi"));
        assertTrue(uri.getValue().getQuery().contains("encoding=linear16&sample_rate=16000&channels=1"));
        assertFalse(uri.getValue().toString().contains("fake-key"));
        session.write(new byte[] {1, 2});
        verify(socket).sendBinary(argThat(b -> b.remaining() == 2 && b.get(0) == 1), eq(true));
        session.close(); session.close();
        verify(socket, times(1)).abort();
    }

    @Test void fragmentedMessagesMapPartialsAndFinalsToNumericOffsets() throws Exception {
        client.open(results);
        String partial = result("1.25", false, "Tam thoi");
        wire.onText(socket, partial.substring(0, 20), false);
        verifyNoInteractions(results);
        wire.onText(socket, partial.substring(20), true);
        wire.onText(socket, result("1.25", true, "Ket qua"), true);
        verify(results).partial("12500000", "Tam thoi");
        verify(results).recognized("12500000", "Ket qua");
        verify(results, never()).failed(); // speech_final must not submit the entire answer.
    }

    @Test void finishWaitsForTailTranscriptSummaryAndNormalClose() throws Exception {
        var session = client.open(results);
        when(socket.sendText("{\"type\":\"CloseStream\"}", true)).thenAnswer(i -> {
            wire.onText(socket, result("0", true, "Cau tra loi cuoi"), true);
            wire.onText(socket, "{\"type\":\"Metadata\",\"duration\":2.5}", true);
            wire.onClose(socket, 1000, "");
            return CompletableFuture.completedFuture(socket);
        });
        session.finish();
        verify(results).recognized("0", "Cau tra loi cuoi");
        verify(results, never()).failed();
        assertThrows(IllegalStateException.class, () -> session.write(new byte[] {1, 2}));
    }

    @Test void unexpectedCloseOrMissingSummaryCannotBeAcceptedAsSavedAnswer() throws Exception {
        var session = client.open(results);
        when(socket.sendText("{\"type\":\"CloseStream\"}", true)).thenAnswer(i -> {
            wire.onClose(socket, 1000, "");
            return CompletableFuture.completedFuture(socket);
        });
        assertThrows(ExecutionException.class, session::finish);
        verify(results, times(1)).failed();
        wire.onError(socket, new IllegalStateException("provider secret"));
        verify(results, times(1)).failed();
    }

    @Test void providerErrorAndMalformedMessagesFailOnlyOnceWithoutForwardingDetails() throws Exception {
        client.open(results);
        wire.onText(socket, "{\"type\":\"Error\",\"description\":\"private provider data\"}", true);
        wire.onText(socket, "invalid JSON", true);
        verify(results, times(1)).failed();
        verify(results, never()).recognized(anyString(), anyString());
    }

    @Test void failedHandshakeIsSanitizedAndDoesNotRequireAzureConfiguration() {
        doReturn(CompletableFuture.failedFuture(new IllegalStateException("private provider body")))
                .when(builder).buildAsync(any(), any());
        Exception error = assertThrows(IllegalStateException.class, () -> client.open(results));
        assertFalse(error.getMessage().contains("private provider body"));
        assertNull(error.getCause());
    }

    @Test void callbacksAfterCloseAreIgnored() throws Exception {
        var session = client.open(results); session.close();
        wire.onText(socket, result("0", true, "late"), true);
        wire.onError(socket, new IllegalStateException());
        verifyNoInteractions(results);
    }
}
