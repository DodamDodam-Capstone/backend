package com.dodamdodam.backend;

import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcPrint;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import({TestcontainersConfiguration.class, LogoutContractTests.Probe.class})
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"spring.session.jdbc.cleanup-cron=-", "logging.level.org.springframework.security=INFO",
                "logging.level.org.springframework.session=INFO", "SESSION_COOKIE_SECURE=false",
                "SESSION_COOKIE_SAME_SITE=lax", "APP_CORS_ALLOWED_ORIGINS=http://localhost:5173"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class LogoutContractTests {

    private static final String PATH = "/api/v1/auth/logout";
    private static final String PROBE = "/api/v1/logout-contract/probe";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private JdbcIndexedSessionRepository sessions;
    @Autowired private CookieSerializer cookieSerializer;
    @LocalServerPort private int port;

    @BeforeEach
    void cleanDatabase() {
        jdbc.update("DELETE FROM SPRING_SESSION");
        jdbc.update("DELETE FROM email_verifications");
        jdbc.update("DELETE FROM session_security");
        jdbc.update("DELETE FROM accounts");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"application/json", "application/json;charset=UTF-8"})
    void zeroByteLogoutRevokesOnlyCurrentCredentialsAndDeletesCookies(String contentType) throws Exception {
        var current = authenticated();
        var other = authenticated();
        var csrf = csrf(current.cookie());
        var call = logout(current, csrf).header("Origin", "http://localhost:5173");
        if (contentType != null) call.contentType(contentType);
        var response = mockMvc.perform(call).andExpect(status().isNoContent()).andReturn().getResponse();

        assertThat(response.getContentAsByteArray()).isEmpty();
        assertHeaders(response);
        assertThat(response.getHeader("Access-Control-Allow-Origin")).isEqualTo("http://localhost:5173");
        assertThat(response.getHeader("Access-Control-Allow-Credentials")).isEqualTo("true");
        assertThat(response.getHeader("Location")).isNull();
        assertRevoked(current);
        assertThat(sessions.findById(current.sessionId()) == null).as("native session is deleted").isTrue();
        assertCookieExpired(response, "JSESSIONID", true);
        assertCookieExpired(response, "XSRF-TOKEN", false);
        assertActive(other);
        error(get(PROBE).cookie(current.cookie()), 401, "AUTHENTICATION_REQUIRED");
        mockMvc.perform(get(PROBE).cookie(other.cookie())).andExpect(status().isNoContent());
    }

    static Stream<Arguments> rejectedRequests() {
        return Stream.of(
                Arguments.of("{}", "application/json", true, "valid", 400, "VALIDATION_FAILED"),
                Arguments.of("null", "application/json", true, "valid", 400, "VALIDATION_FAILED"),
                Arguments.of(" \n", "application/json", true, "valid", 400, "VALIDATION_FAILED"),
                Arguments.of("", null, false, "valid", 401, "AUTHENTICATION_REQUIRED"),
                Arguments.of("{}", "application/json", false, "valid", 401, "AUTHENTICATION_REQUIRED"),
                Arguments.of("{}", "application/json", true, "missing", 403, "CSRF_INVALID"),
                Arguments.of("{}", "application/json", true, "wrong", 403, "CSRF_INVALID"),
                Arguments.of("", null, false, "missing", 403, "CSRF_INVALID"),
                Arguments.of("x".repeat(16385), "application/json", true, "missing", 413, "REQUEST_TOO_LARGE"),
                Arguments.of("{}", "text/plain", true, "missing", 415, "UNSUPPORTED_MEDIA_TYPE"),
                Arguments.of("{}", null, true, "valid", 415, "UNSUPPORTED_MEDIA_TYPE"),
                Arguments.of("{}", "not a media type", false, "missing", 415, "UNSUPPORTED_MEDIA_TYPE"),
                Arguments.of("{}", "application/*", false, "missing", 415, "UNSUPPORTED_MEDIA_TYPE"),
                Arguments.of("{}", "application/json;charset=ISO-8859-1", true, "missing", 415, "UNSUPPORTED_MEDIA_TYPE"));
    }

    @ParameterizedTest(name = "request validation case {index}")
    @MethodSource("rejectedRequests")
    void rejectionUsesContractPriorityWithoutRevokingSession(String body, String type, boolean loggedIn,
            String csrfMode, int expectedStatus, String code) throws Exception {
        var current = loggedIn ? authenticated() : null;
        var csrf = csrf(current == null ? null : current.cookie());
        var call = post(PATH).content(body.getBytes(StandardCharsets.UTF_8));
        if (current != null) call.cookie(current.cookie());
        if (type != null) call.header("Content-Type", type);
        if (!csrfMode.equals("missing")) {
            call.cookie(csrf.cookie()).header("X-XSRF-TOKEN", csrfMode.equals("valid") ? csrf.token() : "wrong");
        }
        error(call, expectedStatus, code);
        if (current != null) assertActive(current);
    }

    @ParameterizedTest
    @CsvSource({"16384,400,VALIDATION_FAILED", "16385,413,REQUEST_TOO_LARGE"})
    void actualUtf8ByteLimitIncludesWhitespace(int size, int expectedStatus, String code) throws Exception {
        var current = authenticated();
        var csrf = csrf(current.cookie());
        // '가'는 UTF-8 3바이트이므로 문자 수가 아닌 전송 바이트 경계를 검증한다.
        String body = "가".repeat(5461) + " ".repeat(size - 16383);
        error(logout(current, csrf).contentType("application/json").content(body.getBytes(StandardCharsets.UTF_8)),
                expectedStatus, code);
        assertActive(current);
    }

    @ParameterizedTest
    @CsvSource({"0,204", "2,400", "16384,400", "16385,413"})
    void chunkedRequestsWithoutContentLengthUseActualBytes(int size, int expectedStatus) throws Exception {
        var current = authenticated();
        var csrf = csrf(current.cookie());
        byte[] body = " ".repeat(size).getBytes(StandardCharsets.UTF_8);
        var call = HttpRequest.newBuilder(URI.create("http://localhost:" + port + PATH))
                .timeout(Duration.ofSeconds(10)).header("Content-Type", "application/json")
                .header("X-XSRF-TOKEN", csrf.token())
                .header("Cookie", "JSESSIONID=" + current.cookie().getValue() + "; XSRF-TOKEN=" + csrf.cookie().getValue())
                .POST(HttpRequest.BodyPublishers.ofInputStream(() -> new ByteArrayInputStream(body))).build();
        try (var client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build()) {
            var response = client.send(call, HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).isEqualTo(expectedStatus);
            assertThat(response.headers().firstValue("Cache-Control")).hasValue("no-store");
            if (expectedStatus != 204) {
                String code = JsonPath.read(response.body(), "$.error.code");
                assertThat(code).isEqualTo(expectedStatus == 413 ? "REQUEST_TOO_LARGE" : "VALIDATION_FAILED");
                assertActive(current);
            }
            else {
                assertThat(response.body()).isEmpty();
                assertRevoked(current);
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"session_security", "email_verifications"})
    void businessFailureRollsBackAndKeepsFrameworkSession(String table) throws Exception {
        var current = authenticated();
        var csrf = csrf(current.cookie());
        createFailureTrigger(table, "UPDATE");
        try {
            error(logout(current, csrf), 503, "AUTH_STATE_UNAVAILABLE");
            assertActive(current);
            assertThat(sessions.findById(current.sessionId()) != null).as("native cleanup did not run").isTrue();
            mockMvc.perform(get(PROBE).cookie(current.cookie())).andExpect(status().isNoContent());
        }
        finally {
            dropFailureTrigger(table);
        }
    }

    @Test
    void nativeDeletionFailureReturns503AfterBusinessCommit() throws Exception {
        var current = authenticated();
        var csrf = csrf(current.cookie());
        createFailureTrigger("SPRING_SESSION", "DELETE");
        try {
            error(logout(current, csrf), 503, "AUTH_STATE_UNAVAILABLE");
            assertRevoked(current);
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM SPRING_SESSION WHERE SESSION_ID = ?",
                    Integer.class, current.sessionId())).isEqualTo(1);
            error(get(PROBE).cookie(current.cookie()), 401, "AUTHENTICATION_REQUIRED");
        }
        finally {
            dropFailureTrigger("SPRING_SESSION");
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"GET", "POST"})
    void legacyLogoutDoesNotInvalidateOrRedirect(String method) throws Exception {
        var current = authenticated();
        var csrf = csrf(current.cookie());
        var response = mockMvc.perform(request(HttpMethod.valueOf(method), "/logout").cookie(current.cookie(), csrf.cookie())
                        .header("X-XSRF-TOKEN", csrf.token()))
                .andExpect(status().isNotFound()).andReturn().getResponse();
        assertThat(response.getHeader("Location")).isNull();
        assertActive(current);
        mockMvc.perform(get(PROBE).cookie(current.cookie())).andExpect(status().isNoContent());
    }

    private MockHttpServletRequestBuilder logout(Fixture current, Csrf csrf) {
        return post(PATH).cookie(current.cookie(), csrf.cookie()).header("X-XSRF-TOKEN", csrf.token());
    }

    private MockHttpServletResponse error(MockHttpServletRequestBuilder call, int status, String code) throws Exception {
        var response = mockMvc.perform(call).andExpect(status().is(status))
                .andExpect(jsonPath("$.error.code").value(code)).andExpect(jsonPath("$.error.fields").isEmpty())
                .andReturn().getResponse();
        assertHeaders(response);
        assertThat(JsonPath.<String>read(response.getContentAsString(), "$.error.requestId"))
                .isEqualTo(response.getHeader("X-Request-ID"));
        return response;
    }

    private void assertHeaders(MockHttpServletResponse response) {
        assertThat(response.getHeader("Cache-Control")).contains("no-store");
        assertThat(response.getHeader("X-Request-ID"))
                .matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }

    private void assertCookieExpired(MockHttpServletResponse response, String name, boolean httpOnly) {
        var cookie = response.getCookie(name);
        assertThat(cookie != null).as("expiry cookie is present").isTrue();
        assertThat(cookie.getMaxAge()).isZero();
        assertThat(cookie.getPath()).isEqualTo("/");
        assertThat(cookie.getDomain()).isNull();
        assertThat(cookie.isHttpOnly()).isEqualTo(httpOnly);
        assertThat(cookie.getSecure()).isFalse();
        assertThat(cookie.getAttribute("SameSite")).isEqualTo("Lax");
    }

    private void assertActive(Fixture fixture) {
        var row = jdbc.queryForMap("SELECT revoked_at, guardian_unlocked_until, guardian_pin_version "
                + "FROM session_security WHERE id = ?", fixture.contextId());
        assertThat(row.get("revoked_at")).isNull();
        assertThat(row.get("guardian_unlocked_until")).isNotNull();
        assertThat(row.get("guardian_pin_version")).isEqualTo(3L);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM email_verifications WHERE setup_session_binding = ? "
                + "AND invalidated_at IS NULL", Integer.class, fixture.contextId())).isEqualTo(2);
    }

    private void assertRevoked(Fixture fixture) {
        var row = jdbc.queryForMap("SELECT revoked_at, guardian_unlocked_until, guardian_pin_version "
                + "FROM session_security WHERE id = ?", fixture.contextId());
        assertThat(row.get("revoked_at")).isNotNull();
        assertThat(row.get("guardian_unlocked_until")).isNull();
        assertThat(row.get("guardian_pin_version")).isNull();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM email_verifications WHERE setup_session_binding = ? "
                + "AND invalidated_at IS NOT NULL", Integer.class, fixture.contextId())).isEqualTo(2);
    }

    private Csrf csrf(Cookie session) throws Exception {
        var call = get("/api/v1/auth/csrf");
        if (session != null) call.cookie(session);
        var response = mockMvc.perform(call).andExpect(status().isOk()).andReturn().getResponse();
        return new Csrf(response.getCookie("XSRF-TOKEN"), JsonPath.read(response.getContentAsString(), "$.token"));
    }

    private Fixture authenticated() {
        var account = UUID.randomUUID();
        var contextId = UUID.randomUUID();
        jdbc.update("INSERT INTO accounts (id, email) VALUES (?, ?)", account, account + "@example.invalid");
        jdbc.update("INSERT INTO session_security (id, account_id, session_version, guardian_unlocked_until, guardian_pin_version) "
                + "VALUES (?, ?, 0, clock_timestamp() + INTERVAL '20 minutes', 3)", contextId, account);
        for (String purpose : List.of("PIN_SETUP", "PIN_RESET")) {
            jdbc.update("""
                    INSERT INTO email_verifications (id, email, purpose, eligible, code_hash, expires_at, setup_account_id,
                        setup_session_binding, setup_generation, pin_version_snapshot)
                    VALUES (?, ?, ?, true, ?, clock_timestamp() + INTERVAL '5 minutes', ?, ?, 0, ?)
                    """, UUID.randomUUID(), account + "@example.invalid", purpose, new byte[32], account, contextId,
                    purpose.equals("PIN_RESET") ? 3L : null);
        }
        Session session = sessions.createSession();
        session.setMaxInactiveInterval(Duration.ofSeconds(-1));
        var security = SecurityContextHolder.createEmptyContext();
        security.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(account.toString(), null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))));
        session.setAttribute("accountId", account);
        session.setAttribute("securityContextId", contextId);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, security);
        var response = new MockHttpServletResponse();
        cookieSerializer.writeCookieValue(new CookieSerializer.CookieValue(new MockHttpServletRequest(), response, session.getId()));
        return new Fixture(contextId, session.getId(), MockCookie.parse(response.getHeader("Set-Cookie")));
    }

    private void createFailureTrigger(String table, String operation) {
        jdbc.execute("CREATE FUNCTION test_logout_failure() RETURNS trigger LANGUAGE plpgsql AS $$ "
                + "BEGIN RAISE EXCEPTION 'injected logout storage failure' USING ERRCODE = '58000'; END; $$");
        jdbc.execute("CREATE TRIGGER test_logout_failure BEFORE " + operation + " ON " + table
                + " FOR EACH ROW EXECUTE FUNCTION test_logout_failure()");
    }

    private void dropFailureTrigger(String table) {
        jdbc.execute("DROP TRIGGER test_logout_failure ON " + table);
        jdbc.execute("DROP FUNCTION test_logout_failure()");
    }

    private record Fixture(UUID contextId, String sessionId, Cookie cookie) { }
    private record Csrf(Cookie cookie, String token) { }

    @TestConfiguration(proxyBeanMethods = false)
    @RestController
    static class Probe {
        @GetMapping(PROBE)
        @ResponseStatus(HttpStatus.NO_CONTENT)
        void protectedResource() { }
    }
}
