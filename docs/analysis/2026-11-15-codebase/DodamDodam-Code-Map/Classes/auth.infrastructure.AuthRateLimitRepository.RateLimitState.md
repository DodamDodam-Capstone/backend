---
tags: [backend, code-map, auth]
aliases: ["RateLimitState"]
---
# RateLimitState

`com.dodamdodam.backend.auth.infrastructure.AuthRateLimitRepository.RateLimitState`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|시작]] → [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요]] → 이 클래스 · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/90-Reference|전체 참조 자료]]

## 클래스 요약

값을 전달하는 데이터 타입이다.

- 원본: `src/main/java/com/dodamdodam/backend/auth/infrastructure/AuthRateLimitRepository.java:92`
- 작성 메서드/생성자 2개 · 자동 생성 7개

## 메서드 목록

| 메서드 | 하는 일 |
| --- | --- |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.AuthRateLimitRepository.RateLimitState#retryAfterSeconds ma0bf3b2a05b8fb05\|retryAfterSeconds]] | 차단과 예산이 소진된 창 중 최늦은 재시도 시각까지 올림 초를 계산한다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.AuthRateLimitRepository.RateLimitState#windowFinished md9f40a5f486b9ab3\|windowFinished]] | 창과 차단 모두 equality부터 끝나며 그 전에는 기존 count를 유지한다. |

> [!example]- 생성자·자동 생성 멤버 7개
>
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.AuthRateLimitRepository.RateLimitState#attemptCount m5db04ccdde2a5a5b|public long attemptCount() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.AuthRateLimitRepository.RateLimitState#blockedUntil m62c1bfaecc5932a9|public OffsetDateTime blockedUntil() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.AuthRateLimitRepository.RateLimitState#equals mbaf9b33737d941cd|public final boolean equals(Object o) [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.AuthRateLimitRepository.RateLimitState#hashCode m240a180bc5676b1e|public final int hashCode() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.AuthRateLimitRepository.RateLimitState#toString m7e3dcc2f55731fa7|public final String toString() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.AuthRateLimitRepository.RateLimitState#windowStartedAt m5a26f0789bf2b408|public OffsetDateTime windowStartedAt() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.AuthRateLimitRepository.RateLimitState#생성자 m0aed0339d96e0b77|RateLimitState(OffsetDateTime windowStartedAt, long attemptCount, OffsetDateTime blockedUntil) [자동]]]

## retryAfterSeconds ma0bf3b2a05b8fb05

차단과 예산이 소진된 창 중 최늦은 재시도 시각까지 올림 초를 계산한다.

```java
long retryAfterSeconds(com.dodamdodam.backend.auth.domain.AuthRateLimitKey.Action action, java.time.OffsetDateTime checkedAt)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.AuthRateLimitRepository#reserve m5db21fe66e21f71b|AuthRateLimitRepository.reserve]] — 호출 · L60

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.domain.AuthRateLimitKey.Action#capacity meb574cc3f4b38c05|Action.capacity]] — 호출 · L97
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.domain.AuthRateLimitKey.Action#windowSeconds ma2abc09be0cef384|Action.windowSeconds]] — 호출 · L96

### 외부 라이브러리 호출

- `java.lang.Math#max(long,long)`
- `java.time.Duration#between(java.time.temporal.Temporal,java.time.temporal.Temporal)`
- `java.time.Duration#getNano()`
- `java.time.Duration#getSeconds()`
- `java.time.OffsetDateTime#isAfter(java.time.OffsetDateTime)`
- `java.time.OffsetDateTime#isBefore(java.time.OffsetDateTime)`
- `java.time.OffsetDateTime#plusSeconds(long)`

### 소스 코드

`src/main/java/com/dodamdodam/backend/auth/infrastructure/AuthRateLimitRepository.java:95`

```java
long retryAfterSeconds(Action action, OffsetDateTime checkedAt) {
    var windowEnd = windowStartedAt.plusSeconds(action.windowSeconds());
    OffsetDateTime retryAt = attemptCount >= action.capacity() && checkedAt.isBefore(windowEnd) ? windowEnd : null;
    if (blockedUntil != null && checkedAt.isBefore(blockedUntil)
            && (retryAt == null || blockedUntil.isAfter(retryAt))) {
        retryAt = blockedUntil;
    }
    if (retryAt == null) {
        return 0;
    }
    var remaining = Duration.between(checkedAt, retryAt);
    return Math.max(1, remaining.getSeconds() + (remaining.getNano() == 0 ? 0 : 1));
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## windowFinished md9f40a5f486b9ab3

창과 차단 모두 equality부터 끝나며 그 전에는 기존 count를 유지한다.

```java
boolean windowFinished(com.dodamdodam.backend.auth.domain.AuthRateLimitKey.Action action, java.time.OffsetDateTime checkedAt)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.AuthRateLimitRepository#reserveAllowed m4983e5d4a84a95d9|AuthRateLimitRepository.reserveAllowed]] — 호출 · L80

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.domain.AuthRateLimitKey.Action#windowSeconds ma2abc09be0cef384|Action.windowSeconds]] — 호출 · L111

### 외부 라이브러리 호출

- `java.time.OffsetDateTime#isBefore(java.time.OffsetDateTime)`
- `java.time.OffsetDateTime#plusSeconds(long)`

### 소스 코드

`src/main/java/com/dodamdodam/backend/auth/infrastructure/AuthRateLimitRepository.java:110`

```java
boolean windowFinished(Action action, OffsetDateTime checkedAt) {
    return !checkedAt.isBefore(windowStartedAt.plusSeconds(action.windowSeconds()))
            && (blockedUntil == null || !checkedAt.isBefore(blockedUntil));
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## attemptCount m5db04ccdde2a5a5b

컴파일러가 생성한 멤버다.

```java
public long attemptCount()
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## blockedUntil m62c1bfaecc5932a9

컴파일러가 생성한 멤버다.

```java
public java.time.OffsetDateTime blockedUntil()
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## equals mbaf9b33737d941cd

컴파일러가 생성한 멤버다.

```java
public final boolean equals(java.lang.Object o)
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## hashCode m240a180bc5676b1e

컴파일러가 생성한 멤버다.

```java
public final int hashCode()
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## toString m7e3dcc2f55731fa7

컴파일러가 생성한 멤버다.

```java
public final java.lang.String toString()
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## windowStartedAt m5a26f0789bf2b408

컴파일러가 생성한 멤버다.

```java
public java.time.OffsetDateTime windowStartedAt()
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## 생성자 m0aed0339d96e0b77

컴파일러가 생성한 멤버다.

```java
RateLimitState(java.time.OffsetDateTime windowStartedAt, long attemptCount, java.time.OffsetDateTime blockedUntil)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.AuthRateLimitRepository#reserve m5db21fe66e21f71b|AuthRateLimitRepository.reserve]] — 생성 · L49

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 외부 라이브러리 호출

- `java.lang.Record#<init>()`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]
