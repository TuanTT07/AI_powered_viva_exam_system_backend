package cocxanhcoder.viva.exam.system.questionbank.service;

import java.nio.file.Path;
import java.util.List;

/** Boundary around the external AI provider; tests mock this interface. */
public interface AiClient {
    String createVectorStore(String courseCode);
    String uploadFile(Path file, String filename);
    void addFileToVectorStore(String vectorStoreId, String fileId);
    void deleteFile(String fileId);
    List<GeneratedQuestion> generateQuestions(String vectorStoreId, String topic, int count, List<String> bloomLevels);
    record GeneratedQuestion(String content, String bloomLevel) { }
}
