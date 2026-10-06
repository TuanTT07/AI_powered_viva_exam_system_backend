package cocxanhcoder.viva.exam.system.auth.controller;

import cocxanhcoder.viva.exam.system.academic.dto.UserResponse;
import cocxanhcoder.viva.exam.system.academic.service.UserService;
import cocxanhcoder.viva.exam.system.auth.dto.LoginRequest;
import cocxanhcoder.viva.exam.system.auth.dto.LoginResponse;
import cocxanhcoder.viva.exam.system.auth.service.AuthService;
import cocxanhcoder.viva.exam.system.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * API xác thực.
 *   POST /api/auth/login : đăng nhập, trả JWT (không cần token)
 *   GET  /api/auth/me    : thông tin người đang đăng nhập (cần token)
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = "Đăng nhập / thông tin tài khoản hiện tại")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    public AuthController(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    @PostMapping("/login")
    @SecurityRequirements // API này không cần token -> bỏ ổ khoá trong Swagger
    @Operation(summary = "Đăng nhập bằng email + mật khẩu, trả về JWT")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok("Đăng nhập thành công", authService.login(request));
    }

    /**
     * @AuthenticationPrincipal Jwt: Spring đã kiểm tra token và đưa nội dung token vào đây.
     * jwt.getSubject() chính là id user lúc tạo token.
     */
    @GetMapping("/me")
    @Operation(summary = "Thông tin tài khoản đang đăng nhập")
    public ResponseEntity<ApiResponse<UserResponse>> me(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ApiResponse.ok("Lấy thông tin tài khoản thành công", userService.getUser(userId));
    }
}
