package com.dodamdodam.backend.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dodamdodam.backend.auth.infrastructure.CurrentAccountRepository;
import com.dodamdodam.backend.auth.infrastructure.CurrentAccountRepository.AccountState;
import com.dodamdodam.backend.auth.session.AuthStateUnavailableException;
import com.dodamdodam.backend.auth.session.AuthenticatedSession;
import com.dodamdodam.backend.guardian.infrastructure.GuardianAccessRepository.GuardianAccessState;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.core.AuthenticationException;

class CurrentAccountServiceTests {

    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-11-17T06:00:00Z");
    private static ValidatorFactory validators;
    private final CurrentAccountRepository repository = mock(CurrentAccountRepository.class);
    private final AuthenticatedSession session = new AuthenticatedSession(UUID.randomUUID(), UUID.randomUUID());
    private CurrentAccountService service;

    @BeforeAll
    static void openValidator() {
        validators = Validation.buildDefaultValidatorFactory();
    }

    @AfterAll
    static void closeValidator() {
        validators.close();
    }

    @BeforeEach
    void configureService() {
        service = new CurrentAccountService(repository, validators.getValidator());
    }

    @Test
    void returnsProfileAndCurrentGuardianInKstSecondsWithoutExtendingGrant() {
        var until = NOW.plusMinutes(20).plusNanos(123456000);
        var access = access(0, 0, null, null, until, 1L, 1L, NOW);
        var stored = state("guardian@example.invalid", access);
        given(stored);

        var view = service.getCurrentAccount(session);

        assertThat(view.accountId()).isEqualTo(session.accountId());
        assertThat(view.email()).isEqualTo(stored.email());
        assertThat(view.childId()).isEqualTo(stored.childId());
        assertThat(view.hasPin()).isTrue();
        assertThat(view.guardianUnlockedUntil()).isEqualTo(OffsetDateTime.parse("2026-11-17T15:20:00+09:00"));
        assertThat(view.guardianUnlockedUntil().getOffset()).isEqualTo(ZoneOffset.ofHours(9));
        assertThat(service.getCurrentAccount(session).guardianUnlockedUntil()).isEqualTo(view.guardianUnlockedUntil());
    }

    @Test
    void beforeChildAndPinIsNormalState() {
        given(new AccountState(session.accountId(), "guardian@example.invalid", null,
                access(0, 0, null, null, null, null, null, NOW)));

        var view = service.getCurrentAccount(session);

        assertThat(view.childId()).isNull();
        assertThat(view.hasPin()).isFalse();
        assertThat(view.guardianUnlockedUntil()).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"NO_STATE", "ACCOUNT_VERSION", "CONTEXT_VERSION", "REVOKED", "EXPIRED", "EXPIRY_EQUAL"})
    void invalidLoginIsAuthenticationFailure(String change) {
        var access = switch (change) {
            case "ACCOUNT_VERSION" -> access(0, 1, null, null, null, null, 1L, NOW);
            case "CONTEXT_VERSION" -> access(1, 0, null, null, null, null, 1L, NOW);
            case "REVOKED" -> access(0, 0, NOW, null, null, null, 1L, NOW);
            case "EXPIRED" -> access(0, 0, null, NOW.minusSeconds(1), null, null, 1L, NOW);
            case "EXPIRY_EQUAL" -> access(0, 0, null, NOW, null, null, 1L, NOW);
            default -> access(0, 0, null, null, null, null, 1L, NOW);
        };
        if (change.equals("NO_STATE")) {
            when(repository.findState(session.accountId(), session.securityContextId())).thenReturn(Optional.empty());
        }
        else {
            given(state("guardian@example.invalid", access));
        }

        assertThatThrownBy(() -> service.getCurrentAccount(session)).isInstanceOf(AuthenticationException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"NO_GRANT", "EXPIRED", "EQUAL", "OLD_PIN", "NO_PIN", "OLD_SUBSECOND"})
    void invalidGuardianReturnsNullWithoutRejectingLogin(String change) {
        var until = change.equals("EXPIRED") ? NOW.minusSeconds(1) : change.equals("EQUAL") ? NOW : NOW.plusMinutes(20);
        if (change.equals("NO_GRANT")) {
            until = null;
        }
        var checked = change.equals("OLD_SUBSECOND") ? NOW.plusNanos(500000000) : NOW;
        if (change.equals("OLD_SUBSECOND")) {
            until = NOW.plusNanos(900000000);
        }
        Long pin = change.equals("NO_PIN") ? null : change.equals("OLD_PIN") ? 2L : 1L;
        given(state("guardian@example.invalid", access(0, 0, null, null, until, until == null ? null : 1L, pin, checked)));

        var view = service.getCurrentAccount(session);

        assertThat(view.hasPin()).isEqualTo(!change.equals("NO_PIN"));
        assertThat(view.guardianUnlockedUntil()).isNull();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"not-an-email", "한글@example.invalid", "someone@example.invalid\n", " someone@example.invalid"})
    void invalidStoredEmailIsUnavailableRatherThanBrokenAccountView(String email) {
        given(state(email, access(0, 0, null, null, null, null, null, NOW)));

        assertThatThrownBy(() -> service.getCurrentAccount(session)).isInstanceOf(AuthStateUnavailableException.class);
    }

    @Test
    void overlongStoredEmailIsUnavailable() {
        var longEmail = "a".repeat(63) + "@" + "a".repeat(63) + "." + "b".repeat(63) + "." + "c".repeat(63) + ".invalid";
        given(state(longEmail, access(0, 0, null, null, null, null, null, NOW)));

        assertThatThrownBy(() -> service.getCurrentAccount(session)).isInstanceOf(AuthStateUnavailableException.class);
    }

    @Test
    void storageUnavailabilityIsNotConvertedToLoggedOutOrEmptyProfile() {
        var failure = new AuthStateUnavailableException();
        when(repository.findState(session.accountId(), session.securityContextId())).thenThrow(failure);

        assertThatThrownBy(() -> service.getCurrentAccount(session)).isSameAs(failure);
    }

    /** DB에서 함께 읽은 프로필과 권한 상태를 모의 저장소에 전달한다. */
    private void given(AccountState state) {
        when(repository.findState(session.accountId(), session.securityContextId())).thenReturn(Optional.of(state));
    }

    /** 현재 계정과 고정한 권한 판정 시각을 사용해 경계 테스트를 구성한다. */
    private AccountState state(String email, GuardianAccessState access) {
        return new AccountState(session.accountId(), email, UUID.randomUUID(), access);
    }

    /** 기존 공통 판정 객체를 그대로 사용해 새 판정 규칙이 생기지 않게 한다. */
    private GuardianAccessState access(long contextVersion, long accountVersion, OffsetDateTime revoked,
            OffsetDateTime expiry, OffsetDateTime until, Long grantedVersion, Long currentVersion, OffsetDateTime checked) {
        return new GuardianAccessState(contextVersion, accountVersion, revoked, expiry, until, grantedVersion, currentVersion, checked);
    }
}
