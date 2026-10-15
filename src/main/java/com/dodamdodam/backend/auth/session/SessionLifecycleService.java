package com.dodamdodam.backend.auth.session;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.session.Session;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class SessionLifecycleService {

    private static final String LOGIN_ATTEMPT = SessionLifecycleService.class.getName() + ".login";
    private final SessionSecurityRepository securityRepository;
    private final JdbcIndexedSessionRepository sessionRepository;
    private final AuthenticationTrustResolverImpl trustResolver = new AuthenticationTrustResolverImpl();

    public SessionLifecycleService(SessionSecurityRepository securityRepository, JdbcIndexedSessionRepository sessionRepository) {
        this.securityRepository = securityRepository;
        this.sessionRepository = sessionRepository;
    }

    AuthenticatedSession beginLogin(Authentication authentication, HttpServletRequest request) {
        if (!trustResolver.isAuthenticated(authentication)) {
            throw new InsufficientAuthenticationException("로그인 계정을 확인할 수 없습니다.");
        }
        UUID accountId;
        try {
            accountId = UUID.fromString(authentication.getName());
            if (!accountId.toString().equals(authentication.getName())) throw new IllegalArgumentException();
        }
        catch (IllegalArgumentException exception) {
            throw new InsufficientAuthenticationException("로그인 계정을 확인할 수 없습니다.");
        }
        var old = request.getSession(false);
        AuthenticatedSession previous = null;
        if (old != null && old.getAttribute(AuthenticatedSessionService.ACCOUNT_ID) instanceof UUID oldAccount
                && old.getAttribute(AuthenticatedSessionService.SECURITY_CONTEXT_ID) instanceof UUID oldContext) {
            previous = new AuthenticatedSession(oldAccount, oldContext);
        }
        var created = securityRepository.createLoginContext(accountId, previous);
        request.setAttribute(LOGIN_ATTEMPT, new PendingLogin(created));
        return created;
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

    public void verifyLogin(HttpServletRequest request) {
        var login = pending(request);
        if (login == null) return;
        if (!login.contextSaved || login.sessionId == null) throw unavailable();
        Session stored = sessionRepository.findById(login.sessionId);
        if (stored == null || !login.context.accountId().equals(stored.getAttribute(AuthenticatedSessionService.ACCOUNT_ID))
                || !login.context.securityContextId().equals(stored.getAttribute(AuthenticatedSessionService.SECURITY_CONTEXT_ID))
                || !stored.getMaxInactiveInterval().isNegative()) {
            throw unavailable();
        }
        Object storedContext = stored.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        if (!(storedContext instanceof SecurityContext securityContext)
                || !trustResolver.isAuthenticated(securityContext.getAuthentication())
                || !login.context.accountId().toString().equals(securityContext.getAuthentication().getName())
                || !securityRepository.isCurrentContext(login.context.accountId(), login.context.securityContextId())) {
            throw unavailable();
        }
    }

    public boolean hasPendingLogin(HttpServletRequest request) {
        return pending(request) != null;
    }

    public void failLogin(HttpServletRequest request) {
        var login = pending(request);
        request.removeAttribute(LOGIN_ATTEMPT);
        if (login == null) return;
        try {
            securityRepository.discardLoginContext(login.context);
        }
        finally {
            SecurityContextHolder.clearContext();
            if (login.sessionId != null) sessionRepository.deleteById(login.sessionId);
        }
    }

    private PendingLogin pending(HttpServletRequest request) {
        return (PendingLogin) request.getAttribute(LOGIN_ATTEMPT);
    }

    private DataAccessResourceFailureException unavailable() {
        return new DataAccessResourceFailureException("로그인 상태 저장을 확인할 수 없습니다.");
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
