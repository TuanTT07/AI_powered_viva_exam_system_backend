package cocxanhcoder.viva.exam.system.interview.controller;

import cocxanhcoder.viva.exam.system.common.dto.ApiResponse;
import cocxanhcoder.viva.exam.system.interview.dto.InterviewState;
import cocxanhcoder.viva.exam.system.interview.service.InterviewFlow;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/interviews/{attemptId}")
@RequiredArgsConstructor
@PreAuthorize("hasRole('STUDENT')")
public class InterviewController {
    private final InterviewFlow flow;

    @PostMapping("/start")
    public ResponseEntity<ApiResponse<InterviewState>> start(@PathVariable UUID attemptId,
            @AuthenticationPrincipal Jwt jwt) {
        return response(flow.start(UUID.fromString(jwt.getSubject()), attemptId));
    }

    @GetMapping("/current")
    public ResponseEntity<ApiResponse<InterviewState>> current(@PathVariable UUID attemptId,
            @AuthenticationPrincipal Jwt jwt) {
        return response(flow.current(UUID.fromString(jwt.getSubject()), attemptId));
    }

    private ResponseEntity<ApiResponse<InterviewState>> response(InterviewState state) {
        var result = ApiResponse.ok("Trạng thái phỏng vấn", state);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(result.getBody());
    }
}
