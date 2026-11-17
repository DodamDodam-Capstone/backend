---
tags: [backend, code-map, conversation]
aliases: ["Trigger"]
---
# Trigger

`com.dodamdodam.backend.conversation.application.ConversationClosureCoordinator.Trigger`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|시작]] → [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요]] → 이 클래스 · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/90-Reference|전체 참조 자료]]

## 클래스 요약

자동 관측과 명시적 종료 요청을 구분하며 해당 API나 worker 자체를 구현하지 않는다.

- 원본: `src/main/java/com/dodamdodam/backend/conversation/application/ConversationClosureCoordinator.java:73`
- 작성 메서드/생성자 0개 · 자동 생성 3개

## 메서드 목록

데이터·설정 타입이다. 아래 목록을 펼쳐 생성자와 접근자를 확인한다.

> [!example]- 생성자·자동 생성 멤버 3개
>
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationClosureCoordinator.Trigger#valueOf m46e5a7fe0d1ac4b3|public static ConversationClosureCoordinator.Trigger valueOf(String name) [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationClosureCoordinator.Trigger#values me5559d25cf136339|public static ConversationClosureCoordinator.Trigger[] values() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationClosureCoordinator.Trigger#생성자 m59859aa57709b30d|private Trigger() [자동]]]

## valueOf m46e5a7fe0d1ac4b3

컴파일러가 생성한 멤버다.

```java
public static com.dodamdodam.backend.conversation.application.ConversationClosureCoordinator.Trigger valueOf(java.lang.String name)
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## values me5559d25cf136339

컴파일러가 생성한 멤버다.

```java
public static com.dodamdodam.backend.conversation.application.ConversationClosureCoordinator.Trigger[] values()
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## 생성자 m59859aa57709b30d

컴파일러가 생성한 멤버다.

```java
private Trigger()
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationClosureCoordinator.Trigger|필드/정적 초기화]] — 생성 · L73

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 외부 라이브러리 호출

- `java.lang.Enum#<init>(java.lang.String,int)`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]
