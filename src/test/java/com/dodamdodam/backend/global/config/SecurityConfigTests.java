package com.dodamdodam.backend.global.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.web.server.Cookie.SameSite;
import org.springframework.boot.web.server.autoconfigure.ServerProperties;
import org.springframework.mock.web.MockHttpServletRequest;

class SecurityConfigTests {

    @ParameterizedTest
    @ValueSource(strings = {"*", "https://*.example.com", "null", "NULL", "Null", "null/"})
    void unsafeOriginConfigurationFailsImmediately(String origin) {
        assertThatIllegalArgumentException().isThrownBy(() ->
                new SecurityConfig().corsConfigurationSource(properties(origin)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"http://localhost:3000", "http://localhost:5173"})
    void explicitOriginsProduceCorsConfiguration(String origin) {
        var source = new SecurityConfig().corsConfigurationSource(properties(origin));
        var configuration = source.getCorsConfiguration(new MockHttpServletRequest("GET", "/api/v1/auth/csrf"));
        assertThat(configuration).isNotNull();
        assertThat(configuration.checkOrigin(origin)).isEqualTo(origin);
        assertThat(configuration.getAllowCredentials()).isTrue();
    }

    @Test
    void sameSiteNoneWithoutSecureFailsBeforeServingRequests() {
        var serverProperties = cookieProperties(false, SameSite.NONE);

        assertThatIllegalArgumentException().isThrownBy(() ->
                new SecurityConfig().csrfTokenRepository(serverProperties));
    }

    @ParameterizedTest
    @CsvSource({"false,LAX", "true,LAX", "true,NONE", "true,STRICT"})
    void validCookiePoliciesAreAccepted(boolean secure, SameSite sameSite) {
        assertThatCode(() -> new SecurityConfig().csrfTokenRepository(cookieProperties(secure, sameSite)))
                .doesNotThrowAnyException();
    }

    private ServerProperties cookieProperties(boolean secure, SameSite sameSite) {
        var serverProperties = new ServerProperties();
        var cookie = serverProperties.getServlet().getSession().getCookie();
        cookie.setSecure(secure);
        cookie.setSameSite(sameSite);
        return serverProperties;
    }

    private AppSecurityProperties properties(String origin) {
        return new AppSecurityProperties(List.of(origin),
                "http://localhost:5173/oauth/callback", "http://localhost:5173");
    }
}
