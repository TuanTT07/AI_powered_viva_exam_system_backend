package cocxanhcoder.viva.exam.system.questionbank.service;

import cocxanhcoder.viva.exam.system.common.exception.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal OpenAI REST adapter. It deliberately keeps provider protocol out of domain services.
 */
@Service
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "openai")
public class OpenAiAiClient implements AiClient {
    private final String key, model;
    private final RestClient client;
    private final ObjectMapper mapper;

    public OpenAiAiClient(@Value("${app.ai.openai-api-key}") String key, @Value("${app.ai.openai-model}") String model) {
        this.key = key;
        this.model = model;
        this.mapper = new ObjectMapper();
        this.client = RestClient.builder().baseUrl("https://api.openai.com/v1").build();
    }

    public String createVectorStore(String courseCode) {
        return request("/vector_stores", Map.of("name", "aives-" + courseCode), "id").asText();
    }

    public String uploadFile(Path file, String filename) {
        requireConfig();
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("purpose", "user_data");
        body.add("file", new FileSystemResource(file));
        try {
            return parse(client.post().uri("/files").header(HttpHeaders.AUTHORIZATION, "Bearer " + key).contentType(MediaType.MULTIPART_FORM_DATA).body(body).retrieve().body(String.class), "id").asText();
        } catch (org.springframework.web.client.RestClientException ex) {
            throw providerError(ex);
        }
    }

    public void addFileToVectorStore(String vectorStoreId, String fileId) {
        request("/vector_stores/" + vectorStoreId + "/files", Map.of("file_id", fileId), "id");
    }

    public void deleteFile(String fileId) {
        requireConfig();
        try {
            client.delete().uri("/files/" + fileId).header(HttpHeaders.AUTHORIZATION, "Bearer " + key).retrieve().toBodilessEntity();
        } catch (org.springframework.web.client.RestClientException ex) {
            throw providerError(ex);
        }
    }

    public List<GeneratedQuestion> generateQuestions(String vectorStoreId, String topic, int count, List<String> blooms) {
        requireConfig();
        String instructions = "You generate Vietnamese oral-exam question drafts. Return ONLY valid JSON object: {\"questions\":[{\"content\":string,\"bloomLevel\":one of REMEMBER,UNDERSTAND,APPLY,ANALYZE,EVALUATE,CREATE}]}. Use the supplied course files only. Create exactly " + count + " questions.";
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", model);
        payload.put("instructions", instructions);
        payload.put("input", "Topic: " + (topic == null ? "cover the indexed course material" : topic) + ". Allowed Bloom levels: " + String.join(",", blooms));
        payload.put("tools", List.of(Map.of("type", "file_search", "vector_store_ids", List.of(vectorStoreId))));
        JsonNode response = request("/responses", payload, null);
        String output = extractOutputText(response);
        try {
            JsonNode questions = mapper.readTree(output).path("questions");
            List<GeneratedQuestion> result = new ArrayList<>();
            if (!questions.isArray()) throw new IllegalArgumentException();
            for (JsonNode q : questions)
                result.add(new GeneratedQuestion(q.path("content").asText(), q.path("bloomLevel").asText()));
            if (result.size() != count || result.stream().anyMatch(q -> q.content().isBlank() || q.bloomLevel().isBlank()))
                throw new IllegalArgumentException();
            return result;
        } catch (Exception ex) {
            throw new BusinessException(HttpStatus.BAD_GATEWAY, "OpenAI trả dữ liệu câu hỏi không hợp lệ");
        }
    }

    private JsonNode request(String uri, Object body, String field) {
        requireConfig();
        try {
            String raw = client.post().uri(uri).header(HttpHeaders.AUTHORIZATION, "Bearer " + key).contentType(MediaType.APPLICATION_JSON).body(body).retrieve().body(String.class);
            return parse(raw, field);
        } catch (org.springframework.web.client.RestClientException ex) {
            throw providerError(ex);
        }
    }

    private JsonNode parse(String raw, String field) {
        try {
            JsonNode root = mapper.readTree(raw);
            return field == null ? root : root.path(field);
        } catch (Exception ex) {
            throw new BusinessException(HttpStatus.BAD_GATEWAY, "Không đọc được phản hồi từ OpenAI");
        }
    }

    private String extractOutputText(JsonNode root) {
        for (JsonNode item : root.path("output"))
            for (JsonNode content : item.path("content")) if (content.has("text")) return content.path("text").asText();
        throw new BusinessException(HttpStatus.BAD_GATEWAY, "OpenAI không trả nội dung câu hỏi");
    }

    private void requireConfig() {
        if (key == null || key.isBlank() || model == null || model.isBlank())
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "OPENAI_API_KEY và OPENAI_MODEL phải được cấu hình để dùng AI");
    }

    private BusinessException providerError(Exception ex) {
        return new BusinessException(HttpStatus.BAD_GATEWAY, "Không thể gọi OpenAI: " + ex.getMessage());
    }
}
