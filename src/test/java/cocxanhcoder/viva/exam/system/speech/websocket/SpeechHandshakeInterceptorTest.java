package cocxanhcoder.viva.exam.system.speech.websocket;

import cocxanhcoder.viva.exam.system.common.exception.BusinessException;
import cocxanhcoder.viva.exam.system.speech.service.SpeechTicketService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.*;
import org.springframework.mock.web.*;
import java.time.Instant;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SpeechHandshakeInterceptorTest {
    private final SpeechTicketService tickets = mock(SpeechTicketService.class);
    private final SpeechHandshakeInterceptor interceptor =
            new SpeechHandshakeInterceptor(tickets, "http://localhost:5173");

    @Test void allowedOriginAndSingleUseTicketBindTheGrant() {
        var grant = new SpeechTicketService.Grant(UUID.randomUUID(), UUID.randomUUID(),
                Instant.now().plusSeconds(30), Instant.now().plusSeconds(60));
        when(tickets.consume("valid")).thenReturn(grant);
        var attrs = new HashMap<String, Object>();
        var response = new MockHttpServletResponse();
        assertTrue(interceptor.beforeHandshake(request("http://localhost:5173", "ticket=valid"),
                new ServletServerHttpResponse(response), null, attrs));
        assertEquals(grant, attrs.get(SpeechHandshakeInterceptor.GRANT));
    }

    @Test void disallowedAndMissingOriginsAreRejectedBeforeConsumingTickets() {
        for (String origin : Arrays.asList("https://attacker.example", null)) {
            var response = new MockHttpServletResponse();
            assertFalse(interceptor.beforeHandshake(request(origin, "ticket=valid"),
                    new ServletServerHttpResponse(response), null, new HashMap<>()));
            assertEquals(403, response.getStatus());
        }
        verifyNoInteractions(tickets);
    }

    @Test void missingDuplicateAndInvalidTicketsAreRejected() {
        when(tickets.consume("invalid")).thenThrow(new BusinessException(HttpStatus.UNAUTHORIZED, "Invalid"));
        for (String query : Arrays.asList(null, "ticket=a&ticket=b", "ticket=invalid")) {
            var response = new MockHttpServletResponse();
            assertFalse(interceptor.beforeHandshake(request("http://localhost:5173", query),
                    new ServletServerHttpResponse(response), null, new HashMap<>()));
            assertEquals(401, response.getStatus());
        }
    }

    private ServletServerHttpRequest request(String origin, String query) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/ws/interview/speech");
        request.setServerName("localhost"); request.setServerPort(8080);
        if (origin != null) request.addHeader("Origin", origin);
        request.setQueryString(query);
        return new ServletServerHttpRequest(request);
    }
}
