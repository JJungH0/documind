ALTER TABLE embedding_jobs
    ADD COLUMN chunk_strategy VARCHAR(20) NOT NULL DEFAULT 'FIXED';

ALTER TABLE embedding_jobs
    ALTER COLUMN chunk_strategy DROP DEFAULT;
