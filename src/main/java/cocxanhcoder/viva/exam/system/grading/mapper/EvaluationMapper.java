package cocxanhcoder.viva.exam.system.grading.mapper;

import cocxanhcoder.viva.exam.system.grading.dto.EvaluationResponse;
import cocxanhcoder.viva.exam.system.grading.dto.TranscriptTurnResponse;
import cocxanhcoder.viva.exam.system.grading.entity.CriterionScore;
import cocxanhcoder.viva.exam.system.grading.entity.Evaluation;
import cocxanhcoder.viva.exam.system.interview.repository.TranscriptTurnRepository;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class EvaluationMapper {

    private final TranscriptTurnRepository transcriptTurns;

    public EvaluationMapper(TranscriptTurnRepository transcriptTurns) {
        this.transcriptTurns = transcriptTurns;
    }

    public EvaluationResponse toResponse(Evaluation evaluation) {
        BigDecimal maxScore = evaluation.getEvaluationDetails().stream()
                .map(CriterionScore::maxScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new EvaluationResponse(
                evaluation.getId(),
                evaluation.getQuestionAttempt().getId(),
                evaluation.getQuestionAttempt().getQuestion().getId(),
                evaluation.getQuestionAttempt().getQuestion().getContent(),
                evaluation.getGradedBy() == null ? null : evaluation.getGradedBy().getId(),
                evaluation.getGradedBy() == null ? null : evaluation.getGradedBy().getFullName(),
                evaluation.getAiSuggestedScore(),
                evaluation.getFinalScore(),
                maxScore,
                evaluation.getOverallFeedback(),
                evaluation.getEvaluationDetails(),
                evaluation.getStatus(),
                evaluation.getCreatedAt(),
                transcriptTurns.findByQuestionAttemptIdOrderByTurnOrderAsc(evaluation.getQuestionAttempt().getId())
                        .stream().filter(t -> t.belongsToCurrentRun(evaluation.getQuestionAttempt()))
                        .map(t -> new TranscriptTurnResponse(
                                t.getId(), t.getTurnOrder(), t.getSpeaker(), t.getTextContent(),
                                t.getAudioClipUrl(), t.getTurnType(), t.getCreatedAt())).toList(),
                evaluation.getQuestionAttempt().getExamAttempt().getAudioRecordUrl(),
                evaluation.getUpdatedAt());
    }
}
