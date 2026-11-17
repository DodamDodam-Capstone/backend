---
tags: [backend, code-map, global]
aliases: ["ApiErrorResponses"]
---
# ApiErrorResponses

`com.dodamdodam.backend.global.error.ApiErrorResponses`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|시작]] → [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/global|기능 개요]] → 이 클래스 · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/90-Reference|전체 참조 자료]]

## 클래스 요약

공통 오류 본문을 만들고 필터 등 MVC 밖에서도 JSON 오류를 쓴다.

- 원본: `src/main/java/com/dodamdodam/backend/global/error/ApiErrorResponses.java:11`
- 작성 메서드/생성자 3개 · 자동 생성 0개

## 메서드 목록

| 메서드 | 하는 일 |
| --- | --- |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.error.ApiErrorResponses#body me905a471c60d2a7e\|body]] | 현재 응답의 요청 ID와 검증 오류를 공통 오류 본문에 담는다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.error.ApiErrorResponses#write m1b723e7027f0ade4\|write]] | MVC 밖의 오류를 호출자가 결정한 상태 코드와 UTF-8 JSON으로 출력한다. |

> [!example]- 생성자·자동 생성 멤버 1개
>
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.error.ApiErrorResponses#생성자 m916cbd56b99db111|private ApiErrorResponses()]]

## body me905a471c60d2a7e

현재 응답의 요청 ID와 검증 오류를 공통 오류 본문에 담는다.

```java
public static com.dodamdodam.backend.global.error.ApiErrorResponse body(jakarta.servlet.http.HttpServletResponse response, java.lang.String code, java.lang.String message, java.util.List<com.dodamdodam.backend.global.error.ApiErrorResponse.FieldError> fields)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.api.AuthLogoutController#logout m43cbd6436794ba0f|AuthLogoutController.logout]] — 호출 · L46
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.api.EmailVerificationExceptionHandler#handle m512792dc15d45e08|EmailVerificationExceptionHandler.handle]] — 호출 · L17
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.api.EmailVerificationExceptionHandler#limited m77259e0b6f3fe9c9|EmailVerificationExceptionHandler.limited]] — 호출 · L25
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.api.ChildController#invalidJson m85e9a4840a5175ad|ChildController.invalidJson]] — 호출 · L48
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.api.ChildExceptionHandler#handle ma451743715a09223|ChildExceptionHandler.handle]] — 호출 · L29
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.api.ConversationExceptionHandler#handle m50c7d1b4e6622616|ConversationExceptionHandler.handle]] — 호출 · L33
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.api.ConversationMediaTypeExceptionHandler#handle m16f2ae2ced691415|ConversationMediaTypeExceptionHandler.handle]] — 호출 · L22
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.error.ApiErrorResponses#write m1b723e7027f0ade4|ApiErrorResponses.write]] — 호출 · L30
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.error.CommonApiExceptionHandler#handle m58650f47953e9086|CommonApiExceptionHandler.handle]] — 호출 · L52,56
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/guardian.api.GuardianAccessExceptionHandler#handle mc83c0587f4b275ce|GuardianAccessExceptionHandler.handle]] — 호출 · L29

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.error.ApiErrorResponse#생성자 mecd727c97ae373eb|ApiErrorResponse.생성자]] — 생성 · L20
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.error.ApiErrorResponse.ErrorDetail#생성자 m5f55229570f9831f|ErrorDetail.생성자]] — 생성 · L20

### 외부 라이브러리 호출

- `jakarta.servlet.http.HttpServletResponse#getHeader(java.lang.String)`

### 소스 코드

`src/main/java/com/dodamdodam/backend/global/error/ApiErrorResponses.java:18`

```java
public static ApiErrorResponse body(HttpServletResponse response, String code, String message,
                                    List<FieldError> fields) {
    return new ApiErrorResponse(new ApiErrorResponse.ErrorDetail(
            code, message, response.getHeader("X-Request-ID"), fields));
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/global|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## write m1b723e7027f0ade4

MVC 밖의 오류를 호출자가 결정한 상태 코드와 UTF-8 JSON으로 출력한다.

```java
public static void write(tools.jackson.databind.json.JsonMapper mapper, jakarta.servlet.http.HttpServletResponse response, int status, java.lang.String code, java.lang.String message)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthStateErrorFilter#doFilterInternal m039987860b5cf49f|AuthStateErrorFilter.doFilterInternal]] — 호출 · L79,82
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthStateErrorHandler#commence ma881aa0732f65535|AuthStateErrorHandler.commence]] — 호출 · L25
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthStateErrorHandler#unavailable md0b4637155a51bcc|AuthStateErrorHandler.unavailable]] — 호출 · L30
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.auth.CsrfAccessDeniedHandler#handle m9adc60284e9c3f84|CsrfAccessDeniedHandler.handle]] — 호출 · L25
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.web.GlobalApiRateLimitFilter#doFilterInternal me8918f88c454eb77|GlobalApiRateLimitFilter.doFilterInternal]] — 호출 · L46
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.web.Part1JsonRequestGuardFilter#doFilterInternal m647a0f12b7d38584|Part1JsonRequestGuardFilter.doFilterInternal]] — 호출 · L54,59
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.web.Part2JsonRequestGuardFilter#doFilterInternal m6ee42797d374d4fc|Part2JsonRequestGuardFilter.doFilterInternal]] — 호출 · L42

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.error.ApiErrorResponses#body me905a471c60d2a7e|ApiErrorResponses.body]] — 호출 · L30

### 외부 라이브러리 호출

- `jakarta.servlet.ServletResponse#getOutputStream()`
- `jakarta.servlet.ServletResponse#setCharacterEncoding(java.lang.String)`
- `jakarta.servlet.ServletResponse#setContentType(java.lang.String)`
- `jakarta.servlet.http.HttpServletResponse#setStatus(int)`
- `java.nio.charset.Charset#name()`
- `java.util.List#of()`
- `tools.jackson.databind.ObjectMapper#writeValue(java.io.OutputStream,java.lang.Object)`

### 소스 코드

`src/main/java/com/dodamdodam/backend/global/error/ApiErrorResponses.java:25`

```java
public static void write(JsonMapper mapper, HttpServletResponse response, int status,
                         String code, String message) throws IOException {
    response.setStatus(status);
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    mapper.writeValue(response.getOutputStream(), body(response, code, message, List.of()));
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/global|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## 생성자 m916cbd56b99db111

요청별 상태 없이 사용하는 공통 함수의 인스턴스 생성을 막는다.

```java
private ApiErrorResponses()
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 외부 라이브러리 호출

- `java.lang.Object#<init>()`

### 소스 코드

`src/main/java/com/dodamdodam/backend/global/error/ApiErrorResponses.java:14`

```java
private ApiErrorResponses() {
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/global|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]
