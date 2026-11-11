package com.dodamdodam.backend.conversation.exception;

import com.dodamdodam.backend.global.error.ApiErrorResponse.FieldError;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class ConversationException extends RuntimeException {

    private final String code;
    private final List<FieldError> fields;
    private final UUID activeConversationId;

    /** 입력 원문이나 내부 식별자를 포함하지 않는 고정 오류를 구성한다. */
    private ConversationException(String code, String message, List<FieldError> fields,
                                  UUID activeConversationId, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.fields = List.copyOf(fields);
        this.activeConversationId = activeConversationId;
    }

    /** 업무 DB 오류는 원인을 보존하되 공통 500 응답에 원문을 노출하지 않는다. */
    public ConversationException(Throwable cause) {
        this("INTERNAL_ERROR", "대화 정보를 저장하거나 조회할 수 없습니다.", List.of(), null, cause);
    }

    /** 정의된 필드 이름만 공개하고 잘못된 입력값은 오류에 복사하지 않는다. */
    public static ConversationException validation(String field) {
        return new ConversationException("VALIDATION_FAILED", "입력값을 확인해 주세요.",
                List.of(new FieldError(field, "유효하지 않은 값입니다.")), null, null);
    }

    /** 아이 대화 이용시간 밖의 요청을 공통 403 업무 오류로 표현한다. */
    public static ConversationException serviceHoursClosed() {
        return new ConversationException("SERVICE_HOURS_CLOSED", "대화 이용시간이 아닙니다.", List.of(), null, null);
    }

    /** 같은 시작 키라도 현재 보안 문맥에 연결되지 않았다면 명시적 이어하기가 필요하다. */
    public static ConversationException childSessionRequired() {
        return new ConversationException("CHILD_SESSION_REQUIRED", "현재 세션에서 대화 이어하기가 필요합니다.", List.of(), null, null);
    }

    /** 현재 ACTIVE가 있는 경우에만 명세가 허용하는 기존 대화 ID를 공개한다. */
    public static ConversationException activeExists(UUID conversationId) {
        return new ConversationException("ACTIVE_CONVERSATION_EXISTS", "진행 중인 대화가 있습니다.", List.of(),
                Objects.requireNonNull(conversationId, "conversationId"), null);
    }

    /** 종료 조정 중에는 새 대화를 만들지 않는다. */
    public static ConversationException closing() {
        return new ConversationException("CONVERSATION_CLOSING", "대화를 종료하고 있습니다.", List.of(), null, null);
    }

    /** 종료된 시작 키의 재사용으로 새로운 행을 만들지 않는다. */
    public static ConversationException ended() {
        return new ConversationException("CONVERSATION_ENDED", "이미 종료된 대화입니다.", List.of(), null, null);
    }

    /** HTTP 처리 계층에서 사용할 업무 오류 코드를 반환한다. */
    public String code() {
        return code;
    }

    /** 공통 오류 응답에 담을 검증 오류 목록을 반환한다. */
    public List<FieldError> fields() {
        return fields;
    }

    /** ACTIVE 충돌 전용 응답에서만 사용할 기존 대화 ID다. */
    public UUID activeConversationId() {
        return activeConversationId;
    }
}
