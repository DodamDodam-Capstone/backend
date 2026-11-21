package com.dodamdodam.backend.guardian.application;

import com.dodamdodam.backend.auth.session.AuthStateUnavailableException;
import java.util.Base64;
import java.util.regex.Pattern;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
class GuardianPinHasher {
    private static final int SALT_BYTES = 16;
    private static final int HASH_BYTES = 32;
    private static final int PARALLELISM = 1;
    private static final int MEMORY_KIB = 19 * 1024;
    private static final int ITERATIONS = 2;
    private static final String PREFIX = "$argon2id$v=19$m=" + MEMORY_KIB
            + ",t=" + ITERATIONS + ",p=" + PARALLELISM + "$";
    private static final Pattern HASH_FORMAT = Pattern.compile(
            Pattern.quote(PREFIX) + "([A-Za-z0-9+/]{22})\\$([A-Za-z0-9+/]{43})");
    private final Argon2PasswordEncoder encoder =
            new Argon2PasswordEncoder(SALT_BYTES, HASH_BYTES, PARALLELISM, MEMORY_KIB, ITERATIONS);

    /** 최초 설정에서 사용할 기존 Argon2id 포맷으로 PIN 해시를 만든다. */
    String encode(String pin) {
        return encoder.encode(pin);
    }

    /** 저장소 오류가 PIN 오답으로 기록되지 않도록 비교 전에 저장 포맷을 확인한다. */
    boolean matches(String pin, String hash) {
        var format = HASH_FORMAT.matcher(hash == null ? "" : hash);
        if (!format.matches() || !isCanonicalBase64(format.group(1), SALT_BYTES)
                || !isCanonicalBase64(format.group(2), HASH_BYTES)) {
            throw new AuthStateUnavailableException();
        }
        return encoder.matches(pin, hash);
    }

    /** 기존 저장 형식과 같은 패딩 없는 표준 Base64만 허용한다. */
    private boolean isCanonicalBase64(String encoded, int expectedBytes) {
        var bytes = Base64.getDecoder().decode(encoded);
        return bytes.length == expectedBytes && Base64.getEncoder().withoutPadding().encodeToString(bytes).equals(encoded);
    }
}
