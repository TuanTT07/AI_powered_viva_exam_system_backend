package cocxanhcoder.viva.exam.system.questionbank.service;

import cocxanhcoder.viva.exam.system.academic.entity.Course;
import cocxanhcoder.viva.exam.system.academic.entity.Document;
import cocxanhcoder.viva.exam.system.academic.entity.DocumentIndexStatus;
import cocxanhcoder.viva.exam.system.academic.repository.CourseRepository;
import cocxanhcoder.viva.exam.system.academic.repository.DocumentRepository;
import cocxanhcoder.viva.exam.system.common.exception.BusinessException;
import cocxanhcoder.viva.exam.system.questionbank.dto.GenerateQuestionsRequest;
import cocxanhcoder.viva.exam.system.questionbank.entity.BloomLevel;
import cocxanhcoder.viva.exam.system.questionbank.repository.RubricRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class QuestionGenerationServiceTest {
    private final CourseAccessService access = mock(CourseAccessService.class);
    private final CourseRepository courses = mock(CourseRepository.class);
    private final DocumentRepository documents = mock(DocumentRepository.class);
    private final RubricRepository rubrics = mock(RubricRepository.class);
    private final AiClient ai = mock(AiClient.class);
    private final QuestionService questions = mock(QuestionService.class);
    private final QuestionGenerationService service = new QuestionGenerationService(
            access, courses, documents, rubrics, ai, questions);

    @Test
    void missingTopicSamplesSelectedReadyDocument() {
        UUID actor = UUID.randomUUID(), courseId = UUID.randomUUID(), documentId = UUID.randomUUID();
        Course course = new Course();
        course.setId(courseId);
        course.setOpenAiVectorStoreId("store-test");
        Document document = new Document();
        document.setId(documentId);
        document.setCourse(course);
        document.setTitle("Java OOP");
        document.setIndexingStatus(DocumentIndexStatus.READY);
        document.setContentExtracted("Encapsulation keeps fields private and exposes controlled access through methods. ".repeat(20));
        when(courses.findById(courseId)).thenReturn(Optional.of(course));
        when(documents.findById(documentId)).thenReturn(Optional.of(document));
        when(ai.generateQuestions(eq("store-test"), eq(null), eq(1), eq(List.of("UNDERSTAND")), any(String.class)))
                .thenReturn(List.of(new AiClient.GeneratedQuestion("Giải thích cơ chế đóng gói dữ liệu trong Java?", "UNDERSTAND")));

        service.generate(actor, courseId, new GenerateQuestionsRequest(1, null, List.of(BloomLevel.UNDERSTAND), null,
                List.of(documentId)));

        verify(ai).generateQuestions(eq("store-test"), eq(null), eq(1), eq(List.of("UNDERSTAND")),
                org.mockito.ArgumentMatchers.argThat(text -> text.contains("Nguồn: Java OOP")
                        && text.contains("Encapsulation keeps fields private")));
        verify(questions).createDraft(eq(actor), eq(courseId), any(String.class), eq(BloomLevel.UNDERSTAND),
                eq(null), eq(true), any());
    }

    @Test
    void missingTopicWithoutExtractableTextDoesNotCallAi() {
        UUID actor = UUID.randomUUID(), courseId = UUID.randomUUID();
        Course course = new Course();
        course.setId(courseId);
        course.setOpenAiVectorStoreId("store-test");
        Document document = new Document();
        document.setCourse(course);
        document.setIndexingStatus(DocumentIndexStatus.READY);
        document.setContentExtracted("  ");
        when(courses.findById(courseId)).thenReturn(Optional.of(course));
        when(documents.findByCourseIdOrderByCreatedAtDesc(courseId)).thenReturn(List.of(document));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.generate(actor, courseId,
                new GenerateQuestionsRequest(1, null, null, null, null)));

        assertEquals(422, ex.getStatus().value());
        verifyNoInteractions(ai, questions);
    }
}
