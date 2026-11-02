package cocxanhcoder.viva.exam.system.speech.dto;

import java.time.Instant;
import java.util.UUID;

public final class SpeechProtocol {
    private SpeechProtocol() {}
    public record AudioFormat(String encoding, int sampleRate, int channels) {}
    public record ClientEvent(String type, UUID requestId, UUID questionTurnId,
                              UUID answerId, AudioFormat audio) {}
    public record ServerEvent(String type, UUID requestId, UUID answerId, Object data) {}
    public record Ticket(String ticket, Instant expiresAt, String websocketPath) {}
    public record Transcript(String segmentId, long revision, String text) {}
    public record Error(String code, String message, boolean retryable) {}
    public record AudioResult(UUID audioId, String contentType, Instant expiresAt, String audioPath) {}
    public record QuestionPlayback(UUID questionTurnId, String text, String ttsProvider, String language) {}
}
