package com.dodamdodam.backend.global.config;

import com.dodamdodam.backend.global.auth.CsrfAccessDeniedHandler;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.server.Cookie.SameSite;
import org.springframework.boot.web.server.autoconfigure.ServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import org.springframework.security.web.access.DelegatingAccessDeniedHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
@EnableConfigurationProperties(AppSecurityProperties.class)
public class SecurityConfig {

    @Bean
    SecurityFilterChain applicationSecurityFilterChain(
            HttpSecurity http,
            AppSecurityProperties properties,
            CsrfAccessDeniedHandler csrfAccessDeniedHandler,
            CookieCsrfTokenRepository csrfTokenRepository
    ) throws Exception {
        var csrfRequestHandler = new CsrfTokenRequestAttributeHandler() {
            @Override
            public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken csrfToken) {
                return request.getHeader(csrfToken.getHeaderName());
            }
        };
        var accessDeniedHandlers = new LinkedHashMap<Class<? extends AccessDeniedException>, AccessDeniedHandler>();
        accessDeniedHandlers.put(CsrfException.class, csrfAccessDeniedHandler);

        http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/",
                                "/error",
                                "/actuator/health",
                                "/actuator/health/**",
                                "/api/v1/auth/csrf",
                                "/oauth2/**",
                                "/login/**"
                        ).permitAll()
                        .requestMatchers(HttpMethod.POST,
                                "/api/v1/auth/email-verifications",
                                "/api/v1/auth/email-verifications/verify",
                                "/api/v1/auth/signup",
                                "/api/v1/auth/login",
                                "/api/v1/auth/password-resets"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokenRepository)
                        .csrfTokenRequestHandler(csrfRequestHandler)
                )
                .exceptionHandling(exceptions -> exceptions.accessDeniedHandler(
                        new DelegatingAccessDeniedHandler(accessDeniedHandlers, new AccessDeniedHandlerImpl())
                ))
                .oauth2Login(oauth -> oauth
                        .defaultSuccessUrl(properties.loginSuccessUrl(), true)
                )
                .logout(logout -> logout
                        .logoutSuccessUrl(properties.logoutSuccessUrl())
                );

        return http.build();
    }

    @Bean
    CookieCsrfTokenRepository csrfTokenRepository(ServerProperties serverProperties) {
        var sessionCookie = serverProperties.getServlet().getSession().getCookie();
        if (sessionCookie.getSameSite() == SameSite.NONE && !Boolean.TRUE.equals(sessionCookie.getSecure())) {
            throw new IllegalArgumentException("SameSite=None requires Secure=true");
        }
        var repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repository.setCookieCustomizer(cookie -> cookie
                .path("/")
                .secure(Boolean.TRUE.equals(sessionCookie.getSecure()))
                .sameSite(sessionCookie.getSameSite().attributeValue())
        );
        return repository;
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(AppSecurityProperties properties) {
        var configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(properties.allowedOrigins());
        for (var origin : configuration.getAllowedOrigins()) {
            if (origin.contains("*") || origin.equalsIgnoreCase("null")) {
                throw new IllegalArgumentException("Wildcard and null CORS origins are not allowed");
            }
        }
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Location", "X-Request-ID", "Retry-After"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}
