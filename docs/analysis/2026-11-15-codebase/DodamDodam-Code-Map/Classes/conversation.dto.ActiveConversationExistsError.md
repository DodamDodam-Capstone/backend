---
tags: [backend, code-map, conversation]
aliases: ["ActiveConversationExistsError"]
---
# ActiveConversationExistsError

`com.dodamdodam.backend.conversation.dto.ActiveConversationExistsError`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|시작]] → [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요]] → 이 클래스 · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/90-Reference|전체 참조 자료]]

## 클래스 요약

일반 오류 스키마를 바꾸지 않고 ACTIVE 충돌에서만 기존 대화 ID를 추가한다.

- 원본: `src/main/java/com/dodamdodam/backend/conversation/dto/ActiveConversationExistsError.java:9`
- 작성 메서드/생성자 1개 · 자동 생성 5개

## 메서드 목록

| 메서드 | 하는 일 |
| --- | --- |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.dto.ActiveConversationExistsError#from mc5d37e16631cc4d2\|from]] | 공통 오류 작성기가 만든 메타데이터를 그대로 재사용한다. |

> [!example]- 생성자·자동 생성 멤버 5개
>
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.dto.ActiveConversationExistsError#equals m3ae1489dfbe7894b|public final boolean equals(Object o) [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.dto.ActiveConversationExistsError#error m5d1b6dd95206edb8|public ActiveConversationExistsError.ErrorDetail error() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.dto.ActiveConversationExistsError#hashCode m1dc29fdbe6bc2f55|public final int hashCode() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.dto.ActiveConversationExistsError#toString m09708bb5fe397212|public final String toString() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.dto.ActiveConversationExistsError#생성자 me74a34f15b2a14bb|public ActiveConversationExistsError(ActiveConversationExistsError.ErrorDetail error) [자동]]]

## from mc5d37e16631cc4d2

공통 오류 작성기가 만든 메타데이터를 그대로 재사용한다.

```java
public static com.dodamdodam.backend.conversation.dto.ActiveConversationExistsError from(com.dodamdodam.backend.global.error.ApiErrorResponse base, java.util.UUID conversationId)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.api.ConversationExceptionHandler#handle m50c7d1b4e6622616|ConversationExceptionHandler.handle]] — 호출 · L35

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.dto.ActiveConversationExistsError#생성자 me74a34f15b2a14bb|ActiveConversationExistsError.생성자]] — 생성 · L14
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.dto.ActiveConversationExistsError.ErrorDetail#생성자 mf61632a7ce3d5f44|ErrorDetail.생성자]] — 생성 · L14
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.error.ApiErrorResponse#error m1c5038d69ce04736|ApiErrorResponse.error]] — 호출 · L13
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.error.ApiErrorResponse.ErrorDetail#code m9fe9809cfceda8f6|ErrorDetail.code]] — 호출 · L14
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.error.ApiErrorResponse.ErrorDetail#fields m67e3857a2c5f73dd|ErrorDetail.fields]] — 호출 · L15
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.error.ApiErrorResponse.ErrorDetail#message m5e2f9074ffa013fc|ErrorDetail.message]] — 호출 · L14
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.error.ApiErrorResponse.ErrorDetail#requestId m33ee85fabce3c1ad|ErrorDetail.requestId]] — 호출 · L14

### 소스 코드

`src/main/java/com/dodamdodam/backend/conversation/dto/ActiveConversationExistsError.java:12`

```java
public static ActiveConversationExistsError from(ApiErrorResponse base, UUID conversationId) {
    var error = base.error();
    return new ActiveConversationExistsError(new ErrorDetail(error.code(), error.message(), error.requestId(),
            error.fields(), conversationId));
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## equals m3ae1489dfbe7894b

컴파일러가 생성한 멤버다.

```java
public final boolean equals(java.lang.Object o)
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## error m5d1b6dd95206edb8

컴파일러가 생성한 멤버다.

```java
public com.dodamdodam.backend.conversation.dto.ActiveConversationExistsError.ErrorDetail error()
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## hashCode m1dc29fdbe6bc2f55

컴파일러가 생성한 멤버다.

```java
public final int hashCode()
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## toString m09708bb5fe397212

컴파일러가 생성한 멤버다.

```java
public final java.lang.String toString()
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## 생성자 me74a34f15b2a14bb

컴파일러가 생성한 멤버다.

```java
public ActiveConversationExistsError(com.dodamdodam.backend.conversation.dto.ActiveConversationExistsError.ErrorDetail error)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.dto.ActiveConversationExistsError#from mc5d37e16631cc4d2|ActiveConversationExistsError.from]] — 생성 · L14

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 외부 라이브러리 호출

- `java.lang.Record#<init>()`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]
