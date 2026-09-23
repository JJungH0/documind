package com.documind.domain.query.controller;

import com.documind.domain.query.dto.AskRequest;
import com.documind.domain.query.dto.AskResponse;
import com.documind.domain.query.service.QueryService;
import com.documind.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Query", description = "문서 기반 질의응답")
@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class QueryController {

    private final QueryService queryService;

    @Operation(summary = "문서에 질문하기",
            description = "관련 청크를 검색한 뒤 LLM이 해당 내용만 근거로 답변함.")
    @PostMapping("/{jobId}/ask")
    public ApiResponse<AskResponse> aks(@PathVariable Long jobId,
                                        @Valid @RequestBody AskRequest request) {
        return ApiResponse.success(queryService.ask(jobId, request));
    }
}
