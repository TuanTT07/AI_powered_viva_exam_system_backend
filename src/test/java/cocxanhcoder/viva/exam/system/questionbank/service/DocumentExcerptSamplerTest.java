package cocxanhcoder.viva.exam.system.questionbank.service;

import cocxanhcoder.viva.exam.system.academic.entity.Document;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentExcerptSamplerTest {
    @Test
    void samplesOnlySelectedDocuments() {
        Document selected = new Document();
        selected.setTitle("Java OOP");
        selected.setContentExtracted("Encapsulation keeps fields private and exposes controlled access through methods. ".repeat(20));
        String excerpts = DocumentExcerptSampler.sample(List.of(selected), 5);

        assertTrue(excerpts.contains("Nguồn: Java OOP"));
        assertTrue(excerpts.contains("Encapsulation keeps fields private"));
        assertFalse(excerpts.contains("No documents uploaded"));
    }

    @Test
    void rejectsDocumentsWithoutExtractableText() {
        Document imageOnly = new Document();
        imageOnly.setTitle("Scan");
        imageOnly.setContentExtracted("  ");
        assertTrue(DocumentExcerptSampler.sample(List.of(imageOnly), 1).isBlank());
    }
}
