package com.documind.domain.document.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class ChunkSearchRepository {

    private static final String NEAREST_CHUNKS_SQL = """
            SELECT c.id,
                   c.chunk_index,
                   c.page_number,
                   c.content,
                   1 - (c.embedding <=> CAST(:queryVector AS vector)) AS similarity
            FROM document_chunks c
            WHERE c.job_id = :jobId
             AND c.embedding IS NOT NULL
            ORDER BY c.embedding <=> CAST(:queryVector AS vector)
            LIMIT :topK
            """;

    private final JdbcClient jdbcClient;

    public List<SimilarChunk> findNearest(Long jobId, float[] queryVector, int topK) {
        return jdbcClient.sql(NEAREST_CHUNKS_SQL)
                .param("queryVector", toVectorLiteral(queryVector))
                .param("jobId", jobId)
                .param("topK", topK)
                .query((rs, rowNum) -> new SimilarChunk(
                        rs.getLong("id"),
                        rs.getInt("chunk_index"),
                        rs.getObject("page_number", Integer.class),
                        rs.getString("content"),
                        rs.getDouble("similarity")))
                .list();
    }

    private static String toVectorLiteral(float[] vector) {
        StringBuilder sb = new StringBuilder(vector.length * 12);
        sb.append('[');
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(vector[i]);
        }
        return sb.append(']').toString();
    }
}
