package cocxanhcoder.viva.exam.system.exammgt.service;

import cocxanhcoder.viva.exam.system.exammgt.dto.request.CreateExamRequest;
import cocxanhcoder.viva.exam.system.exammgt.dto.request.UpdateExamRequest;
import cocxanhcoder.viva.exam.system.exammgt.dto.request.UpdateExamStatusRequest;
import cocxanhcoder.viva.exam.system.exammgt.dto.response.ExamDetailResponse;
import cocxanhcoder.viva.exam.system.exammgt.dto.response.ExamResponse;
import cocxanhcoder.viva.exam.system.exammgt.entity.ExamStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ExamService {

    ExamResponse createExam(CreateExamRequest request);

    ExamResponse updateExam(UUID id, UpdateExamRequest request);

    ExamResponse updateExamStatus(UUID id, UpdateExamStatusRequest request);

    ExamDetailResponse getExamDetail(UUID id);

    Page<ExamResponse> getExams(UUID courseId, ExamStatus status, Pageable pageable);

    void deleteExam(UUID id);
}
