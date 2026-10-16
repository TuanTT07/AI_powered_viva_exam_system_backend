package cocxanhcoder.viva.exam.system.questionbank.controller;
import cocxanhcoder.viva.exam.system.common.dto.ApiResponse;
import cocxanhcoder.viva.exam.system.questionbank.dto.*;
import cocxanhcoder.viva.exam.system.questionbank.service.QuestionGenerationService;
import cocxanhcoder.viva.exam.system.questionbank.service.QuestionImportService;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.ByteArrayOutputStream; import java.util.List; import java.util.UUID;
@RestController @RequestMapping("/api/courses/{courseId}/questions")
public class QuestionWorkflowController {
 private final QuestionImportService importer;private final QuestionGenerationService generator;
 public QuestionWorkflowController(QuestionImportService importer,QuestionGenerationService generator){this.importer=importer;this.generator=generator;}
 @PostMapping(value="/import",consumes="multipart/form-data") public ResponseEntity<ApiResponse<ImportResult>> importFile(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID courseId,@RequestParam(required=false) UUID defaultRubricId,@RequestParam MultipartFile file){ImportResult result=importer.importWorkbook(actor(jwt),courseId,defaultRubricId,file);return ApiResponse.ok(result.errors().isEmpty()?"Import câu hỏi thành công":"Import thất bại; không có dòng nào được lưu",result);}
 @PostMapping("/generate") public ResponseEntity<ApiResponse<List<QuestionResponse>>> generate(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID courseId,@RequestBody @jakarta.validation.Valid GenerateQuestionsRequest request){return ApiResponse.ok("Tạo câu hỏi AI ở trạng thái nháp thành công",generator.generate(actor(jwt),courseId,request));}
 @GetMapping("/import-template") public ResponseEntity<ByteArrayResource> template(){try(XSSFWorkbook wb=new XSSFWorkbook();ByteArrayOutputStream out=new ByteArrayOutputStream()){var sheet=wb.createSheet("questions");var header=sheet.createRow(0);header.createCell(0).setCellValue("content");header.createCell(1).setCellValue("bloomLevel");header.createCell(2).setCellValue("rubricId");wb.write(out);return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=question-import-template.xlsx").contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")).body(new ByteArrayResource(out.toByteArray()));}catch(Exception e){throw new IllegalStateException("Không tạo được file mẫu",e);}}
 private UUID actor(Jwt jwt){return UUID.fromString(jwt.getSubject());}
}
