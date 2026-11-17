---
tags: [backend, code-map, conversation]
aliases: ["ConversationStartResponseFilter"]
---
# ConversationStartResponseFilter

`com.dodamdodam.backend.conversation.api.ConversationStartResponseFilter`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|시작]] → [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요]] → 이 클래스 · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/90-Reference|전체 참조 자료]]

## 클래스 요약

대화 시작의 성공 응답 직전에 현재 접근 권한과 대화 상태를 다시 확인한다.

- 원본: `src/main/java/com/dodamdodam/backend/conversation/api/ConversationStartResponseFilter.java:36`
- 작성 메서드/생성자 8개 · 자동 생성 0개

## 메서드 목록

| 메서드 | 하는 일 |
| --- | --- |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.api.ConversationStartResponseFilter#getOrder mf62246aa5dd24a72\|getOrder]] | 보안 체인이 다음 서블릿 필터를 호출하는 동안에만 현재 인증 문맥을 사용한다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.api.ConversationStartResponseFilter#protect m24d9b8b621ce0746\|protect]] | 시작 성공 후 서버가 검증한 문맥과 반환할 대화만 요청 범위에 기록한다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.api.ConversationStartResponseFilter#shouldNotFilter m4d365d855e081fa5\|shouldNotFilter]] | Part 1과 다른 Part 2 경로의 응답은 감싸거나 재검사하지 않는다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.api.ConversationStartResponseFilter#doFilterInternal m2e5a11b062f28145\|doFilterInternal]] | 첫 전송 및 미커밋 반환 직전에 검사하고 실패한 성공 본문·헤더를 원자적으로 비운다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.api.ConversationStartResponseFilter#verify mbaf83885c918b297\|verify]] | 기존 인증을 먼저 재사용하고 성공 대상의 현재 DB 권한·업무 상태를 확인한다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.api.ConversationStartResponseFilter#findRejection m1b2f5e42f5a3039f\|findRejection]] | MVC/서블릿 포장 안의 전송 거부만 찾고 무관한 기존 예외 처리에는 관여하지 않는다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.api.ConversationStartResponseFilter#clearSuccess me633fd53fe2379f2\|clearSuccess]] | requestId·no-store·CORS는 유지하고 성공 길이·Location·인증 갱신 쿠키를 제거한다. |

> [!example]- 생성자·자동 생성 멤버 1개
>
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.api.ConversationStartResponseFilter#생성자 mc8c74f4418cc7938|public ConversationStartResponseFilter(SecurityFilterProperties security, AuthenticatedSessionService authentication, ConversationService conversations, ConversationExceptionHandler conversationErrors, ChildExceptionHandler childErrors, JsonMapper json)]]

## getOrder mf62246aa5dd24a72

보안 체인이 다음 서블릿 필터를 호출하는 동안에만 현재 인증 문맥을 사용한다.

```java
public int getOrder()
```

설정: `@java.lang.Override`

### 이 메서드를 호출하는 곳

- 프레임워크 진입: Servlet / Spring 필터 실행

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 소스 코드

`src/main/java/com/dodamdodam/backend/conversation/api/ConversationStartResponseFilter.java:64`

```java
@Override
public int getOrder() {
    return order;
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## protect m24d9b8b621ce0746

시작 성공 후 서버가 검증한 문맥과 반환할 대화만 요청 범위에 기록한다.

```java
static void protect(jakarta.servlet.http.HttpServletRequest request, com.dodamdodam.backend.auth.session.AuthenticatedSession session, java.util.UUID childId, java.util.UUID clientRequestId, java.util.UUID conversationId)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.api.ConversationController#start m255287826e69c022|ConversationController.start]] — 호출 · L42

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.api.ConversationStartResponseFilter.ResponseAccess#생성자 me4030e71484eb1fc|ResponseAccess.생성자]] — 생성 · L72

### 외부 라이브러리 호출

- `jakarta.servlet.ServletRequest#setAttribute(java.lang.String,java.lang.Object)`

### 소스 코드

`src/main/java/com/dodamdodam/backend/conversation/api/ConversationStartResponseFilter.java:70`

```java
static void protect(HttpServletRequest request, AuthenticatedSession session, UUID childId,
        UUID clientRequestId, UUID conversationId) {
    request.setAttribute(RESPONSE_ACCESS, new ResponseAccess(session, childId, clientRequestId, conversationId));
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## shouldNotFilter m4d365d855e081fa5

Part 1과 다른 Part 2 경로의 응답은 감싸거나 재검사하지 않는다.

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

`src/main/java/com/dodamdodam/backend/conversation/api/ConversationStartResponseFilter.java:76`

```java
@Override
protected boolean shouldNotFilter(HttpServletRequest request) {
    return !START.matches(request);
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## doFilterInternal m2e5a11b062f28145

첫 전송 및 미커밋 반환 직전에 검사하고 실패한 성공 본문·헤더를 원자적으로 비운다.

```java
protected void doFilterInternal(jakarta.servlet.http.HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response, jakarta.servlet.FilterChain filterChain)
```

설정: `@java.lang.Override`

### 이 메서드를 호출하는 곳

- 프레임워크 진입: Servlet / Spring 필터 실행

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.SessionCookieRenewalFilter.InvalidSessionException#생성자 ma03397eb8f992755|InvalidSessionException.생성자]] — 생성 · L106
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.api.ChildExceptionHandler#handle ma451743715a09223|ChildExceptionHandler.handle]] — 호출 · L112
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.api.ConversationExceptionHandler#handle m50c7d1b4e6622616|ConversationExceptionHandler.handle]] — 호출 · L113
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.api.ConversationStartResponseFilter#clearSuccess me633fd53fe2379f2|ConversationStartResponseFilter.clearSuccess]] — 호출 · L103
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.api.ConversationStartResponseFilter#findRejection m1b2f5e42f5a3039f|ConversationStartResponseFilter.findRejection]] — 호출 · L99
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.api.ConversationStartResponseFilter#verify mbaf83885c918b297|ConversationStartResponseFilter.verify]] — 호출 · L95
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.api.ConversationStartResponseFilter_anonymous_86#생성자 mc79168b20210d2f2|익명 클래스@86.생성자]] — 생성 · L86
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.exception.ConversationException#생성자 mdf07793b291a512a|ConversationException.생성자]] — 생성 · L114

### 외부 라이브러리 호출

- `jakarta.servlet.FilterChain#doFilter(jakarta.servlet.ServletRequest,jakarta.servlet.ServletResponse)`
- `jakarta.servlet.ServletResponse#getOutputStream()`
- `jakarta.servlet.ServletResponse#isCommitted()`
- `jakarta.servlet.ServletResponse#setCharacterEncoding(java.lang.String)`
- `jakarta.servlet.ServletResponse#setContentType(java.lang.String)`
- `jakarta.servlet.http.HttpServletResponse#setStatus(int)`
- `java.nio.charset.Charset#name()`
- `org.springframework.http.HttpEntity#getBody()`
- `org.springframework.http.HttpStatusCode#value()`
- `org.springframework.http.ResponseEntity#getStatusCode()`
- `tools.jackson.databind.ObjectMapper#writeValue(java.io.OutputStream,java.lang.Object)`

### 소스 코드

`src/main/java/com/dodamdodam/backend/conversation/api/ConversationStartResponseFilter.java:82`

```java
@Override
protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
        FilterChain filterChain) throws ServletException, IOException {
    try {
        filterChain.doFilter(request, new OnCommittedResponseWrapper(response) {
            /** MVC 직렬화·flush가 시작해도 거부된 성공 바이트를 그대로 내보내지 않는다. */
            @Override
            protected void onResponseCommitted() {
                disableOnResponseCommitted();
                verify(request, response);
            }
        });
        if (!response.isCommitted()) {
            verify(request, response);
        }
    }
    catch (ServletException | IOException | RuntimeException failure) {
        var rejection = findRejection(failure);
        if (rejection == null || response.isCommitted()) {
            throw failure;
        }
        clearSuccess(response);
        if (rejection instanceof AuthenticationException) {
            // 실제 세션 폐기·저장 확인과 401/503 변환은 기존 외곽 인증 필터가 수행한다.
            throw new InvalidSessionException();
        }
        if (rejection instanceof AuthStateUnavailableException unavailable) {
            throw unavailable;
        }
        var error = rejection instanceof ChildException child
                ? childErrors.handle(child, response)
                : conversationErrors.handle(rejection instanceof ConversationException conversation
                        ? conversation : new ConversationException(rejection), response);
        response.setStatus(error.getStatusCode().value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        json.writeValue(response.getOutputStream(), error.getBody());
    }
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## verify mbaf83885c918b297

기존 인증을 먼저 재사용하고 성공 대상의 현재 DB 권한·업무 상태를 확인한다.

```java
private void verify(jakarta.servlet.http.HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.api.ConversationStartResponseFilter#doFilterInternal m2e5a11b062f28145|ConversationStartResponseFilter.doFilterInternal]] — 호출 · L95
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.api.ConversationStartResponseFilter_anonymous_86#onResponseCommitted m22eb7ff8f3884b7b|익명 클래스@86.onResponseCommitted]] — 호출 · L91

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthenticatedSession#equals mbf17f3450c6bf2bd|AuthenticatedSession.equals]] — 호출 · L130
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthenticatedSessionService#requireAuthenticatedSession mad23075b25d06a28|AuthenticatedSessionService.requireAuthenticatedSession]] — 호출 · L129
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.api.ConversationStartResponseFilter.ResponseAccess#childId m7fd4694b4ed0c52a|ResponseAccess.childId]] — 호출 · L133
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.api.ConversationStartResponseFilter.ResponseAccess#clientRequestId m0b21d49b4ff10bd6|ResponseAccess.clientRequestId]] — 호출 · L133
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.api.ConversationStartResponseFilter.ResponseAccess#conversationId ma781fe8547752389|ResponseAccess.conversationId]] — 호출 · L133
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.api.ConversationStartResponseFilter.ResponseAccess#session m9b08ba3a05f87171|ResponseAccess.session]] — 호출 · L130
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.api.ConversationStartResponseFilter.ResponseRejected#생성자 m6e5c89886a9991f7|ResponseRejected.생성자]] — 생성 · L137
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationService#verifyStartResponse macdb3aca10ad50df|ConversationService.verifyStartResponse]] — 호출 · L133

### 외부 라이브러리 호출

- `jakarta.servlet.ServletRequest#getAttribute(java.lang.String)`
- `jakarta.servlet.http.HttpServletResponse#getStatus()`
- `org.springframework.security.authentication.InsufficientAuthenticationException#<init>(java.lang.String)`

### 소스 코드

`src/main/java/com/dodamdodam/backend/conversation/api/ConversationStartResponseFilter.java:123`

```java
private void verify(HttpServletRequest request, HttpServletResponse response) {
    if ((response.getStatus() != 200 && response.getStatus() != 201)
            || !(request.getAttribute(RESPONSE_ACCESS) instanceof ResponseAccess expected)) {
        return;
    }
    try {
        var current = authentication.requireAuthenticatedSession(request);
        if (!expected.session().equals(current)) {
            throw new InsufficientAuthenticationException("로그인이 필요합니다.");
        }
        conversations.verifyStartResponse(current, expected.childId(), expected.clientRequestId(), expected.conversationId());
    }
    catch (RuntimeException rejection) {
        // MVC가 부분 직렬화 실패를 일반 500으로 소비하기 전에 이 필터까지 되돌린다.
        throw new ResponseRejected(rejection);
    }
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## findRejection m1b2f5e42f5a3039f

MVC/서블릿 포장 안의 전송 거부만 찾고 무관한 기존 예외 처리에는 관여하지 않는다.

```java
private java.lang.@org.jspecify.annotations.Nullable RuntimeException findRejection(java.lang.Throwable failure)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.api.ConversationStartResponseFilter#doFilterInternal m2e5a11b062f28145|ConversationStartResponseFilter.doFilterInternal]] — 호출 · L99

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 외부 라이브러리 호출

- `java.lang.Throwable#getCause()`

### 소스 코드

`src/main/java/com/dodamdodam/backend/conversation/api/ConversationStartResponseFilter.java:142`

```java
private @Nullable RuntimeException findRejection(Throwable failure) {
    for (@Nullable Throwable current = failure; current != null; current = current.getCause()) {
        if (current instanceof ResponseRejected rejected) {
            return rejected.rejection;
        }
    }
    return null;
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## clearSuccess me633fd53fe2379f2

requestId·no-store·CORS는 유지하고 성공 길이·Location·인증 갱신 쿠키를 제거한다.

```java
private void clearSuccess(jakarta.servlet.http.HttpServletResponse response)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.api.ConversationStartResponseFilter#doFilterInternal m2e5a11b062f28145|ConversationStartResponseFilter.doFilterInternal]] — 호출 · L103

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

`src/main/java/com/dodamdodam/backend/conversation/api/ConversationStartResponseFilter.java:152`

```java
private void clearSuccess(HttpServletResponse response) {
    var headers = new LinkedHashMap<String, List<String>>();
    for (var name : response.getHeaderNames()) {
        if (name.equalsIgnoreCase("Content-Length") || name.equalsIgnoreCase("Content-Type")
                || name.equalsIgnoreCase("Location")) {
            continue;
        }
        headers.put(name, response.getHeaders(name).stream()
                .filter(value -> !name.equalsIgnoreCase("Set-Cookie") || !value.startsWith("JSESSIONID=")).toList());
    }
    // Writer/OutputStream 선택도 초기화해 오류 JSON에 성공 본문이 섞이지 않게 한다.
    response.reset();
    headers.forEach((name, values) -> values.forEach(value -> response.addHeader(name, value)));
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## 생성자 mc8c74f4418cc7938

기존 필터 순서는 유지하고 Security 위임 필터 바로 뒤에 한 번 등록한다.

```java
public ConversationStartResponseFilter(org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterProperties security, com.dodamdodam.backend.auth.session.AuthenticatedSessionService authentication, com.dodamdodam.backend.conversation.application.ConversationService conversations, com.dodamdodam.backend.conversation.api.ConversationExceptionHandler conversationErrors, com.dodamdodam.backend.child.api.ChildExceptionHandler childErrors, tools.jackson.databind.json.JsonMapper json)
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 외부 라이브러리 호출

- `java.lang.Math#addExact(int,int)`
- `org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterProperties#getOrder()`
- `org.springframework.web.filter.OncePerRequestFilter#<init>()`

### 소스 코드

`src/main/java/com/dodamdodam/backend/conversation/api/ConversationStartResponseFilter.java:52`

```java
public ConversationStartResponseFilter(SecurityFilterProperties security,
        AuthenticatedSessionService authentication, ConversationService conversations,
        ConversationExceptionHandler conversationErrors, ChildExceptionHandler childErrors, JsonMapper json) {
    this.order = Math.addExact(security.getOrder(), 1);
    this.authentication = authentication;
    this.conversations = conversations;
    this.conversationErrors = conversationErrors;
    this.childErrors = childErrors;
    this.json = json;
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]
