package com.dodamdodam.backend.auth.infrastructure;

import com.dodamdodam.backend.auth.domain.AuthRateLimitKey;
import com.dodamdodam.backend.auth.domain.AuthRateLimitKey.Action;
import com.dodamdodam.backend.auth.dto.IssueEmailVerificationRequest.Purpose;
import com.dodamdodam.backend.auth.session.AuthStateUnavailableException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class AuthRateLimitKeyHasher {

    private static final String ALGORITHM = "HmacSHA256";
    private final SecretKeySpec key;

    /** 번호 인증 키와 별도로 주입하는 요청 제한 전용 32바이트 키를 받는다. */
    public AuthRateLimitKeyHasher(byte[] key) {
        if (key == null || key.length != 32) {
            throw new AuthStateUnavailableException();
        }
        this.key = new SecretKeySpec(key, ALGORITHM);
    }

    /** 상위 진입부가 확인한 실제 IP를 사용하며 전달 헤더를 직접 신뢰하지 않는다. */
    public AuthRateLimitKey apiIp(InetAddress clientAddress) {
        return hash(Action.API_IP, addressScope(clientAddress));
    }

    /** 정규화·권한 검사한 범위로 누적 한도와 현재 문맥의 cooldown 키를 만든다. */
    public List<AuthRateLimitKey> emailIssueKeys(String normalizedEmail, Purpose purpose, UUID accountId,
                                               UUID securityContextId, InetAddress clientAddress) {
        if (purpose == null) {
            throw new AuthStateUnavailableException();
        }
        boolean pinPurpose = purpose == Purpose.PIN_SETUP || purpose == Purpose.PIN_RESET;
        if (pinPurpose && (accountId == null || securityContextId == null)
                || !pinPurpose && (normalizedEmail == null || normalizedEmail.isBlank())) {
            throw new AuthStateUnavailableException();
        }
        String subject = pinPurpose ? accountId.toString() : normalizedEmail;
        var subjectKey = hash(Action.EMAIL_SEND_SUBJECT, purpose.name(), subject);
        // 누적 한도에는 세션을 넣지 않아 새 세션으로 예산을 초기화하지 못하게 한다.
        var resendKey = pinPurpose
                ? hash(Action.EMAIL_RESEND, purpose.name(), subject, securityContextId.toString())
                : hash(Action.EMAIL_RESEND, purpose.name(), subject);
        return List.of(subjectKey, hash(Action.EMAIL_SEND_IP, addressScope(clientAddress)), resendKey);
    }

    /** 같은 IP의 축약 표기가 달라도 주소 바이트가 같으면 같은 예산을 사용한다. */
    private String addressScope(InetAddress address) {
        if (address == null) {
            throw new AuthStateUnavailableException();
        }
        return HexFormat.of().formatHex(address.getAddress());
    }

    /** action과 각 UTF-8 필드의 길이를 포함해 경계가 모호하지 않은 전용 HMAC을 만든다. */
    private AuthRateLimitKey hash(Action action, String... scope) {
        try {
            var mac = Mac.getInstance(ALGORITHM);
            mac.init(key);
            mac.update("auth-rate:v1\0".getBytes(StandardCharsets.UTF_8));
            updateField(mac, action.name());
            for (String component : scope) {
                updateField(mac, component);
            }
            return new AuthRateLimitKey(action, mac.doFinal());
        }
        catch (GeneralSecurityException failure) {
            throw new AuthStateUnavailableException(failure);
        }
    }

    /** 문자열 구분자 충돌 없이 필드 길이와 원문 바이트를 순서대로 계산에만 사용한다. */
    private void updateField(Mac mac, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        mac.update(ByteBuffer.allocate(Integer.BYTES).putInt(bytes.length).array());
        mac.update(bytes);
    }
}
