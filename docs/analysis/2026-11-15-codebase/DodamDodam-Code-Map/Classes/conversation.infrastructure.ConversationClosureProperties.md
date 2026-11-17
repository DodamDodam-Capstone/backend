---
tags: [backend, code-map, conversation]
aliases: ["ConversationClosureProperties"]
---
# ConversationClosureProperties

`com.dodamdodam.backend.conversation.infrastructure.ConversationClosureProperties`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|시작]] → [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요]] → 이 클래스 · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/90-Reference|전체 참조 자료]]

## 클래스 요약

미확정 요약 처리 기한은 운영 기본값 없이 명시적으로 주입한다.

- 원본: `src/main/java/com/dodamdodam/backend/conversation/infrastructure/ConversationClosureProperties.java:8`
- 작성 메서드/생성자 1개 · 자동 생성 4개

## 메서드 목록

데이터·설정 타입이다. 아래 목록을 펼쳐 생성자와 접근자를 확인한다.

> [!example]- 생성자·자동 생성 멤버 5개
>
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.infrastructure.ConversationClosureProperties#생성자 m32fbd417e7771064|public ConversationClosureProperties(Duration summaryProcessingTimeout)]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.infrastructure.ConversationClosureProperties#equals m4c0ea75fdeb3294d|public final boolean equals(Object o) [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.infrastructure.ConversationClosureProperties#hashCode m33792d7b918d521d|public final int hashCode() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.infrastructure.ConversationClosureProperties#summaryProcessingTimeout mffb02d95482b61bd|public Duration summaryProcessingTimeout() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.infrastructure.ConversationClosureProperties#toString me02c2d934484538f|public final String toString() [자동]]]

## 생성자 m32fbd417e7771064

NULL·0·음수와 PostgreSQL 마이크로초보다 세밀한 정밀도를 거부한다.

```java
public ConversationClosureProperties(java.time.Duration summaryProcessingTimeout)
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 외부 라이브러리 호출

- `java.lang.IllegalArgumentException#<init>(java.lang.String)`
- `java.lang.Record#<init>()`
- `java.time.Duration#getNano()`
- `java.time.Duration#isNegative()`
- `java.time.Duration#isZero()`
- `java.util.Objects#requireNonNull(T,java.lang.String)`

### 소스 코드

`src/main/java/com/dodamdodam/backend/conversation/infrastructure/ConversationClosureProperties.java:12`

```java
public ConversationClosureProperties {
    Objects.requireNonNull(summaryProcessingTimeout, "요약 처리 기한을 명시적으로 설정해야 합니다.");
    if (summaryProcessingTimeout.isZero() || summaryProcessingTimeout.isNegative()
            || summaryProcessingTimeout.getNano() % 1_000 != 0) {
        throw new IllegalArgumentException("요약 처리 기한은 양수이며 PostgreSQL 마이크로초 정밀도여야 합니다.");
    }
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## equals m4c0ea75fdeb3294d

컴파일러가 생성한 멤버다.

```java
public final boolean equals(java.lang.Object o)
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## hashCode m33792d7b918d521d

컴파일러가 생성한 멤버다.

```java
public final int hashCode()
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## summaryProcessingTimeout mffb02d95482b61bd

컴파일러가 생성한 멤버다.

```java
public java.time.Duration summaryProcessingTimeout()
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationClosureCoordinator#reconcile m5c7d3b9b2dd0cb88|ConversationClosureCoordinator.reconcile]] — 호출 · L61

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## toString me02c2d934484538f

컴파일러가 생성한 멤버다.

```java
public final java.lang.String toString()
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]
