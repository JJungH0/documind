package com.documind.domain.document.controller;

import com.documind.domain.document.dto.CreateJobRequest;
import com.documind.domain.document.dto.JobResponse;
import com.documind.domain.document.service.EmbeddingJobService;
import com.documind.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "EmbeddingJob", description = "청킹·임베딩 처리 작업")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class EmbeddingJobController {

    private final EmbeddingJobService embeddingJobService;

    @Operation(summary = "처리 작업 생성",
            description = "지정한 청킹 파라미터로 문서를 청크로 분활함.")
    @PostMapping("/documents/{documentId}/jobs")
    public ResponseEntity<ApiResponse<JobResponse>> create(
            @PathVariable Long documentId,
            @Valid @RequestBody CreateJobRequest req){

        JobResponse response = embeddingJobService.createAndChunk(documentId, req);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    @Operation(summary = "처리 작업 조회",
    description = "작업 상태와 진행률을 조회함.")
    @GetMapping("/jobs/{jobId}")
    public ApiResponse<JobResponse> get(@PathVariable Long jobId) {
        return ApiResponse.success(embeddingJobService.getJob(jobId));
    }
}
