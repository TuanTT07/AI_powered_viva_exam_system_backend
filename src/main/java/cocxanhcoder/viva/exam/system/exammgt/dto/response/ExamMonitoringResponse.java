package cocxanhcoder.viva.exam.system.exammgt.dto.response;

import java.util.List;
import java.util.UUID;

public record ExamMonitoringResponse(
    UUID examId,
    String examTitle,
    long totalCandidates,
    long scheduledCount,
    long readyCount,
    long inProgressCount,
    long completedCount,
    long absentCount,
    long cancelledCount,
    List<CandidateScheduleResponse> candidateStatuses
) {}
