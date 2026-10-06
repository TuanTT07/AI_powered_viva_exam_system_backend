package cocxanhcoder.viva.exam.system.auth.service;

import cocxanhcoder.viva.exam.system.academic.entity.User;
import cocxanhcoder.viva.exam.system.academic.mapper.UserMapper;
import cocxanhcoder.viva.exam.system.academic.repository.UserRepository;
import cocxanhcoder.viva.exam.system.auth.dto.LoginRequest;
import cocxanhcoder.viva.exam.system.auth.dto.LoginResponse;
import cocxanhcoder.viva.exam.system.common.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Nghiệp vụ đăng nhập. */
@Service
@Transactional(readOnly = true)
public class AuthService {

    // Cùng 1 thông báo cho cả "sai email" lẫn "sai mật khẩu" để người ngoài không dò được email nào tồn tại
    private static final String INVALID_CREDENTIALS = "Email hoặc mật khẩu không đúng";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public LoginResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase(); // email lưu chữ thường khi tạo tài khoản

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, INVALID_CREDENTIALS));

        // So mật khẩu người dùng nhập với chuỗi BCrypt trong DB (không giải mã được, chỉ so khớp được)
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, INVALID_CREDENTIALS);
        }

        return new LoginResponse(
                jwtService.generateToken(user),
                "Bearer",
                jwtService.getExpirationSeconds(),
                UserMapper.toResponse(user)
        );
    }
}
