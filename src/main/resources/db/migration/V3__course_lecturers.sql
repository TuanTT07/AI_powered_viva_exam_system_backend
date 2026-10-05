-- =====================================================================
-- V3: Phân công giảng viên phụ trách môn học (quan hệ N-N users <-> courses)
-- 1 môn có nhiều giảng viên, 1 giảng viên dạy nhiều môn.
-- Xoá môn / xoá user thì chỉ xoá dòng phân công, không ảnh hưởng dữ liệu khác.
-- =====================================================================
CREATE TABLE course_lecturers (
    course_id   UUID NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    lecturer_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    assigned_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (course_id, lecturer_id) -- chặn phân công trùng
);

-- PK đã có index (course_id, lecturer_id); thêm index để tìm "các môn của 1 giảng viên"
CREATE INDEX idx_course_lecturers_lecturer_id ON course_lecturers(lecturer_id);
