package cocxanhcoder.viva.exam.system.speech.service;

import cocxanhcoder.viva.exam.system.interview.dto.AnswerReceipt;
import cocxanhcoder.viva.exam.system.interview.dto.InterviewPrompt;
import cocxanhcoder.viva.exam.system.interview.service.InterviewAnswers;
import cocxanhcoder.viva.exam.system.interview.service.InterviewAccess;
import cocxanhcoder.viva.exam.system.speech.config.SpeechProperties;
import cocxanhcoder.viva.exam.system.speech.dto.SpeechProtocol.*;
import cocxanhcoder.viva.exam.system.speech.provider.SpeechToTextClient;
import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.*;
import org.springframework.web.socket.*;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SpeechSessionServiceTest {
    private final InterviewAccess access = mock(InterviewAccess.class);
    private final InterviewAnswers transcripts = mock(InterviewAnswers.class);
    private final SpeechProperties properties = new SpeechProperties();
    private final FakeStt provider = new FakeStt();
    private final UUID student = UUID.randomUUID(), attempt = UUID.randomUUID(), prompt = UUID.randomUUID();
    private final UUID question = UUID.randomUUID();
    private final List<JsonNode> messages = new CopyOnWriteArrayList<>();
    private final CountDownLatch finalized = new CountDownLatch(1);
    private final CountDownLatch failed = new CountDownLatch(1);
    private final ObjectMapper json = new ObjectMapper();
    private SpeechSessionService service;
    private WebSocketSession socket;
    private final UUID startRequest = UUID.randomUUID();

    @BeforeEach void setup() throws Exception {
        properties.setEnabled(true); properties.setDeepgramApiKey("test");
        service = new SpeechSessionService(properties, access, transcripts, provider);
        socket = socket("socket-1");
        when(access.requirePrompt(student, attempt, prompt))
                .thenReturn(new InterviewPrompt(prompt, question, "Question?", 60));
        when(access.beginAnswer(eq(student), eq(attempt), eq(prompt), eq(question), anyInt()))
                .thenAnswer(call -> Instant.now().plusSeconds(call.<Integer>getArgument(4)));
        when(transcripts.complete(eq(student), eq(attempt), eq(prompt), eq(question), any(), anyString(), any(), any()))
                .thenAnswer(i -> new AnswerReceipt(UUID.randomUUID(), i.getArgument(5)));
        service.connect(socket, grant(attempt));
    }

    @AfterEach void close() { provider.finishGate.countDown(); service.shutdown(); }

    private WebSocketSession socket(String id) throws Exception {
        WebSocketSession ws = mock(WebSocketSession.class);
        when(ws.getId()).thenReturn(id); when(ws.isOpen()).thenReturn(true);
        doAnswer(i -> {
            JsonNode msg = json.readTree(((TextMessage) i.getArgument(0)).getPayload());
            messages.add(msg);
            if (msg.path("type").asText().equals("answer.finalized")) finalized.countDown();
            if (msg.path("type").asText().equals("speech.error")) failed.countDown();
            return null;
        }).when(ws).sendMessage(any());
        return ws;
    }

    private SpeechTicketService.Grant grant(UUID attemptId) {
        return new SpeechTicketService.Grant(student, attemptId, Instant.now().plusSeconds(30), Instant.now().plusSeconds(600));
    }

    private void start() {
        service.command("socket-1", new ClientEvent("answer.start", startRequest, prompt, null,
                new AudioFormat("PCM_S16LE", 16000, 1)));
    }

    private UUID answer() {
        return UUID.fromString(messages.stream().filter(m -> m.path("type").asText().equals("answer.started"))
                .findFirst().orElseThrow().path("answerId").asText());
    }

    private void end() {
        service.command("socket-1", new ClientEvent("answer.end", UUID.randomUUID(), null, answer(), null));
    }

    @Test void partialIsNotPersistedAndFinalizationDrainsAllFinalSegmentsOnce() throws Exception {
        start();
        provider.listener.partial("0", "temporary");
        service.audio("socket-1", new byte[640]);
        verifyNoInteractions(transcripts);
        provider.listener.recognized("20", "world");
        provider.listener.recognized("0", "Hello");
        provider.listener.recognized("20", "world!");
        end();
        assertTrue(finalized.await(3, TimeUnit.SECONDS));
        end(); // Repeated end is acknowledged without saving twice.
        verify(transcripts, times(1)).complete(eq(student), eq(attempt), eq(prompt), eq(question),
                eq(answer()), eq("Hello world! tail"), any(), any());
        assertEquals(640, provider.audioBytes.get());
    }

    @Test void concurrentEndAndAuthoritativeTimerFinalizeOnlyOnce() throws Exception {
        provider.blockFinish = true;
        start(); end();
        assertTrue(provider.finishEntered.await(3, TimeUnit.SECONDS));
        end(); service.finishCurrentAnswer(attempt, prompt);
        provider.finishGate.countDown();
        assertTrue(finalized.await(3, TimeUnit.SECONDS));
        assertEquals(1, provider.finishCalls.get());
        verify(transcripts, times(1)).complete(any(), any(), any(), any(), any(), anyString(), any(), any());
    }

    @Test void disconnectedUnfinishedAnswerIsNotSavedAndProviderIsDisposed() throws Exception {
        start();
        service.disconnect("socket-1");
        assertTrue(provider.closed.await(3, TimeUnit.SECONDS));
        provider.listener.recognized("0", "late callback");
        verifyNoInteractions(transcripts);
        WebSocketSession next = socket("socket-2");
        service.connect(next, grant(attempt));
        verify(next, never()).close(any());
        assertEquals(1, provider.closeCalls.get());
    }

    @Test void secondConnectionForSameAttemptIsRejected() throws Exception {
        WebSocketSession duplicate = socket("duplicate");
        service.connect(duplicate, grant(attempt));
        verify(duplicate).close(argThat(s -> s.getCode() == 1008));
    }

    @Test void providerCapacityIsNotExceeded() throws Exception {
        start();
        UUID anotherAttempt = UUID.randomUUID();
        service.connect(socket("socket-2"), grant(anotherAttempt));
        when(access.requirePrompt(student, anotherAttempt, prompt))
                .thenReturn(new InterviewPrompt(prompt, question, "Question?", 60));
        service.command("socket-2", new ClientEvent("answer.start", UUID.randomUUID(), prompt, null,
                new AudioFormat("PCM_S16LE", 16000, 1)));
        assertEquals(1, provider.openCalls.get());
        assertTrue(messages.stream().anyMatch(m -> m.path("data").path("code").asText().equals("STT_BUSY")));
    }

    @Test void noSpeechDoesNotCreateAnEmptyAnswer() throws Exception {
        provider.includeTail = false;
        start(); end();
        assertTrue(failed.await(3, TimeUnit.SECONDS));
        verifyNoInteractions(transcripts);
        assertTrue(messages.stream().anyMatch(m -> m.path("data").path("code").asText().equals("NO_SPEECH")));
    }

    @Test void duplicateStartDoesNotOpenAnotherStreamAndOddPcmIsRejected() throws Exception {
        start(); start();
        assertEquals(1, provider.openCalls.get());
        service.audio("socket-1", new byte[3]);
        assertTrue(failed.await(3, TimeUnit.SECONDS));
        verifyNoInteractions(transcripts);
        assertEquals(0, provider.audioBytes.get());
    }

    @Test void discardingKeepsSocketAndDoesNotPersistTheOldRecording() throws Exception {
        Instant deadline = Instant.now().plusSeconds(40);
        when(access.beginAnswer(student, attempt, prompt, question, 60)).thenReturn(deadline);
        start();
        UUID oldAnswer = answer();
        provider.listener.recognized("0", "Discard this");
        UUID request = UUID.randomUUID();
        service.command("socket-1", new ClientEvent("answer.discard", request, null, oldAnswer, null));
        org.awaitility.Awaitility.await().atMost(java.time.Duration.ofSeconds(3)).until(() ->
                messages.stream().anyMatch(m -> m.path("type").asText().equals("answer.discarded")
                        && m.path("requestId").asText().equals(request.toString())));
        verifyNoInteractions(transcripts);
        service.command("socket-1", new ClientEvent("answer.start", UUID.randomUUID(), prompt, null,
                new AudioFormat("PCM_S16LE", 16000, 1)));
        var starts = messages.stream().filter(m -> m.path("type").asText().equals("answer.started")).toList();
        assertEquals(2, starts.size());
        assertNotEquals(oldAnswer.toString(), starts.getLast().path("answerId").asText());
        assertEquals(deadline.toString(), starts.getLast().path("data").path("deadline").asText());
    }

    @Test void reconnectKeepsTheOriginalPromptDeadline() throws Exception {
        Instant deadline = Instant.now().plusSeconds(20);
        when(access.beginAnswer(student, attempt, prompt, question, 60)).thenReturn(deadline);
        when(access.requirePrompt(student, attempt, prompt))
                .thenReturn(new InterviewPrompt(prompt, question, "Question?", 60, deadline));
        start();
        service.disconnect("socket-1");
        assertTrue(provider.closed.await(3, TimeUnit.SECONDS));
        service.connect(socket("socket-2"), grant(attempt));
        service.command("socket-2", new ClientEvent("answer.start", UUID.randomUUID(), prompt, null,
                new AudioFormat("PCM_S16LE", 16000, 1)));

        var starts = messages.stream().filter(m -> m.path("type").asText().equals("answer.started")).toList();
        assertEquals(2, starts.size());
        starts.forEach(m -> assertEquals(deadline.toString(), m.path("data").path("deadline").asText()));
    }

    @Test void invalidFormatCannotOpenProvider() {
        service.command("socket-1", new ClientEvent("answer.start", startRequest, prompt, null,
                new AudioFormat("webm", 48000, 2)));
        assertEquals(0, provider.openCalls.get());
    }

    private static class FakeStt implements SpeechToTextClient {
        volatile Listener listener;
        volatile boolean blockFinish, includeTail = true;
        final AtomicInteger openCalls = new AtomicInteger(), finishCalls = new AtomicInteger();
        final AtomicInteger closeCalls = new AtomicInteger(), audioBytes = new AtomicInteger();
        final CountDownLatch finishEntered = new CountDownLatch(1), finishGate = new CountDownLatch(1);
        final CountDownLatch closed = new CountDownLatch(1);
        @Override public RecognitionSession open(Listener listener) {
            this.listener = listener; openCalls.incrementAndGet();
            return new RecognitionSession() {
                @Override public void write(byte[] pcm) { audioBytes.addAndGet(pcm.length); }
                @Override public void finish() throws Exception {
                    finishCalls.incrementAndGet(); finishEntered.countDown();
                    if (blockFinish) assertTrue(finishGate.await(3, TimeUnit.SECONDS));
                    if (includeTail) listener.recognized("100", "tail");
                }
                @Override public void close() { closeCalls.incrementAndGet(); closed.countDown(); }
            };
        }
    }
}
