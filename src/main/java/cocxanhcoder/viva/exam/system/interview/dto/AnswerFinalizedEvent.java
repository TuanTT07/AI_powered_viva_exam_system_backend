package cocxanhcoder.viva.exam.system.interview.dto;

import java.time.Instant;
import java.util.UUID;

public record AnswerFinalizedEvent(UUID answerId, UUID questionTurnId,
        UUID questionAttemptId, UUID examAttemptId, UUID studentId,
        UUID transcriptTurnId, String text, Instant startedAt, Instant endedAt) {}
