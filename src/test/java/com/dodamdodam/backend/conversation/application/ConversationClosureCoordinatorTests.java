package com.dodamdodam.backend.conversation.application;

import com.dodamdodam.backend.TestcontainersConfiguration;
import com.dodamdodam.backend.auth.session.AuthenticatedSession;
import com.dodamdodam.backend.auth.session.SessionSecurityRepository;
import com.dodamdodam.backend.conversation.application.ConversationClosureCoordinator.Trigger;
import com.dodamdodam.backend.conversation.dto.StartConversationRequest;
import com.dodamdodam.backend.conversation.exception.ConversationException;
import com.dodamdodam.backend.conversation.infrastructure.ConversationRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = {"spring.session.jdbc.cleanup-cron=-"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Timeout(25)
class ConversationClosureCoordinatorTests {

    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-10-04T08:00:00.123456+09:00");
    private static final OffsetDateTime START = OffsetDateTime.parse("2026-10-03T23:59:30+09:00");
    private static final OffsetDateTime MIDNIGHT = OffsetDateTime.parse("2026-10-04T00:00:00+09:00");
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ConversationClosureCoordinator coordinator;
    @Autowired private ConversationService starts;
    @Autowired private SessionSecurityRepository security;
    @Autowired private PlatformTransactionManager transactions;
    @MockitoSpyBean private ConversationRepository clock;

    @BeforeEach
    void clearDatabase() {
        jdbc.update("DELETE FROM conversation_session_links");
        jdbc.update("DELETE FROM conversation_turns");
        jdbc.update("DELETE FROM conversations");
        jdbc.update("DELETE FROM SPRING_SESSION");
        jdbc.update("DELETE FROM email_verifications");
        jdbc.update("DELETE FROM children");
        jdbc.update("DELETE FROM guardian_pins");
        jdbc.update("DELETE FROM session_security");
        jdbc.update("DELETE FROM accounts");
        // 경계 시각만 고정하며 전이·발화 만료·링크 갱신은 실제 PostgreSQL에서 실행한다.
        doReturn(NOW).when(clock).currentTime();
    }

    @Test
    void retainsClosingAndOriginalDeadlineWhileProcessingIsBeforeDeadline() {
        var fixture = fixture();
        var turnId = turn(fixture, "PROCESSING", "OMITTED", "OMITTED", NOW.plusMinutes(1));
        var before = jdbc.queryForMap("SELECT * FROM conversation_turns WHERE id = ?", turnId);
        var result = reconcile(fixture);
        assertThat(result.endedNow()).isFalse();
        assertThat(result.state().status()).isEqualTo("CLOSING");
        assertThat(result.state().endReason()).isEqualTo("MIDNIGHT");
        assertThat(result.state().endRequestedAt().toInstant()).isEqualTo(MIDNIGHT.toInstant());
        assertThat(result.state().endedAt()).isNull();
        assertThat(result.state().summaryStatus()).isEqualTo("NOT_STARTED");
        doReturn(NOW.plusSeconds(10)).when(clock).currentTime();
        assertThat(reconcile(fixture).state()).isEqualTo(result.state());
        assertThat(jdbc.queryForMap("SELECT * FROM conversation_turns WHERE id = ?", turnId)).isEqualTo(before);
        assertThat(recovery(fixture.session().securityContextId())).isNull();
    }

    @ParameterizedTest
    @ValueSource(longs = {-1, 0, 1})
    void processingDeadlineUsesStrictBoundaryAndNeverMovesOriginalDeadline(long microseconds) {
        var fixture = fixture();
        var deadline = NOW.plusNanos(microseconds * 1_000);
        var turnId = turn(fixture, "PROCESSING", "OMITTED", "OMITTED", deadline);
        var result = reconcile(fixture);
        assertThat(time("SELECT processing_deadline_at FROM conversation_turns WHERE id = ?", turnId).toInstant())
                .isEqualTo(deadline.toInstant());
        var row = jdbc.queryForMap("SELECT status, error_code FROM conversation_turns WHERE id = ?", turnId);
        if (microseconds > 0) {
            assertThat(row).containsEntry("status", "PROCESSING").containsEntry("error_code", null);
            assertThat(result.state().status()).isEqualTo("CLOSING");
        }
        else {
            assertThat(row).containsEntry("status", "FAILED").containsEntry("error_code", "AI_TIMEOUT");
            assertThat(time("SELECT completed_at FROM conversation_turns WHERE id = ?", turnId).toInstant()).isEqualTo(NOW.toInstant());
            assertThat(result.state().status()).isEqualTo("ENDED");
            assertThat(result.state().summaryStatus()).isEqualTo("EMPTY");
        }
    }

    @ParameterizedTest
    @CsvSource({"SUCCEEDED,OMITTED,OMITTED,EMPTY", "FAILED,OMITTED,OMITTED,EMPTY",
            "SUCCEEDED,VISIBLE,OMITTED,PENDING", "FAILED,REDACTED,OMITTED,PENDING",
            "FAILED,OMITTED,VISIBLE,PENDING", "SUCCEEDED,OMITTED,REDACTED,PENDING"})
    void derivesSummaryFromAllowedTextIncludingFailedTurns(String status, String child, String reply, String expected) {
        var fixture = fixture();
        var turnId = turn(fixture, status, child, reply, NOW.minusHours(1));
        var before = jdbc.queryForMap("SELECT * FROM conversation_turns WHERE id = ?", turnId);
        var result = reconcile(fixture);
        assertThat(result.endedNow()).isTrue();
        assertThat(result.state().summaryStatus()).isEqualTo(expected);
        assertThat(result.state().endedAt().toInstant()).isEqualTo(NOW.toInstant());
        var row = conversation(fixture);
        assertThat(row).containsEntry("title", null).containsEntry("topic", null).containsEntry("summary", null)
                .containsEntry("summary_completed_at", null).containsEntry("summary_error_code", null);
        if (expected.equals("PENDING")) {
            assertThat(time("SELECT summary_started_at FROM conversations WHERE id = ?", fixture.conversationId()).toInstant())
                    .isEqualTo(NOW.toInstant());
            assertThat(time("SELECT summary_deadline_at FROM conversations WHERE id = ?", fixture.conversationId()).toInstant())
                    .isEqualTo(NOW.plusSeconds(60).toInstant());
        }
        else {
            assertThat(row).containsEntry("summary_started_at", null).containsEntry("summary_deadline_at", null);
        }
        assertThat(jdbc.queryForMap("SELECT * FROM conversation_turns WHERE id = ?", turnId)).isEqualTo(before);
        var snapshot = conversation(fixture);
        var links = jdbc.queryForList("SELECT * FROM conversation_session_links");
        doReturn(NOW.plusHours(1)).when(clock).currentTime();
        assertThat(reconcile(fixture).endedNow()).isFalse();
        assertThat(conversation(fixture)).isEqualTo(snapshot);
        assertThat(jdbc.queryForList("SELECT * FROM conversation_session_links")).isEqualTo(links);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void mixedTerminalAndProcessingTurnsDrainWithoutOverwritingResultsThenAllowNewStart(boolean allowedFailedText) {
        var fixture = fixture();
        var succeeded = turn(fixture, "SUCCEEDED", "OMITTED", "OMITTED", NOW.minusHours(1));
        var failed = turn(fixture, "FAILED", allowedFailedText ? "REDACTED" : "OMITTED", "OMITTED", NOW.minusHours(1));
        var deadline = NOW.plusMinutes(1);
        var processing = turn(fixture, "PROCESSING", "OMITTED", "OMITTED", deadline);
        var terminalRows = jdbc.queryForList("SELECT * FROM conversation_turns WHERE id IN (?, ?) ORDER BY sequence", succeeded, failed);
        var processingBefore = jdbc.queryForMap("SELECT * FROM conversation_turns WHERE id = ?", processing);

        var closing = reconcile(fixture);
        assertThat(closing.state().status()).isEqualTo("CLOSING");
        assertThat(closing.state().summaryStatus()).isEqualTo("NOT_STARTED");
        var blocked = assertThrows(ConversationException.class, () -> start(fixture, UUID.randomUUID()));
        assertThat(blocked.code()).isEqualTo("CONVERSATION_CLOSING");
        assertThat(jdbc.queryForMap("SELECT * FROM conversation_turns WHERE id = ?", processing)).isEqualTo(processingBefore);

        // 원래 저장된 deadline의 equality에서 기존 PROCESSING 하나만 만료시킨다.
        doReturn(deadline).when(clock).currentTime();
        var ended = reconcile(fixture);
        assertThat(ended.endedNow()).isTrue();
        assertThat(ended.state().status()).isEqualTo("ENDED");
        assertThat(ended.state().summaryStatus()).isEqualTo(allowedFailedText ? "PENDING" : "EMPTY");
        assertThat(ended.state().endRequestedAt()).isEqualTo(closing.state().endRequestedAt());
        assertThat(ended.state().endedAt().toInstant()).isEqualTo(deadline.toInstant());
        assertThat(jdbc.queryForList("SELECT * FROM conversation_turns WHERE id IN (?, ?) ORDER BY sequence", succeeded, failed))
                .isEqualTo(terminalRows);
        assertThat(jdbc.queryForMap("SELECT status, error_code FROM conversation_turns WHERE id = ?", processing))
                .containsEntry("status", "FAILED").containsEntry("error_code", "AI_TIMEOUT");
        assertThat(time("SELECT processing_deadline_at FROM conversation_turns WHERE id = ?", processing).toInstant())
                .isEqualTo(deadline.toInstant());
        assertThat(time("SELECT completed_at FROM conversation_turns WHERE id = ?", processing).toInstant()).isEqualTo(deadline.toInstant());
        assertThat(recovery(fixture.session().securityContextId()).toInstant()).isEqualTo(deadline.plusSeconds(600).toInstant());

        var oldConversation = conversation(fixture);
        var created = start(fixture, UUID.randomUUID());
        assertThat(created.created()).isTrue();
        assertThat(created.result().conversationId()).isNotEqualTo(fixture.conversationId());
        assertThat(conversation(fixture)).isEqualTo(oldConversation);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM conversations", Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM conversation_session_links", Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM conversations WHERE status IN ('ACTIVE','CLOSING')", Integer.class))
                .isEqualTo(1);
    }

    @Test
    void simultaneousClosureTriggersProduceOnlyOneFirstEndedTransition() throws Exception {
        var fixture = fixture();
        turn(fixture, "FAILED", "REDACTED", "OMITTED", NOW.minusHours(1));
        var processing = turn(fixture, "PROCESSING", "OMITTED", "OMITTED", NOW);
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        try (var workers = Executors.newFixedThreadPool(2)) {
            var pending = List.of(Trigger.OBSERVE, Trigger.REQUEST_END).stream().map(trigger -> workers.submit(() -> {
                ready.countDown();
                assertThat(start.await(10, TimeUnit.SECONDS)).isTrue();
                return coordinator.reconcile(fixture.session().accountId(), fixture.childId(), fixture.conversationId(), trigger);
            })).toList();
            try {
                assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            }
            finally {
                start.countDown();
            }
            var results = List.of(pending.get(0).get(10, TimeUnit.SECONDS), pending.get(1).get(10, TimeUnit.SECONDS));
            assertThat(results).extracting(ConversationClosureCoordinator.ClosureResult::endedNow)
                    .containsExactlyInAnyOrder(true, false);
            assertThat(results.get(0).state()).isEqualTo(results.get(1).state());
            assertThat(results.get(0).state().status()).isEqualTo("ENDED");
            assertThat(results.get(0).state().endReason()).isEqualTo("MIDNIGHT");
            assertThat(results.get(0).state().summaryStatus()).isEqualTo("PENDING");
        }
        assertThat(jdbc.queryForMap("SELECT status, error_code FROM conversation_turns WHERE id = ?", processing))
                .containsEntry("status", "FAILED").containsEntry("error_code", "AI_TIMEOUT");
        assertThat(time("SELECT ended_at FROM conversations WHERE id = ?", fixture.conversationId()).toInstant()).isEqualTo(NOW.toInstant());
        assertThat(recovery(fixture.session().securityContextId()).toInstant()).isEqualTo(NOW.plusSeconds(600).toInstant());
    }

    @Test
    void actualManualClosureAllowsSameDateRestartWhileSummaryIsPending() {
        var fixture = fixture();
        turn(fixture, "FAILED", "REDACTED", "OMITTED", NOW.minusHours(1));
        var manualAt = START.plusSeconds(10);
        doReturn(manualAt).when(clock).currentTime();
        var ended = coordinator.reconcile(fixture.session().accountId(), fixture.childId(), fixture.conversationId(), Trigger.REQUEST_END);
        assertThat(ended.state().status()).isEqualTo("ENDED");
        assertThat(ended.state().endReason()).isEqualTo("MANUAL");
        assertThat(ended.state().endRequestedAt().toInstant()).isEqualTo(manualAt.toInstant());
        assertThat(ended.state().summaryStatus()).isEqualTo("PENDING");
        var snapshot = conversation(fixture);
        doReturn(manualAt.plusSeconds(1)).when(clock).currentTime();
        var created = start(fixture, UUID.randomUUID());
        assertThat(created.created()).isTrue();
        assertThat(created.result().serviceDate()).isEqualTo(START.toLocalDate());
        assertThat(created.result().conversationId()).isNotEqualTo(fixture.conversationId());
        assertThat(conversation(fixture)).isEqualTo(snapshot);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM conversations", Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT count(DISTINCT service_date) FROM conversations", Integer.class)).isEqualTo(1);
    }

    @Test
    void emptyConversationEndsAndRepeatedCallsKeepAllTimesAndRecoveryUnchanged() {
        var fixture = fixture();
        assertThat(reconcile(fixture).endedNow()).isTrue();
        var conversation = conversation(fixture);
        var links = jdbc.queryForList("SELECT * FROM conversation_session_links");
        assertThat(conversation).containsEntry("summary_status", "EMPTY");
        assertThat(recovery(fixture.session().securityContextId()).toInstant()).isEqualTo(NOW.plusSeconds(600).toInstant());
        doReturn(NOW.plusHours(2)).when(clock).currentTime();
        assertThat(reconcile(fixture).endedNow()).isFalse();
        assertThat(conversation(fixture)).isEqualTo(conversation);
        assertThat(jdbc.queryForList("SELECT * FROM conversation_session_links")).isEqualTo(links);
    }

    @ParameterizedTest
    @ValueSource(strings = {"MANUAL", "MIDNIGHT"})
    void keepsExistingBoundaryAndReasonThroughDrainAndLaterCompletion(String reason) {
        var fixture = fixture();
        var boundary = reason.equals("MANUAL") ? START.plusSeconds(5) : MIDNIGHT;
        jdbc.update("UPDATE conversations SET status = 'CLOSING', end_requested_at = ?, end_reason = ? WHERE id = ?",
                boundary, reason, fixture.conversationId());
        turn(fixture, "PROCESSING", "OMITTED", "OMITTED", NOW.plusSeconds(30));
        assertThat(reconcile(fixture).state().status()).isEqualTo("CLOSING");
        doReturn(NOW.plusSeconds(30)).when(clock).currentTime();
        var ended = reconcile(fixture);
        assertThat(ended.state().status()).isEqualTo("ENDED");
        assertThat(ended.state().endReason()).isEqualTo(reason);
        assertThat(ended.state().endRequestedAt().toInstant()).isEqualTo(boundary.toInstant());
        var snapshot = conversation(fixture);
        doReturn(NOW.plusHours(1)).when(clock).currentTime();
        reconcile(fixture);
        assertThat(conversation(fixture)).isEqualTo(snapshot);
    }

    @Test
    void internalManualTriggerUsesLockedDbTimeButObservationBeforeMidnightDoesNothing() {
        var fixture = fixture();
        var manual = START.plusSeconds(10);
        doReturn(manual).when(clock).currentTime();
        assertThat(reconcile(fixture).state().status()).isEqualTo("ACTIVE");
        var result = coordinator.reconcile(fixture.session().accountId(), fixture.childId(), fixture.conversationId(), Trigger.REQUEST_END);
        assertThat(result.state().endReason()).isEqualTo("MANUAL");
        assertThat(result.state().endRequestedAt().toInstant()).isEqualTo(manual.toInstant());
        assertThat(result.state().status()).isEqualTo("ENDED");
    }

    @Test
    void grantsEveryEligibleExistingLinkAndExcludesBoundaryInvalidAndOtherOwnersContexts() {
        var fixture = fixture();
        var validFinite = link(fixture, "VALID", MIDNIGHT.minusNanos(1_000));
        var equal = link(fixture, "VALID", MIDNIGHT);
        var later = link(fixture, "VALID", MIDNIGHT.plusNanos(1_000));
        var revoked = link(fixture, "REVOKED", START);
        var expired = link(fixture, "EXPIRED", START);
        var deadline = link(fixture, "EQUALITY", START);
        var version = link(fixture, "VERSION", START);
        var other = link(fixture, "OTHER_OWNER", START);
        var bounds = jdbc.queryForList("SELECT session_security_id, bound_at FROM conversation_session_links ORDER BY session_security_id");
        reconcile(fixture);
        for (var id : List.of(fixture.session().securityContextId(), validFinite)) {
            assertThat(recovery(id).toInstant()).isEqualTo(NOW.plusSeconds(600).toInstant());
        }
        for (var id : List.of(equal, later, revoked, expired, deadline, version, other)) {
            assertThat(recovery(id)).isNull();
        }
        assertThat(jdbc.queryForList("SELECT session_security_id, bound_at FROM conversation_session_links ORDER BY session_security_id"))
                .isEqualTo(bounds);
    }

    @Test
    void recoveryStorageFailureRollsBackBoundaryTimeoutAndEndedTogether() {
        var fixture = fixture();
        var turnId = turn(fixture, "PROCESSING", "OMITTED", "OMITTED", NOW);
        var beforeConversation = conversation(fixture);
        var beforeTurn = jdbc.queryForMap("SELECT * FROM conversation_turns WHERE id = ?", turnId);
        jdbc.execute("ALTER TABLE conversation_session_links RENAME COLUMN end_recovery_until TO unavailable_recovery_until");
        try {
            var failure = assertThrows(ConversationException.class, () -> reconcile(fixture));
            assertThat(failure.code()).isEqualTo("INTERNAL_ERROR");
        }
        finally {
            jdbc.execute("ALTER TABLE conversation_session_links RENAME COLUMN unavailable_recovery_until TO end_recovery_until");
        }
        assertThat(conversation(fixture)).isEqualTo(beforeConversation);
        assertThat(jdbc.queryForMap("SELECT * FROM conversation_turns WHERE id = ?", turnId)).isEqualTo(beforeTurn);
        assertThat(recovery(fixture.session().securityContextId())).isNull();
    }

    @Test
    void startCommitsClosingEvenWhenItReturnsBusinessConflict() {
        var fixture = fixture();
        turn(fixture, "PROCESSING", "OMITTED", "OMITTED", NOW.plusMinutes(1));
        var failure = assertThrows(ConversationException.class, () -> start(fixture, UUID.randomUUID()));
        assertThat(failure.code()).isEqualTo("CONVERSATION_CLOSING");
        assertThat(conversation(fixture)).containsEntry("status", "CLOSING").containsEntry("end_reason", "MIDNIGHT");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM conversations", Integer.class)).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void outsideHoursStartCommitsMidnightCleanupButAlwaysReturnsServiceHoursClosed(boolean processing) {
        var fixture = fixture();
        if (processing) {
            turn(fixture, "PROCESSING", "OMITTED", "OMITTED", MIDNIGHT.plusSeconds(30));
        }
        doReturn(MIDNIGHT).when(clock).currentTime();
        var failure = assertThrows(ConversationException.class, () -> start(fixture, UUID.randomUUID()));
        assertThat(failure.code()).isEqualTo("SERVICE_HOURS_CLOSED");
        assertThat(conversation(fixture)).containsEntry("status", processing ? "CLOSING" : "ENDED")
                .containsEntry("end_reason", "MIDNIGHT");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM conversations", Integer.class)).isEqualTo(1);
    }

    @Test
    void oldStartKeyIsRejectedAfterCommittingEndedAndRecovery() {
        var fixture = fixture();
        var failure = assertThrows(ConversationException.class, () -> start(fixture, fixture.startKey()));
        assertThat(failure.code()).isEqualTo("CONVERSATION_ENDED");
        assertThat(conversation(fixture)).containsEntry("status", "ENDED");
        assertThat(recovery(fixture.session().securityContextId()).toInstant()).isEqualTo(NOW.plusSeconds(600).toInstant());
    }

    @Test
    void newKeyStartsAfterPreviousDatesAllowedFailedTextBecomesPendingSummary() {
        var fixture = fixture();
        turn(fixture, "FAILED", "REDACTED", "OMITTED", NOW.minusHours(1));
        var result = start(fixture, UUID.randomUUID());
        assertThat(result.created()).isTrue();
        assertThat(result.result().conversationId()).isNotEqualTo(fixture.conversationId());
        assertThat(conversation(fixture)).containsEntry("status", "ENDED").containsEntry("summary_status", "PENDING");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM conversations", Integer.class)).isEqualTo(2);
    }

    @Test
    void newLinkFailureRollsBackPrecedingClosureAndTurnExpiryInSameStartTransaction() {
        var fixture = fixture();
        var turnId = turn(fixture, "PROCESSING", "OMITTED", "OMITTED", NOW);
        var before = conversation(fixture);
        jdbc.execute("ALTER TABLE conversation_session_links ADD CONSTRAINT test_reject_new_link CHECK (conversation_id = '"
                + fixture.conversationId() + "'::uuid)");
        try {
            var failure = assertThrows(ConversationException.class, () -> start(fixture, UUID.randomUUID()));
            assertThat(failure.code()).isEqualTo("INTERNAL_ERROR");
        }
        finally {
            jdbc.execute("ALTER TABLE conversation_session_links DROP CONSTRAINT test_reject_new_link");
        }
        assertThat(conversation(fixture)).isEqualTo(before);
        assertThat(jdbc.queryForObject("SELECT status FROM conversation_turns WHERE id = ?", String.class, turnId)).isEqualTo("PROCESSING");
        assertThat(recovery(fixture.session().securityContextId())).isNull();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM conversations", Integer.class)).isEqualTo(1);
    }

    @Test
    void concurrentlyCommittedNormalTurnIsNeverOverwrittenByTimeout() throws Exception {
        var fixture = fixture();
        var turnId = turn(fixture, "PROCESSING", "OMITTED", "OMITTED", NOW);
        var entered = new CompletableFuture<Integer>();
        doAnswer(_ -> {
            jdbc.execute("SET LOCAL statement_timeout = '15s'");
            entered.complete(jdbc.queryForObject("SELECT pg_backend_pid()", Integer.class));
            return NOW;
        }).when(clock).currentTime();
        try (var worker = Executors.newSingleThreadExecutor()) {
            var future = new TransactionTemplate(transactions).execute(_ -> {
                // 잠금 경합에 사용하는 JDBC 결과는 언박싱 전에 필수값을 확인한다.
                int blocker = Objects.requireNonNull(jdbc.queryForObject("SELECT pg_backend_pid()", Integer.class),
                        "DB 연결 PID가 필요합니다.");
                jdbc.queryForList("SELECT id FROM conversation_turns WHERE id = ? FOR UPDATE", UUID.class, turnId);
                var pending = worker.submit(() -> reconcile(fixture));
                int pid = entered.orTimeout(5, TimeUnit.SECONDS).join();
                long limit = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
                boolean blocked = false;
                while (System.nanoTime() < limit) {
                    if (Boolean.TRUE.equals(jdbc.queryForObject("SELECT ? = ANY(pg_blocking_pids(?))", Boolean.class, blocker, pid))) {
                        blocked = true;
                        break;
                    }
                    Thread.yield();
                }
                assertThat(blocked).isTrue();
                // 먼저 진행하던 정상 결과의 커밋과 만료 조건부 UPDATE를 실제 두 연결로 경합시킨다.
                jdbc.update("""
                        UPDATE conversation_turns SET status = 'SUCCEEDED', completed_at = ?, child_text = '허용 결과',
                            child_text_visibility = 'VISIBLE' WHERE id = ? AND status = 'PROCESSING'
                        """, NOW.minusNanos(1_000), turnId);
                return pending;
            });
            assertThat(future.get(10, TimeUnit.SECONDS).state().summaryStatus()).isEqualTo("PENDING");
        }
        assertThat(jdbc.queryForMap("SELECT status, error_code, child_text FROM conversation_turns WHERE id = ?", turnId))
                .containsEntry("status", "SUCCEEDED").containsEntry("error_code", null).containsEntry("child_text", "허용 결과");
    }

    private ConversationClosureCoordinator.ClosureResult reconcile(Fixture fixture) {
        return coordinator.reconcile(fixture.session().accountId(), fixture.childId(), fixture.conversationId(), Trigger.OBSERVE);
    }

    private ConversationService.StartOutcome start(Fixture fixture, UUID key) {
        return starts.create(fixture.session(), fixture.childId(), new StartConversationRequest(key));
    }

    private java.util.Map<String, Object> conversation(Fixture fixture) {
        return jdbc.queryForMap("SELECT * FROM conversations WHERE id = ?", fixture.conversationId());
    }

    private OffsetDateTime recovery(UUID contextId) {
        return time("SELECT end_recovery_until FROM conversation_session_links WHERE session_security_id = ?", contextId);
    }

    private OffsetDateTime time(String sql, UUID id) {
        return jdbc.queryForObject(sql, (row, _) -> row.getObject(1, OffsetDateTime.class), id);
    }

    private UUID turn(Fixture fixture, String status, String child, String reply, OffsetDateTime deadline) {
        var id = UUID.randomUUID();
        int sequence = Objects.requireNonNull(jdbc.queryForObject(
                "SELECT COALESCE(MAX(sequence), 0) + 1 FROM conversation_turns WHERE conversation_id = ?",
                Integer.class, fixture.conversationId()), "다음 발화 순번이 필요합니다.");
        jdbc.update("""
                INSERT INTO conversation_turns (id, conversation_id, client_request_id, request_hash, sequence, status,
                    child_text, child_text_visibility, reply_text, reply_text_visibility, error_code, created_at,
                    processing_deadline_at, completed_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, id, fixture.conversationId(), UUID.randomUUID(), "a".repeat(64), sequence, status,
                child.equals("OMITTED") ? null : "허용 아이 텍스트", child, reply.equals("OMITTED") ? null : "허용 답변 텍스트", reply,
                status.equals("FAILED") ? "AI_UPSTREAM_FAILED" : null, START.plusSeconds(1), deadline,
                status.equals("PROCESSING") ? null : START.plusSeconds(2));
        return id;
    }

    private UUID link(Fixture fixture, String kind, OffsetDateTime boundAt) {
        var id = UUID.randomUUID();
        var accountId = kind.equals("OTHER_OWNER") ? UUID.randomUUID() : fixture.session().accountId();
        if (kind.equals("OTHER_OWNER")) {
            jdbc.update("INSERT INTO accounts (id, email) VALUES (?, ?)", accountId, accountId + "@example.invalid");
        }
        var expiresAt = switch (kind) {
            case "EXPIRED" -> NOW.minusNanos(1_000);
            case "EQUALITY" -> NOW;
            default -> NOW.plusSeconds(1);
        };
        jdbc.update("""
                INSERT INTO session_security (id, account_id, session_version, created_at, revoked_at, expires_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """, id, accountId, kind.equals("VERSION") ? 1L : 0L, START.minusDays(1),
                kind.equals("REVOKED") ? START : null, expiresAt);
        jdbc.update("INSERT INTO conversation_session_links (session_security_id, conversation_id, bound_at) VALUES (?, ?, ?)",
                id, fixture.conversationId(), boundAt);
        return id;
    }

    private Fixture fixture() {
        var accountId = UUID.randomUUID();
        var childId = UUID.randomUUID();
        var conversationId = UUID.randomUUID();
        var key = UUID.randomUUID();
        jdbc.update("INSERT INTO accounts (id, email) VALUES (?, ?)", accountId, accountId + "@example.invalid");
        var session = security.createLoginContext(accountId, null);
        jdbc.update("UPDATE session_security SET created_at = ? WHERE id = ?", START.minusDays(1), session.securityContextId());
        jdbc.update("""
                INSERT INTO children (id, account_id, name, nickname, birth_date, gender, character_id)
                VALUES (?, ?, '가나', '별이', DATE '2020-05-12', 'FEMALE', 'test-character')
                """, childId, accountId);
        jdbc.update("""
                INSERT INTO conversations (id, child_id, client_request_id, service_date, scheduled_end_at, status, summary_status, started_at)
                VALUES (?, ?, ?, ?, ?, 'ACTIVE', 'NOT_STARTED', ?)
                """, conversationId, childId, key, START.toLocalDate(), MIDNIGHT, START);
        jdbc.update("INSERT INTO conversation_session_links (session_security_id, conversation_id, bound_at) VALUES (?, ?, ?)",
                session.securityContextId(), conversationId, START);
        return new Fixture(session, childId, conversationId, key);
    }

    private record Fixture(AuthenticatedSession session, UUID childId, UUID conversationId, UUID startKey) { }
}
