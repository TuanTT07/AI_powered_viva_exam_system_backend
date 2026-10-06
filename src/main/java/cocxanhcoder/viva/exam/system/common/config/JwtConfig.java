package cocxanhcoder.viva.exam.system.common.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * Cấu hình tạo (encode) và kiểm tra (decode) JWT.
 *
 * Dùng thuật toán HS256: CÙNG 1 khoá bí mật để ký và để kiểm tra chữ ký.
 * Ai có khoá này là tự tạo được token hợp lệ -> khoá phải giữ bí mật (đặt trong .env / Render, không commit).
 */
@Configuration
public class JwtConfig {

    private static final Logger log = LoggerFactory.getLogger(JwtConfig.class);
    private static final String DEV_SECRET = "dev-only-secret-change-me-at-least-32-characters";

    private final SecretKey secretKey;

    public JwtConfig(@Value("${app.jwt.secret}") String secret) {
        // HS256 yêu cầu khoá tối thiểu 256 bit = 32 byte
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("app.jwt.secret (JWT_SECRET) phải dài tối thiểu 32 ký tự");
        }
        if (DEV_SECRET.equals(secret)) {
            log.warn("Đang dùng JWT_SECRET mặc định cho dev. Khi deploy phải đặt JWT_SECRET riêng!");
        }
        this.secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    /** Dùng khi đăng nhập thành công: ký token gửi về cho FE. */
    @Bean
    public JwtEncoder jwtEncoder() {
        return new NimbusJwtEncoder(new ImmutableSecret<>(secretKey));
    }

    /** Spring Security dùng bean này để kiểm tra token trong header "Authorization: Bearer ..." của mỗi request. */
    @Bean
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }
}
