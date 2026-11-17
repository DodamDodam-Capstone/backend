---
tags: [backend, code-map, conversation]
aliases: ["ConversationExceptionHandler"]
---
# ConversationExceptionHandler

`com.dodamdodam.backend.conversation.api.ConversationExceptionHandler`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|시작]] → [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요]] → 이 클래스 · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/90-Reference|전체 참조 자료]]

## 클래스 요약

응답 전 재검사 실패는 MVC에서 출력하지 않고 전용 필터가 성공 응답부터 비우게 한다.

- 원본: `src/main/java/com/dodamdodam/backend/conversation/api/ConversationExceptionHandler.java:14`
- 작성 메서드/생성자 2개 · 자동 생성 1개

## 메서드 목록

| 메서드 | 하는 일 |
| --- | --- |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.api.ConversationExceptionHandler#propagateResponseRejection mb05ff2b552e323fb\|propagateResponseRejection]] | 응답 전 재검사 실패는 MVC에서 출력하지 않고 전용 필터가 성공 응답부터 비우게 한다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.api.ConversationExceptionHandler#handle m50c7d1b4e6622616\|handle]] | 대화 업무 오류만 변환하고 인증·소유권·JSON 읽기 오류는 기존 처리기에 맡긴다. |

> [!example]- 생성자·자동 생성 멤버 1개
>
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.api.ConversationExceptionHandler#생성자 mfb2389ca6f55126f|public ConversationExceptionHandler() [자동]]]

## propagateResponseRejection mb05ff2b552e323fb

응답 전 재검사 실패는 MVC에서 출력하지 않고 전용 필터가 성공 응답부터 비우게 한다.

```java
void propagateResponseRejection(com.dodamdodam.backend.conversation.api.ConversationStartResponseFilter.ResponseRejected rejection)
```

설정: `@org.springframework.web.bind.annotation.ExceptionHandler({com.dodamdodam.backend.conversation.api.ConversationStartResponseFilter.ResponseRejected.class})`

### 이 메서드를 호출하는 곳

- 프레임워크 진입: Spring MVC 예외 처리

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 소스 코드

`src/main/java/com/dodamdodam/backend/conversation/api/ConversationExceptionHandler.java:19`

```java
@ExceptionHandler(ConversationStartResponseFilter.ResponseRejected.class)
void propagateResponseRejection(ConversationStartResponseFilter.ResponseRejected rejection) {
    throw rejection;
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## handle m50c7d1b4e6622616

대화 업무 오류만 변환하고 인증·소유권·JSON 읽기 오류는 기존 처리기에 맡긴다.

```java
public org.springframework.http.ResponseEntity<?> handle(com.dodamdodam.backend.conversation.exception.ConversationException exception, jakarta.servlet.http.HttpServletResponse response)
```

설정: `@org.springframework.web.bind.annotation.ExceptionHandler({com.dodamdodam.backend.conversation.exception.ConversationException.class})`

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.api.ConversationStartResponseFilter#doFilterInternal m2e5a11b062f28145|ConversationStartResponseFilter.doFilterInternal]] — 호출 · L113
- 프레임워크 진입: Spring MVC 예외 처리

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.dto.ActiveConversationExistsError#from mc5d37e16631cc4d2|ActiveConversationExistsError.from]] — 호출 · L35
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.exception.ConversationException#activeConversationId mca4dcd3b0a983b42|ConversationException.activeConversationId]] — 호출 · L34,35
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.exception.ConversationException#code m57ace0236dcd9832|ConversationException.code]] — 호출 · L27,33
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.exception.ConversationException#fields m23707773febd9fb7|ConversationException.fields]] — 호출 · L33
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.error.ApiErrorResponses#body me905a471c60d2a7e|ApiErrorResponses.body]] — 호출 · L33

### 외부 라이브러리 호출

- `java.lang.Throwable#getMessage()`
- `org.springframework.http.ResponseEntity#status(org.springframework.http.HttpStatusCode)`
- `org.springframework.http.ResponseEntity.BodyBuilder#body(@org.jspecify.annotations.Nullable T)`

### 소스 코드

`src/main/java/com/dodamdodam/backend/conversation/api/ConversationExceptionHandler.java:25`

```java
@ExceptionHandler(ConversationException.class)
public ResponseEntity<?> handle(ConversationException exception, HttpServletResponse response) {
    var status = switch (exception.code()) {
        case "VALIDATION_FAILED" -> HttpStatus.BAD_REQUEST;
        case "SERVICE_HOURS_CLOSED", "CHILD_SESSION_REQUIRED" -> HttpStatus.FORBIDDEN;
        case "ACTIVE_CONVERSATION_EXISTS", "CONVERSATION_CLOSING", "CONVERSATION_ENDED" -> HttpStatus.CONFLICT;
        default -> HttpStatus.INTERNAL_SERVER_ERROR;
    };
    var error = ApiErrorResponses.body(response, exception.code(), exception.getMessage(), exception.fields());
    if (exception.activeConversationId() != null) {
        return ResponseEntity.status(status).body(ActiveConversationExistsError.from(error, exception.activeConversationId()));
    }
    return ResponseEntity.status(status).body(error);
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## 생성자 mfb2389ca6f55126f

컴파일러가 생성한 멤버다.

```java
public ConversationExceptionHandler()
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 외부 라이브러리 호출

- `java.lang.Object#<init>()`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]
