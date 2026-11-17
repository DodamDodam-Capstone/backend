---
tags: [backend, code-map, global]
aliases: ["ErrorDetail"]
---
# ErrorDetail

`com.dodamdodam.backend.global.error.ApiErrorResponse.ErrorDetail`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|시작]] → [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/global|기능 개요]] → 이 클래스 · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/90-Reference|전체 참조 자료]]

## 클래스 요약

값을 전달하는 데이터 타입이다.

- 원본: `src/main/java/com/dodamdodam/backend/global/error/ApiErrorResponse.java:7`
- 작성 메서드/생성자 0개 · 자동 생성 8개

## 메서드 목록

데이터·설정 타입이다. 아래 목록을 펼쳐 생성자와 접근자를 확인한다.

> [!example]- 생성자·자동 생성 멤버 8개
>
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.error.ApiErrorResponse.ErrorDetail#code m9fe9809cfceda8f6|public String code() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.error.ApiErrorResponse.ErrorDetail#equals m54ed06e5980e1a42|public final boolean equals(Object o) [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.error.ApiErrorResponse.ErrorDetail#fields m67e3857a2c5f73dd|public List<ApiErrorResponse.FieldError> fields() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.error.ApiErrorResponse.ErrorDetail#hashCode me9f0cdbe6fcf420e|public final int hashCode() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.error.ApiErrorResponse.ErrorDetail#message m5e2f9074ffa013fc|public String message() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.error.ApiErrorResponse.ErrorDetail#requestId m33ee85fabce3c1ad|public String requestId() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.error.ApiErrorResponse.ErrorDetail#toString m97100a5d70cd5563|public final String toString() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.error.ApiErrorResponse.ErrorDetail#생성자 m5f55229570f9831f|public ErrorDetail(String code, String message, String requestId, List<ApiErrorResponse.FieldError> fields) [자동]]]

## code m9fe9809cfceda8f6

컴파일러가 생성한 멤버다.

```java
public java.lang.String code()
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.dto.ActiveConversationExistsError#from mc5d37e16631cc4d2|ActiveConversationExistsError.from]] — 호출 · L14

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/global|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## equals m54ed06e5980e1a42

컴파일러가 생성한 멤버다.

```java
public final boolean equals(java.lang.Object o)
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/global|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## fields m67e3857a2c5f73dd

컴파일러가 생성한 멤버다.

```java
public java.util.List<com.dodamdodam.backend.global.error.ApiErrorResponse.FieldError> fields()
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.dto.ActiveConversationExistsError#from mc5d37e16631cc4d2|ActiveConversationExistsError.from]] — 호출 · L15

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/global|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## hashCode me9f0cdbe6fcf420e

컴파일러가 생성한 멤버다.

```java
public final int hashCode()
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/global|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## message m5e2f9074ffa013fc

컴파일러가 생성한 멤버다.

```java
public java.lang.String message()
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.dto.ActiveConversationExistsError#from mc5d37e16631cc4d2|ActiveConversationExistsError.from]] — 호출 · L14

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/global|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## requestId m33ee85fabce3c1ad

컴파일러가 생성한 멤버다.

```java
public java.lang.String requestId()
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.dto.ActiveConversationExistsError#from mc5d37e16631cc4d2|ActiveConversationExistsError.from]] — 호출 · L14

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/global|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## toString m97100a5d70cd5563

컴파일러가 생성한 멤버다.

```java
public final java.lang.String toString()
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/global|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## 생성자 m5f55229570f9831f

컴파일러가 생성한 멤버다.

```java
public ErrorDetail(java.lang.String code, java.lang.String message, java.lang.String requestId, java.util.List<com.dodamdodam.backend.global.error.ApiErrorResponse.FieldError> fields)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.error.ApiErrorResponses#body me905a471c60d2a7e|ApiErrorResponses.body]] — 생성 · L20

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 외부 라이브러리 호출

- `java.lang.Record#<init>()`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/global|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]
