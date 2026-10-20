package cocxanhcoder.viva.exam.system.questionbank.service;

import cocxanhcoder.viva.exam.system.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OpenAiAiClientTest {
    @Test
    void missingTopicUsesSampledTextWithoutFileSearch() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.openai.com/v1");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.openai.com/v1/responses"))
                .andExpect(jsonPath("$.tools").doesNotExist())
                .andExpect(jsonPath("$.input").value(containsString("Encapsulation keeps fields private")))
                .andRespond(withSuccess("{\"status\":\"completed\",\"output\":[{\"type\":\"message\",\"content\":[{\"type\":\"output_text\",\"text\":\"{\\\"questions\\\":[{\\\"content\\\":\\\"Giải thích cơ chế đóng gói dữ liệu trong Java?\\\",\\\"bloomLevel\\\":\\\"UNDERSTAND\\\"}]}\"}]}]}", MediaType.APPLICATION_JSON));

        var result = new OpenAiAiClient("test-key", "gpt-5.4-mini", 45, 700, builder.build())
                .generateQuestions("vs_test", null, 1, List.of("UNDERSTAND"),
                        "Nguồn: Java OOP\nEncapsulation keeps fields private and exposes controlled access.");

        assertEquals(1, result.size());
        server.verify();
    }
    @Test
    void generatesWithConfiguredModelAndTokenLimit() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.openai.com/v1");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.openai.com/v1/responses"))
                .andExpect(jsonPath("$.model").value("gpt-5.4-mini"))
                .andExpect(jsonPath("$.max_output_tokens").value(700))
                .andExpect(jsonPath("$.tools[0].vector_store_ids[0]").value("vs_test"))
                .andRespond(withSuccess("{\"status\":\"completed\",\"output\":[{\"type\":\"message\",\"content\":[{\"type\":\"output_text\",\"text\":\"{\\\"questions\\\":[{\\\"content\\\":\\\"Giải thích tính đóng gói?\\\",\\\"bloomLevel\\\":\\\"UNDERSTAND\\\"}]}\"}]}]}", MediaType.APPLICATION_JSON));

        var result = new OpenAiAiClient("test-key", "gpt-5.4-mini", 45, 700, builder.build())
                .generateQuestions("vs_test", "Java", 1, List.of("UNDERSTAND"));

        assertEquals("Giải thích tính đóng gói?", result.getFirst().content());
        server.verify();
    }

    @Test
    void waitsForVectorStoreFileToBeIndexed() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.openai.com/v1");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.openai.com/v1/vector_stores/vs_test/files"))
                .andRespond(withSuccess("{\"id\":\"file_test\",\"status\":\"completed\"}", MediaType.APPLICATION_JSON));

        new OpenAiAiClient("test-key", "gpt-5.4-mini", 45, 700, builder.build())
                .addFileToVectorStore("vs_test", "file_test");

        server.verify();
    }

    @Test
    void reportsIncompleteOutputInsteadOfParsingPartialJson() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.openai.com/v1");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.openai.com/v1/responses"))
                .andRespond(withSuccess("{\"status\":\"incomplete\",\"incomplete_details\":{\"reason\":\"max_output_tokens\"},\"output\":[]}", MediaType.APPLICATION_JSON));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                new OpenAiAiClient("test-key", "gpt-5.4-mini", 45, 700, builder.build())
                        .generateQuestions("vs_test", null, 1, List.of("UNDERSTAND")));

        assertEquals(HttpStatus.BAD_GATEWAY, ex.getStatus());
        assertEquals("OpenAI dừng trước khi tạo đủ câu hỏi (max_output_tokens)", ex.getMessage());
        server.verify();
    }

    @Test
    void blankKeyFailsWithoutCallingOpenAi() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.openai.com/v1");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        BusinessException ex = assertThrows(BusinessException.class, () ->
                new OpenAiAiClient("", "gpt-5.4-mini", 45, 700, builder.build())
                        .generateQuestions("vs_test", null, 1, List.of("UNDERSTAND")));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getStatus());
        server.verify();
    }
}
