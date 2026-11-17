---
tags: [backend, code-map, global]
aliases: ["CommonApiExceptionHandler"]
---
# CommonApiExceptionHandler

`com.dodamdodam.backend.global.error.CommonApiExceptionHandler`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|시작]] → [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/global|기능 개요]] → 이 클래스 · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/90-Reference|전체 참조 자료]]

## 클래스 요약

기능별 처리기가 다루지 않은 MVC 오류를 공통 응답으로 변환한다.

- 원본: `src/main/java/com/dodamdodam/backend/global/error/CommonApiExceptionHandler.java:21`
- 작성 메서드/생성자 2개 · 자동 생성 0개

## 메서드 목록

| 메서드 | 하는 일 |
| --- | --- |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.error.CommonApiExceptionHandler#handle m58650f47953e9086\|handle]] | 기능별 처리 뒤에 남은 컨트롤러 오류를 원문 없는 공통 응답으로 변환한다. |

> [!example]- 생성자·자동 생성 멤버 1개
>
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.error.CommonApiExceptionHandler#생성자 m8c9b77124cfbb104|public CommonApiExceptionHandler(SessionLifecycleService lifecycle)]]

## handle m58650f47953e9086

기능별 처리 뒤에 남은 컨트롤러 오류를 원문 없는 공통 응답으로 변환한다.

```java
public org.springframework.http.ResponseEntity<com.dodamdodam.backend.global.error.ApiErrorResponse> handle(java.lang.Exception exception, jakarta.servlet.http.HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response)
```

설정: `@org.springframework.web.bind.annotation.ExceptionHandler({java.lang.Exception.class})`

### 이 메서드를 호출하는 곳

- 프레임워크 진입: Spring MVC 예외 처리

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.SessionLifecycleService#hasPendingLogin m6ec9457b18ffe30a|SessionLifecycleService.hasPendingLogin]] — 호출 · L37
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.error.ApiErrorResponse.FieldError#생성자 mf0e62423c0dec9de|FieldError.생성자]] — 생성 · L54
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.error.ApiErrorResponses#body me905a471c60d2a7e|ApiErrorResponses.body]] — 호출 · L52,56

### 외부 라이브러리 호출

- `java.lang.Throwable#getCause()`
- `java.util.List#of()`
- `java.util.List#of(E)`
- `org.springframework.http.ResponseEntity#badRequest()`
- `org.springframework.http.ResponseEntity#internalServerError()`
- `org.springframework.http.ResponseEntity.BodyBuilder#body(@org.jspecify.annotations.Nullable T)`
- `org.springframework.web.method.annotation.MethodArgumentTypeMismatchException#getName()`

### 소스 코드

`src/main/java/com/dodamdodam/backend/global/error/CommonApiExceptionHandler.java:33`

```java
@ExceptionHandler(Exception.class)
public ResponseEntity<ApiErrorResponse> handle(Exception exception, HttpServletRequest request,
                                               HttpServletResponse response) throws Exception {
    // 로그인 중 실패는 외곽 필터의 보상 폐기가 필요하다.
    if (lifecycle.hasPendingLogin(request)) {
        throw exception;
    }
    // guardian 표식과 관계없이 인증 실패·확인 장애는 외곽 필터가 성공 헤더·본문부터 비운다.
    for (Throwable failure = exception; failure != null; failure = failure.getCause()) {
        if (failure instanceof AuthenticationException || failure instanceof InvalidSessionException
                || failure instanceof AuthStateUnavailableException || failure instanceof EmailVerificationException) {
            throw exception;
        }
    }
    if (exception instanceof HttpMessageNotReadableException
            || exception instanceof MethodArgumentTypeMismatchException) {
        // 오류가 난 인자 이름만 반환하고 입력값·파서 메시지는 노출하지 않는다.
        String field = exception instanceof MethodArgumentTypeMismatchException mismatch
                ? mismatch.getName() : "body";
        return ResponseEntity.badRequest().body(ApiErrorResponses.body(
                response, "VALIDATION_FAILED", "입력값을 확인해 주세요.",
                List.of(new FieldError(field, "유효하지 않은 값입니다."))));
    }
    return ResponseEntity.internalServerError().body(ApiErrorResponses.body(
            response, "INTERNAL_ERROR", "요청을 처리할 수 없습니다. 잠시 후 다시 시도해 주세요.", List.of()));
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/global|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## 생성자 m8c9b77124cfbb104

진행 중 로그인에 필요한 외곽 필터의 실패 보상 경로를 확인한다.

```java
public CommonApiExceptionHandler(com.dodamdodam.backend.auth.session.SessionLifecycleService lifecycle)
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 외부 라이브러리 호출

- `java.lang.Object#<init>()`

### 소스 코드

`src/main/java/com/dodamdodam/backend/global/error/CommonApiExceptionHandler.java:28`

```java
public CommonApiExceptionHandler(SessionLifecycleService lifecycle) {
    this.lifecycle = lifecycle;
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/global|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]
