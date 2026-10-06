package cocxanhcoder.viva.exam.system.exammgt.repository;

import cocxanhcoder.viva.exam.system.exammgt.entity.Exam;
import cocxanhcoder.viva.exam.system.exammgt.entity.ExamStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ExamRepository extends JpaRepository<Exam, UUID> {

    List<Exam> findByCourseIdOrderByStartTimeDesc(UUID courseId);

    Page<Exam> findByCourseId(UUID courseId, Pageable pageable);

    Page<Exam> findByStatus(ExamStatus status, Pageable pageable);

    Page<Exam> findByCourseIdAndStatus(UUID courseId, ExamStatus status, Pageable pageable);

    @Query("SELECT e FROM Exam e WHERE (:courseId IS NULL OR e.course.id = :courseId) AND (:status IS NULL OR e.status = :status)")
    Page<Exam> searchExams(@Param("courseId") UUID courseId, @Param("status") ExamStatus status, Pageable pageable);
}
