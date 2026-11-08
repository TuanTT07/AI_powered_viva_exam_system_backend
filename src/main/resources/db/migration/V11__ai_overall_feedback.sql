ALTER TABLE evaluations ADD COLUMN ai_overall_feedback TEXT;

-- Backfill: evaluations chưa được giảng viên chốt chỉ có nhận xét do AI sinh ra trong overall_feedback.
UPDATE evaluations SET ai_overall_feedback = overall_feedback WHERE graded_by IS NULL;
