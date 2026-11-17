package com.dodamdodam.backend.auth.infrastructure;

import com.dodamdodam.backend.auth.session.AuthStateUnavailableException;
import java.util.Arrays;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EmailVerificationCodeHasherTests {

    // 공개된 테스트 벡터용 입력이며 운영 키를 주입하거나 기록하지 않는다.
    private static final byte[] KEY = HexFormat.of().parseHex(
            "000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f");
    private static final String HASH = "d41fbbc10cb81c229c47cc0351a0f4e0bf3385f962c73e2ccc882a6aee2b3c02";

    @Test
    void hashesExactSixCharacterCodeUsingHmacSha256() {
        var hasher = new EmailVerificationCodeHasher(KEY);
        assertThat(HexFormat.of().formatHex(hasher.hash("000007"))).isEqualTo(HASH);
        assertThat(HexFormat.of().formatHex(hasher.hash("000008")))
                .isEqualTo("a4d16c200555e55c00cc3d5bd30f48fe6b3555bd4816079fe966f6e54c05f554");
    }

    @Test
    void keyAndResultArraysCannotChangeLaterHashes() {
        var key = KEY.clone();
        var hasher = new EmailVerificationCodeHasher(key);
        Arrays.fill(key, (byte) 99);
        Arrays.fill(hasher.hash("000007"), (byte) 0);
        assertThat(HexFormat.of().formatHex(hasher.hash("000007"))).isEqualTo(HASH);
        assertThat(HexFormat.of().formatHex(new EmailVerificationCodeHasher(key).hash("000007")))
                .isNotEqualTo(HASH);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 16, 31, 33, 64})
    void rejectsWrongKeyLengthAsAuthStateFailure(int length) {
        assertThrows(AuthStateUnavailableException.class, () -> new EmailVerificationCodeHasher(new byte[length]));
    }

    @Test
    void rejectsMissingKeyWithoutFallback() {
        assertThrows(AuthStateUnavailableException.class, () -> new EmailVerificationCodeHasher(null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"7", "00007", "0000007", " 000007", "000007\n", "０００００７", "00000a"})
    void doesNotNormalizeOrCoerceCode(String code) {
        var failure = assertThrows(IllegalArgumentException.class,
                () -> new EmailVerificationCodeHasher(KEY).hash(code));
        assertThat(failure.getMessage()).doesNotContain(code);
    }

    @Test
    void rejectsNullCode() {
        assertThrows(IllegalArgumentException.class, () -> new EmailVerificationCodeHasher(KEY).hash(null));
    }
}
