package com.dodamdodam.backend.home.api;

import com.dodamdodam.backend.TestcontainersConfiguration;
import com.dodamdodam.backend.auth.session.AuthenticatedSession;
import com.dodamdodam.backend.auth.session.SessionSecurityRepository;
import com.dodamdodam.backend.conversation.application.AllowedTextPolicy;
import com.dodamdodam.backend.conversation.application.ConversationClosureCoordinator;
import com.dodamdodam.backend.conversation.application.ConversationClosureCoordinator.Trigger;
import com.dodamdodam.backend.conversation.application.TurnAcceptanceService;
import com.dodamdodam.backend.conversation.application.TurnAcceptanceService.Acceptance;
import com.dodamdodam.backend.conversation.application.TurnResultService;
import com.dodamdodam.backend.conversation.domain.TextVisibility;
import com.dodamdodam.backend.conversation.infrastructure.ConversationRepository;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockCookie;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

/** PostgreSQL·업무 인증·Spring Session·실제 HTTP는 그대로 실행하고 대화 판정 시각만 고정한다. */
@Import({TestcontainersConfiguration.class, ChildHomeContractTests.ResponseGate.class})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.session.jdbc.cleanup-cron=-", "app.security.allowed-origins=http://localhost:5173",
        "logging.level.org.springframework.security=INFO", "logging.level.org.springframework.session=INFO"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Timeout(40)
class ChildHomeContractTests {

    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-10-03T13:00:00+09:00");
    private static final OffsetDateTime MIDNIGHT = OffsetDateTime.parse("2026-10-04T00:00:00+09:00");
    private static final OffsetDateTime JOINED = OffsetDateTime.parse("2026-10-01T06:00:00+09:00");
    @Autowired private JdbcTemplate jdbc;
    @Autowired private JdbcIndexedSessionRepository sessions;
    @Autowired @Qualifier("cookieSerializer") private CookieSerializer cookies;
    @Autowired private SessionSecurityRepository security;
    @Autowired private ConversationClosureCoordinator closure;
    @Autowired private TurnAcceptanceService acceptance;
    @Autowired private TurnResultService results;
    @Autowired private PlatformTransactionManager transactions;
    @Autowired private ResponseGate responseGate;
    @LocalServerPort private int port;
    @MockitoSpyBean private ConversationRepository conversations;
    private HttpClient client;

    @BeforeEach
    void prepare() {
        responseGate.reset();
        client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        jdbc.update("DELETE FROM conversation_session_links");
        jdbc.update("DELETE FROM conversation_turns");
        jdbc.update("DELETE FROM conversations");
        jdbc.update("DELETE FROM SPRING_SESSION");
        jdbc.update("DELETE FROM email_verifications");
        jdbc.update("DELETE FROM children");
        jdbc.update("DELETE FROM guardian_pins");
        jdbc.update("DELETE FROM session_security");
        jdbc.update("DELETE FROM accounts");
        jdbc.update("DELETE FROM auth_rate_limits");
        now(NOW);
    }

    @AfterEach
    void finish() {
        responseGate.reset();
        client.close();
    }

    @Test
    void returnsTheExactHomeContractWithoutCreatingConversationTurnOrLinkRows() throws Exception {
        var browser = authenticated();
        var child = child(browser.accountId(), JOINED);
        var before = businessRows();
        assertThat(sessions.findById(browser.sessionId())).isNotNull();

        var response = home(browser, child);

        assertOk(response);
        assertThat(object(response, "$")).containsOnlyKeys("data");
        assertThat(object(response, "$.data")).containsOnlyKeys("child", "greeting", "joinedAt", "daysTogether",
                "activeConversationId", "menus", "availability")
                .containsEntry("greeting", "별이, 오늘도 반가워!")
                .containsEntry("joinedAt", "2026-10-01T06:00:00+09:00")
                .containsEntry("daysTogether", 3).containsEntry("activeConversationId", null);
        assertThat(object(response, "$.data.child")).containsOnlyKeys("childId", "name", "nickname", "characterId")
                .containsEntry("childId", child.toString()).containsEntry("name", "가나")
                .containsEntry("nickname", "별이").containsEntry("characterId", "test-character");
        assertThat(object(response, "$.data.availability")).containsOnlyKeys("timeZone", "serverTime", "serviceDate",
                "opensAt", "closesAt", "canEnter", "reason", "nextOpensAt")
                .containsEntry("timeZone", "Asia/Seoul").containsEntry("serverTime", "2026-10-03T13:00:00+09:00")
                .containsEntry("serviceDate", "2026-10-03").containsEntry("opensAt", "2026-10-03T08:00:00+09:00")
                .containsEntry("closesAt", "2026-10-04T00:00:00+09:00").containsEntry("canEnter", true)
                .containsEntry("reason", null).containsEntry("nextOpensAt", null);
        assertMenus(response, true);
        assertThat(response.body()).doesNotContain(browser.contextId().toString(), "2020-05-12");
        assertThat(businessRows()).isEqualTo(before);
    }

    @ParameterizedTest
    @CsvSource({
            "2026-10-03T07:59:59+09:00,false,2026-10-03,2026-10-04,3",
            "2026-10-03T07:59:59.999999+09:00,false,2026-10-03,2026-10-04,3",
            "2026-10-03T08:00:00+09:00,true,2026-10-03,2026-10-04,3",
            "2026-10-03T23:59:59+09:00,true,2026-10-03,2026-10-04,3",
            "2026-10-03T23:59:59.999999+09:00,true,2026-10-03,2026-10-04,3",
            "2026-10-04T00:00:00+09:00,false,2026-10-04,2026-10-05,4"
    })
    void usesKstOpeningAndMidnightBoundariesThroughRealHttp(String time, boolean open, String date, String nextDate,
            int days) throws Exception {
        var browser = authenticated();
        var child = child(browser.accountId(), JOINED);
        now(OffsetDateTime.parse(time));

        var response = home(browser, child);

        assertOk(response);
        assertAvailability(response, open, open ? null : "OUTSIDE_SERVICE_HOURS");
        assertThat(object(response, "$.data.availability")).containsEntry("serviceDate", date)
                .containsEntry("opensAt", date + "T08:00:00+09:00")
                .containsEntry("closesAt", nextDate + "T00:00:00+09:00")
                .containsEntry("nextOpensAt", open ? null : date + "T08:00:00+09:00");
        assertThat(object(response, "$.data")).containsEntry("daysTogether", days).containsEntry("activeConversationId", null);
        assertMenus(response, open);
        assertCounts(0, 0, 0);
    }

    @ParameterizedTest
    @CsvSource({
            "2026-10-02T15:00:00Z,2026-10-03T08:00:00+09:00,1",
            "2026-10-03T23:59:59+09:00,2026-10-04T00:00:00+09:00,2",
            "2026-10-31T14:59:59Z,2026-10-31T15:00:00Z,2",
            "2026-12-31T23:59:59+09:00,2027-01-01T00:00:00+09:00,2",
            "2024-02-28T23:59:59+09:00,2024-03-01T00:00:00+09:00,3"
    })
    void countsThePersistedRegistrationDayInclusivelyAcrossCalendarChanges(String joined, String time, int days)
            throws Exception {
        var browser = authenticated();
        var registeredAt = OffsetDateTime.parse(joined);
        var child = child(browser.accountId(), registeredAt);
        now(OffsetDateTime.parse(time));

        var response = home(browser, child);

        assertOk(response);
        assertThat(object(response, "$.data")).containsEntry("daysTogether", days);
        String exposed = JsonPath.read(response.body(), "$.data.joinedAt");
        assertThat(OffsetDateTime.parse(exposed).toInstant()).isEqualTo(registeredAt.toInstant());
        assertThat(exposed).endsWith("+09:00");
        assertCounts(0, 0, 0);
    }

    @Test
    void anonymousAndMalformedRequestsKeepTheAuthenticationAndValidationOrder() throws Exception {
        var browser = authenticated();
        assertError(send(request("/api/v1/children/not-a-uuid/home", null).GET().build()), 401, "AUTHENTICATION_REQUIRED");
        assertError(send(request("/api/v1/children/not-a-uuid/home", browser).GET().build()), 400, "VALIDATION_FAILED");
        assertCounts(0, 0, 0);
    }

    @ParameterizedTest
    @ValueSource(strings = {"REVOKED", "EXPIRED", "VERSION", "FRAMEWORK_SESSION_DELETED", "FRAMEWORK_SESSION_EXPIRED"})
    void rejectsInvalidRealSessionsWithoutCleaningBusinessRows(String invalidation) throws Exception {
        var browser = authenticated();
        var child = child(browser.accountId(), JOINED);
        start(browser, child);
        var before = businessRows();
        switch (invalidation) {
            case "REVOKED" -> security.revokeCurrentContext(browser.accountId(), browser.contextId());
            case "EXPIRED" -> jdbc.update("""
                    UPDATE session_security SET created_at = clock_timestamp() - INTERVAL '2 hours',
                        expires_at = clock_timestamp() - INTERVAL '1 hour' WHERE id = ?
                    """, browser.contextId());
            case "VERSION" -> jdbc.update("UPDATE accounts SET session_version = session_version + 1 WHERE id = ?", browser.accountId());
            case "FRAMEWORK_SESSION_EXPIRED" -> expireFrameworkSession(browser);
            default -> sessions.deleteById(browser.sessionId());
        }

        assertError(home(browser, child), 401, "AUTHENTICATION_REQUIRED");
        assertThat(businessRows()).isEqualTo(before);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void missingAndForeignChildrenAre404AndForeignOverdueConversationIsNeverCleaned(boolean missing) throws Exception {
        var owner = authenticated();
        var foreignChild = child(owner.accountId(), JOINED);
        now(NOW.minusDays(1));
        var conversation = start(owner, foreignChild);
        var attacker = authenticated();
        now(NOW);
        var before = businessRows();

        var response = home(attacker, missing ? UUID.randomUUID() : foreignChild);

        assertError(response, 404, "RESOURCE_NOT_FOUND");
        assertThat(response.body()).doesNotContain(foreignChild.toString(), conversation.toString(), "가나", "별이");
        assertThat(businessRows()).isEqualTo(before);
        assertThat(conversation(conversation)).containsEntry("status", "ACTIVE");
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void activeIdRequiresTheCurrentContextLinkAndRepeatedEntryNeverRebinds(boolean linked) throws Exception {
        var original = authenticated();
        var child = child(original.accountId(), JOINED);
        var conversation = start(original, child);
        var browser = linked ? original : authenticated(original.accountId());
        var before = businessRows();

        for (int visit = 0; visit < 2; visit++) {
            var response = home(browser, child);
            assertOk(response);
            assertAvailability(response, true, null);
            assertThat(object(response, "$.data")).containsEntry("activeConversationId", linked ? conversation.toString() : null);
        }
        assertThat(businessRows()).isEqualTo(before);
        assertCounts(1, 0, 1);
    }

    @Test
    void closingRetainsTheFirstBoundaryDeadlineAndBindingWhileWorkIsStillPending() throws Exception {
        var browser = authenticated();
        var child = child(browser.accountId(), JOINED);
        var conversation = start(browser, child);
        var turn = accept(browser, child, conversation, NOW.plusSeconds(1));
        end(browser, child, conversation, NOW.plusSeconds(10));
        var before = businessRows();
        now(NOW.plusSeconds(20));

        var response = home(browser, child);

        assertOk(response);
        assertAvailability(response, false, "CONVERSATION_CLOSING");
        assertThat(object(response, "$.data")).containsEntry("activeConversationId", null);
        assertThat(object(response, "$.data.availability")).containsEntry("nextOpensAt", null);
        assertMenus(response, false);
        assertThat(businessRows()).isEqualTo(before);
        assertThat(turn(turn.turn().turnId())).containsEntry("status", "PROCESSING");
        assertThat(time("SELECT processing_deadline_at FROM conversation_turns WHERE id = ?", turn.turn().turnId()))
                .isEqualTo(turn.processingDeadlineAt().toInstant());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void endedAllowsANewConversationOnTheSameDayWithoutWaitingForOrExecutingSummary(boolean allowedText) throws Exception {
        var browser = authenticated();
        var child = child(browser.accountId(), JOINED);
        var previous = start(browser, child);
        if (allowedText) {
            terminal(browser, child, previous, NOW.plusSeconds(1), true);
        }
        end(browser, child, previous, NOW.plusSeconds(10));
        var before = businessRows();
        var oldRow = conversation(previous);
        var oldLink = link(browser, previous);
        assertThat(oldRow).containsEntry("status", "ENDED").containsEntry("summary_status", allowedText ? "PENDING" : "EMPTY")
                .containsEntry("summary_completed_at", null).containsEntry("summary_error_code", null)
                .containsEntry("title", null).containsEntry("topic", null).containsEntry("summary", null);
        now(NOW.plusSeconds(20));

        var response = home(browser, child);

        assertOk(response);
        assertAvailability(response, true, null);
        assertThat(object(response, "$.data")).containsEntry("activeConversationId", null);
        assertThat(businessRows()).isEqualTo(before);
        var next = start(browser, child);
        assertThat(next).isNotEqualTo(previous);
        assertThat(conversation(previous)).isEqualTo(oldRow);
        assertThat(link(browser, previous)).isEqualTo(oldLink);
        assertThat(object(home(browser, child), "$.data")).containsEntry("activeConversationId", next.toString());
        assertCounts(2, allowedText ? 1 : 0, 2);
    }

    @ParameterizedTest
    @ValueSource(strings = {"00:00:00", "07:59:59"})
    void outsideHoursWinOverClosingEvenWhenCleanupFinishesIt(String time) throws Exception {
        var browser = authenticated();
        var child = child(browser.accountId(), JOINED);
        now(MIDNIGHT.minusSeconds(30));
        var conversation = start(browser, child);
        var pending = accept(browser, child, conversation, MIDNIGHT.minusSeconds(27));
        var boundary = MIDNIGHT.minusSeconds(20);
        end(browser, child, conversation, boundary);
        now(OffsetDateTime.parse("2026-10-04T" + time + "+09:00"));

        var response = home(browser, child);

        assertOk(response);
        assertAvailability(response, false, "OUTSIDE_SERVICE_HOURS");
        assertThat(object(response, "$.data")).containsEntry("activeConversationId", null);
        assertThat(object(response, "$.data.availability")).containsEntry("nextOpensAt", "2026-10-04T08:00:00+09:00");
        assertThat(conversation(conversation)).containsEntry("end_reason", "MANUAL")
                .containsEntry("status", time.equals("00:00:00") ? "CLOSING" : "ENDED");
        assertThat(time("SELECT end_requested_at FROM conversations WHERE id = ?", conversation)).isEqualTo(boundary.toInstant());
        assertThat(time("SELECT processing_deadline_at FROM conversation_turns WHERE id = ?", pending.turn().turnId()))
                .isEqualTo(pending.processingDeadlineAt().toInstant());
    }

    @ParameterizedTest
    @CsvSource({"ACTIVE,false", "ACTIVE,true", "CLOSING,true"})
    void previousDatesUnendedRowsUseTheExistingCoordinatorAndRecoveryNeverExtends(String state, boolean processing)
            throws Exception {
        var browser = authenticated();
        var child = child(browser.accountId(), JOINED);
        var started = MIDNIGHT.minusDays(1).minusSeconds(30);
        now(started);
        var conversation = start(browser, child);
        Acceptance pending = processing ? accept(browser, child, conversation, started.plusSeconds(3)) : null;
        // 실제 종료 조정기가 미완료 발화를 기다리는 CLOSING을 준비한다.
        if (state.equals("CLOSING")) {
            end(browser, child, conversation, started.plusSeconds(10));
        }
        assertThat(conversation(conversation)).containsEntry("status", state);
        var boundAt = link(browser, conversation).get("bound_at");
        now(NOW);

        var response = home(browser, child);

        assertOk(response);
        assertAvailability(response, true, null);
        assertThat(object(response, "$.data")).containsEntry("activeConversationId", null);
        assertThat(conversation(conversation)).containsEntry("status", "ENDED").containsEntry("summary_status", "EMPTY");
        var boundary = state.equals("ACTIVE") ? MIDNIGHT.minusDays(1) : started.plusSeconds(10);
        assertThat(time("SELECT end_requested_at FROM conversations WHERE id = ?", conversation)).isEqualTo(boundary.toInstant());
        assertThat(conversation(conversation)).containsEntry("end_reason", state.equals("ACTIVE") ? "MIDNIGHT" : "MANUAL");
        assertThat(link(browser, conversation)).containsEntry("bound_at", boundAt);
        if (processing) {
            assertThat(turn(Objects.requireNonNull(pending).turn().turnId())).containsEntry("status", "FAILED")
                    .containsEntry("error_code", "AI_TIMEOUT");
        }
        var cleaned = businessRows();
        var receiptUntil = time("SELECT end_recovery_until FROM conversation_session_links WHERE conversation_id = ?", conversation);
        var endedAt = time("SELECT ended_at FROM conversations WHERE id = ?", conversation);
        assertThat(receiptUntil).isEqualTo(endedAt.plusSeconds(600));
        now(NOW.plusSeconds(30));
        assertOk(home(browser, child));
        var newcomer = authenticated(browser.accountId());
        assertOk(home(newcomer, child));
        assertThat(businessRows()).isEqualTo(cleaned);
    }

    @ParameterizedTest
    @ValueSource(longs = {-1, 0, 1})
    void closingDeadlineEqualityPreservesTerminalResultsAndTheOriginalBoundary(long microseconds) throws Exception {
        var browser = authenticated();
        var child = child(browser.accountId(), JOINED);
        now(NOW.minusMinutes(2));
        var conversation = start(browser, child);
        var succeeded = terminal(browser, child, conversation, NOW.minusSeconds(100), true);
        var failed = terminal(browser, child, conversation, NOW.minusSeconds(90), false);
        var pending = accept(browser, child, conversation, NOW.minusSeconds(60));
        var succeededBefore = turn(succeeded.turn().turnId());
        var failedBefore = turn(failed.turn().turnId());
        var boundary = NOW.minusSeconds(10);
        end(browser, child, conversation, boundary);
        var originalLink = link(browser, conversation);
        now(NOW.plusNanos(microseconds * 1_000));

        var response = home(browser, child);

        assertOk(response);
        boolean expired = microseconds >= 0;
        assertAvailability(response, expired, expired ? null : "CONVERSATION_CLOSING");
        assertThat(turn(pending.turn().turnId())).containsEntry("status", expired ? "FAILED" : "PROCESSING")
                .containsEntry("error_code", expired ? "AI_TIMEOUT" : null);
        assertThat(time("SELECT processing_deadline_at FROM conversation_turns WHERE id = ?", pending.turn().turnId()))
                .isEqualTo(pending.processingDeadlineAt().toInstant());
        assertThat(turn(succeeded.turn().turnId())).isEqualTo(succeededBefore);
        assertThat(turn(failed.turn().turnId())).isEqualTo(failedBefore);
        assertThat(time("SELECT end_requested_at FROM conversations WHERE id = ?", conversation)).isEqualTo(boundary.toInstant());
        assertThat(conversation(conversation)).containsEntry("end_reason", "MANUAL")
                .containsEntry("summary_status", expired ? "PENDING" : "NOT_STARTED");
        assertThat(link(browser, conversation)).containsEntry("bound_at", originalLink.get("bound_at"));
        var beforeRepeat = businessRows();
        assertOk(home(browser, child));
        assertThat(businessRows()).isEqualTo(beforeRepeat);
    }

    @ParameterizedTest
    @ValueSource(longs = {-1, 0, 1})
    void activeTurnTimeoutDoesNotEndTheConversationOrMoveItsDeadline(long microseconds) throws Exception {
        var browser = authenticated();
        var child = child(browser.accountId(), JOINED);
        now(NOW.minusMinutes(2));
        var conversation = start(browser, child);
        var pending = accept(browser, child, conversation, NOW.minusSeconds(60));
        var originalLink = link(browser, conversation);
        now(NOW.plusNanos(microseconds * 1_000));

        var response = home(browser, child);

        assertOk(response);
        assertAvailability(response, true, null);
        assertThat(object(response, "$.data")).containsEntry("activeConversationId", conversation.toString());
        assertThat(conversation(conversation)).containsEntry("status", "ACTIVE").containsEntry("summary_status", "NOT_STARTED")
                .containsEntry("end_requested_at", null).containsEntry("ended_at", null);
        assertThat(turn(pending.turn().turnId())).containsEntry("status", microseconds < 0 ? "PROCESSING" : "FAILED")
                .containsEntry("error_code", microseconds < 0 ? null : "AI_TIMEOUT");
        assertThat(time("SELECT processing_deadline_at FROM conversation_turns WHERE id = ?", pending.turn().turnId()))
                .isEqualTo(pending.processingDeadlineAt().toInstant());
        assertThat(link(browser, conversation)).isEqualTo(originalLink);
    }

    @ParameterizedTest
    @ValueSource(strings = {"MIDNIGHT", "CLOSING", "ENDED", "LINK_REMOVED", "REVOKED", "EXPIRED", "VERSION",
            "FRAMEWORK_SESSION_DELETED", "FRAMEWORK_SESSION_EXPIRED"})
    void realHttpRechecksChangesCommittedAfterTheInitialHomeTransaction(String change) throws Exception {
        var browser = authenticated();
        var child = child(browser.accountId(), JOINED);
        var conversation = start(browser, child);
        if (change.equals("CLOSING")) {
            accept(browser, child, conversation, NOW.plusSeconds(1));
        }
        var response = racedHome(browser, child, () -> {
            switch (change) {
                case "MIDNIGHT" -> now(MIDNIGHT);
                case "CLOSING", "ENDED" -> end(browser, child, conversation, NOW.plusSeconds(10));
                case "LINK_REMOVED" -> jdbc.update("DELETE FROM conversation_session_links WHERE session_security_id = ? AND conversation_id = ?",
                        browser.contextId(), conversation);
                case "REVOKED" -> security.revokeCurrentContext(browser.accountId(), browser.contextId());
                case "EXPIRED" -> jdbc.update("""
                        UPDATE session_security SET created_at = clock_timestamp() - INTERVAL '2 hours',
                            expires_at = clock_timestamp() - INTERVAL '1 hour' WHERE id = ?
                        """, browser.contextId());
                case "VERSION" -> jdbc.update("UPDATE accounts SET session_version = session_version + 1 WHERE id = ?", browser.accountId());
                case "FRAMEWORK_SESSION_EXPIRED" -> expireFrameworkSession(browser);
                default -> sessions.deleteById(browser.sessionId());
            }
        });

        if (List.of("REVOKED", "EXPIRED", "VERSION", "FRAMEWORK_SESSION_DELETED", "FRAMEWORK_SESSION_EXPIRED").contains(change)) {
            assertError(response, 401, "AUTHENTICATION_REQUIRED");
            assertNoRenewalCookie(response);
        } else {
            assertOk(response);
            var reason = change.equals("MIDNIGHT") ? "OUTSIDE_SERVICE_HOURS" : change.equals("CLOSING") ? "CONVERSATION_CLOSING" : null;
            assertAvailability(response, reason == null, reason);
            assertThat(object(response, "$.data")).containsEntry("activeConversationId", null);
            assertThat(response.body()).doesNotContain(conversation.toString());
        }
        assertCounts(1, change.equals("CLOSING") ? 1 : 0, change.equals("LINK_REMOVED") ? 0 : 1);
    }

    @ParameterizedTest
    @CsvSource({"DEADLINE,DELETED", "MIDNIGHT,DELETED", "DEADLINE,EXPIRED", "MIDNIGHT,EXPIRED",
            "DEADLINE,REPLACED", "MIDNIGHT,REPLACED"})
    void invalidStoredSessionPreventsBusinessCleanupAfterTheInitialHomeTransaction(String boundary, String invalidation)
            throws Exception {
        var fixture = prepareHomeRecheck(boundary);
        var afterInitial = new AtomicReference<Map<String, List<Map<String, Object>>>>();

        var response = racedHome(fixture.browser(), fixture.child(), () -> {
            // 최초 조회가 끝난 뒤의 모든 업무 컬럼을 기준으로 재조회의 추가 쓰기만 검출한다.
            afterInitial.set(businessRows());
            assertThat(turn(fixture.pending().turn().turnId())).containsEntry("status", "PROCESSING")
                    .containsEntry("completed_at", null).containsEntry("error_code", null);
            assertThat(conversation(fixture.conversation())).containsEntry("status", "ACTIVE");
            invalidateStoredSession(fixture.browser(), invalidation);
            now(fixture.recheckAt());
            assertThat(businessRows()).isEqualTo(afterInitial.get());
        });

        assertError(response, 401, "AUTHENTICATION_REQUIRED");
        assertNoRenewalCookie(response);
        assertThat(response.body()).doesNotContain(fixture.conversation().toString(), fixture.pending().turn().turnId().toString());
        assertThat(businessRows()).isEqualTo(afterInitial.get());
        assertCounts(1, 1, 1);
    }

    @ParameterizedTest
    @CsvSource({"DEADLINE,SPRING_SESSION", "MIDNIGHT,SPRING_SESSION",
            "DEADLINE,SPRING_SESSION_ATTRIBUTES", "MIDNIGHT,SPRING_SESSION_ATTRIBUTES",
            "DEADLINE,session_security", "MIDNIGHT,session_security"})
    void authenticationLookupFailurePreventsBusinessCleanupAfterTheInitialHomeTransaction(String boundary, String table)
            throws Exception {
        var fixture = prepareHomeRecheck(boundary);
        var afterInitial = new AtomicReference<Map<String, List<Map<String, Object>>>>();
        HttpResponse<String> response;
        try {
            response = racedHome(fixture.browser(), fixture.child(), () -> {
                afterInitial.set(businessRows());
                assertThat(turn(fixture.pending().turn().turnId())).containsEntry("status", "PROCESSING");
                // 독립 Testcontainers DB에서 실제 조회를 실패시키고 업무 테이블은 건드리지 않는다.
                jdbc.execute("ALTER TABLE " + table + " RENAME TO unavailable_home_recheck_store");
                now(fixture.recheckAt());
                assertThat(businessRows()).isEqualTo(afterInitial.get());
            });
        } finally {
            jdbc.execute("ALTER TABLE IF EXISTS unavailable_home_recheck_store RENAME TO " + table);
        }

        assertError(response, 503, "AUTH_STATE_UNAVAILABLE");
        assertNoRenewalCookie(response);
        assertThat(response.body()).doesNotContain(fixture.conversation().toString(), fixture.pending().turn().turnId().toString(),
                "unavailable_home_recheck_store");
        assertThat(businessRows()).isEqualTo(afterInitial.get());
        assertCounts(1, 1, 1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"DEADLINE", "MIDNIGHT", "MIDNIGHT_PENDING"})
    void validStoredSessionStillCommitsTheRequiredCleanupDuringTheResponseRecheck(String boundary) throws Exception {
        var fixture = prepareHomeRecheck(boundary);
        var afterInitial = new AtomicReference<Map<String, List<Map<String, Object>>>>();
        var response = racedHome(fixture.browser(), fixture.child(), () -> {
            afterInitial.set(businessRows());
            assertThat(turn(fixture.pending().turn().turnId())).containsEntry("status", "PROCESSING");
            assertThat(conversation(fixture.conversation())).containsEntry("status", "ACTIVE");
            now(fixture.recheckAt());
        });

        assertOk(response);
        assertThat(object(response, "$")).containsOnlyKeys("data");
        assertThat(object(response, "$.data")).containsOnlyKeys("child", "greeting", "joinedAt", "daysTogether",
                "activeConversationId", "menus", "availability");
        boolean midnight = !boundary.equals("DEADLINE");
        assertAvailability(response, !midnight, midnight ? "OUTSIDE_SERVICE_HOURS" : null);
        assertMenus(response, !midnight);
        assertThat(object(response, "$.data")).containsEntry("activeConversationId", midnight ? null : fixture.conversation().toString());
        assertThat(response.headers().allValues("Set-Cookie").stream().filter(value -> value.startsWith("JSESSIONID="))
                .map(MockCookie::parse).filter(cookie -> cookie.getMaxAge() == 31536000)).hasSize(1);

        // 허용된 정리 컬럼만 기대값에 반영해 deadline·종료 사유·복구 기한을 포함한 전체 행을 비교한다.
        var expected = new LinkedHashMap<>(Objects.requireNonNull(afterInitial.get()));
        if (!boundary.equals("MIDNIGHT_PENDING")) {
            var expectedTurn = new LinkedHashMap<>(expected.get("turns").getFirst());
            expectedTurn.put("status", "FAILED");
            expectedTurn.put("error_code", "AI_TIMEOUT");
            expectedTurn.put("completed_at", java.sql.Timestamp.from(fixture.recheckAt().toInstant()));
            expected.put("turns", List.of(expectedTurn));
        }
        if (midnight) {
            var expectedConversation = new LinkedHashMap<>(expected.get("conversations").getFirst());
            expectedConversation.put("status", boundary.equals("MIDNIGHT_PENDING") ? "CLOSING" : "ENDED");
            expectedConversation.put("end_requested_at", java.sql.Timestamp.from(MIDNIGHT.toInstant()));
            expectedConversation.put("end_reason", "MIDNIGHT");
            if (boundary.equals("MIDNIGHT")) {
                expectedConversation.put("ended_at", java.sql.Timestamp.from(MIDNIGHT.toInstant()));
                expectedConversation.put("summary_status", "EMPTY");
                var expectedLink = new LinkedHashMap<>(expected.get("links").getFirst());
                expectedLink.put("end_recovery_until", java.sql.Timestamp.from(MIDNIGHT.plusSeconds(600).toInstant()));
                expected.put("links", List.of(expectedLink));
            }
            expected.put("conversations", List.of(expectedConversation));
        }
        assertThat(businessRows()).isEqualTo(expected);
        assertCounts(1, 1, 1);
    }

    @Test
    void rejectedMidnightRecheckPreservesCleanupAlreadyCommittedByTheInitialHomeTransaction() throws Exception {
        var fixture = prepareHomeRecheck("MIDNIGHT_ALREADY_EXPIRED");
        var beforeRequest = businessRows();
        assertThat(turn(fixture.pending().turn().turnId())).containsEntry("status", "PROCESSING");
        var afterInitial = new AtomicReference<Map<String, List<Map<String, Object>>>>();

        var response = racedHome(fixture.browser(), fixture.child(), () -> {
            afterInitial.set(businessRows());
            // 최초 조회가 커밋한 발화 만료는 유지하되, 실패한 재조회가 자정 종료를 추가하면 안 된다.
            assertThat(turn(fixture.pending().turn().turnId())).containsEntry("status", "FAILED").containsEntry("error_code", "AI_TIMEOUT");
            assertThat(time("SELECT completed_at FROM conversation_turns WHERE id = ?", fixture.pending().turn().turnId()))
                    .isEqualTo(fixture.initialAt().toInstant());
            assertThat(afterInitial.get().get("turns")).isNotEqualTo(beforeRequest.get("turns"));
            assertThat(afterInitial.get().get("conversations")).isEqualTo(beforeRequest.get("conversations"));
            assertThat(afterInitial.get().get("links")).isEqualTo(beforeRequest.get("links"));
            sessions.deleteById(fixture.browser().sessionId());
            now(MIDNIGHT);
            assertThat(businessRows()).isEqualTo(afterInitial.get());
        });

        assertError(response, 401, "AUTHENTICATION_REQUIRED");
        assertNoRenewalCookie(response);
        assertThat(businessRows()).isEqualTo(afterInitial.get());
        assertThat(conversation(fixture.conversation())).containsEntry("status", "ACTIVE")
                .containsEntry("end_requested_at", null).containsEntry("end_reason", null).containsEntry("ended_at", null);
    }

    @Test
    void aSuccessfulResultCommittedBeforeDeadlineIsNotOverwrittenByHomeAtDeadline() throws Exception {
        var browser = authenticated();
        var child = child(browser.accountId(), JOINED);
        now(NOW.minusMinutes(2));
        var conversation = start(browser, child);
        var pending = accept(browser, child, conversation, NOW.minusSeconds(60));
        now(NOW.minusNanos(1_000));
        var terminal = new AtomicReference<Map<String, Object>>();

        var response = racedHome(browser, child, () -> {
            var allowed = new AllowedTextPolicy().select(TextVisibility.VISIBLE, true, true, "[test] 허용 결과");
            assertThat(results.succeed(pending.completionPermit().orElseThrow(), allowed, null).changed()).isTrue();
            terminal.set(turn(pending.turn().turnId()));
            now(NOW);
        });

        assertOk(response);
        assertThat(turn(pending.turn().turnId())).isEqualTo(terminal.get()).containsEntry("status", "SUCCEEDED");
        assertThat(response.body()).doesNotContain("[test] 허용 결과");
        assertThat(time("SELECT processing_deadline_at FROM conversation_turns WHERE id = ?", pending.turn().turnId()))
                .isEqualTo(pending.processingDeadlineAt().toInstant());
    }

    @Test
    void aLateResultCannotReplaceTheTimeoutCommittedByHome() throws Exception {
        var browser = authenticated();
        var child = child(browser.accountId(), JOINED);
        now(NOW.minusMinutes(2));
        var conversation = start(browser, child);
        var pending = accept(browser, child, conversation, NOW.minusSeconds(60));
        now(NOW);
        assertOk(home(browser, child));
        var timedOut = turn(pending.turn().turnId());
        now(NOW.plusSeconds(1));

        assertThat(results.succeed(pending.completionPermit().orElseThrow(), null, null).changed()).isFalse();
        assertThat(turn(pending.turn().turnId())).isEqualTo(timedOut).containsEntry("status", "FAILED")
                .containsEntry("error_code", "AI_TIMEOUT");
    }

    @ParameterizedTest
    @ValueSource(strings = {"MIDNIGHT", "REVOKED"})
    void homeRechecksTimeAndAuthenticationAfterAnActualAccountLockWait(String change) throws Exception {
        var browser = authenticated();
        var child = child(browser.accountId(), JOINED);
        now(MIDNIGHT.minusSeconds(30));
        var conversation = start(browser, child);
        var before = businessRows();
        var pending = new TransactionTemplate(transactions).execute(_ -> {
            jdbc.queryForList("SELECT id FROM accounts WHERE id = ? FOR UPDATE", UUID.class, browser.accountId());
            int blocker = Objects.requireNonNull(jdbc.queryForObject("SELECT pg_backend_pid()", Integer.class));
            var waiting = client.sendAsync(homeRequest(browser, child), HttpResponse.BodyHandlers.ofString());
            awaitDatabaseWait(blocker);
            if (change.equals("MIDNIGHT")) {
                now(MIDNIGHT);
            } else {
                assertThat(security.revokeCurrentContext(browser.accountId(), browser.contextId())).isTrue();
            }
            return waiting;
        });

        var response = Objects.requireNonNull(pending).get(20, TimeUnit.SECONDS);

        if (change.equals("REVOKED")) {
            assertError(response, 401, "AUTHENTICATION_REQUIRED");
            assertThat(businessRows()).isEqualTo(before);
        } else {
            assertOk(response);
            assertAvailability(response, false, "OUTSIDE_SERVICE_HOURS");
            assertThat(conversation(conversation)).containsEntry("status", "ENDED").containsEntry("end_reason", "MIDNIGHT");
        }
    }

    @Test
    void concurrentHomeRequestsFinishClosingOnceAndPreserveBothExistingRecoveryBindings() throws Exception {
        var first = authenticated();
        var child = child(first.accountId(), JOINED);
        now(NOW.minusMinutes(2));
        var conversation = start(first, child);
        var second = authenticated(first.accountId());
        // 미구현 resume API를 추가하지 않고, 이전에 연결된 다른 로그인 문맥을 합성 fixture로 준비한다.
        jdbc.update("INSERT INTO conversation_session_links (session_security_id, conversation_id, bound_at) VALUES (?, ?, ?)",
                second.contextId(), conversation, NOW.minusSeconds(110));
        var firstBoundAt = link(first, conversation).get("bound_at");
        var secondBoundAt = link(second, conversation).get("bound_at");
        accept(first, child, conversation, NOW.minusSeconds(60));
        end(first, child, conversation, NOW.minusSeconds(10));
        now(NOW);

        var a = client.sendAsync(homeRequest(first, child), HttpResponse.BodyHandlers.ofString());
        var b = client.sendAsync(homeRequest(second, child), HttpResponse.BodyHandlers.ofString());
        assertOk(a.get(20, TimeUnit.SECONDS));
        assertOk(b.get(20, TimeUnit.SECONDS));

        assertThat(conversation(conversation)).containsEntry("status", "ENDED").containsEntry("summary_status", "EMPTY");
        assertThat(time("SELECT ended_at FROM conversations WHERE id = ?", conversation)).isEqualTo(NOW.toInstant());
        assertThat(link(first, conversation)).containsEntry("bound_at", firstBoundAt);
        assertThat(link(second, conversation)).containsEntry("bound_at", secondBoundAt);
        for (var browser : List.of(first, second)) {
            assertThat(time("SELECT end_recovery_until FROM conversation_session_links WHERE session_security_id = ?", browser.contextId()))
                    .isEqualTo(NOW.plusSeconds(600).toInstant());
        }
        var completed = businessRows();
        now(NOW.plusSeconds(15));
        assertOk(home(first, child));
        assertOk(home(second, child));
        assertThat(businessRows()).isEqualTo(completed);
        assertCounts(1, 1, 2);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void authenticationStoreFailureIs503EvenAfterTheInitialHomeBodyExists(boolean delayed) throws Exception {
        var browser = authenticated();
        var child = child(browser.accountId(), JOINED);
        start(browser, child);
        var before = businessRows();
        // Testcontainers의 독립 DB에서만 실패를 주입하고 어떤 결과에서도 스키마 이름을 복원한다.
        try {
            HttpResponse<String> response;
            if (delayed) {
                response = racedHome(browser, child,
                        () -> jdbc.execute("ALTER TABLE session_security RENAME TO unavailable_home_session_security"));
            } else {
                jdbc.execute("ALTER TABLE session_security RENAME TO unavailable_home_session_security");
                response = home(browser, child);
            }
            assertError(response, 503, "AUTH_STATE_UNAVAILABLE");
            assertNoRenewalCookie(response);
        } finally {
            jdbc.execute("ALTER TABLE unavailable_home_session_security RENAME TO session_security");
        }
        assertThat(businessRows()).isEqualTo(before);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void conversationStoreFailureIs500RatherThanAuthenticationFailure(boolean delayed) throws Exception {
        var browser = authenticated();
        var child = child(browser.accountId(), JOINED);
        start(browser, child);
        var before = businessRows();
        try {
            HttpResponse<String> response;
            if (delayed) {
                response = racedHome(browser, child,
                        () -> jdbc.execute("ALTER TABLE conversations RENAME TO unavailable_home_conversations"));
            } else {
                jdbc.execute("ALTER TABLE conversations RENAME TO unavailable_home_conversations");
                response = home(browser, child);
            }
            assertError(response, 500, "INTERNAL_ERROR");
        } finally {
            jdbc.execute("ALTER TABLE unavailable_home_conversations RENAME TO conversations");
        }
        assertThat(businessRows()).isEqualTo(before);
    }

    @Test
    void recoveryWriteFailureRollsBackTimeoutAndClosureTogether() throws Exception {
        var browser = authenticated();
        var child = child(browser.accountId(), JOINED);
        now(NOW.minusMinutes(2));
        var conversation = start(browser, child);
        accept(browser, child, conversation, NOW.minusSeconds(60));
        end(browser, child, conversation, NOW.minusSeconds(10));
        var before = businessRows();
        now(NOW);
        jdbc.execute("ALTER TABLE conversation_session_links RENAME COLUMN end_recovery_until TO unavailable_home_recovery_until");
        try {
            assertError(home(browser, child), 500, "INTERNAL_ERROR");
        } finally {
            jdbc.execute("ALTER TABLE conversation_session_links RENAME COLUMN unavailable_home_recovery_until TO end_recovery_until");
        }
        assertThat(businessRows()).isEqualTo(before);
    }

    @Test
    void theExistingGlobalBudgetReturns429BeforeAnyBusinessCleanup() throws Exception {
        var browser = authenticated();
        var child = child(browser.accountId(), JOINED);
        assertOk(home(browser, child));
        var before = businessRows();
        jdbc.update("""
                UPDATE auth_rate_limits SET attempt_count = 300, window_started_at = clock_timestamp(),
                    expires_at = clock_timestamp() + INTERVAL '60 seconds', blocked_until = NULL WHERE action = 'API_IP'
                """);

        var response = home(browser, child);

        assertError(response, 429, "RATE_LIMITED");
        assertThat(Long.parseLong(response.headers().firstValue("Retry-After").orElseThrow())).isBetween(1L, 60L);
        assertThat(businessRows()).isEqualTo(before);
    }

    /** 최초 조회 전후와 재조회 경계를 분리하며 발화 기한은 실제 수락 서비스가 계산하게 한다. */
    private HomeRecheck prepareHomeRecheck(String boundary) throws Exception {
        var browser = authenticated();
        var child = child(browser.accountId(), JOINED);
        var at = boundary.equals("DEADLINE") ? NOW : MIDNIGHT;
        now(at.minusMinutes(2));
        var conversation = start(browser, child);
        long secondsBefore = switch (boundary) {
            case "MIDNIGHT_PENDING" -> 27;
            case "MIDNIGHT_ALREADY_EXPIRED" -> 61;
            default -> 60;
        };
        var pending = accept(browser, child, conversation, at.minusSeconds(secondsBefore));
        var initialAt = at.minusNanos(1_000);
        var recheckAt = boundary.equals("DEADLINE") ? pending.processingDeadlineAt().plusNanos(1_000) : MIDNIGHT;
        now(initialAt);
        return new HomeRecheck(browser, child, conversation, pending, initialAt, recheckAt);
    }

    /** 요청 캐시의 문맥을 유효하게 둔 채 JDBC 저장 세션만 삭제·만료·교체한다. */
    private void invalidateStoredSession(Browser browser, String invalidation) {
        switch (invalidation) {
            case "DELETED" -> sessions.deleteById(browser.sessionId());
            case "EXPIRED" -> expireFrameworkSession(browser);
            case "REPLACED" -> {
                var replacement = security.createLoginContext(browser.accountId(), null);
                Session stored = sessions.findById(browser.sessionId());
                assertThat(stored).isNotNull();
                // immediate flush 설정으로 저장된 문맥만 바꾸며 기존 요청·업무 문맥은 폐기하지 않는다.
                stored.setAttribute("securityContextId", replacement.securityContextId());
                Session reloaded = sessions.findById(browser.sessionId());
                assertThat(reloaded).isNotNull();
                assertThat(reloaded.<UUID>getAttribute("securityContextId")).isEqualTo(replacement.securityContextId());
                assertThat(security.isCurrentContext(browser.accountId(), browser.contextId())).isTrue();
                assertThat(security.isCurrentContext(browser.accountId(), replacement.securityContextId())).isTrue();
            }
            default -> throw new IllegalArgumentException("Unknown invalidation fixture");
        }
    }

    /** 응답 준비 후에만 변경을 커밋하며 sleep이나 운영 서비스 mock으로 순서를 대신하지 않는다. */
    private HttpResponse<String> racedHome(Browser browser, UUID child, Runnable change) throws Exception {
        var pause = responseGate.pauseNext();
        var pending = client.sendAsync(homeRequest(browser, child), HttpResponse.BodyHandlers.ofString());
        try {
            assertThat(pause.ready.await(10, TimeUnit.SECONDS)).as("초기 Home Tx 커밋 후 응답 대기").isTrue();
            change.run();
        } finally {
            pause.resume.countDown();
        }
        return pending.get(20, TimeUnit.SECONDS);
    }

    /** 실제 PostgreSQL 잠금 대기자를 확인한 다음에만 경계나 자격을 바꾼다. */
    private void awaitDatabaseWait(int blocker) {
        long limit = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < limit) {
            jdbc.execute("SELECT pg_stat_clear_snapshot()");
            if (Boolean.TRUE.equals(jdbc.queryForObject("""
                    SELECT EXISTS (SELECT 1 FROM pg_stat_activity WHERE ? = ANY(pg_blocking_pids(pid)))
                    """, Boolean.class, blocker))) {
                return;
            }
            Thread.yield();
        }
        throw new AssertionError("HTTP 요청의 실제 DB 잠금 대기를 확인하지 못했습니다.");
    }

    private Browser authenticated() {
        var account = UUID.randomUUID();
        jdbc.update("INSERT INTO accounts (id, email) VALUES (?, ?)", account, account + "@example.invalid");
        return authenticated(account);
    }

    /** 업무 로그인 문맥과 물리 Spring Session을 모두 실제 저장소에 보관한다. */
    private Browser authenticated(UUID account) {
        var current = security.createLoginContext(account, null);
        var authentication = SecurityContextHolder.createEmptyContext();
        authentication.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(account.toString(), null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))));
        var stored = savedSession(sessions, session -> {
            session.setMaxInactiveInterval(Duration.ofSeconds(-1));
            session.setAttribute("accountId", account);
            session.setAttribute("securityContextId", current.securityContextId());
            session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, authentication);
        });
        var cookieResponse = new MockHttpServletResponse();
        cookies.writeCookieValue(new CookieSerializer.CookieValue(new MockHttpServletRequest(), cookieResponse, stored.getId()));
        Cookie cookie = MockCookie.parse(Objects.requireNonNull(cookieResponse.getHeader("Set-Cookie")));
        return new Browser(account, current.securityContextId(), stored.getId(), cookie);
    }

    // JdbcSession의 비공개 구현 타입 대신 기존 JDBC 계약 테스트와 같은 Session 경계를 사용한다.
    private static <S extends Session> Session savedSession(SessionRepository<S> repository, Consumer<Session> configure) {
        S session = repository.createSession();
        configure.accept(session);
        repository.save(session);
        return session;
    }

    /** 대화 시계 spy와 무관하게 실제 JDBC 세션의 유휴 만료를 재현한다. */
    private void expireFrameworkSession(Browser browser) {
        long accessedAt = java.time.Instant.now().minusSeconds(60).toEpochMilli();
        jdbc.update("""
                UPDATE SPRING_SESSION SET MAX_INACTIVE_INTERVAL = 1, LAST_ACCESS_TIME = ?, EXPIRY_TIME = ?
                WHERE SESSION_ID = ?
                """, accessedAt, accessedAt + 1_000, browser.sessionId());
    }

    /** 승인 전 운영 캐릭터 목록을 바꾸지 않고 제약을 만족하는 합성 아이만 DB에 준비한다. */
    private UUID child(UUID account, OffsetDateTime joined) {
        var child = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO children (id, account_id, name, nickname, birth_date, gender, character_id, created_at)
                VALUES (?, ?, '가나', '별이', DATE '2020-05-12', 'FEMALE', 'test-character', ?)
                """, child, account, joined);
        return child;
    }

    /** 시작은 기존 공개 API와 실제 CSRF 발급을 사용한다. */
    private UUID start(Browser browser, UUID child) throws Exception {
        var csrf = send(request("/api/v1/auth/csrf", browser).GET().build());
        assertThat(csrf.statusCode()).isEqualTo(200);
        var token = JsonPath.<String>read(csrf.body(), "$.token");
        var csrfCookie = csrf.headers().allValues("Set-Cookie").stream().filter(value -> value.startsWith("XSRF-TOKEN="))
                .findFirst().map(MockCookie::parse).orElseThrow();
        var call = request("/api/v1/children/" + child + "/conversations", null)
                .header("Cookie", browser.cookie().getName() + "=" + browser.cookie().getValue()
                        + "; XSRF-TOKEN=" + csrfCookie.getValue())
                .header("X-XSRF-TOKEN", token).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"clientRequestId\":\"" + UUID.randomUUID() + "\"}")).build();
        var response = send(call);
        assertThat(response.statusCode()).as(response.body()).isEqualTo(201);
        return UUID.fromString(JsonPath.read(response.body(), "$.data.conversationId"));
    }

    private Acceptance accept(Browser browser, UUID child, UUID conversation, OffsetDateTime createdAt) {
        now(createdAt);
        return acceptance.accept(new AuthenticatedSession(browser.accountId(), browser.contextId()), child, conversation,
                UUID.randomUUID(), "synthetic-test-audio".getBytes(StandardCharsets.UTF_8));
    }

    private Acceptance terminal(Browser browser, UUID child, UUID conversation, OffsetDateTime createdAt, boolean success) {
        var accepted = accept(browser, child, conversation, createdAt);
        now(createdAt.plusSeconds(1));
        var visible = new AllowedTextPolicy().select(TextVisibility.VISIBLE, true, true, "[test] 허용 결과");
        if (success) {
            results.succeed(accepted.completionPermit().orElseThrow(), visible, null);
        } else {
            results.fail(accepted.completionPermit().orElseThrow(), "AI_UPSTREAM_FAILED", visible, null);
        }
        return accepted;
    }

    private void end(Browser browser, UUID child, UUID conversation, OffsetDateTime at) {
        now(at);
        closure.reconcile(browser.accountId(), child, conversation, Trigger.REQUEST_END);
    }

    private void now(OffsetDateTime at) {
        doReturn(at).when(conversations).currentTime();
    }

    private HttpResponse<String> home(Browser browser, UUID child) throws Exception {
        return send(homeRequest(browser, child));
    }

    private HttpRequest homeRequest(Browser browser, UUID child) {
        return request("/api/v1/children/" + child + "/home", browser).GET().build();
    }

    private HttpRequest.Builder request(String path, Browser browser) {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).timeout(Duration.ofSeconds(20))
                .header("Origin", "http://localhost:5173").header("X-Request-ID", "client-controlled");
        if (browser != null) {
            request.header("Cookie", browser.cookie().getName() + "=" + browser.cookie().getValue());
        }
        return request;
    }

    private HttpResponse<String> send(HttpRequest request) throws Exception {
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    /** Spring Session 갱신과 별도 요청 예산은 제외하고 Home 업무 테이블만 비교한다. */
    private Map<String, List<Map<String, Object>>> businessRows() {
        return Map.of("conversations", jdbc.queryForList("SELECT * FROM conversations ORDER BY id"),
                "turns", jdbc.queryForList("SELECT * FROM conversation_turns ORDER BY id"),
                "links", jdbc.queryForList("SELECT * FROM conversation_session_links ORDER BY session_security_id, conversation_id"));
    }

    private Map<String, Object> conversation(UUID id) {
        return jdbc.queryForMap("SELECT * FROM conversations WHERE id = ?", id);
    }

    private Map<String, Object> turn(UUID id) {
        return jdbc.queryForMap("SELECT * FROM conversation_turns WHERE id = ?", id);
    }

    private Map<String, Object> link(Browser browser, UUID conversation) {
        return jdbc.queryForMap("SELECT * FROM conversation_session_links WHERE session_security_id = ? AND conversation_id = ?",
                browser.contextId(), conversation);
    }

    private java.time.Instant time(String sql, UUID id) {
        return Objects.requireNonNull(jdbc.queryForObject(sql, OffsetDateTime.class, id)).toInstant();
    }

    private void assertCounts(int conversations, int turns, int links) {
        assertThat(jdbc.queryForObject("SELECT count(*) FROM conversations", Integer.class)).isEqualTo(conversations);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM conversation_turns", Integer.class)).isEqualTo(turns);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM conversation_session_links", Integer.class)).isEqualTo(links);
    }

    private Map<String, Object> object(HttpResponse<String> response, String path) {
        return JsonPath.read(response.body(), path);
    }

    private void assertOk(HttpResponse<String> response) {
        assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
        assertHeaders(response);
    }

    private void assertAvailability(HttpResponse<String> response, boolean canEnter, String reason) {
        assertThat(object(response, "$.data.availability")).containsEntry("canEnter", canEnter).containsEntry("reason", reason);
    }

    private void assertMenus(HttpResponse<String> response, boolean canEnter) {
        assertThat(object(response, "$.data").get("menus")).isEqualTo(List.of(
                Map.of("code", "TALK", "status", canEnter ? "AVAILABLE" : "UNAVAILABLE"),
                Map.of("code", "PLAY", "status", "COMING_SOON")));
    }

    private void assertError(HttpResponse<String> response, int status, String code) {
        assertThat(response.statusCode()).as(response.body()).isEqualTo(status);
        assertThat(object(response, "$")).containsOnlyKeys("error");
        assertThat(object(response, "$.error")).containsOnlyKeys("code", "message", "requestId", "fields")
                .containsEntry("code", code).containsEntry("requestId", response.headers().firstValue("X-Request-ID").orElseThrow());
        assertThat(response.body()).doesNotContain("\"data\"");
        assertHeaders(response);
    }

    private void assertHeaders(HttpResponse<String> response) {
        var id = response.headers().firstValue("X-Request-ID").orElseThrow();
        assertThat(UUID.fromString(id).toString()).isEqualTo(id).isNotEqualTo("client-controlled");
        assertThat(response.headers().firstValue("Cache-Control").orElseThrow()).contains("no-store");
        assertThat(response.headers().firstValue("Access-Control-Allow-Origin")).contains("http://localhost:5173");
        assertThat(response.headers().firstValue("Access-Control-Allow-Credentials")).contains("true");
        response.headers().firstValueAsLong("Content-Length").ifPresent(length ->
                assertThat(length).isEqualTo(response.body().getBytes(StandardCharsets.UTF_8).length));
    }

    private void assertNoRenewalCookie(HttpResponse<String> response) {
        assertThat(response.headers().allValues("Set-Cookie"))
                .noneMatch(value -> value.startsWith("JSESSIONID=") && !value.contains("Max-Age=0"));
    }

    private record Browser(UUID accountId, UUID contextId, String sessionId, Cookie cookie) { }

    private record HomeRecheck(Browser browser, UUID child, UUID conversation, Acceptance pending,
                               OffsetDateTime initialAt, OffsetDateTime recheckAt) { }

    static class Pause {
        final CountDownLatch ready = new CountDownLatch(1);
        final CountDownLatch resume = new CountDownLatch(1);
    }

    /** 기존 대화 HTTP 테스트와 같은 응답 게이트이며 Home Controller에만 적용한다. */
    @TestConfiguration(proxyBeanMethods = false)
    @ControllerAdvice(assignableTypes = ChildHomeController.class)
    @NullMarked
    static class ResponseGate implements ResponseBodyAdvice<Object> {
        private final AtomicReference<@Nullable Pause> next = new AtomicReference<>();
        private final JsonMapper json;

        ResponseGate(JsonMapper json) {
            this.json = json;
        }

        Pause pauseNext() {
            var pause = new Pause();
            assertThat(next.compareAndSet(null, pause)).isTrue();
            return pause;
        }

        void reset() {
            var pause = next.getAndSet(null);
            if (pause != null) {
                pause.resume.countDown();
            }
        }

        @Override
        public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
            return true;
        }

        @Override
        public @Nullable Object beforeBodyWrite(@Nullable Object body, MethodParameter returnType, MediaType contentType,
                Class<? extends HttpMessageConverter<?>> converterType, ServerHttpRequest request, ServerHttpResponse response) {
            var pause = body instanceof Map<?, ?> data && data.containsKey("data") ? next.getAndSet(null) : null;
            if (pause != null) {
                pause.ready.countDown();
                try {
                    if (!pause.resume.await(15, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("홈 응답 경합 대기가 끝나지 않았습니다.");
                    }
                } catch (InterruptedException failure) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(failure);
                }
                response.getHeaders().setContentLength(json.writeValueAsBytes(body).length);
            }
            return body;
        }
    }
}
