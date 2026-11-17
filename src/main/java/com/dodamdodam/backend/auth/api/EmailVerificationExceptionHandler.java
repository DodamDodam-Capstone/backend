package com.dodamdodam.backend.auth.api;

import com.dodamdodam.backend.auth.exception.*;
import com.dodamdodam.backend.global.error.*;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = EmailVerificationController.class)
public class EmailVerificationExceptionHandler {
    /** 이메일 발급 업무 오류를 고정 코드·필드 목록으로만 반환한다. */
    @ExceptionHandler(EmailVerificationException.class)
    public ResponseEntity<ApiErrorResponse> handle(EmailVerificationException failure, HttpServletResponse response) {
        return ResponseEntity.status(failure.status()).body(ApiErrorResponses.body(response,
                failure.code(), failure.getMessage(), failure.fields()));
    }

    /** 적용 제한 중 가장 늦은 재시도까지 계산된 초를 헤더에도 표시한다. */
    @ExceptionHandler(AuthRateLimitException.class)
    public ResponseEntity<ApiErrorResponse> limited(AuthRateLimitException failure, HttpServletResponse response) {
        response.setHeader("Retry-After", Long.toString(failure.retryAfterSeconds()));
        return ResponseEntity.status(429).body(ApiErrorResponses.body(response, failure.code(), failure.getMessage(), java.util.List.of()));
    }

}
