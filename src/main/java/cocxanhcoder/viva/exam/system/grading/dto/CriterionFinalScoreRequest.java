package cocxanhcoder.viva.exam.system.grading.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record CriterionFinalScoreRequest(
        @NotNull UUID criterionId,
        @NotNull @DecimalMin(value = "0", message = "Điểm chốt không được âm") BigDecimal finalScore,
        String comment
) {
}
