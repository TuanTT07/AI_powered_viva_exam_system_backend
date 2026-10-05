package cocxanhcoder.viva.exam.system.academic.mapper;

import cocxanhcoder.viva.exam.system.academic.dto.LecturerResponse;
import cocxanhcoder.viva.exam.system.academic.dto.RoleResponse;
import cocxanhcoder.viva.exam.system.academic.dto.UserResponse;
import cocxanhcoder.viva.exam.system.academic.entity.Role;
import cocxanhcoder.viva.exam.system.academic.entity.User;

/**
 * Chuyển đổi Entity -> DTO cho User / Role.
 * Viết tay (không dùng MapStruct) để thấy rõ field nào được trả ra ngoài.
 * Lưu ý: phải gọi trong @Transactional (ở service) vì user.getRole() là LAZY.
 */
public final class UserMapper {

    private UserMapper() {
        // class tiện ích, không cho new
    }

    public static UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getUserCode(),
                user.getFullName(),
                user.getEmail(),
                user.getRole().getRoleName(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    public static LecturerResponse toLecturerResponse(User user) {
        return new LecturerResponse(
                user.getId(),
                user.getUserCode(),
                user.getFullName(),
                user.getEmail()
        );
    }

    public static RoleResponse toRoleResponse(Role role) {
        return new RoleResponse(role.getId(), role.getRoleName(), role.getDescription());
    }
}
