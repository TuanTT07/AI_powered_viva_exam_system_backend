package cocxanhcoder.viva.exam.system.interview.service;

import cocxanhcoder.viva.exam.system.academic.entity.User;
import cocxanhcoder.viva.exam.system.academic.repository.UserRepository;
import cocxanhcoder.viva.exam.system.common.exception.BusinessException;
import cocxanhcoder.viva.exam.system.exammgt.entity.*;
import cocxanhcoder.viva.exam.system.exammgt.repository.ExamAttemptRepository;
import cocxanhcoder.viva.exam.system.exammgt.repository.QuestionAttemptRepository;
import cocxanhcoder.viva.exam.system.interview.entity.*;
import cocxanhcoder.viva.exam.system.interview.repository.TranscriptTurnRepository;
import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InterviewAccessServiceTest {
    private final ExamAttemptRepository attempts = mock(ExamAttemptRepository.class);
    private final TranscriptTurnRepository turns = mock(TranscriptTurnRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final QuestionAttemptRepository questions = mock(QuestionAttemptRepository.class);
    private final InterviewAccessService access = new InterviewAccessService(attempts, turns, users, questions);
    private final UUID student = UUID.randomUUID(), attemptId = UUID.randomUUID(), promptId = UUID.randomUUID();
    private final User user = new User();
    private final ExamAttempt attempt = new ExamAttempt();
    private final QuestionAttempt qa = new QuestionAttempt();
    private final TranscriptTurn prompt = new TranscriptTurn();

    @BeforeEach void setup() {
        user.setId(student); user.setMustChangePassword(false);
        attempt.setId(attemptId); attempt.setStudent(user); attempt.setStatus(ExamAttemptStatus.IN_PROGRESS);
        Exam exam = new Exam(); exam.setExamConfig(Map.of("timeLimitPerTurnSeconds", 90)); attempt.setExam(exam);
        qa.setId(UUID.randomUUID()); qa.setExamAttempt(attempt); qa.setStatus(QuestionAttemptStatus.IN_PROGRESS);
        prompt.setId(promptId); prompt.setQuestionAttempt(qa); prompt.setSpeaker(Speaker.AI);
        prompt.setTurnType(TurnType.MAIN_QUESTION); prompt.setTextContent("Explain transactions");
        when(users.findById(student)).thenReturn(Optional.of(user));
        when(attempts.findById(attemptId)).thenReturn(Optional.of(attempt));
        when(turns.findById(promptId)).thenReturn(Optional.of(prompt));
        when(turns.findByQuestionAttemptIdOrderByTurnOrderAsc(qa.getId())).thenReturn(List.of(prompt));
    }

    @Test void onlyLatestActiveAiPromptCanBeAnsweredAndExamLimitIsUsed() {
        var result = access.requirePrompt(student, attemptId, promptId);
        assertEquals(qa.getId(), result.questionAttemptId());
        assertEquals(90, result.timeLimitSeconds());
    }

    @Test void firstAcceptedStartSetsDeadlineAndRetryKeepsIt() {
        when(questions.findLockedById(qa.getId())).thenReturn(Optional.of(qa));
        assertNull(access.requirePrompt(student, attemptId, promptId).deadlineAt());

        var before = java.time.Instant.now().minusSeconds(1);
        var deadline = access.beginAnswer(student, attemptId, promptId, qa.getId(), 60);
        assertTrue(deadline.isAfter(before.plusSeconds(60)));
        assertEquals(deadline, access.requirePrompt(student, attemptId, promptId).deadlineAt());
        assertEquals(deadline, access.beginAnswer(student, attemptId, promptId, qa.getId(), 90));
    }

    @Test void anotherStudentsAttemptIsForbidden() {
        User other = new User(); other.setId(UUID.randomUUID()); attempt.setStudent(other);
        assertThrows(BusinessException.class, () -> access.requireActiveAttempt(student, attemptId));
    }

    @Test void completedExamAndPendingQuestionAreRejected() {
        attempt.setStatus(ExamAttemptStatus.COMPLETED);
        assertThrows(BusinessException.class, () -> access.requirePrompt(student, attemptId, promptId));
        attempt.setStatus(ExamAttemptStatus.IN_PROGRESS); qa.setStatus(QuestionAttemptStatus.PENDING);
        assertThrows(BusinessException.class, () -> access.requirePrompt(student, attemptId, promptId));
    }

    @Test void answeredOrSupersededPromptCannotBeReplayed() {
        TranscriptTurn answer = new TranscriptTurn(); answer.setId(UUID.randomUUID());
        when(turns.findByQuestionAttemptIdOrderByTurnOrderAsc(qa.getId())).thenReturn(List.of(prompt, answer));
        assertThrows(BusinessException.class, () -> access.requirePrompt(student, attemptId, promptId));
    }

    @Test void passwordResetRevokesSpeechAccess() {
        user.setMustChangePassword(true);
        assertThrows(BusinessException.class, () -> access.requireActiveAttempt(student, attemptId));
    }
    @Test void recordingKeepsQuestionOpeningDeadlineAndRejectsExpiredPrompt() {
        when(questions.findLockedById(qa.getId())).thenReturn(Optional.of(qa));
        var deadline = java.time.Instant.now().plusSeconds(20);
        prompt.setDeadlineAt(deadline);
        assertEquals(deadline, access.beginAnswer(student, attemptId, promptId, qa.getId(), 90));
        assertEquals(deadline, access.requirePrompt(student, attemptId, promptId).deadlineAt());
        prompt.setDeadlineAt(java.time.Instant.now().minusSeconds(20));
        assertThrows(BusinessException.class, () -> access.beginAnswer(student, attemptId, promptId, qa.getId(), 90));
    }

}
