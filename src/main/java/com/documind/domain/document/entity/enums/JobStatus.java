package com.documind.domain.document.entity.enums;

public enum JobStatus {
    PENDING, // 접수
    PARSING, // PDF 텍스트 추출
    CHUNKING, // 청킹중
    EMBEDDING, // 임베딩 API 호출 중
    COMPLETED, // 완료
    FAILED // 실패
}
