package cocxanhcoder.viva.exam.system.exammgt.dto.response;

import cocxanhcoder.viva.exam.system.exammgt.dto.request.ExamConfigDto;
import cocxanhcoder.viva.exam.system.exammgt.entity.ExamStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ExamResponse(
    UUID id,
    UUID courseId,
    String courseCode,
    String courseName,
    String title,
    OffsetDateTime startTime,
    OffsetDateTime endTime,
    ExamStatus status,
    ExamConfigDto examConfig,
    OffsetDateTime createdAt
) {}
