package com.dodamdodam.backend.auth.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.dodamdodam.backend.TestcontainersConfiguration;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import java.net.HttpCookie;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
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
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@Import({TestcontainersConfiguration.class, CurrentAccountHttpTests.BeforeWriteHook.class})
@ExtendWith(OutputCaptureExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"logging.level.org.springframework.session=INFO", "spring.session.jdbc.cleanup-cron=-",
                "logging.level.org.springframework.web.servlet.mvc.method.annotation.RequestResponseBodyMethodProcessor=DEBUG"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CurrentAccountHttpTests {

    private static final String PATH = "/api/v1/auth/me";
    private static final String ORIGIN = "http://localhost:5173";
    private final HttpClient client = HttpClient.newHttpClient();
    @LocalServerPort private int port;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private JdbcIndexedSessionRepository sessions;
    @Autowired private CookieSerializer cookies;
    @Autowired private BeforeWriteHook hook;

    @BeforeEach
    @AfterEach
    void cleanRows() {
        hook.action.set(null);
        jdbc.update("DELETE FROM SPRING_SESSION");
        jdbc.update("DELETE FROM guardian_pins");
        jdbc.update("DELETE FROM children");
        jdbc.update("DELETE FROM session_security");
        jdbc.update("DELETE FROM accounts");
    }

    @Test
    void beforeChildReturnsAllFiveFieldsAndExplicitNullsWithoutCsrf() throws Exception {
        var fixture = authenticated(false, false);

        var response = get(fixture);
        var data = assertAccount(response, fixture);

        assertThat(data).containsEntry("childId", null).containsEntry("hasPin", false)
                .containsEntry("guardianUnlockedUntil", null);
        assertThat(response.headers().firstValue("Access-Control-Allow-Origin")).contains(ORIGIN);
        assertThat(response.headers().firstValue("Access-Control-Allow-Credentials")).contains("true");
        var renewed = response.headers().allValues("Set-Cookie").stream().map(HttpCookie::parse)
                .flatMap(List::stream).filter(cookie -> cookie.getName().equals("JSESSIONID")).toList();
        assertThat(renewed).hasSize(1);
        assertThat(renewed.getFirst().getMaxAge()).isEqualTo(31536000);
    }

    @Test
    void afterChildBeforePinReturnsOnlyCurrentAccountsChild() throws Exception {
        var fixture = authenticated(true, false);
        authenticated(true, true);

        var data = assertAccount(get(fixture), fixture);

        assertThat(data).containsEntry("childId", fixture.childId().toString()).containsEntry("hasPin", false)
                .containsEntry("guardianUnlockedUntil", null);
    }

    @Test
    void pinSetAndLockedIsSuccessfulAccountState() throws Exception {
        var fixture = authenticated(true, true);
        var data = assertAccount(get(fixture), fixture);

        assertThat(data).containsEntry("hasPin", true).containsEntry("guardianUnlockedUntil", null);
    }

    @Test
    void currentGrantReturnsKstSecondsAndRepeatedReadsDoNotExtendIt() throws Exception {
        var fixture = authenticated(true, true);
        var until = grant(fixture);
        var expected = until.withOffsetSameInstant(ZoneOffset.ofHours(9)).truncatedTo(ChronoUnit.SECONDS)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX"));

        for (int request = 0; request < 2; request++) {
            var data = assertAccount(get(fixture), fixture);
            assertThat(data).containsEntry("hasPin", true).containsEntry("guardianUnlockedUntil", expected);
        }
        assertThat(jdbc.queryForObject("SELECT guardian_unlocked_until FROM session_security WHERE id = ?",
                OffsetDateTime.class, fixture.contextId())).isEqualTo(until);
    }

    @ParameterizedTest
    @ValueSource(strings = {"EXPIRED", "OLD_PIN", "NO_PIN", "LOCKED"})
    void invalidGuardianIsNullRatherThan403(String change) throws Exception {
        var fixture = authenticated(true, true);
        grant(fixture);
        switch (change) {
            case "EXPIRED" -> jdbc.update("UPDATE session_security SET guardian_unlocked_until = clock_timestamp() - INTERVAL '1 second' WHERE id = ?", fixture.contextId());
            case "OLD_PIN" -> jdbc.update("UPDATE guardian_pins SET pin_version = 2 WHERE account_id = ?", fixture.accountId());
            case "NO_PIN" -> jdbc.update("DELETE FROM guardian_pins WHERE account_id = ?", fixture.accountId());
            case "LOCKED" -> jdbc.update("UPDATE session_security SET guardian_unlocked_until = NULL, guardian_pin_version = NULL WHERE id = ?", fixture.contextId());
            default -> throw new AssertionError(change);
        }

        assertThat(assertAccount(get(fixture), fixture)).containsEntry("hasPin", !change.equals("NO_PIN"))
                .containsEntry("guardianUnlockedUntil", null);
    }

    @Test
    void anonymousRequestDoesNotCreateLoginSession() throws Exception {
        assertError(get(null), 401, "AUTHENTICATION_REQUIRED");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM SPRING_SESSION", Integer.class)).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"REVOKED", "EXPIRED", "ACCOUNT_VERSION", "WRONG_CONTEXT", "NATIVE_EXPIRED"})
    void invalidAuthenticationReturns401(String change) throws Exception {
        var fixture = authenticated(false, false);
        switch (change) {
            case "REVOKED" -> jdbc.update("UPDATE session_security SET revoked_at = clock_timestamp() WHERE id = ?", fixture.contextId());
            case "EXPIRED" -> jdbc.update("UPDATE session_security SET expires_at = clock_timestamp() - INTERVAL '1 second' WHERE id = ?", fixture.contextId());
            case "ACCOUNT_VERSION" -> jdbc.update("UPDATE accounts SET session_version = 1 WHERE id = ?", fixture.accountId());
            case "WRONG_CONTEXT" -> {
                var other = authenticated(false, false);
                Session stored = sessions.findById(fixture.sessionId());
                stored.setAttribute("securityContextId", other.contextId());
            }
            case "NATIVE_EXPIRED" -> {
                Session stored = sessions.findById(fixture.sessionId());
                stored.setMaxInactiveInterval(Duration.ofSeconds(30));
                stored.setLastAccessedTime(Instant.now().minusSeconds(120));
            }
            default -> throw new AssertionError(change);
        }

        assertError(get(fixture), 401, "AUTHENTICATION_REQUIRED");
    }

    @ParameterizedTest
    @ValueSource(strings = {"children", "guardian_pins"})
    void projectionDatabaseFailureReturns503(String table) throws Exception {
        var fixture = authenticated(true, true);
        jdbc.execute("ALTER TABLE " + table + " RENAME TO unavailable_" + table);
        try {
            assertError(get(fixture), 503, "AUTH_STATE_UNAVAILABLE");
        }
        finally {
            jdbc.execute("ALTER TABLE unavailable_" + table + " RENAME TO " + table);
        }
    }

    @Test
    void invalidStoredEmailDoesNotEscapeInSuccessOrError() throws Exception {
        var fixture = authenticated(false, false);
        jdbc.update("UPDATE accounts SET email = 'synthetic-invalid-email' WHERE id = ?", fixture.accountId());

        var response = get(fixture);

        assertError(response, 503, "AUTH_STATE_UNAVAILABLE");
        assertThat(response.body()).doesNotContain("synthetic-invalid-email");
    }

    @Test
    void frameworkResponseLoggingDoesNotExposeAccountEmail(CapturedOutput output) throws Exception {
        var fixture = authenticated(false, false);
        String email = "u@example.invalid";
        jdbc.update("UPDATE accounts SET email = ? WHERE id = ?", email, fixture.accountId());

        var response = get(fixture);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat((String) JsonPath.read(response.body(), "$.data.email")).isEqualTo(email);
        assertThat(output.getAll()).doesNotContain(email);
    }

    @ParameterizedTest
    @ValueSource(strings = {"REVOKED", "DELETED_NATIVE"})
    void authenticationLostAfterControllerDoesNotExposeSuccessOrRenewalCookie(String change) throws Exception {
        var fixture = authenticated(true, true);
        grant(fixture);
        hook.action.set(() -> {
            if (change.equals("REVOKED")) {
                jdbc.update("UPDATE session_security SET revoked_at = clock_timestamp() WHERE id = ?", fixture.contextId());
            }
            else {
                sessions.deleteById(fixture.sessionId());
            }
        });

        var response = get(fixture);

        assertError(response, 401, "AUTHENTICATION_REQUIRED");
        assertThat(response.body()).doesNotContain(fixture.email(), fixture.childId().toString());
    }

    @Test
    void authenticationStorageFailureBeforeResponseReturns503() throws Exception {
        var fixture = authenticated(true, true);
        hook.action.set(() -> jdbc.execute("ALTER TABLE session_security RENAME TO unavailable_response_security"));
        try {
            assertError(get(fixture), 503, "AUTH_STATE_UNAVAILABLE");
        }
        finally {
            jdbc.execute("ALTER TABLE unavailable_response_security RENAME TO session_security");
        }
    }

    /** 쿠키 직렬화까지 운영과 같은 JDBC 세션으로 구성한다. */
    private Fixture authenticated(boolean child, boolean pin) {
        var accountId = UUID.randomUUID();
        var contextId = UUID.randomUUID();
        var email = accountId + "@example.invalid";
        jdbc.update("INSERT INTO accounts (id, email) VALUES (?, ?)", accountId, email);
        jdbc.update("""
                INSERT INTO session_security (id, account_id, session_version, created_at, expires_at)
                VALUES (?, ?, 0, clock_timestamp() - INTERVAL '1 minute', NULL)
                """, contextId, accountId);
        UUID childId = child ? UUID.randomUUID() : null;
        if (child) {
            jdbc.update("""
                    INSERT INTO children (id, account_id, name, nickname, birth_date, gender, character_id)
                    VALUES (?, ?, '도담', '도담이', DATE '2020-01-01', 'FEMALE', 'synthetic')
                    """, childId, accountId);
        }
        if (pin) {
            jdbc.update("INSERT INTO guardian_pins (account_id, pin_hash) VALUES (?, 'synthetic-hash')", accountId);
        }
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
        Cookie cookie = MockCookie.parse(response.getHeader("Set-Cookie"));
        return new Fixture(accountId, contextId, childId, email, session.getId(), cookie.getName() + "=" + cookie.getValue());
    }

    /** CSRF 없이 읽을 수 있는 GET에 현재 세션과 허용 origin만 전달한다. */
    private HttpResponse<String> get(Fixture fixture) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + PATH)).header("Origin", ORIGIN);
        if (fixture != null) {
            builder.header("Cookie", fixture.cookie());
        }
        return client.send(builder.GET().build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    /** 현재 PIN 버전과 결합한 실제 DB 기한을 반환해 초 단위 JSON과 비교한다. */
    private OffsetDateTime grant(Fixture fixture) {
        return jdbc.queryForObject("""
                UPDATE session_security SET guardian_unlocked_until = clock_timestamp() + INTERVAL '15 minutes',
                    guardian_pin_version = 1 WHERE id = ? RETURNING guardian_unlocked_until
                """, OffsetDateTime.class, fixture.contextId());
    }

    /** 성공 envelope와 공개 필드를 정확히 확인해 내부 권한·해시 노출을 막는다. */
    private Map<String, Object> assertAccount(HttpResponse<String> response, Fixture fixture) {
        assertThat(response.statusCode()).isEqualTo(200);
        Map<String, Object> envelope = JsonPath.read(response.body(), "$");
        assertThat(envelope).containsOnlyKeys("data");
        Map<String, Object> data = JsonPath.read(response.body(), "$.data");
        assertThat(data).containsOnlyKeys("accountId", "email", "childId", "hasPin", "guardianUnlockedUntil")
                .containsEntry("accountId", fixture.accountId().toString()).containsEntry("email", fixture.email());
        assertCommonHeaders(response);
        return data;
    }

    /** 응답 전 거부에서도 성공 데이터와 갱신 쿠키가 남지 않도록 검사한다. */
    private void assertError(HttpResponse<String> response, int status, String code) {
        assertThat(response.statusCode()).isEqualTo(status);
        Map<String, Object> envelope = JsonPath.read(response.body(), "$");
        assertThat(envelope).containsOnlyKeys("error");
        Map<String, Object> error = JsonPath.read(response.body(), "$.error");
        assertThat(error).containsOnlyKeys("code", "message", "requestId", "fields").containsEntry("code", code)
                .containsEntry("requestId", response.headers().firstValue("X-Request-ID").orElseThrow());
        assertThat((List<?>) error.get("fields")).isEmpty();
        assertThat(response.headers().allValues("Set-Cookie")).noneMatch(value -> value.startsWith("JSESSIONID=")
                && HttpCookie.parse(value).getFirst().getMaxAge() > 0);
        assertThat(response.body()).doesNotContain("SELECT", "unavailable_", "synthetic-hash", "org.springframework");
        assertCommonHeaders(response);
    }

    /** 공통 추적·캐시·응답 형식은 정상과 오류에서 동일하게 유지한다. */
    private void assertCommonHeaders(HttpResponse<String> response) {
        var requestId = response.headers().firstValue("X-Request-ID").orElseThrow();
        assertThat(UUID.fromString(requestId).toString()).isEqualTo(requestId);
        assertThat(response.headers().firstValue("Cache-Control").orElseThrow()).contains("no-store");
        assertThat(response.headers().firstValue("Content-Type").orElseThrow()).startsWith("application/json");
        assertThat(response.headers().firstValue("Location")).isEmpty();
    }

    private record Fixture(UUID accountId, UUID contextId, UUID childId, String email, String sessionId, String cookie) { }

    @TestConfiguration(proxyBeanMethods = false)
    @RestControllerAdvice(basePackages = "com.dodamdodam.backend.auth.api")
    static class BeforeWriteHook implements ResponseBodyAdvice<Object> {
        private final AtomicReference<Runnable> action = new AtomicReference<>();

        /** 현재 계정 Controller의 결과가 만들어진 뒤 실제 직렬화 직전에 경합을 재현한다. */
        @Override
        public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
            return returnType.getContainingClass().getSimpleName().equals("AuthMeController");
        }

        /** 오류 응답에는 반복 적용하지 않고 정상 처리 직후 한 번만 변경한다. */
        @Override
        public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType contentType,
                Class<? extends HttpMessageConverter<?>> converterType, ServerHttpRequest request, ServerHttpResponse response) {
            var change = action.getAndSet(null);
            if (change != null) {
                change.run();
            }
            return body;
        }
    }
}
