package com.documind.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    /**
     * 공통
     */
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "C001", "입력값이 올바르지 않습니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C002", "서버 내부 오류가 발생했습니다."),

    /**
     * 문서
     */
    DOCUMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "D001", "문서를 찾을 수 없습니다."),
    DUPLICATE_DOCUMENT(HttpStatus.CONFLICT, "D002", "이미 업로드된 문서입니다."),
    EMPTY_FILE(HttpStatus.BAD_REQUEST, "D003", "파일이 비어 있습니다."),
    UNSUPPORTED_FILE_TYPE(HttpStatus.BAD_REQUEST, "D004", "PDF 파일만 업로드할 수 있습니다."),
    ENCRYPTED_PDF(HttpStatus.BAD_REQUEST, "D005", "암호화된 PDF는 처리할 수 없습니다."),
    NO_EXTRACTABLE_TEXT(HttpStatus.BAD_REQUEST, "D006", "텍스트를 추출할 수 없는 PDF입니다."),
    FILE_STORAGE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "D007", "파일 저장에 실패했습니다."),
    PDF_PARSE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "D008", "PDF 파싱에 실패했습니다."),
    FILE_SIZE_EXCEEDED(HttpStatus.CONTENT_TOO_LARGE, "D009", "업로드 가능한 최대 파일 크기를 초과했습니다."),
    INVALID_CHUNK_OPTION(HttpStatus.BAD_REQUEST, "D010", "청킹 옵션이 올바르지 않습니다."),
    JOB_NOT_FOUND(HttpStatus.NOT_FOUND, "D011", "처리 작업을 찾을 수 없습니다."),
    EMBEDDING_FAILED(HttpStatus.BAD_GATEWAY, "D012", "임베딩 생성에 실패했습니다.");


    private final HttpStatus status;
    private final String code;
    private final String message;
    }
