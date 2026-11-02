package cocxanhcoder.viva.exam.system.speech.config;

import cocxanhcoder.viva.exam.system.speech.websocket.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.*;

import java.util.Arrays;

@Configuration
@EnableWebSocket
public class SpeechWebSocketConfig implements WebSocketConfigurer {
    private final SpeechWebSocketHandler handler;
    private final SpeechHandshakeInterceptor handshake;
    private final String[] origins;

    public SpeechWebSocketConfig(SpeechWebSocketHandler handler, SpeechHandshakeInterceptor handshake,
                                 @Value("${app.cors.allowed-origins}") String origins) {
        this.handler = handler;
        this.handshake = handshake;
        this.origins = Arrays.stream(origins.split(",")).map(String::trim).toArray(String[]::new);
    }

    @Override public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/ws/interview/speech")
                .addInterceptors(handshake).setAllowedOrigins(origins);
    }
}
