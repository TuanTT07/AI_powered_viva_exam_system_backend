package cocxanhcoder.viva.exam.system.grading.repository;

import cocxanhcoder.viva.exam.system.grading.entity.Evaluation;
import cocxanhcoder.viva.exam.system.grading.entity.EvaluationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EvaluationRepository extends JpaRepository<Evaluation, UUID> {

    Optional<Evaluation> findByQuestionAttemptId(UUID questionAttemptId);

    boolean existsByQuestionAttemptId(UUID questionAttemptId);

    /** Danh sách bài chờ giảng viên duyệt (status = PENDING_REVIEW). */
    Page<Evaluation> findByStatus(EvaluationStatus status, Pageable pageable);
}
