package com.dodamdodam.backend.auth.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dodamdodam.backend.TestcontainersConfiguration;
import com.dodamdodam.backend.auth.exception.PasswordLoginException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = "spring.session.jdbc.cleanup-cron=-")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PasswordLoginContextTests {
    private static final String HASH = "synthetic-verified-password-hash";
    @Autowired private JdbcTemplate jdbc;
    @Autowired private SessionSecurityRepository repository;
    @Autowired private PlatformTransactionManager transactions;

    @BeforeEach
    @AfterEach
    void cleanRows() {
        jdbc.update("DELETE FROM email_verifications");
        jdbc.update("DELETE FROM session_security");
        jdbc.update("DELETE FROM accounts");
    }

    @ParameterizedTest
    @ValueSource(strings = {"HASH", "VERSION", "DELETED", "SOCIAL"})
    void credentialsChangedAfterComparisonCannotRevokePreviousLoginOrCreateNewContext(String change) {
        var old = repository.createLoginContext(account(), null);
        var next = account();
        switch (change) {
            case "HASH" -> jdbc.update("UPDATE accounts SET password_hash='synthetic-new-hash' WHERE id=?", next);
            case "VERSION" -> jdbc.update("UPDATE accounts SET session_version=1 WHERE id=?", next);
            case "DELETED" -> jdbc.update("DELETE FROM accounts WHERE id=?", next);
            case "SOCIAL" -> jdbc.update("UPDATE accounts SET password_hash=NULL WHERE id=?", next);
            default -> throw new AssertionError(change);
        }
        assertThatThrownBy(() -> repository.createPasswordLoginContext(next, HASH, 0, old))
                .isInstanceOf(PasswordLoginException.class);
        assertThat(repository.isCurrentContext(old.accountId(), old.securityContextId())).isTrue();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM session_security", Long.class)).isEqualTo(1);
    }

    @Test
    void replacementRevokesOnlyPreviousContextAndPinProofsWithoutCopyingGuardian() {
        var previous = repository.createLoginContext(account(), null);
        var untouched = repository.createLoginContext(previous.accountId(), null);
        jdbc.update("UPDATE session_security SET guardian_unlocked_until=clock_timestamp()+INTERVAL '30 minutes', guardian_pin_version=2, setup_generation=4 WHERE id=?",
                previous.securityContextId());
        for (var purpose : List.of("PIN_SETUP", "PIN_RESET")) {
            jdbc.update("""
                    INSERT INTO email_verifications(id,email,purpose,eligible,code_hash,expires_at,
                        setup_account_id,setup_session_binding,setup_generation,pin_version_snapshot)
                    VALUES(?,?,?,true,?,clock_timestamp()+INTERVAL '10 minutes',?,?,4,?)
                    """, UUID.randomUUID(), previous.accountId() + "@example.invalid", purpose, new byte[32],
                    previous.accountId(), previous.securityContextId(), purpose.equals("PIN_RESET") ? 2L : null);
        }
        var target = account();
        var created = repository.createPasswordLoginContext(target, HASH, 0, previous);

        assertThat(repository.isCurrentContext(previous.accountId(), previous.securityContextId())).isFalse();
        assertThat(repository.isCurrentContext(untouched.accountId(), untouched.securityContextId())).isTrue();
        assertThat(repository.isCurrentContext(created.accountId(), created.securityContextId())).isTrue();
        assertThat(created.accountId()).isEqualTo(target);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM email_verifications WHERE invalidated_at IS NOT NULL", Long.class)).isEqualTo(2);
        assertThat(jdbc.queryForMap("SELECT guardian_unlocked_until,guardian_pin_version,setup_generation,expires_at FROM session_security WHERE id=?",
                created.securityContextId())).containsEntry("guardian_unlocked_until", null)
                .containsEntry("guardian_pin_version", null).containsEntry("setup_generation", 0L).containsEntry("expires_at", null);
    }

    @Test
    void resetCommittedWhileLoginWaitsForAccountLockRejectsStaleHashAndVersion() throws Exception {
        var account = account();
        var held = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var waiterPid = new CompletableFuture<Integer>();
        try (var workers = Executors.newVirtualThreadPerTaskExecutor()) {
            var reset = workers.submit(() -> new TransactionTemplate(transactions).executeWithoutResult(status -> {
                jdbc.queryForList("SELECT id FROM accounts WHERE id=? FOR UPDATE", UUID.class, account);
                jdbc.update("UPDATE accounts SET password_hash='synthetic-reset-hash',session_version=1 WHERE id=?", account);
                held.countDown();
                await(release);
            }));
            try {
                await(held);
                var login = workers.submit(() -> new TransactionTemplate(transactions).execute(status -> {
                    waiterPid.complete(jdbc.queryForObject("SELECT pg_backend_pid()", Integer.class));
                    return new SessionSecurityRepository(jdbc).createPasswordLoginContext(account, HASH, 0, null);
                }));
                awaitDatabaseBlock(waiterPid.get(5, TimeUnit.SECONDS));
                release.countDown();
                reset.get(10, TimeUnit.SECONDS);
                assertThatThrownBy(() -> login.get(10, TimeUnit.SECONDS))
                        .hasCauseInstanceOf(PasswordLoginException.class);
                assertThat(jdbc.queryForObject("SELECT count(*) FROM session_security", Long.class)).isZero();
            }
            finally {
                release.countDown();
            }
        }
    }

    @Test
    void oppositeAccountSwitchesFollowOneLockOrderAndBothComplete() throws Exception {
        var first = repository.createLoginContext(account(), null);
        var second = repository.createLoginContext(account(), null);
        var start = new CountDownLatch(1);
        try (var workers = Executors.newVirtualThreadPerTaskExecutor()) {
            var toFirst = workers.submit(() -> {
                await(start);
                return repository.createPasswordLoginContext(first.accountId(), HASH, 0, second);
            });
            var toSecond = workers.submit(() -> {
                await(start);
                return repository.createPasswordLoginContext(second.accountId(), HASH, 0, first);
            });
            start.countDown();
            var currentFirst = toFirst.get(10, TimeUnit.SECONDS);
            var currentSecond = toSecond.get(10, TimeUnit.SECONDS);
            assertThat(repository.isCurrentContext(currentFirst.accountId(), currentFirst.securityContextId())).isTrue();
            assertThat(repository.isCurrentContext(currentSecond.accountId(), currentSecond.securityContextId())).isTrue();
        }
        assertThat(repository.isCurrentContext(first.accountId(), first.securityContextId())).isFalse();
        assertThat(repository.isCurrentContext(second.accountId(), second.securityContextId())).isFalse();
    }

    /** 실제 원문 해시 비교는 서비스에서 수행하며 여기서는 비교 이후 스냅샷 경합을 재현한다. */
    private UUID account() {
        var id = UUID.randomUUID();
        jdbc.update("INSERT INTO accounts(id,email,password_hash) VALUES(?,?,?)", id, id + "@example.invalid", HASH);
        return id;
    }

    /** 스레드 실행 순서를 제한하고 실패 때 영구 대기를 막는다. */
    private void await(CountDownLatch latch) {
        try {
            assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
        }
        catch (InterruptedException failure) {
            Thread.currentThread().interrupt();
            throw new AssertionError(failure);
        }
    }

    /** 계정 잠금 대기를 DB에서 확인한 다음 reset을 커밋한다. */
    private void awaitDatabaseBlock(int pid) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < deadline) {
            if (Boolean.TRUE.equals(jdbc.queryForObject("SELECT cardinality(pg_blocking_pids(?))>0", Boolean.class, pid))) {
                return;
            }
        }
        throw new AssertionError("expected account lock wait was not observed");
    }
}
