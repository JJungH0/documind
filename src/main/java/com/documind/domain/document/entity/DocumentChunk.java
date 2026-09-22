package com.documind.domain.document.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Array;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "document_chunks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DocumentChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "documentChunksSeq")
    @SequenceGenerator(
            name = "documentChunkSeq",
            sequenceName = "document_chunk_seq"
    )
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_id")
    private EmbeddingJob job;

    @Column(nullable = false)
    private Integer chunkIndex;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private Integer contentLength;

    private Integer pageNumber;

    public static final int EMBEDDING_DIMENSIONS = 1536;

    @JdbcTypeCode(SqlTypes.VECTOR)
    @Array(length = EMBEDDING_DIMENSIONS)
    private float[] embedding;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Builder
    private DocumentChunk(EmbeddingJob job, int chunkIndex,
                          String content, Integer pageNumber) {
        this.job = job;
        this.chunkIndex = chunkIndex;
        this.content = content;
        this.contentLength = content.length();
        this.pageNumber = pageNumber;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public void applyEmbedding(float[] embedding) {
        this.embedding = embedding;
    }
}
