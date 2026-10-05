package cocxanhcoder.viva.exam.system.questionbank.service;

import cocxanhcoder.viva.exam.system.common.exception.BusinessException;
import cocxanhcoder.viva.exam.system.common.exception.ResourceNotFoundException;
import cocxanhcoder.viva.exam.system.questionbank.dto.RubricCriterionRequest;
import cocxanhcoder.viva.exam.system.questionbank.dto.RubricRequest;
import cocxanhcoder.viva.exam.system.questionbank.dto.RubricResponse;
import cocxanhcoder.viva.exam.system.questionbank.entity.Rubric;
import cocxanhcoder.viva.exam.system.questionbank.entity.RubricCriterion;
import cocxanhcoder.viva.exam.system.questionbank.mapper.RubricMapper;
import cocxanhcoder.viva.exam.system.questionbank.repository.QuestionRepository;
import cocxanhcoder.viva.exam.system.questionbank.repository.RubricRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class RubricService {

    private final RubricRepository rubricRepository;
    private final QuestionRepository questionRepository;
    private final RubricMapper rubricMapper;

    public RubricService(RubricRepository rubricRepository, QuestionRepository questionRepository, RubricMapper rubricMapper) {
        this.rubricRepository = rubricRepository;
        this.questionRepository = questionRepository;
        this.rubricMapper = rubricMapper;
    }

    public RubricResponse create(RubricRequest request) {
        Rubric rubric = new Rubric();
        applyRequest(rubric, request);
        return rubricMapper.toResponse(rubricRepository.save(rubric));
    }

    @Transactional(readOnly = true)
    public List<RubricResponse> findAll() {
        return rubricRepository.findAll().stream()
                .map(rubric -> rubricRepository.findWithCriteriaById(rubric.getId()).orElseThrow())
                .map(rubricMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public RubricResponse findById(UUID id) {
        return rubricMapper.toResponse(findEntity(id));
    }

    public RubricResponse update(UUID id, RubricRequest request) {
        Rubric rubric = findEntity(id);
        applyRequest(rubric, request);
        return rubricMapper.toResponse(rubric);
    }

    public void delete(UUID id) {
        Rubric rubric = findEntity(id);
        if (questionRepository.countByRubricId(id) > 0) {
            throw new BusinessException("Cannot delete a rubric that is assigned to questions", HttpStatus.CONFLICT);
        }
        rubricRepository.delete(rubric);
    }

    private Rubric findEntity(UUID id) {
        return rubricRepository.findWithCriteriaById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rubric", id));
    }

    private void applyRequest(Rubric rubric, RubricRequest request) {
        rubric.setRubricName(request.rubricName().trim());
        rubric.setDescription(request.description());
        rubric.getCriteria().clear();
        for (RubricCriterionRequest criterionRequest : request.criteria()) {
            RubricCriterion criterion = new RubricCriterion();
            criterion.setCriterionName(criterionRequest.criterionName().trim());
            criterion.setDescription(criterionRequest.description());
            criterion.setMaxScore(criterionRequest.maxScore());
            rubric.addCriterion(criterion);
        }
    }
}
