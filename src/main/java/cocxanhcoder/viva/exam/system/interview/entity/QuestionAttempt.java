package cocxanhcoder.viva.exam.system.interview.entity;

import cocxanhcoder.viva.exam.system.questionbank.entity.Question;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Phiên hỏi đáp cho 1 câu hỏi trong 1 lượt thi (gồm câu hỏi chính + các câu hỏi thêm). */
@Entity
@Table(name = "question_attempts")
@Getter
@Setter
@NoArgsConstructor
public class QuestionAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exam_attempt_id", nullable = false)
    private ExamAttempt examAttempt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(name = "start_time")
    private OffsetDateTime startTime;

    @Column(name = "end_time")
    private OffsetDateTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private QuestionAttemptStatus status = QuestionAttemptStatus.PENDING;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    // Các lượt thoại AI <-> sinh viên, sắp theo thứ tự
    @OneToMany(mappedBy = "questionAttempt", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("turnOrder ASC")
    private List<TranscriptTurn> transcriptTurns = new ArrayList<>();

    /** Thêm 1 lượt thoại, tự đánh số turnOrder tiếp theo (1, 2, 3...). */
    public void addTurn(TranscriptTurn turn) {
        turn.setTurnOrder(transcriptTurns.size() + 1);
        transcriptTurns.add(turn);
        turn.setQuestionAttempt(this);
    }
}
