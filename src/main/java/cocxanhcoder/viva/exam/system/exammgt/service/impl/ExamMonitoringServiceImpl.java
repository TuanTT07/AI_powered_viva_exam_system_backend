package cocxanhcoder.viva.exam.system.exammgt.service.impl;

import cocxanhcoder.viva.exam.system.common.exception.ResourceNotFoundException;
import cocxanhcoder.viva.exam.system.exammgt.dto.response.CandidateScheduleResponse;
import cocxanhcoder.viva.exam.system.exammgt.dto.response.ExamMonitoringResponse;
import cocxanhcoder.viva.exam.system.exammgt.entity.Exam;
import cocxanhcoder.viva.exam.system.exammgt.entity.ExamAttempt;
import cocxanhcoder.viva.exam.system.exammgt.entity.ExamAttemptStatus;
import cocxanhcoder.viva.exam.system.exammgt.entity.QuestionAttemptStatus;
import cocxanhcoder.viva.exam.system.exammgt.mapper.ExamAttemptMapper;
import cocxanhcoder.viva.exam.system.exammgt.repository.ExamAttemptRepository;
import cocxanhcoder.viva.exam.system.exammgt.repository.ExamRepository;
import cocxanhcoder.viva.exam.system.exammgt.service.ExamMonitoringService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ExamMonitoringServiceImpl implements ExamMonitoringService {

    private final ExamRepository examRepository;
    private final ExamAttemptRepository examAttemptRepository;

    @Override
    @Transactional(readOnly = true)
    public ExamMonitoringResponse getMonitoringDashboard(UUID examId) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy kỳ thi với ID: " + examId));

        List<ExamAttempt> attempts = examAttemptRepository.findByExamIdOrderBySlotNumberAsc(examId);

        long scheduled = attempts.stream().filter(a -> a.getStatus() == ExamAttemptStatus.SCHEDULED).count();
        long ready = attempts.stream().filter(a -> a.getStatus() == ExamAttemptStatus.READY).count();
        long inProgress = attempts.stream().filter(a -> a.getStatus() == ExamAttemptStatus.IN_PROGRESS).count();
        long completed = attempts.stream().filter(a -> a.getStatus() == ExamAttemptStatus.COMPLETED).count();
        long absent = attempts.stream().filter(a -> a.getStatus() == ExamAttemptStatus.ABSENT).count();
        long cancelled = attempts.stream().filter(a -> a.getStatus() == ExamAttemptStatus.CANCELLED).count();

        List<CandidateScheduleResponse> candidateResponses = attempts.stream()
                .map(ExamAttemptMapper::toScheduleResponse)
                .toList();

        return new ExamMonitoringResponse(
            exam.getId(),
            exam.getTitle(),
            attempts.size(),
            scheduled,
            ready,
            inProgress,
            completed,
            absent,
            cancelled,
            candidateResponses
        );
    }

    @Override
    public CandidateScheduleResponse resetAttempt(UUID examId, UUID attemptId) {
        ExamAttempt attempt = examAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ca thi với ID: " + attemptId));

        attempt.setStatus(ExamAttemptStatus.READY);
        attempt.setActualStartTime(null);
        attempt.setActualEndTime(null);

        attempt.getQuestionAttempts().forEach(qa -> {
            qa.setStatus(QuestionAttemptStatus.PENDING);
            qa.setStartTime(null);
            qa.setEndTime(null);
        });

        ExamAttempt saved = examAttemptRepository.save(attempt);
        return ExamAttemptMapper.toScheduleResponse(saved);
    }

    @Override
    public CandidateScheduleResponse markAbsent(UUID examId, UUID attemptId) {
        ExamAttempt attempt = examAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ca thi với ID: " + attemptId));

        attempt.setStatus(ExamAttemptStatus.ABSENT);
        ExamAttempt saved = examAttemptRepository.save(attempt);
        return ExamAttemptMapper.toScheduleResponse(saved);
    }
}
