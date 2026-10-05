package cocxanhcoder.viva.exam.system.exammgt.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.UUID;

public record RescheduleCandidateRequest(
    @NotNull(message = "ID sinh viên không được để trống")
    UUID studentId,

    @NotNull(message = "Thời gian ca thi mới không được để trống")
    OffsetDateTime newScheduledStartTime,

    @NotNull(message = "Thời gian kết thúc ca thi mới không được để trống")
    OffsetDateTime newScheduledEndTime
) {}
