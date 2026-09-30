package com.documind.domain.document.repository;

import com.documind.domain.document.entity.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, Long> {

    @Query("""
        select new com.documind.domain.document.repository.ChunkContent(c.id, c.content)
        from DocumentChunk c
        where c.job.id = :jobId
         and c.embedding is null
        order by c.chunkIndex
        """)
    List<ChunkContent> findUnembeddedContentsByJobId(@Param("jobId") Long jobId);
}
