package cocxanhcoder.viva.exam.system.speech.controller;

import cocxanhcoder.viva.exam.system.interview.dto.InterviewPrompt;
import cocxanhcoder.viva.exam.system.interview.service.InterviewAnswers;
import cocxanhcoder.viva.exam.system.interview.service.InterviewAccess;
import cocxanhcoder.viva.exam.system.common.config.*;
import cocxanhcoder.viva.exam.system.speech.config.SpeechWebSocketConfig;
import cocxanhcoder.viva.exam.system.speech.config.SpeechProperties;
import cocxanhcoder.viva.exam.system.speech.dto.SpeechProtocol.Ticket;
import cocxanhcoder.viva.exam.system.speech.service.*;
import cocxanhcoder.viva.exam.system.speech.websocket.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.time.Instant;
import java.util.UUID;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = SpeechController.class, properties = {
        "spring.config.import=optional:classpath:/speech-test-empty.properties",
        "app.cors.allowed-origins=http://localhost:5173"
})
@Import({SecurityConfig.class, MustChangePasswordFilter.class, SpeechWebSocketConfig.class,
        SpeechHandshakeInterceptor.class, SpeechWebSocketHandler.class})
class SpeechHttpSecurityTest {
    @Autowired MockMvc mvc;
    @MockitoBean SpeechTicketService tickets;
    @MockitoBean QuestionSpeechService audio;
    @MockitoBean InterviewAnswers transcripts;
    @MockitoBean SpeechSessionService sessions;
    @MockitoBean JwtDecoder decoder;
    @MockitoBean InterviewAccess access;
    @MockitoBean SpeechProperties properties;
    private final UUID student = UUID.randomUUID(), attempt = UUID.randomUUID();

    @Test void unauthenticatedAndLecturerCannotIssueSpeechTicket() throws Exception {
        String path = "/api/interview/attempts/" + attempt + "/speech-ticket";
        mvc.perform(post(path)).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.success").value(false));
        mvc.perform(post(path).with(jwt().authorities(new SimpleGrantedAuthority("ROLE_LECTURER"))))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.status").value(403));
        verifyNoInteractions(tickets);
    }

    @Test void studentReceivesTicketWithNoStoreAndPasswordResetIsBlocked() throws Exception {
        Instant expires = Instant.now().plusSeconds(120);
        when(tickets.issue(eq(student), eq(attempt), any()))
                .thenReturn(new Ticket("test-ticket", expires, "/ws/interview/speech"));
        String path = "/api/interview/attempts/" + attempt + "/speech-ticket";
        mvc.perform(post(path).with(jwt().jwt(j -> j.subject(student.toString()).expiresAt(expires))
                        .authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.data.ticket").value("test-ticket"));
        mvc.perform(post(path).with(jwt().jwt(j -> j.subject(student.toString()).claim("mustChangePassword", true))
                        .authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
                .andExpect(status().is(428));
        verify(tickets, times(1)).issue(any(), any(), any());
    }

    @Test void websocketHandshakeNeedsTicketAndAllowedOriginEvenWithoutBearerHeader() throws Exception {
        mvc.perform(get("/ws/interview/speech").header("Origin", "http://localhost:5173"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/ws/interview/speech?ticket=x").header("Origin", "https://attacker.example"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(tickets);
    }

    @Test void authenticatedAudioResponseIsBinaryAndPrivate() throws Exception {
        UUID id = UUID.randomUUID();
        when(audio.read(student, id)).thenReturn(new byte[] {1, 2, 3});
        mvc.perform(get("/api/interview/speech/audio/" + id)
                        .with(jwt().jwt(j -> j.subject(student.toString()))
                                .authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
                .andExpect(status().isOk()).andExpect(content().contentType("audio/mpeg"))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(content().bytes(new byte[] {1, 2, 3}));
    }

    @Test void playbackReturnsAuthorizedQuestionForDeepgramTts() throws Exception {
        UUID prompt = UUID.randomUUID();
        when(access.requirePrompt(student, attempt, prompt))
                .thenReturn(new InterviewPrompt(prompt, UUID.randomUUID(), "Cau hoi demo", 60));
        when(properties.getLanguage()).thenReturn("vi-VN");
        mvc.perform(get("/api/interview/attempts/" + attempt + "/speech/questions/" + prompt + "/playback")
                        .with(jwt().jwt(j -> j.subject(student.toString()))
                                .authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.text").value("Cau hoi demo"))
                .andExpect(jsonPath("$.data.ttsProvider").value("deepgram"))
                .andExpect(jsonPath("$.data.language").value("vi-VN"));
    }
}
