package cocxanhcoder.viva.exam.system.questionbank.controller;

import cocxanhcoder.viva.exam.system.common.dto.ApiResponse;
import cocxanhcoder.viva.exam.system.common.dto.PageResponse;
import cocxanhcoder.viva.exam.system.common.util.PageableUtils;
import cocxanhcoder.viva.exam.system.questionbank.dto.QuestionRequest;
import cocxanhcoder.viva.exam.system.questionbank.dto.QuestionResponse;
import cocxanhcoder.viva.exam.system.questionbank.entity.BloomLevel;
import cocxanhcoder.viva.exam.system.questionbank.entity.QuestionStatus;
import cocxanhcoder.viva.exam.system.questionbank.service.QuestionService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/questions")
public class QuestionController {

    private final QuestionService questionService;

    public QuestionController(QuestionService questionService) {
        this.questionService = questionService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<QuestionResponse>> create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody QuestionRequest request) {
        QuestionResponse response = questionService.create(actor(jwt), request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(response.id()).toUri();
        return ApiResponse.created(location, "Tạo câu hỏi nháp thành công", response);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<QuestionResponse>>> search(@AuthenticationPrincipal Jwt jwt,
            @RequestParam UUID courseId,
            @RequestParam(required = false) QuestionStatus status,
            @RequestParam(required = false) BloomLevel bloomLevel,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok("Lấy danh sách câu hỏi thành công", PageResponse.from(questionService.search(actor(jwt), courseId, status, bloomLevel, keyword,
                PageableUtils.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt")))));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<QuestionResponse>> findById(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return ApiResponse.ok("Lấy câu hỏi thành công", questionService.findById(actor(jwt), id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<QuestionResponse>> update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id, @Valid @RequestBody QuestionRequest request) {
        return ApiResponse.ok("Cập nhật câu hỏi thành công", questionService.update(actor(jwt), id, request));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<QuestionResponse>> approve(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return ApiResponse.ok("Duyệt câu hỏi thành công", questionService.approve(actor(jwt), id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        questionService.delete(actor(jwt), id);
        return ApiResponse.ok("Xoá câu hỏi thành công");
    }

    private UUID actor(Jwt jwt) { return UUID.fromString(jwt.getSubject()); }
}
