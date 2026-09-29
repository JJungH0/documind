package com.documind.domain.document.controller;

import com.documind.domain.document.dto.CreateJobRequest;
import com.documind.domain.document.dto.JobResponse;
import com.documind.domain.document.entity.enums.ProcessingMode;
import com.documind.domain.document.service.EmbeddingJobService;
import com.documind.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@Tag(name = "EmbeddingJob", description = "청킹·임베딩 처리 작업")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class EmbeddingJobController {

    private final EmbeddingJobService embeddingJobService;

    @Operation(summary = "처리 작업 생성",
            description = "문서를 청크로 나누고 임베딩을 생성한다. 기본은 비동기(202, 처리 상태는 GET /api/jobs/{id}로 확인)," +
                    "mode를 SYNC로 보내면 처리가 끝난 뒤 응답한다(201)")
    @PostMapping("/documents/{documentId}/jobs")
    public ResponseEntity<ApiResponse<JobResponse>> create(
            @PathVariable Long documentId,
            @Valid @RequestBody CreateJobRequest req){

        JobResponse response = embeddingJobService.create(documentId, req);


        return ResponseEntity
                .status(response.processingMode() == ProcessingMode.ASYNC
                        ? HttpStatus.ACCEPTED
                        : HttpStatus.CREATED)
                .location(URI.create("/api/jobs/" + response.id()))
                .body(ApiResponse.success(response));
    }

    @Operation(summary = "처리 작업 조회",
    description = "작업 상태와 진행률을 조회함.")
    @GetMapping("/jobs/{jobId}")
    public ApiResponse<JobResponse> get(@PathVariable Long jobId) {
        return ApiResponse.success(embeddingJobService.getJob(jobId));
    }

    @Operation(summary = "문서별 처리 작업 목록",
            description = "최근 작업 순으로 반환한다.")
    @GetMapping("/documents/{documentId}/jobs")
    public ApiResponse<List<JobResponse>> listByDocument(@PathVariable Long documentId) {
        return ApiResponse.success(embeddingJobService.getJobsByDocument(documentId));
    }
}
