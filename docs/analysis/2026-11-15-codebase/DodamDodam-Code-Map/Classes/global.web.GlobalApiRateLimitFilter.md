---
tags: [backend, code-map, global]
aliases: ["GlobalApiRateLimitFilter"]
---
# GlobalApiRateLimitFilter

`com.dodamdodam.backend.global.web.GlobalApiRateLimitFilter`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|시작]] → [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/global|기능 개요]] → 이 클래스 · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/90-Reference|전체 참조 자료]]

## 클래스 요약

API·OAuth 요청의 IP 예산을 업무 처리 전에 예약하고 초과 요청을 거부한다.

- 원본: `src/main/java/com/dodamdodam/backend/global/web/GlobalApiRateLimitFilter.java:16`
- 작성 메서드/생성자 3개 · 자동 생성 0개

## 메서드 목록

| 메서드 | 하는 일 |
| --- | --- |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.web.GlobalApiRateLimitFilter#shouldNotFilter m4470e85441d8daf6\|shouldNotFilter]] | health·정적 리소스는 제외하며 정상 CORS preflight는 앞선 CORS 필터가 종료한다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.web.GlobalApiRateLimitFilter#doFilterInternal me8918f88c454eb77\|doFilterInternal]] | 업무/전송 실패와 무관하게 전역 IP 예약을 먼저 커밋하고 소진 시 공통429를 반환한다. |

> [!example]- 생성자·자동 생성 멤버 1개
>
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.web.GlobalApiRateLimitFilter#생성자 m3c9dda322a7264a8|public GlobalApiRateLimitFilter(AuthRateLimitService limits, EmailVerificationKeys keys, JsonMapper mapper)]]

## shouldNotFilter m4470e85441d8daf6

health·정적 리소스는 제외하며 정상 CORS preflight는 앞선 CORS 필터가 종료한다.

```java
protected boolean shouldNotFilter(jakarta.servlet.http.HttpServletRequest request)
```

설정: `@java.lang.Override`

### 이 메서드를 호출하는 곳

- 프레임워크 진입: Servlet / Spring 필터 실행

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 외부 라이브러리 호출

- `org.springframework.security.web.util.matcher.RequestMatcher#matches(jakarta.servlet.http.HttpServletRequest)`

### 소스 코드

`src/main/java/com/dodamdodam/backend/global/web/GlobalApiRateLimitFilter.java:32`

```java
@Override
protected boolean shouldNotFilter(HttpServletRequest request) {
    return !API.matches(request) && !OAUTH_START.matches(request) && !OAUTH_CALLBACK.matches(request);
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/global|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## doFilterInternal me8918f88c454eb77

업무/전송 실패와 무관하게 전역 IP 예약을 먼저 커밋하고 소진 시 공통429를 반환한다.

```java
protected void doFilterInternal(jakarta.servlet.http.HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response, jakarta.servlet.FilterChain chain)
```

설정: `@java.lang.Override`

### 이 메서드를 호출하는 곳

- 프레임워크 진입: Servlet / Spring 필터 실행

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.AuthRateLimitService#reserve m63e1737a4e336ba7|AuthRateLimitService.reserve]] — 호출 · L42
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.exception.AuthRateLimitException#code mcaa0ce05ad464eab|AuthRateLimitException.code]] — 호출 · L46
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.exception.AuthRateLimitException#retryAfterSeconds m423c845792a2b7a3|AuthRateLimitException.retryAfterSeconds]] — 호출 · L45
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.AuthRateLimitKeyHasher#apiIp m4f29dcbbd14695b0|AuthRateLimitKeyHasher.apiIp]] — 호출 · L42
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationKeys#rateHasher ma21ea9ca87c2fdb6|EmailVerificationKeys.rateHasher]] — 호출 · L42
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.error.ApiErrorResponses#write m1b723e7027f0ade4|ApiErrorResponses.write]] — 호출 · L46
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.web.ClientAddressResolver#resolve m53c50e28b5cfa314|ClientAddressResolver.resolve]] — 호출 · L42

### 외부 라이브러리 호출

- `jakarta.servlet.FilterChain#doFilter(jakarta.servlet.ServletRequest,jakarta.servlet.ServletResponse)`
- `jakarta.servlet.http.HttpServletResponse#setHeader(java.lang.String,java.lang.String)`
- `java.lang.Long#toString(long)`
- `java.lang.Throwable#getMessage()`
- `java.util.List#of(E)`

### 소스 코드

`src/main/java/com/dodamdodam/backend/global/web/GlobalApiRateLimitFilter.java:38`

```java
@Override
protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {
    try {
        limits.reserve(List.of(keys.rateHasher().apiIp(ClientAddressResolver.resolve(request))));
    }
    catch (AuthRateLimitException exceeded) {
        response.setHeader("Retry-After", Long.toString(exceeded.retryAfterSeconds()));
        ApiErrorResponses.write(mapper, response, 429, exceeded.code(), exceeded.getMessage());
        return;
    }
    chain.doFilter(request, response);
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/global|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## 생성자 m3c9dda322a7264a8

CORS 뒤 API·OAuth에 한 번만 적용할 전역 예산 예약 경로를 구성한다.

```java
public GlobalApiRateLimitFilter(com.dodamdodam.backend.auth.application.AuthRateLimitService limits, com.dodamdodam.backend.auth.infrastructure.EmailVerificationKeys keys, tools.jackson.databind.json.JsonMapper mapper)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.config.SecurityConfig#applicationSecurityFilterChain m8a28e16ae80b5e1e|SecurityConfig.applicationSecurityFilterChain]] — 생성 · L103

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 외부 라이브러리 호출

- `org.springframework.web.filter.OncePerRequestFilter#<init>()`

### 소스 코드

`src/main/java/com/dodamdodam/backend/global/web/GlobalApiRateLimitFilter.java:25`

```java
public GlobalApiRateLimitFilter(AuthRateLimitService limits, EmailVerificationKeys keys, JsonMapper mapper) {
    this.limits = limits;
    this.keys = keys;
    this.mapper = mapper;
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/global|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]
