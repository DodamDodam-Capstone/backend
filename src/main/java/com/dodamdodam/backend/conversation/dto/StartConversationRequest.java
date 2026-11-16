package com.dodamdodam.backend.conversation.dto;

import com.dodamdodam.backend.conversation.exception.ConversationException;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import tools.jackson.databind.JsonNode;

public record StartConversationRequest(UUID clientRequestId) {

    private static final Set<String> FIELDS = Set.of("clientRequestId");
    private static final Pattern UUID_TEXT = Pattern.compile(
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");

    /** 직접 생성한 요청도 필수 시작 키를 가져야 한다. */
    public StartConversationRequest {
        if (clientRequestId == null) {
            throw ConversationException.validation("clientRequestId");
        }
    }

    /** 추가 필드·타입 강제 변환 없이 명세의 단일 UUID 문자열을 읽는다. */
    public static StartConversationRequest fromJson(JsonNode body) {
        // 계정과 세션 문맥은 서버에서 얻으므로 본문으로 추가해도 허용하지 않는다.
        if (body == null || !body.isObject() || !FIELDS.containsAll(body.propertyNames())) {
            throw ConversationException.validation("body");
        }
        var key = body.get("clientRequestId");
        if (key == null || !key.isString()) {
            throw ConversationException.validation("clientRequestId");
        }
        return new StartConversationRequest(parseUuid(key.stringValue(), "clientRequestId"));
    }

    /** 경로의 자녀 UUID에도 본문과 같은 완전한 문자열 형식을 적용한다. */
    public static UUID parseChildId(String childId) {
        return parseUuid(childId, "childId");
    }

    /** UUID.fromString이 허용하는 축약형과 공백·개행을 먼저 거부한다. */
    private static UUID parseUuid(String value, String field) {
        if (value == null || !UUID_TEXT.matcher(value).matches()) {
            throw ConversationException.validation(field);
        }
        return UUID.fromString(value);
    }
}
