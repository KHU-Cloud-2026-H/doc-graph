CREATE TABLE project_webhook (
    id         BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL,
    url        TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_project_webhook_project_id UNIQUE (project_id)
);
