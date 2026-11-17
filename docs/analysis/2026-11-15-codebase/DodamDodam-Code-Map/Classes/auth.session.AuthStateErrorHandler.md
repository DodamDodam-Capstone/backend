---
tags: [backend, code-map, auth]
aliases: ["AuthStateErrorHandler"]
---
# AuthStateErrorHandler

`com.dodamdodam.backend.auth.session.AuthStateErrorHandler`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|시작]] → [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요]] → 이 클래스 · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/90-Reference|전체 참조 자료]]

## 클래스 요약

인증이 필요한 요청에 기존 401 오류를 반환한다.

- 원본: `src/main/java/com/dodamdodam/backend/auth/session/AuthStateErrorHandler.java:12`
- 작성 메서드/생성자 3개 · 자동 생성 0개

## 메서드 목록

| 메서드 | 하는 일 |
| --- | --- |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthStateErrorHandler#commence ma881aa0732f65535\|commence]] | 인증이 필요한 요청에 기존 401 오류를 반환한다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthStateErrorHandler#unavailable md0b4637155a51bcc\|unavailable]] | 인증 상태를 확인할 수 없을 때 기존 503 오류를 반환한다. |

> [!example]- 생성자·자동 생성 멤버 1개
>
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthStateErrorHandler#생성자 m8796e45b357cb747|public AuthStateErrorHandler(JsonMapper jsonMapper)]]

## commence ma881aa0732f65535

인증이 필요한 요청에 기존 401 오류를 반환한다.

```java
public void commence(jakarta.servlet.http.HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response, org.springframework.security.core.AuthenticationException exception)
```

설정: `@java.lang.Override`

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthStateErrorFilter#doFilterInternal m039987860b5cf49f|AuthStateErrorFilter.doFilterInternal]] — 호출 · L85
- 프레임워크 진입: Spring Security 인증 실패

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.error.ApiErrorResponses#write m1b723e7027f0ade4|ApiErrorResponses.write]] — 호출 · L25

### 소스 코드

`src/main/java/com/dodamdodam/backend/auth/session/AuthStateErrorHandler.java:22`

```java
@Override
public void commence(HttpServletRequest request, HttpServletResponse response,
                     AuthenticationException exception) throws IOException {
    ApiErrorResponses.write(jsonMapper, response, 401, "AUTHENTICATION_REQUIRED", "로그인이 필요합니다.");
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## unavailable md0b4637155a51bcc

인증 상태를 확인할 수 없을 때 기존 503 오류를 반환한다.

```java
public void unavailable(jakarta.servlet.http.HttpServletResponse response)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthStateErrorFilter#doFilterInternal m039987860b5cf49f|AuthStateErrorFilter.doFilterInternal]] — 호출 · L88

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.error.ApiErrorResponses#write m1b723e7027f0ade4|ApiErrorResponses.write]] — 호출 · L30

### 소스 코드

`src/main/java/com/dodamdodam/backend/auth/session/AuthStateErrorHandler.java:29`

```java
public void unavailable(HttpServletResponse response) throws IOException {
    ApiErrorResponses.write(jsonMapper, response, 503,
            "AUTH_STATE_UNAVAILABLE", "인증 상태를 확인할 수 없습니다. 잠시 후 다시 시도해 주세요.");
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## 생성자 m8796e45b357cb747

아래 원본 코드에서 구현을 확인한다.

```java
public AuthStateErrorHandler(tools.jackson.databind.json.JsonMapper jsonMapper)
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 외부 라이브러리 호출

- `java.lang.Object#<init>()`

### 소스 코드

`src/main/java/com/dodamdodam/backend/auth/session/AuthStateErrorHandler.java:17`

```java
public AuthStateErrorHandler(JsonMapper jsonMapper) {
    this.jsonMapper = jsonMapper;
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]
