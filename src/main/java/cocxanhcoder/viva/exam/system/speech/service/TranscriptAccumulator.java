package cocxanhcoder.viva.exam.system.speech.service;

import java.util.TreeMap;

/** Offsets from the recognizer are stable segment identifiers, measured from the stream start. */
public class TranscriptAccumulator {
    private final TreeMap<Long, String> finals = new TreeMap<>();
    private int characters;

    public synchronized void accept(String segment, String text) {
        if (text == null || text.isBlank()) return;
        long offset = Long.parseLong(segment);
        String value = text.strip();
        String old = finals.get(offset);
        int next = characters - (old == null ? 0 : old.length()) + value.length();
        if (next > 50000) throw new IllegalStateException("Transcript quá dài");
        finals.put(offset, value);
        characters = next;
    }

    public synchronized String text() { return String.join(" ", finals.values()); }
}
