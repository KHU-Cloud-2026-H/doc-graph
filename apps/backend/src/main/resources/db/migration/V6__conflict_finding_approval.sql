ALTER TABLE conflict_finding
    ADD COLUMN approved_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN approved_by BIGINT;
