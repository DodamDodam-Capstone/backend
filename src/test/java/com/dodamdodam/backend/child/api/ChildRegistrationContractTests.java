package com.dodamdodam.backend.child.api;

import com.dodamdodam.backend.TestcontainersConfiguration;
import com.dodamdodam.backend.auth.session.AuthenticatedSessionService;
import com.dodamdodam.backend.child.application.ChildCatalogueTestConfiguration;
import com.dodamdodam.backend.child.application.ChildService;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import({TestcontainersConfiguration.class, ChildCatalogueTestConfiguration.class,
        ChildRegistrationContractTests.AccessProbe.class})
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@SpringBootTest(properties = {"spring.session.jdbc.cleanup-cron=-", "logging.level.org.springframework.security=INFO",
        "logging.level.org.springframework.session=INFO", "APP_CORS_ALLOWED_ORIGINS=http://localhost:5173"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ChildRegistrationContractTests {

    private static final String PATH = "/api/v1/children";
    private static final String ACCESS_PATH = "/api/v1/child-contract/access/";
    private static final String BODY = """
            {"name": " 가나 ", "nickname": " 별 이 ", "birthDate": "2020-05-12", "gender": "FEMALE",
             "interests": [" 공 룡 ", "그림"], "characterId": "test-character"}
            """;

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private JdbcIndexedSessionRepository sessions;
    @Autowired private CookieSerializer cookieSerializer;

    @BeforeEach
    void clearDatabase() {
        jdbc.update("DELETE FROM children");
        jdbc.update("DELETE FROM SPRING_SESSION");
        jdbc.update("DELETE FROM email_verifications");
        jdbc.update("DELETE FROM session_security");
        jdbc.update("DELETE FROM accounts");
    }

    @Test
    void createsChildWith201AndLocation() throws Exception {
        var current = authenticated();
        var response = mockMvc.perform(registration(current, csrf(current.cookie())).content(BODY)
                        .header("Origin", "http://localhost:5173").header("X-Request-ID", "client-controlled"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("가나"))
                .andExpect(jsonPath("$.data.nickname").value("별 이"))
                .andExpect(jsonPath("$.data.birthDate").value("2020-05-12"))
                .andExpect(jsonPath("$.data.gender").value("FEMALE"))
                .andExpect(jsonPath("$.data.interests[0]").value("공 룡"))
                .andExpect(jsonPath("$.data.interests[1]").value("그림"))
                .andExpect(jsonPath("$.data.characterId").value("test-character"))
                .andReturn().getResponse();

        Map<String, Object> envelope = JsonPath.read(response.getContentAsString(), "$");
        Map<String, Object> child = JsonPath.read(response.getContentAsString(), "$.data");
        assertThat(envelope).containsOnlyKeys("data");
        assertThat(child).containsOnlyKeys("childId", "name", "nickname", "birthDate", "gender", "interests",
                "characterId", "createdAt");
        var childId = UUID.fromString((String) child.get("childId"));
        assertThat(response.getHeader("Location")).isEqualTo(PATH + "/" + childId);
        assertThat(jdbc.queryForObject("SELECT account_id FROM children WHERE id = ?", UUID.class, childId))
                .isEqualTo(current.accountId());
        assertChildCount(1);
        assertHeaders(response);
        assertThat(response.getHeader("Access-Control-Allow-Origin")).isEqualTo("http://localhost:5173");
        assertThat(response.getHeader("Access-Control-Allow-Credentials")).isEqualTo("true");
    }

    @Test
    void createdAtUsesKstSeconds() throws Exception {
        var current = authenticated();
        var response = mockMvc.perform(registration(current, csrf(current.cookie())).content(BODY))
                .andExpect(status().isCreated()).andReturn().getResponse();
        String createdAt = JsonPath.read(response.getContentAsString(), "$.data.createdAt");
        String childId = JsonPath.read(response.getContentAsString(), "$.data.childId");

        assertThat(createdAt).matches("[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}\\+09:00");
        assertThat(jdbc.queryForObject("SELECT date_trunc('second', created_at) = ?::timestamptz FROM children WHERE id = ?",
                Boolean.class, createdAt, UUID.fromString(childId))).isTrue();
    }

    @Test
    void omittedInterestsAreReturnedAsEmptyArray() throws Exception {
        var current = authenticated();
        mockMvc.perform(registration(current, csrf(current.cookie()))
                        .content(BODY.replace("\"interests\": [\" 공 룡 \", \"그림\"], ", "")))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.data.interests").isArray())
                .andExpect(jsonPath("$.data.interests").isEmpty());
        assertChildCount(1);
    }

    @Test
    void rejectsAdditionalAccountId() throws Exception {
        var current = authenticated();
        var suppliedAccount = UUID.randomUUID().toString();
        var body = BODY.replace("\"characterId\":", "\"accountId\": \"" + suppliedAccount + "\", \"characterId\":");

        var response = error(registration(current, csrf(current.cookie())).content(body),
                400, "VALIDATION_FAILED", "body");

        assertThat(response.getContentAsString()).doesNotContain(suppliedAccount);
        assertChildCount(0);
    }

    static Stream<Arguments> invalidBodies() {
        return Stream.of(
                Arguments.of("", "body"),
                Arguments.of(" \n", "body"),
                Arguments.of("{\"RAW_INPUT_MARKER\":", "body"),
                Arguments.of(BODY + " {}", "body"),
                Arguments.of("null", "body"),
                Arguments.of("[]", "body"),
                Arguments.of(BODY.replace("\"name\": \" 가나 \"", "\"name\": 123"), "name"),
                Arguments.of(BODY.replace("\"nickname\": \" 별 이 \"", "\"nickname\": null"), "nickname"),
                Arguments.of(BODY.replace("\" 별 이 \"", "\"RAW_INPUT_MARKER_WITH_MORE_THAN_TWENTY_CHARACTERS\""), "nickname"),
                Arguments.of(BODY.replace("[\" 공 룡 \", \"그림\"]", "null"), "interests"),
                Arguments.of(BODY.replace("2020-05-12", "2026-11-02"), "birthDate"),
                Arguments.of(BODY.replace("FEMALE", "OTHER"), "gender"),
                Arguments.of(BODY.replace("test-character", "unknown-character"), "characterId"));
    }

    @ParameterizedTest(name = "invalid body case {index}")
    @MethodSource("invalidBodies")
    void invalidJsonOrProfileReturnsSanitized400(String body, String field) throws Exception {
        var current = authenticated();

        var response = error(registration(current, csrf(current.cookie())).content(body),
                400, "VALIDATION_FAILED", field);

        assertThat(response.getContentAsString()).doesNotContain("RAW_INPUT_MARKER");
        assertChildCount(0);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{", "null", "{}"})
    void unauthenticatedRequestDoesNotInsert(String body) throws Exception {
        var csrf = csrf(null);
        error(post(PATH).contentType("application/json").cookie(csrf.cookie()).header("X-XSRF-TOKEN", csrf.token())
                .content(body), 401, "AUTHENTICATION_REQUIRED");
        assertChildCount(0);
    }

    @ParameterizedTest
    @ValueSource(strings = {"EXPIRED", "REVOKED", "ACCOUNT_VERSION", "NATIVE_DELETED"})
    void expiredOrRevokedSessionDoesNotExposeChild(String invalid) throws Exception {
        var current = authenticated();
        var csrf = csrf(current.cookie());
        var childId = seedChild(current.accountId());
        switch (invalid) {
            case "EXPIRED" -> jdbc.update("UPDATE session_security SET expires_at = clock_timestamp() WHERE id = ?",
                    current.contextId());
            case "REVOKED" -> jdbc.update("UPDATE session_security SET revoked_at = clock_timestamp() WHERE id = ?",
                    current.contextId());
            case "ACCOUNT_VERSION" -> jdbc.update("UPDATE accounts SET session_version = 1 WHERE id = ?",
                    current.accountId());
            case "NATIVE_DELETED" -> sessions.deleteById(current.sessionId());
            default -> throw new AssertionError("Unknown fixture");
        }

        var response = error(registration(current, csrf).content(BODY), 401, "AUTHENTICATION_REQUIRED");

        assertThat(response.getContentAsString()).doesNotContain(childId.toString(), "별 이", "가나");
        assertChildCount(1);
    }

    @Test
    void duplicateRegistrationReturns409() throws Exception {
        var current = authenticated();
        var csrf = csrf(current.cookie());
        mockMvc.perform(registration(current, csrf).content(BODY)).andExpect(status().isCreated());

        error(registration(current, csrf).content(BODY), 409, "CHILD_LIMIT_REACHED");

        assertChildCount(1);
    }

    @Test
    void childStorageFailureIsNotAuthStateFailure() throws Exception {
        var current = authenticated();
        var csrf = csrf(current.cookie());
        jdbc.execute("ALTER TABLE children RENAME TO unavailable_children");
        try {
            var response = error(registration(current, csrf).content(BODY), 500, "INTERNAL_ERROR");
            assertThat(response.getContentAsString()).doesNotContain("unavailable_children", "SELECT", "INSERT", "PSQL");
        }
        finally {
            jdbc.execute("ALTER TABLE unavailable_children RENAME TO children");
        }
        assertChildCount(0);
    }

    @ParameterizedTest
    @ValueSource(strings = {"session_security", "SPRING_SESSION"})
    void authenticationStoreFailureReturns503(String table) throws Exception {
        var current = authenticated();
        var csrf = csrf(current.cookie());
        jdbc.execute("ALTER TABLE " + table + " RENAME TO unavailable_auth_store");
        try {
            error(registration(current, csrf).content(BODY), 503, "AUTH_STATE_UNAVAILABLE");
        }
        finally {
            jdbc.execute("ALTER TABLE unavailable_auth_store RENAME TO " + table);
        }
        assertChildCount(0);
    }

    @Test
    void unownedAndMissingChildHaveSame404Envelope() throws Exception {
        var owner = authenticated();
        var other = authenticated();
        var childId = seedChild(owner.accountId());
        mockMvc.perform(get(ACCESS_PATH + childId).cookie(owner.cookie())).andExpect(status().isNoContent());

        var unowned = error(get(ACCESS_PATH + childId).cookie(other.cookie()), 404, "RESOURCE_NOT_FOUND");
        var missing = error(get(ACCESS_PATH + UUID.randomUUID()).cookie(other.cookie()), 404, "RESOURCE_NOT_FOUND");

        assertThat(JsonPath.<String>read(unowned.getContentAsString(), "$.error.message"))
                .isEqualTo(JsonPath.read(missing.getContentAsString(), "$.error.message"));
        assertThat(unowned.getContentAsString()).doesNotContain(childId.toString(), "별 이", "가나");
        assertChildCount(1);
    }

    @Test
    void oversizedBodyPrecedesCsrf() throws Exception {
        error(post(PATH).contentType("application/json").content(" ".repeat(16385)), 413, "REQUEST_TOO_LARGE");
        assertChildCount(0);
    }

    @Test
    void unsupportedMediaTypePrecedesCsrf() throws Exception {
        error(post(PATH).contentType("text/plain").content(BODY), 415, "UNSUPPORTED_MEDIA_TYPE");
        assertChildCount(0);
    }

    @Test
    void missingCsrfPrecedesAuthentication() throws Exception {
        error(post(PATH).contentType("application/json").content("{"), 403, "CSRF_INVALID");
        assertChildCount(0);
    }

    private MockHttpServletResponse error(MockHttpServletRequestBuilder call, int expectedStatus, String code,
                                           String... fields) throws Exception {
        var response = mockMvc.perform(call).andExpect(status().is(expectedStatus))
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.error.code").value(code)).andReturn().getResponse();
        Map<String, Object> envelope = JsonPath.read(response.getContentAsString(), "$");
        Map<String, Object> error = JsonPath.read(response.getContentAsString(), "$.error");
        List<Map<String, Object>> errors = JsonPath.read(response.getContentAsString(), "$.error.fields");
        assertThat(envelope).containsOnlyKeys("error");
        assertThat(error).containsOnlyKeys("code", "message", "requestId", "fields");
        assertThat((String) error.get("message")).isNotBlank();
        assertThat(error.get("requestId")).isEqualTo(response.getHeader("X-Request-ID"));
        assertThat(errors).extracting(entry -> entry.get("field")).containsExactlyElementsOf(List.of(fields));
        for (var field : errors) {
            assertThat(field).containsOnlyKeys("field", "reason");
            assertThat((String) field.get("reason")).isNotBlank();
        }
        assertThat(response.getHeader("Location")).isNull();
        assertHeaders(response);
        return response;
    }

    private void assertHeaders(MockHttpServletResponse response) {
        assertThat(response.getHeader("Cache-Control")).contains("no-store");
        assertThat(response.getHeader("X-Request-ID"))
                .matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
        assertThat(response.getContentType()).startsWith("application/json");
    }

    private void assertChildCount(int count) {
        assertThat(jdbc.queryForObject("SELECT count(*) FROM children", Integer.class)).isEqualTo(count);
    }

    private MockHttpServletRequestBuilder registration(Fixture current, Csrf csrf) {
        return post(PATH).contentType("application/json").cookie(current.cookie(), csrf.cookie())
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
        // 운영과 같은 IMMEDIATE 정책이 각 속성 변경을 실제 JDBC 세션에 저장한다.
        Session session = sessions.createSession();
        session.setMaxInactiveInterval(Duration.ofSeconds(-1));
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(accountId.toString(), null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))));
        session.setAttribute("accountId", accountId);
        session.setAttribute("securityContextId", contextId);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        var response = new MockHttpServletResponse();
        cookieSerializer.writeCookieValue(new CookieSerializer.CookieValue(new MockHttpServletRequest(), response, session.getId()));
        return new Fixture(accountId, contextId, session.getId(), MockCookie.parse(response.getHeader("Set-Cookie")));
    }

    private UUID seedChild(UUID accountId) {
        var id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO children (id, account_id, name, nickname, birth_date, gender, interests, character_id)
                VALUES (?, ?, '가나', '별 이', '2020-05-12', 'FEMALE', ARRAY['공 룡', '그림'], 'test-character')
                """, id, accountId);
        return id;
    }

    private record Fixture(UUID accountId, UUID contextId, String sessionId, Cookie cookie) { }
    private record Csrf(Cookie cookie, String token) { }

    @TestConfiguration(proxyBeanMethods = false)
    @RestController
    static class AccessProbe {
        private final AuthenticatedSessionService authentication;
        private final ChildService children;

        AccessProbe(AuthenticatedSessionService authentication, ChildService children) {
            this.authentication = authentication;
            this.children = children;
        }

        @GetMapping(ACCESS_PATH + "{childId}")
        @ResponseStatus(HttpStatus.NO_CONTENT)
        void ownedChild(HttpServletRequest request, @PathVariable UUID childId) {
            var current = authentication.requireAuthenticatedSession(request);
            children.requireChildAccess(current.accountId(), childId);
        }
    }
}
