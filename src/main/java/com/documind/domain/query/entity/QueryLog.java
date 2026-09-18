package com.documind.domain.query.entity;

import com.documind.domain.document.entity.EmbeddingJob;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Objects;

@Entity
@Getter
@Table(name = "query_logs")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QueryLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_id")
    private EmbeddingJob job;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String question;

    @Column(columnDefinition = "TEXT")
    private String answer;

    @Column(nullable = false)
    private Boolean cacheHit;

    private Integer retrievedChunkCount;

    @Column(nullable = false)
    private Integer embeddingTokens;

    @Column(nullable = false)
    private Integer promptTokens;

    @Column(nullable = false)
    private Integer completionTokens;

    private Long latencyMs;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Builder
    public QueryLog(EmbeddingJob job, String question, String answer, Boolean cacheHit, Integer retrievedChunkCount, Integer embeddingTokens, Integer promptTokens, Integer completionTokens, Long latencyMs) {
        this.job = job;
        this.question = question;
        this.answer = answer;
        this.cacheHit = cacheHit;
        this.retrievedChunkCount = retrievedChunkCount;
        this.embeddingTokens = Objects.nonNull(embeddingTokens) ? embeddingTokens : 0;
        this.promptTokens = Objects.nonNull(promptTokens) ? promptTokens : 0;
        this.completionTokens = Objects.nonNull(completionTokens) ? completionTokens : 0;
        this.latencyMs = latencyMs;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }
}
