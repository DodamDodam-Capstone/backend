package com.dodamdodam.backend;

import com.dodamdodam.backend.auth.session.AuthenticatedSession;
import com.dodamdodam.backend.auth.session.AuthenticatedSessionService;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
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
import org.springframework.security.authentication.AnonymousAuthenticationToken;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import({TestcontainersConfiguration.class, AuthenticatedSessionContractTests.Probe.class})
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"logging.level.org.springframework.session=INFO", "spring.session.jdbc.cleanup-cron=-",
                "APP_CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:5173"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AuthenticatedSessionContractTests {

    private static final String PATH = "/api/v1/contract/authenticated-session";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private JdbcIndexedSessionRepository sessionRepository;
    @Autowired private CookieSerializer cookieSerializer;

    @BeforeEach
    void clearSessions() {
        jdbcTemplate.update("DELETE FROM SPRING_SESSION");
        jdbcTemplate.update("DELETE FROM email_verifications");
        jdbcTemplate.update("DELETE FROM session_security");
        jdbcTemplate.update("DELETE FROM accounts");
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void currentSessionAcceptsNullOrFutureExpiry(boolean finite) throws Exception {
        var fixture = authenticated();
        if (finite) {
            jdbcTemplate.update("""
                    UPDATE session_security SET expires_at = clock_timestamp() + INTERVAL '30 minutes' WHERE id = ?
                    """, fixture.contextId());
        }
        mockMvc.perform(get(PATH).cookie(fixture.cookie())).andExpect(status().isNoContent());
    }

    @Test
    void missingFrameworkSessionReturns401() throws Exception {
        assertError(get(PATH), 401, "AUTHENTICATION_REQUIRED");
    }

    @Test
    void authenticatedSessionReturnsTheBoundAccountAndContextIds() throws Exception {
        var fixture = authenticated();
        mockMvc.perform(get(PATH + "/identity").cookie(fixture.cookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accountId").value(fixture.accountId().toString()))
                .andExpect(jsonPath("$.data.securityContextId").value(fixture.contextId().toString()));
    }

    @Test
    void businessContextCannotRestoreDeletedFrameworkSession() throws Exception {
        var fixture = authenticated();
        sessionRepository.deleteById(fixture.session().getId());
        assertError(get(PATH).cookie(fixture.cookie()), 401, "AUTHENTICATION_REQUIRED");
    }

    @ParameterizedTest
    @ValueSource(strings = {"MISSING_AUTH", "ANONYMOUS", "NO_ACCOUNT", "NO_CONTEXT", "BAD_ACCOUNT", "BAD_CONTEXT",
            "PRINCIPAL_MISMATCH", "OTHER_ACCOUNT_CONTEXT", "NO_DB_CONTEXT", "REVOKED", "ACCOUNT_VERSION",
            "CONTEXT_VERSION", "EXPIRED"})
    void invalidFrameworkOrBusinessContextReturns401(String invalid) throws Exception {
        var fixture = authenticated();
        switch (invalid) {
            case "MISSING_AUTH" -> fixture.session().removeAttribute(
                    HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
            case "ANONYMOUS" -> {
                var context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(new AnonymousAuthenticationToken("contract", "anonymousUser",
                        List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));
                fixture.session().setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
            }
            case "NO_ACCOUNT" -> fixture.session().removeAttribute("accountId");
            case "NO_CONTEXT" -> fixture.session().removeAttribute("securityContextId");
            case "BAD_ACCOUNT" -> fixture.session().setAttribute("accountId", "invalid");
            case "BAD_CONTEXT" -> fixture.session().setAttribute("securityContextId", "invalid");
            case "PRINCIPAL_MISMATCH" -> fixture.session().setAttribute("accountId", UUID.randomUUID());
            case "OTHER_ACCOUNT_CONTEXT" -> fixture.session().setAttribute("securityContextId", authenticated().contextId());
            case "NO_DB_CONTEXT" -> jdbcTemplate.update("DELETE FROM session_security WHERE id = ?", fixture.contextId());
            case "REVOKED" -> jdbcTemplate.update(
                    "UPDATE session_security SET revoked_at = clock_timestamp() WHERE id = ?", fixture.contextId());
            case "ACCOUNT_VERSION" -> jdbcTemplate.update(
                    "UPDATE accounts SET session_version = 1 WHERE id = ?", fixture.accountId());
            case "CONTEXT_VERSION" -> jdbcTemplate.update(
                    "UPDATE session_security SET session_version = 1 WHERE id = ?", fixture.contextId());
            case "EXPIRED" -> jdbcTemplate.update(
                    "UPDATE session_security SET expires_at = clock_timestamp() WHERE id = ?", fixture.contextId());
            default -> throw new AssertionError("Unknown fixture");
        }
        assertError(get(PATH).cookie(fixture.cookie()), 401, "AUTHENTICATION_REQUIRED");
    }

    @Test
    void businessStoreFailureReturns503() throws Exception {
        var fixture = authenticated();
        jdbcTemplate.execute("ALTER TABLE session_security RENAME TO unavailable_session_security");
        try {
            assertError(get(PATH).cookie(fixture.cookie()), 503, "AUTH_STATE_UNAVAILABLE");
        }
        finally {
            jdbcTemplate.execute("ALTER TABLE unavailable_session_security RENAME TO session_security");
        }
    }

    @Test
    void frameworkStoreFailureBeforeControllerReturns503() throws Exception {
        var fixture = authenticated();
        jdbcTemplate.execute("ALTER TABLE SPRING_SESSION RENAME TO unavailable_framework_session");
        try {
            var response = assertError(get(PATH).cookie(fixture.cookie()).header("Origin", "http://localhost:5173"),
                    503, "AUTH_STATE_UNAVAILABLE");
            assertThat(response.getHeader("Access-Control-Allow-Origin")).isEqualTo("http://localhost:5173");
            assertThat(response.getHeader("Access-Control-Allow-Credentials")).isEqualTo("true");
        }
        finally {
            jdbcTemplate.execute("ALTER TABLE unavailable_framework_session RENAME TO SPRING_SESSION");
        }
    }

    @Test
    void unreadableFrameworkAttributesReturn503() throws Exception {
        var fixture = authenticated();
        jdbcTemplate.update("""
                UPDATE SPRING_SESSION_ATTRIBUTES SET ATTRIBUTE_BYTES = ? WHERE ATTRIBUTE_NAME = ?
                """, new byte[] {1, 2, 3}, HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        assertError(get(PATH).cookie(fixture.cookie()), 503, "AUTH_STATE_UNAVAILABLE");
    }

    @Test
    void frameworkAttributeWriteFailureReturns503() throws Exception {
        var fixture = authenticated();
        jdbcTemplate.execute("""
                CREATE FUNCTION test_session_write_failure() RETURNS trigger LANGUAGE plpgsql AS $$
                BEGIN
                    IF NEW.ATTRIBUTE_NAME = 'auth-contract-write-marker' THEN
                        RAISE EXCEPTION 'injected session storage failure' USING ERRCODE = '58000';
                    END IF;
                    RETURN NEW;
                END;
                $$
                """);
        jdbcTemplate.execute("""
                CREATE TRIGGER test_session_write_failure BEFORE INSERT OR UPDATE ON SPRING_SESSION_ATTRIBUTES
                FOR EACH ROW EXECUTE FUNCTION test_session_write_failure()
                """);
        try {
            assertError(get(PATH + "/write").cookie(fixture.cookie()), 503, "AUTH_STATE_UNAVAILABLE");
        }
        finally {
            jdbcTemplate.execute("DROP TRIGGER test_session_write_failure ON SPRING_SESSION_ATTRIBUTES");
            jdbcTemplate.execute("DROP FUNCTION test_session_write_failure()");
        }
    }

    @Test
    void missingCsrfPrecedesAuthentication() throws Exception {
        assertError(post(PATH), 403, "CSRF_INVALID");
    }

    @Test
    void validCsrfWithoutAuthenticationReturns401() throws Exception {
        var csrf = mockMvc.perform(get("/api/v1/auth/csrf")).andExpect(status().isOk()).andReturn().getResponse();
        String token = JsonPath.read(csrf.getContentAsString(), "$.token");
        assertError(post(PATH).cookie(csrf.getCookie("XSRF-TOKEN")).header("X-XSRF-TOKEN", token),
                401, "AUTHENTICATION_REQUIRED");
    }

    @Test
    void missingCsrfPrecedesBusinessStoreFailure() throws Exception {
        var fixture = authenticated();
        jdbcTemplate.execute("ALTER TABLE session_security RENAME TO unavailable_session_security");
        try {
            assertError(post(PATH).cookie(fixture.cookie()), 403, "CSRF_INVALID");
        }
        finally {
            jdbcTemplate.execute("ALTER TABLE unavailable_session_security RENAME TO session_security");
        }
    }

    @Test
    void missingCsrfPrecedesFrameworkStoreFailure() throws Exception {
        var fixture = authenticated();
        jdbcTemplate.execute("ALTER TABLE SPRING_SESSION RENAME TO unavailable_framework_session");
        try {
            assertError(post(PATH).cookie(fixture.cookie()), 403, "CSRF_INVALID");
        }
        finally {
            jdbcTemplate.execute("ALTER TABLE unavailable_framework_session RENAME TO SPRING_SESSION");
        }
    }

    @Test
    void publicCsrfDoesNotRequireCurrentBusinessContext() throws Exception {
        var fixture = authenticated();
        jdbcTemplate.update("UPDATE session_security SET revoked_at = clock_timestamp() WHERE id = ?", fixture.contextId());
        mockMvc.perform(get("/api/v1/auth/csrf").cookie(fixture.cookie())).andExpect(status().isOk());
    }

    private MockHttpServletResponse assertError(MockHttpServletRequestBuilder request, int code, String errorCode) throws Exception {
        var response = mockMvc.perform(request)
                .andExpect(status().is(code))
                .andExpect(jsonPath("$.error.code").value(errorCode))
                .andExpect(jsonPath("$.error.fields").isEmpty())
                .andReturn().getResponse();
        String requestId = JsonPath.read(response.getContentAsString(), "$.error.requestId");
        assertThat(requestId).isEqualTo(response.getHeader("X-Request-ID"));
        assertThat(requestId).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
        assertThat(response.getHeader("Cache-Control")).contains("no-store");
        return response;
    }

    private Fixture authenticated() {
        var accountId = UUID.randomUUID();
        var contextId = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO accounts (id, email) VALUES (?, ?)", accountId, accountId + "@example.invalid");
        jdbcTemplate.update("""
                INSERT INTO session_security (id, account_id, session_version, created_at)
                VALUES (?, ?, 0, clock_timestamp() - INTERVAL '1 minute')
                """, contextId, accountId);
        Session session = sessionRepository.createSession();
        var securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                accountId.toString(), null, List.of(new SimpleGrantedAuthority("ROLE_USER"))));
        session.setAttribute("accountId", accountId);
        session.setAttribute("securityContextId", contextId);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);
        var response = new MockHttpServletResponse();
        cookieSerializer.writeCookieValue(new CookieSerializer.CookieValue(new MockHttpServletRequest(), response, session.getId()));
        Cookie cookie = MockCookie.parse(response.getHeader("Set-Cookie"));
        return new Fixture(accountId, contextId, session, cookie);
    }

    private record Fixture(UUID accountId, UUID contextId, Session session, Cookie cookie) { }

    @TestConfiguration(proxyBeanMethods = false)
    @RestController
    static class Probe {
        @Autowired private AuthenticatedSessionService authenticatedSessionService;

        @RequestMapping(path = PATH, method = {RequestMethod.GET, RequestMethod.POST})
        @ResponseStatus(HttpStatus.NO_CONTENT)
        void protectedResource() { }

        @GetMapping(PATH + "/identity")
        Map<String, AuthenticatedSession> identity(HttpServletRequest request) {
            return Map.of("data", authenticatedSessionService.requireAuthenticatedSession(request));
        }

        @GetMapping(PATH + "/write")
        @ResponseStatus(HttpStatus.NO_CONTENT)
        void writeSession(HttpServletRequest request) {
            request.getSession(false).setAttribute("auth-contract-write-marker", "value");
        }
    }
}
