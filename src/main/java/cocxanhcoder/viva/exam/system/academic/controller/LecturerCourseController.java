package cocxanhcoder.viva.exam.system.academic.controller;

import cocxanhcoder.viva.exam.system.academic.dto.CourseResponse;
import cocxanhcoder.viva.exam.system.academic.service.CourseService;
import cocxanhcoder.viva.exam.system.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * API tạm thời cho giao diện giảng viên. Khi JWT được tích hợp, endpoint này sẽ
 * được thay bằng GET /api/lecturer/courses và lấy lecturerId từ access token.
 */
@RestController
@RequestMapping("/api/lecturers")
@Tag(name = "Lecturer - Courses", description = "Môn học được phân công cho giảng viên")
public class LecturerCourseController {

    private final CourseService courseService;

    public LecturerCourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    /** GET /api/lecturers/{lecturerId}/courses */
    @GetMapping("/{lecturerId}/courses")
    @Operation(summary = "Danh sách môn học được phân công cho giảng viên")
    public ResponseEntity<ApiResponse<List<CourseResponse>>> getAssignedCourses(
            @PathVariable UUID lecturerId) {
        return ApiResponse.ok("Lấy danh sách môn học của giảng viên thành công",
                courseService.getCoursesOfLecturer(lecturerId));
    }
}
