package cocxanhcoder.viva.exam.system.interview.dto;

import java.util.UUID;

public record InterviewPrompt(UUID questionTurnId, UUID questionAttemptId, String text, int timeLimitSeconds) {}
