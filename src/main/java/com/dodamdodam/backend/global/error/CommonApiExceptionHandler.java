package com.dodamdodam.backend.global.error;

import com.dodamdodam.backend.auth.session.AuthStateUnavailableException;
import com.dodamdodam.backend.auth.session.SessionCookieRenewalFilter.InvalidSessionException;
import com.dodamdodam.backend.auth.session.SessionLifecycleService;
import com.dodamdodam.backend.global.error.ApiErrorResponse.FieldError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Order(Ordered.LOWEST_PRECEDENCE)
@RestControllerAdvice(basePackages = "com.dodamdodam.backend")
public class CommonApiExceptionHandler {

    private final SessionLifecycleService lifecycle;

    /** 진행 중 로그인에 필요한 외곽 필터의 실패 보상 경로를 확인한다. */
    public CommonApiExceptionHandler(SessionLifecycleService lifecycle) {
        this.lifecycle = lifecycle;
    }

    /** 기능별 처리 뒤에 남은 컨트롤러 오류를 원문 없는 공통 응답으로 변환한다. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handle(Exception exception, HttpServletRequest request,
                                                   HttpServletResponse response) throws Exception {
        // 로그인 중 실패는 외곽 필터의 보상 폐기가 필요하다.
        if (lifecycle.hasPendingLogin(request)) {
            throw exception;
        }
        // guardian 표식과 관계없이 인증 실패·확인 장애는 외곽 필터가 성공 헤더·본문부터 비운다.
        for (Throwable failure = exception; failure != null; failure = failure.getCause()) {
            if (failure instanceof AuthenticationException || failure instanceof InvalidSessionException
                    || failure instanceof AuthStateUnavailableException) {
                throw exception;
            }
        }
        if (exception instanceof HttpMessageNotReadableException
                || exception instanceof MethodArgumentTypeMismatchException) {
            // 오류가 난 인자 이름만 반환하고 입력값·파서 메시지는 노출하지 않는다.
            String field = exception instanceof MethodArgumentTypeMismatchException mismatch
                    ? mismatch.getName() : "body";
            return ResponseEntity.badRequest().body(ApiErrorResponses.body(
                    response, "VALIDATION_FAILED", "입력값을 확인해 주세요.",
                    List.of(new FieldError(field, "유효하지 않은 값입니다."))));
        }
        return ResponseEntity.internalServerError().body(ApiErrorResponses.body(
                response, "INTERNAL_ERROR", "요청을 처리할 수 없습니다. 잠시 후 다시 시도해 주세요.", List.of()));
    }
}
