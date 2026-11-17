package com.dodamdodam.backend.guardian.infrastructure;

import com.dodamdodam.backend.TestcontainersConfiguration;
import com.dodamdodam.backend.guardian.exception.GuardianAccessException;
import com.dodamdodam.backend.guardian.infrastructure.GuardianAccessRepository.GuardianAccessState;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class GuardianAccessRepositoryTests {

    private static final OffsetDateTime CHECKED_AT = OffsetDateTime.parse("2026-11-04T00:00:00Z");
    @Autowired private JdbcTemplate jdbc;
    @Autowired private GuardianAccessRepository repository;
    @Autowired private PlatformTransactionManager transactionManager;

    @BeforeEach
    @AfterEach
    void clearRows() {
        // PIN의 계정 FK를 먼저 정리해 다른 테스트에 fixture를 남기지 않는다.
        jdbc.update("DELETE FROM guardian_pins");
        jdbc.update("DELETE FROM session_security");
        jdbc.update("DELETE FROM accounts");
    }

    @Test
    void migrationStoresPinVersionAndFiniteTimestampDefaults() {
        var accountId = account();

        assertThatCode(() -> jdbc.update("INSERT INTO guardian_pins (account_id, pin_hash) VALUES (?, ?)",
                accountId, "synthetic-hash")).doesNotThrowAnyException();

        var stored = jdbc.queryForMap("""
                SELECT pin_version, isfinite(created_at) AS finite_created, isfinite(updated_at) AS finite_updated
                FROM guardian_pins WHERE account_id = ?
                """, accountId);
        assertThat(stored).containsEntry("pin_version", 1L)
                .containsEntry("finite_created", true).containsEntry("finite_updated", true);
    }

    @Test
    void databaseRejectsSecondPinForSameAccount() {
        var accountId = account();
        pin(accountId);
        assertThatThrownBy(() -> pin(accountId)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void databaseRejectsPinWithoutAccount() {
        assertThatThrownBy(() -> pin(UUID.randomUUID())).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void pinOwnershipPreventsAccountDeletion() {
        var accountId = account();
        pin(accountId);
        assertThatThrownBy(() -> jdbc.update("DELETE FROM accounts WHERE id = ?", accountId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "account_id = NULL", "pin_hash = NULL", "pin_hash = ''",
            "pin_version = NULL", "pin_version = 0", "pin_version = -1",
            "created_at = NULL", "created_at = 'infinity'::timestamptz", "created_at = '-infinity'::timestamptz",
            "updated_at = NULL", "updated_at = 'infinity'::timestamptz", "updated_at = '-infinity'::timestamptz"
    })
    void databaseRejectsInvalidPinState(String assignment) {
        var accountId = account();
        pin(accountId);
        assertThatThrownBy(() -> jdbc.update("UPDATE guardian_pins SET " + assignment + " WHERE account_id = ?", accountId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @ParameterizedTest
    @CsvSource({"false,1", "true,1", "false,128"})
    void acceptsCurrentGuardianWithUnlimitedOrFiniteLogin(boolean finite, long pinVersion) {
        var fixture = granted();
        // 여러 번 재설정한 PIN도 버전 숫자가 같으면 현재 권한으로 인정해야 한다.
        jdbc.update("UPDATE guardian_pins SET pin_version = ? WHERE account_id = ?", pinVersion, fixture.accountId());
        jdbc.update("UPDATE session_security SET guardian_pin_version = ? WHERE id = ?", pinVersion, fixture.contextId());
        if (finite) {
            jdbc.update("UPDATE session_security SET expires_at = clock_timestamp() + INTERVAL '30 minutes' WHERE id = ?",
                    fixture.contextId());
        }

        var state = repository.findState(fixture.accountId(), fixture.contextId()).orElseThrow();

        assertThat(state.isSessionCurrent()).isTrue();
        assertThat(state.isGuardianCurrent()).isTrue();
        assertThat(state.contextSessionVersion()).isZero();
        assertThat(state.accountSessionVersion()).isZero();
        assertThat(state.guardianPinVersion()).isEqualTo(pinVersion);
        assertThat(state.currentPinVersion()).isEqualTo(pinVersion);
        assertThat(state.checkedAt()).isBefore(state.guardianUntil());
    }

    @Test
    void missingOrOtherAccountsContextDoesNotReturnState() {
        var fixture = granted();
        assertThat(repository.findState(fixture.accountId(), UUID.randomUUID())).isEmpty();
        assertThat(repository.findState(account(), fixture.contextId())).isEmpty();
        assertThat(repository.findState(UUID.randomUUID(), fixture.contextId())).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"ACCOUNT_VERSION", "CONTEXT_VERSION", "REVOKED", "EXPIRED"})
    void invalidLoginAlsoRejectsGuardian(String change) {
        var fixture = granted();
        switch (change) {
            case "ACCOUNT_VERSION" -> jdbc.update("UPDATE accounts SET session_version = 1 WHERE id = ?", fixture.accountId());
            case "CONTEXT_VERSION" -> jdbc.update("UPDATE session_security SET session_version = 1 WHERE id = ?", fixture.contextId());
            case "REVOKED" -> jdbc.update("UPDATE session_security SET revoked_at = clock_timestamp() WHERE id = ?", fixture.contextId());
            case "EXPIRED" -> jdbc.update("""
                    UPDATE session_security SET expires_at = clock_timestamp() - INTERVAL '1 second',
                        guardian_unlocked_until = clock_timestamp() - INTERVAL '2 seconds' WHERE id = ?
                    """, fixture.contextId());
            default -> throw new AssertionError(change);
        }

        var state = repository.findState(fixture.accountId(), fixture.contextId()).orElseThrow();
        assertThat(state.isSessionCurrent()).isFalse();
        assertThat(state.isGuardianCurrent()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"NO_GRANT", "EXPIRED_GRANT", "OLD_PIN_VERSION", "NO_PIN"})
    void invalidGuardianKeepsLoginValid(String change) {
        var fixture = granted();
        switch (change) {
            case "NO_GRANT" -> jdbc.update("""
                    UPDATE session_security SET guardian_unlocked_until = NULL, guardian_pin_version = NULL WHERE id = ?
                    """, fixture.contextId());
            case "EXPIRED_GRANT" -> jdbc.update("""
                    UPDATE session_security SET guardian_unlocked_until = clock_timestamp() - INTERVAL '1 second' WHERE id = ?
                    """, fixture.contextId());
            case "OLD_PIN_VERSION" -> jdbc.update("UPDATE guardian_pins SET pin_version = 2 WHERE account_id = ?", fixture.accountId());
            case "NO_PIN" -> jdbc.update("DELETE FROM guardian_pins WHERE account_id = ?", fixture.accountId());
            default -> throw new AssertionError(change);
        }

        var state = repository.findState(fixture.accountId(), fixture.contextId()).orElseThrow();
        assertThat(state.isSessionCurrent()).isTrue();
        assertThat(state.isGuardianCurrent()).isFalse();
    }

    @Test
    void anotherSessionDoesNotInheritGuardianGrant() {
        var fixture = granted();
        var otherContext = context(fixture.accountId());

        var state = repository.findState(fixture.accountId(), otherContext).orElseThrow();
        assertThat(state.isSessionCurrent()).isTrue();
        assertThat(state.isGuardianCurrent()).isFalse();
        assertThat(repository.findState(fixture.accountId(), fixture.contextId()).orElseThrow().isGuardianCurrent()).isTrue();
    }

    @ParameterizedTest
    @CsvSource({"-1,false", "0,false", "1,true"})
    void guardianUntilMustBeStrictlyAfterCheckedAt(long nanos, boolean valid) {
        var state = new GuardianAccessState(0, 0, null, null, CHECKED_AT.plusNanos(nanos), 1L, 1L, CHECKED_AT);
        assertThat(state.isSessionCurrent()).isTrue();
        assertThat(state.isGuardianCurrent()).isEqualTo(valid);
    }

    @ParameterizedTest
    @CsvSource({"-1,false", "0,false", "1,true"})
    void finiteLoginExpiresAtEquality(long nanos, boolean valid) {
        var state = new GuardianAccessState(0, 0, null, CHECKED_AT.plusNanos(nanos), null, null, 1L, CHECKED_AT);
        assertThat(state.isSessionCurrent()).isEqualTo(valid);
    }

    @Test
    void queryUsesFreshDatabaseTimeInsideTransaction() {
        var fixture = granted();
        new TransactionTemplate(transactionManager).executeWithoutResult(transaction -> {
            var started = jdbc.queryForObject("SELECT transaction_timestamp()", OffsetDateTime.class);
            var before = jdbc.queryForObject("SELECT clock_timestamp()", OffsetDateTime.class);
            var state = repository.findState(fixture.accountId(), fixture.contextId()).orElseThrow();
            var after = jdbc.queryForObject("SELECT clock_timestamp()", OffsetDateTime.class);

            assertThat(state.checkedAt().toInstant()).isAfter(started.toInstant())
                    .isBetween(before.toInstant(), after.toInstant());
        });
    }

    @Test
    void queryNeedsNeitherPinHashReadNorWritePermission() {
        var fixture = granted();
        var state = new TransactionTemplate(transactionManager).execute(transaction -> {
            // 역할과 권한은 이 테스트 트랜잭션의 rollback으로 함께 정리한다.
            transaction.setRollbackOnly();
            jdbc.execute("CREATE ROLE guardian_projection_reader NOLOGIN");
            jdbc.execute("GRANT USAGE ON SCHEMA public TO guardian_projection_reader");
            jdbc.execute("GRANT SELECT (id, session_version) ON accounts TO guardian_projection_reader");
            jdbc.execute("""
                    GRANT SELECT (id, account_id, session_version, revoked_at, expires_at,
                        guardian_unlocked_until, guardian_pin_version) ON session_security TO guardian_projection_reader
                    """);
            jdbc.execute("GRANT SELECT (account_id, pin_version) ON guardian_pins TO guardian_projection_reader");
            jdbc.execute("SET LOCAL ROLE guardian_projection_reader");
            return repository.findState(fixture.accountId(), fixture.contextId()).orElseThrow();
        });

        assertThat(state.isGuardianCurrent()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"accounts", "session_security", "guardian_pins"})
    void storageFailureIs503RatherThanMissingState(String table) {
        var fixture = granted();
        jdbc.execute("ALTER TABLE " + table + " RENAME TO unavailable_" + table);
        try {
            assertThatThrownBy(() -> repository.findState(fixture.accountId(), fixture.contextId()))
                    .isInstanceOfSatisfying(GuardianAccessException.class, failure -> {
                        assertThat(failure.status()).isEqualTo(503);
                        assertThat(failure.code()).isEqualTo("AUTH_STATE_UNAVAILABLE");
                        assertThat(failure.getCause()).isInstanceOf(DataAccessException.class);
                        assertThat(failure.getMessage()).doesNotContain("SELECT", "synthetic-hash", "unavailable_");
                    });
        }
        finally {
            jdbc.execute("ALTER TABLE unavailable_" + table + " RENAME TO " + table);
        }
    }

    private UUID account() {
        var accountId = UUID.randomUUID();
        jdbc.update("INSERT INTO accounts (id, email) VALUES (?, ?)", accountId, accountId + "@example.invalid");
        return accountId;
    }

    private void pin(UUID accountId) {
        jdbc.update("INSERT INTO guardian_pins (account_id, pin_hash) VALUES (?, ?)", accountId, "synthetic-hash");
    }

    private UUID context(UUID accountId) {
        var contextId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO session_security (id, account_id, session_version, created_at, expires_at)
                VALUES (?, ?, 0, clock_timestamp() - INTERVAL '1 minute', NULL)
                """, contextId, accountId);
        return contextId;
    }

    private Fixture granted() {
        var accountId = account();
        pin(accountId);
        var contextId = context(accountId);
        jdbc.update("""
                UPDATE session_security SET guardian_unlocked_until = clock_timestamp() + INTERVAL '15 minutes',
                    guardian_pin_version = 1 WHERE id = ?
                """, contextId);
        return new Fixture(accountId, contextId);
    }

    private record Fixture(UUID accountId, UUID contextId) { }
}
