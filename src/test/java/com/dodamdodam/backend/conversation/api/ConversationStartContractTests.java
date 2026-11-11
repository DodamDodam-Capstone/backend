package com.dodamdodam.backend.conversation.api;

import com.dodamdodam.backend.TestcontainersConfiguration;
import com.dodamdodam.backend.conversation.domain.ConversationServiceHours;
import com.dodamdodam.backend.conversation.dto.StartConversationResult;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcPrint;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import({TestcontainersConfiguration.class, ConversationStartContractTests.Probe.class})
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@SpringBootTest(properties = {"spring.session.jdbc.cleanup-cron=-", "logging.level.org.springframework.security=INFO",
        "logging.level.org.springframework.session=INFO", "app.security.allowed-origins=http://localhost:5173"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ConversationStartContractTests {

    private static final String PROBE_PATH = "/api/v1/contract/conversation-start/";
    private static final UUID KEY = UUID.fromString("abcdefab-1234-5678-90ab-abcdefabcdef");
    private static final String BODY = "{\"clientRequestId\":\"" + KEY + "\"}";
    private static final OffsetDateTime OPEN_AT = OffsetDateTime.parse("2026-10-03T08:00:00+09:00");
    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private JdbcIndexedSessionRepository sessions;
    @Autowired @Qualifier("cookieSerializer") private CookieSerializer cookies;
    @Autowired private JsonMapper json;
    @Autowired private Probe probe;
    @Autowired @Qualifier("requestMappingHandlerMapping") private RequestMappingHandlerMapping mappings;

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
        probe.databaseTime = OPEN_AT;
        probe.prepared = null;
    }

    @AfterEach
    void validationDoesNotCreateConversationOrLink() {
        // 검증 소비자는 저장 서비스를 대신하지 않으며 어떤 성공 ID나 대화 행도 생성하지 않는다.
        assertThat(jdbc.queryForObject("SELECT count(*) FROM conversations", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM conversation_session_links", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM conversation_turns", Integer.class)).isZero();
    }

    @Test
    void productionMappingRegistersStartAfterCreationServiceIsAvailable() {
        assertThat(mappings.getHandlerMethods().values())
                .anyMatch(handler -> handler.getBeanType().equals(ConversationController.class));
        assertThat(mappings.getHandlerMethods().keySet())
                .anyMatch(mapping -> mapping.getPatternValues().contains("/api/v1/children/{childId}/conversations"));
    }

    @Test
    void preparesOnlyVerifiedSessionChildAndOriginalRequestKeyWithoutGuardianPin() throws Exception {
        var current = authenticated();
        var childId = child(current.accountId());
        var response = mockMvc.perform(request(current, childId.toString()).content(BODY)
                        .header("Origin", "http://localhost:5173"))
                .andExpect(status().isNoContent()).andReturn().getResponse();

        assertThat(probe.prepared.session().accountId()).isEqualTo(current.accountId());
        assertThat(probe.prepared.session().securityContextId()).isEqualTo(current.contextId());
        assertThat(probe.prepared.childId()).isEqualTo(childId);
        assertThat(probe.prepared.input().clientRequestId()).isEqualTo(KEY);
        assertThat(response.getContentAsString()).isEmpty();
        assertThat(response.getHeader("Location")).isNull();
        assertThat(response.getHeader("Cache-Control")).contains("no-store");
        assertThat(response.getHeader("Access-Control-Allow-Origin")).isEqualTo("http://localhost:5173");
        assertThat(response.getHeader("Access-Control-Allow-Credentials")).isEqualTo("true");
    }

    @Test
    void validCsrfWithoutLoginReturns401BeforeInputParsing() throws Exception {
        var csrf = csrf(null);
        error(post(PROBE_PATH + UUID.randomUUID()).cookie(csrf.cookie()).header("X-XSRF-TOKEN", csrf.token())
                .contentType("application/json").content("{"), 401, "AUTHENTICATION_REQUIRED");
    }

    @Test
    void missingCsrfPrecedesAuthenticationAndBodyParsing() throws Exception {
        error(post(PROBE_PATH + UUID.randomUUID()).contentType("application/json").content("{"), 403, "CSRF_INVALID");
    }

    @Test
    void csrfCookieWithoutMatchingHeaderIsRejected() throws Exception {
        var current = authenticated();
        var csrf = csrf(current.cookie());
        error(post(PROBE_PATH + child(current.accountId())).cookie(current.cookie(), csrf.cookie())
                .header("X-XSRF-TOKEN", csrf.token() + "invalid").contentType("application/json").content(BODY),
                403, "CSRF_INVALID");
    }

    @Test
    void missingAndOtherOwnersChildHaveSame404EvenOutsideHours() throws Exception {
        var current = authenticated();
        var other = authenticated();
        var otherChild = child(other.accountId());
        probe.databaseTime = OffsetDateTime.parse("2026-10-03T07:59:59+09:00");

        var unowned = error(request(current, otherChild.toString()).content(BODY), 404, "RESOURCE_NOT_FOUND");
        var missing = error(request(current, UUID.randomUUID().toString()).content(BODY), 404, "RESOURCE_NOT_FOUND");

        String unownedMessage = JsonPath.read(unowned.getContentAsString(), "$.error.message");
        String missingMessage = JsonPath.read(missing.getContentAsString(), "$.error.message");
        assertThat(unownedMessage).isEqualTo(missingMessage);
        assertThat(unowned.getContentAsString()).doesNotContain(current.accountId().toString(),
                other.accountId().toString(), otherChild.toString(), "별이");
    }

    @ParameterizedTest
    @ValueSource(strings = {"REVOKED", "VERSION", "EXPIRED"})
    void existingSessionRevocationVersionAndExpiryPoliciesStillApply(String change) throws Exception {
        var current = authenticated();
        var childId = child(current.accountId());
        var call = request(current, childId.toString()).content(BODY);
        switch (change) {
            case "REVOKED" -> jdbc.update("UPDATE session_security SET revoked_at = clock_timestamp() WHERE id = ?", current.contextId());
            case "VERSION" -> jdbc.update("UPDATE accounts SET session_version = session_version + 1 WHERE id = ?", current.accountId());
            case "EXPIRED" -> jdbc.update("""
                    UPDATE session_security SET created_at = clock_timestamp() - INTERVAL '2 minutes',
                        expires_at = clock_timestamp() - INTERVAL '1 second' WHERE id = ?
                    """, current.contextId());
            default -> throw new AssertionError(change);
        }
        error(call, 401, "AUTHENTICATION_REQUIRED");
    }

    @ParameterizedTest
    @ValueSource(strings = {"1-1-1-1-1", "not-a-uuid", "00000000-0000-0000-0000-000000000000extra"})
    void rejectsMalformedChildPathBeforeOwnershipLookup(String value) throws Exception {
        error(request(authenticated(), value).content(BODY), 400, "VALIDATION_FAILED", "childId");
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"clientRequestId\":null}", "{\"clientRequestId\":123}",
            "{\"clientRequestId\":\"1-1-1-1-1\"}"})
    void rejectsInvalidKeyThroughSharedErrorEnvelope(String body) throws Exception {
        var current = authenticated();
        error(request(current, child(current.accountId()).toString()).content(body), 400, "VALIDATION_FAILED", "clientRequestId");
    }

    @ParameterizedTest
    @ValueSource(strings = {"accountId", "securityContextId", "sessionId", "childId", "unknown"})
    void rejectsInjectedServerOwnedOrUndefinedBodyFields(String field) throws Exception {
        var current = authenticated();
        var body = json.writeValueAsString(Map.of("clientRequestId", KEY.toString(), field, "synthetic-value"));
        error(request(current, child(current.accountId()).toString()).content(body), 400, "VALIDATION_FAILED", "body");
    }

    @ParameterizedTest
    @ValueSource(strings = {"{", "", "[]", "null"})
    void existingJsonErrorHandlingIsReused(String body) throws Exception {
        var current = authenticated();
        error(request(current, child(current.accountId()).toString()).content(body), 400, "VALIDATION_FAILED", "body");
    }

    @ParameterizedTest
    @ValueSource(strings = {"2026-10-03T00:00:00+09:00", "2026-10-03T07:59:59.999999+09:00", "2026-10-04T00:00:00+09:00"})
    void closedHoursUses403AndNoValidationFields(String databaseTime) throws Exception {
        var current = authenticated();
        probe.databaseTime = OffsetDateTime.parse(databaseTime);
        error(request(current, child(current.accountId()).toString()).content(BODY), 403, "SERVICE_HOURS_CLOSED");
    }

    @ParameterizedTest
    @ValueSource(strings = {"2024-02-29T08:00:00+09:00", "2026-10-31T23:59:59.999999+09:00", "2026-12-31T23:59:59+09:00"})
    void serviceWindowMatchesPostgresqlKstDateAndMidnightExpressions(String value) {
        var now = OffsetDateTime.parse(value);
        var window = ConversationServiceHours.requireOpen(now);
        var date = jdbc.queryForObject("SELECT (?::timestamptz AT TIME ZONE 'Asia/Seoul')::date", LocalDate.class, now);
        var midnight = jdbc.queryForObject("SELECT ((?::date + 1)::timestamp AT TIME ZONE 'Asia/Seoul')",
                (row, _) -> row.getObject(1, OffsetDateTime.class), date);

        assertThat(window.serviceDate()).isEqualTo(date);
        assertThat(window.scheduledEndAt().toInstant()).isEqualTo(midnight.toInstant());
    }

    @Test
    void productionMapperWritesOnlyFiveStartResultFieldsInKstSeconds() {
        var result = new StartConversationResult(UUID.randomUUID(), "ACTIVE", OffsetDateTime.parse("2026-10-03T14:59:59.123456Z"),
                LocalDate.of(2026, 10, 3), OffsetDateTime.parse("2026-10-03T15:00:00Z"));
        var encoded = json.writeValueAsString(Map.of("data", result));
        Map<String, Object> envelope = JsonPath.read(encoded, "$");
        Map<String, Object> data = JsonPath.read(encoded, "$.data");

        assertThat(envelope).containsOnlyKeys("data");
        assertThat(data).containsOnlyKeys("conversationId", "status", "startedAt", "serviceDate", "scheduledEndAt")
                .containsEntry("conversationId", result.conversationId().toString()).containsEntry("status", "ACTIVE")
                .containsEntry("startedAt", "2026-10-03T23:59:59+09:00").containsEntry("serviceDate", "2026-10-03")
                .containsEntry("scheduledEndAt", "2026-10-04T00:00:00+09:00");
    }

    private MockHttpServletResponse error(MockHttpServletRequestBuilder call, int expectedStatus, String code,
                                           String... fields) throws Exception {
        var response = mockMvc.perform(call).andExpect(status().is(expectedStatus))
                .andExpect(jsonPath("$.error.code").value(code)).andReturn().getResponse();
        Map<String, Object> envelope = JsonPath.read(response.getContentAsString(), "$");
        Map<String, Object> error = JsonPath.read(response.getContentAsString(), "$.error");
        List<Map<String, Object>> errors = JsonPath.read(response.getContentAsString(), "$.error.fields");
        assertThat(envelope).containsOnlyKeys("error");
        assertThat(error).containsOnlyKeys("code", "message", "requestId", "fields");
        assertThat(error.get("requestId")).isEqualTo(response.getHeader("X-Request-ID"));
        assertThat((String) error.get("message")).isNotBlank();
        assertThat(errors).extracting(entry -> entry.get("field")).containsExactlyElementsOf(List.of(fields));
        assertThat(response.getHeader("Cache-Control")).contains("no-store");
        assertThat(response.getHeader("X-Request-ID"))
                .matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
        assertThat(response.getHeader("Location")).isNull();
        return response;
    }

    private MockHttpServletRequestBuilder request(Fixture current, String childId) throws Exception {
        var csrf = csrf(current.cookie());
        return post(PROBE_PATH + childId).contentType("application/json").cookie(current.cookie(), csrf.cookie())
                .header("X-XSRF-TOKEN", csrf.token());
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
        // 쿠키 발급 실패를 fixture 구성 단계에서 즉시 확인한다.
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

    private record Fixture(UUID accountId, UUID contextId, Cookie cookie) { }
    private record Csrf(Cookie cookie, String token) { }

    /** 운영 매핑과 분리된 검증 전용 소비자이며 대화 생성·성공 DTO를 흉내내지 않는다. */
    @TestConfiguration(proxyBeanMethods = false)
    @RestController
    static class Probe {
        private final ConversationController controller;
        private OffsetDateTime databaseTime = OPEN_AT;
        private ConversationController.PreparedStart prepared;

        Probe(ConversationController controller) {
            this.controller = controller;
        }

        @PostMapping(PROBE_PATH + "{childId}")
        @ResponseStatus(HttpStatus.NO_CONTENT)
        void validate(HttpServletRequest request, @PathVariable String childId, @RequestBody JsonNode body) {
            prepared = controller.prepareStart(request, childId, body);
            // 고정 시각은 정책 시험 전용이며 운영 서비스의 DB clock_timestamp 조회를 대신하지 않는다.
            ConversationServiceHours.requireOpen(databaseTime);
        }
    }
}
