package com.dodamdodam.backend;

import com.dodamdodam.backend.auth.session.AuthenticatedSessionService;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
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
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.session.Session;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import({TestcontainersConfiguration.class, SessionCookieLifecycleTests.Probe.class})
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "SESSION_COOKIE_SECURE=false", "SESSION_COOKIE_SAME_SITE=lax", "spring.session.jdbc.cleanup-cron=-",
        "APP_CORS_ALLOWED_ORIGINS=http://localhost:5173",
        "logging.level.org.springframework.security=INFO", "logging.level.org.springframework.session=INFO"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class SessionCookieLifecycleTests {

    static final String PROTECTED = "/api/v1/cookie-lifecycle/probe";
    @Autowired private MockMvc mvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private JdbcIndexedSessionRepository sessions;
    @Autowired private CookieSerializer cookies;
    @LocalServerPort private int port;

    @BeforeEach
    void clearDatabase() {
        jdbc.update("DELETE FROM SPRING_SESSION");
        jdbc.update("DELETE FROM email_verifications");
        jdbc.update("DELETE FROM session_security");
        jdbc.update("DELETE FROM accounts");
    }

    @Test
    void manualLoginIssuesExactlyOnePersistentCookie() throws Exception {
        var account = UUID.randomUUID();
        jdbc.update("INSERT INTO accounts (id, email) VALUES (?, ?)", account, account + "@example.invalid");
        var csrf = csrf(null);
        var response = mvc.perform(post("/api/v1/auth/login").param("account", account.toString())
                        .cookie(csrf.cookie()).header("X-XSRF-TOKEN", csrf.token()))
                .andExpect(status().isOk()).andReturn().getResponse();
        assertPersistentCookie(response, false, "Lax");
        var cookie = response.getCookie("JSESSIONID");
        mvc.perform(get(PROTECTED).cookie(cookie)).andExpect(status().isNoContent());
    }

    @Test
    void everyValidUseRenewsSameCookieWithoutExtendingGuardianOrPinProofs() throws Exception {
        var current = authenticated(jdbc, sessions, cookies);
        var before = jdbc.queryForMap("SELECT setup_generation, guardian_unlocked_until, guardian_pin_version, expires_at "
                + "FROM session_security WHERE id = ?", current.context());
        var proofs = jdbc.queryForList("SELECT purpose, expires_at, token_expires_at FROM email_verifications "
                + "WHERE setup_session_binding = ? ORDER BY purpose", current.context());
        for (int i = 0; i < 2; i++) {
            var response = mvc.perform(get(PROTECTED).cookie(current.cookie()))
                    .andExpect(status().isNoContent()).andReturn().getResponse();
            assertPersistentCookie(response, false, "Lax");
            assertThat(response.getCookie("JSESSIONID").getValue().equals(current.cookie().getValue()))
                    .as("renewal keeps the session ID").isTrue();
        }
        assertThat(jdbc.queryForMap("SELECT setup_generation, guardian_unlocked_until, guardian_pin_version, expires_at "
                + "FROM session_security WHERE id = ?", current.context())).isEqualTo(before);
        assertThat(jdbc.queryForList("SELECT purpose, expires_at, token_expires_at FROM email_verifications "
                + "WHERE setup_session_binding = ? ORDER BY purpose", current.context())).isEqualTo(proofs);
        Session stored = sessions.findById(current.sessionId());
        assertThat(current.context().equals(stored.getAttribute("securityContextId"))).isTrue();
        assertThat(stored.getMaxInactiveInterval()).isEqualTo(Duration.ofSeconds(-1));
    }

    @Test
    void realHttpResponseRenewsCookieBeforeCommit() throws Exception {
        var account = UUID.randomUUID();
        jdbc.update("INSERT INTO accounts (id, email) VALUES (?, ?)", account, account + "@example.invalid");
        var baseUrl = "http://localhost:" + port;
        try (var client = HttpClient.newHttpClient()) {
            var csrf = client.send(HttpRequest.newBuilder(URI.create(baseUrl + "/api/v1/auth/csrf")).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            assertThat(csrf.statusCode()).isEqualTo(200);
            var csrfCookie = csrf.headers().allValues("Set-Cookie").stream().filter(h -> h.startsWith("XSRF-TOKEN="))
                    .map(MockCookie::parse).findFirst().orElseThrow();
            var login = client.send(HttpRequest.newBuilder(URI.create(baseUrl + "/api/v1/auth/login?account=" + account))
                            .header("Cookie", "XSRF-TOKEN=" + csrfCookie.getValue())
                            .header("X-XSRF-TOKEN", JsonPath.read(csrf.body(), "$.token"))
                            .POST(HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.discarding());
            assertThat(login.statusCode()).isEqualTo(200);
            var issued = login.headers().allValues("Set-Cookie").stream().filter(h -> h.startsWith("JSESSIONID=")).toList();
            assertThat(issued.size()).as("one login session cookie").isEqualTo(1);
            var original = MockCookie.parse(issued.getFirst());
            assertThat(original.getMaxAge()).isEqualTo(31536000);
            var request = HttpRequest.newBuilder(URI.create(baseUrl + PROTECTED))
                    .header("Cookie", "JSESSIONID=" + original.getValue()).GET().build();
            var response = client.send(request, HttpResponse.BodyHandlers.discarding());
            assertThat(response.statusCode()).isEqualTo(204);
            var headers = response.headers().allValues("Set-Cookie").stream()
                    .filter(h -> h.startsWith("JSESSIONID=")).toList();
            assertThat(headers.size()).as("one persistent session cookie").isEqualTo(1);
            var cookie = MockCookie.parse(headers.getFirst());
            assertThat(cookie.getMaxAge()).isEqualTo(31536000);
            assertThat(cookie.getValue().equals(original.getValue())).isTrue();
        }
    }

    @ParameterizedTest
    @CsvSource({"REVOKED,false", "REVOKED,true", "DB_FAILURE,false", "DB_FAILURE,true"})
    void realHttpResponseDiscardsSuccessBodyWhenRecheckFails(String mode, boolean stream) throws Exception {
        var current = authenticated(jdbc, sessions, cookies);
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + PROTECTED
                        + "/change?mode=" + mode + "&stream=" + stream))
                .header("Cookie", "JSESSIONID=" + current.cookie().getValue())
                .header("Origin", "http://localhost:5173").GET().build();
        try (var client = HttpClient.newHttpClient()) {
            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).isEqualTo(mode.equals("REVOKED") ? 401 : 503);
            assertThat((String) JsonPath.read(response.body(), "$.error.code"))
                    .isEqualTo(mode.equals("REVOKED") ? "AUTHENTICATION_REQUIRED" : "AUTH_STATE_UNAVAILABLE");
            assertThat(response.body()).doesNotContain("protected-result");
            assertThat(response.headers().firstValue("Cache-Control").orElse("")).contains("no-store");
            assertThat(response.headers().firstValue("X-Request-ID").isPresent()).isTrue();
            assertThat(response.headers().firstValue("Access-Control-Allow-Origin").orElse(""))
                    .isEqualTo("http://localhost:5173");
            assertThat(response.headers().firstValue("Access-Control-Allow-Credentials").orElse(""))
                    .isEqualTo("true");
            assertThat(response.headers().allValues("Set-Cookie").stream().filter(h -> h.startsWith("JSESSIONID="))
                    .map(MockCookie::parse).anyMatch(c -> c.getMaxAge() > 0)).isFalse();
        }
        finally {
            if (mode.equals("DB_FAILURE")) jdbc.execute("ALTER TABLE unavailable_cookie_context RENAME TO session_security");
        }
    }

    @Test
    void oauthTemporarySessionKeepsFiniteLifetime() throws Exception {
        var response = mvc.perform(get("/oauth2/authorization/google"))
                .andExpect(status().isFound()).andReturn().getResponse();
        var cookie = response.getCookie("JSESSIONID");
        assertThat(cookie != null).isTrue();
        assertThat(cookie.getMaxAge()).isEqualTo(-1);
        var request = new MockHttpServletRequest();
        request.setCookies(cookie);
        Session stored = sessions.findById(cookies.readCookieValues(request).getFirst());
        assertThat(stored.getMaxInactiveInterval()).isEqualTo(Duration.ofMinutes(30));
    }

    @Test
    void publicCsrfDoesNotPromoteOrValidateAuthentication() throws Exception {
        var current = authenticated(jdbc, sessions, cookies);
        jdbc.update("UPDATE session_security SET revoked_at = clock_timestamp() WHERE id = ?", current.context());
        var response = mvc.perform(get("/api/v1/auth/csrf").cookie(current.cookie()))
                .andExpect(status().isOk()).andReturn().getResponse();
        assertNoPersistentCookie(response);
    }

    @ParameterizedTest
    @ValueSource(strings = {"REVOKED", "VERSION"})
    void invalidSessionDoesNotReceiveRenewal(String mode) throws Exception {
        var current = authenticated(jdbc, sessions, cookies);
        if (mode.equals("REVOKED")) jdbc.update("UPDATE session_security SET revoked_at = clock_timestamp() WHERE id = ?", current.context());
        else jdbc.update("UPDATE accounts SET session_version = 1 WHERE id = ?", current.account());
        var response = mvc.perform(get(PROTECTED).cookie(current.cookie()))
                .andExpect(status().isUnauthorized()).andReturn().getResponse();
        assertNoPersistentCookie(response);
    }

    @Test
    void failedRequestDoesNotRenewValidSession() throws Exception {
        var current = authenticated(jdbc, sessions, cookies);
        var response = mvc.perform(get(PROTECTED + "/bad").cookie(current.cookie()))
                .andExpect(status().isBadRequest()).andReturn().getResponse();
        assertNoPersistentCookie(response);
        response = mvc.perform(post("/api/v1/auth/logout").cookie(current.cookie()))
                .andExpect(status().isForbidden()).andReturn().getResponse();
        assertNoPersistentCookie(response);
    }

    @Test
    void logoutExpiryWinsAndPreviousCookieCannotRenew() throws Exception {
        var current = authenticated(jdbc, sessions, cookies);
        var csrf = csrf(current.cookie());
        var response = mvc.perform(post("/api/v1/auth/logout").cookie(current.cookie(), csrf.cookie())
                        .header("X-XSRF-TOKEN", csrf.token()))
                .andExpect(status().isNoContent()).andReturn().getResponse();
        var headers = response.getHeaders("Set-Cookie").stream().filter(h -> h.startsWith("JSESSIONID=")).toList();
        assertThat(headers.size()).isEqualTo(1);
        assertThat(MockCookie.parse(headers.getFirst()).getMaxAge()).isZero();
        assertNoPersistentCookie(response);
        response = mvc.perform(get(PROTECTED).cookie(current.cookie())).andExpect(status().isUnauthorized()).andReturn().getResponse();
        assertNoPersistentCookie(response);
    }

    @ParameterizedTest
    @ValueSource(strings = {"REVOKED", "VERSION", "NATIVE_MISSING", "PRINCIPAL", "CONTEXT"})
    void responseRecheckRejectsStateChangedAfterAuthorization(String mode) throws Exception {
        var current = authenticated(jdbc, sessions, cookies);
        var response = mvc.perform(get(PROTECTED + "/change").param("mode", mode).cookie(current.cookie()))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.error.code").value("AUTHENTICATION_REQUIRED"))
                .andExpect(jsonPath("$.error.fields").isEmpty()).andReturn().getResponse();
        assertNoPersistentCookie(response);
        assertThat(response.getContentAsString()).doesNotContain("protected-result");
    }

    @Test
    void renewalStoreFailureReturns503BeforeProtectedResponse() throws Exception {
        var current = authenticated(jdbc, sessions, cookies);
        try {
            var response = mvc.perform(get(PROTECTED + "/change").param("mode", "DB_FAILURE").cookie(current.cookie()))
                    .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.error.code").value("AUTH_STATE_UNAVAILABLE"))
                    .andReturn().getResponse();
            assertNoPersistentCookie(response);
            assertThat(response.getContentAsString()).doesNotContain("protected-result");
        }
        finally {
            jdbc.execute("ALTER TABLE unavailable_cookie_context RENAME TO session_security");
        }
    }

    static void assertPersistentCookie(MockHttpServletResponse response, boolean secure, String sameSite) {
        var headers = response.getHeaders("Set-Cookie").stream().filter(h -> h.startsWith("JSESSIONID=")).toList();
        assertThat(headers.size()).as("one persistent session cookie").isEqualTo(1);
        var cookie = MockCookie.parse(headers.getFirst());
        assertThat(cookie.getMaxAge()).isEqualTo(31536000);
        assertThat(cookie.getPath()).isEqualTo("/");
        assertThat(cookie.getDomain()).isNull();
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getSecure()).isEqualTo(secure);
        assertThat(cookie.getAttribute("SameSite")).isEqualTo(sameSite);
        var expiry = ZonedDateTime.parse(cookie.getAttribute("Expires"), DateTimeFormatter.RFC_1123_DATE_TIME).toInstant();
        assertThat(expiry).isBetween(Instant.now().plusSeconds(31535995), Instant.now().plusSeconds(31536005));
        assertThat(response.getHeader("Cache-Control")).contains("no-store");
    }

    private void assertNoPersistentCookie(MockHttpServletResponse response) {
        assertThat(response.getHeaders("Set-Cookie").stream().filter(h -> h.startsWith("JSESSIONID="))
                .map(MockCookie::parse).anyMatch(c -> c.getMaxAge() > 0)).isFalse();
    }

    private Csrf csrf(Cookie cookie) throws Exception {
        var call = get("/api/v1/auth/csrf");
        if (cookie != null) call.cookie(cookie);
        var response = mvc.perform(call).andExpect(status().isOk()).andReturn().getResponse();
        return new Csrf(response.getCookie("XSRF-TOKEN"), JsonPath.read(response.getContentAsString(), "$.token"));
    }

    static Fixture authenticated(JdbcTemplate jdbc, JdbcIndexedSessionRepository sessions, CookieSerializer cookies) {
        var account = UUID.randomUUID();
        var context = UUID.randomUUID();
        jdbc.update("INSERT INTO accounts (id, email) VALUES (?, ?)", account, account + "@example.invalid");
        jdbc.update("INSERT INTO session_security (id, account_id, session_version, setup_generation, guardian_unlocked_until, guardian_pin_version) "
                + "VALUES (?, ?, 0, 7, clock_timestamp() + INTERVAL '30 minutes', 3)", context, account);
        for (String purpose : List.of("PIN_SETUP", "PIN_RESET")) {
            jdbc.update("""
                    INSERT INTO email_verifications (id, email, purpose, eligible, code_hash, expires_at,
                        setup_account_id, setup_session_binding, setup_generation, pin_version_snapshot,
                        verified_at, token_hash, token_expires_at)
                    VALUES (?, ?, ?, true, ?, clock_timestamp() + INTERVAL '5 minutes', ?, ?, 7, ?,
                        clock_timestamp(), ?, clock_timestamp() + INTERVAL '10 minutes')
                    """, UUID.randomUUID(), account + "@example.invalid", purpose, new byte[32], account, context,
                    purpose.equals("PIN_RESET") ? 3L : null, new byte[32]);
        }
        Session session = sessions.createSession();
        session.setMaxInactiveInterval(Duration.ofSeconds(-1));
        var security = SecurityContextHolder.createEmptyContext();
        security.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(account.toString(), null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))));
        session.setAttribute("accountId", account);
        session.setAttribute("securityContextId", context);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, security);
        var response = new MockHttpServletResponse();
        cookies.writeCookieValue(new CookieSerializer.CookieValue(new MockHttpServletRequest(), response, session.getId()));
        return new Fixture(account, context, session.getId(), MockCookie.parse(response.getHeader("Set-Cookie")));
    }

    record Fixture(UUID account, UUID context, String sessionId, Cookie cookie) { }
    private record Csrf(Cookie cookie, String token) { }

    @TestConfiguration(proxyBeanMethods = false)
    @RestController
    static class Probe {
        private final SessionAuthenticationStrategy strategy;
        private final SecurityContextRepository contexts;
        private final JdbcTemplate jdbc;
        private final JdbcIndexedSessionRepository sessions;

        Probe(SessionAuthenticationStrategy strategy, SecurityContextRepository contexts, JdbcTemplate jdbc,
              JdbcIndexedSessionRepository sessions) {
            this.strategy = strategy;
            this.contexts = contexts;
            this.jdbc = jdbc;
            this.sessions = sessions;
        }

        @PostMapping("/api/v1/auth/login")
        void login(@RequestParam("account") UUID account, HttpServletRequest request, HttpServletResponse response) throws IOException {
            var authentication = UsernamePasswordAuthenticationToken.authenticated(account.toString(), null,
                    List.of(new SimpleGrantedAuthority("ROLE_USER")));
            strategy.onAuthentication(authentication, request, response);
            var context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            contexts.saveContext(context, request, response);
            response.flushBuffer();
        }

        @GetMapping(PROTECTED)
        @ResponseStatus(HttpStatus.NO_CONTENT)
        void resource() { }

        @GetMapping(PROTECTED + "/bad")
        @ResponseStatus(HttpStatus.BAD_REQUEST)
        void rejected() { }

        @GetMapping(PROTECTED + "/change")
        void change(@RequestParam("mode") String mode, @RequestParam(defaultValue = "false") boolean stream,
                    HttpServletRequest request, HttpServletResponse response) throws IOException {
            var session = request.getSession(false);
            UUID account = (UUID) session.getAttribute(AuthenticatedSessionService.ACCOUNT_ID);
            UUID context = (UUID) session.getAttribute(AuthenticatedSessionService.SECURITY_CONTEXT_ID);
            switch (mode) {
                case "REVOKED" -> jdbc.update("UPDATE session_security SET revoked_at = clock_timestamp() WHERE id = ?", context);
                case "VERSION" -> jdbc.update("UPDATE accounts SET session_version = 1 WHERE id = ?", account);
                case "NATIVE_MISSING" -> sessions.deleteById(session.getId());
                case "PRINCIPAL" -> {
                    var security = SecurityContextHolder.createEmptyContext();
                    security.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(UUID.randomUUID().toString(), null,
                            List.of(new SimpleGrantedAuthority("ROLE_USER"))));
                    session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, security);
                }
                case "CONTEXT" -> session.setAttribute("securityContextId", UUID.randomUUID());
                case "DB_FAILURE" -> jdbc.execute("ALTER TABLE session_security RENAME TO unavailable_cookie_context");
                default -> throw new IllegalArgumentException("Unknown fixture");
            }
            response.setContentType("text/plain");
            if (stream) response.getOutputStream().write("protected-result".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            else response.getWriter().write("protected-result");
            response.flushBuffer();
        }
    }
}
