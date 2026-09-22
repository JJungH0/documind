package com.documind.domain.document.entity;

import com.documind.domain.document.entity.enums.JobStatus;
import com.documind.domain.document.entity.enums.ProcessingMode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "embedding_jobs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmbeddingJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id")
    private Document document;

    @Column(nullable = false)
    private Integer chunkSize;

    @Column(nullable = false)
    private Integer chunkOverlap;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProcessingMode processingMode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private JobStatus status;

    @Column(nullable = false)
    private Integer totalChunks;

    @Column(nullable = false)
    private Integer embeddedChunks;

    @Column(nullable = false)
    private Long embeddingTokens;

    private Instant startedAt;
    private Instant completedAt;
    private Long durationMs;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Builder
    private EmbeddingJob(
            Document document,
            int chunkSize,
            int chunkOverlap,
            ProcessingMode processingMode) {

        this.document = document;
        this.chunkSize = chunkSize;
        this.chunkOverlap = chunkOverlap;
        this.processingMode = processingMode;
        this.status = JobStatus.PENDING;
        this.totalChunks = 0;
        this.embeddedChunks = 0;
        this.embeddingTokens = 0L;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public void start() {
        this.status = JobStatus.CHUNKING;
        this.startedAt = Instant.now();
    }

    public void changeStatus(JobStatus status) {
        this.status = status;
    }

    public void assignTotalChunks(int totalChunks) {
        this.totalChunks = totalChunks;
    }

    public void addProgress(int embeddedChunks, long tokensUsed) {
        this.embeddedChunks += embeddedChunks;
        this.embeddingTokens += tokensUsed;
    }

    public void markCompleted() {
        this.status = JobStatus.COMPLETED;
        this.completedAt = Instant.now();
        this.durationMs = Duration.between(startedAt, completedAt).toMillis();
    }

    public void markFailed(String errorMessage) {
        this.status = JobStatus.FAILED;
        this.completedAt = Instant.now();
        this.durationMs = Objects.nonNull(startedAt)
                ? Duration.between(startedAt, completedAt).toMillis()
                : null;
        this.errorMessage = errorMessage;
    }

    public int progressPercent() {
        if (Objects.isNull(totalChunks) || totalChunks == 0) {
            return 0;
        }
        return (int) ((embeddedChunks * 100L) / totalChunks);
    }
}
