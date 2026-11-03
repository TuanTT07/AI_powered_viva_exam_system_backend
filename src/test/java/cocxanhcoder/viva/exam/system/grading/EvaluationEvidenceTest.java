package cocxanhcoder.viva.exam.system.grading;

import cocxanhcoder.viva.exam.system.exammgt.entity.*;
import cocxanhcoder.viva.exam.system.grading.entity.Evaluation;
import cocxanhcoder.viva.exam.system.grading.mapper.EvaluationMapper;
import cocxanhcoder.viva.exam.system.interview.entity.*;
import cocxanhcoder.viva.exam.system.interview.repository.TranscriptTurnRepository;
import cocxanhcoder.viva.exam.system.questionbank.entity.Question;
import org.junit.jupiter.api.Test;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EvaluationEvidenceTest {
    @Test
    void evidenceContainsOnlyCurrentRunAndRecording() {
        var repository = mock(TranscriptTurnRepository.class);
        var attempt = new ExamAttempt();
        attempt.setAudioRecordUrl("/recordings/exam.wav");
        var qa = new QuestionAttempt();
        qa.setId(UUID.randomUUID());
        qa.setExamAttempt(attempt);
        qa.setQuestion(new Question());
        qa.setStartTime(OffsetDateTime.now());
        var old = new TranscriptTurn();
        old.setCreatedAt(qa.getStartTime().minusSeconds(1));
        var current = new TranscriptTurn();
        current.setId(UUID.randomUUID());
        current.setTurnOrder(2);
        current.setSpeaker(Speaker.STUDENT);
        current.setTextContent("Answer");
        current.setAudioClipUrl("/recordings/answer.wav");
        current.setCreatedAt(qa.getStartTime().plusSeconds(1));
        when(repository.findByQuestionAttemptIdOrderByTurnOrderAsc(qa.getId())).thenReturn(List.of(old, current));
        var evaluation = new Evaluation();
        evaluation.setQuestionAttempt(qa);
        var response = new EvaluationMapper(repository).toResponse(evaluation);
        assertEquals("/recordings/exam.wav", response.audioUrl());
        assertEquals(1, response.transcript().size());
        assertEquals("Answer", response.transcript().getFirst().textContent());
        assertEquals("/recordings/answer.wav", response.transcript().getFirst().audioUrl());
    }
}
