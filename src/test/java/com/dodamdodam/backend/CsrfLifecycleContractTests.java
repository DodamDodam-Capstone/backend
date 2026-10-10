package com.dodamdodam.backend;

import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcPrint;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2LoginAuthenticationToken;
import org.springframework.security.oauth2.client.web.OAuth2LoginAuthenticationFilter;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import({TestcontainersConfiguration.class, CsrfContractTests.AuthProbe.class,
        CsrfLifecycleContractTests.ProtectedProbe.class})
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@SpringBootTest(properties = {"SESSION_COOKIE_SECURE=false", "SESSION_COOKIE_SAME_SITE=lax"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CsrfLifecycleContractTests {

    private static final String CSRF_PATH = "/api/v1/auth/csrf";
    private static final String CSRF_HEADER = "X-XSRF-TOKEN";
    private static final String CSRF_COOKIE = "XSRF-TOKEN";
    private static final String PROTECTED_PATH = "/api/v1/csrf-lifecycle/probe";
    private static final String PUBLIC_PATH = "/api/v1/auth/login";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FilterChainProxy securityFilterChain;

    private OAuth2LoginAuthenticationFilter oauthFilter;

    @BeforeEach
    void configureOAuthResult() {
        oauthFilter = securityFilterChain.getFilterChains().stream()
                .flatMap(chain -> chain.getFilters().stream())
                .filter(OAuth2LoginAuthenticationFilter.class::isInstance)
                .map(OAuth2LoginAuthenticationFilter.class::cast)
                .findFirst().orElseThrow();
        // 외부 인증 결과만 대체하고 필터의 세션·CSRF·성공 처리는 그대로 실행한다.
        oauthFilter.setAuthenticationManager(CsrfLifecycleContractTests::successfulAuthentication);
    }

    @Test
    void repeatedGetKeepsExistingToken() throws Exception {
        var first = issuedCsrf(null, null);
        var repeated = issuedCsrf(null, first.cookie());

        assertThat(repeated.token().equals(first.token())).as("existing token is retained").isTrue();
    }

    @Test
    void oauthSuccessClearsTokenAndAllowsRefresh() throws Exception {
        var beforeLogin = issuedCsrf(null, null);
        var login = login(beforeLogin.cookie());
        var session = (MockHttpSession) login.getRequest().getSession(false);

        assertClearedCookie(login.getResponse());
        var rejected = expectCsrfInvalid(post(PROTECTED_PATH)
                .session(session).header(CSRF_HEADER, beforeLogin.token()));
        var refreshed = issuedCsrf(session, rejected.getCookie(CSRF_COOKIE));
        assertThat(refreshed.token().equals(beforeLogin.token())).as("login token is replaced").isFalse();

        mockMvc.perform(post(PROTECTED_PATH).session(session)
                        .cookie(refreshed.cookie()).header(CSRF_HEADER, refreshed.token()))
                .andExpect(status().isNoContent());
    }

    @Test
    void logoutClearsTokenAndAllowsAnonymousRefresh() throws Exception {
        var login = login(issuedCsrf(null, null).cookie());
        var session = (MockHttpSession) login.getRequest().getSession(false);
        var beforeLogout = issuedCsrf(session, null);

        var logout = mockMvc.perform(post("/logout").session(session)
                        .cookie(beforeLogout.cookie()).header(CSRF_HEADER, beforeLogout.token()))
                .andExpect(status().isFound()).andReturn().getResponse();

        assertClearedCookie(logout);
        assertThat(session.isInvalid()).as("logout invalidates the session").isTrue();
        var rejected = expectCsrfInvalid(post(PUBLIC_PATH).header(CSRF_HEADER, beforeLogout.token()));
        var refreshed = issuedCsrf(null, rejected.getCookie(CSRF_COOKIE));
        assertThat(refreshed.token().equals(beforeLogout.token())).as("logout token is replaced").isFalse();

        mockMvc.perform(post(PUBLIC_PATH).cookie(refreshed.cookie())
                        .header(CSRF_HEADER, refreshed.token()))
                .andExpect(status().isNoContent());
    }

    @Test
    void oauthFailureKeepsExistingCsrf() throws Exception {
        oauthFilter.setAuthenticationManager(authentication -> {
            throw new OAuth2AuthenticationException(new OAuth2Error("access_denied"));
        });
        var original = issuedCsrf(null, null);
        var failure = completeOAuth(original.cookie());
        unauthenticated().match(failure);
        var session = (MockHttpSession) failure.getRequest().getSession(false);

        assertThat(failure.getResponse().getCookie(CSRF_COOKIE)).isNull();
        var retained = issuedCsrf(session, original.cookie());
        assertThat(retained.token().equals(original.token())).as("failed login retains the token").isTrue();
        mockMvc.perform(post(PUBLIC_PATH).session(session).cookie(retained.cookie())
                        .header(CSRF_HEADER, retained.token()))
                .andExpect(status().isNoContent());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "invalid-csrf-token"})
    void invalidLogoutKeepsAuthenticatedSession(String header) throws Exception {
        var login = login(issuedCsrf(null, null).cookie());
        var session = (MockHttpSession) login.getRequest().getSession(false);
        var csrf = issuedCsrf(session, null);
        var request = post("/logout").session(session).cookie(csrf.cookie());
        if (!header.isEmpty()) {
            request.header(CSRF_HEADER, header);
        }

        var rejected = expectCsrfInvalid(request);
        assertThat(rejected.getCookie(CSRF_COOKIE)).isNull();
        assertThat(session.isInvalid()).as("rejected logout keeps the session").isFalse();
        mockMvc.perform(post(PROTECTED_PATH).session(session)
                        .cookie(csrf.cookie()).header(CSRF_HEADER, csrf.token()))
                .andExpect(status().isNoContent());
    }

    private MvcResult login(Cookie cookie) throws Exception {
        var result = completeOAuth(cookie);
        authenticated().match(result);
        return result;
    }

    private MvcResult completeOAuth(Cookie cookie) throws Exception {
        var start = mockMvc.perform(get("/oauth2/authorization/google").cookie(cookie))
                .andExpect(status().isFound()).andReturn();
        var state = UriComponentsBuilder.fromUriString(start.getResponse().getRedirectedUrl())
                .build().getQueryParams().getFirst("state");
        state = URLDecoder.decode(state, StandardCharsets.UTF_8);
        var session = (MockHttpSession) start.getRequest().getSession(false);
        var callback = mockMvc.perform(get("/login/oauth2/code/google").session(session)
                        .cookie(cookie).param("code", "test-authorization-code").param("state", state))
                .andExpect(status().isFound()).andReturn();
        assertResponseHeaders(callback.getResponse());
        return callback;
    }

    private static Authentication successfulAuthentication(Authentication authentication) {
        var request = (OAuth2LoginAuthenticationToken) authentication;
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        var principal = new DefaultOAuth2User(authorities, Map.of("sub", "csrf-lifecycle-user"), "sub");
        var now = Instant.now();
        var accessToken = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER,
                "test-oauth-access-token", now, now.plusSeconds(300));
        return new OAuth2LoginAuthenticationToken(request.getClientRegistration(),
                request.getAuthorizationExchange(), principal, authorities, accessToken);
    }

    private IssuedCsrf issuedCsrf(MockHttpSession session, Cookie currentCookie) throws Exception {
        var request = get(CSRF_PATH);
        if (session != null) {
            request.session(session);
        }
        if (currentCookie != null) {
            request.cookie(currentCookie);
        }
        var response = mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.headerName").value(CSRF_HEADER))
                .andExpect(jsonPath("$.parameterName").value("_csrf"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andReturn().getResponse();
        var issuedCookie = response.getCookie(CSRF_COOKIE);
        var cookie = issuedCookie != null ? issuedCookie : currentCookie;
        assertThat(cookie).isNotNull();
        String token = JsonPath.read(response.getContentAsString(), "$.token");
        assertThat(token != null && !token.isBlank()).as("nonempty CSRF token").isTrue();
        assertThat(token.equals(cookie.getValue())).as("JSON token matches the CSRF cookie").isTrue();
        assertResponseHeaders(response);
        return new IssuedCsrf(cookie, token);
    }

    private MockHttpServletResponse expectCsrfInvalid(MockHttpServletRequestBuilder request) throws Exception {
        var response = mockMvc.perform(request.accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("CSRF_INVALID"))
                .andExpect(jsonPath("$.error.fields").isEmpty())
                .andReturn().getResponse();
        assertThat(JsonPath.<String>read(response.getContentAsString(), "$.error.requestId"))
                .isEqualTo(response.getHeader("X-Request-ID"));
        assertResponseHeaders(response);
        return response;
    }

    private static void assertClearedCookie(MockHttpServletResponse response) {
        var cookie = response.getCookie(CSRF_COOKIE);
        assertThat(cookie).isNotNull();
        assertThat(cookie.getMaxAge()).isZero();
        assertThat(cookie.getPath()).isEqualTo("/");
        assertThat(cookie.getDomain()).isNull();
        assertThat(cookie.getSecure()).isFalse();
        assertThat(cookie.isHttpOnly()).isFalse();
        assertThat(cookie.getAttribute("SameSite")).isEqualTo("Lax");
        assertResponseHeaders(response);
    }

    private static void assertResponseHeaders(MockHttpServletResponse response) {
        assertThat(response.getHeader("Cache-Control")).contains("no-store");
        assertThat(response.getHeader("X-Request-ID"))
                .matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }

    private record IssuedCsrf(Cookie cookie, String token) {
    }

    @TestConfiguration(proxyBeanMethods = false)
    @RestController
    static class ProtectedProbe {

        @PostMapping(PROTECTED_PATH)
        @ResponseStatus(HttpStatus.NO_CONTENT)
        void authenticatedPost() {
        }
    }
}
