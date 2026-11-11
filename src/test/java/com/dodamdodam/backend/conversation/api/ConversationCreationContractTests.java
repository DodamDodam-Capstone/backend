package com.dodamdodam.backend.conversation.api;

import com.dodamdodam.backend.TestcontainersConfiguration;
import com.dodamdodam.backend.auth.session.SessionSecurityRepository;
import com.dodamdodam.backend.conversation.application.ConversationClosureCoordinator;
import com.dodamdodam.backend.conversation.application.ConversationService;
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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
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
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcPrint;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.mock.web.MockCookie;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.session.Session;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import tools.jackson.databind.json.JsonMapper;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import({TestcontainersConfiguration.class, ConversationCreationContractTests.ResponseGate.class})
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"spring.session.jdbc.cleanup-cron=-", "logging.level.org.springframework.security=INFO",
        "logging.level.org.springframework.session=INFO", "app.security.allowed-origins=http://localhost:5173"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ConversationCreationContractTests {

    private static final OffsetDateTime OPEN_AT = OffsetDateTime.parse("2026-10-03T08:00:00.123456+09:00");
    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private JdbcIndexedSessionRepository sessions;
    @Autowired private SessionSecurityRepository security;
    @Autowired private ConversationClosureCoordinator closure;
    @Autowired private ResponseGate responseGate;
    @LocalServerPort private int port;
    @Autowired @Qualifier("cookieSerializer") private CookieSerializer cookies;
    @MockitoSpyBean private ConversationRepository repository;
    @MockitoSpyBean private ConversationService service;

    @BeforeEach
    void clearDatabase() {
        responseGate.reset();
        jdbc.update("DELETE FROM conversation_session_links");
        jdbc.update("DELETE FROM conversation_turns");
        jdbc.update("DELETE FROM conversations");
        jdbc.update("DELETE FROM SPRING_SESSION");
        jdbc.update("DELETE FROM email_verifications");
        jdbc.update("DELETE FROM children");
        jdbc.update("DELETE FROM guardian_pins");
        jdbc.update("DELETE FROM session_security");
        jdbc.update("DELETE FROM accounts");
        // 시각만 고정하고 HTTP 인증·CSRF·업무 트랜잭션과 저장은 실제 구현을 실행한다.
        doReturn(OPEN_AT).when(repository).currentTime();
    }

    @Test
    void returns201LocationAndOnlyFivePublicFieldsAfterRealCreation() throws Exception {
        var current = authenticated();
        var childId = child(current.accountId());
        var key = UUID.randomUUID();
        var response = mockMvc.perform(request(current, childId, key).header("Origin", "http://localhost:5173"))
                .andExpect(status().isCreated()).andReturn().getResponse();
        Map<String, Object> envelope = JsonPath.read(response.getContentAsString(), "$");
        Map<String, Object> data = JsonPath.read(response.getContentAsString(), "$.data");
        assertThat(envelope).containsOnlyKeys("data");
        assertThat(data).containsOnlyKeys("conversationId", "status", "startedAt", "serviceDate", "scheduledEndAt")
                .containsEntry("status", "ACTIVE").containsEntry("startedAt", "2026-10-03T08:00:00+09:00")
                .containsEntry("serviceDate", "2026-10-03").containsEntry("scheduledEndAt", "2026-10-04T00:00:00+09:00");
        var id = UUID.fromString((String) data.get("conversationId"));
        assertThat(response.getHeader("Location")).isEqualTo(path(childId) + "/" + id);
        assertThat(response.getHeader("Cache-Control")).contains("no-store");
        assertThat(response.getHeader("X-Request-ID")).isNotBlank();
        assertThat(response.getHeader("Access-Control-Allow-Origin")).isEqualTo("http://localhost:5173");
        assertThat(response.getHeader("Access-Control-Allow-Credentials")).isEqualTo("true");
        assertThat(jdbc.queryForObject("SELECT client_request_id FROM conversations WHERE id = ?", UUID.class, id)).isEqualTo(key);
        assertThat(jdbc.queryForObject("SELECT session_security_id FROM conversation_session_links WHERE conversation_id = ?",
                UUID.class, id)).isEqualTo(current.contextId());
        assertCounts(1, 1);
    }

    @Test
    void lostCreationResponseCanBeRecoveredWithOriginalKeyWithoutRebinding() throws Exception {
        var current = authenticated();
        var childId = child(current.accountId());
        var key = UUID.randomUUID();
        // 서버 커밋 뒤 응답 본문이 유실된 상황으로, 최초 응답의 ID·본문은 복구에 사용하지 않는다.
        mockMvc.perform(request(current, childId, key)).andExpect(status().isCreated());
        var conversation = jdbc.queryForMap("SELECT * FROM conversations WHERE child_id = ?", childId);
        var link = jdbc.queryForMap("SELECT * FROM conversation_session_links WHERE session_security_id = ?", current.contextId());
        var expected = Map.of("conversationId", conversation.get("id").toString(), "status", "ACTIVE",
                "startedAt", "2026-10-03T08:00:00+09:00", "serviceDate", "2026-10-03",
                "scheduledEndAt", "2026-10-04T00:00:00+09:00");
        doReturn(OPEN_AT.plusHours(3)).when(repository).currentTime();

        // 재전송과 쿠키 재발급 모두 같은 보안 문맥이면 최초 연결·시각을 그대로 보존한다.
        for (var browser : List.of(current, browserSession(current.accountId(), current.contextId()))) {
            var replay = mockMvc.perform(request(browser, childId, key)).andExpect(status().isOk())
                    .andReturn().getResponse();
            Map<String, Object> data = JsonPath.read(replay.getContentAsString(), "$.data");
            assertThat(data).isEqualTo(expected);
            assertThat(replay.getHeader("Location")).isNull();
            assertThat(replay.getHeader("Cache-Control")).contains("no-store");
        }
        assertThat(jdbc.queryForMap("SELECT * FROM conversations WHERE child_id = ?", childId)).isEqualTo(conversation);
        assertThat(jdbc.queryForMap("SELECT * FROM conversation_session_links WHERE session_security_id = ?", current.contextId()))
                .isEqualTo(link);
        assertCounts(1, 1);
    }

    @ParameterizedTest
    @CsvSource({"false,true", "true,true", "true,false"})
    @Timeout(25)
    void concurrentHttpStartsRespectKeyAndCurrentContext(boolean sameKey, boolean sameContext) throws Exception {
        var first = authenticated();
        var second = sameContext ? first : authenticated(first.accountId());
        var childId = child(first.accountId());
        var key = UUID.randomUUID();
        var calls = List.of(request(first, childId, key), request(second, childId, sameKey ? key : UUID.randomUUID()));
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        try (var workers = Executors.newFixedThreadPool(2)) {
            var pending = calls.stream().map(call -> workers.submit(() -> {
                ready.countDown();
                assertThat(start.await(10, TimeUnit.SECONDS)).isTrue();
                return mockMvc.perform(call).andReturn().getResponse();
            })).toList();
            try {
                assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            }
            finally {
                start.countDown();
            }
            var responses = List.of(pending.get(0).get(10, TimeUnit.SECONDS), pending.get(1).get(10, TimeUnit.SECONDS));
            int otherStatus = !sameKey ? 409 : sameContext ? 200 : 403;
            assertThat(responses).extracting(MockHttpServletResponse::getStatus).containsExactlyInAnyOrder(201, otherStatus);
            var created = responses.stream().filter(response -> response.getStatus() == 201).findFirst().orElseThrow();
            var other = responses.stream().filter(response -> response.getStatus() != 201).findFirst().orElseThrow();
            String id = JsonPath.read(created.getContentAsString(), "$.data.conversationId");
            assertThat(created.getHeader("Location")).isEqualTo(path(childId) + "/" + id);
            assertThat(other.getHeader("Location")).isNull();
            if (otherStatus == 200) {
                assertThat(other.getContentAsString()).isEqualTo(created.getContentAsString());
            }
            else {
                String code = JsonPath.read(other.getContentAsString(), "$.error.code");
                assertThat(code).isEqualTo(otherStatus == 409 ? "ACTIVE_CONVERSATION_EXISTS" : "CHILD_SESSION_REQUIRED");
                if (otherStatus == 409) {
                    String activeId = JsonPath.read(other.getContentAsString(), "$.error.activeConversationId");
                    assertThat(activeId).isEqualTo(id);
                }
            }
            assertThat(jdbc.queryForObject("SELECT id FROM conversations WHERE child_id = ?", UUID.class, childId))
                    .isEqualTo(UUID.fromString(id));
            var winner = responses.indexOf(created) == 0 ? first : second;
            assertThat(jdbc.queryForObject("SELECT session_security_id FROM conversation_session_links WHERE conversation_id = ?",
                    UUID.class, UUID.fromString(id))).isEqualTo(winner.contextId());
        }
        assertCounts(1, 1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM conversations WHERE status IN ('ACTIVE','CLOSING')", Integer.class))
                .isEqualTo(1);
    }

    @Test
    @Timeout(25)
    void invalidationAfterCreationCommitRemovesSuccessBodyLocationAndRenewedCookie() throws Exception {
        var current = authenticated();
        var childId = child(current.accountId());
        var call = request(current, childId, UUID.randomUUID());
        var committed = new CountDownLatch(1);
        var deliver = new CountDownLatch(1);
        doAnswer(invocation -> {
            var outcome = invocation.callRealMethod();
            // 실제 업무 커밋과 HTTP 응답 사이에 독립 트랜잭션의 문맥 폐기를 끼워 넣는다.
            committed.countDown();
            assertThat(deliver.await(10, TimeUnit.SECONDS)).isTrue();
            return outcome;
        }).when(service).create(any(), any(), any());
        try (var worker = Executors.newSingleThreadExecutor()) {
            var pending = worker.submit(() -> mockMvc.perform(call).andReturn().getResponse());
            UUID storedId;
            try {
                assertThat(committed.await(10, TimeUnit.SECONDS)).isTrue();
                storedId = jdbc.queryForObject("SELECT id FROM conversations WHERE child_id = ?", UUID.class, childId);
                assertThat(security.revokeCurrentContext(current.accountId(), current.contextId())).isTrue();
            }
            finally {
                deliver.countDown();
            }
            var response = pending.get(10, TimeUnit.SECONDS);
            assertThat(response.getStatus()).isEqualTo(401);
            Map<String, Object> detail = JsonPath.read(response.getContentAsString(), "$.error");
            assertThat(detail).containsOnlyKeys("code", "message", "requestId", "fields")
                    .containsEntry("code", "AUTHENTICATION_REQUIRED");
            assertThat(detail.get("requestId")).isEqualTo(response.getHeader("X-Request-ID"));
            assertThat(response.getHeader("Location")).isNull();
            assertThat(response.getHeader("Cache-Control")).contains("no-store");
            assertThat(response.getContentAsString()).doesNotContain("\"data\"", "startedAt", Objects.requireNonNull(storedId).toString());
            assertThat(response.getHeaders("Set-Cookie").stream().filter(value -> value.startsWith("JSESSIONID="))
                    .map(MockCookie::parse).anyMatch(cookie -> cookie.getMaxAge() > 0)).isFalse();
            mockMvc.perform(call).andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error.code").value("AUTHENTICATION_REQUIRED"));
        }
        // 늦은 권한 거부는 이미 완료된 업무 커밋을 취소하지 않으며, 재전송도 새 행을 만들지 못한다.
        assertCounts(1, 1);
    }

    @ParameterizedTest
    @CsvSource({"false,MIDNIGHT,403,SERVICE_HOURS_CLOSED", "true,MIDNIGHT,403,SERVICE_HOURS_CLOSED",
            "false,CLOSING,409,CONVERSATION_CLOSING", "true,CLOSING,409,CONVERSATION_CLOSING",
            "false,ENDED,409,CONVERSATION_ENDED", "true,ENDED,409,CONVERSATION_ENDED",
            "false,UNLINKED,403,CHILD_SESSION_REQUIRED", "true,UNLINKED,403,CHILD_SESSION_REQUIRED",
            "false,OWNER_CHANGED,404,RESOURCE_NOT_FOUND", "true,OWNER_CHANGED,404,RESOURCE_NOT_FOUND",
            "false,CLOSED_AND_ENDED,403,SERVICE_HOURS_CLOSED", "true,CLOSED_AND_ENDED,403,SERVICE_HOURS_CLOSED",
            "false,AUTH_AND_MIDNIGHT,401,AUTHENTICATION_REQUIRED", "true,AUTH_AND_MIDNIGHT,401,AUTHENTICATION_REQUIRED",
            "false,ENDED_WITH_NEW_ACTIVE,409,CONVERSATION_ENDED", "true,ENDED_WITH_NEW_ACTIVE,409,CONVERSATION_ENDED",
            "false,CONVERSATION_DELETED,404,RESOURCE_NOT_FOUND", "true,CONVERSATION_DELETED,404,RESOURCE_NOT_FOUND",
            "false,LINK_STORAGE_FAILURE,500,INTERNAL_ERROR", "true,LINK_STORAGE_FAILURE,500,INTERNAL_ERROR",
            "false,AUTH_STORAGE_FAILURE,503,AUTH_STATE_UNAVAILABLE", "true,AUTH_STORAGE_FAILURE,503,AUTH_STATE_UNAVAILABLE"})
    @Timeout(25)
    void delayedResponseRechecksCommittedConversationBeforeSending(boolean replay, String change,
            int expectedStatus, String expectedCode) throws Exception {
        var current = authenticated();
        var childId = child(current.accountId());
        var key = UUID.randomUUID();
        if (replay) {
            mockMvc.perform(request(current, childId, key)).andExpect(status().isCreated());
        }
        var call = request(current, childId, key).header("Origin", "http://localhost:5173");
        var pause = responseGate.pauseNext();
        UUID storedId;
        MockHttpServletResponse response;
        try (var worker = Executors.newSingleThreadExecutor()) {
            var pending = worker.submit(() -> mockMvc.perform(call).andReturn().getResponse());
            try {
                // Controller가 반환한 뒤 멈추므로 서비스 내부의 최종 검사로는 이 경합을 막을 수 없다.
                assertThat(pause.ready.await(10, TimeUnit.SECONDS)).isTrue();
                storedId = Objects.requireNonNull(jdbc.queryForObject(
                        "SELECT id FROM conversations WHERE child_id = ?", UUID.class, childId));
                switch (change) {
                    case "MIDNIGHT" -> doReturn(OPEN_AT.plusDays(1).withHour(0)).when(repository).currentTime();
                    case "CLOSING" -> {
                        jdbc.update("""
                                INSERT INTO conversation_turns (id, conversation_id, client_request_id, request_hash,
                                    sequence, status, child_text_visibility, reply_text_visibility, created_at, processing_deadline_at)
                                VALUES (?, ?, ?, ?, 1, 'PROCESSING', 'OMITTED', 'OMITTED', ?, ?)
                                """, UUID.randomUUID(), storedId, UUID.randomUUID(), "a".repeat(64), OPEN_AT, OPEN_AT.plusHours(2));
                        doReturn(OPEN_AT.plusSeconds(1)).when(repository).currentTime();
                        closure.reconcile(current.accountId(), childId, storedId, ConversationClosureCoordinator.Trigger.REQUEST_END);
                    }
                    case "ENDED", "CLOSED_AND_ENDED", "ENDED_WITH_NEW_ACTIVE" -> {
                        doReturn(OPEN_AT.plusSeconds(1)).when(repository).currentTime();
                        closure.reconcile(current.accountId(), childId, storedId, ConversationClosureCoordinator.Trigger.REQUEST_END);
                        if (change.equals("CLOSED_AND_ENDED")) {
                            doReturn(OPEN_AT.plusDays(1).withHour(0)).when(repository).currentTime();
                        }
                        if (change.equals("ENDED_WITH_NEW_ACTIVE")) {
                            mockMvc.perform(request(current, childId, UUID.randomUUID())).andExpect(status().isCreated());
                        }
                    }
                    case "UNLINKED" -> jdbc.update("DELETE FROM conversation_session_links WHERE conversation_id = ?", storedId);
                    case "OWNER_CHANGED" -> jdbc.update("UPDATE children SET account_id = ? WHERE id = ?",
                            authenticated().accountId(), childId);
                    case "AUTH_AND_MIDNIGHT" -> {
                        assertThat(security.revokeCurrentContext(current.accountId(), current.contextId())).isTrue();
                        doReturn(OPEN_AT.plusDays(1).withHour(0)).when(repository).currentTime();
                    }
                    case "CONVERSATION_DELETED" -> {
                        jdbc.update("DELETE FROM conversation_session_links WHERE conversation_id = ?", storedId);
                        jdbc.update("DELETE FROM conversations WHERE id = ?", storedId);
                    }
                    case "LINK_STORAGE_FAILURE" -> jdbc.execute(
                            "ALTER TABLE conversation_session_links RENAME TO unavailable_response_links");
                    case "AUTH_STORAGE_FAILURE" -> jdbc.execute(
                            "ALTER TABLE session_security RENAME TO unavailable_response_context");
                    default -> throw new AssertionError(change);
                }
            }
            finally {
                pause.resume.countDown();
            }
            try {
                response = pending.get(10, TimeUnit.SECONDS);
            }
            finally {
                if (change.equals("LINK_STORAGE_FAILURE")) {
                    jdbc.execute("ALTER TABLE unavailable_response_links RENAME TO conversation_session_links");
                }
                if (change.equals("AUTH_STORAGE_FAILURE")) {
                    jdbc.execute("ALTER TABLE unavailable_response_context RENAME TO session_security");
                }
            }
        }
        assertThat(response.getStatus()).isEqualTo(expectedStatus);
        Map<String, Object> detail = JsonPath.read(response.getContentAsString(), "$.error");
        assertThat(detail).containsOnlyKeys("code", "message", "requestId", "fields")
                .containsEntry("code", expectedCode).containsEntry("fields", List.of());
        assertThat(detail.get("requestId")).isEqualTo(response.getHeader("X-Request-ID"));
        assertThat(response.getContentAsString()).doesNotContain("\"data\"", "startedAt", storedId.toString());
        assertThat(response.getHeader("Location")).isNull();
        assertThat(response.getHeader("Content-Length")).isNull();
        assertThat(response.getHeader("Cache-Control")).contains("no-store");
        assertThat(response.getHeader("Access-Control-Allow-Origin")).isEqualTo("http://localhost:5173");
        assertThat(response.getHeaders("Set-Cookie").stream().filter(value -> value.startsWith("JSESSIONID="))
                .map(MockCookie::parse).anyMatch(cookie -> cookie.getMaxAge() > 0)).isFalse();
        // 전송 거부는 이미 커밋된 생성·재전송을 취소하거나 새 대화를 추가하지 않는다.
        int rows = change.equals("CONVERSATION_DELETED") ? 0 : change.equals("ENDED_WITH_NEW_ACTIVE") ? 2 : 1;
        assertCounts(rows, change.equals("UNLINKED") ? 0 : rows);
    }

    @ParameterizedTest
    @ValueSource(strings = {"MIDNIGHT", "ENDED"})
    @Timeout(25)
    void realHttpResponseDiscardsDelayedSuccessWithoutTruncatingError(String change) throws Exception {
        var current = authenticated();
        var childId = child(current.accountId());
        var csrf = csrf(current.cookie());
        var call = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path(childId)))
                .timeout(Duration.ofSeconds(15)).header("Content-Type", "application/json")
                .header("Origin", "http://localhost:5173")
                .header("X-XSRF-TOKEN", csrf.token())
                .header("Cookie", "JSESSIONID=" + current.cookie().getValue() + "; XSRF-TOKEN=" + csrf.cookie().getValue())
                .POST(HttpRequest.BodyPublishers.ofString(body(UUID.randomUUID()))).build();
        var pause = responseGate.pauseNext();
        UUID storedId;
        HttpResponse<String> response;
        try (var client = HttpClient.newHttpClient()) {
            var pending = client.sendAsync(call, HttpResponse.BodyHandlers.ofString());
            try {
                assertThat(pause.ready.await(10, TimeUnit.SECONDS)).isTrue();
                storedId = Objects.requireNonNull(jdbc.queryForObject(
                        "SELECT id FROM conversations WHERE child_id = ?", UUID.class, childId));
                if (change.equals("MIDNIGHT")) {
                    doReturn(OPEN_AT.plusDays(1).withHour(0)).when(repository).currentTime();
                }
                else {
                    doReturn(OPEN_AT.plusSeconds(1)).when(repository).currentTime();
                    closure.reconcile(current.accountId(), childId, storedId, ConversationClosureCoordinator.Trigger.REQUEST_END);
                }
            }
            finally {
                pause.resume.countDown();
            }
            response = pending.get(15, TimeUnit.SECONDS);
        }
        assertThat(response.statusCode()).isEqualTo(change.equals("MIDNIGHT") ? 403 : 409);
        Map<String, Object> envelope = JsonPath.read(response.body(), "$");
        Map<String, Object> detail = JsonPath.read(response.body(), "$.error");
        assertThat(envelope).containsOnlyKeys("error");
        assertThat(detail).containsOnlyKeys("code", "message", "requestId", "fields")
                .containsEntry("code", change.equals("MIDNIGHT") ? "SERVICE_HOURS_CLOSED" : "CONVERSATION_ENDED");
        assertThat(detail.get("requestId")).isEqualTo(response.headers().firstValue("X-Request-ID").orElseThrow());
        assertThat(response.body()).doesNotContain("\"data\"", "startedAt", storedId.toString());
        assertThat(response.headers().firstValue("Location")).isEmpty();
        response.headers().firstValueAsLong("Content-Length").ifPresent(length ->
                assertThat(length).isEqualTo(response.body().getBytes(StandardCharsets.UTF_8).length));
        assertThat(response.headers().firstValue("Cache-Control").orElseThrow()).contains("no-store");
        assertThat(response.headers().firstValue("Access-Control-Allow-Origin")).contains("http://localhost:5173");
        assertCounts(1, 1);
    }

    @Test
    void newLoginNeedsExplicitResumeEvenAfterLearningActiveConversationId() throws Exception {
        var original = authenticated();
        var childId = child(original.accountId());
        var key = UUID.randomUUID();
        var response = mockMvc.perform(request(original, childId, key))
                .andExpect(status().isCreated()).andReturn().getResponse();
        String id = JsonPath.read(response.getContentAsString(), "$.data.conversationId");
        var link = jdbc.queryForMap("SELECT * FROM conversation_session_links WHERE session_security_id = ?", original.contextId());
        var newLogin = authenticated(original.accountId());

        mockMvc.perform(request(newLogin, childId, UUID.randomUUID())).andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("ACTIVE_CONVERSATION_EXISTS"))
                .andExpect(jsonPath("$.error.activeConversationId").value(id));
        error(request(newLogin, childId, key), 403, "CHILD_SESSION_REQUIRED");
        error(request(newLogin, childId, key), 403, "CHILD_SESSION_REQUIRED");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM conversation_session_links WHERE session_security_id = ?",
                Integer.class, newLogin.contextId())).isZero();
        assertThat(jdbc.queryForMap("SELECT * FROM conversation_session_links WHERE session_security_id = ?", original.contextId()))
                .isEqualTo(link);
        assertCounts(1, 1);
    }

    @Test
    void sameKeyReplaysFromAnyAlreadyLinkedValidSession() throws Exception {
        var original = authenticated();
        var childId = child(original.accountId());
        var key = UUID.randomUUID();
        var response = mockMvc.perform(request(original, childId, key))
                .andExpect(status().isCreated()).andReturn().getResponse();
        String id = JsonPath.read(response.getContentAsString(), "$.data.conversationId");
        var linked = authenticated(original.accountId());
        // resume API를 구현하지 않고 이미 존재하는 다른 적격 연결을 fixture로 준비한다.
        jdbc.update("INSERT INTO conversation_session_links (session_security_id, conversation_id, bound_at) VALUES (?, ?, ?)",
                linked.contextId(), UUID.fromString(id), OPEN_AT.plusSeconds(1));
        var links = jdbc.queryForList("SELECT * FROM conversation_session_links ORDER BY session_security_id");
        doReturn(OPEN_AT.plusHours(1)).when(repository).currentTime();
        var replay = mockMvc.perform(request(linked, childId, key)).andExpect(status().isOk()).andReturn().getResponse();
        assertThat(replay.getContentAsString()).isEqualTo(response.getContentAsString());
        assertThat(jdbc.queryForList("SELECT * FROM conversation_session_links ORDER BY session_security_id")).isEqualTo(links);
        assertCounts(1, 2);
    }

    @Test
    void realRouteKeepsAuthenticationCsrfAndOwnershipPolicies() throws Exception {
        var csrf = csrf(null);
        error(post(path(UUID.randomUUID())).cookie(csrf.cookie()).header("X-XSRF-TOKEN", csrf.token())
                .contentType("application/json").content(body(UUID.randomUUID())), 401, "AUTHENTICATION_REQUIRED");
        var current = authenticated();
        var ownChild = child(current.accountId());
        error(post(path(ownChild)).cookie(current.cookie())
                .contentType("application/json").content(body(UUID.randomUUID())), 403, "CSRF_INVALID");
        error(request(current, UUID.randomUUID(), UUID.randomUUID()), 404, "RESOURCE_NOT_FOUND");
        var other = authenticated();
        error(request(current, child(other.accountId()), UUID.randomUUID()), 404, "RESOURCE_NOT_FOUND");
        error(request(current, ownChild, UUID.randomUUID()).content("{}"), 400, "VALIDATION_FAILED");
        assertCounts(0, 0);
    }

    @Test
    void closedHoursAtActualRouteDoesNotWriteRows() throws Exception {
        var current = authenticated();
        doReturn(OPEN_AT.withHour(7)).when(repository).currentTime();
        error(request(current, child(current.accountId()), UUID.randomUUID()), 403, "SERVICE_HOURS_CLOSED");
        assertCounts(0, 0);
    }

    @ParameterizedTest
    @ValueSource(strings = {"text/plain", "application/xml", "application/octet-stream", "application/problem+json",
            "application/json;charset=ISO-8859-1", "not a media type"})
    void unsupportedMediaTypePrecedesCsrfAndAuthentication(String type) throws Exception {
        var current = authenticated();
        var childId = child(current.accountId());
        var key = UUID.randomUUID();
        // 전송 형식은 정상 인증·CSRF, 각각의 누락, 동시 누락보다 먼저 거부한다.
        error(request(current, childId, key).contentType(type), 415, "UNSUPPORTED_MEDIA_TYPE");
        error(post(path(childId)).cookie(current.cookie()).contentType(type).content(body(key)),
                415, "UNSUPPORTED_MEDIA_TYPE");
        var anonymousCsrf = csrf(null);
        error(post(path(childId)).cookie(anonymousCsrf.cookie()).header("X-XSRF-TOKEN", anonymousCsrf.token())
                .contentType(type).content(body(key)), 415, "UNSUPPORTED_MEDIA_TYPE");
        error(post(path(childId)).contentType(type).content(body(key)), 415, "UNSUPPORTED_MEDIA_TYPE");
        assertCounts(0, 0);
    }

    @Test
    void missingContentTypeIs415EvenWithValidAuthenticationAndCsrf() throws Exception {
        var current = authenticated();
        var call = request(current, child(current.accountId()), UUID.randomUUID()).with(request -> {
            // MockMvc 빌더는 null 형식을 거부하므로 실제 요청의 헤더 누락을 구성한다.
            request.setContentType(null);
            return request;
        });
        error(call, 415, "UNSUPPORTED_MEDIA_TYPE");
        assertCounts(0, 0);
    }

    @ParameterizedTest
    @ValueSource(strings = {"application/json;charset=UTF-8", "APPLICATION/JSON;charset=utf-8"})
    void explicitUtf8JsonKeepsRealCreationAndReplay(String type) throws Exception {
        var current = authenticated();
        var childId = child(current.accountId());
        var key = UUID.randomUUID();
        mockMvc.perform(request(current, childId, key).contentType(type)).andExpect(status().isCreated());
        mockMvc.perform(request(current, childId, key).contentType(type)).andExpect(status().isOk());
        assertCounts(1, 1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{", "[]", "null", "{\"clientRequestId\":123}"})
    void invalidJsonRemains400AfterAuthenticationAndCsrf(String input) throws Exception {
        var current = authenticated();
        error(request(current, child(current.accountId()), UUID.randomUUID()).content(input),
                400, "VALIDATION_FAILED");
        assertCounts(0, 0);
    }

    @Test
    void validMediaTypeKeepsCsrfBeforeLoginAndJsonValidation() throws Exception {
        var childId = UUID.randomUUID();
        error(post(path(childId)).contentType("application/json").content("{"), 403, "CSRF_INVALID");
        var anonymousCsrf = csrf(null);
        error(post(path(childId)).cookie(anonymousCsrf.cookie()).header("X-XSRF-TOKEN", anonymousCsrf.token())
                .contentType("application/json").content("{"), 401, "AUTHENTICATION_REQUIRED");
        var current = authenticated();
        var currentCsrf = csrf(current.cookie());
        error(post(path(child(current.accountId()))).cookie(current.cookie(), currentCsrf.cookie())
                .header("X-XSRF-TOKEN", currentCsrf.token() + "invalid")
                .contentType("application/json").content("{"), 403, "CSRF_INVALID");
        assertCounts(0, 0);
    }

    @Test
    void existingActiveReturnsItsIdThroughDedicatedConflictSchema() throws Exception {
        var current = authenticated();
        var childId = child(current.accountId());
        var created = mockMvc.perform(request(current, childId, UUID.randomUUID()))
                .andExpect(status().isCreated()).andReturn().getResponse();
        String id = JsonPath.read(created.getContentAsString(), "$.data.conversationId");
        var response = mockMvc.perform(request(current, childId, UUID.randomUUID()))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error.code").value("ACTIVE_CONVERSATION_EXISTS"))
                .andReturn().getResponse();
        Map<String, Object> detail = JsonPath.read(response.getContentAsString(), "$.error");
        assertThat(detail).containsOnlyKeys("code", "message", "requestId", "fields", "activeConversationId")
                .containsEntry("activeConversationId", id).containsEntry("fields", List.of());
        assertThat(detail.get("requestId")).isEqualTo(response.getHeader("X-Request-ID"));
        assertThat(response.getHeader("Location")).isNull();
        assertCounts(1, 1);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void closingConversationBlocksSameAndDifferentKeysWithoutExposingId(boolean sameKey) throws Exception {
        var current = authenticated();
        var childId = child(current.accountId());
        var key = UUID.randomUUID();
        mockMvc.perform(request(current, childId, key)).andExpect(status().isCreated());
        markClosing(childId);
        error(request(current, childId, sameKey ? key : UUID.randomUUID()), 409, "CONVERSATION_CLOSING");
        assertCounts(1, 1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"EMPTY", "PENDING"})
    void endedConversationRejectsOldKeyButAllowsNewKeyOnSameDate(String summary) throws Exception {
        var current = authenticated();
        var childId = child(current.accountId());
        var originalKey = UUID.randomUUID();
        mockMvc.perform(request(current, childId, originalKey)).andExpect(status().isCreated());
        markEnded(childId, summary);
        doReturn(OPEN_AT.plusSeconds(1)).when(repository).currentTime();
        error(request(current, childId, originalKey), 409, "CONVERSATION_ENDED");
        mockMvc.perform(request(current, childId, UUID.randomUUID())).andExpect(status().isCreated());
        assertCounts(2, 2);
        assertThat(jdbc.queryForObject("SELECT count(DISTINCT service_date) FROM conversations", Integer.class)).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ACTIVE", "CLOSING"})
    void endedKeyTakesPriorityOverAnotherNewUnendedConversation(String newStatus) throws Exception {
        var current = authenticated();
        var childId = child(current.accountId());
        var oldKey = UUID.randomUUID();
        mockMvc.perform(request(current, childId, oldKey)).andExpect(status().isCreated());
        markEnded(childId, "PENDING");
        doReturn(OPEN_AT.plusSeconds(1)).when(repository).currentTime();
        mockMvc.perform(request(current, childId, UUID.randomUUID())).andExpect(status().isCreated());
        if (newStatus.equals("CLOSING")) {
            markClosing(childId);
        }
        var rows = jdbc.queryForList("SELECT * FROM conversations ORDER BY id");
        error(request(current, childId, oldKey), 409, "CONVERSATION_ENDED");
        assertThat(jdbc.queryForList("SELECT * FROM conversations ORDER BY id")).isEqualTo(rows);
        assertCounts(2, 2);
    }

    @ParameterizedTest
    @CsvSource({"ACTIVE,true,true", "ACTIVE,true,false", "ACTIVE,false,false", "CLOSING,true,false",
            "CLOSING,false,true", "ENDED,true,true", "ENDED,false,true"})
    void closedHoursPrecedeKeyStateAndLinkDecisions(String state, boolean sameKey, boolean linked) throws Exception {
        var current = authenticated();
        var childId = child(current.accountId());
        var key = UUID.randomUUID();
        mockMvc.perform(request(current, childId, key)).andExpect(status().isCreated());
        if (state.equals("CLOSING")) {
            markClosing(childId);
        }
        else if (state.equals("ENDED")) {
            markEnded(childId, "EMPTY");
        }
        var caller = linked ? current : authenticated(current.accountId());
        var before = jdbc.queryForList("SELECT * FROM conversations");
        doReturn(OPEN_AT.withHour(7)).when(repository).currentTime();
        error(request(caller, childId, sameKey ? key : UUID.randomUUID()), 403, "SERVICE_HOURS_CLOSED");
        assertThat(jdbc.queryForList("SELECT * FROM conversations")).isEqualTo(before);
        assertCounts(1, 1);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void replayCannotSucceedOrReportMissingLinkAfterCrossingMidnight(boolean linked) throws Exception {
        var original = authenticated();
        var childId = child(original.accountId());
        var key = UUID.randomUUID();
        mockMvc.perform(request(original, childId, key)).andExpect(status().isCreated());
        var caller = linked ? original : authenticated(original.accountId());
        var call = request(caller, childId, key);
        var before = jdbc.queryForMap("SELECT * FROM conversations WHERE child_id = ?", childId);
        var late = OPEN_AT.withHour(23).withMinute(59).withSecond(59);
        doReturn(late, late, OPEN_AT.plusDays(1).withHour(0)).when(repository).currentTime();
        error(call, 403, "SERVICE_HOURS_CLOSED");
        assertThat(jdbc.queryForMap("SELECT * FROM conversations WHERE child_id = ?", childId)).isEqualTo(before);
        assertCounts(1, 1);
    }

    @Test
    void previousDatesActiveIsEndedBeforeOldKeyIsRejected() throws Exception {
        var current = authenticated();
        var childId = child(current.accountId());
        var key = UUID.randomUUID();
        mockMvc.perform(request(current, childId, key)).andExpect(status().isCreated());
        doReturn(OPEN_AT.plusDays(1)).when(repository).currentTime();
        mockMvc.perform(request(current, childId, key)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONVERSATION_ENDED"));
        assertThat(jdbc.queryForMap("SELECT status, summary_status, end_reason FROM conversations WHERE child_id = ?", childId))
                .containsEntry("status", "ENDED").containsEntry("summary_status", "EMPTY").containsEntry("end_reason", "MIDNIGHT");
        assertCounts(1, 1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"REVOKED", "VERSION", "EXPIRED"})
    void sameKeyReplayKeepsExistingLoginInvalidationPolicies(String invalidation) throws Exception {
        var current = authenticated();
        var childId = child(current.accountId());
        var key = UUID.randomUUID();
        mockMvc.perform(request(current, childId, key)).andExpect(status().isCreated());
        var call = request(current, childId, key);
        switch (invalidation) {
            case "REVOKED" -> jdbc.update("UPDATE session_security SET revoked_at = clock_timestamp() WHERE id = ?", current.contextId());
            case "VERSION" -> jdbc.update("UPDATE accounts SET session_version = session_version + 1 WHERE id = ?", current.accountId());
            case "EXPIRED" -> jdbc.update("""
                    UPDATE session_security SET created_at = clock_timestamp() - INTERVAL '2 minutes',
                        expires_at = clock_timestamp() - INTERVAL '1 second' WHERE id = ?
                    """, current.contextId());
            default -> throw new AssertionError(invalidation);
        }
        error(call, 401, "AUTHENTICATION_REQUIRED");
        assertCounts(1, 1);
    }

    @Test
    void linkLookupFailureIs500RatherThanMissingSessionOrConflict() throws Exception {
        var current = authenticated();
        var childId = child(current.accountId());
        var key = UUID.randomUUID();
        mockMvc.perform(request(current, childId, key)).andExpect(status().isCreated());
        var call = request(current, childId, key);
        jdbc.execute("ALTER TABLE conversation_session_links RENAME TO unavailable_conversation_links");
        try {
            var response = error(call, 500, "INTERNAL_ERROR");
            assertThat(response.getContentAsString()).doesNotContain("SELECT session_security_id", "unavailable_conversation_links");
        }
        finally {
            jdbc.execute("ALTER TABLE unavailable_conversation_links RENAME TO conversation_session_links");
        }
        assertCounts(1, 1);
    }

    @Test
    void linkFailureReturns500AndLeavesNoConversationOrLocation() throws Exception {
        var current = authenticated();
        var call = request(current, child(current.accountId()), UUID.randomUUID());
        jdbc.execute("ALTER TABLE conversation_session_links RENAME TO unavailable_conversation_links");
        try {
            var response = error(call, 500, "INTERNAL_ERROR");
            assertThat(response.getContentAsString()).doesNotContain("INSERT INTO", "unavailable_conversation_links");
        }
        finally {
            jdbc.execute("ALTER TABLE unavailable_conversation_links RENAME TO conversation_session_links");
        }
        assertCounts(0, 0);
    }

    private MockHttpServletResponse error(MockHttpServletRequestBuilder call, int expected, String code) throws Exception {
        var response = mockMvc.perform(call).andExpect(status().is(expected))
                .andExpect(jsonPath("$.error.code").value(code)).andReturn().getResponse();
        Map<String, Object> detail = JsonPath.read(response.getContentAsString(), "$.error");
        assertThat(detail).containsOnlyKeys("code", "message", "requestId", "fields");
        assertThat(detail.get("requestId")).isEqualTo(response.getHeader("X-Request-ID"));
        assertThat(response.getHeader("Cache-Control")).contains("no-store");
        assertThat(response.getHeader("Location")).isNull();
        return response;
    }

    private MockHttpServletRequestBuilder request(Fixture current, UUID childId, UUID key) throws Exception {
        var csrf = csrf(current.cookie());
        return post(path(childId)).contentType("application/json").content(body(key))
                .cookie(current.cookie(), csrf.cookie()).header("X-XSRF-TOKEN", csrf.token());
    }

    private Csrf csrf(Cookie session) throws Exception {
        var call = get("/api/v1/auth/csrf");
        if (session != null) {
            call.cookie(session);
        }
        var response = mockMvc.perform(call).andExpect(status().isOk()).andReturn().getResponse();
        return new Csrf(response.getCookie("XSRF-TOKEN"), JsonPath.read(response.getContentAsString(), "$.token"));
    }

    private Fixture authenticated() {
        var accountId = UUID.randomUUID();
        jdbc.update("INSERT INTO accounts (id, email) VALUES (?, ?)", accountId, accountId + "@example.invalid");
        return authenticated(accountId);
    }

    private Fixture authenticated(UUID accountId) {
        var contextId = UUID.randomUUID();
        jdbc.update("INSERT INTO session_security (id, account_id, session_version) VALUES (?, ?, 0)", contextId, accountId);
        return browserSession(accountId, contextId);
    }

    private Fixture browserSession(UUID accountId, UUID contextId) {
        Session session = sessions.createSession();
        session.setMaxInactiveInterval(Duration.ofSeconds(-1));
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(accountId.toString(), null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))));
        session.setAttribute("accountId", accountId);
        session.setAttribute("securityContextId", contextId);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        var response = new MockHttpServletResponse();
        cookies.writeCookieValue(new CookieSerializer.CookieValue(new MockHttpServletRequest(), response, session.getId()));
        var setCookie = Objects.requireNonNull(response.getHeader("Set-Cookie"), "테스트 세션 쿠키가 발급되어야 합니다.");
        return new Fixture(accountId, contextId, MockCookie.parse(setCookie));
    }

    private void markClosing(UUID childId) {
        // 종료 조정 후에도 CLOSING인 경우는 원래 기한이 남은 실제 PROCESSING으로 구성한다.
        jdbc.update("""
                INSERT INTO conversation_turns (id, conversation_id, client_request_id, request_hash, sequence, status,
                    child_text_visibility, reply_text_visibility, created_at, processing_deadline_at)
                SELECT ?, id, ?, ?, 1, 'PROCESSING', 'OMITTED', 'OMITTED', started_at, started_at + INTERVAL '2 hours'
                FROM conversations WHERE child_id = ? AND status = 'ACTIVE'
                """, UUID.randomUUID(), UUID.randomUUID(), "a".repeat(64), childId);
        jdbc.update("""
                UPDATE conversations SET status = 'CLOSING', end_reason = 'MANUAL', end_requested_at = started_at
                WHERE child_id = ? AND status = 'ACTIVE'
                """, childId);
    }

    private void markEnded(UUID childId, String summary) {
        // 종료 API를 흉내내지 않고 제약을 만족하는 종료 상태를 DB fixture로 준비한다.
        jdbc.update("""
                UPDATE conversations SET status = 'ENDED', end_reason = 'MANUAL', end_requested_at = started_at,
                    ended_at = started_at, summary_status = ?,
                    summary_started_at = CASE WHEN ? = 'PENDING' THEN started_at ELSE NULL END,
                    summary_deadline_at = CASE WHEN ? = 'PENDING' THEN started_at + INTERVAL '60 seconds' ELSE NULL END
                WHERE child_id = ? AND status = 'ACTIVE'
                """, summary, summary, summary, childId);
    }

    private UUID child(UUID accountId) {
        var id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO children (id, account_id, name, nickname, birth_date, gender, character_id)
                VALUES (?, ?, '가나', '별이', DATE '2020-05-12', 'FEMALE', 'test-character')
                """, id, accountId);
        return id;
    }

    private String path(UUID childId) {
        return "/api/v1/children/" + childId + "/conversations";
    }

    private String body(UUID key) {
        return "{\"clientRequestId\":\"" + key + "\"}";
    }

    private void assertCounts(int conversations, int links) {
        assertThat(jdbc.queryForObject("SELECT count(*) FROM conversations", Integer.class)).isEqualTo(conversations);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM conversation_session_links", Integer.class)).isEqualTo(links);
    }

    private record Fixture(UUID accountId, UUID contextId, Cookie cookie) { }
    private record Csrf(Cookie cookie, String token) { }

    static class Pause {
        final CountDownLatch ready = new CountDownLatch(1);
        final CountDownLatch resume = new CountDownLatch(1);
    }

    @TestConfiguration(proxyBeanMethods = false)
    @ControllerAdvice(assignableTypes = ConversationController.class)
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
                    if (!pause.resume.await(10, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("응답 경합 대기가 끝나지 않았습니다.");
                    }
                }
                catch (InterruptedException failure) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(failure);
                }
                // 실제 성공 본문의 길이로 첫 전송 전 callback과 오래된 길이 제거도 검증한다.
                response.getHeaders().setContentLength(json.writeValueAsBytes(body).length);
            }
            return body;
        }
    }
}
