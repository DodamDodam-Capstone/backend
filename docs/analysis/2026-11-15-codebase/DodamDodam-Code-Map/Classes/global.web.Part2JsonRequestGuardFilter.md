---
tags: [backend, code-map, global]
aliases: ["Part2JsonRequestGuardFilter"]
---
# Part2JsonRequestGuardFilter

`com.dodamdodam.backend.global.web.Part2JsonRequestGuardFilter`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|시작]] → [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/global|기능 개요]] → 이 클래스 · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/90-Reference|전체 참조 자료]]

## 클래스 요약

Part 2 요청의 JSON 전송 조건을 보안 체인 안에서 검사한다.

- 원본: `src/main/java/com/dodamdodam/backend/global/web/Part2JsonRequestGuardFilter.java:19`
- 작성 메서드/생성자 4개 · 자동 생성 0개

## 메서드 목록

| 메서드 | 하는 일 |
| --- | --- |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.web.Part2JsonRequestGuardFilter#shouldNotFilter m4e06692ca61906ae\|shouldNotFilter]] | 현재 구현된 Part 2 JSON 본문 경로인 대화 시작에만 적용한다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.web.Part2JsonRequestGuardFilter#doFilterInternal m6ee42797d374d4fc\|doFilterInternal]] | CSRF·인증 전에 형식을 거부하고 본문·구조 검증은 후속 처리에 남긴다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.web.Part2JsonRequestGuardFilter#supportsContentType m3ea923c309058637\|supportsContentType]] | 명세의 application/json과 생략 또는 UTF-8인 charset만 허용한다. |

> [!example]- 생성자·자동 생성 멤버 1개
>
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.web.Part2JsonRequestGuardFilter#생성자 mc3d3d138ee1d9ea9|public Part2JsonRequestGuardFilter(JsonMapper jsonMapper)]]

## shouldNotFilter m4e06692ca61906ae

현재 구현된 Part 2 JSON 본문 경로인 대화 시작에만 적용한다.

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

`src/main/java/com/dodamdodam/backend/global/web/Part2JsonRequestGuardFilter.java:32`

```java
@Override
protected boolean shouldNotFilter(HttpServletRequest request) {
    return !START.matches(request);
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/global|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## doFilterInternal m6ee42797d374d4fc

CSRF·인증 전에 형식을 거부하고 본문·구조 검증은 후속 처리에 남긴다.

```java
protected void doFilterInternal(jakarta.servlet.http.HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response, jakarta.servlet.FilterChain filterChain)
```

설정: `@java.lang.Override`

### 이 메서드를 호출하는 곳

- 프레임워크 진입: Servlet / Spring 필터 실행

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.error.ApiErrorResponses#write m1b723e7027f0ade4|ApiErrorResponses.write]] — 호출 · L42
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.web.Part2JsonRequestGuardFilter#supportsContentType m3ea923c309058637|Part2JsonRequestGuardFilter.supportsContentType]] — 호출 · L41

### 외부 라이브러리 호출

- `jakarta.servlet.FilterChain#doFilter(jakarta.servlet.ServletRequest,jakarta.servlet.ServletResponse)`
- `jakarta.servlet.ServletRequest#getContentType()`

### 소스 코드

`src/main/java/com/dodamdodam/backend/global/web/Part2JsonRequestGuardFilter.java:38`

```java
@Override
protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                FilterChain filterChain) throws ServletException, IOException {
    if (!supportsContentType(request.getContentType())) {
        ApiErrorResponses.write(jsonMapper, response, 415,
                "UNSUPPORTED_MEDIA_TYPE", "지원하지 않는 Content-Type입니다.");
        return;
    }
    // Part 2 크기 정책은 미정이므로 스트림을 읽거나 Part 1 제한을 적용하지 않는다.
    filterChain.doFilter(request, response);
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/global|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## supportsContentType m3ea923c309058637

명세의 application/json과 생략 또는 UTF-8인 charset만 허용한다.

```java
private boolean supportsContentType(java.lang.@org.jspecify.annotations.Nullable String contentType)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.web.Part2JsonRequestGuardFilter#doFilterInternal m6ee42797d374d4fc|Part2JsonRequestGuardFilter.doFilterInternal]] — 호출 · L41

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 외부 라이브러리 호출

- `java.lang.String#equals(java.lang.Object)`
- `java.nio.charset.Charset#equals(java.lang.Object)`
- `org.springframework.http.MediaType#parseMediaType(java.lang.String)`
- `org.springframework.util.MimeType#getCharset()`
- `org.springframework.util.MimeType#getSubtype()`
- `org.springframework.util.MimeType#getType()`

### 소스 코드

`src/main/java/com/dodamdodam/backend/global/web/Part2JsonRequestGuardFilter.java:51`

```java
private boolean supportsContentType(@Nullable String contentType) {
    if (contentType == null) {
        return false;
    }
    try {
        var type = MediaType.parseMediaType(contentType);
        return type.getType().equals("application") && type.getSubtype().equals("json")
                && (type.getCharset() == null || StandardCharsets.UTF_8.equals(type.getCharset()));
    }
    catch (IllegalArgumentException invalid) {
        return false;
    }
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/global|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## 생성자 mc3d3d138ee1d9ea9

Security 체인에서만 등록하며 공통 오류 직렬화기를 재사용한다.

```java
public Part2JsonRequestGuardFilter(tools.jackson.databind.json.JsonMapper jsonMapper)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.config.SecurityConfig#applicationSecurityFilterChain m8a28e16ae80b5e1e|SecurityConfig.applicationSecurityFilterChain]] — 생성 · L102

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 외부 라이브러리 호출

- `org.springframework.web.filter.OncePerRequestFilter#<init>()`

### 소스 코드

`src/main/java/com/dodamdodam/backend/global/web/Part2JsonRequestGuardFilter.java:27`

```java
public Part2JsonRequestGuardFilter(JsonMapper jsonMapper) {
    this.jsonMapper = jsonMapper;
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/global|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]
