package com.dodamdodam.backend;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

class CookieContractTests {

    private static final String SESSION_PROBE_PATH = "/login/session-cookie-probe";

    @Nested
    @Import({TestcontainersConfiguration.class, SessionProbe.class})
    @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
            "SESSION_COOKIE_SECURE=false", "SESSION_COOKIE_SAME_SITE=lax"
    })
    class LocalHttp {

        @LocalServerPort
        private int port;

        @Test
        void sessionCookieIsHttpOnlyHostOnlyWithLaxPolicy() throws Exception {
            assertThat(cookieAttributes(port, SESSION_PROBE_PATH, "JSESSIONID"))
                    .contains("Path=/", "HttpOnly", "SameSite=Lax")
                    .doesNotContain("Secure")
                    .noneMatch(attribute -> attribute.startsWith("Domain="));
        }

        @Test
        void csrfCookieKeepsJavaScriptAccessWithLaxPolicy() throws Exception {
            assertThat(cookieAttributes(port, "/api/v1/auth/csrf", "XSRF-TOKEN"))
                    .contains("Path=/", "SameSite=Lax")
                    .doesNotContain("HttpOnly", "Secure")
                    .noneMatch(attribute -> attribute.startsWith("Domain="));
        }
    }

    @Nested
    @Import({TestcontainersConfiguration.class, SessionProbe.class})
    @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
            "SESSION_COOKIE_SECURE=true", "SESSION_COOKIE_SAME_SITE=none"
    })
    class SecureCookies {

        @LocalServerPort
        private int port;

        @Test
        void sessionCookieUsesSecureNonePolicyAndRemainsHttpOnly() throws Exception {
            assertThat(cookieAttributes(port, SESSION_PROBE_PATH, "JSESSIONID"))
                    .contains("Path=/", "HttpOnly", "Secure", "SameSite=None")
                    .noneMatch(attribute -> attribute.startsWith("Domain="));
        }

        @Test
        void csrfCookieUsesSecureNonePolicyWithoutHttpOnly() throws Exception {
            assertThat(cookieAttributes(port, "/api/v1/auth/csrf", "XSRF-TOKEN"))
                    .contains("Path=/", "Secure", "SameSite=None")
                    .doesNotContain("HttpOnly")
                    .noneMatch(attribute -> attribute.startsWith("Domain="));
        }
    }

    private static Set<String> cookieAttributes(int port, String path, String cookieName) throws Exception {
        try (var client = HttpClient.newHttpClient()) {
            var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build();
            var response = client.send(request, HttpResponse.BodyHandlers.discarding());
            assertThat(response.statusCode()).isEqualTo(200);
            var cookie = response.headers().allValues("Set-Cookie").stream()
                    .filter(value -> value.startsWith(cookieName + "="))
                    .findFirst();
            assertThat(cookie.isPresent()).as("%s must be issued", cookieName).isTrue();
            // Cookie values must never appear in assertion failures or logs.
            return Arrays.stream(cookie.orElseThrow().split(";"))
                    .skip(1).map(String::trim).collect(Collectors.toSet());
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    @RestController
    static class SessionProbe {

        @GetMapping(SESSION_PROBE_PATH)
        void createSession(HttpServletRequest request) {
            request.getSession();
        }
    }
}
