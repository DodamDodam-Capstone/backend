---
tags: [backend, code-map, auth]
aliases: ["AuthRateLimitException"]
---
# AuthRateLimitException

`com.dodamdodam.backend.auth.exception.AuthRateLimitException`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|시작]] → [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요]] → 이 클래스 · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/90-Reference|전체 참조 자료]]

## 클래스 요약

업무 실패의 상태 코드와 공개 오류 정보를 전달한다.

- 원본: `src/main/java/com/dodamdodam/backend/auth/exception/AuthRateLimitException.java:3`
- 작성 메서드/생성자 3개 · 자동 생성 0개

## 메서드 목록

| 메서드 | 하는 일 |
| --- | --- |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.exception.AuthRateLimitException#code mcaa0ce05ad464eab\|code]] | HTTP 연결부에서 사용할 고정 업무 오류 코드를 반환한다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.exception.AuthRateLimitException#retryAfterSeconds m423c845792a2b7a3\|retryAfterSeconds]] | 가장 늦은 제한이 끝나는 시각까지 올림한 대기 초를 반환한다. |

> [!example]- 생성자·자동 생성 멤버 1개
>
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.exception.AuthRateLimitException#생성자 m1856a1b1e165196d|public AuthRateLimitException(long retryAfterSeconds)]]

## code mcaa0ce05ad464eab

HTTP 연결부에서 사용할 고정 업무 오류 코드를 반환한다.

```java
public java.lang.String code()
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.api.EmailVerificationExceptionHandler#limited m77259e0b6f3fe9c9|EmailVerificationExceptionHandler.limited]] — 호출 · L25
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.web.GlobalApiRateLimitFilter#doFilterInternal me8918f88c454eb77|GlobalApiRateLimitFilter.doFilterInternal]] — 호출 · L46

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 소스 코드

`src/main/java/com/dodamdodam/backend/auth/exception/AuthRateLimitException.java:14`

```java
public String code() {
    return "RATE_LIMITED";
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## retryAfterSeconds m423c845792a2b7a3

가장 늦은 제한이 끝나는 시각까지 올림한 대기 초를 반환한다.

```java
public long retryAfterSeconds()
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.api.EmailVerificationExceptionHandler#limited m77259e0b6f3fe9c9|EmailVerificationExceptionHandler.limited]] — 호출 · L24
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.web.GlobalApiRateLimitFilter#doFilterInternal me8918f88c454eb77|GlobalApiRateLimitFilter.doFilterInternal]] — 호출 · L45

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 소스 코드

`src/main/java/com/dodamdodam/backend/auth/exception/AuthRateLimitException.java:19`

```java
public long retryAfterSeconds() {
    return retryAfterSeconds;
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## 생성자 m1856a1b1e165196d

식별자·DB 상태 없이 최소 재시도 대기시간만 전달한다.

```java
public AuthRateLimitException(long retryAfterSeconds)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.AuthRateLimitService#reserve m63e1737a4e336ba7|AuthRateLimitService.reserve]] — 생성 · L43
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationRepository#requireResendReady m1cc9b17629706b6b|EmailVerificationRepository.requireResendReady]] — 생성 · L107

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 외부 라이브러리 호출

- `java.lang.Math#max(long,long)`
- `java.lang.RuntimeException#<init>(java.lang.String)`

### 소스 코드

`src/main/java/com/dodamdodam/backend/auth/exception/AuthRateLimitException.java:8`

```java
public AuthRateLimitException(long retryAfterSeconds) {
    super("요청이 너무 많습니다. 잠시 후 다시 시도해 주세요.");
    this.retryAfterSeconds = Math.max(1, retryAfterSeconds);
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]
