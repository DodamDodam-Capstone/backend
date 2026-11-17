package com.dodamdodam.backend.auth.api;

import com.dodamdodam.backend.auth.session.AuthenticatedSessionService;
import com.dodamdodam.backend.auth.session.AuthStateUnavailableException;
import com.dodamdodam.backend.global.web.Part1JsonRequestGuardFilter;
import com.dodamdodam.backend.auth.session.SessionSecurityRepository;
import com.dodamdodam.backend.global.error.ApiErrorResponses;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import org.springframework.core.convert.ConversionException;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfLogoutHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.transaction.TransactionException;

@RestController
public class AuthLogoutController {

    private final AuthenticatedSessionService authenticatedSessions;
    private final SessionSecurityRepository securityRepository;
    private final SecurityContextLogoutHandler contextLogout = new SecurityContextLogoutHandler();
    private final CsrfLogoutHandler csrfLogout;

    /** 기존 세션·CSRF 정리 기능으로 로그아웃 API를 구성한다. */
    public AuthLogoutController(AuthenticatedSessionService authenticatedSessions, SessionSecurityRepository securityRepository,
                                SecurityContextRepository securityContextRepository, CookieCsrfTokenRepository csrfRepository) {
        this.authenticatedSessions = authenticatedSessions;
        this.securityRepository = securityRepository;
        contextLogout.setSecurityContextRepository(securityContextRepository);
        csrfLogout = new CsrfLogoutHandler(csrfRepository);
    }

    /** 인증과 빈 본문을 확인한 뒤 현재 문맥·세션·CSRF 쿠키를 폐기한다. */
    @PostMapping("/api/v1/auth/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        var current = authenticatedSessions.requireAuthenticatedSession(request);
        if (!Boolean.TRUE.equals(request.getAttribute(Part1JsonRequestGuardFilter.EMPTY_BODY))) {
            return ResponseEntity.badRequest().body(ApiErrorResponses.body(
                    response, "VALIDATION_FAILED", "요청 본문은 비어 있어야 합니다.", List.of()));
        }
        try {
            if (!securityRepository.revokeCurrentContext(current.accountId(), current.securityContextId())) {
                throw new InsufficientAuthenticationException("로그인이 필요합니다.");
            }
            var authentication = SecurityContextHolder.getContext().getAuthentication();
            contextLogout.logout(request, response, authentication);
            csrfLogout.logout(request, response, authentication);
            return ResponseEntity.noContent().build();
        }
        catch (DataAccessException | TransactionException | ConversionException failure) {
            // 문맥 폐기의 커밋과 뒤이은 네이티브 세션 삭제 실패도 인증 장애다.
            throw new AuthStateUnavailableException(failure);
        }
    }
}
