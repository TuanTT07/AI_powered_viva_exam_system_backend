-- =====================================================================
-- V1: Schema khởi tạo cho AIVES (theo thiết kế DB của nhóm)
-- KHÔNG sửa file này sau khi đã chạy. Muốn đổi bảng -> tạo V3__..., V4__...
-- gen_random_uuid() có sẵn từ PostgreSQL 13+ (compose đang dùng postgres:17)
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. Quản lý học vụ (Academic Management)
-- ---------------------------------------------------------------------
CREATE TABLE roles (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    role_name   VARCHAR(50) NOT NULL UNIQUE, -- ADMIN, LECTURER, STUDENT
    description TEXT
);

CREATE TABLE users (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    role_id       UUID NOT NULL REFERENCES roles(id) ON DELETE RESTRICT,
    user_code     VARCHAR(50) UNIQUE NOT NULL, -- MSSV hoặc mã GV
    full_name     VARCHAR(255) NOT NULL,
    email         VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at    TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE courses (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    course_code VARCHAR(50) UNIQUE NOT NULL,
    course_name VARCHAR(255) NOT NULL,
    department  VARCHAR(255),
    created_at  TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE documents (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    course_id         UUID NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    title             VARCHAR(255) NOT NULL,
    file_url          VARCHAR(500) NOT NULL,
    content_extracted TEXT, -- text bóc tách để AI làm RAG
    created_at        TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- ---------------------------------------------------------------------
-- 2. Ngân hàng câu hỏi & Rubric (Question Bank & Rubric)
-- ---------------------------------------------------------------------
CREATE TABLE rubrics (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    rubric_name VARCHAR(255) NOT NULL,
    description TEXT,
    created_at  TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE rubric_criteria (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    rubric_id      UUID NOT NULL REFERENCES rubrics(id) ON DELETE CASCADE,
    criterion_name VARCHAR(255) NOT NULL,
    description    TEXT,
    max_score      DECIMAL(5,2) NOT NULL,
    created_at     TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE questions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    course_id       UUID NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    rubric_id       UUID REFERENCES rubrics(id) ON DELETE SET NULL,
    created_by      UUID NOT NULL REFERENCES users(id), -- giảng viên tạo
    content         TEXT NOT NULL,
    bloom_level     VARCHAR(50), -- REMEMBER, UNDERSTAND, APPLY, ANALYZE, EVALUATE, CREATE
    is_ai_generated BOOLEAN DEFAULT FALSE,
    status          VARCHAR(50) DEFAULT 'DRAFT', -- DRAFT, APPROVED
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- ---------------------------------------------------------------------
-- 3. Tổ chức thi & hội thoại AI (Exam Execution & AI Conversation)
-- ---------------------------------------------------------------------
CREATE TABLE exams (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    course_id   UUID NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    title       VARCHAR(255) NOT NULL,
    start_time  TIMESTAMP WITH TIME ZONE NOT NULL,
    end_time    TIMESTAMP WITH TIME ZONE NOT NULL,
    exam_config JSONB, -- số câu hỏi, thời gian thi...
    created_at  TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE exam_attempts (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    exam_id          UUID NOT NULL REFERENCES exams(id) ON DELETE CASCADE,
    student_id       UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    start_time       TIMESTAMP WITH TIME ZONE,
    end_time         TIMESTAMP WITH TIME ZONE,
    status           VARCHAR(50) DEFAULT 'SCHEDULED', -- SCHEDULED, IN_PROGRESS, COMPLETED
    audio_record_url VARCHAR(500),
    created_at       TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE question_attempts (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    exam_attempt_id UUID NOT NULL REFERENCES exam_attempts(id) ON DELETE CASCADE,
    question_id     UUID NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
    start_time      TIMESTAMP WITH TIME ZONE,
    end_time        TIMESTAMP WITH TIME ZONE,
    status          VARCHAR(50) DEFAULT 'PENDING',
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE transcript_turns (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    question_attempt_id UUID NOT NULL REFERENCES question_attempts(id) ON DELETE CASCADE,
    turn_order          INT NOT NULL,
    speaker             VARCHAR(50) NOT NULL, -- AI, STUDENT
    text_content        TEXT NOT NULL,
    audio_clip_url      VARCHAR(500),
    turn_type           VARCHAR(50), -- MAIN_QUESTION, FOLLOW_UP, ANSWER
    created_at          TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- ---------------------------------------------------------------------
-- 4. Chấm điểm (Grading)
-- ---------------------------------------------------------------------
CREATE TABLE evaluations (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    question_attempt_id UUID NOT NULL UNIQUE REFERENCES question_attempts(id) ON DELETE CASCADE,
    graded_by           UUID REFERENCES users(id),
    ai_suggested_score  DECIMAL(5,2),
    final_score         DECIMAL(5,2),
    overall_feedback    TEXT,
    evaluation_details  JSONB, -- điểm chi tiết theo từng rubric criterion
    status              VARCHAR(50) DEFAULT 'PENDING_REVIEW', -- PENDING_REVIEW, GRADED
    created_at          TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- ---------------------------------------------------------------------
-- Index cho khoá ngoại (PostgreSQL KHÔNG tự tạo index cho FK,
-- thiếu index thì JOIN / lọc theo FK sẽ chậm khi dữ liệu nhiều)
-- ---------------------------------------------------------------------
CREATE INDEX idx_users_role_id                  ON users(role_id);
CREATE INDEX idx_documents_course_id            ON documents(course_id);
CREATE INDEX idx_rubric_criteria_rubric_id      ON rubric_criteria(rubric_id);
CREATE INDEX idx_questions_course_id            ON questions(course_id);
CREATE INDEX idx_questions_rubric_id            ON questions(rubric_id);
CREATE INDEX idx_questions_created_by           ON questions(created_by);
CREATE INDEX idx_exams_course_id                ON exams(course_id);
CREATE INDEX idx_exam_attempts_exam_id          ON exam_attempts(exam_id);
CREATE INDEX idx_exam_attempts_student_id       ON exam_attempts(student_id);
CREATE INDEX idx_question_attempts_exam_attempt ON question_attempts(exam_attempt_id);
CREATE INDEX idx_question_attempts_question_id  ON question_attempts(question_id);
CREATE INDEX idx_transcript_turns_qa_id         ON transcript_turns(question_attempt_id);
CREATE INDEX idx_evaluations_graded_by          ON evaluations(graded_by);
