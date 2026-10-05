### 1. Nhóm Quản lý Học vụ (Academic Management)

```sql
-- Bảng Phân quyền (Role)
CREATE TABLE roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    role_name VARCHAR(50) NOT NULL UNIQUE, -- VD: ADMIN, LECTURER, STUDENT
    description TEXT
);

-- Bảng Người dùng (User)
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE RESTRICT,
    user_code VARCHAR(50) UNIQUE NOT NULL, -- MSSV hoặc Mã GV
    full_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Bảng Môn học (Course)
CREATE TABLE courses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    course_code VARCHAR(50) UNIQUE NOT NULL,
    course_name VARCHAR(255) NOT NULL,
    department VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Bảng Tài liệu (Document)
CREATE TABLE documents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    course_id UUID NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    file_url VARCHAR(500) NOT NULL,
    content_extracted TEXT, -- Text bóc tách để AI làm RAG
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

```

### 2. Nhóm Ngân hàng câu hỏi & Rubric (Question Bank & Rubric)

```sql
-- Bảng Bộ tiêu chí (Rubric)
CREATE TABLE rubrics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    rubric_name VARCHAR(255) NOT NULL,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Bảng Tiêu chí thành phần (Rubric Criterion)
CREATE TABLE rubric_criteria (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    rubric_id UUID NOT NULL REFERENCES rubrics(id) ON DELETE CASCADE,
    criterion_name VARCHAR(255) NOT NULL,
    description TEXT,
    max_score DECIMAL(5,2) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Bảng Câu hỏi (Question)
CREATE TABLE questions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    course_id UUID NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    rubric_id UUID REFERENCES rubrics(id) ON DELETE SET NULL,
    created_by UUID NOT NULL REFERENCES users(id), -- Giảng viên tạo
    content TEXT NOT NULL,
    bloom_level VARCHAR(50), -- REMEMBER, UNDERSTAND, APPLY...
    is_ai_generated BOOLEAN DEFAULT FALSE,
    status VARCHAR(50) DEFAULT 'DRAFT', -- DRAFT, APPROVED
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

```

### 3. Nhóm Lõi tổ chức thi & Hội thoại AI (Exam Execution & AI Conversation)

```sql
-- Bảng Kỳ thi (Exam)
CREATE TABLE exams (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    course_id UUID NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    start_time TIMESTAMP WITH TIME ZONE NOT NULL,
    end_time TIMESTAMP WITH TIME ZONE NOT NULL,
    exam_config JSONB, -- Lưu cấu hình phụ như số câu hỏi, thời gian thi...
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Bảng Lượt thi của Sinh viên (Exam Attempt)
CREATE TABLE exam_attempts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    exam_id UUID NOT NULL REFERENCES exams(id) ON DELETE CASCADE,
    student_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    start_time TIMESTAMP WITH TIME ZONE,
    end_time TIMESTAMP WITH TIME ZONE,
    status VARCHAR(50) DEFAULT 'SCHEDULED', -- SCHEDULED, IN_PROGRESS, COMPLETED
    audio_record_url VARCHAR(500), -- Ghi âm toàn bộ buổi thi
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Bảng Phiên hỏi đáp cho 1 câu hỏi (Question Attempt)
CREATE TABLE question_attempts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    exam_attempt_id UUID NOT NULL REFERENCES exam_attempts(id) ON DELETE CASCADE,
    question_id UUID NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
    start_time TIMESTAMP WITH TIME ZONE,
    end_time TIMESTAMP WITH TIME ZONE,
    status VARCHAR(50) DEFAULT 'PENDING',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Bảng Lượt thoại bóc băng (Transcript Turn)
CREATE TABLE transcript_turns (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    question_attempt_id UUID NOT NULL REFERENCES question_attempts(id) ON DELETE CASCADE,
    turn_order INT NOT NULL, -- Thứ tự câu thoại (1, 2, 3...)
    speaker VARCHAR(50) NOT NULL, -- 'AI' hoặc 'STUDENT'
    text_content TEXT NOT NULL,
    audio_clip_url VARCHAR(500), -- Link file audio cắt lẻ của lượt thoại này
    turn_type VARCHAR(50), -- MAIN_QUESTION, FOLLOW_UP, ANSWER
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

```

### 4. Nhóm Chấm điểm (Grading)

```sql
-- Bảng Chấm điểm và Nhận xét (Evaluation)
CREATE TABLE evaluations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    -- Đảm bảo quan hệ 1-1 với Question_Attempt bằng UNIQUE constraint
    question_attempt_id UUID NOT NULL UNIQUE REFERENCES question_attempts(id) ON DELETE CASCADE,
    graded_by UUID REFERENCES users(id), -- Giảng viên chốt điểm
    ai_suggested_score DECIMAL(5,2),
    final_score DECIMAL(5,2),
    overall_feedback TEXT,
    -- JSONB để lưu chi tiết điểm cho từng RubricCriterion mà không cần tạo thêm bảng
    evaluation_details JSONB,
    status VARCHAR(50) DEFAULT 'PENDING_REVIEW', -- PENDING_REVIEW, GRADED
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

```
