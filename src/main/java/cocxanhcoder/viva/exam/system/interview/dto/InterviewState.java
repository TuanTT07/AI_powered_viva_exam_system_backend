package cocxanhcoder.viva.exam.system.interview.dto;

import java.time.Instant;
import java.util.UUID;

public record InterviewState(UUID attemptId, Status status, Prompt prompt) {
    public enum Status { READY, AWAITING_ANSWER, PROCESSING, COMPLETED }

    public record Prompt(UUID questionAttemptId, UUID questionTurnId, String text, Instant deadlineAt) {}
}
