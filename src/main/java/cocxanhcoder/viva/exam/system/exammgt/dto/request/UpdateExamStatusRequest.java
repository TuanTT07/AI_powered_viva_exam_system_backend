package cocxanhcoder.viva.exam.system.exammgt.dto.request;

import cocxanhcoder.viva.exam.system.exammgt.entity.ExamStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateExamStatusRequest(
    @NotNull(message = "Trạng thái mới không được để trống")
    ExamStatus status
) {}
