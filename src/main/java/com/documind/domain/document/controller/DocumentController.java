package com.documind.domain.document.controller;

import com.documind.domain.document.dto.DocumentResponse;
import com.documind.domain.document.service.DocumentService;
import com.documind.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "Document", description = "문서 업로드 및 조회")
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @Operation(summary = "PDF 문서 업로드",
            description = "PDF를 업로드하고 텍스트를 추출함. 임베딩은 별도 요청으로 수행")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<DocumentResponse>> upload(
            @RequestPart("file") MultipartFile file
    ) {
        DocumentResponse response = documentService.upload(file);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    @Operation(summary = "문서 목록 조회",
            description = "최근 업로드 순으로 반환한다. 추출 텍스트는 포함되지 않는다.")
    @GetMapping
    public ApiResponse<List<DocumentResponse>> list() {
        return ApiResponse.success(documentService.getDocuments());
    }
}
