package cocxanhcoder.viva.exam.system.interview.service;

import cocxanhcoder.viva.exam.system.common.exception.BusinessException;
import cocxanhcoder.viva.exam.system.interview.dto.InterviewPrompt;
import cocxanhcoder.viva.exam.system.academic.repository.UserRepository;
import cocxanhcoder.viva.exam.system.exammgt.entity.*;
import cocxanhcoder.viva.exam.system.exammgt.repository.ExamAttemptRepository;
import cocxanhcoder.viva.exam.system.interview.entity.*;
import cocxanhcoder.viva.exam.system.interview.repository.TranscriptTurnRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InterviewAccessService implements InterviewAccess {
    private final ExamAttemptRepository attempts;
    private final TranscriptTurnRepository turns;
    private final UserRepository users;

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

    public InterviewPrompt requirePrompt(UUID studentId, UUID attemptId, UUID promptId) {
        requireActiveAttempt(studentId, attemptId);
        TranscriptTurn turn = turns.findById(promptId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Không tìm thấy lượt hỏi"));
        QuestionAttempt qa = turn.getQuestionAttempt();
        if (!qa.getExamAttempt().getId().equals(attemptId)
                || qa.getStatus() != QuestionAttemptStatus.IN_PROGRESS
                || turn.getSpeaker() != Speaker.AI
                || (turn.getTurnType() != TurnType.MAIN_QUESTION && turn.getTurnType() != TurnType.FOLLOW_UP))
            throw new BusinessException(HttpStatus.CONFLICT, "Lượt hỏi không hợp lệ hoặc không còn hoạt động");
        var latest = turns.findByQuestionAttemptIdOrderByTurnOrderAsc(qa.getId());
        if (latest.isEmpty() || !latest.getLast().getId().equals(promptId))
            throw new BusinessException(HttpStatus.CONFLICT, "Chỉ được trả lời lượt hỏi mới nhất chưa được trả lời");
        var configuration = qa.getExamAttempt().getExam().getExamConfig();
        Object configured = configuration == null ? null : configuration.get("timeLimitPerTurnSeconds");
        int limit = configured instanceof Number n && n.intValue() >= 10 ? n.intValue() : 60;
        return new InterviewPrompt(promptId, qa.getId(), turn.getTextContent(), limit);
    }

}
