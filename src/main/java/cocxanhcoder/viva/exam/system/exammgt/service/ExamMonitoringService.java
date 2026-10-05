package cocxanhcoder.viva.exam.system.exammgt.service;

import cocxanhcoder.viva.exam.system.exammgt.dto.response.CandidateScheduleResponse;
import cocxanhcoder.viva.exam.system.exammgt.dto.response.ExamMonitoringResponse;

import java.util.UUID;

public interface ExamMonitoringService {

    ExamMonitoringResponse getMonitoringDashboard(UUID examId);

    CandidateScheduleResponse resetAttempt(UUID examId, UUID attemptId);

    CandidateScheduleResponse markAbsent(UUID examId, UUID attemptId);
}
