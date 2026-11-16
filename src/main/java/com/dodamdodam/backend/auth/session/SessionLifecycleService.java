package com.dodamdodam.backend.auth.session;

import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.springframework.core.convert.ConversionException;
import org.springframework.dao.DataAccessException;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.session.Session;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionException;

@Service
public class SessionLifecycleService {

    private static final String LOGIN_ATTEMPT = SessionLifecycleService.class.getName() + ".login";
    private final SessionSecurityRepository securityRepository;
    private final JdbcIndexedSessionRepository sessionRepository;
    private final AuthenticatedSessionService authenticatedSessions;
    private final AuthenticationTrustResolverImpl trustResolver = new AuthenticationTrustResolverImpl();

    public SessionLifecycleService(SessionSecurityRepository securityRepository, JdbcIndexedSessionRepository sessionRepository,
                                   AuthenticatedSessionService authenticatedSessions) {
        this.securityRepository = securityRepository;
        this.sessionRepository = sessionRepository;
        this.authenticatedSessions = authenticatedSessions;
    }

    /** 이전 세션을 확인하고 새 로그인 문맥의 생성·커밋 장애를 인증 실패와 구분한다. */
    AuthenticatedSession beginLogin(Authentication authentication, HttpServletRequest request) {
        if (!trustResolver.isAuthenticated(authentication)) {
            throw new InsufficientAuthenticationException("로그인 계정을 확인할 수 없습니다.");
        }
        UUID accountId;
        try {
            accountId = UUID.fromString(authentication.getName());
            if (!accountId.toString().equals(authentication.getName())) {
                throw new IllegalArgumentException();
            }
        }
        catch (IllegalArgumentException exception) {
            throw new InsufficientAuthenticationException("로그인 계정을 확인할 수 없습니다.");
        }
        try {
            var old = request.getSession(false);
            AuthenticatedSession previous = null;
            if (old != null && old.getAttribute(AuthenticatedSessionService.ACCOUNT_ID) instanceof UUID oldAccount
                    && old.getAttribute(AuthenticatedSessionService.SECURITY_CONTEXT_ID) instanceof UUID oldContext) {
                previous = new AuthenticatedSession(oldAccount, oldContext);
            }
            // REQUIRES_NEW의 커밋 실패도 프록시 호출이 끝나는 이 경계에서 분류한다.
            var created = securityRepository.createLoginContext(accountId, previous);
            request.setAttribute(LOGIN_ATTEMPT, new PendingLogin(created));
            return created;
        }
        catch (DataAccessException | TransactionException | ConversionException failure) {
            throw new AuthStateUnavailableException(failure);
        }
    }

    void nativeSessionPrepared(HttpServletRequest request, String sessionId) {
        pending(request).sessionId = sessionId;
    }

    void contextSaved(HttpServletRequest request) {
        var login = pending(request);
        if (login != null) {
            login.contextSaved = true;
            verifyLogin(request);
        }
    }

    /** 응답 전에 실제 로그인 저장 결과를 확인하고 불완전한 저장을 503 대상으로 표시한다. */
    public void verifyLogin(HttpServletRequest request) {
        var login = pending(request);
        if (login == null) {
            return;
        }
        try {
            if (!login.contextSaved || login.sessionId == null) {
                throw new AuthStateUnavailableException();
            }
            Session stored = sessionRepository.findById(login.sessionId);
            if (stored == null || !stored.getMaxInactiveInterval().isNegative()) {
                throw new AuthStateUnavailableException();
            }
            authenticatedSessions.requireStoredSession(stored, login.context);
            AuthenticatedSessionService.markCookieRenewal(request, login.sessionId, login.context);
        }
        catch (AuthenticationException | DataAccessException | TransactionException | ConversionException failure) {
            throw new AuthStateUnavailableException(failure);
        }
    }

    public boolean hasPendingLogin(HttpServletRequest request) {
        return pending(request) != null;
    }

    /** 실패한 새 문맥을 보상 폐기하고 프레임워크 세션도 반드시 정리한다. */
    public void failLogin(HttpServletRequest request) {
        var login = pending(request);
        request.removeAttribute(LOGIN_ATTEMPT);
        if (login == null) {
            return;
        }
        try {
            try {
                securityRepository.discardLoginContext(login.context);
            }
            finally {
                SecurityContextHolder.clearContext();
                if (login.sessionId != null) {
                    sessionRepository.deleteById(login.sessionId);
                }
            }
        }
        catch (DataAccessException | TransactionException | ConversionException failure) {
            throw new AuthStateUnavailableException(failure);
        }
    }

    private PendingLogin pending(HttpServletRequest request) {
        return (PendingLogin) request.getAttribute(LOGIN_ATTEMPT);
    }

    private static class PendingLogin {
        final AuthenticatedSession context;
        String sessionId;
        boolean contextSaved;

        PendingLogin(AuthenticatedSession context) {
            this.context = context;
        }
    }
}
