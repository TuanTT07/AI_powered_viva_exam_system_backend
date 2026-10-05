package cocxanhcoder.viva.exam.system.academic.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Thông tin cơ bản của môn học (dùng cho danh sách). */
public record CourseResponse(
        UUID id,
        String courseCode,
        String courseName,
        String department,
        OffsetDateTime createdAt
) {
}
