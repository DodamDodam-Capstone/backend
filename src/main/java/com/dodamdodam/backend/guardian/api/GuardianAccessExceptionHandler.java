package com.dodamdodam.backend.guardian.api;

import com.dodamdodam.backend.global.error.ApiErrorResponse;
import com.dodamdodam.backend.global.error.ApiErrorResponses;
import com.dodamdodam.backend.guardian.application.GuardianAccessService;
import com.dodamdodam.backend.guardian.exception.GuardianAccessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackages = "com.dodamdodam.backend")
public class GuardianAccessExceptionHandler {

    /** 최초 권한 오류를 공통 응답으로 바꾸고 응답 직전 실패는 외곽 필터로 넘긴다. */
    @ExceptionHandler(GuardianAccessException.class)
    public ResponseEntity<ApiErrorResponse> handle(GuardianAccessException exception, HttpServletRequest request,
                                                   HttpServletResponse response) {
        if (GuardianAccessService.requiresResponseVerification(request)) {
            // 성공 본문·길이·쿠키가 이미 준비된 경우 외곽 필터가 먼저 응답 전체를 비워야 한다.
            throw exception;
        }
        // 예외의 고정 메시지만 공개하며 원인·PIN·보안 문맥은 응답에 포함하지 않는다.
        return ResponseEntity.status(exception.status()).body(ApiErrorResponses.body(
                response, exception.code(), exception.getMessage(), List.of()));
    }
}
