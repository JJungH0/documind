package com.documind.domain.document.dto;

import com.documind.domain.document.entity.Document;
import com.documind.domain.document.repository.DocumentSummary;

import java.time.Instant;

public record DocumentResponse(
        long id,
        String filename,
        long fileSize,
        Integer pageCount,
        Instant createdAt
) {

    public static DocumentResponse from(Document document) {
        return new DocumentResponse(
                document.getId(),
                document.getOriginalFilename(),
                document.getFileSize(),
                document.getPageCount(),
                document.getCreatedAt()
        );
    }

    public static DocumentResponse from(DocumentSummary summary) {
        return new DocumentResponse(
                summary.id(),
                summary.originalFilename(),
                summary.fileSize(),
                summary.pageCount(),
                summary.createdAt()
        );
    }
}
