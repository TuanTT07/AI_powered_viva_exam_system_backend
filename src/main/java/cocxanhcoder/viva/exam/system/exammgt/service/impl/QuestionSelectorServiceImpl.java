package cocxanhcoder.viva.exam.system.exammgt.service.impl;

import cocxanhcoder.viva.exam.system.common.exception.BusinessException;
import cocxanhcoder.viva.exam.system.exammgt.dto.request.ExamConfigDto;
import cocxanhcoder.viva.exam.system.exammgt.entity.Exam;
import cocxanhcoder.viva.exam.system.exammgt.entity.ExamAttempt;
import cocxanhcoder.viva.exam.system.exammgt.entity.QuestionAttempt;
import cocxanhcoder.viva.exam.system.exammgt.entity.QuestionAttemptStatus;
import cocxanhcoder.viva.exam.system.exammgt.mapper.ExamMapper;
import cocxanhcoder.viva.exam.system.exammgt.repository.ExamAttemptRepository;
import cocxanhcoder.viva.exam.system.exammgt.service.QuestionSelectorService;
import cocxanhcoder.viva.exam.system.questionbank.entity.Question;
import cocxanhcoder.viva.exam.system.questionbank.entity.QuestionStatus;
import cocxanhcoder.viva.exam.system.questionbank.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class QuestionSelectorServiceImpl implements QuestionSelectorService {

    private final QuestionRepository questionRepository;
    private final ExamAttemptRepository examAttemptRepository;

    @Override
    public List<QuestionAttempt> generateQuestionsForAttempt(ExamAttempt attempt) {
        Exam exam = attempt.getExam();
        ExamConfigDto config = ExamMapper.parseConfig(exam.getExamConfig());

        // 1. Lấy tất cả các câu hỏi APPROVED thuộc môn học
        List<Question> approvedQuestions = questionRepository
                .findByCourseIdAndStatus(exam.getCourse().getId(), QuestionStatus.APPROVED);

        if (approvedQuestions.isEmpty()) {
            throw new BusinessException("Ngân hàng câu hỏi của môn học chưa có câu hỏi nào được duyệt (APPROVED)!");
        }

        // 2. Anti-Overlap Checking (Chống trùng lặp câu hỏi với 2 ca thi liền trước)
        List<UUID> excludeQuestionIds = Collections.emptyList();
        if (Boolean.TRUE.equals(config.antiOverlapEnabled()) && attempt.getSlotNumber() != null && attempt.getSlotNumber() > 1) {
            int fromSlot = Math.max(1, attempt.getSlotNumber() - 2);
            int toSlot = attempt.getSlotNumber() - 1;
            excludeQuestionIds = examAttemptRepository.findRecentUsedQuestionIds(exam.getId(), fromSlot, toSlot);
        }

        final List<UUID> finalExcludeIds = excludeQuestionIds;
        List<Question> availableCandidates = approvedQuestions.stream()
                .filter(q -> !finalExcludeIds.contains(q.getId()))
                .collect(Collectors.toList());

        // Fallback nếu số câu hỏi khả thi sau lọc ít hơn số câu cần chọn
        if (availableCandidates.size() < config.maxMainQuestions()) {
            availableCandidates = new ArrayList<>(approvedQuestions);
        }

        // 3. Xáo trộn ngẫu nhiên & Chọn đủ số câu hỏi chính theo cấu hình
        Collections.shuffle(availableCandidates);
        List<Question> selectedQuestions = availableCandidates.stream()
                .limit(config.maxMainQuestions())
                .toList();

        // 4. Khởi tạo danh sách QuestionAttempt
        int order = 1;
        List<QuestionAttempt> result = new ArrayList<>();
        for (Question q : selectedQuestions) {
            QuestionAttempt qa = new QuestionAttempt();
            qa.setExamAttempt(attempt);
            qa.setQuestion(q);
            qa.setQuestionOrder(order++);
            qa.setStatus(QuestionAttemptStatus.PENDING);
            result.add(qa);
        }

        return result;
    }
}
