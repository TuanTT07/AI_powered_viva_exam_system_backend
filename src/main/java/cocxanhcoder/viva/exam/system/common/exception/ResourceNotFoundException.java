package cocxanhcoder.viva.exam.system.common.exception;

/**
 * Ném ra khi không tìm thấy dữ liệu (user, môn học...) -> GlobalExceptionHandler trả về HTTP 404.
 * Ví dụ: throw new ResourceNotFoundException("User", id);
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resourceName, Object id) {
        this(resourceName + " không tồn tại (id = " + id + ")");
    }
}
