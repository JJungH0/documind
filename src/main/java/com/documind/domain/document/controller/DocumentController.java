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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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
}
