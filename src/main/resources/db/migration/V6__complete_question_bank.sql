-- Module 1 completion: course-scoped rubrics, document indexing and source traceability.
ALTER TABLE rubrics ADD COLUMN course_id UUID REFERENCES courses(id) ON DELETE CASCADE;
-- Existing installations can assign legacy rubrics before enforcing this requirement.
CREATE INDEX IF NOT EXISTS idx_rubrics_course_id ON rubrics(course_id);

ALTER TABLE courses ADD COLUMN openai_vector_store_id VARCHAR(255);

ALTER TABLE documents ADD COLUMN original_filename VARCHAR(500);
ALTER TABLE documents ADD COLUMN stored_filename VARCHAR(500);
ALTER TABLE documents ADD COLUMN content_type VARCHAR(100);
ALTER TABLE documents ADD COLUMN file_size BIGINT;
ALTER TABLE documents ADD COLUMN uploaded_by UUID REFERENCES users(id);
ALTER TABLE documents ADD COLUMN openai_file_id VARCHAR(255);
ALTER TABLE documents ADD COLUMN indexing_status VARCHAR(30) NOT NULL DEFAULT 'PENDING';
ALTER TABLE documents ADD COLUMN indexing_error TEXT;
CREATE INDEX IF NOT EXISTS idx_documents_course_status ON documents(course_id, indexing_status);

CREATE TABLE question_source_documents (
    question_id UUID NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
    document_id UUID NOT NULL REFERENCES documents(id) ON DELETE RESTRICT,
    PRIMARY KEY (question_id, document_id)
);
CREATE INDEX idx_question_source_documents_document ON question_source_documents(document_id);
