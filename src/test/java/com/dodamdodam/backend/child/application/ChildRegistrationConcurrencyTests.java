package com.dodamdodam.backend.child.application;

import com.dodamdodam.backend.TestcontainersConfiguration;
import com.dodamdodam.backend.auth.session.AuthenticatedSession;
import com.dodamdodam.backend.auth.session.AuthStateUnavailableException;
import com.dodamdodam.backend.auth.session.SessionSecurityRepository;
import com.dodamdodam.backend.child.api.ChildController;
import com.dodamdodam.backend.child.dto.ChildView;
import com.dodamdodam.backend.child.dto.CreateChildRequest;
import com.dodamdodam.backend.child.exception.ChildException;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DataAccessException;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockCookie;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.session.Session;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Import({TestcontainersConfiguration.class, ChildCatalogueTestConfiguration.class,
        ChildRegistrationConcurrencyTests.ResponseGate.class})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.session.jdbc.cleanup-cron=-", "SESSION_COOKIE_SECURE=false", "SESSION_COOKIE_SAME_SITE=lax",
        "logging.level.org.springframework.security=INFO", "logging.level.org.springframework.session=INFO"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Timeout(20)
class ChildRegistrationConcurrencyTests {

    private static final String PATH = "/api/v1/children";
    private static final String HTTP_BODY = """
            {"name":"가나","nickname":"별이","birthDate":"2020-05-12","gender":"FEMALE",
             "interests":["공룡"],"characterId":"test-character"}
            """;

    @Autowired private JdbcTemplate jdbc;
    @Autowired private ChildService service;
    @Autowired @Qualifier("childService") private ChildService withoutCatalogue;
    @Autowired private SessionSecurityRepository sessions;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private JdbcIndexedSessionRepository nativeSessions;
    @Autowired private CookieSerializer cookies;
    @Autowired private ResponseGate responseGate;
    @LocalServerPort private int port;

    @BeforeEach
    void clear() {
        responseGate.reset();
        jdbc.update("DELETE FROM children");
        jdbc.update("DELETE FROM SPRING_SESSION");
        jdbc.update("DELETE FROM email_verifications");
        jdbc.update("DELETE FROM session_security");
        jdbc.update("DELETE FROM accounts");
    }

    @Test
    void registersNormalizedProfileWithSessionOwnerAndInjectedClock() {
        var session = session();
        var request = new CreateChildRequest("\u00a0가나 ", " 별 이 ", LocalDate.of(2026, 11, 1),
                ChildView.Gender.FEMALE, List.of(" 공 룡 ", "그림"), "test-character");

        var child = service.create(session, request);

        assertThat(child.name()).isEqualTo("가나");
        assertThat(child.nickname()).isEqualTo("별 이");
        assertThat(child.birthDate()).isEqualTo(LocalDate.of(2026, 11, 1));
        assertThat(child.interests()).containsExactly("공 룡", "그림");
        assertThat(jdbc.queryForObject("SELECT account_id FROM children WHERE id = ?", UUID.class, child.childId()))
                .isEqualTo(session.accountId());
        assertChildCount(1);
    }

    @Test
    void rejectsSecondChildForSameAccount() {
        var session = session();
        service.create(session, profile());

        var failure = assertThrows(ChildException.class, () -> service.create(session, profile()));

        assertThat(failure.code()).isEqualTo("CHILD_LIMIT_REACHED");
        assertThat(failure.fields()).isEmpty();
        assertChildCount(1);
    }

    @Test
    void rejectsRevokedContextBeforeInsert() {
        var session = session();
        jdbc.update("UPDATE session_security SET revoked_at = clock_timestamp() WHERE id = ?", session.securityContextId());

        assertThatThrownBy(() -> service.create(session, profile())).isInstanceOf(InsufficientAuthenticationException.class);
        assertChildCount(0);
    }

    @ParameterizedTest
    @ValueSource(strings = {"MISSING_CONTEXT", "MISSING_ACCOUNT", "WRONG_OWNER", "VERSION", "EXPIRED"})
    void rejectsInvalidContextsBeforeInsert(String change) {
        var original = session();
        var session = switch (change) {
            case "MISSING_ACCOUNT" -> new AuthenticatedSession(UUID.randomUUID(), original.securityContextId());
            case "WRONG_OWNER" -> new AuthenticatedSession(account(), original.securityContextId());
            default -> original;
        };
        if (change.equals("MISSING_CONTEXT")) jdbc.update("DELETE FROM session_security WHERE id = ?", session.securityContextId());
        if (change.equals("VERSION")) jdbc.update("UPDATE accounts SET session_version = 1 WHERE id = ?", session.accountId());
        if (change.equals("EXPIRED")) jdbc.update("""
                UPDATE session_security SET created_at = clock_timestamp() - INTERVAL '2 days',
                    expires_at = clock_timestamp() - INTERVAL '1 day' WHERE id = ?
                """, session.securityContextId());

        assertThatThrownBy(() -> service.create(session, profile())).isInstanceOf(InsufficientAuthenticationException.class);
        assertChildCount(0);
    }

    @Test
    void validatesInputBeforeCheckingContext() {
        var invalid = new CreateChildRequest("", "별이", LocalDate.of(2020, 5, 12),
                ChildView.Gender.FEMALE, List.of(), "test-character");
        var missing = new AuthenticatedSession(UUID.randomUUID(), UUID.randomUUID());

        var failure = assertThrows(ChildException.class, () -> service.create(missing, invalid));

        assertThat(failure.code()).isEqualTo("VALIDATION_FAILED");
        assertThat(failure.fields()).extracting(error -> error.field()).containsExactly("name");
        assertChildCount(0);
    }

    @Test
    void unconfiguredCatalogueCannotRegisterFixtureCharacters() {
        var session = session();

        var failure = assertThrows(ChildException.class, () -> withoutCatalogue.create(session, profile()));

        assertThat(failure.code()).isEqualTo("VALIDATION_FAILED");
        assertThat(failure.fields()).extracting(error -> error.field()).containsExactly("characterId");
        assertChildCount(0);
    }

    @Test
    void accountAndContextLocksRequireTheRegistrationTransaction() {
        var session = session();

        assertThatThrownBy(() -> sessions.requireCurrentContextForUpdate(session.accountId(), session.securityContextId()))
                .isInstanceOf(IllegalTransactionStateException.class);
    }

    @Test
    void registrationWaitsForAccountLockAndRechecksVersion() throws Exception {
        var session = session();
        var entered = new CompletableFuture<Integer>();
        try (var worker = Executors.newSingleThreadExecutor()) {
            var pending = transaction().execute(status -> {
                var blocker = jdbc.queryForObject("SELECT pg_backend_pid()", Integer.class);
                jdbc.queryForList("SELECT id FROM accounts WHERE id = ? FOR UPDATE", UUID.class, session.accountId());
                var attempt = worker.submit(() -> transaction().execute(other -> {
                    jdbc.execute("SET LOCAL statement_timeout = '10s'");
                    entered.complete(jdbc.queryForObject("SELECT pg_backend_pid()", Integer.class));
                    return service.create(session, profile());
                }));
                awaitBlocked(entered.orTimeout(5, TimeUnit.SECONDS).join(), blocker);
                // 계정 대기 중 문맥을 먼저 잠그지 않았음을 실제 NOWAIT로 확인한다.
                jdbc.queryForList("SELECT id FROM session_security WHERE id = ? FOR UPDATE NOWAIT",
                        UUID.class, session.securityContextId());
                jdbc.update("UPDATE accounts SET session_version = 1 WHERE id = ?", session.accountId());
                return attempt;
            });

            assertThatThrownBy(() -> pending.get(5, TimeUnit.SECONDS))
                    .isInstanceOf(ExecutionException.class).hasCauseInstanceOf(InsufficientAuthenticationException.class);
            assertChildCount(0);
        }
    }

    @Test
    void registrationRechecksDbTimeAfterWaitingForContextLock() throws Exception {
        var session = session();
        var entered = new CompletableFuture<Integer>();
        try (var worker = Executors.newSingleThreadExecutor()) {
            var pending = transaction().execute(status -> {
                var blocker = jdbc.queryForObject("SELECT pg_backend_pid()", Integer.class);
                jdbc.queryForList("SELECT id FROM session_security WHERE id = ? FOR UPDATE",
                        UUID.class, session.securityContextId());
                var attempt = worker.submit(() -> transaction().execute(other -> {
                    jdbc.execute("SET LOCAL statement_timeout = '10s'");
                    entered.complete(jdbc.queryForObject("SELECT pg_backend_pid()", Integer.class));
                    return service.create(session, profile());
                }));
                awaitBlocked(entered.orTimeout(5, TimeUnit.SECONDS).join(), blocker);
                jdbc.update("UPDATE session_security SET expires_at = clock_timestamp() WHERE id = ?", session.securityContextId());
                return attempt;
            });

            assertThatThrownBy(() -> pending.get(5, TimeUnit.SECONDS))
                    .isInstanceOf(ExecutionException.class).hasCauseInstanceOf(InsufficientAuthenticationException.class);
            assertChildCount(0);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"CHILD", "AUTH"})
    void separatesBusinessStorageFailuresFromAuthStorageFailures(String storage) {
        var session = session();

        var failure = assertThrows(RuntimeException.class, () -> transaction().execute(status -> {
            if (storage.equals("CHILD")) jdbc.execute("ALTER TABLE children RENAME TO unavailable_children");
            else jdbc.execute("ALTER TABLE session_security RENAME TO unavailable_security");
            return service.create(session, profile());
        }));

        if (storage.equals("CHILD")) {
            assertThat(failure).isInstanceOfSatisfying(ChildException.class, business -> {
                assertThat(business.code()).isEqualTo("INTERNAL_ERROR");
                assertThat(business.getCause()).isInstanceOf(DataAccessException.class);
            });
        }
        else {
            assertThat(failure).isInstanceOf(AuthStateUnavailableException.class)
                    .hasCauseInstanceOf(DataAccessException.class);
        }
        assertChildCount(0);
    }

    @Test
    void twoTabsCreateExactlyOneChild() throws Exception {
        var current = httpSession();
        try (var client = HttpClient.newHttpClient()) {
            var csrf = csrf(client, current);
            var pending = transaction().execute(status -> {
                jdbc.queryForList("SELECT id FROM accounts WHERE id = ? FOR UPDATE", UUID.class, current.context().accountId());
                var first = client.sendAsync(registration(csrf), HttpResponse.BodyHandlers.ofString());
                var second = client.sendAsync(registration(csrf), HttpResponse.BodyHandlers.ofString());
                // 독립된 두 등록 Tx가 실제 계정 잠금을 기다린 뒤에만 잠금을 해제한다.
                assertThat(awaitAccountWaiters(2)).hasSize(2);
                return List.of(first, second);
            });

            var responses = List.of(pending.get(0).get(10, TimeUnit.SECONDS), pending.get(1).get(10, TimeUnit.SECONDS));
            assertThat(responses).extracting(HttpResponse::statusCode).containsExactlyInAnyOrder(201, 409);
            for (var response : responses) {
                if (response.statusCode() == 201) {
                    assertCreated(response, current.context().accountId());
                }
                else {
                    assertApiError(response, 409, "CHILD_LIMIT_REACHED");
                }
            }
            assertChildCount(1);
        }
    }

    @Test
    void differentAccountsCanCreateIndependently() throws Exception {
        var first = httpSession();
        var second = httpSession();
        try (var client = HttpClient.newHttpClient()) {
            var firstCsrf = csrf(client, first);
            var secondCsrf = csrf(client, second);
            var pending = transaction().execute(status -> {
                jdbc.queryForList("SELECT id FROM accounts WHERE id = ? FOR UPDATE", UUID.class, first.context().accountId());
                var waiting = client.sendAsync(registration(firstCsrf), HttpResponse.BodyHandlers.ofString());
                assertThat(awaitAccountWaiters(1)).hasSize(1);
                try {
                    // 첫 계정이 잠긴 동안 다른 계정은 같은 애칭으로도 저장·응답을 마쳐야 한다.
                    assertCreated(client.send(registration(secondCsrf), HttpResponse.BodyHandlers.ofString()),
                            second.context().accountId());
                }
                catch (Exception failure) {
                    throw new IllegalStateException("다른 계정의 등록이 완료되지 않았습니다.", failure);
                }
                return waiting;
            });

            assertCreated(pending.get(10, TimeUnit.SECONDS), first.context().accountId());
            assertChildCount(2);
            assertThat(jdbc.queryForList("SELECT nickname FROM children", String.class)).containsExactly("별이", "별이");
        }
    }

    @Test
    void revocationWinningTheLockRejectsRegistration() throws Exception {
        var current = httpSession();
        try (var client = HttpClient.newHttpClient()) {
            var csrf = csrf(client, current);
            var pending = transaction().execute(status -> {
                jdbc.queryForList("SELECT id FROM accounts WHERE id = ? FOR UPDATE", UUID.class, current.context().accountId());
                var waiting = client.sendAsync(registration(csrf), HttpResponse.BodyHandlers.ofString());
                assertThat(awaitAccountWaiters(1)).hasSize(1);
                // 요청의 최초 인증 이후 폐기를 먼저 커밋해 등록 Tx의 재검사를 검증한다.
                assertThat(sessions.revokeCurrentContext(current.context().accountId(), current.context().securityContextId()))
                        .isTrue();
                return waiting;
            });

            assertApiError(pending.get(10, TimeUnit.SECONDS), 401, "AUTHENTICATION_REQUIRED");
            assertChildCount(0);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"CONTEXT", "LOGOUT"})
    void revocationAfterRegistrationCommitBlocksResponse(String revocation) throws Exception {
        var current = httpSession();
        try (var client = HttpClient.newHttpClient()) {
            var csrf = csrf(client, current);
            var pause = responseGate.pauseNext();
            var pending = client.sendAsync(registration(csrf), HttpResponse.BodyHandlers.ofString());
            UUID childId;
            try {
                childId = pause.committed.get(5, TimeUnit.SECONDS);
                // 별도 연결에서 행을 읽어 등록이 이미 커밋됐고 아직 응답 전임을 확인한다.
                assertThat(jdbc.queryForObject("SELECT account_id FROM children WHERE id = ?", UUID.class, childId))
                        .isEqualTo(current.context().accountId());
                if (revocation.equals("LOGOUT")) {
                    var logout = client.send(request("/api/v1/auth/logout", csrf)
                            .POST(HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString());
                    assertThat(logout.statusCode()).isEqualTo(204);
                    assertThat(nativeSessions.findById(current.sessionId()) == null).isTrue();
                }
                else {
                    assertThat(sessions.revokeCurrentContext(current.context().accountId(), current.context().securityContextId()))
                            .isTrue();
                    assertThat(nativeSessions.findById(current.sessionId()) != null).isTrue();
                }
            }
            finally {
                pause.resume.countDown();
            }

            var response = pending.get(10, TimeUnit.SECONDS);
            assertApiError(response, 401, "AUTHENTICATION_REQUIRED");
            assertThat(response.body()).doesNotContain(childId.toString(), "가나", "별이");
            assertThat(response.headers().allValues("Set-Cookie").stream()
                    .filter(value -> value.startsWith("JSESSIONID=")).map(MockCookie::parse)
                    .anyMatch(cookie -> cookie.getMaxAge() > 0)).isFalse();
            assertChildCount(1);
            // POST를 재실행하지 않는다. 실제 복구용 목록 API는 SCRUM-81에서 제공한다.
            assertApiError(client.send(request(PATH, csrf).GET().build(), HttpResponse.BodyHandlers.ofString()),
                    401, "AUTHENTICATION_REQUIRED");
            if (revocation.equals("LOGOUT")) {
                assertThat(nativeSessions.findById(current.sessionId()) == null).isTrue();
            }
        }
    }

    @Test
    void businessConflictAndAuthStoreFailureStayDistinct() throws Exception {
        var current = httpSession();
        try (var client = HttpClient.newHttpClient()) {
            var csrf = csrf(client, current);
            assertCreated(client.send(registration(csrf), HttpResponse.BodyHandlers.ofString()), current.context().accountId());
            assertApiError(client.send(registration(csrf), HttpResponse.BodyHandlers.ofString()), 409, "CHILD_LIMIT_REACHED");

            jdbc.execute("ALTER TABLE session_security RENAME TO unavailable_http_security");
            try {
                assertApiError(client.send(registration(csrf), HttpResponse.BodyHandlers.ofString()), 503, "AUTH_STATE_UNAVAILABLE");
            }
            finally {
                jdbc.execute("ALTER TABLE unavailable_http_security RENAME TO session_security");
            }
            jdbc.execute("ALTER TABLE children RENAME TO unavailable_http_children");
            try {
                assertApiError(client.send(registration(csrf), HttpResponse.BodyHandlers.ofString()), 500, "INTERNAL_ERROR");
            }
            finally {
                jdbc.execute("ALTER TABLE unavailable_http_children RENAME TO children");
            }
            assertChildCount(1);
        }
    }

    private List<Integer> awaitAccountWaiters(int expected) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < deadline) {
            var waiting = jdbc.queryForList("""
                    SELECT DISTINCT pid FROM pg_locks
                    WHERE database = (SELECT oid FROM pg_database WHERE datname = current_database())
                      AND relation = 'accounts'::regclass AND mode = 'RowShareLock'
                      AND cardinality(pg_blocking_pids(pid)) > 0
                    """, Integer.class);
            if (waiting.size() >= expected) {
                return waiting;
            }
            Thread.yield();
        }
        throw new AssertionError("독립 등록 트랜잭션이 계정 잠금을 기다리지 않았습니다.");
    }

    private HttpFixture httpSession() {
        var current = session();
        Session stored = nativeSessions.createSession();
        stored.setMaxInactiveInterval(Duration.ofSeconds(-1));
        var security = SecurityContextHolder.createEmptyContext();
        security.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(current.accountId().toString(), null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))));
        stored.setAttribute("accountId", current.accountId());
        stored.setAttribute("securityContextId", current.securityContextId());
        stored.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, security);
        var response = new MockHttpServletResponse();
        cookies.writeCookieValue(new CookieSerializer.CookieValue(new MockHttpServletRequest(), response, stored.getId()));
        return new HttpFixture(current, stored.getId(), MockCookie.parse(response.getHeader("Set-Cookie")));
    }

    private Csrf csrf(HttpClient client, HttpFixture current) throws Exception {
        var sessionCookie = "JSESSIONID=" + current.cookie().getValue();
        var response = client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/auth/csrf"))
                .timeout(Duration.ofSeconds(10)).header("Cookie", sessionCookie).GET().build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        var csrfCookie = response.headers().allValues("Set-Cookie").stream()
                .filter(value -> value.startsWith("XSRF-TOKEN=")).map(MockCookie::parse).findFirst().orElseThrow();
        return new Csrf(sessionCookie + "; XSRF-TOKEN=" + csrfCookie.getValue(), JsonPath.read(response.body(), "$.token"));
    }

    private HttpRequest.Builder request(String path, Csrf csrf) {
        return HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).timeout(Duration.ofSeconds(10))
                .header("Cookie", csrf.cookieHeader()).header("X-XSRF-TOKEN", csrf.token()).header("Content-Type", "application/json");
    }

    private HttpRequest registration(Csrf csrf) {
        return request(PATH, csrf).POST(HttpRequest.BodyPublishers.ofString(HTTP_BODY)).build();
    }

    private void assertCreated(HttpResponse<String> response, UUID accountId) {
        assertThat(response.statusCode()).isEqualTo(201);
        String childId = JsonPath.read(response.body(), "$.data.childId");
        assertThat(response.headers().firstValue("Location")).contains(PATH + "/" + childId);
        assertThat(jdbc.queryForObject("SELECT account_id FROM children WHERE id = ?", UUID.class, UUID.fromString(childId)))
                .isEqualTo(accountId);
    }

    private void assertApiError(HttpResponse<String> response, int status, String code) {
        assertThat(response.statusCode()).isEqualTo(status);
        Map<String, Object> envelope = JsonPath.read(response.body(), "$");
        Map<String, Object> error = JsonPath.read(response.body(), "$.error");
        assertThat(envelope).containsOnlyKeys("error");
        assertThat(error).containsOnlyKeys("code", "message", "requestId", "fields");
        assertThat(error.get("code")).isEqualTo(code);
        assertThat((List<?>) error.get("fields")).isEmpty();
        assertThat(error.get("requestId")).isEqualTo(response.headers().firstValue("X-Request-ID").orElseThrow());
        assertThat(response.headers().firstValue("Cache-Control").orElseThrow()).contains("no-store");
        assertThat(response.headers().firstValue("Location")).isEmpty();
    }

    private void awaitBlocked(int worker, int blocker) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < deadline) {
            if (Boolean.TRUE.equals(jdbc.queryForObject("SELECT ? = ANY(pg_blocking_pids(?))", Boolean.class, blocker, worker))) return;
            Thread.yield();
        }
        throw new AssertionError("등록이 잠금 해제를 기다리지 않았습니다.");
    }

    private TransactionTemplate transaction() {
        return new TransactionTemplate(transactionManager);
    }

    private void assertChildCount(int expected) {
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM children", Integer.class)).isEqualTo(expected);
    }

    private UUID account() {
        var id = UUID.randomUUID();
        jdbc.update("INSERT INTO accounts (id, email) VALUES (?, ?)", id, id + "@example.invalid");
        return id;
    }

    private AuthenticatedSession session() {
        return sessions.createLoginContext(account(), null);
    }

    private CreateChildRequest profile() {
        return new CreateChildRequest("가나", "별이", LocalDate.of(2020, 5, 12),
                ChildView.Gender.FEMALE, List.of("공룡"), "test-character");
    }

    private record HttpFixture(AuthenticatedSession context, String sessionId, Cookie cookie) { }
    private record Csrf(String cookieHeader, String token) { }

    static class ResponsePause {
        final CompletableFuture<UUID> committed = new CompletableFuture<>();
        final CountDownLatch resume = new CountDownLatch(1);
    }

    @TestConfiguration(proxyBeanMethods = false)
    @ControllerAdvice(assignableTypes = ChildController.class)
    static class ResponseGate implements ResponseBodyAdvice<Object> {
        private final AtomicReference<ResponsePause> next = new AtomicReference<>();

        ResponsePause pauseNext() {
            var pause = new ResponsePause();
            assertThat(next.compareAndSet(null, pause)).as("one response gate is armed").isTrue();
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
        public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType contentType,
                Class<? extends HttpMessageConverter<?>> converterType, ServerHttpRequest request, ServerHttpResponse response) {
            if (body instanceof Map<?, ?> envelope && envelope.get("data") instanceof ChildView child) {
                var pause = next.getAndSet(null);
                if (pause != null) {
                    // 서비스 Tx가 반환된 뒤 JSON 직렬화 전에 응답만 멈춘다. 운영 코드는 건드리지 않는다.
                    pause.committed.complete(child.childId());
                    try {
                        if (!pause.resume.await(10, TimeUnit.SECONDS)) {
                            throw new IllegalStateException("폐기 요청이 완료되지 않았습니다.");
                        }
                    }
                    catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException("응답 대기가 중단됐습니다.", interrupted);
                    }
                }
            }
            return body;
        }
    }

}
