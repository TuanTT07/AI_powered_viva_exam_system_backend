package cocxanhcoder.viva.exam.system.speech.websocket;

import cocxanhcoder.viva.exam.system.common.exception.BusinessException;
import cocxanhcoder.viva.exam.system.speech.service.SpeechTicketService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.*;

@Component
public class SpeechHandshakeInterceptor implements HandshakeInterceptor {
    public static final String GRANT = "speechGrant";
    private final SpeechTicketService tickets;
    private final Set<String> origins;

    public SpeechHandshakeInterceptor(SpeechTicketService tickets,
            @Value("${app.cors.allowed-origins}") String allowedOrigins) {
        this.tickets = tickets;
        origins = new HashSet<>(Arrays.stream(allowedOrigins.split(",")).map(String::trim).toList());
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler handler, Map<String, Object> attributes) {
        if (!origins.contains(request.getHeaders().getOrigin())) {
            response.setStatusCode(HttpStatus.FORBIDDEN);
            return false;
        }
        try {
            var params = UriComponentsBuilder.fromUri(request.getURI()).build().getQueryParams();
            var values = params.get("ticket");
            if (values == null || values.size() != 1) {
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return false;
            }
            attributes.put(GRANT, tickets.consume(values.getFirst()));
            response.getHeaders().setCacheControl("no-store");
            return true;
        } catch (BusinessException e) {
            response.setStatusCode(e.getStatus());
            return false;
        }
    }

    @Override public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                        WebSocketHandler handler, Exception exception) {}
}
