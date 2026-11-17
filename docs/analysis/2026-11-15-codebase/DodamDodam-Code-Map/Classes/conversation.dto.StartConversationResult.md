---
tags: [backend, code-map, conversation]
aliases: ["StartConversationResult"]
---
# StartConversationResult

`com.dodamdodam.backend.conversation.dto.StartConversationResult`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|시작]] → [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요]] → 이 클래스 · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/90-Reference|전체 참조 자료]]

## 클래스 요약

저장된 시작 정보만 공개하며, 재전송 시 새 시각이나 대화 ID를 만들지 않는다.

- 원본: `src/main/java/com/dodamdodam/backend/conversation/dto/StartConversationResult.java:11`
- 작성 메서드/생성자 1개 · 자동 생성 8개

## 메서드 목록

데이터·설정 타입이다. 아래 목록을 펼쳐 생성자와 접근자를 확인한다.

> [!example]- 생성자·자동 생성 멤버 9개
>
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.dto.StartConversationResult#생성자 m9ffc5c7b78e8b45c|public StartConversationResult(UUID conversationId, String status, OffsetDateTime startedAt, LocalDate serviceDate, OffsetDateTime scheduledEndAt)]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.dto.StartConversationResult#conversationId m3dca6e4445701a55|public UUID conversationId() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.dto.StartConversationResult#equals mf6f1e4dc1f88a1f5|public final boolean equals(Object o) [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.dto.StartConversationResult#hashCode me85a845bab646d96|public final int hashCode() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.dto.StartConversationResult#scheduledEndAt m7d0d409a6111ea30|public OffsetDateTime scheduledEndAt() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.dto.StartConversationResult#serviceDate m39badc08c1c7a12f|public LocalDate serviceDate() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.dto.StartConversationResult#startedAt mb8b08b0216004eb7|public OffsetDateTime startedAt() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.dto.StartConversationResult#status mae605b06f4f17f1e|public String status() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.dto.StartConversationResult#toString maba03611d22eeddd|public final String toString() [자동]]]

## 생성자 m9ffc5c7b78e8b45c

필수 공개 필드를 확인하고 같은 순간을 KST 초 정밀도로 표현한다.

```java
public StartConversationResult(java.util.UUID conversationId, java.lang.String status, java.time.OffsetDateTime startedAt, java.time.LocalDate serviceDate, java.time.OffsetDateTime scheduledEndAt)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.infrastructure.ConversationRepository#insertActive mff8e74c752863f8c|ConversationRepository.insertActive]] — 생성 · L92
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.infrastructure.ConversationRepository.StoredConversation#startResult m2a1a09c7685f09a4|StoredConversation.startResult]] — 생성 · L122

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 외부 라이브러리 호출

- `java.lang.IllegalArgumentException#<init>(java.lang.String)`
- `java.lang.Record#<init>()`
- `java.lang.String#equals(java.lang.Object)`
- `java.time.OffsetDateTime#truncatedTo(java.time.temporal.TemporalUnit)`
- `java.time.OffsetDateTime#withOffsetSameInstant(java.time.ZoneOffset)`
- `java.time.ZoneOffset#ofHours(int)`
- `java.util.Objects#requireNonNull(T,java.lang.String)`

### 소스 코드

`src/main/java/com/dodamdodam/backend/conversation/dto/StartConversationResult.java:15`

```java
public StartConversationResult {
    Objects.requireNonNull(conversationId, "conversationId");
    Objects.requireNonNull(serviceDate, "serviceDate");
    if (!"ACTIVE".equals(status)) {
        throw new IllegalArgumentException("시작 응답의 상태는 ACTIVE여야 합니다.");
    }
    // 수동으로 9시간을 더하지 않고 저장 시각의 오프셋만 변환한다.
    startedAt = Objects.requireNonNull(startedAt, "startedAt")
            .withOffsetSameInstant(ZoneOffset.ofHours(9)).truncatedTo(ChronoUnit.SECONDS);
    scheduledEndAt = Objects.requireNonNull(scheduledEndAt, "scheduledEndAt")
            .withOffsetSameInstant(ZoneOffset.ofHours(9)).truncatedTo(ChronoUnit.SECONDS);
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## conversationId m3dca6e4445701a55

컴파일러가 생성한 멤버다.

```java
public java.util.UUID conversationId()
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.api.ConversationController#start m255287826e69c022|ConversationController.start]] — 호출 · L43,48
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationService#decideStart m074dea56e9276dfa|ConversationService.decideStart]] — 호출 · L125

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## equals mf6f1e4dc1f88a1f5

컴파일러가 생성한 멤버다.

```java
public final boolean equals(java.lang.Object o)
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## hashCode me85a845bab646d96

컴파일러가 생성한 멤버다.

```java
public final int hashCode()
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## scheduledEndAt m7d0d409a6111ea30

컴파일러가 생성한 멤버다.

```java
public java.time.OffsetDateTime scheduledEndAt()
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## serviceDate m39badc08c1c7a12f

컴파일러가 생성한 멤버다.

```java
public java.time.LocalDate serviceDate()
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## startedAt mb8b08b0216004eb7

컴파일러가 생성한 멤버다.

```java
public java.time.OffsetDateTime startedAt()
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## status mae605b06f4f17f1e

컴파일러가 생성한 멤버다.

```java
public java.lang.String status()
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## toString maba03611d22eeddd

컴파일러가 생성한 멤버다.

```java
public final java.lang.String toString()
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]
