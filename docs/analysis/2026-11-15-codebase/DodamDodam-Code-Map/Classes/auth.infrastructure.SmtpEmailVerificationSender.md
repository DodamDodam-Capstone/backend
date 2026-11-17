---
tags: [backend, code-map, auth]
aliases: ["SmtpEmailVerificationSender"]
---
# SmtpEmailVerificationSender

`com.dodamdodam.backend.auth.infrastructure.SmtpEmailVerificationSender`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|시작]] → [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요]] → 이 클래스 · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/90-Reference|전체 참조 자료]]

## 클래스 요약

SMTP 서버에 실제 메일 또는 계정 노출 방지용 안내 메일을 전달한다.

- 원본: `src/main/java/com/dodamdodam/backend/auth/infrastructure/SmtpEmailVerificationSender.java:13`
- 작성 메서드/생성자 4개 · 자동 생성 0개

## 메서드 목록

| 메서드 | 하는 일 |
| --- | --- |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.SmtpEmailVerificationSender#send mbb7d859c7dfa3045\|send]] | 정상·decoy 모두 같은 SMTP 경로를 사용하며 decoy에는 번호를 넣지 않는다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.SmtpEmailVerificationSender#bool m32447f00c3ac3ab1\|bool]] | 문자열 설정 오류를 false로 바꿔 TLS를 끄지 않는다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.SmtpEmailVerificationSender#loopback m2e617429e4ba3c74\|loopback]] | 외부 DNS 조회 없이 명시적인 loopback 주소만 테스트용 평문 전송으로 인정한다. |

> [!example]- 생성자·자동 생성 멤버 1개
>
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.SmtpEmailVerificationSender#생성자 mf35cbdfe0f7b51ae|public SmtpEmailVerificationSender(Environment environment)]]

## send mbb7d859c7dfa3045

정상·decoy 모두 같은 SMTP 경로를 사용하며 decoy에는 번호를 넣지 않는다.

```java
public void send(java.lang.String email, java.lang.String code, boolean eligible)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.EmailVerificationDeliveryService#deliver mce05e4112cdb91d4|EmailVerificationDeliveryService.deliver]] — 호출 · L35

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 외부 라이브러리 호출

- `java.lang.IllegalStateException#<init>(java.lang.String)`
- `org.springframework.mail.MailSender#send(org.springframework.mail.SimpleMailMessage)`
- `org.springframework.mail.SimpleMailMessage#<init>()`
- `org.springframework.mail.SimpleMailMessage#setFrom(java.lang.@org.jspecify.annotations.Nullable String)`
- `org.springframework.mail.SimpleMailMessage#setSubject(java.lang.@org.jspecify.annotations.Nullable String)`
- `org.springframework.mail.SimpleMailMessage#setText(java.lang.@org.jspecify.annotations.Nullable String)`
- `org.springframework.mail.SimpleMailMessage#setTo(java.lang.@org.jspecify.annotations.Nullable String)`

### 소스 코드

`src/main/java/com/dodamdodam/backend/auth/infrastructure/SmtpEmailVerificationSender.java:69`

```java
public void send(String email, String code, boolean eligible) {
    if (sender == null) {
        throw new IllegalStateException("이메일 전송 설정을 확인할 수 없습니다.");
    }
    var message = new SimpleMailMessage();
    message.setFrom(from);
    message.setTo(email);
    message.setSubject("도담도담 이메일 확인 안내");
    message.setText(eligible ? "이메일 확인 번호: " + code + "\n10분 안에 입력해 주세요. 요청하지 않았다면 무시해 주세요."
            : "이메일 확인 요청을 받았습니다. 이 요청으로 인증을 진행할 수 없다면 입력 정보와 이용 경로를 확인해 주세요. 요청하지 않았다면 무시해 주세요.");
    sender.send(message);
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## bool m32447f00c3ac3ab1

문자열 설정 오류를 false로 바꿔 TLS를 끄지 않는다.

```java
private static boolean bool(org.springframework.core.env.Environment environment, java.lang.String name, boolean fallback)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.SmtpEmailVerificationSender#생성자 mf35cbdfe0f7b51ae|SmtpEmailVerificationSender.생성자]] — 호출 · L26,27

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 외부 라이브러리 호출

- `java.lang.Boolean#parseBoolean(java.lang.String)`
- `java.lang.Boolean#toString(boolean)`
- `java.lang.IllegalArgumentException#<init>()`
- `java.lang.String#equalsIgnoreCase(java.lang.String)`
- `org.springframework.core.env.PropertyResolver#getProperty(java.lang.String,java.lang.String)`

### 소스 코드

`src/main/java/com/dodamdodam/backend/auth/infrastructure/SmtpEmailVerificationSender.java:83`

```java
private static boolean bool(Environment environment, String name, boolean fallback) {
    String value = environment.getProperty(name, Boolean.toString(fallback));
    if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false")) {
        throw new IllegalArgumentException();
    }
    return Boolean.parseBoolean(value);
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## loopback m2e617429e4ba3c74

외부 DNS 조회 없이 명시적인 loopback 주소만 테스트용 평문 전송으로 인정한다.

```java
private static boolean loopback(java.lang.String host)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.SmtpEmailVerificationSender#생성자 mf35cbdfe0f7b51ae|SmtpEmailVerificationSender.생성자]] — 호출 · L34

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 외부 라이브러리 호출

- `java.lang.String#equals(java.lang.Object)`
- `java.net.InetAddress#isLoopbackAddress()`
- `java.net.InetAddress#ofLiteral(java.lang.String)`

### 소스 코드

`src/main/java/com/dodamdodam/backend/auth/infrastructure/SmtpEmailVerificationSender.java:92`

```java
private static boolean loopback(String host) {
    return host.equals("localhost") || InetAddress.ofLiteral(host).isLoopbackAddress();
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## 생성자 mf35cbdfe0f7b51ae

유한 timeout과 TLS를 적용하며 메일 설정 오류는 다른 API의 기동을 막지 않는다.

```java
public SmtpEmailVerificationSender(org.springframework.core.env.Environment environment)
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.SmtpEmailVerificationSender#bool m32447f00c3ac3ab1|SmtpEmailVerificationSender.bool]] — 호출 · L26,27
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.SmtpEmailVerificationSender#loopback m2e617429e4ba3c74|SmtpEmailVerificationSender.loopback]] — 호출 · L34

### 외부 라이브러리 호출

- `jakarta.mail.internet.InternetAddress#<init>(java.lang.String,boolean)`
- `jakarta.mail.internet.InternetAddress#validate()`
- `java.lang.Boolean#toString(boolean)`
- `java.lang.IllegalArgumentException#<init>()`
- `java.lang.Integer#parseInt(java.lang.String)`
- `java.lang.Integer#toString(int)`
- `java.lang.Object#<init>()`
- `java.lang.String#contains(java.lang.CharSequence)`
- `java.lang.String#isBlank()`
- `java.lang.String#isEmpty()`
- `java.nio.file.Files#readString(java.nio.file.Path,java.nio.charset.Charset)`
- `java.nio.file.Path#of(java.lang.String,java.lang.String[])`
- `java.util.Properties#setProperty(java.lang.String,java.lang.String)`
- `org.springframework.core.env.PropertyResolver#getProperty(java.lang.String,java.lang.String)`
- `org.springframework.mail.javamail.JavaMailSenderImpl#<init>()`
- `org.springframework.mail.javamail.JavaMailSenderImpl#getJavaMailProperties()`
- `org.springframework.mail.javamail.JavaMailSenderImpl#setDefaultEncoding(java.lang.@org.jspecify.annotations.Nullable String)`
- `org.springframework.mail.javamail.JavaMailSenderImpl#setHost(java.lang.@org.jspecify.annotations.Nullable String)`
- `org.springframework.mail.javamail.JavaMailSenderImpl#setPassword(java.lang.@org.jspecify.annotations.Nullable String)`
- `org.springframework.mail.javamail.JavaMailSenderImpl#setPort(int)`
- `org.springframework.mail.javamail.JavaMailSenderImpl#setUsername(java.lang.@org.jspecify.annotations.Nullable String)`

### 소스 코드

`src/main/java/com/dodamdodam/backend/auth/infrastructure/SmtpEmailVerificationSender.java:19`

```java
public SmtpEmailVerificationSender(Environment environment) {
    JavaMailSenderImpl configured = null;
    this.from = environment.getProperty("app.email.smtp.from", "");
    try {
        String host = environment.getProperty("app.email.smtp.host", "");
        int port = Integer.parseInt(environment.getProperty("app.email.smtp.port", "587"));
        int timeout = Integer.parseInt(environment.getProperty("app.email.smtp.timeout-ms", "5000"));
        boolean tls = bool(environment, "app.email.smtp.starttls", true);
        boolean ssl = bool(environment, "app.email.smtp.ssl", false);
        if (host.isBlank() || port < 1 || port > 65535 || timeout < 1 || from.contains("\r") || from.contains("\n")) {
            throw new IllegalArgumentException();
        }
        new jakarta.mail.internet.InternetAddress(from, true).validate();
        // 평문은 인증 없는 loopback SMTP 테스트에만 허용한다.
        String username = environment.getProperty("app.email.smtp.username", "");
        if (!tls && !ssl && (!username.isEmpty() || !loopback(host))) {
            throw new IllegalArgumentException();
        }
        var candidate = new JavaMailSenderImpl();
        candidate.setHost(host);
        candidate.setPort(port);
        candidate.setDefaultEncoding("UTF-8");
        var properties = candidate.getJavaMailProperties();
        properties.setProperty("mail.debug", "false");
        properties.setProperty("mail.smtp.connectiontimeout", Integer.toString(timeout));
        properties.setProperty("mail.smtp.timeout", Integer.toString(timeout));
        properties.setProperty("mail.smtp.writetimeout", Integer.toString(timeout));
        properties.setProperty("mail.smtp.starttls.enable", Boolean.toString(tls));
        properties.setProperty("mail.smtp.starttls.required", Boolean.toString(tls));
        properties.setProperty("mail.smtp.ssl.enable", Boolean.toString(ssl));
        properties.setProperty("mail.smtp.ssl.checkserveridentity", "true");
        properties.setProperty("mail.smtp.auth", Boolean.toString(!username.isEmpty()));
        if (!username.isEmpty()) {
            String file = environment.getProperty("app.email.smtp.password-file", "");
            String password = Files.readString(Path.of(file), StandardCharsets.UTF_8);
            if (password.isEmpty()) {
                throw new IllegalArgumentException();
            }
            candidate.setUsername(username);
            candidate.setPassword(password);
        }
        configured = candidate;
    }
    catch (IOException | RuntimeException | jakarta.mail.MessagingException invalid) {
        configured = null;
    }
    this.sender = configured;
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]
