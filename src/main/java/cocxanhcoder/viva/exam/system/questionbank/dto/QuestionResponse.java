package cocxanhcoder.viva.exam.system.questionbank.dto;

import cocxanhcoder.viva.exam.system.questionbank.entity.BloomLevel;
import cocxanhcoder.viva.exam.system.questionbank.entity.QuestionStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record QuestionResponse(
        UUID id,
        UUID courseId,
        String courseCode,
        UUID rubricId,
        String rubricName,
        UUID createdById,
        String content,
        BloomLevel bloomLevel,
        boolean aiGenerated,
        QuestionStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
