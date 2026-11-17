---
tags: [backend, code-map, auth]
aliases: ["AuthStateUnavailableException"]
---
# AuthStateUnavailableException

`com.dodamdodam.backend.auth.session.AuthStateUnavailableException`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|시작]] → [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요]] → 이 클래스 · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/90-Reference|전체 참조 자료]]

## 클래스 요약

업무 실패의 상태 코드와 공개 오류 정보를 전달한다.

- 원본: `src/main/java/com/dodamdodam/backend/auth/session/AuthStateUnavailableException.java:3`
- 작성 메서드/생성자 2개 · 자동 생성 0개

## 메서드 목록

데이터·설정 타입이다. 아래 목록을 펼쳐 생성자와 접근자를 확인한다.

> [!example]- 생성자·자동 생성 멤버 2개
>
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthStateUnavailableException#생성자 m03e9c81ea8cf5421|public AuthStateUnavailableException()]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthStateUnavailableException#생성자 m2ecef5ced435fdbf|public AuthStateUnavailableException(Throwable cause)]]

## 생성자 m03e9c81ea8cf5421

로그인 저장 결과를 확인할 수 없는 상태를 인증 장애로 표시한다.

```java
public AuthStateUnavailableException()
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.AuthRateLimitService#reserve m63e1737a4e336ba7|AuthRateLimitService.reserve]] — 생성 · L40
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.EmailVerificationDeliveryService#deliver mce05e4112cdb91d4|EmailVerificationDeliveryService.deliver]] — 생성 · L32
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.EmailVerificationIssuanceService#issue mcb0a83ce2cd55ce0|EmailVerificationIssuanceService.issue]] — 생성 · L70
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.AuthRateLimitKeyHasher#생성자 m709dfe6acfd13ccf|AuthRateLimitKeyHasher.생성자]] — 생성 · L25
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.AuthRateLimitKeyHasher#addressScope mb00b2a842218f55b|AuthRateLimitKeyHasher.addressScope]] — 생성 · L58
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.AuthRateLimitKeyHasher#emailIssueKeys mf2b48bc8bc79ecdf|AuthRateLimitKeyHasher.emailIssueKeys]] — 생성 · L39,44
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.AuthRateLimitRepository#reserve m5db21fe66e21f71b|AuthRateLimitRepository.reserve]] — 생성 · L31,36
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationCodeHasher#생성자 m946367bd413db940|EmailVerificationCodeHasher.생성자]] — 생성 · L19
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationKeys#codeHasher mb9d6c6ffe4dfc2aa|EmailVerificationKeys.codeHasher]] — 생성 · L43
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationKeys#rateHasher ma21ea9ca87c2fdb6|EmailVerificationKeys.rateHasher]] — 생성 · L35
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.SessionLifecycleService#verifyLogin mf3c5d20aa396347e|SessionLifecycleService.verifyLogin]] — 생성 · L85,89
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.web.ClientAddressResolver#resolve m53c50e28b5cfa314|ClientAddressResolver.resolve]] — 생성 · L24

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthStateUnavailableException#생성자 m2ecef5ced435fdbf|AuthStateUnavailableException.생성자]] — 호출 · L7

### 소스 코드

`src/main/java/com/dodamdodam/backend/auth/session/AuthStateUnavailableException.java:6`

```java
public AuthStateUnavailableException() {
    this(null);
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## 생성자 m2ecef5ced435fdbf

원인은 보존하되 외부 응답에 사용할 메시지는 고정한다.

```java
public AuthStateUnavailableException(java.lang.Throwable cause)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.api.AuthLogoutController#logout m43cbd6436794ba0f|AuthLogoutController.logout]] — 생성 · L60
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.AuthRateLimitService#reserve m63e1737a4e336ba7|AuthRateLimitService.reserve]] — 생성 · L37
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.EmailVerificationDeliveryService#invalidate m2641bd7bf8eedcbb|EmailVerificationDeliveryService.invalidate]] — 생성 · L50
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.EmailVerificationIssuanceService#issue mcb0a83ce2cd55ce0|EmailVerificationIssuanceService.issue]] — 생성 · L67
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.EmailVerificationIssuanceService#verifyResponseAccess me8292ce668aadbdf|EmailVerificationIssuanceService.verifyResponseAccess]] — 생성 · L145
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.AuthRateLimitKeyHasher#hash m884d2fbcdafa589b|AuthRateLimitKeyHasher.hash]] — 생성 · L76
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationCodeHasher#hash mf612dc13977923a2|EmailVerificationCodeHasher.hash]] — 생성 · L37
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationRepository#invalidateUnconsumed md4f33dbac51270fb|EmailVerificationRepository.invalidateUnconsumed]] — 생성 · L80
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationRepository#replace m14c7e6b25ddbfbd4|EmailVerificationRepository.replace]] — 생성 · L64
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthStateUnavailableException#생성자 m03e9c81ea8cf5421|AuthStateUnavailableException.생성자]] — 호출 · L7
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthenticatedSessionService#requireAuthenticatedSession mad23075b25d06a28|AuthenticatedSessionService.requireAuthenticatedSession]] — 생성 · L45
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthenticatedSessionService#requireStoredSession mc7d6b5fbad4be98b|AuthenticatedSessionService.requireStoredSession]] — 생성 · L63
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.SessionCookieRenewalFilter#renew me6ea84e6a538169a|SessionCookieRenewalFilter.renew]] — 생성 · L70
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.SessionLifecycleService#beginLogin m86b3ae016950c512|SessionLifecycleService.beginLogin]] — 생성 · L61
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.SessionLifecycleService#failLogin m8a418625ae91ad24|SessionLifecycleService.failLogin]] — 생성 · L122
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.SessionLifecycleService#verifyLogin mf3c5d20aa396347e|SessionLifecycleService.verifyLogin]] — 생성 · L95
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.SessionSecurityRepository#requireCurrentContextForUpdate m950184451aa631db|SessionSecurityRepository.requireCurrentContextForUpdate]] — 생성 · L98

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 외부 라이브러리 호출

- `java.lang.RuntimeException#<init>(java.lang.String,java.lang.Throwable)`

### 소스 코드

`src/main/java/com/dodamdodam/backend/auth/session/AuthStateUnavailableException.java:11`

```java
public AuthStateUnavailableException(Throwable cause) {
    super("인증 상태를 확인할 수 없습니다. 잠시 후 다시 시도해 주세요.", cause);
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]
