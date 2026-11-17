package com.dodamdodam.backend.auth.api;

import com.dodamdodam.backend.auth.exception.AuthRateLimitException;
import com.dodamdodam.backend.auth.exception.PasswordLoginException;
import com.dodamdodam.backend.auth.session.SessionLifecycleService;
import com.dodamdodam.backend.global.error.ApiErrorResponse;
import com.dodamdodam.backend.global.error.ApiErrorResponses;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = PasswordLoginController.class)
public class PasswordLoginExceptionHandler {
    private final SessionLifecycleService lifecycle;

    /** 세션 생성 뒤의 오류가 외곽 필터의 실패 보상 경로를 유지하도록 구성한다. */
    public PasswordLoginExceptionHandler(SessionLifecycleService lifecycle) {
        this.lifecycle = lifecycle;
    }

    /** 입력·자격 오류를 고정 응답으로 반환하며 진행 중 로그인은 외곽 보상으로 넘긴다. */
    @ExceptionHandler(PasswordLoginException.class)
    public ResponseEntity<ApiErrorResponse> handle(PasswordLoginException failure, HttpServletRequest request,
                                                   HttpServletResponse response) {
        if (lifecycle.hasPendingLogin(request)) {
            throw failure;
        }
        return ResponseEntity.status(failure.status()).body(ApiErrorResponses.body(response,
                failure.code(), failure.getMessage(), failure.fields()));
    }

    /** 실제 남은 고정창 제한을 공통 429와 Retry-After로 전달한다. */
    @ExceptionHandler(AuthRateLimitException.class)
    public ResponseEntity<ApiErrorResponse> limited(AuthRateLimitException failure, HttpServletResponse response) {
        response.setHeader("Retry-After", Long.toString(failure.retryAfterSeconds()));
        return ResponseEntity.status(429).body(ApiErrorResponses.body(response,
                failure.code(), failure.getMessage(), List.of()));
    }
}
