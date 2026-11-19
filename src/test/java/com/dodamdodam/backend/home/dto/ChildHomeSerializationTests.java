package com.dodamdodam.backend.home.dto;

import com.dodamdodam.backend.child.dto.ChildContext;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** DB나 서비스 없이 실제 Jackson 설정과 전달받은 시각으로 홈 계약을 검증한다. */
class ChildHomeSerializationTests {

    private static final UUID CHILD_ID = UUID.fromString("22222222-2222-4222-8222-222222222222");
    private static final UUID ACTIVE_ID = UUID.fromString("55555555-5555-4555-8555-555555555555");
    private static final String REGISTERED_AT = "2024-01-01T00:00:00+09:00";
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class));

    @Test
    void exposesOnlyHomeFieldsAndKeepsNameSeparateFromNickname() {
        contextRunner.run(context -> {
            var home = home(REGISTERED_AT, "2026-10-03T13:00:00+09:00", ACTIVE_ID, false);
            var json = json(context.getBean(JsonMapper.class), home);

            assertThat(json).containsOnlyKeys("child", "greeting", "joinedAt", "daysTogether",
                    "activeConversationId", "menus", "availability");
            assertThat(object(json, "child")).containsOnlyKeys("childId", "name", "nickname", "characterId")
                    .containsEntry("childId", CHILD_ID.toString()).containsEntry("name", "가나")
                    .containsEntry("nickname", "별이").containsEntry("characterId", "test-character");
            assertThat(json).containsEntry("greeting", "별이, 오늘도 반가워!")
                    .containsEntry("activeConversationId", ACTIVE_ID.toString());
            assertThat(object(json, "availability")).containsOnlyKeys("timeZone", "serverTime", "serviceDate",
                    "opensAt", "closesAt", "canEnter", "reason", "nextOpensAt");
            assertThat(json.get("menus")).isEqualTo(List.of(Map.of("code", "TALK", "status", "AVAILABLE"),
                    Map.of("code", "PLAY", "status", "COMING_SOON")));
        });
    }

    @Test
    void countsRegistrationDayAsOneUsingKstInsteadOfUtcDate() {
        var home = home("2026-10-02T15:00:00Z", "2026-10-03T01:00:00Z", null, false);

        assertThat(home.daysTogether()).isEqualTo(1);
        assertThat(home.joinedAt()).isEqualTo(OffsetDateTime.parse("2026-10-03T00:00:00+09:00"));
        assertThat(home.availability().serviceDate()).isEqualTo(LocalDate.of(2026, 10, 3));
    }

    @ParameterizedTest
    @CsvSource({
            "2026-10-03T23:59:59+09:00,2026-10-04T00:00:00+09:00,2",
            "2026-10-31T14:59:59Z,2026-10-31T15:00:00Z,2",
            "2026-12-31T23:59:59+09:00,2027-01-01T00:00:00+09:00,2",
            "2024-02-28T23:59:59+09:00,2024-02-29T00:00:00+09:00,2",
            "2024-02-28T23:59:59+09:00,2024-03-01T00:00:00+09:00,3",
            "2026-10-02T14:59:59Z,2026-10-02T15:00:00Z,2"
    })
    void countsInclusiveKstDaysAcrossCalendarBoundaries(String registeredAt, String now, long expectedDays) {
        assertThat(home(registeredAt, now, null, false).daysTogether()).isEqualTo(expectedDays);
    }

    @ParameterizedTest
    @CsvSource({
            "00:00:00,false", "07:59:59,false", "07:59:59.999999,false",
            "08:00:00,true", "23:59:59,true", "23:59:59.999999,true"
    })
    void usesInclusiveOpeningAndExclusiveMidnight(String time, boolean expectedCanEnter) {
        var home = home(REGISTERED_AT, "2026-10-03T" + time + "+09:00", ACTIVE_ID, false);
        var availability = home.availability();

        assertThat(availability.timeZone()).isEqualTo("Asia/Seoul");
        assertThat(availability.serviceDate()).isEqualTo(LocalDate.of(2026, 10, 3));
        assertThat(availability.opensAt()).isEqualTo(OffsetDateTime.parse("2026-10-03T08:00:00+09:00"));
        assertThat(availability.closesAt()).isEqualTo(OffsetDateTime.parse("2026-10-04T00:00:00+09:00"));
        assertThat(availability.canEnter()).isEqualTo(expectedCanEnter);
        assertThat(availability.reason()).isEqualTo(expectedCanEnter ? null : "OUTSIDE_SERVICE_HOURS");
        assertThat(availability.nextOpensAt()).isEqualTo(expectedCanEnter ? null : availability.opensAt());
        assertThat(home.activeConversationId()).isEqualTo(expectedCanEnter ? ACTIVE_ID : null);
        assertThat(home.menus()).containsExactly(new ChildHome.Menu("TALK", expectedCanEnter ? "AVAILABLE" : "UNAVAILABLE"),
                new ChildHome.Menu("PLAY", "COMING_SOON"));
    }

    @ParameterizedTest
    @CsvSource({
            "00:00:00,OUTSIDE_SERVICE_HOURS", "07:59:59,OUTSIDE_SERVICE_HOURS",
            "08:00:00,CONVERSATION_CLOSING", "23:59:59,CONVERSATION_CLOSING"
    })
    void outsideHoursTakePriorityOverClosing(String time, String expectedReason) {
        var home = home(REGISTERED_AT, "2026-10-03T" + time + "+09:00", ACTIVE_ID, true);

        assertThat(home.availability().canEnter()).isFalse();
        assertThat(home.availability().reason()).isEqualTo(expectedReason);
        assertThat(home.availability().nextOpensAt()).isEqualTo(expectedReason.equals("OUTSIDE_SERVICE_HOURS")
                ? OffsetDateTime.parse("2026-10-03T08:00:00+09:00") : null);
        assertThat(home.activeConversationId()).isNull();
        assertThat(home.menus()).containsExactly(new ChildHome.Menu("TALK", "UNAVAILABLE"),
                new ChildHome.Menu("PLAY", "COMING_SOON"));
    }

    @Test
    void allowsEntryWithoutCreatingAnActiveConversationId() {
        var home = home(REGISTERED_AT, "2026-10-03T08:00:00+09:00", null, false);

        assertThat(home.availability().canEnter()).isTrue();
        assertThat(home.activeConversationId()).isNull();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void keepsExplicitNullsEvenWhenMapperOmitsThemByDefault(boolean closing) {
        contextRunner.withPropertyValues("spring.jackson.default-property-inclusion=non_null").run(context -> {
            var json = json(context.getBean(JsonMapper.class),
                    home(REGISTERED_AT, "2026-10-03T13:00:00+09:00", null, closing));

            assertThat(json).containsEntry("activeConversationId", null);
            assertThat(object(json, "availability")).containsEntry("nextOpensAt", null)
                    .containsEntry("reason", closing ? "CONVERSATION_CLOSING" : null);
            assertThat(json.get("menus")).isEqualTo(List.of(
                    Map.of("code", "TALK", "status", closing ? "UNAVAILABLE" : "AVAILABLE"),
                    Map.of("code", "PLAY", "status", "COMING_SOON")));
        });
    }

    @Test
    void serializesAllPublicTimesInKstWithSecondsAtMidnight() {
        contextRunner.run(context -> {
            var home = home("2026-10-02T14:59:59.987654Z", "2026-10-02T15:00:00.123456Z", ACTIVE_ID, true);
            var json = json(context.getBean(JsonMapper.class), home);

            assertThat(json).containsEntry("joinedAt", "2026-10-02T23:59:59+09:00")
                    .containsEntry("daysTogether", 2).containsEntry("activeConversationId", null);
            assertThat(object(json, "availability")).containsEntry("timeZone", "Asia/Seoul")
                    .containsEntry("serverTime", "2026-10-03T00:00:00+09:00")
                    .containsEntry("serviceDate", "2026-10-03")
                    .containsEntry("opensAt", "2026-10-03T08:00:00+09:00")
                    .containsEntry("closesAt", "2026-10-04T00:00:00+09:00")
                    .containsEntry("canEnter", false).containsEntry("reason", "OUTSIDE_SERVICE_HOURS")
                    .containsEntry("nextOpensAt", "2026-10-03T08:00:00+09:00");
        });
    }

    @Test
    void rejectsAnEarlierServiceDateInsteadOfClampingDaysTogether() {
        assertThatThrownBy(() -> home("2026-10-03T00:00:00+09:00", "2026-10-02T23:59:59+09:00", null, false))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /** 실제 아이 정보 대신 합성 문맥과 고정 시각만 전달한다. */
    private static ChildHome home(String registeredAt, String now, UUID activeId, boolean closing) {
        var child = new ChildContext(CHILD_ID, "가나", "별이", LocalDate.of(2020, 5, 12),
                List.of("공룡"), "test-character", OffsetDateTime.parse(registeredAt));
        return ChildHome.from(child, OffsetDateTime.parse(now), activeId, closing);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> json(JsonMapper mapper, ChildHome home) {
        return mapper.readValue(mapper.writeValueAsString(home), Map.class);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> object(Map<String, Object> parent, String field) {
        return (Map<String, Object>) parent.get(field);
    }
}
