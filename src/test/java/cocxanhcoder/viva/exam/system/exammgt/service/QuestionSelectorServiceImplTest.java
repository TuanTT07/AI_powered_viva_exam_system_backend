package cocxanhcoder.viva.exam.system.exammgt.service;

import cocxanhcoder.viva.exam.system.academic.entity.Course;
import cocxanhcoder.viva.exam.system.common.exception.BusinessException;
import cocxanhcoder.viva.exam.system.exammgt.entity.Exam;
import cocxanhcoder.viva.exam.system.exammgt.entity.ExamAttempt;
import cocxanhcoder.viva.exam.system.exammgt.entity.QuestionAttempt;
import cocxanhcoder.viva.exam.system.exammgt.repository.ExamAttemptRepository;
import cocxanhcoder.viva.exam.system.exammgt.service.impl.QuestionSelectorServiceImpl;
import cocxanhcoder.viva.exam.system.questionbank.entity.BloomLevel;
import cocxanhcoder.viva.exam.system.questionbank.entity.Question;
import cocxanhcoder.viva.exam.system.questionbank.entity.QuestionStatus;
import cocxanhcoder.viva.exam.system.questionbank.repository.QuestionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuestionSelectorServiceImplTest {

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private ExamAttemptRepository examAttemptRepository;

    @InjectMocks
    private QuestionSelectorServiceImpl questionSelectorService;

    private Course course;
    private Exam exam;
    private ExamAttempt attempt;
    private Question q1;
    private Question q2;
    private Question q3;

    @BeforeEach
    void setUp() {
        course = new Course();
        course.setId(UUID.randomUUID());

        exam = new Exam();
        exam.setId(UUID.randomUUID());
        exam.setCourse(course);

        Map<String, Object> config = new HashMap<>();
        config.put("maxMainQuestions", 2);
        config.put("antiOverlapEnabled", true);
        exam.setExamConfig(config);

        attempt = new ExamAttempt();
        attempt.setId(UUID.randomUUID());
        attempt.setExam(exam);
        attempt.setSlotNumber(2);

        q1 = new Question();
        q1.setId(UUID.randomUUID());
        q1.setContent("Câu hỏi 1");
        q1.setBloomLevel(BloomLevel.REMEMBER);
        q1.setStatus(QuestionStatus.APPROVED);

        q2 = new Question();
        q2.setId(UUID.randomUUID());
        q2.setContent("Câu hỏi 2");
        q2.setBloomLevel(BloomLevel.UNDERSTAND);
        q2.setStatus(QuestionStatus.APPROVED);

        q3 = new Question();
        q3.setId(UUID.randomUUID());
        q3.setContent("Câu hỏi 3");
        q3.setBloomLevel(BloomLevel.APPLY);
        q3.setStatus(QuestionStatus.APPROVED);
    }

    @Test
    @DisplayName("Bốc thăm câu hỏi thành công và áp dụng Anti-Overlap lọc bỏ câu hỏi ca trước")
    void generateQuestions_AntiOverlap_Success() {
        when(questionRepository.findByCourseIdAndStatus(course.getId(), QuestionStatus.APPROVED))
                .thenReturn(List.of(q1, q2, q3));
        // Lần thi ca 1 đã dùng q1 -> ca 2 sẽ loại q1, chọn q2 và q3
        when(examAttemptRepository.findRecentUsedQuestionIds(exam.getId(), 1, 1))
                .thenReturn(List.of(q1.getId()));

        List<QuestionAttempt> results = questionSelectorService.generateQuestionsForAttempt(attempt);

        assertNotNull(results);
        assertEquals(2, results.size());
        assertFalse(results.stream().anyMatch(qa -> qa.getQuestion().getId().equals(q1.getId())));
    }

    @Test
    @DisplayName("Ném ngoại lệ BusinessException khi môn học chưa có câu hỏi được duyệt")
    void generateQuestions_NoApprovedQuestions_ThrowsException() {
        when(questionRepository.findByCourseIdAndStatus(course.getId(), QuestionStatus.APPROVED))
                .thenReturn(List.of());

        assertThrows(BusinessException.class, () -> questionSelectorService.generateQuestionsForAttempt(attempt));
    }
}
