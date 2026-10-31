package com.dodamdodam.backend.child;

import com.dodamdodam.backend.global.error.ApiErrorResponse.FieldError;
import java.util.List;

public class ChildException extends RuntimeException {

    ChildException(Throwable cause) {
        super("자녀 정보를 저장하거나 조회할 수 없습니다.", cause);
    }

    public String code() {
        return "INTERNAL_ERROR";
    }

    public List<FieldError> fields() {
        return List.of();
    }
}
