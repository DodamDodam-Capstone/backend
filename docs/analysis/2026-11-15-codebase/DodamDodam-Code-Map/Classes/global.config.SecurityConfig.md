---
tags: [backend, code-map, global]
aliases: ["SecurityConfig"]
---
# SecurityConfig

`com.dodamdodam.backend.global.config.SecurityConfig`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|시작]] → [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/global|기능 개요]] → 이 클래스 · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/90-Reference|전체 참조 자료]]

## 클래스 요약

CORS·CSRF·OAuth와 사용자 정의 필터를 Spring Security 체인에 연결한다.

- 원본: `src/main/java/com/dodamdodam/backend/global/config/SecurityConfig.java:44`
- 작성 메서드/생성자 4개 · 자동 생성 1개

## 메서드 목록

| 메서드 | 하는 일 |
| --- | --- |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.config.SecurityConfig#applicationSecurityFilterChain m8a28e16ae80b5e1e\|applicationSecurityFilterChain]] | 공통 인증·CSRF와 Part 1·2 전송 검사를 순서대로 연결한다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.config.SecurityConfig#csrfTokenRequestHandler mbc54a3db2ec31eab\|csrfTokenRequestHandler]] | 아래 구현과 호출 관계에서 확인 |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.config.SecurityConfig#csrfTokenRepository m27b2106272e43ff4\|csrfTokenRepository]] | 아래 구현과 호출 관계에서 확인 |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.config.SecurityConfig#corsConfigurationSource mf369b7a9aa69415d\|corsConfigurationSource]] | 아래 구현과 호출 관계에서 확인 |

> [!example]- 생성자·자동 생성 멤버 1개
>
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.config.SecurityConfig#생성자 m6fe7101596297a01|public SecurityConfig() [자동]]]

## applicationSecurityFilterChain m8a28e16ae80b5e1e

공통 인증·CSRF와 Part 1·2 전송 검사를 순서대로 연결한다.

```java
org.springframework.security.web.SecurityFilterChain applicationSecurityFilterChain(org.springframework.security.config.annotation.web.builders.HttpSecurity http, com.dodamdodam.backend.global.config.AppSecurityProperties properties, com.dodamdodam.backend.global.auth.CsrfAccessDeniedHandler csrfAccessDeniedHandler, org.springframework.security.web.csrf.CookieCsrfTokenRepository csrfTokenRepository, com.dodamdodam.backend.auth.session.AuthenticatedSessionService authenticatedSessionService, com.dodamdodam.backend.auth.session.AuthStateErrorHandler authStateErrorHandler, com.dodamdodam.backend.auth.session.LoginSessionAuthenticationStrategy loginSessionAuthenticationStrategy, org.springframework.security.web.context.SecurityContextRepository securityContextRepository, org.springframework.security.web.csrf.CsrfTokenRequestHandler csrfRequestHandler, tools.jackson.databind.json.JsonMapper jsonMapper, com.dodamdodam.backend.auth.application.AuthRateLimitService rateLimits, com.dodamdodam.backend.auth.infrastructure.EmailVerificationKeys verificationKeys)
```

설정: `@org.springframework.context.annotation.Bean`

### 이 메서드를 호출하는 곳

- 프레임워크 진입: Spring 컨테이너 Bean 생성

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthenticatedSessionService#requireAuthenticatedSession mad23075b25d06a28|AuthenticatedSessionService.requireAuthenticatedSession]] — 호출 · L87
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.config.AppSecurityProperties#loginSuccessUrl m3702e77020b91861|AppSecurityProperties.loginSuccessUrl]] — 호출 · L111
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.web.GlobalApiRateLimitFilter#생성자 m3c9dda322a7264a8|GlobalApiRateLimitFilter.생성자]] — 생성 · L103
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.web.Part1JsonRequestGuardFilter#생성자 m0f91a8b84e7b5b2c|Part1JsonRequestGuardFilter.생성자]] — 생성 · L101
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.web.Part2JsonRequestGuardFilter#생성자 mc3d3d138ee1d9ea9|Part2JsonRequestGuardFilter.생성자]] — 생성 · L102

### 외부 라이브러리 호출

- `java.util.HashMap#put(K,V)`
- `java.util.LinkedHashMap#<init>()`
- `org.springframework.security.authorization.AuthorizationDecision#<init>(boolean)`
- `org.springframework.security.config.Customizer#withDefaults()`
- `org.springframework.security.config.annotation.AbstractSecurityBuilder#build()`
- `org.springframework.security.config.annotation.web.AbstractRequestMatcherRegistry#anyRequest()`
- `org.springframework.security.config.annotation.web.AbstractRequestMatcherRegistry#requestMatchers(java.lang.String[])`
- `org.springframework.security.config.annotation.web.AbstractRequestMatcherRegistry#requestMatchers(org.springframework.http.HttpMethod,java.lang.String[])`
- `org.springframework.security.config.annotation.web.builders.HttpSecurity#addFilterBefore(jakarta.servlet.Filter,java.lang.Class<? extends jakarta.servlet.Filter>)`
- `org.springframework.security.config.annotation.web.builders.HttpSecurity#authorizeHttpRequests(org.springframework.security.config.Customizer<org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer<org.springframework.security.config.annotation.web.builders.HttpSecurity>.AuthorizationManagerRequestMatcherRegistry>)`
- `org.springframework.security.config.annotation.web.builders.HttpSecurity#cors(org.springframework.security.config.Customizer<org.springframework.security.config.annotation.web.configurers.CorsConfigurer<org.springframework.security.config.annotation.web.builders.HttpSecurity>>)`
- `org.springframework.security.config.annotation.web.builders.HttpSecurity#csrf(org.springframework.security.config.Customizer<org.springframework.security.config.annotation.web.configurers.CsrfConfigurer<org.springframework.security.config.annotation.web.builders.HttpSecurity>>)`
- `org.springframework.security.config.annotation.web.builders.HttpSecurity#exceptionHandling(org.springframework.security.config.Customizer<org.springframework.security.config.annotation.web.configurers.ExceptionHandlingConfigurer<org.springframework.security.config.annotation.web.builders.HttpSecurity>>)`
- `org.springframework.security.config.annotation.web.builders.HttpSecurity#logout(org.springframework.security.config.Customizer<org.springframework.security.config.annotation.web.configurers.LogoutConfigurer<org.springframework.security.config.annotation.web.builders.HttpSecurity>>)`
- `org.springframework.security.config.annotation.web.builders.HttpSecurity#oauth2Login(org.springframework.security.config.Customizer<org.springframework.security.config.annotation.web.configurers.oauth2.client.OAuth2LoginConfigurer<org.springframework.security.config.annotation.web.builders.HttpSecurity>>)`
- `org.springframework.security.config.annotation.web.builders.HttpSecurity#requestCache(org.springframework.security.config.Customizer<org.springframework.security.config.annotation.web.configurers.RequestCacheConfigurer<org.springframework.security.config.annotation.web.builders.HttpSecurity>>)`
- `org.springframework.security.config.annotation.web.builders.HttpSecurity#securityContext(org.springframework.security.config.Customizer<org.springframework.security.config.annotation.web.configurers.SecurityContextConfigurer<org.springframework.security.config.annotation.web.builders.HttpSecurity>>)`
- `org.springframework.security.config.annotation.web.builders.HttpSecurity#sessionManagement(org.springframework.security.config.Customizer<org.springframework.security.config.annotation.web.configurers.SessionManagementConfigurer<org.springframework.security.config.annotation.web.builders.HttpSecurity>>)`
- `org.springframework.security.config.annotation.web.configurers.AbstractAuthenticationFilterConfigurer#defaultSuccessUrl(java.lang.String,boolean)`
- `org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer#disable()`
- `org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer.AuthorizedUrl#access(org.springframework.security.authorization.AuthorizationManager<? super org.springframework.security.web.access.intercept.RequestAuthorizationContext>)`
- `org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer.AuthorizedUrl#permitAll()`
- `org.springframework.security.config.annotation.web.configurers.CsrfConfigurer#csrfTokenRepository(org.springframework.security.web.csrf.CsrfTokenRepository)`
- `org.springframework.security.config.annotation.web.configurers.CsrfConfigurer#csrfTokenRequestHandler(org.springframework.security.web.csrf.CsrfTokenRequestHandler)`
- `org.springframework.security.config.annotation.web.configurers.CsrfConfigurer#sessionAuthenticationStrategy(org.springframework.security.web.authentication.session.SessionAuthenticationStrategy)`
- `org.springframework.security.config.annotation.web.configurers.ExceptionHandlingConfigurer#accessDeniedHandler(org.springframework.security.web.access.AccessDeniedHandler)`
- `org.springframework.security.config.annotation.web.configurers.ExceptionHandlingConfigurer#authenticationEntryPoint(org.springframework.security.web.AuthenticationEntryPoint)`
- `org.springframework.security.config.annotation.web.configurers.RequestCacheConfigurer#disable()`
- `org.springframework.security.config.annotation.web.configurers.SecurityContextConfigurer#securityContextRepository(org.springframework.security.web.context.SecurityContextRepository)`
- `org.springframework.security.config.annotation.web.configurers.SessionManagementConfigurer#sessionAuthenticationStrategy(org.springframework.security.web.authentication.session.SessionAuthenticationStrategy)`
- `org.springframework.security.web.access.AccessDeniedHandlerImpl#<init>()`
- `org.springframework.security.web.access.DelegatingAccessDeniedHandler#<init>(java.util.LinkedHashMap<java.lang.Class<? extends org.springframework.security.access.AccessDeniedException>,org.springframework.security.web.access.AccessDeniedHandler>,org.springframework.security.web.access.AccessDeniedHandler)`
- `org.springframework.security.web.access.intercept.RequestAuthorizationContext#getRequest()`
- `org.springframework.security.web.authentication.session.NullAuthenticatedSessionStrategy#<init>()`

### 소스 코드

`src/main/java/com/dodamdodam/backend/global/config/SecurityConfig.java:50`

```java
@Bean
SecurityFilterChain applicationSecurityFilterChain(
        HttpSecurity http,
        AppSecurityProperties properties,
        CsrfAccessDeniedHandler csrfAccessDeniedHandler,
        CookieCsrfTokenRepository csrfTokenRepository,
        AuthenticatedSessionService authenticatedSessionService,
        AuthStateErrorHandler authStateErrorHandler,
        LoginSessionAuthenticationStrategy loginSessionAuthenticationStrategy,
        SecurityContextRepository securityContextRepository,
        CsrfTokenRequestHandler csrfRequestHandler,
        JsonMapper jsonMapper,
        AuthRateLimitService rateLimits,
        EmailVerificationKeys verificationKeys
) throws Exception {
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
                    .anyRequest().access((authentication, context) -> {
                        authenticatedSessionService.requireAuthenticatedSession(context.getRequest());
                        return new AuthorizationDecision(true);
                    })
            )
            .cors(Customizer.withDefaults())
            .csrf(csrf -> csrf
                    .csrfTokenRepository(csrfTokenRepository)
                    .csrfTokenRequestHandler(csrfRequestHandler)
                    // 공유 로그인 전략이 표준 CSRF 교체까지 한 번 수행한다.
                    .sessionAuthenticationStrategy(new NullAuthenticatedSessionStrategy())
            )
            .sessionManagement(session -> session.sessionAuthenticationStrategy(loginSessionAuthenticationStrategy))
            .securityContext(context -> context.securityContextRepository(securityContextRepository))
            // 전송 크기·형식만 CSRF보다 먼저 검사하고 JSON 구조 검증은 MVC에 남긴다.
            .addFilterBefore(new Part1JsonRequestGuardFilter(jsonMapper), CsrfFilter.class)
            .addFilterBefore(new Part2JsonRequestGuardFilter(jsonMapper), Part1JsonRequestGuardFilter.class)
            .addFilterBefore(new GlobalApiRateLimitFilter(rateLimits, verificationKeys, jsonMapper), Part2JsonRequestGuardFilter.class)
            .requestCache(cache -> cache.disable())
            .exceptionHandling(exceptions -> exceptions
                    .authenticationEntryPoint(authStateErrorHandler)
                    .accessDeniedHandler(new DelegatingAccessDeniedHandler(
                            accessDeniedHandlers, new AccessDeniedHandlerImpl()))
            )
            .oauth2Login(oauth -> oauth
                    .defaultSuccessUrl(properties.loginSuccessUrl(), true)
            )
            .logout(logout -> logout.disable());

    return http.build();
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/global|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## csrfTokenRequestHandler mbc54a3db2ec31eab

아래 원본 코드에서 구현을 확인한다.

```java
org.springframework.security.web.csrf.CsrfTokenRequestHandler csrfTokenRequestHandler()
```

설정: `@org.springframework.context.annotation.Bean`

### 이 메서드를 호출하는 곳

- 프레임워크 진입: Spring 컨테이너 Bean 생성

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.config.SecurityConfig_anonymous_120#생성자 m239f4bb31547a80d|익명 클래스@120.생성자]] — 생성 · L120

### 소스 코드

`src/main/java/com/dodamdodam/backend/global/config/SecurityConfig.java:118`

```java
@Bean
CsrfTokenRequestHandler csrfTokenRequestHandler() {
    return new CsrfTokenRequestAttributeHandler() {
        @Override
        public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken csrfToken) {
            return request.getHeader(csrfToken.getHeaderName());
        }
    };
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/global|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## csrfTokenRepository m27b2106272e43ff4

아래 원본 코드에서 구현을 확인한다.

```java
org.springframework.security.web.csrf.CookieCsrfTokenRepository csrfTokenRepository(org.springframework.boot.web.server.autoconfigure.ServerProperties serverProperties)
```

설정: `@org.springframework.context.annotation.Bean`

### 이 메서드를 호출하는 곳

- 프레임워크 진입: Spring 컨테이너 Bean 생성

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 외부 라이브러리 호출

- `java.lang.Boolean#equals(java.lang.Object)`
- `java.lang.IllegalArgumentException#<init>(java.lang.String)`
- `org.springframework.boot.web.server.Cookie#getSameSite()`
- `org.springframework.boot.web.server.Cookie#getSecure()`
- `org.springframework.boot.web.server.Cookie.SameSite#attributeValue()`
- `org.springframework.boot.web.server.autoconfigure.ServerProperties#getServlet()`
- `org.springframework.boot.web.server.autoconfigure.ServerProperties.Servlet#getSession()`
- `org.springframework.boot.web.server.servlet.Session#getCookie()`
- `org.springframework.http.ResponseCookie.ResponseCookieBuilder#path(java.lang.@org.jspecify.annotations.Nullable String)`
- `org.springframework.http.ResponseCookie.ResponseCookieBuilder#sameSite(java.lang.@org.jspecify.annotations.Nullable String)`
- `org.springframework.http.ResponseCookie.ResponseCookieBuilder#secure(boolean)`
- `org.springframework.security.web.csrf.CookieCsrfTokenRepository#setCookieCustomizer(java.util.function.Consumer<org.springframework.http.ResponseCookie.ResponseCookieBuilder>)`
- `org.springframework.security.web.csrf.CookieCsrfTokenRepository#withHttpOnlyFalse()`

### 소스 코드

`src/main/java/com/dodamdodam/backend/global/config/SecurityConfig.java:128`

```java
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
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/global|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## corsConfigurationSource mf369b7a9aa69415d

아래 원본 코드에서 구현을 확인한다.

```java
org.springframework.web.cors.CorsConfigurationSource corsConfigurationSource(com.dodamdodam.backend.global.config.AppSecurityProperties properties)
```

설정: `@org.springframework.context.annotation.Bean`

### 이 메서드를 호출하는 곳

- 프레임워크 진입: Spring 컨테이너 Bean 생성

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.config.AppSecurityProperties#allowedOrigins m66ee7b0d586a2a9d|AppSecurityProperties.allowedOrigins]] — 호출 · L146

### 외부 라이브러리 호출

- `java.lang.IllegalArgumentException#<init>(java.lang.String)`
- `java.lang.String#contains(java.lang.CharSequence)`
- `java.lang.String#equalsIgnoreCase(java.lang.String)`
- `java.util.List#of(E)`
- `java.util.List#of(E,E,E)`
- `java.util.List#of(E,E,E,E,E,E)`
- `org.springframework.web.cors.CorsConfiguration#<init>()`
- `org.springframework.web.cors.CorsConfiguration#getAllowedOrigins()`
- `org.springframework.web.cors.CorsConfiguration#setAllowCredentials(java.lang.@org.jspecify.annotations.Nullable Boolean)`
- `org.springframework.web.cors.CorsConfiguration#setAllowedHeaders(java.util.@org.jspecify.annotations.Nullable List<java.lang.String>)`
- `org.springframework.web.cors.CorsConfiguration#setAllowedMethods(java.util.@org.jspecify.annotations.Nullable List<java.lang.String>)`
- `org.springframework.web.cors.CorsConfiguration#setAllowedOrigins(java.util.@org.jspecify.annotations.Nullable List<java.lang.String>)`
- `org.springframework.web.cors.CorsConfiguration#setExposedHeaders(java.util.@org.jspecify.annotations.Nullable List<java.lang.String>)`
- `org.springframework.web.cors.CorsConfiguration#setMaxAge(java.lang.@org.jspecify.annotations.Nullable Long)`
- `org.springframework.web.cors.UrlBasedCorsConfigurationSource#<init>()`
- `org.springframework.web.cors.UrlBasedCorsConfigurationSource#registerCorsConfiguration(java.lang.String,org.springframework.web.cors.CorsConfiguration)`

### 소스 코드

`src/main/java/com/dodamdodam/backend/global/config/SecurityConfig.java:143`

```java
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
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/global|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## 생성자 m6fe7101596297a01

컴파일러가 생성한 멤버다.

```java
public SecurityConfig()
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 외부 라이브러리 호출

- `java.lang.Object#<init>()`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/global|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]
