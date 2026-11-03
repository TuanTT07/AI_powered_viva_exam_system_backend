package cocxanhcoder.viva.exam.system.interview.service;

import cocxanhcoder.viva.exam.system.exammgt.entity.ExamAttempt;
import cocxanhcoder.viva.exam.system.interview.entity.TranscriptTurn;

import java.time.Instant;

final class InterviewTiming {
    static final int FINALIZATION_GRACE_SECONDS = 10;

    private InterviewTiming() {}

    static int turnSeconds(ExamAttempt attempt) {
        var config = attempt.getExam() == null ? null : attempt.getExam().getExamConfig();
        Object value = config == null ? null : config.get("timeLimitPerTurnSeconds");
        return value instanceof Number n && n.intValue() >= 10 ? n.intValue() : 60;
    }

    static Instant deadline(TranscriptTurn turn) {
        if (turn.getDeadlineAt() != null) return turn.getDeadlineAt();
        if (turn.getCreatedAt() == null) return null;
        return turn.getCreatedAt().toInstant()
                .plusSeconds(turnSeconds(turn.getQuestionAttempt().getExamAttempt()));
    }
}
