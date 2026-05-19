ALTER TABLE document ADD COLUMN parent_document_id BIGINT REFERENCES document(id);
CREATE INDEX ix_document_parent_document_id ON document (parent_document_id);