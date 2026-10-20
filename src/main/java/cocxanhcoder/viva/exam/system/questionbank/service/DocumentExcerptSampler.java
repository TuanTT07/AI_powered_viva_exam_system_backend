package cocxanhcoder.viva.exam.system.questionbank.service;

import cocxanhcoder.viva.exam.system.academic.entity.Document;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/** Samples varied passages from the documents selected for an untargeted question batch. */
final class DocumentExcerptSampler {
    private static final int CHUNK_SIZE = 1200;

    private DocumentExcerptSampler() { }

    static String sample(List<Document> documents, int questionCount) {
        int budget = Math.min(12000, Math.max(6000, questionCount * 600));
        List<List<String>> byDocument = new ArrayList<>();
        for (Document document : documents) {
            String raw = document.getContentExtracted();
            if (raw == null || raw.isBlank()) continue;
            String text = raw.replaceAll("\\s+", " ").trim();
            if (text.length() < 40) continue;
            List<String> chunks = new ArrayList<>();
            for (int start = 0; start < text.length();) {
                int end = Math.min(start + CHUNK_SIZE, text.length());
                if (end < text.length()) {
                    int wordEnd = text.lastIndexOf(' ', end);
                    if (wordEnd > start + 800) end = wordEnd;
                }
                chunks.add("Nguồn: " + document.getTitle() + "\n" + text.substring(start, end).trim());
                start = end;
            }
            Collections.shuffle(chunks, ThreadLocalRandom.current());
            byDocument.add(chunks);
        }

        StringBuilder selected = new StringBuilder();
        for (int round = 0; selected.length() < budget; round++) {
            boolean added = false;
            for (List<String> chunks : byDocument) {
                if (round >= chunks.size()) continue;
                String chunk = chunks.get(round);
                if (selected.length() + chunk.length() > budget && !selected.isEmpty()) continue;
                selected.append(chunk).append("\n\n");
                added = true;
            }
            if (!added) break;
        }
        return selected.toString().trim();
    }
}
