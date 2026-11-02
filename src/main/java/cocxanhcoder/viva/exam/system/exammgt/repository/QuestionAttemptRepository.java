package cocxanhcoder.viva.exam.system.exammgt.repository;

import cocxanhcoder.viva.exam.system.exammgt.entity.QuestionAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuestionAttemptRepository extends JpaRepository<QuestionAttempt, UUID> {

    List<QuestionAttempt> findByExamAttemptIdOrderByQuestionOrderAsc(UUID examAttemptId);

    boolean existsByQuestionId(UUID questionId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select q from QuestionAttempt q where q.id = :id")
    Optional<QuestionAttempt> findLockedById(@Param("id") UUID id);
}
