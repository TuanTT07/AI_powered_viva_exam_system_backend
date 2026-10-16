package cocxanhcoder.viva.exam.system.questionbank.service;

import cocxanhcoder.viva.exam.system.academic.entity.Role;
import cocxanhcoder.viva.exam.system.academic.entity.User;
import cocxanhcoder.viva.exam.system.academic.repository.CourseRepository;
import cocxanhcoder.viva.exam.system.academic.repository.UserRepository;
import cocxanhcoder.viva.exam.system.common.exception.BusinessException;
import cocxanhcoder.viva.exam.system.common.exception.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.UUID;

/** Centralizes the ownership rule for all Question Bank resources. */
@Service
public class CourseAccessService {
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;

    public CourseAccessService(UserRepository userRepository, CourseRepository courseRepository) {
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
    }

    public User requireManager(UUID actorId, UUID courseId) {
        User actor = userRepository.findWithRoleById(actorId)
                .orElseThrow(() -> new ResourceNotFoundException("User", actorId));
        if (Role.ADMIN.equals(actor.getRole().getRoleName())) return actor;
        if (!Role.LECTURER.equals(actor.getRole().getRoleName())
                || !courseRepository.existsByLecturers_IdAndId(actorId, courseId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN,
                    "Bạn không được phân công quản lý ngân hàng câu hỏi của môn này");
        }
        return actor;
    }
}
