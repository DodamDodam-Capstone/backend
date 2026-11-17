package com.dodamdodam.backend.global.error;

import java.util.List;

public record ApiErrorResponse(ErrorDetail error) {

    public record ErrorDetail(String code, String message, String requestId, List<FieldError> fields) {
    }

    public record FieldError(String field, String reason) {
    }
}
