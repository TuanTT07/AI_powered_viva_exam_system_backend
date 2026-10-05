package cocxanhcoder.viva.exam.system.exammgt.dto.response;

import cocxanhcoder.viva.exam.system.exammgt.entity.QuestionAttemptStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record QuestionAttemptResponse(
    UUID questionAttemptId,
    UUID questionId,
    String questionContent,
    String bloomLevel,
    Integer questionOrder,
    QuestionAttemptStatus status,
    OffsetDateTime startTime,
    OffsetDateTime endTime
) {}
