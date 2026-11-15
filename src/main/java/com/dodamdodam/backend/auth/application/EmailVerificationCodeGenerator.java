package com.dodamdodam.backend.auth.application;

import java.security.SecureRandom;
import java.util.Locale;

public final class EmailVerificationCodeGenerator {

    private final SecureRandom random;

    /** 운영 생성에는 암호학적으로 안전한 난수만 사용한다. */
    public EmailVerificationCodeGenerator() {
        this(new SecureRandom());
    }

    /** 같은 패키지 테스트에서 난수 경계를 재현한다. */
    EmailVerificationCodeGenerator(SecureRandom random) {
        this.random = random;
    }

    /** 선행 0과 ASCII 형식을 보존한 정확히 6자리 인증번호를 생성한다. */
    public String generate() {
        return String.format(Locale.ROOT, "%06d", random.nextInt(1_000_000));
    }
}
