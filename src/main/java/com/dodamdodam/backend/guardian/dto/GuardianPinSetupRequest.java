package com.dodamdodam.backend.guardian.dto;

import com.dodamdodam.backend.auth.application.EmailVerificationService;
import com.dodamdodam.backend.auth.exception.EmailVerificationException;
import tools.jackson.databind.JsonNode;

import java.util.Set;
import java.util.regex.Pattern;

public record GuardianPinSetupRequest(String pin, String verificationToken) {
    private static final Set<String> FIELDS = Set.of("pin", "verificationToken");
    private static final Pattern PIN = Pattern.compile("[0-9]{4}");

    /** 토큰 누락만 업무 판단에 넘기고 명시적 null과 형식 오류는 여기서 거부한다. */
    public static GuardianPinSetupRequest fromJson(JsonNode body) {
        if (body == null || !body.isObject() || !FIELDS.containsAll(body.propertyNames())) {
            throw EmailVerificationException.validation("body");
        }
        var pin = body.get("pin");
        if (pin == null || !pin.isString() || !PIN.matcher(pin.stringValue()).matches()) {
            throw EmailVerificationException.validation("pin");
        }
        var token = body.get("verificationToken");
        if (token != null && (!token.isString() || !EmailVerificationService.isTokenFormatValid(token.stringValue()))) {
            throw EmailVerificationException.validation("verificationToken");
        }

        return new GuardianPinSetupRequest(pin.stringValue(), token == null ? null : token.stringValue());
    }

    /** 진단 문자열에 PIN과 이메일 토큰이 포함되지 않게 한다. */
    @Override
    public String toString() {
        return "GuardianPinSetupRequest[redacted]";
    }
}
