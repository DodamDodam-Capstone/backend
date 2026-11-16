package com.dodamdodam.backend.conversation.dto;

import com.dodamdodam.backend.conversation.exception.ConversationException;
import com.dodamdodam.backend.global.error.ApiErrorResponse.FieldError;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StartConversationDtoTests {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final UUID KEY = UUID.fromString("abcdefab-1234-5678-90ab-abcdefabcdef");

    @ParameterizedTest
    @ValueSource(strings = {"abcdefab-1234-5678-90ab-abcdefabcdef", "ABCDEFAB-1234-5678-90AB-ABCDEFABCDEF",
            "00000000-0000-0000-0000-000000000000"})
    void acceptsCompleteUuidWithoutInventingVersionRestriction(String value) {
        var input = StartConversationRequest.fromJson(JSON.readTree(JSON.writeValueAsString(Map.of("clientRequestId", value))));
        assertThat(input.clientRequestId()).isEqualTo(UUID.fromString(value));
        assertThat(StartConversationRequest.parseChildId(value)).isEqualTo(UUID.fromString(value));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"clientRequestId\":null}", "{\"clientRequestId\":123}",
            "{\"clientRequestId\":true}", "{\"clientRequestId\":[]}", "{\"clientRequestId\":{}}"})
    void rejectsMissingNullOrNonStringKey(String body) {
        rejects(() -> StartConversationRequest.fromJson(JSON.readTree(body)), "clientRequestId");
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "[]", "123", "true", "\"value\""})
    void rejectsNonObjectBody(String body) {
        rejects(() -> StartConversationRequest.fromJson(JSON.readTree(body)), "body");
    }

    @Test
    void rejectsAbsentBodyAndDirectNullKey() {
        rejects(() -> StartConversationRequest.fromJson(null), "body");
        rejects(() -> new StartConversationRequest(null), "clientRequestId");
        rejects(() -> StartConversationRequest.parseChildId(null), "childId");
    }

    @ParameterizedTest
    @ValueSource(strings = {"accountId", "securityContextId", "sessionId", "childId", "unknown"})
    void rejectsEveryAdditionalFieldIncludingServerOwnedIdentifiers(String field) {
        var body = JSON.readTree(JSON.writeValueAsString(Map.of("clientRequestId", KEY.toString(), field, "synthetic-value")));
        rejects(() -> StartConversationRequest.fromJson(body), "body");
    }

    @ParameterizedTest
    @MethodSource("invalidUuids")
    void rejectsAbbreviatedMalformedOrPaddedUuid(String value) {
        var body = JSON.readTree(JSON.writeValueAsString(Map.of("clientRequestId", value)));
        rejects(() -> StartConversationRequest.fromJson(body), "clientRequestId");
        rejects(() -> StartConversationRequest.parseChildId(value), "childId");
    }

    static Stream<String> invalidUuids() {
        return Stream.of("", "1-1-1-1-1", "0-0-0-0-0", "not-a-uuid", KEY.toString().replace("-", ""),
                "{" + KEY + "}", "urn:uuid:" + KEY, " " + KEY, KEY + " ", KEY + "\n", KEY + "\r\n",
                KEY + "\t", KEY + "\u0000", KEY + "\u2028", KEY + "0", "g" + KEY.toString().substring(1));
    }

    @Test
    void resultConvertsStoredInstantToKstSecondsWithoutChangingDateFields() {
        var result = new StartConversationResult(KEY, "ACTIVE", OffsetDateTime.parse("2026-10-03T14:59:59.987654Z"),
                LocalDate.of(2026, 10, 3), OffsetDateTime.parse("2026-10-03T15:00:00Z"));

        assertThat(result.conversationId()).isEqualTo(KEY);
        assertThat(result.startedAt()).isEqualTo(OffsetDateTime.parse("2026-10-03T23:59:59+09:00"));
        assertThat(result.scheduledEndAt()).isEqualTo(OffsetDateTime.parse("2026-10-04T00:00:00+09:00"));
        assertThat(result.serviceDate()).isEqualTo(LocalDate.of(2026, 10, 3));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"CLOSING", "ENDED", "active", "UNKNOWN", ""})
    void resultCannotRepresentNonActiveStart(String status) {
        assertThatThrownBy(() -> new StartConversationResult(KEY, status, OffsetDateTime.parse("2026-10-03T08:00:00+09:00"),
                LocalDate.of(2026, 10, 3), OffsetDateTime.parse("2026-10-04T00:00:00+09:00")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"conversationId", "startedAt", "serviceDate", "scheduledEndAt"})
    void resultRequiresEveryPublicField(String field) {
        assertThatThrownBy(() -> new StartConversationResult(field.equals("conversationId") ? null : KEY, "ACTIVE",
                field.equals("startedAt") ? null : OffsetDateTime.parse("2026-10-03T08:00:00+09:00"),
                field.equals("serviceDate") ? null : LocalDate.of(2026, 10, 3),
                field.equals("scheduledEndAt") ? null : OffsetDateTime.parse("2026-10-04T00:00:00+09:00")))
                .isInstanceOf(NullPointerException.class);
    }

    private static void rejects(Runnable operation, String field) {
        assertThatThrownBy(operation::run).isInstanceOfSatisfying(ConversationException.class, failure -> {
            assertThat(failure.code()).isEqualTo("VALIDATION_FAILED");
            assertThat(failure.getMessage()).isEqualTo("입력값을 확인해 주세요.");
            assertThat(failure.fields()).extracting(FieldError::field).containsExactly(field);
            assertThat(failure.fields()).allSatisfy(error -> assertThat(error.reason()).isEqualTo("유효하지 않은 값입니다."));
            assertThat(failure.getCause()).isNull();
        });
    }
}
