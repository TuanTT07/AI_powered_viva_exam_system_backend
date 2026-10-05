package cocxanhcoder.viva.exam.system.exammgt.service.impl;

import cocxanhcoder.viva.exam.system.academic.entity.Course;
import cocxanhcoder.viva.exam.system.academic.repository.CourseRepository;
import cocxanhcoder.viva.exam.system.common.exception.BusinessException;
import cocxanhcoder.viva.exam.system.common.exception.ResourceNotFoundException;
import cocxanhcoder.viva.exam.system.exammgt.dto.request.CreateExamRequest;
import cocxanhcoder.viva.exam.system.exammgt.dto.request.UpdateExamRequest;
import cocxanhcoder.viva.exam.system.exammgt.dto.request.UpdateExamStatusRequest;
import cocxanhcoder.viva.exam.system.exammgt.dto.response.CandidateScheduleResponse;
import cocxanhcoder.viva.exam.system.exammgt.dto.response.ExamDetailResponse;
import cocxanhcoder.viva.exam.system.exammgt.dto.response.ExamResponse;
import cocxanhcoder.viva.exam.system.exammgt.entity.Exam;
import cocxanhcoder.viva.exam.system.exammgt.entity.ExamAttempt;
import cocxanhcoder.viva.exam.system.exammgt.entity.ExamStatus;
import cocxanhcoder.viva.exam.system.exammgt.mapper.ExamAttemptMapper;
import cocxanhcoder.viva.exam.system.exammgt.mapper.ExamMapper;
import cocxanhcoder.viva.exam.system.exammgt.repository.ExamAttemptRepository;
import cocxanhcoder.viva.exam.system.exammgt.repository.ExamRepository;
import cocxanhcoder.viva.exam.system.exammgt.service.ExamService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ExamServiceImpl implements ExamService {

    private final ExamRepository examRepository;
    private final CourseRepository courseRepository;
    private final ExamAttemptRepository examAttemptRepository;

    @Override
    public ExamResponse createExam(CreateExamRequest request) {
        if (request.startTime().isAfter(request.endTime())) {
            throw new BusinessException("Thời gian bắt đầu kỳ thi không thể sau thời gian kết thúc!");
        }

        Course course = courseRepository.findById(request.courseId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy môn học với ID: " + request.courseId()));

        Exam exam = new Exam();
        exam.setCourse(course);
        exam.setTitle(request.title());
        exam.setStartTime(request.startTime());
        exam.setEndTime(request.endTime());
        exam.setStatus(ExamStatus.DRAFT);
        exam.setExamConfig(ExamMapper.toConfigMap(request.examConfig()));

        Exam saved = examRepository.save(exam);
        return ExamMapper.toResponse(saved);
    }

    @Override
    public ExamResponse updateExam(UUID id, UpdateExamRequest request) {
        Exam exam = examRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy kỳ thi với ID: " + id));

        if (exam.getStatus() == ExamStatus.COMPLETED || exam.getStatus() == ExamStatus.CANCELLED) {
            throw new BusinessException("Không thể chỉnh sửa kỳ thi đã hoàn thành hoặc đã bị hủy!");
        }

        if (request.startTime().isAfter(request.endTime())) {
            throw new BusinessException("Thời gian bắt đầu kỳ thi không thể sau thời gian kết thúc!");
        }

        exam.setTitle(request.title());
        exam.setStartTime(request.startTime());
        exam.setEndTime(request.endTime());
        if (request.examConfig() != null) {
            exam.setExamConfig(ExamMapper.toConfigMap(request.examConfig()));
        }

        Exam updated = examRepository.save(exam);
        return ExamMapper.toResponse(updated);
    }

    @Override
    public ExamResponse updateExamStatus(UUID id, UpdateExamStatusRequest request) {
        Exam exam = examRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy kỳ thi với ID: " + id));

        exam.setStatus(request.status());
        Exam updated = examRepository.save(exam);
        return ExamMapper.toResponse(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public ExamDetailResponse getExamDetail(UUID id) {
        Exam exam = examRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy kỳ thi với ID: " + id));

        List<ExamAttempt> attempts = examAttemptRepository.findByExamIdOrderBySlotNumberAsc(id);
        List<CandidateScheduleResponse> candidateResponses = attempts.stream()
                .map(ExamAttemptMapper::toScheduleResponse)
                .toList();

        return ExamMapper.toDetailResponse(exam, candidateResponses);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ExamResponse> getExams(UUID courseId, ExamStatus status, Pageable pageable) {
        return examRepository.searchExams(courseId, status, pageable)
                .map(ExamMapper::toResponse);
    }

    @Override
    public void deleteExam(UUID id) {
        Exam exam = examRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy kỳ thi với ID: " + id));

        if (exam.getStatus() == ExamStatus.IN_PROGRESS) {
            throw new BusinessException("Không thể xóa kỳ thi đang diễn ra!");
        }

        examRepository.delete(exam);
    }
}
