package com.dodamdodam.backend.auth.session;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.ConversionException;
import org.springframework.dao.DataAccessException;
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
            // shortcut: 다른 업무 저장소 도입 시 그 도메인의 DB 오류 처리와 분리한다.
            Throwable failure = exception;
            while (failure != null && !(failure instanceof DataAccessException)
                    && !(failure instanceof TransactionException) && !(failure instanceof ConversionException)) {
                failure = failure.getCause();
            }
            if ((!loginFailure && failure == null) || response.isCommitted()) {
                throw exception;
            }
            response.resetBuffer();
            if (loginFailure) {
                var cookies = response.getHeaders("Set-Cookie").stream()
                        .filter(cookie -> !cookie.startsWith("JSESSIONID=")).toList();
                response.setHeader("Set-Cookie", null);
                cookies.forEach(cookie -> response.addHeader("Set-Cookie", cookie));
                response.setHeader("Location", null);
            }
            errorHandler.unavailable(response);
        }
    }
}
