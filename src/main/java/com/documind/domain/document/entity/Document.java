package com.documind.domain.document.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Getter
@Table(name = "documents")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Document {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String originalFilename;

    @Column(nullable = false, unique = true)
    private String storedFilename;

    @Column(nullable = false, length = 64)
    private String contentHash;

    @Column(nullable = false)
    private Long fileSize;

    private Integer pageCount;

    @Column(columnDefinition = "TEXT")
    private String extractedText;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;


    @Builder
    private Document(String originalFilename, String storedFilename, String contentHash, Long fileSize) {
        this.originalFilename = originalFilename;
        this.storedFilename = storedFilename;
        this.contentHash = contentHash;
        this.fileSize = fileSize;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public void applyParseResult(String extractedText, int pageCount) {
        this.extractedText = extractedText;
        this.pageCount = pageCount;
    }
}
