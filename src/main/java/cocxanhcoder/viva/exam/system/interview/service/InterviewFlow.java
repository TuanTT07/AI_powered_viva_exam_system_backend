package cocxanhcoder.viva.exam.system.interview.service;

import cocxanhcoder.viva.exam.system.common.exception.BusinessException;
import cocxanhcoder.viva.exam.system.exammgt.entity.*;
import cocxanhcoder.viva.exam.system.exammgt.repository.ExamAttemptRepository;
import cocxanhcoder.viva.exam.system.exammgt.repository.QuestionAttemptRepository;
import cocxanhcoder.viva.exam.system.interview.dto.InterviewState;
import cocxanhcoder.viva.exam.system.interview.dto.InterviewState.Status;
import cocxanhcoder.viva.exam.system.interview.entity.*;
import cocxanhcoder.viva.exam.system.interview.repository.TranscriptTurnRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InterviewFlow {
    private final ExamAttemptRepository attempts;
    private final QuestionAttemptRepository questions;
    private final TranscriptTurnRepository turns;
    private final TranscriptService transcripts;
    private final InterviewAccessService access;

    @Transactional
    public InterviewState start(UUID student, UUID id) {
        ExamAttempt attempt = attempts.findLockedById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Không tìm thấy lượt thi"));
        access.requireOwner(student, id);
        if (attempt.getStatus() == ExamAttemptStatus.READY) {
            QuestionAttempt first = nextPending(attempt);
            if (first == null || activeQuestion(attempt) != null)
                throw conflict("Lượt thi chưa có bộ câu hỏi hợp lệ");
            attempt.setStatus(ExamAttemptStatus.IN_PROGRESS);
            attempt.setActualStartTime(OffsetDateTime.now());
            open(attempt, first);
        }
        return state(attempt);
    }

    @Transactional(readOnly = true)
    public InterviewState current(UUID student, UUID id) {
        return state(access.requireOwner(student, id));
    }

    private InterviewState state(ExamAttempt attempt) {
        return switch (attempt.getStatus()) {
            case READY -> new InterviewState(attempt.getId(), Status.READY, null);
            case COMPLETED -> new InterviewState(attempt.getId(), Status.COMPLETED, null);
            case IN_PROGRESS -> {
                QuestionAttempt question = activeQuestion(attempt);
                TranscriptTurn last = question == null ? null : latest(question);
                Instant deadline = last == null ? null : InterviewTiming.deadline(last);
                if (last == null || last.getTurnType() == TurnType.ANSWER
                        || deadline != null && !Instant.now().isBefore(deadline))
                    yield new InterviewState(attempt.getId(), Status.PROCESSING, null);
                yield new InterviewState(attempt.getId(), Status.AWAITING_ANSWER,
                        new InterviewState.Prompt(question.getId(), last.getId(), last.getTextContent(), deadline));
            }
            default -> throw conflict("Lượt thi chưa sẵn sàng");
        };
    }

    @Transactional(readOnly = true)
    public Work pending(UUID id) {
        ExamAttempt attempt = attempts.findById(id).orElse(null);
        if (attempt == null || attempt.getStatus() != ExamAttemptStatus.IN_PROGRESS) return null;
        QuestionAttempt question = activeQuestion(attempt);
        if (question == null) return null;
        List<TranscriptTurn> history = history(question);
        if (history.isEmpty()) return null;
        TranscriptTurn last = history.getLast();
        if (last.getTurnType() != TurnType.ANSWER) {
            Instant deadline = InterviewTiming.deadline(last);
            return deadline != null && !Instant.now().isBefore(deadline)
                    ? new Work(id, last.getId(), deadline, null) : null;
        }
        String context = null;
        if (followUpsAvailable(attempt, history)) {
            var rubric = question.getQuestion().getRubric();
            String criteria = rubric == null ? "" : rubric.getCriteria().stream()
                    .map(c -> c.getCriterionName() + " " + c.getDescription())
                    .collect(Collectors.joining("\n"));
            String conversation = history.stream()
                    .map(turn -> turn.getSpeaker() + ": " + turn.getTextContent())
                    .collect(Collectors.joining("\n"));
            context = "Rubric:\n" + criteria + "\nHội thoại:\n" + conversation;
        }
        return new Work(id, last.getId(), null, context);
    }

    @Transactional
    public void advance(Work work, String followUp) {
        ExamAttempt attempt = attempts.findLockedById(work.attemptId()).orElse(null);
        if (attempt == null || attempt.getStatus() != ExamAttemptStatus.IN_PROGRESS) return;
        QuestionAttempt current = activeQuestion(attempt);
        if (current == null) return;
        questions.findLockedById(current.getId()).orElseThrow();
        List<TranscriptTurn> history = history(current);
        if (history.isEmpty() || !work.turnId().equals(history.getLast().getId())) return;
        TranscriptTurn last = history.getLast();
        if (work.deadline() != null) {
            if (Instant.now().isBefore(work.deadline().plusSeconds(InterviewTiming.FINALIZATION_GRACE_SECONDS)))
                return;
            last.setTimedOutAt(Instant.now());
        } else if (last.getTurnType() != TurnType.ANSWER) {
            return;
        } else if (followUp != null && followUp.length() <= 10000 && !normalize(followUp).isBlank()
                && followUpsAvailable(attempt, history)
                && history.stream().filter(t -> t.getSpeaker() == Speaker.AI)
                        .noneMatch(t -> normalize(t.getTextContent()).equals(normalize(followUp)))) {
            transcripts.recordQuestion(attempt.getStudent().getId(), attempt.getId(), current.getId(),
                    followUp, TurnType.FOLLOW_UP);
            return;
        }
        OffsetDateTime now = OffsetDateTime.now();
        current.setStatus(QuestionAttemptStatus.COMPLETED);
        current.setEndTime(now);
        QuestionAttempt next = nextPending(attempt);
        if (next != null) {
            open(attempt, next);
        } else {
            attempt.setStatus(ExamAttemptStatus.COMPLETED);
            attempt.setActualEndTime(now);
        }
    }

    private void open(ExamAttempt attempt, QuestionAttempt question) {
        question.setStatus(QuestionAttemptStatus.IN_PROGRESS);
        question.setStartTime(OffsetDateTime.now().truncatedTo(ChronoUnit.MICROS));
        transcripts.recordQuestion(attempt.getStudent().getId(), attempt.getId(), question.getId(),
                question.getQuestion().getContent(), TurnType.MAIN_QUESTION);
    }

    private QuestionAttempt activeQuestion(ExamAttempt attempt) {
        return attempt.getQuestionAttempts().stream()
                .filter(q -> q.getStatus() == QuestionAttemptStatus.IN_PROGRESS).findFirst().orElse(null);
    }

    private QuestionAttempt nextPending(ExamAttempt attempt) {
        return attempt.getQuestionAttempts().stream()
                .filter(q -> q.getStatus() == QuestionAttemptStatus.PENDING).findFirst().orElse(null);
    }

    private List<TranscriptTurn> history(QuestionAttempt question) {
        return turns.findByQuestionAttemptIdOrderByTurnOrderAsc(question.getId()).stream()
                .filter(turn -> turn.belongsToCurrentRun(question)).toList();
    }

    private TranscriptTurn latest(QuestionAttempt question) {
        var history = history(question);
        return history.isEmpty() ? null : history.getLast();
    }

    private boolean followUpsAvailable(ExamAttempt attempt, List<TranscriptTurn> history) {
        var config = attempt.getExam().getExamConfig();
        Object value = config == null ? null : config.get("maxFollowUpQuestions");
        int max = value instanceof Number n ? n.intValue() : 2;
        return history.stream().filter(turn -> turn.getTurnType() == TurnType.FOLLOW_UP).count() < max;
    }

    private String normalize(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}\\p{M}]+", " ").strip();
    }

    private BusinessException conflict(String message) {
        return new BusinessException(HttpStatus.CONFLICT, message);
    }

    record Work(UUID attemptId, UUID turnId, Instant deadline, String context) {}
}
