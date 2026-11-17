---
tags: [backend, code-map, auth]
aliases: ["SessionValidity"]
---
# SessionValidity

`com.dodamdodam.backend.auth.session.SessionSecurityRepository.SessionValidity`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|시작]] → [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요]] → 이 클래스 · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/90-Reference|전체 참조 자료]]

## 클래스 요약

값을 전달하는 데이터 타입이다.

- 원본: `src/main/java/com/dodamdodam/backend/auth/session/SessionSecurityRepository.java:72`
- 작성 메서드/생성자 1개 · 자동 생성 9개

## 메서드 목록

| 메서드 | 하는 일 |
| --- | --- |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.SessionSecurityRepository.SessionValidity#isValid mc39155fcbb025abe\|isValid]] | 아래 구현과 호출 관계에서 확인 |

> [!example]- 생성자·자동 생성 멤버 9개
>
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.SessionSecurityRepository.SessionValidity#accountVersion m0520c0fc8b4971f6|public long accountVersion() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.SessionSecurityRepository.SessionValidity#checkedAt mb67571218475b712|public OffsetDateTime checkedAt() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.SessionSecurityRepository.SessionValidity#equals md7a7d7720c4aaa27|public final boolean equals(Object o) [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.SessionSecurityRepository.SessionValidity#expiresAt md30da9324a34b049|public OffsetDateTime expiresAt() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.SessionSecurityRepository.SessionValidity#hashCode mb39a230f3fe5a451|public final int hashCode() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.SessionSecurityRepository.SessionValidity#revokedAt m7d059955242e6698|public OffsetDateTime revokedAt() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.SessionSecurityRepository.SessionValidity#sessionVersion m6d0ab5556492fb23|public long sessionVersion() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.SessionSecurityRepository.SessionValidity#toString mbbc79bf8e83b8a4f|public final String toString() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.SessionSecurityRepository.SessionValidity#생성자 m630c889182c65473|SessionValidity(long sessionVersion, long accountVersion, OffsetDateTime revokedAt, OffsetDateTime expiresAt, OffsetDateTime checkedAt) [자동]]]

## isValid mc39155fcbb025abe

아래 원본 코드에서 구현을 확인한다.

```java
boolean isValid()
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.SessionSecurityRepository#isCurrentContext mda2203671fdb2db3|SessionSecurityRepository.isCurrentContext]] — 호출 · L69

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 외부 라이브러리 호출

- `java.time.OffsetDateTime#isBefore(java.time.OffsetDateTime)`

### 소스 코드

`src/main/java/com/dodamdodam/backend/auth/session/SessionSecurityRepository.java:74`

```java
boolean isValid() {
    return sessionVersion == accountVersion && revokedAt == null
            && (expiresAt == null || checkedAt.isBefore(expiresAt));
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## accountVersion m0520c0fc8b4971f6

컴파일러가 생성한 멤버다.

```java
public long accountVersion()
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## checkedAt mb67571218475b712

컴파일러가 생성한 멤버다.

```java
public java.time.OffsetDateTime checkedAt()
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## equals md7a7d7720c4aaa27

컴파일러가 생성한 멤버다.

```java
public final boolean equals(java.lang.Object o)
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## expiresAt md30da9324a34b049

컴파일러가 생성한 멤버다.

```java
public java.time.OffsetDateTime expiresAt()
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## hashCode mb39a230f3fe5a451

컴파일러가 생성한 멤버다.

```java
public final int hashCode()
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## revokedAt m7d059955242e6698

컴파일러가 생성한 멤버다.

```java
public java.time.OffsetDateTime revokedAt()
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## sessionVersion m6d0ab5556492fb23

컴파일러가 생성한 멤버다.

```java
public long sessionVersion()
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## toString mbbc79bf8e83b8a4f

컴파일러가 생성한 멤버다.

```java
public final java.lang.String toString()
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## 생성자 m630c889182c65473

컴파일러가 생성한 멤버다.

```java
SessionValidity(long sessionVersion, long accountVersion, java.time.OffsetDateTime revokedAt, java.time.OffsetDateTime expiresAt, java.time.OffsetDateTime checkedAt)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.SessionSecurityRepository#isCurrentContext mda2203671fdb2db3|SessionSecurityRepository.isCurrentContext]] — 생성 · L64

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 외부 라이브러리 호출

- `java.lang.Record#<init>()`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]
