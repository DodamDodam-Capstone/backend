package com.dodamdodam.backend.auth.domain;

import jakarta.validation.Validator;
import jakarta.validation.constraints.Email;
import java.util.Locale;

/** 발급·로그인·가입에서 같은 서비스 이메일 식별 규칙을 사용한다. */
public record EmailAddress(@Email String value) {

    /** 양끝 Unicode 공백과 대소문자만 보정하고 유효 ASCII·254자 정책을 검사한다. */
    public static String normalize(String value, Validator validator) {
        if (value == null) {
            throw new IllegalArgumentException("이메일 형식이 올바르지 않습니다.");
        }
        int start = 0;
        int end = value.length();
        while (start < end && isSpace(value.codePointAt(start))) {
            start += Character.charCount(value.codePointAt(start));
        }
        while (end > start && isSpace(value.codePointBefore(end))) {
            end -= Character.charCount(value.codePointBefore(end));
        }
        var normalized = value.substring(start, end).toLowerCase(Locale.ROOT);
        // 점·+suffix와 내부 제어 문자는 임의로 지우거나 provider 규칙으로 변경하지 않는다.
        if (normalized.isEmpty() || normalized.length() > 254
                || !normalized.chars().allMatch(character -> character >= 0x21 && character <= 0x7e)
                || !validator.validateValue(EmailAddress.class, "value", normalized).isEmpty()) {
            throw new IllegalArgumentException("이메일 형식이 올바르지 않습니다.");
        }
        return normalized;
    }

    /** NBSP를 포함한 바깥 공백만 판정한다. */
    private static boolean isSpace(int codePoint) {
        return Character.isWhitespace(codePoint) || Character.isSpaceChar(codePoint);
    }

    /** 진단 로그의 문자열 변환에 계정 이메일을 남기지 않는다. */
    @Override
    public String toString() {
        return "EmailAddress[redacted]";
    }
}
