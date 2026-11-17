package com.dodamdodam.backend.auth.infrastructure;

import com.dodamdodam.backend.auth.session.AuthStateUnavailableException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Arrays;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class EmailVerificationKeys {
    private final AuthRateLimitKeyHasher rate;
    private final EmailVerificationCodeHasher code;

    /** 프로세스 시작 시 전용 파일을 한 번 읽고 미설정 기능만 안전하게 닫는다. */
    public EmailVerificationKeys(Environment environment) {
        byte[] rateKey = readKey(environment.getProperty("app.security.rate-limit-key-file", ""));
        byte[] codeKey = readKey(environment.getProperty("app.email.code-key-file", ""));
        this.rate = rateKey == null ? null : new AuthRateLimitKeyHasher(rateKey);
        this.code = rateKey == null || codeKey == null || MessageDigest.isEqual(rateKey, codeKey)
                ? null : new EmailVerificationCodeHasher(codeKey);
        if (rateKey != null) {
            Arrays.fill(rateKey, (byte) 0);
        }
        if (codeKey != null) {
            Arrays.fill(codeKey, (byte) 0);
        }
    }

    /** 전역 제한 키가 없으면 무제한 처리를 허용하지 않는다. */
    public AuthRateLimitKeyHasher rateHasher() {
        if (rate == null) {
            throw new AuthStateUnavailableException();
        }
        return rate;
    }

    /** 번호 키가 없거나 제한 키와 같으면 발급을 거부한다. */
    public EmailVerificationCodeHasher codeHasher() {
        if (code == null) {
            throw new AuthStateUnavailableException();
        }
        return code;
    }

    /** 파일 경로·내용을 예외에 복사하지 않고 raw 32바이트만 읽는다. */
    private static byte[] readKey(String file) {
        if (file.isBlank()) {
            return null;
        }
        try (var input = Files.newInputStream(Path.of(file))) {
            var bytes = input.readNBytes(33);
            return bytes.length == 32 ? bytes : null;
        }
        catch (IOException | RuntimeException unavailable) {
            return null;
        }
    }
}
