package cocxanhcoder.viva.exam.system.common.exception;

import cocxanhcoder.viva.exam.system.common.dto.ApiResponse;
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
 * Bắt lỗi tập trung cho TẤT CẢ controller, trả về cùng format ApiResponse như lúc thành công:
 * {
 *   "success": false,
 *   "status": 404,
 *   "message": "User không tồn tại (id = ...)",
 *   "data": null
 * }
 *
 * Kế thừa ResponseEntityExceptionHandler: Spring đã xử lý sẵn các lỗi chuẩn của Spring MVC
 * (sai HTTP method 405, sai kiểu tham số như UUID không hợp lệ 400, body JSON hỏng 400, ...).
 * Ta override handleExceptionInternal để các lỗi đó cũng được bọc vào ApiResponse.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** 404 - không tìm thấy dữ liệu. */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(ResourceNotFoundException ex) {
        return ApiResponse.error(HttpStatus.NOT_FOUND, ex.getMessage(), null);
    }

    /** Lỗi nghiệp vụ - status lấy từ exception (400, 409...). */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException ex) {
        return ApiResponse.error(ex.getStatus(), ex.getMessage(), null);
    }

    /** 409 - vi phạm ràng buộc DB (trùng UNIQUE, khoá ngoại...) mà service chưa kiểm tra trước. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation: {}", ex.getMessage());
        return ApiResponse.error(HttpStatus.CONFLICT,
                "Dữ liệu bị trùng hoặc đang được sử dụng ở nơi khác", null);
    }

    /** 500 - lỗi không lường trước. Log đầy đủ để debug, nhưng KHÔNG trả chi tiết lỗi ra cho client. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        return ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR,
                "Đã có lỗi xảy ra, vui lòng thử lại sau", null);
    }

    /**
     * 400 - lỗi validation (@Valid + @NotBlank, @Email...).
     * data chứa lỗi từng field để FE hiện dưới từng ô input:
     * "data": { "email": "Email không đúng định dạng", "fullName": "Họ tên không được để trống" }
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));

        ApiResponse<Map<String, String>> body =
                new ApiResponse<>(false, HttpStatus.BAD_REQUEST.value(), "Dữ liệu gửi lên không hợp lệ", errors);
        return ResponseEntity.badRequest().body(body);
    }

    /**
     * Mọi lỗi chuẩn khác của Spring MVC (405, 415, sai kiểu tham số...) đều đi qua hàm này.
     * Mặc định Spring trả ProblemDetail -> ta đổi sang ApiResponse cho thống nhất.
     */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex,
                                                             Object body,
                                                             HttpHeaders headers,
                                                             HttpStatusCode statusCode,
                                                             WebRequest request) {
        String message = (body instanceof ProblemDetail problem && problem.getDetail() != null)
                ? problem.getDetail()
                : ex.getMessage();

        ApiResponse<Void> apiBody = new ApiResponse<>(false, statusCode.value(), message, null);
        return ResponseEntity.status(statusCode).headers(headers).body(apiBody);
    }
}
