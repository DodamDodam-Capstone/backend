package com.dodamdodam.backend.auth.api;

import com.dodamdodam.backend.TestcontainersConfiguration;
import com.dodamdodam.backend.auth.infrastructure.SmtpEmailVerificationSender;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Timeout(30)
class SignupHttpTests {
    private static final String PATH = "/api/v1/auth/signup";
    private static final String EMAIL = "pa.rent+tag@example.invalid";
    private static final String PASSWORD = "synthetic-password-example";
    private static final String FIXTURE_SECRET = "A".repeat(43);

    @LocalServerPort private int port;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private JsonMapper json;
    @Autowired private PasswordEncoder passwords;
    @MockitoBean private SmtpEmailVerificationSender sender;

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5)).build();

    @BeforeEach
    @AfterEach
    void clearRows() {
        jdbc.update("DELETE FROM spring_session");
        jdbc.update("DELETE FROM email_verifications");
        jdbc.update("DELETE FROM auth_rate_limits");
        jdbc.update("DELETE FROM session_security");
        jdbc.update("DELETE FROM accounts");
    }

    @ParameterizedTest(name = "{index} 신규 비밀번호 길이 경계")
    @MethodSource("creationBoundaryPasswords")
    void createsAccountFromVerifiedEmailAndThenAllowsSeparateLogin(String password) throws Exception {
        var csrf = csrf();
        var grant = issueAndVerify(csrf);

        var response = send(signup(grant, password, csrf));
        assertThat(response.statusCode()).isEqualTo(201);
        assertResponseHeaders(response);
        var body = json.readTree(response.body());
        var data = body.get("data");
        assertThat(body.size()).isEqualTo(1);
        assertThat(data.size()).isEqualTo(1);
        var accountId = UUID.fromString(data.get("accountId").asText());
        assertThat(response.headers().firstValue("Location"))
                .contains("/api/v1/accounts/" + accountId);
        var stored = jdbc.queryForMap("SELECT email, password_hash, session_version FROM accounts WHERE id=?", accountId);
        assertThat(stored).containsEntry("email", EMAIL).containsEntry("session_version", 0L);
        String storedHash = (String) stored.get("password_hash");
        assertThat(storedHash).startsWith("{pbkdf2-sha256-v1}");
        assertThat(passwords.matches(password, storedHash)).isTrue();
        assertThat(consumed(grant)).isTrue();
        assertThat(response.headers().allValues("Set-Cookie")).noneMatch(cookie -> cookie.startsWith("JSESSIONID="));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM session_security", Long.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM spring_session", Long.class)).isZero();
        assertError(send(HttpRequest.newBuilder(uri("/api/v1/auth/me")).GET().build()), 401, "AUTHENTICATION_REQUIRED");

        var wrongPassword = encode(Map.of("email", EMAIL, "password", "wrong-synthetic-password"));
        assertError(send(post("/api/v1/auth/login", wrongPassword, csrf())), 401, "LOGIN_FAILED");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM session_security", Long.class)).isZero();
        var login = send(post("/api/v1/auth/login", encode(Map.of("email", EMAIL, "password", password)), csrf()));
        assertThat(login.statusCode()).isEqualTo(200);
        assertThat(json.readTree(login.body()).get("data").get("accountId").asText()).isEqualTo(accountId.toString());
        assertThat(login.headers().allValues("Set-Cookie")).anyMatch(cookie -> cookie.startsWith("JSESSIONID="));
    }

    static Stream<String> creationBoundaryPasswords() {
        return Stream.of(" 한🙂" + "a".repeat(11) + " ", "🙂".repeat(128));
    }

    @Test
    void rejectsInvalidJsonOrPasswordBeforeConsumingToken() throws Exception {
        var grant = seedGrant(EMAIL, null);
        var csrf = csrf();
        var valid = credentials(grant.token(), PASSWORD);
        var extra = valid.substring(0, valid.length() - 1);
        var bad = Map.ofEntries(
                Map.entry("missing token", encode(Map.of("password", PASSWORD))),
                Map.entry("null token", "{\"verificationToken\":null,\"password\":\"" + PASSWORD + "\"}"),
                Map.entry("number password", encode(Map.of("verificationToken", grant.token(), "password", 123456))),
                Map.entry("short password", credentials(grant.token(), "🙂".repeat(14))),
                Map.entry("long password", credentials(grant.token(), "🙂".repeat(129))),
                Map.entry("unpaired surrogate", credentials(grant.token(), PASSWORD).replace(PASSWORD, PASSWORD + "\\uD800")),
                Map.entry("token trailing newline", credentials(grant.token() + "\n", PASSWORD)),
                Map.entry("email override", extra + ",\"email\":\"elsewhere@example.invalid\"}"),
                Map.entry("unknown consent field", extra + ",\"consent\":true}"),
                Map.entry("duplicate field", extra + ",\"password\":\"" + PASSWORD + "\"}"),
                Map.entry("trailing JSON", valid + "{}"));
        for (var entry : bad.entrySet()) {
            var response = send(post(PATH, entry.getValue(), csrf));
            assertThat(response.statusCode()).as(entry.getKey()).isEqualTo(400);
            assertError(response, 400, "VALIDATION_FAILED");
        }
        assertThat(consumed(grant)).isFalse();
        assertThat(accountCount(EMAIL)).isZero();
    }

    @Test
    void transportValidationPrecedesCsrfAndExactly16KiBIsAccepted() throws Exception {
        var grant = seedGrant(EMAIL, null);
        var csrf = csrf();
        var body = credentials(grant.token(), PASSWORD);
        var max = body + " ".repeat(16384 - body.length());
        assertError(send(request(PATH, "application/json", max + " ").build()), 413, "REQUEST_TOO_LARGE");
        assertError(send(request(PATH, "text/plain", body).build()), 415, "UNSUPPORTED_MEDIA_TYPE");
        assertError(send(request(PATH, "application/json", body).build()), 403, "CSRF_INVALID");
        assertThat(consumed(grant)).isFalse();
        assertThat(send(post(PATH, max, csrf)).statusCode()).isEqualTo(201);
    }

    @Test
    void exhaustedGlobalBudgetRejectsSignupWithoutConsumingGrant() throws Exception {
        var grant = seedGrant(EMAIL, null);
        var csrf = csrf();
        assertThat(jdbc.update("UPDATE auth_rate_limits SET attempt_count=300 WHERE action='API_IP'")).isEqualTo(1);
        var response = send(signup(grant, PASSWORD, csrf));

        assertError(response, 429, "RATE_LIMITED");
        long waitSeconds = Long.parseLong(response.headers().firstValue("Retry-After").orElseThrow());
        assertThat(waitSeconds).isBetween(1L, 60L);
        assertThat(consumed(grant)).isFalse();
        assertThat(accountCount(EMAIL)).isZero();
    }

    @Test
    void rejectsWrongPurposeAndInvalidExpiredOrRevokedProofs() throws Exception {
        var csrf = csrf();
        var otherEmail = "reset@example.invalid";
        var accountId = UUID.randomUUID();
        jdbc.update("INSERT INTO accounts(id,email) VALUES (?,?)", accountId, otherEmail);
        var wrongPurpose = seedGrant(otherEmail, accountId);
        assertError(send(signup(wrongPurpose, PASSWORD, csrf)), 400, "VERIFICATION_INVALID");
        assertThat(consumed(wrongPurpose)).isFalse();

        var expired = seedGrant(EMAIL, null);
        jdbc.update("UPDATE email_verifications SET token_expires_at=clock_timestamp() WHERE id=?", expired.id());
        var revoked = seedGrant(EMAIL, null);
        jdbc.update("UPDATE email_verifications SET invalidated_at=clock_timestamp() WHERE id=?", revoked.id());
        for (var grant : List.of(expired, revoked)) {
            assertError(send(signup(grant, PASSWORD, csrf)), 400, "VERIFICATION_INVALID");
            assertThat(consumed(grant)).isFalse();
        }
        var active = seedGrant(EMAIL, null);
        var wrongHash = active.id() + "." + "B".repeat(43);
        assertError(send(post(PATH, credentials(wrongHash, PASSWORD), csrf)), 400, "VERIFICATION_INVALID");
        assertThat(consumed(active)).isFalse();
        assertThat(accountCount(EMAIL)).isZero();
    }

    @Test
    void competingRequestsUsingTheSameGrantCreateExactlyOneAccount() throws Exception {
        var grant = seedGrant(EMAIL, null);
        var csrf = csrf();
        var first = client.sendAsync(signup(grant, PASSWORD, csrf), HttpResponse.BodyHandlers.ofString());
        var second = client.sendAsync(signup(grant, PASSWORD, csrf), HttpResponse.BodyHandlers.ofString());
        var responses = List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS));
        assertThat(responses).extracting(HttpResponse::statusCode).containsExactlyInAnyOrder(201, 400);
        assertError(responses.stream().filter(response -> response.statusCode() == 400).findFirst().orElseThrow(),
                400, "VERIFICATION_INVALID");
        assertThat(consumed(grant)).isTrue();
        assertThat(accountCount(EMAIL)).isEqualTo(1L);
    }

    @Test
    void competingGrantsForOneEmailConsumeOnlyTheWinningGrant() throws Exception {
        // 독립된 증명 행이 같은 이메일을 가리키도록 구성해 계정 UNIQUE 경합을 따로 검증한다.
        var firstGrant = seedGrant(EMAIL, null);
        var secondGrant = seedGrant(EMAIL, null);
        var csrf = csrf();
        var first = client.sendAsync(signup(firstGrant, PASSWORD, csrf), HttpResponse.BodyHandlers.ofString());
        var second = client.sendAsync(signup(secondGrant, PASSWORD, csrf), HttpResponse.BodyHandlers.ofString());
        var firstResponse = first.get(10, TimeUnit.SECONDS);
        var secondResponse = second.get(10, TimeUnit.SECONDS);
        assertThat(List.of(firstResponse.statusCode(), secondResponse.statusCode())).containsExactlyInAnyOrder(201, 409);
        assertError(firstResponse.statusCode() == 409 ? firstResponse : secondResponse, 409, "ACCOUNT_ALREADY_EXISTS");
        assertThat(consumed(firstGrant)).isEqualTo(firstResponse.statusCode() == 201);
        assertThat(consumed(secondGrant)).isEqualTo(secondResponse.statusCode() == 201);
        assertThat(accountCount(EMAIL)).isEqualTo(1L);
    }

    @Test
    void existingAccountIsPreservedAndDuplicateRollsBackGrantConsumption() throws Exception {
        var grant = seedGrant(EMAIL, null);
        var existingId = UUID.randomUUID();
        jdbc.update("INSERT INTO accounts(id,email,session_version) VALUES (?,?,3)", existingId, EMAIL);

        assertError(send(signup(grant, PASSWORD, csrf())), 409, "ACCOUNT_ALREADY_EXISTS");
        assertThat(consumed(grant)).isFalse();
        assertThat(jdbc.queryForMap("SELECT id,password_hash,session_version FROM accounts WHERE email=?", EMAIL))
                .containsEntry("id", existingId).containsEntry("password_hash", null).containsEntry("session_version", 3L);
    }

    @Test
    void failedAccountWriteRollsBackConsumedGrantAndAllowsRetry() throws Exception {
        var grant = seedGrant(EMAIL, null);
        var csrf = csrf();
        // 임시 제약으로 계정 쓰기만 실패시켜 실제 DB 트랜잭션의 롤백을 확인한다.
        jdbc.execute("ALTER TABLE accounts ADD CONSTRAINT signup_fixture_failure CHECK (false)");
        try {
            assertError(send(signup(grant, PASSWORD, csrf)), 503, "AUTH_STATE_UNAVAILABLE");
            assertThat(consumed(grant)).isFalse();
            assertThat(accountCount(EMAIL)).isZero();
        }
        finally {
            jdbc.execute("ALTER TABLE accounts DROP CONSTRAINT signup_fixture_failure");
        }
        assertThat(send(signup(grant, PASSWORD, csrf)).statusCode()).isEqualTo(201);
        assertThat(consumed(grant)).isTrue();
        assertThat(accountCount(EMAIL)).isEqualTo(1L);
    }

    private Grant issueAndVerify(String csrf) throws Exception {
        var issue = send(post("/api/v1/auth/email-verifications",
                encode(Map.of("purpose", "SIGNUP", "email", " \u00a0Pa.rent+Tag@EXAMPLE.INVALID ")), csrf));
        assertThat(issue.statusCode()).isEqualTo(200);
        var id = UUID.fromString(json.readTree(issue.body()).get("data").get("challengeId").asText());
        var code = forClass(String.class);
        verify(sender).send(eq(EMAIL), code.capture(), eq(true));
        var verified = send(post("/api/v1/auth/email-verifications/verify",
                encode(Map.of("challengeId", id, "code", code.getValue())), csrf));
        assertThat(verified.statusCode()).isEqualTo(200);
        return new Grant(id, json.readTree(verified.body()).get("data").get("verificationToken").asText());
    }

    private Grant seedGrant(String email, UUID resetAccountId) {
        var id = UUID.randomUUID();
        String purpose = resetAccountId == null ? "SIGNUP" : "RESET_PASSWORD";
        Long resetVersion = resetAccountId == null ? null : 0L;
        jdbc.update("""
                INSERT INTO email_verifications(id,email,purpose,eligible,code_hash,created_at,expires_at,
                                               verified_at,token_hash,token_expires_at,reset_account_id,reset_session_version)
                VALUES (?,?,?,true,sha256('fixture'::bytea),clock_timestamp()-interval '1 minute',
                        clock_timestamp()+interval '9 minutes',clock_timestamp()-interval '10 seconds',
                        sha256(convert_to(?,'UTF8')),clock_timestamp()+interval '590 seconds',?,?)
                """, id, email, purpose, FIXTURE_SECRET, resetAccountId, resetVersion);
        return new Grant(id, id + "." + FIXTURE_SECRET);
    }

    private boolean consumed(Grant grant) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT token_consumed_at IS NOT NULL FROM email_verifications WHERE id=?", Boolean.class, grant.id()));
    }

    private long accountCount(String email) {
        return jdbc.queryForObject("SELECT count(*) FROM accounts WHERE email=?", Long.class, email);
    }

    private String csrf() throws Exception {
        var response = send(HttpRequest.newBuilder(uri("/api/v1/auth/csrf")).GET().build());
        assertThat(response.statusCode()).isEqualTo(200);
        return json.readTree(response.body()).get("token").asText();
    }

    private HttpRequest signup(Grant grant, String password, String csrf) {
        return post(PATH, credentials(grant.token(), password), csrf);
    }

    private String credentials(String token, String password) {
        return encode(Map.of("verificationToken", token, "password", password));
    }

    private String encode(Map<String, ?> fields) {
        return json.writeValueAsString(fields);
    }

    private HttpRequest post(String path, String body, String csrf) {
        return request(path, "application/json", body).header("Cookie", "XSRF-TOKEN=" + csrf)
                .header("X-XSRF-TOKEN", csrf).build();
    }

    private HttpRequest.Builder request(String path, String type, String body) {
        return HttpRequest.newBuilder(uri(path)).header("Content-Type", type)
                .POST(HttpRequest.BodyPublishers.ofString(body));
    }

    private HttpResponse<String> send(HttpRequest request) throws Exception {
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(String path) {
        return URI.create("http://127.0.0.1:" + port + path);
    }

    private void assertError(HttpResponse<String> response, int httpStatus, String code) {
        assertThat(response.statusCode()).isEqualTo(httpStatus);
        assertResponseHeaders(response);
        var error = json.readTree(response.body()).get("error");
        assertThat(error.get("code").asText()).isEqualTo(code);
        assertThat(response.headers().firstValue("X-Request-ID")).contains(error.get("requestId").asText());
        assertThat(response.body()).doesNotContain(PASSWORD, FIXTURE_SECRET);
    }

    private void assertResponseHeaders(HttpResponse<String> response) {
        assertThat(response.headers().firstValue("Cache-Control").orElseThrow()).contains("no-store");
        assertThat(response.headers().firstValue("X-Request-ID")).isPresent();
    }

    private record Grant(UUID id, String token) { }
}
