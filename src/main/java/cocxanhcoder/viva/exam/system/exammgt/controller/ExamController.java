package cocxanhcoder.viva.exam.system.exammgt.controller;

import cocxanhcoder.viva.exam.system.exammgt.dto.request.CreateExamRequest;
import cocxanhcoder.viva.exam.system.exammgt.dto.request.UpdateExamRequest;
import cocxanhcoder.viva.exam.system.exammgt.dto.request.UpdateExamStatusRequest;
import cocxanhcoder.viva.exam.system.exammgt.dto.response.ExamDetailResponse;
import cocxanhcoder.viva.exam.system.exammgt.dto.response.ExamResponse;
import cocxanhcoder.viva.exam.system.exammgt.entity.ExamStatus;
import cocxanhcoder.viva.exam.system.exammgt.service.ExamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/exams")
@RequiredArgsConstructor
@Tag(name = "Exam Management", description = "APIs Quản lý kỳ thi & cấu hình phiên vấn đáp (Module 2)")
public class ExamController {

    private final ExamService examService;

    @PostMapping
    @Operation(summary = "Tạo kỳ thi mới", description = "Tạo kỳ thi vấn đáp cho môn học và thiết lập cấu hình exam_config")
    public ResponseEntity<ExamResponse> createExam(@Valid @RequestBody CreateExamRequest request) {
        ExamResponse response = examService.createExam(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Danh sách kỳ thi", description = "Lấy danh sách kỳ thi phân trang, tìm kiếm theo môn học hoặc trạng thái")
    public ResponseEntity<Page<ExamResponse>> getExams(
            @RequestParam(required = false) UUID courseId,
            @RequestParam(required = false) ExamStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String direction
    ) {
        Sort sort = direction.equalsIgnoreCase("ASC") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<ExamResponse> result = examService.getExams(courseId, status, pageable);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Chi tiết kỳ thi", description = "Xem chi tiết kỳ thi bao gồm cấu hình và danh sách thí sinh")
    public ResponseEntity<ExamDetailResponse> getExamDetail(@PathVariable UUID id) {
        ExamDetailResponse response = examService.getExamDetail(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật kỳ thi", description = "Cập nhật tên, thời gian và cấu hình kỳ thi")
    public ResponseEntity<ExamResponse> updateExam(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateExamRequest request
    ) {
        ExamResponse response = examService.updateExam(id, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Cập nhật trạng thái kỳ thi", description = "Chuyển trạng thái kỳ thi (DRAFT -> PUBLISHED -> IN_PROGRESS -> COMPLETED)")
    public ResponseEntity<ExamResponse> updateExamStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateExamStatusRequest request
    ) {
        ExamResponse response = examService.updateExamStatus(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa kỳ thi", description = "Xóa kỳ thi khi chưa diễn ra")
    public ResponseEntity<Void> deleteExam(@PathVariable UUID id) {
        examService.deleteExam(id);
        return ResponseEntity.noContent().build();
    }
}
