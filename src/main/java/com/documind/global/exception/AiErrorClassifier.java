package com.documind.global.exception;

import com.openai.errors.OpenAIIoException;
import com.openai.errors.OpenAIServiceException;

import java.util.Set;

public final class AiErrorClassifier {

    private static final String INSUFFICIENT_QUOTA = "insufficient_quota";

    private static final Set<ErrorCode> RETRYABLE = Set.of(
            ErrorCode.AI_RATE_LIMITED,
            ErrorCode.AI_UNAVAILABLE,
            ErrorCode.JOB_INTERRUPTED
    );

    private AiErrorClassifier() {}

    public static BusinessException classify(RuntimeException e, ErrorCode fallback) {
        return new BusinessException(errorCodeOf(e, fallback), e);
    }

    public static boolean isRetryable(String code) {
        return code != null && RETRYABLE.stream()
                .anyMatch(c -> c.getCode().equals(code));
    }

    static ErrorCode errorCodeOf(Throwable e, ErrorCode fallback) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof OpenAIServiceException service) {
                return fromStatus(service, fallback);
            }
            if (t instanceof OpenAIIoException) {
                return ErrorCode.AI_UNAVAILABLE;
            }
        }
        return fallback;
    }

    private static ErrorCode fromStatus(OpenAIServiceException e, ErrorCode fallback) {
        int status = e.statusCode();
        if (status == 401 || status == 403) {
            return ErrorCode.AI_AUTH_FAILED;
        }
        if (status == 429) {
            return isInsufficientQuota(e) ? ErrorCode.AI_QUOTA_EXCEEDED : ErrorCode.AI_RATE_LIMITED;
        }
        if (status >= 500) {
            return ErrorCode.AI_UNAVAILABLE;
        }
        return fallback;
    }


    private static boolean isInsufficientQuota(OpenAIServiceException e) {
        return INSUFFICIENT_QUOTA.equals(e.code().orElse(null))
                || INSUFFICIENT_QUOTA.equals(e.type().orElse(null));
    }
}
