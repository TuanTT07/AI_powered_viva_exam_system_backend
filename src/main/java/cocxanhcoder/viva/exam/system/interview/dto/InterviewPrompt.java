package cocxanhcoder.viva.exam.system.interview.dto;

import java.time.Instant;
import java.util.UUID;

public record InterviewPrompt(UUID questionTurnId, UUID questionAttemptId, String text,
                              int timeLimitSeconds, Instant deadlineAt) {
    public InterviewPrompt(UUID questionTurnId, UUID questionAttemptId, String text, int timeLimitSeconds) {
        this(questionTurnId, questionAttemptId, text, timeLimitSeconds, null);
    }
}
