---
tags: [backend, code-map, auth]
aliases: ["SessionLifecycleService"]
---
# SessionLifecycleService

`com.dodamdodam.backend.auth.session.SessionLifecycleService`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|시작]] → [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요]] → 이 클래스 · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/90-Reference|전체 참조 자료]]

## 클래스 요약

로그인 시 새 업무 문맥과 네이티브 세션을 연결하고 실패를 보상한다.

- 원본: `src/main/java/com/dodamdodam/backend/auth/session/SessionLifecycleService.java:17`
- 작성 메서드/생성자 8개 · 자동 생성 0개

## 메서드 목록

| 메서드 | 하는 일 |
| --- | --- |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.SessionLifecycleService#beginLogin m86b3ae016950c512\|beginLogin]] | 이전 세션을 확인하고 새 로그인 문맥의 생성·커밋 장애를 인증 실패와 구분한다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.SessionLifecycleService#nativeSessionPrepared mf110e2ed4943ea47\|nativeSessionPrepared]] | 아래 구현과 호출 관계에서 확인 |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.SessionLifecycleService#contextSaved mf9c9c28ed7751e2c\|contextSaved]] | 아래 구현과 호출 관계에서 확인 |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.SessionLifecycleService#verifyLogin mf3c5d20aa396347e\|verifyLogin]] | 응답 전에 실제 로그인 저장 결과를 확인하고 불완전한 저장을 503 대상으로 표시한다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.SessionLifecycleService#hasPendingLogin m6ec9457b18ffe30a\|hasPendingLogin]] | 아래 구현과 호출 관계에서 확인 |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.SessionLifecycleService#failLogin m8a418625ae91ad24\|failLogin]] | 실패한 새 문맥을 보상 폐기하고 프레임워크 세션도 반드시 정리한다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.SessionLifecycleService#pending m07094e1cbade6fe3\|pending]] | 아래 구현과 호출 관계에서 확인 |

> [!example]- 생성자·자동 생성 멤버 1개
>
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.SessionLifecycleService#생성자 m67a4335a831a75b0|public SessionLifecycleService(SessionSecurityRepository securityRepository, JdbcIndexedSessionRepository sessionRepository, AuthenticatedSessionService authenticatedSessions)]]

## beginLogin m86b3ae016950c512

이전 세션을 확인하고 새 로그인 문맥의 생성·커밋 장애를 인증 실패와 구분한다.

```java
com.dodamdodam.backend.auth.session.AuthenticatedSession beginLogin(org.springframework.security.core.Authentication authentication, jakarta.servlet.http.HttpServletRequest request)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.LoginSessionAuthenticationStrategy#onAuthentication m62579b298c019e97|LoginSessionAuthenticationStrategy.onAuthentication]] — 호출 · L30

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthStateUnavailableException#생성자 m2ecef5ced435fdbf|AuthStateUnavailableException.생성자]] — 생성 · L61
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthenticatedSession#생성자 mf3197381afe1fc7a|AuthenticatedSession.생성자]] — 생성 · L53
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.SessionLifecycleService.PendingLogin#생성자 m28a9766683ed8489|PendingLogin.생성자]] — 생성 · L57
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.SessionSecurityRepository#createLoginContext m7e32b11c78d3f326|SessionSecurityRepository.createLoginContext]] — 호출 · L56

### 외부 라이브러리 호출

- `jakarta.servlet.ServletRequest#setAttribute(java.lang.String,java.lang.Object)`
- `jakarta.servlet.http.HttpServletRequest#getSession(boolean)`
- `jakarta.servlet.http.HttpSession#getAttribute(java.lang.String)`
- `java.lang.IllegalArgumentException#<init>()`
- `java.lang.String#equals(java.lang.Object)`
- `java.security.Principal#getName()`
- `java.util.UUID#fromString(java.lang.String)`
- `java.util.UUID#toString()`
- `org.springframework.security.authentication.AuthenticationTrustResolver#isAuthenticated(org.springframework.security.core.@org.jspecify.annotations.Nullable Authentication)`
- `org.springframework.security.authentication.InsufficientAuthenticationException#<init>(java.lang.String)`

### 소스 코드

`src/main/java/com/dodamdodam/backend/auth/session/SessionLifecycleService.java:34`

```java
AuthenticatedSession beginLogin(Authentication authentication, HttpServletRequest request) {
    if (!trustResolver.isAuthenticated(authentication)) {
        throw new InsufficientAuthenticationException("로그인 계정을 확인할 수 없습니다.");
    }
    UUID accountId;
    try {
        accountId = UUID.fromString(authentication.getName());
        if (!accountId.toString().equals(authentication.getName())) {
            throw new IllegalArgumentException();
        }
    }
    catch (IllegalArgumentException exception) {
        throw new InsufficientAuthenticationException("로그인 계정을 확인할 수 없습니다.");
    }
    try {
        var old = request.getSession(false);
        AuthenticatedSession previous = null;
        if (old != null && old.getAttribute(AuthenticatedSessionService.ACCOUNT_ID) instanceof UUID oldAccount
                && old.getAttribute(AuthenticatedSessionService.SECURITY_CONTEXT_ID) instanceof UUID oldContext) {
            previous = new AuthenticatedSession(oldAccount, oldContext);
        }
        // REQUIRES_NEW의 커밋 실패도 프록시 호출이 끝나는 이 경계에서 분류한다.
        var created = securityRepository.createLoginContext(accountId, previous);
        request.setAttribute(LOGIN_ATTEMPT, new PendingLogin(created));
        return created;
    }
    catch (DataAccessException | TransactionException | ConversionException failure) {
        throw new AuthStateUnavailableException(failure);
    }
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## nativeSessionPrepared mf110e2ed4943ea47

아래 원본 코드에서 구현을 확인한다.

```java
void nativeSessionPrepared(jakarta.servlet.http.HttpServletRequest request, java.lang.String sessionId)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.LoginSessionAuthenticationStrategy#onAuthentication m62579b298c019e97|LoginSessionAuthenticationStrategy.onAuthentication]] — 호출 · L39

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.SessionLifecycleService#pending m07094e1cbade6fe3|SessionLifecycleService.pending]] — 호출 · L66

### 소스 코드

`src/main/java/com/dodamdodam/backend/auth/session/SessionLifecycleService.java:65`

```java
void nativeSessionPrepared(HttpServletRequest request, String sessionId) {
    pending(request).sessionId = sessionId;
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## contextSaved mf9c9c28ed7751e2c

아래 원본 코드에서 구현을 확인한다.

```java
void contextSaved(jakarta.servlet.http.HttpServletRequest request)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.LoginSecurityContextRepository#saveContext m554bf261f42a0b0a|LoginSecurityContextRepository.saveContext]] — 호출 · L19

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.SessionLifecycleService#pending m07094e1cbade6fe3|SessionLifecycleService.pending]] — 호출 · L70
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.SessionLifecycleService#verifyLogin mf3c5d20aa396347e|SessionLifecycleService.verifyLogin]] — 호출 · L73

### 소스 코드

`src/main/java/com/dodamdodam/backend/auth/session/SessionLifecycleService.java:69`

```java
void contextSaved(HttpServletRequest request) {
    var login = pending(request);
    if (login != null) {
        login.contextSaved = true;
        verifyLogin(request);
    }
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## verifyLogin mf3c5d20aa396347e

응답 전에 실제 로그인 저장 결과를 확인하고 불완전한 저장을 503 대상으로 표시한다.

```java
public void verifyLogin(jakarta.servlet.http.HttpServletRequest request)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthStateErrorFilter#verifyResponseAccess m1098528f38d3ff11|AuthStateErrorFilter.verifyResponseAccess]] — 호출 · L95
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.SessionLifecycleService#contextSaved mf9c9c28ed7751e2c|SessionLifecycleService.contextSaved]] — 호출 · L73

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthStateUnavailableException#생성자 m03e9c81ea8cf5421|AuthStateUnavailableException.생성자]] — 생성 · L85,89
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthStateUnavailableException#생성자 m2ecef5ced435fdbf|AuthStateUnavailableException.생성자]] — 생성 · L95
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthenticatedSessionService#markCookieRenewal m1d1e9ada990d3482|AuthenticatedSessionService.markCookieRenewal]] — 호출 · L92
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthenticatedSessionService#requireStoredSession mc7d6b5fbad4be98b|AuthenticatedSessionService.requireStoredSession]] — 호출 · L91
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.SessionLifecycleService#pending m07094e1cbade6fe3|SessionLifecycleService.pending]] — 호출 · L79

### 외부 라이브러리 호출

- `java.time.Duration#isNegative()`
- `org.springframework.session.Session#getMaxInactiveInterval()`
- `org.springframework.session.jdbc.JdbcIndexedSessionRepository#findById(java.lang.String)`

### 소스 코드

`src/main/java/com/dodamdodam/backend/auth/session/SessionLifecycleService.java:78`

```java
public void verifyLogin(HttpServletRequest request) {
    var login = pending(request);
    if (login == null) {
        return;
    }
    try {
        if (!login.contextSaved || login.sessionId == null) {
            throw new AuthStateUnavailableException();
        }
        Session stored = sessionRepository.findById(login.sessionId);
        if (stored == null || !stored.getMaxInactiveInterval().isNegative()) {
            throw new AuthStateUnavailableException();
        }
        authenticatedSessions.requireStoredSession(stored, login.context);
        AuthenticatedSessionService.markCookieRenewal(request, login.sessionId, login.context);
    }
    catch (AuthenticationException | DataAccessException | TransactionException | ConversionException failure) {
        throw new AuthStateUnavailableException(failure);
    }
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## hasPendingLogin m6ec9457b18ffe30a

아래 원본 코드에서 구현을 확인한다.

```java
public boolean hasPendingLogin(jakarta.servlet.http.HttpServletRequest request)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthStateErrorFilter#doFilterInternal m039987860b5cf49f|AuthStateErrorFilter.doFilterInternal]] — 호출 · L64
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.error.CommonApiExceptionHandler#handle m58650f47953e9086|CommonApiExceptionHandler.handle]] — 호출 · L37

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.SessionLifecycleService#pending m07094e1cbade6fe3|SessionLifecycleService.pending]] — 호출 · L100

### 소스 코드

`src/main/java/com/dodamdodam/backend/auth/session/SessionLifecycleService.java:99`

```java
public boolean hasPendingLogin(HttpServletRequest request) {
    return pending(request) != null;
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## failLogin m8a418625ae91ad24

실패한 새 문맥을 보상 폐기하고 프레임워크 세션도 반드시 정리한다.

```java
public void failLogin(jakarta.servlet.http.HttpServletRequest request)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthStateErrorFilter#doFilterInternal m039987860b5cf49f|AuthStateErrorFilter.doFilterInternal]] — 호출 · L67

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthStateUnavailableException#생성자 m2ecef5ced435fdbf|AuthStateUnavailableException.생성자]] — 생성 · L122
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.SessionLifecycleService#pending m07094e1cbade6fe3|SessionLifecycleService.pending]] — 호출 · L105
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.SessionSecurityRepository#discardLoginContext m53a371370ce09898|SessionSecurityRepository.discardLoginContext]] — 호출 · L112

### 외부 라이브러리 호출

- `jakarta.servlet.ServletRequest#removeAttribute(java.lang.String)`
- `org.springframework.security.core.context.SecurityContextHolder#clearContext()`
- `org.springframework.session.jdbc.JdbcIndexedSessionRepository#deleteById(java.lang.String)`

### 소스 코드

`src/main/java/com/dodamdodam/backend/auth/session/SessionLifecycleService.java:104`

```java
public void failLogin(HttpServletRequest request) {
    var login = pending(request);
    request.removeAttribute(LOGIN_ATTEMPT);
    if (login == null) {
        return;
    }
    try {
        try {
            securityRepository.discardLoginContext(login.context);
        }
        finally {
            SecurityContextHolder.clearContext();
            if (login.sessionId != null) {
                sessionRepository.deleteById(login.sessionId);
            }
        }
    }
    catch (DataAccessException | TransactionException | ConversionException failure) {
        throw new AuthStateUnavailableException(failure);
    }
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## pending m07094e1cbade6fe3

아래 원본 코드에서 구현을 확인한다.

```java
private com.dodamdodam.backend.auth.session.SessionLifecycleService.PendingLogin pending(jakarta.servlet.http.HttpServletRequest request)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.SessionLifecycleService#contextSaved mf9c9c28ed7751e2c|SessionLifecycleService.contextSaved]] — 호출 · L70
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.SessionLifecycleService#failLogin m8a418625ae91ad24|SessionLifecycleService.failLogin]] — 호출 · L105
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.SessionLifecycleService#hasPendingLogin m6ec9457b18ffe30a|SessionLifecycleService.hasPendingLogin]] — 호출 · L100
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.SessionLifecycleService#nativeSessionPrepared mf110e2ed4943ea47|SessionLifecycleService.nativeSessionPrepared]] — 호출 · L66
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.SessionLifecycleService#verifyLogin mf3c5d20aa396347e|SessionLifecycleService.verifyLogin]] — 호출 · L79

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 외부 라이브러리 호출

- `jakarta.servlet.ServletRequest#getAttribute(java.lang.String)`

### 소스 코드

`src/main/java/com/dodamdodam/backend/auth/session/SessionLifecycleService.java:126`

```java
private PendingLogin pending(HttpServletRequest request) {
    return (PendingLogin) request.getAttribute(LOGIN_ATTEMPT);
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## 생성자 m67a4335a831a75b0

아래 원본 코드에서 구현을 확인한다.

```java
public SessionLifecycleService(com.dodamdodam.backend.auth.session.SessionSecurityRepository securityRepository, org.springframework.session.jdbc.JdbcIndexedSessionRepository sessionRepository, com.dodamdodam.backend.auth.session.AuthenticatedSessionService authenticatedSessions)
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 외부 라이브러리 호출

- `java.lang.Object#<init>()`

### 소스 코드

`src/main/java/com/dodamdodam/backend/auth/session/SessionLifecycleService.java:26`

```java
public SessionLifecycleService(SessionSecurityRepository securityRepository, JdbcIndexedSessionRepository sessionRepository,
                               AuthenticatedSessionService authenticatedSessions) {
    this.securityRepository = securityRepository;
    this.sessionRepository = sessionRepository;
    this.authenticatedSessions = authenticatedSessions;
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]
