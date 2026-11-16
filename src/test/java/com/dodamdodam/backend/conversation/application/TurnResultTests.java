package com.dodamdodam.backend.conversation.application;

import com.dodamdodam.backend.TestcontainersConfiguration;
import com.dodamdodam.backend.auth.session.AuthenticatedSession;
import com.dodamdodam.backend.auth.session.SessionSecurityRepository;
import com.dodamdodam.backend.child.exception.ChildException;
import com.dodamdodam.backend.conversation.application.AllowedTextPolicy.AllowedText;
import com.dodamdodam.backend.conversation.application.ConversationClosureCoordinator.ClosureResult;
import com.dodamdodam.backend.conversation.application.TurnAcceptanceService.Acceptance;
import com.dodamdodam.backend.conversation.application.TurnAcceptanceService.AcceptedTurn;
import com.dodamdodam.backend.conversation.domain.TextVisibility;
import com.dodamdodam.backend.conversation.domain.TurnStatus;
import com.dodamdodam.backend.conversation.dto.StartConversationRequest;
import com.dodamdodam.backend.conversation.exception.ConversationException;
import com.dodamdodam.backend.conversation.infrastructure.ConversationClosureRepository;
import com.dodamdodam.backend.conversation.infrastructure.ConversationRepository;
import com.dodamdodam.backend.conversation.infrastructure.ConversationTurnRepository;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.AopTestUtils;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

/** 결과 저장과 ACTIVE 만료·공통 종료 정리를 실제 PostgreSQL에서 검증한다. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = {"spring.session.jdbc.cleanup-cron=-"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Timeout(30)
class TurnResultTests {

    private static final OffsetDateTime CREATED_AT = OffsetDateTime.parse("2026-10-03T14:00:00.123456+09:00");
    private static final byte[] AUDIO = "synthetic audio".getBytes(StandardCharsets.UTF_8);
    private static final String FORBIDDEN = "[test] forbidden original";
    private final AllowedTextPolicy policy = new AllowedTextPolicy();
    @Autowired private JdbcTemplate jdbc;
    @Autowired private TurnAcceptanceService acceptance;
    @Autowired private TurnResultService results;
    @Autowired private ConversationService starts;
    @Autowired private ConversationClosureCoordinator closure;
    @Autowired private SessionSecurityRepository security;
    @Autowired private PlatformTransactionManager transactions;
    @Autowired private JsonMapper mapper;
    @MockitoSpyBean private ConversationRepository conversations;
    @MockitoSpyBean private ConversationTurnRepository turns;
    @MockitoSpyBean private ConversationClosureRepository closureRepository;

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
        // 시간 경계만 고정하며 저장·권한·잠금·종료·허용 정책은 실제 구현을 사용한다.
        now(CREATED_AT);
    }

    @Test
    void savesSuccessWithDatabaseTimeAndNoErrorCode() {
        var fixture = fixture();
        var checkedAt = CREATED_AT.plusSeconds(1);
        now(checkedAt);
        var result = results.succeed(permit(fixture), visible("[test] child"), redacted("[test] processed reply"));

        assertThat(result.changed()).isTrue();
        assertThat(result.status()).isEqualTo(TurnStatus.SUCCEEDED);
        assertThat(result.closure().state().status()).isEqualTo("ACTIVE");
        assertThat(result.closure().endedNow()).isFalse();
        assertThat(turnRow(fixture)).containsEntry("error_code", null).containsEntry("status", "SUCCEEDED")
                .containsEntry("child_text", "[test] child").containsEntry("reply_text", "[test] processed reply");
        assertThat(completedAt(fixture).toInstant()).isEqualTo(checkedAt.toInstant());
        assertOriginalDeadline(fixture);
    }

    @ParameterizedTest
    @ValueSource(strings = {"STT_NO_SPEECH", "AI_UPSTREAM_FAILED"})
    void preservesIndependentlyAllowedTextForDefinedFailures(String code) {
        var fixture = fixture();
        now(CREATED_AT.plusSeconds(1));
        var result = results.fail(permit(fixture), code, redacted("[test] processed child"), visible("[test] reply"));
        assertThat(result.status()).isEqualTo(TurnStatus.FAILED);
        assertThat(result.changed()).isTrue();
        assertThat(turnRow(fixture)).containsEntry("error_code", code).containsEntry("child_text", "[test] processed child")
                .containsEntry("child_text_visibility", "REDACTED").containsEntry("reply_text", "[test] reply")
                .containsEntry("reply_text_visibility", "VISIBLE");
        assertThat(completedAt(fixture)).isNotNull();
        assertOriginalDeadline(fixture);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "UNKNOWN", "AI_TIMEOUT", FORBIDDEN})
    void rejectsUndefinedFailureCodesWithoutKeepingInputInError(String code) {
        var fixture = fixture();
        var failure = assertThrows(ConversationException.class, () -> results.fail(permit(fixture), code, null, null));
        assertThat(failure.code()).isEqualTo("VALIDATION_FAILED");
        assertThat(failure.toString()).doesNotContain(FORBIDDEN);
        assertThat(turnRow(fixture)).containsEntry("status", "PROCESSING").containsEntry("error_code", null);
    }

    @ParameterizedTest
    @EnumSource(value = TurnStatus.class, names = {"SUCCEEDED", "FAILED"})
    void terminalRowsCannotBeOverwrittenByLateOrConflictingResults(TurnStatus firstStatus) {
        var fixture = fixture();
        now(CREATED_AT.plusSeconds(1));
        settle(fixture, firstStatus, visible("[test] first child"), null);
        var snapshot = turnRow(fixture);
        now(fixture.accepted().processingDeadlineAt().plusSeconds(1));

        assertThat(results.succeed(permit(fixture), visible("[test] late child"), visible("[test] late reply")).changed()).isFalse();
        assertThat(results.fail(permit(fixture), "AI_UPSTREAM_FAILED", null, visible("[test] late failure")).changed()).isFalse();
        assertThat(results.expireOverdue(fixture.session().accountId(), fixture.childId(), fixture.conversationId()).expired()).isZero();
        assertThat(turnRow(fixture)).isEqualTo(snapshot);
        assertOriginalDeadline(fixture);
    }

    @Test
    void acceptsSuccessAtLastMicrosecondBeforeOriginalDeadline() {
        var fixture = fixture();
        now(fixture.accepted().processingDeadlineAt().minusNanos(1_000));
        assertThat(results.succeed(permit(fixture), visible("[test] in time"), null).status()).isEqualTo(TurnStatus.SUCCEEDED);
        assertThat(completedAt(fixture)).isBefore(fixture.accepted().processingDeadlineAt());
    }

    @ParameterizedTest
    @ValueSource(longs = {0, 1_000})
    void lateSuccessOrFailureBecomesTimeoutWithoutSavingItsText(long nanosAfterDeadline) {
        var fixture = fixture();
        var checkedAt = fixture.accepted().processingDeadlineAt().plusNanos(nanosAfterDeadline);
        now(checkedAt);
        var outcome = results.fail(permit(fixture), "AI_UPSTREAM_FAILED", visible("[test] late permitted"), null);
        assertThat(outcome.status()).isEqualTo(TurnStatus.FAILED);
        assertThat(outcome.changed()).isTrue();
        assertThat(turnRow(fixture)).containsEntry("error_code", "AI_TIMEOUT").containsEntry("child_text", null)
                .containsEntry("child_text_visibility", "OMITTED").containsEntry("reply_text", null);
        assertThat(completedAt(fixture).toInstant()).isEqualTo(checkedAt.toInstant());
        assertThat(results.succeed(permit(fixture), null, visible("[test] later reply")).changed()).isFalse();
        assertOriginalDeadline(fixture);
    }

    @ParameterizedTest
    @ValueSource(longs = {-1_000, 0, 1_000})
    void expiresActiveAtDeadlineEqualityWithoutEndingConversation(long nanosAfterDeadline) {
        var fixture = fixture();
        var checkedAt = fixture.accepted().processingDeadlineAt().plusNanos(nanosAfterDeadline);
        now(checkedAt);
        var expiration = results.expireOverdue(fixture.session().accountId(), fixture.childId(), fixture.conversationId());
        assertThat(expiration.expired()).isEqualTo(nanosAfterDeadline < 0 ? 0 : 1);
        assertThat(expiration.closure().state().status()).isEqualTo("ACTIVE");
        assertThat(expiration.closure().state().summaryStatus()).isEqualTo("NOT_STARTED");
        assertThat(expiration.closure().state().endRequestedAt()).isNull();
        assertThat(expiration.closure().state().endedAt()).isNull();
        assertThat(turnRow(fixture)).containsEntry("status", nanosAfterDeadline < 0 ? "PROCESSING" : "FAILED")
                .containsEntry("error_code", nanosAfterDeadline < 0 ? null : "AI_TIMEOUT");
        assertOriginalDeadline(fixture);
    }

    @ParameterizedTest
    @MethodSource("visibilityCases")
    void storesOnlyPermittedRepresentationForEachText(TurnStatus status, TextVisibility childVisibility, TextVisibility replyVisibility) {
        var fixture = fixture();
        var child = policy.select(childVisibility, true, true,
                childVisibility == TextVisibility.REDACTED ? "[test] processed child" : "[test] original child");
        var reply = policy.select(replyVisibility, true, true,
                replyVisibility == TextVisibility.REDACTED ? "[test] processed reply" : "[test] original reply");
        now(CREATED_AT.plusSeconds(1));
        settle(fixture, status, child, reply);

        assertThat(turnRow(fixture)).containsEntry("child_text", child.text()).containsEntry("child_text_visibility", childVisibility.name())
                .containsEntry("reply_text", reply.text()).containsEntry("reply_text_visibility", replyVisibility.name());
        // 아이용 내용 조회는 현재 접근 검사를 다시 통과한 기존 키 재사용 경로로만 검증한다.
        var view = acceptance.accept(fixture.session(), fixture.childId(), fixture.conversationId(), fixture.key(), AUDIO).turn();
        assertThat(view.status()).isEqualTo(status);
        assertThat(view.childText()).isEqualTo(child.text());
        assertThat(view.replyText()).isEqualTo(reply.text());
        var json = json(view);
        assertThat(json).containsOnlyKeys("turnId", "sequence", "status", "childText", "childTextVisibility", "replyText",
                "replyTextVisibility", "createdAt", "completedAt", "errorCode");
        assertThat(json).containsEntry("createdAt", "2026-10-03T14:00:00+09:00")
                .containsEntry("completedAt", "2026-10-03T14:00:01+09:00");
    }

    private static Stream<Arguments> visibilityCases() {
        return Stream.of(TurnStatus.SUCCEEDED, TurnStatus.FAILED).flatMap(status ->
                Stream.of(TextVisibility.values()).flatMap(child -> Stream.of(TextVisibility.values())
                        .map(reply -> Arguments.of(status, child, reply))));
    }

    @ParameterizedTest
    @CsvSource({"false,true,true,true", "true,false,true,true", ",true,true,true", "true,,true,true",
            "true,true,false,true", "true,true,true,false", "true,true,,true", "true,true,true,"})
    void rejectedOriginalIsOmittedIndependentlyAndNeverAppearsInDiagnostics(
            Boolean childStorage, Boolean childDisplay, Boolean replyStorage, Boolean replyDisplay) {
        var fixture = fixture();
        var child = policy.select(TextVisibility.VISIBLE, childStorage, childDisplay,
                Boolean.TRUE.equals(childStorage) && Boolean.TRUE.equals(childDisplay) ? "[test] allowed child" : FORBIDDEN);
        var reply = policy.select(TextVisibility.REDACTED, replyStorage, replyDisplay,
                Boolean.TRUE.equals(replyStorage) && Boolean.TRUE.equals(replyDisplay) ? "[test] allowed processed reply" : FORBIDDEN);
        now(CREATED_AT.plusSeconds(1));
        var result = results.succeed(permit(fixture), child, reply);
        assertThat(turnRow(fixture)).containsEntry("child_text", child.text()).containsEntry("reply_text", reply.text());
        assertThat(turnRow(fixture).values().toString()).doesNotContain(FORBIDDEN);
        assertThat(List.of(child.toString(), reply.toString(), result.toString(), permit(fixture).toString()).toString()).doesNotContain(FORBIDDEN);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void absentOrRejectedTextRemainsOmittedAndProducesEmptySummary(boolean missingText) {
        var fixture = fixture();
        requestEnd(fixture, CREATED_AT.plusSeconds(1));
        now(CREATED_AT.plusSeconds(2));
        var child = missingText ? policy.select(TextVisibility.REDACTED, true, true, null)
                : policy.select(TextVisibility.VISIBLE, false, true, FORBIDDEN);
        var reply = missingText ? null : policy.select(TextVisibility.REDACTED, true, false, FORBIDDEN);
        var outcome = results.succeed(permit(fixture), child, reply);
        assertThat(outcome.closure().state().summaryStatus()).isEqualTo("EMPTY");
        assertThat(outcome.closure().endedNow()).isTrue();
        assertThat(turnRow(fixture)).containsEntry("child_text", null).containsEntry("reply_text", null)
                .containsEntry("child_text_visibility", "OMITTED").containsEntry("reply_text_visibility", "OMITTED");
        var hasSummaryInput = transaction().execute(_ -> closureRepository.hasSummaryInput(fixture.conversationId()));
        assertThat(hasSummaryInput).isFalse();
        assertThat(turnRow(fixture).values().toString()).doesNotContain(FORBIDDEN);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM conversation_turns", Integer.class)).isEqualTo(1);
    }

    @ParameterizedTest
    @CsvSource({"SUCCEEDED,false,false,EMPTY", "FAILED,false,false,EMPTY", "FAILED,true,false,PENDING",
            "FAILED,false,true,PENDING", "SUCCEEDED,true,true,PENDING"})
    void closingResultAtomicallyFinishesConversationWithAllowedSummaryInput(
            TurnStatus status, boolean childAllowed, boolean replyAllowed, String summaryStatus) {
        var fixture = fixture();
        var boundary = CREATED_AT.plusSeconds(1);
        requestEnd(fixture, boundary);
        now(fixture.accepted().processingDeadlineAt().minusNanos(1_000));
        var outcome = settle(fixture, status, childAllowed ? visible("[test] child") : null,
                replyAllowed ? redacted("[test] processed reply") : null);

        assertThat(outcome.closure().endedNow()).isTrue();
        assertThat(outcome.closure().state().status()).isEqualTo("ENDED");
        assertThat(outcome.closure().state().summaryStatus()).isEqualTo(summaryStatus);
        assertThat(outcome.closure().state().endReason()).isEqualTo("MANUAL");
        assertThat(outcome.closure().state().endRequestedAt()).isEqualTo(boundary);
        assertThat(outcome.closure().state().endedAt()).isAfterOrEqualTo(completedAt(fixture));
        var recovery = jdbc.queryForObject("SELECT end_recovery_until FROM conversation_session_links WHERE conversation_id = ?",
                (row, _) -> row.getObject(1, OffsetDateTime.class), fixture.conversationId());
        assertThat(recovery).isEqualTo(outcome.closure().state().endedAt().plusSeconds(600));
        assertOriginalDeadline(fixture);
        var failure = assertThrows(ConversationException.class,
                () -> acceptance.accept(fixture.session(), fixture.childId(), fixture.conversationId(), UUID.randomUUID(), AUDIO));
        assertThat(failure.code()).isEqualTo("CONVERSATION_ENDED");
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void midnightAllowsPreviouslyAcceptedStorageButPreservesFirstEndBoundary(boolean manualFirst) {
        var acceptedAt = OffsetDateTime.parse("2026-10-03T23:59:50+09:00");
        var fixture = fixture(acceptedAt);
        var manualBoundary = acceptedAt.plusSeconds(5);
        if (manualFirst) {
            requestEnd(fixture, manualBoundary);
        }
        now(OffsetDateTime.parse("2026-10-04T00:00:01+09:00"));
        var result = results.succeed(permit(fixture), visible("[test] original child"), null);
        assertThat(result.status()).isEqualTo(TurnStatus.SUCCEEDED);
        assertThat(result.closure().state().status()).isEqualTo("ENDED");
        assertThat(result.closure().state().endReason()).isEqualTo(manualFirst ? "MANUAL" : "MIDNIGHT");
        assertThat(result.closure().state().endRequestedAt()).isEqualTo(manualFirst ? manualBoundary
                : OffsetDateTime.parse("2026-10-04T00:00:00+09:00"));
        var rejected = assertThrows(ConversationException.class,
                () -> acceptance.accept(fixture.session(), fixture.childId(), fixture.conversationId(), fixture.key(), AUDIO));
        assertThat(rejected.code()).isEqualTo("CONVERSATION_ENDED");
        assertOriginalDeadline(fixture);
    }

    @ParameterizedTest
    @CsvSource({"EXPIRED,false", "REVOKED,false", "VERSION,false", "EXPIRED,true", "REVOKED,true", "VERSION,true"})
    void acceptedStorageSurvivesSessionInvalidationButContentAccessDoesNot(String kind, boolean closing) {
        var fixture = fixture();
        if (closing) {
            requestEnd(fixture, CREATED_AT.plusSeconds(1));
        }
        switch (kind) {
            case "EXPIRED" -> jdbc.update("""
                    UPDATE session_security SET created_at = ?, expires_at = ? WHERE id = ?
                    """, CREATED_AT.minusSeconds(2), CREATED_AT.minusSeconds(1), fixture.session().securityContextId());
            case "REVOKED" -> security.revokeCurrentContext(fixture.session().accountId(), fixture.session().securityContextId());
            case "VERSION" -> jdbc.update("UPDATE accounts SET session_version = session_version + 1 WHERE id = ?", fixture.session().accountId());
            default -> throw new AssertionError(kind);
        }
        now(CREATED_AT.plusSeconds(2));
        var outcome = results.fail(permit(fixture), "AI_UPSTREAM_FAILED", visible("[test] allowed child"), null);
        assertThat(outcome.status()).isEqualTo(TurnStatus.FAILED);
        assertThat(turnRow(fixture)).containsEntry("child_text", "[test] allowed child");
        assertThatThrownBy(() -> acceptance.accept(fixture.session(), fixture.childId(), fixture.conversationId(), fixture.key(), AUDIO))
                .isInstanceOf(InsufficientAuthenticationException.class);
        assertThat(json(outcome)).containsOnlyKeys("turnId", "status", "changed", "closure");
        if (closing) {
            assertThat(outcome.closure().state().status()).isEqualTo("ENDED");
            assertThat(outcome.closure().state().summaryStatus()).isEqualTo("PENDING");
            assertThat(jdbc.queryForMap("SELECT end_recovery_until FROM conversation_session_links WHERE conversation_id = ?",
                    fixture.conversationId())).containsEntry("end_recovery_until", null);
        }
    }

    @ParameterizedTest
    @EnumSource(value = TurnStatus.class, names = {"SUCCEEDED", "FAILED"})
    void conditionalUpdateItselfRejectsTerminalRows(TurnStatus terminal) {
        var fixture = fixture();
        now(CREATED_AT.plusSeconds(1));
        settle(fixture, terminal, visible("[test] original permitted child"), null);
        var snapshot = turnRow(fixture);
        int affected = Objects.requireNonNull(transaction().execute(_ -> {
            conversations.findForUpdate(fixture.childId(), fixture.conversationId()).orElseThrow();
            turns.findForUpdate(fixture.conversationId(), fixture.accepted().turn().turnId()).orElseThrow();
            return turns.completeProcessing(fixture.conversationId(), fixture.accepted().turn().turnId(), TurnStatus.FAILED,
                    "AI_UPSTREAM_FAILED", visible("[test] replacement"), visible("[test] replacement reply"), CREATED_AT.plusSeconds(2));
        }));
        assertThat(affected).isZero();
        assertThat(turnRow(fixture)).isEqualTo(snapshot);
    }

    @Test
    void conditionalUpdateItselfRejectsDeadlineEquality() {
        var fixture = fixture();
        int affected = Objects.requireNonNull(transaction().execute(_ -> {
            conversations.findForUpdate(fixture.childId(), fixture.conversationId()).orElseThrow();
            turns.findForUpdate(fixture.conversationId(), fixture.accepted().turn().turnId()).orElseThrow();
            return turns.completeProcessing(fixture.conversationId(), fixture.accepted().turn().turnId(), TurnStatus.SUCCEEDED,
                    null, visible("[test] late"), visible("[test] late reply"), fixture.accepted().processingDeadlineAt());
        }));
        assertThat(affected).isZero();
        assertThat(turnRow(fixture)).containsEntry("status", "PROCESSING").containsEntry("child_text", null);
    }

    @Test
    void closingTimeoutFinishesEmptyOnceAndKeepsOriginalEndAndDeadline() {
        var fixture = fixture();
        requestEnd(fixture, CREATED_AT.plusSeconds(1));
        now(fixture.accepted().processingDeadlineAt());
        var expired = results.expireOverdue(fixture.session().accountId(), fixture.childId(), fixture.conversationId());
        assertThat(expired.expired()).isEqualTo(1);
        assertThat(expired.closure().endedNow()).isTrue();
        assertThat(expired.closure().state().status()).isEqualTo("ENDED");
        assertThat(expired.closure().state().summaryStatus()).isEqualTo("EMPTY");
        now(fixture.accepted().processingDeadlineAt().plusSeconds(10));
        var replay = results.expireOverdue(fixture.session().accountId(), fixture.childId(), fixture.conversationId());
        assertThat(replay.expired()).isZero();
        assertThat(replay.closure().endedNow()).isFalse();
        assertThat(replay.closure().state()).isEqualTo(expired.closure().state());
        assertOriginalDeadline(fixture);
    }

    @Test
    void concurrentCallbacksChooseOneTerminalResult() throws Exception {
        var fixture = fixture();
        now(CREATED_AT.plusSeconds(1));
        var start = new CountDownLatch(1);
        try (var workers = Executors.newFixedThreadPool(2)) {
            var success = workers.submit(() -> { start.await(); return results.succeed(permit(fixture), visible("[test] success child"), null); });
            var failure = workers.submit(() -> { start.await(); return results.fail(permit(fixture), "AI_UPSTREAM_FAILED", visible("[test] failure child"), null); });
            start.countDown();
            assertThat((success.get(10, TimeUnit.SECONDS).changed() ? 1 : 0) + (failure.get(10, TimeUnit.SECONDS).changed() ? 1 : 0)).isEqualTo(1);
        }
        var row = turnRow(fixture);
        assertThat(row).containsEntry("child_text", row.get("status").equals("SUCCEEDED") ? "[test] success child" : "[test] failure child");
    }

    @Test
    void onlyOriginalNewAcceptanceGetsAResultStorageContext() {
        var fixture = fixture();
        var replay = acceptance.accept(fixture.session(), fixture.childId(), fixture.conversationId(), fixture.key(), AUDIO);
        assertThat(fixture.accepted().completionPermit()).isPresent();
        assertThat(replay.completionPermit()).isEmpty();
        assertThatThrownBy(() -> results.succeed(replay.completionPermit().orElse(null), null, null)).isInstanceOf(NullPointerException.class);
        assertThat(turnRow(fixture)).containsEntry("status", "PROCESSING");
    }

    @Test
    void externalJsonCannotConstructResultStorageContextFromIds() {
        var fixture = fixture();
        var input = mapper.writeValueAsString(Map.of("accountId", fixture.session().accountId(),
                "securityContextId", fixture.session().securityContextId(), "childId", fixture.childId(),
                "conversationId", fixture.conversationId(), "turnId", fixture.accepted().turn().turnId()));
        assertThatThrownBy(() -> mapper.readValue(input, AcceptedTurn.class)).isInstanceOf(Exception.class);
        assertThat(turnRow(fixture)).containsEntry("status", "PROCESSING");
    }

    @Test
    void completionPermitCannotRetargetAnotherConversation() {
        var first = fixture();
        var other = fixture();
        now(CREATED_AT.plusSeconds(1));
        results.succeed(permit(first), null, visible("[test] first reply"));
        assertThat(turnRow(first)).containsEntry("reply_text", "[test] first reply");
        assertThat(turnRow(other)).containsEntry("status", "PROCESSING").containsEntry("reply_text", null);
    }

    @Test
    void maintenanceChecksAccountChildConversationRelationship() {
        var first = fixture();
        var other = fixture();
        now(first.accepted().processingDeadlineAt());
        assertThatThrownBy(() -> results.expireOverdue(first.session().accountId(), first.childId(), other.conversationId()))
                .isInstanceOf(ChildException.class);
        assertThat(turnRow(first)).containsEntry("status", "PROCESSING");
        assertThat(turnRow(other)).containsEntry("status", "PROCESSING");
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void successAndTimeoutCompeteWithoutOverwritingEachOther(boolean beforeDeadline) throws Exception {
        var fixture = fixture();
        now(beforeDeadline ? fixture.accepted().processingDeadlineAt().minusNanos(1_000) : fixture.accepted().processingDeadlineAt());
        var start = new CountDownLatch(1);
        try (var workers = Executors.newFixedThreadPool(2)) {
            var success = workers.submit(() -> { start.await(); return results.succeed(permit(fixture), visible("[test] child"), null); });
            var timeout = workers.submit(() -> { start.await(); return results.expireOverdue(fixture.session().accountId(), fixture.childId(), fixture.conversationId()); });
            start.countDown();
            var settled = success.get(10, TimeUnit.SECONDS);
            var expired = timeout.get(10, TimeUnit.SECONDS);
            assertThat((settled.changed() ? 1 : 0) + expired.expired()).isEqualTo(1);
        }
        assertThat(turnRow(fixture)).containsEntry("status", beforeDeadline ? "SUCCEEDED" : "FAILED")
                .containsEntry("error_code", beforeDeadline ? null : "AI_TIMEOUT")
                .containsEntry("child_text", beforeDeadline ? "[test] child" : null);
        assertOriginalDeadline(fixture);
    }

    @Test
    void readsDatabaseTimeAfterWaitingForTurnLock() throws Exception {
        var fixture = fixture();
        var clock = new AtomicReference<>(fixture.accepted().processingDeadlineAt().minusNanos(1_000));
        doAnswer(_ -> clock.get()).when(conversations).currentTime();
        var entered = new CompletableFuture<Integer>();
        doAnswer(invocation -> {
            entered.complete(backendPid());
            return invocation.callRealMethod();
        }).when(turnSpy()).findForUpdate(any(), any());
        try (var worker = Executors.newSingleThreadExecutor()) {
            var pending = transaction().execute(_ -> {
                int blocker = backendPid();
                jdbc.queryForList("SELECT id FROM conversation_turns WHERE id = ? FOR UPDATE", fixture.accepted().turn().turnId());
                var attempt = worker.submit(() -> results.succeed(permit(fixture), visible("[test] late result"), null));
                awaitBlocked(entered.orTimeout(5, TimeUnit.SECONDS).join(), blocker);
                clock.set(fixture.accepted().processingDeadlineAt());
                return attempt;
            });
            assertThat(pending.get(10, TimeUnit.SECONDS).status()).isEqualTo(TurnStatus.FAILED);
        }
        assertThat(turnRow(fixture)).containsEntry("error_code", "AI_TIMEOUT").containsEntry("child_text", null);
    }

    @Test
    void resultAndManualEndSerializeAndFinishOnce() throws Exception {
        var fixture = fixture();
        now(CREATED_AT.plusSeconds(1));
        var start = new CountDownLatch(1);
        try (var workers = Executors.newFixedThreadPool(2)) {
            var complete = workers.submit(() -> { start.await(); return results.fail(permit(fixture), "AI_UPSTREAM_FAILED", visible("[test] child"), null); });
            var end = workers.submit(() -> { start.await(); return closure.reconcile(fixture.session().accountId(), fixture.childId(), fixture.conversationId(),
                    ConversationClosureCoordinator.Trigger.REQUEST_END); });
            start.countDown();
            var saved = complete.get(10, TimeUnit.SECONDS);
            var ended = end.get(10, TimeUnit.SECONDS);
            assertThat((saved.closure().endedNow() ? 1 : 0) + (ended.endedNow() ? 1 : 0)).isEqualTo(1);
        }
        assertThat(jdbc.queryForMap("SELECT status, summary_status FROM conversations WHERE id = ?", fixture.conversationId()))
                .containsEntry("status", "ENDED").containsEntry("summary_status", "PENDING");
        assertThat(turnRow(fixture)).containsEntry("status", "FAILED").containsEntry("child_text", "[test] child");
    }

    @Test
    void rollsBackResultAndClosureTogetherWhenRecoveryStorageFails() {
        var fixture = fixture();
        requestEnd(fixture, CREATED_AT.plusSeconds(1));
        now(CREATED_AT.plusSeconds(2));
        doThrow(new ConversationException(new DataAccessResourceFailureException("합성 복구 저장 장애")))
                .when(closureSpy()).grantRecovery(any(), any(), any(), any());
        assertThrows(ConversationException.class, () -> results.succeed(permit(fixture), visible("[test] child"), null));
        assertThat(turnRow(fixture)).containsEntry("status", "PROCESSING").containsEntry("child_text", null).containsEntry("completed_at", null);
        assertThat(jdbc.queryForMap("SELECT status, summary_status, ended_at FROM conversations WHERE id = ?", fixture.conversationId()))
                .containsEntry("status", "CLOSING").containsEntry("summary_status", "NOT_STARTED").containsEntry("ended_at", null);
        doCallRealMethod().when(closureSpy()).grantRecovery(any(), any(), any(), any());
        assertThat(results.succeed(permit(fixture), visible("[test] retry permitted child"), null).closure().endedNow()).isTrue();
        assertOriginalDeadline(fixture);
    }

    @Test
    void resultAndExpirationRequireAnIndependentCommitBoundary() {
        var fixture = fixture();
        assertThatThrownBy(() -> transaction().execute(_ -> results.succeed(permit(fixture), null, null)))
                .isInstanceOf(IllegalTransactionStateException.class);
        assertThatThrownBy(() -> transaction().execute(_ -> results.expireOverdue(fixture.session().accountId(), fixture.childId(), fixture.conversationId())))
                .isInstanceOf(IllegalTransactionStateException.class);
        assertThat(turnRow(fixture)).containsEntry("status", "PROCESSING");
    }

    @ParameterizedTest
    @CsvSource({"RESULT,true", "END,true", "TIMEOUT,true", "RESULT,false", "END,false", "TIMEOUT,false"})
    void threeWayRaceFollowsParentLocksAndIssuesOneEndWithFixedRecovery(String first, boolean beforeDeadline) throws Exception {
        var fixture = fixture();
        var checkedAt = beforeDeadline ? fixture.accepted().processingDeadlineAt().minusNanos(1_000)
                : fixture.accepted().processingDeadlineAt();
        var linked = security.createLoginContext(fixture.session().accountId(), null);
        var onBoundary = security.createLoginContext(fixture.session().accountId(), null);
        link(linked, fixture, CREATED_AT);
        link(onBoundary, fixture, checkedAt);
        now(checkedAt);

        var reserved = new AtomicBoolean();
        var firstLocked = new CompletableFuture<Integer>();
        var waitingPids = new ConcurrentLinkedQueue<Integer>();
        var othersEntered = new CountDownLatch(2);
        var release = new CountDownLatch(1);
        doAnswer(invocation -> {
            int pid = backendPid();
            boolean leading = reserved.compareAndSet(false, true);
            if (!leading) {
                waitingPids.add(pid);
                othersEntered.countDown();
            }
            var locked = invocation.callRealMethod();
            if (leading) {
                firstLocked.complete(pid);
                assertThat(release.await(10, TimeUnit.SECONDS)).isTrue();
            }
            return locked;
        }).when(closureSpy()).lockAccount(any());

        Map<String, Callable<ClosureResult>> operations = Map.of(
                "RESULT", () -> results.succeed(permit(fixture), visible("[test] permitted child"), null).closure(),
                "END", () -> closure.reconcile(fixture.session().accountId(), fixture.childId(), fixture.conversationId(),
                        ConversationClosureCoordinator.Trigger.REQUEST_END),
                "TIMEOUT", () -> results.expireOverdue(fixture.session().accountId(), fixture.childId(), fixture.conversationId()).closure());
        try (var workers = Executors.newFixedThreadPool(3)) {
            try {
                var leader = workers.submit(operations.get(first));
                int blocker = firstLocked.orTimeout(5, TimeUnit.SECONDS).join();
                // 계정을 먼저 잡은 시점에는 하위 문맥·아이·대화를 먼저 보유하지 않아야 한다.
                transaction().executeWithoutResult(_ -> {
                    jdbc.queryForList("SELECT id FROM session_security WHERE id = ? FOR UPDATE NOWAIT", fixture.session().securityContextId());
                    jdbc.queryForList("SELECT id FROM children WHERE id = ? FOR UPDATE NOWAIT", fixture.childId());
                    jdbc.queryForList("SELECT id FROM conversations WHERE id = ? FOR UPDATE NOWAIT", fixture.conversationId());
                });
                var followers = operations.entrySet().stream().filter(entry -> !entry.getKey().equals(first))
                        .map(entry -> workers.submit(entry.getValue())).toList();
                assertThat(othersEntered.await(5, TimeUnit.SECONDS)).isTrue();
                // 두 독립 Tx가 실제 PostgreSQL 계정 잠금에서 대기한 뒤에만 첫 실행자를 진행시킨다.
                for (var pid : waitingPids) {
                    awaitBlockedThroughQueue(pid, blocker);
                }
                release.countDown();
                int endedNow = leader.get(10, TimeUnit.SECONDS).endedNow() ? 1 : 0;
                for (var follower : followers) {
                    endedNow += follower.get(10, TimeUnit.SECONDS).endedNow() ? 1 : 0;
                }
                assertThat(endedNow).isEqualTo(1);
            } finally {
                release.countDown();
            }
        }
        var conversation = jdbc.queryForMap("SELECT * FROM conversations WHERE id = ?", fixture.conversationId());
        var turn = turnRow(fixture);
        assertThat(conversation).containsEntry("status", "ENDED").containsEntry("end_reason", "MANUAL")
                .containsEntry("summary_status", beforeDeadline ? "PENDING" : "EMPTY");
        assertThat(turn).containsEntry("status", beforeDeadline ? "SUCCEEDED" : "FAILED")
                .containsEntry("error_code", beforeDeadline ? null : "AI_TIMEOUT")
                .containsEntry("child_text", beforeDeadline ? "[test] permitted child" : null);
        var until = checkedAt.plusSeconds(600);
        assertThat(recovery(fixture.session())).isEqualTo(until);
        assertThat(recovery(linked)).isEqualTo(until);
        assertThat(recovery(onBoundary)).isNull();
        assertOriginalDeadline(fixture);

        now(checkedAt.plusSeconds(10));
        assertThat(results.fail(permit(fixture), "AI_UPSTREAM_FAILED", null, null).changed()).isFalse();
        results.expireOverdue(fixture.session().accountId(), fixture.childId(), fixture.conversationId());
        assertThat(jdbc.queryForMap("SELECT * FROM conversations WHERE id = ?", fixture.conversationId())).isEqualTo(conversation);
        assertThat(turnRow(fixture)).isEqualTo(turn);
        assertThat(recovery(linked)).isEqualTo(until);
    }

    @Test
    void oldCallbackCannotModifyNewConversationStartedWhileOldSummaryIsPending() {
        var fixture = fixture();
        requestEnd(fixture, CREATED_AT.plusSeconds(1));
        now(CREATED_AT.plusSeconds(2));
        assertThat(results.fail(permit(fixture), "AI_UPSTREAM_FAILED", visible("[test] old child"), null)
                .closure().state().summaryStatus()).isEqualTo("PENDING");
        var oldRow = turnRow(fixture);
        var restarted = starts.create(fixture.session(), fixture.childId(), new StartConversationRequest(UUID.randomUUID()));
        assertThat(restarted.created()).isTrue();
        assertThat(restarted.result().conversationId()).isNotEqualTo(fixture.conversationId());
        var next = acceptance.accept(fixture.session(), fixture.childId(), restarted.result().conversationId(), UUID.randomUUID(), AUDIO);
        assertThat(next.turn().sequence()).isEqualTo(1);
        now(CREATED_AT.plusSeconds(3));

        assertThat(results.succeed(permit(fixture), null, visible("[test] old late reply")).changed()).isFalse();
        results.expireOverdue(fixture.session().accountId(), fixture.childId(), fixture.conversationId());
        assertThat(turnRow(fixture)).isEqualTo(oldRow);
        assertThat(jdbc.queryForMap("SELECT * FROM conversation_turns WHERE id = ?", next.turn().turnId()))
                .containsEntry("status", "PROCESSING").containsEntry("sequence", 1)
                .containsEntry("child_text", null).containsEntry("reply_text", null);
        var nextDeadline = jdbc.queryForObject("SELECT processing_deadline_at FROM conversation_turns WHERE id = ?",
                (row, _) -> row.getObject(1, OffsetDateTime.class), next.turn().turnId());
        assertThat(nextDeadline).isEqualTo(next.processingDeadlineAt());
    }

    @Test
    void acceptedResultSurvivesOriginalContextCleanupButNewLoginCannotReadWithoutLink() {
        var fixture = fixture();
        jdbc.update("DELETE FROM conversation_session_links WHERE session_security_id = ?", fixture.session().securityContextId());
        jdbc.update("DELETE FROM session_security WHERE id = ?", fixture.session().securityContextId());
        now(CREATED_AT.plusSeconds(1));
        assertThat(results.succeed(permit(fixture), visible("[test] retained child"), null).status()).isEqualTo(TurnStatus.SUCCEEDED);
        var newLogin = security.createLoginContext(fixture.session().accountId(), null);
        var denied = assertThrows(ConversationException.class,
                () -> acceptance.accept(newLogin, fixture.childId(), fixture.conversationId(), fixture.key(), AUDIO));
        assertThat(denied.code()).isEqualTo("CHILD_SESSION_REQUIRED");
        assertThat(turnRow(fixture)).containsEntry("child_text", "[test] retained child");
    }

    @Test
    void timeoutStoresTheExactUnmockedPostgresqlDecisionTime() {
        var databaseNow = jdbc.queryForObject("SELECT clock_timestamp()", (row, _) -> row.getObject(1, OffsetDateTime.class));
        var previousOpening = Objects.requireNonNull(databaseNow).atZoneSameInstant(ZoneId.of("Asia/Seoul"))
                .toLocalDate().minusDays(1).atTime(8, 0).atZone(ZoneId.of("Asia/Seoul")).toOffsetDateTime();
        var fixture = fixture(previousOpening);
        var decision = new AtomicReference<OffsetDateTime>();
        doAnswer(invocation -> {
            var observed = (OffsetDateTime) invocation.callRealMethod();
            decision.compareAndSet(null, observed);
            return observed;
        }).when(conversations).currentTime();

        var expired = results.expireOverdue(fixture.session().accountId(), fixture.childId(), fixture.conversationId());
        assertThat(expired.expired()).isEqualTo(1);
        assertThat(completedAt(fixture)).isEqualTo(decision.get());
        assertThat(completedAt(fixture)).isAfter(fixture.accepted().processingDeadlineAt());
        assertOriginalDeadline(fixture);
    }

    @Test
    @ExtendWith(OutputCaptureExtension.class)
    void rejectedOriginalNeverAppearsInDatabaseLogsOrValidationError(CapturedOutput output) {
        var fixture = fixture();
        now(CREATED_AT.plusSeconds(1));
        var child = policy.select(TextVisibility.VISIBLE, false, true, FORBIDDEN);
        var reply = policy.select(TextVisibility.REDACTED, true, null, FORBIDDEN);
        var settled = results.fail(permit(fixture), "AI_UPSTREAM_FAILED", child, reply);
        var invalid = assertThrows(ConversationException.class, () -> results.fail(permit(fixture), FORBIDDEN, child, reply));
        assertThat(turnRow(fixture).values().toString()).doesNotContain(FORBIDDEN);
        assertThat(invalid.toString()).doesNotContain(FORBIDDEN);
        assertThat(settled.toString()).doesNotContain(FORBIDDEN);
        assertThat(output.getAll()).doesNotContain(FORBIDDEN);
    }

    private void link(AuthenticatedSession session, Fixture fixture, OffsetDateTime boundAt) {
        jdbc.update("INSERT INTO conversation_session_links (session_security_id, conversation_id, bound_at) VALUES (?, ?, ?)",
                session.securityContextId(), fixture.conversationId(), boundAt);
    }

    private OffsetDateTime recovery(AuthenticatedSession session) {
        return jdbc.queryForObject("SELECT end_recovery_until FROM conversation_session_links WHERE session_security_id = ?",
                (row, _) -> row.getObject(1, OffsetDateTime.class), session.securityContextId());
    }

    private TurnResultService.Settlement settle(Fixture fixture, TurnStatus status, AllowedText child, AllowedText reply) {
        return status == TurnStatus.SUCCEEDED ? results.succeed(permit(fixture), child, reply)
                : results.fail(permit(fixture), "AI_UPSTREAM_FAILED", child, reply);
    }

    private void requestEnd(Fixture fixture, OffsetDateTime at) {
        now(at);
        var result = closure.reconcile(fixture.session().accountId(), fixture.childId(), fixture.conversationId(),
                ConversationClosureCoordinator.Trigger.REQUEST_END);
        assertThat(result.state().status()).isEqualTo("CLOSING");
    }

    private AllowedText visible(String text) { return policy.select(TextVisibility.VISIBLE, true, true, text); }

    private AllowedText redacted(String text) { return policy.select(TextVisibility.REDACTED, true, true, text); }

    private AcceptedTurn permit(Fixture fixture) { return fixture.accepted().completionPermit().orElseThrow(); }

    private void now(OffsetDateTime value) { doReturn(value).when(conversations).currentTime(); }

    private Map<String, Object> turnRow(Fixture fixture) {
        return jdbc.queryForMap("SELECT * FROM conversation_turns WHERE id = ?", fixture.accepted().turn().turnId());
    }

    private OffsetDateTime completedAt(Fixture fixture) {
        return jdbc.queryForObject("SELECT completed_at FROM conversation_turns WHERE id = ?",
                (row, _) -> row.getObject(1, OffsetDateTime.class), fixture.accepted().turn().turnId());
    }

    private void assertOriginalDeadline(Fixture fixture) {
        var deadline = jdbc.queryForObject("SELECT processing_deadline_at FROM conversation_turns WHERE id = ?",
                (row, _) -> row.getObject(1, OffsetDateTime.class), fixture.accepted().turn().turnId());
        assertThat(deadline).isEqualTo(fixture.accepted().processingDeadlineAt());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> json(Object value) {
        return mapper.readValue(mapper.writeValueAsString(value), Map.class);
    }

    private ConversationTurnRepository turnSpy() { return AopTestUtils.getUltimateTargetObject(turns); }

    private ConversationClosureRepository closureSpy() { return AopTestUtils.getUltimateTargetObject(closureRepository); }

    private void awaitBlocked(int worker, int blocker) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < deadline) {
            if (Boolean.TRUE.equals(jdbc.queryForObject("SELECT ? = ANY(pg_blocking_pids(?))", Boolean.class, blocker, worker))) {
                return;
            }
            Thread.yield();
        }
        throw new AssertionError("업무 처리가 지정한 PostgreSQL 행 잠금을 기다리지 않았습니다.");
    }

    private void awaitBlockedThroughQueue(int worker, int owner) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < deadline) {
            // 뒤 대기자는 앞 대기자의 tuple 잠금에 막힐 수 있으므로 최초 보유자까지 실제 대기 경로를 따라간다.
            var blocked = jdbc.queryForObject("""
                    WITH RECURSIVE blockers(pid) AS (
                        SELECT unnest(pg_blocking_pids(?))
                        UNION
                        SELECT unnest(pg_blocking_pids(blockers.pid)) FROM blockers
                    )
                    SELECT EXISTS (SELECT 1 FROM blockers WHERE pid = ?)
                        AND EXISTS (SELECT 1 FROM pg_stat_activity WHERE pid = ? AND wait_event_type = 'Lock'
                            AND query LIKE '%FROM accounts%FOR UPDATE%')
                    """, Boolean.class, worker, owner, worker);
            if (Boolean.TRUE.equals(blocked)) {
                return;
            }
            Thread.yield();
        }
        throw new AssertionError("계정 잠금의 대기 경로가 최초 PostgreSQL 잠금 보유자에 도달하지 않았습니다.");
    }

    private int backendPid() { return Objects.requireNonNull(jdbc.queryForObject("SELECT pg_backend_pid()", Integer.class)); }

    private TransactionTemplate transaction() { return new TransactionTemplate(transactions); }

    private Fixture fixture() { return fixture(CREATED_AT); }

    private Fixture fixture(OffsetDateTime acceptedAt) {
        now(acceptedAt);
        var date = acceptedAt.atZoneSameInstant(ZoneId.of("Asia/Seoul")).toLocalDate();
        var accountId = UUID.randomUUID();
        var childId = UUID.randomUUID();
        var conversationId = UUID.randomUUID();
        var key = UUID.randomUUID();
        jdbc.update("INSERT INTO accounts (id, email) VALUES (?, ?)", accountId, accountId + "@example.invalid");
        var session = security.createLoginContext(accountId, null);
        jdbc.update("""
                INSERT INTO children (id, account_id, name, nickname, birth_date, gender, character_id)
                VALUES (?, ?, '가나', '별이', DATE '2020-05-12', 'FEMALE', 'test-character')
                """, childId, accountId);
        jdbc.update("""
                INSERT INTO conversations (id, child_id, client_request_id, service_date, scheduled_end_at, status, summary_status, started_at)
                VALUES (?, ?, ?, ?, ?, 'ACTIVE', 'NOT_STARTED', ?)
                """, conversationId, childId, UUID.randomUUID(), date, date.plusDays(1).atStartOfDay(ZoneId.of("Asia/Seoul")).toOffsetDateTime(),
                date.atTime(8, 0).atZone(ZoneId.of("Asia/Seoul")).toOffsetDateTime());
        jdbc.update("INSERT INTO conversation_session_links (session_security_id, conversation_id, bound_at) VALUES (?, ?, ?)",
                session.securityContextId(), conversationId, acceptedAt.minusSeconds(1));
        var accepted = acceptance.accept(session, childId, conversationId, key, AUDIO);
        return new Fixture(session, childId, conversationId, key, accepted);
    }

    private record Fixture(AuthenticatedSession session, UUID childId, UUID conversationId, UUID key, Acceptance accepted) { }
}
