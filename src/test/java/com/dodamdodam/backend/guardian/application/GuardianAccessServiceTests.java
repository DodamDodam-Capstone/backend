package com.dodamdodam.backend.guardian.application;

import com.dodamdodam.backend.auth.session.AuthenticatedSession;
import com.dodamdodam.backend.guardian.exception.GuardianAccessException;
import com.dodamdodam.backend.guardian.infrastructure.GuardianAccessRepository;
import com.dodamdodam.backend.guardian.infrastructure.GuardianAccessRepository.GuardianAccessState;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GuardianAccessServiceTests {

    private static final OffsetDateTime CHECKED_AT = OffsetDateTime.parse("2026-11-04T00:00:00Z");
    private final AuthenticatedSession authenticated = new AuthenticatedSession(UUID.randomUUID(), UUID.randomUUID());
    private final MockHttpServletRequest request = new MockHttpServletRequest();

    @Mock private GuardianAccessRepository repository;
    private GuardianAccessService service;

    @BeforeEach
    void setUp() {
        service = new GuardianAccessService(repository);
    }

    @Test
    void returnsDbDeadlineWithoutCreatingOrChangingNativeSession() {
        state(valid());

        assertThat(service.requireGuardianAccess(request, authenticated)).isEqualTo(CHECKED_AT.plusMinutes(15));
        assertThat(request.getSession(false)).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"MISSING", "REVOKED", "VERSION", "EXPIRED"})
    void invalidLoginContextRequiresAuthenticationBeforeGuardian(String invalid) {
        var state = switch (invalid) {
            case "MISSING" -> null;
            case "REVOKED" -> new GuardianAccessState(0, 0, CHECKED_AT, null, null, null, null, CHECKED_AT);
            case "VERSION" -> new GuardianAccessState(0, 1, null, null, null, null, null, CHECKED_AT);
            case "EXPIRED" -> new GuardianAccessState(0, 0, null, CHECKED_AT, null, null, null, CHECKED_AT);
            default -> throw new IllegalArgumentException(invalid);
        };
        state(state);

        assertThatThrownBy(() -> service.requireGuardianAccess(request, authenticated))
                .isInstanceOfSatisfying(GuardianAccessException.class, failure -> {
                    assertThat(failure.status()).isEqualTo(401);
                    assertThat(failure.code()).isEqualTo("AUTHENTICATION_REQUIRED");
                });
    }

    @Test
    void repositoryFailureKeeps503AndItsPrivateCause() {
        var cause = new IllegalStateException("synthetic-private-value");
        when(repository.findState(authenticated.accountId(), authenticated.securityContextId()))
                .thenThrow(GuardianAccessException.unavailable(cause));

        assertThatThrownBy(() -> service.requireGuardianAccess(request, authenticated))
                .isInstanceOfSatisfying(GuardianAccessException.class, failure -> {
                    assertThat(failure.status()).isEqualTo(503);
                    assertThat(failure.getCause()).isSameAs(cause);
                    assertThat(failure.getMessage()).doesNotContain(cause.getMessage());
                });
    }

    @ParameterizedTest
    @ValueSource(strings = {"EXPIRED", "REVOKED", "UNAVAILABLE"})
    void responseChecksReadFreshStateEveryTime(String change) {
        state(valid());
        service.requireGuardianAccess(request, authenticated);
        // 성공 응답 재검사도 표식을 소비하지 않아 다음 검사에서 폐기를 감지해야 한다.
        service.verifyResponseAccess(request);
        var expectedStatus = switch (change) {
            case "EXPIRED" -> {
                state(new GuardianAccessState(0, 0, null, null, CHECKED_AT, 1L, 1L, CHECKED_AT));
                yield 403;
            }
            case "REVOKED" -> {
                state(null);
                yield 401;
            }
            case "UNAVAILABLE" -> {
                when(repository.findState(authenticated.accountId(), authenticated.securityContextId()))
                        .thenThrow(GuardianAccessException.unavailable(new IllegalStateException()));
                yield 503;
            }
            default -> throw new IllegalArgumentException(change);
        };

        assertThatThrownBy(() -> service.verifyResponseAccess(request))
                .isInstanceOfSatisfying(GuardianAccessException.class,
                        failure -> assertThat(failure.status()).isEqualTo(expectedStatus));
    }

    @Test
    void responseWithoutSuccessfulGuardianCheckDoesNotAccessRepository() {
        assertThatCode(() -> service.verifyResponseAccess(request)).doesNotThrowAnyException();
    }

    @Test
    void failedGuardianCheckDoesNotMarkResponse() {
        state(new GuardianAccessState(0, 0, null, null, null, null, null, CHECKED_AT));
        assertThatThrownBy(() -> service.requireGuardianAccess(request, authenticated))
                .isInstanceOf(GuardianAccessException.class);

        assertThatCode(() -> service.verifyResponseAccess(request)).doesNotThrowAnyException();
    }

    @Test
    void successfulMarkerIsConfinedToTheSameRequest() {
        state(valid());
        service.requireGuardianAccess(request, authenticated);

        assertThatCode(() -> service.verifyResponseAccess(new MockHttpServletRequest())).doesNotThrowAnyException();
    }

    private GuardianAccessState valid() {
        return new GuardianAccessState(0, 0, null, null, CHECKED_AT.plusMinutes(15), 1L, 1L, CHECKED_AT);
    }

    private void state(GuardianAccessState state) {
        when(repository.findState(authenticated.accountId(), authenticated.securityContextId()))
                .thenReturn(Optional.ofNullable(state));
    }
}
