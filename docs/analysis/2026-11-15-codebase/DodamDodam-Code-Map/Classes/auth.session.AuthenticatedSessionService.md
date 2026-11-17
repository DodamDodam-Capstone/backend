---
tags: [backend, code-map, auth]
aliases: ["AuthenticatedSessionService"]
---
# AuthenticatedSessionService

`com.dodamdodam.backend.auth.session.AuthenticatedSessionService`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|시작]] → [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요]] → 이 클래스 · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/90-Reference|전체 참조 자료]]

## 클래스 요약

프레임워크 인증·저장된 세션·현재 DB 보안 문맥이 일치하는지 검증한다.

- 원본: `src/main/java/com/dodamdodam/backend/auth/session/AuthenticatedSessionService.java:17`
- 작성 메서드/생성자 5개 · 자동 생성 0개

## 메서드 목록

| 메서드 | 하는 일 |
| --- | --- |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthenticatedSessionService#requireAuthenticatedSession mad23075b25d06a28\|requireAuthenticatedSession]] | 실제 세션과 업무 문맥을 확인하고 저장소 장애를 인증 무효와 구분한다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthenticatedSessionService#requireStoredSession mc7d6b5fbad4be98b\|requireStoredSession]] | 저장된 세션을 다시 검증하며 인증 저장소 확인 실패를 명시적으로 전달한다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthenticatedSessionService#markCookieRenewal m1d1e9ada990d3482\|markCookieRenewal]] | 아래 구현과 호출 관계에서 확인 |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthenticatedSessionService#validate m72fbb73c9b868baa\|validate]] | 전달받은 인증·세션 저장 정보·DB 문맥을 대조하고 하나라도 불일치하면 인증을 거부한다. |

> [!example]- 생성자·자동 생성 멤버 1개
>
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthenticatedSessionService#생성자 m04f646ffcd6a46c4|public AuthenticatedSessionService(SessionSecurityRepository repository)]]

## requireAuthenticatedSession mad23075b25d06a28

실제 세션과 업무 문맥을 확인하고 저장소 장애를 인증 무효와 구분한다.

```java
public com.dodamdodam.backend.auth.session.AuthenticatedSession requireAuthenticatedSession(jakarta.servlet.http.HttpServletRequest request)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.api.AuthLogoutController#logout m43cbd6436794ba0f|AuthLogoutController.logout]] — 호출 · L44
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.api.EmailVerificationController#issue m90c67bb20e0ca690|EmailVerificationController.issue]] — 호출 · L48
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.api.ChildController#create m85a3864c9b94be48|ChildController.create]] — 호출 · L38
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.api.ConversationController#prepareStart m1cac5f319ccf317a|ConversationController.prepareStart]] — 호출 · L54
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.api.ConversationStartResponseFilter#verify mbaf83885c918b297|ConversationStartResponseFilter.verify]] — 호출 · L129
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.config.SecurityConfig#applicationSecurityFilterChain m8a28e16ae80b5e1e|SecurityConfig.applicationSecurityFilterChain]] — 호출 · L87

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthStateUnavailableException#생성자 m2ecef5ced435fdbf|AuthStateUnavailableException.생성자]] — 생성 · L45
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthenticatedSessionService#markCookieRenewal m1d1e9ada990d3482|AuthenticatedSessionService.markCookieRenewal]] — 호출 · L41
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthenticatedSessionService#validate m72fbb73c9b868baa|AuthenticatedSessionService.validate]] — 호출 · L38

### 외부 라이브러리 호출

- `jakarta.servlet.http.HttpServletRequest#getSession(boolean)`
- `jakarta.servlet.http.HttpSession#getAttribute(java.lang.String)`
- `jakarta.servlet.http.HttpSession#getId()`
- `org.springframework.security.authentication.InsufficientAuthenticationException#<init>(java.lang.String)`
- `org.springframework.security.core.context.SecurityContext#getAuthentication()`
- `org.springframework.security.core.context.SecurityContextHolder#getContext()`

### 소스 코드

`src/main/java/com/dodamdodam/backend/auth/session/AuthenticatedSessionService.java:32`

```java
public AuthenticatedSession requireAuthenticatedSession(HttpServletRequest request) {
    try {
        var session = request.getSession(false);
        if (session == null) {
            throw new InsufficientAuthenticationException("로그인이 필요합니다.");
        }
        var authenticated = validate(SecurityContextHolder.getContext().getAuthentication(),
                session.getAttribute(ACCOUNT_ID), session.getAttribute(SECURITY_CONTEXT_ID),
                session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY));
        markCookieRenewal(request, session.getId(), authenticated);
        return authenticated;
    }
    catch (DataAccessException | TransactionException | ConversionException failure) {
        throw new AuthStateUnavailableException(failure);
    }
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## requireStoredSession mc7d6b5fbad4be98b

저장된 세션을 다시 검증하며 인증 저장소 확인 실패를 명시적으로 전달한다.

```java
void requireStoredSession(org.springframework.session.Session stored, com.dodamdodam.backend.auth.session.AuthenticatedSession expected)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.SessionCookieRenewalFilter#renew me6ea84e6a538169a|SessionCookieRenewalFilter.renew]] — 호출 · L62
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.SessionLifecycleService#verifyLogin mf3c5d20aa396347e|SessionLifecycleService.verifyLogin]] — 호출 · L91

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthStateUnavailableException#생성자 m2ecef5ced435fdbf|AuthStateUnavailableException.생성자]] — 생성 · L63
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthenticatedSession#equals mbf17f3450c6bf2bd|AuthenticatedSession.equals]] — 호출 · L58
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthenticatedSessionService#validate m72fbb73c9b868baa|AuthenticatedSessionService.validate]] — 호출 · L57

### 외부 라이브러리 호출

- `org.springframework.security.authentication.InsufficientAuthenticationException#<init>(java.lang.String)`
- `org.springframework.security.core.context.SecurityContext#getAuthentication()`
- `org.springframework.session.Session#getAttribute(java.lang.String)`

### 소스 코드

`src/main/java/com/dodamdodam/backend/auth/session/AuthenticatedSessionService.java:50`

```java
void requireStoredSession(Session stored, AuthenticatedSession expected) {
    try {
        if (stored == null) {
            throw new InsufficientAuthenticationException("로그인이 필요합니다.");
        }
        Object context = stored.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        var authentication = context instanceof SecurityContext security ? security.getAuthentication() : null;
        var actual = validate(authentication, stored.getAttribute(ACCOUNT_ID), stored.getAttribute(SECURITY_CONTEXT_ID), context);
        if (!expected.equals(actual)) {
            throw new InsufficientAuthenticationException("로그인이 필요합니다.");
        }
    }
    catch (DataAccessException | TransactionException | ConversionException failure) {
        throw new AuthStateUnavailableException(failure);
    }
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## markCookieRenewal m1d1e9ada990d3482

아래 원본 코드에서 구현을 확인한다.

```java
static void markCookieRenewal(jakarta.servlet.http.HttpServletRequest request, java.lang.String sessionId, com.dodamdodam.backend.auth.session.AuthenticatedSession context)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthenticatedSessionService#requireAuthenticatedSession mad23075b25d06a28|AuthenticatedSessionService.requireAuthenticatedSession]] — 호출 · L41
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.SessionLifecycleService#verifyLogin mf3c5d20aa396347e|SessionLifecycleService.verifyLogin]] — 호출 · L92

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthenticatedSessionService.CookieRenewal#생성자 m87b288e89010fbc8|CookieRenewal.생성자]] — 생성 · L69

### 외부 라이브러리 호출

- `jakarta.servlet.ServletRequest#setAttribute(java.lang.String,java.lang.Object)`

### 소스 코드

`src/main/java/com/dodamdodam/backend/auth/session/AuthenticatedSessionService.java:68`

```java
static void markCookieRenewal(HttpServletRequest request, String sessionId, AuthenticatedSession context) {
    request.setAttribute(COOKIE_RENEWAL, new CookieRenewal(sessionId, context));
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## validate m72fbb73c9b868baa

전달받은 인증·세션 저장 정보·DB 문맥을 대조하고 하나라도 불일치하면 인증을 거부한다.

```java
private com.dodamdodam.backend.auth.session.AuthenticatedSession validate(org.springframework.security.core.Authentication authentication, java.lang.Object account, java.lang.Object context, java.lang.Object security)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthenticatedSessionService#requireAuthenticatedSession mad23075b25d06a28|AuthenticatedSessionService.requireAuthenticatedSession]] — 호출 · L38
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthenticatedSessionService#requireStoredSession mc7d6b5fbad4be98b|AuthenticatedSessionService.requireStoredSession]] — 호출 · L57

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthenticatedSession#생성자 mf3197381afe1fc7a|AuthenticatedSession.생성자]] — 생성 · L101
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.SessionSecurityRepository#isCurrentContext mda2203671fdb2db3|SessionSecurityRepository.isCurrentContext]] — 호출 · L97

### 외부 라이브러리 호출

- `java.lang.String#equals(java.lang.Object)`
- `java.security.Principal#getName()`
- `java.util.UUID#toString()`
- `org.springframework.security.authentication.AuthenticationTrustResolver#isAuthenticated(org.springframework.security.core.@org.jspecify.annotations.Nullable Authentication)`
- `org.springframework.security.authentication.InsufficientAuthenticationException#<init>(java.lang.String)`
- `org.springframework.security.core.context.SecurityContext#getAuthentication()`

### 소스 코드

`src/main/java/com/dodamdodam/backend/auth/session/AuthenticatedSessionService.java:81`

```java
private AuthenticatedSession validate(Authentication authentication, Object account, Object context, Object security) {
    // 전달받은 인증 정보가 익명이 아닌 인증 완료 상태인지 확인한다.
    if (!trustResolver.isAuthenticated(authentication)
            // 세션의 계정 ID가 누락되지 않은 UUID인지 확인한다.
            || !(account instanceof UUID accountId)
            // 세션의 로그인 문맥 ID가 누락되지 않은 UUID인지 확인한다.
            || !(context instanceof UUID contextId)
            // 전달받은 principal이 세션의 계정 ID와 같은지 확인한다.
            || !accountId.toString().equals(authentication.getName())
            // 세션의 보안 정보가 SecurityContext 타입으로 저장되어 있는지 확인한다.
            || !(security instanceof SecurityContext storedContext)
            // 저장된 인증 정보도 익명이 아닌 인증 완료 상태인지 확인한다.
            || !trustResolver.isAuthenticated(storedContext.getAuthentication())
            // 저장된 principal도 같은 계정을 가리키는지 확인한다.
            || !accountId.toString().equals(storedContext.getAuthentication().getName())
            // DB에서 계정·문맥 연결, 미폐기, 버전 일치와 만료 시각을 확인한다.
            || !repository.isCurrentContext(accountId, contextId)) {
        // 어느 조건이든 실패하면 이 요청의 인증을 거부한다.
        throw new InsufficientAuthenticationException("로그인이 필요합니다.");
    }
    return new AuthenticatedSession(accountId, contextId);
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## 생성자 m04f646ffcd6a46c4

아래 원본 코드에서 구현을 확인한다.

```java
public AuthenticatedSessionService(com.dodamdodam.backend.auth.session.SessionSecurityRepository repository)
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 외부 라이브러리 호출

- `java.lang.Object#<init>()`

### 소스 코드

`src/main/java/com/dodamdodam/backend/auth/session/AuthenticatedSessionService.java:27`

```java
public AuthenticatedSessionService(SessionSecurityRepository repository) {
    this.repository = repository;
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]
