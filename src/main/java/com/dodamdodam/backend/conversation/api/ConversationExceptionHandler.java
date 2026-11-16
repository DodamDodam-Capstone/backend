package com.dodamdodam.backend.conversation.api;

import com.dodamdodam.backend.conversation.exception.ConversationException;
import com.dodamdodam.backend.conversation.dto.ActiveConversationExistsError;
import com.dodamdodam.backend.global.error.ApiErrorResponses;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackages = "com.dodamdodam.backend")
public class ConversationExceptionHandler {

    /** 응답 전 재검사 실패는 MVC에서 출력하지 않고 전용 필터가 성공 응답부터 비우게 한다. */
    @ExceptionHandler(ConversationStartResponseFilter.ResponseRejected.class)
    void propagateResponseRejection(ConversationStartResponseFilter.ResponseRejected rejection) {
        throw rejection;
    }

    /** 대화 업무 오류만 변환하고 인증·소유권·JSON 읽기 오류는 기존 처리기에 맡긴다. */
    @ExceptionHandler(ConversationException.class)
    public ResponseEntity<?> handle(ConversationException exception, HttpServletResponse response) {
        var status = switch (exception.code()) {
            case "VALIDATION_FAILED" -> HttpStatus.BAD_REQUEST;
            case "SERVICE_HOURS_CLOSED", "CHILD_SESSION_REQUIRED" -> HttpStatus.FORBIDDEN;
            case "ACTIVE_CONVERSATION_EXISTS", "CONVERSATION_CLOSING", "CONVERSATION_ENDED" -> HttpStatus.CONFLICT;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
        var error = ApiErrorResponses.body(response, exception.code(), exception.getMessage(), exception.fields());
        if (exception.activeConversationId() != null) {
            return ResponseEntity.status(status).body(ActiveConversationExistsError.from(error, exception.activeConversationId()));
        }
        return ResponseEntity.status(status).body(error);
    }
}
