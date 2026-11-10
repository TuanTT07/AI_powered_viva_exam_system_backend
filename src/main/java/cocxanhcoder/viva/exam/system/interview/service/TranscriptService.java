package cocxanhcoder.viva.exam.system.interview.service;

import cocxanhcoder.viva.exam.system.interview.dto.AnswerFinalizedEvent;
import cocxanhcoder.viva.exam.system.interview.dto.AnswerReceipt;
import cocxanhcoder.viva.exam.system.common.exception.BusinessException;
import cocxanhcoder.viva.exam.system.exammgt.repository.QuestionAttemptRepository;
import cocxanhcoder.viva.exam.system.interview.entity.*;
import cocxanhcoder.viva.exam.system.interview.repository.TranscriptTurnRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/** Shared persistence boundary. BE1 should use the same QA lock when appending AI turns. */
@Service
@RequiredArgsConstructor
public class TranscriptService implements InterviewAnswers {
    private final InterviewAccessService access;
    private final TranscriptTurnRepository turns;
    private final QuestionAttemptRepository questions;
    private final ApplicationEventPublisher events;

    /** BE1 uses this method after deciding the next main/follow-up question. */
    @Transactional
    public UUID recordQuestion(UUID student, UUID attempt, UUID questionAttempt, String text, TurnType type) {
        if (text == null || text.isBlank() || text.length() > 10000
                || (type != TurnType.MAIN_QUESTION && type != TurnType.FOLLOW_UP))
            throw new BusinessException("Nội dung hoặc loại câu hỏi không hợp lệ");
        var qa = questions.findLockedById(questionAttempt)
                .orElseThrow(() -> new BusinessException("Không tìm thấy câu hỏi"));
        access.requireActiveAttempt(student, attempt);
        if (!qa.getExamAttempt().getId().equals(attempt)
                || qa.getStatus() != cocxanhcoder.viva.exam.system.exammgt.entity.QuestionAttemptStatus.IN_PROGRESS)
            throw new BusinessException("Câu hỏi không thuộc lượt thi đang hoạt động");
        var history = turns.findByQuestionAttemptIdOrderByTurnOrderAsc(questionAttempt);
        if (!history.isEmpty() && history.getLast().getSpeaker() == Speaker.AI
                && history.getLast().belongsToCurrentRun(qa))
            throw new BusinessException(HttpStatus.CONFLICT, "Câu hỏi trước chưa được trả lời");
        TranscriptTurn row = new TranscriptTurn();
        row.setQuestionAttempt(qa);
        row.setTurnOrder(history.isEmpty() ? 1 : history.getLast().getTurnOrder() + 1);
        row.setSpeaker(Speaker.AI);
        row.setTurnType(type);
        row.setTextContent(text.strip());
        return turns.saveAndFlush(row).getId();
    }

    @Transactional(readOnly = true)
    public AnswerReceipt findAnswer(UUID student, UUID attempt, UUID answer) {
        access.requireOwner(student, attempt);
        var saved = turns.findByAnswerId(answer).orElseThrow(() ->
                new BusinessException(HttpStatus.NOT_FOUND, "Câu trả lời chưa được lưu"));
        if (!saved.getQuestionAttempt().getExamAttempt().getId().equals(attempt))
            throw new BusinessException(HttpStatus.NOT_FOUND, "Không tìm thấy câu trả lời");
        return new AnswerReceipt(saved.getId(), saved.getTextContent());
    }

    @Transactional
    public AnswerReceipt complete(UUID student, UUID attempt, UUID prompt, UUID questionAttempt,
                                UUID answer, String text, Instant started, Instant ended) {
        if (text == null || text.isBlank()) throw new BusinessException("Không nhận dạng được câu trả lời");
        questions.findLockedById(questionAttempt).orElseThrow(() -> new BusinessException("Không tìm thấy câu hỏi"));
        var previous = turns.findByAnswerId(answer);
        if (previous.isPresent()) {
            var saved = previous.get();
            if (!saved.getQuestionAttempt().getExamAttempt().getStudent().getId().equals(student)
                    || !saved.getQuestionAttempt().getExamAttempt().getId().equals(attempt)
                    || !prompt.equals(saved.getResponseToTurnId()))
                throw new BusinessException(HttpStatus.CONFLICT, "Answer ID đã được sử dụng");
            return new AnswerReceipt(saved.getId(), saved.getTextContent());
        }
        var verified = access.requirePrompt(student, attempt, prompt);
        if (!verified.questionAttemptId().equals(questionAttempt)) throw new BusinessException("Sai lượt hỏi");
        var history = turns.findByQuestionAttemptIdOrderByTurnOrderAsc(questionAttempt);
        TranscriptTurn row = new TranscriptTurn();
        row.setQuestionAttempt(questions.getReferenceById(questionAttempt));
        row.setTurnOrder(history.getLast().getTurnOrder() + 1);
        row.setSpeaker(Speaker.STUDENT);
        row.setTurnType(TurnType.ANSWER);
        row.setTextContent(text.strip());
        row.setAnswerId(answer);
        row.setResponseToTurnId(prompt);
        turns.saveAndFlush(row);
        // BE1 can subscribe with @TransactionalEventListener(AFTER_COMMIT).
        events.publishEvent(new AnswerFinalizedEvent(answer, prompt, questionAttempt, attempt,
                student, row.getId(), row.getTextContent(), started, ended));
        return new AnswerReceipt(row.getId(), row.getTextContent());
    }
}
