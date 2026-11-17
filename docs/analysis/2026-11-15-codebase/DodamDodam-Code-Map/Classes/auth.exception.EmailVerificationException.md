---
tags: [backend, code-map, auth]
aliases: ["EmailVerificationException"]
---
# EmailVerificationException

`com.dodamdodam.backend.auth.exception.EmailVerificationException`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|시작]] → [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요]] → 이 클래스 · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/90-Reference|전체 참조 자료]]

## 클래스 요약

업무 실패의 상태 코드와 공개 오류 정보를 전달한다.

- 원본: `src/main/java/com/dodamdodam/backend/auth/exception/EmailVerificationException.java:7`
- 작성 메서드/생성자 11개 · 자동 생성 0개

## 메서드 목록

| 메서드 | 하는 일 |
| --- | --- |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.exception.EmailVerificationException#validation m01790b591df96407\|validation]] | 요청 구조·값 검증에 공통으로 사용할 오류를 만든다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.exception.EmailVerificationException#code m8644eccb71cd5266\|code]] | HTTP 오류 연결에 사용할 고정 검증 코드를 반환한다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.exception.EmailVerificationException#deliveryUnavailable m490d86171f7ec4b2\|deliveryUnavailable]] | SMTP 실패 보상이 완료된 뒤 일반화한 재발급 안내만 반환한다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.exception.EmailVerificationException#status m20af4176d3ed6553\|status]] | HTTP 연결에 사용할 고정 상태를 반환한다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.exception.EmailVerificationException#authenticationRequired m42d01e93bfad8fd4\|authenticationRequired]] | 현재 문맥이 폐기·만료·교체된 늦은 응답을 거부한다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.exception.EmailVerificationException#authorizationRequired maac9c6910411385d\|authorizationRequired]] | PIN 목적의 현재 이메일·문맥·generation·PIN 상태 결합이 달라졌음을 표현한다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.exception.EmailVerificationException#pinAlreadySet m21633c5920f88f62\|pinAlreadySet]] | 이미 PIN이 있는 계정에는 최초 설정 발급을 허용하지 않는다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.exception.EmailVerificationException#pinNotSet maf5c01b31a0693e5\|pinNotSet]] | PIN이 없는 계정에는 재설정 발급을 허용하지 않는다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.exception.EmailVerificationException#fields mdca59bb72875c112\|fields]] | 이메일·번호 원문이 없는 필드 오류를 반환한다. |

> [!example]- 생성자·자동 생성 멤버 2개
>
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.exception.EmailVerificationException#생성자 m4fe40ecda50436f2|private EmailVerificationException(String field)]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.exception.EmailVerificationException#생성자 m63cebad49f570c35|private EmailVerificationException(int status, String code, String message, List<ApiErrorResponse.FieldError> fields)]]

## validation m01790b591df96407

요청 구조·값 검증에 공통으로 사용할 오류를 만든다.

```java
public static com.dodamdodam.backend.auth.exception.EmailVerificationException validation(java.lang.String field)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.api.EmailVerificationController#issue m90c67bb20e0ca690|EmailVerificationController.issue]] — 호출 · L43
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.EmailVerificationIssuanceService#preparePin m330c48cfa5c13034|EmailVerificationIssuanceService.preparePin]] — 호출 · L102
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.dto.IssueEmailVerificationRequest#fromJson m94a43db6e5d8c7eb|IssueEmailVerificationRequest.fromJson]] — 호출 · L18,22,29,33
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.dto.IssueEmailVerificationRequest#normalized m0ad571c98c7a6c5e|IssueEmailVerificationRequest.normalized]] — 호출 · L42,46,56

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.exception.EmailVerificationException#생성자 m4fe40ecda50436f2|EmailVerificationException.생성자]] — 생성 · L28

### 소스 코드

`src/main/java/com/dodamdodam/backend/auth/exception/EmailVerificationException.java:27`

```java
public static EmailVerificationException validation(String field) {
    return new EmailVerificationException(field);
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## code m8644eccb71cd5266

HTTP 오류 연결에 사용할 고정 검증 코드를 반환한다.

```java
public java.lang.String code()
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.api.EmailVerificationExceptionHandler#handle m512792dc15d45e08|EmailVerificationExceptionHandler.handle]] — 호출 · L18
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthStateErrorFilter#doFilterInternal m039987860b5cf49f|AuthStateErrorFilter.doFilterInternal]] — 호출 · L79

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 소스 코드

`src/main/java/com/dodamdodam/backend/auth/exception/EmailVerificationException.java:32`

```java
public String code() {
    return code;
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## deliveryUnavailable m490d86171f7ec4b2

SMTP 실패 보상이 완료된 뒤 일반화한 재발급 안내만 반환한다.

```java
public static com.dodamdodam.backend.auth.exception.EmailVerificationException deliveryUnavailable()
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.EmailVerificationDeliveryService#deliver mce05e4112cdb91d4|EmailVerificationDeliveryService.deliver]] — 호출 · L40
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.EmailVerificationIssuanceService#verifyResponseAccess me8292ce668aadbdf|EmailVerificationIssuanceService.verifyResponseAccess]] — 호출 · L141

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.exception.EmailVerificationException#생성자 m63cebad49f570c35|EmailVerificationException.생성자]] — 생성 · L38

### 외부 라이브러리 호출

- `java.util.List#of()`

### 소스 코드

`src/main/java/com/dodamdodam/backend/auth/exception/EmailVerificationException.java:37`

```java
public static EmailVerificationException deliveryUnavailable() {
    return new EmailVerificationException(503, "EMAIL_DELIVERY_UNAVAILABLE",
            "이메일을 전송할 수 없습니다. 잠시 후 새 인증번호를 요청해 주세요.", List.of());
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## status m20af4176d3ed6553

HTTP 연결에 사용할 고정 상태를 반환한다.

```java
public int status()
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.api.EmailVerificationExceptionHandler#handle m512792dc15d45e08|EmailVerificationExceptionHandler.handle]] — 호출 · L17
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthStateErrorFilter#doFilterInternal m039987860b5cf49f|AuthStateErrorFilter.doFilterInternal]] — 호출 · L79

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 소스 코드

`src/main/java/com/dodamdodam/backend/auth/exception/EmailVerificationException.java:43`

```java
public int status() {
    return status;
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## authenticationRequired m42d01e93bfad8fd4

현재 문맥이 폐기·만료·교체된 늦은 응답을 거부한다.

```java
public static com.dodamdodam.backend.auth.exception.EmailVerificationException authenticationRequired()
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.EmailVerificationIssuanceService#preparePin m330c48cfa5c13034|EmailVerificationIssuanceService.preparePin]] — 호출/참조 · L96,98
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.EmailVerificationIssuanceService#verifyResponseAccess me8292ce668aadbdf|EmailVerificationIssuanceService.verifyResponseAccess]] — 호출 · L129

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.exception.EmailVerificationException#생성자 m63cebad49f570c35|EmailVerificationException.생성자]] — 생성 · L49

### 외부 라이브러리 호출

- `java.util.List#of()`

### 소스 코드

`src/main/java/com/dodamdodam/backend/auth/exception/EmailVerificationException.java:48`

```java
public static EmailVerificationException authenticationRequired() {
    return new EmailVerificationException(401, "AUTHENTICATION_REQUIRED", "로그인이 필요합니다.", List.of());
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## authorizationRequired maac9c6910411385d

PIN 목적의 현재 이메일·문맥·generation·PIN 상태 결합이 달라졌음을 표현한다.

```java
public static com.dodamdodam.backend.auth.exception.EmailVerificationException authorizationRequired(com.dodamdodam.backend.auth.dto.IssueEmailVerificationRequest.Purpose purpose)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.EmailVerificationIssuanceService#verifyResponseAccess me8292ce668aadbdf|EmailVerificationIssuanceService.verifyResponseAccess]] — 호출 · L136

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.exception.EmailVerificationException#생성자 m63cebad49f570c35|EmailVerificationException.생성자]] — 생성 · L54

### 외부 라이브러리 호출

- `java.util.List#of()`

### 소스 코드

`src/main/java/com/dodamdodam/backend/auth/exception/EmailVerificationException.java:53`

```java
public static EmailVerificationException authorizationRequired(Purpose purpose) {
    return new EmailVerificationException(403, purpose == Purpose.PIN_SETUP
            ? "PIN_SETUP_AUTHORIZATION_REQUIRED" : "PIN_RESET_AUTHORIZATION_REQUIRED",
            "현재 세션에서 이메일 인증을 다시 진행해 주세요.", List.of());
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## pinAlreadySet m21633c5920f88f62

이미 PIN이 있는 계정에는 최초 설정 발급을 허용하지 않는다.

```java
public static com.dodamdodam.backend.auth.exception.EmailVerificationException pinAlreadySet()
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.EmailVerificationIssuanceService#preparePin m330c48cfa5c13034|EmailVerificationIssuanceService.preparePin]] — 호출 · L105

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.exception.EmailVerificationException#생성자 m63cebad49f570c35|EmailVerificationException.생성자]] — 생성 · L61

### 외부 라이브러리 호출

- `java.util.List#of()`

### 소스 코드

`src/main/java/com/dodamdodam/backend/auth/exception/EmailVerificationException.java:60`

```java
public static EmailVerificationException pinAlreadySet() {
    return new EmailVerificationException(409, "PIN_ALREADY_SET", "이미 보호자 PIN이 설정되어 있습니다.", List.of());
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## pinNotSet maf5c01b31a0693e5

PIN이 없는 계정에는 재설정 발급을 허용하지 않는다.

```java
public static com.dodamdodam.backend.auth.exception.EmailVerificationException pinNotSet()
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.EmailVerificationIssuanceService#preparePin m330c48cfa5c13034|EmailVerificationIssuanceService.preparePin]] — 호출 · L108

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.exception.EmailVerificationException#생성자 m63cebad49f570c35|EmailVerificationException.생성자]] — 생성 · L66

### 외부 라이브러리 호출

- `java.util.List#of()`

### 소스 코드

`src/main/java/com/dodamdodam/backend/auth/exception/EmailVerificationException.java:65`

```java
public static EmailVerificationException pinNotSet() {
    return new EmailVerificationException(409, "PIN_NOT_SET", "보호자 PIN이 설정되어 있지 않습니다.", List.of());
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## fields mdca59bb72875c112

이메일·번호 원문이 없는 필드 오류를 반환한다.

```java
public java.util.List<com.dodamdodam.backend.global.error.ApiErrorResponse.FieldError> fields()
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.api.EmailVerificationExceptionHandler#handle m512792dc15d45e08|EmailVerificationExceptionHandler.handle]] — 호출 · L18

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 소스 코드

`src/main/java/com/dodamdodam/backend/auth/exception/EmailVerificationException.java:70`

```java
public List<FieldError> fields() {
    return fields;
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## 생성자 m4fe40ecda50436f2

입력 원문 대신 검증에 실패한 필드만 보관한다.

```java
private EmailVerificationException(java.lang.String field)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.exception.EmailVerificationException#validation m01790b591df96407|EmailVerificationException.validation]] — 생성 · L28

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.exception.EmailVerificationException#생성자 m63cebad49f570c35|EmailVerificationException.생성자]] — 호출 · L15
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.error.ApiErrorResponse.FieldError#생성자 mf0e62423c0dec9de|FieldError.생성자]] — 생성 · L15

### 외부 라이브러리 호출

- `java.util.List#of(E)`

### 소스 코드

`src/main/java/com/dodamdodam/backend/auth/exception/EmailVerificationException.java:14`

```java
private EmailVerificationException(String field) {
    this(400, "VALIDATION_FAILED", "입력값을 확인해 주세요.", List.of(new FieldError(field, "유효하지 않은 값입니다.")));
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## 생성자 m63cebad49f570c35

외부 실패 원문을 보관하지 않고 허용한 업무 오류만 구성한다.

```java
private EmailVerificationException(int status, java.lang.String code, java.lang.String message, java.util.List<com.dodamdodam.backend.global.error.ApiErrorResponse.FieldError> fields)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.exception.EmailVerificationException#생성자 m4fe40ecda50436f2|EmailVerificationException.생성자]] — 호출 · L15
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.exception.EmailVerificationException#authenticationRequired m42d01e93bfad8fd4|EmailVerificationException.authenticationRequired]] — 생성 · L49
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.exception.EmailVerificationException#authorizationRequired maac9c6910411385d|EmailVerificationException.authorizationRequired]] — 생성 · L54
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.exception.EmailVerificationException#deliveryUnavailable m490d86171f7ec4b2|EmailVerificationException.deliveryUnavailable]] — 생성 · L38
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.exception.EmailVerificationException#pinAlreadySet m21633c5920f88f62|EmailVerificationException.pinAlreadySet]] — 생성 · L61
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.exception.EmailVerificationException#pinNotSet maf5c01b31a0693e5|EmailVerificationException.pinNotSet]] — 생성 · L66

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 외부 라이브러리 호출

- `java.lang.RuntimeException#<init>(java.lang.String)`

### 소스 코드

`src/main/java/com/dodamdodam/backend/auth/exception/EmailVerificationException.java:19`

```java
private EmailVerificationException(int status, String code, String message, List<FieldError> fields) {
    super(message);
    this.status = status;
    this.code = code;
    this.fields = fields;
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]
