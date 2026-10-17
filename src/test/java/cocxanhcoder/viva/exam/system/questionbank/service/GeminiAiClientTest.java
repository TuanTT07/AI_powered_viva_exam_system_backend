package cocxanhcoder.viva.exam.system.questionbank.service;

import cocxanhcoder.viva.exam.system.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class GeminiAiClientTest {
    private static final String URL = "https://generativelanguage.googleapis.com/v1beta/interactions";

    @Test
    void readsJsonEvenWhenResponseIsOctetStream() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(URL)).andExpect(jsonPath("$.tools[0].type").value("file_search"))
                .andRespond(withSuccess("{\"status\":\"completed\",\"output_text\":\"Java có tính đóng gói.\"}", MediaType.APPLICATION_OCTET_STREAM));
        server.expect(requestTo(URL)).andExpect(jsonPath("$.tools").doesNotExist()).andRespond(withSuccess(
                "{\"output_text\":\"{\\\"questions\\\":[{\\\"content\\\":\\\"Tính đóng gói trong Java hoạt động như thế nào?\\\",\\\"bloomLevel\\\":\\\"UNDERSTAND\\\"}]}\"}",
                MediaType.APPLICATION_OCTET_STREAM));

        var questions = new GeminiAiClient("test-key", "gemini-3.5-flash-lite", builder.build())
                .generateQuestions("fileSearchStores/test", null, 1, List.of("UNDERSTAND"));

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
    void rejectsQuestionsAboutDocumentStatus() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(URL)).andRespond(withSuccess(
                "{\"status\":\"completed\",\"output_text\":\"Java có tính đóng gói.\"}", MediaType.APPLICATION_JSON));
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
