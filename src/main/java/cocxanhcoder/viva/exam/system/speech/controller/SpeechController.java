package cocxanhcoder.viva.exam.system.speech.controller;

import cocxanhcoder.viva.exam.system.interview.dto.AnswerReceipt;
import cocxanhcoder.viva.exam.system.interview.service.InterviewAnswers;
import cocxanhcoder.viva.exam.system.interview.service.InterviewAccess;
import cocxanhcoder.viva.exam.system.common.dto.ApiResponse;
import cocxanhcoder.viva.exam.system.speech.dto.SpeechProtocol.*;
import cocxanhcoder.viva.exam.system.speech.service.*;
import cocxanhcoder.viva.exam.system.speech.config.SpeechProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/interview")
@RequiredArgsConstructor
@PreAuthorize("hasRole('STUDENT')")
public class SpeechController {
    private final SpeechTicketService tickets;
    private final QuestionSpeechService audio;
    private final InterviewAnswers transcripts;
    private final InterviewAccess access;
    private final SpeechProperties properties;

    @GetMapping("/attempts/{attemptId}/speech/questions/{questionTurnId}/playback")
    public ResponseEntity<ApiResponse<QuestionPlayback>> playback(@PathVariable UUID attemptId,
            @PathVariable UUID questionTurnId, @AuthenticationPrincipal Jwt jwt) {
        var prompt = access.requirePrompt(UUID.fromString(jwt.getSubject()), attemptId, questionTurnId);
        return ApiResponse.ok("Lấy nội dung câu hỏi để phát audio",
                new QuestionPlayback(questionTurnId, prompt.text(), "deepgram", properties.getLanguage()));
    }

    @PostMapping("/attempts/{attemptId}/speech-ticket")
    public ResponseEntity<ApiResponse<Ticket>> ticket(@PathVariable UUID attemptId, @AuthenticationPrincipal Jwt jwt) {
        var result = ApiResponse.ok("Cấp ticket speech thành công",
                tickets.issue(UUID.fromString(jwt.getSubject()), attemptId, jwt.getExpiresAt()));
        return ResponseEntity.status(result.getStatusCode()).cacheControl(CacheControl.noStore()).body(result.getBody());
    }

    @PostMapping("/attempts/{attemptId}/speech/questions/{questionTurnId}/audio")
    public ResponseEntity<ApiResponse<AudioResult>> synthesize(@PathVariable UUID attemptId,
            @PathVariable UUID questionTurnId, @AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.ok("Tạo audio câu hỏi thành công",
                audio.create(UUID.fromString(jwt.getSubject()), attemptId, questionTurnId));
    }

    @GetMapping("/attempts/{attemptId}/speech/answers/{answerId}")
    public ResponseEntity<ApiResponse<AnswerReceipt>> answer(@PathVariable UUID attemptId,
            @PathVariable UUID answerId, @AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.ok("Lấy câu trả lời thành công",
                transcripts.findAnswer(UUID.fromString(jwt.getSubject()), attemptId, answerId));
    }

    /** Media response intentionally returns bytes; metadata/errors retain ApiResponse. */
    @GetMapping("/speech/audio/{audioId}")
    public ResponseEntity<byte[]> read(@PathVariable UUID audioId, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("audio/mpeg"))
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=question.mp3")
                .body(audio.read(UUID.fromString(jwt.getSubject()), audioId));
    }
}
