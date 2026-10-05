package cocxanhcoder.viva.exam.system.academic.mapper;

import cocxanhcoder.viva.exam.system.academic.dto.CourseDetailResponse;
import cocxanhcoder.viva.exam.system.academic.dto.CourseRequest;
import cocxanhcoder.viva.exam.system.academic.dto.CourseResponse;
import cocxanhcoder.viva.exam.system.academic.dto.LecturerResponse;
import cocxanhcoder.viva.exam.system.academic.entity.Course;
import cocxanhcoder.viva.exam.system.academic.entity.User;

import java.util.Comparator;
import java.util.List;

/** Chuyển đổi Course: Request DTO -> Entity và Entity -> Response DTO. */
public final class CourseMapper {

    private CourseMapper() {
    }

    public static CourseResponse toResponse(Course course) {
        return new CourseResponse(
                course.getId(),
                course.getCourseCode(),
                course.getCourseName(),
                course.getDepartment(),
                course.getCreatedAt()
        );
    }

    public static CourseDetailResponse toDetailResponse(Course course) {
        // Sắp xếp giảng viên theo tên cho FE hiển thị ổn định (Set không có thứ tự)
        List<LecturerResponse> lecturers = course.getLecturers().stream()
                .sorted(Comparator.comparing(User::getFullName))
                .map(UserMapper::toLecturerResponse)
                .toList();

        return new CourseDetailResponse(
                course.getId(),
                course.getCourseCode(),
                course.getCourseName(),
                course.getDepartment(),
                course.getCreatedAt(),
                lecturers
        );
    }

    /** Copy dữ liệu từ request vào entity (dùng cho cả tạo mới và cập nhật). */
    public static void updateEntity(Course course, CourseRequest request) {
        course.setCourseCode(normalizeCode(request.courseCode()));
        course.setCourseName(request.courseName().trim());
        course.setDepartment(request.department() == null || request.department().isBlank()
                ? null
                : request.department().trim());
    }

    /** " swd392 " -> "SWD392": mã môn luôn viết hoa, không có khoảng trắng thừa. */
    public static String normalizeCode(String code) {
        return code.trim().toUpperCase();
    }
}
