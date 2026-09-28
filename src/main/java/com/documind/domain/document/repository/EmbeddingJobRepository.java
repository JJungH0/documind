package com.documind.domain.document.repository;

import com.documind.domain.document.entity.EmbeddingJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmbeddingJobRepository extends JpaRepository<EmbeddingJob, Long> {

    List<EmbeddingJob> findByDocumentIdOrderByIdDesc(Long documentId);
}
