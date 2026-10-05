package cocxanhcoder.viva.exam.system.common.dto;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;

import java.net.URI;

/**
 * Format JSON chung cho MỌI API (cả thành công lẫn lỗi), FE chỉ cần xử lý 1 kiểu:
 * {
 *   "success": true,
 *   "status": 200,
 *   "message": "Lấy dữ liệu thành công",
 *   "data": { ... }
 * }
 * - success: true nếu status 2xx, false nếu lỗi
 * - status : trùng với HTTP status code của response
 * - data   : dữ liệu trả về; null khi không có (vd: xoá) hoặc chứa chi tiết lỗi (vd: lỗi validation)
 *
 * Các hàm static bên dưới tạo sẵn ResponseEntity để controller viết gọn:
 *   return ApiResponse.ok("Lấy dữ liệu thành công", user);
 */
public record ApiResponse<T>(
        boolean success,
        int status,
        String message,
        T data
) {

    /** 200 OK - có dữ liệu. */
    public static <T> ResponseEntity<ApiResponse<T>> ok(String message, T data) {
        return build(HttpStatus.OK, message, data);
    }

    /** 200 OK - không có dữ liệu (vd: xoá, đổi mật khẩu thành công). */
    public static ResponseEntity<ApiResponse<Void>> ok(String message) {
        return build(HttpStatus.OK, message, null);
    }

    /** 201 Created - tạo mới thành công, kèm header Location trỏ tới tài nguyên vừa tạo. */
    public static <T> ResponseEntity<ApiResponse<T>> created(URI location, String message, T data) {
        return ResponseEntity.created(location)
                .body(new ApiResponse<>(true, HttpStatus.CREATED.value(), message, data));
    }

    /** Lỗi (4xx, 5xx). data có thể null hoặc chứa chi tiết lỗi. */
    public static <T> ResponseEntity<ApiResponse<T>> error(HttpStatusCode status, String message, T data) {
        return build(status, message, data);
    }

    private static <T> ResponseEntity<ApiResponse<T>> build(HttpStatusCode status, String message, T data) {
        return ResponseEntity.status(status)
                .body(new ApiResponse<>(status.is2xxSuccessful(), status.value(), message, data));
    }
}
