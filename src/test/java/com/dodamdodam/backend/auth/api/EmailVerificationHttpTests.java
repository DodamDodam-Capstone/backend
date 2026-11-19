package com.dodamdodam.backend.auth.api;

import com.dodamdodam.backend.TestcontainersConfiguration;
import com.dodamdodam.backend.auth.application.EmailVerificationService;
import com.dodamdodam.backend.auth.domain.AuthRateLimitKey.Action;
import com.dodamdodam.backend.auth.dto.IssueEmailVerificationRequest.Purpose;
import com.dodamdodam.backend.auth.exception.EmailVerificationException;
import com.dodamdodam.backend.auth.session.AuthenticatedSession;
import com.dodamdodam.backend.auth.infrastructure.*;
import com.dodamdodam.backend.auth.session.AuthStateUnavailableException;
import com.dodamdodam.backend.global.web.ClientAddressResolver;
import jakarta.servlet.http.HttpServletRequestWrapper;
import java.net.*;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.session.Session;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.*;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT, properties={"spring.session.jdbc.cleanup-cron=-"})
@Import({TestcontainersConfiguration.class, EmailVerificationHttpTests.ResponseGate.class})
@DirtiesContext(classMode=DirtiesContext.ClassMode.AFTER_CLASS)
@Timeout(30)
class EmailVerificationHttpTests {
    static final String PATH="/api/v1/auth/email-verifications";
    static final String VERIFY_PATH = PATH + "/verify";
    @LocalServerPort int port;
    @Autowired JdbcTemplate jdbc;
    @Autowired EmailVerificationService verification;
    @Autowired PlatformTransactionManager transactions;
    @Autowired JsonMapper mapper;
    @Autowired JdbcIndexedSessionRepository sessions;
    @Autowired CookieSerializer cookies;
    @MockitoBean SmtpEmailVerificationSender sender;
    @MockitoSpyBean EmailVerificationKeys keys;
    @MockitoSpyBean EmailVerificationRepository challenges;
    @MockitoSpyBean EmailVerificationAccountRepository accounts;
    final HttpClient client=HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();
    Fixture fixture;

    @BeforeEach @AfterEach void clean() {
        var armed=ResponseGate.armed.getAndSet(null);
        if(armed!=null) { armed.release.countDown(); }
        jdbc.update("DELETE FROM email_verifications"); jdbc.update("DELETE FROM auth_rate_limits");
        jdbc.update("DELETE FROM guardian_pins"); jdbc.update("DELETE FROM session_security"); jdbc.update("DELETE FROM accounts");
    }

    @ParameterizedTest @CsvSource({"SIGNUP,false", "SIGNUP,true", "RESET_PASSWORD,false", "RESET_PASSWORD,true"})
    void publicNormalAndDecoyExposeOnlySameTwoFields(String purpose,boolean existing) throws Exception {
        if(existing) { jdbc.update("INSERT INTO accounts(id,email) VALUES(?,'person@example.invalid')",UUID.randomUUID()); }
        var response=send(post("{\"purpose\":\""+purpose+"\",\"email\":\"person@example.invalid\"}",csrf("")));
        assertThat(response.statusCode()).isEqualTo(200);
        var body=mapper.readTree(response.body());
        assertThat(body.size()).isEqualTo(1); assertThat(body.get("data").size()).isEqualTo(2);
        assertThat(body.get("data").get("challengeId").asText()).hasSize(36);
        assertThat(body.get("data").get("expiresAt").asText()).matches(".*\\.[0-9]{6}\\+09:00");
        assertThat(response.body()).doesNotContain("eligible","code","email","account");
    }

    @ParameterizedTest @ValueSource(strings={"PIN_SETUP","PIN_RESET"})
    void pinEndpointsUseRealNativeSessionAndOmittedEmail(String purpose) throws Exception {
        fixture=fixture(purpose.equals("PIN_RESET"));
        var response=send(post("{\"purpose\":\""+purpose+"\"}",csrf(fixture.cookie)));
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(jdbc.queryForObject("SELECT setup_account_id FROM email_verifications",UUID.class)).isEqualTo(fixture.account);
    }

    @Test void transmissionCsrfConditionalAuthenticationAndStructureKeepPriority() throws Exception {
        assertError(send(HttpRequest.newBuilder(uri(PATH)).POST(HttpRequest.BodyPublishers.ofString("x")).header("Content-Type","text/plain").build()),415,"UNSUPPORTED_MEDIA_TYPE");
        assertError(send(HttpRequest.newBuilder(uri(PATH)).POST(HttpRequest.BodyPublishers.ofString("x".repeat(16385))).header("Content-Type","application/json").build()),413,"REQUEST_TOO_LARGE");
        assertError(send(HttpRequest.newBuilder(uri(PATH)).POST(HttpRequest.BodyPublishers.ofString("{}")).header("Content-Type","application/json").build()),403,"CSRF_INVALID");
        assertError(send(post("{\"purpose\":\"PIN_SETUP\",\"email\":1}",csrf(""))),401,"AUTHENTICATION_REQUIRED");
        assertError(send(post("{\"purpose\":\"SIGNUP\",\"email\":\"person@example.invalid\",\"extra\":1}",csrf(""))),400,"VALIDATION_FAILED");
        assertError(send(post("{\"purpose\":\"SIGNUP\",\"purpose\":\"RESET_PASSWORD\",\"email\":\"person@example.invalid\"}",csrf(""))),400,"VALIDATION_FAILED");
        assertError(send(post("{\"purpose\":\"SIGNUP\",",csrf(""))),400,"VALIDATION_FAILED");
        assertError(send(post("{\"purpose\":\"SIGNUP\",\"email\":\"person@example.invalid\"} {}",csrf(""))),400,"VALIDATION_FAILED");
    }

    @Test void smtpFailureAndCooldownKeepClosedErrorEnvelope() throws Exception {
        var csrf=csrf("");
        String body="{\"purpose\":\"SIGNUP\",\"email\":\"person@example.invalid\"}";
        doThrow(new RuntimeException("synthetic SMTP private detail")).when(sender).send(anyString(),anyString(),anyBoolean());
        assertError(send(post(body,csrf)),503,"EMAIL_DELIVERY_UNAVAILABLE");
        var rate=send(post(body,csrf));
        assertError(rate,429,"RATE_LIMITED");
        assertThat(Long.parseLong(rate.headers().firstValue("Retry-After").orElseThrow())).isBetween(1L,60L);
    }

    @Test void frameworkArgumentLoggingDoesNotExposeEmail() throws Exception {
        var logger=(ch.qos.logback.classic.Logger)org.slf4j.LoggerFactory.getLogger(
                org.springframework.web.servlet.mvc.method.annotation.RequestResponseBodyMethodProcessor.class);
        var previous=logger.getLevel();
        var captured=new ch.qos.logback.core.read.ListAppender<ch.qos.logback.classic.spi.ILoggingEvent>();
        captured.start(); logger.addAppender(captured); logger.setLevel(ch.qos.logback.classic.Level.DEBUG);
        try {
            var response=send(post("{\"purpose\":\"SIGNUP\",\"email\":\"private@example.invalid\"}",csrf("")));
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(captured.list).noneMatch(event->event.getFormattedMessage().contains("private@example.invalid"));
        }
        finally { logger.detachAppender(captured); captured.stop(); logger.setLevel(previous); }
    }

    @Test void apiAndOauthShareBudgetAndForgedForwardedHeadersDoNotResetIt() throws Exception {
        send(HttpRequest.newBuilder(uri("/api/v1/auth/csrf")).header("X-Forwarded-For","192.0.2.1").GET().build());
        jdbc.update("UPDATE auth_rate_limits SET attempt_count=299 WHERE action='API_IP'");
        assertThat(send(HttpRequest.newBuilder(uri("/oauth2/authorization/google")).GET().build()).statusCode()).isEqualTo(302);
        var denied=send(HttpRequest.newBuilder(uri("/api/v1/auth/csrf")).header("Forwarded","for=198.51.100.22").header("X-Forwarded-For","203.0.113.7").GET().build());
        assertError(denied,429,"RATE_LIMITED");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM auth_rate_limits WHERE action='API_IP'",Long.class)).isEqualTo(1);
        assertThat(send(HttpRequest.newBuilder(uri("/actuator/health")).GET().build()).statusCode()).isEqualTo(200);
        var preflight=send(HttpRequest.newBuilder(uri(PATH)).method("OPTIONS",HttpRequest.BodyPublishers.noBody())
                .header("Origin","http://localhost:5173").header("Access-Control-Request-Method","POST").build());
        assertThat(preflight.statusCode()).isEqualTo(200);
        assertThat(jdbc.queryForObject("SELECT attempt_count FROM auth_rate_limits WHERE action='API_IP'",Long.class)).isEqualTo(300);
    }

    @Test void unconfiguredGlobalKeyOrBudgetDatabaseFailsClosedButHealthWorks() throws Exception {
        doThrow(new AuthStateUnavailableException()).when(keys).rateHasher();
        assertError(send(HttpRequest.newBuilder(uri("/api/v1/auth/csrf")).GET().build()),503,"AUTH_STATE_UNAVAILABLE");
        assertThat(send(HttpRequest.newBuilder(uri("/actuator/health")).GET().build()).statusCode()).isEqualTo(200);
        reset(keys);
        jdbc.execute("ALTER TABLE auth_rate_limits RENAME TO unavailable_auth_rate_limits");
        try { assertError(send(HttpRequest.newBuilder(uri("/api/v1/auth/csrf")).GET().build()),503,"AUTH_STATE_UNAVAILABLE"); }
        finally { jdbc.execute("ALTER TABLE unavailable_auth_rate_limits RENAME TO auth_rate_limits"); }
    }

    @ParameterizedTest @CsvSource({"LOCK,403,PIN_SETUP_AUTHORIZATION_REQUIRED", "HTTP_LOGOUT,401,AUTHENTICATION_REQUIRED",
            "PIN_VERSION,403,PIN_RESET_AUTHORIZATION_REQUIRED", "EMAIL,403,PIN_SETUP_AUTHORIZATION_REQUIRED", "DB_FAILURE,503,AUTH_STATE_UNAVAILABLE"})
    void authorityChangeJustBeforeNativeBodyWriteDoesNotExposeSuccess(String change,int status,String code) throws Exception {
        fixture=fixture(change.equals("PIN_VERSION"));
        var csrf=csrf(fixture.cookie);
        var gate=new Gate(); ResponseGate.armed.set(gate);
        var response=client.sendAsync(post("{\"purpose\":\""+(change.equals("PIN_VERSION")?"PIN_RESET":"PIN_SETUP")+"\"}",csrf),java.net.http.HttpResponse.BodyHandlers.ofString());
        boolean renamed=false;
        try {
            assertThat(gate.entered.await(8,TimeUnit.SECONDS)).isTrue();
            switch(change) {
                case "LOCK" -> jdbc.update("UPDATE session_security SET setup_generation=setup_generation+1 WHERE id=?",fixture.context);
                case "HTTP_LOGOUT" -> assertThat(send(HttpRequest.newBuilder(uri("/api/v1/auth/logout")).header("Cookie",csrf.cookie).header("X-XSRF-TOKEN",csrf.token).POST(HttpRequest.BodyPublishers.noBody()).build()).statusCode()).isEqualTo(204);
                case "PIN_VERSION" -> jdbc.update("UPDATE guardian_pins SET pin_version=2 WHERE account_id=?",fixture.account);
                case "EMAIL" -> jdbc.update("UPDATE accounts SET email='changed@example.invalid' WHERE id=?",fixture.account);
                case "DB_FAILURE" -> { jdbc.execute("ALTER TABLE guardian_pins RENAME TO unavailable_guardian_pins"); renamed=true; }
                default -> throw new AssertionError(change);
            }
            gate.release.countDown();
            var denied=response.get(10,TimeUnit.SECONDS);
            assertError(denied,status,code);
            assertThat(denied.body()).doesNotContain("challengeId","expiresAt","data");
            assertThat(denied.headers().allValues("Set-Cookie")).noneMatch(value->value.startsWith("JSESSIONID=")&&!value.contains("Max-Age=0"));
        }
        finally { gate.release.countDown(); if(renamed) { jdbc.execute("ALTER TABLE unavailable_guardian_pins RENAME TO guardian_pins"); } }
    }

    @Test void addressResolverUnwrapsForwardingWrappers() {
        var raw=new MockHttpServletRequest(); raw.setRemoteAddr("192.0.2.10");
        var wrapped=new HttpServletRequestWrapper(raw) { public String getRemoteAddr() { return "198.51.100.1"; } };
        assertThat(ClientAddressResolver.resolve(wrapped)).isEqualTo(InetAddress.ofLiteral("192.0.2.10"));
    }


    @Test void parallelCodeChecksIssueExactlyOneToken() throws Exception {
        var id = publicChallenge(true, false);
        var request = verifyPost(id, "012345", csrf(""));
        var first = client.sendAsync(request, java.net.http.HttpResponse.BodyHandlers.ofString());
        var second = client.sendAsync(request, java.net.http.HttpResponse.BodyHandlers.ofString());
        var responses = List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS));
        assertThat(responses).extracting(java.net.http.HttpResponse::statusCode).containsExactlyInAnyOrder(200, 400);

        var success = responses.stream().filter(response -> response.statusCode() == 200).findFirst().orElseThrow();
        var data = mapper.readTree(success.body()).get("data");
        String token = data.get("verificationToken").asText();
        assertThat(data.size()).isEqualTo(2);
        assertThat(token).matches(id + "\\.[A-Za-z0-9_-]{43}");
        assertThat(data.get("tokenExpiresAt").asText()).matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\+09:00");
        byte[] expectedHash = java.security.MessageDigest.getInstance("SHA-256")
                .digest(token.substring(37).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        assertThat(jdbc.queryForObject("SELECT token_hash FROM email_verifications WHERE id=?", byte[].class, id))
                .containsExactly(expectedHash);
        assertThat(jdbc.queryForObject("""
                SELECT token_expires_at - verified_at = interval '600 seconds' AND token_consumed_at IS NULL
                FROM email_verifications WHERE id=?
                """, Boolean.class, id)).isTrue();
        assertError(responses.stream().filter(response -> response.statusCode() == 400).findFirst().orElseThrow(),
                400, "VERIFICATION_INVALID");
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void fifthWrongCodeCommitsInvalidationBefore429(boolean eligible) throws Exception {
        var id = publicChallenge(eligible, false);
        var csrf = csrf("");
        for (int attempt = 1; attempt <= 4; attempt++) {
            assertError(send(verifyPost(id, "012346", csrf)), 400, "VERIFICATION_INVALID");
            assertThat(jdbc.queryForObject("SELECT attempt_count FROM email_verifications WHERE id=?", Integer.class, id))
                    .isEqualTo(attempt);
        }
        var fifth = send(verifyPost(id, "012346", csrf));
        assertError(fifth, 429, "RATE_LIMITED");
        assertThat(Long.parseLong(fifth.headers().firstValue("Retry-After").orElseThrow())).isBetween(1L, 60L);
        assertError(send(verifyPost(id, "012345", csrf)), 400, "VERIFICATION_INVALID");
        assertThat(jdbc.queryForObject("""
                SELECT attempt_count=5 AND invalidated_at IS NOT NULL AND token_hash IS NULL
                FROM email_verifications WHERE id=?
                """, Boolean.class, id)).isTrue();
    }


    @ParameterizedTest
    @ValueSource(strings = {"PIN_SETUP", "PIN_RESET"})
    void exhaustedPinChallengeRequiresNewAuthorization(String purpose) throws Exception {
        var owner = fixture(purpose.equals("PIN_RESET"));
        var id = pinChallenge(owner, purpose);
        var csrf = csrf(owner.cookie());
        // 대상 예산의 마지막 요청과 다섯 번째 오답이 겹치도록 준비한다.
        var key = keys.rateHasher().emailVerifySubject(Purpose.valueOf(purpose), owner.account() + "@example.invalid", owner.account());
        jdbc.update("""
                INSERT INTO auth_rate_limits(action,key_hash,window_started_at,attempt_count,expires_at)
                VALUES(?,?,clock_timestamp(),?,clock_timestamp()+interval '900 seconds')
                """, key.action().name(), key.keyHash(), key.action().capacity() - 1);
        jdbc.update("UPDATE email_verifications SET attempt_count=4 WHERE id=?", id);
        assertError(send(verifyPost(id, "012346", csrf)), 429, "RATE_LIMITED");
        assertError(send(verifyPost(id, "012345", csrf)), 403, purpose + "_AUTHORIZATION_REQUIRED");
    }

    @ParameterizedTest
    @CsvSource({"PIN_SETUP,1", "PIN_SETUP,10", "PIN_RESET,1", "PIN_RESET,10"})
    void stalePinAuthorityLeavesSubjectBudgetUntouched(Purpose purpose, int reserved) throws Exception {
        var owner = fixture(purpose == Purpose.PIN_RESET);
        var id = pinChallenge(owner, purpose.name());
        var csrf = csrf(owner.cookie());
        var key = keys.rateHasher().emailVerifySubject(purpose, owner.account() + "@example.invalid", owner.account());
        jdbc.update("""
                INSERT INTO auth_rate_limits(action,key_hash,window_started_at,attempt_count,expires_at)
                VALUES(?,?,clock_timestamp(),?,clock_timestamp()+interval '900 seconds')
                """, key.action().name(), key.keyHash(), reserved);
        if (purpose == Purpose.PIN_RESET) {
            jdbc.update("UPDATE guardian_pins SET pin_version=pin_version+1 WHERE account_id=?", owner.account());
        } else {
            jdbc.update("UPDATE session_security SET setup_generation=setup_generation+1 WHERE id=?", owner.context());
        }
        assertError(send(verifyPost(id, "012345", csrf)), 403, purpose + "_AUTHORIZATION_REQUIRED");
        assertThat(jdbc.queryForObject("SELECT attempt_count FROM auth_rate_limits WHERE action=? AND key_hash=?",
                Integer.class, key.action().name(), key.keyHash())).isEqualTo(reserved);
    }

    @Test void exhaustedVerificationIpStopsBeforeChallengeLookup() throws Exception {
        var id = publicChallenge(true, false);
        var request = verifyPost(id, "012346", csrf(""));
        assertError(send(request), 400, "VERIFICATION_INVALID");
        // IP 단계에서 거부되면 대상 창이 더 길어도 challenge나 대상 키를 조회하지 않는다.
        jdbc.update("""
                UPDATE auth_rate_limits SET attempt_count=?, window_started_at=clock_timestamp()-interval '870 seconds'
                WHERE action='EMAIL_VERIFY_IP'
                """, Action.EMAIL_VERIFY_IP.capacity());
        jdbc.update("UPDATE auth_rate_limits SET blocked_until=clock_timestamp()+interval '800 seconds' WHERE action='EMAIL_VERIFY_SUBJECT'");
        clearInvocations(challenges);
        var rejected = send(request);
        assertError(rejected, 429, "RATE_LIMITED");
        assertThat(Long.parseLong(rejected.headers().firstValue("Retry-After").orElseThrow())).isBetween(1L, 30L);
        verify(challenges, never()).findChallenge(any());
        assertThat(jdbc.queryForObject("SELECT attempt_count FROM auth_rate_limits WHERE action='EMAIL_VERIFY_SUBJECT'",
                Long.class)).isEqualTo(1);
    }

    @Test void fifthWrongCodeReportsTheLongerSubjectWindow() throws Exception {
        var id = publicChallenge(true, false);
        var key = keys.rateHasher().emailVerifySubject(Purpose.SIGNUP, "verify@example.invalid", null);
        jdbc.update("""
                INSERT INTO auth_rate_limits(action,key_hash,window_started_at,attempt_count,expires_at)
                VALUES(?,?,clock_timestamp(),5,clock_timestamp()+interval '900 seconds')
                """, key.action().name(), key.keyHash());
        var csrf = csrf("");
        for (int attempt = 1; attempt <= 4; attempt++) {
            assertError(send(verifyPost(id, "012346", csrf)), 400, "VERIFICATION_INVALID");
        }
        var rejected = send(verifyPost(id, "012346", csrf));
        assertError(rejected, 429, "RATE_LIMITED");
        assertThat(Long.parseLong(rejected.headers().firstValue("Retry-After").orElseThrow())).isBetween(800L, 900L);
        assertError(send(verifyPost(id, "012345", csrf)), 400, "VERIFICATION_INVALID");
        assertThat(jdbc.queryForObject("SELECT attempt_count FROM auth_rate_limits WHERE action='EMAIL_VERIFY_SUBJECT'",
                Long.class)).isEqualTo(10);
    }

    @ParameterizedTest
    @CsvSource({"API_IP, EMAIL_VERIFY_IP", "EMAIL_VERIFY_IP, EMAIL_VERIFY_SUBJECT"})
    void rejectedVerificationReportsEarlierBudgetWithoutReservingItTwice(Action earlier, Action rejected) throws Exception {
        var id = publicChallenge(true, false);
        var request = verifyPost(id, "012346", csrf(""));
        assertError(send(request), 400, "VERIFICATION_INVALID");
        jdbc.update("UPDATE auth_rate_limits SET attempt_count=? WHERE action=?", earlier.capacity() - 1, earlier.name());
        jdbc.update("""
                UPDATE auth_rate_limits SET blocked_until=clock_timestamp()+interval '10 seconds' WHERE action=?
                """, rejected.name());

        var response = send(request);
        assertError(response, 429, "RATE_LIMITED");
        assertThat(Long.parseLong(response.headers().firstValue("Retry-After").orElseThrow()))
                .isBetween(earlier.windowSeconds() - 20, earlier.windowSeconds());
        assertThat(jdbc.queryForObject("SELECT attempt_count FROM auth_rate_limits WHERE action=?", Long.class,
                earlier.name())).isEqualTo(earlier.capacity());
        assertThat(jdbc.queryForObject("SELECT attempt_count FROM email_verifications WHERE id=?", Integer.class, id))
                .isEqualTo(1);
    }

    @Test void pinChallengeRequiresLoginBeforeCheckingCodeOrExtraFields() throws Exception {
        var owner = fixture(false);
        var id = pinChallenge(owner, "PIN_SETUP");
        String invalid = "{\"challengeId\":\"" + id + "\",\"code\":123456,\"extra\":0}";
        assertError(send(post(VERIFY_PATH, invalid, csrf(""))), 401, "AUTHENTICATION_REQUIRED");
        assertError(send(post(VERIFY_PATH, invalid, csrf(owner.cookie()))), 400, "VALIDATION_FAILED");
    }

    @ParameterizedTest
    @ValueSource(strings = {"123456", "\"012345\\n\"", "\"０１２３４５\"", "\"12345\"", "null"})
    void verifyRequiresSixAsciiDigitsInAJsonString(String value) throws Exception {
        var id = publicChallenge(true, false);
        var response = send(post(VERIFY_PATH, "{\"challengeId\":\"" + id + "\",\"code\":" + value + "}", csrf("")));
        assertValidationField(response, "code");
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "[]|body", "{}|challengeId", "{\"challengeId\":\"invalid\"}|challengeId",
            "{\"challengeId\":\"1-1-1-1-1\"}|challengeId",
            "{\"challengeId\":\"00000000-0000-0000-0000-000000000000\",\"extra\":true}|body"
    })
    void verificationErrorsIdentifyTheInvalidField(String body, String field) throws Exception {
        assertValidationField(send(post(VERIFY_PATH, body, csrf(""))), field);
    }

    private void assertValidationField(java.net.http.HttpResponse<String> response, String field) throws Exception {
        assertError(response, 400, "VALIDATION_FAILED");
        assertThat(mapper.readTree(response.body()).at("/error/fields/0/field").asText()).isEqualTo(field);
    }

    @Test void verifyUsesExistingTransportAndCsrfChecks() throws Exception {
        assertError(send(HttpRequest.newBuilder(uri(VERIFY_PATH)).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(" ".repeat(16385))).build()), 413, "REQUEST_TOO_LARGE");
        assertError(send(HttpRequest.newBuilder(uri(VERIFY_PATH)).header("Content-Type", "text/plain")
                .POST(HttpRequest.BodyPublishers.ofString("{}")).build()), 415, "UNSUPPORTED_MEDIA_TYPE");
        assertError(send(HttpRequest.newBuilder(uri(VERIFY_PATH)).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{}")).build()), 403, "CSRF_INVALID");
    }

    @Test void unknownChallengeStillSpendsVerificationIpBudget() throws Exception {
        assertError(send(verifyPost(UUID.randomUUID(), "012345", csrf(""))), 400, "VERIFICATION_INVALID");
        assertThat(jdbc.queryForObject("""
                SELECT attempt_count FROM auth_rate_limits WHERE action='EMAIL_VERIFY_IP'
                """, Long.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM auth_rate_limits WHERE action='EMAIL_VERIFY_SUBJECT'
                """, Long.class)).isZero();
    }

    @Test void unacceptableResponseLeavesTheCodeAvailableForJson() throws Exception {
        var id = publicChallenge(true, false);
        var csrf = csrf("");
        var request = verifyPost(id, "012345", csrf);
        var plain = HttpRequest.newBuilder(request, (name, value) -> true).header("Accept", "text/plain").build();
        assertThat(send(plain).statusCode()).isEqualTo(406);
        assertThat(jdbc.queryForObject("""
                SELECT verified_at IS NULL AND token_hash IS NULL FROM email_verifications WHERE id=?
                """, Boolean.class, id)).isTrue();
        assertThat(issuedToken(id, csrf)).startsWith(id + ".");
    }

    @ParameterizedTest
    @CsvSource({"false,false", "true,true"})
    void decoyAndExpiredChallengesNeverIssueTokens(boolean eligible, boolean expired) throws Exception {
        var id = publicChallenge(eligible, expired);
        assertError(send(verifyPost(id, "012345", csrf(""))), 400, "VERIFICATION_INVALID");
        assertThat(jdbc.queryForObject("""
                SELECT token_hash IS NULL AND attempt_count=? FROM email_verifications WHERE id=?
                """, Boolean.class, expired ? 0 : 1, id)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"PIN_SETUP", "PIN_RESET"})
    void pinBindingErrorsTakePrecedenceOverExpiry(String purpose) throws Exception {
        boolean reset = purpose.equals("PIN_RESET");
        var owner = fixture(reset);
        var id = pinChallenge(owner, purpose);
        var ownerCsrf = csrf(owner.cookie());
        var other = fixture(reset);
        assertError(send(verifyPost(id, "012345", csrf(""))), 401, "AUTHENTICATION_REQUIRED");

        jdbc.update("""
                UPDATE email_verifications SET created_at=clock_timestamp()-interval '601 seconds',
                  expires_at=clock_timestamp()-interval '1 second' WHERE id=?
                """, id);
        assertError(send(verifyPost(id, "012345", ownerCsrf)), 400, "VERIFICATION_INVALID");
        assertError(send(verifyPost(id, "012345", csrf(other.cookie()))), 403, purpose + "_AUTHORIZATION_REQUIRED");
        if (reset) {
            jdbc.update("UPDATE guardian_pins SET pin_version=pin_version+1 WHERE account_id=?", owner.account());
        } else {
            jdbc.update("UPDATE session_security SET setup_generation=setup_generation+1 WHERE id=?", owner.context());
        }
        assertError(send(verifyPost(id, "012345", ownerCsrf)), 403, purpose + "_AUTHORIZATION_REQUIRED");
    }


    @ParameterizedTest
    @CsvSource({"GENERATION,403,PIN_RESET_AUTHORIZATION_REQUIRED", "PIN_VERSION,403,PIN_RESET_AUTHORIZATION_REQUIRED",
            "LOGOUT,401,AUTHENTICATION_REQUIRED"})
    void pinAuthorityChangeBetweenVerificationAndResponseDoesNotExposeGrant(String change, int status, String code)
            throws Exception {
        var owner = fixture(true);
        var id = pinChallenge(owner, "PIN_RESET");
        var csrf = csrf(owner.cookie());
        var request = verifyPost(id, "012345", csrf);
        var gate = new Gate();
        ResponseGate.armed.set(gate);
        var response = client.sendAsync(request, java.net.http.HttpResponse.BodyHandlers.ofString());
        try {
            assertThat(gate.entered.await(8, TimeUnit.SECONDS)).isTrue();
            switch (change) {
                case "GENERATION" -> jdbc.update(
                        "UPDATE session_security SET setup_generation=setup_generation+1 WHERE id=?", owner.context());
                case "PIN_VERSION" -> jdbc.update(
                        "UPDATE guardian_pins SET pin_version=pin_version+1 WHERE account_id=?", owner.account());
                case "LOGOUT" -> assertThat(send(HttpRequest.newBuilder(uri("/api/v1/auth/logout"))
                        .header("Cookie", csrf.cookie()).header("X-XSRF-TOKEN", csrf.token())
                        .POST(HttpRequest.BodyPublishers.noBody()).build()).statusCode()).isEqualTo(204);
                default -> throw new AssertionError(change);
            }
            gate.release.countDown();
            var denied = response.get(10, TimeUnit.SECONDS);
            assertError(denied, status, code);
            assertThat(denied.body()).doesNotContain("verificationToken");
        }
        finally {
            gate.release.countDown();
        }
    }


    @Test void tokenConsumptionRollsBackWithSignupWriteAndCommitsOnlyOnce() throws Exception {
        var id = publicChallenge(true, false);
        String token = issuedToken(id, csrf(""));
        var tx = new TransactionTemplate(transactions);
        assertThatThrownBy(() -> verification.consume(token, Purpose.SIGNUP, null))
                .isInstanceOf(IllegalTransactionStateException.class);

        tx.executeWithoutResult(status -> {
            var proof = verification.consume(token, Purpose.SIGNUP, null);
            jdbc.update("INSERT INTO accounts(id,email) VALUES(?,?)", UUID.randomUUID(), proof.email());
            status.setRollbackOnly();
        });
        assertThat(jdbc.queryForObject("SELECT count(*) FROM accounts", Long.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT token_consumed_at IS NULL FROM email_verifications WHERE id=?",
                Boolean.class, id)).isTrue();

        // 토큰이 발급된 뒤에는 번호 자체의 만료가 토큰을 만료시키지 않는다.
        jdbc.update("""
                UPDATE email_verifications SET created_at=clock_timestamp()-interval '601 seconds',
                  expires_at=clock_timestamp()-interval '1 second' WHERE id=?
                """, id);
        tx.executeWithoutResult(status -> {
            var proof = verification.consume(token, Purpose.SIGNUP, null);
            jdbc.update("INSERT INTO accounts(id,email) VALUES(?,?)", UUID.randomUUID(), proof.email());
        });
        assertThat(jdbc.queryForObject("SELECT email FROM accounts", String.class)).isEqualTo("verify@example.invalid");
        assertThat(jdbc.queryForObject("""
                SELECT token_consumed_at IS NOT NULL AND invalidated_at IS NULL FROM email_verifications WHERE id=?
                """, Boolean.class, id)).isTrue();
        assertConsumeError(token, Purpose.SIGNUP, null, 400, "VERIFICATION_INVALID");
    }

    @Test void tokenRequiresExpectedPurposeCorrectHashShapeAndExpiry() throws Exception {
        var id = publicChallenge(true, false);
        String token = issuedToken(id, csrf(""));
        assertConsumeError(token, Purpose.RESET_PASSWORD, null, 400, "VERIFICATION_INVALID");
        assertConsumeError(token + "\n", Purpose.SIGNUP, null, 400, "VALIDATION_FAILED");
        String wrongSecret = token.substring(0, 37) + (token.charAt(37) == 'A' ? 'B' : 'A') + token.substring(38);
        assertConsumeError(wrongSecret, Purpose.SIGNUP, null, 400, "VERIFICATION_INVALID");

        jdbc.update("""
                UPDATE email_verifications SET created_at=clock_timestamp()-interval '602 seconds',
                  verified_at=clock_timestamp()-interval '601 seconds',
                  token_expires_at=clock_timestamp()-interval '1 second' WHERE id=?
                """, id);
        assertConsumeError(token, Purpose.SIGNUP, null, 400, "VERIFICATION_INVALID");
    }

    @Test void pinResetTokenRejectsOtherContextAndChangedVersionEvenAfterConsumption() throws Exception {
        var owner = fixture(true);
        var id = pinChallenge(owner, "PIN_RESET");
        String token = issuedToken(id, csrf(owner.cookie()));
        var current = new AuthenticatedSession(owner.account(), owner.context());
        var anotherContextId = UUID.randomUUID();
        jdbc.update("INSERT INTO session_security(id,account_id,session_version) VALUES(?,?,0)",
                anotherContextId, owner.account());
        var other = new AuthenticatedSession(owner.account(), anotherContextId);
        assertConsumeError(token, Purpose.PIN_SETUP, current, 400, "VERIFICATION_INVALID");
        assertConsumeError(token, Purpose.PIN_RESET, other, 403, "PIN_RESET_AUTHORIZATION_REQUIRED");

        new TransactionTemplate(transactions).executeWithoutResult(status ->
                verification.consume(token, Purpose.PIN_RESET, current));
        jdbc.update("UPDATE guardian_pins SET pin_version=pin_version+1 WHERE account_id=?", owner.account());
        assertConsumeError(token, Purpose.PIN_RESET, current, 403, "PIN_RESET_AUTHORIZATION_REQUIRED");
    }


    @Test void reissueAfterVerificationSuppressesTheOldGrant() throws Exception {
        var id = publicChallenge(true, false);
        jdbc.update("UPDATE email_verifications SET created_at=clock_timestamp()-interval '61 seconds' WHERE id=?", id);
        var csrf = csrf("");
        var gate = new Gate();
        ResponseGate.armed.set(gate);
        var pending = client.sendAsync(verifyPost(id, "012345", csrf), java.net.http.HttpResponse.BodyHandlers.ofString());
        try {
            assertThat(gate.entered.await(8, TimeUnit.SECONDS)).isTrue();
            var replacement = send(post("{\"purpose\":\"SIGNUP\",\"email\":\"verify@example.invalid\"}", csrf));
            assertThat(replacement.statusCode()).isEqualTo(200);
            assertThat(jdbc.queryForObject("SELECT invalidated_at IS NOT NULL FROM email_verifications WHERE id=?",
                    Boolean.class, id)).isTrue();
            gate.release.countDown();
            assertError(pending.get(10, TimeUnit.SECONDS), 400, "VERIFICATION_INVALID");
        }
        finally {
            gate.release.countDown();
        }
    }

    @Test void competingBusinessTransactionsCannotBothConsumeOneToken() throws Exception {
        String token = issuedToken(publicChallenge(true, false), csrf(""));
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Callable<String> consume = () -> {
                try {
                    var tx = new TransactionTemplate(transactions);
                    tx.executeWithoutResult(status -> verification.consume(token, Purpose.SIGNUP, null));
                    return "CONSUMED";
                }
                catch (EmailVerificationException failure) {
                    return failure.code();
                }
            };
            var first = executor.submit(consume);
            var second = executor.submit(consume);
            assertThat(List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder("CONSUMED", "VERIFICATION_INVALID");
        }
    }

    @Test void passwordResetGrantStopsWorkingWhenAccountVersionChanges() throws Exception {
        var accountId = UUID.randomUUID();
        var id = resetChallenge(accountId);
        String token = issuedToken(id, csrf(""));
        jdbc.update("UPDATE accounts SET session_version=3 WHERE id=?", accountId);
        assertConsumeError(token, Purpose.RESET_PASSWORD, null, 400, "VERIFICATION_INVALID");
        assertThat(jdbc.queryForObject("SELECT token_consumed_at IS NULL FROM email_verifications WHERE id=?",
                Boolean.class, id)).isTrue();
    }

    @Test void changedResetAccountVersionPreventsIssuingToken() throws Exception {
        var account = UUID.randomUUID();
        var id = resetChallenge(account);
        jdbc.update("UPDATE accounts SET session_version=3 WHERE id=?", account);
        assertError(send(verifyPost(id, "012345", csrf(""))), 400, "VERIFICATION_INVALID");
        assertThat(jdbc.queryForObject("SELECT verified_at IS NULL AND token_hash IS NULL FROM email_verifications WHERE id=?",
                Boolean.class, id)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void wrongResetCodeUsesOnlyChallengeState(boolean eligible) throws Exception {
        UUID id;
        if (eligible) {
            var account = UUID.randomUUID();
            id = resetChallenge(account);
            jdbc.update("UPDATE accounts SET session_version=session_version+1 WHERE id=?", account);
        } else {
            id = publicChallenge(false, false);
            jdbc.update("UPDATE email_verifications SET purpose='RESET_PASSWORD' WHERE id=?", id);
        }
        jdbc.update("UPDATE email_verifications SET attempt_count=3 WHERE id=?", id);
        var request = verifyPost(id, "012346", csrf(""));
        assertError(send(request), 400, "VERIFICATION_INVALID");
        assertError(send(request), 429, "RATE_LIMITED");
        verify(accounts, never()).findForUpdate(anyString());
        assertThat(jdbc.queryForObject("""
                SELECT attempt_count=5 AND invalidated_at IS NOT NULL AND token_hash IS NULL
                FROM email_verifications WHERE id=?
                """, Boolean.class, id)).isTrue();
    }

    @Test void finalGrantCheckRejectsChangedResetAccountVersion() {
        var account = UUID.randomUUID();
        var id = resetChallenge(account);
        var request = new MockHttpServletRequest();
        verification.verify(request, id, () -> "012345", InetAddress.ofLiteral("192.0.2.10"));
        jdbc.update("UPDATE accounts SET session_version=3 WHERE id=?", account);
        assertThatThrownBy(() -> verification.verifyResponseAccess(request))
                .isInstanceOfSatisfying(EmailVerificationException.class, error ->
                        assertThat(error.code()).isEqualTo("VERIFICATION_INVALID"));
    }

    private UUID resetChallenge(UUID accountId) {
        var id = UUID.randomUUID();
        jdbc.update("INSERT INTO accounts(id,email,session_version) VALUES(?,'reset@example.invalid',2)", accountId);
        jdbc.update("""
                INSERT INTO email_verifications(id,email,purpose,eligible,code_hash,expires_at,reset_account_id,reset_session_version)
                VALUES(?,'reset@example.invalid','RESET_PASSWORD',true,?,clock_timestamp()+interval '600 seconds',?,2)
                """, id, keys.codeHasher().hash("012345"), accountId);
        return id;
    }

    private void assertConsumeError(String token, Purpose purpose, AuthenticatedSession current, int status, String code) {
        assertThatThrownBy(() -> new TransactionTemplate(transactions).executeWithoutResult(tx ->
                verification.consume(token, purpose, current)))
                .isInstanceOfSatisfying(EmailVerificationException.class, failure -> {
                    assertThat(failure.status()).isEqualTo(status);
                    assertThat(failure.code()).isEqualTo(code);
                });
    }

    private String issuedToken(UUID id, Csrf csrf) throws Exception {
        var response = send(verifyPost(id, "012345", csrf));
        assertThat(response.statusCode()).isEqualTo(200);
        return mapper.readTree(response.body()).get("data").get("verificationToken").asText();
    }

    private UUID publicChallenge(boolean eligible, boolean expired) {
        var id = UUID.randomUUID();
        jdbc.update("""
                WITH t AS (SELECT clock_timestamp() - (? * interval '1 second') AS created_at)
                INSERT INTO email_verifications(id,email,purpose,eligible,code_hash,created_at,expires_at)
                SELECT ?,'verify@example.invalid','SIGNUP',?,?,created_at,created_at+interval '600 seconds' FROM t
                """, expired ? 601 : 1, id, eligible, keys.codeHasher().hash("012345"));
        return id;
    }

    private UUID pinChallenge(Fixture owner, String purpose) {
        var id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO email_verifications(id,email,purpose,eligible,code_hash,expires_at,
                  setup_account_id,setup_session_binding,setup_generation,pin_version_snapshot)
                SELECT ?,a.email,?,true,?,clock_timestamp()+interval '600 seconds',
                  a.id,s.id,s.setup_generation,p.pin_version
                FROM accounts a JOIN session_security s ON s.account_id=a.id
                LEFT JOIN guardian_pins p ON p.account_id=a.id WHERE s.id=?
                """, id, purpose, keys.codeHasher().hash("012345"), owner.context());
        return id;
    }

    private HttpRequest verifyPost(UUID id, String code, Csrf csrf) {
        return post(VERIFY_PATH, "{\"challengeId\":\"" + id + "\",\"code\":\"" + code + "\"}", csrf);
    }

    private Fixture fixture(boolean pin) {
        var account=UUID.randomUUID(); var contextId=UUID.randomUUID();
        jdbc.update("INSERT INTO accounts(id,email) VALUES(?,?)",account,account+"@example.invalid");
        jdbc.update("INSERT INTO session_security(id,account_id,session_version) VALUES(?,?,0)",contextId,account);
        if(pin) { jdbc.update("INSERT INTO guardian_pins(account_id,pin_hash) VALUES(?,'synthetic-hash')",account); }
        Session session=sessions.createSession(); session.setMaxInactiveInterval(Duration.ofSeconds(-1));
        var security=SecurityContextHolder.createEmptyContext();
        security.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(account.toString(),null,List.of(new SimpleGrantedAuthority("ROLE_USER"))));
        session.setAttribute("accountId",account); session.setAttribute("securityContextId",contextId);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,security);
        var response=new MockHttpServletResponse();
        cookies.writeCookieValue(new CookieSerializer.CookieValue(new MockHttpServletRequest(),response,session.getId()));
        return new Fixture(account,contextId,"JSESSIONID="+MockCookie.parse(response.getHeader("Set-Cookie")).getValue());
    }
    private Csrf csrf(String session) throws Exception {
        var request=HttpRequest.newBuilder(uri("/api/v1/auth/csrf")).GET();
        if(!session.isEmpty()) { request.header("Cookie",session); }
        var response=send(request.build());
        assertThat(response.statusCode()).isEqualTo(200);
        var body=mapper.readTree(response.body());
        assertThat(body.has("token")).isTrue();
        assertThat(body.has("data")).isFalse();
        String token=body.get("token").asText();
        return new Csrf((session.isEmpty()?"":session+"; ")+"XSRF-TOKEN="+token,token);
    }
    private HttpRequest post(String body,Csrf csrf) {
        return post(PATH, body, csrf);
    }
    private HttpRequest post(String path, String body, Csrf csrf) {
        return HttpRequest.newBuilder(uri(path)).header("Content-Type","application/json").header("Cookie",csrf.cookie)
                .header("X-XSRF-TOKEN",csrf.token).header("Origin","http://localhost:5173").POST(HttpRequest.BodyPublishers.ofString(body)).build();
    }
    private java.net.http.HttpResponse<String> send(HttpRequest request) throws Exception { return client.send(request,java.net.http.HttpResponse.BodyHandlers.ofString()); }
    private URI uri(String path) { return URI.create("http://127.0.0.1:"+port+path); }
    private void assertError(java.net.http.HttpResponse<String> response,int status,String code) throws Exception {
        assertThat(response.statusCode()).isEqualTo(status);
        var error=mapper.readTree(response.body()).get("error");
        assertThat(error.size()).isEqualTo(4); assertThat(error.get("code").asText()).isEqualTo(code);
        assertThat(error.get("requestId").asText()).isEqualTo(response.headers().firstValue("X-Request-ID").orElseThrow());
        assertThat(response.headers().firstValue("Cache-Control").orElseThrow()).contains("no-store");
        assertThat(response.body()).doesNotContain("synthetic SMTP private detail","SELECT ");
    }
    record Fixture(UUID account,UUID context,String cookie) { }
    record Csrf(String cookie,String token) { }
    static class Gate { final CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1); }
    @ControllerAdvice(assignableTypes=EmailVerificationController.class)
    static class ResponseGate implements ResponseBodyAdvice<Object> {
        static final AtomicReference<Gate> armed=new AtomicReference<>();
        public boolean supports(MethodParameter method,Class<? extends HttpMessageConverter<?>> converter) { return true; }
        public Object beforeBodyWrite(Object body,MethodParameter method,MediaType media,Class<? extends HttpMessageConverter<?>> converter,ServerHttpRequest request,ServerHttpResponse response) {
            var gate=armed.getAndSet(null);
            if(gate!=null) { gate.entered.countDown(); try { if(!gate.release.await(10,TimeUnit.SECONDS)) { throw new AssertionError("gate timeout"); } }
                catch(InterruptedException e) { Thread.currentThread().interrupt(); throw new AssertionError(e); } }
            return body;
        }
    }
}
