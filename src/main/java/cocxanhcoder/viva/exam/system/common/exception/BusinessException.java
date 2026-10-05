package cocxanhcoder.viva.exam.system.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Lỗi nghiệp vụ (vi phạm quy tắc), ví dụ: trùng tên câu hỏi, xoá rubric đang được dùng...
 * Mặc định HTTP 400, có thể truyền status khác (vd: 409 CONFLICT).
 */
public class BusinessException extends RuntimeException {

    private final HttpStatus status;

    public BusinessException(String message) {
        this(message, HttpStatus.BAD_REQUEST);
    }

    public BusinessException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
