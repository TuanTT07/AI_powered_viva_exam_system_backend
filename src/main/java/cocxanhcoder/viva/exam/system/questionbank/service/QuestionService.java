package cocxanhcoder.viva.exam.system.questionbank.service;

import cocxanhcoder.viva.exam.system.academic.entity.Course;
import cocxanhcoder.viva.exam.system.academic.entity.User;
import cocxanhcoder.viva.exam.system.academic.repository.CourseRepository;
import cocxanhcoder.viva.exam.system.common.exception.ResourceNotFoundException;
import cocxanhcoder.viva.exam.system.questionbank.dto.QuestionRequest;
import cocxanhcoder.viva.exam.system.questionbank.dto.QuestionResponse;
import cocxanhcoder.viva.exam.system.questionbank.entity.BloomLevel;
import cocxanhcoder.viva.exam.system.questionbank.entity.Question;
import cocxanhcoder.viva.exam.system.questionbank.entity.QuestionStatus;
import cocxanhcoder.viva.exam.system.questionbank.entity.Rubric;
import cocxanhcoder.viva.exam.system.questionbank.mapper.QuestionMapper;
import cocxanhcoder.viva.exam.system.questionbank.repository.QuestionRepository;
import cocxanhcoder.viva.exam.system.questionbank.repository.RubricRepository;
import cocxanhcoder.viva.exam.system.exammgt.repository.QuestionAttemptRepository;
import cocxanhcoder.viva.exam.system.common.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.Set;

@Service
@Transactional
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final CourseRepository courseRepository;
    private final RubricRepository rubricRepository;
    private final QuestionMapper questionMapper;
    private final CourseAccessService courseAccessService;
    private final QuestionAttemptRepository questionAttemptRepository;

    public QuestionService(QuestionRepository questionRepository, CourseRepository courseRepository,
                           RubricRepository rubricRepository, QuestionMapper questionMapper,
                           CourseAccessService courseAccessService, QuestionAttemptRepository questionAttemptRepository) {
        this.questionRepository = questionRepository;
        this.courseRepository = courseRepository;
        this.rubricRepository = rubricRepository;
        this.questionMapper = questionMapper;
        this.courseAccessService = courseAccessService;
        this.questionAttemptRepository = questionAttemptRepository;
    }

    public QuestionResponse create(UUID actorId, QuestionRequest request) {
        User actor = courseAccessService.requireManager(actorId, request.courseId());
        Question question = new Question();
        applyRequest(question, request, actor);
        question.setStatus(QuestionStatus.DRAFT);
        return questionMapper.toResponse(questionRepository.save(question));
    }

    /** Used by import and AI flows after they have validated their complete input. */
    public QuestionResponse createDraft(UUID actorId, UUID courseId, String content, BloomLevel bloomLevel, UUID rubricId,
                                        boolean aiGenerated, Set<cocxanhcoder.viva.exam.system.academic.entity.Document> sources) {
        User actor = courseAccessService.requireManager(actorId, courseId);
        Question question = new Question();
        applyRequest(question, new QuestionRequest(courseId, rubricId, content, bloomLevel, aiGenerated), actor);
        question.setStatus(QuestionStatus.DRAFT);
        if (sources != null) question.getSourceDocuments().addAll(sources);
        return questionMapper.toResponse(questionRepository.save(question));
    }

    @Transactional(readOnly = true)
    public Page<QuestionResponse> search(UUID actorId, UUID courseId, QuestionStatus status, BloomLevel bloomLevel,
                                         String keyword, Pageable pageable) {
        courseAccessService.requireManager(actorId, courseId);
        String normalizedKeyword = keyword == null || keyword.isBlank() ? null : keyword.trim();
        return questionRepository.search(courseId, status, bloomLevel, normalizedKeyword, pageable)
                .map(questionMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public QuestionResponse findById(UUID actorId, UUID id) {
        Question question = findEntity(id);
        courseAccessService.requireManager(actorId, question.getCourse().getId());
        return questionMapper.toResponse(question);
    }

    public QuestionResponse update(UUID actorId, UUID id, QuestionRequest request) {
        Question question = findEntity(id);
        courseAccessService.requireManager(actorId, question.getCourse().getId());
        User actor = courseAccessService.requireManager(actorId, request.courseId());
        applyRequest(question, request, actor);
        question.setStatus(QuestionStatus.DRAFT);
        return questionMapper.toResponse(question);
    }

    public QuestionResponse approve(UUID actorId, UUID id) {
        Question question = findEntity(id);
        courseAccessService.requireManager(actorId, question.getCourse().getId());
        question.setStatus(QuestionStatus.APPROVED);
        return questionMapper.toResponse(question);
    }

    public void delete(UUID actorId, UUID id) {
        Question question = findEntity(id);
        courseAccessService.requireManager(actorId, question.getCourse().getId());
        if (questionAttemptRepository.existsByQuestionId(id)) {
            throw new BusinessException(HttpStatus.CONFLICT, "Không thể xoá câu hỏi đã được cấp vào lần thi");
        }
        questionRepository.delete(question);
    }

    private Question findEntity(UUID id) {
        return questionRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Question", id));
    }

    private void applyRequest(Question question, QuestionRequest request, User actor) {
        Course course = courseRepository.findById(request.courseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course", request.courseId()));
        Rubric rubric = request.rubricId() == null ? null : rubricRepository.findById(request.rubricId())
                .orElseThrow(() -> new ResourceNotFoundException("Rubric", request.rubricId()));
        if (rubric != null && !rubric.getCourse().getId().equals(course.getId())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Rubric phải thuộc cùng môn học với câu hỏi");
        }
        question.setCourse(course);
        if (question.getCreatedBy() == null) question.setCreatedBy(actor);
        question.setRubric(rubric);
        question.setContent(request.content().trim());
        question.setBloomLevel(request.bloomLevel());
        question.setAiGenerated(request.aiGenerated());
    }
}
