package com.dodamdodam.backend.auth.dto;

import com.dodamdodam.backend.auth.exception.EmailVerificationException;
import com.dodamdodam.backend.auth.domain.EmailAddress;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Email;
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
        try {
            return new IssueEmailVerificationRequest(purpose, EmailAddress.normalize(email, validator));
        }
        catch (IllegalArgumentException invalid) {
            throw EmailVerificationException.validation("email");
        }
    }

    public enum Purpose {
        SIGNUP, RESET_PASSWORD, PIN_SETUP, PIN_RESET
    }
}
