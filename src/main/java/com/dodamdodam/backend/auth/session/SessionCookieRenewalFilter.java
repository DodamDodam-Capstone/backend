package com.dodamdodam.backend.auth.session;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.HttpCookie;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.util.OnCommittedResponseWrapper;
import org.springframework.session.Session;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 2)
public class SessionCookieRenewalFilter extends OncePerRequestFilter {

    private final CookieSerializer cookies;
    private final JdbcIndexedSessionRepository sessions;
    private final AuthenticatedSessionService authentication;

    public SessionCookieRenewalFilter(CookieSerializer cookies, JdbcIndexedSessionRepository sessions,
                                     AuthenticatedSessionService authentication) {
        this.cookies = cookies;
        this.sessions = sessions;
        this.authentication = authentication;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // Spring Session의 native 저장/쿠키 기록 후, 실제 응답 커밋 전에 갱신한다.
        filterChain.doFilter(request, new OnCommittedResponseWrapper(response) {
            @Override
            protected void onResponseCommitted() {
                renew(request, response);
            }
        });
        if (!response.isCommitted()) renew(request, response);
    }

    private void renew(HttpServletRequest request, HttpServletResponse response) {
        var marker = request.getAttribute(AuthenticatedSessionService.COOKIE_RENEWAL);
        request.removeAttribute(AuthenticatedSessionService.COOKIE_RENEWAL);
        if (!(marker instanceof AuthenticatedSessionService.CookieRenewal renewal) || response.getStatus() >= 400) return;
        var headers = response.getHeaders("Set-Cookie");
        if (headers.stream().filter(value -> value.startsWith("JSESSIONID="))
                .flatMap(value -> HttpCookie.parse(value).stream()).anyMatch(cookie -> cookie.getMaxAge() == 0)) return;

        Session stored = sessions.findById(renewal.sessionId());
        try {
            authentication.requireStoredSession(stored, renewal.context());
        }
        catch (AuthenticationException exception) {
            // 기존 본문에 오류를 덧붙이지 않도록 Security 바깥의 오류 필터에서 처리한다.
            throw new InvalidSessionException();
        }
        if (!stored.getMaxInactiveInterval().isNegative()) return;

        var otherCookies = headers.stream().filter(value -> !value.startsWith("JSESSIONID=")).toList();
        response.setHeader("Set-Cookie", null);
        otherCookies.forEach(value -> response.addHeader("Set-Cookie", value));
        var value = new CookieSerializer.CookieValue(request, response, renewal.sessionId());
        value.setCookieMaxAge(31536000);
        cookies.writeCookieValue(value);
    }

    static class InvalidSessionException extends RuntimeException { }
}
