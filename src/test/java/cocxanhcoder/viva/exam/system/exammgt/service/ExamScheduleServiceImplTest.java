package cocxanhcoder.viva.exam.system.exammgt.service;

import cocxanhcoder.viva.exam.system.academic.entity.User;
import cocxanhcoder.viva.exam.system.academic.repository.UserRepository;
import cocxanhcoder.viva.exam.system.common.exception.BusinessException;
import cocxanhcoder.viva.exam.system.exammgt.dto.request.AutoScheduleRequest;
import cocxanhcoder.viva.exam.system.exammgt.dto.response.CandidateScheduleResponse;
import cocxanhcoder.viva.exam.system.exammgt.entity.Exam;
import cocxanhcoder.viva.exam.system.exammgt.entity.ExamAttempt;
import cocxanhcoder.viva.exam.system.exammgt.repository.ExamAttemptRepository;
import cocxanhcoder.viva.exam.system.exammgt.repository.ExamRepository;
import cocxanhcoder.viva.exam.system.exammgt.service.impl.ExamScheduleServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExamScheduleServiceImplTest {

    @Mock
    private ExamRepository examRepository;

    @Mock
    private ExamAttemptRepository examAttemptRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private QuestionSelectorService questionSelectorService;

    @InjectMocks
    private ExamScheduleServiceImpl examScheduleService;

    private UUID examId;
    private Exam exam;
    private User student1;
    private User student2;

    @BeforeEach
    void setUp() {
        examId = UUID.randomUUID();
        exam = new Exam();
        exam.setId(examId);
        exam.setTitle("Thi vấn đáp Java");
        exam.setStartTime(OffsetDateTime.now().plusDays(1));
        exam.setEndTime(exam.getStartTime().plusHours(1));

        student1 = new User();
        student1.setId(UUID.randomUUID());
        student1.setFullName("Nguyen Van A");
        student1.setUserCode("SV001");
        student1.setEmail("a@student.edu.vn");

        student2 = new User();
        student2.setId(UUID.randomUUID());
        student2.setFullName("Tran Thi B");
        student2.setUserCode("SV002");
        student2.setEmail("b@student.edu.vn");
    }

    @Test
    @DisplayName("Tự động chia slot thi thành công khi khung giờ đủ lớn")
    void autoScheduleSlots_Success() {
        List<UUID> studentIds = List.of(student1.getId(), student2.getId());
        AutoScheduleRequest request = new AutoScheduleRequest(studentIds, 15, 2);

        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        when(userRepository.findAllById(studentIds)).thenReturn(List.of(student1, student2));
        when(examAttemptRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<CandidateScheduleResponse> responses = examScheduleService.autoScheduleSlots(examId, request);

        assertNotNull(responses);
        assertEquals(2, responses.size());
        assertEquals(1, responses.get(0).slotNumber());
        assertEquals(2, responses.get(1).slotNumber());
        verify(examAttemptRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("Ném ngoại lệ BusinessException khi thời gian kỳ thi quá ngắn không đủ xếp slot")
    void autoScheduleSlots_NotEnoughTime_ThrowsException() {
        exam.setEndTime(exam.getStartTime().plusMinutes(10)); // Chỉ 10 phút không đủ 2 SV thi
        List<UUID> studentIds = List.of(student1.getId(), student2.getId());
        AutoScheduleRequest request = new AutoScheduleRequest(studentIds, 15, 2);

        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        when(userRepository.findAllById(studentIds)).thenReturn(List.of(student1, student2));

        assertThrows(BusinessException.class, () -> examScheduleService.autoScheduleSlots(examId, request));
    }
}
