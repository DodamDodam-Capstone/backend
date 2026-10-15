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
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionException;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class AuthStateErrorFilter extends OncePerRequestFilter {

    private final AuthStateErrorHandler errorHandler;

    public AuthStateErrorFilter(AuthStateErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            filterChain.doFilter(request, response);
        }
        catch (ServletException | RuntimeException exception) {
            // shortcut: 다른 업무 저장소 도입 시 그 도메인의 DB 오류 처리와 분리한다.
            Throwable failure = exception;
            while (failure != null && !(failure instanceof DataAccessException)
                    && !(failure instanceof TransactionException) && !(failure instanceof ConversionException)) {
                failure = failure.getCause();
            }
            if (failure == null || response.isCommitted()) {
                throw exception;
            }
            response.resetBuffer();
            errorHandler.unavailable(response);
        }
    }
}
