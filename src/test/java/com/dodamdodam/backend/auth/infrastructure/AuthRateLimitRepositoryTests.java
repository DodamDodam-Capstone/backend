package com.dodamdodam.backend.auth.infrastructure;

import com.dodamdodam.backend.TestcontainersConfiguration;
import com.dodamdodam.backend.auth.application.AuthRateLimitService;
import com.dodamdodam.backend.auth.domain.AuthRateLimitKey;
import com.dodamdodam.backend.auth.domain.AuthRateLimitKey.Action;
import com.dodamdodam.backend.auth.dto.IssueEmailVerificationRequest.Purpose;
import com.dodamdodam.backend.auth.exception.AuthRateLimitException;
import com.dodamdodam.backend.auth.infrastructure.AuthRateLimitRepository.RateLimitState;
import com.dodamdodam.backend.auth.session.AuthStateUnavailableException;
import java.net.InetAddress;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AuthRateLimitRepositoryTests {

    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-11-15T00:00:00Z");
    private static final InetAddress IP = InetAddress.ofLiteral("192.0.2.10");
    private final AuthRateLimitKeyHasher hasher = new AuthRateLimitKeyHasher(new byte[32]);
    @Autowired private JdbcTemplate jdbc;
    @Autowired private AuthRateLimitService service;
    @Autowired private PlatformTransactionManager transactionManager;

    @BeforeEach
    @AfterEach
    void clearRows() {
        jdbc.update("DELETE FROM auth_rate_limits");
    }

    @ParameterizedTest
    @EnumSource(Action.class)
    void capacityRequestIsAllowedAndNextRequestKeepsOriginalWindow(Action action) {
        var key = key(action);
        int capacity = switch (action) {
            case API_IP -> 300;
            case LOGIN_EMAIL -> 10;
            case LOGIN_IP -> 100;
            case EMAIL_SEND_SUBJECT -> 5;
            case EMAIL_SEND_IP -> 20;
            case EMAIL_RESEND -> 1;
        };
        for (int index = 0; index < capacity; index++) {
            service.reserve(List.of(key));
        }
        var before = row(key);

        assertThatThrownBy(() -> service.reserve(List.of(key)))
                .isInstanceOfSatisfying(AuthRateLimitException.class, failure -> {
                    assertThat(failure.code()).isEqualTo("RATE_LIMITED");
                    assertThat(failure.retryAfterSeconds()).isBetween(1L, action == Action.API_IP || action == Action.EMAIL_RESEND ? 60L : 900L);
                });

        assertThat(count(key)).isEqualTo(capacity);
        assertThat(row(key)).usingRecursiveComparison().isEqualTo(before);
    }

    @Test
    void duplicateKeysReserveOnlyOnceAndStoreOnlyHashedIdentifier() {
        var key = hasher.apiIp(IP);
        service.reserve(List.of(key, new AuthRateLimitKey(key.action(), key.keyHash())));
        assertThat(count(key)).isEqualTo(1);
        assertThat((byte[]) row(key).get("key_hash")).containsExactly(key.keyHash()).hasSize(32);
    }

    @Test
    void rejectedEmailStageKeepsAllThreeBudgetsAndPreviouslyCommittedApiBudget() {
        var apiKey = hasher.apiIp(IP);
        var emailKeys = hasher.emailIssueKeys("person@example.invalid", Purpose.SIGNUP, null, null, IP);
        service.reserve(List.of(apiKey));
        service.reserve(emailKeys);
        var before = emailKeys.stream().map(this::row).toList();
        service.reserve(List.of(apiKey));

        assertThatThrownBy(() -> service.reserve(emailKeys)).isInstanceOf(AuthRateLimitException.class);

        assertThat(emailKeys.stream().map(this::row).toList()).usingRecursiveComparison().isEqualTo(before);
        assertThat(count(apiKey)).isEqualTo(2);
    }

    @Test
    void reservationSurvivesLaterBusinessRollback() {
        var key = key(Action.EMAIL_SEND_SUBJECT);
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            service.reserve(List.of(key));
            status.setRollbackOnly();
        });
        assertThat(count(key)).isEqualTo(1);
    }

    @Test
    void writeFailureRollsBackWholeReservationStage() {
        var keys = hasher.emailIssueKeys("person@example.invalid", Purpose.SIGNUP, null, null, IP);
        // 정렬상 마지막 쓰기를 실패시켜 이미 실행된 앞선 쓰기도 되돌아가는지 검사한다.
        jdbc.execute("ALTER TABLE auth_rate_limits ADD CONSTRAINT synthetic_write_failure CHECK (action <> 'EMAIL_SEND_SUBJECT')");
        try {
            assertThatThrownBy(() -> service.reserve(keys)).isInstanceOf(AuthStateUnavailableException.class);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM auth_rate_limits", Long.class)).isZero();
        }
        finally {
            jdbc.execute("ALTER TABLE auth_rate_limits DROP CONSTRAINT synthetic_write_failure");
        }
    }

    @Test
    void changingPinSessionDoesNotResetAccountBudget() {
        var accountId = UUID.randomUUID();
        for (int index = 0; index < 5; index++) {
            service.reserve(hasher.emailIssueKeys(null, Purpose.PIN_SETUP, accountId, UUID.randomUUID(), IP));
        }
        var sixth = hasher.emailIssueKeys(null, Purpose.PIN_SETUP, accountId, UUID.randomUUID(), IP);

        assertThatThrownBy(() -> service.reserve(sixth)).isInstanceOf(AuthRateLimitException.class);

        var subject = sixth.stream().filter(key -> key.action() == Action.EMAIL_SEND_SUBJECT).findFirst().orElseThrow();
        assertThat(count(subject)).isEqualTo(5);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM auth_rate_limits WHERE action = 'EMAIL_RESEND'", Long.class))
                .isEqualTo(5);
    }

    @ParameterizedTest
    @CsvSource({"-100,2,-1,true,3", "-100,5,-1,false,5", "-100,2,30,false,2",
            "-1000,5,30,false,5", "-1000,5,-1,true,1"})
    void existingWindowAndBlockMustBothPermitReservation(long startedSeconds, long attempts, long blockSeconds,
                                                        boolean allowed, long expectedCount) {
        var key = key(Action.EMAIL_SEND_SUBJECT);
        var databaseNow = databaseNow();
        var startedAt = databaseNow.plusSeconds(startedSeconds);
        var blockedUntil = databaseNow.plusSeconds(blockSeconds);
        var windowEnd = startedAt.plusSeconds(900);
        var expiresAt = windowEnd.isAfter(blockedUntil) ? windowEnd : blockedUntil;
        seed(key, startedAt, attempts, blockedUntil, expiresAt);
        var before = row(key);

        if (allowed) {
            service.reserve(List.of(key));
        }
        else {
            assertThatThrownBy(() -> service.reserve(List.of(key))).isInstanceOf(AuthRateLimitException.class);
            assertThat(row(key)).usingRecursiveComparison().isEqualTo(before);
        }

        assertThat(count(key)).isEqualTo(expectedCount);
        if (allowed && startedSeconds == -100) {
            assertThat(row(key)).containsEntry("window_started_at", before.get("window_started_at"))
                    .containsEntry("expires_at", before.get("expires_at"))
                    .containsEntry("blocked_until", before.get("blocked_until"));
        }
        if (allowed && startedSeconds == -1000) {
            assertThat(row(key)).containsEntry("blocked_until", null);
            assertThat(timestamp(key, "window_started_at")).isAfterOrEqualTo(databaseNow);
            assertThat(timestamp(key, "expires_at")).isEqualTo(timestamp(key, "window_started_at").plusSeconds(900));
        }
    }

    @ParameterizedTest
    @CsvSource({"-1,0,true", "0,0,true", "1,1,false"})
    void windowEndEqualityStartsNewBudget(long nanos, long retrySeconds, boolean reset) {
        var state = new RateLimitState(NOW.minusSeconds(900).plusNanos(nanos), 5, null);
        assertThat(state.retryAfterSeconds(Action.EMAIL_SEND_SUBJECT, NOW)).isEqualTo(retrySeconds);
        assertThat(state.windowFinished(Action.EMAIL_SEND_SUBJECT, NOW)).isEqualTo(reset);
    }

    @ParameterizedTest
    @CsvSource({"-1,0,true", "0,0,true", "1,1,false"})
    void blockEndEqualityOnlyResetsWhenWindowAlsoEnded(long nanos, long retrySeconds, boolean reset) {
        var state = new RateLimitState(NOW.minusSeconds(1000), 5, NOW.plusNanos(nanos));
        assertThat(state.retryAfterSeconds(Action.EMAIL_SEND_SUBJECT, NOW)).isEqualTo(retrySeconds);
        assertThat(state.windowFinished(Action.EMAIL_SEND_SUBJECT, NOW)).isEqualTo(reset);
    }

    @ParameterizedTest
    @CsvSource({"1,1", "999999999,1", "1000000001,2", "61000000001,62"})
    void retryAfterRoundsUpAndUsesLaterOfBlockAndExhaustedWindow(long blockNanos, long retrySeconds) {
        var state = new RateLimitState(NOW.minusSeconds(900).plusNanos(1), 5, NOW.plusNanos(blockNanos));
        assertThat(state.retryAfterSeconds(Action.EMAIL_SEND_SUBJECT, NOW)).isEqualTo(retrySeconds);
    }

    @Test
    void rejectedBatchReportsLatestRetryAcrossAllKeys() {
        var shortWait = key(Action.EMAIL_RESEND);
        var longWait = key(Action.EMAIL_SEND_IP);
        var now = databaseNow();
        seed(shortWait, now.minusSeconds(30), 1, null, now.plusSeconds(30));
        seed(longWait, now.minusSeconds(5), 20, null, now.plusSeconds(895));

        assertThatThrownBy(() -> service.reserve(List.of(shortWait, longWait)))
                .isInstanceOfSatisfying(AuthRateLimitException.class,
                        failure -> assertThat(failure.retryAfterSeconds()).isBetween(894L, 895L));
    }

    @ParameterizedTest
    @ValueSource(strings = {"key_hash = decode('00','hex')", "attempt_count = -1",
            "expires_at = window_started_at", "blocked_until = expires_at + INTERVAL '1 second'",
            "window_started_at = '-infinity'::timestamptz", "expires_at = 'infinity'::timestamptz",
            "blocked_until = '-infinity'::timestamptz", "failure_times = ARRAY[clock_timestamp()]",
            "action = 'PIN_FAILURE_ACCOUNT'",
            "action = 'PIN_FAILURE_ACCOUNT', failure_times = ARRAY[NULL]::timestamptz[], attempt_count = 1",
            "action = 'PIN_FAILURE_ACCOUNT', failure_times = ARRAY[[clock_timestamp()]], attempt_count = 1"})
    void schemaRejectsInvalidBudgetOrFailureArray(String assignment) {
        var key = key(Action.API_IP);
        service.reserve(List.of(key));
        assertThatThrownBy(() -> jdbc.update("UPDATE auth_rate_limits SET " + assignment + " WHERE action = ? AND key_hash = ?",
                key.action().name(), key.keyHash())).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void databaseFailureDoesNotReturnUnlimitedBudgetOrExposeQuery() {
        jdbc.execute("ALTER TABLE auth_rate_limits RENAME TO unavailable_auth_rate_limits");
        try {
            assertThatThrownBy(() -> service.reserve(List.of(key(Action.API_IP))))
                    .isInstanceOfSatisfying(AuthStateUnavailableException.class,
                            failure -> assertThat(failure.getMessage()).doesNotContain("SELECT", "unavailable_", "192.0.2.10"));
        }
        finally {
            jdbc.execute("ALTER TABLE unavailable_auth_rate_limits RENAME TO auth_rate_limits");
        }
    }

    @Test
    @Timeout(30)
    void simultaneousFirstReservationsWithOppositeKeyOrderNeverExceedCapacity() throws Exception {
        var keys = List.of(key(Action.EMAIL_SEND_IP), key(Action.EMAIL_SEND_SUBJECT));
        var start = new CountDownLatch(1);
        var ready = new CountDownLatch(10);
        var results = new ArrayList<java.util.concurrent.Future<Boolean>>();
        try (var workers = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int index = 0; index < 10; index++) {
                var ordered = new ArrayList<>(keys);
                if (index % 2 == 0) {
                    Collections.reverse(ordered);
                }
                results.add(workers.submit(() -> {
                    ready.countDown();
                    await(start);
                    try {
                        service.reserve(ordered);
                        return true;
                    }
                    catch (AuthRateLimitException rejected) {
                        return false;
                    }
                }));
            }
            try {
                await(ready);
            }
            finally {
                start.countDown();
            }
            long accepted = 0;
            for (var result : results) {
                if (result.get(15, TimeUnit.SECONDS)) {
                    accepted++;
                }
            }
            assertThat(accepted).isEqualTo(5);
        }
        assertThat(count(keys.getFirst())).isEqualTo(5);
        assertThat(count(keys.getLast())).isEqualTo(5);
    }

    @Test
    @Timeout(30)
    void newWindowUsesDatabaseClockAfterWaitingForRowLock() throws Exception {
        var key = key(Action.API_IP);
        var before = databaseNow();
        seed(key, before.minusSeconds(120), 300, null, before.minusSeconds(60));
        var held = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var waiterPid = new CompletableFuture<Integer>();
        try (var workers = Executors.newVirtualThreadPerTaskExecutor()) {
            var holder = workers.submit(() -> new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                jdbc.queryForList("SELECT action FROM auth_rate_limits WHERE action = ? AND key_hash = ? FOR UPDATE",
                        String.class, key.action().name(), key.keyHash());
                held.countDown();
                await(release);
            }));
            try {
                await(held);
                // service의 별도 Tx가 아니라 같은 예약 경계 안에서 기다리는 DB pid를 관측한다.
                var waiter = workers.submit(() -> new TransactionTemplate(transactionManager).execute(status -> {
                    waiterPid.complete(jdbc.queryForObject("SELECT pg_backend_pid()", Integer.class));
                    return new AuthRateLimitRepository(jdbc).reserve(List.of(key));
                }));
                awaitDatabaseBlock(waiterPid.get(5, TimeUnit.SECONDS));
                var releasedAt = databaseNow();
                release.countDown();
                holder.get(10, TimeUnit.SECONDS);
                assertThat(waiter.get(10, TimeUnit.SECONDS)).isZero();
                assertThat(timestamp(key, "window_started_at")).isAfterOrEqualTo(releasedAt);
                assertThat(count(key)).isEqualTo(1);
            }
            finally {
                release.countDown();
            }
        }
    }

    private AuthRateLimitKey key(Action action) {
        return new AuthRateLimitKey(action, new byte[32]);
    }

    private void seed(AuthRateLimitKey key, OffsetDateTime startedAt, long attempts,
                      OffsetDateTime blockedUntil, OffsetDateTime expiresAt) {
        jdbc.update("""
                INSERT INTO auth_rate_limits (action, key_hash, window_started_at, attempt_count, blocked_until, expires_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """, key.action().name(), key.keyHash(), startedAt, attempts, blockedUntil, expiresAt);
    }

    private Map<String, Object> row(AuthRateLimitKey key) {
        return jdbc.queryForMap("SELECT * FROM auth_rate_limits WHERE action = ? AND key_hash = ?",
                key.action().name(), key.keyHash());
    }

    private long count(AuthRateLimitKey key) {
        return jdbc.queryForObject("SELECT attempt_count FROM auth_rate_limits WHERE action = ? AND key_hash = ?",
                Long.class, key.action().name(), key.keyHash());
    }

    private OffsetDateTime timestamp(AuthRateLimitKey key, String column) {
        return jdbc.queryForObject("SELECT " + column + " FROM auth_rate_limits WHERE action = ? AND key_hash = ?",
                OffsetDateTime.class, key.action().name(), key.keyHash());
    }

    private OffsetDateTime databaseNow() {
        return jdbc.queryForObject("SELECT clock_timestamp()", OffsetDateTime.class);
    }

    private void await(CountDownLatch latch) {
        try {
            assertThat(latch.await(10, TimeUnit.SECONDS)).as("worker reached expected state").isTrue();
        }
        catch (InterruptedException failure) {
            Thread.currentThread().interrupt();
            throw new AssertionError(failure);
        }
    }

    private void awaitDatabaseBlock(int pid) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < deadline) {
            if (Boolean.TRUE.equals(jdbc.queryForObject("SELECT cardinality(pg_blocking_pids(?)) > 0", Boolean.class, pid))) {
                return;
            }
        }
        throw new AssertionError("expected database lock wait was not observed");
    }
}
