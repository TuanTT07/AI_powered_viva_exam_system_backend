package cocxanhcoder.viva.exam.system.academic.repository;

import cocxanhcoder.viva.exam.system.academic.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    /** Dùng khi cập nhật: email đã thuộc về user KHÁC (id khác) chưa? */
    boolean existsByEmailAndIdNot(String email, UUID id);

    boolean existsByUserCode(String userCode);

    /** findById nhưng load luôn role trong cùng 1 query (tránh query thêm khi map sang DTO). */
    @EntityGraph(attributePaths = "role")
    Optional<User> findWithRoleById(UUID id);

    /**
     * Tìm kiếm user có phân trang. Tham số nào null thì bỏ qua điều kiện đó.
     * - roleName: lọc theo role (ADMIN / LECTURER / STUDENT)
     * - keyword : chuỗi dạng "%abc%" (đã lowercase ở service), tìm trong họ tên, email, mã user
     */
    @EntityGraph(attributePaths = "role")
    @Query("""
            SELECT u FROM User u
            WHERE (:roleName IS NULL OR u.role.roleName = :roleName)
              AND (:keyword IS NULL
                   OR LOWER(u.fullName) LIKE :keyword
                   OR LOWER(u.email) LIKE :keyword
                   OR LOWER(u.userCode) LIKE :keyword)
            """)
    Page<User> search(@Param("roleName") String roleName,
                      @Param("keyword") String keyword,
                      Pageable pageable);

    /**
     * User đã có dữ liệu liên quan chưa (đã tạo câu hỏi, đã thi, đã chấm bài)?
     * Nếu có thì KHÔNG cho xoá, tránh mất dữ liệu thi / lỗi khoá ngoại.
     * Dùng native SQL vì cần kiểm tra bảng của module khác mà không phụ thuộc vào entity của module đó.
     */
    @Query(value = """
            SELECT EXISTS (SELECT 1 FROM questions WHERE created_by = :userId)
                OR EXISTS (SELECT 1 FROM exam_attempts WHERE student_id = :userId)
                OR EXISTS (SELECT 1 FROM evaluations WHERE graded_by = :userId)
            """, nativeQuery = true)
    boolean hasRelatedData(@Param("userId") UUID userId);
}
