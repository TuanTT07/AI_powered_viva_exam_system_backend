package cocxanhcoder.viva.exam.system.grading.repository;

import cocxanhcoder.viva.exam.system.grading.entity.Evaluation;
import cocxanhcoder.viva.exam.system.grading.entity.EvaluationStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EvaluationRepository extends JpaRepository<Evaluation, UUID> {

    Optional<Evaluation> findByQuestionAttemptId(UUID questionAttemptId);

    Page<Evaluation> findByQuestionAttempt_ExamAttempt_Exam_Id(UUID examId, Pageable pageable);

    Page<Evaluation> findByQuestionAttempt_ExamAttempt_Exam_IdAndStatus(
            UUID examId, EvaluationStatus status, Pageable pageable);

    boolean existsByQuestionAttemptId(UUID questionAttemptId);

    /** Danh sách bài chờ giảng viên duyệt (status = PENDING_REVIEW). */
    Page<Evaluation> findByStatus(EvaluationStatus status, Pageable pageable);

    /** Danh sách evaluation của 1 môn học, lọc theo status (vd: PENDING_REVIEW chờ giảng viên duyệt). */
    Page<Evaluation> findByQuestionAttempt_ExamAttempt_Exam_Course_IdAndStatus(
            UUID courseId, EvaluationStatus status, Pageable pageable);

    /** Danh sách evaluation của 1 môn học, không lọc status. */
    Page<Evaluation> findByQuestionAttempt_ExamAttempt_Exam_Course_Id(UUID courseId, Pageable pageable);
}
