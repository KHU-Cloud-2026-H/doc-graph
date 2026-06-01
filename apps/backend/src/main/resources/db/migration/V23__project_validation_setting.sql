CREATE TABLE project_validation_setting (
    id         BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL,
    enabled    BOOLEAN NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_project_validation_setting_project_id UNIQUE (project_id)
);