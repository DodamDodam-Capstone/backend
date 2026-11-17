package com.dodamdodam.backend.auth.exception;

import com.dodamdodam.backend.global.error.ApiErrorResponse.FieldError;
import java.util.List;

public final class PasswordLoginException extends RuntimeException {
    private final int status;
    private final String code;
    private final List<FieldError> fields;

    /** 원문 자격이나 저장 해시 없이 허용한 상태·코드·필드만 보관한다. */
    private PasswordLoginException(int status, String code, String message, List<FieldError> fields) {
        super(message);
        this.status = status;
        this.code = code;
        this.fields = fields;
    }

    /** 구조·값이 잘못된 요청에 원문 없는 필드 오류를 만든다. */
    public static PasswordLoginException validation(String field) {
        return new PasswordLoginException(400, "VALIDATION_FAILED", "입력값을 확인해 주세요.",
                List.of(new FieldError(field, "유효하지 않은 값입니다.")));
    }

    /** 없는 계정·오답·소셜 전용을 구별할 수 없는 고정 실패를 반환한다. */
    public static PasswordLoginException failed() {
        return new PasswordLoginException(401, "LOGIN_FAILED", "이메일 또는 비밀번호를 확인해 주세요.", List.of());
    }

    /** HTTP 연결에 필요한 상태를 반환한다. */
    public int status() {
        return status;
    }

    /** 공통 오류 본문의 고정 코드를 반환한다. */
    public String code() {
        return code;
    }

    /** 원문이 없는 입력 오류 목록을 반환한다. */
    public List<FieldError> fields() {
        return fields;
    }
}
