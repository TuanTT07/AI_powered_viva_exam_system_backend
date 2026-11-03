package cocxanhcoder.viva.exam.system.exammgt.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record AutoScheduleRequest(
    @NotEmpty(message = "Danh sách ID sinh viên không được để trống")
    List<UUID> studentIds,

    @NotNull(message = "Thời lượng ca thi (phút) không được để trống")
    @Min(value = 5, message = "Thời lượng ca thi tối thiểu 5 phút")
    Integer slotDurationMinutes,

    @Min(value = 0, message = "Thời gian nghỉ giữa các ca không được âm")
    Integer breakDurationMinutes,

    ScheduleMode mode
) {
    public enum ScheduleMode { SIMULTANEOUS, SEQUENTIAL }

    public AutoScheduleRequest(List<UUID> studentIds, Integer slotDurationMinutes, Integer breakDurationMinutes) {
        this(studentIds, slotDurationMinutes, breakDurationMinutes, ScheduleMode.SIMULTANEOUS);
    }

    public AutoScheduleRequest {
        if (mode == null) mode = ScheduleMode.SIMULTANEOUS;
        if (breakDurationMinutes == null) breakDurationMinutes = 2;
    }
}
