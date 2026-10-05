package cocxanhcoder.viva.exam.system.academic.repository;

import cocxanhcoder.viva.exam.system.academic.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CourseRepository extends JpaRepository<Course, UUID> {

    boolean existsByCourseCode(String courseCode);
}
