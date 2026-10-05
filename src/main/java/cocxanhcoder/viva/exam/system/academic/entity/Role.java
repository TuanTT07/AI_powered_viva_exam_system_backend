package cocxanhcoder.viva.exam.system.academic.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
public class Role {

    // Tên các role mặc định (seed trong V2__seed_roles.sql), dùng thay cho chuỗi "gõ tay" rải rác trong code
    public static final String ADMIN = "ADMIN";
    public static final String LECTURER = "LECTURER";
    public static final String STUDENT = "STUDENT";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "role_name", nullable = false, unique = true, length = 50)
    private String roleName;

    @Column(columnDefinition = "TEXT")
    private String description;
}
