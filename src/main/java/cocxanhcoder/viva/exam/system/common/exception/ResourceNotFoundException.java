package cocxanhcoder.viva.exam.system.common.exception;

/** Ném ra khi không tìm thấy dữ liệu → HTTP 404. */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resource, Object id) {
        super(resource + " not found with id: " + id);
    }
}
