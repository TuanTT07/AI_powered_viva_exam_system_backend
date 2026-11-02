package cocxanhcoder.viva.exam.system.speech.service;

import cocxanhcoder.viva.exam.system.interview.service.InterviewAccess;
import cocxanhcoder.viva.exam.system.common.exception.BusinessException;
import cocxanhcoder.viva.exam.system.speech.config.SpeechProperties;
import cocxanhcoder.viva.exam.system.speech.dto.SpeechProtocol.Ticket;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.*;

/** Single-instance ticket store. Never log tickets or websocket query strings. */
@Service
public class SpeechTicketService {
    private final InterviewAccess access;
    private final SpeechProperties properties;
    private final Map<String, Grant> tickets = new HashMap<>();
    private final SecureRandom random = new SecureRandom();
    private final Clock clock;

    @Autowired
    public SpeechTicketService(InterviewAccess access, SpeechProperties properties) {
        this(access, properties, Clock.systemUTC());
    }

    SpeechTicketService(InterviewAccess access, SpeechProperties properties, Clock clock) {
        this.access = access;
        this.properties = properties;
        this.clock = clock;
    }

    public synchronized Ticket issue(UUID student, UUID attempt, Instant jwtExpiresAt) {
        properties.requireConfigured();
        access.requireActiveAttempt(student, attempt);
        Instant now = clock.instant();
        if (jwtExpiresAt == null || !jwtExpiresAt.isAfter(now)) throw invalid();
        tickets.entrySet().removeIf(e -> !e.getValue().expiresAt().isAfter(now)
                || e.getValue().studentId().equals(student));
        if (tickets.size() >= 1000)
            throw new BusinessException(HttpStatus.TOO_MANY_REQUESTS, "Có quá nhiều yêu cầu kết nối speech");
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant expires = now.plusSeconds(properties.getTicketTtlSeconds());
        if (jwtExpiresAt.isBefore(expires)) expires = jwtExpiresAt;
        tickets.put(token, new Grant(student, attempt, expires, jwtExpiresAt));
        return new Ticket(token, expires, "/ws/interview/speech");
    }

    public synchronized Grant consume(String ticket) {
        Grant grant = ticket == null ? null : tickets.remove(ticket);
        if (grant == null || !grant.expiresAt().isAfter(clock.instant())) throw invalid();
        access.requireActiveAttempt(grant.studentId(), grant.attemptId());
        return grant;
    }

    private BusinessException invalid() {
        return new BusinessException(HttpStatus.UNAUTHORIZED, "Ticket không hợp lệ hoặc đã hết hạn");
    }

    public record Grant(UUID studentId, UUID attemptId, Instant expiresAt, Instant jwtExpiresAt) {}
}
