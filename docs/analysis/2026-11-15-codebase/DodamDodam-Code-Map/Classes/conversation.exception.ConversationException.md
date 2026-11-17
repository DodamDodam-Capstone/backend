---
tags: [backend, code-map, conversation]
aliases: ["ConversationException"]
---
# ConversationException

`com.dodamdodam.backend.conversation.exception.ConversationException`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|시작]] → [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요]] → 이 클래스 · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/90-Reference|전체 참조 자료]]

## 클래스 요약

업무 실패의 상태 코드와 공개 오류 정보를 전달한다.

- 원본: `src/main/java/com/dodamdodam/backend/conversation/exception/ConversationException.java:8`
- 작성 메서드/생성자 11개 · 자동 생성 0개

## 메서드 목록

| 메서드 | 하는 일 |
| --- | --- |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.exception.ConversationException#validation mdb1af83d0d5a667c\|validation]] | 정의된 필드 이름만 공개하고 잘못된 입력값은 오류에 복사하지 않는다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.exception.ConversationException#serviceHoursClosed mdd01dc4ac5506f44\|serviceHoursClosed]] | 아이 대화 이용시간 밖의 요청을 공통 403 업무 오류로 표현한다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.exception.ConversationException#childSessionRequired mfac9ec20240efe61\|childSessionRequired]] | 같은 시작 키라도 현재 보안 문맥에 연결되지 않았다면 명시적 이어하기가 필요하다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.exception.ConversationException#activeExists m4697f9606f9c286b\|activeExists]] | 현재 ACTIVE가 있는 경우에만 명세가 허용하는 기존 대화 ID를 공개한다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.exception.ConversationException#closing mf4eb5930af2bafa7\|closing]] | 종료 조정 중에는 새 대화를 만들지 않는다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.exception.ConversationException#ended m7095f7ecea180249\|ended]] | 종료된 시작 키의 재사용으로 새로운 행을 만들지 않는다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.exception.ConversationException#code m57ace0236dcd9832\|code]] | HTTP 처리 계층에서 사용할 업무 오류 코드를 반환한다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.exception.ConversationException#fields m23707773febd9fb7\|fields]] | 공통 오류 응답에 담을 검증 오류 목록을 반환한다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.exception.ConversationException#activeConversationId mca4dcd3b0a983b42\|activeConversationId]] | ACTIVE 충돌 전용 응답에서만 사용할 기존 대화 ID다. |

> [!example]- 생성자·자동 생성 멤버 2개
>
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.exception.ConversationException#생성자 mabc724be386e3a0c|private ConversationException(String code, String message, List<ApiErrorResponse.FieldError> fields, UUID activeConversationId, Throwable cause)]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.exception.ConversationException#생성자 mdf07793b291a512a|public ConversationException(Throwable cause)]]

## validation mdb1af83d0d5a667c

정의된 필드 이름만 공개하고 잘못된 입력값은 오류에 복사하지 않는다.

```java
public static com.dodamdodam.backend.conversation.exception.ConversationException validation(java.lang.String field)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.dto.StartConversationRequest#생성자 maec59ec0f4972568|StartConversationRequest.생성자]] — 호출 · L18
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.dto.StartConversationRequest#fromJson m21dba4fe8be80ffe|StartConversationRequest.fromJson]] — 호출 · L26,30
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.dto.StartConversationRequest#parseUuid m224916bbb94e5c51|StartConversationRequest.parseUuid]] — 호출 · L43

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.exception.ConversationException#생성자 mabc724be386e3a0c|ConversationException.생성자]] — 생성 · L30
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.error.ApiErrorResponse.FieldError#생성자 mf0e62423c0dec9de|FieldError.생성자]] — 생성 · L31

### 외부 라이브러리 호출

- `java.util.List#of(E)`

### 소스 코드

`src/main/java/com/dodamdodam/backend/conversation/exception/ConversationException.java:29`

```java
public static ConversationException validation(String field) {
    return new ConversationException("VALIDATION_FAILED", "입력값을 확인해 주세요.",
            List.of(new FieldError(field, "유효하지 않은 값입니다.")), null, null);
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## serviceHoursClosed mdd01dc4ac5506f44

아이 대화 이용시간 밖의 요청을 공통 403 업무 오류로 표현한다.

```java
public static com.dodamdodam.backend.conversation.exception.ConversationException serviceHoursClosed()
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationService#decideStart m074dea56e9276dfa|ConversationService.decideStart]] — 호출 · L103,129
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationService#replay m8a9b848e75e75181|ConversationService.replay]] — 호출 · L144
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationService#verifyStartResponse macdb3aca10ad50df|ConversationService.verifyStartResponse]] — 호출 · L68
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.domain.ConversationServiceHours#requireOpen mf365fa956cdf2081|ConversationServiceHours.requireOpen]] — 호출 · L23

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.exception.ConversationException#생성자 mabc724be386e3a0c|ConversationException.생성자]] — 생성 · L36

### 외부 라이브러리 호출

- `java.util.List#of()`

### 소스 코드

`src/main/java/com/dodamdodam/backend/conversation/exception/ConversationException.java:35`

```java
public static ConversationException serviceHoursClosed() {
    return new ConversationException("SERVICE_HOURS_CLOSED", "대화 이용시간이 아닙니다.", List.of(), null, null);
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## childSessionRequired mfac9ec20240efe61

같은 시작 키라도 현재 보안 문맥에 연결되지 않았다면 명시적 이어하기가 필요하다.

```java
public static com.dodamdodam.backend.conversation.exception.ConversationException childSessionRequired()
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationService#replay m8a9b848e75e75181|ConversationService.replay]] — 호출 · L147

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.exception.ConversationException#생성자 mabc724be386e3a0c|ConversationException.생성자]] — 생성 · L41

### 외부 라이브러리 호출

- `java.util.List#of()`

### 소스 코드

`src/main/java/com/dodamdodam/backend/conversation/exception/ConversationException.java:40`

```java
public static ConversationException childSessionRequired() {
    return new ConversationException("CHILD_SESSION_REQUIRED", "현재 세션에서 대화 이어하기가 필요합니다.", List.of(), null, null);
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## activeExists m4697f9606f9c286b

현재 ACTIVE가 있는 경우에만 명세가 허용하는 기존 대화 ID를 공개한다.

```java
public static com.dodamdodam.backend.conversation.exception.ConversationException activeExists(java.util.UUID conversationId)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationService#conflictFor m1c136cdb5700e953|ConversationService.conflictFor]] — 호출 · L155

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.exception.ConversationException#생성자 mabc724be386e3a0c|ConversationException.생성자]] — 생성 · L46

### 외부 라이브러리 호출

- `java.util.List#of()`
- `java.util.Objects#requireNonNull(T,java.lang.String)`

### 소스 코드

`src/main/java/com/dodamdodam/backend/conversation/exception/ConversationException.java:45`

```java
public static ConversationException activeExists(UUID conversationId) {
    return new ConversationException("ACTIVE_CONVERSATION_EXISTS", "진행 중인 대화가 있습니다.", List.of(),
            Objects.requireNonNull(conversationId, "conversationId"), null);
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## closing mf4eb5930af2bafa7

종료 조정 중에는 새 대화를 만들지 않는다.

```java
public static com.dodamdodam.backend.conversation.exception.ConversationException closing()
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationService#conflictFor m1c136cdb5700e953|ConversationService.conflictFor]] — 호출 · L156

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.exception.ConversationException#생성자 mabc724be386e3a0c|ConversationException.생성자]] — 생성 · L52

### 외부 라이브러리 호출

- `java.util.List#of()`

### 소스 코드

`src/main/java/com/dodamdodam/backend/conversation/exception/ConversationException.java:51`

```java
public static ConversationException closing() {
    return new ConversationException("CONVERSATION_CLOSING", "대화를 종료하고 있습니다.", List.of(), null, null);
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## ended m7095f7ecea180249

종료된 시작 키의 재사용으로 새로운 행을 만들지 않는다.

```java
public static com.dodamdodam.backend.conversation.exception.ConversationException ended()
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationService#conflictFor m1c136cdb5700e953|ConversationService.conflictFor]] — 호출 · L157

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.exception.ConversationException#생성자 mabc724be386e3a0c|ConversationException.생성자]] — 생성 · L57

### 외부 라이브러리 호출

- `java.util.List#of()`

### 소스 코드

`src/main/java/com/dodamdodam/backend/conversation/exception/ConversationException.java:56`

```java
public static ConversationException ended() {
    return new ConversationException("CONVERSATION_ENDED", "이미 종료된 대화입니다.", List.of(), null, null);
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## code m57ace0236dcd9832

HTTP 처리 계층에서 사용할 업무 오류 코드를 반환한다.

```java
public java.lang.String code()
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.api.ConversationExceptionHandler#handle m50c7d1b4e6622616|ConversationExceptionHandler.handle]] — 호출 · L27,33

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 소스 코드

`src/main/java/com/dodamdodam/backend/conversation/exception/ConversationException.java:61`

```java
public String code() {
    return code;
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## fields m23707773febd9fb7

공통 오류 응답에 담을 검증 오류 목록을 반환한다.

```java
public java.util.List<com.dodamdodam.backend.global.error.ApiErrorResponse.FieldError> fields()
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.api.ConversationExceptionHandler#handle m50c7d1b4e6622616|ConversationExceptionHandler.handle]] — 호출 · L33

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 소스 코드

`src/main/java/com/dodamdodam/backend/conversation/exception/ConversationException.java:66`

```java
public List<FieldError> fields() {
    return fields;
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## activeConversationId mca4dcd3b0a983b42

ACTIVE 충돌 전용 응답에서만 사용할 기존 대화 ID다.

```java
public java.util.UUID activeConversationId()
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.api.ConversationExceptionHandler#handle m50c7d1b4e6622616|ConversationExceptionHandler.handle]] — 호출 · L34,35

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 소스 코드

`src/main/java/com/dodamdodam/backend/conversation/exception/ConversationException.java:71`

```java
public UUID activeConversationId() {
    return activeConversationId;
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## 생성자 mabc724be386e3a0c

입력 원문이나 내부 식별자를 포함하지 않는 고정 오류를 구성한다.

```java
private ConversationException(java.lang.String code, java.lang.String message, java.util.List<com.dodamdodam.backend.global.error.ApiErrorResponse.FieldError> fields, java.util.UUID activeConversationId, java.lang.Throwable cause)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.exception.ConversationException#생성자 mdf07793b291a512a|ConversationException.생성자]] — 호출 · L25
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.exception.ConversationException#activeExists m4697f9606f9c286b|ConversationException.activeExists]] — 생성 · L46
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.exception.ConversationException#childSessionRequired mfac9ec20240efe61|ConversationException.childSessionRequired]] — 생성 · L41
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.exception.ConversationException#closing mf4eb5930af2bafa7|ConversationException.closing]] — 생성 · L52
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.exception.ConversationException#ended m7095f7ecea180249|ConversationException.ended]] — 생성 · L57
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.exception.ConversationException#serviceHoursClosed mdd01dc4ac5506f44|ConversationException.serviceHoursClosed]] — 생성 · L36
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.exception.ConversationException#validation mdb1af83d0d5a667c|ConversationException.validation]] — 생성 · L30

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 외부 라이브러리 호출

- `java.lang.RuntimeException#<init>(java.lang.String,java.lang.Throwable)`
- `java.util.List#copyOf(java.util.Collection<? extends E>)`

### 소스 코드

`src/main/java/com/dodamdodam/backend/conversation/exception/ConversationException.java:15`

```java
private ConversationException(String code, String message, List<FieldError> fields,
                              UUID activeConversationId, Throwable cause) {
    super(message, cause);
    this.code = code;
    this.fields = List.copyOf(fields);
    this.activeConversationId = activeConversationId;
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## 생성자 mdf07793b291a512a

업무 DB 오류는 원인을 보존하되 공통 500 응답에 원문을 노출하지 않는다.

```java
public ConversationException(java.lang.Throwable cause)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.api.ConversationStartResponseFilter#doFilterInternal m2e5a11b062f28145|ConversationStartResponseFilter.doFilterInternal]] — 생성 · L114
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationService#conflictFor m1c136cdb5700e953|ConversationService.conflictFor]] — 생성 · L158
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationService#decideStart m074dea56e9276dfa|ConversationService.decideStart]] — 생성 · L120
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.infrastructure.ConversationClosureRepository#beginClosing m8c1904cc945fbe62|ConversationClosureRepository.beginClosing]] — 생성 · L59
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.infrastructure.ConversationClosureRepository#expireProcessing m013d6a27bd537b45|ConversationClosureRepository.expireProcessing]] — 생성 · L84
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.infrastructure.ConversationClosureRepository#findForUpdate m31b37bc252579185|ConversationClosureRepository.findForUpdate]] — 생성 · L46
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.infrastructure.ConversationClosureRepository#finishClosing mabcd7eabe63e07b8|ConversationClosureRepository.finishClosing]] — 생성 · L141
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.infrastructure.ConversationClosureRepository#grantRecovery m7ac5ec5179a9bacb|ConversationClosureRepository.grantRecovery]] — 생성 · L159
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.infrastructure.ConversationClosureRepository#hasProcessing mbb01ff6c85dec5a2|ConversationClosureRepository.hasProcessing]] — 생성 · L96
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.infrastructure.ConversationClosureRepository#hasSummaryInput mc52240ff470ce37e|ConversationClosureRepository.hasSummaryInput]] — 생성 · L111
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.infrastructure.ConversationClosureRepository#lockAccount me75547411e9e6805|ConversationClosureRepository.lockAccount]] — 생성 · L30
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.infrastructure.ConversationClosureRepository#lockLinks m1f2723814c4ce03e|ConversationClosureRepository.lockLinks]] — 생성 · L124
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.infrastructure.ConversationClosureRepository#lockProcessingTurns macac7a26cd3dcb0a|ConversationClosureRepository.lockProcessingTurns]] — 생성 · L71
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.infrastructure.ConversationClosureRepository#requireTransition m3fbf937b913ee91d|ConversationClosureRepository.requireTransition]] — 생성 · L166
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.infrastructure.ConversationRepository#currentTime m7f615951fbdd6a0f|ConversationRepository.currentTime]] — 생성 · L38
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.infrastructure.ConversationRepository#findRequestForUpdate m8f65c4eacd8961ee|ConversationRepository.findRequestForUpdate]] — 생성 · L51
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.infrastructure.ConversationRepository#findUnendedForUpdate m395989c791432963|ConversationRepository.findUnendedForUpdate]] — 생성 · L64
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.infrastructure.ConversationRepository#hasLinkForUpdate m72ad3628685c5f36|ConversationRepository.hasLinkForUpdate]] — 생성 · L78
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.infrastructure.ConversationRepository#insertActive mff8e74c752863f8c|ConversationRepository.insertActive]] — 생성 · L99
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.infrastructure.ConversationRepository#insertLink m5d92b93bc37cc1e5|ConversationRepository.insertLink]] — 생성 · L112

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.exception.ConversationException#생성자 mabc724be386e3a0c|ConversationException.생성자]] — 호출 · L25

### 외부 라이브러리 호출

- `java.util.List#of()`

### 소스 코드

`src/main/java/com/dodamdodam/backend/conversation/exception/ConversationException.java:24`

```java
public ConversationException(Throwable cause) {
    this("INTERNAL_ERROR", "대화 정보를 저장하거나 조회할 수 없습니다.", List.of(), null, cause);
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]
