package cocxanhcoder.viva.exam.system.exammgt.dto.response;

import cocxanhcoder.viva.exam.system.exammgt.entity.ExamAttemptStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record StudentExamSummary(
        UUID examId, UUID attemptId, String examTitle, String courseCode, String courseName,
        Integer slotNumber, OffsetDateTime scheduledStartTime, OffsetDateTime scheduledEndTime,
        ExamAttemptStatus status
) {}
