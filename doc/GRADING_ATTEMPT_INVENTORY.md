# Complete attempt grading inventory

New endpoints (deploy backend before frontend):

- `GET /api/exams/{examId}/attempts/{attemptId}/grading`: authenticated course manager. Standard success envelope with an ordered array `{ questionOrder, questionStatus, evaluation }`. Every assigned QuestionAttempt is included, including those without a persisted Evaluation. Draft evaluation has null id/final score and the current-run transcript and rubric. Reading inventory never invokes AI or saves an evaluation. Wrong exam/attempt scope is rejected before reading transcripts. Empty assignments return an empty list.
- `GET /api/v1/student/exams/{attemptId}/result-summary`: student JWT owner only. Envelope `{ totalQuestions, questions }`; count is actual assignments, questions includes only published results. No private unpublished transcript or AI score is returned. Other owner/missing attempt returns 404.

Existing explicit per-question AI-grade, final and publish endpoints remain unchanged. Frontend must submit the selected `questionAttemptId`, not an examAttemptId. Drafts can be finalized directly; no AI call is required first.

Verification: full Maven test suite with isolated PostgreSQL 17 and `-Dinterview.pg.test=true` passed 103 tests. The integration regression starts with 3 assigned questions and zero evaluations, reads all 3 without writes/AI, finalizes and publishes each question, then verifies 3 released student results. Includes student-role denial on lecturer inventory and unrelated-owner denial on student summary.

## Publish all results once
`PUT /api/exams/{examId}/attempts/{attemptId}/publish` (no body) checks course manager, exam ownership, completed attempt and every assigned question's finalized evaluation. It locks attempt then question rows and publishes all unpublished evaluations atomically, preserving existing publishedAt on retry. Missing grade/incomplete attempt returns409 with no publication changes. Old per-question publication stays backward compatible. Deploy this endpoint before the corresponding FE action.
