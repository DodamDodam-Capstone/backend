package com.dodamdodam.backend.auth;

import com.dodamdodam.backend.auth.session.AuthenticatedSessionService;
import com.dodamdodam.backend.auth.session.LogoutRequestGuardFilter;
import com.dodamdodam.backend.auth.session.SessionSecurityRepository;
import com.dodamdodam.backend.global.error.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfLogoutHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthLogoutController {

    private final AuthenticatedSessionService authenticatedSessions;
    private final SessionSecurityRepository securityRepository;
    private final SecurityContextLogoutHandler contextLogout = new SecurityContextLogoutHandler();
    private final CsrfLogoutHandler csrfLogout;

    public AuthLogoutController(AuthenticatedSessionService authenticatedSessions, SessionSecurityRepository securityRepository,
                                SecurityContextRepository securityContextRepository, CookieCsrfTokenRepository csrfRepository) {
        this.authenticatedSessions = authenticatedSessions;
        this.securityRepository = securityRepository;
        contextLogout.setSecurityContextRepository(securityContextRepository);
        csrfLogout = new CsrfLogoutHandler(csrfRepository);
    }

    @PostMapping("/api/v1/auth/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        var current = authenticatedSessions.requireAuthenticatedSession(request);
        if (!Boolean.TRUE.equals(request.getAttribute(LogoutRequestGuardFilter.EMPTY_BODY))) {
            return ResponseEntity.badRequest().body(new ApiErrorResponse(new ApiErrorResponse.ErrorDetail(
                    "VALIDATION_FAILED", "요청 본문은 비어 있어야 합니다.", response.getHeader("X-Request-ID"), List.of())));
        }
        if (!securityRepository.revokeCurrentContext(current.accountId(), current.securityContextId())) {
            throw new InsufficientAuthenticationException("로그인이 필요합니다.");
        }
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        contextLogout.logout(request, response, authentication);
        csrfLogout.logout(request, response, authentication);
        return ResponseEntity.noContent().build();
    }
}
