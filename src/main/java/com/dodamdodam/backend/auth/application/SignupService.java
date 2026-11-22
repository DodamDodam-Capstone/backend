package com.dodamdodam.backend.auth.application;

import com.dodamdodam.backend.auth.dto.IssueEmailVerificationRequest.Purpose;
import com.dodamdodam.backend.auth.dto.SignupRequest;
import com.dodamdodam.backend.auth.dto.SignupResult;
import com.dodamdodam.backend.auth.exception.EmailVerificationException;
import com.dodamdodam.backend.auth.infrastructure.SignupRepository;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SignupService {
    private final EmailVerificationService verification;
    private final SignupRepository accounts;
    private final PasswordEncoder passwords;

    /** 일회성 토큰 소비와 기존 로그인용 비밀번호 인코더를 연결한다. */
    public SignupService(EmailVerificationService verification, SignupRepository accounts, PasswordEncoder passwords) {
        this.verification = verification;
        this.accounts = accounts;
        this.passwords = passwords;
    }

    /** 검증된 이메일로 계정을 생성하고 저장에 실패하면 토큰 소비도 되돌린다. */
    @Transactional
    public SignupResult signup(SignupRequest input) {
        var grant = verification.consume(input.verificationToken(), Purpose.SIGNUP, null);
        var id = UUID.randomUUID();
        var hash = passwords.encode(input.password());
        if (!accounts.insert(id, grant.email(), hash)) {
            throw EmailVerificationException.accountAlreadyExists();
        }
        return new SignupResult(id);
    }
}
