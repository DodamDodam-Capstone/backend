package com.dodamdodam.backend.child;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CreateChildRequestTests {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-31T15:00:00Z"), ZoneOffset.UTC);
    // 테스트 전용 카탈로그이며 제품의 지원 목록을 의미하지 않는다.
    private static final Set<String> CHARACTERS = Set.of("test-character", "a", "a".repeat(64));

    @Test
    void parsesClosedRequestAndDefaultsInterests() {
        var parsed = parse(body());

        assertThat(parsed.name()).isEqualTo("가나");
        assertThat(parsed.nickname()).isEqualTo("별이");
        assertThat(parsed.birthDate()).isEqualTo(LocalDate.of(2020, 5, 12));
        assertThat(parsed.gender()).isEqualTo(ChildView.Gender.FEMALE);
        assertThat(parsed.interests()).isEmpty();
        assertThat(parsed.characterId()).isEqualTo("test-character");
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "[]", "true", "1", "\"text\""})
    void rejectsNonObjectBodies(String json) {
        var failure = assertThrows(ChildException.class, () -> CreateChildRequest.fromJson(JSON.readTree(json)));
        assertValidation(failure, "body");
    }

    @ParameterizedTest
    @ValueSource(strings = {"name", "nickname", "birthDate", "gender", "characterId"})
    void rejectsMissingAndNullRequiredFields(String field) {
        var body = body();
        body.remove(field);
        rejects(body, field);
        body.put(field, null);
        rejects(body, field);
    }

    @ParameterizedTest
    @MethodSource("wrongFieldTypes")
    void doesNotCoerceNonStringFields(String field, Object value) {
        var body = body();
        body.put(field, value);
        rejects(body, field);
    }

    static Stream<Arguments> wrongFieldTypes() {
        return Stream.of("name", "nickname", "birthDate", "gender", "characterId")
                .flatMap(field -> Stream.of(1, true, List.of(), Map.of()).map(value -> Arguments.of(field, value)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"accountId", "securityContextId", "private-input-key"})
    void rejectsUnknownFieldsWithoutEchoingTheirNames(String field) {
        var body = body();
        body.put(field, "private-input-value");
        var failure = rejects(body, "body");
        assertThat(failure.getMessage()).doesNotContain(field, "private-input-value");
        assertThat(failure.fields().toString()).doesNotContain(field, "private-input-value");
    }

    @ParameterizedTest
    @MethodSource("validNames")
    void countsNameLimitsInCodePoints(String field, String value) {
        var body = body();
        body.put(field, value);
        var parsed = parse(body);
        assertThat(field.equals("name") ? parsed.name() : parsed.nickname()).isEqualTo(value);
    }

    static Stream<Arguments> validNames() {
        return Stream.of(Arguments.of("name", "가"), Arguments.of("name", "가나다라마"),
                Arguments.of("name", "😀".repeat(5)), Arguments.of("nickname", "별"),
                Arguments.of("nickname", "별".repeat(20)), Arguments.of("nickname", "😀".repeat(20)));
    }

    @ParameterizedTest
    @MethodSource("invalidNames")
    void rejectsEmptyAndOverlongNames(String field, String value) {
        var body = body();
        body.put(field, value);
        rejects(body, field);
    }

    static Stream<Arguments> invalidNames() {
        return Stream.of(Arguments.of("name", ""), Arguments.of("name", " \t\u00a0\u2003\u3000"),
                Arguments.of("name", "😀".repeat(6)), Arguments.of("nickname", ""),
                Arguments.of("nickname", " \t\u00a0\u2003\u3000"), Arguments.of("nickname", "😀".repeat(21)));
    }

    @Test
    void trimsUnicodeEdgesAndPreservesInternalSpacesAndInterestOrder() {
        var body = body();
        body.put("name", " \t\u00a0\u2003\u3000가 나\u3000\u2003\u00a0\t ");
        body.put("nickname", "\u00a0별 이\u2003");
        body.put("interests", List.of("\u3000공 룡\u00a0", " 그림 "));

        var parsed = parse(body);

        assertThat(parsed.name()).isEqualTo("가 나");
        assertThat(parsed.nickname()).isEqualTo("별 이");
        assertThat(parsed.interests()).containsExactly("공 룡", "그림");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "2020-5-12", "2020-05-1", "2023-02-29", "2024-02-30", "2020-13-01",
            "2020-05-12\n", "2020-05-12 ", " 2020-05-12", "+2020-05-12"})
    void rejectsInvalidOrNonExactDates(String value) {
        var body = body();
        body.put("birthDate", value);
        rejects(body, "birthDate");
    }

    @Test
    void acceptsLeapDayAndKstTodayButRejectsTomorrow() {
        var body = body();
        body.put("birthDate", "2024-02-29");
        assertThat(parse(body).birthDate()).isEqualTo(LocalDate.of(2024, 2, 29));
        body.put("birthDate", "2026-11-01");
        assertThat(parse(body).birthDate()).isEqualTo(LocalDate.of(2026, 11, 1));
        body.put("birthDate", "2026-11-02");
        rejects(body, "birthDate");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "female", "UNSPECIFIED", "FEMALE\n", "FEMALE ", " FEMALE"})
    void rejectsNonExactGender(String value) {
        var body = body();
        body.put("gender", value);
        rejects(body, "gender");
    }

    @ParameterizedTest
    @ValueSource(strings = {"MALE", "FEMALE"})
    void acceptsBothGenders(String value) {
        var body = body();
        body.put("gender", value);
        assertThat(parse(body).gender().name()).isEqualTo(value);
    }

    @ParameterizedTest
    @MethodSource("invalidInterests")
    void rejectsInvalidInterests(Object value) {
        var body = body();
        body.put("interests", value);
        rejects(body, "interests");
    }

    static Stream<Arguments> invalidInterests() {
        return Stream.of(Arguments.of((Object) null), Arguments.of(1), Arguments.of(true), Arguments.of("공룡"),
                Arguments.of(Map.of()), Arguments.of(Collections.singletonList(null)), Arguments.of(List.of(1)),
                Arguments.of(List.of(true)), Arguments.of(List.of(List.of())), Arguments.of(List.of(Map.of())),
                Arguments.of(List.of("")), Arguments.of(List.of("\u00a0\u2003")), Arguments.of(List.of("😀".repeat(31))),
                Arguments.of(List.of("공룡", "\u3000공룡 ")),
                Arguments.of(IntStream.rangeClosed(1, 11).mapToObj(String::valueOf).toList()));
    }

    @Test
    void permitsEmptyAndMaximumInterests() {
        var body = body();
        body.put("interests", List.of());
        assertThat(parse(body).interests()).isEmpty();
        var interests = new ArrayList<>(List.of("😀".repeat(30), "a"));
        interests.addAll(IntStream.rangeClosed(1, 8).mapToObj(String::valueOf).toList());
        body.put("interests", interests);
        assertThat(parse(body).interests()).containsExactlyElementsOf(interests);
    }

    @Test
    void acceptsCharacterIdLengthBoundariesOnlyWhenSupported() {
        var body = body();
        body.put("characterId", "a");
        assertThat(parse(body).characterId()).isEqualTo("a");
        body.put("characterId", "a".repeat(64));
        assertThat(parse(body).characterId()).isEqualTo("a".repeat(64));
        body.put("characterId", "a".repeat(65));
        rejects(body, "characterId");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "test-character\n", "test-character ", " test-character", "캐릭터", "a/b", "a b",
            "unsupported-character"})
    void rejectsInvalidOrUnsupportedCharacters(String value) {
        var body = body();
        body.put("characterId", value);
        rejects(body, "characterId");
    }

    @Test
    void emptyCatalogueDoesNotAcceptAnyCharacter() {
        var parsed = CreateChildRequest.fromJson(JSON.readTree(JSON.writeValueAsString(body())));
        assertValidation(assertThrows(ChildException.class, () -> parsed.normalized(CLOCK, Set.of())), "characterId");
    }

    @ParameterizedTest
    @MethodSource("unstorableText")
    void rejectsTextThatCannotBeStoredWithoutCorruption(String field, String value) {
        var request = new CreateChildRequest(field.equals("name") ? value : "가나",
                field.equals("nickname") ? value : "별이", LocalDate.of(2020, 5, 12), ChildView.Gender.FEMALE,
                field.equals("interests") ? List.of(value) : List.of(), "test-character");

        assertValidation(assertThrows(ChildException.class, () -> request.normalized(CLOCK, CHARACTERS)), field);
    }

    static Stream<Arguments> unstorableText() {
        return Stream.of("name", "nickname", "interests")
                .flatMap(field -> Stream.of("\u0000", "\ud800", "\udc00").map(value -> Arguments.of(field, value)));
    }

    private static CreateChildRequest parse(Map<String, Object> body) {
        return CreateChildRequest.fromJson(JSON.readTree(JSON.writeValueAsString(body))).normalized(CLOCK, CHARACTERS);
    }

    private static ChildException rejects(Map<String, Object> body, String field) {
        var failure = assertThrows(ChildException.class, () -> parse(body));
        assertValidation(failure, field);
        return failure;
    }

    private static void assertValidation(ChildException failure, String field) {
        assertThat(failure.code()).isEqualTo("VALIDATION_FAILED");
        assertThat(failure.fields()).extracting(error -> error.field()).containsExactly(field);
        assertThat(failure.fields()).allSatisfy(error -> assertThat(error.reason()).isNotBlank());
    }

    private static Map<String, Object> body() {
        return new LinkedHashMap<>(Map.of("name", "가나", "nickname", "별이", "birthDate", "2020-05-12",
                "gender", "FEMALE", "characterId", "test-character"));
    }
}
