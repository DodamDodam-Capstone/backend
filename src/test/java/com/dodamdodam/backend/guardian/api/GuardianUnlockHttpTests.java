package com.dodamdodam.backend.guardian.api;

import com.dodamdodam.backend.TestcontainersConfiguration;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcPrint;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockCookie;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.session.Session;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@SpringBootTest(properties = "spring.session.jdbc.cleanup-cron=-")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class GuardianUnlockHttpTests {
    private static final String PATH = "/api/v1/guardian/unlock";
    private static final String PIN = "{\"pin\":\"0123\"}";
    private static final String WRONG_PIN = "{\"pin\":\"9876\"}";
    private static final String PIN_HASH = new Argon2PasswordEncoder(16, 32, 1, 19 * 1024, 2).encode("0123");

    @Autowired private MockMvc mvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private JdbcIndexedSessionRepository sessions;
    @Autowired private CookieSerializer cookies;
    @Autowired private PlatformTransactionManager transactions;

    @BeforeEach
    void clearDatabase() {
        jdbc.update("DELETE FROM SPRING_SESSION");
        jdbc.update("DELETE FROM auth_rate_limits");
        jdbc.update("DELETE FROM guardian_pins");
        jdbc.update("DELETE FROM session_security");
        jdbc.update("DELETE FROM accounts");
    }

    @Test
    void unlocksOnlyCurrentContextAtThePublishedKstDeadline() throws Exception {
        var account = account(true);
        var current = login(account);
        var other = login(account);
        var before = jdbc.queryForObject("SELECT clock_timestamp()", OffsetDateTime.class).withNano(0);

        String published = JsonPath.read(unlock(current, PIN).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), "$.data.guardianUnlockedUntil");
        var until = OffsetDateTime.parse(published);
        assertThat(until.getOffset()).isEqualTo(ZoneOffset.ofHours(9));
        assertThat(until.getNano()).isZero();
        assertThat(until.toInstant()).isAfterOrEqualTo(before.plusSeconds(1800).toInstant());
        assertThat(until.toInstant()).isBeforeOrEqualTo(jdbc.queryForObject(
                "SELECT clock_timestamp()", OffsetDateTime.class).plusSeconds(1800).toInstant());
        assertThat(storedDeadline(current).toInstant()).isEqualTo(until.toInstant());
        assertThat(storedDeadline(other)).isNull();
        assertThat(jdbc.queryForObject("SELECT guardian_pin_version FROM session_security WHERE id=?", Long.class,
                current.id())).isEqualTo(3L);
    }

    @Test
    void clipsTheGrantToAFiniteLoginAndRequiresAConfiguredPin() throws Exception {
        var current = login(account(false));
        assertError(unlock(current, PIN), 409, "PIN_NOT_SET");
        addPin(current.account());
        var ends = jdbc.queryForObject("SELECT date_trunc('second', clock_timestamp()) + interval '60 seconds'",
                OffsetDateTime.class);
        jdbc.update("UPDATE session_security SET expires_at=? WHERE id=?", ends, current.id());
        unlock(current, PIN).andExpect(status().isOk());
        assertThat(storedDeadline(current).toInstant()).isEqualTo(ends.toInstant());
    }

    @Test
    void pinVersionChangedAheadOfUnlockInvalidatesTheOldPin() throws Exception {
        var current = login(account(true));
        var changedPin = new Argon2PasswordEncoder(16, 32, 1, 19 * 1024, 2).encode("4567");
        var pending = new TransactionTemplate(transactions).execute(ignored -> {
            jdbc.queryForObject("SELECT id FROM accounts WHERE id=? FOR UPDATE", UUID.class, current.account());
            var attempt = CompletableFuture.supplyAsync(() -> {
                try {
                    return unlock(current, PIN);
                }
                catch (Exception failure) {
                    throw new AssertionError(failure);
                }
            });
            // 잠금 대기 중인 요청을 확인하고 비로소 새 PIN을 커밋한다.
            await().atMost(Duration.ofSeconds(5)).until(() -> Boolean.TRUE.equals(jdbc.queryForObject("""
                    SELECT EXISTS(SELECT 1 FROM pg_stat_activity
                      WHERE wait_event_type='Lock' AND query LIKE 'SELECT id FROM accounts WHERE id = % FOR UPDATE')
                    """, Boolean.class)));
            jdbc.update("UPDATE guardian_pins SET pin_hash=?, pin_version=4 WHERE account_id=?",
                    changedPin, current.account());
            return attempt;
        });
        assertError(pending.get(10, TimeUnit.SECONDS), 400, "PIN_INVALID");
        assertThat(storedDeadline(current)).isNull();
        unlock(current, "{\"pin\":\"4567\"}").andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT guardian_pin_version FROM session_security WHERE id=?", Long.class,
                current.id())).isEqualTo(4L);
    }

    @Test
    void fifthRecentWrongPinCommitsBlockAcrossSessionsAndSuccessKeepsHistory() throws Exception {
        var current = login(account(true));
        var other = login(current.account());
        for (int index = 0; index < 2; index++) {
            assertError(unlock(current, WRONG_PIN), 400, "PIN_INVALID");
        }
        unlock(current, PIN).andExpect(status().isOk());
        var originalGrant = storedDeadline(current);
        for (int index = 2; index < 4; index++) {
            assertError(unlock(other, WRONG_PIN), 400, "PIN_INVALID");
        }
        long retry = Long.parseLong(assertError(unlock(other, WRONG_PIN), 429, "RATE_LIMITED")
                .andReturn().getResponse().getHeader("Retry-After"));
        assertThat(retry).isBetween(1L, 900L);
        var blockedUntil = pinBlockedUntil();
        assertError(unlock(current, PIN), 429, "RATE_LIMITED");
        assertThat(pinBlockedUntil()).isEqualTo(blockedUntil);
        assertThat(jdbc.queryForObject("SELECT cardinality(failure_times) FROM auth_rate_limits "
                + "WHERE action='PIN_FAILURE_ACCOUNT'", Integer.class)).isEqualTo(5);
        assertThat(storedDeadline(current)).isEqualTo(originalGrant);
        assertThat(storedDeadline(other)).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"raw", "type", "version", "cost", "salt", "hash", "padding"})
    void damagedStoredPinReturns503WithoutChangingFailureHistory(String damage) throws Exception {
        var current = login(account(true));
        assertError(unlock(current, WRONG_PIN), 400, "PIN_INVALID");
        String damagedHash = switch (damage) {
            case "raw" -> "synthetic-unreadable-hash";
            case "type" -> PIN_HASH.replace("$argon2id$", "$argon2i$");
            case "version" -> PIN_HASH.replace("$v=19$", "$v=16$");
            case "cost" -> PIN_HASH.replace(",t=2,", ",t=1,");
            case "salt" -> PIN_HASH.replaceFirst("\\$[^$]+(?=\\$[^$]+$)", "\\$AAAA");
            case "hash" -> PIN_HASH.substring(0, PIN_HASH.length() - 1);
            case "padding" -> PIN_HASH + "=";
            default -> throw new AssertionError(damage);
        };
        String historyQuery = """
                SELECT attempt_count, failure_times::text, blocked_until, expires_at
                FROM auth_rate_limits WHERE action='PIN_FAILURE_ACCOUNT'
                """;
        var before = jdbc.queryForMap(historyQuery);
        jdbc.update("UPDATE guardian_pins SET pin_hash=? WHERE account_id=?", damagedHash, current.account());

        var response = assertError(unlock(current, PIN), 503, "AUTH_STATE_UNAVAILABLE").andReturn().getResponse();
        assertThat(response.getContentAsString()).doesNotContain(damagedHash, "\"0123\"");
        assertThat(jdbc.queryForMap(historyQuery)).isEqualTo(before);
        assertThat(storedDeadline(current)).isNull();
    }

    @Test
    void ignoresFailuresAtTheWindowLowerBoundAndAllowsRetryAtBlockEnd() throws Exception {
        var current = login(account(true));
        assertError(unlock(current, WRONG_PIN), 400, "PIN_INVALID");
        jdbc.update("""
                UPDATE auth_rate_limits SET failure_times=ARRAY[clock_timestamp()-interval '900 seconds'],
                    blocked_until=clock_timestamp(), expires_at=clock_timestamp()+interval '1 second'
                WHERE action='PIN_FAILURE_ACCOUNT'
                """);
        assertError(unlock(current, WRONG_PIN), 400, "PIN_INVALID");
        assertThat(jdbc.queryForObject("SELECT cardinality(failure_times) FROM auth_rate_limits "
                + "WHERE action='PIN_FAILURE_ACCOUNT'", Integer.class)).isEqualTo(1);
        assertThat(pinBlockedUntil()).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"PIN_UNLOCK_ACCOUNT", "PIN_UNLOCK_IP"})
    void globalUnlockBudgetDenialDoesNotAddWrongPin(String action) throws Exception {
        var current = login(account(true));
        unlock(current, PIN).andExpect(status().isOk());
        jdbc.update("UPDATE auth_rate_limits SET attempt_count=? WHERE action=?",
                action.endsWith("ACCOUNT") ? 20 : 100, action);
        var response = assertError(unlock(current, WRONG_PIN), 429, "RATE_LIMITED").andReturn().getResponse();
        assertThat(Long.parseLong(response.getHeader("Retry-After"))).isBetween(1L, 900L);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM auth_rate_limits WHERE action='PIN_FAILURE_ACCOUNT'",
                Long.class)).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"pin\":1234}", "{\"pin\":null}", "{\"pin\":\"１２３４\"}", "{\"pin\":\"0123\\n\"}",
            "{\"pin\":\"0123\",\"other\":true}", "{\"pin\":\"0123\",\"pin\":\"0123\"}", "{\"pin\":\"0123\"}{}"})
    void invalidShapesDoNotReserveUnlockBudget(String payload) throws Exception {
        var current = login(account(true));
        assertError(unlock(current, payload), 400, "VALIDATION_FAILED");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM auth_rate_limits WHERE action LIKE 'PIN_%'",
                Long.class)).isZero();
    }

    @Test
    void transportPrecedesCsrfAndCsrfPrecedesAuthentication() throws Exception {
        assertError(mvc.perform(post(PATH).contentType("text/plain").content(PIN)), 415, "UNSUPPORTED_MEDIA_TYPE");
        assertError(mvc.perform(post(PATH).contentType("application/json").content(" ".repeat(16385))),
                413, "REQUEST_TOO_LARGE");
        assertError(mvc.perform(post(PATH).contentType("application/json").content(PIN)), 403, "CSRF_INVALID");
        assertError(mvc.perform(withCsrf(post(PATH).contentType("application/json").content(PIN))),
                401, "AUTHENTICATION_REQUIRED");
    }

    private ResultActions unlock(Context session, String body) throws Exception {
        return mvc.perform(withCsrf(post(PATH).contentType("application/json").content(body)).cookie(session.cookie()));
    }

    private MockHttpServletRequestBuilder withCsrf(MockHttpServletRequestBuilder request) throws Exception {
        var response = mvc.perform(get("/api/v1/auth/csrf")).andExpect(status().isOk()).andReturn().getResponse();
        String token = JsonPath.read(response.getContentAsString(), "$.token");
        return request.cookie(response.getCookie("XSRF-TOKEN")).header("X-XSRF-TOKEN", token);
    }

    private ResultActions assertError(ResultActions call, int httpStatus, String code) throws Exception {
        return call.andExpect(status().is(httpStatus)).andExpect(jsonPath("$.error.code").value(code));
    }

    private UUID account(boolean withPin) {
        var id = UUID.randomUUID();
        jdbc.update("INSERT INTO accounts(id,email) VALUES (?,?)", id, id + "@example.invalid");
        if (withPin) {
            addPin(id);
        }
        return id;
    }

    private void addPin(UUID account) {
        jdbc.update("INSERT INTO guardian_pins(account_id,pin_hash,pin_version) VALUES (?,?,3)", account, PIN_HASH);
    }

    private Context login(UUID account) {
        var id = UUID.randomUUID();
        jdbc.update("INSERT INTO session_security(id,account_id,session_version) VALUES (?,?,0)", id, account);
        Session session = sessions.createSession();
        session.setMaxInactiveInterval(Duration.ofSeconds(-1));
        var security = SecurityContextHolder.createEmptyContext();
        security.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(account.toString(), null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))));
        session.setAttribute("accountId", account);
        session.setAttribute("securityContextId", id);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, security);
        var response = new MockHttpServletResponse();
        cookies.writeCookieValue(new CookieSerializer.CookieValue(new MockHttpServletRequest(), response, session.getId()));
        return new Context(account, id, MockCookie.parse(response.getHeader("Set-Cookie")));
    }

    private OffsetDateTime storedDeadline(Context context) {
        return jdbc.queryForObject("SELECT guardian_unlocked_until FROM session_security WHERE id=?",
                OffsetDateTime.class, context.id());
    }

    private OffsetDateTime pinBlockedUntil() {
        return jdbc.queryForObject("SELECT blocked_until FROM auth_rate_limits WHERE action='PIN_FAILURE_ACCOUNT'",
                OffsetDateTime.class);
    }

    private record Context(UUID account, UUID id, Cookie cookie) { }
}
