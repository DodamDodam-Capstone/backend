package com.dodamdodam.backend.auth.api;

import com.dodamdodam.backend.auth.application.CurrentAccountService;
import com.dodamdodam.backend.auth.dto.AccountView;
import com.dodamdodam.backend.auth.session.AuthenticatedSessionService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthMeController {

    private final AuthenticatedSessionService authentication;
    private final CurrentAccountService accounts;

    /** 기존 세션 인증과 현재 계정 조회를 연결한다. */
    public AuthMeController(AuthenticatedSessionService authentication, CurrentAccountService accounts) {
        this.authentication = authentication;
        this.accounts = accounts;
    }

    /** 현재 로그인 계정의 진입 상태만 반환하며 아이·PIN 설정 여부로 접근을 거부하지 않는다. */
    @GetMapping("/api/v1/auth/me")
    public Map<String, AccountView> getCurrentAccount(HttpServletRequest request) {
        var current = authentication.requireAuthenticatedSession(request);
        return Map.of("data", accounts.getCurrentAccount(current));
    }
}
