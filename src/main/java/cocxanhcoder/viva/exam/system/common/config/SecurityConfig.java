package cocxanhcoder.viva.exam.system.common.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * Cấu hình Spring Security với JWT.
 *
 * Luồng đăng nhập:
 *   1. FE gọi POST /api/auth/login (email + mật khẩu) -> nhận về accessToken (JWT)
 *   2. Các request sau FE gửi kèm header:  Authorization: Bearer <accessToken>
 *   3. Spring kiểm tra chữ ký + hạn của token, đọc claim "role" để phân quyền
 *
 * Quy tắc phân quyền:
 *   - /api/auth/login, Swagger              : ai cũng gọi được
 *   - /api/admin/**                          : chỉ ADMIN
 *   - các API còn lại                        : phải đăng nhập (role nào cũng được)
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String[] PUBLIC_URLS = {
            "/api/auth/login",
            "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**",
            "/error"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // REST API dùng token, không dùng cookie/session -> tắt CSRF và không tạo session
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Cho phép FE (khác domain) gọi API, cấu hình ở bean corsConfigurationSource bên dưới
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll() // preflight của trình duyệt
                        .requestMatchers(PUBLIC_URLS).permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                // Bật kiểm tra JWT trong header Authorization
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                        // 401: thiếu token / token sai / hết hạn
                        .authenticationEntryPoint((request, response, ex) ->
                                writeJson(response, HttpServletResponse.SC_UNAUTHORIZED,
                                        "Chưa đăng nhập hoặc token không hợp lệ / đã hết hạn")))
                // 403: đã đăng nhập nhưng không đủ quyền (vd: LECTURER gọi /api/admin/**)
                .exceptionHandling(ex -> ex.accessDeniedHandler((request, response, e) ->
                        writeJson(response, HttpServletResponse.SC_FORBIDDEN, "Bạn không có quyền truy cập chức năng này")));
        return http.build();
    }

    /**
     * Token có claim "role": "ADMIN" -> Spring hiểu thành quyền "ROLE_ADMIN",
     * nhờ vậy .hasRole("ADMIN") / @PreAuthorize("hasRole('ADMIN')") hoạt động.
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("role");
        authorities.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }

    /** CORS: danh sách domain FE lấy từ app.cors.allowed-origins (biến CORS_ALLOWED_ORIGINS). */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(@Value("${app.cors.allowed-origins}") String allowedOrigins) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setExposedHeaders(List.of("Location"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    /** Mã hoá mật khẩu bằng BCrypt (một chiều, có salt). DB chỉ lưu chuỗi "$2a$10$...", không lưu mật khẩu gốc. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Lỗi 401/403 xảy ra trong filter của Security (trước khi vào controller),
     * nên GlobalExceptionHandler không bắt được -> tự ghi JSON cùng format ApiResponse.
     */
    private static void writeJson(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(
                "{\"success\":false,\"status\":" + status + ",\"message\":\"" + message + "\",\"data\":null}");
    }
}
