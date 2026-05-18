CREATE TABLE project (
    id                  BIGSERIAL PRIMARY KEY,
    workspace_id        BIGINT NOT NULL REFERENCES workspace(id) ON DELETE CASCADE,
    name                VARCHAR(500) NOT NULL,
    notion_root_page_id VARCHAR(100) NOT NULL,
    created_by          BIGINT NOT NULL,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_project_workspace_root UNIQUE (workspace_id, notion_root_page_id)
);
CREATE INDEX ix_project_workspace_id ON project (workspace_id);
