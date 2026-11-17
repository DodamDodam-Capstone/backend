---
tags: [backend, code-map, conversation]
aliases: ["익명 클래스@86"]
---
# 익명 클래스@86

`com.dodamdodam.backend.conversation.api.ConversationStartResponseFilter$anonymous@86`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|시작]] → [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요]] → 이 클래스 · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/90-Reference|전체 참조 자료]]

## 클래스 요약

MVC 직렬화·flush가 시작해도 거부된 성공 바이트를 그대로 내보내지 않는다.

- 원본: `src/main/java/com/dodamdodam/backend/conversation/api/ConversationStartResponseFilter.java:86`
- 작성 메서드/생성자 1개 · 자동 생성 1개

## 메서드 목록

| 메서드 | 하는 일 |
| --- | --- |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.api.ConversationStartResponseFilter_anonymous_86#onResponseCommitted m22eb7ff8f3884b7b\|onResponseCommitted]] | MVC 직렬화·flush가 시작해도 거부된 성공 바이트를 그대로 내보내지 않는다. |

> [!example]- 생성자·자동 생성 멤버 1개
>
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.api.ConversationStartResponseFilter_anonymous_86#생성자 mc79168b20210d2f2|익명 클래스 [자동]]]

## onResponseCommitted m22eb7ff8f3884b7b

MVC 직렬화·flush가 시작해도 거부된 성공 바이트를 그대로 내보내지 않는다.

```java
protected void onResponseCommitted()
```

설정: `@java.lang.Override`

### 이 메서드를 호출하는 곳

- 프레임워크 진입: 응답 commit 콜백

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.api.ConversationStartResponseFilter#verify mbaf83885c918b297|ConversationStartResponseFilter.verify]] — 호출 · L91

### 외부 라이브러리 호출

- `org.springframework.security.web.util.OnCommittedResponseWrapper#disableOnResponseCommitted()`

### 소스 코드

`src/main/java/com/dodamdodam/backend/conversation/api/ConversationStartResponseFilter.java:88`

```java
@Override
protected void onResponseCommitted() {
    disableOnResponseCommitted();
    verify(request, response);
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## 생성자 mc79168b20210d2f2

컴파일러가 생성한 멤버다.

```java
익명 클래스@86(jakarta.servlet.http.HttpServletResponse arg0)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.api.ConversationStartResponseFilter#doFilterInternal m2e5a11b062f28145|ConversationStartResponseFilter.doFilterInternal]] — 생성 · L86

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 외부 라이브러리 호출

- `org.springframework.security.web.util.OnCommittedResponseWrapper#<init>(jakarta.servlet.http.HttpServletResponse)`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]
