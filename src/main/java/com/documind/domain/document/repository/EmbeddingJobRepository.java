package com.documind.domain.document.repository;

import com.documind.domain.document.entity.EmbeddingJob;
import com.documind.domain.document.entity.enums.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface EmbeddingJobRepository extends JpaRepository<EmbeddingJob, Long> {

    List<EmbeddingJob> findByDocumentIdOrderByIdDesc(Long documentId);

    List<EmbeddingJob> findByStatusNotInAndCreatedAtBefore(Collection<JobStatus> statuses, Instant time);
}
