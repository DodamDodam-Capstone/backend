package com.dodamdodam.backend.auth.session;

import com.dodamdodam.backend.global.error.ApiErrorResponses;
import com.dodamdodam.backend.guardian.application.GuardianAccessService;
import com.dodamdodam.backend.guardian.exception.GuardianAccessException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.ConversionException;
import org.springframework.dao.DataAccessException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.web.util.OnCommittedResponseWrapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionException;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.json.JsonMapper;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class AuthStateErrorFilter extends OncePerRequestFilter {

    private final AuthStateErrorHandler errorHandler;
    private final SessionLifecycleService lifecycle;
    private final GuardianAccessService guardian;
    private final JsonMapper jsonMapper;

    /** 로그인 보상과 응답 직전 보호자 검증·공통 오류 출력 경로를 구성한다. */
    public AuthStateErrorFilter(AuthStateErrorHandler errorHandler, SessionLifecycleService lifecycle,
                                GuardianAccessService guardian, JsonMapper jsonMapper) {
        this.errorHandler = errorHandler;
        this.lifecycle = lifecycle;
        this.guardian = guardian;
        this.jsonMapper = jsonMapper;
    }

    /** 실제 전송 전과 미커밋 종료 시 권한을 확인하고 실패한 성공 응답을 비운다. */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            filterChain.doFilter(request, new OnCommittedResponseWrapper(response) {
                /** native 저장·쿠키 처리 뒤 실제 전송 전에 현재 권한을 확인한다. */
                @Override
                protected void onResponseCommitted() {
                    verifyResponseAccess(request, response);
                }
            });
            if (!response.isCommitted()) {
                verifyResponseAccess(request, response);
            }
        }
        catch (ServletException | IOException | RuntimeException exception) {
            boolean loginFailure = lifecycle.hasPendingLogin(request);
            if (loginFailure) {
                try {
                    lifecycle.failLogin(request);
                }
                catch (RuntimeException compensationFailure) {
                    exception.addSuppressed(compensationFailure);
                }
            }
            Throwable failure = findAuthStateFailure(exception);
            if ((!loginFailure && failure == null) || response.isCommitted()) {
                throw exception;
            }
            resetForError(response);
            if (!loginFailure && failure instanceof GuardianAccessException access) {
                ApiErrorResponses.write(jsonMapper, response, access.status(), access.code(), access.getMessage());
            }
            else if (!loginFailure && failure instanceof SessionCookieRenewalFilter.InvalidSessionException) {
                errorHandler.commence(request, response, new InsufficientAuthenticationException("로그인이 필요합니다."));
            }
            else {
                errorHandler.unavailable(response);
            }
        }
    }

    /** 로그인 최종 검증을 유지하고 아직 성공 상태인 보호자 응답만 DB에서 재검사한다. */
    private void verifyResponseAccess(HttpServletRequest request, HttpServletResponse response) {
        lifecycle.verifyLogin(request);
        // 업무 오류 응답을 guardian 오류로 덮어쓰거나 오류 직렬화 중 재검사를 반복하지 않는다.
        if (response.getStatus() < 400) {
            guardian.verifyResponseAccess(request);
        }
    }

    /** 명시적인 인증·보호자 오류와 MVC 밖의 프레임워크 세션 장애를 찾는다. */
    private Throwable findAuthStateFailure(Throwable failure) {
        // Spring Session 저장 실패는 MVC 밖에서도 발생하므로 기존 저장소 오류 분류를 유지한다.
        while (failure != null && !(failure instanceof DataAccessException)
                && !(failure instanceof TransactionException) && !(failure instanceof ConversionException)
                && !(failure instanceof AuthStateUnavailableException)
                && !(failure instanceof GuardianAccessException)
                && !(failure instanceof SessionCookieRenewalFilter.InvalidSessionException)) {
            failure = failure.getCause();
        }
        return failure;
    }

    /** 아직 전송하지 않은 성공 본문·전송 길이·위치·인증 쿠키를 공통 헤더와 분리해 제거한다. */
    private void resetForError(HttpServletResponse response) {
        var headers = new LinkedHashMap<String, List<String>>();
        for (var name : response.getHeaderNames()) {
            if (name.equalsIgnoreCase("Content-Length") || name.equalsIgnoreCase("Content-Type")
                    || name.equalsIgnoreCase("Location")) continue;
            headers.put(name, response.getHeaders(name).stream()
                    .filter(value -> !name.equalsIgnoreCase("Set-Cookie") || !value.startsWith("JSESSIONID=")).toList());
        }
        // resetBuffer는 Writer/OutputStream 선택을 해제하지 않아 오류 JSON 쓰기가 실패할 수 있다.
        response.reset();
        headers.forEach((name, values) -> values.forEach(value -> response.addHeader(name, value)));
    }
}
