package com.dodamdodam.backend.auth.session;

import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.springframework.core.convert.ConversionException;
import org.springframework.dao.DataAccessException;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.session.Session;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionException;

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

    /** 실제 세션과 업무 문맥을 확인하고 저장소 장애를 인증 무효와 구분한다. */
    public AuthenticatedSession requireAuthenticatedSession(HttpServletRequest request) {
        try {
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
        catch (DataAccessException | TransactionException | ConversionException failure) {
            throw new AuthStateUnavailableException(failure);
        }
    }

    /** 저장된 세션을 다시 검증하며 인증 저장소 확인 실패를 명시적으로 전달한다. */
    void requireStoredSession(Session stored, AuthenticatedSession expected) {
        try {
            if (stored == null) {
                throw new InsufficientAuthenticationException("로그인이 필요합니다.");
            }
            Object context = stored.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
            var authentication = context instanceof SecurityContext security ? security.getAuthentication() : null;
            var actual = validate(authentication, stored.getAttribute(ACCOUNT_ID), stored.getAttribute(SECURITY_CONTEXT_ID), context);
            if (!expected.equals(actual)) {
                throw new InsufficientAuthenticationException("로그인이 필요합니다.");
            }
        }
        catch (DataAccessException | TransactionException | ConversionException failure) {
            throw new AuthStateUnavailableException(failure);
        }
    }

    // 현재 요청에 쿠키 갱신용 정보를 남깁니다.
    static void markCookieRenewal(HttpServletRequest request, String sessionId, AuthenticatedSession context) {
        request.setAttribute(COOKIE_RENEWAL, new CookieRenewal(sessionId, context));
    }

    //다음 조건을 모두 만족해야 인증을 허용합니다.
    // - 현재 인증 정보가 유효함
    // - 계정 ID와 문맥 ID가 실제 UUID 타입임
    // - 현재 principal 이름이 계정 UUID와 같음
    // - 세션에도 유효한 SecurityContext가 저장되어 있음
    // - 저장된 principal도 같은 계정을 가리킴
    // - DB의 해당 문맥이 현재 유효함
    // 하나라도 실패하면 인증 예외를 던지고, 모두 통과하면 AuthenticatedSession을 만듭니다.
    /** 전달받은 인증·세션 저장 정보·DB 문맥을 대조하고 하나라도 불일치하면 인증을 거부한다. */
    private AuthenticatedSession validate(Authentication authentication, Object account, Object context, Object security) {
        // 전달받은 인증 정보가 익명이 아닌 인증 완료 상태인지 확인한다.
        if (!trustResolver.isAuthenticated(authentication)
                // 세션의 계정 ID가 누락되지 않은 UUID인지 확인한다.
                || !(account instanceof UUID accountId)
                // 세션의 로그인 문맥 ID가 누락되지 않은 UUID인지 확인한다.
                || !(context instanceof UUID contextId)
                // 전달받은 principal이 세션의 계정 ID와 같은지 확인한다.
                || !accountId.toString().equals(authentication.getName())
                // 세션의 보안 정보가 SecurityContext 타입으로 저장되어 있는지 확인한다.
                || !(security instanceof SecurityContext storedContext)
                // 저장된 인증 정보도 익명이 아닌 인증 완료 상태인지 확인한다.
                || !trustResolver.isAuthenticated(storedContext.getAuthentication())
                // 저장된 principal도 같은 계정을 가리키는지 확인한다.
                || !accountId.toString().equals(storedContext.getAuthentication().getName())
                // DB에서 계정·문맥 연결, 미폐기, 버전 일치와 만료 시각을 확인한다.
                || !repository.isCurrentContext(accountId, contextId)) {
            // 어느 조건이든 실패하면 이 요청의 인증을 거부한다.
            throw new InsufficientAuthenticationException("로그인이 필요합니다.");
        }
        return new AuthenticatedSession(accountId, contextId);
    }

    record CookieRenewal(String sessionId, AuthenticatedSession context) { }
}
