package cocxanhcoder.viva.exam.system.academic.controller;

import cocxanhcoder.viva.exam.system.academic.dto.CourseDetailResponse;
import cocxanhcoder.viva.exam.system.academic.dto.CourseRequest;
import cocxanhcoder.viva.exam.system.academic.dto.CourseResponse;
import cocxanhcoder.viva.exam.system.academic.dto.LecturerResponse;
import cocxanhcoder.viva.exam.system.academic.service.CourseService;
import cocxanhcoder.viva.exam.system.common.dto.PageResponse;
import cocxanhcoder.viva.exam.system.common.util.PageableUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
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
 * API quản lý môn học và phân công giảng viên phụ trách môn (cho Admin).
 *
 * Phân công dùng URL dạng "tài nguyên con":
 *   PUT    /api/admin/courses/{courseId}/lecturers/{lecturerId}  -> gán giảng viên vào môn
 *   DELETE /api/admin/courses/{courseId}/lecturers/{lecturerId}  -> gỡ giảng viên khỏi môn
 * PUT idempotent: gọi lại nhiều lần kết quả vẫn như nhau (không bị gán trùng).
 */
@RestController
@RequestMapping("/api/admin/courses")
@Tag(name = "Admin - Courses", description = "Quản lý môn học và phân công giảng viên")
public class AdminCourseController {

    private final CourseService courseService;

    public AdminCourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    // ===================== MÔN HỌC =====================

    /** GET /api/admin/courses?keyword=swd&page=0&size=10 */
    @GetMapping
    @Operation(summary = "Danh sách môn học (tìm theo mã / tên, phân trang)")
    public ResponseEntity<PageResponse<CourseResponse>> searchCourses(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        var pageable = PageableUtils.of(page, size, Sort.by("courseCode"));
        return ResponseEntity.ok(courseService.searchCourses(keyword, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Chi tiết môn học kèm danh sách giảng viên")
    public ResponseEntity<CourseDetailResponse> getCourse(@PathVariable UUID id) {
        return ResponseEntity.ok(courseService.getCourse(id));
    }

    @PostMapping
    @Operation(summary = "Tạo môn học")
    public ResponseEntity<CourseDetailResponse> createCourse(@Valid @RequestBody CourseRequest request) {
        CourseDetailResponse created = courseService.createCourse(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật môn học")
    public ResponseEntity<CourseDetailResponse> updateCourse(@PathVariable UUID id,
                                                             @Valid @RequestBody CourseRequest request) {
        return ResponseEntity.ok(courseService.updateCourse(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xoá môn học (chỉ khi chưa có câu hỏi / kỳ thi)")
    public ResponseEntity<Void> deleteCourse(@PathVariable UUID id) {
        courseService.deleteCourse(id);
        return ResponseEntity.noContent().build();
    }

    // ===================== PHÂN CÔNG GIẢNG VIÊN =====================

    @GetMapping("/{courseId}/lecturers")
    @Operation(summary = "Danh sách giảng viên phụ trách môn")
    public ResponseEntity<List<LecturerResponse>> getLecturers(@PathVariable UUID courseId) {
        return ResponseEntity.ok(courseService.getLecturersOfCourse(courseId));
    }

    @PutMapping("/{courseId}/lecturers/{lecturerId}")
    @Operation(summary = "Phân công giảng viên phụ trách môn")
    public ResponseEntity<CourseDetailResponse> assignLecturer(@PathVariable UUID courseId,
                                                               @PathVariable UUID lecturerId) {
        return ResponseEntity.ok(courseService.assignLecturer(courseId, lecturerId));
    }

    @DeleteMapping("/{courseId}/lecturers/{lecturerId}")
    @Operation(summary = "Gỡ giảng viên khỏi môn")
    public ResponseEntity<Void> unassignLecturer(@PathVariable UUID courseId,
                                                 @PathVariable UUID lecturerId) {
        courseService.unassignLecturer(courseId, lecturerId);
        return ResponseEntity.noContent().build();
    }
}
