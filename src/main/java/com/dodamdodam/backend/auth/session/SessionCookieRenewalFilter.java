package com.dodamdodam.backend.auth.session;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.HttpCookie;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.ConversionException;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.util.OnCommittedResponseWrapper;
import org.springframework.session.Session;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionException;
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

    /** native 세션·업무 문맥 재검사 뒤 쿠키를 갱신하며 확인 장애는 인증 오류로 전달한다. */
    private void renew(HttpServletRequest request, HttpServletResponse response) {
        var marker = request.getAttribute(AuthenticatedSessionService.COOKIE_RENEWAL);
        request.removeAttribute(AuthenticatedSessionService.COOKIE_RENEWAL);
        if (!(marker instanceof AuthenticatedSessionService.CookieRenewal renewal) || response.getStatus() >= 400) return;
        var headers = response.getHeaders("Set-Cookie");
        if (headers.stream().filter(value -> value.startsWith("JSESSIONID="))
                .flatMap(value -> HttpCookie.parse(value).stream()).anyMatch(cookie -> cookie.getMaxAge() == 0)) return;

        Session stored;
        try {
            stored = sessions.findById(renewal.sessionId());
            authentication.requireStoredSession(stored, renewal.context());
        }
        catch (AuthenticationException exception) {
            // 기존 본문에 오류를 덧붙이지 않도록 Security 바깥의 오류 필터에서 처리한다.
            throw new InvalidSessionException();
        }
        catch (DataAccessException | TransactionException | ConversionException failure) {
            // 프레임워크 세션 재조회 실패도 업무 오류500과 구분해 외곽 필터에서503으로 처리한다.
            throw new AuthStateUnavailableException(failure);
        }
        if (!stored.getMaxInactiveInterval().isNegative()) return;

        var otherCookies = headers.stream().filter(value -> !value.startsWith("JSESSIONID=")).toList();
        response.setHeader("Set-Cookie", null);
        otherCookies.forEach(value -> response.addHeader("Set-Cookie", value));
        var value = new CookieSerializer.CookieValue(request, response, renewal.sessionId());
        value.setCookieMaxAge(31536000);
        cookies.writeCookieValue(value);
    }

    /** MVC가 소비하지 않고 외곽 필터의 본문 초기화·401 변환으로 넘겨야 하는 재검사 실패다. */
    public static class InvalidSessionException extends RuntimeException { }
}
