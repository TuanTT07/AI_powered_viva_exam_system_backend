package cocxanhcoder.viva.exam.system.grading.mapper;

import cocxanhcoder.viva.exam.system.academic.entity.User;
import cocxanhcoder.viva.exam.system.exammgt.entity.ExamAttempt;
import cocxanhcoder.viva.exam.system.exammgt.entity.QuestionAttempt;
import cocxanhcoder.viva.exam.system.grading.dto.EvaluationResponse;
import cocxanhcoder.viva.exam.system.grading.dto.TranscriptTurnResponse;
import cocxanhcoder.viva.exam.system.grading.entity.CriterionScore;
import cocxanhcoder.viva.exam.system.grading.entity.Evaluation;
import cocxanhcoder.viva.exam.system.grading.entity.EvaluationStatus;
import cocxanhcoder.viva.exam.system.interview.entity.TranscriptTurn;
import cocxanhcoder.viva.exam.system.interview.repository.TranscriptTurnRepository;
import cocxanhcoder.viva.exam.system.questionbank.entity.Rubric;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class EvaluationMapper {

    private final TranscriptTurnRepository transcriptTurns;

    public EvaluationMapper(TranscriptTurnRepository transcriptTurns) {
        this.transcriptTurns = transcriptTurns;
    }

    public EvaluationResponse toResponse(Evaluation evaluation) {
        QuestionAttempt qa = evaluation.getQuestionAttempt();
        ExamAttempt ea = qa != null ? qa.getExamAttempt() : null;
        User student = ea != null ? ea.getStudent() : null;

        List<CriterionScore> details = evaluation.getEvaluationDetails() != null
                ? evaluation.getEvaluationDetails()
                : List.of();
        BigDecimal maxScore = details.stream()
                .map(CriterionScore::maxScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new EvaluationResponse(
                evaluation.getId(),
                qa != null ? qa.getId() : null,
                qa != null && qa.getQuestion() != null ? qa.getQuestion().getId() : null,
                qa != null && qa.getQuestion() != null ? qa.getQuestion().getContent() : null,
                evaluation.getGradedBy() == null ? null : evaluation.getGradedBy().getId(),
                evaluation.getGradedBy() == null ? null : evaluation.getGradedBy().getFullName(),
                evaluation.getAiSuggestedScore(),
                evaluation.getFinalScore(),
                maxScore,
                evaluation.getOverallFeedback(),
                details,
                evaluation.getStatus(),
                evaluation.getCreatedAt(),
                qa != null ? transcriptTurns.findByQuestionAttemptIdOrderByTurnOrderAsc(qa.getId())
                        .stream().filter(t -> t.belongsToCurrentRun(qa))
                        .map(t -> new TranscriptTurnResponse(
                                t.getId(), t.getTurnOrder(), t.getSpeaker(), t.getTextContent(),
                                t.getAudioClipUrl(), t.getTurnType(), t.getCreatedAt())).toList() : List.of(),
                ea != null ? ea.getAudioRecordUrl() : null,
                evaluation.getUpdatedAt(),
                ea != null ? ea.getId() : null,
                student != null ? student.getId() : null,
                student != null ? student.getFullName() : null,
                student != null ? student.getUserCode() : null,
                evaluation.getPublishedAt());
    }

    public EvaluationResponse toDraftResponse(QuestionAttempt qa, List<TranscriptTurn> turns, Rubric rubric) {
        ExamAttempt ea = qa.getExamAttempt();
        User student = ea != null ? ea.getStudent() : null;
        List<CriterionScore> criteria = rubric != null && rubric.getCriteria() != null
                ? rubric.getCriteria().stream()
                        .map(c -> new CriterionScore(c.getId(), c.getCriterionName(), c.getMaxScore(), BigDecimal.ZERO, null, null))
                        .toList()
                : List.of();
        BigDecimal maxScore = criteria.stream()
                .map(CriterionScore::maxScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new EvaluationResponse(
                null,
                qa.getId(),
                qa.getQuestion() != null ? qa.getQuestion().getId() : null,
                qa.getQuestion() != null ? qa.getQuestion().getContent() : null,
                null,
                null,
                null,
                null,
                maxScore,
                null,
                criteria,
                EvaluationStatus.PENDING_REVIEW,
                qa.getCreatedAt(),
                turns != null ? turns.stream().map(t -> new TranscriptTurnResponse(
                        t.getId(), t.getTurnOrder(), t.getSpeaker(), t.getTextContent(),
                        t.getAudioClipUrl(), t.getTurnType(), t.getCreatedAt())).toList() : List.of(),
                ea != null ? ea.getAudioRecordUrl() : null,
                qa.getEndTime() != null ? qa.getEndTime() : qa.getCreatedAt(),
                ea != null ? ea.getId() : null,
                student != null ? student.getId() : null,
                student != null ? student.getFullName() : null,
                student != null ? student.getUserCode() : null,
                null);
    }
}
