package cocxanhcoder.viva.exam.system.interview.service;

import cocxanhcoder.viva.exam.system.interview.dto.AnswerFinalizedEvent;
import cocxanhcoder.viva.exam.system.interview.dto.InterviewPrompt;
import cocxanhcoder.viva.exam.system.academic.entity.User;
import cocxanhcoder.viva.exam.system.common.exception.BusinessException;
import cocxanhcoder.viva.exam.system.exammgt.entity.*;
import cocxanhcoder.viva.exam.system.exammgt.repository.QuestionAttemptRepository;
import cocxanhcoder.viva.exam.system.interview.entity.*;
import cocxanhcoder.viva.exam.system.interview.repository.TranscriptTurnRepository;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import java.time.Instant;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TranscriptServiceTest {
    private final InterviewAccessService access = mock(InterviewAccessService.class);
    private final TranscriptTurnRepository turns = mock(TranscriptTurnRepository.class);
    private final QuestionAttemptRepository questions = mock(QuestionAttemptRepository.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final TranscriptService service = new TranscriptService(access, turns, questions, events);
    private final UUID student = UUID.randomUUID(), attempt = UUID.randomUUID(), prompt = UUID.randomUUID();
    private final UUID question = UUID.randomUUID(), answer = UUID.randomUUID();
    private final QuestionAttempt qa = new QuestionAttempt();

    @BeforeEach void setup() {
        User user = new User(); user.setId(student);
        ExamAttempt ea = new ExamAttempt(); ea.setId(attempt); ea.setStudent(user);
        qa.setId(question); qa.setExamAttempt(ea);
        when(questions.findLockedById(question)).thenReturn(Optional.of(qa));
        when(questions.getReferenceById(question)).thenReturn(qa);
    }

    @Test void savesLinkedAnswerAndPublishesIntegrationEventOnce() {
        TranscriptTurn ai = new TranscriptTurn(); ai.setTurnOrder(3);
        when(access.requirePrompt(student, attempt, prompt))
                .thenReturn(new InterviewPrompt(prompt, question, "Question", 60));
        when(turns.findByQuestionAttemptIdOrderByTurnOrderAsc(question)).thenReturn(List.of(ai));
        when(turns.saveAndFlush(any())).thenAnswer(i -> {
            TranscriptTurn row = i.getArgument(0); row.setId(UUID.randomUUID()); return row;
        });
        var result = service.complete(student, attempt, prompt, question, answer, "  My answer  ", Instant.now(), Instant.now());
        assertEquals("My answer", result.text());
        var captured = ArgumentCaptor.forClass(TranscriptTurn.class);
        verify(turns).saveAndFlush(captured.capture());
        TranscriptTurn saved = captured.getValue();
        assertEquals(4, saved.getTurnOrder()); assertEquals(Speaker.STUDENT, saved.getSpeaker());
        assertEquals(answer, saved.getAnswerId()); assertEquals(prompt, saved.getResponseToTurnId());
        when(turns.findByAnswerId(answer)).thenReturn(Optional.of(saved));
        assertEquals(result, service.complete(student, attempt, prompt, question, answer, "Retry", Instant.now(), Instant.now()));
        verify(turns, times(1)).saveAndFlush(any());
        verify(events, times(1)).publishEvent(any(AnswerFinalizedEvent.class));
    }

    @Test void duplicateAnswerIdCannotBeUsedForAnotherPrompt() {
        TranscriptTurn saved = new TranscriptTurn(); saved.setQuestionAttempt(qa);
        saved.setResponseToTurnId(UUID.randomUUID());
        when(turns.findByAnswerId(answer)).thenReturn(Optional.of(saved));
        assertThrows(BusinessException.class, () -> service.complete(student, attempt, prompt, question,
                answer, "Answer", Instant.now(), Instant.now()));
        verify(turns, never()).saveAndFlush(any()); verifyNoInteractions(events);
    }

    @Test void blankAnswerIsNeverPersisted() {
        assertThrows(BusinessException.class, () -> service.complete(student, attempt, prompt, question,
                answer, " ", Instant.now(), Instant.now()));
        verifyNoInteractions(turns, events);
    }

    @Test void recordsAiPromptWithSharedOrderingAndRejectsASecondUnansweredPrompt() {
        qa.setStatus(QuestionAttemptStatus.IN_PROGRESS);
        when(turns.findByQuestionAttemptIdOrderByTurnOrderAsc(question)).thenReturn(List.of());
        when(turns.saveAndFlush(any())).thenAnswer(i -> {
            TranscriptTurn row = i.getArgument(0); row.setId(UUID.randomUUID()); return row;
        });
        UUID id = service.recordQuestion(student, attempt, question, "Main?", TurnType.MAIN_QUESTION);
        assertNotNull(id);
        var captured = ArgumentCaptor.forClass(TranscriptTurn.class);
        verify(turns).saveAndFlush(captured.capture());
        TranscriptTurn row = captured.getValue();
        assertEquals(1, row.getTurnOrder()); assertEquals(Speaker.AI, row.getSpeaker());
        when(turns.findByQuestionAttemptIdOrderByTurnOrderAsc(question)).thenReturn(List.of(row));
        assertThrows(BusinessException.class, () -> service.recordQuestion(student, attempt, question,
                "Another?", TurnType.FOLLOW_UP));
        verify(turns, times(1)).saveAndFlush(any());
    }
    @Test void mainAndFollowUpHaveDeadlinesBeforeRecording() {
        qa.setStatus(QuestionAttemptStatus.IN_PROGRESS);
        Exam exam = new Exam(); exam.setExamConfig(Map.of("timeLimitPerTurnSeconds", 120));
        qa.getExamAttempt().setExam(exam);
        when(turns.findByQuestionAttemptIdOrderByTurnOrderAsc(question)).thenReturn(List.of());
        when(turns.saveAndFlush(any())).thenAnswer(i -> {
            TranscriptTurn row = i.getArgument(0); row.setId(UUID.randomUUID()); return row;
        });
        Instant before = Instant.now();
        service.recordQuestion(student, attempt, question, "Main?", TurnType.MAIN_QUESTION);
        service.recordQuestion(student, attempt, question, "Follow-up?", TurnType.FOLLOW_UP);
        var captured = ArgumentCaptor.forClass(TranscriptTurn.class);
        verify(turns, times(2)).saveAndFlush(captured.capture());
        for (var row : captured.getAllValues()) {
            assertFalse(row.getDeadlineAt().isBefore(before.plusSeconds(120)));
            assertFalse(row.getDeadlineAt().isAfter(Instant.now().plusSeconds(120)));
        }
    }

    @Test void questionDeadlineCannotExceedSlotEnd() {
        qa.setStatus(QuestionAttemptStatus.IN_PROGRESS);
        var end = java.time.OffsetDateTime.now().plusSeconds(15);
        qa.getExamAttempt().setScheduledEndTime(end);
        when(turns.findByQuestionAttemptIdOrderByTurnOrderAsc(question)).thenReturn(List.of());
        when(turns.saveAndFlush(any())).thenAnswer(i -> {
            TranscriptTurn row = i.getArgument(0); row.setId(UUID.randomUUID()); return row;
        });
        service.recordQuestion(student, attempt, question, "Question?", TurnType.MAIN_QUESTION);
        var captured = ArgumentCaptor.forClass(TranscriptTurn.class);
        verify(turns).saveAndFlush(captured.capture());
        assertEquals(end.toInstant(), captured.getValue().getDeadlineAt());
    }

}
