package cocxanhcoder.viva.exam.system.academic.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

/** Tài liệu môn học (slide, giáo trình...) để AI đọc và làm RAG. */
@Entity
@Table(name = "documents")
@Getter
@Setter
@NoArgsConstructor
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(nullable = false)
    private String title;

    @Column(name = "file_url", nullable = false, length = 500)
    private String fileUrl;

    @Column(name = "original_filename", length = 500)
    private String originalFilename;
    @Column(name = "stored_filename", length = 500)
    private String storedFilename;
    @Column(name = "content_type", length = 100)
    private String contentType;
    @Column(name = "file_size")
    private Long fileSize;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by")
    private cocxanhcoder.viva.exam.system.academic.entity.User uploadedBy;
    @Column(name = "openai_file_id", length = 255)
    private String openAiFileId;
    @jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)
    @Column(name = "indexing_status", length = 30, nullable = false)
    private DocumentIndexStatus indexingStatus = DocumentIndexStatus.PENDING;
    @Column(name = "indexing_error", columnDefinition = "TEXT")
    private String indexingError;

    // Text bóc tách từ file, AI dùng để sinh câu hỏi / làm ngữ cảnh khi hỏi đáp
    @Column(name = "content_extracted", columnDefinition = "TEXT")
    private String contentExtracted;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
}
