---
tags: [backend, code-map, auth]
aliases: ["SessionCookieRenewalFilter"]
---
# SessionCookieRenewalFilter

`com.dodamdodam.backend.auth.session.SessionCookieRenewalFilter`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|시작]] → [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요]] → 이 클래스 · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/90-Reference|전체 참조 자료]]

## 클래스 요약

현재 세션의 유효성을 다시 확인한 뒤 로그인 쿠키를 갱신한다.

- 원본: `src/main/java/com/dodamdodam/backend/auth/session/SessionCookieRenewalFilter.java:22`
- 작성 메서드/생성자 3개 · 자동 생성 0개

## 메서드 목록

| 메서드 | 하는 일 |
| --- | --- |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.SessionCookieRenewalFilter#doFilterInternal m5b3b8f1a080501e9\|doFilterInternal]] | 아래 구현과 호출 관계에서 확인 |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.SessionCookieRenewalFilter#renew me6ea84e6a538169a\|renew]] | native 세션·업무 문맥 재검사 뒤 쿠키를 갱신하며 확인 장애는 인증 오류로 전달한다. |

> [!example]- 생성자·자동 생성 멤버 1개
>
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.SessionCookieRenewalFilter#생성자 m84764fa9211fa561|public SessionCookieRenewalFilter(CookieSerializer cookies, JdbcIndexedSessionRepository sessions, AuthenticatedSessionService authentication)]]

## doFilterInternal m5b3b8f1a080501e9

아래 원본 코드에서 구현을 확인한다.

```java
protected void doFilterInternal(jakarta.servlet.http.HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response, jakarta.servlet.FilterChain filterChain)
```

설정: `@java.lang.Override`

### 이 메서드를 호출하는 곳

- 프레임워크 진입: Servlet / Spring 필터 실행

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.SessionCookieRenewalFilter#renew me6ea84e6a538169a|SessionCookieRenewalFilter.renew]] — 호출 · L47
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.SessionCookieRenewalFilter_anonymous_41#생성자 m894f8d670cee2a2a|익명 클래스@41.생성자]] — 생성 · L41

### 외부 라이브러리 호출

- `jakarta.servlet.FilterChain#doFilter(jakarta.servlet.ServletRequest,jakarta.servlet.ServletResponse)`
- `jakarta.servlet.ServletResponse#isCommitted()`

### 소스 코드

`src/main/java/com/dodamdodam/backend/auth/session/SessionCookieRenewalFilter.java:37`

```java
@Override
protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                FilterChain filterChain) throws ServletException, IOException {
    // Spring Session의 native 저장/쿠키 기록 후, 실제 응답 커밋 전에 갱신한다.
    filterChain.doFilter(request, new OnCommittedResponseWrapper(response) {
        @Override
        protected void onResponseCommitted() {
            renew(request, response);
        }
    });
    if (!response.isCommitted()) renew(request, response);
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## renew me6ea84e6a538169a

native 세션·업무 문맥 재검사 뒤 쿠키를 갱신하며 확인 장애는 인증 오류로 전달한다.

```java
private void renew(jakarta.servlet.http.HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.SessionCookieRenewalFilter#doFilterInternal m5b3b8f1a080501e9|SessionCookieRenewalFilter.doFilterInternal]] — 호출 · L47
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.SessionCookieRenewalFilter_anonymous_41#onResponseCommitted m55ab85a891497417|익명 클래스@41.onResponseCommitted]] — 호출 · L44

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthStateUnavailableException#생성자 m2ecef5ced435fdbf|AuthStateUnavailableException.생성자]] — 생성 · L70
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthenticatedSessionService#requireStoredSession mc7d6b5fbad4be98b|AuthenticatedSessionService.requireStoredSession]] — 호출 · L62
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthenticatedSessionService.CookieRenewal#context m9887be4afb5e778d|CookieRenewal.context]] — 호출 · L62
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthenticatedSessionService.CookieRenewal#sessionId m113a7cfb4a1939b0|CookieRenewal.sessionId]] — 호출 · L61,77
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.SessionCookieRenewalFilter.InvalidSessionException#생성자 ma03397eb8f992755|InvalidSessionException.생성자]] — 생성 · L66

### 외부 라이브러리 호출

- `jakarta.servlet.ServletRequest#getAttribute(java.lang.String)`
- `jakarta.servlet.ServletRequest#removeAttribute(java.lang.String)`
- `jakarta.servlet.http.HttpServletResponse#addHeader(java.lang.String,java.lang.String)`
- `jakarta.servlet.http.HttpServletResponse#getHeaders(java.lang.String)`
- `jakarta.servlet.http.HttpServletResponse#getStatus()`
- `jakarta.servlet.http.HttpServletResponse#setHeader(java.lang.String,java.lang.String)`
- `java.lang.Iterable#forEach(java.util.function.Consumer<? super T>)`
- `java.lang.String#startsWith(java.lang.String)`
- `java.net.HttpCookie#getMaxAge()`
- `java.net.HttpCookie#parse(java.lang.String)`
- `java.time.Duration#isNegative()`
- `java.util.Collection#stream()`
- `java.util.stream.Stream#anyMatch(java.util.function.Predicate<? super T>)`
- `java.util.stream.Stream#filter(java.util.function.Predicate<? super T>)`
- `java.util.stream.Stream#flatMap(java.util.function.Function<? super T,? extends java.util.stream.Stream<? extends R>>)`
- `java.util.stream.Stream#toList()`
- `org.springframework.session.Session#getMaxInactiveInterval()`
- `org.springframework.session.jdbc.JdbcIndexedSessionRepository#findById(java.lang.String)`
- `org.springframework.session.web.http.CookieSerializer#writeCookieValue(org.springframework.session.web.http.CookieSerializer.CookieValue)`
- `org.springframework.session.web.http.CookieSerializer.CookieValue#<init>(jakarta.servlet.http.HttpServletRequest,jakarta.servlet.http.HttpServletResponse,java.lang.String)`
- `org.springframework.session.web.http.CookieSerializer.CookieValue#setCookieMaxAge(int)`

### 소스 코드

`src/main/java/com/dodamdodam/backend/auth/session/SessionCookieRenewalFilter.java:51`

```java
private void renew(HttpServletRequest request, HttpServletResponse response) {
    var marker = request.getAttribute(AuthenticatedSessionService.COOKIE_RENEWAL);
    request.removeAttribute(AuthenticatedSessionService.COOKIE_RENEWAL);
    if (!(marker instanceof AuthenticatedSessionService.CookieRenewal renewal) || response.getStatus() >= 400) return;
    var headers = response.getHeaders("Set-Cookie");
    if (headers.stream().filter(value -> value.startsWith("JSESSIONID="))
            .flatMap(value -> HttpCookie.parse(value).stream()).anyMatch(cookie -> cookie.getMaxAge() == 0)) return;

    Session stored;
    try {
        stored = sessions.findById(renewal.sessionId());
        authentication.requireStoredSession(stored, renewal.context());
    }
    catch (AuthenticationException exception) {
        // 기존 본문에 오류를 덧붙이지 않도록 Security 바깥의 오류 필터에서 처리한다.
        throw new InvalidSessionException();
    }
    catch (DataAccessException | TransactionException | ConversionException failure) {
        // 프레임워크 세션 재조회 실패도 업무 오류500과 구분해 외곽 필터에서503으로 처리한다.
        throw new AuthStateUnavailableException(failure);
    }
    if (!stored.getMaxInactiveInterval().isNegative()) return;

    var otherCookies = headers.stream().filter(value -> !value.startsWith("JSESSIONID=")).toList();
    response.setHeader("Set-Cookie", null);
    otherCookies.forEach(value -> response.addHeader("Set-Cookie", value));
    var value = new CookieSerializer.CookieValue(request, response, renewal.sessionId());
    value.setCookieMaxAge(31536000);
    cookies.writeCookieValue(value);
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## 생성자 m84764fa9211fa561

아래 원본 코드에서 구현을 확인한다.

```java
public SessionCookieRenewalFilter(org.springframework.session.web.http.CookieSerializer cookies, org.springframework.session.jdbc.JdbcIndexedSessionRepository sessions, com.dodamdodam.backend.auth.session.AuthenticatedSessionService authentication)
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 외부 라이브러리 호출

- `org.springframework.web.filter.OncePerRequestFilter#<init>()`

### 소스 코드

`src/main/java/com/dodamdodam/backend/auth/session/SessionCookieRenewalFilter.java:30`

```java
public SessionCookieRenewalFilter(CookieSerializer cookies, JdbcIndexedSessionRepository sessions,
                                 AuthenticatedSessionService authentication) {
    this.cookies = cookies;
    this.sessions = sessions;
    this.authentication = authentication;
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]
