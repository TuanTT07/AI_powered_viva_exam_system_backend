package cocxanhcoder.viva.exam.system.academic.service;

import cocxanhcoder.viva.exam.system.academic.dto.CreateUserRequest;
import cocxanhcoder.viva.exam.system.academic.dto.ResetPasswordRequest;
import cocxanhcoder.viva.exam.system.academic.dto.RoleResponse;
import cocxanhcoder.viva.exam.system.academic.dto.UpdateUserRequest;
import cocxanhcoder.viva.exam.system.academic.dto.UserResponse;
import cocxanhcoder.viva.exam.system.academic.entity.Role;
import cocxanhcoder.viva.exam.system.academic.entity.User;
import cocxanhcoder.viva.exam.system.academic.mapper.UserMapper;
import cocxanhcoder.viva.exam.system.academic.repository.CourseRepository;
import cocxanhcoder.viva.exam.system.academic.repository.RoleRepository;
import cocxanhcoder.viva.exam.system.academic.repository.UserRepository;
import cocxanhcoder.viva.exam.system.common.dto.PageResponse;
import cocxanhcoder.viva.exam.system.common.exception.BusinessException;
import cocxanhcoder.viva.exam.system.common.exception.ResourceNotFoundException;
import cocxanhcoder.viva.exam.system.common.util.SearchUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Nghiệp vụ quản lý tài khoản (dành cho Admin).
 *
 * @Transactional(readOnly = true) ở mức class: mặc định mọi hàm chỉ đọc (Hibernate tối ưu hơn).
 * Hàm nào ghi dữ liệu thì đánh dấu lại @Transactional (readOnly = false).
 * Service luôn trả về DTO, không trả Entity ra ngoài.
 */
@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CourseRepository courseRepository;
    private final PasswordEncoder passwordEncoder;

    // Constructor injection: Spring tự truyền các bean vào (không dùng @Autowired trên field)
    public UserService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       CourseRepository courseRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.courseRepository = courseRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // ===================== ĐỌC =====================

    /** Danh sách tài khoản có lọc theo role + từ khoá, phân trang. */
    public PageResponse<UserResponse> searchUsers(String roleName, String keyword, Pageable pageable) {
        String role = (roleName == null || roleName.isBlank()) ? null : roleName.trim().toUpperCase();
        return PageResponse.from(
                userRepository.search(role, SearchUtils.toLikePattern(keyword), pageable)
                        .map(UserMapper::toResponse) // Page<User> -> Page<UserResponse>
        );
    }

    public UserResponse getUser(UUID id) {
        return UserMapper.toResponse(findUser(id));
    }

    public List<RoleResponse> getRoles() {
        return roleRepository.findAll(Sort.by("roleName")).stream()
                .map(UserMapper::toRoleResponse)
                .toList();
    }

    // ===================== GHI =====================

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        String email = normalizeEmail(request.email());
        String userCode = request.userCode().trim();

        // 1. Kiểm tra trùng trước để trả lỗi rõ ràng (DB cũng có UNIQUE làm chốt chặn cuối)
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException(HttpStatus.CONFLICT, "Email đã được sử dụng: " + email);
        }
        if (userRepository.existsByUserCode(userCode)) {
            throw new BusinessException(HttpStatus.CONFLICT, "Mã người dùng đã tồn tại: " + userCode);
        }

        // 2. Tạo entity, mã hoá mật khẩu trước khi lưu
        User user = new User();
        user.setUserCode(userCode);
        user.setFullName(request.fullName().trim());
        user.setEmail(email);
        user.setRole(findRole(request.roleName()));
        user.setPasswordHash(passwordEncoder.encode(request.password()));

        // 3. saveAndFlush: ghi xuống DB ngay để createdAt/updatedAt được điền trước khi trả về
        return UserMapper.toResponse(userRepository.saveAndFlush(user));
    }

    @Transactional
    public UserResponse updateUser(UUID id, UpdateUserRequest request) {
        User user = findUser(id);
        String email = normalizeEmail(request.email());

        if (userRepository.existsByEmailAndIdNot(email, id)) {
            throw new BusinessException(HttpStatus.CONFLICT, "Email đã được sử dụng: " + email);
        }

        Role newRole = findRole(request.roleName());
        // Giảng viên đang phụ trách môn thì không được đổi sang role khác (dữ liệu phân công sẽ sai)
        boolean wasLecturer = Role.LECTURER.equals(user.getRole().getRoleName());
        boolean stillLecturer = Role.LECTURER.equals(newRole.getRoleName());
        if (wasLecturer && !stillLecturer && courseRepository.existsByLecturers_Id(id)) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "Giảng viên đang được phân công môn học, hãy gỡ phân công trước khi đổi role");
        }

        user.setFullName(request.fullName().trim());
        user.setEmail(email);
        user.setRole(newRole);

        // Entity đang được Hibernate quản lý: chỉ cần set là đủ, khi commit sẽ tự UPDATE (dirty checking).
        // Gọi saveAndFlush để @UpdateTimestamp cập nhật updatedAt ngay, response trả về giá trị mới.
        return UserMapper.toResponse(userRepository.saveAndFlush(user));
    }

    @Transactional
    public void resetPassword(UUID id, ResetPasswordRequest request) {
        User user = findUser(id);
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        // Không cần gọi save: dirty checking tự UPDATE khi transaction commit
    }

    @Transactional
    public void deleteUser(UUID id) {
        User user = findUser(id);
        if (userRepository.hasRelatedData(id)) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "Không thể xoá: tài khoản đã có dữ liệu câu hỏi / bài thi / chấm điểm");
        }
        // Dòng phân công trong course_lecturers tự xoá theo (ON DELETE CASCADE ở V3)
        userRepository.delete(user);
    }

    // ===================== HÀM PHỤ =====================

    private User findUser(UUID id) {
        return userRepository.findWithRoleById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    private Role findRole(String roleName) {
        String name = roleName.trim().toUpperCase();
        return roleRepository.findByRoleName(name)
                .orElseThrow(() -> new BusinessException("Role không hợp lệ: " + name
                        + " (chỉ nhận ADMIN, LECTURER, STUDENT)"));
    }

    /** Email không phân biệt hoa thường -> luôn lưu chữ thường để so sánh trùng cho đúng. */
    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}
