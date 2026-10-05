package cocxanhcoder.viva.exam.system.questionbank.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "rubrics")
@Getter
@Setter
@NoArgsConstructor
public class Rubric {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "rubric_name", nullable = false)
    private String rubricName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    /*
     * Rubric "sở hữu" các tiêu chí:
     * - cascade = ALL: save/delete Rubric thì criteria cũng được save/delete theo
     * - orphanRemoval: bỏ 1 criterion khỏi list -> tự xoá dòng đó trong DB
     */
    @OneToMany(mappedBy = "rubric", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<RubricCriterion> criteria = new ArrayList<>();

    /** Luôn dùng 2 hàm này thay vì criteria.add/remove để giữ 2 chiều quan hệ đồng bộ. */
    public void addCriterion(RubricCriterion criterion) {
        criteria.add(criterion);
        criterion.setRubric(this);
    }

    public void removeCriterion(RubricCriterion criterion) {
        criteria.remove(criterion);
        criterion.setRubric(null);
    }

    /** Tổng điểm tối đa của rubric = tổng max_score các tiêu chí. */
    public BigDecimal getTotalMaxScore() {
        return criteria.stream()
                .map(RubricCriterion::getMaxScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
