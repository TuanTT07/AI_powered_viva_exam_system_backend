package cocxanhcoder.viva.exam.system.exammgt.controller;

import cocxanhcoder.viva.exam.system.common.dto.PageResponse;
import cocxanhcoder.viva.exam.system.common.util.PageableUtils;
import cocxanhcoder.viva.exam.system.exammgt.dto.response.StudentExamSlotResponse;
import cocxanhcoder.viva.exam.system.exammgt.dto.response.StudentExamSummary;
import cocxanhcoder.viva.exam.system.exammgt.entity.ExamAttemptStatus;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import cocxanhcoder.viva.exam.system.exammgt.service.ExamScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/student/exams")
@RequiredArgsConstructor
@Tag(name = "Student Exam Access", description = "APIs dành cho Sinh viên tra cứu lịch thi & bài thi của cá nhân (Module 2)")
public class StudentExamController {

    private final ExamScheduleService examScheduleService;

    @GetMapping
    @Operation(summary = "Danh sách lịch thi cá nhân theo tài khoản đang đăng nhập")
    public PageResponse<StudentExamSummary> getMyExams(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) ExamAttemptStatus status
    ) {
        return PageResponse.from(examScheduleService.getStudentExams(UUID.fromString(jwt.getSubject()), status,
                PageableUtils.of(page, size, Sort.unsorted())));
    }

    @GetMapping("/{examId}/my-slot")
    @Operation(summary = "Sinh viên tra cứu ca thi cá nhân", description = "Lấy thông tin slot thi, thời gian, câu hỏi được cấp phát cho sinh viên")
    public ResponseEntity<StudentExamSlotResponse> getMySlot(
            @PathVariable UUID examId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        StudentExamSlotResponse response = examScheduleService.getStudentExamSlot(examId, UUID.fromString(jwt.getSubject()));
        return ResponseEntity.ok(response);
    }
}
