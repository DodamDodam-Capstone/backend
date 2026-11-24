package com.dodamdodam.backend.auth.dto;

import com.dodamdodam.backend.auth.exception.EmailVerificationException;
import tools.jackson.databind.JsonNode;

import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

public record VerifyEmailCodeRequest(UUID challengeId, String code) {

    private static final Set<String> FIELDS = Set.of("challengeId", "code");
    private static final Pattern CODE = Pattern.compile("[0-9]{6}");

    /** 목적을 조회하는 데 필요한 UUID만 먼저 읽는다. */
    public static UUID readChallengeId(JsonNode body) {
        if (body == null || !body.isObject()) {
            throw EmailVerificationException.validation("body");
        }
        var value = body.get("challengeId");
        if (value == null || !value.isString()) {
            throw EmailVerificationException.validation("challengeId");
        }

        String text = value.stringValue();
        UUID id;
        try {
            id = UUID.fromString(text);
        }
        catch (IllegalArgumentException invalid) {
            throw EmailVerificationException.validation("challengeId");
        }
        if (!id.toString().equalsIgnoreCase(text)) {
            throw EmailVerificationException.validation("challengeId");
        }

        return id;
    }

    /** 목적에 따른 로그인 확인 뒤 닫힌 필드 목록과 정확한 ASCII 번호를 검사한다. */
    public static String readCode(JsonNode body) {
        if (!FIELDS.containsAll(body.propertyNames())) {
            throw EmailVerificationException.validation("body");
        }
        var code = body.get("code");
        if (code == null || !code.isString() || !CODE.matcher(code.stringValue()).matches()) {
            throw EmailVerificationException.validation("code");
        }
        return code.stringValue();
    }

    /** 프레임워크 진단 문자열에 인증번호 원문을 남기지 않는다. */
    @Override
    public String toString() {
        return "VerifyEmailCodeRequest[challengeId=" + challengeId + "]";
    }
}
