package cocxanhcoder.viva.exam.system.questionbank.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record RubricCriterionRequest(
        @NotBlank @Size(max = 255) String criterionName,
        String description,
        @NotNull @DecimalMin(value = "0.01") BigDecimal maxScore
) {
}
