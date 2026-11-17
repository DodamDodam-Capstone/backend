---
tags: [backend, code-map, auth]
aliases: ["LoginSessionAuthenticationStrategy"]
---
# LoginSessionAuthenticationStrategy

`com.dodamdodam.backend.auth.session.LoginSessionAuthenticationStrategy`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|시작]] → [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요]] → 이 클래스 · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/90-Reference|전체 참조 자료]]

## 클래스 요약

프레임워크 설정 또는 보조 타입이다. 아래 메서드와 소스에서 연결 위치를 확인한다.

- 원본: `src/main/java/com/dodamdodam/backend/auth/session/LoginSessionAuthenticationStrategy.java:14`
- 작성 메서드/생성자 2개 · 자동 생성 0개

## 메서드 목록

| 메서드 | 하는 일 |
| --- | --- |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.LoginSessionAuthenticationStrategy#onAuthentication m62579b298c019e97\|onAuthentication]] | 아래 구현과 호출 관계에서 확인 |

> [!example]- 생성자·자동 생성 멤버 1개
>
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.LoginSessionAuthenticationStrategy#생성자 mc69a76ba15c64801|public LoginSessionAuthenticationStrategy(SessionLifecycleService lifecycle, CookieCsrfTokenRepository csrfRepository, CsrfTokenRequestHandler csrfRequestHandler)]]

## onAuthentication m62579b298c019e97

아래 원본 코드에서 구현을 확인한다.

```java
public void onAuthentication(org.springframework.security.core.Authentication authentication, jakarta.servlet.http.HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response)
```

설정: `@java.lang.Override`

### 이 메서드를 호출하는 곳

- 프레임워크 진입: Spring Security 로그인 성공 전략

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthenticatedSession#accountId m23b42fff4f806999|AuthenticatedSession.accountId]] — 호출 · L41
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthenticatedSession#securityContextId me6d8be795777e536|AuthenticatedSession.securityContextId]] — 호출 · L42
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.SessionLifecycleService#beginLogin m86b3ae016950c512|SessionLifecycleService.beginLogin]] — 호출 · L30
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.SessionLifecycleService#nativeSessionPrepared mf110e2ed4943ea47|SessionLifecycleService.nativeSessionPrepared]] — 호출 · L39

### 외부 라이브러리 호출

- `jakarta.servlet.http.HttpServletRequest#getSession(boolean)`
- `jakarta.servlet.http.HttpSession#getId()`
- `jakarta.servlet.http.HttpSession#removeAttribute(java.lang.String)`
- `jakarta.servlet.http.HttpSession#setAttribute(java.lang.String,java.lang.Object)`
- `jakarta.servlet.http.HttpSession#setMaxInactiveInterval(int)`
- `org.springframework.dao.DataAccessResourceFailureException#<init>(java.lang.@org.jspecify.annotations.Nullable String,java.lang.@org.jspecify.annotations.Nullable Throwable)`
- `org.springframework.security.web.authentication.session.AbstractSessionFixationProtectionStrategy#onAuthentication(org.springframework.security.core.Authentication,jakarta.servlet.http.HttpServletRequest,jakarta.servlet.http.HttpServletResponse)`
- `org.springframework.security.web.csrf.CsrfAuthenticationStrategy#onAuthentication(org.springframework.security.core.@org.jspecify.annotations.Nullable Authentication,jakarta.servlet.http.HttpServletRequest,jakarta.servlet.http.HttpServletResponse)`

### 소스 코드

`src/main/java/com/dodamdodam/backend/auth/session/LoginSessionAuthenticationStrategy.java:28`

```java
@Override
public void onAuthentication(Authentication authentication, HttpServletRequest request, HttpServletResponse response) {
    var context = lifecycle.beginLogin(authentication, request);
    try {
        var previous = request.getSession(false);
        if (previous != null) {
            // Spring Security 예약 속성도 이전 principal은 새 로그인으로 옮기지 않는다.
            previous.removeAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        }
        fixation.onAuthentication(authentication, request, response);
        var session = request.getSession(true);
        lifecycle.nativeSessionPrepared(request, session.getId());
        session.setMaxInactiveInterval(-1);
        session.setAttribute(AuthenticatedSessionService.ACCOUNT_ID, context.accountId());
        session.setAttribute(AuthenticatedSessionService.SECURITY_CONTEXT_ID, context.securityContextId());
        csrf.onAuthentication(authentication, request, response);
    }
    catch (RuntimeException exception) {
        throw new DataAccessResourceFailureException("로그인 세션을 준비할 수 없습니다.", exception);
    }
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## 생성자 mc69a76ba15c64801

아래 원본 코드에서 구현을 확인한다.

```java
public LoginSessionAuthenticationStrategy(com.dodamdodam.backend.auth.session.SessionLifecycleService lifecycle, org.springframework.security.web.csrf.CookieCsrfTokenRepository csrfRepository, org.springframework.security.web.csrf.CsrfTokenRequestHandler csrfRequestHandler)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.config.SessionConfig#loginSessionAuthenticationStrategy m869f6655de6b7c46|SessionConfig.loginSessionAuthenticationStrategy]] — 생성 · L28

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 외부 라이브러리 호출

- `java.lang.Object#<init>()`
- `org.springframework.security.web.authentication.session.SessionFixationProtectionStrategy#setMigrateSessionAttributes(boolean)`
- `org.springframework.security.web.csrf.CsrfAuthenticationStrategy#<init>(org.springframework.security.web.csrf.CsrfTokenRepository)`
- `org.springframework.security.web.csrf.CsrfAuthenticationStrategy#setRequestHandler(org.springframework.security.web.csrf.CsrfTokenRequestHandler)`

### 소스 코드

`src/main/java/com/dodamdodam/backend/auth/session/LoginSessionAuthenticationStrategy.java:20`

```java
public LoginSessionAuthenticationStrategy(SessionLifecycleService lifecycle, CookieCsrfTokenRepository csrfRepository,
                                          CsrfTokenRequestHandler csrfRequestHandler) {
    this.lifecycle = lifecycle;
    fixation.setMigrateSessionAttributes(false);
    csrf = new CsrfAuthenticationStrategy(csrfRepository);
    csrf.setRequestHandler(csrfRequestHandler);
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]
