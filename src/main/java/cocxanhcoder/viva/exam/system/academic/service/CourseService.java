package cocxanhcoder.viva.exam.system.academic.service;

import cocxanhcoder.viva.exam.system.academic.dto.CourseDetailResponse;
import cocxanhcoder.viva.exam.system.academic.dto.CourseRequest;
import cocxanhcoder.viva.exam.system.academic.dto.CourseResponse;
import cocxanhcoder.viva.exam.system.academic.dto.LecturerResponse;
import cocxanhcoder.viva.exam.system.academic.entity.Course;
import cocxanhcoder.viva.exam.system.academic.entity.Role;
import cocxanhcoder.viva.exam.system.academic.entity.User;
import cocxanhcoder.viva.exam.system.academic.mapper.CourseMapper;
import cocxanhcoder.viva.exam.system.academic.repository.CourseRepository;
import cocxanhcoder.viva.exam.system.academic.repository.UserRepository;
import cocxanhcoder.viva.exam.system.common.dto.PageResponse;
import cocxanhcoder.viva.exam.system.common.exception.BusinessException;
import cocxanhcoder.viva.exam.system.common.exception.ResourceNotFoundException;
import cocxanhcoder.viva.exam.system.common.util.SearchUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Nghiệp vụ quản lý môn học + phân công giảng viên phụ trách môn (dành cho Admin).
 */
@Service
@Transactional(readOnly = true)
public class CourseService {

    private final CourseRepository courseRepository;
    private final UserRepository userRepository;

    public CourseService(CourseRepository courseRepository, UserRepository userRepository) {
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
    }

    // ===================== MÔN HỌC =====================

    public PageResponse<CourseResponse> searchCourses(String keyword, Pageable pageable) {
        return PageResponse.from(
                courseRepository.search(SearchUtils.toLikePattern(keyword), pageable)
                        .map(CourseMapper::toResponse)
        );
    }

    public CourseDetailResponse getCourse(UUID id) {
        return CourseMapper.toDetailResponse(findCourseWithLecturers(id));
    }

    @Transactional
    public CourseDetailResponse createCourse(CourseRequest request) {
        String code = CourseMapper.normalizeCode(request.courseCode());
        if (courseRepository.existsByCourseCode(code)) {
            throw new BusinessException(HttpStatus.CONFLICT, "Mã môn đã tồn tại: " + code);
        }

        Course course = new Course();
        CourseMapper.updateEntity(course, request);
        return CourseMapper.toDetailResponse(courseRepository.saveAndFlush(course));
    }

    @Transactional
    public CourseDetailResponse updateCourse(UUID id, CourseRequest request) {
        Course course = findCourseWithLecturers(id);
        String code = CourseMapper.normalizeCode(request.courseCode());
        if (courseRepository.existsByCourseCodeAndIdNot(code, id)) {
            throw new BusinessException(HttpStatus.CONFLICT, "Mã môn đã tồn tại: " + code);
        }

        CourseMapper.updateEntity(course, request);
        return CourseMapper.toDetailResponse(course); // dirty checking tự UPDATE khi commit
    }

    @Transactional
    public void deleteCourse(UUID id) {
        Course course = findCourse(id);
        if (courseRepository.hasQuestionsOrExams(id)) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "Không thể xoá: môn học đã có câu hỏi hoặc kỳ thi");
        }
        courseRepository.delete(course);
    }

    // ===================== PHÂN CÔNG GIẢNG VIÊN =====================

    public List<LecturerResponse> getLecturersOfCourse(UUID courseId) {
        return CourseMapper.toDetailResponse(findCourseWithLecturers(courseId)).lecturers();
    }

    /**
     * Gán giảng viên vào môn. Gọi nhiều lần cũng chỉ gán 1 lần (Set tự bỏ trùng) -> API idempotent.
     */
    @Transactional
    public CourseDetailResponse assignLecturer(UUID courseId, UUID lecturerId) {
        Course course = findCourseWithLecturers(courseId);
        User lecturer = findLecturer(lecturerId);

        // Thêm vào collection -> Hibernate tự INSERT 1 dòng vào bảng course_lecturers khi commit
        course.getLecturers().add(lecturer);
        return CourseMapper.toDetailResponse(course);
    }

    @Transactional
    public void unassignLecturer(UUID courseId, UUID lecturerId) {
        Course course = findCourseWithLecturers(courseId);

        // removeIf trả về false nếu không có ai bị xoá -> giảng viên chưa được phân công môn này
        boolean removed = course.getLecturers().removeIf(l -> l.getId().equals(lecturerId));
        if (!removed) {
            throw new ResourceNotFoundException("Giảng viên chưa được phân công cho môn học này");
        }
        // Hibernate tự DELETE dòng tương ứng trong course_lecturers khi commit
    }

    /** Các môn mà 1 giảng viên đang phụ trách. */
    public List<CourseResponse> getCoursesOfLecturer(UUID lecturerId) {
        findLecturer(lecturerId);
        return courseRepository.findByLecturers_IdOrderByCourseCodeAsc(lecturerId).stream()
                .map(CourseMapper::toResponse)
                .toList();
    }

    // ===================== HÀM PHỤ =====================

    private Course findCourse(UUID id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Môn học", id));
    }

    private Course findCourseWithLecturers(UUID id) {
        return courseRepository.findWithLecturersById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Môn học", id));
    }

    /** Tìm user và đảm bảo user đó có role LECTURER. */
    private User findLecturer(UUID lecturerId) {
        User user = userRepository.findWithRoleById(lecturerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", lecturerId));
        if (!Role.LECTURER.equals(user.getRole().getRoleName())) {
            throw new BusinessException("User " + user.getUserCode() + " không phải giảng viên");
        }
        return user;
    }
}
