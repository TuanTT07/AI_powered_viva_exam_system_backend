package cocxanhcoder.viva.exam.system.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Lỗi nghiệp vụ: dữ liệu hợp lệ về format nhưng vi phạm quy tắc của hệ thống.
 * Ví dụ: email đã tồn tại (409), gán môn học cho user không phải giảng viên (400).
 * Mặc định trả về 400 Bad Request, có thể truyền status khác.
 */
public class BusinessException extends RuntimeException {

    private final HttpStatus status;

    public BusinessException(String message) {
        this(HttpStatus.BAD_REQUEST, message);
    }

    public BusinessException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
