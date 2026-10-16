package cocxanhcoder.viva.exam.system.common.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * Tài khoản còn claim "mustChangePassword" = true (Admin vừa tạo / vừa reset mật khẩu)
 * thì CHẶN mọi API khác, chỉ cho gọi /api/auth/me và /api/auth/change-password,
 * để bắt buộc đổi mật khẩu ngay sau khi đăng nhập lần đầu.
 *
 * Chạy sau khi JWT đã được xác thực (SecurityContext đã có Authentication), trước khi vào controller.
 */
@Component
public class MustChangePasswordFilter extends OncePerRequestFilter {

    private static final Set<String> ALLOWED_PATHS = Set.of(
            "/api/auth/me",
            "/api/auth/change-password"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (request.getMethod().equals(HttpMethod.OPTIONS.name())
                || ALLOWED_PATHS.contains(request.getRequestURI())
                || !(authentication != null && authentication.getPrincipal() instanceof Jwt jwt)
                || !Boolean.TRUE.equals(jwt.getClaim("mustChangePassword"))) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(428); // 428 Precondition Required (không có hằng số trong HttpServletResponse)
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(
                "{\"success\":false,\"status\":428,"
                        + "\"message\":\"Bạn phải đổi mật khẩu trước khi tiếp tục sử dụng hệ thống\",\"data\":null}");
    }
}
