package com.dodamdodam.backend.conversation.exception;

import com.dodamdodam.backend.global.error.ApiErrorResponse.FieldError;
import java.util.List;

public class ConversationException extends RuntimeException {

    private final String code;
    private final List<FieldError> fields;

    /** 입력 원문이나 내부 식별자를 포함하지 않는 고정 오류를 구성한다. */
    private ConversationException(String code, String message, List<FieldError> fields) {
        super(message);
        this.code = code;
        this.fields = List.copyOf(fields);
    }

    /** 정의된 필드 이름만 공개하고 잘못된 입력값은 오류에 복사하지 않는다. */
    public static ConversationException validation(String field) {
        return new ConversationException("VALIDATION_FAILED", "입력값을 확인해 주세요.",
                List.of(new FieldError(field, "유효하지 않은 값입니다.")));
    }

    /** 아이 대화 이용시간 밖의 요청을 공통 403 업무 오류로 표현한다. */
    public static ConversationException serviceHoursClosed() {
        return new ConversationException("SERVICE_HOURS_CLOSED", "대화 이용시간이 아닙니다.", List.of());
    }

    /** HTTP 처리 계층에서 사용할 업무 오류 코드를 반환한다. */
    public String code() {
        return code;
    }

    /** 공통 오류 응답에 담을 검증 오류 목록을 반환한다. */
    public List<FieldError> fields() {
        return fields;
    }
}
