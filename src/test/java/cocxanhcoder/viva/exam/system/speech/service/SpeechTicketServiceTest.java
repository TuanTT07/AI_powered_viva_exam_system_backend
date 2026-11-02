package cocxanhcoder.viva.exam.system.speech.service;

import cocxanhcoder.viva.exam.system.interview.service.InterviewAccess;
import cocxanhcoder.viva.exam.system.common.exception.BusinessException;
import cocxanhcoder.viva.exam.system.speech.config.SpeechProperties;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SpeechTicketServiceTest {
    private final InterviewAccess access = mock(InterviewAccess.class);
    private final Clock clock = mock(Clock.class);
    private final SpeechProperties properties = new SpeechProperties();
    private final UUID student = UUID.randomUUID(), attempt = UUID.randomUUID();
    private final Instant now = Instant.parse("2026-11-02T00:00:00Z");

    private SpeechTicketService service() {
        properties.setEnabled(true); properties.setDeepgramApiKey("test");
        when(clock.instant()).thenReturn(now);
        return new SpeechTicketService(access, properties, clock);
    }

    @Test void ticketIsSingleUseAndBoundToTheAttempt() {
        var service = service();
        var ticket = service.issue(student, attempt, now.plusSeconds(120));
        var grant = service.consume(ticket.ticket());
        assertEquals(student, grant.studentId()); assertEquals(attempt, grant.attemptId());
        assertThrows(BusinessException.class, () -> service.consume(ticket.ticket()));
        verify(access, times(2)).requireActiveAttempt(student, attempt);
    }

    @Test void expiredTicketIsRejectedAndRemoved() {
        var service = service();
        var ticket = service.issue(student, attempt, now.plusSeconds(120));
        when(clock.instant()).thenReturn(now.plusSeconds(31));
        assertThrows(BusinessException.class, () -> service.consume(ticket.ticket()));
        when(clock.instant()).thenReturn(now);
        assertThrows(BusinessException.class, () -> service.consume(ticket.ticket()));
    }

    @Test void ticketCannotOutliveJwtAndNewTicketRevokesPreviousOne() {
        var service = service();
        var first = service.issue(student, attempt, now.plusSeconds(10));
        assertEquals(now.plusSeconds(10), first.expiresAt());
        var second = service.issue(student, attempt, now.plusSeconds(10));
        assertThrows(BusinessException.class, () -> service.consume(first.ticket()));
        assertNotNull(service.consume(second.ticket()));
    }

    @Test void unauthorizedAttemptCannotGetTicket() {
        var service = service();
        doThrow(new BusinessException("Forbidden")).when(access).requireActiveAttempt(student, attempt);
        assertThrows(BusinessException.class, () -> service.issue(student, attempt, now.plusSeconds(120)));
    }
}
