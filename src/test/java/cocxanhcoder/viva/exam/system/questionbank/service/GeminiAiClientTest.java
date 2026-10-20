package cocxanhcoder.viva.exam.system.questionbank.service;

import cocxanhcoder.viva.exam.system.common.exception.BusinessException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class GeminiAiClientTest {
    private static final String URL = "https://generativelanguage.googleapis.com/v1beta/interactions";

    @Test
    void missingTopicUsesSampledTextWithoutGenericFileSearch() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(URL)).andExpect(jsonPath("$.tools").doesNotExist())
                .andExpect(jsonPath("$.input").value(containsString("Encapsulation keeps fields private")))
                .andRespond(withSuccess(
                        "{\"status\":\"completed\",\"output_text\":\"{\\\"questions\\\":[{\\\"content\\\":\\\"Giải thích cơ chế đóng gói dữ liệu trong Java?\\\",\\\"bloomLevel\\\":\\\"UNDERSTAND\\\"}]}\"}",
                        MediaType.APPLICATION_JSON));

        var result = new GeminiAiClient("test-key", "gemini-3.5-flash-lite", builder.build())
                .generateQuestions("fileSearchStores/test", null, 1, List.of("UNDERSTAND"),
                        "Nguồn: Java OOP\nEncapsulation keeps fields private and exposes controlled access.");

        assertEquals(1, result.size());
        server.verify();
    }

    @Test
    void readsJsonEvenWhenResponseIsOctetStream() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(URL)).andExpect(jsonPath("$.tools[0].type").value("file_search"))
                .andExpect(jsonPath("$.input").value(containsString("'Lập trình hướng đối tượng Java' (Java object-oriented programming / OOP)")))
                .andRespond(withSuccess("{\"status\":\"completed\",\"output_text\":\"Java có tính đóng gói.\"}", MediaType.APPLICATION_OCTET_STREAM));
        server.expect(requestTo(URL)).andExpect(jsonPath("$.tools").doesNotExist()).andRespond(withSuccess(
                "{\"output_text\":\"{\\\"questions\\\":[{\\\"content\\\":\\\"Tính đóng gói trong Java hoạt động như thế nào?\\\",\\\"bloomLevel\\\":\\\"UNDERSTAND\\\"}]}\"}",
                MediaType.APPLICATION_OCTET_STREAM));

        var questions = new GeminiAiClient("test-key", "gemini-3.5-flash-lite", builder.build())
                .generateQuestions("fileSearchStores/test", "Lập trình hướng đối tượng Java", 1, List.of("UNDERSTAND"));

        assertEquals(1, questions.size());
        assertEquals("Tính đóng gói trong Java hoạt động như thế nào?", questions.getFirst().content());
        server.verify();
    }

    @Test
    void rejectsNonJsonOctetStreamResponse() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(URL)).andRespond(withSuccess("{\"status\":\"completed\",\"output_text\":\"Java có tính đóng gói.\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(URL)).andRespond(withSuccess(new byte[]{0, 1, 2}, MediaType.APPLICATION_OCTET_STREAM));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                new GeminiAiClient("test-key", "gemini-3.5-flash-lite", builder.build())
                        .generateQuestions("fileSearchStores/test", null, 1, List.of("UNDERSTAND")));

        assertEquals("Gemini trả phản hồi không phải JSON hợp lệ", ex.getMessage());
        server.verify();
    }

    @Test
    void doesNotRetryExhaustedDailyQuota() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"error\":{\"code\":\"quota_exceeded\"}}"));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                new GeminiAiClient("test-key", "gemini-3.5-flash-lite", builder.build())
                        .generateQuestions("fileSearchStores/test", null, 1, List.of("UNDERSTAND")));

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, ex.getStatus());
        server.verify();
    }

    @Test
    void pollsBackgroundInteractionUntilCompleted() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(URL)).andRespond(withSuccess(
                "{\"id\":\"v1_abc123\",\"status\":\"in_progress\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(URL + "/v1_abc123")).andRespond(withSuccess(
                "{\"id\":\"v1_abc123\",\"status\":\"completed\",\"steps\":[{\"type\":\"user_input\",\"content\":[{\"text\":\"prompt\"}]},{\"type\":\"model_output\",\"content\":[{\"text\":\"Java có tính đóng gói.\"}]}]}",
                MediaType.APPLICATION_JSON));
        server.expect(requestTo(URL)).andRespond(withSuccess(
                "{\"status\":\"completed\",\"output_text\":\"{\\\"questions\\\":[{\\\"content\\\":\\\"Tính đóng gói trong Java hoạt động như thế nào?\\\",\\\"bloomLevel\\\":\\\"UNDERSTAND\\\"}]}\"}",
                MediaType.APPLICATION_JSON));

        var questions = new GeminiAiClient("test-key", "gemini-3.5-flash-lite", builder.build())
                .generateQuestions("fileSearchStores/test", null, 1, List.of("UNDERSTAND"));

        assertEquals("Tính đóng gói trong Java hoạt động như thế nào?", questions.getFirst().content());
        server.verify();
    }

    @Test
    void stopsWhenFileSearchReturnsApologyInsteadOfCourseFacts() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(URL)).andRespond(withSuccess(
                "{\"status\":\"completed\",\"output_text\":\"Xin lỗi, không tìm thấy thông tin về ABC trong tài liệu. Vui lòng cung cấp thêm nội dung.\"}",
                MediaType.APPLICATION_JSON));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                new GeminiAiClient("test-key", "gemini-3.5-flash-lite", builder.build())
                        .generateQuestions("fileSearchStores/test", "ABC", 6, List.of("UNDERSTAND")));

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatus());
        server.verify();
    }

    @Test
    void stopsWhenFileSearchSaysNoDocumentsWereUploaded() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(URL)).andRespond(withSuccess(
                "{\"status\":\"completed\",\"output_text\":\"Based on the available course files, there are no documents or text passages uploaded that describe the main course concepts. Please upload the relevant course materials.\"}",
                MediaType.APPLICATION_JSON));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                new GeminiAiClient("test-key", "gemini-3.5-flash-lite", builder.build())
                        .generateQuestions("fileSearchStores/test", null, 5, List.of("UNDERSTAND")));

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatus());
        server.verify();
    }

    @Test
    void rejectsQuestionsAboutDocumentStatus() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(URL)).andRespond(withSuccess(
                "{\"status\":\"completed\",\"output_text\":\"Java có tính đóng gói.\"}", MediaType.APPLICATION_JSON));
        for (int i = 0; i < 3; i++)
            server.expect(requestTo(URL)).andRespond(withSuccess(
                    "{\"status\":\"completed\",\"output_text\":\"{\\\"questions\\\":[{\\\"content\\\":\\\"Theo tài liệu, chủ đề ABC có thông tin gì?\\\",\\\"bloomLevel\\\":\\\"UNDERSTAND\\\"}]}\"}",
                    MediaType.APPLICATION_JSON));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                new GeminiAiClient("test-key", "gemini-3.5-flash-lite", builder.build())
                        .generateQuestions("fileSearchStores/test", "ABC", 1, List.of("UNDERSTAND")));

        assertEquals(HttpStatus.BAD_GATEWAY, ex.getStatus());
        server.verify();
    }

    @Test
    void rejectsQuestionAboutMissingUploadedCourseFiles() throws Exception {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(URL)).andRespond(withSuccess(
                "{\"status\":\"completed\",\"output_text\":\"Java OOP có tính đóng gói.\"}", MediaType.APPLICATION_JSON));
        for (int i = 0; i < 3; i++)
            server.expect(requestTo(URL)).andRespond(withSuccess(questionResponse(
                    "Hãy giải thích tại sao hệ thống không tìm thấy tài liệu khóa học nào đã được tải lên."),
                    MediaType.APPLICATION_JSON));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                new GeminiAiClient("test-key", "gemini-3.5-flash-lite", builder.build())
                        .generateQuestions("fileSearchStores/test", null, 1, List.of("UNDERSTAND")));

        assertEquals(HttpStatus.BAD_GATEWAY, ex.getStatus());
        server.verify();
    }

    @Test
    void keepsValidQuestionsAndRequestsOnlyMissingOnes() throws Exception {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(URL)).andRespond(withSuccess(
                "{\"status\":\"completed\",\"output_text\":\"Java OOP gồm đóng gói, kế thừa, đa hình và trừu tượng.\"}",
                MediaType.APPLICATION_JSON));
        server.expect(requestTo(URL)).andExpect(jsonPath("$.input").value(containsString("tạo đúng 5 câu hỏi")))
                .andRespond(withSuccess(questionResponse(
                        "Tính đóng gói trong Java bảo vệ dữ liệu ra sao?",
                        "Hãy so sánh kế thừa và đa hình trong Java.",
                        "Theo tài liệu, chủ đề này có thông tin gì?",
                        "Tính đóng gói trong Java bảo vệ dữ liệu ra sao?", "Ngắn?"), MediaType.APPLICATION_JSON));
        server.expect(requestTo(URL)).andExpect(jsonPath("$.input").value(containsString("tạo đúng 2 câu hỏi")))
                .andRespond(withSuccess(questionResponse(
                        "Phân biệt lớp trừu tượng và interface trong Java.",
                        "Giải thích cơ chế ghi đè phương thức trong Java."), MediaType.APPLICATION_JSON));
        server.expect(requestTo(URL)).andExpect(jsonPath("$.input").value(containsString("tạo đúng 1 câu hỏi")))
                .andRespond(withSuccess(questionResponse(
                        "Khi nào nên dùng composition thay vì inheritance trong Java?"), MediaType.APPLICATION_JSON));

        var questions = new GeminiAiClient("test-key", "gemini-3.5-flash-lite", builder.build())
                .generateQuestions("fileSearchStores/test", "Lập trình hướng đối tượng Java", 5,
                        List.of("UNDERSTAND", "APPLY"));

        assertEquals(5, questions.size());
        assertEquals("Hãy so sánh kế thừa và đa hình trong Java.", questions.get(1).content());
        server.verify();
    }

    private static String questionResponse(String... contents) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        List<Map<String, String>> questions = Arrays.stream(contents)
                .map(content -> Map.of("content", content, "bloomLevel", "UNDERSTAND")).toList();
        return mapper.writeValueAsString(Map.of("output_text", mapper.writeValueAsString(Map.of("questions", questions))));
    }

    @Test
    void reportsBackgroundInteractionErrorFromErrorsArray() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(URL)).andRespond(withSuccess(
                "{\"status\":\"failed\",\"errors\":[{\"code\":\"invalid_request\",\"message\":\"File Search unavailable for this model\"}]}",
                MediaType.APPLICATION_JSON));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                new GeminiAiClient("test-key", "gemini-3.5-flash-lite", builder.build())
                        .generateQuestions("fileSearchStores/test", null, 1, List.of("UNDERSTAND")));

        assertEquals(HttpStatus.BAD_GATEWAY, ex.getStatus());
        assertEquals("Gemini tạo câu hỏi thất bại [invalid_request]: File Search unavailable for this model", ex.getMessage());
        server.verify();
    }
}
