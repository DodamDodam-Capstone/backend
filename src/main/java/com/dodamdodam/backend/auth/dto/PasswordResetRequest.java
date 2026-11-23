package com.dodamdodam.backend.auth.dto;

import com.dodamdodam.backend.auth.application.EmailVerificationService;
import com.dodamdodam.backend.auth.exception.EmailVerificationException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import tools.jackson.databind.JsonNode;

public record PasswordResetRequest(String verificationToken, String newPassword) {
    private static final Set<String> FIELDS = Set.of("verificationToken", "newPassword");

    /** 토큰 형식과 비밀번호 코드포인트 수를 검사하고 입력 문자열을 그대로 보존한다. */
    public static PasswordResetRequest fromJson(JsonNode body) {
        if (body == null || !body.isObject() || !FIELDS.containsAll(body.propertyNames())) {
            throw EmailVerificationException.validation("body");
        }
        var token = body.get("verificationToken");
        if (token == null || !token.isString() || !EmailVerificationService.isTokenFormatValid(token.stringValue())) {
            throw EmailVerificationException.validation("verificationToken");
        }
        var password = body.get("newPassword");
        if (password == null || !password.isString()) {
            throw EmailVerificationException.validation("newPassword");
        }
        String value = password.stringValue();
        int codePoints = value.codePointCount(0, value.length());
        if (codePoints < 15 || codePoints > 128 || !StandardCharsets.UTF_8.newEncoder().canEncode(value)) {
            throw EmailVerificationException.validation("newPassword");
        }
        return new PasswordResetRequest(token.stringValue(), value);
    }

    /** 진단 문자열에 토큰과 비밀번호를 노출하지 않는다. */
    @Override
    public String toString() {
        return "PasswordResetRequest[redacted]";
    }
}
