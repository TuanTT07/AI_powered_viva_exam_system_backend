package cocxanhcoder.viva.exam.system.questionbank.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record RubricResponse(
        UUID id,
        String rubricName,
        String description,
        List<RubricCriterionResponse> criteria,
        BigDecimal totalMaxScore,
        OffsetDateTime createdAt
) {
}
