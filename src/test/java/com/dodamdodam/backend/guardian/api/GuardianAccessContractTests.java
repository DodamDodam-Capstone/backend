package com.dodamdodam.backend.guardian.api;

import com.dodamdodam.backend.TestcontainersConfiguration;
import com.dodamdodam.backend.auth.session.AuthenticatedSessionService;
import com.dodamdodam.backend.child.application.ChildService;
import com.dodamdodam.backend.child.dto.ChildView;
import com.dodamdodam.backend.guardian.application.GuardianAccessService;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcPrint;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import({TestcontainersConfiguration.class, GuardianAccessContractTests.Probe.class})
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@SpringBootTest(properties = {"spring.session.jdbc.cleanup-cron=-",
        "logging.level.org.springframework.security=INFO", "logging.level.org.springframework.session=INFO"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class GuardianAccessContractTests {

    private static final String PATH = "/api/v1/contract/guardian";

    @Autowired private MockMvc mvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private JdbcIndexedSessionRepository sessions;
    @Autowired private CookieSerializer cookies;

    @BeforeEach
    @AfterEach
    void clearDatabase() {
        jdbc.update("DELETE FROM SPRING_SESSION");
        jdbc.update("DELETE FROM guardian_pins");
        jdbc.update("DELETE FROM email_verifications");
        jdbc.update("DELETE FROM children");
        jdbc.update("DELETE FROM session_security");
        jdbc.update("DELETE FROM accounts");
    }

    @Test
    void missingSessionRequiresAuthentication() throws Exception {
        error(get(PATH + "/children/" + UUID.randomUUID()), 401, "AUTHENTICATION_REQUIRED");
    }

    @Test
    void ownerWithCurrentGuardianGrantCanReadChild() throws Exception {
        var current = authenticated();
        var childId = child(current.accountId());
        grant(current);

        var response = mvc.perform(get(PATH + "/children/" + childId).cookie(current.cookie()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.childId").value(childId.toString()))
                .andReturn().getResponse();

        Map<String, Object> data = JsonPath.read(response.getContentAsString(), "$.data");
        assertThat(data).containsOnlyKeys("childId", "name", "nickname", "birthDate", "gender", "interests",
                "characterId", "createdAt");
    }

    @ParameterizedTest
    @ValueSource(strings = {"MISSING", "EXPIRED", "OLD_PIN", "NO_PIN"})
    void invalidGuardianGrantReturns403WithoutLoggingOut(String state) throws Exception {
        var current = authenticated();
        var childId = child(current.accountId());
        if (!state.equals("MISSING")) {
            grant(current);
            switch (state) {
                case "EXPIRED" -> jdbc.update("UPDATE session_security SET guardian_unlocked_until = "
                        + "clock_timestamp() - interval '1 second' WHERE id = ?", current.contextId());
                case "OLD_PIN" -> jdbc.update("UPDATE guardian_pins SET pin_version = 2 WHERE account_id = ?",
                        current.accountId());
                case "NO_PIN" -> jdbc.update("DELETE FROM guardian_pins WHERE account_id = ?", current.accountId());
                default -> throw new IllegalArgumentException(state);
            }
        }

        error(get(PATH + "/children/" + childId).cookie(current.cookie()), 403, "GUARDIAN_UNLOCK_REQUIRED");
        mvc.perform(get(PATH + "/authenticated").cookie(current.cookie())).andExpect(status().isNoContent());
    }

    @Test
    void unownedAndMissingChildHaveSame404BeforeGuardianCheck() throws Exception {
        var owner = authenticated();
        var other = authenticated();
        var childId = child(owner.accountId());

        var unowned = error(get(PATH + "/children/" + childId).cookie(other.cookie()), 404, "RESOURCE_NOT_FOUND");
        var missing = error(get(PATH + "/children/" + UUID.randomUUID()).cookie(other.cookie()),
                404, "RESOURCE_NOT_FOUND");

        assertThat(JsonPath.<String>read(unowned.getContentAsString(), "$.error.message"))
                .isEqualTo(JsonPath.read(missing.getContentAsString(), "$.error.message"));
        assertThat(unowned.getContentAsString()).doesNotContain(childId.toString(), "가나", "별이");
    }

    @Test
    void invalidUuidReturns400BeforeGuardianCheck() throws Exception {
        error(get(PATH + "/children/private-input").cookie(authenticated().cookie()),
                400, "VALIDATION_FAILED", "childId");
    }

    @Test
    void missingCsrfPrecedesAuthenticationAndParsing() throws Exception {
        error(post(PATH + "/children/private-input").contentType("application/json").content("{"),
                403, "CSRF_INVALID");
    }

    @Test
    void missingSessionWithValidCsrfPrecedesParsing() throws Exception {
        var csrf = csrf(null);
        error(post(PATH + "/children/private-input").contentType("application/json").content("{")
                .cookie(csrf.cookie()).header("X-XSRF-TOKEN", csrf.token()), 401, "AUTHENTICATION_REQUIRED");
    }

    @Test
    void malformedJsonReturns400BeforeGuardianCheck() throws Exception {
        var current = authenticated();
        var csrf = csrf(current.cookie());
        error(post(PATH + "/children/" + child(current.accountId())).contentType("application/json")
                .content("{\"private-input").cookie(current.cookie(), csrf.cookie())
                .header("X-XSRF-TOKEN", csrf.token()), 400, "VALIDATION_FAILED", "body");
    }

    @Test
    void anotherSessionCannotUseGrantOrForgedNativeSessionAttributes() throws Exception {
        var owner = authenticated();
        var other = session(owner.accountId());
        var childId = child(owner.accountId());
        grant(owner);

        mvc.perform(get(PATH + "/children/" + childId).cookie(owner.cookie())).andExpect(status().isOk());
        error(get(PATH + "/children/" + childId).cookie(other.cookie()), 403, "GUARDIAN_UNLOCK_REQUIRED");
        // 늦게 저장된 HTTP 세션 속성은 DB의 해당 문맥 권한을 대신하지 못한다.
        other.session().setAttribute("guardian_unlocked_until", "2099-01-01T00:00:00Z");
        other.session().setAttribute("guardian_pin_version", 1L);
        error(get(PATH + "/children/" + childId).cookie(other.cookie()), 403, "GUARDIAN_UNLOCK_REQUIRED");
    }

    @ParameterizedTest
    @CsvSource({"AUTH,503,AUTH_STATE_UNAVAILABLE", "GUARDIAN,503,AUTH_STATE_UNAVAILABLE",
            "CHILD,500,INTERNAL_ERROR"})
    void storageFailuresKeepTheirOwnStatus(String storage, int status, String code) throws Exception {
        var current = authenticated();
        var childId = child(current.accountId());
        grant(current);

        error(get(PATH + "/failure/" + storage + "/" + childId).cookie(current.cookie()), status, code);
    }

    private MockHttpServletResponse error(MockHttpServletRequestBuilder call, int status, String code,
                                          String... fields) throws Exception {
        var response = mvc.perform(call.header("X-Request-ID", "client-controlled"))
                .andExpect(status().is(status)).andExpect(jsonPath("$.error.code").value(code))
                .andReturn().getResponse();
        Map<String, Object> envelope = JsonPath.read(response.getContentAsString(), "$");
        Map<String, Object> error = JsonPath.read(response.getContentAsString(), "$.error");
        List<Map<String, Object>> errors = JsonPath.read(response.getContentAsString(), "$.error.fields");
        assertThat(envelope).containsOnlyKeys("error");
        assertThat(error).containsOnlyKeys("code", "message", "requestId", "fields");
        assertThat((String) error.get("message")).isNotBlank();
        assertThat(error.get("requestId")).isEqualTo(response.getHeader("X-Request-ID"));
        assertThat(errors).extracting(entry -> entry.get("field")).containsExactlyElementsOf(List.of(fields));
        for (var field : errors) {
            assertThat(field).containsOnlyKeys("field", "reason");
            assertThat((String) field.get("reason")).isNotBlank();
        }
        assertThat(response.getContentAsString()).doesNotContain("private-input", "SELECT ", "pin_hash", "Exception");
        assertThat(response.getHeader("Location")).isNull();
        assertThat(response.getHeader("Cache-Control")).contains("no-store");
        assertThat(response.getHeader("X-Request-ID"))
                .matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
        assertThat(response.getContentType()).startsWith("application/json");
        return response;
    }

    private Csrf csrf(Cookie cookie) throws Exception {
        var call = get("/api/v1/auth/csrf");
        if (cookie != null) {
            call.cookie(cookie);
        }
        var response = mvc.perform(call).andExpect(status().isOk()).andReturn().getResponse();
        return new Csrf(response.getCookie("XSRF-TOKEN"), JsonPath.read(response.getContentAsString(), "$.token"));
    }

    private Fixture authenticated() {
        var accountId = UUID.randomUUID();
        jdbc.update("INSERT INTO accounts (id, email) VALUES (?, ?)", accountId, accountId + "@example.invalid");
        return session(accountId);
    }

    private Fixture session(UUID accountId) {
        var contextId = UUID.randomUUID();
        jdbc.update("INSERT INTO session_security (id, account_id, session_version) VALUES (?, ?, 0)", contextId, accountId);
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
        return new Fixture(accountId, contextId, session, MockCookie.parse(response.getHeader("Set-Cookie")));
    }

    private UUID child(UUID accountId) {
        var childId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO children (id, account_id, name, nickname, birth_date, gender, character_id)
                VALUES (?, ?, '가나', '별이', '2020-05-12', 'FEMALE', 'test-character')
                """, childId, accountId);
        return childId;
    }

    private void grant(Fixture current) {
        jdbc.update("INSERT INTO guardian_pins (account_id, pin_hash) VALUES (?, 'synthetic-hash')", current.accountId());
        jdbc.update("UPDATE session_security SET guardian_unlocked_until = clock_timestamp() + interval '15 minutes', "
                + "guardian_pin_version = 1 WHERE id = ?", current.contextId());
    }

    private record Fixture(UUID accountId, UUID contextId, Session session, Cookie cookie) { }
    private record Csrf(Cookie cookie, String token) { }

    @TestConfiguration(proxyBeanMethods = false)
    @RestController
    static class Probe {
        private final AuthenticatedSessionService authentication;
        private final ChildService children;
        private final GuardianAccessService guardian;
        private final JdbcTemplate jdbc;

        Probe(AuthenticatedSessionService authentication, ChildService children,
              GuardianAccessService guardian, JdbcTemplate jdbc) {
            this.authentication = authentication;
            this.children = children;
            this.guardian = guardian;
            this.jdbc = jdbc;
        }

        @GetMapping(PATH + "/children/{childId}")
        Map<String, ChildView> read(HttpServletRequest request, @PathVariable UUID childId) {
            var authenticated = authentication.requireAuthenticatedSession(request);
            var child = children.requireChildAccess(authenticated.accountId(), childId);
            guardian.requireGuardianAccess(request, authenticated);
            return Map.of("data", child);
        }

        @PostMapping(PATH + "/children/{childId}")
        Map<String, ChildView> postChild(HttpServletRequest request, @PathVariable UUID childId,
                                        @RequestBody Map<String, String> body) {
            return read(request, childId);
        }

        @GetMapping(PATH + "/authenticated")
        @ResponseStatus(HttpStatus.NO_CONTENT)
        void authenticated(HttpServletRequest request) {
            authentication.requireAuthenticatedSession(request);
        }

        @GetMapping(PATH + "/failure/{storage}/{childId}")
        Map<String, ChildView> failure(HttpServletRequest request, @PathVariable String storage,
                                      @PathVariable UUID childId) {
            // Security 통과 후 각 저장소만 중단해 실제 업무 호출 경로의 오류 분류를 검증한다.
            var table = switch (storage) {
                case "AUTH" -> "session_security";
                case "GUARDIAN" -> "guardian_pins";
                case "CHILD" -> "children";
                default -> throw new IllegalArgumentException(storage);
            };
            jdbc.execute("ALTER TABLE " + table + " RENAME TO guardian_test_unavailable");
            try {
                return read(request, childId);
            }
            finally {
                jdbc.execute("ALTER TABLE guardian_test_unavailable RENAME TO " + table);
            }
        }
    }
}
