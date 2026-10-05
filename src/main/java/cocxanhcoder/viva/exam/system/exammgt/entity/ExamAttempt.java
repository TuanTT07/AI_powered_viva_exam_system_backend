package cocxanhcoder.viva.exam.system.exammgt.entity;

import cocxanhcoder.viva.exam.system.academic.entity.User;
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

/**
 * Entity đại diện cho ca thi cá nhân của một sinh viên trong kỳ thi.
 */
@Entity
@Table(name = "exam_attempts")
@Getter
@Setter
@NoArgsConstructor
public class ExamAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exam_id", nullable = false)
    private Exam exam;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Column(name = "scheduled_start_time")
    private OffsetDateTime scheduledStartTime;

    @Column(name = "scheduled_end_time")
    private OffsetDateTime scheduledEndTime;

    @Column(name = "start_time")
    private OffsetDateTime actualStartTime;

    @Column(name = "end_time")
    private OffsetDateTime actualEndTime;

    @Column(name = "slot_number")
    private Integer slotNumber;

    @Column(name = "access_code", length = 50)
    private String accessCode;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private ExamAttemptStatus status = ExamAttemptStatus.SCHEDULED;

    @Column(name = "audio_record_url", length = 500)
    private String audioRecordUrl;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @OneToMany(mappedBy = "examAttempt", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("questionOrder ASC")
    private List<QuestionAttempt> questionAttempts = new ArrayList<>();

    public void addQuestionAttempt(QuestionAttempt questionAttempt) {
        questionAttempts.add(questionAttempt);
        questionAttempt.setExamAttempt(this);
    }
}
