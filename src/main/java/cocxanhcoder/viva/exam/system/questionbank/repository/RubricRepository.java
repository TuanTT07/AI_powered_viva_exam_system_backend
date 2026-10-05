package cocxanhcoder.viva.exam.system.questionbank.repository;

import cocxanhcoder.viva.exam.system.questionbank.entity.Rubric;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RubricRepository extends JpaRepository<Rubric, UUID> {

    /** Lấy rubric kèm luôn danh sách criteria trong 1 query (tránh lỗi N+1 / LazyInitialization). */
    @EntityGraph(attributePaths = "criteria")
    Optional<Rubric> findWithCriteriaById(UUID id);
}
