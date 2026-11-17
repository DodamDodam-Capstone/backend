---
tags: [backend, code-map, auth]
aliases: ["AuthStateErrorFilter"]
---
# AuthStateErrorFilter

`com.dodamdodam.backend.auth.session.AuthStateErrorFilter`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|시작]] → [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요]] → 이 클래스 · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/90-Reference|전체 참조 자료]]

## 클래스 요약

MVC 밖의 인증 장애를 처리하고 성공 응답을 보내기 직전 권한을 재검사한다.

- 원본: `src/main/java/com/dodamdodam/backend/auth/session/AuthStateErrorFilter.java:27`
- 작성 메서드/생성자 5개 · 자동 생성 0개

## 메서드 목록

| 메서드 | 하는 일 |
| --- | --- |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthStateErrorFilter#doFilterInternal m039987860b5cf49f\|doFilterInternal]] | 실제 전송 전과 미커밋 종료 시 권한을 확인하고 실패한 성공 응답을 비운다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthStateErrorFilter#verifyResponseAccess m1098528f38d3ff11\|verifyResponseAccess]] | 로그인 최종 검증을 유지하고 성공 상태인 보호자·이메일 발급 응답을 DB에서 재검사한다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthStateErrorFilter#findAuthStateFailure me3d00accc7019f58\|findAuthStateFailure]] | 명시적인 인증·보호자 오류와 MVC 밖의 프레임워크 세션 장애를 찾는다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthStateErrorFilter#resetForError ma24d1ee96540d5f8\|resetForError]] | 아직 전송하지 않은 성공 본문·전송 길이·위치·인증 쿠키를 공통 헤더와 분리해 제거한다. |

> [!example]- 생성자·자동 생성 멤버 1개
>
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthStateErrorFilter#생성자 m4591444c9aecae10|public AuthStateErrorFilter(AuthStateErrorHandler errorHandler, SessionLifecycleService lifecycle, GuardianAccessService guardian, JsonMapper jsonMapper, EmailVerificationIssuanceService email)]]

## doFilterInternal m039987860b5cf49f

실제 전송 전과 미커밋 종료 시 권한을 확인하고 실패한 성공 응답을 비운다.

```java
protected void doFilterInternal(jakarta.servlet.http.HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response, jakarta.servlet.FilterChain filterChain)
```

설정: `@java.lang.Override`

### 이 메서드를 호출하는 곳

- 프레임워크 진입: Servlet / Spring 필터 실행

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.exception.EmailVerificationException#code m8644eccb71cd5266|EmailVerificationException.code]] — 호출 · L79
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.exception.EmailVerificationException#status m20af4176d3ed6553|EmailVerificationException.status]] — 호출 · L79
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthStateErrorFilter#findAuthStateFailure me3d00accc7019f58|AuthStateErrorFilter.findAuthStateFailure]] — 호출 · L73
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthStateErrorFilter#resetForError ma24d1ee96540d5f8|AuthStateErrorFilter.resetForError]] — 호출 · L77
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthStateErrorFilter#verifyResponseAccess m1098528f38d3ff11|AuthStateErrorFilter.verifyResponseAccess]] — 호출 · L60
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthStateErrorFilter_anonymous_52#생성자 m0d54bee888c1dbbc|익명 클래스@52.생성자]] — 생성 · L52
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthStateErrorHandler#commence ma881aa0732f65535|AuthStateErrorHandler.commence]] — 호출 · L85
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthStateErrorHandler#unavailable md0b4637155a51bcc|AuthStateErrorHandler.unavailable]] — 호출 · L88
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.SessionLifecycleService#failLogin m8a418625ae91ad24|SessionLifecycleService.failLogin]] — 호출 · L67
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.SessionLifecycleService#hasPendingLogin m6ec9457b18ffe30a|SessionLifecycleService.hasPendingLogin]] — 호출 · L64
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.error.ApiErrorResponses#write m1b723e7027f0ade4|ApiErrorResponses.write]] — 호출 · L79,82
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/guardian.exception.GuardianAccessException#code m7dd5b7198c3b7a07|GuardianAccessException.code]] — 호출 · L82
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/guardian.exception.GuardianAccessException#status md61668cf15a2751a|GuardianAccessException.status]] — 호출 · L82

### 외부 라이브러리 호출

- `jakarta.servlet.FilterChain#doFilter(jakarta.servlet.ServletRequest,jakarta.servlet.ServletResponse)`
- `jakarta.servlet.ServletResponse#isCommitted()`
- `java.lang.Throwable#addSuppressed(java.lang.Throwable)`
- `java.lang.Throwable#getMessage()`
- `org.springframework.security.authentication.InsufficientAuthenticationException#<init>(java.lang.String)`

### 소스 코드

`src/main/java/com/dodamdodam/backend/auth/session/AuthStateErrorFilter.java:48`

```java
@Override
protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                FilterChain filterChain) throws ServletException, IOException {
    try {
        filterChain.doFilter(request, new OnCommittedResponseWrapper(response) {
            /** native 저장·쿠키 처리 뒤 실제 전송 전에 현재 권한을 확인한다. */
            @Override
            protected void onResponseCommitted() {
                verifyResponseAccess(request, response);
            }
        });
        if (!response.isCommitted()) {
            verifyResponseAccess(request, response);
        }
    }
    catch (ServletException | IOException | RuntimeException exception) {
        boolean loginFailure = lifecycle.hasPendingLogin(request);
        if (loginFailure) {
            try {
                lifecycle.failLogin(request);
            }
            catch (RuntimeException compensationFailure) {
                exception.addSuppressed(compensationFailure);
            }
        }
        Throwable failure = findAuthStateFailure(exception);
        if ((!loginFailure && failure == null) || response.isCommitted()) {
            throw exception;
        }
        resetForError(response);
        if (!loginFailure && failure instanceof EmailVerificationException access) {
            ApiErrorResponses.write(jsonMapper, response, access.status(), access.code(), access.getMessage());
        }
        else if (!loginFailure && failure instanceof GuardianAccessException access) {
            ApiErrorResponses.write(jsonMapper, response, access.status(), access.code(), access.getMessage());
        }
        else if (!loginFailure && failure instanceof SessionCookieRenewalFilter.InvalidSessionException) {
            errorHandler.commence(request, response, new InsufficientAuthenticationException("로그인이 필요합니다."));
        }
        else {
            errorHandler.unavailable(response);
        }
    }
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## verifyResponseAccess m1098528f38d3ff11

로그인 최종 검증을 유지하고 성공 상태인 보호자·이메일 발급 응답을 DB에서 재검사한다.

```java
private void verifyResponseAccess(jakarta.servlet.http.HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthStateErrorFilter#doFilterInternal m039987860b5cf49f|AuthStateErrorFilter.doFilterInternal]] — 호출 · L60
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthStateErrorFilter_anonymous_52#onResponseCommitted mada22b8cbf51ec3e|익명 클래스@52.onResponseCommitted]] — 호출 · L56

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.EmailVerificationIssuanceService#verifyResponseAccess me8292ce668aadbdf|EmailVerificationIssuanceService.verifyResponseAccess]] — 호출 · L99
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.SessionLifecycleService#verifyLogin mf3c5d20aa396347e|SessionLifecycleService.verifyLogin]] — 호출 · L95
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/guardian.application.GuardianAccessService#verifyResponseAccess m08ab0b3e274d6cb9|GuardianAccessService.verifyResponseAccess]] — 호출 · L98

### 외부 라이브러리 호출

- `jakarta.servlet.http.HttpServletResponse#getStatus()`

### 소스 코드

`src/main/java/com/dodamdodam/backend/auth/session/AuthStateErrorFilter.java:94`

```java
private void verifyResponseAccess(HttpServletRequest request, HttpServletResponse response) {
    lifecycle.verifyLogin(request);
    // 업무 오류 응답을 guardian 오류로 덮어쓰거나 오류 직렬화 중 재검사를 반복하지 않는다.
    if (response.getStatus() < 400) {
        guardian.verifyResponseAccess(request);
        email.verifyResponseAccess(request);
    }
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## findAuthStateFailure me3d00accc7019f58

명시적인 인증·보호자 오류와 MVC 밖의 프레임워크 세션 장애를 찾는다.

```java
private java.lang.Throwable findAuthStateFailure(java.lang.Throwable failure)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthStateErrorFilter#doFilterInternal m039987860b5cf49f|AuthStateErrorFilter.doFilterInternal]] — 호출 · L73

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 외부 라이브러리 호출

- `java.lang.Throwable#getCause()`

### 소스 코드

`src/main/java/com/dodamdodam/backend/auth/session/AuthStateErrorFilter.java:104`

```java
private Throwable findAuthStateFailure(Throwable failure) {
    // Spring Session 저장 실패는 MVC 밖에서도 발생하므로 기존 저장소 오류 분류를 유지한다.
    while (failure != null && !(failure instanceof DataAccessException)
            && !(failure instanceof TransactionException) && !(failure instanceof ConversionException)
            && !(failure instanceof AuthStateUnavailableException)
            && !(failure instanceof GuardianAccessException)
            && !(failure instanceof EmailVerificationException)
            && !(failure instanceof SessionCookieRenewalFilter.InvalidSessionException)) {
        failure = failure.getCause();
    }
    return failure;
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## resetForError ma24d1ee96540d5f8

아직 전송하지 않은 성공 본문·전송 길이·위치·인증 쿠키를 공통 헤더와 분리해 제거한다.

```java
private void resetForError(jakarta.servlet.http.HttpServletResponse response)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthStateErrorFilter#doFilterInternal m039987860b5cf49f|AuthStateErrorFilter.doFilterInternal]] — 호출 · L77

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 외부 라이브러리 호출

- `jakarta.servlet.ServletResponse#reset()`
- `jakarta.servlet.http.HttpServletResponse#addHeader(java.lang.String,java.lang.String)`
- `jakarta.servlet.http.HttpServletResponse#getHeaderNames()`
- `jakarta.servlet.http.HttpServletResponse#getHeaders(java.lang.String)`
- `java.lang.Iterable#forEach(java.util.function.Consumer<? super T>)`
- `java.lang.String#equalsIgnoreCase(java.lang.String)`
- `java.lang.String#startsWith(java.lang.String)`
- `java.util.Collection#stream()`
- `java.util.HashMap#put(K,V)`
- `java.util.LinkedHashMap#<init>()`
- `java.util.LinkedHashMap#forEach(java.util.function.BiConsumer<? super K,? super V>)`
- `java.util.stream.Stream#filter(java.util.function.Predicate<? super T>)`
- `java.util.stream.Stream#toList()`

### 소스 코드

`src/main/java/com/dodamdodam/backend/auth/session/AuthStateErrorFilter.java:118`

```java
private void resetForError(HttpServletResponse response) {
    var headers = new LinkedHashMap<String, List<String>>();
    for (var name : response.getHeaderNames()) {
        if (name.equalsIgnoreCase("Content-Length") || name.equalsIgnoreCase("Content-Type")
                || name.equalsIgnoreCase("Location")) continue;
        headers.put(name, response.getHeaders(name).stream()
                .filter(value -> !name.equalsIgnoreCase("Set-Cookie") || !value.startsWith("JSESSIONID=")).toList());
    }
    // resetBuffer는 Writer/OutputStream 선택을 해제하지 않아 오류 JSON 쓰기가 실패할 수 있다.
    response.reset();
    headers.forEach((name, values) -> values.forEach(value -> response.addHeader(name, value)));
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## 생성자 m4591444c9aecae10

로그인 보상과 보호자·이메일 발급의 응답 재검사·공통 오류 경로를 구성한다.

```java
public AuthStateErrorFilter(com.dodamdodam.backend.auth.session.AuthStateErrorHandler errorHandler, com.dodamdodam.backend.auth.session.SessionLifecycleService lifecycle, com.dodamdodam.backend.guardian.application.GuardianAccessService guardian, tools.jackson.databind.json.JsonMapper jsonMapper, com.dodamdodam.backend.auth.application.EmailVerificationIssuanceService email)
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 외부 라이브러리 호출

- `org.springframework.web.filter.OncePerRequestFilter#<init>()`

### 소스 코드

`src/main/java/com/dodamdodam/backend/auth/session/AuthStateErrorFilter.java:38`

```java
public AuthStateErrorFilter(AuthStateErrorHandler errorHandler, SessionLifecycleService lifecycle,
                            GuardianAccessService guardian, JsonMapper jsonMapper, EmailVerificationIssuanceService email) {
    this.errorHandler = errorHandler;
    this.lifecycle = lifecycle;
    this.guardian = guardian;
    this.jsonMapper = jsonMapper;
    this.email = email;
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]
