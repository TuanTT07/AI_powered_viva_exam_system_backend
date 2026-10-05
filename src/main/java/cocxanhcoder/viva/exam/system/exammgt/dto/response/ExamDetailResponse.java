package cocxanhcoder.viva.exam.system.exammgt.dto.response;

import cocxanhcoder.viva.exam.system.exammgt.dto.request.ExamConfigDto;
import cocxanhcoder.viva.exam.system.exammgt.entity.ExamStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record ExamDetailResponse(
    UUID id,
    UUID courseId,
    String courseCode,
    String courseName,
    String title,
    OffsetDateTime startTime,
    OffsetDateTime endTime,
    ExamStatus status,
    ExamConfigDto examConfig,
    OffsetDateTime createdAt,
    int totalCandidates,
    List<CandidateScheduleResponse> candidates
) {}
