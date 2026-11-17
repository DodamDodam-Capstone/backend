package com.dodamdodam.backend.auth.session;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class SessionValidityTests {
    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-10-14T12:00:00Z");

    @ParameterizedTest
    @ValueSource(longs = {-1, 0, 1})
    void finiteExpiryIsValidOnlyBeforeItsExactBoundary(long seconds) {
        var validity = new SessionSecurityRepository.SessionValidity(0, 0, null, NOW.plusSeconds(seconds), NOW);
        assertThat(validity.isValid()).isEqualTo(seconds > 0);
    }

    @Test
    void nullExpiryStillRequiresCurrentVersionAndNoRevocation() {
        assertThat(new SessionSecurityRepository.SessionValidity(4, 4, null, null, NOW).isValid()).isTrue();
        assertThat(new SessionSecurityRepository.SessionValidity(4, 5, null, null, NOW).isValid()).isFalse();
        assertThat(new SessionSecurityRepository.SessionValidity(4, 4, NOW, null, NOW).isValid()).isFalse();
    }
}
