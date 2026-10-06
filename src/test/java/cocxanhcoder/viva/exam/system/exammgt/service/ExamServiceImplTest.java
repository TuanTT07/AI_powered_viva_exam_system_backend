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
}
