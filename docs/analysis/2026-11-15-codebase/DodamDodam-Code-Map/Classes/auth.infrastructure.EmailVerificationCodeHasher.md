---
tags: [backend, code-map, auth]
aliases: ["EmailVerificationCodeHasher"]
---
# EmailVerificationCodeHasher

`com.dodamdodam.backend.auth.infrastructure.EmailVerificationCodeHasher`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|시작]] → [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요]] → 이 클래스 · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/90-Reference|전체 참조 자료]]

## 클래스 요약

코드 원문을 보정·보관하지 않고 DB code_hash용 32바이트 HMAC을 반환한다.

- 원본: `src/main/java/com/dodamdodam/backend/auth/infrastructure/EmailVerificationCodeHasher.java:10`
- 작성 메서드/생성자 2개 · 자동 생성 0개

## 메서드 목록

| 메서드 | 하는 일 |
| --- | --- |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationCodeHasher#hash mf612dc13977923a2\|hash]] | 코드 원문을 보정·보관하지 않고 DB code_hash용 32바이트 HMAC을 반환한다. |

> [!example]- 생성자·자동 생성 멤버 1개
>
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationCodeHasher#생성자 m946367bd413db940|public EmailVerificationCodeHasher(byte[] key)]]

## hash mf612dc13977923a2

코드 원문을 보정·보관하지 않고 DB code_hash용 32바이트 HMAC을 반환한다.

```java
public byte[] hash(java.lang.String code)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.EmailVerificationIssuanceService#issue mcb0a83ce2cd55ce0|EmailVerificationIssuanceService.issue]] — 호출 · L61

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthStateUnavailableException#생성자 m2ecef5ced435fdbf|AuthStateUnavailableException.생성자]] — 생성 · L37

### 외부 라이브러리 호출

- `java.lang.IllegalArgumentException#<init>(java.lang.String)`
- `java.lang.String#getBytes(java.nio.charset.Charset)`
- `java.util.regex.Matcher#matches()`
- `java.util.regex.Pattern#matcher(java.lang.CharSequence)`
- `javax.crypto.Mac#doFinal(byte[])`
- `javax.crypto.Mac#getInstance(java.lang.String)`
- `javax.crypto.Mac#init(java.security.Key)`

### 소스 코드

`src/main/java/com/dodamdodam/backend/auth/infrastructure/EmailVerificationCodeHasher.java:26`

```java
public byte[] hash(String code) {
    if (code == null || !CODE.matcher(code).matches()) {
        throw new IllegalArgumentException("인증번호는 정확히 6자리 ASCII 숫자여야 합니다.");
    }
    try {
        // Mac은 요청마다 생성해 동시 발급 사이에 내부 계산 상태를 공유하지 않는다.
        var mac = Mac.getInstance(ALGORITHM);
        mac.init(key);
        return mac.doFinal(code.getBytes(StandardCharsets.UTF_8));
    }
    catch (GeneralSecurityException unavailable) {
        throw new AuthStateUnavailableException(unavailable);
    }
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## 생성자 m946367bd413db940

외부 주입한 이메일 인증 전용 32바이트 키만 받으며 임의 대체 키를 만들지 않는다.

```java
public EmailVerificationCodeHasher(byte[] key)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationKeys#생성자 m6a60175707d219eb|EmailVerificationKeys.생성자]] — 생성 · L23

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthStateUnavailableException#생성자 m03e9c81ea8cf5421|AuthStateUnavailableException.생성자]] — 생성 · L19

### 외부 라이브러리 호출

- `java.lang.Object#<init>()`
- `javax.crypto.spec.SecretKeySpec#<init>(byte[],java.lang.String)`

### 소스 코드

`src/main/java/com/dodamdodam/backend/auth/infrastructure/EmailVerificationCodeHasher.java:17`

```java
public EmailVerificationCodeHasher(byte[] key) {
    if (key == null || key.length != 32) {
        throw new AuthStateUnavailableException();
    }
    // SecretKeySpec이 복사하므로 호출자가 원본 배열을 바꿔도 운영 키는 유지된다.
    this.key = new SecretKeySpec(key, ALGORITHM);
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]
