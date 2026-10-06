package cocxanhcoder.viva.exam.system.exammgt.dto.response;

import cocxanhcoder.viva.exam.system.exammgt.entity.ExamAttemptStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CandidateScheduleResponse(
    UUID attemptId,
    UUID examId,
    UUID studentId,
    String studentCode,
    String studentName,
    String studentEmail,
    Integer slotNumber,
    OffsetDateTime scheduledStartTime,
    OffsetDateTime scheduledEndTime,
    OffsetDateTime actualStartTime,
    OffsetDateTime actualEndTime,
    ExamAttemptStatus status,
    String accessCode
) {}
