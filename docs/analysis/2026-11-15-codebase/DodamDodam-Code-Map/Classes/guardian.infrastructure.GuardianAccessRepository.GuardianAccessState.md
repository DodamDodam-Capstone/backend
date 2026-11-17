---
tags: [backend, code-map, guardian]
aliases: ["GuardianAccessState"]
---
# GuardianAccessState

`com.dodamdodam.backend.guardian.infrastructure.GuardianAccessRepository.GuardianAccessState`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|시작]] → [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/guardian|기능 개요]] → 이 클래스 · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/90-Reference|전체 참조 자료]]

## 클래스 요약

값을 전달하는 데이터 타입이다.

- 원본: `src/main/java/com/dodamdodam/backend/guardian/infrastructure/GuardianAccessRepository.java:50`
- 작성 메서드/생성자 2개 · 자동 생성 12개

## 메서드 목록

| 메서드 | 하는 일 |
| --- | --- |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/guardian.infrastructure.GuardianAccessRepository.GuardianAccessState#isSessionCurrent m51366fdd7d44c0f0\|isSessionCurrent]] | DB의 세션 버전·폐기·만료 기준으로 문맥 유효성을 판정한다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/guardian.infrastructure.GuardianAccessRepository.GuardianAccessState#isGuardianCurrent m9b272566953e54cb\|isGuardianCurrent]] | 같은 문맥의 미만료 보호자 확인과 현재 PIN 버전 일치를 함께 요구한다. |

> [!example]- 생성자·자동 생성 멤버 12개
>
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/guardian.infrastructure.GuardianAccessRepository.GuardianAccessState#accountSessionVersion m7b139bd642255366|public long accountSessionVersion() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/guardian.infrastructure.GuardianAccessRepository.GuardianAccessState#checkedAt m31c5c9db13e82358|public OffsetDateTime checkedAt() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/guardian.infrastructure.GuardianAccessRepository.GuardianAccessState#contextSessionVersion m709858159b646843|public long contextSessionVersion() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/guardian.infrastructure.GuardianAccessRepository.GuardianAccessState#currentPinVersion mad9d4875edcf0f53|public Long currentPinVersion() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/guardian.infrastructure.GuardianAccessRepository.GuardianAccessState#equals m299e688b69e52e37|public final boolean equals(Object o) [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/guardian.infrastructure.GuardianAccessRepository.GuardianAccessState#expiresAt m0d1dfdfbf48c7157|public OffsetDateTime expiresAt() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/guardian.infrastructure.GuardianAccessRepository.GuardianAccessState#guardianPinVersion m56df23790d154cdb|public Long guardianPinVersion() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/guardian.infrastructure.GuardianAccessRepository.GuardianAccessState#guardianUntil m91cf4fd36a77eee4|public OffsetDateTime guardianUntil() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/guardian.infrastructure.GuardianAccessRepository.GuardianAccessState#hashCode m7feff43c89452c6b|public final int hashCode() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/guardian.infrastructure.GuardianAccessRepository.GuardianAccessState#revokedAt m081ab43ab4fb881a|public OffsetDateTime revokedAt() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/guardian.infrastructure.GuardianAccessRepository.GuardianAccessState#toString m1a9c4e19b90dc934|public final String toString() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/guardian.infrastructure.GuardianAccessRepository.GuardianAccessState#생성자 m565381b90003821f|public GuardianAccessState(long contextSessionVersion, long accountSessionVersion, OffsetDateTime revokedAt, OffsetDateTime expiresAt, OffsetDateTime guardianUntil, Long guardianPinVersion, Long currentPinVersion, OffsetDateTime checkedAt) [자동]]]

## isSessionCurrent m51366fdd7d44c0f0

DB의 세션 버전·폐기·만료 기준으로 문맥 유효성을 판정한다.

```java
public boolean isSessionCurrent()
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/guardian.application.GuardianAccessService#requireCurrentAccess mef6c2129fa7e5585|GuardianAccessService.requireCurrentAccess]] — 호출 · L47
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/guardian.infrastructure.GuardianAccessRepository.GuardianAccessState#isGuardianCurrent m9b272566953e54cb|GuardianAccessState.isGuardianCurrent]] — 호출 · L64

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 외부 라이브러리 호출

- `java.time.OffsetDateTime#isBefore(java.time.OffsetDateTime)`

### 소스 코드

`src/main/java/com/dodamdodam/backend/guardian/infrastructure/GuardianAccessRepository.java:56`

```java
public boolean isSessionCurrent() {
    // NULL 만료는 허용하지만 계정 버전과 폐기 여부 검사는 항상 유지한다.
    return contextSessionVersion == accountSessionVersion && revokedAt == null
            && (expiresAt == null || checkedAt.isBefore(expiresAt));
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/guardian|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## isGuardianCurrent m9b272566953e54cb

같은 문맥의 미만료 보호자 확인과 현재 PIN 버전 일치를 함께 요구한다.

```java
public boolean isGuardianCurrent()
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/guardian.application.GuardianAccessService#requireCurrentAccess mef6c2129fa7e5585|GuardianAccessService.requireCurrentAccess]] — 호출 · L50

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/guardian.infrastructure.GuardianAccessRepository.GuardianAccessState#isSessionCurrent m51366fdd7d44c0f0|GuardianAccessState.isSessionCurrent]] — 호출 · L64

### 외부 라이브러리 호출

- `java.lang.Long#equals(java.lang.Object)`
- `java.time.OffsetDateTime#isBefore(java.time.OffsetDateTime)`

### 소스 코드

`src/main/java/com/dodamdodam/backend/guardian/infrastructure/GuardianAccessRepository.java:63`

```java
public boolean isGuardianCurrent() {
    return isSessionCurrent()
            // 기한과 DB 시각이 같아지는 순간부터 보호자 확인을 거부한다.
            && guardianUntil != null && checkedAt.isBefore(guardianUntil)
            // 현재 PIN이 존재하고 확인 당시 버전과 같아야 한다.
            && currentPinVersion != null && guardianPinVersion != null
            && guardianPinVersion.equals(currentPinVersion);
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/guardian|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## accountSessionVersion m7b139bd642255366

컴파일러가 생성한 멤버다.

```java
public long accountSessionVersion()
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/guardian|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## checkedAt m31c5c9db13e82358

컴파일러가 생성한 멤버다.

```java
public java.time.OffsetDateTime checkedAt()
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/guardian|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## contextSessionVersion m709858159b646843

컴파일러가 생성한 멤버다.

```java
public long contextSessionVersion()
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/guardian|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## currentPinVersion mad9d4875edcf0f53

컴파일러가 생성한 멤버다.

```java
public java.lang.Long currentPinVersion()
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/guardian|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## equals m299e688b69e52e37

컴파일러가 생성한 멤버다.

```java
public final boolean equals(java.lang.Object o)
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/guardian|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## expiresAt m0d1dfdfbf48c7157

컴파일러가 생성한 멤버다.

```java
public java.time.OffsetDateTime expiresAt()
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/guardian|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## guardianPinVersion m56df23790d154cdb

컴파일러가 생성한 멤버다.

```java
public java.lang.Long guardianPinVersion()
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/guardian|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## guardianUntil m91cf4fd36a77eee4

컴파일러가 생성한 멤버다.

```java
public java.time.OffsetDateTime guardianUntil()
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/guardian.application.GuardianAccessService#requireCurrentAccess mef6c2129fa7e5585|GuardianAccessService.requireCurrentAccess]] — 호출 · L53

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/guardian|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## hashCode m7feff43c89452c6b

컴파일러가 생성한 멤버다.

```java
public final int hashCode()
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/guardian|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## revokedAt m081ab43ab4fb881a

컴파일러가 생성한 멤버다.

```java
public java.time.OffsetDateTime revokedAt()
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/guardian|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## toString m1a9c4e19b90dc934

컴파일러가 생성한 멤버다.

```java
public final java.lang.String toString()
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/guardian|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## 생성자 m565381b90003821f

컴파일러가 생성한 멤버다.

```java
public GuardianAccessState(long contextSessionVersion, long accountSessionVersion, java.time.OffsetDateTime revokedAt, java.time.OffsetDateTime expiresAt, java.time.OffsetDateTime guardianUntil, java.lang.Long guardianPinVersion, java.lang.Long currentPinVersion, java.time.OffsetDateTime checkedAt)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/guardian.infrastructure.GuardianAccessRepository#findState mb1e4bd2e6cd4fcaf|GuardianAccessRepository.findState]] — 생성 · L34

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 외부 라이브러리 호출

- `java.lang.Record#<init>()`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/guardian|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]
