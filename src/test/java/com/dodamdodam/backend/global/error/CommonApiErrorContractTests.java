package com.dodamdodam.backend.global.error;

import com.dodamdodam.backend.TestcontainersConfiguration;
import com.dodamdodam.backend.auth.session.AuthenticatedSessionService;
import com.dodamdodam.backend.auth.session.SessionSecurityRepository;
import com.dodamdodam.backend.child.exception.ChildException;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockCookie;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.session.Session;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;

@Import({TestcontainersConfiguration.class, CommonApiErrorContractTests.Probe.class})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"logging.level.org.springframework.session=INFO", "spring.session.jdbc.cleanup-cron=-"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CommonApiErrorContractTests {

    private static final String PATH = "/api/v1/contract/common-errors";
    private static final String UNSAFE_DETAIL = "synthetic-private-value";
    private final HttpClient client = HttpClient.newHttpClient();

    @LocalServerPort private int port;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private JdbcIndexedSessionRepository sessions;
    @Autowired private CookieSerializer cookies;

    @BeforeEach
    void cleanDatabase() {
        jdbc.update("DELETE FROM SPRING_SESSION");
        jdbc.update("DELETE FROM email_verifications");
        jdbc.update("DELETE FROM children");
        jdbc.update("DELETE FROM session_security");
        jdbc.update("DELETE FROM accounts");
    }

    @Test
    void businessDatabaseFailureReturns500() throws Exception {
        assertError(get(PATH + "/database", authenticated()), 500, "INTERNAL_ERROR");
    }

    @Test
    void unexpectedFailureDoesNotExposeCause() throws Exception {
        assertError(get(PATH + "/unexpected", authenticated()), 500, "INTERNAL_ERROR");
    }

    @ParameterizedTest
    @ValueSource(strings = {"auth-store", "wrapped-auth-store"})
    void authenticationStoreFailureReturns503(String failure) throws Exception {
        assertError(get(PATH + "/" + failure, authenticated()), 503, "AUTH_STATE_UNAVAILABLE");
    }

    @Test
    void authenticationLockFailureReturns503() throws Exception {
        assertError(get(PATH + "/auth-lock", authenticated()), 503, "AUTH_STATE_UNAVAILABLE");
    }

    @Test
    void invalidUuidReturnsSanitized400() throws Exception {
        assertError(get(PATH + "/uuid/" + UNSAFE_DETAIL, authenticated()), 400, "VALIDATION_FAILED", "id");
    }

    @Test
    void malformedJsonReturnsSanitized400() throws Exception {
        assertError(post(PATH + "/json", authenticated(), "{\"" + UNSAFE_DETAIL),
                400, "VALIDATION_FAILED", "body");
    }

    @ParameterizedTest
    @ValueSource(strings = {"unauthorized", "wrapped-authentication"})
    void controllerAuthenticationFailureRemains401(String failure) throws Exception {
        assertError(get(PATH + "/" + failure, authenticated()), 401, "AUTHENTICATION_REQUIRED");
    }

    @ParameterizedTest
    @CsvSource({"child-missing,404,RESOURCE_NOT_FOUND", "child-limit,409,CHILD_LIMIT_REACHED"})
    void childExceptionKeeps404And409(String failure, int status, String code) throws Exception {
        assertError(get(PATH + "/" + failure, authenticated()), status, code);
    }

    @Test
    void loginStoreFailureStillCompensates() throws Exception {
        var fixture = authenticated();
        assertError(post(PATH + "/login-failure", fixture, ""), 503, "AUTH_STATE_UNAVAILABLE");
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM session_security WHERE account_id = ? AND revoked_at IS NULL
                """, Integer.class, fixture.accountId())).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM SPRING_SESSION WHERE PRINCIPAL_NAME = ?",
                Integer.class, fixture.accountId().toString())).isZero();
    }

    @Test
    void loginContextCommitFailureReturns503AndKeepsPreviousLogin() throws Exception {
        var fixture = authenticated();
        jdbc.execute("""
                CREATE FUNCTION test_context_commit_failure() RETURNS trigger LANGUAGE plpgsql AS $$
                BEGIN
                    RAISE EXCEPTION 'synthetic commit failure' USING ERRCODE = '58000';
                END;
                $$
                """);
        jdbc.execute("""
                CREATE CONSTRAINT TRIGGER test_context_commit_failure AFTER INSERT ON session_security
                DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION test_context_commit_failure()
                """);
        try {
            assertError(post(PATH + "/login-failure", fixture, ""), 503, "AUTH_STATE_UNAVAILABLE");
            assertThat(jdbc.queryForObject("SELECT revoked_at IS NULL FROM session_security WHERE id = ?",
                    Boolean.class, fixture.contextId())).isTrue();
            assertThat(jdbc.queryForObject("SELECT count(*) FROM session_security WHERE account_id = ?",
                    Integer.class, fixture.accountId())).isEqualTo(1);
        }
        finally {
            jdbc.execute("DROP TRIGGER test_context_commit_failure ON session_security");
            jdbc.execute("DROP FUNCTION test_context_commit_failure()");
        }
    }

    @Test
    void unmappedRouteKeepsFramework404() throws Exception {
        assertThat(get(PATH + "/unmapped/route", authenticated()).statusCode()).isEqualTo(404);
    }

    /** 실제 JDBC 세션과 문맥을 만들어 운영 보안 필터를 거쳐 요청한다. */
    private Fixture authenticated() {
        var accountId = UUID.randomUUID();
        var contextId = UUID.randomUUID();
        jdbc.update("INSERT INTO accounts (id, email) VALUES (?, ?)", accountId, accountId + "@example.invalid");
        jdbc.update("""
                INSERT INTO session_security (id, account_id, session_version, expires_at)
                VALUES (?, ?, 0, NULL)
                """, contextId, accountId);
        // 프로젝트의 immediate 저장 설정으로 속성 변경이 바로 DB에 반영된다.
        Session session = sessions.createSession();
        session.setMaxInactiveInterval(Duration.ofSeconds(-1));
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                accountId.toString(), null, List.of(new SimpleGrantedAuthority("ROLE_USER"))));
        session.setAttribute("accountId", accountId);
        session.setAttribute("securityContextId", contextId);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        var response = new MockHttpServletResponse();
        cookies.writeCookieValue(new CookieSerializer.CookieValue(new MockHttpServletRequest(), response, session.getId()));
        Cookie cookie = MockCookie.parse(response.getHeader("Set-Cookie"));
        return new Fixture(accountId, contextId, cookie.getName() + "=" + cookie.getValue());
    }

    private HttpResponse<String> get(String path, Fixture fixture) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Cookie", fixture.cookie()).GET().build();
        return client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private HttpResponse<String> post(String path, Fixture fixture, String body) throws Exception {
        var csrf = get("/api/v1/auth/csrf", fixture);
        var csrfCookie = csrf.headers().allValues("Set-Cookie").stream().map(MockCookie::parse)
                .filter(cookie -> cookie.getName().equals("XSRF-TOKEN")).findFirst().orElseThrow();
        String token = JsonPath.read(csrf.body(), "$.token");
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Cookie", fixture.cookie() + "; XSRF-TOKEN=" + csrfCookie.getValue())
                .header("X-XSRF-TOKEN", token).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8)).build();
        return client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private void assertError(HttpResponse<String> response, int status, String code, String... fields) {
        assertThat(response.statusCode()).isEqualTo(status);
        Map<String, Object> envelope = JsonPath.read(response.body(), "$");
        assertThat(envelope).containsOnlyKeys("error");
        Map<String, Object> error = JsonPath.read(response.body(), "$.error");
        assertThat(error).containsOnlyKeys("code", "message", "requestId", "fields");
        assertThat(error.get("code")).isEqualTo(code);
        assertThat((String) error.get("message")).isNotBlank();
        var requestId = response.headers().firstValue("X-Request-ID").orElseThrow();
        assertThat(UUID.fromString(requestId).toString()).isEqualTo(requestId);
        assertThat(error.get("requestId")).isEqualTo(requestId);
        List<Map<String, Object>> errors = JsonPath.read(response.body(), "$.error.fields");
        assertThat(errors).extracting(field -> field.get("field")).containsExactlyElementsOf(List.of(fields));
        for (var field : errors) {
            assertThat(field).containsOnlyKeys("field", "reason");
            assertThat((String) field.get("reason")).isNotBlank();
        }
        assertThat(response.headers().firstValue("Content-Type").orElseThrow()).startsWith("application/json");
        assertThat(response.headers().firstValue("Cache-Control").orElseThrow()).contains("no-store");
        assertThat(response.headers().firstValue("Location")).isEmpty();
        assertThat(response.body()).doesNotContain(UNSAFE_DETAIL, "SELECT", "org.springframework", "java.lang");
    }

    private record Fixture(UUID accountId, UUID contextId, String cookie) { }

    @TestConfiguration(proxyBeanMethods = false)
    @RestController
    static class Probe {

        private final JdbcTemplate jdbc;
        private final AuthenticatedSessionService authentication;
        private final SessionSecurityRepository security;
        private final TransactionTemplate transactions;
        private final SessionAuthenticationStrategy strategy;
        private final SecurityContextRepository contexts;

        Probe(JdbcTemplate jdbc, AuthenticatedSessionService authentication, SessionSecurityRepository security,
              PlatformTransactionManager manager, SessionAuthenticationStrategy strategy,
              SecurityContextRepository contexts) {
            this.jdbc = jdbc;
            this.authentication = authentication;
            this.security = security;
            this.transactions = new TransactionTemplate(manager);
            this.strategy = strategy;
            this.contexts = contexts;
        }

        @GetMapping(PATH + "/{failure}")
        void fail(@PathVariable String failure, HttpServletRequest request) {
            switch (failure) {
                case "database" -> jdbc.queryForObject("SELECT CAST(? AS integer)", Integer.class, UNSAFE_DETAIL);
                case "unauthorized" -> throw new InsufficientAuthenticationException(UNSAFE_DETAIL);
                case "wrapped-authentication" -> throw new IllegalStateException(
                        new InsufficientAuthenticationException(UNSAFE_DETAIL));
                case "child-missing" -> throw ChildException.notFound();
                case "child-limit" -> throw ChildException.limitReached();
                case "auth-store", "wrapped-auth-store" -> {
                    jdbc.execute("ALTER TABLE session_security RENAME TO test_unavailable_security");
                    try {
                        authentication.requireAuthenticatedSession(request);
                    }
                    catch (RuntimeException exception) {
                        if (failure.equals("wrapped-auth-store")) {
                            throw new IllegalStateException(UNSAFE_DETAIL, exception);
                        }
                        throw exception;
                    }
                    finally {
                        jdbc.execute("ALTER TABLE test_unavailable_security RENAME TO session_security");
                    }
                }
                case "auth-lock" -> {
                    var current = authentication.requireAuthenticatedSession(request);
                    transactions.executeWithoutResult(transaction -> {
                        // 쿼리 실패 뒤 rollback으로 테이블 이름도 복원한다.
                        jdbc.execute("ALTER TABLE session_security RENAME TO test_unavailable_security");
                        security.requireCurrentContextForUpdate(current.accountId(), current.securityContextId());
                    });
                }
                default -> throw new IllegalStateException(UNSAFE_DETAIL);
            }
        }

        @GetMapping(PATH + "/uuid/{id}")
        void uuid(@PathVariable UUID id) {
        }

        @PostMapping(PATH + "/json")
        void json(@RequestBody Map<String, String> body) {
        }

        @PostMapping(PATH + "/login-failure")
        void loginFailure(HttpServletRequest request, HttpServletResponse response) {
            var authentication = SecurityContextHolder.getContext().getAuthentication();
            strategy.onAuthentication(authentication, request, response);
            var context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            contexts.saveContext(context, request, response);
            // 저장까지 성공한 로그인도 후속 실패 시 외곽 필터에서 보상해야 한다.
            throw new IllegalStateException(UNSAFE_DETAIL);
        }
    }
}
