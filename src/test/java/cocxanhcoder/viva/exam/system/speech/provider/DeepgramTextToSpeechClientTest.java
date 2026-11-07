package cocxanhcoder.viva.exam.system.speech.provider;

import cocxanhcoder.viva.exam.system.speech.config.SpeechProperties;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import java.net.http.*;
import java.io.IOException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DeepgramTextToSpeechClientTest {
    private final HttpClient http = mock(HttpClient.class);
    @SuppressWarnings("unchecked") private final HttpResponse<byte[]> response = mock(HttpResponse.class);
    private final SpeechProperties properties = new SpeechProperties();
    private DeepgramTextToSpeechClient client;

    @BeforeEach void setup() throws Exception {
        properties.setEnabled(true); properties.setDeepgramApiKey("private-test-key");
        client = new DeepgramTextToSpeechClient(properties, http);
        when(http.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);
        when(response.statusCode()).thenReturn(200);
        when(response.headers()).thenReturn(HttpHeaders.of(Map.of("Content-Type", List.of("audio/mpeg")), (k,v) -> true));
        when(response.body()).thenReturn(new byte[] {73, 68, 51});
    }

    @Test void requestsMaleVietnameseMp3WithPrivateAuthorizationAndTimeout() throws Exception {
        assertArrayEquals(new byte[] {73, 68, 51}, client.synthesize("Câu hỏi tiếng Việt"));
        var capture = ArgumentCaptor.forClass(HttpRequest.class);
        verify(http).send(capture.capture(), any(HttpResponse.BodyHandler.class));
        var request = capture.getValue();
        assertEquals("POST", request.method());
        assertEquals("https://api.deepgram.com/v2/speak?model=flux-minh-vi&encoding=mp3", request.uri().toString());
        assertEquals("Token private-test-key", request.headers().firstValue("Authorization").orElseThrow());
        assertEquals(properties.getProviderTimeoutSeconds(), request.timeout().orElseThrow().toSeconds());
        assertFalse(request.uri().toString().contains("private-test-key"));
    }

    @Test void rejectsProviderErrorsJsonAndEmptyAudio() {
        when(response.statusCode()).thenReturn(429);
        assertThrows(IOException.class, () -> client.synthesize("Question"));
        when(response.statusCode()).thenReturn(200);
        when(response.headers()).thenReturn(HttpHeaders.of(Map.of("Content-Type", List.of("application/json")), (k,v) -> true));
        assertThrows(IOException.class, () -> client.synthesize("Question"));
        when(response.headers()).thenReturn(HttpHeaders.of(Map.of("Content-Type", List.of("audio/mpeg")), (k,v) -> true));
        when(response.body()).thenReturn(new byte[0]);
        assertThrows(IOException.class, () -> client.synthesize("Question"));
    }

    @Test void disabledSpeechDoesNotCallProvider() {
        properties.setEnabled(false);
        assertThrows(RuntimeException.class, () -> client.synthesize("Question"));
        verifyNoInteractions(http);
    }
}
