package cocxanhcoder.viva.exam.system.questionbank.controller;
import cocxanhcoder.viva.exam.system.common.dto.ApiResponse;
import cocxanhcoder.viva.exam.system.questionbank.dto.DocumentResponse;
import cocxanhcoder.viva.exam.system.questionbank.service.DocumentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List; import java.util.UUID;
@RestController @RequestMapping("/api/courses/{courseId}/documents")
public class DocumentController {
 private final DocumentService service; public DocumentController(DocumentService service){this.service=service;}
 @GetMapping public ResponseEntity<ApiResponse<List<DocumentResponse>>> list(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID courseId){return ApiResponse.ok("Lấy tài liệu thành công",service.list(actor(jwt),courseId));}
 @PostMapping(consumes="multipart/form-data") public ResponseEntity<ApiResponse<DocumentResponse>> upload(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID courseId,@RequestParam(required=false) String title,@RequestParam MultipartFile file){return ApiResponse.ok("Tải tài liệu và lập chỉ mục thành công",service.upload(actor(jwt),courseId,title,file));}
 @DeleteMapping("/{documentId}") public ResponseEntity<ApiResponse<Void>> delete(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID courseId,@PathVariable UUID documentId){service.delete(actor(jwt),courseId,documentId);return ApiResponse.ok("Xoá tài liệu thành công");}
 private UUID actor(Jwt jwt){return UUID.fromString(jwt.getSubject());}
}
