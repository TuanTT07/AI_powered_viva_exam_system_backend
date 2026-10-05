package cocxanhcoder.viva.exam.system.interview.repository;

import cocxanhcoder.viva.exam.system.interview.entity.ExamAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExamAttemptRepository extends JpaRepository<ExamAttempt, UUID> {

    List<ExamAttempt> findByExamId(UUID examId);

    List<ExamAttempt> findByStudentIdOrderByCreatedAtDesc(UUID studentId);

    Optional<ExamAttempt> findByExamIdAndStudentId(UUID examId, UUID studentId);

    boolean existsByExamIdAndStudentId(UUID examId, UUID studentId);
}
