package cocxanhcoder.viva.exam.system.speech.service;

import cocxanhcoder.viva.exam.system.interview.service.InterviewAccess;
import cocxanhcoder.viva.exam.system.common.exception.BusinessException;
import cocxanhcoder.viva.exam.system.speech.config.SpeechProperties;
import cocxanhcoder.viva.exam.system.speech.dto.SpeechProtocol.AudioResult;
import cocxanhcoder.viva.exam.system.speech.provider.TextToSpeechClient;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

/** Bounded private in-memory MP3 cache; audio is never published under a public URL. */
@Service
public class QuestionSpeechService {
    private static final Logger log = LoggerFactory.getLogger(QuestionSpeechService.class);
    private final InterviewAccess access;
    private final SpeechProperties properties;
    private final TextToSpeechClient tts;
    private final Semaphore capacity;
    private final Map<UUID, Clip> clips = new LinkedHashMap<>();
    private long cacheBytes;
    private final ScheduledExecutorService cleanup = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "speech-audio-cleanup"); t.setDaemon(true); return t;
    });

    public QuestionSpeechService(InterviewAccess access, SpeechProperties properties, TextToSpeechClient tts) {
        this.access = access; this.properties = properties; this.tts = tts;
        capacity = new Semaphore(properties.getMaxConcurrentTts());
        cleanup.scheduleWithFixedDelay(this::evictExpired, 30, 30, TimeUnit.SECONDS);
    }

    public AudioResult create(UUID student, UUID attempt, UUID promptId) {
        properties.requireConfigured();
        var prompt = access.requirePrompt(student, attempt, promptId);
        if (prompt.text().length() > properties.getMaxTtsCharacters())
            throw new BusinessException("Câu hỏi vượt giới hạn ký tự TTS");
        synchronized (clips) {
            evictExpired();
            for (var entry : clips.entrySet()) {
                Clip c = entry.getValue();
                if (c.student.equals(student) && c.attempt.equals(attempt)
                        && c.prompt.equals(promptId) && c.text.equals(prompt.text())) return response(entry.getKey(), c);
            }
        }
        if (!capacity.tryAcquire())
            throw new BusinessException(HttpStatus.TOO_MANY_REQUESTS, "Dịch vụ TTS đang bận, vui lòng thử lại");
        long start = System.nanoTime();
        try {
            byte[] audio = tts.synthesize(prompt.text());
            if (audio == null || audio.length == 0 || audio.length > properties.getMaxAudioCacheMb() * 1024L * 1024)
                throw new IllegalStateException("Audio TTS không hợp lệ hoặc quá lớn");
            access.requirePrompt(student, attempt, promptId);
            Clip clip = new Clip(student, attempt, promptId, prompt.text(), audio,
                    Instant.now().plusSeconds(properties.getAudioTtlSeconds()));
            UUID id = UUID.randomUUID();
            synchronized (clips) {
                evictExpired();
                var iterator = clips.entrySet().iterator();
                while (cacheBytes + audio.length > properties.getMaxAudioCacheMb() * 1024L * 1024 && iterator.hasNext()) {
                    cacheBytes -= iterator.next().getValue().bytes.length;
                    iterator.remove();
                }
                clips.put(id, clip);
                cacheBytes += audio.length;
            }
            log.info("Speech synthesized questionTurnId={} synthesisMs={} audioBytes={}", promptId,
                    TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start), audio.length);
            return response(id, clip);
        } catch (BusinessException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "Tạo audio đã bị gián đoạn");
        } catch (Exception | LinkageError e) {
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "Không thể tạo giọng nói, vui lòng thử lại");
        } finally { capacity.release(); }
    }

    public byte[] read(UUID student, UUID id) {
        Clip clip;
        synchronized (clips) {
            evictExpired();
            clip = clips.get(id);
        }
        if (clip == null || !clip.student.equals(student))
            throw new BusinessException(HttpStatus.NOT_FOUND, "Audio không tồn tại hoặc đã hết hạn");
        access.requireActiveAttempt(student, clip.attempt);
        return clip.bytes.clone();
    }

    private AudioResult response(UUID id, Clip clip) {
        return new AudioResult(id, "audio/mpeg", clip.expires,
                "/api/interview/speech/audio/" + id);
    }

    private void evictExpired() {
        synchronized (clips) {
            Instant now = Instant.now();
            var iterator = clips.entrySet().iterator();
            while (iterator.hasNext()) {
                Clip clip = iterator.next().getValue();
                if (!clip.expires.isAfter(now)) { cacheBytes -= clip.bytes.length; iterator.remove(); }
            }
        }
    }

    @PreDestroy public void shutdown() {
        cleanup.shutdownNow();
        synchronized (clips) { clips.clear(); cacheBytes = 0; }
    }

    private record Clip(UUID student, UUID attempt, UUID prompt, String text, byte[] bytes, Instant expires) {}
}
