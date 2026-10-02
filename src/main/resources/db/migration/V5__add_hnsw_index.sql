SET LOCAL maintenance_work_mem = '1GB';

CREATE INDEX idx_document_chunks_embedding_hnsw
    ON document_chunks USING hnsw (embedding vector_cosine_ops)
    WITH (m = 32, ef_construction = 128);
