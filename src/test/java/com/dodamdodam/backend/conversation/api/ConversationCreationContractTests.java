package com.dodamdodam.backend.conversation.api;

import com.dodamdodam.backend.TestcontainersConfiguration;
import com.dodamdodam.backend.conversation.infrastructure.ConversationRepository;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcPrint;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@SpringBootTest(properties = {"spring.session.jdbc.cleanup-cron=-", "logging.level.org.springframework.security=INFO",
        "logging.level.org.springframework.session=INFO", "app.security.allowed-origins=http://localhost:5173"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ConversationCreationContractTests {

    private static final OffsetDateTime OPEN_AT = OffsetDateTime.parse("2026-10-03T08:00:00.123456+09:00");
    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private JdbcIndexedSessionRepository sessions;
    @Autowired @Qualifier("cookieSerializer") private CookieSerializer cookies;
    @MockitoSpyBean private ConversationRepository repository;

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

    @Test
    void closingConversationBlocksNewStartWithoutExposingIdInGeneralError() throws Exception {
        var current = authenticated();
        var childId = child(current.accountId());
        mockMvc.perform(request(current, childId, UUID.randomUUID())).andExpect(status().isCreated());
        jdbc.update("""
                UPDATE conversations SET status = 'CLOSING', end_reason = 'MANUAL', end_requested_at = started_at
                WHERE child_id = ?
                """, childId);
        error(request(current, childId, UUID.randomUUID()), 409, "CONVERSATION_CLOSING");
        assertCounts(1, 1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"EMPTY", "PENDING"})
    void endedConversationRejectsOldKeyButAllowsNewKeyOnSameDate(String summary) throws Exception {
        var current = authenticated();
        var childId = child(current.accountId());
        var originalKey = UUID.randomUUID();
        mockMvc.perform(request(current, childId, originalKey)).andExpect(status().isCreated());
        // 종료 API를 흉내내지 않고 제약을 만족하는 종료 상태를 DB fixture로 준비한다.
        jdbc.update("""
                UPDATE conversations SET status = 'ENDED', end_reason = 'MANUAL', end_requested_at = started_at,
                    ended_at = started_at, summary_status = ?,
                    summary_started_at = CASE WHEN ? = 'PENDING' THEN started_at ELSE NULL END,
                    summary_deadline_at = CASE WHEN ? = 'PENDING' THEN started_at + INTERVAL '60 seconds' ELSE NULL END
                WHERE child_id = ?
                """, summary, summary, summary, childId);
        doReturn(OPEN_AT.plusSeconds(1)).when(repository).currentTime();
        error(request(current, childId, originalKey), 409, "CONVERSATION_ENDED");
        mockMvc.perform(request(current, childId, UUID.randomUUID())).andExpect(status().isCreated());
        assertCounts(2, 2);
        assertThat(jdbc.queryForObject("SELECT count(DISTINCT service_date) FROM conversations", Integer.class)).isEqualTo(1);
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
        var contextId = UUID.randomUUID();
        jdbc.update("INSERT INTO accounts (id, email) VALUES (?, ?)", accountId, accountId + "@example.invalid");
        jdbc.update("INSERT INTO session_security (id, account_id, session_version) VALUES (?, ?, 0)", contextId, accountId);
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
}
