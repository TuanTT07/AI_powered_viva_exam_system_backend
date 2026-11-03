package cocxanhcoder.viva.exam.system.interview.service;

import cocxanhcoder.viva.exam.system.exammgt.entity.ExamAttemptStatus;
import cocxanhcoder.viva.exam.system.exammgt.repository.ExamAttemptRepository;
import cocxanhcoder.viva.exam.system.interview.dto.AnswerDeadlineReachedEvent;
import cocxanhcoder.viva.exam.system.interview.dto.AnswerFinalizedEvent;
import cocxanhcoder.viva.exam.system.questionbank.service.AiClient;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.*;

@Component
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
class InterviewWorker {
    private final InterviewFlow flow;
    private final ExamAttemptRepository attempts;
    private final ApplicationEventPublisher events;
    private final AiClient ai;
    private final Set<UUID> running = ConcurrentHashMap.newKeySet();
    private final ExecutorService provider = new ThreadPoolExecutor(4, 4, 0, TimeUnit.SECONDS,
            new SynchronousQueue<>(), Thread.ofVirtual().name("interview-ai-", 0).factory());

    @TransactionalEventListener
    public void onAnswer(AnswerFinalizedEvent event) {
        CompletableFuture.runAsync(() -> check(event.examAttemptId()));
    }

    @Scheduled(fixedDelay = 1000)
    public void recover() {
        attempts.findIdsByStatus(ExamAttemptStatus.IN_PROGRESS).forEach(this::check);
    }

    private void check(UUID id) {
        if (!running.add(id)) return;
        boolean handedOff = false;
        try {
            var work = flow.pending(id);
            if (work == null) return;
            if (work.deadline() != null) {
                events.publishEvent(new AnswerDeadlineReachedEvent(id, work.turnId()));
                flow.advance(work, null);
            } else if (work.context() == null) {
                flow.advance(work, null);
            } else {
                CompletableFuture<String> result = new CompletableFuture<>();
                Future<?> call;
                try {
                    call = provider.submit(() -> {
                        try {
                            result.complete(ai.suggestFollowUp(work.context()));
                        } catch (RuntimeException e) {
                            result.completeExceptionally(e);
                        }
                    });
                } catch (RejectedExecutionException e) {
                    log.debug("Interview AI workers busy for attempt {}", id);
                    flow.advance(work, null);
                    return;
                }
                result
                        .orTimeout(10, TimeUnit.SECONDS)
                        .whenCompleteAsync((question, error) -> {
                            try {
                                if (error != null) {
                                    call.cancel(true);
                                    log.warn("Interview AI fallback for attempt {} ({})",
                                            id, error.getClass().getSimpleName());
                                }
                                flow.advance(work, question);
                            } catch (RuntimeException e) {
                                log.error("Could not advance interview {}", id, e);
                            } finally {
                                running.remove(id);
                            }
                        });
                handedOff = true;
            }
        } catch (RuntimeException e) {
            log.error("Could not check interview {}", id, e);
        } finally {
            if (!handedOff) running.remove(id);
        }
    }

    @PreDestroy
    void stop() {
        provider.shutdownNow();
    }
}
