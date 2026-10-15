package com.dodamdodam.backend;

import com.dodamdodam.backend.auth.session.AuthenticatedSession;
import com.dodamdodam.backend.auth.session.AuthenticatedSessionService;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcPrint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockCookie;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2LoginAuthenticationToken;
import org.springframework.security.oauth2.client.web.OAuth2LoginAuthenticationFilter;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.util.OnCommittedResponseWrapper;
import org.springframework.session.Session;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.SessionRepositoryFilter;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import({TestcontainersConfiguration.class, LoginSessionPersistenceTests.Probe.class})
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"spring.session.jdbc.cleanup-cron=-", "SESSION_COOKIE_SECURE=false",
                "logging.level.org.springframework.session=INFO", "APP_CORS_ALLOWED_ORIGINS=http://localhost:5173"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class LoginSessionPersistenceTests {

    private static final String PROTECTED = "/api/v1/contract/login-session";
    private static final String SECURITY_CONTEXT = HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY;

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private JdbcIndexedSessionRepository sessionRepository;
    @Autowired private CookieSerializer cookieSerializer;
    @Autowired private FilterChainProxy securityFilterChain;
    @Autowired private CommitFault commitFault;

    private UUID accountId;

    @BeforeEach
    void prepareAccountAndProviderResult() {
        commitFault.phase = null;
        commitFault.skipContextSave = false;
        jdbcTemplate.update("DELETE FROM SPRING_SESSION");
        jdbcTemplate.update("DELETE FROM email_verifications");
        jdbcTemplate.update("DELETE FROM session_security");
        jdbcTemplate.update("DELETE FROM accounts");
        accountId = UUID.randomUUID();
        commitFault.accountName = accountId.toString();
        jdbcTemplate.update("INSERT INTO accounts (id, email, session_version) VALUES (?, ?, 7)",
                accountId, accountId + "@example.invalid");
        var oauth = securityFilterChain.getFilterChains().stream().flatMap(chain -> chain.getFilters().stream())
                .filter(OAuth2LoginAuthenticationFilter.class::isInstance)
                .map(OAuth2LoginAuthenticationFilter.class::cast).findFirst().orElseThrow();
        // 외부 제공자의 인증 결과만 대체한다. 세션 전략·저장·성공 응답은 실제 필터를 실행한다.
        oauth.setAuthenticationManager(this::providerResult);
    }

    @Test
    void oauthSuccessPersistsCurrentUnlimitedLoginBeforeRedirect() throws Exception {
        var start = startOAuth(null);
        var before = cookie(start);
        var result = completeOAuth(start);
        assertThat(result.getResponse().getStatus()).isEqualTo(302);
        var after = cookie(result);
        var session = stored(after);
        assertThat(!session.getId().equals(storedId(before))).as("login rotates the native session ID").isTrue();
        assertThat(accountId.equals(session.getAttribute("accountId"))).as("persisted account binding").isTrue();
        Object contextId = session.getAttribute("securityContextId");
        assertThat(contextId instanceof UUID).as("persisted context binding").isTrue();
        assertThat(session.getMaxInactiveInterval()).isEqualTo(Duration.ofSeconds(-1));
        SecurityContext security = session.getAttribute(SECURITY_CONTEXT);
        assertThat(security != null && accountId.toString().equals(security.getAuthentication().getName()))
                .as("persisted authenticated principal").isTrue();
        var state = jdbcTemplate.queryForMap("SELECT * FROM session_security WHERE id = ?", contextId);
        assertThat(state.get("session_version")).isEqualTo(7L);
        assertThat(state.get("expires_at")).isNull();
        assertThat(state.get("revoked_at")).isNull();
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM session_security", Integer.class)).isEqualTo(1);
        mockMvc.perform(get(PROTECTED).cookie(after)).andExpect(status().isOk());
    }

    @Test
    void reloginRevokesOldGuardianAndProofsWithoutInheritingThem() throws Exception {
        var previous = previousLogin();
        var untouched = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO session_security (id, account_id, session_version) VALUES (?, ?, 7)",
                untouched, accountId);
        var result = completeOAuth(startOAuth(previous.cookie()));
        assertThat(result.getResponse().getStatus()).isEqualTo(302);
        var current = stored(cookie(result));
        assertThat(previous.contextId().equals(current.getAttribute("securityContextId")))
                .as("new login uses a fresh business context").isFalse();
        assertThat(current.<Object>getAttribute("guardianUnlockedUntil")).isNull();
        var old = jdbcTemplate.queryForMap("SELECT * FROM session_security WHERE id = ?", previous.contextId());
        assertThat(old.get("revoked_at")).isNotNull();
        assertThat(old.get("guardian_unlocked_until")).isNull();
        assertThat(old.get("guardian_pin_version")).isNull();
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM email_verifications WHERE invalidated_at IS NOT NULL",
                Integer.class)).isEqualTo(2);
        var fresh = jdbcTemplate.queryForMap("SELECT * FROM session_security WHERE id = ?",
                current.<Object>getAttribute("securityContextId"));
        assertThat(fresh.get("guardian_unlocked_until")).isNull();
        assertThat(fresh.get("guardian_pin_version")).isNull();
        assertThat(fresh.get("setup_generation")).isEqualTo(0L);
        assertThat(jdbcTemplate.queryForObject("SELECT revoked_at IS NULL FROM session_security WHERE id = ?",
                Boolean.class, untouched)).isTrue();
        assertThat(stored(previous.cookie()) == null).as("replacement deletes the previous native session").isTrue();
        mockMvc.perform(get(PROTECTED).cookie(previous.cookie())).andExpect(status().isUnauthorized());
        mockMvc.perform(get(PROTECTED).cookie(cookie(result))).andExpect(status().isOk());
    }

    @Test
    void frameworkWriteFailureCompensatesNewContextAndKeepsOldContextRevoked() throws Exception {
        var previous = previousLogin();
        var start = startOAuth(previous.cookie());
        installAttributeFailure("58000", SECURITY_CONTEXT);
        try {
            assertUnavailable(completeOAuth(start));
            assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM session_security WHERE revoked_at IS NOT NULL",
                    Integer.class)).isEqualTo(2);
            assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM session_security WHERE revoked_at IS NULL",
                    Integer.class)).isZero();
            assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM email_verifications WHERE invalidated_at IS NOT NULL",
                    Integer.class)).isEqualTo(2);
            assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM SPRING_SESSION", Integer.class)).isZero();
        }
        finally {
            removeAttributeFailure();
        }
    }

    @Test
    void silentlyIgnoredNativeWriteCannotProduceSuccessfulLogin() throws Exception {
        var start = startOAuth(null);
        installAttributeFailure("23514", SECURITY_CONTEXT);
        try {
            assertUnavailable(completeOAuth(start));
            assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM session_security WHERE revoked_at IS NOT NULL",
                    Integer.class)).isEqualTo(1);
        }
        finally {
            removeAttributeFailure();
        }
    }

    @Test
    void failedBusinessTransactionDoesNotPartiallyRevokePreviousLogin() throws Exception {
        var previous = previousLogin();
        var start = startOAuth(previous.cookie());
        jdbcTemplate.execute("""
                CREATE FUNCTION test_login_insert_failure() RETURNS trigger LANGUAGE plpgsql AS $$
                BEGIN RAISE EXCEPTION 'injected business storage failure' USING ERRCODE = '58000'; END;
                $$
                """);
        jdbcTemplate.execute("""
                CREATE TRIGGER test_login_insert_failure BEFORE INSERT ON session_security
                FOR EACH ROW EXECUTE FUNCTION test_login_insert_failure()
                """);
        try {
            assertUnavailable(completeOAuth(start));
            assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM session_security", Integer.class)).isEqualTo(1);
            assertThat(jdbcTemplate.queryForObject("SELECT revoked_at IS NULL FROM session_security", Boolean.class)).isTrue();
            assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM email_verifications WHERE invalidated_at IS NULL",
                    Integer.class)).isEqualTo(2);
            mockMvc.perform(get(PROTECTED).cookie(previous.cookie())).andExpect(status().isOk());
        }
        finally {
            jdbcTemplate.execute("DROP TRIGGER test_login_insert_failure ON session_security");
            jdbcTemplate.execute("DROP FUNCTION test_login_insert_failure()");
        }
    }

    @Test
    void pendingOAuthRemainsAnonymousWithThirtyMinuteTimeout() throws Exception {
        var pending = stored(cookie(startOAuth(null)));
        assertThat(pending.getMaxInactiveInterval()).isEqualTo(Duration.ofMinutes(30));
        assertThat(pending.<Object>getAttribute("securityContextId")).isNull();
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM session_security", Integer.class)).isZero();
    }

    @Test
    void nativeFailureDuringRedirectCommitStillCompensatesLogin() throws Exception {
        var start = startOAuth(null);
        commitFault.phase = "LATE_WRITE";
        installAttributeFailure("58000", "test-late-login-write");
        try {
            assertUnavailable(completeOAuth(start));
            assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM session_security WHERE revoked_at IS NOT NULL",
                    Integer.class)).isEqualTo(1);
        }
        finally {
            removeAttributeFailure();
        }
    }

    @Test
    void accountVersionChangeAfterContextSavePreventsSuccessResponse() throws Exception {
        var start = startOAuth(null);
        commitFault.phase = "VERSION_AFTER_SAVE";
        assertUnavailable(completeOAuth(start));
        assertThat(jdbcTemplate.queryForObject("SELECT session_version FROM accounts", Long.class)).isEqualTo(8L);
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM session_security WHERE revoked_at IS NOT NULL",
                Integer.class)).isEqualTo(1);
    }

    @Test
    void revocationAfterContextSavePreventsSuccessResponse() throws Exception {
        var start = startOAuth(null);
        commitFault.phase = "REVOKE_AFTER_SAVE";
        assertUnavailable(completeOAuth(start));
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM session_security WHERE revoked_at IS NOT NULL",
                Integer.class)).isEqualTo(1);
    }

    @Test
    void compensationWriteFailureStillReturns503WithoutAuthenticationCookie() throws Exception {
        var start = startOAuth(null);
        installAttributeFailure("58000", SECURITY_CONTEXT);
        jdbcTemplate.execute("""
                CREATE FUNCTION test_compensation_failure() RETURNS trigger LANGUAGE plpgsql AS $$
                BEGIN
                    RAISE EXCEPTION 'injected compensation failure' USING ERRCODE = '58000';
                END;
                $$
                """);
        jdbcTemplate.execute("""
                CREATE TRIGGER test_compensation_failure BEFORE UPDATE ON session_security
                FOR EACH ROW EXECUTE FUNCTION test_compensation_failure()
                """);
        try {
            assertUnavailable(completeOAuth(start));
            assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM session_security", Integer.class)).isEqualTo(1);
            assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM SPRING_SESSION_ATTRIBUTES WHERE ATTRIBUTE_NAME = ?",
                    Integer.class, SECURITY_CONTEXT)).isZero();
            assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM SPRING_SESSION", Integer.class)).isZero();
        }
        finally {
            jdbcTemplate.execute("DROP TRIGGER test_compensation_failure ON session_security");
            jdbcTemplate.execute("DROP FUNCTION test_compensation_failure()");
            removeAttributeFailure();
        }
    }

    @Test
    void switchingAccountsRevokesOnlyThePreviousBoundContext() throws Exception {
        var previous = previousLogin();
        var other = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO accounts (id, email, session_version) VALUES (?, ?, 11)",
                other, other + "@example.invalid");
        commitFault.accountName = other.toString();
        var result = completeOAuth(startOAuth(previous.cookie()));
        assertThat(result.getResponse().getStatus()).isEqualTo(302);
        var current = stored(cookie(result));
        assertThat(other.equals(current.getAttribute("accountId"))).as("new account is bound").isTrue();
        assertThat(jdbcTemplate.queryForObject("SELECT revoked_at IS NOT NULL FROM session_security WHERE id = ?",
                Boolean.class, previous.contextId())).isTrue();
        assertThat(jdbcTemplate.queryForObject("SELECT session_version FROM session_security WHERE id = ?",
                Long.class, current.<Object>getAttribute("securityContextId"))).isEqualTo(11L);
        mockMvc.perform(get(PROTECTED).cookie(cookie(result))).andExpect(status().isOk());
    }

    @Test
    void manualLoginConsumerUsesTheSameStrategyAndContextRepository() throws Exception {
        var login = manualLogin();
        assertThat(login.getResponse().getStatus()).isEqualTo(200);
        Session stored = stored(cookie(login));
        assertThat(stored.getMaxInactiveInterval()).isEqualTo(Duration.ofSeconds(-1));
        assertThat(accountId.equals(stored.getAttribute("accountId"))).as("manual login persists account").isTrue();
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM session_security WHERE revoked_at IS NULL",
                Integer.class)).isEqualTo(1);
        mockMvc.perform(get(PROTECTED).cookie(cookie(login))).andExpect(status().isOk());
    }

    @Test
    void manualLoginWithoutExplicitContextSaveCannotReturn200() throws Exception {
        commitFault.skipContextSave = true;
        assertUnavailable(manualLogin());
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM session_security WHERE revoked_at IS NOT NULL",
                Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM SPRING_SESSION", Integer.class)).isZero();
    }

    @Test
    void manualLoginCommitFailureReturnsJson503WithCors() throws Exception {
        commitFault.phase = "LATE_WRITE";
        installAttributeFailure("58000", "test-late-login-write");
        try {
            var result = manualLogin();
            assertUnavailable(result);
            assertThat(result.getResponse().getHeader("Access-Control-Allow-Origin")).isEqualTo("http://localhost:5173");
            assertThat(result.getResponse().getHeader("Access-Control-Allow-Credentials")).isEqualTo("true");
            assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM session_security WHERE revoked_at IS NOT NULL",
                    Integer.class)).isEqualTo(1);
        }
        finally {
            removeAttributeFailure();
        }
    }

    @Test
    void unrecognisedAccountPrincipalCannotCreateLoginState() throws Exception {
        commitFault.accountName = "external-provider-sub";
        assertThat(manualLogin().getResponse().getStatus()).isEqualTo(401);
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM session_security", Integer.class)).isZero();
        commitFault.accountName = UUID.randomUUID().toString();
        assertThat(manualLogin().getResponse().getStatus()).isEqualTo(401);
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM session_security", Integer.class)).isZero();
    }

    @Test
    void storeOutageAtCommitStillSuppressesSuccessCookieAndRedirect() throws Exception {
        var start = startOAuth(null);
        var anonymous = cookie(start);
        commitFault.phase = "STORES_UNAVAILABLE";
        try {
            assertUnavailable(completeOAuth(start));
        }
        finally {
            jdbcTemplate.execute("ALTER TABLE unavailable_login_native RENAME TO SPRING_SESSION");
            jdbcTemplate.execute("ALTER TABLE unavailable_login_security RENAME TO session_security");
        }
        // 보상도 실패한 DB 상태를 성공으로 꾸미지 않는다. 새 세션 식별자는 응답하지 않았다.
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM session_security", Integer.class)).isEqualTo(1);
        mockMvc.perform(get(PROTECTED).cookie(anonymous)).andExpect(status().isUnauthorized());
    }

    private MvcResult manualLogin() throws Exception {
        var csrf = mockMvc.perform(get("/api/v1/auth/csrf")).andExpect(status().isOk()).andReturn().getResponse();
        String token = JsonPath.read(csrf.getContentAsString(), "$.token");
        return mockMvc.perform(post("/api/v1/auth/login").cookie(csrf.getCookie("XSRF-TOKEN"))
                .header("X-XSRF-TOKEN", token).header("Origin", "http://localhost:5173")).andReturn();
    }

    private MvcResult startOAuth(Cookie previous) throws Exception {
        var request = get("/oauth2/authorization/google");
        if (previous != null) request.cookie(previous);
        return mockMvc.perform(request).andExpect(status().isFound()).andReturn();
    }

    private MvcResult completeOAuth(MvcResult start) throws Exception {
        String state = UriComponentsBuilder.fromUriString(start.getResponse().getRedirectedUrl())
                .build().getQueryParams().getFirst("state");
        return mockMvc.perform(get("/login/oauth2/code/google").cookie(cookie(start))
                .param("code", "test-authorization-code")
                .param("state", URLDecoder.decode(state, StandardCharsets.UTF_8))).andReturn();
    }

    private Authentication providerResult(Authentication authentication) {
        var request = (OAuth2LoginAuthenticationToken) authentication;
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        var principal = new DefaultOAuth2User(authorities, Map.of("sub", commitFault.accountName), "sub");
        var now = Instant.now();
        return new OAuth2LoginAuthenticationToken(request.getClientRegistration(), request.getAuthorizationExchange(),
                principal, authorities, new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER,
                "test-oauth-access-token", now, now.plusSeconds(300)));
    }

    private Previous previousLogin() {
        var contextId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO session_security (id, account_id, session_version, setup_generation,
                    guardian_unlocked_until, guardian_pin_version)
                VALUES (?, ?, 7, 4, clock_timestamp() + INTERVAL '30 minutes', 2)
                """, contextId, accountId);
        for (String purpose : List.of("PIN_SETUP", "PIN_RESET")) {
            jdbcTemplate.update("""
                    INSERT INTO email_verifications (id, email, purpose, eligible, code_hash, expires_at,
                        setup_account_id, setup_session_binding, setup_generation, pin_version_snapshot)
                    VALUES (?, ?, ?, true, ?, clock_timestamp() + INTERVAL '10 minutes', ?, ?, 4, ?)
                    """, UUID.randomUUID(), accountId + "@example.invalid", purpose, new byte[32],
                    accountId, contextId, purpose.equals("PIN_RESET") ? 2L : null);
        }
        Session session = sessionRepository.createSession();
        var security = SecurityContextHolder.createEmptyContext();
        security.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(accountId.toString(), null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))));
        session.setAttribute("accountId", accountId);
        session.setAttribute("securityContextId", contextId);
        session.setAttribute("guardianUnlockedUntil", Instant.now().plusSeconds(1800));
        session.setAttribute(SECURITY_CONTEXT, security);
        var response = new MockHttpServletResponse();
        cookieSerializer.writeCookieValue(new CookieSerializer.CookieValue(new MockHttpServletRequest(), response, session.getId()));
        return new Previous(contextId, MockCookie.parse(response.getHeader("Set-Cookie")));
    }

    private Cookie cookie(MvcResult result) {
        return result.getResponse().getHeaders("Set-Cookie").stream().filter(h -> h.startsWith("JSESSIONID="))
                .<Cookie>map(MockCookie::parse).findFirst().orElseGet(() -> List.of(result.getRequest().getCookies()).stream()
                        .filter(c -> c.getName().equals("JSESSIONID")).findFirst().orElseThrow());
    }

    private String storedId(Cookie cookie) {
        var request = new MockHttpServletRequest();
        request.setCookies(cookie);
        return cookieSerializer.readCookieValues(request).getFirst();
    }

    private Session stored(Cookie cookie) {
        return sessionRepository.findById(storedId(cookie));
    }

    private void installAttributeFailure(String sqlState, String attribute) {
        jdbcTemplate.execute("""
                CREATE FUNCTION test_login_attribute_failure() RETURNS trigger LANGUAGE plpgsql AS $$
                BEGIN
                    IF NEW.ATTRIBUTE_NAME = '%s' THEN
                        RAISE EXCEPTION 'injected native storage failure' USING ERRCODE = '%s';
                    END IF;
                    RETURN NEW;
                END;
                $$
                """.formatted(attribute, sqlState));
        jdbcTemplate.execute("""
                CREATE TRIGGER test_login_attribute_failure BEFORE INSERT OR UPDATE ON SPRING_SESSION_ATTRIBUTES
                FOR EACH ROW EXECUTE FUNCTION test_login_attribute_failure()
                """);
    }

    private void removeAttributeFailure() {
        jdbcTemplate.execute("DROP TRIGGER test_login_attribute_failure ON SPRING_SESSION_ATTRIBUTES");
        jdbcTemplate.execute("DROP FUNCTION test_login_attribute_failure()");
    }

    private void assertUnavailable(MvcResult result) throws Exception {
        var response = result.getResponse();
        assertThat(response.getStatus()).isEqualTo(503);
        assertThat(JsonPath.<String>read(response.getContentAsString(), "$.error.code"))
                .isEqualTo("AUTH_STATE_UNAVAILABLE");
        assertThat(JsonPath.<List<?>>read(response.getContentAsString(), "$.error.fields")).isEmpty();
        assertThat(JsonPath.<String>read(response.getContentAsString(), "$.error.requestId"))
                .isEqualTo(response.getHeader("X-Request-ID"));
        assertThat(response.getHeader("Cache-Control")).contains("no-store");
        assertThat(response.getRedirectedUrl()).isNull();
        assertThat(response.getHeader("Location")).isNull();
        assertThat(response.getHeaders("Set-Cookie").stream().noneMatch(h -> h.startsWith("JSESSIONID=")))
                .as("failed login does not issue an authentication cookie").isTrue();
    }

    private record Previous(UUID contextId, Cookie cookie) { }

    static class CommitFault {
        String phase;
        String accountName;
        boolean skipContextSave;
    }

    @TestConfiguration(proxyBeanMethods = false)
    @RestController
    static class Probe {
        @Autowired private AuthenticatedSessionService service;
        @Autowired private SessionAuthenticationStrategy strategy;
        @Autowired private SecurityContextRepository contexts;
        @Autowired private CommitFault fault;

        @GetMapping(PROTECTED)
        Map<String, AuthenticatedSession> authenticated(HttpServletRequest request) {
            return Map.of("data", service.requireAuthenticatedSession(request));
        }

        @PostMapping("/api/v1/auth/login")
        void manualLogin(HttpServletRequest request, HttpServletResponse response) throws IOException {
            // 인증 완료 결과만 만드는 테스트 소비자다. 실제 비밀번호 확인 API는 SCRUM-78 범위다.
            var authentication = UsernamePasswordAuthenticationToken.authenticated(fault.accountName, null,
                    List.of(new SimpleGrantedAuthority("ROLE_USER")));
            strategy.onAuthentication(authentication, request, response);
            var context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            if (!fault.skipContextSave) contexts.saveContext(context, request, response);
            response.flushBuffer();
        }

        @Bean
        static CommitFault commitFault() {
            return new CommitFault();
        }

        @Bean
        FilterRegistrationBean<OncePerRequestFilter> loginCommitFaultFilter(CommitFault fault, JdbcTemplate jdbc) {
            var filter = new OncePerRequestFilter() {
                @Override
                protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                                FilterChain chain) throws ServletException, IOException {
                    if (!(request.getRequestURI().equals("/login/oauth2/code/google")
                            || request.getRequestURI().equals("/api/v1/auth/login")) || fault.phase == null) {
                        chain.doFilter(request, response);
                        return;
                    }
                    chain.doFilter(request, new OnCommittedResponseWrapper(response) {
                        @Override
                        protected void onResponseCommitted() {
                            disableOnResponseCommitted();
                            switch (fault.phase) {
                                case "LATE_WRITE" -> request.getSession(false).setAttribute("test-late-login-write", true);
                                case "VERSION_AFTER_SAVE" -> jdbc.update("UPDATE accounts SET session_version = 8");
                                case "REVOKE_AFTER_SAVE" -> jdbc.update("UPDATE session_security SET revoked_at = clock_timestamp()");
                                case "STORES_UNAVAILABLE" -> {
                                    jdbc.execute("ALTER TABLE session_security RENAME TO unavailable_login_security");
                                    jdbc.execute("ALTER TABLE SPRING_SESSION RENAME TO unavailable_login_native");
                                }
                                default -> throw new AssertionError("Unknown commit fault");
                            }
                        }
                    });
                }
            };
            var registration = new FilterRegistrationBean<OncePerRequestFilter>(filter);
            registration.setOrder(SessionRepositoryFilter.DEFAULT_ORDER + 1);
            return registration;
        }
    }
}
