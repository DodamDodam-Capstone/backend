package com.dodamdodam.backend.guardian.application;

import com.dodamdodam.backend.TestcontainersConfiguration;
import com.dodamdodam.backend.auth.session.AuthenticatedSessionService;
import com.dodamdodam.backend.child.application.ChildService;
import com.dodamdodam.backend.child.exception.ChildException;
import com.jayway.jsonpath.JsonPath;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
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
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import static org.assertj.core.api.Assertions.assertThat;

@Import({TestcontainersConfiguration.class, GuardianAccessConcurrencyTests.Probe.class,
        GuardianAccessConcurrencyTests.ResponseGate.class})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.session.jdbc.cleanup-cron=-", "SESSION_COOKIE_SECURE=false", "SESSION_COOKIE_SAME_SITE=lax",
        "APP_CORS_ALLOWED_ORIGINS=http://localhost:5173", "logging.level.org.springframework.security=INFO",
        "logging.level.org.springframework.session=INFO"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Timeout(25)
class GuardianAccessConcurrencyTests {

    private static final String PATH = "/api/v1/contract/guardian-race";
    private static final String ORIGIN = "http://localhost:5173";
    private static final String SENSITIVE = "guardian-sensitive-result";
    private static final int BODY_LENGTH = ("{\"data\":\"" + SENSITIVE + "\"}").getBytes(StandardCharsets.UTF_8).length;

    @Autowired private JdbcTemplate jdbc;
    @Autowired private JdbcIndexedSessionRepository sessions;
    @Autowired private CookieSerializer cookies;
    @Autowired private ResponseGate gate;
    @LocalServerPort private int port;

    @BeforeEach
    @AfterEach
    void clearDatabase() {
        gate.reset();
        jdbc.update("DELETE FROM SPRING_SESSION");
        jdbc.update("DELETE FROM guardian_pins");
        jdbc.update("DELETE FROM email_verifications");
        jdbc.update("DELETE FROM children");
        jdbc.update("DELETE FROM session_security");
        jdbc.update("DELETE FROM accounts");
    }

    @Test
    void unchangedGrantReturnsSensitiveBodyAndRenewsSameSessionCookie() throws Exception {
        var current = fixture();
        try (var client = HttpClient.newHttpClient()) {
            var response = client.send(request(PATH + "/body/" + current.childId(), current).GET().build(),
                    HttpResponse.BodyHandlers.ofString());

            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(JsonPath.<String>read(response.body(), "$.data")).isEqualTo(SENSITIVE);
            assertThat(response.headers().firstValue("Location")).contains(PATH + "/sensitive");
            var renewed = response.headers().allValues("Set-Cookie").stream()
                    .filter(value -> value.startsWith("JSESSIONID=")).map(MockCookie::parse).findFirst().orElseThrow();
            assertThat(renewed.getValue()).isEqualTo(current.cookie().substring("JSESSIONID=".length()));
            assertThat(renewed.getMaxAge()).isEqualTo(31536000);
        }
    }

    @ParameterizedTest
    @CsvSource({"CLEAR,403,GUARDIAN_UNLOCK_REQUIRED", "EXPIRED,403,GUARDIAN_UNLOCK_REQUIRED",
            "PIN_VERSION,403,GUARDIAN_UNLOCK_REQUIRED", "REVOKED,401,AUTHENTICATION_REQUIRED",
            "LOGOUT,401,AUTHENTICATION_REQUIRED", "GUARDIAN_STORE,503,AUTH_STATE_UNAVAILABLE",
            "AUTH_STORE,503,AUTH_STATE_UNAVAILABLE"})
    void changeBeforeBodyBlocksSensitiveResponse(String change, int status, String code) throws Exception {
        var current = fixture();
        var pause = gate.pauseNext();
        var table = switch (change) {
            case "GUARDIAN_STORE" -> "guardian_pins";
            case "AUTH_STORE" -> "session_security";
            default -> null;
        };
        boolean unavailable = false;
        try (var client = HttpClient.newHttpClient()) {
            var pending = client.sendAsync(request(PATH + "/body/" + current.childId(), current).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            try {
                assertThat(pause.ready.await(10, TimeUnit.SECONDS)).as("최초 권한 검사 뒤 직렬화 직전").isTrue();
                if (table != null) {
                    jdbc.execute("ALTER TABLE " + table + " RENAME TO guardian_race_unavailable");
                    unavailable = true;
                }
                else {
                    changePermission(change, current, client);
                }
            }
            finally {
                pause.resume.countDown();
            }

            var response = pending.get(10, TimeUnit.SECONDS);
            assertError(response, status, code);
            assertThat(pause.saved.getCount()).as("폐기 이후 늦은 native 속성 저장 완료").isZero();
            if (status == 403) {
                // guardian 거부와 일반 로그인은 별개이며 늦은 속성이 DB 권한을 복구하지 못한다.
                assertThat(client.send(request(PATH + "/authenticated", current).GET().build(),
                        HttpResponse.BodyHandlers.discarding()).statusCode()).isEqualTo(204);
                assertError(client.send(request(PATH + "/body/" + current.childId(), current).GET().build(),
                        HttpResponse.BodyHandlers.ofString()), 403, "GUARDIAN_UNLOCK_REQUIRED");
            }
            if (status == 401) {
                assertThat(client.send(request(PATH + "/authenticated", current).GET().build(),
                        HttpResponse.BodyHandlers.discarding()).statusCode()).isEqualTo(401);
                if (change.equals("LOGOUT")) {
                    assertThat((Session) sessions.findById(current.sessionId())).isNull();
                }
            }
        }
        finally {
            pause.resume.countDown();
            if (unavailable) {
                jdbc.execute("ALTER TABLE guardian_race_unavailable RENAME TO " + table);
            }
        }
    }

    @Test
    void uncommittedEmptyResponseIsAlsoRechecked() throws Exception {
        var current = fixture();
        var pause = gate.pauseNext();
        try (var client = HttpClient.newHttpClient()) {
            var pending = client.sendAsync(request(PATH + "/empty/" + current.childId(), current).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            try {
                assertThat(pause.ready.await(10, TimeUnit.SECONDS)).isTrue();
                changePermission("CLEAR", current, client);
            }
            finally {
                pause.resume.countDown();
            }
            assertError(pending.get(10, TimeUnit.SECONDS), 403, "GUARDIAN_UNLOCK_REQUIRED");
        }
    }

    @Test
    void businessErrorDoesNotTriggerGuardianRecheckDuringOutage() throws Exception {
        var current = fixture();
        var pause = gate.pauseNext();
        boolean unavailable = false;
        try (var client = HttpClient.newHttpClient()) {
            var pending = client.sendAsync(request(PATH + "/business-error/" + current.childId(), current).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            try {
                assertThat(pause.ready.await(10, TimeUnit.SECONDS)).isTrue();
                jdbc.execute("ALTER TABLE guardian_pins RENAME TO guardian_race_unavailable");
                unavailable = true;
            }
            finally {
                pause.resume.countDown();
            }
            assertError(pending.get(10, TimeUnit.SECONDS), 404, "RESOURCE_NOT_FOUND");
        }
        finally {
            pause.resume.countDown();
            if (unavailable) {
                jdbc.execute("ALTER TABLE guardian_race_unavailable RENAME TO guardian_pins");
            }
        }
    }

    private void changePermission(String change, Fixture current, HttpClient client) throws Exception {
        switch (change) {
            case "CLEAR" -> jdbc.update("UPDATE session_security SET guardian_unlocked_until = NULL, "
                    + "guardian_pin_version = NULL WHERE id = ?", current.contextId());
            case "EXPIRED" -> jdbc.update("UPDATE session_security SET guardian_unlocked_until = "
                    + "clock_timestamp() - interval '1 second' WHERE id = ?", current.contextId());
            case "PIN_VERSION" -> jdbc.update("UPDATE guardian_pins SET pin_version = pin_version + 1 WHERE account_id = ?",
                    current.accountId());
            case "REVOKED" -> jdbc.update("UPDATE session_security SET revoked_at = clock_timestamp(), "
                    + "guardian_unlocked_until = NULL, guardian_pin_version = NULL WHERE id = ?", current.contextId());
            case "LOGOUT" -> {
                var csrf = client.send(request("/api/v1/auth/csrf", current).GET().build(), HttpResponse.BodyHandlers.ofString());
                assertThat(csrf.statusCode()).isEqualTo(200);
                var cookie = csrf.headers().allValues("Set-Cookie").stream()
                        .filter(value -> value.startsWith("XSRF-TOKEN=")).map(MockCookie::parse).findFirst().orElseThrow();
                var logout = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/auth/logout"))
                        .timeout(Duration.ofSeconds(10)).header("Cookie", current.cookie() + "; XSRF-TOKEN=" + cookie.getValue())
                        .header("X-XSRF-TOKEN", JsonPath.read(csrf.body(), "$.token"))
                        .POST(HttpRequest.BodyPublishers.noBody()).build();
                assertThat(client.send(logout, HttpResponse.BodyHandlers.discarding()).statusCode()).isEqualTo(204);
            }
            default -> throw new IllegalArgumentException(change);
        }
    }

    private HttpRequest.Builder request(String path, Fixture current) {
        return HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).timeout(Duration.ofSeconds(10))
                .header("Cookie", current.cookie()).header("Origin", ORIGIN).header("X-Request-ID", "client-controlled");
    }

    private void assertError(HttpResponse<String> response, int status, String code) {
        assertThat(response.statusCode()).isEqualTo(status);
        Map<String, Object> envelope = JsonPath.read(response.body(), "$");
        Map<String, Object> error = JsonPath.read(response.body(), "$.error");
        assertThat(envelope).containsOnlyKeys("error");
        assertThat(error).containsOnlyKeys("code", "message", "requestId", "fields");
        assertThat(error.get("code")).isEqualTo(code);
        assertThat((List<?>) error.get("fields")).isEmpty();
        assertThat(error.get("requestId")).isEqualTo(response.headers().firstValue("X-Request-ID").orElseThrow());
        assertThat((String) error.get("requestId"))
                .matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
        assertThat(response.body()).doesNotContain(SENSITIVE, "SELECT ", "pin_hash", "org.springframework", "java.lang");
        assertThat(response.headers().firstValue("Location")).isEmpty();
        assertThat(response.headers().firstValue("Content-Type").orElseThrow()).startsWith("application/json");
        assertThat(response.headers().firstValue("Cache-Control").orElseThrow()).contains("no-store");
        assertThat(response.headers().firstValue("Access-Control-Allow-Origin")).contains(ORIGIN);
        assertThat(response.headers().firstValue("Access-Control-Allow-Credentials")).contains("true");
        response.headers().firstValueAsLong("Content-Length").ifPresent(length ->
                assertThat(length).isEqualTo(response.body().getBytes(StandardCharsets.UTF_8).length));
        assertThat(response.headers().allValues("Set-Cookie").stream()
                .filter(value -> value.startsWith("JSESSIONID=")).map(MockCookie::parse)
                .anyMatch(cookie -> cookie.getMaxAge() > 0)).isFalse();
    }

    private Fixture fixture() {
        var accountId = UUID.randomUUID();
        var contextId = UUID.randomUUID();
        var childId = UUID.randomUUID();
        jdbc.update("INSERT INTO accounts (id, email) VALUES (?, ?)", accountId, accountId + "@example.invalid");
        jdbc.update("INSERT INTO session_security (id, account_id, session_version, guardian_unlocked_until, "
                + "guardian_pin_version) VALUES (?, ?, 0, clock_timestamp() + interval '15 minutes', 1)", contextId, accountId);
        jdbc.update("INSERT INTO guardian_pins (account_id, pin_hash) VALUES (?, 'synthetic-hash')", accountId);
        jdbc.update("""
                INSERT INTO children (id, account_id, name, nickname, birth_date, gender, character_id)
                VALUES (?, ?, '가나', '별이', '2020-05-12', 'FEMALE', 'test-character')
                """, childId, accountId);
        Session nativeSession = sessions.createSession();
        nativeSession.setMaxInactiveInterval(Duration.ofSeconds(-1));
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(accountId.toString(), null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))));
        nativeSession.setAttribute("accountId", accountId);
        nativeSession.setAttribute("securityContextId", contextId);
        nativeSession.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        var response = new MockHttpServletResponse();
        cookies.writeCookieValue(new CookieSerializer.CookieValue(new MockHttpServletRequest(), response, nativeSession.getId()));
        var cookie = MockCookie.parse(response.getHeader("Set-Cookie"));
        return new Fixture(accountId, contextId, childId, nativeSession.getId(), "JSESSIONID=" + cookie.getValue());
    }

    private record Fixture(UUID accountId, UUID contextId, UUID childId, String sessionId, String cookie) { }

    static class Pause {
        final CountDownLatch ready = new CountDownLatch(1);
        final CountDownLatch resume = new CountDownLatch(1);
        final CountDownLatch saved = new CountDownLatch(1);
    }

    @TestConfiguration(proxyBeanMethods = false)
    @ControllerAdvice(assignableTypes = Probe.class)
    static class ResponseGate implements ResponseBodyAdvice<Object> {
        private final AtomicReference<Pause> next = new AtomicReference<>();

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

        void awaitChange(HttpServletRequest request) {
            var pause = next.getAndSet(null);
            if (pause == null) {
                return;
            }
            var nativeSession = request.getSession(false);
            pause.ready.countDown();
            try {
                if (!pause.resume.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("권한 변경 대기가 끝나지 않았습니다.");
                }
            }
            catch (InterruptedException failure) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(failure);
            }
            // 최초 요청이 쥔 native 세션을 늦게 저장해도 DB guardian은 복원되지 않아야 한다.
            nativeSession.setAttribute("guardian_unlocked_until", "2099-01-01T00:00:00Z");
            nativeSession.setAttribute("guardian_pin_version", 1L);
            pause.saved.countDown();
        }

        @Override
        public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
            return true;
        }

        @Override
        public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType contentType,
                Class<? extends HttpMessageConverter<?>> converterType, ServerHttpRequest request, ServerHttpResponse response) {
            if (body instanceof Map<?, ?> data && data.containsKey("data")) {
                awaitChange(((ServletServerHttpRequest) request).getServletRequest());
            }
            return body;
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    @RestController
    static class Probe {
        private final AuthenticatedSessionService authentication;
        private final ChildService children;
        private final GuardianAccessService guardian;
        private final ResponseGate gate;

        Probe(AuthenticatedSessionService authentication, ChildService children,
              GuardianAccessService guardian, ResponseGate gate) {
            this.authentication = authentication;
            this.children = children;
            this.guardian = guardian;
            this.gate = gate;
        }

        private void requireAccess(HttpServletRequest request, UUID childId) {
            var authenticated = authentication.requireAuthenticatedSession(request);
            children.requireChildAccess(authenticated.accountId(), childId);
            guardian.requireGuardianAccess(request, authenticated);
        }

        @GetMapping(PATH + "/body/{childId}")
        Map<String, String> body(HttpServletRequest request, HttpServletResponse response, @PathVariable UUID childId) {
            requireAccess(request, childId);
            response.setHeader("Location", PATH + "/sensitive");
            response.setContentLength(BODY_LENGTH);
            return Map.of("data", SENSITIVE);
        }

        @GetMapping(PATH + "/empty/{childId}")
        @ResponseStatus(HttpStatus.NO_CONTENT)
        void empty(HttpServletRequest request, HttpServletResponse response, @PathVariable UUID childId) {
            requireAccess(request, childId);
            response.setHeader("Location", PATH + "/sensitive");
            gate.awaitChange(request);
        }

        @GetMapping(PATH + "/business-error/{childId}")
        void businessError(HttpServletRequest request, @PathVariable UUID childId) {
            requireAccess(request, childId);
            gate.awaitChange(request);
            throw ChildException.notFound();
        }

        @GetMapping(PATH + "/authenticated")
        @ResponseStatus(HttpStatus.NO_CONTENT)
        void authenticated(HttpServletRequest request) {
            authentication.requireAuthenticatedSession(request);
        }
    }
}
