CREATE TABLE conflict (
    id                BIGSERIAL PRIMARY KEY,
    edge_id           BIGINT NOT NULL,
    first_detected_at TIMESTAMP WITH TIME ZONE NOT NULL,
    last_detected_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    resolved_at       TIMESTAMP WITH TIME ZONE,
    ignored_at        TIMESTAMP WITH TIME ZONE,
    ignored_by        BIGINT,
    ignore_reason     VARCHAR(500)
);
CREATE UNIQUE INDEX uk_conflict_edge_active ON conflict (edge_id) WHERE resolved_at IS NULL;
CREATE INDEX ix_conflict_edge_id ON conflict (edge_id);

CREATE TABLE conflict_finding (
    id                 BIGSERIAL PRIMARY KEY,
    conflict_id        BIGINT NOT NULL REFERENCES conflict(id) ON DELETE CASCADE,
    validation_task_id BIGINT NOT NULL REFERENCES validation_task(id) ON DELETE CASCADE,
    source_block_ids   JSONB NOT NULL,
    target_block_ids   JSONB NOT NULL,
    rationale          TEXT NOT NULL,
    detected_at        TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX ix_conflict_finding_conflict_id ON conflict_finding (conflict_id);
CREATE INDEX ix_conflict_finding_validation_task_id ON conflict_finding (validation_task_id);