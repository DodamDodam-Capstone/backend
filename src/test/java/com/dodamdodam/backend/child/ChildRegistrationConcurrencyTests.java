package com.dodamdodam.backend.child;

import com.dodamdodam.backend.TestcontainersConfiguration;
import com.dodamdodam.backend.auth.session.AuthenticatedSession;
import com.dodamdodam.backend.auth.session.SessionSecurityRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Import({TestcontainersConfiguration.class, ChildRegistrationConcurrencyTests.CatalogueFixture.class})
@SpringBootTest(properties = {"spring.session.jdbc.cleanup-cron=-"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Timeout(20)
class ChildRegistrationConcurrencyTests {

    @Autowired private JdbcTemplate jdbc;
    @Autowired private ChildService service;
    @Autowired @Qualifier("childService") private ChildService withoutCatalogue;
    @Autowired private SessionSecurityRepository sessions;
    @Autowired private PlatformTransactionManager transactionManager;

    @BeforeEach
    void clear() {
        jdbc.update("DELETE FROM children");
        jdbc.update("DELETE FROM email_verifications");
        jdbc.update("DELETE FROM session_security");
        jdbc.update("DELETE FROM accounts");
    }

    @Test
    void registersNormalizedProfileWithSessionOwnerAndInjectedClock() {
        var session = session();
        var request = new CreateChildRequest("\u00a0가나 ", " 별 이 ", LocalDate.of(2026, 11, 1),
                ChildView.Gender.FEMALE, List.of(" 공 룡 ", "그림"), "test-character");

        var child = service.create(session, request);

        assertThat(child.name()).isEqualTo("가나");
        assertThat(child.nickname()).isEqualTo("별 이");
        assertThat(child.birthDate()).isEqualTo(LocalDate.of(2026, 11, 1));
        assertThat(child.interests()).containsExactly("공 룡", "그림");
        assertThat(jdbc.queryForObject("SELECT account_id FROM children WHERE id = ?", UUID.class, child.childId()))
                .isEqualTo(session.accountId());
        assertChildCount(1);
    }

    @Test
    void rejectsSecondChildForSameAccount() {
        var session = session();
        service.create(session, profile());

        var failure = assertThrows(ChildException.class, () -> service.create(session, profile()));

        assertThat(failure.code()).isEqualTo("CHILD_LIMIT_REACHED");
        assertThat(failure.fields()).isEmpty();
        assertChildCount(1);
    }

    @Test
    void rejectsRevokedContextBeforeInsert() {
        var session = session();
        jdbc.update("UPDATE session_security SET revoked_at = clock_timestamp() WHERE id = ?", session.securityContextId());

        assertThatThrownBy(() -> service.create(session, profile())).isInstanceOf(InsufficientAuthenticationException.class);
        assertChildCount(0);
    }

    @ParameterizedTest
    @ValueSource(strings = {"MISSING_CONTEXT", "MISSING_ACCOUNT", "WRONG_OWNER", "VERSION", "EXPIRED"})
    void rejectsInvalidContextsBeforeInsert(String change) {
        var original = session();
        var session = switch (change) {
            case "MISSING_ACCOUNT" -> new AuthenticatedSession(UUID.randomUUID(), original.securityContextId());
            case "WRONG_OWNER" -> new AuthenticatedSession(account(), original.securityContextId());
            default -> original;
        };
        if (change.equals("MISSING_CONTEXT")) jdbc.update("DELETE FROM session_security WHERE id = ?", session.securityContextId());
        if (change.equals("VERSION")) jdbc.update("UPDATE accounts SET session_version = 1 WHERE id = ?", session.accountId());
        if (change.equals("EXPIRED")) jdbc.update("""
                UPDATE session_security SET created_at = clock_timestamp() - INTERVAL '2 days',
                    expires_at = clock_timestamp() - INTERVAL '1 day' WHERE id = ?
                """, session.securityContextId());

        assertThatThrownBy(() -> service.create(session, profile())).isInstanceOf(InsufficientAuthenticationException.class);
        assertChildCount(0);
    }

    @Test
    void validatesInputBeforeCheckingContext() {
        var invalid = new CreateChildRequest("", "별이", LocalDate.of(2020, 5, 12),
                ChildView.Gender.FEMALE, List.of(), "test-character");
        var missing = new AuthenticatedSession(UUID.randomUUID(), UUID.randomUUID());

        var failure = assertThrows(ChildException.class, () -> service.create(missing, invalid));

        assertThat(failure.code()).isEqualTo("VALIDATION_FAILED");
        assertThat(failure.fields()).extracting(error -> error.field()).containsExactly("name");
        assertChildCount(0);
    }

    @Test
    void unconfiguredCatalogueCannotRegisterFixtureCharacters() {
        var session = session();

        var failure = assertThrows(ChildException.class, () -> withoutCatalogue.create(session, profile()));

        assertThat(failure.code()).isEqualTo("VALIDATION_FAILED");
        assertThat(failure.fields()).extracting(error -> error.field()).containsExactly("characterId");
        assertChildCount(0);
    }

    @Test
    void accountAndContextLocksRequireTheRegistrationTransaction() {
        var session = session();

        assertThatThrownBy(() -> sessions.requireCurrentContextForUpdate(session.accountId(), session.securityContextId()))
                .isInstanceOf(IllegalTransactionStateException.class);
    }

    @Test
    void registrationWaitsForAccountLockAndRechecksVersion() throws Exception {
        var session = session();
        var entered = new CompletableFuture<Integer>();
        try (var worker = Executors.newSingleThreadExecutor()) {
            var pending = transaction().execute(status -> {
                var blocker = jdbc.queryForObject("SELECT pg_backend_pid()", Integer.class);
                jdbc.queryForList("SELECT id FROM accounts WHERE id = ? FOR UPDATE", UUID.class, session.accountId());
                var attempt = worker.submit(() -> transaction().execute(other -> {
                    jdbc.execute("SET LOCAL statement_timeout = '10s'");
                    entered.complete(jdbc.queryForObject("SELECT pg_backend_pid()", Integer.class));
                    return service.create(session, profile());
                }));
                awaitBlocked(entered.orTimeout(5, TimeUnit.SECONDS).join(), blocker);
                // 계정 대기 중 문맥을 먼저 잠그지 않았음을 실제 NOWAIT로 확인한다.
                jdbc.queryForList("SELECT id FROM session_security WHERE id = ? FOR UPDATE NOWAIT",
                        UUID.class, session.securityContextId());
                jdbc.update("UPDATE accounts SET session_version = 1 WHERE id = ?", session.accountId());
                return attempt;
            });

            assertThatThrownBy(() -> pending.get(5, TimeUnit.SECONDS))
                    .isInstanceOf(ExecutionException.class).hasCauseInstanceOf(InsufficientAuthenticationException.class);
            assertChildCount(0);
        }
    }

    @Test
    void registrationRechecksDbTimeAfterWaitingForContextLock() throws Exception {
        var session = session();
        var entered = new CompletableFuture<Integer>();
        try (var worker = Executors.newSingleThreadExecutor()) {
            var pending = transaction().execute(status -> {
                var blocker = jdbc.queryForObject("SELECT pg_backend_pid()", Integer.class);
                jdbc.queryForList("SELECT id FROM session_security WHERE id = ? FOR UPDATE",
                        UUID.class, session.securityContextId());
                var attempt = worker.submit(() -> transaction().execute(other -> {
                    jdbc.execute("SET LOCAL statement_timeout = '10s'");
                    entered.complete(jdbc.queryForObject("SELECT pg_backend_pid()", Integer.class));
                    return service.create(session, profile());
                }));
                awaitBlocked(entered.orTimeout(5, TimeUnit.SECONDS).join(), blocker);
                jdbc.update("UPDATE session_security SET expires_at = clock_timestamp() WHERE id = ?", session.securityContextId());
                return attempt;
            });

            assertThatThrownBy(() -> pending.get(5, TimeUnit.SECONDS))
                    .isInstanceOf(ExecutionException.class).hasCauseInstanceOf(InsufficientAuthenticationException.class);
            assertChildCount(0);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"CHILD", "AUTH"})
    void separatesBusinessStorageFailuresFromAuthStorageFailures(String storage) {
        var session = session();

        var failure = assertThrows(RuntimeException.class, () -> transaction().execute(status -> {
            if (storage.equals("CHILD")) jdbc.execute("ALTER TABLE children RENAME TO unavailable_children");
            else jdbc.execute("ALTER TABLE session_security RENAME TO unavailable_security");
            return service.create(session, profile());
        }));

        if (storage.equals("CHILD")) {
            assertThat(failure).isInstanceOfSatisfying(ChildException.class, business -> {
                assertThat(business.code()).isEqualTo("INTERNAL_ERROR");
                assertThat(business.getCause()).isInstanceOf(DataAccessException.class);
            });
        }
        else assertThat(failure).isInstanceOf(DataAccessException.class);
        assertChildCount(0);
    }

    private void awaitBlocked(int worker, int blocker) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < deadline) {
            if (Boolean.TRUE.equals(jdbc.queryForObject("SELECT ? = ANY(pg_blocking_pids(?))", Boolean.class, blocker, worker))) return;
            Thread.yield();
        }
        throw new AssertionError("등록이 잠금 해제를 기다리지 않았습니다.");
    }

    private TransactionTemplate transaction() {
        return new TransactionTemplate(transactionManager);
    }

    private void assertChildCount(int expected) {
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM children", Integer.class)).isEqualTo(expected);
    }

    private UUID account() {
        var id = UUID.randomUUID();
        jdbc.update("INSERT INTO accounts (id, email) VALUES (?, ?)", id, id + "@example.invalid");
        return id;
    }

    private AuthenticatedSession session() {
        return sessions.createLoginContext(account(), null);
    }

    private CreateChildRequest profile() {
        return new CreateChildRequest("가나", "별이", LocalDate.of(2020, 5, 12),
                ChildView.Gender.FEMALE, List.of("공룡"), "test-character");
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class CatalogueFixture {
        @Bean
        @Primary
        ChildService configuredChildService(ChildRepository repository, SessionSecurityRepository sessions) {
            return new ChildService(repository, sessions,
                    Clock.fixed(Instant.parse("2026-10-31T15:00:00Z"), ZoneOffset.UTC), Set.of("test-character"));
        }
    }
}
