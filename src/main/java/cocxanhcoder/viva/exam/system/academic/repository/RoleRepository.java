package cocxanhcoder.viva.exam.system.academic.repository;

import cocxanhcoder.viva.exam.system.academic.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RoleRepository extends JpaRepository<Role, UUID> {

    Optional<Role> findByRoleName(String roleName);
}
