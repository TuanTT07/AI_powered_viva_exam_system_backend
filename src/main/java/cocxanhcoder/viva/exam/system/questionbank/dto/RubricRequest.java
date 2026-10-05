package cocxanhcoder.viva.exam.system.questionbank.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record RubricRequest(
        @NotBlank @Size(max = 255) String rubricName,
        String description,
        @NotEmpty List<@Valid RubricCriterionRequest> criteria
) {
}
