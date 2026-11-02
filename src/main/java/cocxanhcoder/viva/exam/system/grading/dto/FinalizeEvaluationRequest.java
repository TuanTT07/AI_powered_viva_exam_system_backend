package cocxanhcoder.viva.exam.system.grading.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/** Giảng viên điều chỉnh điểm từng tiêu chí (dựa trên gợi ý AI) rồi chốt điểm cuối. */
public record FinalizeEvaluationRequest(
        @NotEmpty List<@Valid CriterionFinalScoreRequest> criteria,
        String overallFeedback
) {
}
