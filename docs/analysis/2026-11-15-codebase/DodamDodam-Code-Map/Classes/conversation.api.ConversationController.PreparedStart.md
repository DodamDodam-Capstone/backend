---
tags: [backend, code-map, conversation]
aliases: ["PreparedStart"]
---
# PreparedStart

`com.dodamdodam.backend.conversation.api.ConversationController.PreparedStart`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|시작]] → [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요]] → 이 클래스 · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/90-Reference|전체 참조 자료]]

## 클래스 요약

생성 서비스에 전달할 내부 문맥이며 공개 API 응답으로 직렬화하지 않는다.

- 원본: `src/main/java/com/dodamdodam/backend/conversation/api/ConversationController.java:63`
- 작성 메서드/생성자 0개 · 자동 생성 7개

## 메서드 목록

데이터·설정 타입이다. 아래 목록을 펼쳐 생성자와 접근자를 확인한다.

> [!example]- 생성자·자동 생성 멤버 7개
>
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.api.ConversationController.PreparedStart#childId m4f9531562bffc78c|public UUID childId() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.api.ConversationController.PreparedStart#equals m4fa239f228ea4c7b|public final boolean equals(Object o) [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.api.ConversationController.PreparedStart#hashCode m42ab27764a86e4e3|public final int hashCode() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.api.ConversationController.PreparedStart#input m0c4ce954d6a68627|public StartConversationRequest input() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.api.ConversationController.PreparedStart#session m2bf96516049290bb|public AuthenticatedSession session() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.api.ConversationController.PreparedStart#toString m8ddf7dfec82fddc1|public final String toString() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.api.ConversationController.PreparedStart#생성자 m693a465c6c6f7b56|public PreparedStart(AuthenticatedSession session, UUID childId, StartConversationRequest input) [자동]]]

## childId m4f9531562bffc78c

컴파일러가 생성한 멤버다.

```java
public java.util.UUID childId()
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.api.ConversationController#start m255287826e69c022|ConversationController.start]] — 호출 · L41,42,48

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## equals m4fa239f228ea4c7b

컴파일러가 생성한 멤버다.

```java
public final boolean equals(java.lang.Object o)
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## hashCode m42ab27764a86e4e3

컴파일러가 생성한 멤버다.

```java
public final int hashCode()
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## input m0c4ce954d6a68627

컴파일러가 생성한 멤버다.

```java
public com.dodamdodam.backend.conversation.dto.StartConversationRequest input()
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.api.ConversationController#start m255287826e69c022|ConversationController.start]] — 호출 · L41,43

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## session m2bf96516049290bb

컴파일러가 생성한 멤버다.

```java
public com.dodamdodam.backend.auth.session.AuthenticatedSession session()
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.api.ConversationController#start m255287826e69c022|ConversationController.start]] — 호출 · L41,42

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## toString m8ddf7dfec82fddc1

컴파일러가 생성한 멤버다.

```java
public final java.lang.String toString()
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## 생성자 m693a465c6c6f7b56

컴파일러가 생성한 멤버다.

```java
public PreparedStart(com.dodamdodam.backend.auth.session.AuthenticatedSession session, java.util.UUID childId, com.dodamdodam.backend.conversation.dto.StartConversationRequest input)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.api.ConversationController#prepareStart m1cac5f319ccf317a|ConversationController.prepareStart]] — 생성 · L59

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 외부 라이브러리 호출

- `java.lang.Record#<init>()`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]
