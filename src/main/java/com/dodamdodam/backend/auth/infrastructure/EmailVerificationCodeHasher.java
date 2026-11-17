package com.dodamdodam.backend.auth.infrastructure;

import com.dodamdodam.backend.auth.session.AuthStateUnavailableException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.regex.Pattern;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class EmailVerificationCodeHasher {

    private static final Pattern CODE = Pattern.compile("[0-9]{6}");
    private static final String ALGORITHM = "HmacSHA256";
    private final SecretKeySpec key;

    /** 외부 주입한 이메일 인증 전용 32바이트 키만 받으며 임의 대체 키를 만들지 않는다. */
    public EmailVerificationCodeHasher(byte[] key) {
        if (key == null || key.length != 32) {
            throw new AuthStateUnavailableException();
        }
        // SecretKeySpec이 복사하므로 호출자가 원본 배열을 바꿔도 운영 키는 유지된다.
        this.key = new SecretKeySpec(key, ALGORITHM);
    }

    /** 코드 원문을 보정·보관하지 않고 DB code_hash용 32바이트 HMAC을 반환한다. */
    public byte[] hash(String code) {
        if (code == null || !CODE.matcher(code).matches()) {
            throw new IllegalArgumentException("인증번호는 정확히 6자리 ASCII 숫자여야 합니다.");
        }
        try {
            // Mac은 요청마다 생성해 동시 발급 사이에 내부 계산 상태를 공유하지 않는다.
            var mac = Mac.getInstance(ALGORITHM);
            mac.init(key);
            return mac.doFinal(code.getBytes(StandardCharsets.UTF_8));
        }
        catch (GeneralSecurityException unavailable) {
            throw new AuthStateUnavailableException(unavailable);
        }
    }
}
