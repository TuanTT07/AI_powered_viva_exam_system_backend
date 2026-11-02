package cocxanhcoder.viva.exam.system.questionbank.service;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

/** Boundary around the external AI provider; tests mock this interface. */
public interface AiClient {
    String createVectorStore(String courseCode);
    String uploadFile(Path file, String filename);
    void addFileToVectorStore(String vectorStoreId, String fileId);
    void deleteFile(String fileId);
    List<GeneratedQuestion> generateQuestions(String vectorStoreId, String topic, int count, List<String> bloomLevels);
    default List<GeneratedQuestion> generateQuestions(String vectorStoreId, String topic, int count,
                                                      List<String> bloomLevels, String sourceContext) {
        return generateQuestions(vectorStoreId, topic, count, bloomLevels);
    }
    record GeneratedQuestion(String content, String bloomLevel) { }

    /** Chấm transcript theo rubric, trả điểm gợi ý + nhận xét cho từng tiêu chí. */
    GradingResult gradeAnswer(String questionContent, List<CriterionInput> criteria, String transcriptText);

    record CriterionInput(UUID criterionId, String criterionName, String description, BigDecimal maxScore) { }
    record CriterionSuggestion(UUID criterionId, BigDecimal score, String comment) { }
    record GradingResult(List<CriterionSuggestion> criteria, String overallFeedback) { }
}
