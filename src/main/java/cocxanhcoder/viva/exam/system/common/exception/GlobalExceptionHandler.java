package cocxanhcoder.viva.exam.system.common.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Bắt lỗi tập trung cho TẤT CẢ controller, trả về chuẩn ProblemDetail (RFC 9457):
 * {
 *   "type": "about:blank",
 *   "title": "Không tìm thấy dữ liệu",
 *   "status": 404,
 *   "detail": "User không tồn tại (id = ...)",
 *   "instance": "/api/admin/users/..."
 * }
 *
 * Kế thừa ResponseEntityExceptionHandler: Spring đã xử lý sẵn các lỗi chuẩn của Spring MVC
 * (sai HTTP method 405, sai kiểu tham số như UUID không hợp lệ 400, body JSON hỏng 400, ...).
 * Ở đây chỉ bổ sung các lỗi của riêng mình.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** 404 - không tìm thấy dữ liệu. */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(ResourceNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "Không tìm thấy dữ liệu", ex.getMessage());
    }

    /** Lỗi nghiệp vụ - status lấy từ exception (400, 409...). */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ProblemDetail> handleBusiness(BusinessException ex) {
        return problem(ex.getStatus(), "Yêu cầu không hợp lệ", ex.getMessage());
    }

    /** 409 - vi phạm ràng buộc DB (trùng UNIQUE, khoá ngoại...) mà service chưa kiểm tra trước. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation: {}", ex.getMessage());
        return problem(HttpStatus.CONFLICT, "Xung đột dữ liệu",
                "Dữ liệu bị trùng hoặc đang được sử dụng ở nơi khác");
    }

    /** 500 - lỗi không lường trước. Log đầy đủ để debug, nhưng KHÔNG trả chi tiết lỗi ra cho client. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi hệ thống",
                "Đã có lỗi xảy ra, vui lòng thử lại sau");
    }

    /**
     * 400 - lỗi validation (@Valid + @NotBlank, @Email...).
     * Trả thêm field "errors" để FE hiện lỗi dưới từng ô input:
     * "errors": { "email": "Email không đúng định dạng", "fullName": "Họ tên không được để trống" }
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));

        ProblemDetail body = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Dữ liệu gửi lên không hợp lệ");
        body.setTitle("Lỗi validation");
        body.setProperty("errors", errors);
        return ResponseEntity.badRequest().body(body);
    }

    private ResponseEntity<ProblemDetail> problem(HttpStatusCode status, String title, String detail) {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(status, detail);
        body.setTitle(title);
        return ResponseEntity.status(status).body(body);
    }
}
