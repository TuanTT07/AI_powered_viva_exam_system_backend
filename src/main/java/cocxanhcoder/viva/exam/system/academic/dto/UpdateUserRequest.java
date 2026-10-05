package cocxanhcoder.viva.exam.system.academic.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body khi Admin cập nhật tài khoản.
 * userCode (MSSV / mã GV) không cho sửa; đổi mật khẩu dùng API riêng.
 */
public record UpdateUserRequest(
        @NotBlank(message = "Họ tên không được để trống")
        @Size(max = 255, message = "Họ tên tối đa 255 ký tự")
        String fullName,

        @NotBlank(message = "Email không được để trống")
        @Email(message = "Email không đúng định dạng")
        @Size(max = 255, message = "Email tối đa 255 ký tự")
        String email,

        @NotBlank(message = "Role không được để trống")
        String roleName
) {
}
