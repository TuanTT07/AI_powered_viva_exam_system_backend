package cocxanhcoder.viva.exam.system.exammgt.service.impl;

import cocxanhcoder.viva.exam.system.academic.entity.User;
import cocxanhcoder.viva.exam.system.academic.repository.UserRepository;
import cocxanhcoder.viva.exam.system.common.exception.BusinessException;
import cocxanhcoder.viva.exam.system.common.exception.ResourceNotFoundException;
import cocxanhcoder.viva.exam.system.exammgt.dto.request.AssignCandidatesRequest;
import cocxanhcoder.viva.exam.system.exammgt.dto.request.AutoScheduleRequest;
import cocxanhcoder.viva.exam.system.exammgt.dto.request.RescheduleCandidateRequest;
import cocxanhcoder.viva.exam.system.exammgt.dto.response.CandidateScheduleResponse;
import cocxanhcoder.viva.exam.system.exammgt.dto.response.QuestionAttemptResponse;
import cocxanhcoder.viva.exam.system.exammgt.dto.response.StudentExamSlotResponse;
import cocxanhcoder.viva.exam.system.exammgt.entity.Exam;
import cocxanhcoder.viva.exam.system.exammgt.entity.ExamAttempt;
import cocxanhcoder.viva.exam.system.exammgt.entity.ExamAttemptStatus;
import cocxanhcoder.viva.exam.system.exammgt.entity.QuestionAttempt;
import cocxanhcoder.viva.exam.system.exammgt.mapper.ExamAttemptMapper;
import cocxanhcoder.viva.exam.system.exammgt.repository.ExamAttemptRepository;
import cocxanhcoder.viva.exam.system.exammgt.repository.ExamRepository;
import cocxanhcoder.viva.exam.system.exammgt.service.ExamScheduleService;
import cocxanhcoder.viva.exam.system.exammgt.service.QuestionSelectorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ExamScheduleServiceImpl implements ExamScheduleService {

    private final ExamRepository examRepository;
    private final ExamAttemptRepository examAttemptRepository;
    private final UserRepository userRepository;
    private final QuestionSelectorService questionSelectorService;

    @Override
    public List<CandidateScheduleResponse> assignCandidates(UUID examId, AssignCandidatesRequest request) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy kỳ thi với ID: " + examId));

        List<User> students = userRepository.findAllById(request.studentIds());
        List<ExamAttempt> newAttempts = new ArrayList<>();

        int slotNumber = 1;
        for (User student : students) {
            boolean exists = examAttemptRepository.findByExamIdAndStudentId(examId, student.getId()).isPresent();
            if (!exists) {
                ExamAttempt attempt = new ExamAttempt();
                attempt.setExam(exam);
                attempt.setStudent(student);
                attempt.setSlotNumber(slotNumber++);
                attempt.setStatus(ExamAttemptStatus.SCHEDULED);
                newAttempts.add(attempt);
            }
        }

        List<ExamAttempt> saved = examAttemptRepository.saveAll(newAttempts);
        return saved.stream().map(ExamAttemptMapper::toScheduleResponse).toList();
    }

    @Override
    public List<CandidateScheduleResponse> autoScheduleSlots(UUID examId, AutoScheduleRequest request) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy kỳ thi với ID: " + examId));

        List<User> students = userRepository.findAllById(request.studentIds());
        if (students.isEmpty()) {
            throw new BusinessException("Danh sách sinh viên trống hoặc không tìm thấy trong hệ thống!");
        }

        OffsetDateTime currentSlotStart = exam.getStartTime();
        int slotDuration = request.slotDurationMinutes();
        int breakDuration = request.breakDurationMinutes() != null ? request.breakDurationMinutes() : 2;
        int currentSlotNumber = 1;

        List<ExamAttempt> attemptsToSave = new ArrayList<>();

        for (User student : students) {
            OffsetDateTime slotEnd = currentSlotStart.plusMinutes(slotDuration);

            if (slotEnd.isAfter(exam.getEndTime())) {
                throw new BusinessException("Khung thời gian kỳ thi không đủ để xếp ca thi cho sinh viên " + student.getFullName());
            }

            ExamAttempt attempt = examAttemptRepository.findByExamIdAndStudentId(examId, student.getId())
                    .orElseGet(() -> {
                        ExamAttempt ea = new ExamAttempt();
                        ea.setExam(exam);
                        ea.setStudent(student);
                        return ea;
                    });

            attempt.setScheduledStartTime(currentSlotStart);
            attempt.setScheduledEndTime(slotEnd);
            attempt.setSlotNumber(currentSlotNumber++);
            attempt.setStatus(ExamAttemptStatus.SCHEDULED);

            // Tự động sinh câu hỏi thi cho ca này
            if (attempt.getQuestionAttempts().isEmpty()) {
                List<QuestionAttempt> generatedQuestions = questionSelectorService.generateQuestionsForAttempt(attempt);
                generatedQuestions.forEach(attempt::addQuestionAttempt);
            }

            attemptsToSave.add(attempt);

            currentSlotStart = slotEnd.plusMinutes(breakDuration);
        }

        List<ExamAttempt> saved = examAttemptRepository.saveAll(attemptsToSave);
        return saved.stream().map(ExamAttemptMapper::toScheduleResponse).toList();
    }

    @Override
    public CandidateScheduleResponse rescheduleCandidate(UUID examId, RescheduleCandidateRequest request) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy kỳ thi với ID: " + examId));

        if (request.newScheduledStartTime().isBefore(exam.getStartTime()) || request.newScheduledEndTime().isAfter(exam.getEndTime())) {
            throw new BusinessException("Thời gian ca thi mới phải nằm trong khung thời gian của kỳ thi!");
        }

        ExamAttempt attempt = examAttemptRepository.findByExamIdAndStudentId(examId, request.studentId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ca thi của sinh viên!"));

        attempt.setScheduledStartTime(request.newScheduledStartTime());
        attempt.setScheduledEndTime(request.newScheduledEndTime());

        ExamAttempt updated = examAttemptRepository.save(attempt);
        return ExamAttemptMapper.toScheduleResponse(updated);
    }


    @Override
    @Transactional(readOnly = true)
    public List<CandidateScheduleResponse> getCandidateSchedules(UUID examId) {
        return examAttemptRepository.findByExamIdOrderByScheduledStartTimeAsc(examId)
                .stream()
                .map(ExamAttemptMapper::toScheduleResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public StudentExamSlotResponse getStudentExamSlot(UUID examId, UUID studentId) {
        ExamAttempt attempt = examAttemptRepository.findByExamIdAndStudentId(examId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ca thi của sinh viên trong kỳ thi này!"));

        List<QuestionAttemptResponse> questionResponses = attempt.getQuestionAttempts().stream()
                .map(ExamAttemptMapper::toQuestionAttemptResponse)
                .toList();

        return ExamAttemptMapper.toStudentSlotResponse(attempt, questionResponses);
    }
}
