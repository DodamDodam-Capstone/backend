---
tags: [backend, code-map, auth]
aliases: ["EmailVerificationExceptionHandler"]
---
# EmailVerificationExceptionHandler

`com.dodamdodam.backend.auth.api.EmailVerificationExceptionHandler`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|시작]] → [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요]] → 이 클래스 · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/90-Reference|전체 참조 자료]]

## 클래스 요약

이메일 발급 업무 오류를 고정 코드·필드 목록으로만 반환한다.

- 원본: `src/main/java/com/dodamdodam/backend/auth/api/EmailVerificationExceptionHandler.java:11`
- 작성 메서드/생성자 2개 · 자동 생성 1개

## 메서드 목록

| 메서드 | 하는 일 |
| --- | --- |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.api.EmailVerificationExceptionHandler#handle m512792dc15d45e08\|handle]] | 이메일 발급 업무 오류를 고정 코드·필드 목록으로만 반환한다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.api.EmailVerificationExceptionHandler#limited m77259e0b6f3fe9c9\|limited]] | 적용 제한 중 가장 늦은 재시도까지 계산된 초를 헤더에도 표시한다. |

> [!example]- 생성자·자동 생성 멤버 1개
>
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.api.EmailVerificationExceptionHandler#생성자 m921ab8979a2d4b88|public EmailVerificationExceptionHandler() [자동]]]

## handle m512792dc15d45e08

이메일 발급 업무 오류를 고정 코드·필드 목록으로만 반환한다.

```java
public org.springframework.http.ResponseEntity<com.dodamdodam.backend.global.error.ApiErrorResponse> handle(com.dodamdodam.backend.auth.exception.EmailVerificationException failure, jakarta.servlet.http.HttpServletResponse response)
```

설정: `@org.springframework.web.bind.annotation.ExceptionHandler({com.dodamdodam.backend.auth.exception.EmailVerificationException.class})`

### 이 메서드를 호출하는 곳

- 프레임워크 진입: Spring MVC 예외 처리

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.exception.EmailVerificationException#code m8644eccb71cd5266|EmailVerificationException.code]] — 호출 · L18
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.exception.EmailVerificationException#fields mdca59bb72875c112|EmailVerificationException.fields]] — 호출 · L18
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.exception.EmailVerificationException#status m20af4176d3ed6553|EmailVerificationException.status]] — 호출 · L17
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.error.ApiErrorResponses#body me905a471c60d2a7e|ApiErrorResponses.body]] — 호출 · L17

### 외부 라이브러리 호출

- `java.lang.Throwable#getMessage()`
- `org.springframework.http.ResponseEntity#status(int)`
- `org.springframework.http.ResponseEntity.BodyBuilder#body(@org.jspecify.annotations.Nullable T)`

### 소스 코드

`src/main/java/com/dodamdodam/backend/auth/api/EmailVerificationExceptionHandler.java:15`

```java
@ExceptionHandler(EmailVerificationException.class)
public ResponseEntity<ApiErrorResponse> handle(EmailVerificationException failure, HttpServletResponse response) {
    return ResponseEntity.status(failure.status()).body(ApiErrorResponses.body(response,
            failure.code(), failure.getMessage(), failure.fields()));
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## limited m77259e0b6f3fe9c9

적용 제한 중 가장 늦은 재시도까지 계산된 초를 헤더에도 표시한다.

```java
public org.springframework.http.ResponseEntity<com.dodamdodam.backend.global.error.ApiErrorResponse> limited(com.dodamdodam.backend.auth.exception.AuthRateLimitException failure, jakarta.servlet.http.HttpServletResponse response)
```

설정: `@org.springframework.web.bind.annotation.ExceptionHandler({com.dodamdodam.backend.auth.exception.AuthRateLimitException.class})`

### 이 메서드를 호출하는 곳

- 프레임워크 진입: Spring MVC 예외 처리

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.exception.AuthRateLimitException#code mcaa0ce05ad464eab|AuthRateLimitException.code]] — 호출 · L25
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.exception.AuthRateLimitException#retryAfterSeconds m423c845792a2b7a3|AuthRateLimitException.retryAfterSeconds]] — 호출 · L24
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.error.ApiErrorResponses#body me905a471c60d2a7e|ApiErrorResponses.body]] — 호출 · L25

### 외부 라이브러리 호출

- `jakarta.servlet.http.HttpServletResponse#setHeader(java.lang.String,java.lang.String)`
- `java.lang.Long#toString(long)`
- `java.lang.Throwable#getMessage()`
- `java.util.List#of()`
- `org.springframework.http.ResponseEntity#status(int)`
- `org.springframework.http.ResponseEntity.BodyBuilder#body(@org.jspecify.annotations.Nullable T)`

### 소스 코드

`src/main/java/com/dodamdodam/backend/auth/api/EmailVerificationExceptionHandler.java:22`

```java
@ExceptionHandler(AuthRateLimitException.class)
public ResponseEntity<ApiErrorResponse> limited(AuthRateLimitException failure, HttpServletResponse response) {
    response.setHeader("Retry-After", Long.toString(failure.retryAfterSeconds()));
    return ResponseEntity.status(429).body(ApiErrorResponses.body(response, failure.code(), failure.getMessage(), java.util.List.of()));
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## 생성자 m921ab8979a2d4b88

컴파일러가 생성한 멤버다.

```java
public EmailVerificationExceptionHandler()
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 외부 라이브러리 호출

- `java.lang.Object#<init>()`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]
