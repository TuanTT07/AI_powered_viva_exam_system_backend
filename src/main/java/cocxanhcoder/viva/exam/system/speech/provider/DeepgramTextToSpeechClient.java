package cocxanhcoder.viva.exam.system.speech.provider;

import cocxanhcoder.viva.exam.system.speech.config.SpeechProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

/** Flux TTS batch audio. Credentials and question text stay on the server/provider boundary. */
@Component
public class DeepgramTextToSpeechClient implements TextToSpeechClient {
    private final SpeechProperties properties;
    private final HttpClient http;
    private final ObjectMapper json = new ObjectMapper();

    @Autowired
    public DeepgramTextToSpeechClient(SpeechProperties properties) {
        this(properties, HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(properties.getProviderTimeoutSeconds())).build());
    }

    DeepgramTextToSpeechClient(SpeechProperties properties, HttpClient http) {
        this.properties = properties;
        this.http = http;
    }

    @Override public byte[] synthesize(String text) throws Exception {
        properties.requireConfigured();
        String model = URLEncoder.encode(properties.getDeepgramTtsModel(), StandardCharsets.UTF_8);
        var request = HttpRequest.newBuilder(URI.create(
                        "https://api.deepgram.com/v2/speak?model=" + model + "&encoding=mp3"))
                .timeout(Duration.ofSeconds(properties.getProviderTimeoutSeconds()))
                .header("Authorization", "Token " + properties.getDeepgramApiKey())
                .header("Content-Type", "application/json")
                .header("Accept", "audio/mpeg")
                .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(Map.of("text", text))))
                .build();
        var response = http.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() != 200
                || !response.headers().firstValue("Content-Type").orElse("")
                    .toLowerCase(java.util.Locale.ROOT).startsWith("audio/")
                || response.body() == null || response.body().length == 0
                || response.body().length > properties.getMaxAudioCacheMb() * 1024L * 1024)
            throw new IOException("Deepgram TTS did not return valid audio (HTTP " + response.statusCode() + ")");
        return response.body();
    }
}
