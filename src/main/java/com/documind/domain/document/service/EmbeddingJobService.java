package com.documind.domain.document.service;

import com.documind.domain.document.chunker.ChunkDraft;
import com.documind.domain.document.chunker.TextChunker;
import com.documind.domain.document.dto.CreateJobRequest;
import com.documind.domain.document.dto.JobResponse;
import com.documind.domain.document.entity.Document;
import com.documind.domain.document.entity.DocumentChunk;
import com.documind.domain.document.entity.EmbeddingJob;
import com.documind.domain.document.entity.enums.ProcessingMode;
import com.documind.domain.document.repository.DocumentChunkRepository;
import com.documind.domain.document.repository.DocumentRepository;
import com.documind.domain.document.repository.EmbeddingJobRepository;
import com.documind.global.exception.BusinessException;
import com.documind.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingJobService {

    private final DocumentRepository documentRepository;
    private final EmbeddingJobRepository embeddingJobRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final TextChunker textChunker;

    @Transactional
    public JobResponse createAndChunk(Long documentId, CreateJobRequest req) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND, "ID: " + documentId));

        EmbeddingJob job = EmbeddingJob.builder()
                .document(document)
                .chunkSize(req.chunkSize())
                .chunkOverlap(req.chunkOverlap())
                .processingMode(ProcessingMode.SYNC)
                .build();

        embeddingJobRepository.save(job);

        job.start();

        List<ChunkDraft> drafts = textChunker.chunk(
                document.getExtractedText(),
                req.chunkSize(),
                req.chunkOverlap()
        );

        List<DocumentChunk> chunks = drafts.stream()
                .map(draft -> DocumentChunk.builder()
                        .job(job)
                        .chunkIndex(draft.index())
                        .content(draft.content())
                        .pageNumber(draft.pageNumber())
                        .build())
                .toList();

        documentChunkRepository.saveAll(chunks);
        job.assignTotalChunks(chunks.size());

        log.info("청킹 완료: jobId={}, documentId={}, chunkSize={}, overlap={}, chunks={}",
                job.getId(), documentId, req.chunkSize(), req.chunkOverlap(), chunks.size());

        return JobResponse.from(job);
    }

    @Transactional(readOnly = true)
    public JobResponse getJob(Long jobId) {
        EmbeddingJob job = embeddingJobRepository.findById(jobId)
                .orElseThrow(() -> new BusinessException(ErrorCode.JOB_NOT_FOUND, "ID: " + jobId));
        return JobResponse.from(job);
    }
}
