---
tags: [backend, code-map, auth]
aliases: ["NewChallenge"]
---
# NewChallenge

`com.dodamdodam.backend.auth.infrastructure.EmailVerificationRepository.NewChallenge`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|시작]] → [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요]] → 이 클래스 · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/90-Reference|전체 참조 자료]]

## 클래스 요약

서비스가 판정한 발급 값이다. 번호 원문 없이 기존 스키마의 목적별 결합 값만 받는다.

- 원본: `src/main/java/com/dodamdodam/backend/auth/infrastructure/EmailVerificationRepository.java:127`
- 작성 메서드/생성자 0개 · 자동 생성 15개

## 메서드 목록

데이터·설정 타입이다. 아래 목록을 펼쳐 생성자와 접근자를 확인한다.

> [!example]- 생성자·자동 생성 멤버 15개
>
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationRepository.NewChallenge#codeHash mf24c18fb044290af|public byte[] codeHash() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationRepository.NewChallenge#eligible m63887491899caa79|public boolean eligible() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationRepository.NewChallenge#email m133eec4c2423ba35|public String email() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationRepository.NewChallenge#equals mc8d9e8905e65ce39|public final boolean equals(Object o) [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationRepository.NewChallenge#hashCode m630a7d71eb8c1903|public final int hashCode() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationRepository.NewChallenge#id m5a9d6c1170252280|public UUID id() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationRepository.NewChallenge#pinVersionSnapshot mecbbbc9252ef6bfe|public Long pinVersionSnapshot() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationRepository.NewChallenge#purpose m1ea1c554e5f2c6ea|public IssueEmailVerificationRequest.Purpose purpose() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationRepository.NewChallenge#resetAccountId m03d8680785b3ffe8|public UUID resetAccountId() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationRepository.NewChallenge#resetSessionVersion mec616719547a4627|public Long resetSessionVersion() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationRepository.NewChallenge#setupAccountId m4e9c4faacdb5993f|public UUID setupAccountId() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationRepository.NewChallenge#setupGeneration m82bdbdf8a59a4bb8|public Long setupGeneration() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationRepository.NewChallenge#setupSessionBinding m2c0efae75740ba3b|public UUID setupSessionBinding() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationRepository.NewChallenge#toString md1cdfd9b38f0b0ca|public final String toString() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationRepository.NewChallenge#생성자 m76eb4da549a3fc85|public NewChallenge(UUID id, String email, IssueEmailVerificationRequest.Purpose purpose, boolean eligible, byte[] codeHash, UUID setupAccountId, UUID setupSessionBinding, Long setupGeneration, Long pinVersionSnapshot, UUID resetAccountId, Long resetSessionVersion) [자동]]]

## codeHash mf24c18fb044290af

컴파일러가 생성한 멤버다.

```java
public byte[] codeHash()
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationRepository#replace m14c7e6b25ddbfbd4|EmailVerificationRepository.replace]] — 호출 · L59

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## eligible m63887491899caa79

컴파일러가 생성한 멤버다.

```java
public boolean eligible()
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.EmailVerificationIssuanceService#issue mcb0a83ce2cd55ce0|EmailVerificationIssuanceService.issue]] — 호출 · L73
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationRepository#replace m14c7e6b25ddbfbd4|EmailVerificationRepository.replace]] — 호출 · L58

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## email m133eec4c2423ba35

컴파일러가 생성한 멤버다.

```java
public java.lang.String email()
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.EmailVerificationIssuanceService#issue mcb0a83ce2cd55ce0|EmailVerificationIssuanceService.issue]] — 호출 · L73
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.EmailVerificationIssuanceService#verifyResponseAccess me8292ce668aadbdf|EmailVerificationIssuanceService.verifyResponseAccess]] — 호출 · L134
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationRepository#replace m14c7e6b25ddbfbd4|EmailVerificationRepository.replace]] — 호출 · L36,39,58

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## equals mc8d9e8905e65ce39

컴파일러가 생성한 멤버다.

```java
public final boolean equals(java.lang.Object o)
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## hashCode m630a7d71eb8c1903

컴파일러가 생성한 멤버다.

```java
public final int hashCode()
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## id m5a9d6c1170252280

컴파일러가 생성한 멤버다.

```java
public java.util.UUID id()
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.EmailVerificationIssuanceService#verifyResponseAccess me8292ce668aadbdf|EmailVerificationIssuanceService.verifyResponseAccess]] — 호출 · L128,135,139,140
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationRepository#replace m14c7e6b25ddbfbd4|EmailVerificationRepository.replace]] — 호출 · L58,61

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## pinVersionSnapshot mecbbbc9252ef6bfe

컴파일러가 생성한 멤버다.

```java
public java.lang.Long pinVersionSnapshot()
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.EmailVerificationIssuanceService#verifyResponseAccess me8292ce668aadbdf|EmailVerificationIssuanceService.verifyResponseAccess]] — 호출 · L133
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationRepository#replace m14c7e6b25ddbfbd4|EmailVerificationRepository.replace]] — 호출 · L60

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## purpose m1ea1c554e5f2c6ea

컴파일러가 생성한 멤버다.

```java
public com.dodamdodam.backend.auth.dto.IssueEmailVerificationRequest.Purpose purpose()
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.EmailVerificationIssuanceService#verifyResponseAccess me8292ce668aadbdf|EmailVerificationIssuanceService.verifyResponseAccess]] — 호출 · L132,136
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationRepository#replace m14c7e6b25ddbfbd4|EmailVerificationRepository.replace]] — 호출 · L31,35,36,39,58

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## resetAccountId m03d8680785b3ffe8

컴파일러가 생성한 멤버다.

```java
public java.util.UUID resetAccountId()
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationRepository#replace m14c7e6b25ddbfbd4|EmailVerificationRepository.replace]] — 호출 · L60

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## resetSessionVersion mec616719547a4627

컴파일러가 생성한 멤버다.

```java
public java.lang.Long resetSessionVersion()
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationRepository#replace m14c7e6b25ddbfbd4|EmailVerificationRepository.replace]] — 호출 · L60

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## setupAccountId m4e9c4faacdb5993f

컴파일러가 생성한 멤버다.

```java
public java.util.UUID setupAccountId()
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.EmailVerificationIssuanceService#verifyResponseAccess me8292ce668aadbdf|EmailVerificationIssuanceService.verifyResponseAccess]] — 호출 · L125,126
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationRepository#replace m14c7e6b25ddbfbd4|EmailVerificationRepository.replace]] — 호출 · L35,39,59

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## setupGeneration m82bdbdf8a59a4bb8

컴파일러가 생성한 멤버다.

```java
public java.lang.Long setupGeneration()
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.EmailVerificationIssuanceService#verifyResponseAccess me8292ce668aadbdf|EmailVerificationIssuanceService.verifyResponseAccess]] — 호출 · L134
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationRepository#replace m14c7e6b25ddbfbd4|EmailVerificationRepository.replace]] — 호출 · L60

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## setupSessionBinding m2c0efae75740ba3b

컴파일러가 생성한 멤버다.

```java
public java.util.UUID setupSessionBinding()
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.EmailVerificationIssuanceService#verifyResponseAccess me8292ce668aadbdf|EmailVerificationIssuanceService.verifyResponseAccess]] — 호출 · L126
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationRepository#replace m14c7e6b25ddbfbd4|EmailVerificationRepository.replace]] — 호출 · L35,39,59

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## toString md1cdfd9b38f0b0ca

컴파일러가 생성한 멤버다.

```java
public final java.lang.String toString()
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## 생성자 m76eb4da549a3fc85

컴파일러가 생성한 멤버다.

```java
public NewChallenge(java.util.UUID id, java.lang.String email, com.dodamdodam.backend.auth.dto.IssueEmailVerificationRequest.Purpose purpose, boolean eligible, byte[] codeHash, java.util.UUID setupAccountId, java.util.UUID setupSessionBinding, java.lang.Long setupGeneration, java.lang.Long pinVersionSnapshot, java.util.UUID resetAccountId, java.lang.Long resetSessionVersion)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.EmailVerificationIssuanceService#preparePin m330c48cfa5c13034|EmailVerificationIssuanceService.preparePin]] — 생성 · L112
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.EmailVerificationIssuanceService#preparePublic m8348ca544b936e88|EmailVerificationIssuanceService.preparePublic]] — 생성 · L86

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 외부 라이브러리 호출

- `java.lang.Record#<init>()`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]
