package com.dodamdodam.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcPrint;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

@Import({TestcontainersConfiguration.class, CsrfContractTests.AuthProbe.class})
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@SpringBootTest(properties = "app.security.allowed-origins=http://localhost:3000,http://localhost:5173")
class CorsContractTests {

    private static final String ORIGIN = "http://localhost:5173";
    private static final String LOGIN_PATH = "/api/v1/auth/login";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void allowedPreflightDoesNotRequireCookiesOrCsrf() throws Exception {
        var response = mockMvc.perform(options(LOGIN_PATH)
                        .header("Origin", ORIGIN)
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "Content-Type,X-XSRF-TOKEN"))
                .andExpect(status().isOk())
                .andReturn().getResponse();

        assertThat(response.getHeader("Access-Control-Allow-Origin")).isEqualTo(ORIGIN);
        assertThat(response.getHeader("Access-Control-Allow-Credentials")).isEqualTo("true");
        assertThat(response.getHeader("Access-Control-Allow-Methods").split(",\\s*"))
                .contains("POST");
        assertThat(response.getHeader("Access-Control-Allow-Headers").split(",\\s*"))
                .contains("Content-Type", "X-XSRF-TOKEN");
        assertCommonHeaders(response);
    }

    @Test
    void allowedOriginCanReadCsrfResponseAndContractHeaders() throws Exception {
        var response = mockMvc.perform(get("/api/v1/auth/csrf").header("Origin", ORIGIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn().getResponse();

        assertCredentialedCors(response);
        assertCommonHeaders(response);
    }

    @Test
    void allowedOriginCanSendValidCsrfPost() throws Exception {
        var csrfResponse = mockMvc.perform(get("/api/v1/auth/csrf").header("Origin", ORIGIN))
                .andExpect(status().isOk()).andReturn().getResponse();
        String token = JsonPath.read(csrfResponse.getContentAsString(), "$.token");
        var cookie = csrfResponse.getCookie("XSRF-TOKEN");
        assertThat(cookie).isNotNull();

        var response = mockMvc.perform(post(LOGIN_PATH)
                        .header("Origin", ORIGIN)
                        .header("X-XSRF-TOKEN", token)
                        .cookie(cookie))
                .andExpect(status().isNoContent()).andReturn().getResponse();

        assertCredentialedCors(response);
        assertCommonHeaders(response);
    }

    @Test
    void allowedOriginStillReceivesCsrfErrorWithContractHeaders() throws Exception {
        var response = mockMvc.perform(post(LOGIN_PATH)
                        .header("Origin", ORIGIN)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("CSRF_INVALID"))
                .andReturn().getResponse();

        assertCredentialedCors(response);
        assertCommonHeaders(response);
        assertThat(JsonPath.<String>read(response.getContentAsString(), "$.error.requestId"))
                .isEqualTo(response.getHeader("X-Request-ID"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"https://untrusted.example", "http://localhost:5173.untrusted.example", "null"})
    void untrustedOriginIsRejectedForPreflightAndPost(String origin) throws Exception {
        var requests = List.of(
                options(LOGIN_PATH).header("Access-Control-Request-Method", "POST"),
                post(LOGIN_PATH)
        );
        for (var request : requests) {
            var response = mockMvc.perform(request.header("Origin", origin))
                    .andExpect(status().isForbidden()).andReturn().getResponse();

            assertThat(response.getHeader("Access-Control-Allow-Origin")).isNull();
            assertCommonHeaders(response);
        }
    }

    @Test
    void requestWithoutOriginKeepsPublicCsrfContract() throws Exception {
        mockMvc.perform(get("/api/v1/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.headerName").value("X-XSRF-TOKEN"));
    }

    private void assertCredentialedCors(MockHttpServletResponse response) {
        assertThat(response.getHeader("Access-Control-Allow-Origin")).isEqualTo(ORIGIN);
        assertThat(response.getHeader("Access-Control-Allow-Credentials")).isEqualTo("true");
        assertThat(response.getHeader("Access-Control-Expose-Headers")).isNotNull();
        assertThat(response.getHeader("Access-Control-Expose-Headers").split(",\\s*"))
                .contains("Location", "X-Request-ID", "Retry-After");
    }

    private void assertCommonHeaders(MockHttpServletResponse response) {
        assertThat(response.getHeader("Cache-Control")).contains("no-store");
        assertThat(response.getHeader("X-Request-ID"))
                .matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }
}
