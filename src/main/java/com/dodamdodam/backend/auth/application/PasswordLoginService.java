package com.dodamdodam.backend.auth.application;

import com.dodamdodam.backend.auth.dto.AccountView;
import com.dodamdodam.backend.auth.dto.LoginRequest;
import com.dodamdodam.backend.auth.exception.PasswordLoginException;
import com.dodamdodam.backend.auth.infrastructure.EmailVerificationKeys;
import com.dodamdodam.backend.auth.infrastructure.PasswordLoginRepository;
import com.dodamdodam.backend.auth.session.AuthStateUnavailableException;
import com.dodamdodam.backend.auth.session.AuthenticatedSessionService;
import com.dodamdodam.backend.auth.session.LoginSessionAuthenticationStrategy;
import com.dodamdodam.backend.global.web.ClientAddressResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;

@Service
public class PasswordLoginService {
    private static final Pattern STORED_HASH = Pattern.compile("\\{pbkdf2-sha256-v1\\}[0-9a-f]{96}");
    private final PasswordLoginRepository repository;
    private final PasswordEncoder passwords;
    private final AuthRateLimitService limits;
    private final EmailVerificationKeys keys;
    private final LoginSessionAuthenticationStrategy strategy;
    private final SecurityContextRepository contexts;
    private final AuthenticatedSessionService authentication;
    private final CurrentAccountService accounts;
    private final String dummyHash;

    /** 공통 세션·예산·encoder를 연결하고 비존재 계정용 해시는 프로세스에서 한 번만 만든다. */
    public PasswordLoginService(PasswordLoginRepository repository, PasswordEncoder passwords,
                               AuthRateLimitService limits, EmailVerificationKeys keys,
                               LoginSessionAuthenticationStrategy strategy, SecurityContextRepository contexts,
                               AuthenticatedSessionService authentication, CurrentAccountService accounts) {
        this.repository = repository;
        this.passwords = passwords;
        this.limits = limits;
        this.keys = keys;
        this.strategy = strategy;
        this.contexts = contexts;
        this.authentication = authentication;
        this.accounts = accounts;
        this.dummyHash = passwords.encode(UUID.randomUUID().toString());
    }

    /** 구조 검증·정규화가 끝난 입력의 자격을 확인하고 실제 저장된 새 세션의 계정 상태만 반환한다. */
    public AccountView login(LoginRequest input, HttpServletRequest request, HttpServletResponse response) {
        // 계정 존재 여부나 성공 여부를 확인하기 전에 두 업무 예산을 독립 커밋한다.
        limits.reserve(keys.rateHasher().loginKeys(input.email(), ClientAddressResolver.resolve(request)));
        var credential = repository.findCredential(input.email()).orElse(null);
        var storedHash = credential == null ? null : credential.passwordHash();
        if (storedHash != null && !STORED_HASH.matcher(storedHash).matches()) {
            throw new AuthStateUnavailableException();
        }
        boolean matched;
        try {
            // 소셜 전용도 비존재 계정과 같은 실제 KDF 연산을 거치며 정시간 실행을 보장하지는 않는다.
            matched = passwords.matches(input.password(), storedHash == null ? dummyHash : storedHash);
        }
        catch (RuntimeException unavailable) {
            // encoder 예외에는 비밀번호·해시가 포함될 수 있어 외부 원문을 전달하지 않는다.
            throw new AuthStateUnavailableException();
        }
        if (!matched || storedHash == null) {
            throw PasswordLoginException.failed();
        }

        var principal = UsernamePasswordAuthenticationToken.authenticated(credential.accountId().toString(), null,
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
        // 비교 뒤 reset이 일어나면 계정 잠금 안의 해시·버전 재검사가 이전 세션 교체를 막는다.
        strategy.onPasswordAuthentication(principal, storedHash, credential.sessionVersion(), request, response);
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(principal);
        SecurityContextHolder.setContext(context);
        contexts.saveContext(context, request, response);
        return accounts.getCurrentAccount(authentication.requireAuthenticatedSession(request));
    }
}
