package cocxanhcoder.viva.exam.system.interview.entity;

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
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/** 1 câu thoại đã bóc băng (của AI hoặc sinh viên). */
@Entity
@Table(name = "transcript_turns")
@Getter
@Setter
@NoArgsConstructor
public class TranscriptTurn {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_attempt_id", nullable = false)
    private QuestionAttempt questionAttempt;

    @Column(name = "turn_order", nullable = false)
    private int turnOrder;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Speaker speaker;

    @Column(name = "text_content", nullable = false, columnDefinition = "TEXT")
    private String textContent;

    @Column(name = "audio_clip_url", length = 500)
    private String audioClipUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "turn_type", length = 50)
    private TurnType turnType;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
}
