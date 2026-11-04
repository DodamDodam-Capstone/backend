package com.dodamdodam.backend.child.api;

import com.dodamdodam.backend.child.exception.ChildException;
import com.dodamdodam.backend.global.error.ApiErrorResponse;
import com.dodamdodam.backend.global.error.ApiErrorResponses;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ChildExceptionHandler {

    /** 자녀 업무 예외만 공통 오류 응답으로 변환해 다른 기능의 인증 장애와 구분한다. */
    @ExceptionHandler(ChildException.class)
    public ResponseEntity<ApiErrorResponse> handle(ChildException exception, HttpServletResponse response) {
        var status = switch (exception.code()) {
            case "VALIDATION_FAILED" -> HttpStatus.BAD_REQUEST;
            case "RESOURCE_NOT_FOUND" -> HttpStatus.NOT_FOUND;
            case "CHILD_LIMIT_REACHED" -> HttpStatus.CONFLICT;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
        // 업무 DB 오류는 MVC에서 처리해 원인 순회 중 인증 장애 503으로 바뀌지 않게 한다.
        return ResponseEntity.status(status).body(
            ApiErrorResponses.body(
                response,
                exception.code(),
                exception.getMessage(),
                exception.fields()
            )
        );
    }
}
