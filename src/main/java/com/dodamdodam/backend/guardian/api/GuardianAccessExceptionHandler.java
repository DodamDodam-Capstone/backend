package com.dodamdodam.backend.guardian.api;

import com.dodamdodam.backend.global.error.ApiErrorResponse;
import com.dodamdodam.backend.global.error.ApiErrorResponses;
import com.dodamdodam.backend.guardian.exception.GuardianAccessException;
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

    /** 로그인 무효·보호자 확인 필요·확인 장애를 정해진 상태와 공통 오류 본문으로 변환한다. */
    @ExceptionHandler(GuardianAccessException.class)
    public ResponseEntity<ApiErrorResponse> handle(GuardianAccessException exception, HttpServletResponse response) {
        // 예외의 고정 메시지만 공개하며 원인·PIN·보안 문맥은 응답에 포함하지 않는다.
        return ResponseEntity.status(exception.status()).body(ApiErrorResponses.body(
                response, exception.code(), exception.getMessage(), List.of()));
    }
}
