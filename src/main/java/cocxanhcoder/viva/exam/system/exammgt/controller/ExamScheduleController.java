package cocxanhcoder.viva.exam.system.exammgt.controller;

import cocxanhcoder.viva.exam.system.exammgt.dto.request.AssignCandidatesRequest;
import cocxanhcoder.viva.exam.system.exammgt.dto.request.AutoScheduleRequest;
import cocxanhcoder.viva.exam.system.exammgt.dto.request.RescheduleCandidateRequest;
import cocxanhcoder.viva.exam.system.exammgt.dto.response.CandidateScheduleResponse;
import cocxanhcoder.viva.exam.system.exammgt.service.ExamScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/exams/{examId}")
@RequiredArgsConstructor
@Tag(name = "Exam Scheduling", description = "APIs Quản lý danh sách thí sinh & xếp lịch ca thi (Module 2)")
public class ExamScheduleController {

    private final ExamScheduleService examScheduleService;

    @PostMapping("/candidates")
    @Operation(summary = "Gán danh sách thí sinh", description = "Thêm danh sách sinh viên vào kỳ thi")
    public ResponseEntity<List<CandidateScheduleResponse>> assignCandidates(
            @PathVariable UUID examId,
            @Valid @RequestBody AssignCandidatesRequest request
    ) {
        List<CandidateScheduleResponse> response = examScheduleService.assignCandidates(examId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/schedule/auto")
    @Operation(summary = "Tự động xếp ca thi", description = "Tự động tính toán khung giờ ca thi (Slots) và cấp phát bộ câu hỏi ngẫu nhiên/chống trùng cho từng sinh viên")
    public ResponseEntity<List<CandidateScheduleResponse>> autoScheduleSlots(
            @PathVariable UUID examId,
            @Valid @RequestBody AutoScheduleRequest request
    ) {
        List<CandidateScheduleResponse> response = examScheduleService.autoScheduleSlots(examId, request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/schedule/reschedule")
    @Operation(summary = "Đổi ca thi thủ công", description = "Điều chỉnh slot/khung giờ ca thi cho sinh viên khi bị bận hoặc xin đổi ca")
    public ResponseEntity<CandidateScheduleResponse> rescheduleCandidate(
            @PathVariable UUID examId,
            @Valid @RequestBody RescheduleCandidateRequest request
    ) {
        CandidateScheduleResponse response = examScheduleService.rescheduleCandidate(examId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/schedule")
    @Operation(summary = "Lấy danh sách lịch thi", description = "Lấy toàn bộ lịch thi cá nhân của tất cả sinh viên trong kỳ thi")
    public ResponseEntity<List<CandidateScheduleResponse>> getCandidateSchedules(@PathVariable UUID examId) {
        List<CandidateScheduleResponse> response = examScheduleService.getCandidateSchedules(examId);
        return ResponseEntity.ok(response);
    }
}
