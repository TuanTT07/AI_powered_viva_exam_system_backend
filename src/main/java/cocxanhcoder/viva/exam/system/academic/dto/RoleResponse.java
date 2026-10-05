package cocxanhcoder.viva.exam.system.academic.dto;

import java.util.UUID;

/** Thông tin role trả về cho FE (dùng cho dropdown chọn role khi tạo tài khoản). */
public record RoleResponse(
        UUID id,
        String roleName,
        String description
) {
}
