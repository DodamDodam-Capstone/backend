---
tags: [backend, code-map, conversation]
aliases: ["ResponseRejected"]
---
# ResponseRejected

`com.dodamdodam.backend.conversation.api.ConversationStartResponseFilter.ResponseRejected`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|시작]] → [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요]] → 이 클래스 · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/90-Reference|전체 참조 자료]]

## 클래스 요약

업무 예외를 MVC가 먼저 출력하지 못하도록 원인을 별도 보관하는 내부 전송 신호다.

- 원본: `src/main/java/com/dodamdodam/backend/conversation/api/ConversationStartResponseFilter.java:170`
- 작성 메서드/생성자 1개 · 자동 생성 0개

## 메서드 목록

데이터·설정 타입이다. 아래 목록을 펼쳐 생성자와 접근자를 확인한다.

> [!example]- 생성자·자동 생성 멤버 1개
>
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.api.ConversationStartResponseFilter.ResponseRejected#생성자 m6e5c89886a9991f7|private ResponseRejected(RuntimeException rejection)]]

## 생성자 m6e5c89886a9991f7

고정 메시지만 가지며 실제 오류 변환은 성공 응답 초기화 뒤 수행한다.

```java
private ResponseRejected(java.lang.RuntimeException rejection)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.api.ConversationStartResponseFilter#verify mbaf83885c918b297|ConversationStartResponseFilter.verify]] — 생성 · L137

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 외부 라이브러리 호출

- `java.lang.RuntimeException#<init>(java.lang.String,java.lang.Throwable,boolean,boolean)`

### 소스 코드

`src/main/java/com/dodamdodam/backend/conversation/api/ConversationStartResponseFilter.java:174`

```java
private ResponseRejected(RuntimeException rejection) {
    super("대화 시작 응답을 다시 검증해야 합니다.", null, false, false);
    this.rejection = rejection;
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]
