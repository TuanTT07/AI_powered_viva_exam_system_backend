package cocxanhcoder.viva.exam.system.speech.websocket;

import cocxanhcoder.viva.exam.system.speech.dto.SpeechProtocol.ClientEvent;
import cocxanhcoder.viva.exam.system.speech.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

@Component
@RequiredArgsConstructor
public class SpeechWebSocketHandler extends AbstractWebSocketHandler {
    // Match the existing project's Jackson 2 DTO serialization; no provider data is exposed.
    private final ObjectMapper json = new ObjectMapper();
    private final SpeechSessionService sessions;

    @Override public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        session.setTextMessageSizeLimit(4096);
        session.setBinaryMessageSizeLimit(32768);
        sessions.connect(session, (SpeechTicketService.Grant) session.getAttributes()
                .get(SpeechHandshakeInterceptor.GRANT));
    }

    @Override protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            ClientEvent event = json.readValue(message.getPayload(), ClientEvent.class);
            if (event.type() == null || event.requestId() == null) throw new IllegalArgumentException();
            sessions.command(session.getId(), event);
        } catch (Exception e) {
            sessions.protocolError(session.getId());
        }
    }

    @Override protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        byte[] pcm = new byte[message.getPayloadLength()];
        message.getPayload().get(pcm);
        sessions.audio(session.getId(), pcm);
    }

    @Override public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.disconnect(session.getId());
    }

    @Override public void handleTransportError(WebSocketSession session, Throwable exception) {
        sessions.disconnect(session.getId());
        try { session.close(CloseStatus.SERVER_ERROR); } catch (Exception ignored) {}
    }
}
