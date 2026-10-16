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
import cocxanhcoder.viva.exam.system.academic.repository.CourseRepository;
import cocxanhcoder.viva.exam.system.academic.entity.Course;
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
    private final CourseRepository courseRepository;
    private final CourseAccessService courseAccessService;

    public RubricService(RubricRepository rubricRepository, QuestionRepository questionRepository, RubricMapper rubricMapper,
                         CourseRepository courseRepository, CourseAccessService courseAccessService) {
        this.rubricRepository = rubricRepository;
        this.questionRepository = questionRepository;
        this.rubricMapper = rubricMapper;
        this.courseRepository = courseRepository;
        this.courseAccessService = courseAccessService;
    }

    public RubricResponse create(UUID actorId, UUID courseId, RubricRequest request) {
        courseAccessService.requireManager(actorId, courseId);
        Course course = courseRepository.findById(courseId).orElseThrow(() -> new ResourceNotFoundException("Course", courseId));
        Rubric rubric = new Rubric();
        rubric.setCourse(course);
        applyRequest(rubric, request);
        return rubricMapper.toResponse(rubricRepository.save(rubric));
    }

    @Transactional(readOnly = true)
    public List<RubricResponse> findAll(UUID actorId, UUID courseId) {
        courseAccessService.requireManager(actorId, courseId);
        return rubricRepository.findByCourseIdOrderByCreatedAtDesc(courseId).stream()
                .map(rubric -> rubricRepository.findWithCriteriaById(rubric.getId()).orElseThrow())
                .map(rubricMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public RubricResponse findById(UUID actorId, UUID courseId, UUID id) {
        courseAccessService.requireManager(actorId, courseId);
        Rubric rubric = findEntity(id);
        requireCourse(rubric, courseId);
        return rubricMapper.toResponse(rubric);
    }

    public RubricResponse update(UUID actorId, UUID courseId, UUID id, RubricRequest request) {
        courseAccessService.requireManager(actorId, courseId);
        Rubric rubric = findEntity(id);
        requireCourse(rubric, courseId);
        applyRequest(rubric, request);
        return rubricMapper.toResponse(rubric);
    }

    public void delete(UUID actorId, UUID courseId, UUID id) {
        courseAccessService.requireManager(actorId, courseId);
        Rubric rubric = findEntity(id);
        requireCourse(rubric, courseId);
        if (questionRepository.countByRubricId(id) > 0) {
            throw new BusinessException(HttpStatus.CONFLICT,"Cannot delete a rubric that is assigned to questions" );
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

    private void requireCourse(Rubric rubric, UUID courseId) {
        if (!rubric.getCourse().getId().equals(courseId)) {
            throw new ResourceNotFoundException("Rubric", rubric.getId());
        }
    }
}
