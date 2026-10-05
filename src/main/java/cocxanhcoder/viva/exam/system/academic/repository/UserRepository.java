package cocxanhcoder.viva.exam.system.academic.repository;

import cocxanhcoder.viva.exam.system.academic.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByUserCode(String userCode);
}
