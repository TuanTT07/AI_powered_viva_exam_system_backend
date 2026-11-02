package cocxanhcoder.viva.exam.system.speech.config;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Component
@Validated
@ConfigurationProperties("app.speech")
public class SpeechProperties {
    private boolean enabled = false;
    @Pattern(regexp = "deepgram") private String sttProvider = "deepgram";
    private String deepgramApiKey = "";
    @NotBlank private String deepgramModel = "nova-3";
    @NotBlank private String deepgramLanguage = "vi";
    private String language = "vi-VN"; // Browser TTS language; Deepgram uses "vi".
    @Min(5) @Max(120) private int ticketTtlSeconds = 30;
    @Min(1) @Max(100) private int maxConcurrentStt = 1;
    @Min(1) @Max(20) private int maxConcurrentTts = 1;
    @Min(1) @Max(60) private int providerTimeoutSeconds = 15;
    @Min(1) @Max(60) private int finalizeTimeoutSeconds = 15;
    @Min(5) @Max(1800) private int maxAnswerSeconds = 180;
    @Min(5) @Max(120) private int idleTimeoutSeconds = 30;
    @Min(1) @Max(120) private int maxConnectionMinutes = 30;
    @Min(60) @Max(3600) private int audioTtlSeconds = 600;
    @Min(1) @Max(100) private int maxAudioCacheMb = 16;
    @Min(100) @Max(10000) private int maxTtsCharacters = 3000;

    public void requireConfigured() {
        if (!enabled || deepgramApiKey.isBlank()) {
            throw new cocxanhcoder.viva.exam.system.common.exception.BusinessException(
                    org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,
                    "Speech chưa được bật hoặc chưa cấu hình DEEPGRAM_API_KEY");
        }
    }
}
