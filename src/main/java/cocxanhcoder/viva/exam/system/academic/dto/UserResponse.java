package cocxanhcoder.viva.exam.system.academic.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Thông tin tài khoản trả về cho FE.
 * Chú ý: KHÔNG có passwordHash - không bao giờ trả mật khẩu (kể cả đã mã hoá) ra API.
 */
public record UserResponse(
        UUID id,
        String userCode,
        String fullName,
        String email,
        String roleName,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
