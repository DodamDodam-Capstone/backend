package com.dodamdodam.backend.auth.dto;

import com.dodamdodam.backend.auth.exception.EmailVerificationException;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Email;
import java.util.Locale;
import java.util.Set;
import tools.jackson.databind.JsonNode;

public record IssueEmailVerificationRequest(Purpose purpose, @Email String email) {

    private static final Set<String> FIELDS = Set.of("purpose", "email");

    /** 목적과 email만 파싱하며 PIN 목적의 email 생략과 명시적 null을 구분한다. */
    public static IssueEmailVerificationRequest fromJson(JsonNode body) {
        // 계정·보안 문맥은 서버가 결정하므로 요청에서 추가로 받지 않는다.
        if (body == null || !body.isObject() || !FIELDS.containsAll(body.propertyNames())) {
            throw EmailVerificationException.validation("body");
        }
        var suppliedPurpose = body.get("purpose");
        if (suppliedPurpose == null || !suppliedPurpose.isString()) {
            throw EmailVerificationException.validation("purpose");
        }
        Purpose purpose;
        try {
            purpose = Purpose.valueOf(suppliedPurpose.stringValue());
        }
        catch (IllegalArgumentException invalid) {
            throw EmailVerificationException.validation("purpose");
        }
        var suppliedEmail = body.get("email");
        if (suppliedEmail != null && !suppliedEmail.isString()) {
            throw EmailVerificationException.validation("email");
        }
        return new IssueEmailVerificationRequest(purpose,
                suppliedEmail == null ? null : suppliedEmail.stringValue());
    }

    /** 서비스 처리 전에 호출하며 정규화한 이메일의 형식·길이·목적별 필수 여부를 검사한다. */
    public IssueEmailVerificationRequest normalized(Validator validator) {
        if (purpose == null) {
            throw EmailVerificationException.validation("purpose");
        }
        if (email == null) {
            if (purpose == Purpose.SIGNUP || purpose == Purpose.RESET_PASSWORD) {
                throw EmailVerificationException.validation("email");
            }
            // PIN 목적의 생략 값은 후속 서비스에서 현재 계정 이메일로 채운다.
            return this;
        }
        var normalizedEmail = trimEmail(email).toLowerCase(Locale.ROOT);
        // provider가 허용하는 국제화 이메일도 서비스의 ASCII 식별 정책으로 제한한다.
        if (normalizedEmail.isEmpty() || normalizedEmail.length() > 254
                || !normalizedEmail.chars().allMatch(character -> character >= 0x21 && character <= 0x7e)
                || !validator.validateValue(IssueEmailVerificationRequest.class, "email", normalizedEmail).isEmpty()) {
            throw EmailVerificationException.validation("email");
        }
        return new IssueEmailVerificationRequest(purpose, normalizedEmail);
    }

    /** 자녀 입력과 같은 Unicode 공백 기준으로 양끝만 제거하며 점·+suffix는 보존한다. */
    private static String trimEmail(String value) {
        int start = 0;
        int end = value.length();
        while (start < end && isSpace(value.codePointAt(start))) {
            start += Character.charCount(value.codePointAt(start));
        }
        while (end > start && isSpace(value.codePointBefore(end))) {
            end -= Character.charCount(value.codePointBefore(end));
        }
        return value.substring(start, end);
    }

    /** NBSP를 포함한 바깥 공백을 판정하고 NUL 등 제어 문자는 보정하지 않는다. */
    private static boolean isSpace(int codePoint) {
        return Character.isWhitespace(codePoint) || Character.isSpaceChar(codePoint);
    }

    public enum Purpose {
        SIGNUP, RESET_PASSWORD, PIN_SETUP, PIN_RESET
    }
}
