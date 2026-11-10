package cocxanhcoder.viva.exam.system.grading;

import cocxanhcoder.viva.exam.system.academic.entity.Course;
import cocxanhcoder.viva.exam.system.academic.entity.User;
import cocxanhcoder.viva.exam.system.common.exception.BusinessException;
import cocxanhcoder.viva.exam.system.common.exception.ResourceNotFoundException;
import cocxanhcoder.viva.exam.system.exammgt.entity.Exam;
import cocxanhcoder.viva.exam.system.exammgt.entity.ExamAttempt;
import cocxanhcoder.viva.exam.system.exammgt.entity.QuestionAttempt;
import cocxanhcoder.viva.exam.system.exammgt.repository.*;
import cocxanhcoder.viva.exam.system.grading.dto.EvaluationResponse;
import cocxanhcoder.viva.exam.system.grading.dto.FinalizeEvaluationRequest;
import cocxanhcoder.viva.exam.system.grading.dto.CriterionFinalScoreRequest;
import cocxanhcoder.viva.exam.system.grading.dto.StudentResult;
import cocxanhcoder.viva.exam.system.grading.entity.CriterionScore;
import cocxanhcoder.viva.exam.system.grading.entity.Evaluation;
import cocxanhcoder.viva.exam.system.grading.entity.EvaluationStatus;
import cocxanhcoder.viva.exam.system.grading.mapper.EvaluationMapper;
import cocxanhcoder.viva.exam.system.grading.repository.EvaluationRepository;
import cocxanhcoder.viva.exam.system.grading.service.EvaluationService;
import cocxanhcoder.viva.exam.system.interview.entity.Speaker;
import cocxanhcoder.viva.exam.system.interview.entity.TranscriptTurn;
import cocxanhcoder.viva.exam.system.interview.repository.TranscriptTurnRepository;
import cocxanhcoder.viva.exam.system.questionbank.entity.Question;
import cocxanhcoder.viva.exam.system.questionbank.entity.Rubric;
import cocxanhcoder.viva.exam.system.questionbank.entity.RubricCriterion;
import cocxanhcoder.viva.exam.system.questionbank.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EvaluationServiceTest {
    private final EvaluationRepository evaluations = mock(EvaluationRepository.class);
    private final ExamAttemptRepository attempts = mock(ExamAttemptRepository.class);
    private final ExamRepository exams = mock(ExamRepository.class);
    private final QuestionAttemptRepository questionAttempts = mock(QuestionAttemptRepository.class);
    private final TranscriptTurnRepository transcriptTurns = mock(TranscriptTurnRepository.class);
    private final AiClient ai = mock(AiClient.class);
    private final CourseAccessService access = mock(CourseAccessService.class);
    private final EvaluationMapper mapper = mock(EvaluationMapper.class);
    private final EvaluationService service = new EvaluationService(evaluations, questionAttempts,
            transcriptTurns, ai, access, mapper, exams, attempts);

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

    @Test
    void getByQuestionAttemptResolvesViaExamAttemptIdAndReturnsDraftWhenNoEvaluation() {
        UUID actor = UUID.randomUUID();
        UUID examAttemptId = UUID.randomUUID();
        QuestionAttempt qa = sampleQuestionAttempt();
        when(questionAttempts.findById(examAttemptId)).thenReturn(Optional.empty());
        when(questionAttempts.findByExamAttemptIdOrderByQuestionOrderAsc(examAttemptId)).thenReturn(List.of(qa));
        when(evaluations.findByQuestionAttemptId(qa.getId())).thenReturn(Optional.empty());
        when(transcriptTurns.findByQuestionAttemptIdOrderByTurnOrderAsc(qa.getId())).thenReturn(List.of());

        EvaluationResponse mockDraft = mock(EvaluationResponse.class);
        when(mapper.toDraftResponse(eq(qa), any(), any())).thenReturn(mockDraft);

        EvaluationResponse res = service.getByQuestionAttempt(actor, examAttemptId);
        assertNotNull(res);
        verify(mapper).toDraftResponse(eq(qa), any(), any());
    }

    @Test
    void finalizeEvaluationCreatesAndSavesNewEvaluationWhenNotPreExisting() {
        UUID actor = UUID.randomUUID();
        QuestionAttempt qa = sampleQuestionAttempt();
        RubricCriterion criterion = new RubricCriterion();
        criterion.setId(UUID.randomUUID());
        criterion.setCriterionName("Criterion 1");
        criterion.setMaxScore(new BigDecimal("10.0"));

        Rubric rubric = new Rubric();
        rubric.setCriteria(List.of(criterion));
        qa.getQuestion().setRubric(rubric);

        when(questionAttempts.findLockedById(qa.getId())).thenReturn(Optional.of(qa));
        when(evaluations.findByQuestionAttemptId(qa.getId())).thenReturn(Optional.empty());
        when(access.requireManager(eq(actor), any())).thenReturn(new User());

        when(evaluations.save(any(Evaluation.class))).thenAnswer(invocation -> invocation.getArgument(0));
        EvaluationResponse mockRes = mock(EvaluationResponse.class);
        when(mapper.toResponse(any(Evaluation.class))).thenReturn(mockRes);

        FinalizeEvaluationRequest req = new FinalizeEvaluationRequest(
                List.of(new CriterionFinalScoreRequest(criterion.getId(), new BigDecimal("8.5"), "Good")),
                "Great work"
        );

        EvaluationResponse res = service.finalizeEvaluation(actor, qa.getId(), req);
        assertNotNull(res);
        verify(evaluations).save(argThat(eval ->
                eval.getFinalScore().compareTo(new BigDecimal("8.5")) == 0 &&
                eval.getStatus() == EvaluationStatus.GRADED
        ));
    }

    @Test
    void publishedResultsIncludeQuestionContentCriteriaAndTranscriptForStudentReview() {
        UUID studentId = UUID.randomUUID();
        UUID attemptId = UUID.randomUUID();
        QuestionAttempt qa = sampleQuestionAttempt();

        Evaluation evaluation = new Evaluation();
        evaluation.setQuestionAttempt(qa);
        evaluation.setFinalScore(new BigDecimal("8.5"));
        evaluation.setOverallFeedback("Trả lời tốt");
        evaluation.setPublishedAt(java.time.OffsetDateTime.now());
        CriterionScore criterion = new CriterionScore(UUID.randomUUID(), "Đúng kiến thức",
                new BigDecimal("10"), new BigDecimal("8"), new BigDecimal("8.5"), "Hiểu đúng vấn đề");
        evaluation.setEvaluationDetails(List.of(criterion));

        when(evaluations.findPublishedEvaluations(studentId, attemptId)).thenReturn(List.of(evaluation));

        TranscriptTurn turn = new TranscriptTurn();
        turn.setId(UUID.randomUUID());
        turn.setTurnOrder(1);
        turn.setSpeaker(Speaker.STUDENT);
        turn.setTextContent("Câu trả lời của tôi");
        turn.setCreatedAt(java.time.OffsetDateTime.now());
        when(transcriptTurns.findByQuestionAttemptIdOrderByTurnOrderAsc(qa.getId())).thenReturn(List.of(turn));

        List<StudentResult> results = service.publishedResults(studentId, attemptId);

        assertEquals(1, results.size());
        StudentResult result = results.getFirst();
        assertEquals("Sample Question Content", result.questionContent());
        assertEquals(1, result.criteria().size());
        assertEquals("Hiểu đúng vấn đề", result.criteria().getFirst().comment());
        assertEquals(1, result.transcript().size());
        assertEquals("Câu trả lời của tôi", result.transcript().getFirst().textContent());
    }

    @Test
    void listsAllThreeAssignedQuestionsEvenWhenOnlyFirstHasAnEvaluation() {
        UUID actor = UUID.randomUUID();
        Exam exam = exam();
        QuestionAttempt first = sampleQuestionAttempt();
        first.getExamAttempt().setExam(exam);
        first.setQuestionOrder(1);
        QuestionAttempt second = sampleQuestionAttempt();
        second.setExamAttempt(first.getExamAttempt());
        second.setQuestionOrder(2);
        QuestionAttempt third = sampleQuestionAttempt();
        third.setExamAttempt(first.getExamAttempt());
        third.setQuestionOrder(3);
        UUID attemptId = first.getExamAttempt().getId();
        when(questionAttempts.findByExamAttemptIdOrderByQuestionOrderAsc(attemptId))
                .thenReturn(List.of(first, second, third));
        Evaluation existing = new Evaluation();
        when(evaluations.findByQuestionAttemptId(first.getId())).thenReturn(Optional.of(existing));
        EvaluationResponse saved = mock(EvaluationResponse.class);
        when(mapper.toResponse(existing)).thenReturn(saved);
        EvaluationResponse draft2 = mock(EvaluationResponse.class);
        EvaluationResponse draft3 = mock(EvaluationResponse.class);
        when(mapper.toDraftResponse(eq(second), any(), any())).thenReturn(draft2);
        when(mapper.toDraftResponse(eq(third), any(), any())).thenReturn(draft3);
        var result = service.findAttemptQuestions(actor, exam.getId(), attemptId);
        assertEquals(List.of(1, 2, 3), result.stream().map(r -> r.questionOrder()).toList());
        assertSame(saved, result.get(0).evaluation());
        assertSame(draft2, result.get(1).evaluation());
        assertSame(draft3, result.get(2).evaluation());
        verifyNoInteractions(ai);
        verify(evaluations, never()).save(any());
    }

    @Test
    void cannotReadQuestionsFromAnotherExam() {
        Exam exam = exam();
        QuestionAttempt other = sampleQuestionAttempt();
        UUID attemptId = other.getExamAttempt().getId();
        when(questionAttempts.findByExamAttemptIdOrderByQuestionOrderAsc(attemptId)).thenReturn(List.of(other));
        assertThrows(ResourceNotFoundException.class,
                () -> service.findAttemptQuestions(UUID.randomUUID(), exam.getId(), attemptId));
        verifyNoInteractions(evaluations, transcriptTurns, mapper, ai);
    }

    @Test
    void checksManagerBeforeReadingAssignedQuestions() {
        Exam exam = exam();
        UUID actor = UUID.randomUUID();
        doThrow(new BusinessException("Forbidden")).when(access).requireManager(actor, exam.getCourse().getId());
        assertThrows(BusinessException.class,
                () -> service.findAttemptQuestions(actor, exam.getId(), UUID.randomUUID()));
        verifyNoInteractions(questionAttempts, evaluations, transcriptTurns, mapper, ai);
    }

    private QuestionAttempt sampleQuestionAttempt() {
        Course course = new Course();
        course.setId(UUID.randomUUID());
        Exam exam = new Exam();
        exam.setId(UUID.randomUUID());
        exam.setCourse(course);
        ExamAttempt ea = new ExamAttempt();
        ea.setId(UUID.randomUUID());
        ea.setExam(exam);
        Question question = new Question();
        question.setId(UUID.randomUUID());
        question.setContent("Sample Question Content");
        QuestionAttempt qa = new QuestionAttempt();
        qa.setId(UUID.randomUUID());
        qa.setExamAttempt(ea);
        qa.setQuestion(question);
        return qa;
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
