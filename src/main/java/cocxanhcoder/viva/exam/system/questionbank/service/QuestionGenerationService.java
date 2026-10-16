package cocxanhcoder.viva.exam.system.questionbank.service;

import cocxanhcoder.viva.exam.system.academic.entity.Document;
import cocxanhcoder.viva.exam.system.academic.entity.DocumentIndexStatus;
import cocxanhcoder.viva.exam.system.academic.repository.CourseRepository;
import cocxanhcoder.viva.exam.system.academic.repository.DocumentRepository;
import cocxanhcoder.viva.exam.system.common.exception.BusinessException;
import cocxanhcoder.viva.exam.system.common.exception.ResourceNotFoundException;
import cocxanhcoder.viva.exam.system.questionbank.dto.GenerateQuestionsRequest;
import cocxanhcoder.viva.exam.system.questionbank.dto.QuestionResponse;
import cocxanhcoder.viva.exam.system.questionbank.entity.BloomLevel;
import cocxanhcoder.viva.exam.system.questionbank.repository.RubricRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Transactional(readOnly = true)
public class QuestionGenerationService {
    private final CourseAccessService access;
    private final CourseRepository courses;
    private final DocumentRepository documents;
    private final RubricRepository rubrics;
    private final AiClient ai;
    private final QuestionService questions;

    public QuestionGenerationService(CourseAccessService access, CourseRepository courses, DocumentRepository documents, RubricRepository rubrics, AiClient ai, QuestionService questions) {
        this.access = access;
        this.courses = courses;
        this.documents = documents;
        this.rubrics = rubrics;
        this.ai = ai;
        this.questions = questions;
    }

    @Transactional
    public List<QuestionResponse> generate(UUID actor, UUID courseId, GenerateQuestionsRequest request) {
        access.requireManager(actor, courseId);
        var course = courses.findById(courseId).orElseThrow(() -> new ResourceNotFoundException("Course", courseId));
        if (course.getOpenAiVectorStoreId() == null || course.getOpenAiVectorStoreId().isBlank())
            throw new BusinessException(HttpStatus.CONFLICT, "Môn học chưa có tài liệu được lập chỉ mục");

        if (request.rubricId() != null && !rubrics.existsByIdAndCourseId(request.rubricId(), courseId))
            throw new BusinessException("Rubric không thuộc môn học");

        List<Document> selected = request.documentIds() == null || request.documentIds().isEmpty() ? documents.findByCourseIdOrderByCreatedAtDesc(courseId) : request.documentIds().stream().map(id -> documents.findById(id).orElseThrow(() -> new ResourceNotFoundException("Document", id))).toList();

        if (selected.isEmpty() || selected.stream().anyMatch(d -> !d.getCourse().getId().equals(courseId) || d.getIndexingStatus() != DocumentIndexStatus.READY))
            throw new BusinessException(HttpStatus.CONFLICT, "Chỉ có thể sinh câu hỏi từ tài liệu cùng môn đã index thành công");

        List<BloomLevel> levels = request.bloomLevels() == null || request.bloomLevels().isEmpty() ? List.of(BloomLevel.UNDERSTAND) : request.bloomLevels();

        List<AiClient.GeneratedQuestion> output = ai.generateQuestions(course.getOpenAiVectorStoreId(), request.topic(), request.count(), levels.stream().map(Enum::name).toList());

        Set<Document> sources = new HashSet<>(selected);

        List<QuestionResponse> result = new ArrayList<>();

        for (AiClient.GeneratedQuestion q : output) {
            BloomLevel level;
            try {
                level = BloomLevel.valueOf(q.bloomLevel());
            } catch (Exception e) {
                throw new BusinessException(HttpStatus.BAD_GATEWAY, "OpenAI trả Bloom level không hợp lệ");
            }
            if (!levels.contains(level))
                throw new BusinessException(HttpStatus.BAD_GATEWAY, "OpenAI trả Bloom level ngoài yêu cầu");
            result.add(questions.createDraft(actor, courseId, q.content(), level, request.rubricId(), true, sources));
        }
        return result;
    }
}
