CREATE TABLE workspace (
    id                    BIGSERIAL PRIMARY KEY,
    notion_workspace_id   VARCHAR(100) NOT NULL,
    notion_workspace_name VARCHAR(500) NOT NULL,
    notion_bot_id         VARCHAR(100) NOT NULL,
    created_by            BIGINT NOT NULL,
    created_at            TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_workspace_notion_workspace_id UNIQUE (notion_workspace_id)
);

CREATE TABLE workspace_member (
    id           BIGSERIAL PRIMARY KEY,
    workspace_id BIGINT NOT NULL REFERENCES workspace(id) ON DELETE CASCADE,
    user_id    BIGINT NOT NULL,
    joined_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_workspace_member_workspace_user UNIQUE (workspace_id, user_id)
);
CREATE INDEX ix_workspace_member_workspace_id ON workspace_member (workspace_id);