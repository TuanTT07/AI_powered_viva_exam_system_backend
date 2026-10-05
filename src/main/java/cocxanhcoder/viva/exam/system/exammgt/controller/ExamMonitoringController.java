package cocxanhcoder.viva.exam.system.exammgt.controller;

import cocxanhcoder.viva.exam.system.exammgt.dto.response.CandidateScheduleResponse;
import cocxanhcoder.viva.exam.system.exammgt.dto.response.ExamMonitoringResponse;
import cocxanhcoder.viva.exam.system.exammgt.service.ExamMonitoringService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/exams/{examId}")
@RequiredArgsConstructor
@Tag(name = "Exam Monitoring", description = "APIs Dashboard Giám sát phiên thi thời gian thực & Xử lý sự cố (Module 2)")
public class ExamMonitoringController {

    private final ExamMonitoringService examMonitoringService;

    @GetMapping("/monitor")
    @Operation(summary = "Dashboard Giám sát ca thi", description = "Lấy thống kê thời gian thực trạng thái các ca thi (SCHEDULED, READY, IN_PROGRESS, COMPLETED, ABSENT)")
    public ResponseEntity<ExamMonitoringResponse> getMonitoringDashboard(@PathVariable UUID examId) {
        ExamMonitoringResponse response = examMonitoringService.getMonitoringDashboard(examId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/attempts/{attemptId}/reset")
    @Operation(summary = "Reset ca thi khi có sự cố", description = "Cho phép Giảng viên/Giám thị reset lượt thi của thí sinh về trạng thái READY để thi lại khi đứt kết nối mạng/lỗi thiết bị")
    public ResponseEntity<CandidateScheduleResponse> resetAttempt(
            @PathVariable UUID examId,
            @PathVariable UUID attemptId
    ) {
        CandidateScheduleResponse response = examMonitoringService.resetAttempt(examId, attemptId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/attempts/{attemptId}/absent")
    @Operation(summary = "Ghi nhận vắng thi", description = "Đánh dấu sinh viên vắng mặt trong ca thi")
    public ResponseEntity<CandidateScheduleResponse> markAbsent(
            @PathVariable UUID examId,
            @PathVariable UUID attemptId
    ) {
        CandidateScheduleResponse response = examMonitoringService.markAbsent(examId, attemptId);
        return ResponseEntity.ok(response);
    }
}
