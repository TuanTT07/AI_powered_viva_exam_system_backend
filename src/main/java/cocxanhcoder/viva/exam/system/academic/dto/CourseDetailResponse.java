package cocxanhcoder.viva.exam.system.academic.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** Chi tiết môn học: thông tin cơ bản + danh sách giảng viên phụ trách. */
public record CourseDetailResponse(
        UUID id,
        String courseCode,
        String courseName,
        String department,
        OffsetDateTime createdAt,
        List<LecturerResponse> lecturers
) {
}
