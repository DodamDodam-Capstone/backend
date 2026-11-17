package com.dodamdodam.backend.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.dodamdodam.backend.auth.domain.EmailAddress;
import com.dodamdodam.backend.auth.exception.PasswordLoginException;
import jakarta.validation.Validator;
import java.util.Set;
import tools.jackson.databind.JsonNode;

public record LoginRequest(String email, @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) String password) {
    private static final Set<String> FIELDS = Set.of("email", "password");

    /** 허용 필드·타입을 확인한 뒤 이메일만 정규화하고 비밀번호 원문을 보존한다. */
    public static LoginRequest fromJson(JsonNode body, Validator validator) {
        if (body == null || !body.isObject() || !FIELDS.containsAll(body.propertyNames())) {
            throw PasswordLoginException.validation("body");
        }
        var email = body.get("email");
        if (email == null || !email.isString()) {
            throw PasswordLoginException.validation("email");
        }
        var password = body.get("password");
        if (password == null || !password.isString() || password.stringValue().isEmpty()
                || !isValidUnicode(password.stringValue())) {
            throw PasswordLoginException.validation("password");
        }
        try {
            return new LoginRequest(EmailAddress.normalize(email.stringValue(), validator), password.stringValue());
        }
        catch (IllegalArgumentException invalid) {
            throw PasswordLoginException.validation("email");
        }
    }

    /** UTF-8 변환에서 조용히 대체될 단독 surrogate를 거부하고 정상 쌍은 그대로 둔다. */
    private static boolean isValidUnicode(String value) {
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (Character.isHighSurrogate(current)) {
                if (++index >= value.length() || !Character.isLowSurrogate(value.charAt(index))) {
                    return false;
                }
            }
            else if (Character.isLowSurrogate(current)) {
                return false;
            }
        }
        return true;
    }

    /** 프레임워크·진단 로그에서 이메일과 비밀번호를 숨긴다. */
    @Override
    public String toString() {
        return "LoginRequest[redacted]";
    }
}
