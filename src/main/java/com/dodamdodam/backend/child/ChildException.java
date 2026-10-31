package com.dodamdodam.backend.child;

import com.dodamdodam.backend.global.error.ApiErrorResponse.FieldError;
import java.util.List;

public class ChildException extends RuntimeException {

    private final String code;
    private final List<FieldError> fields;

    ChildException(Throwable cause) {
        this("INTERNAL_ERROR", "자녀 정보를 저장하거나 조회할 수 없습니다.", List.of(), cause);
    }

    private ChildException(String code, String message, List<FieldError> fields, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.fields = fields;
    }

    static ChildException notFound() {
        return new ChildException("RESOURCE_NOT_FOUND", "자녀 정보를 찾을 수 없습니다.", List.of(), null);
    }

    /** 원본 입력을 노출하지 않고 검증에 실패한 필드만 전달한다. */
    static ChildException validation(String field) {
        return new ChildException("VALIDATION_FAILED", "입력값을 확인해 주세요.",
                List.of(new FieldError(field, "유효하지 않은 값입니다.")), null);
    }

    /** 계정당 자녀 1명 제한에 해당하는 업무 오류를 만든다. */
    static ChildException limitReached() {
        return new ChildException("CHILD_LIMIT_REACHED", "계정당 자녀는 1명만 등록할 수 있습니다.", List.of(), null);
    }

    public String code() {
        return code;
    }

    public List<FieldError> fields() {
        return fields;
    }
}
