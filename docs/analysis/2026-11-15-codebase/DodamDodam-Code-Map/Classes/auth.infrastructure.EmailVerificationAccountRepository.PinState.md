---
tags: [backend, code-map, auth]
aliases: ["PinState"]
---
# PinState

`com.dodamdodam.backend.auth.infrastructure.EmailVerificationAccountRepository.PinState`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|시작]] → [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요]] → 이 클래스 · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/90-Reference|전체 참조 자료]]

## 클래스 요약

값을 전달하는 데이터 타입이다.

- 원본: `src/main/java/com/dodamdodam/backend/auth/infrastructure/EmailVerificationAccountRepository.java:50`
- 작성 메서드/생성자 1개 · 자동 생성 12개

## 메서드 목록

| 메서드 | 하는 일 |
| --- | --- |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationAccountRepository.PinState#isSessionCurrent m3254562c28eeb659\|isSessionCurrent]] | NULL expiry에서도 폐기·계정 버전 검사를 생략하지 않는다. |

> [!example]- 생성자·자동 생성 멤버 12개
>
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationAccountRepository.PinState#accountVersion m72ddb2b698c02da4|public long accountVersion() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationAccountRepository.PinState#checkedAt mc97b4d63b9fbfc64|public OffsetDateTime checkedAt() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationAccountRepository.PinState#contextVersion m8b279be4f006d54c|public long contextVersion() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationAccountRepository.PinState#email m9bb78edc558a4125|public String email() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationAccountRepository.PinState#equals m02d61d7156aa5da3|public final boolean equals(Object o) [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationAccountRepository.PinState#expiresAt m0ec5efd96bacd5c6|public OffsetDateTime expiresAt() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationAccountRepository.PinState#hashCode m95cf7bebd5014d52|public final int hashCode() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationAccountRepository.PinState#pinVersion maba4e7db672a21a2|public Long pinVersion() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationAccountRepository.PinState#revokedAt m26535580c5709f83|public OffsetDateTime revokedAt() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationAccountRepository.PinState#setupGeneration mf8e2f01fb560367e|public long setupGeneration() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationAccountRepository.PinState#toString m4eb1a337d68a5b9f|public final String toString() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationAccountRepository.PinState#생성자 m70affc768552d379|public PinState(String email, long accountVersion, long contextVersion, long setupGeneration, OffsetDateTime revokedAt, OffsetDateTime expiresAt, Long pinVersion, OffsetDateTime checkedAt) [자동]]]

## isSessionCurrent m3254562c28eeb659

NULL expiry에서도 폐기·계정 버전 검사를 생략하지 않는다.

```java
public boolean isSessionCurrent()
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.EmailVerificationIssuanceService#preparePin m330c48cfa5c13034|EmailVerificationIssuanceService.preparePin]] — 호출 · L97
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.EmailVerificationIssuanceService#verifyResponseAccess me8292ce668aadbdf|EmailVerificationIssuanceService.verifyResponseAccess]] — 호출 · L127

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 외부 라이브러리 호출

- `java.time.OffsetDateTime#isBefore(java.time.OffsetDateTime)`

### 소스 코드

`src/main/java/com/dodamdodam/backend/auth/infrastructure/EmailVerificationAccountRepository.java:53`

```java
public boolean isSessionCurrent() {
    return accountVersion == contextVersion && revokedAt == null && (expiresAt == null || checkedAt.isBefore(expiresAt));
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## accountVersion m72ddb2b698c02da4

컴파일러가 생성한 멤버다.

```java
public long accountVersion()
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## checkedAt mc97b4d63b9fbfc64

컴파일러가 생성한 멤버다.

```java
public java.time.OffsetDateTime checkedAt()
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## contextVersion m8b279be4f006d54c

컴파일러가 생성한 멤버다.

```java
public long contextVersion()
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## email m9bb78edc558a4125

컴파일러가 생성한 멤버다.

```java
public java.lang.String email()
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.EmailVerificationIssuanceService#preparePin m330c48cfa5c13034|EmailVerificationIssuanceService.preparePin]] — 호출 · L100
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.EmailVerificationIssuanceService#verifyResponseAccess me8292ce668aadbdf|EmailVerificationIssuanceService.verifyResponseAccess]] — 호출 · L134

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## equals m02d61d7156aa5da3

컴파일러가 생성한 멤버다.

```java
public final boolean equals(java.lang.Object o)
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## expiresAt m0ec5efd96bacd5c6

컴파일러가 생성한 멤버다.

```java
public java.time.OffsetDateTime expiresAt()
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## hashCode m95cf7bebd5014d52

컴파일러가 생성한 멤버다.

```java
public final int hashCode()
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## pinVersion maba4e7db672a21a2

컴파일러가 생성한 멤버다.

```java
public java.lang.Long pinVersion()
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.EmailVerificationIssuanceService#preparePin m330c48cfa5c13034|EmailVerificationIssuanceService.preparePin]] — 호출 · L104,107,113
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.EmailVerificationIssuanceService#verifyResponseAccess me8292ce668aadbdf|EmailVerificationIssuanceService.verifyResponseAccess]] — 호출 · L132,133

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## revokedAt m26535580c5709f83

컴파일러가 생성한 멤버다.

```java
public java.time.OffsetDateTime revokedAt()
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## setupGeneration mf8e2f01fb560367e

컴파일러가 생성한 멤버다.

```java
public long setupGeneration()
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.EmailVerificationIssuanceService#preparePin m330c48cfa5c13034|EmailVerificationIssuanceService.preparePin]] — 호출 · L113
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.EmailVerificationIssuanceService#verifyResponseAccess me8292ce668aadbdf|EmailVerificationIssuanceService.verifyResponseAccess]] — 호출 · L134

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## toString m4eb1a337d68a5b9f

컴파일러가 생성한 멤버다.

```java
public final java.lang.String toString()
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## 생성자 m70affc768552d379

컴파일러가 생성한 멤버다.

```java
public PinState(java.lang.String email, long accountVersion, long contextVersion, long setupGeneration, java.time.OffsetDateTime revokedAt, java.time.OffsetDateTime expiresAt, java.lang.Long pinVersion, java.time.OffsetDateTime checkedAt)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationAccountRepository#findPinState maa86816375106a0c|EmailVerificationAccountRepository.findPinState]] — 생성 · L43

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 외부 라이브러리 호출

- `java.lang.Record#<init>()`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]
