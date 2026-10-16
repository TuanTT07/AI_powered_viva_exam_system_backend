package cocxanhcoder.viva.exam.system.questionbank.service;

import cocxanhcoder.viva.exam.system.academic.entity.Role;
import cocxanhcoder.viva.exam.system.academic.entity.User;
import cocxanhcoder.viva.exam.system.academic.repository.CourseRepository;
import cocxanhcoder.viva.exam.system.academic.repository.UserRepository;
import cocxanhcoder.viva.exam.system.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseAccessServiceTest {
    @Mock UserRepository users;
    @Mock CourseRepository courses;
    @InjectMocks CourseAccessService access;

    @Test
    void adminCanManageAnyCourse() {
        UUID adminId = UUID.randomUUID(); User admin = user(Role.ADMIN);
        when(users.findWithRoleById(adminId)).thenReturn(Optional.of(admin));
        assertSame(admin, access.requireManager(adminId, UUID.randomUUID()));
    }

    @Test
    void unassignedLecturerIsForbidden() {
        UUID lecturerId = UUID.randomUUID(); UUID courseId = UUID.randomUUID();
        when(users.findWithRoleById(lecturerId)).thenReturn(Optional.of(user(Role.LECTURER)));
        when(courses.existsByLecturers_IdAndId(lecturerId, courseId)).thenReturn(false);
        assertThrows(BusinessException.class, () -> access.requireManager(lecturerId, courseId));
    }

    private User user(String roleName) { Role role = new Role(); role.setRoleName(roleName); User user = new User(); user.setRole(role); return user; }
}
