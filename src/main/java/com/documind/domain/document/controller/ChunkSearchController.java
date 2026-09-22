package com.documind.domain.document.controller;

import com.documind.domain.document.dto.SearchRequest;
import com.documind.domain.document.dto.SearchResponse;
import com.documind.domain.document.service.ChunkSearchService;
import com.documind.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Search", description = "백터 유사도 검색")
@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class ChunkSearchController {

    private final ChunkSearchService chunkSearchService;

    @Operation(summary = "유사 청크 검색",
    description = "질문과 의미가 가까운 청크를 코사인 거리 기준을 찾음. 답변 생성은 하지 않음")
    @PostMapping("/{jobId}/search")
    public ApiResponse<SearchResponse> search(
            @PathVariable Long jobId,
            @Valid @RequestBody SearchRequest req) {
        return ApiResponse.success(chunkSearchService.search(jobId, req));
    }
}
