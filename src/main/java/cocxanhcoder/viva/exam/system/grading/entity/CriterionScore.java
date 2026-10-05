package cocxanhcoder.viva.exam.system.grading.entity;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Điểm của 1 tiêu chí rubric. KHÔNG phải entity (không có bảng riêng),
 * mà được lưu thành 1 phần tử trong cột JSONB evaluations.evaluation_details.
 * Ví dụ JSON: {"criterionId":"...","criterionName":"Đúng kiến thức","maxScore":4,"aiScore":3.5,"finalScore":3,"comment":"..."}
 */
public record CriterionScore(
        UUID criterionId,
        String criterionName,
        BigDecimal maxScore,
        BigDecimal aiScore,
        BigDecimal finalScore,
        String comment
) {
}
