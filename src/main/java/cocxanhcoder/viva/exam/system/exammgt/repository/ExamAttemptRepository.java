package cocxanhcoder.viva.exam.system.exammgt.repository;

import cocxanhcoder.viva.exam.system.exammgt.entity.ExamAttempt;
import cocxanhcoder.viva.exam.system.exammgt.entity.ExamAttemptStatus;
import cocxanhcoder.viva.exam.system.exammgt.dto.response.StudentExamSummary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExamAttemptRepository extends JpaRepository<ExamAttempt, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from ExamAttempt a where a.id = :id")
    Optional<ExamAttempt> findLockedById(@Param("id") UUID id);

    @Query("select a.id from ExamAttempt a where a.status = :status")
    List<UUID> findIdsByStatus(@Param("status") ExamAttemptStatus status);

    List<ExamAttempt> findByExamIdOrderByScheduledStartTimeAsc(UUID examId);

    List<ExamAttempt> findByExamIdOrderBySlotNumberAsc(UUID examId);

    List<ExamAttempt> findByStudentIdOrderByScheduledStartTimeDesc(UUID studentId);

    @Query(value = """
        select new cocxanhcoder.viva.exam.system.exammgt.dto.response.StudentExamSummary(
            e.id, a.id, e.title, c.courseCode, c.courseName, a.slotNumber,
            a.scheduledStartTime, a.scheduledEndTime, a.status)
        from ExamAttempt a join a.exam e join e.course c
        where a.student.id = :studentId and (:status is null or a.status = :status)
        order by a.scheduledStartTime desc nulls last, a.id asc
        """, countQuery = """
        select count(a) from ExamAttempt a
        where a.student.id = :studentId and (:status is null or a.status = :status)
        """)
    Page<StudentExamSummary> findStudentExams(@Param("studentId") UUID studentId,
            @Param("status") ExamAttemptStatus status, Pageable pageable);

    Optional<ExamAttempt> findByExamIdAndStudentId(UUID examId, UUID studentId);

    long countByExamIdAndStatus(UUID examId, ExamAttemptStatus status);

    @Query("""
        SELECT qa.question.id FROM QuestionAttempt qa 
        WHERE qa.examAttempt.exam.id = :examId 
        AND qa.examAttempt.slotNumber BETWEEN :fromSlot AND :toSlot
    """)
    List<UUID> findRecentUsedQuestionIds(
        @Param("examId") UUID examId,
        @Param("fromSlot") int fromSlot,
        @Param("toSlot") int toSlot
    );

    @Query("SELECT ea FROM ExamAttempt ea JOIN FETCH ea.student JOIN FETCH ea.exam WHERE ea.exam.id = :examId")
    List<ExamAttempt> findAllByExamIdWithDetails(@Param("examId") UUID examId);
}
