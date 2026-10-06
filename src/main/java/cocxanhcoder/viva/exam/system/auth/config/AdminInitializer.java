package cocxanhcoder.viva.exam.system.auth.config;

import cocxanhcoder.viva.exam.system.academic.entity.Role;
import cocxanhcoder.viva.exam.system.academic.entity.User;
import cocxanhcoder.viva.exam.system.academic.repository.RoleRepository;
import cocxanhcoder.viva.exam.system.academic.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Chạy 1 lần mỗi khi app khởi động: nếu DB CHƯA có tài khoản ADMIN nào
 * thì tạo admin đầu tiên từ biến môi trường ADMIN_EMAIL / ADMIN_PASSWORD.
 * (Không có admin thì không ai đăng nhập được để tạo các tài khoản khác.)
 *
 * Đã có ADMIN rồi -> không làm gì. Không set ADMIN_EMAIL/ADMIN_PASSWORD -> bỏ qua.
 */
@Component
public class AdminInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInitializer.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;
    private final String fullName;
    private final String userCode;

    public AdminInitializer(UserRepository userRepository,
                            RoleRepository roleRepository,
                            PasswordEncoder passwordEncoder,
                            @Value("${app.bootstrap-admin.email}") String email,
                            @Value("${app.bootstrap-admin.password}") String password,
                            @Value("${app.bootstrap-admin.full-name}") String fullName,
                            @Value("${app.bootstrap-admin.user-code}") String userCode) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.password = password;
        this.fullName = fullName;
        this.userCode = userCode;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        boolean hasAdmin = userRepository.search(Role.ADMIN, null, PageRequest.of(0, 1)).hasContent();
        if (hasAdmin) {
            return;
        }
        if (email.isBlank() || password.isBlank()) {
            log.warn("Chưa có tài khoản ADMIN nào. Đặt ADMIN_EMAIL và ADMIN_PASSWORD rồi khởi động lại để tạo admin đầu tiên.");
            return;
        }

        Role adminRole = roleRepository.findByRoleName(Role.ADMIN)
                .orElseThrow(() -> new IllegalStateException("Thiếu role ADMIN trong bảng roles (xem V2__seed_roles.sql)"));

        User admin = new User();
        admin.setRole(adminRole);
        admin.setUserCode(userCode.trim());
        admin.setFullName(fullName.trim());
        admin.setEmail(email.trim().toLowerCase());
        admin.setPasswordHash(passwordEncoder.encode(password));
        userRepository.save(admin);

        log.info("Đã tạo tài khoản ADMIN đầu tiên: {}", admin.getEmail());
    }
}
