CREATE TABLE course_students (
    course_id UUID NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    student_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    PRIMARY KEY (course_id, student_id)
);
CREATE INDEX idx_course_students_student_id ON course_students(student_id);
