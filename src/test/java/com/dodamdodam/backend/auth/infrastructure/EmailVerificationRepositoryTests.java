package com.dodamdodam.backend.auth.infrastructure;

import com.dodamdodam.backend.TestcontainersConfiguration;
import com.dodamdodam.backend.auth.dto.IssueEmailVerificationRequest.Purpose;
import com.dodamdodam.backend.auth.infrastructure.EmailVerificationRepository.IssuedChallenge;
import com.dodamdodam.backend.auth.infrastructure.EmailVerificationRepository.NewChallenge;
import com.dodamdodam.backend.auth.session.AuthStateUnavailableException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class EmailVerificationRepositoryTests {

    private static final String EMAIL = "challenge@example.invalid";
    private static final byte[] HASH = HexFormat.of().parseHex(
            "000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f");

    @Autowired private JdbcTemplate jdbc;
    @Autowired private EmailVerificationRepository repository;
    @Autowired private PlatformTransactionManager transactionManager;

    @BeforeEach
    @AfterEach
    void clearRows() {
        jdbc.update("DELETE FROM email_verifications");
        jdbc.update("DELETE FROM session_security");
        jdbc.update("DELETE FROM accounts");
    }

    @Test
    void documentedEmailVerificationColumnsMatchFlyway() throws IOException {
        var erd = new ClassPathResource("Integrated_ERD_Design.md").getContentAsString(StandardCharsets.UTF_8);
        var ddl = Pattern.compile("<!-- integrated-ddl:start -->\\R```sql\\R(.*?)\\R```\\R<!-- integrated-ddl:end -->",
                Pattern.DOTALL).matcher(erd);
        assertThat(ddl.find()).as("ERD §12.2 DDL").isTrue();
        String columns = """
                SELECT column_name, data_type, character_maximum_length, is_nullable, column_default
                FROM information_schema.columns
                WHERE table_schema=current_schema() AND table_name='email_verifications' ORDER BY column_name
                """;
        var migrated = jdbc.queryForList(columns);
        assertThat(migrated).isNotEmpty();
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            // 문서 DDL을 빈 스키마에 적용하고 트랜잭션 종료 시 스키마까지 되돌린다.
            status.setRollbackOnly();
            jdbc.execute("CREATE SCHEMA erd_verification_test");
            jdbc.execute("SET LOCAL search_path TO erd_verification_test");
            jdbc.execute(ddl.group(1));
            assertThat(jdbc.queryForList(columns)).containsExactlyElementsOf(migrated);
        });
    }

    @ParameterizedTest
    @CsvSource({"SIGNUP,true", "SIGNUP,false", "RESET_PASSWORD,true", "RESET_PASSWORD,false"})
    void storesPublicEligibilityHashAndDatabaseTtl(Purpose purpose, boolean eligible) {
        var accountId = purpose == Purpose.RESET_PASSWORD && eligible ? account() : null;
        var input = publicChallenge(EMAIL, purpose, eligible, accountId, accountId == null ? null : 7L);
        var before = databaseNow();

        var issued = save(input);

        var after = databaseNow();
        var stored = row(issued.challengeId());
        assertThat(issued.challengeId()).isEqualTo(input.id());
        assertThat(stored).containsEntry("email", EMAIL).containsEntry("purpose", purpose.name())
                .containsEntry("eligible", eligible).containsEntry("attempt_count", 0)
                .containsEntry("verified_at", null).containsEntry("token_hash", null)
                .containsEntry("invalidated_at", null).containsEntry("setup_account_id", null)
                .containsEntry("reset_account_id", accountId)
                .containsEntry("reset_session_version", accountId == null ? null : 7L);
        assertThat((byte[]) stored.get("code_hash")).containsExactly(HASH);
        var createdAt = timestamp(issued.challengeId(), "created_at");
        assertThat(createdAt.toInstant()).isBetween(before.toInstant(), after.toInstant());
        assertThat(issued.expiresAt()).isEqualTo(createdAt.plusSeconds(600));
        assertThat(timestamp(issued.challengeId(), "expires_at")).isEqualTo(issued.expiresAt());
    }

    @ParameterizedTest
    @ValueSource(strings = {"PIN_SETUP", "PIN_RESET"})
    void storesPinAccountContextGenerationAndVersionSnapshot(Purpose purpose) {
        var accountId = account();
        var contextId = context(accountId);
        var pinVersion = purpose == Purpose.PIN_RESET ? 17L : null;

        var issued = save(pinChallenge(EMAIL, purpose, accountId, contextId, 26, pinVersion));

        assertThat(row(issued.challengeId())).containsEntry("eligible", true)
                .containsEntry("setup_account_id", accountId).containsEntry("setup_session_binding", contextId)
                .containsEntry("setup_generation", 26L).containsEntry("pin_version_snapshot", pinVersion)
                .containsEntry("reset_account_id", null).containsEntry("reset_session_version", null);
    }

    @Test
    void reissueInvalidatesUnverifiedCodeAndDecoyInSameScope() {
        var first = save(publicChallenge(EMAIL, Purpose.SIGNUP, false, null, null));

        var second = save(signup(EMAIL));

        assertThat(timestamp(first.challengeId(), "invalidated_at"))
                .isEqualTo(timestamp(second.challengeId(), "created_at"));
        assertThat(row(second.challengeId())).containsEntry("invalidated_at", null);
        assertThat(activeCount()).isEqualTo(1);
    }

    @Test
    void reissueInvalidatesVerifiedTokenEvenAfterItsCodeExpired() {
        var first = save(signup(EMAIL));
        jdbc.update("""
                UPDATE email_verifications SET created_at = clock_timestamp() - INTERVAL '20 minutes',
                    expires_at = clock_timestamp() - INTERVAL '10 minutes',
                    verified_at = clock_timestamp() - INTERVAL '2 minutes', token_hash = ?,
                    token_expires_at = clock_timestamp() + INTERVAL '8 minutes' WHERE id = ?
                """, HASH, first.challengeId());

        var second = save(signup(EMAIL));

        assertThat(timestamp(first.challengeId(), "invalidated_at"))
                .isEqualTo(timestamp(second.challengeId(), "created_at"));
        assertThat(row(first.challengeId())).containsEntry("token_consumed_at", null);
        assertThat(activeCount()).isEqualTo(1);
    }

    @Test
    void replacementPreservesConsumedAndAlreadyInvalidatedRows() {
        var consumed = save(signup(EMAIL));
        consume(consumed.challengeId());
        var consumedBefore = row(consumed.challengeId());
        var invalidated = save(signup(EMAIL));
        invalidate(invalidated.challengeId());
        var invalidatedBefore = row(invalidated.challengeId());

        save(signup(EMAIL));

        assertThat(row(consumed.challengeId())).usingRecursiveComparison().isEqualTo(consumedBefore);
        assertThat(row(invalidated.challengeId())).usingRecursiveComparison().isEqualTo(invalidatedBefore);
    }

    @Test
    void publicScopeSeparatesEmailAndPurpose() {
        var first = save(signup(EMAIL));
        var otherEmail = save(signup("other@example.invalid"));
        var otherPurpose = save(publicChallenge(EMAIL, Purpose.RESET_PASSWORD, false, null, null));

        save(signup(EMAIL));

        assertThat(timestamp(first.challengeId(), "invalidated_at")).isNotNull();
        assertThat(row(otherEmail.challengeId())).containsEntry("invalidated_at", null);
        assertThat(row(otherPurpose.challengeId())).containsEntry("invalidated_at", null);
        assertThat(activeCount()).isEqualTo(3);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ACCOUNT", "CONTEXT", "PURPOSE"})
    void pinScopeSeparatesAccountContextAndPurpose(String difference) {
        var accountId = account();
        var contextId = context(accountId);
        var first = save(pinChallenge(EMAIL, Purpose.PIN_SETUP, accountId, contextId, 0, null));
        var otherAccount = difference.equals("ACCOUNT") ? account() : accountId;
        var otherContext = difference.equals("CONTEXT") || difference.equals("ACCOUNT")
                ? context(otherAccount) : contextId;
        var otherPurpose = difference.equals("PURPOSE") ? Purpose.PIN_RESET : Purpose.PIN_SETUP;

        var second = save(pinChallenge(EMAIL, otherPurpose, otherAccount, otherContext, 0,
                otherPurpose == Purpose.PIN_RESET ? 1L : null));

        assertThat(row(first.challengeId())).containsEntry("invalidated_at", null);
        assertThat(row(second.challengeId())).containsEntry("invalidated_at", null);
    }

    @Test
    void samePinScopeReplacesOldEmailAndGeneration() {
        var accountId = account();
        var contextId = context(accountId);
        var first = save(pinChallenge(EMAIL, Purpose.PIN_SETUP, accountId, contextId, 2, null));

        var second = save(pinChallenge("changed@example.invalid", Purpose.PIN_SETUP,
                accountId, contextId, 3, null));

        assertThat(timestamp(first.challengeId(), "invalidated_at"))
                .isEqualTo(timestamp(second.challengeId(), "created_at"));
        assertThat(activeCount()).isEqualTo(1);
    }

    @Test
    void failedInsertRollsBackPreviousInvalidation() {
        var input = signup(EMAIL);
        var first = save(input);

        assertThatThrownBy(() -> save(input)).isInstanceOf(AuthStateUnavailableException.class);

        assertThat(row(first.challengeId())).containsEntry("invalidated_at", null);
        assertThat(activeCount()).isEqualTo(1);
    }

    @Test
    void callerRollbackRestoresPreviousChallengeAndRemovesNewOne() {
        var first = save(signup(EMAIL));
        var second = signup(EMAIL);
        transaction().executeWithoutResult(status -> {
            repository.replace(second);
            status.setRollbackOnly();
        });

        assertThat(row(first.challengeId())).containsEntry("invalidated_at", null);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM email_verifications WHERE id = ?",
                Long.class, second.id())).isZero();
    }

    @Test
    void invalidationIsIdempotentAndDoesNotTouchLaterIssueOrConsumedProof() {
        var first = save(signup(EMAIL));
        invalidate(first.challengeId());
        var invalidatedAt = timestamp(first.challengeId(), "invalidated_at");
        var later = save(signup(EMAIL));

        invalidate(first.challengeId());
        invalidate(UUID.randomUUID());

        assertThat(timestamp(first.challengeId(), "invalidated_at")).isEqualTo(invalidatedAt);
        assertThat(row(later.challengeId())).containsEntry("invalidated_at", null);
        consume(later.challengeId());
        invalidate(later.challengeId());
        assertThat(row(later.challengeId())).containsEntry("invalidated_at", null);
        assertThat(timestamp(later.challengeId(), "token_consumed_at")).isNotNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"ISSUE", "INVALIDATE"})
    void writesRequireCallerTransaction(String operation) {
        assertThatThrownBy(() -> {
            if (operation.equals("ISSUE")) {
                repository.replace(signup(EMAIL));
            }
            else {
                repository.invalidateUnconsumed(UUID.randomUUID());
            }
        }).isInstanceOf(IllegalTransactionStateException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ISSUE", "INVALIDATE"})
    void databaseFailureIsExplicitAndDoesNotCopyQueryOrInput(String operation) {
        jdbc.execute("ALTER TABLE email_verifications RENAME TO unavailable_email_verifications");
        try {
            assertThatThrownBy(() -> {
                if (operation.equals("ISSUE")) {
                    save(signup(EMAIL));
                }
                else {
                    invalidate(UUID.randomUUID());
                }
            }).isInstanceOfSatisfying(AuthStateUnavailableException.class, failure -> {
                assertThat(failure.getCause()).isInstanceOf(DataAccessException.class);
                assertThat(failure.getMessage()).doesNotContain(EMAIL, "SELECT", "UPDATE", "unavailable_");
            });
        }
        finally {
            jdbc.execute("ALTER TABLE unavailable_email_verifications RENAME TO email_verifications");
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"SIGNUP", "PIN_SETUP"})
    @Timeout(30)
    void simultaneousFirstIssuesSerializeBeforeAnyRowExists(Purpose purpose) throws Exception {
        var accountId = purpose == Purpose.PIN_SETUP ? account() : null;
        var contextId = accountId == null ? null : context(accountId);
        var first = purpose == Purpose.SIGNUP ? signup(EMAIL)
                : pinChallenge(EMAIL, purpose, accountId, contextId, 0, null);
        var second = purpose == Purpose.SIGNUP ? signup(EMAIL)
                : pinChallenge(EMAIL, purpose, accountId, contextId, 0, null);
        var held = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var waiterPid = new CompletableFuture<Integer>();
        try (var workers = Executors.newVirtualThreadPerTaskExecutor()) {
            var holder = workers.submit(() -> transaction().execute(status -> {
                var issued = repository.replace(first);
                held.countDown();
                await(release);
                return issued;
            }));
            try {
                await(held);
                var waiter = workers.submit(() -> transaction().execute(status -> {
                    waiterPid.complete(jdbc.queryForObject("SELECT pg_backend_pid()", Integer.class));
                    return repository.replace(second);
                }));
                awaitDatabaseBlock(waiterPid.get(5, TimeUnit.SECONDS));
                var releasedAt = databaseNow();
                release.countDown();

                holder.get(10, TimeUnit.SECONDS);
                var issued = waiter.get(10, TimeUnit.SECONDS);

                assertThat(timestamp(first.id(), "invalidated_at"))
                        .isEqualTo(timestamp(issued.challengeId(), "created_at"));
                assertThat(timestamp(issued.challengeId(), "created_at")).isAfterOrEqualTo(releasedAt);
                assertThat(activeCount()).isEqualTo(1);
            }
            finally {
                release.countDown();
            }
        }
    }

    @Test
    @Timeout(30)
    void databaseClockIsReadAfterWaitingForExistingChallengeLock() throws Exception {
        var first = save(signup(EMAIL));
        var held = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var waiterPid = new CompletableFuture<Integer>();
        try (var workers = Executors.newVirtualThreadPerTaskExecutor()) {
            var holder = workers.submit(() -> transaction().executeWithoutResult(status -> {
                jdbc.queryForObject("SELECT id FROM email_verifications WHERE id = ? FOR UPDATE",
                        UUID.class, first.challengeId());
                held.countDown();
                await(release);
            }));
            try {
                await(held);
                var waiter = workers.submit(() -> transaction().execute(status -> {
                    waiterPid.complete(jdbc.queryForObject("SELECT pg_backend_pid()", Integer.class));
                    return repository.replace(signup(EMAIL));
                }));
                awaitDatabaseBlock(waiterPid.get(5, TimeUnit.SECONDS));
                var releasedAt = databaseNow();
                release.countDown();

                holder.get(10, TimeUnit.SECONDS);
                var issued = waiter.get(10, TimeUnit.SECONDS);

                assertThat(timestamp(issued.challengeId(), "created_at")).isAfterOrEqualTo(releasedAt);
                assertThat(timestamp(first.challengeId(), "invalidated_at"))
                        .isEqualTo(timestamp(issued.challengeId(), "created_at"));
            }
            finally {
                release.countDown();
            }
        }
    }

    private NewChallenge signup(String email) {
        return publicChallenge(email, Purpose.SIGNUP, true, null, null);
    }

    private NewChallenge publicChallenge(String email, Purpose purpose, boolean eligible,
                                         UUID accountId, Long version) {
        return new NewChallenge(UUID.randomUUID(), email, purpose, eligible, HASH,
                null, null, null, null, accountId, version);
    }

    private NewChallenge pinChallenge(String email, Purpose purpose, UUID accountId, UUID contextId,
                                      long generation, Long pinVersion) {
        return new NewChallenge(UUID.randomUUID(), email, purpose, true, HASH,
                accountId, contextId, generation, pinVersion, null, null);
    }

    private UUID account() {
        var id = UUID.randomUUID();
        jdbc.update("INSERT INTO accounts (id, email) VALUES (?, ?)", id, id + "@example.invalid");
        return id;
    }

    private UUID context(UUID accountId) {
        var id = UUID.randomUUID();
        jdbc.update("INSERT INTO session_security (id, account_id, session_version) VALUES (?, ?, 0)", id, accountId);
        return id;
    }

    private TransactionTemplate transaction() {
        var transaction = new TransactionTemplate(transactionManager);
        transaction.setTimeout(15);
        return transaction;
    }

    private IssuedChallenge save(NewChallenge challenge) {
        return transaction().execute(status -> repository.replace(challenge));
    }

    private void invalidate(UUID id) {
        transaction().executeWithoutResult(status -> repository.invalidateUnconsumed(id));
    }

    private void consume(UUID id) {
        jdbc.update("""
                UPDATE email_verifications SET verified_at = clock_timestamp(), token_hash = ?,
                    token_expires_at = clock_timestamp() + INTERVAL '10 minutes',
                    token_consumed_at = clock_timestamp() WHERE id = ?
                """, HASH, id);
    }

    private Map<String, Object> row(UUID id) {
        return jdbc.queryForMap("SELECT * FROM email_verifications WHERE id = ?", id);
    }

    private OffsetDateTime timestamp(UUID id, String column) {
        // 컬럼 이름은 테스트 안의 고정 문자열만 전달한다.
        return jdbc.queryForObject("SELECT " + column + " FROM email_verifications WHERE id = ?",
                OffsetDateTime.class, id);
    }

    private OffsetDateTime databaseNow() {
        return jdbc.queryForObject("SELECT clock_timestamp()", OffsetDateTime.class);
    }

    private long activeCount() {
        return jdbc.queryForObject("""
                SELECT count(*) FROM email_verifications
                WHERE invalidated_at IS NULL AND token_consumed_at IS NULL
                """, Long.class);
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
        // 임의 대기시간 대신 PostgreSQL이 실제로 잠금 대기 중인지 확인한다.
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < deadline) {
            if (Boolean.TRUE.equals(jdbc.queryForObject(
                    "SELECT cardinality(pg_blocking_pids(?)) > 0", Boolean.class, pid))) {
                return;
            }
        }
        throw new AssertionError("expected database lock wait was not observed");
    }
}
