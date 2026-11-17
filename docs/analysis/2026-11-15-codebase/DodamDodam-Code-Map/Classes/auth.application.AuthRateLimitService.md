---
tags: [backend, code-map, auth]
aliases: ["AuthRateLimitService"]
---
# AuthRateLimitService

`com.dodamdodam.backend.auth.application.AuthRateLimitService`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|시작]] → [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요]] → 이 클래스 · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/90-Reference|전체 참조 자료]]

## 클래스 요약

독립 트랜잭션으로 요청 제한 예산을 예약한다.

- 원본: `src/main/java/com/dodamdodam/backend/auth/application/AuthRateLimitService.java:16`
- 작성 메서드/생성자 2개 · 자동 생성 0개

## 메서드 목록

| 메서드 | 하는 일 |
| --- | --- |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.AuthRateLimitService#reserve m63e1737a4e336ba7\|reserve]] | 업무 row lock 전에 호출하며 API_IP 단독 예약 뒤 같은 단계의 업무 키들을 함께 예약한다. |

> [!example]- 생성자·자동 생성 멤버 1개
>
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.AuthRateLimitService#생성자 m7774f7a3aaa00a1c|public AuthRateLimitService(AuthRateLimitRepository repository, PlatformTransactionManager transactionManager)]]

## reserve m63e1737a4e336ba7

업무 row lock 전에 호출하며 API_IP 단독 예약 뒤 같은 단계의 업무 키들을 함께 예약한다.

```java
public void reserve(java.util.List<com.dodamdodam.backend.auth.domain.AuthRateLimitKey> keys)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.EmailVerificationIssuanceService#issue mcb0a83ce2cd55ce0|EmailVerificationIssuanceService.issue]] — 호출 · L58
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.web.GlobalApiRateLimitFilter#doFilterInternal me8918f88c454eb77|GlobalApiRateLimitFilter.doFilterInternal]] — 호출 · L42

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.exception.AuthRateLimitException#생성자 m1856a1b1e165196d|AuthRateLimitException.생성자]] — 생성 · L43
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.AuthRateLimitRepository#reserve m5db21fe66e21f71b|AuthRateLimitRepository.reserve]] — 호출 · L34
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthStateUnavailableException#생성자 m03e9c81ea8cf5421|AuthStateUnavailableException.생성자]] — 생성 · L40
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthStateUnavailableException#생성자 m2ecef5ced435fdbf|AuthStateUnavailableException.생성자]] — 생성 · L37

### 외부 라이브러리 호출

- `org.springframework.transaction.support.TransactionTemplate#execute(org.springframework.transaction.support.TransactionCallback<T>)`

### 소스 코드

`src/main/java/com/dodamdodam/backend/auth/application/AuthRateLimitService.java:30`

```java
public void reserve(List<AuthRateLimitKey> keys) {
    Long retryAfter;
    try {
        // 메서드 본문뿐 아니라 Tx 시작·커밋 실패도 인증 상태 장애로 구분한다.
        retryAfter = reservation.execute(status -> repository.reserve(keys));
    }
    catch (DataAccessException | TransactionException | ConversionException failure) {
        throw new AuthStateUnavailableException(failure);
    }
    if (retryAfter == null) {
        throw new AuthStateUnavailableException();
    }
    if (retryAfter > 0) {
        throw new AuthRateLimitException(retryAfter);
    }
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## 생성자 m7774f7a3aaa00a1c

이후 업무 rollback과 독립적으로 예산을 커밋하는 짧은 예약 Tx를 구성한다.

```java
public AuthRateLimitService(com.dodamdodam.backend.auth.infrastructure.AuthRateLimitRepository repository, org.springframework.transaction.PlatformTransactionManager transactionManager)
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

`src/main/java/com/dodamdodam/backend/auth/application/AuthRateLimitService.java:23`

```java
public AuthRateLimitService(AuthRateLimitRepository repository, PlatformTransactionManager transactionManager) {
    this.repository = repository;
    this.reservation = new TransactionTemplate(transactionManager);
    this.reservation.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]
