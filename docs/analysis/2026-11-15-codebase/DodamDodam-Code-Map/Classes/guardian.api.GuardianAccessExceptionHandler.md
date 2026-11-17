---
tags: [backend, code-map, guardian]
aliases: ["GuardianAccessExceptionHandler"]
---
# GuardianAccessExceptionHandler

`com.dodamdodam.backend.guardian.api.GuardianAccessExceptionHandler`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|시작]] → [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/guardian|기능 개요]] → 이 클래스 · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/90-Reference|전체 참조 자료]]

## 클래스 요약

최초 권한 오류를 공통 응답으로 바꾸고 응답 직전 실패는 외곽 필터로 넘긴다.

- 원본: `src/main/java/com/dodamdodam/backend/guardian/api/GuardianAccessExceptionHandler.java:16`
- 작성 메서드/생성자 1개 · 자동 생성 1개

## 메서드 목록

| 메서드 | 하는 일 |
| --- | --- |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/guardian.api.GuardianAccessExceptionHandler#handle mc83c0587f4b275ce\|handle]] | 최초 권한 오류를 공통 응답으로 바꾸고 응답 직전 실패는 외곽 필터로 넘긴다. |

> [!example]- 생성자·자동 생성 멤버 1개
>
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/guardian.api.GuardianAccessExceptionHandler#생성자 mc1968c2ca688becc|public GuardianAccessExceptionHandler() [자동]]]

## handle mc83c0587f4b275ce

최초 권한 오류를 공통 응답으로 바꾸고 응답 직전 실패는 외곽 필터로 넘긴다.

```java
public org.springframework.http.ResponseEntity<com.dodamdodam.backend.global.error.ApiErrorResponse> handle(com.dodamdodam.backend.guardian.exception.GuardianAccessException exception, jakarta.servlet.http.HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response)
```

설정: `@org.springframework.web.bind.annotation.ExceptionHandler({com.dodamdodam.backend.guardian.exception.GuardianAccessException.class})`

### 이 메서드를 호출하는 곳

- 프레임워크 진입: Spring MVC 예외 처리

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.error.ApiErrorResponses#body me905a471c60d2a7e|ApiErrorResponses.body]] — 호출 · L29
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/guardian.application.GuardianAccessService#requiresResponseVerification m41add87d49066ff2|GuardianAccessService.requiresResponseVerification]] — 호출 · L24
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/guardian.exception.GuardianAccessException#code m7dd5b7198c3b7a07|GuardianAccessException.code]] — 호출 · L30
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/guardian.exception.GuardianAccessException#status md61668cf15a2751a|GuardianAccessException.status]] — 호출 · L29

### 외부 라이브러리 호출

- `java.lang.Throwable#getMessage()`
- `java.util.List#of()`
- `org.springframework.http.ResponseEntity#status(int)`
- `org.springframework.http.ResponseEntity.BodyBuilder#body(@org.jspecify.annotations.Nullable T)`

### 소스 코드

`src/main/java/com/dodamdodam/backend/guardian/api/GuardianAccessExceptionHandler.java:21`

```java
@ExceptionHandler(GuardianAccessException.class)
public ResponseEntity<ApiErrorResponse> handle(GuardianAccessException exception, HttpServletRequest request,
                                               HttpServletResponse response) {
    if (GuardianAccessService.requiresResponseVerification(request)) {
        // 성공 본문·길이·쿠키가 이미 준비된 경우 외곽 필터가 먼저 응답 전체를 비워야 한다.
        throw exception;
    }
    // 예외의 고정 메시지만 공개하며 원인·PIN·보안 문맥은 응답에 포함하지 않는다.
    return ResponseEntity.status(exception.status()).body(ApiErrorResponses.body(
            response, exception.code(), exception.getMessage(), List.of()));
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/guardian|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## 생성자 mc1968c2ca688becc

컴파일러가 생성한 멤버다.

```java
public GuardianAccessExceptionHandler()
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 외부 라이브러리 호출

- `java.lang.Object#<init>()`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/guardian|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]
