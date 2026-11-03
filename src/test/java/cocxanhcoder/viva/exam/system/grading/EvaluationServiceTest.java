package cocxanhcoder.viva.exam.system.grading;

import cocxanhcoder.viva.exam.system.academic.entity.Course;
import cocxanhcoder.viva.exam.system.common.exception.BusinessException;
import cocxanhcoder.viva.exam.system.exammgt.entity.Exam;
import cocxanhcoder.viva.exam.system.exammgt.repository.*;
import cocxanhcoder.viva.exam.system.grading.entity.EvaluationStatus;
import cocxanhcoder.viva.exam.system.grading.mapper.EvaluationMapper;
import cocxanhcoder.viva.exam.system.grading.repository.EvaluationRepository;
import cocxanhcoder.viva.exam.system.grading.service.EvaluationService;
import cocxanhcoder.viva.exam.system.interview.repository.TranscriptTurnRepository;
import cocxanhcoder.viva.exam.system.questionbank.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EvaluationServiceTest {
    private final EvaluationRepository evaluations = mock(EvaluationRepository.class);
    private final ExamRepository exams = mock(ExamRepository.class);
    private final CourseAccessService access = mock(CourseAccessService.class);
    private final EvaluationService service = new EvaluationService(evaluations, mock(QuestionAttemptRepository.class),
            mock(TranscriptTurnRepository.class), mock(AiClient.class), access, mock(EvaluationMapper.class), exams);

    @Test
    void examEvaluationsSupportStatusFilterAndPagination() {
        UUID actor = UUID.randomUUID();
        Exam exam = exam();
        var pageable = PageRequest.of(1, 5);
        when(evaluations.findByQuestionAttempt_ExamAttempt_Exam_IdAndStatus(exam.getId(), EvaluationStatus.GRADED, pageable))
                .thenReturn(Page.empty(pageable));
        assertNotNull(service.findByExam(actor, exam.getId(), EvaluationStatus.GRADED, pageable));
        verify(access).requireManager(actor, exam.getCourse().getId());
        verify(evaluations).findByQuestionAttempt_ExamAttempt_Exam_IdAndStatus(exam.getId(), EvaluationStatus.GRADED, pageable);
    }

    @Test
    void examEvaluationsRejectUnauthorizedManagerBeforeReadingResults() {
        UUID actor = UUID.randomUUID();
        Exam exam = exam();
        when(access.requireManager(actor, exam.getCourse().getId())).thenThrow(new BusinessException("Forbidden"));
        assertThrows(BusinessException.class, () -> service.findByExam(actor, exam.getId(), null, PageRequest.of(0, 20)));
        verifyNoInteractions(evaluations);
    }

    private Exam exam() {
        Course course = new Course();
        course.setId(UUID.randomUUID());
        Exam exam = new Exam();
        exam.setId(UUID.randomUUID());
        exam.setCourse(course);
        when(exams.findById(exam.getId())).thenReturn(Optional.of(exam));
        return exam;
    }
}
