---
tags: [backend, code-map, global]
aliases: ["Part1JsonRequestGuardFilter"]
---
# Part1JsonRequestGuardFilter

`com.dodamdodam.backend.global.web.Part1JsonRequestGuardFilter`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|시작]] → [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/global|기능 개요]] → 이 클래스 · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/90-Reference|전체 참조 자료]]

## 클래스 요약

로그아웃·아이 등록·이메일 발급 요청의 본문 크기와 JSON 전송 형식을 검사한다.

- 원본: `src/main/java/com/dodamdodam/backend/global/web/Part1JsonRequestGuardFilter.java:24`
- 작성 메서드/생성자 4개 · 자동 생성 0개

## 메서드 목록

| 메서드 | 하는 일 |
| --- | --- |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.web.Part1JsonRequestGuardFilter#shouldNotFilter m5dbc4dd1b63be391\|shouldNotFilter]] | 로그아웃·자녀 등록·이메일 발급 POST에 전송 크기·형식 검사를 적용한다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.web.Part1JsonRequestGuardFilter#doFilterInternal m647a0f12b7d38584\|doFilterInternal]] | CSRF 전에 전송 규칙을 검사하고, 후속 MVC가 읽을 수 있도록 본문을 보관한다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.web.Part1JsonRequestGuardFilter#supportsContentType m7db3dd96f6b4a375\|supportsContentType]] | JSON·UTF-8을 허용하고, Content-Type 없는 빈 본문은 로그아웃에만 허용한다. |

> [!example]- 생성자·자동 생성 멤버 1개
>
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.web.Part1JsonRequestGuardFilter#생성자 m0f91a8b84e7b5b2c|public Part1JsonRequestGuardFilter(JsonMapper jsonMapper)]]

## shouldNotFilter m5dbc4dd1b63be391

로그아웃·자녀 등록·이메일 발급 POST에 전송 크기·형식 검사를 적용한다.

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

`src/main/java/com/dodamdodam/backend/global/web/Part1JsonRequestGuardFilter.java:42`

```java
@Override
protected boolean shouldNotFilter(HttpServletRequest request) {
    return !LOGOUT.matches(request) && !CHILDREN.matches(request) && !EMAIL.matches(request);
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/global|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## doFilterInternal m647a0f12b7d38584

CSRF 전에 전송 규칙을 검사하고, 후속 MVC가 읽을 수 있도록 본문을 보관한다.

```java
protected void doFilterInternal(jakarta.servlet.http.HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response, jakarta.servlet.FilterChain filterChain)
```

설정: `@java.lang.Override`

### 이 메서드를 호출하는 곳

- 프레임워크 진입: Servlet / Spring 필터 실행

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.error.ApiErrorResponses#write m1b723e7027f0ade4|ApiErrorResponses.write]] — 호출 · L54,59
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.web.Part1JsonRequestGuardFilter#supportsContentType m7db3dd96f6b4a375|Part1JsonRequestGuardFilter.supportsContentType]] — 호출 · L58
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.web.Part1JsonRequestGuardFilter.CachedBodyRequest#생성자 md064e3f783df1f61|CachedBodyRequest.생성자]] — 생성 · L65

### 외부 라이브러리 호출

- `jakarta.servlet.FilterChain#doFilter(jakarta.servlet.ServletRequest,jakarta.servlet.ServletResponse)`
- `jakarta.servlet.ServletInputStream#readNBytes(int)`
- `jakarta.servlet.ServletRequest#getInputStream()`
- `jakarta.servlet.ServletRequest#setAttribute(java.lang.String,java.lang.Object)`

### 소스 코드

`src/main/java/com/dodamdodam/backend/global/web/Part1JsonRequestGuardFilter.java:48`

```java
@Override
protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                FilterChain filterChain) throws ServletException, IOException {
    // Content-Length를 신뢰하지 않고 최대 크기보다 한 바이트만 더 읽어 초과 여부를 판정한다.
    byte[] body = request.getInputStream().readNBytes(MAX_BODY_BYTES + 1);
    if (body.length > MAX_BODY_BYTES) {
        ApiErrorResponses.write(jsonMapper, response, 413,
                "REQUEST_TOO_LARGE", "요청 본문이 허용 크기를 초과했습니다.");
        return;
    }
    if (!supportsContentType(request, body.length == 0)) {
        ApiErrorResponses.write(jsonMapper, response, 415,
                "UNSUPPORTED_MEDIA_TYPE", "지원하지 않는 Content-Type입니다.");
        return;
    }
    // 빈 로그아웃과 자녀·이메일 JSON 구조 검증은 CSRF·조건부 로그인 검사 이후에 수행한다.
    request.setAttribute(EMPTY_BODY, body.length == 0);
    filterChain.doFilter(new CachedBodyRequest(request, body), response);
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/global|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## supportsContentType m7db3dd96f6b4a375

JSON·UTF-8을 허용하고, Content-Type 없는 빈 본문은 로그아웃에만 허용한다.

```java
private boolean supportsContentType(jakarta.servlet.http.HttpServletRequest request, boolean emptyBody)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.web.Part1JsonRequestGuardFilter#doFilterInternal m647a0f12b7d38584|Part1JsonRequestGuardFilter.doFilterInternal]] — 호출 · L58

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 외부 라이브러리 호출

- `jakarta.servlet.ServletRequest#getContentType()`
- `java.lang.String#equals(java.lang.Object)`
- `java.nio.charset.Charset#equals(java.lang.Object)`
- `org.springframework.http.MediaType#parseMediaType(java.lang.String)`
- `org.springframework.security.web.util.matcher.RequestMatcher#matches(jakarta.servlet.http.HttpServletRequest)`
- `org.springframework.util.MimeType#getCharset()`
- `org.springframework.util.MimeType#getSubtype()`
- `org.springframework.util.MimeType#getType()`

### 소스 코드

`src/main/java/com/dodamdodam/backend/global/web/Part1JsonRequestGuardFilter.java:69`

```java
private boolean supportsContentType(HttpServletRequest request, boolean emptyBody) {
    if (request.getContentType() == null) {
        return emptyBody && LOGOUT.matches(request);
    }
    try {
        var type = MediaType.parseMediaType(request.getContentType());
        return type.getType().equals("application") && type.getSubtype().equals("json")
                && (type.getCharset() == null || StandardCharsets.UTF_8.equals(type.getCharset()));
    }
    catch (IllegalArgumentException invalid) {
        return false;
    }
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/global|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## 생성자 m0f91a8b84e7b5b2c

공통 오류 응답에 사용할 JSON 직렬화기를 받는다.

```java
public Part1JsonRequestGuardFilter(tools.jackson.databind.json.JsonMapper jsonMapper)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.config.SecurityConfig#applicationSecurityFilterChain m8a28e16ae80b5e1e|SecurityConfig.applicationSecurityFilterChain]] — 생성 · L101

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 외부 라이브러리 호출

- `org.springframework.web.filter.OncePerRequestFilter#<init>()`

### 소스 코드

`src/main/java/com/dodamdodam/backend/global/web/Part1JsonRequestGuardFilter.java:37`

```java
public Part1JsonRequestGuardFilter(JsonMapper jsonMapper) {
    this.jsonMapper = jsonMapper;
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/global|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]
