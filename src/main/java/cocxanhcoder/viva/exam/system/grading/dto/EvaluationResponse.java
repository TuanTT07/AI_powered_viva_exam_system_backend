package cocxanhcoder.viva.exam.system.grading.dto;

import cocxanhcoder.viva.exam.system.grading.entity.CriterionScore;
import cocxanhcoder.viva.exam.system.grading.entity.EvaluationStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record EvaluationResponse(
        UUID id,
        UUID questionAttemptId,
        UUID questionId,
        String questionContent,
        UUID gradedById,
        String gradedByName,
        BigDecimal aiSuggestedScore,
        BigDecimal finalScore,
        BigDecimal maxScore,
        String overallFeedback,
        List<CriterionScore> criteria,
        EvaluationStatus status,
        OffsetDateTime createdAt,
        List<TranscriptTurnResponse> transcript,
        String audioUrl,
        OffsetDateTime publishedAt,
        OffsetDateTime updatedAt
) {
}
