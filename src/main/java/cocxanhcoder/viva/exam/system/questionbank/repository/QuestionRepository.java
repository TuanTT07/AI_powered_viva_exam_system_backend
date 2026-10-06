package cocxanhcoder.viva.exam.system.questionbank.repository;

import cocxanhcoder.viva.exam.system.questionbank.entity.BloomLevel;
import cocxanhcoder.viva.exam.system.questionbank.entity.Question;
import cocxanhcoder.viva.exam.system.questionbank.entity.QuestionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface QuestionRepository extends JpaRepository<Question, UUID> {

    /**
     * Tìm kiếm + lọc câu hỏi. Tham số nào null thì bỏ qua điều kiện đó.
     * keyword: tìm gần đúng trong nội dung câu hỏi (không phân biệt hoa thường).
     */
    @Query("""
            SELECT q FROM Question q
            WHERE (:courseId IS NULL OR q.course.id = :courseId)
              AND (:status IS NULL OR q.status = :status)
              AND (:bloomLevel IS NULL OR q.bloomLevel = :bloomLevel)
              AND (:keyword IS NULL OR LOWER(q.content) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')))
            """)
    Page<Question> search(@Param("courseId") UUID courseId,
                          @Param("status") QuestionStatus status,
                          @Param("bloomLevel") BloomLevel bloomLevel,
                          @Param("keyword") String keyword,
                          Pageable pageable);

    long countByRubricId(UUID rubricId);

    java.util.List<Question> findByCourseIdAndStatus(UUID courseId, QuestionStatus status);
}

