package cocxanhcoder.viva.exam.system.interview.service;

import cocxanhcoder.viva.exam.system.common.exception.BusinessException;
import cocxanhcoder.viva.exam.system.interview.dto.AnswerReceipt;
import cocxanhcoder.viva.exam.system.interview.dto.InterviewState.Status;
import cocxanhcoder.viva.exam.system.questionbank.service.AiClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.config.import=optional:classpath:/interview-test-empty.properties")
@AutoConfigureMockMvc
@EnabledIfSystemProperty(named = "interview.pg.test", matches = "true")
class InterviewPostgresTest {
    @Autowired InterviewFlow flow;
    @Autowired TranscriptService transcripts;
    @Autowired JdbcTemplate db;
    @Autowired MockMvc mvc;
    @MockitoBean AiClient ai;

    private final UUID student = UUID.randomUUID();
    private final UUID otherStudent = UUID.randomUUID();
    private final UUID course = UUID.randomUUID();
    private final UUID exam = UUID.randomUUID();
    private final UUID attempt = UUID.randomUUID();

    @AfterEach
    void cleanUp() {
        db.update("delete from courses where id = ?", course);
        db.update("delete from users where id in (?, ?)", student, otherStudent);
    }

    @Test
    void concurrentStartAndDuplicateAnswerAdvanceExactlyOnce() throws Exception {
        seed(2, 0);
        assertThrows(BusinessException.class, () -> flow.start(otherStudent, attempt));
        db.update("update exam_attempts set status = 'SCHEDULED' where id = ?", attempt);
        assertThrows(BusinessException.class, () -> flow.start(student, attempt));
        db.update("update exam_attempts set status = 'READY' where id = ?", attempt);
        assertEquals(Status.READY, flow.current(student, attempt).status());

        var gate = new CountDownLatch(1);
        try (var pool = Executors.newVirtualThreadPerTaskExecutor()) {
            var first = pool.submit(() -> { gate.await(); return flow.start(student, attempt); });
            var second = pool.submit(() -> { gate.await(); return flow.start(student, attempt); });
            gate.countDown();
            var prompt = first.get(10, TimeUnit.SECONDS).prompt();
            assertEquals(prompt, second.get(10, TimeUnit.SECONDS).prompt());
            assertEquals("Main 1", prompt.text());
            assertEquals(prompt, flow.current(student, attempt).prompt());
            UUID answer = UUID.randomUUID();
            assertEquals(submit(prompt.questionAttemptId(), prompt.questionTurnId(), answer),
                    submit(prompt.questionAttemptId(), prompt.questionTurnId(), answer));
        }

        await().atMost(Duration.ofSeconds(8)).untilAsserted(() -> {
            var state = flow.current(student, attempt);
            assertEquals(Status.AWAITING_ANSWER, state.status());
            assertEquals("Main 2", state.prompt().text());
        });
        var prompt = flow.current(student, attempt).prompt();
        submit(prompt.questionAttemptId(), prompt.questionTurnId(), UUID.randomUUID());
        awaitCompleted();
        assertEquals(4, db.queryForObject("""
                select count(*) from transcript_turns t join question_attempts q on t.question_attempt_id = q.id
                where q.exam_attempt_id = ?
                """, Integer.class, attempt));
        verifyNoInteractions(ai);
    }

    @Test
    void schedulerRecoversCommittedAnswerAndEnforcesFollowUpLimit() {
        seed(1, 1);
        when(ai.suggestFollowUp(anyString())).thenReturn("What mechanism makes that happen?");
        var prompt = flow.start(student, attempt).prompt();
        // Simulate a committed answer with no in-process event after a restart.
        db.update("""
                insert into transcript_turns
                (question_attempt_id, turn_order, speaker, text_content, turn_type, answer_id, response_to_turn_id)
                values (?, 2, 'STUDENT', 'An explanation', 'ANSWER', ?, ?)
                """, prompt.questionAttemptId(), UUID.randomUUID(), prompt.questionTurnId());
        await().atMost(Duration.ofSeconds(8)).untilAsserted(() -> {
            var state = flow.current(student, attempt);
            assertEquals(Status.AWAITING_ANSWER, state.status());
            assertEquals("What mechanism makes that happen?", state.prompt().text());
        });
        var followUp = flow.current(student, attempt).prompt();
        assertEquals(prompt.questionAttemptId(), followUp.questionAttemptId());
        submit(followUp.questionAttemptId(), followUp.questionTurnId(), UUID.randomUUID());
        awaitCompleted();
        verify(ai).suggestFollowUp(contains("An explanation"));
    }

    @Test
    void expiredPromptAllowsFinalizationDuringGraceThenSkipsUnansweredQuestion() {
        seed(2, 1);
        when(ai.suggestFollowUp(anyString())).thenThrow(new IllegalStateException("provider failed"));
        var first = flow.start(student, attempt).prompt();
        expire(first.questionTurnId(), 1);
        assertEquals(Status.PROCESSING, flow.current(student, attempt).status());
        submit(first.questionAttemptId(), first.questionTurnId(), UUID.randomUUID());
        await().atMost(Duration.ofSeconds(8)).untilAsserted(() -> {
            var state = flow.current(student, attempt);
            assertEquals(Status.AWAITING_ANSWER, state.status());
            assertEquals("Main 2", state.prompt().text());
        });
        var last = flow.current(student, attempt).prompt();
        expire(last.questionTurnId(), 11);
        assertThrows(BusinessException.class,
                () -> submit(last.questionAttemptId(), last.questionTurnId(), UUID.randomUUID()));
        awaitCompleted();
        assertEquals(1, db.queryForObject(
                "select count(*) from transcript_turns where id = ? and timed_out_at is not null",
                Integer.class, last.questionTurnId()));
    }

    @Test
    void aiBudgetExpiresAndLateResultCannotOpenAnotherTurn() {
        seed(1, 1);
        when(ai.suggestFollowUp(anyString())).thenAnswer(call -> {
            Thread.sleep(Duration.ofSeconds(30));
            return "Late follow-up";
        });
        var prompt = flow.start(student, attempt).prompt();
        submit(prompt.questionAttemptId(), prompt.questionTurnId(), UUID.randomUUID());
        await().atMost(Duration.ofSeconds(15))
                .untilAsserted(() -> assertEquals(Status.COMPLETED, flow.current(student, attempt).status()));
        assertEquals(2, db.queryForObject(
                "select count(*) from transcript_turns where question_attempt_id = ?",
                Integer.class, prompt.questionAttemptId()));
    }

    @Test
    void normalizedDuplicateQuestionIsDiscarded() {
        seed(1, 1);
        when(ai.suggestFollowUp(anyString())).thenReturn(" MAIN   1?! ");
        var prompt = flow.start(student, attempt).prompt();
        submit(prompt.questionAttemptId(), prompt.questionTurnId(), UUID.randomUUID());
        awaitCompleted();
        assertEquals(2, db.queryForObject(
                "select count(*) from transcript_turns where question_attempt_id = ?",
                Integer.class, prompt.questionAttemptId()));
    }

    @Test
    void httpEndpointsRequireStudentJwtAndReturnOnlyTheActivePrompt() throws Exception {
        seed(1, 0);
        String path = "/api/interviews/" + attempt;
        mvc.perform(get(path + "/current")).andExpect(status().isUnauthorized());
        mvc.perform(post(path + "/start").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_LECTURER"))))
                .andExpect(status().isForbidden());
        var owner = jwt().jwt(token -> token.subject(student.toString()))
                .authorities(new SimpleGrantedAuthority("ROLE_STUDENT"));
        mvc.perform(post(path + "/start").with(owner))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.data.status").value("AWAITING_ANSWER"))
                .andExpect(jsonPath("$.data.prompt.text").value("Main 1"))
                .andExpect(jsonPath("$.data.prompt.rubric").doesNotExist());
        var prompt = flow.current(student, attempt).prompt();
        mvc.perform(get(path + "/current").with(owner))
                .andExpect(jsonPath("$.data.prompt.questionTurnId").value(prompt.questionTurnId().toString()))
                .andExpect(jsonPath("$.data.prompt.deadlineAt").exists());
    }

    @Test
    void studentScheduleUsesJwtForListAndLegacySlotLookup() throws Exception {
        seed(1, 0);
        String path = "/api/v1/student/exams";
        var owner = jwt().jwt(token -> token.subject(student.toString()))
                .authorities(new SimpleGrantedAuthority("ROLE_STUDENT"));
        var other = jwt().jwt(token -> token.subject(otherStudent.toString()))
                .authorities(new SimpleGrantedAuthority("ROLE_STUDENT"));
        mvc.perform(get(path).with(owner).param("studentId", otherStudent.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].attemptId").value(attempt.toString()))
                .andExpect(jsonPath("$.content[0].questions").doesNotExist());
        mvc.perform(get(path).with(owner).param("status", "COMPLETED"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content").isEmpty());
        mvc.perform(get(path).with(other).param("studentId", student.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content").isEmpty());
        mvc.perform(get(path + "/" + exam + "/my-slot").with(other).param("studentId", student.toString()))
                .andExpect(status().isNotFound());
    }

    private void awaitCompleted() {
        await().atMost(Duration.ofSeconds(8))
                .untilAsserted(() -> assertEquals(Status.COMPLETED, flow.current(student, attempt).status()));
    }

    private void expire(UUID turn, int secondsAgo) {
        db.update("update transcript_turns set deadline_at = now() - (? * interval '1 second') where id = ?",
                secondsAgo, turn);
    }

    private AnswerReceipt submit(UUID question, UUID turn, UUID answer) {
        return transcripts.complete(student, attempt, turn, question, answer, "A response", Instant.now(), Instant.now());
    }

    private void seed(int count, int maxFollowUps) {
        for (UUID user : new UUID[] {student, otherStudent}) {
            db.update("""
                    insert into users (id, role_id, user_code, full_name, email, password_hash, must_change_password)
                    select ?, id, ?, 'Test student', ?, 'unused', false from roles where role_name = 'STUDENT'
                    """, user, user.toString(), user + "@interview.test");
        }
        db.update("insert into courses (id, course_code, course_name) values (?, ?, 'Test course')",
                course, course.toString());
        db.update("""
                insert into exams (id, course_id, title, start_time, end_time, exam_config)
                values (?, ?, 'Test exam', now(), now() + interval '1 hour',
                        jsonb_build_object('maxFollowUpQuestions', cast(? as integer), 'timeLimitPerTurnSeconds', 60))
                """, exam, course, maxFollowUps);
        db.update("insert into exam_attempts (id, exam_id, student_id, status) values (?, ?, ?, 'READY')",
                attempt, exam, student);
        for (int index = count; index > 0; index--) {
            UUID question = UUID.randomUUID();
            db.update("insert into questions (id, course_id, created_by, content) values (?, ?, ?, ?)",
                    question, course, student, "Main " + index);
            db.update("insert into question_attempts (exam_attempt_id, question_id, question_order) values (?, ?, ?)",
                    attempt, question, index);
        }
    }
}
