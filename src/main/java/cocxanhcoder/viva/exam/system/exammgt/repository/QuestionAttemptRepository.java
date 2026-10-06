package cocxanhcoder.viva.exam.system.exammgt.repository;

import cocxanhcoder.viva.exam.system.exammgt.entity.QuestionAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface QuestionAttemptRepository extends JpaRepository<QuestionAttempt, UUID> {

    List<QuestionAttempt> findByExamAttemptIdOrderByQuestionOrderAsc(UUID examAttemptId);
}
