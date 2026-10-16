package cocxanhcoder.viva.exam.system.questionbank.dto;
import cocxanhcoder.viva.exam.system.academic.entity.DocumentIndexStatus;
import java.time.OffsetDateTime;
import java.util.UUID;
public record DocumentResponse(UUID id, String title, String originalFilename, String contentType, Long fileSize,
                               DocumentIndexStatus indexingStatus, String indexingError, OffsetDateTime createdAt) { }
