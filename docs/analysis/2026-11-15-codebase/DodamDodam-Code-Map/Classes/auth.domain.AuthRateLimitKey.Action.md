---
tags: [backend, code-map, auth]
aliases: ["Action"]
---
# Action

`com.dodamdodam.backend.auth.domain.AuthRateLimitKey.Action`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|시작]] → [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요]] → 이 클래스 · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/90-Reference|전체 참조 자료]]

## 클래스 요약

기존 기술 기준의 고정창 요청량이며 PIN 실제 오답 차단과 구분한다.

- 원본: `src/main/java/com/dodamdodam/backend/auth/domain/AuthRateLimitKey.java:21`
- 작성 메서드/생성자 3개 · 자동 생성 2개

## 메서드 목록

| 메서드 | 하는 일 |
| --- | --- |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.domain.AuthRateLimitKey.Action#capacity meb574cc3f4b38c05\|capacity]] | 한도번째 요청까지 허용하는 예약 수를 반환한다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.domain.AuthRateLimitKey.Action#windowSeconds ma2abc09be0cef384\|windowSeconds]] | 최초 예약부터 고정하는 요청창 길이를 초로 반환한다. |

> [!example]- 생성자·자동 생성 멤버 3개
>
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.domain.AuthRateLimitKey.Action#생성자 m592a6c76399c5aac|private Action(long capacity, long windowSeconds)]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.domain.AuthRateLimitKey.Action#valueOf m2c9988ca12459b51|public static AuthRateLimitKey.Action valueOf(String name) [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.domain.AuthRateLimitKey.Action#values mb1b05169b4e17deb|public static AuthRateLimitKey.Action[] values() [자동]]]

## capacity meb574cc3f4b38c05

한도번째 요청까지 허용하는 예약 수를 반환한다.

```java
public long capacity()
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.AuthRateLimitRepository.RateLimitState#retryAfterSeconds ma0bf3b2a05b8fb05|RateLimitState.retryAfterSeconds]] — 호출 · L97

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 소스 코드

`src/main/java/com/dodamdodam/backend/auth/domain/AuthRateLimitKey.java:37`

```java
public long capacity() {
    return capacity;
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## windowSeconds ma2abc09be0cef384

최초 예약부터 고정하는 요청창 길이를 초로 반환한다.

```java
public long windowSeconds()
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.AuthRateLimitRepository#reserveAllowed m4983e5d4a84a95d9|AuthRateLimitRepository.reserveAllowed]] — 호출 · L78,84
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.AuthRateLimitRepository.RateLimitState#retryAfterSeconds ma0bf3b2a05b8fb05|RateLimitState.retryAfterSeconds]] — 호출 · L96
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.AuthRateLimitRepository.RateLimitState#windowFinished md9f40a5f486b9ab3|RateLimitState.windowFinished]] — 호출 · L111

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 소스 코드

`src/main/java/com/dodamdodam/backend/auth/domain/AuthRateLimitKey.java:42`

```java
public long windowSeconds() {
    return windowSeconds;
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## 생성자 m592a6c76399c5aac

창 안에서 허용하는 예약 수와 창 길이를 보관한다.

```java
private Action(long capacity, long windowSeconds)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.domain.AuthRateLimitKey.Action|필드/정적 초기화]] — 생성 · L22,23,24,25

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 외부 라이브러리 호출

- `java.lang.Enum#<init>(java.lang.String,int)`

### 소스 코드

`src/main/java/com/dodamdodam/backend/auth/domain/AuthRateLimitKey.java:31`

```java
Action(long capacity, long windowSeconds) {
    this.capacity = capacity;
    this.windowSeconds = windowSeconds;
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## valueOf m2c9988ca12459b51

컴파일러가 생성한 멤버다.

```java
public static com.dodamdodam.backend.auth.domain.AuthRateLimitKey.Action valueOf(java.lang.String name)
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## values mb1b05169b4e17deb

컴파일러가 생성한 멤버다.

```java
public static com.dodamdodam.backend.auth.domain.AuthRateLimitKey.Action[] values()
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]
