package cocxanhcoder.viva.exam.system.interview.service;

import cocxanhcoder.viva.exam.system.interview.dto.AnswerReceipt;
import java.time.Instant;
import java.util.UUID;

/** Submission/read contract. Creating AI questions stays inside interview. */
public interface InterviewAnswers {
    AnswerReceipt complete(UUID student, UUID attempt, UUID prompt, UUID questionAttempt,
                           UUID answer, String text, Instant started, Instant ended);
    AnswerReceipt findAnswer(UUID student, UUID attempt, UUID answer);
}
