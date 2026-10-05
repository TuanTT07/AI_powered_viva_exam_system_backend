package cocxanhcoder.viva.exam.system.academic.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body khi Admin tạo tài khoản mới.
 * Các annotation @NotBlank, @Email... chỉ có tác dụng khi controller đánh dấu @Valid.
 * Sai -> GlobalExceptionHandler trả 400 kèm danh sách lỗi từng field.
 */
public record CreateUserRequest(
        @NotBlank(message = "Mã người dùng không được để trống")
        @Size(max = 50, message = "Mã người dùng tối đa 50 ký tự")
        String userCode,

        @NotBlank(message = "Họ tên không được để trống")
        @Size(max = 255, message = "Họ tên tối đa 255 ký tự")
        String fullName,

        @NotBlank(message = "Email không được để trống")
        @Email(message = "Email không đúng định dạng")
        @Size(max = 255, message = "Email tối đa 255 ký tự")
        String email,

        @NotBlank(message = "Mật khẩu không được để trống")
        @Size(min = 6, max = 100, message = "Mật khẩu phải từ 6 đến 100 ký tự")
        String password,

        @NotBlank(message = "Role không được để trống")
        String roleName // ADMIN / LECTURER / STUDENT
) {
}
