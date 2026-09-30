package com.documind.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    /**
     * 공통 :
     */
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "C001", "입력값이 올바르지 않습니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C002", "서버 내부 오류가 발생했습니다."),
    MALFORMED_REQUEST_BODY(HttpStatus.BAD_REQUEST, "C003", "요청 본문의 형식이 올바르지 않습니다."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "C004", "요청한 경로를 찾을 수 없습니다."),

    /**
     * 문서 :
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
    EMBEDDING_FAILED(HttpStatus.BAD_GATEWAY, "D012", "임베딩 생성에 실패했습니다."),
    JOB_NOT_READY(HttpStatus.CONFLICT, "D013", "임베딩이 완료되지 않은 작업입니다."),
    JOB_QUEUE_FULL(HttpStatus.SERVICE_UNAVAILABLE, "D014", "처리 대기 중인 작업이 많아 지금은 받을 수 없습니다. 잠시 후 다시 시도해 주세요."),
    JOB_INTERRUPTED(HttpStatus.INTERNAL_SERVER_ERROR, "D015", "서버가 처리 도중 종료되어 작업이 중단되었습니다."),

    /**
     * OpenAI :
     */
    AI_AUTH_FAILED(HttpStatus.BAD_GATEWAY, "A001", "AI 서비스 인증에 실패했습니다. 관리자에게 API 키 설정 확인을 요청하세요."),
    AI_QUOTA_EXCEEDED(HttpStatus.BAD_GATEWAY, "A002", "AI 서비스 사용 가능 금액이 소진되었습니다. 관리자에게 문의하세요."),
    AI_RATE_LIMITED(HttpStatus.SERVICE_UNAVAILABLE, "A003", "AI 서비스 요청 한도를 초과했습니다. 잠시 후 다시 시도해 주세요."),
    AI_UNAVAILABLE(HttpStatus.BAD_GATEWAY, "A004", "AI 서비스가 일시적으로 응답하지 않습니다. 잠시 후 다시 시도해 주세요."),


    /**
     * 질의 :
     */
    ANSWER_GENERATION_FAILED(HttpStatus.BAD_GATEWAY, "Q001", "답변 생성에 실패했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
    }
