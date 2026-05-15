CREATE TABLE document (
    id                       BIGSERIAL PRIMARY KEY,
    project_id               BIGINT NOT NULL,
    notion_page_id           VARCHAR(100) NOT NULL,
    parent_notion_page_id    VARCHAR(100),
    title                    VARCHAR(500) NOT NULL,
    type                     VARCHAR(40),
    assignee_member_id       BIGINT,
    raw_content              JSONB,
    flat_text                TEXT,
    notion_created_by        VARCHAR(100),
    notion_last_edited_by    VARCHAR(100),
    notion_last_edited_at    TIMESTAMP WITH TIME ZONE,
    synced_at                TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at               TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at               TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE block (
    id               BIGSERIAL PRIMARY KEY,
    document_id      BIGINT NOT NULL REFERENCES document(id) ON DELETE CASCADE,
    notion_block_id  VARCHAR(100) NOT NULL,
    parent_type      VARCHAR(50),
    parent_id        VARCHAR(100),
    type             VARCHAR(60) NOT NULL,
    text             TEXT,
    sort_order       INTEGER NOT NULL,
    created_time     TIMESTAMP WITH TIME ZONE,
    created_by       VARCHAR(100),
    last_edited_time TIMESTAMP WITH TIME ZONE,
    last_edited_by   VARCHAR(100),
    has_children     BOOLEAN NOT NULL,
    archived         BOOLEAN NOT NULL,
    in_trash         BOOLEAN NOT NULL,
    raw_block        JSONB
);
CREATE INDEX ix_block_document_id ON block (document_id);

CREATE TABLE document_change_notice (
    id                  BIGSERIAL PRIMARY KEY,
    status              VARCHAR(20) NOT NULL,
    attempts            INTEGER NOT NULL,
    last_attempt_at     TIMESTAMP WITH TIME ZONE,
    failure_reason      VARCHAR(1000),
    change_kind         VARCHAR(30) NOT NULL,
    project_id          BIGINT,
    notion_event_id     VARCHAR(100),
    event_type          VARCHAR(100),
    notion_workspace_id VARCHAR(100),
    subscription_id     VARCHAR(100),
    notion_page_id      VARCHAR(100) NOT NULL,
    entity_type         VARCHAR(30) NOT NULL,
    actor_type          VARCHAR(30),
    attempt_number      INTEGER,
    occurred_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    received_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    raw_payload         JSONB,
    CONSTRAINT uk_document_change_notice_notion_event_id UNIQUE (notion_event_id)
);
