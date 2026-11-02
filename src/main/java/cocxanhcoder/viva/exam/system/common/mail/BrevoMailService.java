package cocxanhcoder.viva.exam.system.common.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Gửi mail qua Brevo HTTP API (https://api.brevo.com) thay cho SMTP.
 * Dùng khi app.mail.provider=brevo - cần vì nhiều host cloud (vd Render free tier)
 * chặn outbound port SMTP 25/465/587 nhưng vẫn cho phép gọi HTTPS (port 443) bình thường.
 */
@Service
@ConditionalOnProperty(name = "app.mail.provider", havingValue = "brevo")
public class BrevoMailService implements MailService {

    private static final Logger log = LoggerFactory.getLogger(BrevoMailService.class);

    private final String apiKey;
    private final String fromEmail;
    private final String fromName;
    private final RestClient client;

    public BrevoMailService(@Value("${app.mail.brevo-api-key}") String apiKey,
                             @Value("${app.mail.from}") String fromEmail,
                             @Value("${app.mail.from-name:AIVES}") String fromName) {
        this.apiKey = apiKey;
        this.fromEmail = fromEmail;
        this.fromName = fromName;
        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(10));
        this.client = RestClient.builder()
                .baseUrl("https://api.brevo.com/v3")
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    @Async
    public void send(String to, String subject, String body) {
        Map<String, Object> payload = Map.of(
                "sender", Map.of("email", fromEmail, "name", fromName),
                "to", List.of(Map.of("email", to)),
                "subject", subject,
                "textContent", body
        );
        try {
            client.post()
                    .uri("/smtp/email")
                    .header("api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException ex) {
            // Không ném lỗi: tạo/đổi tài khoản đã thành công trong DB rồi, gửi mail thất bại không được rollback
            log.error("Gửi email (Brevo) tới {} thất bại: {}", to, ex.getMessage());
        }
    }
}
