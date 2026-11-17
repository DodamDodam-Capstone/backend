---
tags: [backend, code-map, auth]
aliases: ["AuthRateLimitKey"]
---
# AuthRateLimitKey

`com.dodamdodam.backend.auth.domain.AuthRateLimitKey`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|시작]] → [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요]] → 이 클래스 · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/90-Reference|전체 참조 자료]]

## 클래스 요약

원문 식별자 대신 목적과 32바이트 HMAC으로 예약 대상을 구분한다.

- 원본: `src/main/java/com/dodamdodam/backend/auth/domain/AuthRateLimitKey.java:4`
- 작성 메서드/생성자 2개 · 자동 생성 4개

## 메서드 목록

| 메서드 | 하는 일 |
| --- | --- |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.domain.AuthRateLimitKey#keyHash m1c77dca4d2e2810f\|keyHash]] | 해시 원본을 노출하지 않아 같은 예약의 식별값을 유지한다. |

> [!example]- 생성자·자동 생성 멤버 5개
>
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.domain.AuthRateLimitKey#생성자 mf522ea90f29170e6|public AuthRateLimitKey(AuthRateLimitKey.Action action, byte[] keyHash)]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.domain.AuthRateLimitKey#action m3bb673cd9501adf1|public AuthRateLimitKey.Action action() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.domain.AuthRateLimitKey#equals m359b394541630866|public final boolean equals(Object o) [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.domain.AuthRateLimitKey#hashCode m5cb771b798b70b54|public final int hashCode() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.domain.AuthRateLimitKey#toString m6b99c9d97b6df0bd|public final String toString() [자동]]]

## keyHash m1c77dca4d2e2810f

해시 원본을 노출하지 않아 같은 예약의 식별값을 유지한다.

```java
public byte[] keyHash()
```

설정: `@java.lang.Override`

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.AuthRateLimitRepository#reserve m5db21fe66e21f71b|AuthRateLimitRepository.reserve]] — 호출 · L38,51
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.AuthRateLimitRepository#reserveAllowed m4983e5d4a84a95d9|AuthRateLimitRepository.reserveAllowed]] — 호출 · L78,84,88

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 외부 라이브러리 호출

- `Array#clone()`

### 소스 코드

`src/main/java/com/dodamdodam/backend/auth/domain/AuthRateLimitKey.java:15`

```java
@Override
public byte[] keyHash() {
    return keyHash.clone();
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## 생성자 mf522ea90f29170e6

DB에 전달할 해시를 검증·복사해 호출자의 배열 변경으로 범위가 바뀌지 않게 한다.

```java
public AuthRateLimitKey(com.dodamdodam.backend.auth.domain.AuthRateLimitKey.Action action, byte[] keyHash)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.AuthRateLimitKeyHasher#hash m884d2fbcdafa589b|AuthRateLimitKeyHasher.hash]] — 생성 · L73

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 외부 라이브러리 호출

- `Array#clone()`
- `java.lang.IllegalArgumentException#<init>(java.lang.String)`
- `java.lang.Record#<init>()`

### 소스 코드

`src/main/java/com/dodamdodam/backend/auth/domain/AuthRateLimitKey.java:7`

```java
public AuthRateLimitKey {
    if (action == null || keyHash == null || keyHash.length != 32) {
        throw new IllegalArgumentException("요청 제한 키가 올바르지 않습니다.");
    }
    keyHash = keyHash.clone();
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## action m3bb673cd9501adf1

컴파일러가 생성한 멤버다.

```java
public com.dodamdodam.backend.auth.domain.AuthRateLimitKey.Action action()
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.AuthRateLimitRepository#reserve m5db21fe66e21f71b|AuthRateLimitRepository.reserve]] — 호출 · L38,51,60
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.AuthRateLimitRepository#reserveAllowed m4983e5d4a84a95d9|AuthRateLimitRepository.reserveAllowed]] — 호출 · L78,80,84,88

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## equals m359b394541630866

컴파일러가 생성한 멤버다.

```java
public final boolean equals(java.lang.Object o)
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## hashCode m5cb771b798b70b54

컴파일러가 생성한 멤버다.

```java
public final int hashCode()
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## toString m6b99c9d97b6df0bd

컴파일러가 생성한 멤버다.

```java
public final java.lang.String toString()
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]
