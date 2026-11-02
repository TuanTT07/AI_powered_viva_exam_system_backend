package cocxanhcoder.viva.exam.system.speech.service;

import cocxanhcoder.viva.exam.system.interview.dto.InterviewPrompt;
import cocxanhcoder.viva.exam.system.interview.service.InterviewAccess;
import cocxanhcoder.viva.exam.system.common.exception.BusinessException;
import cocxanhcoder.viva.exam.system.speech.config.SpeechProperties;
import cocxanhcoder.viva.exam.system.speech.provider.TextToSpeechClient;
import org.junit.jupiter.api.*;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class QuestionSpeechServiceTest {
    private final InterviewAccess access = mock(InterviewAccess.class);
    private final TextToSpeechClient provider = mock(TextToSpeechClient.class);
    private final SpeechProperties properties = new SpeechProperties();
    private final UUID student = UUID.randomUUID(), attempt = UUID.randomUUID(), prompt = UUID.randomUUID();
    private QuestionSpeechService service;

    @BeforeEach void setup() throws Exception {
        properties.setEnabled(true); properties.setDeepgramApiKey("test");
        when(access.requirePrompt(student, attempt, prompt))
                .thenReturn(new InterviewPrompt(prompt, UUID.randomUUID(), "Question", 60));
        when(provider.synthesize("Question")).thenReturn(new byte[] { 1, 2, 3 });
        service = new QuestionSpeechService(access, properties, provider);
    }

    @AfterEach void close() { service.shutdown(); }

    @Test void cachedAudioIsPrivateAndDoesNotCallProviderAgain() throws Exception {
        var first = service.create(student, attempt, prompt);
        assertEquals(first.audioId(), service.create(student, attempt, prompt).audioId());
        assertArrayEquals(new byte[] {1, 2, 3}, service.read(student, first.audioId()));
        assertThrows(BusinessException.class, () -> service.read(UUID.randomUUID(), first.audioId()));
        verify(provider, times(1)).synthesize("Question");
    }

    @Test void expiredAudioIsUnavailable() {
        properties.setAudioTtlSeconds(-1); // Simulate a clip already expired at read time.
        var clip = service.create(student, attempt, prompt);
        assertThrows(BusinessException.class, () -> service.read(student, clip.audioId()));
    }

    @Test void completedAttemptCannotReadCachedAudio() {
        var clip = service.create(student, attempt, prompt);
        doThrow(new BusinessException("Exam ended")).when(access).requireActiveAttempt(student, attempt);
        assertThrows(BusinessException.class, () -> service.read(student, clip.audioId()));
    }

    @Test void providerErrorDoesNotExposeCredentialsOrCacheAnInvalidClip() throws Exception {
        when(provider.synthesize("Question")).thenThrow(new IllegalStateException("secret credential"));
        BusinessException ex = assertThrows(BusinessException.class, () -> service.create(student, attempt, prompt));
        assertFalse(ex.getMessage().contains("secret credential"));
        doReturn(new byte[] { 4 }).when(provider).synthesize("Question");
        assertNotNull(service.create(student, attempt, prompt));
    }
}
