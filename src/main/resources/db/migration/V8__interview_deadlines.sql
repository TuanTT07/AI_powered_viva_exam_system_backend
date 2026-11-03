ALTER TABLE transcript_turns ADD COLUMN deadline_at TIMESTAMPTZ;
ALTER TABLE transcript_turns ADD COLUMN timed_out_at TIMESTAMPTZ;
