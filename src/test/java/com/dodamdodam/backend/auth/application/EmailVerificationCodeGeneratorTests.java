package com.dodamdodam.backend.auth.application;

import java.security.SecureRandom;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import static org.assertj.core.api.Assertions.assertThat;

class EmailVerificationCodeGeneratorTests {

    @Test
    void preservesAllLeadingZeros() {
        assertThat(new EmailVerificationCodeGenerator(new FixedRandom(7)).generate()).isEqualTo("000007");
        assertThat(new EmailVerificationCodeGenerator(new FixedRandom(0)).generate()).isEqualTo("000000");
    }

    @Test
    @ResourceLock("java.util.Locale.default")
    void upperBoundRemainsSixAsciiDigitsUnderArabicLocale() {
        var previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("ar-EG"));
            assertThat(new EmailVerificationCodeGenerator(new FixedRandom(999_999)).generate())
                    .isEqualTo("999999");
        }
        finally {
            Locale.setDefault(previous);
        }
    }

    @Test
    void defaultGeneratorProducesSixAsciiDigits() {
        var generator = new EmailVerificationCodeGenerator();
        for (int i = 0; i < 32; i++) {
            assertThat(generator.generate()).matches("[0-9]{6}");
        }
    }

    // 난수 분포를 추측하지 않고 실제 생성기의 경계 포맷만 재현한다.
    private static final class FixedRandom extends SecureRandom {
        private final int value;

        private FixedRandom(int value) {
            this.value = value;
        }

        @Override
        public int nextInt(int bound) {
            if (bound != 1_000_000) {
                throw new IllegalArgumentException("잘못된 인증번호 범위");
            }
            return value;
        }
    }
}
