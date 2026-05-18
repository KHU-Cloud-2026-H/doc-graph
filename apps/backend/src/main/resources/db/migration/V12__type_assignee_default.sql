CREATE TABLE type_assignee_default (
    id                              BIGSERIAL PRIMARY KEY,
    project_id                      BIGINT NOT NULL REFERENCES project(id) ON DELETE CASCADE,
    document_type                   VARCHAR(50) NOT NULL,
    assignee_workspace_member_id    BIGINT REFERENCES workspace_member(id) ON DELETE SET NULL,
    assigned_at                     TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_type_assignee UNIQUE (project_id, document_type)
);
CREATE INDEX ix_type_assignee_project_id ON type_assignee_default (project_id);
