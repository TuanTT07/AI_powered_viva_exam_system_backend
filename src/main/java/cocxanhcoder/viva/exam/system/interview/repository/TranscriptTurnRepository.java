package cocxanhcoder.viva.exam.system.interview.repository;

import cocxanhcoder.viva.exam.system.interview.entity.TranscriptTurn;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TranscriptTurnRepository extends JpaRepository<TranscriptTurn, UUID> {

    List<TranscriptTurn> findByQuestionAttemptIdOrderByTurnOrderAsc(UUID questionAttemptId);
}
