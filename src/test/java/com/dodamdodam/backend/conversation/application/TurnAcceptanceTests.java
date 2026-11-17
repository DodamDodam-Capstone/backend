package com.dodamdodam.backend.conversation.application;

import com.dodamdodam.backend.TestcontainersConfiguration;
import com.dodamdodam.backend.auth.session.AuthenticatedSession;
import com.dodamdodam.backend.auth.session.SessionSecurityRepository;
import com.dodamdodam.backend.child.exception.ChildException;
import com.dodamdodam.backend.conversation.domain.TurnStatus;
import com.dodamdodam.backend.conversation.exception.ConversationException;
import com.dodamdodam.backend.conversation.infrastructure.ConversationRepository;
import com.dodamdodam.backend.conversation.infrastructure.ConversationTurnRepository;
import java.sql.SQLException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
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
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.AopTestUtils;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;

/** 음성 HTTP·AI 없이 실제 PostgreSQL 접수·멱등성·잠금·커밋 경계를 검증한다. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = {"spring.session.jdbc.cleanup-cron=-"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Timeout(30)
class TurnAcceptanceTests {

    private static final OffsetDateTime CREATED_AT = OffsetDateTime.parse("2026-10-03T14:00:00.123456+09:00");
    private static final byte[] AUDIO = "abc".getBytes(StandardCharsets.UTF_8);
    private static final String AUDIO_HASH = "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad";
    @Autowired private JdbcTemplate jdbc;
    @Autowired private TurnAcceptanceService service;
    @Autowired private ConversationClosureCoordinator closure;
    @Autowired private PlatformTransactionManager transactions;
    @MockitoSpyBean private SessionSecurityRepository security;
    @MockitoSpyBean private ConversationRepository conversations;
    @MockitoSpyBean private ConversationTurnRepository turns;

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
        // 업무 시각만 고정하고 인증 유효성·잠금·발화 저장은 실제 DB에서 실행한다.
        doReturn(CREATED_AT).when(conversations).currentTime();
    }

    @Test
    void commitsFirstProcessingWithOnlyHashAndOriginalDeadline() {
        var fixture = fixture();
        var key = UUID.randomUUID();
        var result = accept(fixture, key, AUDIO);

        assertThat(result.newlyAccepted()).isTrue();
        assertThat(result.completionPermit()).isPresent();
        assertThat(result.turn().sequence()).isEqualTo(1);
        assertThat(result.turn().status()).isEqualTo(TurnStatus.PROCESSING);
        assertThat(result.processingDeadlineAt().toInstant()).isEqualTo(CREATED_AT.plusSeconds(60).toInstant());
        var row = jdbc.queryForMap("SELECT * FROM conversation_turns WHERE id = ?", result.turn().turnId());
        assertThat(row).containsEntry("conversation_id", fixture.conversationId()).containsEntry("client_request_id", key)
                .containsEntry("request_hash", AUDIO_HASH).containsEntry("status", "PROCESSING")
                .containsEntry("child_text_visibility", "OMITTED").containsEntry("reply_text_visibility", "OMITTED");
        for (var column : List.of("child_text", "reply_text", "completed_at", "error_code")) {
            assertThat(row.get(column)).as(column).isNull();
        }
        assertThat(result.turn().createdAt().toInstant()).isEqualTo(CREATED_AT.toInstant());
        assertCount(1);
    }

    @Test
    void hashesUnmodifiedBinaryBytesWithoutDecodingOrNormalizingLineEndings() {
        var audio = HexFormat.of().parseHex("00ff800d0a2a");
        var result = accept(fixture(), UUID.randomUUID(), audio);
        // 잘못된 UTF-8과 CRLF도 원본 바이트 그대로 포함하는 알려진 SHA-256 벡터다.
        assertThat(jdbc.queryForObject("SELECT request_hash FROM conversation_turns WHERE id = ?", String.class, result.turn().turnId()))
                .isEqualTo("385440f438280dc22838364cf301d4716c0460d2b027ef6a9510cff12929c28b");
        assertThat(audio).containsExactly((byte) 0, (byte) 255, (byte) 128, (byte) 13, (byte) 10, (byte) 42);
    }

    @ParameterizedTest
    @EnumSource(value = TurnStatus.class, names = {"SUCCEEDED", "FAILED"})
    void assignsNextSequenceWithoutReusingTerminalOrMatchingAudio(TurnStatus status) {
        var fixture = fixture();
        var first = accept(fixture, UUID.randomUUID(), AUDIO);
        complete(first.turn().turnId(), status);

        var next = accept(fixture, UUID.randomUUID(), AUDIO);
        assertThat(next.newlyAccepted()).isTrue();
        assertThat(next.turn().sequence()).isEqualTo(2);
        assertThat(next.turn().turnId()).isNotEqualTo(first.turn().turnId());
        assertCount(2);
    }

    @Test
    void usesMaximumSequenceRatherThanCountEvenWhenThereAreGaps() {
        var fixture = fixture();
        var first = accept(fixture, UUID.randomUUID(), AUDIO);
        complete(first.turn().turnId(), TurnStatus.FAILED);
        jdbc.update("UPDATE conversation_turns SET sequence = 7 WHERE id = ?", first.turn().turnId());
        assertThat(accept(fixture, UUID.randomUUID(), AUDIO).turn().sequence()).isEqualTo(8);
    }

    @ParameterizedTest
    @EnumSource(TurnStatus.class)
    void sameKeyAndBytesReturnStoredStateAndNeverRenewDeadline(TurnStatus status) {
        var fixture = fixture();
        var key = UUID.randomUUID();
        var first = accept(fixture, key, AUDIO);
        if (status != TurnStatus.PROCESSING) {
            complete(first.turn().turnId(), status);
        }
        var before = jdbc.queryForMap("SELECT * FROM conversation_turns WHERE id = ?", first.turn().turnId());
        doReturn(CREATED_AT.plusMinutes(2)).when(conversations).currentTime();

        var replay = accept(fixture, key, AUDIO.clone());
        assertThat(replay.newlyAccepted()).isFalse();
        assertThat(replay.completionPermit()).isEmpty();
        assertThat(replay.turn().turnId()).isEqualTo(first.turn().turnId());
        assertThat(replay.turn().status()).isEqualTo(status);
        assertThat(replay.turn().sequence()).isEqualTo(1);
        assertThat(replay.turn().childText()).isEqualTo(status == TurnStatus.PROCESSING ? null : "[test] permitted child");
        assertThat(replay.turn().errorCode()).isEqualTo(status == TurnStatus.FAILED ? "AI_UPSTREAM_FAILED" : null);
        assertThat(replay.processingDeadlineAt()).isEqualTo(first.processingDeadlineAt());
        assertThat(jdbc.queryForMap("SELECT * FROM conversation_turns WHERE id = ?", first.turn().turnId())).isEqualTo(before);
        assertCount(1);
    }

    @Test
    void reusesTerminalKeyBeforeCheckingAnotherProcessingTurn() {
        var fixture = fixture();
        var key = UUID.randomUUID();
        var first = accept(fixture, key, AUDIO);
        complete(first.turn().turnId(), TurnStatus.FAILED);
        accept(fixture, UUID.randomUUID(), AUDIO);

        var replay = accept(fixture, key, AUDIO);
        assertThat(replay.newlyAccepted()).isFalse();
        assertThat(replay.turn().turnId()).isEqualTo(first.turn().turnId());
        assertThat(replay.turn().status()).isEqualTo(TurnStatus.FAILED);
        assertCount(2);
    }

    @ParameterizedTest
    @EnumSource(TurnStatus.class)
    void sameKeyAndDifferentBytesAreIdempotencyConflict(TurnStatus status) {
        var fixture = fixture();
        var key = UUID.randomUUID();
        var first = accept(fixture, key, AUDIO);
        if (status != TurnStatus.PROCESSING) {
            complete(first.turn().turnId(), status);
        }
        assertCode("IDEMPOTENCY_CONFLICT", () -> accept(fixture, key, new byte[]{1, 2, 3}));
        assertCount(1);
    }

    @Test
    void newKeyCannotBypassProcessingLimitEvenWithIdenticalAudio() {
        var fixture = fixture();
        accept(fixture, UUID.randomUUID(), AUDIO);
        assertCode("TURN_IN_PROGRESS", () -> accept(fixture, UUID.randomUUID(), AUDIO));
        assertCount(1);
    }

    @Test
    void scopesRequestKeyAndSequenceToConversation() {
        var key = UUID.randomUUID();
        var first = accept(fixture(), key, AUDIO);
        var second = accept(fixture(), key, AUDIO);
        assertThat(first.turn().sequence()).isEqualTo(1);
        assertThat(second.turn().sequence()).isEqualTo(1);
        assertThat(first.turn().turnId()).isNotEqualTo(second.turn().turnId());
        assertCount(2);
    }

    @ParameterizedTest
    @CsvSource({"CLOSING,CONVERSATION_CLOSING", "ENDED,CONVERSATION_ENDED"})
    void closingAndEndedRejectExistingKeysBeforeHashComparison(String state, String code) {
        var fixture = fixture();
        var key = UUID.randomUUID();
        var first = accept(fixture, key, AUDIO);
        complete(first.turn().turnId(), TurnStatus.SUCCEEDED);
        jdbc.update("""
                UPDATE conversations SET status = 'CLOSING', end_reason = 'MANUAL', end_requested_at = ? WHERE id = ?
                """, CREATED_AT.plusSeconds(2), fixture.conversationId());
        if (state.equals("ENDED")) {
            jdbc.update("UPDATE conversations SET status = 'ENDED', summary_status = 'EMPTY', ended_at = end_requested_at WHERE id = ?",
                    fixture.conversationId());
        }
        assertCode(code, () -> accept(fixture, key, AUDIO));
        assertCode(code, () -> accept(fixture, key, new byte[]{42}));
        assertCode(code, () -> accept(fixture, UUID.randomUUID(), AUDIO));
        assertCount(1);
    }

    @ParameterizedTest
    @CsvSource({"2026-10-03T00:00:00+09:00,SERVICE_HOURS_CLOSED", "2026-10-03T07:59:59.999999+09:00,SERVICE_HOURS_CLOSED",
            "2026-10-04T00:00:00+09:00,SERVICE_HOURS_CLOSED", "2026-10-04T08:00:00+09:00,CONVERSATION_CLOSING"})
    void rejectsClosedHoursAndPastScheduledEnd(String now, String code) {
        var fixture = fixture();
        doReturn(OffsetDateTime.parse(now)).when(conversations).currentTime();
        assertCode(code, () -> accept(fixture, UUID.randomUUID(), AUDIO));
        assertCount(0);
    }

    @ParameterizedTest
    @ValueSource(strings = {"2026-10-03T08:00:00+09:00", "2026-10-03T23:59:59.999999+09:00"})
    void acceptsOpeningAndLastMicrosecondBeforeMidnight(String value) {
        var fixture = fixture();
        var now = OffsetDateTime.parse(value);
        doReturn(now).when(conversations).currentTime();
        assertThat(accept(fixture, UUID.randomUUID(), AUDIO).turn().createdAt().toInstant()).isEqualTo(now.toInstant());
    }

    @Test
    void unlinkedCurrentSessionCannotReplayOrLearnHashConflicts() {
        var fixture = fixture();
        var key = UUID.randomUUID();
        accept(fixture, key, AUDIO);
        var otherContext = security.createLoginContext(fixture.session().accountId(), null);
        var unlinked = new Fixture(otherContext, fixture.childId(), fixture.conversationId());
        assertCode("CHILD_SESSION_REQUIRED", () -> accept(unlinked, key, AUDIO));
        assertCode("CHILD_SESSION_REQUIRED", () -> accept(unlinked, key, new byte[]{42}));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM conversation_session_links", Integer.class)).isEqualTo(1);
        assertCount(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"MISSING_CHILD", "OTHER_CHILD", "OTHER_CONVERSATION", "MISSING_CONVERSATION"})
    void requiresOwnedChildAndMatchingConversation(String kind) {
        var fixture = fixture();
        var other = fixture();
        var childId = switch (kind) {
            case "MISSING_CHILD" -> UUID.randomUUID();
            case "OTHER_CHILD" -> other.childId();
            default -> fixture.childId();
        };
        var conversationId = switch (kind) {
            case "OTHER_CONVERSATION" -> other.conversationId();
            case "MISSING_CONVERSATION" -> UUID.randomUUID();
            default -> fixture.conversationId();
        };
        var failure = assertThrows(ChildException.class,
                () -> service.accept(fixture.session(), childId, conversationId, UUID.randomUUID(), AUDIO));
        assertThat(failure.code()).isEqualTo("RESOURCE_NOT_FOUND");
        assertCount(0);
    }

    @ParameterizedTest
    @ValueSource(strings = {"REVOKED", "EXPIRED", "VERSION", "OTHER_ACCOUNT"})
    void rejectsInvalidServerContextEvenForExistingKey(String kind) {
        var fixture = fixture();
        var key = UUID.randomUUID();
        accept(fixture, key, AUDIO);
        var session = fixture.session();
        switch (kind) {
            case "REVOKED" -> security.revokeCurrentContext(session.accountId(), session.securityContextId());
            case "EXPIRED" -> jdbc.update("""
                    UPDATE session_security SET created_at = clock_timestamp() - INTERVAL '2 seconds',
                        expires_at = clock_timestamp() - INTERVAL '1 second' WHERE id = ?
                    """,
                    session.securityContextId());
            case "VERSION" -> jdbc.update("UPDATE accounts SET session_version = session_version + 1 WHERE id = ?", session.accountId());
            case "OTHER_ACCOUNT" -> session = new AuthenticatedSession(fixture().session().accountId(), session.securityContextId());
            default -> throw new AssertionError(kind);
        }
        var invalid = new Fixture(session, fixture.childId(), fixture.conversationId());
        assertThatThrownBy(() -> accept(invalid, key, AUDIO)).isInstanceOf(InsufficientAuthenticationException.class);
        assertCount(1);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void concurrentSameKeyReturnsNewlyAcceptedOnlyToFirstCommitter(boolean sameAudio) throws Exception {
        var fixture = fixture();
        var key = UUID.randomUUID();
        var start = new CountDownLatch(1);
        try (var workers = Executors.newFixedThreadPool(2)) {
            var first = workers.submit(() -> { start.await(); return attempt(fixture, key, AUDIO); });
            var second = workers.submit(() -> { start.await(); return attempt(fixture, key, sameAudio ? AUDIO.clone() : new byte[]{42}); });
            start.countDown();
            assertThat(List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder("NEW", sameAudio ? "REPLAY" : "IDEMPOTENCY_CONFLICT");
        }
        assertCount(1);
        assertThat(accept(fixture, key, sameAudio ? AUDIO : storedAudio()).newlyAccepted()).isFalse();
    }

    @Test
    void concurrentDifferentKeysCommitOnlyOneProcessingAndSequence() throws Exception {
        var fixture = fixture();
        var secondContext = security.createLoginContext(fixture.session().accountId(), null);
        jdbc.update("INSERT INTO conversation_session_links (session_security_id, conversation_id, bound_at) VALUES (?, ?, ?)",
                secondContext.securityContextId(), fixture.conversationId(), CREATED_AT.minusSeconds(1));
        var other = new Fixture(secondContext, fixture.childId(), fixture.conversationId());
        var start = new CountDownLatch(1);
        try (var workers = Executors.newFixedThreadPool(2)) {
            var first = workers.submit(() -> { start.await(); return attempt(fixture, UUID.randomUUID(), AUDIO); });
            var second = workers.submit(() -> { start.await(); return attempt(other, UUID.randomUUID(), AUDIO); });
            start.countDown();
            assertThat(List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder("NEW", "TURN_IN_PROGRESS");
        }
        assertCount(1);
        assertThat(jdbc.queryForObject("SELECT sequence FROM conversation_turns", Integer.class)).isEqualTo(1);
    }

    @ParameterizedTest
    @CsvSource({"REQUEST,uq_turn_request", "SEQUENCE,uq_turn_sequence", "PROCESSING,uq_turns_processing_conversation"})
    void databaseUniqueWaitsForOtherTransactionAndRejectsOnlyConflictingInsert(String kind, String constraint) throws Exception {
        var fixture = fixture();
        var firstId = UUID.randomUUID();
        var firstKey = UUID.randomUUID();
        var entered = new CompletableFuture<Integer>();
        try (var worker = Executors.newSingleThreadExecutor()) {
            var pending = transaction().execute(_ -> {
                int blocker = backendPid();
                turns.insertProcessing(firstId, fixture.conversationId(), firstKey, "a".repeat(64), 1,
                        CREATED_AT, CREATED_AT.plusSeconds(60));
                if (!kind.equals("PROCESSING")) {
                    complete(firstId, TurnStatus.SUCCEEDED);
                }
                // 서비스 사전 검사 없이 별도 Tx의 실제 UNIQUE 인덱스 대기·실패를 확인한다.
                var attempt = worker.submit(() -> assertThrows(ConversationException.class, () -> transaction().execute(_ -> {
                    entered.complete(backendPid());
                    return turns.insertProcessing(UUID.randomUUID(), fixture.conversationId(),
                            kind.equals("REQUEST") ? firstKey : UUID.randomUUID(), "b".repeat(64),
                            kind.equals("SEQUENCE") ? 1 : 2, CREATED_AT, CREATED_AT.plusSeconds(60));
                })));
                awaitBlocked(entered.orTimeout(5, TimeUnit.SECONDS).join(), blocker);
                return attempt;
            });
            var failure = pending.get(10, TimeUnit.SECONDS);
            assertThat(failure.code()).isEqualTo("INTERNAL_ERROR");
            assertThat(failure.getCause()).isInstanceOfSatisfying(DataIntegrityViolationException.class,
                    cause -> assertThat(cause.getMostSpecificCause()).isInstanceOfSatisfying(SQLException.class, sql -> {
                        assertThat(sql.getSQLState()).isEqualTo("23505");
                        assertThat(sql.getMessage()).contains(constraint);
                    }));
        }
        assertCount(1);
        assertThat(jdbc.queryForObject("SELECT id FROM conversation_turns", UUID.class)).isEqualTo(firstId);
    }

    @ParameterizedTest
    @ValueSource(strings = {"CHILD", "CONVERSATION", "TURN", "LINK"})
    void rechecksDatabaseTimeAfterEveryLowerLockWait(String target) throws Exception {
        var fixture = fixture();
        var key = UUID.randomUUID();
        var first = accept(fixture, key, AUDIO);
        var entered = observeTransactionEntry();
        var clock = new AtomicReference<>(CREATED_AT);
        doAnswer(_ -> clock.get()).when(conversations).currentTime();
        try (var worker = Executors.newSingleThreadExecutor()) {
            var pending = transaction().execute(_ -> {
                int blocker = backendPid();
                switch (target) {
                    case "CHILD" -> jdbc.queryForList("SELECT id FROM children WHERE id = ? FOR UPDATE", fixture.childId());
                    case "CONVERSATION" -> jdbc.queryForList("SELECT id FROM conversations WHERE id = ? FOR UPDATE", fixture.conversationId());
                    case "TURN" -> jdbc.queryForList("SELECT id FROM conversation_turns WHERE id = ? FOR UPDATE", first.turn().turnId());
                    case "LINK" -> jdbc.queryForList("SELECT session_security_id FROM conversation_session_links WHERE conversation_id = ? FOR UPDATE",
                            fixture.conversationId());
                    default -> throw new AssertionError(target);
                }
                var attempt = worker.submit(() -> attempt(fixture, key, AUDIO));
                awaitBlocked(entered.orTimeout(5, TimeUnit.SECONDS).join(), blocker);
                clock.set(OffsetDateTime.parse("2026-10-04T00:00:00+09:00"));
                return attempt;
            });
            assertThat(pending.get(10, TimeUnit.SECONDS)).isEqualTo("SERVICE_HOURS_CLOSED");
        }
        assertCount(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"CLOSING", "ENDED"})
    void readsStateCommittedWhileWaitingForConversation(String state) throws Exception {
        var fixture = fixture();
        var entered = observeTransactionEntry();
        try (var worker = Executors.newSingleThreadExecutor()) {
            var pending = transaction().execute(_ -> {
                int blocker = backendPid();
                jdbc.queryForList("SELECT id FROM conversations WHERE id = ? FOR UPDATE", fixture.conversationId());
                var attempt = worker.submit(() -> attempt(fixture, UUID.randomUUID(), AUDIO));
                awaitBlocked(entered.orTimeout(5, TimeUnit.SECONDS).join(), blocker);
                // 잠금 대기 뒤 보이는 최종 상태를 한 문장으로 만든다. 실제 종료 경합은 조정기 테스트에서 별도 검증한다.
                jdbc.update("""
                        UPDATE conversations SET status = ?, end_reason = 'MANUAL', end_requested_at = ?,
                            ended_at = CASE WHEN ? = 'ENDED' THEN ?::timestamptz ELSE NULL END,
                            summary_status = CASE WHEN ? = 'ENDED' THEN 'EMPTY' ELSE 'NOT_STARTED' END WHERE id = ?
                        """, state, CREATED_AT, state, CREATED_AT, state, fixture.conversationId());
                return attempt;
            });
            assertThat(pending.get(10, TimeUnit.SECONDS)).isEqualTo("CONVERSATION_" + state);
        }
        assertCount(0);
    }

    @Test
    void accountLockComesFirstAndCommittedRevocationBlocksAcceptance() throws Exception {
        var fixture = fixture();
        var entered = observeTransactionEntry();
        try (var worker = Executors.newSingleThreadExecutor()) {
            var pending = transaction().execute(_ -> {
                int blocker = backendPid();
                jdbc.queryForList("SELECT id FROM accounts WHERE id = ? FOR UPDATE", fixture.session().accountId());
                var attempt = worker.submit(() -> {
                    try {
                        return attempt(fixture, UUID.randomUUID(), AUDIO);
                    } catch (InsufficientAuthenticationException failure) {
                        return "AUTH_INVALID";
                    }
                });
                awaitBlocked(entered.orTimeout(5, TimeUnit.SECONDS).join(), blocker);
                jdbc.queryForList("SELECT id FROM session_security WHERE id = ? FOR UPDATE NOWAIT", fixture.session().securityContextId());
                jdbc.update("UPDATE accounts SET session_version = session_version + 1 WHERE id = ?", fixture.session().accountId());
                return attempt;
            });
            assertThat(pending.get(10, TimeUnit.SECONDS)).isEqualTo("AUTH_INVALID");
        }
        assertCount(0);
    }

    @Test
    void manualEndWaitsForCommittedAcceptanceThenDrainsOriginalProcessing() throws Exception {
        var fixture = fixture();
        var inserted = new CompletableFuture<Integer>();
        var release = new CountDownLatch(1);
        doAnswer(invocation -> {
            var result = invocation.callRealMethod();
            inserted.complete(backendPid());
            assertThat(release.await(10, TimeUnit.SECONDS)).isTrue();
            return result;
        }).when(turnSpy()).insertProcessing(any(), any(), any(), anyString(), anyInt(), any(), any());
        try (var workers = Executors.newFixedThreadPool(2)) {
            var accepted = workers.submit(() -> accept(fixture, UUID.randomUUID(), AUDIO));
            int blocker = inserted.orTimeout(5, TimeUnit.SECONDS).join();
            var entered = new CompletableFuture<Integer>();
            var ended = workers.submit(() -> transaction().execute(_ -> {
                entered.complete(backendPid());
                return closure.reconcile(fixture.session().accountId(), fixture.childId(), fixture.conversationId(),
                        ConversationClosureCoordinator.Trigger.REQUEST_END);
            }));
            try {
                awaitBlocked(entered.orTimeout(5, TimeUnit.SECONDS).join(), blocker);
            } finally {
                release.countDown();
            }
            var result = accepted.get(10, TimeUnit.SECONDS);
            assertThat(result.newlyAccepted()).isTrue();
            assertThat(ended.get(10, TimeUnit.SECONDS).state().status()).isEqualTo("CLOSING");
            var storedDeadline = jdbc.queryForObject("SELECT processing_deadline_at FROM conversation_turns WHERE id = ?",
                    (row, _) -> row.getObject(1, OffsetDateTime.class), result.turn().turnId());
            assertThat(storedDeadline).isEqualTo(result.processingDeadlineAt());
        } finally {
            release.countDown();
        }
        assertCount(1);
    }

    @Test
    void manualEndCommittedFirstPreventsAnyAcceptance() throws Exception {
        var fixture = fixture();
        var entered = observeTransactionEntry();
        try (var worker = Executors.newSingleThreadExecutor()) {
            var pending = transaction().execute(_ -> {
                var end = closure.reconcile(fixture.session().accountId(), fixture.childId(), fixture.conversationId(),
                        ConversationClosureCoordinator.Trigger.REQUEST_END);
                assertThat(end.state().status()).isEqualTo("ENDED");
                int blocker = backendPid();
                var attempt = worker.submit(() -> attempt(fixture, UUID.randomUUID(), AUDIO));
                awaitBlocked(entered.orTimeout(5, TimeUnit.SECONDS).join(), blocker);
                return attempt;
            });
            assertThat(pending.get(10, TimeUnit.SECONDS)).isEqualTo("CONVERSATION_ENDED");
        }
        assertCount(0);
    }

    @Test
    void failureAfterInsertRollsBackAndRetryStillStartsAtOne() {
        var fixture = fixture();
        var key = UUID.randomUUID();
        doAnswer(invocation -> {
            invocation.callRealMethod();
            throw new ConversationException(new DataAccessResourceFailureException("합성 저장 장애"));
        }).when(turnSpy()).insertProcessing(any(), any(), any(), anyString(), anyInt(), any(), any());
        assertCode("INTERNAL_ERROR", () -> accept(fixture, key, AUDIO));
        assertCount(0);
        restoreInsert();
        var retry = accept(fixture, key, AUDIO);
        assertThat(retry.newlyAccepted()).isTrue();
        assertThat(retry.turn().sequence()).isEqualTo(1);
    }

    @Test
    void commitFailureNeverReturnsNewAcceptanceOrPersistsTurn() {
        var fixture = fixture();
        doAnswer(invocation -> {
            var result = invocation.callRealMethod();
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void beforeCommit(boolean readOnly) {
                    throw new IllegalStateException("합성 커밋 장애");
                }
            });
            return result;
        }).when(turnSpy()).insertProcessing(any(), any(), any(), anyString(), anyInt(), any(), any());
        assertThatThrownBy(() -> accept(fixture, UUID.randomUUID(), AUDIO)).isInstanceOf(IllegalStateException.class)
                .hasMessage("합성 커밋 장애");
        assertCount(0);
    }

    @Test
    void rejectsAmbientTransactionSoNewAcceptanceCannotEscapeBeforeCommit() {
        var fixture = fixture();
        assertThatThrownBy(() -> transaction().execute(_ -> accept(fixture, UUID.randomUUID(), AUDIO)))
                .isInstanceOf(IllegalTransactionStateException.class);
        assertCount(0);
    }

    @Test
    void rechecksContextAfterLinkLockBeforeReusingExistingResult() {
        var fixture = fixture();
        var key = UUID.randomUUID();
        accept(fixture, key, AUDIO);
        doAnswer(invocation -> {
            var linked = invocation.callRealMethod();
            jdbc.update("""
                    UPDATE session_security SET created_at = clock_timestamp() - INTERVAL '2 seconds',
                        expires_at = clock_timestamp() - INTERVAL '1 second' WHERE id = ?
                    """,
                    fixture.session().securityContextId());
            return linked;
        }).when(AopTestUtils.<ConversationRepository>getUltimateTargetObject(conversations)).hasLinkForUpdate(any(), any());
        assertThatThrownBy(() -> accept(fixture, key, AUDIO)).isInstanceOf(InsufficientAuthenticationException.class);
        assertCount(1);
    }

    @Test
    void expiredContextAfterConversationLockTakesPriorityOverTimeRejection() {
        var fixture = fixture();
        doAnswer(invocation -> {
            var conversation = invocation.callRealMethod();
            jdbc.update("""
                    UPDATE session_security SET created_at = clock_timestamp() - INTERVAL '2 seconds',
                        expires_at = clock_timestamp() - INTERVAL '1 second' WHERE id = ?
                    """, fixture.session().securityContextId());
            return conversation;
        }).when(AopTestUtils.<ConversationRepository>getUltimateTargetObject(conversations)).findForUpdate(any(), any());
        doReturn(OffsetDateTime.parse("2026-10-04T00:00:00+09:00")).when(conversations).currentTime();

        assertThatThrownBy(() -> accept(fixture, UUID.randomUUID(), AUDIO)).isInstanceOf(InsufficientAuthenticationException.class);
        assertCount(0);
    }

    @Test
    void crossingMidnightDuringInsertRollsBackNewSequence() {
        var fixture = fixture();
        doReturn(CREATED_AT, CREATED_AT, CREATED_AT, OffsetDateTime.parse("2026-10-04T00:00:00+09:00"))
                .when(conversations).currentTime();
        assertCode("SERVICE_HOURS_CLOSED", () -> accept(fixture, UUID.randomUUID(), AUDIO));
        assertCount(0);
    }

    private TurnAcceptanceService.Acceptance accept(Fixture fixture, UUID key, byte[] audio) {
        return service.accept(fixture.session(), fixture.childId(), fixture.conversationId(), key, audio);
    }

    private String attempt(Fixture fixture, UUID key, byte[] audio) {
        try {
            return accept(fixture, key, audio).newlyAccepted() ? "NEW" : "REPLAY";
        } catch (ConversationException failure) {
            if (failure.code().equals("INTERNAL_ERROR")) {
                throw failure;
            }
            return failure.code();
        }
    }

    private void complete(UUID turnId, TurnStatus status) {
        // Commit 02는 결과 저장을 구현하지 않으므로 terminal은 제약을 만족하는 합성 fixture로만 만든다.
        jdbc.update("""
                UPDATE conversation_turns SET status = ?, completed_at = created_at + INTERVAL '1 second', error_code = ?,
                    child_text = '[test] permitted child', child_text_visibility = 'VISIBLE' WHERE id = ?
                """, status.name(), status == TurnStatus.FAILED ? "AI_UPSTREAM_FAILED" : null, turnId);
    }

    private byte[] storedAudio() {
        return AUDIO_HASH.equals(jdbc.queryForObject("SELECT request_hash FROM conversation_turns", String.class)) ? AUDIO : new byte[]{42};
    }

    private void restoreInsert() {
        doCallRealMethod().when(turnSpy()).insertProcessing(any(), any(), any(), anyString(), anyInt(), any(), any());
    }

    private CompletableFuture<Integer> observeTransactionEntry() {
        var entered = new CompletableFuture<Integer>();
        doAnswer(invocation -> {
            entered.complete(backendPid());
            return invocation.callRealMethod();
        }).when(AopTestUtils.<SessionSecurityRepository>getUltimateTargetObject(security)).requireCurrentContextForUpdate(any(), any());
        return entered;
    }

    private ConversationTurnRepository turnSpy() {
        // 스텁 설정 시에만 대상을 꺼내고 검증 본체는 MANDATORY 프록시를 통과한다.
        return AopTestUtils.getUltimateTargetObject(turns);
    }

    private void awaitBlocked(int worker, int blocker) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < deadline) {
            if (Boolean.TRUE.equals(jdbc.queryForObject("SELECT ? = ANY(pg_blocking_pids(?))", Boolean.class, blocker, worker))) {
                return;
            }
            Thread.yield();
        }
        throw new AssertionError("발화 접수가 지정한 행 잠금을 기다리지 않았습니다.");
    }

    private int backendPid() {
        return Objects.requireNonNull(jdbc.queryForObject("SELECT pg_backend_pid()", Integer.class));
    }

    private TransactionTemplate transaction() {
        return new TransactionTemplate(transactions);
    }

    private void assertCode(String code, Runnable operation) {
        assertThat(assertThrows(ConversationException.class, operation::run).code()).isEqualTo(code);
    }

    private void assertCount(int expected) {
        assertThat(jdbc.queryForObject("SELECT count(*) FROM conversation_turns", Integer.class)).isEqualTo(expected);
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM (SELECT conversation_id FROM conversation_turns WHERE status = 'PROCESSING'
                    GROUP BY conversation_id HAVING count(*) > 1) violations
                """, Integer.class)).isZero();
    }

    private Fixture fixture() {
        var accountId = UUID.randomUUID();
        var childId = UUID.randomUUID();
        var conversationId = UUID.randomUUID();
        jdbc.update("INSERT INTO accounts (id, email) VALUES (?, ?)", accountId, accountId + "@example.invalid");
        var session = security.createLoginContext(accountId, null);
        jdbc.update("""
                INSERT INTO children (id, account_id, name, nickname, birth_date, gender, character_id)
                VALUES (?, ?, '가나', '별이', DATE '2020-05-12', 'FEMALE', 'test-character')
                """, childId, accountId);
        jdbc.update("""
                INSERT INTO conversations (id, child_id, client_request_id, service_date, scheduled_end_at, status, summary_status, started_at)
                VALUES (?, ?, ?, '2026-10-03', '2026-10-04T00:00:00+09:00', 'ACTIVE', 'NOT_STARTED', '2026-10-03T08:00:00+09:00')
                """, conversationId, childId, UUID.randomUUID());
        jdbc.update("INSERT INTO conversation_session_links (session_security_id, conversation_id, bound_at) VALUES (?, ?, ?)",
                session.securityContextId(), conversationId, CREATED_AT.minusHours(1));
        return new Fixture(session, childId, conversationId);
    }

    private record Fixture(AuthenticatedSession session, UUID childId, UUID conversationId) { }
}
