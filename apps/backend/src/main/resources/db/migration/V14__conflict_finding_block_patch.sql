ALTER TABLE conflict_finding DROP COLUMN target_block_ids;
ALTER TABLE conflict_finding DROP COLUMN suggestion;
ALTER TABLE conflict_finding ADD COLUMN target_block_id TEXT NOT NULL;
ALTER TABLE conflict_finding ADD COLUMN new_text TEXT NOT NULL;
