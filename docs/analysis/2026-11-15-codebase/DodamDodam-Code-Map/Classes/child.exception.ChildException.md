---
tags: [backend, code-map, child]
aliases: ["ChildException"]
---
# ChildException

`com.dodamdodam.backend.child.exception.ChildException`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|시작]] → [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/child|기능 개요]] → 이 클래스 · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/90-Reference|전체 참조 자료]]

## 클래스 요약

업무 실패의 상태 코드와 공개 오류 정보를 전달한다.

- 원본: `src/main/java/com/dodamdodam/backend/child/exception/ChildException.java:6`
- 작성 메서드/생성자 7개 · 자동 생성 0개

## 메서드 목록

| 메서드 | 하는 일 |
| --- | --- |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.exception.ChildException#notFound mda3f586558efa3e2\|notFound]] | 미소유·미존재 자녀를 같은 오류로 처리한다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.exception.ChildException#validation m04bce91b4a5ea457\|validation]] | 원본 입력을 노출하지 않고 검증에 실패한 필드만 전달한다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.exception.ChildException#limitReached me8321a7d6a24ae8e\|limitReached]] | 계정당 자녀 1명 제한에 해당하는 업무 오류를 만든다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.exception.ChildException#code m9503281b2a5259d9\|code]] | HTTP 처리 계층이 사용할 업무 오류 코드를 반환한다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.exception.ChildException#fields m5ca1eb5c4e2bd206\|fields]] | 원본 입력을 포함하지 않는 필드 오류 정보를 반환한다. |

> [!example]- 생성자·자동 생성 멤버 2개
>
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.exception.ChildException#생성자 m1309e1738b206625|public ChildException(Throwable cause)]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.exception.ChildException#생성자 m09180f2bb9ab53c7|private ChildException(String code, String message, List<ApiErrorResponse.FieldError> fields, Throwable cause)]]

## notFound mda3f586558efa3e2

미소유·미존재 자녀를 같은 오류로 처리한다.

```java
public static com.dodamdodam.backend.child.exception.ChildException notFound()
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.application.ChildService#getChildContext m6caa8e347baa2700|ChildService.getChildContext]] — 참조 · L67
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.application.ChildService#requireChildAccess mbb6af419b1ceab47|ChildService.requireChildAccess]] — 참조 · L56
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.application.ChildService#requireChildAccessForUpdate m10ec0868c9e8ded5|ChildService.requireChildAccessForUpdate]] — 참조 · L62
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationClosureCoordinator#reconcile m5c7d3b9b2dd0cb88|ConversationClosureCoordinator.reconcile]] — 호출 · L37
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationClosureCoordinator#requireState mbbd5c374803feeec|ConversationClosureCoordinator.requireState]] — 참조 · L69
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationService#verifyStartResponse macdb3aca10ad50df|ConversationService.verifyStartResponse]] — 참조 · L56,62

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.exception.ChildException#생성자 m09180f2bb9ab53c7|ChildException.생성자]] — 생성 · L24

### 외부 라이브러리 호출

- `java.util.List#of()`

### 소스 코드

`src/main/java/com/dodamdodam/backend/child/exception/ChildException.java:23`

```java
public static ChildException notFound() {
    return new ChildException("RESOURCE_NOT_FOUND", "자녀 정보를 찾을 수 없습니다.", List.of(), null);
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/child|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## validation m04bce91b4a5ea457

원본 입력을 노출하지 않고 검증에 실패한 필드만 전달한다.

```java
public static com.dodamdodam.backend.child.exception.ChildException validation(java.lang.String field)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.api.ChildController#invalidJson m85e9a4840a5175ad|ChildController.invalidJson]] — 호출 · L47
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.dto.CreateChildRequest#fromJson mf8c618d868acf95b|CreateChildRequest.fromJson]] — 호출 · L26,35,40,48,55
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.dto.CreateChildRequest#normalized mf6b45ea2162d93f1|CreateChildRequest.normalized]] — 호출 · L71,74,77,85,92
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.dto.CreateChildRequest#normalizedText m1fe5e0b24f99439b|CreateChildRequest.normalizedText]] — 호출 · L111,126,131
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.dto.CreateChildRequest#requiredString m56d642f98e92dbce|CreateChildRequest.requiredString]] — 호출 · L103

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.exception.ChildException#생성자 m09180f2bb9ab53c7|ChildException.생성자]] — 생성 · L29
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.error.ApiErrorResponse.FieldError#생성자 mf0e62423c0dec9de|FieldError.생성자]] — 생성 · L30

### 외부 라이브러리 호출

- `java.util.List#of(E)`

### 소스 코드

`src/main/java/com/dodamdodam/backend/child/exception/ChildException.java:28`

```java
public static ChildException validation(String field) {
    return new ChildException("VALIDATION_FAILED", "입력값을 확인해 주세요.",
            List.of(new FieldError(field, "유효하지 않은 값입니다.")), null);
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/child|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## limitReached me8321a7d6a24ae8e

계정당 자녀 1명 제한에 해당하는 업무 오류를 만든다.

```java
public static com.dodamdodam.backend.child.exception.ChildException limitReached()
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.application.ChildService#create m022c1fb522a0efea|ChildService.create]] — 참조 · L51

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.exception.ChildException#생성자 m09180f2bb9ab53c7|ChildException.생성자]] — 생성 · L35

### 외부 라이브러리 호출

- `java.util.List#of()`

### 소스 코드

`src/main/java/com/dodamdodam/backend/child/exception/ChildException.java:34`

```java
public static ChildException limitReached() {
    return new ChildException("CHILD_LIMIT_REACHED", "계정당 자녀는 1명만 등록할 수 있습니다.", List.of(), null);
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/child|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## code m9503281b2a5259d9

HTTP 처리 계층이 사용할 업무 오류 코드를 반환한다.

```java
public java.lang.String code()
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.api.ChildController#invalidJson m85e9a4840a5175ad|ChildController.invalidJson]] — 호출 · L49
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.api.ChildExceptionHandler#handle ma451743715a09223|ChildExceptionHandler.handle]] — 호출 · L21,31

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 소스 코드

`src/main/java/com/dodamdodam/backend/child/exception/ChildException.java:39`

```java
public String code() {
    return code;
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/child|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## fields m5ca1eb5c4e2bd206

원본 입력을 포함하지 않는 필드 오류 정보를 반환한다.

```java
public java.util.List<com.dodamdodam.backend.global.error.ApiErrorResponse.FieldError> fields()
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.api.ChildController#invalidJson m85e9a4840a5175ad|ChildController.invalidJson]] — 호출 · L49
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.api.ChildExceptionHandler#handle ma451743715a09223|ChildExceptionHandler.handle]] — 호출 · L33

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 소스 코드

`src/main/java/com/dodamdodam/backend/child/exception/ChildException.java:44`

```java
public List<FieldError> fields() {
    return fields;
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/child|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## 생성자 m1309e1738b206625

저장 장애의 원인을 보존하고 응답에는 고정된 업무 오류만 전달한다.

```java
public ChildException(java.lang.Throwable cause)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.infrastructure.ChildRepository#findById mdfeacb26eaf99e57|ChildRepository.findById]] — 생성 · L91
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.infrastructure.ChildRepository#findOwned m0c455eef6c80ad12|ChildRepository.findOwned]] — 생성 · L69
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.infrastructure.ChildRepository#findOwnedForUpdate maf36c957db0ad4a1|ChildRepository.findOwnedForUpdate]] — 생성 · L80
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.infrastructure.ChildRepository#insert mcdb7b197a0d8c26e|ChildRepository.insert]] — 생성 · L58

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.exception.ChildException#생성자 m09180f2bb9ab53c7|ChildException.생성자]] — 호출 · L13

### 외부 라이브러리 호출

- `java.util.List#of()`

### 소스 코드

`src/main/java/com/dodamdodam/backend/child/exception/ChildException.java:12`

```java
public ChildException(Throwable cause) {
    this("INTERNAL_ERROR", "자녀 정보를 저장하거나 조회할 수 없습니다.", List.of(), cause);
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/child|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## 생성자 m09180f2bb9ab53c7

아래 원본 코드에서 구현을 확인한다.

```java
private ChildException(java.lang.String code, java.lang.String message, java.util.List<com.dodamdodam.backend.global.error.ApiErrorResponse.FieldError> fields, java.lang.Throwable cause)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.exception.ChildException#생성자 m1309e1738b206625|ChildException.생성자]] — 호출 · L13
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.exception.ChildException#limitReached me8321a7d6a24ae8e|ChildException.limitReached]] — 생성 · L35
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.exception.ChildException#notFound mda3f586558efa3e2|ChildException.notFound]] — 생성 · L24
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.exception.ChildException#validation m04bce91b4a5ea457|ChildException.validation]] — 생성 · L29

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 외부 라이브러리 호출

- `java.lang.RuntimeException#<init>(java.lang.String,java.lang.Throwable)`

### 소스 코드

`src/main/java/com/dodamdodam/backend/child/exception/ChildException.java:16`

```java
private ChildException(String code, String message, List<FieldError> fields, Throwable cause) {
    super(message, cause);
    this.code = code;
    this.fields = fields;
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/child|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]
