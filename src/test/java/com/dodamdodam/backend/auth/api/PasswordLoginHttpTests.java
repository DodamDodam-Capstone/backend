package com.dodamdodam.backend.auth.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.dodamdodam.backend.TestcontainersConfiguration;
import com.dodamdodam.backend.auth.domain.AuthRateLimitKey.Action;
import com.dodamdodam.backend.auth.infrastructure.EmailVerificationKeys;
import com.dodamdodam.backend.auth.support.PasswordLoginTestConfiguration;
import com.dodamdodam.backend.auth.support.PasswordLoginTestConfiguration.ObservedPasswords;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.HttpServletRequest;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpCookie;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.session.Session;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import tools.jackson.databind.json.JsonMapper;

@Import({TestcontainersConfiguration.class, PasswordLoginTestConfiguration.class, PasswordLoginHttpTests.BeforeResponse.class})
@ExtendWith(OutputCaptureExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"spring.session.jdbc.cleanup-cron=-", "logging.level.org.springframework.session=INFO",
                "logging.level.org.springframework.web.servlet.mvc.method.annotation.RequestResponseBodyMethodProcessor=DEBUG"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PasswordLoginHttpTests {
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String PASSWORD = "synthetic-password-🙂";
    private static String hash;
    @LocalServerPort private int port;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObservedPasswords passwords;
    @Autowired private EmailVerificationKeys keys;
    @Autowired private JdbcIndexedSessionRepository sessions;
    @Autowired private BeforeResponse beforeResponse;

    @BeforeAll
    static void encodeFixture(@Autowired ObservedPasswords passwords) {
        hash = passwords.encode(PASSWORD);
    }

    @BeforeEach
    @AfterEach
    void cleanRows() {
        beforeResponse.action.set(null);
        passwords.reset();
        jdbc.update("DELETE FROM SPRING_SESSION");
        jdbc.update("DELETE FROM auth_rate_limits");
        jdbc.update("DELETE FROM email_verifications");
        jdbc.update("DELETE FROM guardian_pins");
        jdbc.update("DELETE FROM children");
        jdbc.update("DELETE FROM session_security");
        jdbc.update("DELETE FROM accounts");
    }

    @ParameterizedTest
    @CsvSource({"false,false", "true,false", "false,true", "true,true"})
    void loginPersistsUnlimitedSessionAndReturnsCurrentFiveFieldAccount(boolean child, boolean pin) throws Exception {
        var fixture = account("u@example.invalid", hash, child, pin);
        var browser = new Browser();
        browser.csrf();
        var response = browser.login("\u00a0 U@EXAMPLE.INVALID \u2003", PASSWORD);
        var data = accountData(response, fixture);
        assertThat(data).containsEntry("hasPin", pin).containsEntry("guardianUnlockedUntil", null);
        var cookie = response.headers().allValues("Set-Cookie").stream().filter(v -> v.startsWith("JSESSIONID="))
                .flatMap(v -> HttpCookie.parse(v).stream()).toList();
        assertThat(cookie).hasSize(1);
        assertThat(cookie.getFirst().getMaxAge()).isEqualTo(31536000);
        assertThat(cookie.getFirst().isHttpOnly()).isTrue();
        Session stored = currentSession();
        assertThat(stored.getMaxInactiveInterval()).isEqualTo(Duration.ofSeconds(-1));
        assertThat(stored.<Object>getAttribute("accountId")).isEqualTo(fixture.account());
        assertThat(jdbc.queryForObject("SELECT expires_at IS NULL FROM session_security", Boolean.class)).isTrue();
        assertThat(jdbc.queryForObject("SELECT principal_name FROM SPRING_SESSION", String.class)).isEqualTo(fixture.account().toString());
        accountData(browser.get("/api/v1/auth/me"), fixture);
        assertError(browser.login(fixture.email(), PASSWORD), 403, "CSRF_INVALID");
        browser.csrf();
        accountData(browser.login(fixture.email(), PASSWORD), fixture);
    }

    @ParameterizedTest
    @ValueSource(strings = {"MISSING", "SOCIAL", "WRONG"})
    void accountExistenceAndSocialOnlyStatusUseSame401(String kind) throws Exception {
        if (!kind.equals("MISSING")) {
            account("u@example.invalid", kind.equals("SOCIAL") ? null : hash, false, false);
        }
        var browser = new Browser();
        browser.csrf();
        var response = browser.login("u@example.invalid", "synthetic-wrong-password");
        assertError(response, 401, "LOGIN_FAILED");
        assertThat((String) JsonPath.read(response.body(), "$.error.message")).isEqualTo("이메일 또는 비밀번호를 확인해 주세요.");
        assertThat(passwords.comparisons).hasValue(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM SPRING_SESSION", Long.class)).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"x", " ", "\n", "é", "e\u0301", "🙂"})
    void legacyPasswordIsComparedAsExactOriginalString(String original) throws Exception {
        var fixture = account("u@example.invalid", passwords.encode(original), false, false);
        var browser = new Browser();
        browser.csrf();
        accountData(browser.login(fixture.email(), original), fixture);
    }

    @Test
    void longUnicodeAndNullCharacterPasswordIsNeitherNormalizedNorTruncated() throws Exception {
        var original = " 🙂\u0000e\u0301 ".repeat(128);
        var fixture = account("u@example.invalid", passwords.encode(original), false, false);
        var browser = new Browser();
        browser.csrf();
        accountData(browser.login(fixture.email(), original), fixture);
        browser.csrf();
        assertError(browser.login(fixture.email(), original.strip()), 401, "LOGIN_FAILED");
    }

    @Test
    void replacementDeletesOldNativeSessionRevokesProofsAndDoesNotInheritGuardian() throws Exception {
        var fixture = account("u@example.invalid", hash, true, true);
        var browser = new Browser();
        browser.csrf();
        accountData(browser.login(fixture.email(), PASSWORD), fixture);
        Session old = currentSession();
        UUID oldContext = old.getAttribute("securityContextId");
        String oldCookie = browser.cookie("JSESSIONID");
        jdbc.update("UPDATE session_security SET guardian_unlocked_until=clock_timestamp()+INTERVAL '30 minutes',guardian_pin_version=1,setup_generation=4 WHERE id=?", oldContext);
        for (String purpose : List.of("PIN_SETUP", "PIN_RESET")) {
            jdbc.update("""
                    INSERT INTO email_verifications(id,email,purpose,eligible,code_hash,expires_at,
                        setup_account_id,setup_session_binding,setup_generation,pin_version_snapshot)
                    VALUES(?,?,?,true,?,clock_timestamp()+INTERVAL '10 minutes',?,?,4,?)
                    """, UUID.randomUUID(), fixture.email(), purpose, new byte[32], fixture.account(), oldContext,
                    purpose.equals("PIN_RESET") ? 1L : null);
        }
        browser.csrf();
        assertThat(accountData(browser.login(fixture.email(), PASSWORD), fixture)).containsEntry("guardianUnlockedUntil", null);
        Session fresh = currentSession();
        assertThat(fresh.getId()).isNotEqualTo(old.getId());
        assertThat(sessions.findById(old.getId())).isNull();
        assertThat(jdbc.queryForObject("SELECT revoked_at IS NOT NULL FROM session_security WHERE id=?", Boolean.class, oldContext)).isTrue();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM email_verifications WHERE invalidated_at IS NOT NULL", Long.class)).isEqualTo(2);
        var stale = HttpClient.newHttpClient().send(HttpRequest.newBuilder(uri("/api/v1/auth/me"))
                .header("Origin", "http://localhost:5173").header("Cookie", oldCookie).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertError(stale, 401, "AUTHENTICATION_REQUIRED");
    }

    @Test
    void wrongPasswordKeepsExistingLoginIntact() throws Exception {
        var fixture = account("u@example.invalid", hash, false, false);
        var browser = new Browser();
        browser.csrf();
        accountData(browser.login(fixture.email(), PASSWORD), fixture);
        String before = currentSession().getId();
        browser.csrf();
        assertError(browser.login(fixture.email(), "synthetic-wrong"), 401, "LOGIN_FAILED");
        assertThat(currentSession().getId()).isEqualTo(before);
        accountData(browser.get("/api/v1/auth/me"), fixture);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "null", "[]", "{", "{\"email\":\"u@example.invalid\"}",
            "{\"email\":\"u@example.invalid\",\"password\":null}",
            "{\"email\":\"u@example.invalid\",\"password\":1}",
            "{\"email\":\"u@example.invalid\",\"password\":\"x\",\"rememberMe\":true}",
            "{\"email\":\"u@example.invalid\",\"password\":\"x\",\"password\":\"y\"}",
            "{\"email\":\"u@example.invalid\",\"password\":\"x\"} {}",
            "{\"email\":\"u@example.invalid\",\"password\":\"\\ud800\"}"})
    void invalidJsonOrSchemaIs400AfterCsrfAndNeverCreatesLogin(String json) throws Exception {
        var browser = new Browser();
        browser.csrf();
        assertError(browser.post(json, true, "application/json"), 400, "VALIDATION_FAILED");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM session_security", Long.class)).isZero();
    }

    @Test
    void transmissionAndCsrfFailurePrecedeStructureAndLoginBudget() throws Exception {
        var browser = new Browser();
        assertError(browser.post("{}", false, "text/plain"), 415, "UNSUPPORTED_MEDIA_TYPE");
        assertError(browser.post(" ".repeat(16385), false, "application/json"), 413, "REQUEST_TOO_LARGE");
        assertError(browser.post("{", false, "application/json"), 403, "CSRF_INVALID");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM auth_rate_limits WHERE action LIKE 'LOGIN_%'", Long.class)).isZero();
    }

    @Test
    void malformedUtf8CannotBeSilentlyReplacedAndMatchDifferentPassword() throws Exception {
        account("u@example.invalid", passwords.encode("\ufffd"), false, false);
        var browser = new Browser();
        browser.csrf();
        byte[] prefix = "{\"email\":\"u@example.invalid\",\"password\":\"".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] body = java.util.Arrays.copyOf(prefix, prefix.length + 3);
        body[prefix.length] = (byte) 0xc3;
        body[prefix.length + 1] = '"';
        body[prefix.length + 2] = '}';
        var request = HttpRequest.newBuilder(uri("/api/v1/auth/login")).header("Content-Type", "application/json")
                .header("Origin", "http://localhost:5173").header("X-XSRF-TOKEN", browser.token)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body)).build();
        assertError(browser.client.send(request, HttpResponse.BodyHandlers.ofString()), 400, "VALIDATION_FAILED");
        assertThat(passwords.comparisons).hasValue(0);
    }

    @ParameterizedTest
    @ValueSource(strings = {"OVERLONG_SLASH", "CESU8_EMOJI", "UTF16LE", "UTF16BE", "UTF32LE", "UTF32BE"})
    void invalidByteEncodingsCannotBecomeValidPassword(String encoding) throws Exception {
        String expected = encoding.equals("CESU8_EMOJI") ? "🙂" : "/";
        account("u@example.invalid", passwords.encode(expected), false, false);
        var browser = new Browser();
        browser.csrf();
        byte[] body;
        if (encoding.equals("OVERLONG_SLASH") || encoding.equals("CESU8_EMOJI")) {
            byte[] prefix = "{\"email\":\"u@example.invalid\",\"password\":\""
                    .getBytes(java.nio.charset.StandardCharsets.UTF_8);
            byte[] invalid = encoding.equals("OVERLONG_SLASH") ? new byte[] {(byte) 0xc0, (byte) 0xaf}
                    : new byte[] {(byte) 0xed, (byte) 0xa0, (byte) 0xbd, (byte) 0xed, (byte) 0xb9, (byte) 0x82};
            body = java.util.Arrays.copyOf(prefix, prefix.length + invalid.length + 2);
            System.arraycopy(invalid, 0, body, prefix.length, invalid.length);
            body[body.length - 2] = '"';
            body[body.length - 1] = '}';
        }
        else {
            String name = switch (encoding) {
                case "UTF16LE" -> "UTF-16LE";
                case "UTF16BE" -> "UTF-16BE";
                case "UTF32LE" -> "UTF-32LE";
                case "UTF32BE" -> "UTF-32BE";
                default -> throw new AssertionError(encoding);
            };
            body = JSON.writeValueAsString(Map.of("email", "u@example.invalid", "password", expected))
                    .getBytes(java.nio.charset.Charset.forName(name));
        }
        var request = HttpRequest.newBuilder(uri("/api/v1/auth/login")).header("Content-Type", "application/json")
                .header("Origin", "http://localhost:5173").header("X-XSRF-TOKEN", browser.token)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body)).build();

        assertError(browser.client.send(request, HttpResponse.BodyHandlers.ofString()), 400, "VALIDATION_FAILED");
        assertThat(passwords.comparisons).hasValue(0);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM session_security", Long.class)).isZero();
    }

    @Test
    void exact16KiBBodyIsAcceptedAndNextByteIs413() throws Exception {
        var fixture = account("u@example.invalid", hash, false, false);
        var browser = new Browser();
        browser.csrf();
        String json = JSON.writeValueAsString(Map.of("email", fixture.email(), "password", PASSWORD));
        String exact = json + " ".repeat(16384 - json.getBytes(java.nio.charset.StandardCharsets.UTF_8).length);
        accountData(browser.post(exact, true, "application/json"), fixture);
        assertError(browser.post(exact + " ", false, "application/json"), 413, "REQUEST_TOO_LARGE");
    }

    @Test
    void successAndFailureBothConsumeEmailBudgetAndNewSessionDoesNotResetIt() throws Exception {
        var fixture = account("u@example.invalid", hash, false, false);
        var browser = new Browser();
        for (int attempt = 0; attempt < 10; attempt++) {
            browser.csrf();
            var response = browser.login(attempt % 2 == 0 ? " U@EXAMPLE.INVALID " : fixture.email(),
                    attempt % 2 == 0 ? PASSWORD : "synthetic-wrong");
            if (attempt % 2 == 0) {
                accountData(response, fixture);
            }
            else {
                assertError(response, 401, "LOGIN_FAILED");
            }
        }
        browser.csrf();
        var denied = browser.login(fixture.email(), PASSWORD);
        assertError(denied, 429, "RATE_LIMITED");
        assertThat(Long.parseLong(denied.headers().firstValue("Retry-After").orElseThrow())).isBetween(1L, 900L);
        assertThat(jdbc.queryForObject("SELECT attempt_count FROM auth_rate_limits WHERE action='LOGIN_EMAIL'", Long.class)).isEqualTo(10);
        assertThat(passwords.comparisons).hasValue(10);
    }

    @Test
    void ipBudgetBoundaryCannotBeResetWithForwardedHeaders() throws Exception {
        var fixture = account("u@example.invalid", hash, false, false);
        var ip = keys.rateHasher().loginKeys(fixture.email(), InetAddress.ofLiteral("127.0.0.1")).stream()
                .filter(k -> k.action() == Action.LOGIN_IP).findFirst().orElseThrow();
        jdbc.update("""
                INSERT INTO auth_rate_limits(action,key_hash,window_started_at,attempt_count,expires_at)
                VALUES('LOGIN_IP',?,clock_timestamp(),99,clock_timestamp()+INTERVAL '900 seconds')
                """, ip.keyHash());
        var browser = new Browser();
        browser.csrf();
        accountData(browser.login(fixture.email(), PASSWORD), fixture);
        browser.csrf();
        assertError(browser.login("other@example.invalid", PASSWORD), 429, "RATE_LIMITED");
        assertThat(jdbc.queryForObject("SELECT attempt_count FROM auth_rate_limits WHERE action='LOGIN_IP'", Long.class)).isEqualTo(100);
    }

    @ParameterizedTest
    @ValueSource(strings = {"HASH", "VERSION"})
    void credentialChangeAfterRealComparisonCannotCreateSession(String type) throws Exception {
        var fixture = account("u@example.invalid", hash, false, false);
        passwords.afterComparison.set(() -> jdbc.update(type.equals("HASH")
                ? "UPDATE accounts SET password_hash='synthetic-changed' WHERE id=?"
                : "UPDATE accounts SET session_version=1 WHERE id=?", fixture.account()));
        var browser = new Browser();
        browser.csrf();
        assertError(browser.login(fixture.email(), PASSWORD), 401, "LOGIN_FAILED");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM session_security", Long.class)).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"VERSION", "REVOKED", "DELETED_NATIVE"})
    void invalidationAfterControllerReturns503WithoutSuccessCookieOrBody(String type) throws Exception {
        var fixture = account("u@example.invalid", hash, false, false);
        beforeResponse.action.set(request -> {
            switch (type) {
                case "VERSION" -> jdbc.update("UPDATE accounts SET session_version=1 WHERE id=?", fixture.account());
                case "REVOKED" -> jdbc.update("UPDATE session_security SET revoked_at=clock_timestamp()");
                case "DELETED_NATIVE" -> sessions.deleteById(request.getSession(false).getId());
                default -> throw new AssertionError(type);
            }
        });
        var browser = new Browser();
        browser.csrf();
        var response = browser.login(fixture.email(), PASSWORD);
        assertError(response, 503, "AUTH_STATE_UNAVAILABLE");
        assertThat(response.body()).doesNotContain(fixture.email(), fixture.account().toString());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM session_security WHERE revoked_at IS NULL", Long.class)).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"58000", "23514"})
    void nativeContextWriteFailureCompensatesNewLoginAndKeepsOldRevoked(String sqlState) throws Exception {
        var fixture = account("u@example.invalid", hash, false, false);
        var browser = new Browser();
        browser.csrf();
        accountData(browser.login(fixture.email(), PASSWORD), fixture);
        browser.csrf();
        jdbc.execute("""
                CREATE FUNCTION password_native_failure() RETURNS trigger LANGUAGE plpgsql AS $$
                BEGIN
                    IF NEW.ATTRIBUTE_NAME='SPRING_SECURITY_CONTEXT' THEN
                        RAISE EXCEPTION 'synthetic native failure' USING ERRCODE='%s';
                    END IF;
                    RETURN NEW;
                END;
                $$
                """.formatted(sqlState));
        jdbc.execute("CREATE TRIGGER password_native_failure BEFORE INSERT OR UPDATE ON SPRING_SESSION_ATTRIBUTES FOR EACH ROW EXECUTE FUNCTION password_native_failure()");
        try {
            assertError(browser.login(fixture.email(), PASSWORD), 503, "AUTH_STATE_UNAVAILABLE");
            assertThat(jdbc.queryForObject("SELECT count(*) FROM SPRING_SESSION", Long.class)).isZero();
            assertThat(jdbc.queryForObject("SELECT count(*) FROM session_security WHERE revoked_at IS NULL", Long.class)).isZero();
            assertThat(jdbc.queryForObject("SELECT count(*) FROM session_security", Long.class)).isEqualTo(2);
        }
        finally {
            jdbc.execute("DROP TRIGGER password_native_failure ON SPRING_SESSION_ATTRIBUTES");
            jdbc.execute("DROP FUNCTION password_native_failure()");
        }
    }

    @Test
    void accountViewFailureAfterSessionSaveCompensatesLogin() throws Exception {
        var fixture = account("u@example.invalid", hash, false, false);
        jdbc.execute("ALTER TABLE children RENAME TO unavailable_password_children");
        try {
            var browser = new Browser();
            browser.csrf();
            assertError(browser.login(fixture.email(), PASSWORD), 503, "AUTH_STATE_UNAVAILABLE");
        }
        finally {
            jdbc.execute("ALTER TABLE unavailable_password_children RENAME TO children");
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM SPRING_SESSION", Long.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM session_security WHERE revoked_at IS NULL", Long.class)).isZero();
    }

    @Test
    void frameworkDebugLogsAndSerializedSessionDoNotContainCredentials(CapturedOutput output) throws Exception {
        var fixture = account("u@example.invalid", hash, false, false);
        var browser = new Browser();
        browser.csrf();
        accountData(browser.login(fixture.email(), PASSWORD), fixture);
        assertThat(output.getAll()).doesNotContain(PASSWORD, fixture.email(), hash);
        for (var bytes : jdbc.queryForList("SELECT ATTRIBUTE_BYTES FROM SPRING_SESSION_ATTRIBUTES", byte[].class)) {
            assertThat(new String(bytes, java.nio.charset.StandardCharsets.ISO_8859_1)).doesNotContain(PASSWORD, hash);
        }
    }

    /** 계정·아이·PIN의 실제 FK 제약을 만족하는 합성 계정을 만든다. */
    private Fixture account(String email, String passwordHash, boolean child, boolean pin) {
        var id = UUID.randomUUID();
        jdbc.update("INSERT INTO accounts(id,email,password_hash) VALUES(?,?,?)", id, email, passwordHash);
        UUID childId = child ? UUID.randomUUID() : null;
        if (child) {
            jdbc.update("INSERT INTO children(id,account_id,name,nickname,birth_date,gender,character_id) VALUES(?,?,'도담','도담이',DATE '2020-01-01','FEMALE','synthetic')", childId, id);
        }
        if (pin) {
            jdbc.update("INSERT INTO guardian_pins(account_id,pin_hash) VALUES(?,'synthetic-pin-hash')", id);
        }
        return new Fixture(id, email, childId);
    }

    /** 응답한 쿠키가 실제로 접근할 수 있는 native 세션의 저장 상태를 읽는다. */
    private Session currentSession() {
        String id = jdbc.queryForObject("SELECT SESSION_ID FROM SPRING_SESSION", String.class);
        return sessions.findById(id);
    }

    /** 5개 공개 필드와 nullable·공통 응답 헤더를 확인한다. */
    private Map<String, Object> accountData(HttpResponse<String> response, Fixture fixture) {
        assertThat(response.statusCode()).isEqualTo(200);
        Map<String, Object> envelope = JsonPath.read(response.body(), "$");
        assertThat(envelope).containsOnlyKeys("data");
        Map<String, Object> data = JsonPath.read(response.body(), "$.data");
        assertThat(data).containsOnlyKeys("accountId", "email", "childId", "hasPin", "guardianUnlockedUntil")
                .containsEntry("accountId", fixture.account().toString()).containsEntry("email", fixture.email())
                .containsEntry("childId", fixture.child() == null ? null : fixture.child().toString());
        commonHeaders(response);
        return data;
    }

    /** 실패에는 성공 본문·자격 원문·갱신 로그인 쿠키가 포함되지 않아야 한다. */
    private void assertError(HttpResponse<String> response, int status, String code) {
        assertThat(response.statusCode()).isEqualTo(status);
        Map<String, Object> envelope = JsonPath.read(response.body(), "$");
        assertThat(envelope).containsOnlyKeys("error");
        Map<String, Object> error = JsonPath.read(response.body(), "$.error");
        assertThat(error).containsOnlyKeys("code", "message", "fields", "requestId")
                .containsEntry("code", code).containsEntry("requestId", response.headers().firstValue("X-Request-ID").orElseThrow());
        assertThat(response.body()).doesNotContain(PASSWORD, hash, "SELECT", "synthetic native failure");
        assertThat(response.headers().allValues("Set-Cookie")).noneMatch(v -> v.startsWith("JSESSIONID=")
                && HttpCookie.parse(v).getFirst().getMaxAge() > 0);
        commonHeaders(response);
    }

    /** 성공·실패 모두 같은 추적·캐시·CORS 계약으로 응답한다. */
    private void commonHeaders(HttpResponse<String> response) {
        assertThat(UUID.fromString(response.headers().firstValue("X-Request-ID").orElseThrow())).isNotNull();
        assertThat(response.headers().firstValue("Cache-Control").orElseThrow()).contains("no-store");
        assertThat(response.headers().firstValue("Content-Type").orElseThrow()).startsWith("application/json");
        assertThat(response.headers().firstValue("Location")).isEmpty();
        assertThat(response.headers().firstValue("Access-Control-Allow-Origin")).contains("http://localhost:5173");
        assertThat(response.headers().firstValue("Access-Control-Allow-Credentials")).contains("true");
    }

    private URI uri(String path) {
        return URI.create("http://127.0.0.1:" + port + path);
    }

    private record Fixture(UUID account, String email, UUID child) { }

    class Browser {
        final CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        final HttpClient client = HttpClient.newBuilder().cookieHandler(cookies).build();
        String token;

        /** 실제 발급 응답과 브라우저 쿠키 저장을 사용한다. */
        void csrf() throws Exception {
            var response = get("/api/v1/auth/csrf");
            assertThat(response.statusCode()).isEqualTo(200);
            token = JsonPath.read(response.body(), "$.token");
        }

        HttpResponse<String> get(String path) throws Exception {
            return client.send(HttpRequest.newBuilder(uri(path)).header("Origin", "http://localhost:5173").GET().build(),
                    HttpResponse.BodyHandlers.ofString());
        }

        HttpResponse<String> login(String email, String password) throws Exception {
            return post(JSON.writeValueAsString(Map.of("email", email, "password", password)), true, "application/json");
        }

        /** 신뢰할 수 없는 전달 헤더가 실제 IP 예산을 바꾸지 않도록 항상 합성 헤더를 보낸다. */
        HttpResponse<String> post(String body, boolean withCsrf, String contentType) throws Exception {
            var builder = HttpRequest.newBuilder(uri("/api/v1/auth/login"))
                    .header("Origin", "http://localhost:5173").header("Content-Type", contentType)
                    .header("X-Forwarded-For", UUID.randomUUID() + ".invalid")
                    .header("Forwarded", "for=192.0.2.99");
            if (withCsrf && token != null) {
                builder.header("X-XSRF-TOKEN", token);
            }
            return client.send(builder.POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
        }

        String cookie(String name) {
            var cookie = cookies.getCookieStore().getCookies().stream().filter(c -> c.getName().equals(name)).findFirst().orElseThrow();
            return cookie.getName() + "=" + cookie.getValue();
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    @RestControllerAdvice(basePackages = "com.dodamdodam.backend.auth.api")
    static class BeforeResponse implements ResponseBodyAdvice<Object> {
        final AtomicReference<Consumer<HttpServletRequest>> action = new AtomicReference<>();

        @Override
        public boolean supports(MethodParameter type, Class<? extends HttpMessageConverter<?>> converter) {
            return type.getContainingClass().getSimpleName().equals("PasswordLoginController");
        }

        /** Controller가 성공 데이터를 만든 다음 실제 전송 전에만 한 번 경합을 주입한다. */
        @Override
        public Object beforeBodyWrite(Object body, MethodParameter type, MediaType mediaType,
                Class<? extends HttpMessageConverter<?>> converter, ServerHttpRequest request, ServerHttpResponse response) {
            var current = action.getAndSet(null);
            if (current != null) {
                current.accept(((ServletServerHttpRequest) request).getServletRequest());
            }
            return body;
        }
    }
}
