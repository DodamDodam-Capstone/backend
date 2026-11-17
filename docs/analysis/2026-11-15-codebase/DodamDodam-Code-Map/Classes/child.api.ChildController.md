---
tags: [backend, code-map, child]
aliases: ["ChildController"]
---
# ChildController

`com.dodamdodam.backend.child.api.ChildController`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|시작]] → [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/child|기능 개요]] → 이 클래스 · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/90-Reference|전체 참조 자료]]

## 클래스 요약

아이 등록 HTTP 진입점. 세션의 계정으로 등록하고 201 응답을 만든다.

- 원본: `src/main/java/com/dodamdodam/backend/child/api/ChildController.java:22`
- 작성 메서드/생성자 3개 · 자동 생성 0개

## 메서드 목록

| 메서드 | 하는 일 |
| --- | --- |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.api.ChildController#create m85a3864c9b94be48\|create]] | 로그인 계정에 자녀를 등록하고 공개 프로필과 식별 URI를 반환한다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.api.ChildController#invalidJson m85e9a4840a5175ad\|invalidJson]] | 원문을 노출하지 않고 이 컨트롤러의 JSON 읽기 오류만 400으로 처리한다. |

> [!example]- 생성자·자동 생성 멤버 1개
>
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.api.ChildController#생성자 m4127e44b19ab6078|public ChildController(AuthenticatedSessionService authentication, ChildService children)]]

## create m85a3864c9b94be48

로그인 계정에 자녀를 등록하고 공개 프로필과 식별 URI를 반환한다.

```java
public org.springframework.http.ResponseEntity<java.util.Map<java.lang.String,com.dodamdodam.backend.child.dto.ChildView>> create(jakarta.servlet.http.HttpServletRequest request, tools.jackson.databind.JsonNode body)
```

설정: `@org.springframework.web.bind.annotation.PostMapping({"/api/v1/children"})`

### 이 메서드를 호출하는 곳

- 프레임워크 진입: POST /api/v1/children

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthenticatedSessionService#requireAuthenticatedSession mad23075b25d06a28|AuthenticatedSessionService.requireAuthenticatedSession]] — 호출 · L38
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.application.ChildService#create m022c1fb522a0efea|ChildService.create]] — 호출 · L39
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.dto.ChildView#childId m7990d88dbffa4abf|ChildView.childId]] — 호출 · L41
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.dto.CreateChildRequest#fromJson mf8c618d868acf95b|CreateChildRequest.fromJson]] — 호출 · L39

### 외부 라이브러리 호출

- `java.net.URI#create(java.lang.String)`
- `java.util.Map#of(K,V)`
- `org.springframework.http.ResponseEntity#created(java.net.URI)`
- `org.springframework.http.ResponseEntity.BodyBuilder#body(@org.jspecify.annotations.Nullable T)`

### 소스 코드

`src/main/java/com/dodamdodam/backend/child/api/ChildController.java:35`

```java
@PostMapping("/api/v1/children")
public ResponseEntity<Map<String, ChildView>> create(HttpServletRequest request, @RequestBody JsonNode body) {
    // accountId는 본문 대신 검증한 세션에서만 가져온다.
    var current = authentication.requireAuthenticatedSession(request);
    var child = children.create(current, CreateChildRequest.fromJson(body));
    // Location은 리소스 식별자이며 단건 GET 경로의 추가를 뜻하지 않는다.
    return ResponseEntity.created(URI.create("/api/v1/children/" + child.childId())).body(Map.of("data", child));
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/child|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## invalidJson m85e9a4840a5175ad

원문을 노출하지 않고 이 컨트롤러의 JSON 읽기 오류만 400으로 처리한다.

```java
public org.springframework.http.ResponseEntity<com.dodamdodam.backend.global.error.ApiErrorResponse> invalidJson(jakarta.servlet.http.HttpServletResponse response)
```

설정: `@org.springframework.web.bind.annotation.ExceptionHandler({org.springframework.http.converter.HttpMessageNotReadableException.class})`

### 이 메서드를 호출하는 곳

- 프레임워크 진입: Spring MVC 예외 처리

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.exception.ChildException#code m9503281b2a5259d9|ChildException.code]] — 호출 · L49
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.exception.ChildException#fields m5ca1eb5c4e2bd206|ChildException.fields]] — 호출 · L49
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.exception.ChildException#validation m04bce91b4a5ea457|ChildException.validation]] — 호출 · L47
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.error.ApiErrorResponses#body me905a471c60d2a7e|ApiErrorResponses.body]] — 호출 · L48

### 외부 라이브러리 호출

- `java.lang.Throwable#getMessage()`
- `org.springframework.http.ResponseEntity#badRequest()`
- `org.springframework.http.ResponseEntity.BodyBuilder#body(@org.jspecify.annotations.Nullable T)`

### 소스 코드

`src/main/java/com/dodamdodam/backend/child/api/ChildController.java:45`

```java
@ExceptionHandler(HttpMessageNotReadableException.class)
public ResponseEntity<ApiErrorResponse> invalidJson(HttpServletResponse response) {
    var invalid = ChildException.validation("body");
    return ResponseEntity.badRequest().body(ApiErrorResponses.body(
            response, invalid.code(), invalid.getMessage(), invalid.fields()));
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/child|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## 생성자 m4127e44b19ab6078

기존 세션 검증과 자녀 등록 서비스를 연결한다.

```java
public ChildController(com.dodamdodam.backend.auth.session.AuthenticatedSessionService authentication, com.dodamdodam.backend.child.application.ChildService children)
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 외부 라이브러리 호출

- `java.lang.Object#<init>()`

### 소스 코드

`src/main/java/com/dodamdodam/backend/child/api/ChildController.java:29`

```java
public ChildController(AuthenticatedSessionService authentication, ChildService children) {
    this.authentication = authentication;
    this.children = children;
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/child|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]
