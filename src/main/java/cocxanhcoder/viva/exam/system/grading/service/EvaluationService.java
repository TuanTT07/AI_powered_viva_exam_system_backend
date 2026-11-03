package cocxanhcoder.viva.exam.system.grading.service;

import cocxanhcoder.viva.exam.system.academic.entity.User;
import cocxanhcoder.viva.exam.system.common.dto.PageResponse;
import cocxanhcoder.viva.exam.system.common.exception.BusinessException;
import cocxanhcoder.viva.exam.system.common.exception.ResourceNotFoundException;
import cocxanhcoder.viva.exam.system.exammgt.entity.QuestionAttempt;
import cocxanhcoder.viva.exam.system.exammgt.repository.QuestionAttemptRepository;
import cocxanhcoder.viva.exam.system.grading.dto.CriterionFinalScoreRequest;
import cocxanhcoder.viva.exam.system.grading.dto.EvaluationResponse;
import cocxanhcoder.viva.exam.system.grading.dto.FinalizeEvaluationRequest;
import cocxanhcoder.viva.exam.system.grading.entity.CriterionScore;
import cocxanhcoder.viva.exam.system.grading.entity.Evaluation;
import cocxanhcoder.viva.exam.system.grading.entity.EvaluationStatus;
import cocxanhcoder.viva.exam.system.grading.mapper.EvaluationMapper;
import cocxanhcoder.viva.exam.system.grading.repository.EvaluationRepository;
import cocxanhcoder.viva.exam.system.interview.entity.Speaker;
import cocxanhcoder.viva.exam.system.interview.entity.TranscriptTurn;
import cocxanhcoder.viva.exam.system.interview.repository.TranscriptTurnRepository;
import cocxanhcoder.viva.exam.system.questionbank.entity.Rubric;
import cocxanhcoder.viva.exam.system.questionbank.entity.RubricCriterion;
import cocxanhcoder.viva.exam.system.questionbank.service.AiClient;
import cocxanhcoder.viva.exam.system.questionbank.service.CourseAccessService;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/** Chấm điểm AI (CN4): chấm transcript theo rubric, giảng viên xem/điều chỉnh/chốt điểm cuối. */
@Service
@Transactional(readOnly = true)
public class EvaluationService {

    private final EvaluationRepository evaluations;
    private final QuestionAttemptRepository questionAttempts;
    private final TranscriptTurnRepository transcriptTurns;
    private final AiClient ai;
    private final CourseAccessService courseAccess;
    private final EvaluationMapper mapper;

    public EvaluationService(EvaluationRepository evaluations, QuestionAttemptRepository questionAttempts,
                              TranscriptTurnRepository transcriptTurns, AiClient ai,
                              CourseAccessService courseAccess, EvaluationMapper mapper) {
        this.evaluations = evaluations;
        this.questionAttempts = questionAttempts;
        this.transcriptTurns = transcriptTurns;
        this.ai = ai;
        this.courseAccess = courseAccess;
        this.mapper = mapper;
    }

    /** Giảng viên/Admin yêu cầu AI chấm điểm 1 câu trả lời dựa trên transcript + rubric của câu hỏi. */
    @Transactional
    public EvaluationResponse gradeWithAi(UUID actorId, UUID questionAttemptId) {
        QuestionAttempt attempt = findAttempt(questionAttemptId);
        courseAccess.requireManager(actorId, courseIdOf(attempt));

        Evaluation existing = evaluations.findByQuestionAttemptId(questionAttemptId).orElse(null);
        if (existing != null && existing.getStatus() == EvaluationStatus.GRADED)
            throw new BusinessException(HttpStatus.CONFLICT, "Bài đã được giảng viên chốt điểm, không thể chấm lại bằng AI");

        Rubric rubric = attempt.getQuestion().getRubric();
        if (rubric == null || rubric.getCriteria().isEmpty())
            throw new BusinessException(HttpStatus.CONFLICT, "Câu hỏi chưa gắn rubric, không thể chấm điểm AI");

        List<TranscriptTurn> turns = transcriptTurns.findByQuestionAttemptIdOrderByTurnOrderAsc(questionAttemptId)
                .stream().filter(turn -> turn.belongsToCurrentRun(attempt)).toList();
        if (turns.isEmpty())
            throw new BusinessException(HttpStatus.CONFLICT, "Chưa có transcript để chấm điểm");

        String transcriptText = turns.stream()
                .map(t -> (t.getSpeaker() == Speaker.AI ? "AI" : "Sinh viên") + ": " + t.getTextContent())
                .collect(Collectors.joining("\n"));

        List<AiClient.CriterionInput> criteriaInputs = rubric.getCriteria().stream()
                .map(c -> new AiClient.CriterionInput(c.getId(), c.getCriterionName(), c.getDescription(), c.getMaxScore()))
                .toList();

        AiClient.GradingResult result = ai.gradeAnswer(attempt.getQuestion().getContent(), criteriaInputs, transcriptText);
        Map<UUID, AiClient.CriterionSuggestion> byId = result.criteria().stream()
                .collect(Collectors.toMap(AiClient.CriterionSuggestion::criterionId, s -> s));

        List<CriterionScore> details = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (RubricCriterion criterion : rubric.getCriteria()) {
            AiClient.CriterionSuggestion suggestion = byId.get(criterion.getId());
            if (suggestion == null)
                throw new BusinessException(HttpStatus.BAD_GATEWAY, "AI không chấm đủ các tiêu chí của rubric");
            details.add(new CriterionScore(criterion.getId(), criterion.getCriterionName(), criterion.getMaxScore(),
                    suggestion.score(), null, suggestion.comment()));
            total = total.add(suggestion.score());
        }

        Evaluation evaluation = existing != null ? existing : new Evaluation();
        evaluation.setQuestionAttempt(attempt);
        evaluation.setGradedBy(null);
        evaluation.setAiSuggestedScore(total);
        evaluation.setFinalScore(null);
        evaluation.setOverallFeedback(result.overallFeedback());
        evaluation.setEvaluationDetails(details);
        evaluation.setStatus(EvaluationStatus.PENDING_REVIEW);

        return mapper.toResponse(evaluations.save(evaluation));
    }

    public EvaluationResponse getByQuestionAttempt(UUID actorId, UUID questionAttemptId) {
        QuestionAttempt attempt = findAttempt(questionAttemptId);
        courseAccess.requireManager(actorId, courseIdOf(attempt));
        Evaluation evaluation = evaluations.findByQuestionAttemptId(questionAttemptId)
                .orElseThrow(() -> new ResourceNotFoundException("Evaluation của câu trả lời này chưa được AI chấm"));
        return mapper.toResponse(evaluation);
    }

    /** Giảng viên điều chỉnh điểm từng tiêu chí (từ gợi ý AI) và chốt điểm cuối. */
    @Transactional
    public EvaluationResponse finalizeEvaluation(UUID actorId, UUID questionAttemptId, FinalizeEvaluationRequest request) {
        QuestionAttempt attempt = findAttempt(questionAttemptId);
        User actor = courseAccess.requireManager(actorId, courseIdOf(attempt));

        Evaluation evaluation = evaluations.findByQuestionAttemptId(questionAttemptId)
                .orElseThrow(() -> new ResourceNotFoundException("Evaluation của câu trả lời này chưa được AI chấm"));

        Map<UUID, CriterionFinalScoreRequest> byId = request.criteria().stream()
                .collect(Collectors.toMap(CriterionFinalScoreRequest::criterionId, c -> c));
        if (byId.size() != evaluation.getEvaluationDetails().size())
            throw new BusinessException("Danh sách tiêu chí chốt điểm không khớp với rubric đã chấm");

        List<CriterionScore> updated = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (CriterionScore current : evaluation.getEvaluationDetails()) {
            CriterionFinalScoreRequest adjustment = byId.get(current.criterionId());
            if (adjustment == null)
                throw new BusinessException("Thiếu điểm chốt cho tiêu chí \"" + current.criterionName() + "\"");
            if (adjustment.finalScore().compareTo(current.maxScore()) > 0)
                throw new BusinessException("Điểm chốt tiêu chí \"" + current.criterionName()
                        + "\" vượt quá điểm tối đa " + current.maxScore());
            String comment = adjustment.comment() == null || adjustment.comment().isBlank()
                    ? current.comment() : adjustment.comment();
            updated.add(new CriterionScore(current.criterionId(), current.criterionName(), current.maxScore(),
                    current.aiScore(), adjustment.finalScore(), comment));
            total = total.add(adjustment.finalScore());
        }

        evaluation.setEvaluationDetails(updated);
        evaluation.setFinalScore(total);
        if (request.overallFeedback() != null && !request.overallFeedback().isBlank())
            evaluation.setOverallFeedback(request.overallFeedback());
        evaluation.setGradedBy(actor);
        evaluation.setStatus(EvaluationStatus.GRADED);

        return mapper.toResponse(evaluation);
    }

    public PageResponse<EvaluationResponse> findByCourse(UUID actorId, UUID courseId, EvaluationStatus status, Pageable pageable) {
        courseAccess.requireManager(actorId, courseId);
        var page = status == null
                ? evaluations.findByQuestionAttempt_ExamAttempt_Exam_Course_Id(courseId, pageable)
                : evaluations.findByQuestionAttempt_ExamAttempt_Exam_Course_IdAndStatus(courseId, status, pageable);
        return PageResponse.from(page.map(mapper::toResponse));
    }

    private QuestionAttempt findAttempt(UUID questionAttemptId) {
        return questionAttempts.findById(questionAttemptId)
                .orElseThrow(() -> new ResourceNotFoundException("QuestionAttempt", questionAttemptId));
    }

    private UUID courseIdOf(QuestionAttempt attempt) {
        return attempt.getExamAttempt().getExam().getCourse().getId();
    }
}
