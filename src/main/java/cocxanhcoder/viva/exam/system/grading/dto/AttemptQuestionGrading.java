package cocxanhcoder.viva.exam.system.grading.dto;

import cocxanhcoder.viva.exam.system.exammgt.entity.QuestionAttemptStatus;

/** An assigned main question, including questions with no persisted evaluation yet. */
public record AttemptQuestionGrading(int questionOrder, QuestionAttemptStatus questionStatus,
                                     EvaluationResponse evaluation) {}
