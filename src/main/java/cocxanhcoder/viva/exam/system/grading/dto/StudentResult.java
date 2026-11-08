package cocxanhcoder.viva.exam.system.grading.dto;

import cocxanhcoder.viva.exam.system.grading.entity.CriterionScore;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** Kết quả 1 câu hỏi đã được công bố điểm, kèm transcript để sinh viên xem lại đã trả lời những gì. */
public record StudentResult(
        UUID questionAttemptId,
        Integer questionOrder,
        String questionContent,
        BigDecimal finalScore,
        BigDecimal maxScore,
        String feedback,
        List<CriterionScore> criteria,
        List<TranscriptTurnResponse> transcript,
        String audioUrl,
        OffsetDateTime publishedAt
) {}
