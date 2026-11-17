package com.dodamdodam.backend.auth.session;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionFixationProtectionStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfAuthenticationStrategy;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;

public class LoginSessionAuthenticationStrategy implements SessionAuthenticationStrategy {

    private final SessionLifecycleService lifecycle;
    private final SessionFixationProtectionStrategy fixation = new SessionFixationProtectionStrategy();
    private final CsrfAuthenticationStrategy csrf;

    /** 두 로그인 방식이 세션 고정 방어와 CSRF 교체를 같은 경로로 수행하게 구성한다. */
    public LoginSessionAuthenticationStrategy(SessionLifecycleService lifecycle, CookieCsrfTokenRepository csrfRepository,
                                              CsrfTokenRequestHandler csrfRequestHandler) {
        this.lifecycle = lifecycle;
        fixation.setMigrateSessionAttributes(false);
        csrf = new CsrfAuthenticationStrategy(csrfRepository);
        csrf.setRequestHandler(csrfRequestHandler);
    }

    /** 외부 인증 완료 결과를 기존 계정 문맥 교체로 연결한다. */
    @Override
    public void onAuthentication(Authentication authentication, HttpServletRequest request, HttpServletResponse response) {
        var context = lifecycle.beginLogin(authentication, request);
        prepareSession(authentication, context, request, response);
    }

    /** 비밀번호 자격 재검사를 통과한 문맥으로만 새 프레임워크 세션을 준비한다. */
    public void onPasswordAuthentication(Authentication authentication, String expectedHash, long expectedVersion,
                                          HttpServletRequest request, HttpServletResponse response) {
        var context = lifecycle.beginPasswordLogin(authentication, expectedHash, expectedVersion, request);
        prepareSession(authentication, context, request, response);
    }

    /** 이전 속성을 승계하지 않고 새 문맥·일반 로그인 수명·CSRF를 설정한다. */
    private void prepareSession(Authentication authentication, AuthenticatedSession context,
                                HttpServletRequest request, HttpServletResponse response) {
        try {
            var previous = request.getSession(false);
            if (previous != null) {
                // Spring Security 예약 속성도 이전 principal은 새 로그인으로 옮기지 않는다.
                previous.removeAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
            }
            fixation.onAuthentication(authentication, request, response);
            var session = request.getSession(true);
            lifecycle.nativeSessionPrepared(request, session.getId());
            session.setMaxInactiveInterval(-1);
            session.setAttribute(AuthenticatedSessionService.ACCOUNT_ID, context.accountId());
            session.setAttribute(AuthenticatedSessionService.SECURITY_CONTEXT_ID, context.securityContextId());
            csrf.onAuthentication(authentication, request, response);
        }
        catch (RuntimeException exception) {
            throw new DataAccessResourceFailureException("로그인 세션을 준비할 수 없습니다.", exception);
        }
    }
}
