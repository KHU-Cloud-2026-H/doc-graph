CREATE TABLE graph_rule (
    id                    BIGSERIAL PRIMARY KEY,
    project_id            BIGINT REFERENCES project(id) ON DELETE CASCADE,
    source_type           VARCHAR(50) NOT NULL,
    target_type           VARCHAR(50) NOT NULL,
    validation_criterion  VARCHAR(500) NOT NULL,
    is_default            BOOLEAN NOT NULL,
    created_at            TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_graph_rule_distinct_types CHECK (source_type <> target_type),
    CONSTRAINT ck_graph_rule_scope CHECK (
        (is_default = TRUE AND project_id IS NULL)
        OR (is_default = FALSE AND project_id IS NOT NULL)
    )
);
CREATE INDEX ix_graph_rule_project_id ON graph_rule (project_id);
CREATE INDEX ix_graph_rule_type_pair ON graph_rule (source_type, target_type);

CREATE TABLE dependency_edge (
    id                    BIGSERIAL PRIMARY KEY,
    project_id            BIGINT NOT NULL REFERENCES project(id) ON DELETE CASCADE,
    source_document_id    BIGINT NOT NULL REFERENCES document(id) ON DELETE CASCADE,
    target_document_id    BIGINT NOT NULL REFERENCES document(id) ON DELETE CASCADE,
    rule_id               BIGINT REFERENCES graph_rule(id) ON DELETE SET NULL,
    validation_criterion  VARCHAR(500) NOT NULL,
    source                VARCHAR(30) NOT NULL,
    conflict_status       VARCHAR(20) NOT NULL,
    created_at            TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at            TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_dependency_edge_distinct_documents CHECK (source_document_id <> target_document_id),
    CONSTRAINT uk_dependency_edge_project_direction UNIQUE (
        project_id,
        source_document_id,
        target_document_id
    )
);
CREATE INDEX ix_dependency_edge_project_id ON dependency_edge (project_id);
CREATE INDEX ix_dependency_edge_source_document_id ON dependency_edge (source_document_id);
CREATE INDEX ix_dependency_edge_target_document_id ON dependency_edge (target_document_id);

CREATE TABLE edge_proposal (
    id                    BIGSERIAL PRIMARY KEY,
    project_id            BIGINT NOT NULL REFERENCES project(id) ON DELETE CASCADE,
    source_document_id    BIGINT NOT NULL REFERENCES document(id) ON DELETE CASCADE,
    target_document_id    BIGINT NOT NULL REFERENCES document(id) ON DELETE CASCADE,
    rule_id               BIGINT REFERENCES graph_rule(id) ON DELETE SET NULL,
    validation_criterion  VARCHAR(500) NOT NULL,
    similarity_score      DOUBLE PRECISION NOT NULL,
    created_at            TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_edge_proposal_distinct_documents CHECK (source_document_id <> target_document_id),
    CONSTRAINT ck_edge_proposal_similarity_score CHECK (similarity_score >= 0.0 AND similarity_score <= 1.0),
    CONSTRAINT uk_edge_proposal_project_direction UNIQUE (
        project_id,
        source_document_id,
        target_document_id
    )
);
CREATE INDEX ix_edge_proposal_project_id ON edge_proposal (project_id);

INSERT INTO graph_rule (
    project_id,
    source_type,
    target_type,
    validation_criterion,
    is_default,
    created_at
) VALUES
    (NULL, 'MEETING_NOTES', 'PLANNING', '결정사항 반영 여부', TRUE, now()),
    (NULL, 'MEETING_NOTES', 'REQUIREMENTS', '결정사항 반영 여부', TRUE, now()),
    (NULL, 'RESEARCH', 'PLANNING', '전제 조건 유지 여부', TRUE, now()),
    (NULL, 'PLANNING', 'REQUIREMENTS', '범위 일치 여부', TRUE, now()),
    (NULL, 'REQUIREMENTS', 'DESIGN', '스펙 일치 여부', TRUE, now());
