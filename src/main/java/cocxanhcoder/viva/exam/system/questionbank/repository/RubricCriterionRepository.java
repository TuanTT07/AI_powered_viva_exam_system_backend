package cocxanhcoder.viva.exam.system.questionbank.repository;

import cocxanhcoder.viva.exam.system.questionbank.entity.RubricCriterion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RubricCriterionRepository extends JpaRepository<RubricCriterion, UUID> {

    List<RubricCriterion> findByRubricIdOrderByCreatedAtAsc(UUID rubricId);
}
