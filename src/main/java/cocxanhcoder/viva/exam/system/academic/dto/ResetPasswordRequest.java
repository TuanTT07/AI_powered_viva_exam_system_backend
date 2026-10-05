package cocxanhcoder.viva.exam.system.academic.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Body khi Admin đặt lại mật khẩu cho 1 tài khoản. */
public record ResetPasswordRequest(
        @NotBlank(message = "Mật khẩu mới không được để trống")
        @Size(min = 6, max = 100, message = "Mật khẩu phải từ 6 đến 100 ký tự")
        String newPassword
) {
}
