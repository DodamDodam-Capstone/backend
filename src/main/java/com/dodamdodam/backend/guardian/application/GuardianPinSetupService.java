package com.dodamdodam.backend.guardian.application;

import com.dodamdodam.backend.auth.application.EmailVerificationService;
import com.dodamdodam.backend.auth.dto.IssueEmailVerificationRequest.Purpose;
import com.dodamdodam.backend.auth.exception.EmailVerificationException;
import com.dodamdodam.backend.auth.infrastructure.EmailVerificationAccountRepository;
import com.dodamdodam.backend.auth.session.AuthenticatedSession;
import com.dodamdodam.backend.auth.session.SessionSecurityRepository;
import com.dodamdodam.backend.guardian.dto.GuardianPinSetupRequest;
import com.dodamdodam.backend.guardian.infrastructure.GuardianAccessRepository;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GuardianPinSetupService {
    private final SessionSecurityRepository sessions;
    private final EmailVerificationAccountRepository accounts;
    private final EmailVerificationService verification;
    private final GuardianAccessRepository guardian;
    private final Argon2PasswordEncoder pins = new Argon2PasswordEncoder(16, 32, 1, 19 * 1024, 2);

    /** 기존 로그인 문맥·이메일 증명과 보호자 PIN 저장을 연결한다. */
    public GuardianPinSetupService(SessionSecurityRepository sessions, EmailVerificationAccountRepository accounts,
                                   EmailVerificationService verification, GuardianAccessRepository guardian) {
        this.sessions = sessions;
        this.accounts = accounts;
        this.verification = verification;
        this.guardian = guardian;
    }

    /** 계정 잠금으로 동시 최초 생성을 직렬화하고 토큰 소비·PIN 생성·나머지 증명 폐기를 함께 커밋한다. */
    @Transactional
    public void setup(AuthenticatedSession current, GuardianPinSetupRequest input) {
        var accountId = current.accountId();
        sessions.requireCurrentContextForUpdate(accountId, current.securityContextId());
        if (accounts.lockPin(accountId)) {
            throw EmailVerificationException.pinAlreadySet();
        }
        if (input.verificationToken() == null) {
            throw EmailVerificationException.authorizationRequired(Purpose.PIN_SETUP);
        }

        verification.consume(input.verificationToken(), Purpose.PIN_SETUP, current);
        String hash = pins.encode(input.pin());
        // 해시 계산 중 유한 세션이 만료됐으면 소비도 함께 롤백한다.
        sessions.requireCurrentContextForUpdate(accountId, current.securityContextId());
        guardian.createFirstPin(accountId, hash);
    }
}
