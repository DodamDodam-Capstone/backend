package com.dodamdodam.backend.auth.session;

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

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class AuthStateErrorFilter extends OncePerRequestFilter {

    private final AuthStateErrorHandler errorHandler;
    private final SessionLifecycleService lifecycle;

    public AuthStateErrorFilter(AuthStateErrorHandler errorHandler, SessionLifecycleService lifecycle) {
        this.errorHandler = errorHandler;
        this.lifecycle = lifecycle;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            filterChain.doFilter(request, new OnCommittedResponseWrapper(response) {
                @Override
                protected void onResponseCommitted() {
                    lifecycle.verifyLogin(request);
                }
            });
            if (!response.isCommitted()) lifecycle.verifyLogin(request);
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
            if (!loginFailure && failure instanceof SessionCookieRenewalFilter.InvalidSessionException) {
                errorHandler.commence(request, response, new InsufficientAuthenticationException("로그인이 필요합니다."));
            }
            else {
                errorHandler.unavailable(response);
            }
        }
    }

    /** 명시적인 인증 장애와 MVC 밖에서 발생한 프레임워크 세션 장애를 찾는다. */
    private Throwable findAuthStateFailure(Throwable failure) {
        // Spring Session 저장 실패는 MVC 밖에서도 발생하므로 기존 저장소 오류 분류를 유지한다.
        while (failure != null && !(failure instanceof DataAccessException)
                && !(failure instanceof TransactionException) && !(failure instanceof ConversionException)
                && !(failure instanceof AuthStateUnavailableException)
                && !(failure instanceof SessionCookieRenewalFilter.InvalidSessionException)) {
            failure = failure.getCause();
        }
        return failure;
    }

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
