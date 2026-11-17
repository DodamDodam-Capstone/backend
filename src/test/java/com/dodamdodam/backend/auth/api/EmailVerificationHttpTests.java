package com.dodamdodam.backend.auth.api;

import com.dodamdodam.backend.TestcontainersConfiguration;
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
import tools.jackson.databind.json.JsonMapper;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT, properties={"spring.session.jdbc.cleanup-cron=-"})
@Import({TestcontainersConfiguration.class, EmailVerificationHttpTests.ResponseGate.class})
@DirtiesContext(classMode=DirtiesContext.ClassMode.AFTER_CLASS)
@Timeout(30)
class EmailVerificationHttpTests {
    static final String PATH="/api/v1/auth/email-verifications";
    @LocalServerPort int port;
    @Autowired JdbcTemplate jdbc;
    @Autowired JsonMapper mapper;
    @Autowired JdbcIndexedSessionRepository sessions;
    @Autowired CookieSerializer cookies;
    @MockitoBean SmtpEmailVerificationSender sender;
    @MockitoSpyBean EmailVerificationKeys keys;
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
        return HttpRequest.newBuilder(uri(PATH)).header("Content-Type","application/json").header("Cookie",csrf.cookie)
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
