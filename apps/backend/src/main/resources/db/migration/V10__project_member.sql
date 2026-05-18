CREATE TABLE project_member (
    id                  BIGSERIAL PRIMARY KEY,
    project_id          BIGINT NOT NULL REFERENCES project(id) ON DELETE CASCADE,
    workspace_member_id BIGINT NOT NULL REFERENCES workspace_member(id) ON DELETE CASCADE,
    role                VARCHAR(20) NOT NULL,
    assigned_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_project_member UNIQUE (project_id, workspace_member_id)
);
CREATE INDEX ix_project_member_project_id ON project_member (project_id);
