---
tags: [backend, code-map, auth]
aliases: ["IssueEmailVerificationRequest"]
---
# IssueEmailVerificationRequest

`com.dodamdodam.backend.auth.dto.IssueEmailVerificationRequest`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|시작]] → [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요]] → 이 클래스 · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/90-Reference|전체 참조 자료]]

## 클래스 요약

값을 전달하는 데이터 타입이다.

- 원본: `src/main/java/com/dodamdodam/backend/auth/dto/IssueEmailVerificationRequest.java:10`
- 작성 메서드/생성자 4개 · 자동 생성 6개

## 메서드 목록

| 메서드 | 하는 일 |
| --- | --- |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.dto.IssueEmailVerificationRequest#fromJson m94a43db6e5d8c7eb\|fromJson]] | 목적과 email만 파싱하며 PIN 목적의 email 생략과 명시적 null을 구분한다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.dto.IssueEmailVerificationRequest#normalized m0ad571c98c7a6c5e\|normalized]] | 서비스 처리 전에 호출하며 정규화한 이메일의 형식·길이·목적별 필수 여부를 검사한다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.dto.IssueEmailVerificationRequest#trimEmail mfa022165922cc36a\|trimEmail]] | 자녀 입력과 같은 Unicode 공백 기준으로 양끝만 제거하며 점·+suffix는 보존한다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.dto.IssueEmailVerificationRequest#isSpace mb67b39e3c2723950\|isSpace]] | NBSP를 포함한 바깥 공백을 판정하고 NUL 등 제어 문자는 보정하지 않는다. |

> [!example]- 생성자·자동 생성 멤버 6개
>
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.dto.IssueEmailVerificationRequest#email m16107b4d40454c06|public String email() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.dto.IssueEmailVerificationRequest#equals mf9c4c36d9fa89109|public final boolean equals(Object o) [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.dto.IssueEmailVerificationRequest#hashCode md34bbec10b42eac6|public final int hashCode() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.dto.IssueEmailVerificationRequest#purpose m709393858f28d55e|public IssueEmailVerificationRequest.Purpose purpose() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.dto.IssueEmailVerificationRequest#toString mb3d1c2aae0095223|public final String toString() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.dto.IssueEmailVerificationRequest#생성자 mb489f1512fd4efcb|public IssueEmailVerificationRequest(IssueEmailVerificationRequest.Purpose purpose, String email) [자동]]]

## fromJson m94a43db6e5d8c7eb

목적과 email만 파싱하며 PIN 목적의 email 생략과 명시적 null을 구분한다.

```java
public static com.dodamdodam.backend.auth.dto.IssueEmailVerificationRequest fromJson(tools.jackson.databind.JsonNode body)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.api.EmailVerificationController#issue m90c67bb20e0ca690|EmailVerificationController.issue]] — 호출 · L49

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.dto.IssueEmailVerificationRequest#생성자 mb489f1512fd4efcb|IssueEmailVerificationRequest.생성자]] — 생성 · L35
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.dto.IssueEmailVerificationRequest.Purpose#valueOf m4718c6a857663d83|Purpose.valueOf]] — 호출 · L26
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.exception.EmailVerificationException#validation m01790b591df96407|EmailVerificationException.validation]] — 호출 · L18,22,29,33

### 외부 라이브러리 호출

- `java.util.Set#containsAll(java.util.Collection<?>)`
- `tools.jackson.databind.JsonNode#get(java.lang.String)`
- `tools.jackson.databind.JsonNode#isObject()`
- `tools.jackson.databind.JsonNode#isString()`
- `tools.jackson.databind.JsonNode#propertyNames()`
- `tools.jackson.databind.JsonNode#stringValue()`

### 소스 코드

`src/main/java/com/dodamdodam/backend/auth/dto/IssueEmailVerificationRequest.java:15`

```java
public static IssueEmailVerificationRequest fromJson(JsonNode body) {
    // 계정·보안 문맥은 서버가 결정하므로 요청에서 추가로 받지 않는다.
    if (body == null || !body.isObject() || !FIELDS.containsAll(body.propertyNames())) {
        throw EmailVerificationException.validation("body");
    }
    var suppliedPurpose = body.get("purpose");
    if (suppliedPurpose == null || !suppliedPurpose.isString()) {
        throw EmailVerificationException.validation("purpose");
    }
    Purpose purpose;
    try {
        purpose = Purpose.valueOf(suppliedPurpose.stringValue());
    }
    catch (IllegalArgumentException invalid) {
        throw EmailVerificationException.validation("purpose");
    }
    var suppliedEmail = body.get("email");
    if (suppliedEmail != null && !suppliedEmail.isString()) {
        throw EmailVerificationException.validation("email");
    }
    return new IssueEmailVerificationRequest(purpose,
            suppliedEmail == null ? null : suppliedEmail.stringValue());
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## normalized m0ad571c98c7a6c5e

서비스 처리 전에 호출하며 정규화한 이메일의 형식·길이·목적별 필수 여부를 검사한다.

```java
public com.dodamdodam.backend.auth.dto.IssueEmailVerificationRequest normalized(jakarta.validation.Validator validator)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.EmailVerificationIssuanceService#issue mcb0a83ce2cd55ce0|EmailVerificationIssuanceService.issue]] — 호출 · L57
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.EmailVerificationIssuanceService#preparePin m330c48cfa5c13034|EmailVerificationIssuanceService.preparePin]] — 호출 · L100

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.dto.IssueEmailVerificationRequest#생성자 mb489f1512fd4efcb|IssueEmailVerificationRequest.생성자]] — 생성 · L58
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.dto.IssueEmailVerificationRequest#trimEmail mfa022165922cc36a|IssueEmailVerificationRequest.trimEmail]] — 호출 · L51
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.exception.EmailVerificationException#validation m01790b591df96407|EmailVerificationException.validation]] — 호출 · L42,46,56

### 외부 라이브러리 호출

- `jakarta.validation.Validator#validateValue(java.lang.Class<T>,java.lang.String,java.lang.Object,java.lang.Class<?>[])`
- `java.lang.String#chars()`
- `java.lang.String#isEmpty()`
- `java.lang.String#length()`
- `java.lang.String#toLowerCase(java.util.Locale)`
- `java.util.Set#isEmpty()`
- `java.util.stream.IntStream#allMatch(java.util.function.IntPredicate)`

### 소스 코드

`src/main/java/com/dodamdodam/backend/auth/dto/IssueEmailVerificationRequest.java:40`

```java
public IssueEmailVerificationRequest normalized(Validator validator) {
    if (purpose == null) {
        throw EmailVerificationException.validation("purpose");
    }
    if (email == null) {
        if (purpose == Purpose.SIGNUP || purpose == Purpose.RESET_PASSWORD) {
            throw EmailVerificationException.validation("email");
        }
        // PIN 목적의 생략 값은 후속 서비스에서 현재 계정 이메일로 채운다.
        return this;
    }
    var normalizedEmail = trimEmail(email).toLowerCase(Locale.ROOT);
    // provider가 허용하는 국제화 이메일도 서비스의 ASCII 식별 정책으로 제한한다.
    if (normalizedEmail.isEmpty() || normalizedEmail.length() > 254
            || !normalizedEmail.chars().allMatch(character -> character >= 0x21 && character <= 0x7e)
            || !validator.validateValue(IssueEmailVerificationRequest.class, "email", normalizedEmail).isEmpty()) {
        throw EmailVerificationException.validation("email");
    }
    return new IssueEmailVerificationRequest(purpose, normalizedEmail);
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## trimEmail mfa022165922cc36a

자녀 입력과 같은 Unicode 공백 기준으로 양끝만 제거하며 점·+suffix는 보존한다.

```java
private static java.lang.String trimEmail(java.lang.String value)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.dto.IssueEmailVerificationRequest#normalized m0ad571c98c7a6c5e|IssueEmailVerificationRequest.normalized]] — 호출 · L51

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.dto.IssueEmailVerificationRequest#isSpace mb67b39e3c2723950|IssueEmailVerificationRequest.isSpace]] — 호출 · L65,68

### 외부 라이브러리 호출

- `java.lang.Character#charCount(int)`
- `java.lang.String#codePointAt(int)`
- `java.lang.String#codePointBefore(int)`
- `java.lang.String#length()`
- `java.lang.String#substring(int,int)`

### 소스 코드

`src/main/java/com/dodamdodam/backend/auth/dto/IssueEmailVerificationRequest.java:62`

```java
private static String trimEmail(String value) {
    int start = 0;
    int end = value.length();
    while (start < end && isSpace(value.codePointAt(start))) {
        start += Character.charCount(value.codePointAt(start));
    }
    while (end > start && isSpace(value.codePointBefore(end))) {
        end -= Character.charCount(value.codePointBefore(end));
    }
    return value.substring(start, end);
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## isSpace mb67b39e3c2723950

NBSP를 포함한 바깥 공백을 판정하고 NUL 등 제어 문자는 보정하지 않는다.

```java
private static boolean isSpace(int codePoint)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.dto.IssueEmailVerificationRequest#trimEmail mfa022165922cc36a|IssueEmailVerificationRequest.trimEmail]] — 호출 · L65,68

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 외부 라이브러리 호출

- `java.lang.Character#isSpaceChar(int)`
- `java.lang.Character#isWhitespace(int)`

### 소스 코드

`src/main/java/com/dodamdodam/backend/auth/dto/IssueEmailVerificationRequest.java:75`

```java
private static boolean isSpace(int codePoint) {
    return Character.isWhitespace(codePoint) || Character.isSpaceChar(codePoint);
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## email m16107b4d40454c06

컴파일러가 생성한 멤버다.

```java
public java.lang.@jakarta.validation.constraints.Email String email()
```

설정: `@jakarta.validation.constraints.Email`

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.EmailVerificationIssuanceService#issue mcb0a83ce2cd55ce0|EmailVerificationIssuanceService.issue]] — 호출 · L58
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.EmailVerificationIssuanceService#preparePin m330c48cfa5c13034|EmailVerificationIssuanceService.preparePin]] — 호출 · L100,101
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.EmailVerificationIssuanceService#preparePublic m8348ca544b936e88|EmailVerificationIssuanceService.preparePublic]] — 호출 · L81,82,84,86

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## equals mf9c4c36d9fa89109

컴파일러가 생성한 멤버다.

```java
public final boolean equals(java.lang.Object o)
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## hashCode md34bbec10b42eac6

컴파일러가 생성한 멤버다.

```java
public final int hashCode()
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## purpose m709393858f28d55e

컴파일러가 생성한 멤버다.

```java
public com.dodamdodam.backend.auth.dto.IssueEmailVerificationRequest.Purpose purpose()
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.EmailVerificationIssuanceService#issue mcb0a83ce2cd55ce0|EmailVerificationIssuanceService.issue]] — 호출 · L53,58
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.EmailVerificationIssuanceService#preparePin m330c48cfa5c13034|EmailVerificationIssuanceService.preparePin]] — 호출 · L104,107,110,111,112,113
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.EmailVerificationIssuanceService#preparePublic m8348ca544b936e88|EmailVerificationIssuanceService.preparePublic]] — 호출 · L81,83,84,85,86

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## toString mb3d1c2aae0095223

컴파일러가 생성한 멤버다.

```java
public final java.lang.String toString()
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## 생성자 mb489f1512fd4efcb

컴파일러가 생성한 멤버다.

```java
public IssueEmailVerificationRequest(com.dodamdodam.backend.auth.dto.IssueEmailVerificationRequest.Purpose purpose, java.lang.@jakarta.validation.constraints.Email String email)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.EmailVerificationIssuanceService#preparePin m330c48cfa5c13034|EmailVerificationIssuanceService.preparePin]] — 생성 · L100
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.dto.IssueEmailVerificationRequest#fromJson m94a43db6e5d8c7eb|IssueEmailVerificationRequest.fromJson]] — 생성 · L35
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.dto.IssueEmailVerificationRequest#normalized m0ad571c98c7a6c5e|IssueEmailVerificationRequest.normalized]] — 생성 · L58

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 외부 라이브러리 호출

- `java.lang.Record#<init>()`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]
