package com.dodamdodam.backend.child.exception;

import com.dodamdodam.backend.global.error.ApiErrorResponse.FieldError;
import java.util.List;

public class ChildException extends RuntimeException {

    private final String code;
    private final List<FieldError> fields;

    /** 저장 장애의 원인을 보존하고 응답에는 고정된 업무 오류만 전달한다. */
    public ChildException(Throwable cause) {
        this("INTERNAL_ERROR", "자녀 정보를 저장하거나 조회할 수 없습니다.", List.of(), cause);
    }

    private ChildException(String code, String message, List<FieldError> fields, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.fields = fields;
    }

    /** 미소유·미존재 자녀를 같은 오류로 처리한다. */
    public static ChildException notFound() {
        return new ChildException("RESOURCE_NOT_FOUND", "자녀 정보를 찾을 수 없습니다.", List.of(), null);
    }

    /** 원본 입력을 노출하지 않고 검증에 실패한 필드만 전달한다. */
    public static ChildException validation(String field) {
        return new ChildException("VALIDATION_FAILED", "입력값을 확인해 주세요.",
                List.of(new FieldError(field, "유효하지 않은 값입니다.")), null);
    }

    /** 계정당 자녀 1명 제한에 해당하는 업무 오류를 만든다. */
    public static ChildException limitReached() {
        return new ChildException("CHILD_LIMIT_REACHED", "계정당 자녀는 1명만 등록할 수 있습니다.", List.of(), null);
    }

    /** HTTP 처리 계층이 사용할 업무 오류 코드를 반환한다. */
    public String code() {
        return code;
    }

    /** 원본 입력을 포함하지 않는 필드 오류 정보를 반환한다. */
    public List<FieldError> fields() {
        return fields;
    }
}
