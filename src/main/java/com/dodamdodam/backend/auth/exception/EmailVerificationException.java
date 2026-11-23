package com.dodamdodam.backend.auth.exception;

import com.dodamdodam.backend.global.error.ApiErrorResponse.FieldError;
import java.util.List;
import com.dodamdodam.backend.auth.dto.IssueEmailVerificationRequest.Purpose;

public final class EmailVerificationException extends RuntimeException {

    private final List<FieldError> fields;
    private final int status;
    private final String code;

    /** 입력 원문 대신 검증에 실패한 필드만 보관한다. */
    private EmailVerificationException(String field) {
        this(400, "VALIDATION_FAILED", "입력값을 확인해 주세요.", List.of(new FieldError(field, "유효하지 않은 값입니다.")));
    }

    /** 외부 실패 원문을 보관하지 않고 허용한 업무 오류만 구성한다. */
    private EmailVerificationException(int status, String code, String message, List<FieldError> fields) {
        super(message);
        this.status = status;
        this.code = code;
        this.fields = fields;
    }

    /** 요청 구조·값 검증에 공통으로 사용할 오류를 만든다. */
    public static EmailVerificationException validation(String field) {
        return new EmailVerificationException(field);
    }

    /** 번호나 토큰이 유효하지 않은 이유를 외부에서 구분할 수 없게 한다. */
    public static EmailVerificationException invalid() {
        return new EmailVerificationException(400, "VERIFICATION_INVALID",
            "이메일 인증 정보를 다시 확인해 주세요.", List.of());
    }

    /** 유효한 가입 토큰으로 이미 존재하는 이메일을 생성할 때 사용한다. */
    public static EmailVerificationException accountAlreadyExists() {
        return new EmailVerificationException(409, "ACCOUNT_ALREADY_EXISTS", "이미 가입된 계정입니다.", List.of());
    }

    /** Google-only 계정은 검증 후 기존 로그인 수단을 안내한다. */
    public static EmailVerificationException passwordResetNotAvailable() {
        return new EmailVerificationException(409, "PASSWORD_RESET_NOT_AVAILABLE", "Google로 로그인해 주세요.", List.of());
    }

    /** HTTP 오류 연결에 사용할 고정 검증 코드를 반환한다. */
    public String code() {
        return code;
    }

    /** SMTP 실패 보상이 완료된 뒤 일반화한 재발급 안내만 반환한다. */
    public static EmailVerificationException deliveryUnavailable() {
        return new EmailVerificationException(503, "EMAIL_DELIVERY_UNAVAILABLE",
                "이메일을 전송할 수 없습니다. 잠시 후 새 인증번호를 요청해 주세요.", List.of());
    }

    /** HTTP 연결에 사용할 고정 상태를 반환한다. */
    public int status() {
        return status;
    }

    /** 현재 문맥이 폐기·만료·교체된 늦은 응답을 거부한다. */
    public static EmailVerificationException authenticationRequired() {
        return new EmailVerificationException(401, "AUTHENTICATION_REQUIRED", "로그인이 필요합니다.", List.of());
    }

    /** PIN 목적의 현재 이메일·문맥·generation·PIN 상태 결합이 달라졌음을 표현한다. */
    public static EmailVerificationException authorizationRequired(Purpose purpose) {
        return new EmailVerificationException(403, purpose == Purpose.PIN_SETUP
                ? "PIN_SETUP_AUTHORIZATION_REQUIRED" : "PIN_RESET_AUTHORIZATION_REQUIRED",
                "현재 세션에서 이메일 인증을 다시 진행해 주세요.", List.of());
    }

    /** 이미 PIN이 있는 계정에는 최초 설정 발급을 허용하지 않는다. */
    public static EmailVerificationException pinAlreadySet() {
        return new EmailVerificationException(409, "PIN_ALREADY_SET", "이미 보호자 PIN이 설정되어 있습니다.", List.of());
    }

    /** PIN이 없는 계정에는 재설정 발급을 허용하지 않는다. */
    public static EmailVerificationException pinNotSet() {
        return new EmailVerificationException(409, "PIN_NOT_SET", "보호자 PIN이 설정되어 있지 않습니다.", List.of());
    }

    /** 이메일·번호 원문이 없는 필드 오류를 반환한다. */
    public List<FieldError> fields() {
        return fields;
    }
}
