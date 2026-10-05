package cocxanhcoder.viva.exam.system.interview.repository;

import cocxanhcoder.viva.exam.system.interview.entity.QuestionAttempt;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuestionAttemptRepository extends JpaRepository<QuestionAttempt, UUID> {

    List<QuestionAttempt> findByExamAttemptIdOrderByCreatedAtAsc(UUID examAttemptId);

    /** Lấy phiên hỏi đáp kèm toàn bộ transcript (dùng khi AI chấm điểm / GV xem lại). */
    @EntityGraph(attributePaths = "transcriptTurns")
    Optional<QuestionAttempt> findWithTranscriptById(UUID id);
}
