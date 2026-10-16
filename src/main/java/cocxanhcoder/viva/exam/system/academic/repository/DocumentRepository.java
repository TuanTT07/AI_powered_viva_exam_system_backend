package cocxanhcoder.viva.exam.system.academic.repository;

import cocxanhcoder.viva.exam.system.academic.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DocumentRepository extends JpaRepository<Document, UUID> {

    List<Document> findByCourseIdOrderByCreatedAtDesc(UUID courseId);

    boolean existsByIdAndCourseId(UUID id, UUID courseId);

    @org.springframework.data.jpa.repository.Query(value = "SELECT EXISTS (SELECT 1 FROM question_source_documents WHERE document_id = :documentId)", nativeQuery = true)
    boolean isUsedAsQuestionSource(@org.springframework.data.repository.query.Param("documentId") UUID documentId);
}
