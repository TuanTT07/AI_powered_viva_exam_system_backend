package cocxanhcoder.viva.exam.system.exammgt.dto.request;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

public record AssignCandidatesRequest(
    @NotEmpty(message = "Danh sách ID sinh viên không được để trống")
    List<UUID> studentIds
) {}
