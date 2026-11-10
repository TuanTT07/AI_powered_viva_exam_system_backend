package cocxanhcoder.viva.exam.system.grading.dto;

import java.util.List;

public record StudentAttemptResult(int totalQuestions, List<StudentResult> questions) {}
