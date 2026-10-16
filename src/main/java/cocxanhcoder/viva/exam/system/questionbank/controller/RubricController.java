package cocxanhcoder.viva.exam.system.questionbank.controller;

import cocxanhcoder.viva.exam.system.common.dto.ApiResponse;
import cocxanhcoder.viva.exam.system.questionbank.dto.RubricRequest;
import cocxanhcoder.viva.exam.system.questionbank.dto.RubricResponse;
import cocxanhcoder.viva.exam.system.questionbank.service.RubricService;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/courses/{courseId}/rubrics")
public class RubricController {

    private final RubricService rubricService;

    public RubricController(RubricService rubricService) {
        this.rubricService = rubricService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<RubricResponse>> create(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID courseId,
                                                               @Valid @RequestBody RubricRequest request) {
        RubricResponse response = rubricService.create(actor(jwt), courseId, request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(response.id()).toUri();
        return ApiResponse.created(location, "Tạo rubric thành công", response);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<RubricResponse>>> findAll(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID courseId) {
        return ApiResponse.ok("Lấy danh sách rubric thành công", rubricService.findAll(actor(jwt), courseId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RubricResponse>> findById(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID courseId, @PathVariable UUID id) {
        return ApiResponse.ok("Lấy rubric thành công", rubricService.findById(actor(jwt), courseId, id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<RubricResponse>> update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID courseId, @PathVariable UUID id,
                                                               @Valid @RequestBody RubricRequest request) {
        return ApiResponse.ok("Cập nhật rubric thành công", rubricService.update(actor(jwt), courseId, id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID courseId, @PathVariable UUID id) {
        rubricService.delete(actor(jwt), courseId, id);
        return ApiResponse.ok("Xoá rubric thành công");
    }

    private UUID actor(Jwt jwt) { return UUID.fromString(jwt.getSubject()); }
}
