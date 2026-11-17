---
tags: [backend, code-map, auth]
aliases: ["Prepared"]
---
# Prepared

`com.dodamdodam.backend.auth.application.EmailVerificationIssuanceService.Prepared`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|시작]] → [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요]] → 이 클래스 · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/90-Reference|전체 참조 자료]]

## 클래스 요약

내부 적격 값은 HTTP 응답에 포함하지 않는다.

- 원본: `src/main/java/com/dodamdodam/backend/auth/application/EmailVerificationIssuanceService.java:150`
- 작성 메서드/생성자 0개 · 자동 생성 6개

## 메서드 목록

데이터·설정 타입이다. 아래 목록을 펼쳐 생성자와 접근자를 확인한다.

> [!example]- 생성자·자동 생성 멤버 6개
>
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.EmailVerificationIssuanceService.Prepared#data mc6ce4a7b0c494314|public EmailVerificationRepository.NewChallenge data() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.EmailVerificationIssuanceService.Prepared#equals me75a602656169939|public final boolean equals(Object o) [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.EmailVerificationIssuanceService.Prepared#hashCode m3696a4d02c5968d2|public final int hashCode() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.EmailVerificationIssuanceService.Prepared#issued mf12e02c7efda6d67|public EmailVerificationRepository.IssuedChallenge issued() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.EmailVerificationIssuanceService.Prepared#toString m61765c272b03a80a|public final String toString() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.EmailVerificationIssuanceService.Prepared#생성자 maa7e559b2c1c8917|private Prepared(EmailVerificationRepository.NewChallenge data, EmailVerificationRepository.IssuedChallenge issued) [자동]]]

## data mc6ce4a7b0c494314

컴파일러가 생성한 멤버다.

```java
public com.dodamdodam.backend.auth.infrastructure.EmailVerificationRepository.NewChallenge data()
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.EmailVerificationIssuanceService#issue mcb0a83ce2cd55ce0|EmailVerificationIssuanceService.issue]] — 호출 · L73
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.EmailVerificationIssuanceService#verifyResponseAccess me8292ce668aadbdf|EmailVerificationIssuanceService.verifyResponseAccess]] — 호출 · L124

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## equals me75a602656169939

컴파일러가 생성한 멤버다.

```java
public final boolean equals(java.lang.Object o)
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## hashCode m3696a4d02c5968d2

컴파일러가 생성한 멤버다.

```java
public final int hashCode()
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## issued mf12e02c7efda6d67

컴파일러가 생성한 멤버다.

```java
public com.dodamdodam.backend.auth.infrastructure.EmailVerificationRepository.IssuedChallenge issued()
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.EmailVerificationIssuanceService#issue mcb0a83ce2cd55ce0|EmailVerificationIssuanceService.issue]] — 호출 · L73,75,76

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## toString m61765c272b03a80a

컴파일러가 생성한 멤버다.

```java
public final java.lang.String toString()
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## 생성자 maa7e559b2c1c8917

컴파일러가 생성한 멤버다.

```java
private Prepared(com.dodamdodam.backend.auth.infrastructure.EmailVerificationRepository.NewChallenge data, com.dodamdodam.backend.auth.infrastructure.EmailVerificationRepository.IssuedChallenge issued)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.EmailVerificationIssuanceService#preparePin m330c48cfa5c13034|EmailVerificationIssuanceService.preparePin]] — 생성 · L115
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.EmailVerificationIssuanceService#preparePublic m8348ca544b936e88|EmailVerificationIssuanceService.preparePublic]] — 생성 · L88

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 외부 라이브러리 호출

- `java.lang.Record#<init>()`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]
