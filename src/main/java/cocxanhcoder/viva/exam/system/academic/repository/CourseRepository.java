package cocxanhcoder.viva.exam.system.academic.repository;

import cocxanhcoder.viva.exam.system.academic.entity.Course;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CourseRepository extends JpaRepository<Course, UUID> {

    boolean existsByCourseCode(String courseCode);

    boolean existsByCourseCodeAndIdNot(String courseCode, UUID id);

    /** Lấy môn học kèm danh sách giảng viên trong 1 query. */
    @EntityGraph(attributePaths = "lecturers")
    Optional<Course> findWithLecturersById(UUID id);

    /**
     * Tìm môn học theo mã hoặc tên. keyword dạng "%swd%" (đã lowercase ở service), null = lấy tất cả.
     */
    @Query("""
            SELECT c FROM Course c
            WHERE (:keyword IS NULL
                   OR LOWER(c.courseCode) LIKE :keyword
                   OR LOWER(c.courseName) LIKE :keyword)
            """)
    Page<Course> search(@Param("keyword") String keyword, Pageable pageable);

    /**
     * Các môn mà 1 giảng viên phụ trách.
     * "Lecturers_Id" = đi vào collection lecturers rồi so sánh field id (Spring Data tự sinh JOIN).
     */
    List<Course> findByLecturers_IdOrderByCourseCodeAsc(UUID lecturerId);

    /** Giảng viên này còn đang được phân công môn nào không? */
    boolean existsByLecturers_Id(UUID lecturerId);

    /** Môn học đã có câu hỏi hoặc kỳ thi chưa? Có rồi thì không cho xoá (DB sẽ CASCADE xoá sạch dữ liệu thi). */
    @Query(value = """
            SELECT EXISTS (SELECT 1 FROM questions WHERE course_id = :courseId)
                OR EXISTS (SELECT 1 FROM exams WHERE course_id = :courseId)
            """, nativeQuery = true)
    boolean hasQuestionsOrExams(@Param("courseId") UUID courseId);
}
