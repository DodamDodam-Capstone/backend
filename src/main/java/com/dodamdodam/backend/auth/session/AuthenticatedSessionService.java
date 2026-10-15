package com.dodamdodam.backend.auth.session;

import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;

@Service
public class AuthenticatedSessionService {

    public static final String ACCOUNT_ID = "accountId";
    public static final String SECURITY_CONTEXT_ID = "securityContextId";

    private final SessionSecurityRepository repository;
    private final AuthenticationTrustResolverImpl trustResolver = new AuthenticationTrustResolverImpl();

    public AuthenticatedSessionService(SessionSecurityRepository repository) {
        this.repository = repository;
    }

    public AuthenticatedSession requireAuthenticatedSession(HttpServletRequest request) {
        var session = request.getSession(false);
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (session == null || !trustResolver.isAuthenticated(authentication)) {
            throw new InsufficientAuthenticationException("로그인이 필요합니다.");
        }
        if (!(session.getAttribute(ACCOUNT_ID) instanceof UUID accountId)
                || !(session.getAttribute(SECURITY_CONTEXT_ID) instanceof UUID contextId)
                || !accountId.toString().equals(authentication.getName())
                || !(session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY)
                    instanceof SecurityContext storedContext)
                || !trustResolver.isAuthenticated(storedContext.getAuthentication())
                || !accountId.toString().equals(storedContext.getAuthentication().getName())
                || !repository.isCurrentContext(accountId, contextId)) {
            throw new InsufficientAuthenticationException("로그인이 필요합니다.");
        }
        return new AuthenticatedSession(accountId, contextId);
    }
}
