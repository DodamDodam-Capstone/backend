---
tags: [backend, code-map, conversation]
aliases: ["StartOutcome"]
---
# StartOutcome

`com.dodamdodam.backend.conversation.application.ConversationService.StartOutcome`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|시작]] → [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요]] → 이 클래스 · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/90-Reference|전체 참조 자료]]

## 클래스 요약

HTTP 상태 선택용 내부 결과이며 created 표시는 공개 data에 포함하지 않는다.

- 원본: `src/main/java/com/dodamdodam/backend/conversation/application/ConversationService.java:163`
- 작성 메서드/생성자 0개 · 자동 생성 6개

## 메서드 목록

데이터·설정 타입이다. 아래 목록을 펼쳐 생성자와 접근자를 확인한다.

> [!example]- 생성자·자동 생성 멤버 6개
>
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationService.StartOutcome#created md3ee62a0e82cc20a|public boolean created() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationService.StartOutcome#equals mecff6ce178097af8|public final boolean equals(Object o) [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationService.StartOutcome#hashCode m3ee0b2ae4954744e|public final int hashCode() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationService.StartOutcome#result m4df068918fe96704|public StartConversationResult result() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationService.StartOutcome#toString mae21dec058c61781|public final String toString() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationService.StartOutcome#생성자 mb897e2535fd3e442|public StartOutcome(StartConversationResult result, boolean created) [자동]]]

## created md3ee62a0e82cc20a

컴파일러가 생성한 멤버다.

```java
public boolean created()
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.api.ConversationController#start m255287826e69c022|ConversationController.start]] — 호출 · L45

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## equals mecff6ce178097af8

컴파일러가 생성한 멤버다.

```java
public final boolean equals(java.lang.Object o)
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## hashCode m3ee0b2ae4954744e

컴파일러가 생성한 멤버다.

```java
public final int hashCode()
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## result m4df068918fe96704

컴파일러가 생성한 멤버다.

```java
public com.dodamdodam.backend.conversation.dto.StartConversationResult result()
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.api.ConversationController#start m255287826e69c022|ConversationController.start]] — 호출 · L43,44,48

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## toString mae21dec058c61781

컴파일러가 생성한 멤버다.

```java
public final java.lang.String toString()
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## 생성자 mb897e2535fd3e442

컴파일러가 생성한 멤버다.

```java
public StartOutcome(com.dodamdodam.backend.conversation.dto.StartConversationResult result, boolean created)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationService#decideStart m074dea56e9276dfa|ConversationService.decideStart]] — 생성 · L131
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationService#replay m8a9b848e75e75181|ConversationService.replay]] — 생성 · L149

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 외부 라이브러리 호출

- `java.lang.Record#<init>()`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]
