package com.dodamdodam.backend.conversation.domain;

import com.dodamdodam.backend.conversation.exception.ConversationException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConversationServiceHoursTests {

    @ParameterizedTest
    @CsvSource({
            "2026-10-03T08:00:00+09:00,2026-10-03,2026-10-04T00:00:00+09:00",
            "2026-10-03T23:59:59.999999+09:00,2026-10-03,2026-10-04T00:00:00+09:00",
            "2026-10-02T23:00:00Z,2026-10-03,2026-10-04T00:00:00+09:00",
            "2026-10-03T01:00:00+02:00,2026-10-03,2026-10-04T00:00:00+09:00",
            "2024-02-29T23:59:59+09:00,2024-02-29,2024-03-01T00:00:00+09:00",
            "2026-10-31T08:00:00+09:00,2026-10-31,2026-11-01T00:00:00+09:00",
            "2026-12-31T23:59:59+09:00,2026-12-31,2027-01-01T00:00:00+09:00"
    })
    void derivesKstDateAndNextMidnightFromSuppliedDatabaseInstant(String input, String date, String end) {
        var window = ConversationServiceHours.requireOpen(OffsetDateTime.parse(input));

        assertThat(window.serviceDate()).isEqualTo(LocalDate.parse(date));
        assertThat(window.scheduledEndAt()).isEqualTo(OffsetDateTime.parse(end));
    }

    @ParameterizedTest
    @ValueSource(strings = {"2026-10-03T00:00:00+09:00", "2026-10-03T07:59:59.999999+09:00",
            "2026-10-04T00:00:00+09:00", "2026-10-03T15:00:00Z", "2026-10-02T22:59:59.999999Z"})
    void rejectsOutsideHoursIncludingExactMidnight(String input) {
        assertThatThrownBy(() -> ConversationServiceHours.requireOpen(OffsetDateTime.parse(input)))
                .isInstanceOfSatisfying(ConversationException.class, failure -> {
                    assertThat(failure.code()).isEqualTo("SERVICE_HOURS_CLOSED");
                    assertThat(failure.fields()).isEmpty();
                });
    }

    @Test
    void suppliedTimeAfterLockWaitDeterminesAccessInsteadOfEarlierCheck() {
        ConversationServiceHours.requireOpen(OffsetDateTime.parse("2026-10-03T23:59:59.999999+09:00"));

        assertThatThrownBy(() -> ConversationServiceHours.requireOpen(OffsetDateTime.parse("2026-10-04T00:00:00+09:00")))
                .isInstanceOf(ConversationException.class);
    }

    @Test
    void missingDatabaseTimeDoesNotFallBackToSystemClock() {
        assertThatThrownBy(() -> ConversationServiceHours.requireOpen(null)).isInstanceOf(NullPointerException.class);
    }
}
