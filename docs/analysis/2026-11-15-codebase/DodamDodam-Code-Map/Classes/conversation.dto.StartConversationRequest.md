---
tags: [backend, code-map, conversation]
aliases: ["StartConversationRequest"]
---
# StartConversationRequest

`com.dodamdodam.backend.conversation.dto.StartConversationRequest`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|시작]] → [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요]] → 이 클래스 · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/90-Reference|전체 참조 자료]]

## 클래스 요약

값을 전달하는 데이터 타입이다.

- 원본: `src/main/java/com/dodamdodam/backend/conversation/dto/StartConversationRequest.java:9`
- 작성 메서드/생성자 4개 · 자동 생성 4개

## 메서드 목록

| 메서드 | 하는 일 |
| --- | --- |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.dto.StartConversationRequest#fromJson m21dba4fe8be80ffe\|fromJson]] | 추가 필드·타입 강제 변환 없이 명세의 단일 UUID 문자열을 읽는다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.dto.StartConversationRequest#parseChildId mda966a22df85e293\|parseChildId]] | 경로의 자녀 UUID에도 본문과 같은 완전한 문자열 형식을 적용한다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.dto.StartConversationRequest#parseUuid m224916bbb94e5c51\|parseUuid]] | UUID.fromString이 허용하는 축약형과 공백·개행을 먼저 거부한다. |

> [!example]- 생성자·자동 생성 멤버 5개
>
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.dto.StartConversationRequest#생성자 maec59ec0f4972568|public StartConversationRequest(UUID clientRequestId)]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.dto.StartConversationRequest#clientRequestId m6d3b72a72eb6f686|public UUID clientRequestId() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.dto.StartConversationRequest#equals m202151c275b31620|public final boolean equals(Object o) [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.dto.StartConversationRequest#hashCode m48c19d2af594caca|public final int hashCode() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.dto.StartConversationRequest#toString m6e15de78a5df87ce|public final String toString() [자동]]]

## fromJson m21dba4fe8be80ffe

추가 필드·타입 강제 변환 없이 명세의 단일 UUID 문자열을 읽는다.

```java
public static com.dodamdodam.backend.conversation.dto.StartConversationRequest fromJson(tools.jackson.databind.JsonNode body)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.api.ConversationController#prepareStart m1cac5f319ccf317a|ConversationController.prepareStart]] — 호출 · L56

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.dto.StartConversationRequest#생성자 maec59ec0f4972568|StartConversationRequest.생성자]] — 생성 · L32
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.dto.StartConversationRequest#parseUuid m224916bbb94e5c51|StartConversationRequest.parseUuid]] — 호출 · L32
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.exception.ConversationException#validation mdb1af83d0d5a667c|ConversationException.validation]] — 호출 · L26,30

### 외부 라이브러리 호출

- `java.util.Set#containsAll(java.util.Collection<?>)`
- `tools.jackson.databind.JsonNode#get(java.lang.String)`
- `tools.jackson.databind.JsonNode#isObject()`
- `tools.jackson.databind.JsonNode#isString()`
- `tools.jackson.databind.JsonNode#propertyNames()`
- `tools.jackson.databind.JsonNode#stringValue()`

### 소스 코드

`src/main/java/com/dodamdodam/backend/conversation/dto/StartConversationRequest.java:23`

```java
public static StartConversationRequest fromJson(JsonNode body) {
    // 계정과 세션 문맥은 서버에서 얻으므로 본문으로 추가해도 허용하지 않는다.
    if (body == null || !body.isObject() || !FIELDS.containsAll(body.propertyNames())) {
        throw ConversationException.validation("body");
    }
    var key = body.get("clientRequestId");
    if (key == null || !key.isString()) {
        throw ConversationException.validation("clientRequestId");
    }
    return new StartConversationRequest(parseUuid(key.stringValue(), "clientRequestId"));
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## parseChildId mda966a22df85e293

경로의 자녀 UUID에도 본문과 같은 완전한 문자열 형식을 적용한다.

```java
public static java.util.UUID parseChildId(java.lang.String childId)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.api.ConversationController#prepareStart m1cac5f319ccf317a|ConversationController.prepareStart]] — 호출 · L55

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.dto.StartConversationRequest#parseUuid m224916bbb94e5c51|StartConversationRequest.parseUuid]] — 호출 · L37

### 소스 코드

`src/main/java/com/dodamdodam/backend/conversation/dto/StartConversationRequest.java:36`

```java
public static UUID parseChildId(String childId) {
    return parseUuid(childId, "childId");
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## parseUuid m224916bbb94e5c51

UUID.fromString이 허용하는 축약형과 공백·개행을 먼저 거부한다.

```java
private static java.util.UUID parseUuid(java.lang.String value, java.lang.String field)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.dto.StartConversationRequest#fromJson m21dba4fe8be80ffe|StartConversationRequest.fromJson]] — 호출 · L32
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.dto.StartConversationRequest#parseChildId mda966a22df85e293|StartConversationRequest.parseChildId]] — 호출 · L37

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.exception.ConversationException#validation mdb1af83d0d5a667c|ConversationException.validation]] — 호출 · L43

### 외부 라이브러리 호출

- `java.util.UUID#fromString(java.lang.String)`
- `java.util.regex.Matcher#matches()`
- `java.util.regex.Pattern#matcher(java.lang.CharSequence)`

### 소스 코드

`src/main/java/com/dodamdodam/backend/conversation/dto/StartConversationRequest.java:41`

```java
private static UUID parseUuid(String value, String field) {
    if (value == null || !UUID_TEXT.matcher(value).matches()) {
        throw ConversationException.validation(field);
    }
    return UUID.fromString(value);
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## 생성자 maec59ec0f4972568

직접 생성한 요청도 필수 시작 키를 가져야 한다.

```java
public StartConversationRequest(java.util.UUID clientRequestId)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.dto.StartConversationRequest#fromJson m21dba4fe8be80ffe|StartConversationRequest.fromJson]] — 생성 · L32

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.exception.ConversationException#validation mdb1af83d0d5a667c|ConversationException.validation]] — 호출 · L18

### 외부 라이브러리 호출

- `java.lang.Record#<init>()`

### 소스 코드

`src/main/java/com/dodamdodam/backend/conversation/dto/StartConversationRequest.java:16`

```java
public StartConversationRequest {
    if (clientRequestId == null) {
        throw ConversationException.validation("clientRequestId");
    }
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## clientRequestId m6d3b72a72eb6f686

컴파일러가 생성한 멤버다.

```java
public java.util.UUID clientRequestId()
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.api.ConversationController#start m255287826e69c022|ConversationController.start]] — 호출 · L43
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationService#decideStart m074dea56e9276dfa|ConversationService.decideStart]] — 호출 · L88,97,116

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## equals m202151c275b31620

컴파일러가 생성한 멤버다.

```java
public final boolean equals(java.lang.Object o)
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## hashCode m48c19d2af594caca

컴파일러가 생성한 멤버다.

```java
public final int hashCode()
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## toString m6e15de78a5df87ce

컴파일러가 생성한 멤버다.

```java
public final java.lang.String toString()
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]
