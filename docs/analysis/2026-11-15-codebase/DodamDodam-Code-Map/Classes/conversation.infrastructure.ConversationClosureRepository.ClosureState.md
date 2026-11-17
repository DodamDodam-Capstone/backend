---
tags: [backend, code-map, conversation]
aliases: ["ClosureState"]
---
# ClosureState

`com.dodamdodam.backend.conversation.infrastructure.ConversationClosureRepository.ClosureState`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|시작]] → [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요]] → 이 클래스 · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/90-Reference|전체 참조 자료]]

## 클래스 요약

종료 확인 API가 재사용할 저장 상태이며 아이 내용·발화는 포함하지 않는다.

- 원본: `src/main/java/com/dodamdodam/backend/conversation/infrastructure/ConversationClosureRepository.java:171`
- 작성 메서드/생성자 0개 · 자동 생성 11개

## 메서드 목록

데이터·설정 타입이다. 아래 목록을 펼쳐 생성자와 접근자를 확인한다.

> [!example]- 생성자·자동 생성 멤버 11개
>
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.infrastructure.ConversationClosureRepository.ClosureState#conversationId me70651f6b9e44a95|public UUID conversationId() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.infrastructure.ConversationClosureRepository.ClosureState#endReason m0b93815de2a8615e|public String endReason() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.infrastructure.ConversationClosureRepository.ClosureState#endRequestedAt m68ff0976ba9e6035|public OffsetDateTime endRequestedAt() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.infrastructure.ConversationClosureRepository.ClosureState#endedAt m75038da0d47207f9|public OffsetDateTime endedAt() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.infrastructure.ConversationClosureRepository.ClosureState#equals mff22de9f7a7117bf|public final boolean equals(Object o) [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.infrastructure.ConversationClosureRepository.ClosureState#hashCode m4b8191f2f3792b65|public final int hashCode() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.infrastructure.ConversationClosureRepository.ClosureState#scheduledEndAt m0301ef381d8056d8|public OffsetDateTime scheduledEndAt() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.infrastructure.ConversationClosureRepository.ClosureState#status mc61770c44b1d7c22|public String status() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.infrastructure.ConversationClosureRepository.ClosureState#summaryStatus m624bb2ce8836610c|public String summaryStatus() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.infrastructure.ConversationClosureRepository.ClosureState#toString m49fd2b1ce5192e61|public final String toString() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.infrastructure.ConversationClosureRepository.ClosureState#생성자 m5699b272bd502772|public ClosureState(UUID conversationId, String status, OffsetDateTime scheduledEndAt, OffsetDateTime endRequestedAt, String endReason, OffsetDateTime endedAt, String summaryStatus) [자동]]]

## conversationId me70651f6b9e44a95

컴파일러가 생성한 멤버다.

```java
public java.util.UUID conversationId()
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## endReason m0b93815de2a8615e

컴파일러가 생성한 멤버다.

```java
public java.lang.String endReason()
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## endRequestedAt m68ff0976ba9e6035

컴파일러가 생성한 멤버다.

```java
public java.time.OffsetDateTime endRequestedAt()
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationClosureCoordinator#reconcile m5c7d3b9b2dd0cb88|ConversationClosureCoordinator.reconcile]] — 호출 · L63

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## endedAt m75038da0d47207f9

컴파일러가 생성한 멤버다.

```java
public java.time.OffsetDateTime endedAt()
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## equals mff22de9f7a7117bf

컴파일러가 생성한 멤버다.

```java
public final boolean equals(java.lang.Object o)
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## hashCode m4b8191f2f3792b65

컴파일러가 생성한 멤버다.

```java
public final int hashCode()
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## scheduledEndAt m0301ef381d8056d8

컴파일러가 생성한 멤버다.

```java
public java.time.OffsetDateTime scheduledEndAt()
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationClosureCoordinator#reconcile m5c7d3b9b2dd0cb88|ConversationClosureCoordinator.reconcile]] — 호출 · L46,50

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## status mc61770c44b1d7c22

컴파일러가 생성한 멤버다.

```java
public java.lang.String status()
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationClosureCoordinator#reconcile m5c7d3b9b2dd0cb88|ConversationClosureCoordinator.reconcile]] — 호출 · L42,45

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## summaryStatus m624bb2ce8836610c

컴파일러가 생성한 멤버다.

```java
public java.lang.String summaryStatus()
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## toString m49fd2b1ce5192e61

컴파일러가 생성한 멤버다.

```java
public final java.lang.String toString()
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## 생성자 m5699b272bd502772

컴파일러가 생성한 멤버다.

```java
public ClosureState(java.util.UUID conversationId, java.lang.String status, java.time.OffsetDateTime scheduledEndAt, java.time.OffsetDateTime endRequestedAt, java.lang.String endReason, java.time.OffsetDateTime endedAt, java.lang.String summaryStatus)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.infrastructure.ConversationClosureRepository#findForUpdate m31b37bc252579185|ConversationClosureRepository.findForUpdate]] — 생성 · L40

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 외부 라이브러리 호출

- `java.lang.Record#<init>()`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]
