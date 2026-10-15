package com.dodamdodam.backend;

import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockCookie;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;

@Import({TestcontainersConfiguration.class, SessionCookieLifecycleTests.Probe.class, SessionConcurrencyTests.DelayedRequest.class})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "SESSION_COOKIE_SECURE=false", "SESSION_COOKIE_SAME_SITE=lax", "spring.session.jdbc.cleanup-cron=-",
        "logging.level.org.springframework.security=INFO", "logging.level.org.springframework.session=INFO"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class SessionConcurrencyTests {

    private static final String DELAYED = "/api/v1/session-concurrency/delayed";
    @Autowired private JdbcTemplate jdbc;
    @Autowired private JdbcIndexedSessionRepository sessions;
    @Autowired private CookieSerializer cookies;
    @Autowired private DelayedRequest delayed;
    @LocalServerPort private int port;

    @BeforeEach
    void reset() {
        jdbc.update("DELETE FROM SPRING_SESSION");
        jdbc.update("DELETE FROM email_verifications");
        jdbc.update("DELETE FROM session_security");
        jdbc.update("DELETE FROM accounts");
        delayed.loaded = new CountDownLatch(1);
        delayed.resume = new CountDownLatch(1);
        delayed.saved = new CountDownLatch(1);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void lateRequestCannotRestoreCredentialsAfterLogoutOrReplacement(boolean replace) throws Exception {
        var current = SessionCookieLifecycleTests.authenticated(jdbc, sessions, cookies);
        var oldCookie = "JSESSIONID=" + current.cookie().getValue();
        try (var client = HttpClient.newHttpClient()) {
            var csrf = client.send(request("/api/v1/auth/csrf", oldCookie).GET().build(), HttpResponse.BodyHandlers.ofString());
            assertThat(csrf.statusCode()).isEqualTo(200);
            var csrfCookie = csrf.headers().allValues("Set-Cookie").stream()
                    .filter(value -> value.startsWith("XSRF-TOKEN=")).map(MockCookie::parse).findFirst().orElseThrow();
            var lateResponse = client.sendAsync(request(DELAYED, oldCookie).GET().build(), HttpResponse.BodyHandlers.ofString());
            HttpResponse<Void> completed;
            try {
                assertThat(delayed.loaded.await(10, TimeUnit.SECONDS)).as("old request loaded its native session").isTrue();
                var path = replace ? "/api/v1/auth/login?account=" + current.account() : "/api/v1/auth/logout";
                completed = client.send(request(path, oldCookie + "; XSRF-TOKEN=" + csrfCookie.getValue())
                                .header("X-XSRF-TOKEN", JsonPath.read(csrf.body(), "$.token"))
                                .POST(HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.discarding());
                assertThat(completed.statusCode()).isEqualTo(replace ? 200 : 204);
                assertCredentialsRevoked(current);
            }
            finally {
                // 업무 폐기와 native 삭제가 끝난 뒤에만 이전 요청의 실제 JDBC 저장을 허용한다.
                delayed.resume.countDown();
            }

            var late = lateResponse.get(35, TimeUnit.SECONDS);
            assertThat(delayed.saved.getCount()).as("late native attribute write returned").isZero();
            assertThat(late.statusCode()).isEqualTo(401);
            assertThat((String) JsonPath.read(late.body(), "$.error.code")).isEqualTo("AUTHENTICATION_REQUIRED");
            assertThat(late.body()).doesNotContain("protected-result");
            assertThat(late.headers().allValues("Set-Cookie").stream().filter(value -> value.startsWith("JSESSIONID="))
                    .map(MockCookie::parse).anyMatch(cookie -> cookie.getMaxAge() > 0)).isFalse();
            assertCredentialsRevoked(current);
            var rejected = client.send(request(SessionCookieLifecycleTests.PROTECTED, oldCookie).GET().build(),
                    HttpResponse.BodyHandlers.discarding());
            assertThat(rejected.statusCode()).as("old cookie stays unusable after late save").isEqualTo(401);

            if (replace) {
                var replacement = completed.headers().allValues("Set-Cookie").stream()
                        .filter(value -> value.startsWith("JSESSIONID=")).map(MockCookie::parse).findFirst().orElseThrow();
                assertThat(replacement.getValue().equals(current.cookie().getValue())).isFalse();
                var accepted = client.send(request(SessionCookieLifecycleTests.PROTECTED,
                        "JSESSIONID=" + replacement.getValue()).GET().build(), HttpResponse.BodyHandlers.discarding());
                assertThat(accepted.statusCode()).as("replacement remains usable after late save").isEqualTo(204);
                assertThat(jdbc.queryForObject("""
                        SELECT COUNT(*) FROM session_security
                        WHERE account_id = ? AND id <> ? AND revoked_at IS NULL AND expires_at IS NULL
                            AND guardian_unlocked_until IS NULL AND guardian_pin_version IS NULL
                        """, Integer.class, current.account(), current.context())).isEqualTo(1);
            }
        }
    }

    private HttpRequest.Builder request(String path, String cookie) {
        return HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .timeout(Duration.ofSeconds(30)).header("Cookie", cookie);
    }

    private void assertCredentialsRevoked(SessionCookieLifecycleTests.Fixture current) {
        var context = jdbc.queryForMap("""
                SELECT revoked_at, guardian_unlocked_until, guardian_pin_version
                FROM session_security WHERE id = ?
                """, current.context());
        assertThat(context.get("revoked_at")).isNotNull();
        assertThat(context.get("guardian_unlocked_until")).isNull();
        assertThat(context.get("guardian_pin_version")).isNull();
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM email_verifications
                WHERE setup_session_binding = ? AND purpose IN ('PIN_SETUP', 'PIN_RESET')
                    AND token_consumed_at IS NULL AND invalidated_at IS NOT NULL
                """, Integer.class, current.context())).isEqualTo(2);
    }

    @TestConfiguration(proxyBeanMethods = false)
    @RestController
    static class DelayedRequest {
        CountDownLatch loaded;
        CountDownLatch resume;
        CountDownLatch saved;

        @GetMapping(DELAYED)
        void saveAfterRevocation(HttpServletRequest request, HttpServletResponse response) throws InterruptedException, IOException {
            var session = request.getSession(false);
            loaded.countDown();
            if (!resume.await(30, TimeUnit.SECONDS)) throw new IllegalStateException("Concurrent request did not finish");
            session.setAttribute("late-request-marker", "saved");
            saved.countDown();
            response.getWriter().write("protected-result");
            response.flushBuffer();
        }
    }
}
