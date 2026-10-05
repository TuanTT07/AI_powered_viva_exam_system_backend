package cocxanhcoder.viva.exam.system.exammgt.service;

import cocxanhcoder.viva.exam.system.exammgt.entity.ExamAttempt;
import cocxanhcoder.viva.exam.system.exammgt.entity.QuestionAttempt;

import java.util.List;

public interface QuestionSelectorService {

    List<QuestionAttempt> generateQuestionsForAttempt(ExamAttempt attempt);
}
