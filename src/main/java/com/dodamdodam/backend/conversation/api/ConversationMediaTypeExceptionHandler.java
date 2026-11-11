package com.dodamdodam.backend.conversation.api;

import com.dodamdodam.backend.global.error.ApiErrorResponse;
import com.dodamdodam.backend.global.error.ApiErrorResponses;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = ConversationController.class)
public class ConversationMediaTypeExceptionHandler {

    /** 시작 컨트롤러의 MVC 미디어 타입 오류만 공통 500 처리보다 먼저 변환한다. */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handle(HttpServletResponse response) {
        var error = ApiErrorResponses.body(response, "UNSUPPORTED_MEDIA_TYPE",
                "지원하지 않는 Content-Type입니다.", List.of());
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(error);
    }
}
