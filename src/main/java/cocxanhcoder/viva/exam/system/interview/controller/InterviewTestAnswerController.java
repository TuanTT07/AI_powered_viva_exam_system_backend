package cocxanhcoder.viva.exam.system.interview.controller;

import cocxanhcoder.viva.exam.system.common.dto.ApiResponse;
import cocxanhcoder.viva.exam.system.common.exception.BusinessException;
import cocxanhcoder.viva.exam.system.interview.dto.AnswerReceipt;
import cocxanhcoder.viva.exam.system.interview.service.InterviewAccess;
import cocxanhcoder.viva.exam.system.interview.service.InterviewAnswers;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@Profile("local-test")
@RequiredArgsConstructor
@PreAuthorize("hasRole('STUDENT')")
@RequestMapping("/api/interviews/{attemptId}/test-answers")
public class InterviewTestAnswerController {
    private final InterviewAccess access;
    private final InterviewAnswers answers;

    @PostMapping
    public ResponseEntity<ApiResponse<AnswerReceipt>> submit(@PathVariable UUID attemptId,
            @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody TestAnswer request) {
        UUID studentId = UUID.fromString(jwt.getSubject());
        var prompt = access.requirePrompt(studentId, attemptId, request.questionTurnId());
        Instant now = Instant.now();
        if (prompt.deadlineAt() != null && !now.isBefore(prompt.deadlineAt()))
            throw new BusinessException(HttpStatus.CONFLICT, "Lượt hỏi đã hết thời gian");
        return ApiResponse.ok("Đã lưu câu trả lời thử",
                answers.complete(studentId, attemptId, prompt.questionTurnId(), prompt.questionAttemptId(),
                        UUID.randomUUID(), request.text(), now, now));
    }

    public record TestAnswer(@NotNull UUID questionTurnId, @NotBlank @Size(max = 10000) String text) {}
}
