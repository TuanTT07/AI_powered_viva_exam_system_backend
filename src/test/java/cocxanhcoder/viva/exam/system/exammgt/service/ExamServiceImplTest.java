package cocxanhcoder.viva.exam.system.exammgt.service;

import cocxanhcoder.viva.exam.system.academic.entity.Course;
import cocxanhcoder.viva.exam.system.academic.repository.CourseRepository;
import cocxanhcoder.viva.exam.system.common.exception.BusinessException;
import cocxanhcoder.viva.exam.system.common.exception.ResourceNotFoundException;
import cocxanhcoder.viva.exam.system.exammgt.dto.request.CreateExamRequest;
import cocxanhcoder.viva.exam.system.exammgt.dto.request.ExamConfigDto;
import cocxanhcoder.viva.exam.system.exammgt.dto.response.ExamResponse;
import cocxanhcoder.viva.exam.system.exammgt.entity.Exam;
import cocxanhcoder.viva.exam.system.exammgt.entity.ExamStatus;
import cocxanhcoder.viva.exam.system.exammgt.repository.ExamAttemptRepository;
import cocxanhcoder.viva.exam.system.exammgt.repository.ExamRepository;
import cocxanhcoder.viva.exam.system.exammgt.service.impl.ExamServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExamServiceImplTest {

    @Mock
    private ExamRepository examRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private ExamAttemptRepository examAttemptRepository;

    @Mock private cocxanhcoder.viva.exam.system.questionbank.repository.QuestionRepository questionRepository;
    @Mock private cocxanhcoder.viva.exam.system.questionbank.service.CourseAccessService courseAccess;

    @InjectMocks
    private ExamServiceImpl examService;

    private UUID courseId;
    private Course course;

    @BeforeEach
    void setUp() {
        courseId = UUID.randomUUID();
        course = new Course();
        course.setId(courseId);
        course.setCourseCode("CS101");
        course.setCourseName("Lập trình Java");
    }

    @Test
    @DisplayName("Tạo kỳ thi thành công khi thông tin hợp lệ")
    void createExam_Success() {
        OffsetDateTime start = OffsetDateTime.now().plusDays(1);
        OffsetDateTime end = start.plusHours(2);
        ExamConfigDto config = new ExamConfigDto(3, 2, 60, Map.of(), true);
        CreateExamRequest request = new CreateExamRequest(courseId, "Kỳ thi giữa kỳ Java", start, end, config);

        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(examRepository.save(any(Exam.class))).thenAnswer(invocation -> {
            Exam savedExam = invocation.getArgument(0);
            savedExam.setId(UUID.randomUUID());
            return savedExam;
        });

        ExamResponse response = examService.createExam(request);

        assertNotNull(response);
        assertNotNull(response.id());
        assertEquals("Kỳ thi giữa kỳ Java", response.title());
        assertEquals(ExamStatus.DRAFT, response.status());
        verify(examRepository).save(any(Exam.class));
    }

    @Test
    @DisplayName("Ném ngoại lệ BusinessException khi thời gian bắt đầu sau thời gian kết thúc")
    void createExam_InvalidTime_ThrowsBusinessException() {
        OffsetDateTime start = OffsetDateTime.now().plusDays(2);
        OffsetDateTime end = start.minusHours(2);
        CreateExamRequest request = new CreateExamRequest(courseId, "Kỳ thi lỗi thời gian", start, end, null);

        assertThrows(BusinessException.class, () -> examService.createExam(request));
    }

    @Test
    @DisplayName("Ném ResourceNotFoundException khi không tìm thấy môn học")
    void createExam_CourseNotFound_ThrowsResourceNotFoundException() {
        OffsetDateTime start = OffsetDateTime.now().plusDays(1);
        OffsetDateTime end = start.plusHours(2);
        CreateExamRequest request = new CreateExamRequest(courseId, "Kỳ thi Java", start, end, null);

        when(courseRepository.findById(courseId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> examService.createExam(request));
    }

    @Test
    void questionConfigRoundTripPreservesOtherSettings() {
        UUID actor = UUID.randomUUID();
        Exam exam = new Exam();
        exam.setId(UUID.randomUUID());
        exam.setCourse(course);
        exam.setExamConfig(new java.util.HashMap<>(Map.of("maxMainQuestions", 4, "antiOverlapEnabled", false)));
        var question = new cocxanhcoder.viva.exam.system.questionbank.entity.Question();
        question.setId(UUID.randomUUID());
        question.setCourse(course);
        question.setStatus(cocxanhcoder.viva.exam.system.questionbank.entity.QuestionStatus.APPROVED);
        var request = new cocxanhcoder.viva.exam.system.exammgt.dto.request.QuestionConfigDto(java.util.List.of(question.getId()), 90, 3);
        when(examRepository.findById(exam.getId())).thenReturn(Optional.of(exam));
        when(questionRepository.findAllById(request.questionIds())).thenReturn(java.util.List.of(question));
        assertEquals(request, examService.updateQuestionConfig(actor, exam.getId(), request));
        assertEquals(request, examService.getQuestionConfig(actor, exam.getId()));
        assertEquals(4, exam.getExamConfig().get("maxMainQuestions"));
        assertEquals(false, exam.getExamConfig().get("antiOverlapEnabled"));
        verify(courseAccess, org.mockito.Mockito.times(2)).requireManager(actor, courseId);
    }

    @Test
    void questionConfigRejectsQuestionFromAnotherCourse() {
        Exam exam = new Exam();
        exam.setId(UUID.randomUUID());
        exam.setCourse(course);
        var question = new cocxanhcoder.viva.exam.system.questionbank.entity.Question();
        question.setId(UUID.randomUUID());
        Course other = new Course();
        other.setId(UUID.randomUUID());
        question.setCourse(other);
        question.setStatus(cocxanhcoder.viva.exam.system.questionbank.entity.QuestionStatus.APPROVED);
        var request = new cocxanhcoder.viva.exam.system.exammgt.dto.request.QuestionConfigDto(java.util.List.of(question.getId()), 60, 2);
        when(examRepository.findById(exam.getId())).thenReturn(Optional.of(exam));
        when(questionRepository.findAllById(request.questionIds())).thenReturn(java.util.List.of(question));
        assertThrows(BusinessException.class, () -> examService.updateQuestionConfig(UUID.randomUUID(), exam.getId(), request));
        org.mockito.Mockito.verify(examRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void examListIncludesCandidateCount() {
        Exam exam = new Exam();
        exam.setCourse(course);
        exam.setTotalCandidates(12);
        var pageable = org.springframework.data.domain.PageRequest.of(0, 10);
        when(examRepository.searchExams(null, null, pageable)).thenReturn(new org.springframework.data.domain.PageImpl<>(java.util.List.of(exam)));
        assertEquals(12, examService.getExams(null, null, pageable).getContent().getFirst().totalCandidates());
    }
}
