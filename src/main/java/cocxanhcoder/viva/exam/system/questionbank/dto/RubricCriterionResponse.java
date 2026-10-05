package cocxanhcoder.viva.exam.system.questionbank.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record RubricCriterionResponse(UUID id, String criterionName, String description, BigDecimal maxScore) {
}
