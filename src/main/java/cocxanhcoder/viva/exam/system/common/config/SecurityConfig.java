package cocxanhcoder.viva.exam.system.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Cấu hình Spring Security.
 * GIAI ĐOẠN DEV: cho phép mọi request (permitAll) để test API bằng Swagger/Postman cho dễ.
 * TODO: thay bằng JWT + phân quyền theo role, ví dụ /api/admin/** chỉ cho ADMIN.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // REST API không dùng cookie/session form nên tắt CSRF
                .csrf(AbstractHttpConfigurer::disable)
                // Không tạo HTTP session (sau này dùng JWT: mỗi request tự mang token)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }

    /**
     * Mã hoá mật khẩu bằng BCrypt (một chiều, có salt).
     * Lưu vào DB chuỗi kiểu "$2a$10$...", KHÔNG BAO GIỜ lưu mật khẩu gốc.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
