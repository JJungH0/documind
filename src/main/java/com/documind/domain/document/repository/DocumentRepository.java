package com.documind.domain.document.repository;

import com.documind.domain.document.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface DocumentRepository extends JpaRepository<Document, Long> {
    Optional<Document> findByContentHash(String contentHash);

    @Query("""
            select new com.documind.domain.document.repository.DocumentSummary(
            d.id, d.originalFilename, d.fileSize, d.pageCount, d.createdAt)
            from Document d
            order by d.id desc
            """)
    List<DocumentSummary> findAllSummaries();
}
