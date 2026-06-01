CREATE TABLE ai_usage_record (
    id                 BIGSERIAL PRIMARY KEY,
    validation_task_id BIGINT NOT NULL,
    project_id         BIGINT NOT NULL,
    model              VARCHAR(100) NOT NULL,
    prompt_tokens      INTEGER NOT NULL,
    completion_tokens  INTEGER NOT NULL,
    total_tokens       INTEGER NOT NULL,
    created_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_ai_usage_record_validation_task_id UNIQUE (validation_task_id)
);

CREATE INDEX idx_ai_usage_record_project_id ON ai_usage_record (project_id);
