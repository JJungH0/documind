package com.documind.domain.document.service;

import com.documind.domain.document.dto.DocumentResponse;
import com.documind.domain.document.entity.Document;
import com.documind.domain.document.parser.PdfParser;
import com.documind.domain.document.repository.DocumentRepository;
import com.documind.global.exception.BusinessException;
import com.documind.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private static final String PDF_CONTENT_TYPE = "application/pdf";

    private final DocumentRepository documentRepository;
    private final FileStorage fileStorage;
    private final PdfParser pdfParser;

    @Transactional
    public DocumentResponse upload(MultipartFile file) {
        validate(file);

        String contentHash = fileStorage.calculateHash(file);
        documentRepository.findByContentHash(contentHash)
                .ifPresent(existing -> {
                    throw new BusinessException(ErrorCode.DUPLICATE_DOCUMENT,
                            "기존 문서 ID: " + existing.getId());
                });

        String storedPath = fileStorage.store(file);

        Document document = Document.builder()
                .originalFilename(file.getOriginalFilename())
                .storedFilename(storedPath)
                .contentHash(contentHash)
                .fileSize(file.getSize())
                .build();

        documentRepository.save(document);

        Path path = fileStorage.resolve(storedPath);
        PdfParser.ParseResult result = pdfParser.parse(path);

        if (result.text().isBlank()) {
            throw new BusinessException(ErrorCode.NO_EXTRACTABLE_TEXT);
        }

        document.applyParseResult(result.text(), result.pageCount());

        log.info("문서 업로드 완료: id={}, pages={}, chars={}",
                document.getId(), result.pageCount(), result.text().length());

        return DocumentResponse.from(document);
    }

    private void validate(MultipartFile file) {
        if (Objects.isNull(file) || file.isEmpty()) {
            throw new BusinessException(ErrorCode.EMPTY_FILE);
        }
        if (!PDF_CONTENT_TYPE.equals(file.getContentType())) {
            throw new BusinessException(ErrorCode.UNSUPPORTED_FILE_TYPE,
                    "요청된 타입: " + file.getContentType());
        }
    }

}
