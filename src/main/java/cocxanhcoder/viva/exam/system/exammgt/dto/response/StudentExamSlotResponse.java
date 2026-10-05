package cocxanhcoder.viva.exam.system.exammgt.dto.response;

import cocxanhcoder.viva.exam.system.exammgt.dto.request.ExamConfigDto;
import cocxanhcoder.viva.exam.system.exammgt.entity.ExamAttemptStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record StudentExamSlotResponse(
    UUID attemptId,
    UUID examId,
    String examTitle,
    String courseCode,
    String courseName,
    Integer slotNumber,
    OffsetDateTime scheduledStartTime,
    OffsetDateTime scheduledEndTime,
    OffsetDateTime actualStartTime,
    OffsetDateTime actualEndTime,
    ExamAttemptStatus status,
    String accessCode,
    ExamConfigDto examConfig,
    List<QuestionAttemptResponse> questions
) {}
