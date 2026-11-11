package com.dodamdodam.backend.conversation.dto;

import com.dodamdodam.backend.global.error.ApiErrorResponse;
import com.dodamdodam.backend.global.error.ApiErrorResponse.FieldError;
import java.util.List;
import java.util.UUID;

/** 일반 오류 스키마를 바꾸지 않고 ACTIVE 충돌에서만 기존 대화 ID를 추가한다. */
public record ActiveConversationExistsError(ErrorDetail error) {

    /** 공통 오류 작성기가 만든 메타데이터를 그대로 재사용한다. */
    public static ActiveConversationExistsError from(ApiErrorResponse base, UUID conversationId) {
        var error = base.error();
        return new ActiveConversationExistsError(new ErrorDetail(error.code(), error.message(), error.requestId(),
                error.fields(), conversationId));
    }

    public record ErrorDetail(String code, String message, String requestId, List<FieldError> fields,
                              UUID activeConversationId) { }
}
