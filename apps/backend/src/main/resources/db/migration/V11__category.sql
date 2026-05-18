CREATE TABLE category (
    id              BIGSERIAL PRIMARY KEY,
    project_id      BIGINT NOT NULL REFERENCES project(id) ON DELETE CASCADE,
    notion_page_id  VARCHAR(100) NOT NULL,
    document_type   VARCHAR(50) NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_category_project_page UNIQUE (project_id, notion_page_id)
);
CREATE INDEX ix_category_project_id ON category (project_id);
