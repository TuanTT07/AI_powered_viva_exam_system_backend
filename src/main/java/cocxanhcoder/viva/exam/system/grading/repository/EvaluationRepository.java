package cocxanhcoder.viva.exam.system.grading.repository;

import cocxanhcoder.viva.exam.system.grading.entity.Evaluation;
import cocxanhcoder.viva.exam.system.grading.entity.EvaluationStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EvaluationRepository extends JpaRepository<Evaluation, UUID> {

    @Query("""
        select e from Evaluation e
        join fetch e.questionAttempt q
        join fetch q.examAttempt a
        left join fetch q.question qu
        where a.id = :attemptId and a.student.id = :studentId and e.publishedAt is not null
        order by q.questionOrder, q.id
        """)
    List<Evaluation> findPublishedEvaluations(
            @Param("studentId") UUID studentId,
            @Param("attemptId") UUID attemptId);

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
