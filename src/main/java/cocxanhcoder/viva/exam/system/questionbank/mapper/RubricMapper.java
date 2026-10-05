package cocxanhcoder.viva.exam.system.questionbank.mapper;

import cocxanhcoder.viva.exam.system.questionbank.dto.RubricCriterionResponse;
import cocxanhcoder.viva.exam.system.questionbank.dto.RubricResponse;
import cocxanhcoder.viva.exam.system.questionbank.entity.Rubric;
import cocxanhcoder.viva.exam.system.questionbank.entity.RubricCriterion;
import org.springframework.stereotype.Component;

@Component
public class RubricMapper {

    public RubricResponse toResponse(Rubric rubric) {
        return new RubricResponse(rubric.getId(), rubric.getRubricName(), rubric.getDescription(),
                rubric.getCriteria().stream().map(this::toCriterionResponse).toList(),
                rubric.getTotalMaxScore(), rubric.getCreatedAt());
    }

    private RubricCriterionResponse toCriterionResponse(RubricCriterion criterion) {
        return new RubricCriterionResponse(criterion.getId(), criterion.getCriterionName(),
                criterion.getDescription(), criterion.getMaxScore());
    }
}
