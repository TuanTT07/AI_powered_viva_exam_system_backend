package cocxanhcoder.viva.exam.system.exammgt.service;

import cocxanhcoder.viva.exam.system.exammgt.dto.request.AssignCandidatesRequest;
import cocxanhcoder.viva.exam.system.exammgt.dto.request.AutoScheduleRequest;
import cocxanhcoder.viva.exam.system.exammgt.dto.request.RescheduleCandidateRequest;
import cocxanhcoder.viva.exam.system.exammgt.dto.response.CandidateScheduleResponse;
import cocxanhcoder.viva.exam.system.exammgt.dto.response.StudentExamSlotResponse;

import java.util.List;
import java.util.UUID;

public interface ExamScheduleService {

    List<CandidateScheduleResponse> assignCandidates(UUID examId, AssignCandidatesRequest request);

    List<CandidateScheduleResponse> autoScheduleSlots(UUID examId, AutoScheduleRequest request);

    CandidateScheduleResponse rescheduleCandidate(UUID examId, RescheduleCandidateRequest request);

    List<CandidateScheduleResponse> getCandidateSchedules(UUID examId);

    StudentExamSlotResponse getStudentExamSlot(UUID examId, UUID studentId);
}
