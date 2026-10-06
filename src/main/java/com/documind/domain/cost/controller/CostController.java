package com.documind.domain.cost.controller;

import com.documind.domain.cost.dto.CostSummaryResponse;
import com.documind.domain.cost.service.CostService;
import com.documind.global.common.ApiResponse;
import com.documind.global.exception.BusinessException;
import com.documind.global.exception.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@Tag(name = "Cost", description = "OpenAI 사용 비용 집계")
@RestController
@RequestMapping("/api/costs")
@RequiredArgsConstructor
public class CostController {

    private final CostService costService;

    @Operation(summary = "기간별 비용 요약",
            description = "질문 처리(임베딩+답변)와 문서 처리(임베딩)의 토큰과 비용. 날짜는 설정된 시간대 기준이며 양 끝을 포함한다.")
    @GetMapping("/summary")
    public ApiResponse<CostSummaryResponse> summary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        if (from.isAfter(to)) {
            throw new BusinessException(ErrorCode.INVALID_DATE_RANGE, "from=" + from + ", to=" + to);
        }
        return ApiResponse.success(costService.summarize(from, to));
    }
}
