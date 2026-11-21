package com.dodamdodam.backend.auth.dto;

import com.dodamdodam.backend.auth.exception.EmailVerificationException;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import tools.jackson.databind.JsonNode;

public record SignupRequest(
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) String verificationToken,
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) String password) {
    private static final Set<String> FIELDS = Set.of("verificationToken", "password");

    /** 가입 입력을 확인하고 비밀번호의 Unicode 원문을 보존한다. */
    public static SignupRequest fromJson(JsonNode body) {
        if (body == null || !body.isObject() || !FIELDS.containsAll(body.propertyNames())) {
            throw EmailVerificationException.validation("body");
        }
        var token = body.get("verificationToken");
        if (token == null || !token.isString()) {
            throw EmailVerificationException.validation("verificationToken");
        }
        var password = body.get("password");
        if (password == null || !password.isString()) {
            throw EmailVerificationException.validation("password");
        }
        var value = password.stringValue();
        int length = value.codePointCount(0, value.length());
        if (length < 15 || length > 128 || !StandardCharsets.UTF_8.newEncoder().canEncode(value)) {
            throw EmailVerificationException.validation("password");
        }
        return new SignupRequest(token.stringValue(), value);
    }

    /** 진단 문자열에 가입 자격 원문을 남기지 않는다. */
    @Override
    public String toString() {
        return "SignupRequest[redacted]";
    }
}
