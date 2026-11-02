package cocxanhcoder.viva.exam.system.speech.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TranscriptAccumulatorTest {
    @Test void ordersByNumericOffsetAndReplacesDuplicateSegments() {
        var transcript = new TranscriptAccumulator();
        transcript.accept("20", "world");
        transcript.accept("3", "Hello");
        transcript.accept("20", "world!");
        transcript.accept("30", "  ");
        assertEquals("Hello world!", transcript.text());
    }

    @Test void boundsTranscriptMemory() {
        var transcript = new TranscriptAccumulator();
        transcript.accept("0", "a".repeat(50000));
        assertThrows(IllegalStateException.class, () -> transcript.accept("1", "b"));
        assertEquals(50000, transcript.text().length());
    }
}
