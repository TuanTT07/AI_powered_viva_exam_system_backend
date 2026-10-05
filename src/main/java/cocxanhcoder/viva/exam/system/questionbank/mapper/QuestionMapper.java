package cocxanhcoder.viva.exam.system.questionbank.mapper;

import cocxanhcoder.viva.exam.system.questionbank.dto.QuestionResponse;
import cocxanhcoder.viva.exam.system.questionbank.entity.Question;
import org.springframework.stereotype.Component;

@Component
public class QuestionMapper {

    public QuestionResponse toResponse(Question question) {
        return new QuestionResponse(question.getId(), question.getCourse().getId(), question.getCourse().getCourseCode(),
                question.getRubric() == null ? null : question.getRubric().getId(),
                question.getRubric() == null ? null : question.getRubric().getRubricName(),
                question.getCreatedBy().getId(), question.getContent(), question.getBloomLevel(), question.isAiGenerated(),
                question.getStatus(), question.getCreatedAt(), question.getUpdatedAt());
    }
}
