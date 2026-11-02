package cocxanhcoder.viva.exam.system.speech.service;

import cocxanhcoder.viva.exam.system.interview.dto.AnswerDeadlineReachedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import java.util.UUID;
import static org.mockito.Mockito.*;

class SpeechInterviewEventListenerTest {
    @Test void interviewDeadlineReachesSpeechThroughSpringEventDispatch() {
        SpeechSessionService sessions = mock(SpeechSessionService.class);
        try (var context = new AnnotationConfigApplicationContext()) {
            context.registerBean(SpeechSessionService.class, () -> sessions);
            context.register(SpeechInterviewEventListener.class);
            context.refresh();
            UUID attempt = UUID.randomUUID(), prompt = UUID.randomUUID();
            context.publishEvent(new AnswerDeadlineReachedEvent(attempt, prompt));
            verify(sessions).finishCurrentAnswer(attempt, prompt);
        }
    }
}
