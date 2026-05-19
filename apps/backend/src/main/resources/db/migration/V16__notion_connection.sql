CREATE TABLE notion_connection (
    id                       BIGSERIAL PRIMARY KEY,
    user_id                  BIGINT NOT NULL REFERENCES user_account(id) ON DELETE CASCADE,
    notion_workspace_id       VARCHAR(100) NOT NULL,
    notion_workspace_name     VARCHAR(500) NOT NULL,
    notion_bot_id             VARCHAR(100) NOT NULL,
    access_token_encrypted    TEXT NOT NULL,
    token_type                VARCHAR(50) NOT NULL,
    connected_at              TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked_at                TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uk_notion_connection_user_workspace UNIQUE (user_id, notion_workspace_id)
);

CREATE INDEX ix_notion_connection_notion_workspace_id
    ON notion_connection (notion_workspace_id);

