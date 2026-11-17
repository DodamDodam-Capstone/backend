---
tags: [backend, code-map, conversation]
aliases: ["ConversationController"]
---
# ConversationController

`com.dodamdodam.backend.conversation.api.ConversationController`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|시작]] → [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요]] → 이 클래스 · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/90-Reference|전체 참조 자료]]

## 클래스 요약

대화 시작 요청을 받아 인증·입력·소유권을 검사하고 201 또는 200을 반환한다.

- 원본: `src/main/java/com/dodamdodam/backend/conversation/api/ConversationController.java:21`
- 작성 메서드/생성자 3개 · 자동 생성 0개

## 메서드 목록

| 메서드 | 하는 일 |
| --- | --- |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.api.ConversationController#start m255287826e69c022\|start]] | 신규 생성은 201과 Location, 같은 키 재전송은 기존 시작 정보만 200으로 반환한다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.api.ConversationController#prepareStart m1cac5f319ccf317a\|prepareStart]] | 인증→구조→소유권 순서로 검증한 내부 입력을 준비하며 성공 응답이나 ID를 만들지 않는다. |

> [!example]- 생성자·자동 생성 멤버 1개
>
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.api.ConversationController#생성자 mfe203737dbc8397e|public ConversationController(AuthenticatedSessionService authentication, ChildService children, ConversationService conversations)]]

## start m255287826e69c022

신규 생성은 201과 Location, 같은 키 재전송은 기존 시작 정보만 200으로 반환한다.

```java
public org.springframework.http.ResponseEntity<java.util.Map<java.lang.String,com.dodamdodam.backend.conversation.dto.StartConversationResult>> start(jakarta.servlet.http.HttpServletRequest request, java.lang.String childId, tools.jackson.databind.JsonNode body)
```

설정: `@org.springframework.web.bind.annotation.PostMapping({"/api/v1/children/{childId}/conversations"})`

### 이 메서드를 호출하는 곳

- 프레임워크 진입: POST /api/v1/children/{childId}/conversations

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.api.ConversationController#prepareStart m1cac5f319ccf317a|ConversationController.prepareStart]] — 호출 · L40
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.api.ConversationController.PreparedStart#childId m4f9531562bffc78c|PreparedStart.childId]] — 호출 · L41,42,48
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.api.ConversationController.PreparedStart#input m0c4ce954d6a68627|PreparedStart.input]] — 호출 · L41,43
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.api.ConversationController.PreparedStart#session m2bf96516049290bb|PreparedStart.session]] — 호출 · L41,42
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.api.ConversationStartResponseFilter#protect m24d9b8b621ce0746|ConversationStartResponseFilter.protect]] — 호출 · L42
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationService#create mf9ff06c04e6d7fad|ConversationService.create]] — 호출 · L41
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationService.StartOutcome#created md3ee62a0e82cc20a|StartOutcome.created]] — 호출 · L45
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationService.StartOutcome#result m4df068918fe96704|StartOutcome.result]] — 호출 · L43,44,48
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.dto.StartConversationRequest#clientRequestId m6d3b72a72eb6f686|StartConversationRequest.clientRequestId]] — 호출 · L43
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.dto.StartConversationResult#conversationId m3dca6e4445701a55|StartConversationResult.conversationId]] — 호출 · L43,48

### 외부 라이브러리 호출

- `java.net.URI#create(java.lang.String)`
- `java.util.Map#of(K,V)`
- `org.springframework.http.ResponseEntity#created(java.net.URI)`
- `org.springframework.http.ResponseEntity#ok(@org.jspecify.annotations.Nullable T)`
- `org.springframework.http.ResponseEntity.BodyBuilder#body(@org.jspecify.annotations.Nullable T)`

### 소스 코드

`src/main/java/com/dodamdodam/backend/conversation/api/ConversationController.java:37`

```java
@PostMapping("/api/v1/children/{childId}/conversations")
public ResponseEntity<Map<String, StartConversationResult>> start(HttpServletRequest request,
        @PathVariable String childId, @RequestBody JsonNode body) {
    var prepared = prepareStart(request, childId, body);
    var outcome = conversations.create(prepared.session(), prepared.childId(), prepared.input());
    ConversationStartResponseFilter.protect(request, prepared.session(), prepared.childId(),
            prepared.input().clientRequestId(), outcome.result().conversationId());
    var bodyData = Map.of("data", outcome.result());
    if (!outcome.created()) {
        return ResponseEntity.ok(bodyData);
    }
    var location = URI.create("/api/v1/children/" + prepared.childId() + "/conversations/" + outcome.result().conversationId());
    return ResponseEntity.created(location).body(bodyData);
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## prepareStart m1cac5f319ccf317a

인증→구조→소유권 순서로 검증한 내부 입력을 준비하며 성공 응답이나 ID를 만들지 않는다.

```java
public com.dodamdodam.backend.conversation.api.ConversationController.PreparedStart prepareStart(jakarta.servlet.http.HttpServletRequest request, java.lang.String childId, tools.jackson.databind.JsonNode body)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.api.ConversationController#start m255287826e69c022|ConversationController.start]] — 호출 · L40

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthenticatedSession#accountId m23b42fff4f806999|AuthenticatedSession.accountId]] — 호출 · L57
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthenticatedSessionService#requireAuthenticatedSession mad23075b25d06a28|AuthenticatedSessionService.requireAuthenticatedSession]] — 호출 · L54
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.application.ChildService#requireChildAccess mbb6af419b1ceab47|ChildService.requireChildAccess]] — 호출 · L57
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.dto.ChildView#childId m7990d88dbffa4abf|ChildView.childId]] — 호출 · L59
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.api.ConversationController.PreparedStart#생성자 m693a465c6c6f7b56|PreparedStart.생성자]] — 생성 · L59
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.dto.StartConversationRequest#fromJson m21dba4fe8be80ffe|StartConversationRequest.fromJson]] — 호출 · L56
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.dto.StartConversationRequest#parseChildId mda966a22df85e293|StartConversationRequest.parseChildId]] — 호출 · L55

### 소스 코드

`src/main/java/com/dodamdodam/backend/conversation/api/ConversationController.java:53`

```java
public PreparedStart prepareStart(HttpServletRequest request, String childId, JsonNode body) {
    var current = authentication.requireAuthenticatedSession(request);
    var parsedChildId = StartConversationRequest.parseChildId(childId);
    var input = StartConversationRequest.fromJson(body);
    var child = children.requireChildAccess(current.accountId(), parsedChildId);
    // CSRF는 기존 Security 필터가 처리하며, 최종 시간·문맥 확인은 생성 트랜잭션에 남긴다.
    return new PreparedStart(current, child.childId(), input);
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## 생성자 mfe203737dbc8397e

기존 로그인·자녀 소유권 정책을 재사용한다.

```java
public ConversationController(com.dodamdodam.backend.auth.session.AuthenticatedSessionService authentication, com.dodamdodam.backend.child.application.ChildService children, com.dodamdodam.backend.conversation.application.ConversationService conversations)
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 외부 라이브러리 호출

- `java.lang.Object#<init>()`

### 소스 코드

`src/main/java/com/dodamdodam/backend/conversation/api/ConversationController.java:29`

```java
public ConversationController(AuthenticatedSessionService authentication, ChildService children,
                              ConversationService conversations) {
    this.authentication = authentication;
    this.children = children;
    this.conversations = conversations;
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]
