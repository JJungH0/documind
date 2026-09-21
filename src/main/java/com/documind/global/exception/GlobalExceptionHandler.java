package com.documind.global.exception;

import com.documind.global.common.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException e) {
        ErrorCode errorCode = e.getErrorCode();

        if (errorCode.getStatus().is5xxServerError()) {
            log.error("[{}] {}", errorCode.getCode(), e.getMessage(), e);
        } else {
            log.warn("[{}] {}", errorCode.getCode(), e.getMessage());
        }

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.error(errorCode.getCode(), errorCode.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException e) {
        String detail = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .reduce((a, b) -> a + ", " + b)
                .orElse(ErrorCode.INVALID_INPUT.getMessage());

        log.warn("[{}] {}", ErrorCode.INVALID_INPUT.getCode(), detail);

        return ResponseEntity
                .status(ErrorCode.INVALID_INPUT.getStatus())
                .body(ApiResponse.error(ErrorCode.INVALID_INPUT.getCode(), detail));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSize(MaxUploadSizeExceededException e) {
        log.warn("업로드 용량 초과: {}", e.getMessage());

        return ResponseEntity
                .status(ErrorCode.FILE_SIZE_EXCEEDED.getStatus())
                .body(ApiResponse.error(ErrorCode.FILE_SIZE_EXCEEDED.getCode(), ErrorCode.FILE_SIZE_EXCEEDED.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexcepted(Exception e) {
        log.error("처리되지 않은 예외",e);

        return ResponseEntity
                .status(ErrorCode.INTERNAL_ERROR.getStatus())
                .body(ApiResponse.error(ErrorCode.INTERNAL_ERROR.getCode(), ErrorCode.INTERNAL_ERROR.getMessage()));
    }
}
