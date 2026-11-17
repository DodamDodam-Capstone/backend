---
tags: [backend, code-map, auth]
aliases: ["AuthLogoutController"]
---
# AuthLogoutController

`com.dodamdodam.backend.auth.api.AuthLogoutController`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|시작]] → [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요]] → 이 클래스 · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/90-Reference|전체 참조 자료]]

## 클래스 요약

현재 업무 문맥을 폐기하고 네이티브 세션·CSRF 정보를 정리한다.

- 원본: `src/main/java/com/dodamdodam/backend/auth/api/AuthLogoutController.java:24`
- 작성 메서드/생성자 2개 · 자동 생성 0개

## 메서드 목록

| 메서드 | 하는 일 |
| --- | --- |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.api.AuthLogoutController#logout m43cbd6436794ba0f\|logout]] | 인증과 빈 본문을 확인한 뒤 현재 문맥·세션·CSRF 쿠키를 폐기한다. |

> [!example]- 생성자·자동 생성 멤버 1개
>
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.api.AuthLogoutController#생성자 m70fce35c57ab16b6|public AuthLogoutController(AuthenticatedSessionService authenticatedSessions, SessionSecurityRepository securityRepository, SecurityContextRepository securityContextRepository, CookieCsrfTokenRepository csrfRepository)]]

## logout m43cbd6436794ba0f

인증과 빈 본문을 확인한 뒤 현재 문맥·세션·CSRF 쿠키를 폐기한다.

```java
public org.springframework.http.ResponseEntity<?> logout(jakarta.servlet.http.HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response)
```

설정: `@org.springframework.web.bind.annotation.PostMapping({"/api/v1/auth/logout"})`

### 이 메서드를 호출하는 곳

- 프레임워크 진입: POST /api/v1/auth/logout

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthStateUnavailableException#생성자 m2ecef5ced435fdbf|AuthStateUnavailableException.생성자]] — 생성 · L60
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthenticatedSession#accountId m23b42fff4f806999|AuthenticatedSession.accountId]] — 호출 · L50
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthenticatedSession#securityContextId me6d8be795777e536|AuthenticatedSession.securityContextId]] — 호출 · L50
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthenticatedSessionService#requireAuthenticatedSession mad23075b25d06a28|AuthenticatedSessionService.requireAuthenticatedSession]] — 호출 · L44
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.SessionSecurityRepository#revokeCurrentContext ma61a7dc8bfc5d117|SessionSecurityRepository.revokeCurrentContext]] — 호출 · L50
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.error.ApiErrorResponses#body me905a471c60d2a7e|ApiErrorResponses.body]] — 호출 · L46

### 외부 라이브러리 호출

- `jakarta.servlet.ServletRequest#getAttribute(java.lang.String)`
- `java.lang.Boolean#equals(java.lang.Object)`
- `java.util.List#of()`
- `org.springframework.http.ResponseEntity#badRequest()`
- `org.springframework.http.ResponseEntity#noContent()`
- `org.springframework.http.ResponseEntity.BodyBuilder#body(@org.jspecify.annotations.Nullable T)`
- `org.springframework.http.ResponseEntity.HeadersBuilder#build()`
- `org.springframework.security.authentication.InsufficientAuthenticationException#<init>(java.lang.String)`
- `org.springframework.security.core.context.SecurityContext#getAuthentication()`
- `org.springframework.security.core.context.SecurityContextHolder#getContext()`
- `org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler#logout(jakarta.servlet.http.HttpServletRequest,jakarta.servlet.http.HttpServletResponse,org.springframework.security.core.@org.jspecify.annotations.Nullable Authentication)`
- `org.springframework.security.web.csrf.CsrfLogoutHandler#logout(jakarta.servlet.http.HttpServletRequest,jakarta.servlet.http.HttpServletResponse,org.springframework.security.core.@org.jspecify.annotations.Nullable Authentication)`

### 소스 코드

`src/main/java/com/dodamdodam/backend/auth/api/AuthLogoutController.java:42`

```java
@PostMapping("/api/v1/auth/logout")
public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
    var current = authenticatedSessions.requireAuthenticatedSession(request);
    if (!Boolean.TRUE.equals(request.getAttribute(Part1JsonRequestGuardFilter.EMPTY_BODY))) {
        return ResponseEntity.badRequest().body(ApiErrorResponses.body(
                response, "VALIDATION_FAILED", "요청 본문은 비어 있어야 합니다.", List.of()));
    }
    try {
        if (!securityRepository.revokeCurrentContext(current.accountId(), current.securityContextId())) {
            throw new InsufficientAuthenticationException("로그인이 필요합니다.");
        }
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        contextLogout.logout(request, response, authentication);
        csrfLogout.logout(request, response, authentication);
        return ResponseEntity.noContent().build();
    }
    catch (DataAccessException | TransactionException | ConversionException failure) {
        // 문맥 폐기의 커밋과 뒤이은 네이티브 세션 삭제 실패도 인증 장애다.
        throw new AuthStateUnavailableException(failure);
    }
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## 생성자 m70fce35c57ab16b6

기존 세션·CSRF 정리 기능으로 로그아웃 API를 구성한다.

```java
public AuthLogoutController(com.dodamdodam.backend.auth.session.AuthenticatedSessionService authenticatedSessions, com.dodamdodam.backend.auth.session.SessionSecurityRepository securityRepository, org.springframework.security.web.context.SecurityContextRepository securityContextRepository, org.springframework.security.web.csrf.CookieCsrfTokenRepository csrfRepository)
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 외부 라이브러리 호출

- `java.lang.Object#<init>()`
- `org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler#setSecurityContextRepository(org.springframework.security.web.context.SecurityContextRepository)`
- `org.springframework.security.web.csrf.CsrfLogoutHandler#<init>(org.springframework.security.web.csrf.CsrfTokenRepository)`

### 소스 코드

`src/main/java/com/dodamdodam/backend/auth/api/AuthLogoutController.java:33`

```java
public AuthLogoutController(AuthenticatedSessionService authenticatedSessions, SessionSecurityRepository securityRepository,
                            SecurityContextRepository securityContextRepository, CookieCsrfTokenRepository csrfRepository) {
    this.authenticatedSessions = authenticatedSessions;
    this.securityRepository = securityRepository;
    contextLogout.setSecurityContextRepository(securityContextRepository);
    csrfLogout = new CsrfLogoutHandler(csrfRepository);
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]
