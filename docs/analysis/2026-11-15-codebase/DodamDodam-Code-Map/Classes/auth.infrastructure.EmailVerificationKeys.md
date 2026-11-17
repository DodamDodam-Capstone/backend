---
tags: [backend, code-map, auth]
aliases: ["EmailVerificationKeys"]
---
# EmailVerificationKeys

`com.dodamdodam.backend.auth.infrastructure.EmailVerificationKeys`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|시작]] → [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요]] → 이 클래스 · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/90-Reference|전체 참조 자료]]

## 클래스 요약

인증번호와 요청 제한에 필요한 전용 HMAC 키를 읽고 검증한다.

- 원본: `src/main/java/com/dodamdodam/backend/auth/infrastructure/EmailVerificationKeys.java:12`
- 작성 메서드/생성자 4개 · 자동 생성 0개

## 메서드 목록

| 메서드 | 하는 일 |
| --- | --- |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationKeys#rateHasher ma21ea9ca87c2fdb6\|rateHasher]] | 전역 제한 키가 없으면 무제한 처리를 허용하지 않는다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationKeys#codeHasher mb9d6c6ffe4dfc2aa\|codeHasher]] | 번호 키가 없거나 제한 키와 같으면 발급을 거부한다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationKeys#readKey m06bb5c4ec04bd0b1\|readKey]] | 파일 경로·내용을 예외에 복사하지 않고 raw 32바이트만 읽는다. |

> [!example]- 생성자·자동 생성 멤버 1개
>
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationKeys#생성자 m6a60175707d219eb|public EmailVerificationKeys(Environment environment)]]

## rateHasher ma21ea9ca87c2fdb6

전역 제한 키가 없으면 무제한 처리를 허용하지 않는다.

```java
public com.dodamdodam.backend.auth.infrastructure.AuthRateLimitKeyHasher rateHasher()
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.EmailVerificationIssuanceService#issue mcb0a83ce2cd55ce0|EmailVerificationIssuanceService.issue]] — 호출 · L58
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.web.GlobalApiRateLimitFilter#doFilterInternal me8918f88c454eb77|GlobalApiRateLimitFilter.doFilterInternal]] — 호출 · L42

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthStateUnavailableException#생성자 m03e9c81ea8cf5421|AuthStateUnavailableException.생성자]] — 생성 · L35

### 소스 코드

`src/main/java/com/dodamdodam/backend/auth/infrastructure/EmailVerificationKeys.java:33`

```java
public AuthRateLimitKeyHasher rateHasher() {
    if (rate == null) {
        throw new AuthStateUnavailableException();
    }
    return rate;
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## codeHasher mb9d6c6ffe4dfc2aa

번호 키가 없거나 제한 키와 같으면 발급을 거부한다.

```java
public com.dodamdodam.backend.auth.infrastructure.EmailVerificationCodeHasher codeHasher()
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.EmailVerificationIssuanceService#issue mcb0a83ce2cd55ce0|EmailVerificationIssuanceService.issue]] — 호출 · L61

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthStateUnavailableException#생성자 m03e9c81ea8cf5421|AuthStateUnavailableException.생성자]] — 생성 · L43

### 소스 코드

`src/main/java/com/dodamdodam/backend/auth/infrastructure/EmailVerificationKeys.java:41`

```java
public EmailVerificationCodeHasher codeHasher() {
    if (code == null) {
        throw new AuthStateUnavailableException();
    }
    return code;
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## readKey m06bb5c4ec04bd0b1

파일 경로·내용을 예외에 복사하지 않고 raw 32바이트만 읽는다.

```java
private static byte[] readKey(java.lang.String file)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationKeys#생성자 m6a60175707d219eb|EmailVerificationKeys.생성자]] — 호출 · L19,20

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 외부 라이브러리 호출

- `java.io.InputStream#readNBytes(int)`
- `java.lang.String#isBlank()`
- `java.nio.file.Files#newInputStream(java.nio.file.Path,java.nio.file.OpenOption[])`
- `java.nio.file.Path#of(java.lang.String,java.lang.String[])`

### 소스 코드

`src/main/java/com/dodamdodam/backend/auth/infrastructure/EmailVerificationKeys.java:49`

```java
private static byte[] readKey(String file) {
    if (file.isBlank()) {
        return null;
    }
    try (var input = Files.newInputStream(Path.of(file))) {
        var bytes = input.readNBytes(33);
        return bytes.length == 32 ? bytes : null;
    }
    catch (IOException | RuntimeException unavailable) {
        return null;
    }
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## 생성자 m6a60175707d219eb

프로세스 시작 시 전용 파일을 한 번 읽고 미설정 기능만 안전하게 닫는다.

```java
public EmailVerificationKeys(org.springframework.core.env.Environment environment)
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.AuthRateLimitKeyHasher#생성자 m709dfe6acfd13ccf|AuthRateLimitKeyHasher.생성자]] — 생성 · L21
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationCodeHasher#생성자 m946367bd413db940|EmailVerificationCodeHasher.생성자]] — 생성 · L23
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationKeys#readKey m06bb5c4ec04bd0b1|EmailVerificationKeys.readKey]] — 호출 · L19,20

### 외부 라이브러리 호출

- `java.lang.Object#<init>()`
- `java.security.MessageDigest#isEqual(byte[],byte[])`
- `java.util.Arrays#fill(byte[],byte)`
- `org.springframework.core.env.PropertyResolver#getProperty(java.lang.String,java.lang.String)`

### 소스 코드

`src/main/java/com/dodamdodam/backend/auth/infrastructure/EmailVerificationKeys.java:18`

```java
public EmailVerificationKeys(Environment environment) {
    byte[] rateKey = readKey(environment.getProperty("app.security.rate-limit-key-file", ""));
    byte[] codeKey = readKey(environment.getProperty("app.email.code-key-file", ""));
    this.rate = rateKey == null ? null : new AuthRateLimitKeyHasher(rateKey);
    this.code = rateKey == null || codeKey == null || MessageDigest.isEqual(rateKey, codeKey)
            ? null : new EmailVerificationCodeHasher(codeKey);
    if (rateKey != null) {
        Arrays.fill(rateKey, (byte) 0);
    }
    if (codeKey != null) {
        Arrays.fill(codeKey, (byte) 0);
    }
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]
