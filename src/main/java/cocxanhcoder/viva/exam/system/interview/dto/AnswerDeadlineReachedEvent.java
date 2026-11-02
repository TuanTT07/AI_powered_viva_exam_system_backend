package cocxanhcoder.viva.exam.system.interview.dto;

import java.util.UUID;

/** Published by interview's authoritative timer, outside transactions holding interview locks. */
public record AnswerDeadlineReachedEvent(UUID examAttemptId, UUID questionTurnId) {}
