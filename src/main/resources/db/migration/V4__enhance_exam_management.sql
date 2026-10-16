-- =====================================================================
-- V3: Enhance Exam Management & Scheduling Schema
-- Bổ sung các cột phục vụ quản lý kỳ thi, xếp ca thi và giám sát
-- =====================================================================

-- 1. Bổ sung trạng thái và người tạo cho bảng exams
ALTER TABLE exams ADD COLUMN IF NOT EXISTS status VARCHAR(50) DEFAULT 'DRAFT';
ALTER TABLE exams ADD COLUMN IF NOT EXISTS created_by UUID REFERENCES users(id);

-- 2. Bổ sung các cột xếp ca thi cho exam_attempts
ALTER TABLE exam_attempts ADD COLUMN IF NOT EXISTS scheduled_start_time TIMESTAMP WITH TIME ZONE;
ALTER TABLE exam_attempts ADD COLUMN IF NOT EXISTS scheduled_end_time TIMESTAMP WITH TIME ZONE;
ALTER TABLE exam_attempts ADD COLUMN IF NOT EXISTS actual_start_time TIMESTAMP WITH TIME ZONE;
ALTER TABLE exam_attempts ADD COLUMN IF NOT EXISTS actual_end_time TIMESTAMP WITH TIME ZONE;
ALTER TABLE exam_attempts ADD COLUMN IF NOT EXISTS slot_number INT;
ALTER TABLE exam_attempts ADD COLUMN IF NOT EXISTS access_code VARCHAR(50);

-- 3. Bổ sung thứ tự câu hỏi chính trong question_attempts
ALTER TABLE question_attempts ADD COLUMN IF NOT EXISTS question_order INT DEFAULT 1;

-- 4. Tạo Index tối ưu hóa truy vấn ca thi và tìm kiếm
CREATE INDEX IF NOT EXISTS idx_exams_status ON exams(status);
CREATE INDEX IF NOT EXISTS idx_exams_created_by ON exams(created_by);
CREATE INDEX IF NOT EXISTS idx_exam_attempts_scheduled_time ON exam_attempts(scheduled_start_time, scheduled_end_time);
CREATE INDEX IF NOT EXISTS idx_exam_attempts_status ON exam_attempts(status);
CREATE INDEX IF NOT EXISTS idx_exam_attempts_student_exam ON exam_attempts(exam_id, student_id);
