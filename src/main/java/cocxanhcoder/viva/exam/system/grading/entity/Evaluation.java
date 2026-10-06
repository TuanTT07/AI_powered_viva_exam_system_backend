package cocxanhcoder.viva.exam.system.grading.entity;

import cocxanhcoder.viva.exam.system.academic.entity.User;
import cocxanhcoder.viva.exam.system.exammgt.entity.QuestionAttempt;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Kết quả chấm cho 1 phiên hỏi đáp: AI gợi ý điểm, giảng viên chốt điểm cuối. */
@Entity
@Table(name = "evaluations")
@Getter
@Setter
@NoArgsConstructor
public class Evaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Quan hệ 1-1: mỗi QuestionAttempt chỉ có tối đa 1 Evaluation (DB có UNIQUE)
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_attempt_id", nullable = false, unique = true)
    private QuestionAttempt questionAttempt;

    // Giảng viên chốt điểm; null khi mới chỉ có điểm AI gợi ý
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "graded_by")
    private User gradedBy;

    @Column(name = "ai_suggested_score", precision = 5, scale = 2)
    private BigDecimal aiSuggestedScore;

    @Column(name = "final_score", precision = 5, scale = 2)
    private BigDecimal finalScore;

    @Column(name = "overall_feedback", columnDefinition = "TEXT")
    private String overallFeedback;

    // Cột JSONB: Hibernate tự chuyển List<CriterionScore> <-> mảng JSON
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "evaluation_details", columnDefinition = "jsonb")
    private List<CriterionScore> evaluationDetails = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private EvaluationStatus status = EvaluationStatus.PENDING_REVIEW;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
