-- Additive: legacy transcript rows retain NULL keys.
ALTER TABLE transcript_turns ADD COLUMN answer_id UUID UNIQUE;
ALTER TABLE transcript_turns ADD COLUMN response_to_turn_id UUID UNIQUE
    REFERENCES transcript_turns(id) ON DELETE CASCADE;
