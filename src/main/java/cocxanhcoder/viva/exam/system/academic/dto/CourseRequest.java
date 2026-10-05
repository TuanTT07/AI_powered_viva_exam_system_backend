package cocxanhcoder.viva.exam.system.academic.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Body khi tạo / cập nhật môn học (dùng chung cho POST và PUT). */
public record CourseRequest(
        @NotBlank(message = "Mã môn không được để trống")
        @Size(max = 50, message = "Mã môn tối đa 50 ký tự")
        String courseCode,

        @NotBlank(message = "Tên môn không được để trống")
        @Size(max = 255, message = "Tên môn tối đa 255 ký tự")
        String courseName,

        @Size(max = 255, message = "Tên bộ môn tối đa 255 ký tự")
        String department // không bắt buộc
) {
}
