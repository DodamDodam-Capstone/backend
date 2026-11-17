package com.dodamdodam.backend.auth.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dodamdodam.backend.TestcontainersConfiguration;
import com.dodamdodam.backend.auth.session.AuthStateUnavailableException;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = "spring.session.jdbc.cleanup-cron=-")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CurrentAccountRepositoryTests {

    @Autowired private JdbcTemplate jdbc;
    @Autowired private CurrentAccountRepository repository;
    @Autowired private PlatformTransactionManager transactions;

    @BeforeEach
    @AfterEach
    void cleanRows() {
        jdbc.update("DELETE FROM guardian_pins");
        jdbc.update("DELETE FROM children");
        jdbc.update("DELETE FROM session_security");
        jdbc.update("DELETE FROM accounts");
    }

    @ParameterizedTest
    @CsvSource({"false,false", "true,false", "true,true", "false,true"})
    void returnsCurrentAccountsOptionalChildAndPin(boolean hasChild, boolean hasPin) {
        var fixture = fixture(hasChild, hasPin);

        var state = repository.findState(fixture.accountId(), fixture.contextId()).orElseThrow();

        assertThat(state.accountId()).isEqualTo(fixture.accountId());
        assertThat(state.email()).isEqualTo(fixture.email());
        assertThat(state.childId()).isEqualTo(fixture.childId());
        assertThat(state.access().currentPinVersion()).isEqualTo(hasPin ? 1L : null);
        assertThat(state.access().isSessionCurrent()).isTrue();
        assertThat(state.access().isGuardianCurrent()).isFalse();
    }

    @Test
    void missingAndOtherAccountsContextReturnNoState() {
        var first = fixture(true, true);
        var second = fixture(true, true);

        assertThat(repository.findState(first.accountId(), second.contextId())).isEmpty();
        assertThat(repository.findState(second.accountId(), first.contextId())).isEmpty();
        assertThat(repository.findState(first.accountId(), UUID.randomUUID())).isEmpty();
        assertThat(repository.findState(UUID.randomUUID(), first.contextId())).isEmpty();
    }

    @Test
    void anotherContextForSameAccountDoesNotInheritGuardian() {
        var fixture = fixture(true, true);
        grant(fixture);
        var secondContext = context(fixture.accountId());

        assertThat(repository.findState(fixture.accountId(), fixture.contextId()).orElseThrow()
                .access().isGuardianCurrent()).isTrue();
        var second = repository.findState(fixture.accountId(), secondContext).orElseThrow();
        assertThat(second.childId()).isEqualTo(fixture.childId());
        assertThat(second.access().isSessionCurrent()).isTrue();
        assertThat(second.access().isGuardianCurrent()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"ACCOUNT_VERSION", "CONTEXT_VERSION", "REVOKED", "EXPIRED"})
    void projectsInvalidSessionStateForAuthenticationDecision(String change) {
        var fixture = fixture(true, true);
        switch (change) {
            case "ACCOUNT_VERSION" -> jdbc.update("UPDATE accounts SET session_version = 1 WHERE id = ?", fixture.accountId());
            case "CONTEXT_VERSION" -> jdbc.update("UPDATE session_security SET session_version = 1 WHERE id = ?", fixture.contextId());
            case "REVOKED" -> jdbc.update("UPDATE session_security SET revoked_at = clock_timestamp() WHERE id = ?", fixture.contextId());
            case "EXPIRED" -> jdbc.update("UPDATE session_security SET expires_at = clock_timestamp() - INTERVAL '1 second' WHERE id = ?", fixture.contextId());
            default -> throw new AssertionError(change);
        }

        assertThat(repository.findState(fixture.accountId(), fixture.contextId()).orElseThrow()
                .access().isSessionCurrent()).isFalse();
    }

    @Test
    void readsFreshDatabaseTimeInsideTransaction() {
        var fixture = fixture(true, true);
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            var started = jdbc.queryForObject("SELECT transaction_timestamp()", OffsetDateTime.class);
            var before = jdbc.queryForObject("SELECT clock_timestamp()", OffsetDateTime.class);
            var state = repository.findState(fixture.accountId(), fixture.contextId()).orElseThrow();
            var after = jdbc.queryForObject("SELECT clock_timestamp()", OffsetDateTime.class);

            assertThat(state.access().checkedAt().toInstant()).isAfter(started.toInstant())
                    .isBetween(before.toInstant(), after.toInstant());
        });
    }

    @Test
    void projectionNeedsNoPasswordOrPinHashOrWritePermission() {
        var fixture = fixture(true, true);
        grant(fixture);
        var state = new TransactionTemplate(transactions).execute(status -> {
            // 역할 생성과 권한 부여는 rollback으로 정리한다. 조회에 필요한 열만 허용한다.
            status.setRollbackOnly();
            jdbc.execute("CREATE ROLE current_account_reader NOLOGIN");
            jdbc.execute("GRANT USAGE ON SCHEMA public TO current_account_reader");
            jdbc.execute("GRANT SELECT (id, email, session_version) ON accounts TO current_account_reader");
            jdbc.execute("GRANT SELECT (id, account_id) ON children TO current_account_reader");
            jdbc.execute("GRANT SELECT (account_id, pin_version) ON guardian_pins TO current_account_reader");
            jdbc.execute("""
                    GRANT SELECT (id, account_id, session_version, revoked_at, expires_at,
                        guardian_unlocked_until, guardian_pin_version) ON session_security TO current_account_reader
                    """);
            jdbc.execute("SET LOCAL ROLE current_account_reader");
            return repository.findState(fixture.accountId(), fixture.contextId()).orElseThrow();
        });

        assertThat(state.childId()).isEqualTo(fixture.childId());
        assertThat(state.access().isGuardianCurrent()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"accounts", "children", "guardian_pins", "session_security"})
    void databaseFailureIsUnavailableRatherThanMissingState(String table) {
        var fixture = fixture(true, true);
        jdbc.execute("ALTER TABLE " + table + " RENAME TO unavailable_" + table);
        try {
            assertThatThrownBy(() -> repository.findState(fixture.accountId(), fixture.contextId()))
                    .isInstanceOfSatisfying(AuthStateUnavailableException.class, failure -> {
                        assertThat(failure.getCause()).isInstanceOf(DataAccessException.class);
                        assertThat(failure.getMessage()).doesNotContain("SELECT", "unavailable_", fixture.email());
                    });
        }
        finally {
            jdbc.execute("ALTER TABLE unavailable_" + table + " RENAME TO " + table);
        }
    }

    /** 실제 FK와 계정당 1명 제약을 따르는 합성 조회 상태를 만든다. */
    private Fixture fixture(boolean child, boolean pin) {
        var accountId = UUID.randomUUID();
        var email = accountId + "@example.invalid";
        jdbc.update("INSERT INTO accounts (id, email) VALUES (?, ?)", accountId, email);
        UUID childId = child ? UUID.randomUUID() : null;
        if (child) {
            jdbc.update("""
                    INSERT INTO children (id, account_id, name, nickname, birth_date, gender, character_id)
                    VALUES (?, ?, '도담', '도담이', DATE '2020-01-01', 'FEMALE', 'synthetic')
                    """, childId, accountId);
        }
        if (pin) {
            jdbc.update("INSERT INTO guardian_pins (account_id, pin_hash) VALUES (?, 'synthetic-hash')", accountId);
        }
        return new Fixture(accountId, context(accountId), childId, email);
    }

    /** 같은 계정의 독립 로그인도 보호자 확인 상태를 공유하지 않도록 문맥을 분리한다. */
    private UUID context(UUID accountId) {
        var contextId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO session_security (id, account_id, session_version, created_at, expires_at)
                VALUES (?, ?, 0, clock_timestamp() - INTERVAL '1 minute', NULL)
                """, contextId, accountId);
        return contextId;
    }

    /** 미래 보호자 기한을 현재 PIN 버전과 같은 문맥에만 결합한다. */
    private void grant(Fixture fixture) {
        jdbc.update("""
                UPDATE session_security SET guardian_unlocked_until = clock_timestamp() + INTERVAL '15 minutes',
                    guardian_pin_version = 1 WHERE id = ?
                """, fixture.contextId());
    }

    private record Fixture(UUID accountId, UUID contextId, UUID childId, String email) { }
}
