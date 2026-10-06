package cocxanhcoder.viva.exam.system.auth.service;

import cocxanhcoder.viva.exam.system.academic.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Tạo JWT cho user đã đăng nhập.
 *
 * Nội dung (claims) của token:
 *   sub   = id của user (dùng để biết "ai đang gọi API")
 *   role  = ADMIN / LECTURER / STUDENT (dùng để phân quyền)
 *   email, name = thông tin phụ cho FE
 *   iat / exp   = thời điểm tạo / hết hạn
 * Lưu ý: JWT chỉ được KÝ chứ không mã hoá, ai cũng đọc được nội dung -> không bao giờ để mật khẩu trong token.
 */
@Service
public class JwtService {

    private static final String ISSUER = "aives";

    private final JwtEncoder jwtEncoder;
    private final long expirationMinutes;

    public JwtService(JwtEncoder jwtEncoder,
                      @Value("${app.jwt.expiration-minutes}") long expirationMinutes) {
        this.jwtEncoder = jwtEncoder;
        this.expirationMinutes = expirationMinutes;
    }

    public String generateToken(User user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .subject(user.getId().toString())
                .issuedAt(now)
                .expiresAt(now.plus(expirationMinutes, ChronoUnit.MINUTES))
                .claim("role", user.getRole().getRoleName())
                .claim("email", user.getEmail())
                .claim("name", user.getFullName())
                .build();

        // Phải chỉ rõ HS256 vì mặc định encoder dùng RS256 (khoá công khai/bí mật)
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    /** Thời gian sống của token tính bằng giây (trả cho FE biết khi nào cần đăng nhập lại). */
    public long getExpirationSeconds() {
        return expirationMinutes * 60;
    }
}
