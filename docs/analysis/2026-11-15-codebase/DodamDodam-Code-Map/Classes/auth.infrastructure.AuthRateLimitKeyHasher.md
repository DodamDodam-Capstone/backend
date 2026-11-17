---
tags: [backend, code-map, auth]
aliases: ["AuthRateLimitKeyHasher"]
---
# AuthRateLimitKeyHasher

`com.dodamdodam.backend.auth.infrastructure.AuthRateLimitKeyHasher`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|시작]] → [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요]] → 이 클래스 · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/90-Reference|전체 참조 자료]]

## 클래스 요약

상위 진입부가 확인한 실제 IP를 사용하며 전달 헤더를 직접 신뢰하지 않는다.

- 원본: `src/main/java/com/dodamdodam/backend/auth/infrastructure/AuthRateLimitKeyHasher.java:17`
- 작성 메서드/생성자 6개 · 자동 생성 0개

## 메서드 목록

| 메서드 | 하는 일 |
| --- | --- |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.AuthRateLimitKeyHasher#apiIp m4f29dcbbd14695b0\|apiIp]] | 상위 진입부가 확인한 실제 IP를 사용하며 전달 헤더를 직접 신뢰하지 않는다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.AuthRateLimitKeyHasher#emailIssueKeys mf2b48bc8bc79ecdf\|emailIssueKeys]] | 정규화·권한 검사한 범위로 누적 한도와 현재 문맥의 cooldown 키를 만든다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.AuthRateLimitKeyHasher#addressScope mb00b2a842218f55b\|addressScope]] | 같은 IP의 축약 표기가 달라도 주소 바이트가 같으면 같은 예산을 사용한다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.AuthRateLimitKeyHasher#hash m884d2fbcdafa589b\|hash]] | action과 각 UTF-8 필드의 길이를 포함해 경계가 모호하지 않은 전용 HMAC을 만든다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.AuthRateLimitKeyHasher#updateField md5945b6d8fdf841e\|updateField]] | 문자열 구분자 충돌 없이 필드 길이와 원문 바이트를 순서대로 계산에만 사용한다. |

> [!example]- 생성자·자동 생성 멤버 1개
>
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.AuthRateLimitKeyHasher#생성자 m709dfe6acfd13ccf|public AuthRateLimitKeyHasher(byte[] key)]]

## apiIp m4f29dcbbd14695b0

상위 진입부가 확인한 실제 IP를 사용하며 전달 헤더를 직접 신뢰하지 않는다.

```java
public com.dodamdodam.backend.auth.domain.AuthRateLimitKey apiIp(java.net.InetAddress clientAddress)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.web.GlobalApiRateLimitFilter#doFilterInternal me8918f88c454eb77|GlobalApiRateLimitFilter.doFilterInternal]] — 호출 · L42

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.AuthRateLimitKeyHasher#addressScope mb00b2a842218f55b|AuthRateLimitKeyHasher.addressScope]] — 호출 · L32
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.AuthRateLimitKeyHasher#hash m884d2fbcdafa589b|AuthRateLimitKeyHasher.hash]] — 호출 · L32

### 소스 코드

`src/main/java/com/dodamdodam/backend/auth/infrastructure/AuthRateLimitKeyHasher.java:31`

```java
public AuthRateLimitKey apiIp(InetAddress clientAddress) {
    return hash(Action.API_IP, addressScope(clientAddress));
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## emailIssueKeys mf2b48bc8bc79ecdf

정규화·권한 검사한 범위로 누적 한도와 현재 문맥의 cooldown 키를 만든다.

```java
public java.util.List<com.dodamdodam.backend.auth.domain.AuthRateLimitKey> emailIssueKeys(java.lang.String normalizedEmail, com.dodamdodam.backend.auth.dto.IssueEmailVerificationRequest.Purpose purpose, java.util.UUID accountId, java.util.UUID securityContextId, java.net.InetAddress clientAddress)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.EmailVerificationIssuanceService#issue mcb0a83ce2cd55ce0|EmailVerificationIssuanceService.issue]] — 호출 · L58

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.AuthRateLimitKeyHasher#addressScope mb00b2a842218f55b|AuthRateLimitKeyHasher.addressScope]] — 호출 · L52
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.AuthRateLimitKeyHasher#hash m884d2fbcdafa589b|AuthRateLimitKeyHasher.hash]] — 호출 · L47,50,51,52
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthStateUnavailableException#생성자 m03e9c81ea8cf5421|AuthStateUnavailableException.생성자]] — 생성 · L39,44

### 외부 라이브러리 호출

- `java.lang.Enum#name()`
- `java.lang.String#isBlank()`
- `java.util.List#of(E,E,E)`
- `java.util.UUID#toString()`

### 소스 코드

`src/main/java/com/dodamdodam/backend/auth/infrastructure/AuthRateLimitKeyHasher.java:36`

```java
public List<AuthRateLimitKey> emailIssueKeys(String normalizedEmail, Purpose purpose, UUID accountId,
                                           UUID securityContextId, InetAddress clientAddress) {
    if (purpose == null) {
        throw new AuthStateUnavailableException();
    }
    boolean pinPurpose = purpose == Purpose.PIN_SETUP || purpose == Purpose.PIN_RESET;
    if (pinPurpose && (accountId == null || securityContextId == null)
            || !pinPurpose && (normalizedEmail == null || normalizedEmail.isBlank())) {
        throw new AuthStateUnavailableException();
    }
    String subject = pinPurpose ? accountId.toString() : normalizedEmail;
    var subjectKey = hash(Action.EMAIL_SEND_SUBJECT, purpose.name(), subject);
    // 누적 한도에는 세션을 넣지 않아 새 세션으로 예산을 초기화하지 못하게 한다.
    var resendKey = pinPurpose
            ? hash(Action.EMAIL_RESEND, purpose.name(), subject, securityContextId.toString())
            : hash(Action.EMAIL_RESEND, purpose.name(), subject);
    return List.of(subjectKey, hash(Action.EMAIL_SEND_IP, addressScope(clientAddress)), resendKey);
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## addressScope mb00b2a842218f55b

같은 IP의 축약 표기가 달라도 주소 바이트가 같으면 같은 예산을 사용한다.

```java
private java.lang.String addressScope(java.net.InetAddress address)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.AuthRateLimitKeyHasher#apiIp m4f29dcbbd14695b0|AuthRateLimitKeyHasher.apiIp]] — 호출 · L32
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.AuthRateLimitKeyHasher#emailIssueKeys mf2b48bc8bc79ecdf|AuthRateLimitKeyHasher.emailIssueKeys]] — 호출 · L52

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthStateUnavailableException#생성자 m03e9c81ea8cf5421|AuthStateUnavailableException.생성자]] — 생성 · L58

### 외부 라이브러리 호출

- `java.net.InetAddress#getAddress()`
- `java.util.HexFormat#formatHex(byte[])`
- `java.util.HexFormat#of()`

### 소스 코드

`src/main/java/com/dodamdodam/backend/auth/infrastructure/AuthRateLimitKeyHasher.java:56`

```java
private String addressScope(InetAddress address) {
    if (address == null) {
        throw new AuthStateUnavailableException();
    }
    return HexFormat.of().formatHex(address.getAddress());
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## hash m884d2fbcdafa589b

action과 각 UTF-8 필드의 길이를 포함해 경계가 모호하지 않은 전용 HMAC을 만든다.

```java
private com.dodamdodam.backend.auth.domain.AuthRateLimitKey hash(com.dodamdodam.backend.auth.domain.AuthRateLimitKey.Action action, java.lang.String[] scope)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.AuthRateLimitKeyHasher#apiIp m4f29dcbbd14695b0|AuthRateLimitKeyHasher.apiIp]] — 호출 · L32
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.AuthRateLimitKeyHasher#emailIssueKeys mf2b48bc8bc79ecdf|AuthRateLimitKeyHasher.emailIssueKeys]] — 호출 · L47,50,51,52

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.domain.AuthRateLimitKey#생성자 mf522ea90f29170e6|AuthRateLimitKey.생성자]] — 생성 · L73
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.AuthRateLimitKeyHasher#updateField md5945b6d8fdf841e|AuthRateLimitKeyHasher.updateField]] — 호출 · L69,71
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthStateUnavailableException#생성자 m2ecef5ced435fdbf|AuthStateUnavailableException.생성자]] — 생성 · L76

### 외부 라이브러리 호출

- `java.lang.Enum#name()`
- `java.lang.String#getBytes(java.nio.charset.Charset)`
- `javax.crypto.Mac#doFinal()`
- `javax.crypto.Mac#getInstance(java.lang.String)`
- `javax.crypto.Mac#init(java.security.Key)`
- `javax.crypto.Mac#update(byte[])`

### 소스 코드

`src/main/java/com/dodamdodam/backend/auth/infrastructure/AuthRateLimitKeyHasher.java:64`

```java
private AuthRateLimitKey hash(Action action, String... scope) {
    try {
        var mac = Mac.getInstance(ALGORITHM);
        mac.init(key);
        mac.update("auth-rate:v1\0".getBytes(StandardCharsets.UTF_8));
        updateField(mac, action.name());
        for (String component : scope) {
            updateField(mac, component);
        }
        return new AuthRateLimitKey(action, mac.doFinal());
    }
    catch (GeneralSecurityException failure) {
        throw new AuthStateUnavailableException(failure);
    }
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## updateField md5945b6d8fdf841e

문자열 구분자 충돌 없이 필드 길이와 원문 바이트를 순서대로 계산에만 사용한다.

```java
private void updateField(javax.crypto.Mac mac, java.lang.String value)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.AuthRateLimitKeyHasher#hash m884d2fbcdafa589b|AuthRateLimitKeyHasher.hash]] — 호출 · L69,71

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 외부 라이브러리 호출

- `java.lang.String#getBytes(java.nio.charset.Charset)`
- `java.nio.ByteBuffer#allocate(int)`
- `java.nio.ByteBuffer#array()`
- `java.nio.ByteBuffer#putInt(int)`
- `javax.crypto.Mac#update(byte[])`

### 소스 코드

`src/main/java/com/dodamdodam/backend/auth/infrastructure/AuthRateLimitKeyHasher.java:81`

```java
private void updateField(Mac mac, String value) {
    byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
    mac.update(ByteBuffer.allocate(Integer.BYTES).putInt(bytes.length).array());
    mac.update(bytes);
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## 생성자 m709dfe6acfd13ccf

번호 인증 키와 별도로 주입하는 요청 제한 전용 32바이트 키를 받는다.

```java
public AuthRateLimitKeyHasher(byte[] key)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationKeys#생성자 m6a60175707d219eb|EmailVerificationKeys.생성자]] — 생성 · L21

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthStateUnavailableException#생성자 m03e9c81ea8cf5421|AuthStateUnavailableException.생성자]] — 생성 · L25

### 외부 라이브러리 호출

- `java.lang.Object#<init>()`
- `javax.crypto.spec.SecretKeySpec#<init>(byte[],java.lang.String)`

### 소스 코드

`src/main/java/com/dodamdodam/backend/auth/infrastructure/AuthRateLimitKeyHasher.java:23`

```java
public AuthRateLimitKeyHasher(byte[] key) {
    if (key == null || key.length != 32) {
        throw new AuthStateUnavailableException();
    }
    this.key = new SecretKeySpec(key, ALGORITHM);
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]
