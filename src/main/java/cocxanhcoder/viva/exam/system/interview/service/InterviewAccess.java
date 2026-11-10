package cocxanhcoder.viva.exam.system.interview.service;

import cocxanhcoder.viva.exam.system.interview.dto.InterviewPrompt;
import java.time.Instant;
import java.util.UUID;

/** Public interview contract for clients such as speech; never exposes JPA entities. */
public interface InterviewAccess {
    void requireActiveAttempt(UUID studentId, UUID attemptId);
    Instant beginAnswer(UUID studentId, UUID attemptId, UUID questionTurnId, UUID questionAttemptId, int limitSeconds);
    InterviewPrompt requirePrompt(UUID studentId, UUID attemptId, UUID questionTurnId);
}
