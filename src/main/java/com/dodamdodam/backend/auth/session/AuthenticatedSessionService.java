package com.dodamdodam.backend.auth.session;

import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.session.Session;
import org.springframework.stereotype.Service;

@Service
public class AuthenticatedSessionService {

    public static final String ACCOUNT_ID = "accountId";
    public static final String SECURITY_CONTEXT_ID = "securityContextId";
    static final String COOKIE_RENEWAL = AuthenticatedSessionService.class.getName() + ".cookieRenewal";

    private final SessionSecurityRepository repository;
    private final AuthenticationTrustResolverImpl trustResolver = new AuthenticationTrustResolverImpl();

    public AuthenticatedSessionService(SessionSecurityRepository repository) {
        this.repository = repository;
    }

    public AuthenticatedSession requireAuthenticatedSession(HttpServletRequest request) {
        var session = request.getSession(false);
        if (session == null) {
            throw new InsufficientAuthenticationException("로그인이 필요합니다.");
        }
        var authenticated = validate(SecurityContextHolder.getContext().getAuthentication(),
                session.getAttribute(ACCOUNT_ID), session.getAttribute(SECURITY_CONTEXT_ID),
                session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY));
        markCookieRenewal(request, session.getId(), authenticated);
        return authenticated;
    }

    void requireStoredSession(Session stored, AuthenticatedSession expected) {
        if (stored == null) throw new InsufficientAuthenticationException("로그인이 필요합니다.");
        Object context = stored.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        var authentication = context instanceof SecurityContext security ? security.getAuthentication() : null;
        var actual = validate(authentication, stored.getAttribute(ACCOUNT_ID), stored.getAttribute(SECURITY_CONTEXT_ID), context);
        if (!expected.equals(actual)) throw new InsufficientAuthenticationException("로그인이 필요합니다.");
    }

    static void markCookieRenewal(HttpServletRequest request, String sessionId, AuthenticatedSession context) {
        request.setAttribute(COOKIE_RENEWAL, new CookieRenewal(sessionId, context));
    }

    private AuthenticatedSession validate(Authentication authentication, Object account, Object context, Object security) {
        if (!trustResolver.isAuthenticated(authentication)
                || !(account instanceof UUID accountId)
                || !(context instanceof UUID contextId)
                || !accountId.toString().equals(authentication.getName())
                || !(security instanceof SecurityContext storedContext)
                || !trustResolver.isAuthenticated(storedContext.getAuthentication())
                || !accountId.toString().equals(storedContext.getAuthentication().getName())
                || !repository.isCurrentContext(accountId, contextId)) {
            throw new InsufficientAuthenticationException("로그인이 필요합니다.");
        }
        return new AuthenticatedSession(accountId, contextId);
    }

    record CookieRenewal(String sessionId, AuthenticatedSession context) { }
}
