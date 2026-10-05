package cocxanhcoder.viva.exam.system.exammgt.repository;

import cocxanhcoder.viva.exam.system.exammgt.entity.ExamAttempt;
import cocxanhcoder.viva.exam.system.exammgt.entity.ExamAttemptStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExamAttemptRepository extends JpaRepository<ExamAttempt, UUID> {

    List<ExamAttempt> findByExamIdOrderByScheduledStartTimeAsc(UUID examId);

    List<ExamAttempt> findByExamIdOrderBySlotNumberAsc(UUID examId);

    List<ExamAttempt> findByStudentIdOrderByScheduledStartTimeDesc(UUID studentId);

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
