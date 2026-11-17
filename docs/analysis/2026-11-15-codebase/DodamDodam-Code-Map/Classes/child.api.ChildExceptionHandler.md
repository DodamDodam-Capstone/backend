---
tags: [backend, code-map, child]
aliases: ["ChildExceptionHandler"]
---
# ChildExceptionHandler

`com.dodamdodam.backend.child.api.ChildExceptionHandler`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|시작]] → [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/child|기능 개요]] → 이 클래스 · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/90-Reference|전체 참조 자료]]

## 클래스 요약

자녀 업무 예외만 공통 오류 응답으로 변환해 다른 기능의 인증 장애와 구분한다.

- 원본: `src/main/java/com/dodamdodam/backend/child/api/ChildExceptionHandler.java:14`
- 작성 메서드/생성자 1개 · 자동 생성 1개

## 메서드 목록

| 메서드 | 하는 일 |
| --- | --- |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.api.ChildExceptionHandler#handle ma451743715a09223\|handle]] | 자녀 업무 예외만 공통 오류 응답으로 변환해 다른 기능의 인증 장애와 구분한다. |

> [!example]- 생성자·자동 생성 멤버 1개
>
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.api.ChildExceptionHandler#생성자 m05592ff2e913c1cf|public ChildExceptionHandler() [자동]]]

## handle ma451743715a09223

자녀 업무 예외만 공통 오류 응답으로 변환해 다른 기능의 인증 장애와 구분한다.

```java
public org.springframework.http.ResponseEntity<com.dodamdodam.backend.global.error.ApiErrorResponse> handle(com.dodamdodam.backend.child.exception.ChildException exception, jakarta.servlet.http.HttpServletResponse response)
```

설정: `@org.springframework.web.bind.annotation.ExceptionHandler({com.dodamdodam.backend.child.exception.ChildException.class})`

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.api.ConversationStartResponseFilter#doFilterInternal m2e5a11b062f28145|ConversationStartResponseFilter.doFilterInternal]] — 호출 · L112
- 프레임워크 진입: Spring MVC 예외 처리

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.exception.ChildException#code m9503281b2a5259d9|ChildException.code]] — 호출 · L21,31
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.exception.ChildException#fields m5ca1eb5c4e2bd206|ChildException.fields]] — 호출 · L33
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.error.ApiErrorResponses#body me905a471c60d2a7e|ApiErrorResponses.body]] — 호출 · L29

### 외부 라이브러리 호출

- `java.lang.Throwable#getMessage()`
- `org.springframework.http.ResponseEntity#status(org.springframework.http.HttpStatusCode)`
- `org.springframework.http.ResponseEntity.BodyBuilder#body(@org.jspecify.annotations.Nullable T)`

### 소스 코드

`src/main/java/com/dodamdodam/backend/child/api/ChildExceptionHandler.java:19`

```java
@ExceptionHandler(ChildException.class)
public ResponseEntity<ApiErrorResponse> handle(ChildException exception, HttpServletResponse response) {
    var status = switch (exception.code()) {
        case "VALIDATION_FAILED" -> HttpStatus.BAD_REQUEST;
        case "RESOURCE_NOT_FOUND" -> HttpStatus.NOT_FOUND;
        case "CHILD_LIMIT_REACHED" -> HttpStatus.CONFLICT;
        default -> HttpStatus.INTERNAL_SERVER_ERROR;
    };
    // 업무 DB 오류는 MVC에서 처리해 원인 순회 중 인증 장애 503으로 바뀌지 않게 한다.
    return ResponseEntity.status(status).body(
        ApiErrorResponses.body(
            response,
            exception.code(),
            exception.getMessage(),
            exception.fields()
        )
    );
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/child|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## 생성자 m05592ff2e913c1cf

컴파일러가 생성한 멤버다.

```java
public ChildExceptionHandler()
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 외부 라이브러리 호출

- `java.lang.Object#<init>()`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/child|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]
