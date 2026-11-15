package com.dodamdodam.backend.auth.exception;

import com.dodamdodam.backend.global.error.ApiErrorResponse.FieldError;
import java.util.List;

public final class EmailVerificationException extends RuntimeException {

    private final List<FieldError> fields;

    /** 입력 원문 대신 검증에 실패한 필드만 보관한다. */
    private EmailVerificationException(String field) {
        super("입력값을 확인해 주세요.");
        this.fields = List.of(new FieldError(field, "유효하지 않은 값입니다."));
    }

    /** 요청 구조·값 검증에 공통으로 사용할 오류를 만든다. */
    public static EmailVerificationException validation(String field) {
        return new EmailVerificationException(field);
    }

    /** HTTP 오류 연결에 사용할 고정 검증 코드를 반환한다. */
    public String code() {
        return "VALIDATION_FAILED";
    }

    /** 이메일·번호 원문이 없는 필드 오류를 반환한다. */
    public List<FieldError> fields() {
        return fields;
    }
}
