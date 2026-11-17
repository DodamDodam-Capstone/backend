---
tags: [backend, code-map, auth]
aliases: ["EmailVerificationCodeGenerator"]
---
# EmailVerificationCodeGenerator

`com.dodamdodam.backend.auth.application.EmailVerificationCodeGenerator`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|시작]] → [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요]] → 이 클래스 · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/90-Reference|전체 참조 자료]]

## 클래스 요약

보안 난수로 정확히 6자리 인증번호를 생성한다.

- 원본: `src/main/java/com/dodamdodam/backend/auth/application/EmailVerificationCodeGenerator.java:6`
- 작성 메서드/생성자 3개 · 자동 생성 0개

## 메서드 목록

| 메서드 | 하는 일 |
| --- | --- |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.EmailVerificationCodeGenerator#generate mdd4cf146a550e06f\|generate]] | 선행 0과 ASCII 형식을 보존한 정확히 6자리 인증번호를 생성한다. |

> [!example]- 생성자·자동 생성 멤버 2개
>
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.EmailVerificationCodeGenerator#생성자 mabde9d6b9037bf43|public EmailVerificationCodeGenerator()]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.EmailVerificationCodeGenerator#생성자 md9fe3d457fa2d3b4|EmailVerificationCodeGenerator(SecureRandom random)]]

## generate mdd4cf146a550e06f

선행 0과 ASCII 형식을 보존한 정확히 6자리 인증번호를 생성한다.

```java
public java.lang.String generate()
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.EmailVerificationIssuanceService#issue mcb0a83ce2cd55ce0|EmailVerificationIssuanceService.issue]] — 호출 · L60

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 외부 라이브러리 호출

- `java.lang.String#format(java.util.Locale,java.lang.String,java.lang.Object[])`
- `java.util.Random#nextInt(int)`

### 소스 코드

`src/main/java/com/dodamdodam/backend/auth/application/EmailVerificationCodeGenerator.java:21`

```java
public String generate() {
    return String.format(Locale.ROOT, "%06d", random.nextInt(1_000_000));
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## 생성자 mabde9d6b9037bf43

운영 생성에는 암호학적으로 안전한 난수만 사용한다.

```java
public EmailVerificationCodeGenerator()
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.EmailVerificationIssuanceService|필드/정적 초기화]] — 생성 · L33

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.EmailVerificationCodeGenerator#생성자 md9fe3d457fa2d3b4|EmailVerificationCodeGenerator.생성자]] — 호출 · L12

### 외부 라이브러리 호출

- `java.security.SecureRandom#<init>()`

### 소스 코드

`src/main/java/com/dodamdodam/backend/auth/application/EmailVerificationCodeGenerator.java:11`

```java
public EmailVerificationCodeGenerator() {
    this(new SecureRandom());
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## 생성자 md9fe3d457fa2d3b4

같은 패키지 테스트에서 난수 경계를 재현한다.

```java
EmailVerificationCodeGenerator(java.security.SecureRandom random)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.EmailVerificationCodeGenerator#생성자 mabde9d6b9037bf43|EmailVerificationCodeGenerator.생성자]] — 호출 · L12

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 외부 라이브러리 호출

- `java.lang.Object#<init>()`

### 소스 코드

`src/main/java/com/dodamdodam/backend/auth/application/EmailVerificationCodeGenerator.java:16`

```java
EmailVerificationCodeGenerator(SecureRandom random) {
    this.random = random;
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]
