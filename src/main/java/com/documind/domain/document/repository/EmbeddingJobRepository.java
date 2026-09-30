package com.documind.domain.document.repository;

import com.documind.domain.document.entity.EmbeddingJob;
import com.documind.domain.document.entity.enums.JobStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface EmbeddingJobRepository extends JpaRepository<EmbeddingJob, Long> {

    List<EmbeddingJob> findByDocumentIdOrderByIdDesc(Long documentId);

    List<EmbeddingJob> findByStatusNotInAndCreatedAtBefore(Collection<JobStatus> statuses, Instant time);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select j from EmbeddingJob j where j.id = :id")
    Optional<EmbeddingJob> findByIdForUpdate(@Param("id") Long id);
}
