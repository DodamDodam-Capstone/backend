package com.dodamdodam.backend.conversation.application;

import com.dodamdodam.backend.TestcontainersConfiguration;
import com.dodamdodam.backend.auth.session.AuthenticatedSession;
import com.dodamdodam.backend.auth.session.SessionSecurityRepository;
import com.dodamdodam.backend.child.exception.ChildException;
import com.dodamdodam.backend.conversation.domain.ConversationServiceHours;
import com.dodamdodam.backend.conversation.dto.StartConversationRequest;
import com.dodamdodam.backend.conversation.exception.ConversationException;
import com.dodamdodam.backend.conversation.infrastructure.ConversationRepository;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = {"spring.session.jdbc.cleanup-cron=-"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Timeout(25)
class ConversationCreationTests {

    private static final OffsetDateTime STARTED_AT = OffsetDateTime.parse("2026-10-03T08:00:00.123456+09:00");
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ConversationService service;
    @Autowired private SessionSecurityRepository security;
    @Autowired private PlatformTransactionManager transactions;
    @MockitoSpyBean private ConversationRepository repository;

    @BeforeEach
    void clear() {
        jdbc.update("DELETE FROM conversation_session_links");
        jdbc.update("DELETE FROM conversation_turns");
        jdbc.update("DELETE FROM conversations");
        jdbc.update("DELETE FROM children");
        jdbc.update("DELETE FROM SPRING_SESSION");
        jdbc.update("DELETE FROM email_verifications");
        jdbc.update("DELETE FROM session_security");
        jdbc.update("DELETE FROM accounts");
        // 시간 경계만 고정하며 잠금·조회·대화와 연결 INSERT는 모두 실제 PostgreSQL에서 실행한다.
        doReturn(STARTED_AT).when(repository).currentTime();
    }

    @Test
    void storesNewActiveConversationAndCurrentSessionLinkAtomically() {
        var fixture = fixture();
        var request = request();

        var outcome = service.create(fixture.session(), fixture.childId(), request);
        assertThat(outcome.created()).isTrue();
        var result = outcome.result();

        assertThat(result.status()).isEqualTo("ACTIVE");
        assertThat(result.startedAt().toInstant()).isEqualTo(STARTED_AT.withNano(0).toInstant());
        var row = jdbc.queryForMap("SELECT * FROM conversations WHERE id = ?", result.conversationId());
        assertThat(row).containsEntry("child_id", fixture.childId()).containsEntry("client_request_id", request.clientRequestId())
                .containsEntry("status", "ACTIVE").containsEntry("summary_status", "NOT_STARTED");
        for (var field : List.of("end_requested_at", "end_reason", "ended_at", "title", "topic", "summary",
                "summary_started_at", "summary_deadline_at", "summary_completed_at", "summary_error_code")) {
            assertThat(row.get(field)).as(field).isNull();
        }
        var link = jdbc.queryForMap("SELECT * FROM conversation_session_links WHERE conversation_id = ?", result.conversationId());
        assertThat(link).containsEntry("session_security_id", fixture.session().securityContextId())
                .containsEntry("end_recovery_until", null);
        var storedStart = jdbc.queryForObject("SELECT started_at FROM conversations WHERE id = ?",
                (rowSet, _) -> rowSet.getObject(1, OffsetDateTime.class), result.conversationId());
        var bound = jdbc.queryForObject("SELECT bound_at FROM conversation_session_links WHERE conversation_id = ?",
                (rowSet, _) -> rowSet.getObject(1, OffsetDateTime.class), result.conversationId());
        assertThat(storedStart.toInstant()).isEqualTo(STARTED_AT.toInstant()).isEqualTo(bound.toInstant());
        assertCounts(1, 1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM conversation_turns", Integer.class)).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"2024-02-29T08:00:00+09:00", "2026-10-31T23:59:59.999999+09:00", "2026-12-31T23:59:59+09:00"})
    void persistsKstDateAndFollowingMidnightAtCalendarBoundaries(String value) {
        var fixture = fixture();
        var now = OffsetDateTime.parse(value);
        doReturn(now).when(repository).currentTime();
        var result = service.create(fixture.session(), fixture.childId(), request()).result();
        assertThat(result.serviceDate()).isEqualTo(now.toLocalDate());
        assertThat(result.scheduledEndAt()).isEqualTo(now.toLocalDate().plusDays(1).atStartOfDay().atOffset(now.getOffset()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"2026-10-03T00:00:00+09:00", "2026-10-03T07:59:59.999999+09:00"})
    void rejectsClosedHoursWithoutSavingAnything(String value) {
        var fixture = fixture();
        doReturn(OffsetDateTime.parse(value)).when(repository).currentTime();
        var failure = assertThrows(ConversationException.class, () -> service.create(fixture.session(), fixture.childId(), request()));
        assertThat(failure.code()).isEqualTo("SERVICE_HOURS_CLOSED");
        assertCounts(0, 0);
    }

    @Test
    void rollsBackConversationWhenLinkStorageFails() {
        var fixture = fixture();
        jdbc.execute("ALTER TABLE conversation_session_links RENAME TO unavailable_conversation_links");
        try {
            var failure = assertThrows(ConversationException.class, () -> service.create(fixture.session(), fixture.childId(), request()));
            assertThat(failure.code()).isEqualTo("INTERNAL_ERROR");
            assertThat(failure.getCause()).isInstanceOf(org.springframework.dao.DataAccessException.class);
        }
        finally {
            jdbc.execute("ALTER TABLE unavailable_conversation_links RENAME TO conversation_session_links");
        }
        assertCounts(0, 0);
    }

    @Test
    void rollsBackBothRowsIfStorageCrossesMidnight() {
        var fixture = fixture();
        doReturn(OffsetDateTime.parse("2026-10-03T23:59:59.999999+09:00"),
                OffsetDateTime.parse("2026-10-03T23:59:59.999999+09:00"),
                OffsetDateTime.parse("2026-10-04T00:00:00+09:00")).when(repository).currentTime();
        var failure = assertThrows(ConversationException.class, () -> service.create(fixture.session(), fixture.childId(), request()));
        assertThat(failure.code()).isEqualTo("SERVICE_HOURS_CLOSED");
        assertCounts(0, 0);
    }

    @ParameterizedTest
    @ValueSource(strings = {"MISSING", "OTHER_OWNER"})
    void lockedAccessUsesTheSameNotFoundPolicy(String kind) {
        var fixture = fixture();
        var childId = kind.equals("MISSING") ? UUID.randomUUID() : fixture().childId();
        var failure = assertThrows(ChildException.class, () -> service.create(fixture.session(), childId, request()));
        assertThat(failure.code()).isEqualTo("RESOURCE_NOT_FOUND");
        assertCounts(0, 0);
    }

    @Test
    void revokedSessionCannotCreateConversation() {
        var fixture = fixture();
        security.revokeCurrentContext(fixture.session().accountId(), fixture.session().securityContextId());
        assertThatThrownBy(() -> service.create(fixture.session(), fixture.childId(), request()))
                .isInstanceOf(InsufficientAuthenticationException.class);
        assertCounts(0, 0);
    }

    @Test
    void concurrentStartsForSameChildCommitOnlyOneConversationAndLink() throws Exception {
        var fixture = fixture();
        var secondSession = security.createLoginContext(fixture.session().accountId(), null);
        var start = new CountDownLatch(1);
        try (var workers = Executors.newFixedThreadPool(2)) {
            var first = workers.submit(() -> { start.await(); return attempt(fixture.session(), fixture.childId(), request()); });
            var second = workers.submit(() -> { start.await(); return attempt(secondSession, fixture.childId(), request()); });
            start.countDown();
            assertThat(List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder("CREATED", "ACTIVE_CONVERSATION_EXISTS");
        }
        assertCounts(1, 1);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void concurrentSameKeyCreatesOnceAndOnlyReplaysForTheLinkedContext(boolean sameContext) throws Exception {
        var fixture = fixture();
        var secondSession = sameContext ? fixture.session() : security.createLoginContext(fixture.session().accountId(), null);
        var key = request();
        var start = new CountDownLatch(1);
        try (var workers = Executors.newFixedThreadPool(2)) {
            var first = workers.submit(() -> { start.await(); return attempt(fixture.session(), fixture.childId(), key); });
            var second = workers.submit(() -> { start.await(); return attempt(secondSession, fixture.childId(), key); });
            start.countDown();
            assertThat(List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder("CREATED", sameContext ? "REPLAY" : "CHILD_SESSION_REQUIRED");
        }
        assertCounts(1, 1);
    }

    @ParameterizedTest
    @CsvSource({"2026-10-03T07:59:59+09:00,2026-10-03T08:00:00+09:00,CREATED",
            "2026-10-03T23:59:59+09:00,2026-10-04T00:00:00+09:00,SERVICE_HOURS_CLOSED"})
    void waitsForChildBeforeReadingTimeAndUsesTimeAfterWait(String before, String after, String expected) throws Exception {
        var fixture = fixture();
        var clock = new AtomicReference<>(OffsetDateTime.parse(before));
        doAnswer(_ -> clock.get()).when(repository).currentTime();
        var entered = new CompletableFuture<Integer>();
        try (var worker = Executors.newSingleThreadExecutor()) {
            var pending = transaction().execute(tx -> {
                int blocker = backendPid();
                jdbc.queryForList("SELECT id FROM children WHERE id = ? FOR UPDATE", UUID.class, fixture.childId());
                Future<String> attempt = worker.submit(() -> attemptInTransaction(fixture, entered));
                awaitBlocked(entered.orTimeout(5, TimeUnit.SECONDS).join(), blocker);
                verify(repository, never()).currentTime();
                clock.set(OffsetDateTime.parse(after));
                return attempt;
            });
            assertThat(pending.get(10, TimeUnit.SECONDS)).isEqualTo(expected);
        }
        assertCounts(expected.equals("CREATED") ? 1 : 0, expected.equals("CREATED") ? 1 : 0);
    }

    @Test
    void reloadsConversationStateCommittedWhileWaitingForChildLock() throws Exception {
        var fixture = fixture();
        var priorSession = security.createLoginContext(fixture.session().accountId(), null);
        var priorId = UUID.randomUUID();
        var key = request();
        var entered = new CompletableFuture<Integer>();
        try (var worker = Executors.newSingleThreadExecutor()) {
            var pending = transaction().execute(_ -> {
                int blocker = backendPid();
                jdbc.queryForList("SELECT id FROM children WHERE id = ? FOR UPDATE", UUID.class, fixture.childId());
                var attempt = worker.submit(() -> assertThrows(ConversationException.class, () -> transaction().execute(_ -> {
                    jdbc.execute("SET LOCAL statement_timeout = '15s'");
                    entered.complete(backendPid());
                    return service.create(fixture.session(), fixture.childId(), key);
                })));
                awaitBlocked(entered.orTimeout(5, TimeUnit.SECONDS).join(), blocker);
                // 대기 중 독립 Tx가 유효한 대화·연결을 실제 저장소로 함께 커밋한다.
                repository.insertActive(priorId, fixture.childId(), UUID.randomUUID(), STARTED_AT,
                        ConversationServiceHours.requireOpen(STARTED_AT)).orElseThrow();
                repository.insertLink(priorSession.securityContextId(), priorId, STARTED_AT);
                return attempt;
            });
            var conflict = pending.get(10, TimeUnit.SECONDS);
            assertThat(conflict.code()).isEqualTo("ACTIVE_CONVERSATION_EXISTS");
            assertThat(conflict.activeConversationId()).isEqualTo(priorId);
        }
        assertCounts(1, 1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM conversations WHERE client_request_id = ?",
                Integer.class, key.clientRequestId())).isZero();
    }

    @Test
    void blockedChildDoesNotPreventAnotherAccountsCreation() throws Exception {
        var blocked = fixture();
        var independent = fixture();
        var entered = new CompletableFuture<Integer>();
        try (var workers = Executors.newFixedThreadPool(2)) {
            var pending = transaction().execute(tx -> {
                int blocker = backendPid();
                jdbc.queryForList("SELECT id FROM children WHERE id = ? FOR UPDATE", UUID.class, blocked.childId());
                var first = workers.submit(() -> attemptInTransaction(blocked, entered));
                awaitBlocked(entered.orTimeout(5, TimeUnit.SECONDS).join(), blocker);
                var second = workers.submit(() -> attempt(independent.session(), independent.childId(), request()));
                try {
                    assertThat(second.get(5, TimeUnit.SECONDS)).isEqualTo("CREATED");
                }
                catch (Exception failure) {
                    throw new AssertionError(failure);
                }
                return first;
            });
            assertThat(pending.get(10, TimeUnit.SECONDS)).isEqualTo("CREATED");
        }
        assertCounts(2, 2);
    }

    @Test
    void accountLockPrecedesContextAndInvalidationPreventsCreation() throws Exception {
        var fixture = fixture();
        var entered = new CompletableFuture<Integer>();
        try (var worker = Executors.newSingleThreadExecutor()) {
            var pending = transaction().execute(tx -> {
                int blocker = backendPid();
                jdbc.queryForList("SELECT id FROM accounts WHERE id = ? FOR UPDATE", UUID.class, fixture.session().accountId());
                var attempt = worker.submit(() -> attemptInTransaction(fixture, entered));
                awaitBlocked(entered.orTimeout(5, TimeUnit.SECONDS).join(), blocker);
                jdbc.queryForList("SELECT id FROM session_security WHERE id = ? FOR UPDATE NOWAIT",
                        UUID.class, fixture.session().securityContextId());
                jdbc.update("UPDATE accounts SET session_version = session_version + 1 WHERE id = ?", fixture.session().accountId());
                return attempt;
            });
            assertThat(pending.get(10, TimeUnit.SECONDS)).isEqualTo("AUTH_INVALID");
        }
        assertCounts(0, 0);
    }

    @Test
    void sessionExpiryDuringChildLockWaitIsRechecked() throws Exception {
        var fixture = fixture();
        jdbc.update("UPDATE session_security SET expires_at = clock_timestamp() + INTERVAL '10 seconds' WHERE id = ?",
                fixture.session().securityContextId());
        var entered = new CompletableFuture<Integer>();
        try (var worker = Executors.newSingleThreadExecutor()) {
            var pending = transaction().execute(tx -> {
                int blocker = backendPid();
                jdbc.queryForList("SELECT id FROM children WHERE id = ? FOR UPDATE", UUID.class, fixture.childId());
                var attempt = worker.submit(() -> attemptInTransaction(fixture, entered));
                awaitBlocked(entered.orTimeout(5, TimeUnit.SECONDS).join(), blocker);
                // 보안 행은 작업자가 잠갔으므로 변경하지 않고 실제 DB 시각이 만료에 도달하기를 기다린다.
                jdbc.queryForList("""
                        SELECT pg_sleep(GREATEST(EXTRACT(EPOCH FROM expires_at - clock_timestamp()), 0)::double precision + 0.05)
                        FROM session_security WHERE id = ?
                        """, fixture.session().securityContextId());
                return attempt;
            });
            assertThat(pending.get(10, TimeUnit.SECONDS)).isEqualTo("AUTH_INVALID");
        }
        assertCounts(0, 0);
    }

    @Test
    void realRepositoryClockAdvancesWithinOneTransactionWhileCurrentTimestampStaysFixed() {
        doCallRealMethod().when(repository).currentTime();
        transaction().executeWithoutResult(tx -> {
            var started = jdbc.queryForObject("SELECT CURRENT_TIMESTAMP", (row, _) -> row.getObject(1, OffsetDateTime.class));
            jdbc.execute("SELECT pg_sleep(0.02)");
            var actual = repository.currentTime();
            var unchanged = jdbc.queryForObject("SELECT CURRENT_TIMESTAMP", (row, _) -> row.getObject(1, OffsetDateTime.class));
            assertThat(actual.toInstant()).isAfter(started.toInstant());
            assertThat(unchanged.toInstant()).isEqualTo(started.toInstant());
        });
    }

    @Test
    void partialUniqueIsStillEnforcedWhenRepositoryIsCalledWithoutServicePrecheck() {
        var fixture = fixture();
        service.create(fixture.session(), fixture.childId(), request());
        assertThat(repository.insertActive(UUID.randomUUID(), fixture.childId(), UUID.randomUUID(), STARTED_AT,
                ConversationServiceHours.requireOpen(STARTED_AT))).isEmpty();
        assertCounts(1, 1);
    }

    @Test
    void unrelatedUniqueViolationRemainsStorageFailureInsteadOf409() {
        var first = fixture();
        var stored = service.create(first.session(), first.childId(), request()).result();
        var second = fixture();
        var failure = assertThrows(ConversationException.class, () -> repository.insertActive(stored.conversationId(),
                second.childId(), UUID.randomUUID(), STARTED_AT, ConversationServiceHours.requireOpen(STARTED_AT)));
        assertThat(failure.code()).isEqualTo("INTERNAL_ERROR");
        assertThat(failure.activeConversationId()).isNull();
        assertThat(failure.getCause()).isInstanceOfSatisfying(DataIntegrityViolationException.class,
                cause -> assertThat(cause.getMostSpecificCause()).isInstanceOfSatisfying(SQLException.class,
                        sql -> assertThat(sql.getSQLState()).isEqualTo("23505")));
        assertCounts(1, 1);
    }

    private String attemptInTransaction(Fixture fixture, CompletableFuture<Integer> entered) {
        try {
            transaction().execute(tx -> {
                jdbc.execute("SET LOCAL statement_timeout = '15s'");
                entered.complete(backendPid());
                return service.create(fixture.session(), fixture.childId(), request());
            });
            return "CREATED";
        }
        catch (ConversationException failure) {
            return failure.code();
        }
        catch (InsufficientAuthenticationException failure) {
            return "AUTH_INVALID";
        }
    }

    private String attempt(AuthenticatedSession session, UUID childId, StartConversationRequest request) {
        try {
            return service.create(session, childId, request).created() ? "CREATED" : "REPLAY";
        }
        catch (ConversationException failure) {
            return failure.code();
        }
    }

    private void awaitBlocked(int worker, int blocker) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < deadline) {
            if (Boolean.TRUE.equals(jdbc.queryForObject("SELECT ? = ANY(pg_blocking_pids(?))", Boolean.class, blocker, worker))) {
                return;
            }
            Thread.yield();
        }
        throw new AssertionError("대화 생성이 지정한 행 잠금을 기다리지 않았습니다.");
    }

    private int backendPid() {
        return Objects.requireNonNull(jdbc.queryForObject("SELECT pg_backend_pid()", Integer.class), "DB 연결 PID가 필요합니다.");
    }

    private TransactionTemplate transaction() {
        return new TransactionTemplate(transactions);
    }

    private StartConversationRequest request() {
        return new StartConversationRequest(UUID.randomUUID());
    }

    private Fixture fixture() {
        var accountId = UUID.randomUUID();
        var childId = UUID.randomUUID();
        jdbc.update("INSERT INTO accounts (id, email) VALUES (?, ?)", accountId, accountId + "@example.invalid");
        var session = security.createLoginContext(accountId, null);
        jdbc.update("UPDATE session_security SET created_at = ? WHERE id = ?", STARTED_AT.minusDays(1), session.securityContextId());
        jdbc.update("""
                INSERT INTO children (id, account_id, name, nickname, birth_date, gender, character_id, created_at)
                VALUES (?, ?, '가나', '별이', DATE '2020-05-12', 'FEMALE', 'test-character', ?)
                """, childId, accountId, STARTED_AT.minusDays(1));
        return new Fixture(session, childId);
    }

    private void assertCounts(int conversations, int links) {
        assertThat(jdbc.queryForObject("SELECT count(*) FROM conversations", Integer.class)).isEqualTo(conversations);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM conversation_session_links", Integer.class)).isEqualTo(links);
    }

    private record Fixture(AuthenticatedSession session, UUID childId) { }
}
