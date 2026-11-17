---
tags: [backend, code-map, auth]
aliases: ["EmailVerificationDeliveryService"]
---
# EmailVerificationDeliveryService

`com.dodamdodam.backend.auth.application.EmailVerificationDeliveryService`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|시작]] → [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요]] → 이 클래스 · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/90-Reference|전체 참조 자료]]

## 클래스 요약

업무 트랜잭션 밖에서 메일을 보내고 전송 실패 시 해당 발급을 폐기한다.

- 원본: `src/main/java/com/dodamdodam/backend/auth/application/EmailVerificationDeliveryService.java:14`
- 작성 메서드/생성자 3개 · 자동 생성 0개

## 메서드 목록

| 메서드 | 하는 일 |
| --- | --- |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.EmailVerificationDeliveryService#deliver mce05e4112cdb91d4\|deliver]] | 업무 Tx 종료 뒤 발송하고 거부·timeout·결과 불명확은 해당 id만 보상 폐기한다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.EmailVerificationDeliveryService#invalidate m2641bd7bf8eedcbb\|invalidate]] | 늦은 발송 실패나 권한 변경에서도 새 발급·소비 완료 증명을 건드리지 않는다. |

> [!example]- 생성자·자동 생성 멤버 1개
>
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.EmailVerificationDeliveryService#생성자 mf963f26d2550352e|public EmailVerificationDeliveryService(SmtpEmailVerificationSender sender, EmailVerificationRepository repository, PlatformTransactionManager manager)]]

## deliver mce05e4112cdb91d4

업무 Tx 종료 뒤 발송하고 거부·timeout·결과 불명확은 해당 id만 보상 폐기한다.

```java
public void deliver(java.util.UUID challengeId, java.lang.String email, java.lang.String code, boolean eligible)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.EmailVerificationIssuanceService#issue mcb0a83ce2cd55ce0|EmailVerificationIssuanceService.issue]] — 호출 · L73

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.EmailVerificationDeliveryService#invalidate m2641bd7bf8eedcbb|EmailVerificationDeliveryService.invalidate]] — 호출 · L39
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.exception.EmailVerificationException#deliveryUnavailable m490d86171f7ec4b2|EmailVerificationException.deliveryUnavailable]] — 호출 · L40
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.SmtpEmailVerificationSender#send mbb7d859c7dfa3045|SmtpEmailVerificationSender.send]] — 호출 · L35
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthStateUnavailableException#생성자 m03e9c81ea8cf5421|AuthStateUnavailableException.생성자]] — 생성 · L32

### 외부 라이브러리 호출

- `org.springframework.transaction.support.TransactionSynchronizationManager#isActualTransactionActive()`

### 소스 코드

`src/main/java/com/dodamdodam/backend/auth/application/EmailVerificationDeliveryService.java:30`

```java
public void deliver(UUID challengeId, String email, String code, boolean eligible) {
    if (TransactionSynchronizationManager.isActualTransactionActive()) {
        throw new AuthStateUnavailableException();
    }
    try {
        sender.send(email, code, eligible);
    }
    catch (RuntimeException failure) {
        // SMTP 예외는 이메일·번호를 포함할 수 있어 원인으로도 보관하지 않는다.
        invalidate(challengeId);
        throw EmailVerificationException.deliveryUnavailable();
    }
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## invalidate m2641bd7bf8eedcbb

늦은 발송 실패나 권한 변경에서도 새 발급·소비 완료 증명을 건드리지 않는다.

```java
public void invalidate(java.util.UUID challengeId)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.EmailVerificationDeliveryService#deliver mce05e4112cdb91d4|EmailVerificationDeliveryService.deliver]] — 호출 · L39
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.EmailVerificationIssuanceService#verifyResponseAccess me8292ce668aadbdf|EmailVerificationIssuanceService.verifyResponseAccess]] — 호출 · L128,135,140

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationRepository#invalidateUnconsumed md4f33dbac51270fb|EmailVerificationRepository.invalidateUnconsumed]] — 호출 · L47
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthStateUnavailableException#생성자 m2ecef5ced435fdbf|AuthStateUnavailableException.생성자]] — 생성 · L50

### 외부 라이브러리 호출

- `org.springframework.transaction.support.TransactionOperations#executeWithoutResult(java.util.function.Consumer<org.springframework.transaction.TransactionStatus>)`

### 소스 코드

`src/main/java/com/dodamdodam/backend/auth/application/EmailVerificationDeliveryService.java:45`

```java
public void invalidate(UUID challengeId) {
    try {
        compensation.executeWithoutResult(status -> repository.invalidateUnconsumed(challengeId));
    }
    catch (DataAccessException | TransactionException | ConversionException failure) {
        throw new AuthStateUnavailableException(failure);
    }
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## 생성자 mf963f26d2550352e

발송 실패 시 이미 커밋한 그 발급만 폐기하는 별도 Tx를 구성한다.

```java
public EmailVerificationDeliveryService(com.dodamdodam.backend.auth.infrastructure.SmtpEmailVerificationSender sender, com.dodamdodam.backend.auth.infrastructure.EmailVerificationRepository repository, org.springframework.transaction.PlatformTransactionManager manager)
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 외부 라이브러리 호출

- `java.lang.Object#<init>()`
- `org.springframework.transaction.support.DefaultTransactionDefinition#setPropagationBehavior(int)`
- `org.springframework.transaction.support.TransactionTemplate#<init>(org.springframework.transaction.PlatformTransactionManager)`

### 소스 코드

`src/main/java/com/dodamdodam/backend/auth/application/EmailVerificationDeliveryService.java:21`

```java
public EmailVerificationDeliveryService(SmtpEmailVerificationSender sender, EmailVerificationRepository repository,
                                         PlatformTransactionManager manager) {
    this.sender = sender;
    this.repository = repository;
    this.compensation = new TransactionTemplate(manager);
    this.compensation.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]
