package cocxanhcoder.viva.exam.system.speech.service;

import cocxanhcoder.viva.exam.system.interview.dto.AnswerDeadlineReachedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Allows interview to announce deadlines without depending on a speech implementation. */
@Component
@RequiredArgsConstructor
public class SpeechInterviewEventListener {
    private final SpeechSessionService sessions;

    @EventListener
    public void onAnswerDeadline(AnswerDeadlineReachedEvent event) {
        sessions.finishCurrentAnswer(event.examAttemptId(), event.questionTurnId());
    }
}
