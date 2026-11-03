package cocxanhcoder.viva.exam.system.academic.dto;

import java.util.UUID;

public record CourseStudentResponse(UUID id, String userCode, String fullName, String email) {}
