package cocxanhcoder.viva.exam.system.exammgt.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record QuestionConfigDto(
        @NotNull List<@NotNull UUID> questionIds,
        @NotNull @Min(10) Integer timeLimitPerTurnSeconds,
        @NotNull @Min(0) Integer maxFollowUpQuestions) {}
