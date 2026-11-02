package cocxanhcoder.viva.exam.system.grading.controller;

import cocxanhcoder.viva.exam.system.common.dto.ApiResponse;
import cocxanhcoder.viva.exam.system.common.dto.PageResponse;
import cocxanhcoder.viva.exam.system.common.util.PageableUtils;
import cocxanhcoder.viva.exam.system.grading.dto.EvaluationResponse;
import cocxanhcoder.viva.exam.system.grading.dto.FinalizeEvaluationRequest;
import cocxanhcoder.viva.exam.system.grading.entity.EvaluationStatus;
import cocxanhcoder.viva.exam.system.grading.service.EvaluationService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class EvaluationController {

    private final EvaluationService evaluationService;

    public EvaluationController(EvaluationService evaluationService) {
        this.evaluationService = evaluationService;
    }

    @PostMapping("/api/question-attempts/{questionAttemptId}/evaluation/ai-grade")
    public ResponseEntity<ApiResponse<EvaluationResponse>> gradeWithAi(@AuthenticationPrincipal Jwt jwt,
                                                                        @PathVariable UUID questionAttemptId) {
        return ApiResponse.ok("AI chấm điểm thành công", evaluationService.gradeWithAi(actor(jwt), questionAttemptId));
    }

    @GetMapping("/api/question-attempts/{questionAttemptId}/evaluation")
    public ResponseEntity<ApiResponse<EvaluationResponse>> getEvaluation(@AuthenticationPrincipal Jwt jwt,
                                                                          @PathVariable UUID questionAttemptId) {
        return ApiResponse.ok("Lấy kết quả chấm điểm thành công",
                evaluationService.getByQuestionAttempt(actor(jwt), questionAttemptId));
    }

    @PutMapping("/api/question-attempts/{questionAttemptId}/evaluation/final")
    public ResponseEntity<ApiResponse<EvaluationResponse>> finalizeEvaluation(@AuthenticationPrincipal Jwt jwt,
                                                                               @PathVariable UUID questionAttemptId,
                                                                               @Valid @RequestBody FinalizeEvaluationRequest request) {
        return ApiResponse.ok("Chốt điểm thành công",
                evaluationService.finalizeEvaluation(actor(jwt), questionAttemptId, request));
    }

    @GetMapping("/api/courses/{courseId}/evaluations")
    public ResponseEntity<ApiResponse<PageResponse<EvaluationResponse>>> findByCourse(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID courseId,
            @RequestParam(required = false) EvaluationStatus status,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        var pageable = PageableUtils.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ApiResponse.ok("Lấy danh sách kết quả chấm điểm thành công",
                evaluationService.findByCourse(actor(jwt), courseId, status, pageable));
    }

    private UUID actor(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
