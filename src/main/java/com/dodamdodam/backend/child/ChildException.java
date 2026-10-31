package com.dodamdodam.backend.child;

import com.dodamdodam.backend.global.error.ApiErrorResponse.FieldError;
import java.util.List;

public class ChildException extends RuntimeException {

    private final String code;

    ChildException(Throwable cause) {
        this("INTERNAL_ERROR", "자녀 정보를 저장하거나 조회할 수 없습니다.", cause);
    }

    private ChildException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    static ChildException notFound() {
        return new ChildException("RESOURCE_NOT_FOUND", "자녀 정보를 찾을 수 없습니다.", null);
    }

    public String code() {
        return code;
    }

    public List<FieldError> fields() {
        return List.of();
    }
}
