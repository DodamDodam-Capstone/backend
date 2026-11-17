---
tags: [backend, code-map, auth]
aliases: ["EmailVerificationIssuanceService"]
---
# EmailVerificationIssuanceService

`com.dodamdodam.backend.auth.application.EmailVerificationIssuanceService`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|시작]] → [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요]] → 이 클래스 · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/90-Reference|전체 참조 자료]]

## 클래스 요약

요청 예산 → challenge 저장 → SMTP → 응답 권한 재검사 순서를 조정한다.

- 원본: `src/main/java/com/dodamdodam/backend/auth/application/EmailVerificationIssuanceService.java:22`
- 작성 메서드/생성자 5개 · 자동 생성 0개

## 메서드 목록

| 메서드 | 하는 일 |
| --- | --- |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.EmailVerificationIssuanceService#issue mcb0a83ce2cd55ce0\|issue]] | 구조 검증 뒤 예산을 먼저 커밋하고, challenge 커밋이 끝난 다음에만 메일을 발송한다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.EmailVerificationIssuanceService#preparePublic m8348ca544b936e88\|preparePublic]] | 같은 이메일·목적을 잠근 뒤 현재 계정으로 normal/decoy와 reset 버전을 판정한다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.EmailVerificationIssuanceService#preparePin m330c48cfa5c13034\|preparePin]] | 계정→문맥→PIN→challenge 순서로 잠그고 현재 이메일·generation·PIN 버전을 저장한다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.EmailVerificationIssuanceService#verifyResponseAccess me8292ce668aadbdf\|verifyResponseAccess]] | 전송 직전 현재 DB를 읽고 늦은 폐기·계정/문맥/PIN 변경이면 이 발급만 폐기한다. |

> [!example]- 생성자·자동 생성 멤버 1개
>
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.EmailVerificationIssuanceService#생성자 m08398be383a88081|public EmailVerificationIssuanceService(EmailVerificationRepository repository, EmailVerificationAccountRepository accounts, EmailVerificationKeys keys, AuthRateLimitService limits, EmailVerificationDeliveryService delivery, Validator validator, PlatformTransactionManager manager, SessionSecurityRepository sessions)]]

## issue mcb0a83ce2cd55ce0

구조 검증 뒤 예산을 먼저 커밋하고, challenge 커밋이 끝난 다음에만 메일을 발송한다.

```java
public com.dodamdodam.backend.auth.dto.VerificationIssueResponse issue(jakarta.servlet.http.HttpServletRequest request, com.dodamdodam.backend.auth.dto.IssueEmailVerificationRequest input, com.dodamdodam.backend.auth.session.AuthenticatedSession current, java.net.InetAddress address)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.api.EmailVerificationController#issue m90c67bb20e0ca690|EmailVerificationController.issue]] — 호출 · L50

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.AuthRateLimitService#reserve m63e1737a4e336ba7|AuthRateLimitService.reserve]] — 호출 · L58
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.EmailVerificationCodeGenerator#generate mdd4cf146a550e06f|EmailVerificationCodeGenerator.generate]] — 호출 · L60
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.EmailVerificationDeliveryService#deliver mce05e4112cdb91d4|EmailVerificationDeliveryService.deliver]] — 호출 · L73
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.EmailVerificationIssuanceService#preparePin m330c48cfa5c13034|EmailVerificationIssuanceService.preparePin]] — 호출 · L64
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.EmailVerificationIssuanceService#preparePublic m8348ca544b936e88|EmailVerificationIssuanceService.preparePublic]] — 호출 · L64
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.EmailVerificationIssuanceService#verifyResponseAccess me8292ce668aadbdf|EmailVerificationIssuanceService.verifyResponseAccess]] — 호출 · L74
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.EmailVerificationIssuanceService.Prepared#data mc6ce4a7b0c494314|Prepared.data]] — 호출 · L73
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.EmailVerificationIssuanceService.Prepared#issued mf12e02c7efda6d67|Prepared.issued]] — 호출 · L73,75,76
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.dto.IssueEmailVerificationRequest#email m16107b4d40454c06|IssueEmailVerificationRequest.email]] — 호출 · L58
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.dto.IssueEmailVerificationRequest#normalized m0ad571c98c7a6c5e|IssueEmailVerificationRequest.normalized]] — 호출 · L57
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.dto.IssueEmailVerificationRequest#purpose m709393858f28d55e|IssueEmailVerificationRequest.purpose]] — 호출 · L53,58
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.dto.VerificationIssueResponse#생성자 mb460fd74aa89e6a9|VerificationIssueResponse.생성자]] — 생성 · L75
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.AuthRateLimitKeyHasher#emailIssueKeys mf2b48bc8bc79ecdf|AuthRateLimitKeyHasher.emailIssueKeys]] — 호출 · L58
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationCodeHasher#hash mf612dc13977923a2|EmailVerificationCodeHasher.hash]] — 호출 · L61
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationKeys#codeHasher mb9d6c6ffe4dfc2aa|EmailVerificationKeys.codeHasher]] — 호출 · L61
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationKeys#rateHasher ma21ea9ca87c2fdb6|EmailVerificationKeys.rateHasher]] — 호출 · L58
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationRepository.IssuedChallenge#challengeId md6f3b6df8f56816d|IssuedChallenge.challengeId]] — 호출 · L73,75
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationRepository.IssuedChallenge#expiresAt m77c941de489c65ee|IssuedChallenge.expiresAt]] — 호출 · L76
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationRepository.NewChallenge#eligible m63887491899caa79|NewChallenge.eligible]] — 호출 · L73
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationRepository.NewChallenge#email m133eec4c2423ba35|NewChallenge.email]] — 호출 · L73
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthStateUnavailableException#생성자 m03e9c81ea8cf5421|AuthStateUnavailableException.생성자]] — 생성 · L70
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthStateUnavailableException#생성자 m2ecef5ced435fdbf|AuthStateUnavailableException.생성자]] — 생성 · L67
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthenticatedSession#accountId m23b42fff4f806999|AuthenticatedSession.accountId]] — 호출 · L59
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthenticatedSession#securityContextId me6d8be795777e536|AuthenticatedSession.securityContextId]] — 호출 · L59

### 외부 라이브러리 호출

- `jakarta.servlet.ServletRequest#setAttribute(java.lang.String,java.lang.Object)`
- `java.time.OffsetDateTime#withOffsetSameInstant(java.time.ZoneOffset)`
- `java.time.ZoneOffset#ofHours(int)`
- `org.springframework.security.authentication.InsufficientAuthenticationException#<init>(java.lang.String)`
- `org.springframework.transaction.support.TransactionTemplate#execute(org.springframework.transaction.support.TransactionCallback<T>)`

### 소스 코드

`src/main/java/com/dodamdodam/backend/auth/application/EmailVerificationIssuanceService.java:51`

```java
public VerificationIssueResponse issue(HttpServletRequest request, IssueEmailVerificationRequest input,
                                      AuthenticatedSession current, InetAddress address) {
    boolean pin = input.purpose() == Purpose.PIN_SETUP || input.purpose() == Purpose.PIN_RESET;
    if (pin && current == null) {
        throw new InsufficientAuthenticationException("로그인이 필요합니다.");
    }
    var normalized = input.normalized(validator);
    limits.reserve(keys.rateHasher().emailIssueKeys(normalized.email(), normalized.purpose(),
            pin ? current.accountId() : null, pin ? current.securityContextId() : null, address));
    String code = codes.generate();
    byte[] hash = keys.codeHasher().hash(code);
    Prepared prepared;
    try {
        prepared = work.execute(status -> pin ? preparePin(normalized, current, hash) : preparePublic(normalized, hash));
    }
    catch (DataAccessException | TransactionException | ConversionException failure) {
        throw new AuthStateUnavailableException(failure);
    }
    if (prepared == null) {
        throw new AuthStateUnavailableException();
    }
    request.setAttribute(RESPONSE, prepared);
    delivery.deliver(prepared.issued().challengeId(), prepared.data().email(), code, prepared.data().eligible());
    verifyResponseAccess(request);
    return new VerificationIssueResponse(prepared.issued().challengeId(),
            prepared.issued().expiresAt().withOffsetSameInstant(ZoneOffset.ofHours(9)));
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## preparePublic m8348ca544b936e88

같은 이메일·목적을 잠근 뒤 현재 계정으로 normal/decoy와 reset 버전을 판정한다.

```java
private com.dodamdodam.backend.auth.application.EmailVerificationIssuanceService.Prepared preparePublic(com.dodamdodam.backend.auth.dto.IssueEmailVerificationRequest input, byte[] hash)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.EmailVerificationIssuanceService#issue mcb0a83ce2cd55ce0|EmailVerificationIssuanceService.issue]] — 호출 · L64

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.EmailVerificationIssuanceService.Prepared#생성자 maa7e559b2c1c8917|Prepared.생성자]] — 생성 · L88
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.dto.IssueEmailVerificationRequest#email m16107b4d40454c06|IssueEmailVerificationRequest.email]] — 호출 · L81,82,84,86
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.dto.IssueEmailVerificationRequest#purpose m709393858f28d55e|IssueEmailVerificationRequest.purpose]] — 호출 · L81,83,84,85,86
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationAccountRepository#findForUpdate m335c95532feb01f2|EmailVerificationAccountRepository.findForUpdate]] — 호출 · L82
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationAccountRepository.AccountState#id m3b0797f9ee3e692d|AccountState.id]] — 호출 · L87
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationAccountRepository.AccountState#sessionVersion mc6c1c367baa46c63|AccountState.sessionVersion]] — 호출 · L87
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationRepository#lockScope ma7c40b57ecd4ef2d|EmailVerificationRepository.lockScope]] — 호출 · L81
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationRepository#replace m14c7e6b25ddbfbd4|EmailVerificationRepository.replace]] — 호출 · L88
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationRepository#requireResendReady m1cc9b17629706b6b|EmailVerificationRepository.requireResendReady]] — 호출 · L84
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationRepository.NewChallenge#생성자 m76eb4da549a3fc85|NewChallenge.생성자]] — 생성 · L86

### 외부 라이브러리 호출

- `java.util.Optional#isEmpty()`
- `java.util.Optional#isPresent()`
- `java.util.Optional#orElseThrow()`
- `java.util.UUID#randomUUID()`

### 소스 코드

`src/main/java/com/dodamdodam/backend/auth/application/EmailVerificationIssuanceService.java:80`

```java
private Prepared preparePublic(IssueEmailVerificationRequest input, byte[] hash) {
    repository.lockScope(input.email(), input.purpose(), null, null);
    var account = accounts.findForUpdate(input.email());
    boolean eligible = input.purpose() == Purpose.SIGNUP ? account.isEmpty() : account.isPresent();
    repository.requireResendReady(input.email(), input.purpose(), null, null);
    var reset = input.purpose() == Purpose.RESET_PASSWORD && eligible ? account.orElseThrow() : null;
    var data = new NewChallenge(UUID.randomUUID(), input.email(), input.purpose(), eligible, hash,
            null, null, null, null, reset == null ? null : reset.id(), reset == null ? null : reset.sessionVersion());
    return new Prepared(data, repository.replace(data));
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## preparePin m330c48cfa5c13034

계정→문맥→PIN→challenge 순서로 잠그고 현재 이메일·generation·PIN 버전을 저장한다.

```java
private com.dodamdodam.backend.auth.application.EmailVerificationIssuanceService.Prepared preparePin(com.dodamdodam.backend.auth.dto.IssueEmailVerificationRequest input, com.dodamdodam.backend.auth.session.AuthenticatedSession current, byte[] hash)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.EmailVerificationIssuanceService#issue mcb0a83ce2cd55ce0|EmailVerificationIssuanceService.issue]] — 호출 · L64

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.EmailVerificationIssuanceService.Prepared#생성자 maa7e559b2c1c8917|Prepared.생성자]] — 생성 · L115
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.dto.IssueEmailVerificationRequest#생성자 mb489f1512fd4efcb|IssueEmailVerificationRequest.생성자]] — 생성 · L100
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.dto.IssueEmailVerificationRequest#email m16107b4d40454c06|IssueEmailVerificationRequest.email]] — 호출 · L100,101
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.dto.IssueEmailVerificationRequest#normalized m0ad571c98c7a6c5e|IssueEmailVerificationRequest.normalized]] — 호출 · L100
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.dto.IssueEmailVerificationRequest#purpose m709393858f28d55e|IssueEmailVerificationRequest.purpose]] — 호출 · L104,107,110,111,112,113
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.exception.EmailVerificationException#authenticationRequired m42d01e93bfad8fd4|EmailVerificationException.authenticationRequired]] — 호출/참조 · L96,98
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.exception.EmailVerificationException#pinAlreadySet m21633c5920f88f62|EmailVerificationException.pinAlreadySet]] — 호출 · L105
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.exception.EmailVerificationException#pinNotSet maf5c01b31a0693e5|EmailVerificationException.pinNotSet]] — 호출 · L108
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.exception.EmailVerificationException#validation m01790b591df96407|EmailVerificationException.validation]] — 호출 · L102
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationAccountRepository#findPinState maa86816375106a0c|EmailVerificationAccountRepository.findPinState]] — 호출 · L95
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationAccountRepository#lockPin mcfbdc90d46d30c44|EmailVerificationAccountRepository.lockPin]] — 호출 · L94
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationAccountRepository.PinState#email m9bb78edc558a4125|PinState.email]] — 호출 · L100
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationAccountRepository.PinState#isSessionCurrent m3254562c28eeb659|PinState.isSessionCurrent]] — 호출 · L97
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationAccountRepository.PinState#pinVersion maba4e7db672a21a2|PinState.pinVersion]] — 호출 · L104,107,113
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationAccountRepository.PinState#setupGeneration mf8e2f01fb560367e|PinState.setupGeneration]] — 호출 · L113
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationRepository#lockScope ma7c40b57ecd4ef2d|EmailVerificationRepository.lockScope]] — 호출 · L110
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationRepository#replace m14c7e6b25ddbfbd4|EmailVerificationRepository.replace]] — 호출 · L115
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationRepository#requireResendReady m1cc9b17629706b6b|EmailVerificationRepository.requireResendReady]] — 호출 · L111
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationRepository.NewChallenge#생성자 m76eb4da549a3fc85|NewChallenge.생성자]] — 생성 · L112
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthenticatedSession#accountId m23b42fff4f806999|AuthenticatedSession.accountId]] — 호출 · L93,94,95,110,111,112
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthenticatedSession#securityContextId me6d8be795777e536|AuthenticatedSession.securityContextId]] — 호출 · L93,95,110,111,113
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.SessionSecurityRepository#requireCurrentContextForUpdate m950184451aa631db|SessionSecurityRepository.requireCurrentContextForUpdate]] — 호출 · L93

### 외부 라이브러리 호출

- `java.lang.String#equals(java.lang.Object)`
- `java.util.Optional#orElseThrow(java.util.function.Supplier<? extends X>)`
- `java.util.UUID#randomUUID()`

### 소스 코드

`src/main/java/com/dodamdodam/backend/auth/application/EmailVerificationIssuanceService.java:92`

```java
private Prepared preparePin(IssueEmailVerificationRequest input, AuthenticatedSession current, byte[] hash) {
    sessions.requireCurrentContextForUpdate(current.accountId(), current.securityContextId());
    accounts.lockPin(current.accountId());
    var state = accounts.findPinState(current.accountId(), current.securityContextId()).orElseThrow(
            EmailVerificationException::authenticationRequired);
    if (!state.isSessionCurrent()) {
        throw EmailVerificationException.authenticationRequired();
    }
    String email = new IssueEmailVerificationRequest(Purpose.SIGNUP, state.email()).normalized(validator).email();
    if (input.email() != null && !input.email().equals(email)) {
        throw EmailVerificationException.validation("email");
    }
    if (input.purpose() == Purpose.PIN_SETUP && state.pinVersion() != null) {
        throw EmailVerificationException.pinAlreadySet();
    }
    if (input.purpose() == Purpose.PIN_RESET && state.pinVersion() == null) {
        throw EmailVerificationException.pinNotSet();
    }
    repository.lockScope(email, input.purpose(), current.accountId(), current.securityContextId());
    repository.requireResendReady(email, input.purpose(), current.accountId(), current.securityContextId());
    var data = new NewChallenge(UUID.randomUUID(), email, input.purpose(), true, hash, current.accountId(),
            current.securityContextId(), state.setupGeneration(), input.purpose() == Purpose.PIN_RESET ? state.pinVersion() : null,
            null, null);
    return new Prepared(data, repository.replace(data));
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## verifyResponseAccess me8292ce668aadbdf

전송 직전 현재 DB를 읽고 늦은 폐기·계정/문맥/PIN 변경이면 이 발급만 폐기한다.

```java
public void verifyResponseAccess(jakarta.servlet.http.HttpServletRequest request)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.EmailVerificationIssuanceService#issue mcb0a83ce2cd55ce0|EmailVerificationIssuanceService.issue]] — 호출 · L74
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthStateErrorFilter#verifyResponseAccess m1098528f38d3ff11|AuthStateErrorFilter.verifyResponseAccess]] — 호출 · L99

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.EmailVerificationDeliveryService#invalidate m2641bd7bf8eedcbb|EmailVerificationDeliveryService.invalidate]] — 호출 · L128,135,140
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.EmailVerificationIssuanceService.Prepared#data mc6ce4a7b0c494314|Prepared.data]] — 호출 · L124
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.exception.EmailVerificationException#authenticationRequired m42d01e93bfad8fd4|EmailVerificationException.authenticationRequired]] — 호출 · L129
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.exception.EmailVerificationException#authorizationRequired maac9c6910411385d|EmailVerificationException.authorizationRequired]] — 호출 · L136
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.exception.EmailVerificationException#deliveryUnavailable m490d86171f7ec4b2|EmailVerificationException.deliveryUnavailable]] — 호출 · L141
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationAccountRepository#findPinState maa86816375106a0c|EmailVerificationAccountRepository.findPinState]] — 호출 · L126
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationAccountRepository.PinState#email m9bb78edc558a4125|PinState.email]] — 호출 · L134
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationAccountRepository.PinState#isSessionCurrent m3254562c28eeb659|PinState.isSessionCurrent]] — 호출 · L127
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationAccountRepository.PinState#pinVersion maba4e7db672a21a2|PinState.pinVersion]] — 호출 · L132,133
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationAccountRepository.PinState#setupGeneration mf8e2f01fb560367e|PinState.setupGeneration]] — 호출 · L134
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationRepository#isIssueResponseCurrent m1f11b5692acde05b|EmailVerificationRepository.isIssueResponseCurrent]] — 호출 · L139
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationRepository.NewChallenge#email m133eec4c2423ba35|NewChallenge.email]] — 호출 · L134
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationRepository.NewChallenge#id m5a9d6c1170252280|NewChallenge.id]] — 호출 · L128,135,139,140
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationRepository.NewChallenge#pinVersionSnapshot mecbbbc9252ef6bfe|NewChallenge.pinVersionSnapshot]] — 호출 · L133
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationRepository.NewChallenge#purpose m1ea1c554e5f2c6ea|NewChallenge.purpose]] — 호출 · L132,136
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationRepository.NewChallenge#setupAccountId m4e9c4faacdb5993f|NewChallenge.setupAccountId]] — 호출 · L125,126
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationRepository.NewChallenge#setupGeneration m82bdbdf8a59a4bb8|NewChallenge.setupGeneration]] — 호출 · L134
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationRepository.NewChallenge#setupSessionBinding m2c0efae75740ba3b|NewChallenge.setupSessionBinding]] — 호출 · L126
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthStateUnavailableException#생성자 m2ecef5ced435fdbf|AuthStateUnavailableException.생성자]] — 생성 · L145

### 외부 라이브러리 호출

- `jakarta.servlet.ServletRequest#getAttribute(java.lang.String)`
- `java.lang.String#equals(java.lang.Object)`
- `java.util.Objects#equals(java.lang.Object,java.lang.Object)`
- `java.util.Optional#get()`
- `java.util.Optional#isEmpty()`

### 소스 코드

`src/main/java/com/dodamdodam/backend/auth/application/EmailVerificationIssuanceService.java:119`

```java
public void verifyResponseAccess(HttpServletRequest request) {
    if (!(request.getAttribute(RESPONSE) instanceof Prepared prepared)) {
        return;
    }
    try {
        var data = prepared.data();
        if (data.setupAccountId() != null) {
            var state = accounts.findPinState(data.setupAccountId(), data.setupSessionBinding());
            if (state.isEmpty() || !state.get().isSessionCurrent()) {
                delivery.invalidate(data.id());
                throw EmailVerificationException.authenticationRequired();
            }
            var current = state.get();
            boolean pinMatches = data.purpose() == Purpose.PIN_SETUP ? current.pinVersion() == null
                    : Objects.equals(current.pinVersion(), data.pinVersionSnapshot());
            if (!data.email().equals(current.email()) || data.setupGeneration() != current.setupGeneration() || !pinMatches) {
                delivery.invalidate(data.id());
                throw EmailVerificationException.authorizationRequired(data.purpose());
            }
        }
        if (!repository.isIssueResponseCurrent(data.id())) {
            delivery.invalidate(data.id());
            throw EmailVerificationException.deliveryUnavailable();
        }
    }
    catch (DataAccessException | TransactionException | ConversionException failure) {
        throw new AuthStateUnavailableException(failure);
    }
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## 생성자 m08398be383a88081

예산 예약·업무 저장·외부 발송을 서로 다른 경계로 연결한다.

```java
public EmailVerificationIssuanceService(com.dodamdodam.backend.auth.infrastructure.EmailVerificationRepository repository, com.dodamdodam.backend.auth.infrastructure.EmailVerificationAccountRepository accounts, com.dodamdodam.backend.auth.infrastructure.EmailVerificationKeys keys, com.dodamdodam.backend.auth.application.AuthRateLimitService limits, com.dodamdodam.backend.auth.application.EmailVerificationDeliveryService delivery, jakarta.validation.Validator validator, org.springframework.transaction.PlatformTransactionManager manager, com.dodamdodam.backend.auth.session.SessionSecurityRepository sessions)
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

`src/main/java/com/dodamdodam/backend/auth/application/EmailVerificationIssuanceService.java:36`

```java
public EmailVerificationIssuanceService(EmailVerificationRepository repository, EmailVerificationAccountRepository accounts,
        EmailVerificationKeys keys, AuthRateLimitService limits, EmailVerificationDeliveryService delivery,
        Validator validator, PlatformTransactionManager manager, SessionSecurityRepository sessions) {
    this.repository = repository;
    this.accounts = accounts;
    this.keys = keys;
    this.limits = limits;
    this.delivery = delivery;
    this.validator = validator;
    this.sessions = sessions;
    this.work = new TransactionTemplate(manager);
    this.work.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]
