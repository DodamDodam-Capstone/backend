---
tags: [backend, code-map, global]
aliases: ["CsrfAccessDeniedHandler"]
---
# CsrfAccessDeniedHandler

`com.dodamdodam.backend.global.auth.CsrfAccessDeniedHandler`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|시작]] → [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/global|기능 개요]] → 이 클래스 · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/90-Reference|전체 참조 자료]]

## 클래스 요약

CSRF 거부를 기존 고정 메시지와 공통 JSON 형식으로 반환한다.

- 원본: `src/main/java/com/dodamdodam/backend/global/auth/CsrfAccessDeniedHandler.java:12`
- 작성 메서드/생성자 2개 · 자동 생성 0개

## 메서드 목록

| 메서드 | 하는 일 |
| --- | --- |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.auth.CsrfAccessDeniedHandler#handle m9adc60284e9c3f84\|handle]] | CSRF 거부를 기존 고정 메시지와 공통 JSON 형식으로 반환한다. |

> [!example]- 생성자·자동 생성 멤버 1개
>
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.auth.CsrfAccessDeniedHandler#생성자 m79ec07fd53717638|public CsrfAccessDeniedHandler(JsonMapper jsonMapper)]]

## handle m9adc60284e9c3f84

CSRF 거부를 기존 고정 메시지와 공통 JSON 형식으로 반환한다.

```java
public void handle(jakarta.servlet.http.HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response, org.springframework.security.access.AccessDeniedException exception)
```

설정: `@java.lang.Override`

### 이 메서드를 호출하는 곳

- 프레임워크 진입: Spring Security CSRF 거부

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.error.ApiErrorResponses#write m1b723e7027f0ade4|ApiErrorResponses.write]] — 호출 · L25

### 소스 코드

`src/main/java/com/dodamdodam/backend/global/auth/CsrfAccessDeniedHandler.java:22`

```java
@Override
public void handle(HttpServletRequest request, HttpServletResponse response,
                   AccessDeniedException exception) throws IOException {
    ApiErrorResponses.write(jsonMapper, response, HttpServletResponse.SC_FORBIDDEN,
            "CSRF_INVALID", "CSRF 토큰을 다시 발급받아 주세요.");
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/global|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## 생성자 m79ec07fd53717638

아래 원본 코드에서 구현을 확인한다.

```java
public CsrfAccessDeniedHandler(tools.jackson.databind.json.JsonMapper jsonMapper)
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 외부 라이브러리 호출

- `java.lang.Object#<init>()`

### 소스 코드

`src/main/java/com/dodamdodam/backend/global/auth/CsrfAccessDeniedHandler.java:17`

```java
public CsrfAccessDeniedHandler(JsonMapper jsonMapper) {
    this.jsonMapper = jsonMapper;
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/global|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]
