package com.dodamdodam.backend.auth.application;

import com.dodamdodam.backend.auth.dto.IssueEmailVerificationRequest.Purpose;
import com.dodamdodam.backend.auth.dto.PasswordResetRequest;
import com.dodamdodam.backend.auth.exception.EmailVerificationException;
import com.dodamdodam.backend.auth.infrastructure.EmailVerificationAccountRepository;
import com.dodamdodam.backend.auth.infrastructure.EmailVerificationRepository;
import com.dodamdodam.backend.auth.session.AuthStateUnavailableException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PasswordResetService {
    private final EmailVerificationService verification;
    private final EmailVerificationAccountRepository accounts;
    private final EmailVerificationRepository proofs;
    private final PasswordEncoder passwords;

    /** 현재 인증 증명과 로그인에서 사용하는 비밀번호 인코더를 연결한다. */
    public PasswordResetService(EmailVerificationService verification, EmailVerificationAccountRepository accounts,
                                EmailVerificationRepository proofs, PasswordEncoder passwords) {
        this.verification = verification;
        this.accounts = accounts;
        this.proofs = proofs;
        this.passwords = passwords;
    }

    /** 계정을 먼저 잠그는 토큰 소비와 해시·세션 버전 변경, PIN 증명 폐기를 함께 커밋한다. */
    @Transactional
    public void reset(PasswordResetRequest input) {
        var account = verification.consume(input.verificationToken(), Purpose.RESET_PASSWORD, null).resetAccountId();
        String hash;
        try {
            hash = passwords.encode(input.newPassword());
        }
        catch (RuntimeException unavailable) {
            // 인코더 예외에 포함될 수 있는 비밀번호와 해시를 전달하지 않는다.
            throw new AuthStateUnavailableException();
        }
        if (!accounts.replacePassword(account, hash)) {
            // 기존 비밀번호가 없으면 재설정 증명 소비를 롤백한다.
            throw EmailVerificationException.passwordResetNotAvailable();
        }
        proofs.invalidateAccountPinProofs(account);
    }
}
