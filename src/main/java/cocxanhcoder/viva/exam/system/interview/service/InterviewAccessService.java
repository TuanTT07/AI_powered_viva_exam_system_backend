package cocxanhcoder.viva.exam.system.interview.service;

import cocxanhcoder.viva.exam.system.common.exception.BusinessException;
import cocxanhcoder.viva.exam.system.interview.dto.InterviewPrompt;
import cocxanhcoder.viva.exam.system.academic.repository.UserRepository;
import cocxanhcoder.viva.exam.system.exammgt.entity.*;
import cocxanhcoder.viva.exam.system.exammgt.repository.ExamAttemptRepository;
import cocxanhcoder.viva.exam.system.exammgt.repository.QuestionAttemptRepository;
import cocxanhcoder.viva.exam.system.interview.entity.*;
import cocxanhcoder.viva.exam.system.interview.repository.TranscriptTurnRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InterviewAccessService implements InterviewAccess {
    private final ExamAttemptRepository attempts;
    private final TranscriptTurnRepository turns;
    private final UserRepository users;
    private final QuestionAttemptRepository questions;

    public ExamAttempt requireOwner(UUID studentId, UUID attemptId) {
        var user = users.findById(studentId)
                .orElseThrow(() -> new BusinessException(HttpStatus.FORBIDDEN, "Tài khoản không còn tồn tại"));
        if (user.isMustChangePassword())
            throw new BusinessException(HttpStatus.FORBIDDEN, "Bạn phải đổi mật khẩu trước khi sử dụng speech");
        ExamAttempt attempt = attempts.findById(attemptId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Không tìm thấy lượt thi"));
        if (!attempt.getStudent().getId().equals(studentId))
            throw new BusinessException(HttpStatus.FORBIDDEN, "Không có quyền truy cập lượt thi");
        return attempt;
    }

    public void requireActiveAttempt(UUID studentId, UUID attemptId) {
        ExamAttempt attempt = requireOwner(studentId, attemptId);
        if (attempt.getStatus() != ExamAttemptStatus.IN_PROGRESS)
            throw new BusinessException(HttpStatus.CONFLICT, "Lượt thi chưa bắt đầu hoặc đã kết thúc");
    }

    @Override
    @Transactional
    public Instant beginAnswer(UUID studentId, UUID attemptId, UUID questionTurnId, UUID questionAttemptId, int limitSeconds) {
        var question = questions.findLockedById(questionAttemptId).orElseThrow();
        var prompt = requirePrompt(studentId, attemptId, questionTurnId);
        if (!prompt.questionAttemptId().equals(questionAttemptId)) throw new BusinessException("Sai lượt hỏi");
        Instant now = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        var turn = turns.findById(questionTurnId).orElseThrow();
        Instant existing = turn.getDeadlineAt();
        if (existing != null) {
            if (!now.isBefore(existing))
                throw new BusinessException(HttpStatus.CONFLICT, "Lượt hỏi đã hết thời gian");
            return existing;
        }
        Instant end = InterviewTiming.slotEnd(question.getExamAttempt());
        if (end != null && !now.isBefore(end))
            throw new BusinessException(HttpStatus.CONFLICT, "Ca thi đã hết thời gian");
        Instant deadline = now.plusSeconds(Math.min(prompt.timeLimitSeconds(), limitSeconds));
        if (end != null && end.isBefore(deadline)) deadline = end;
        turn.setDeadlineAt(deadline);
        return deadline;
    }

    public InterviewPrompt requirePrompt(UUID studentId, UUID attemptId, UUID promptId) {
        requireActiveAttempt(studentId, attemptId);
        TranscriptTurn turn = turns.findById(promptId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Không tìm thấy lượt hỏi"));
        QuestionAttempt qa = turn.getQuestionAttempt();
        if (!qa.getExamAttempt().getId().equals(attemptId)
                || qa.getStatus() != QuestionAttemptStatus.IN_PROGRESS
                || !turn.belongsToCurrentRun(qa)
                || turn.getSpeaker() != Speaker.AI
                || (turn.getTurnType() != TurnType.MAIN_QUESTION && turn.getTurnType() != TurnType.FOLLOW_UP))
            throw new BusinessException(HttpStatus.CONFLICT, "Lượt hỏi không hợp lệ hoặc không còn hoạt động");
        var latest = turns.findByQuestionAttemptIdOrderByTurnOrderAsc(qa.getId());
        if (latest.isEmpty() || !latest.getLast().getId().equals(promptId))
            throw new BusinessException(HttpStatus.CONFLICT, "Chỉ được trả lời lượt hỏi mới nhất chưa được trả lời");
        Instant deadline = InterviewTiming.deadline(turn);
        if (deadline != null && !Instant.now().isBefore(
                deadline.plusSeconds(InterviewTiming.FINALIZATION_GRACE_SECONDS)))
            throw new BusinessException(HttpStatus.CONFLICT, "Lượt hỏi đã hết thời gian");
        return new InterviewPrompt(promptId, qa.getId(), turn.getTextContent(),
                InterviewTiming.turnSeconds(qa.getExamAttempt()), turn.getDeadlineAt());
    }

}
