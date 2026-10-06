package cocxanhcoder.viva.exam.system.exammgt.dto.request;

import jakarta.validation.constraints.Min;
import java.util.Map;

public record ExamConfigDto(
    @Min(value = 1, message = "Số câu hỏi chính tối đa phải từ 1 trở lên")
    Integer maxMainQuestions,

    @Min(value = 0, message = "Số câu hỏi đào sâu tối đa không được âm")
    Integer maxFollowUpQuestions,

    @Min(value = 10, message = "Thời gian trả lời mỗi lượt tối thiểu là 10 giây")
    Integer timeLimitPerTurnSeconds,

    Map<String, Double> bloomRatios,

    Boolean antiOverlapEnabled
) {
    public ExamConfigDto {
        if (maxMainQuestions == null) maxMainQuestions = 3;
        if (maxFollowUpQuestions == null) maxFollowUpQuestions = 2;
        if (timeLimitPerTurnSeconds == null) timeLimitPerTurnSeconds = 60;
        if (antiOverlapEnabled == null) antiOverlapEnabled = true;
    }
}
