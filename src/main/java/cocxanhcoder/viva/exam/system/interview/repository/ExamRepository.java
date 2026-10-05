package cocxanhcoder.viva.exam.system.interview.repository;

import cocxanhcoder.viva.exam.system.interview.entity.Exam;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ExamRepository extends JpaRepository<Exam, UUID> {

    List<Exam> findByCourseIdOrderByStartTimeDesc(UUID courseId);
}
