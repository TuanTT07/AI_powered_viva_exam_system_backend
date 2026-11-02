package cocxanhcoder.viva.exam.system.interview.dto;

import java.util.UUID;

/** Receipt returned only after the interview answer has been persisted. */
public record AnswerReceipt(UUID transcriptTurnId, String text) {}
