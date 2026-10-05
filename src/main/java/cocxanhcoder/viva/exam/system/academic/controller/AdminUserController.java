package cocxanhcoder.viva.exam.system.academic.controller;

import cocxanhcoder.viva.exam.system.academic.dto.CourseResponse;
import cocxanhcoder.viva.exam.system.academic.dto.CreateUserRequest;
import cocxanhcoder.viva.exam.system.academic.dto.ResetPasswordRequest;
import cocxanhcoder.viva.exam.system.academic.dto.UpdateUserRequest;
import cocxanhcoder.viva.exam.system.academic.dto.UserResponse;
import cocxanhcoder.viva.exam.system.academic.service.CourseService;
import cocxanhcoder.viva.exam.system.academic.service.UserService;
import cocxanhcoder.viva.exam.system.common.dto.ApiResponse;
import cocxanhcoder.viva.exam.system.common.dto.PageResponse;
import cocxanhcoder.viva.exam.system.common.util.PageableUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * API quản lý tài khoản cho Admin.
 *
 * Controller chỉ làm 3 việc: nhận request -> gọi service -> bọc kết quả vào ApiResponse.
 * Mọi API trả về cùng format { success, status, message, data }:
 *   200 OK      - đọc / cập nhật / xoá thành công
 *   201 Created - tạo mới thành công, kèm header Location trỏ tới tài nguyên mới
 * Lỗi (400, 404, 409...) do GlobalExceptionHandler xử lý, controller không cần try/catch.
 *
 * TODO: khi có JWT thêm @PreAuthorize("hasRole('ADMIN')") để chỉ Admin gọi được.
 */
@RestController
@RequestMapping("/api/admin/users")
@Tag(name = "Admin - Users", description = "Quản lý tài khoản")
public class AdminUserController {

    private final UserService userService;
    private final CourseService courseService;

    public AdminUserController(UserService userService, CourseService courseService) {
        this.userService = userService;
        this.courseService = courseService;
    }

    /** GET /api/admin/users?role=LECTURER&keyword=nguyen&page=0&size=10 */
    @GetMapping
    @Operation(summary = "Danh sách tài khoản (lọc theo role, tìm theo tên / email / mã, phân trang)")
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> searchUsers(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        var pageable = PageableUtils.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ApiResponse.ok("Lấy danh sách tài khoản thành công",
                userService.searchUsers(role, keyword, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Chi tiết 1 tài khoản")
    public ResponseEntity<ApiResponse<UserResponse>> getUser(@PathVariable UUID id) {
        return ApiResponse.ok("Lấy thông tin tài khoản thành công", userService.getUser(id));
    }

    @PostMapping
    @Operation(summary = "Tạo tài khoản mới")
    public ResponseEntity<ApiResponse<UserResponse>> createUser(@Valid @RequestBody CreateUserRequest request) {
        UserResponse created = userService.createUser(request);

        // Header Location: /api/admin/users/{id-vừa-tạo}
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ApiResponse.created(location, "Tạo tài khoản thành công", created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật họ tên, email, role")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(@PathVariable UUID id,
                                                                @Valid @RequestBody UpdateUserRequest request) {
        return ApiResponse.ok("Cập nhật tài khoản thành công", userService.updateUser(id, request));
    }

    @PatchMapping("/{id}/password")
    @Operation(summary = "Admin đặt lại mật khẩu")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@PathVariable UUID id,
                                                           @Valid @RequestBody ResetPasswordRequest request) {
        userService.resetPassword(id, request);
        return ApiResponse.ok("Đặt lại mật khẩu thành công");
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xoá tài khoản (chỉ khi chưa có dữ liệu thi / câu hỏi / chấm điểm)")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable UUID id) {
        userService.deleteUser(id);
        return ApiResponse.ok("Xoá tài khoản thành công");
    }

    /** GET /api/admin/users/{id}/courses - các môn giảng viên này đang phụ trách. */
    @GetMapping("/{id}/courses")
    @Operation(summary = "Các môn học mà giảng viên đang phụ trách")
    public ResponseEntity<ApiResponse<List<CourseResponse>>> getCoursesOfLecturer(@PathVariable UUID id) {
        return ApiResponse.ok("Lấy danh sách môn học của giảng viên thành công",
                courseService.getCoursesOfLecturer(id));
    }
}
