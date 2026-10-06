package cocxanhcoder.viva.exam.system.exammgt.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CreateExamRequest(
    @NotNull(message = "ID môn học không được để trống")
    UUID courseId,

    @NotBlank(message = "Tên kỳ thi không được để trống")
    @Size(max = 255, message = "Tên kỳ thi tối đa 255 ký tự")
    String title,

    @NotNull(message = "Thời gian bắt đầu kỳ thi không được để trống")
    OffsetDateTime startTime,

    @NotNull(message = "Thời gian kết thúc kỳ thi không được để trống")
    OffsetDateTime endTime,

    @Valid
    ExamConfigDto examConfig
) {}
