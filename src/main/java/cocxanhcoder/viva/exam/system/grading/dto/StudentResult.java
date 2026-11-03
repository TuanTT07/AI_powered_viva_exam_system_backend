package cocxanhcoder.viva.exam.system.grading.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record StudentResult(Integer questionOrder, BigDecimal finalScore, String feedback, OffsetDateTime publishedAt) {}
