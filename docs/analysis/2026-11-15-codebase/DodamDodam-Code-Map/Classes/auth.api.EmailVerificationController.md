---
tags: [backend, code-map, auth]
aliases: ["EmailVerificationController"]
---
# EmailVerificationController

`com.dodamdodam.backend.auth.api.EmailVerificationController`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|시작]] → [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요]] → 이 클래스 · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/90-Reference|전체 참조 자료]]

## 클래스 요약

이메일 발급 HTTP 진입점. JSON을 읽고 PIN 목적의 선행 인증을 처리한다.

- 원본: `src/main/java/com/dodamdodam/backend/auth/api/EmailVerificationController.java:18`
- 작성 메서드/생성자 2개 · 자동 생성 0개

## 메서드 목록

| 메서드 | 하는 일 |
| --- | --- |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.api.EmailVerificationController#issue m90c67bb20e0ca690\|issue]] | PIN 목적은 구조·필드 검사 전에 현재 로그인을 요구하며 허용한 응답 두 필드만 반환한다. |

> [!example]- 생성자·자동 생성 멤버 1개
>
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.api.EmailVerificationController#생성자 m7328bd045d5560a8|public EmailVerificationController(AuthenticatedSessionService authentication, EmailVerificationIssuanceService issuance, JsonMapper mapper)]]

## issue m90c67bb20e0ca690

PIN 목적은 구조·필드 검사 전에 현재 로그인을 요구하며 허용한 응답 두 필드만 반환한다.

```java
public java.util.Map<java.lang.String,com.dodamdodam.backend.auth.dto.VerificationIssueResponse> issue(jakarta.servlet.http.HttpServletRequest request)
```

설정: `@org.springframework.web.bind.annotation.PostMapping({"/api/v1/auth/email-verifications"})`

### 이 메서드를 호출하는 곳

- 프레임워크 진입: POST /api/v1/auth/email-verifications

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.EmailVerificationIssuanceService#issue mcb0a83ce2cd55ce0|EmailVerificationIssuanceService.issue]] — 호출 · L50
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.dto.IssueEmailVerificationRequest#fromJson m94a43db6e5d8c7eb|IssueEmailVerificationRequest.fromJson]] — 호출 · L49
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.exception.EmailVerificationException#validation m01790b591df96407|EmailVerificationException.validation]] — 호출 · L43
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthenticatedSessionService#requireAuthenticatedSession mad23075b25d06a28|AuthenticatedSessionService.requireAuthenticatedSession]] — 호출 · L48
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.web.ClientAddressResolver#resolve m53c50e28b5cfa314|ClientAddressResolver.resolve]] — 호출 · L50

### 외부 라이브러리 호출

- `jakarta.servlet.ServletRequest#getReader()`
- `java.lang.String#equals(java.lang.Object)`
- `java.util.Map#of(K,V)`
- `tools.jackson.databind.JsonNode#asText()`
- `tools.jackson.databind.JsonNode#get(java.lang.String)`
- `tools.jackson.databind.JsonNode#isObject()`
- `tools.jackson.databind.JsonNode#isString()`
- `tools.jackson.databind.ObjectMapper#readTree(java.io.Reader)`

### 소스 코드

`src/main/java/com/dodamdodam/backend/auth/api/EmailVerificationController.java:34`

```java
@PostMapping("/api/v1/auth/email-verifications")
public Map<String, VerificationIssueResponse> issue(HttpServletRequest request) throws IOException {
    JsonNode body;
    try {
        // JsonNode로 변환한 뒤에는 사라지는 중복 필드와 뒤따르는 JSON도 입구에서 거부한다.
        // @RequestBody 원문 인자는 framework DEBUG 로그에 남을 수 있어 직접 읽는다.
        body = requestMapper.readTree(request.getReader());
    }
    catch (JacksonException invalid) {
        throw EmailVerificationException.validation("body");
    }
    var purpose = body != null && body.isObject() ? body.get("purpose") : null;
    boolean pin = purpose != null && purpose.isString()
            && (purpose.asText().equals("PIN_SETUP") || purpose.asText().equals("PIN_RESET"));
    var current = pin ? authentication.requireAuthenticatedSession(request) : null;
    var input = IssueEmailVerificationRequest.fromJson(body);
    return Map.of("data", issuance.issue(request, input, current, ClientAddressResolver.resolve(request)));
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## 생성자 m7328bd045d5560a8

조건부 로그인과 기존 입력·발급 업무를 연결한다.

```java
public EmailVerificationController(com.dodamdodam.backend.auth.session.AuthenticatedSessionService authentication, com.dodamdodam.backend.auth.application.EmailVerificationIssuanceService issuance, tools.jackson.databind.json.JsonMapper mapper)
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 외부 라이브러리 호출

- `java.lang.Object#<init>()`
- `tools.jackson.databind.cfg.MapperBuilder#enable(tools.jackson.core.StreamReadFeature[])`
- `tools.jackson.databind.cfg.MapperBuilder#enable(tools.jackson.databind.DeserializationFeature[])`
- `tools.jackson.databind.json.JsonMapper#rebuild()`
- `tools.jackson.databind.json.JsonMapper.Builder#build()`

### 소스 코드

`src/main/java/com/dodamdodam/backend/auth/api/EmailVerificationController.java:25`

```java
public EmailVerificationController(AuthenticatedSessionService authentication, EmailVerificationIssuanceService issuance,
                                   JsonMapper mapper) {
    this.authentication = authentication;
    this.issuance = issuance;
    this.requestMapper = mapper.rebuild().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).build();
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]
