package cocxanhcoder.viva.exam.system.questionbank.service;

import cocxanhcoder.viva.exam.system.academic.entity.Course;
import cocxanhcoder.viva.exam.system.academic.entity.User;
import cocxanhcoder.viva.exam.system.academic.repository.CourseRepository;
import cocxanhcoder.viva.exam.system.academic.repository.UserRepository;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final RubricRepository rubricRepository;
    private final QuestionMapper questionMapper;

    public QuestionService(QuestionRepository questionRepository, CourseRepository courseRepository,
                           UserRepository userRepository, RubricRepository rubricRepository, QuestionMapper questionMapper) {
        this.questionRepository = questionRepository;
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
        this.rubricRepository = rubricRepository;
        this.questionMapper = questionMapper;
    }

    public QuestionResponse create(QuestionRequest request) {
        Question question = new Question();
        applyRequest(question, request);
        question.setStatus(QuestionStatus.DRAFT);
        return questionMapper.toResponse(questionRepository.save(question));
    }

    @Transactional(readOnly = true)
    public Page<QuestionResponse> search(UUID courseId, QuestionStatus status, BloomLevel bloomLevel,
                                         String keyword, Pageable pageable) {
        String normalizedKeyword = keyword == null || keyword.isBlank() ? null : keyword.trim();
        return questionRepository.search(courseId, status, bloomLevel, normalizedKeyword, pageable)
                .map(questionMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public QuestionResponse findById(UUID id) {
        return questionMapper.toResponse(findEntity(id));
    }

    public QuestionResponse update(UUID id, QuestionRequest request) {
        Question question = findEntity(id);
        applyRequest(question, request);
        return questionMapper.toResponse(question);
    }

    public QuestionResponse approve(UUID id) {
        Question question = findEntity(id);
        question.setStatus(QuestionStatus.APPROVED);
        return questionMapper.toResponse(question);
    }

    public void delete(UUID id) {
        questionRepository.delete(findEntity(id));
    }

    private Question findEntity(UUID id) {
        return questionRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Question", id));
    }

    private void applyRequest(Question question, QuestionRequest request) {
        Course course = courseRepository.findById(request.courseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course", request.courseId()));
        User creator = userRepository.findById(request.createdById())
                .orElseThrow(() -> new ResourceNotFoundException("User", request.createdById()));
        Rubric rubric = request.rubricId() == null ? null : rubricRepository.findById(request.rubricId())
                .orElseThrow(() -> new ResourceNotFoundException("Rubric", request.rubricId()));
        question.setCourse(course);
        question.setCreatedBy(creator);
        question.setRubric(rubric);
        question.setContent(request.content().trim());
        question.setBloomLevel(request.bloomLevel());
        question.setAiGenerated(request.aiGenerated());
    }
}
