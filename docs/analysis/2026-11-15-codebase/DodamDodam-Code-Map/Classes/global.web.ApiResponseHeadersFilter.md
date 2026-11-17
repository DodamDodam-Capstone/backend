---
tags: [backend, code-map, global]
aliases: ["ApiResponseHeadersFilter"]
---
# ApiResponseHeadersFilter

`com.dodamdodam.backend.global.web.ApiResponseHeadersFilter`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|시작]] → [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/global|기능 개요]] → 이 클래스 · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/90-Reference|전체 참조 자료]]

## 클래스 요약

공통 요청 식별자와 응답 캐시 정책 헤더를 설정한다.

- 원본: `src/main/java/com/dodamdodam/backend/global/web/ApiResponseHeadersFilter.java:15`
- 작성 메서드/생성자 1개 · 자동 생성 1개

## 메서드 목록

| 메서드 | 하는 일 |
| --- | --- |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.web.ApiResponseHeadersFilter#doFilterInternal mdb9438ca5f22a128\|doFilterInternal]] | 아래 구현과 호출 관계에서 확인 |

> [!example]- 생성자·자동 생성 멤버 1개
>
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.web.ApiResponseHeadersFilter#생성자 mf7f677c7d9ed3ad9|public ApiResponseHeadersFilter() [자동]]]

## doFilterInternal mdb9438ca5f22a128

아래 원본 코드에서 구현을 확인한다.

```java
protected void doFilterInternal(jakarta.servlet.http.HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response, jakarta.servlet.FilterChain filterChain)
```

설정: `@java.lang.Override`

### 이 메서드를 호출하는 곳

- 프레임워크 진입: Servlet / Spring 필터 실행

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 외부 라이브러리 호출

- `jakarta.servlet.FilterChain#doFilter(jakarta.servlet.ServletRequest,jakarta.servlet.ServletResponse)`
- `jakarta.servlet.http.HttpServletResponse#setHeader(java.lang.String,java.lang.String)`
- `java.util.UUID#randomUUID()`
- `java.util.UUID#toString()`

### 소스 코드

`src/main/java/com/dodamdodam/backend/global/web/ApiResponseHeadersFilter.java:19`

```java
@Override
protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                FilterChain filterChain) throws ServletException, IOException {
    response.setHeader("X-Request-ID", UUID.randomUUID().toString());
    response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
    filterChain.doFilter(request, response);
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/global|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## 생성자 mf7f677c7d9ed3ad9

컴파일러가 생성한 멤버다.

```java
public ApiResponseHeadersFilter()
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 외부 라이브러리 호출

- `org.springframework.web.filter.OncePerRequestFilter#<init>()`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/global|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]
