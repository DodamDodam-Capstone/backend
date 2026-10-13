package com.dodamdodam.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Import({TestcontainersConfiguration.class, CsrfContractTests.AuthProbe.class})
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@SpringBootTest
class CsrfContractTests {

    private static final String LOGIN_PATH = "/api/v1/auth/login";
    private static final String CSRF_HEADER = "X-XSRF-TOKEN";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void csrfEndpointPreservesPublicResponseContract() throws Exception {
        mockMvc.perform(get("/api/v1/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.headerName").value(CSRF_HEADER))
                .andExpect(jsonPath("$.parameterName").value("_csrf"))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/v1/auth/email-verifications",
            "/api/v1/auth/email-verifications/verify",
            "/api/v1/auth/signup",
            LOGIN_PATH,
            "/api/v1/auth/password-resets"
    })
    void publicAuthAcceptsIssuedCookieAndHeader(String path) throws Exception {
        var csrf = issuedCsrf();

        mockMvc.perform(post(path)
                        .accept(MediaType.APPLICATION_JSON)
                        .cookie(csrf.cookie())
                        .header(CSRF_HEADER, csrf.token()))
                .andExpect(status().isNoContent());
    }

    @Test
    void successfulResponseUsesServerGeneratedRequestId() throws Exception {
        var clientRequestId = "00000000-0000-0000-0000-000000000000";
        var response = mockMvc.perform(get("/api/v1/auth/csrf")
                        .header("X-Request-ID", clientRequestId))
                .andExpect(status().isOk())
                .andReturn().getResponse();

        assertThat(response.getHeader("X-Request-ID"))
                .matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")
                .isNotEqualTo(clientRequestId);
        assertThat(response.getHeader("Cache-Control")).contains("no-store");
    }

    @Test
    void authenticatedRequestAcceptsIssuedCookieAndHeader() throws Exception {
        var csrf = issuedCsrf();

        mockMvc.perform(post(LOGIN_PATH)
                        .with(user("csrf-contract-user"))
                        .accept(MediaType.APPLICATION_JSON)
                        .cookie(csrf.cookie())
                        .header(CSRF_HEADER, csrf.token()))
                .andExpect(status().isNoContent());
    }

    @Test
    void missingHeaderReturnsCsrfErrorContract() throws Exception {
        var csrf = issuedCsrf();

        expectCsrfInvalid(post(LOGIN_PATH).cookie(csrf.cookie()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "invalid-csrf-token"})
    void emptyOrMismatchedHeaderReturnsCsrfErrorContract(String token) throws Exception {
        var csrf = issuedCsrf();

        expectCsrfInvalid(post(LOGIN_PATH)
                .cookie(csrf.cookie())
                .header(CSRF_HEADER, token));
    }

    @Test
    void headerWithoutCookieReturnsCsrfErrorContract() throws Exception {
        var csrf = issuedCsrf();

        expectCsrfInvalid(post(LOGIN_PATH).header(CSRF_HEADER, csrf.token()));
    }

    @Test
    void queryParameterCannotReplaceCsrfHeader() throws Exception {
        var csrf = issuedCsrf();

        expectCsrfInvalid(post(LOGIN_PATH)
                .cookie(csrf.cookie())
                .queryParam("_csrf", csrf.token()));
    }

    @Test
    void formParameterCannotReplaceCsrfHeader() throws Exception {
        var csrf = issuedCsrf();

        expectCsrfInvalid(post(LOGIN_PATH)
                .cookie(csrf.cookie())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("_csrf", csrf.token()));
    }

    private IssuedCsrf issuedCsrf() throws Exception {
        var response = mockMvc.perform(get("/api/v1/auth/csrf"))
                .andExpect(status().isOk())
                .andReturn().getResponse();
        var cookie = response.getCookie("XSRF-TOKEN");
        assertThat(cookie).isNotNull();
        String token = JsonPath.read(response.getContentAsString(), "$.token");
        assertThat(token).isNotBlank();
        return new IssuedCsrf(cookie, token);
    }

    private void expectCsrfInvalid(MockHttpServletRequestBuilder request) throws Exception {
        var response = mockMvc.perform(request.accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("CSRF_INVALID"))
                .andExpect(jsonPath("$.error.message").isNotEmpty())
                .andExpect(jsonPath("$.error.fields").isArray())
                .andExpect(jsonPath("$.error.fields").isEmpty())
                .andReturn().getResponse();

        var requestId = response.getHeader("X-Request-ID");
        assertThat(requestId).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
        assertThat(JsonPath.<String>read(response.getContentAsString(), "$.error.requestId"))
                .isEqualTo(requestId);
        assertThat(response.getHeader("Cache-Control")).contains("no-store");
    }

    private record IssuedCsrf(Cookie cookie, String token) {
    }

    @TestConfiguration(proxyBeanMethods = false)
    @RestController
    static class AuthProbe {

        @PostMapping({
                "/api/v1/auth/email-verifications",
                "/api/v1/auth/email-verifications/verify",
                "/api/v1/auth/signup",
                LOGIN_PATH,
                "/api/v1/auth/password-resets"
        })
        @ResponseStatus(HttpStatus.NO_CONTENT)
        void publicAuth() {
        }
    }
}
