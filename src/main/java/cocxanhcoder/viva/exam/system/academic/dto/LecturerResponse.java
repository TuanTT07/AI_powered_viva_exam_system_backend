package cocxanhcoder.viva.exam.system.academic.dto;

import java.util.UUID;

/** Thông tin rút gọn của giảng viên (hiển thị trong chi tiết môn học). */
public record LecturerResponse(
        UUID id,
        String userCode,
        String fullName,
        String email
) {
}
