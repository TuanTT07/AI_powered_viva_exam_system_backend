package cocxanhcoder.viva.exam.system.auth.dto;

import cocxanhcoder.viva.exam.system.academic.dto.UserResponse;

/**
 * Kết quả đăng nhập.
 * FE lưu accessToken lại và gửi kèm mỗi request: header "Authorization: Bearer <accessToken>".
 */
public record LoginResponse(
        String accessToken,
        String tokenType,   // luôn là "Bearer"
        long expiresIn,     // số giây token còn hiệu lực
        UserResponse user   // thông tin người dùng để FE hiển thị / điều hướng theo role
) {
}
