DROP INDEX idx_documents_content_hash;

ALTER TABLE documents
    ADD CONSTRAINT uq_documents_content_hash UNIQUE (content_hash);