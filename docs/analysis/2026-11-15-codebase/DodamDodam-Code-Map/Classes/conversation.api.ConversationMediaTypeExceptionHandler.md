---
tags: [backend, code-map, conversation]
aliases: ["ConversationMediaTypeExceptionHandler"]
---
# ConversationMediaTypeExceptionHandler

`com.dodamdodam.backend.conversation.api.ConversationMediaTypeExceptionHandler`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|시작]] → [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요]] → 이 클래스 · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/90-Reference|전체 참조 자료]]

## 클래스 요약

시작 컨트롤러의 MVC 미디어 타입 오류만 공통 500 처리보다 먼저 변환한다.

- 원본: `src/main/java/com/dodamdodam/backend/conversation/api/ConversationMediaTypeExceptionHandler.java:15`
- 작성 메서드/생성자 1개 · 자동 생성 1개

## 메서드 목록

| 메서드 | 하는 일 |
| --- | --- |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.api.ConversationMediaTypeExceptionHandler#handle m16f2ae2ced691415\|handle]] | 시작 컨트롤러의 MVC 미디어 타입 오류만 공통 500 처리보다 먼저 변환한다. |

> [!example]- 생성자·자동 생성 멤버 1개
>
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.api.ConversationMediaTypeExceptionHandler#생성자 m067b6ec14fc9f139|public ConversationMediaTypeExceptionHandler() [자동]]]

## handle m16f2ae2ced691415

시작 컨트롤러의 MVC 미디어 타입 오류만 공통 500 처리보다 먼저 변환한다.

```java
public org.springframework.http.ResponseEntity<com.dodamdodam.backend.global.error.ApiErrorResponse> handle(jakarta.servlet.http.HttpServletResponse response)
```

설정: `@org.springframework.web.bind.annotation.ExceptionHandler({org.springframework.web.HttpMediaTypeNotSupportedException.class})`

### 이 메서드를 호출하는 곳

- 프레임워크 진입: Spring MVC 예외 처리

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.error.ApiErrorResponses#body me905a471c60d2a7e|ApiErrorResponses.body]] — 호출 · L22

### 외부 라이브러리 호출

- `java.util.List#of()`
- `org.springframework.http.ResponseEntity#status(org.springframework.http.HttpStatusCode)`
- `org.springframework.http.ResponseEntity.BodyBuilder#body(@org.jspecify.annotations.Nullable T)`

### 소스 코드

`src/main/java/com/dodamdodam/backend/conversation/api/ConversationMediaTypeExceptionHandler.java:20`

```java
@ExceptionHandler(HttpMediaTypeNotSupportedException.class)
public ResponseEntity<ApiErrorResponse> handle(HttpServletResponse response) {
    var error = ApiErrorResponses.body(response, "UNSUPPORTED_MEDIA_TYPE",
            "지원하지 않는 Content-Type입니다.", List.of());
    return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(error);
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## 생성자 m067b6ec14fc9f139

컴파일러가 생성한 멤버다.

```java
public ConversationMediaTypeExceptionHandler()
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 외부 라이브러리 호출

- `java.lang.Object#<init>()`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]
