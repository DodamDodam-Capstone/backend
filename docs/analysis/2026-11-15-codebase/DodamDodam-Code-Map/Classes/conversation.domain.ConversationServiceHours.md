---
tags: [backend, code-map, conversation]
aliases: ["ConversationServiceHours"]
---
# ConversationServiceHours

`com.dodamdodam.backend.conversation.domain.ConversationServiceHours`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|시작]] → [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요]] → 이 클래스 · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/90-Reference|전체 참조 자료]]

## 클래스 요약

대화 서비스 이용시간과 서비스 날짜 경계를 계산·검증한다.

- 원본: `src/main/java/com/dodamdodam/backend/conversation/domain/ConversationServiceHours.java:10`
- 작성 메서드/생성자 3개 · 자동 생성 0개

## 메서드 목록

| 메서드 | 하는 일 |
| --- | --- |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.domain.ConversationServiceHours#requireOpen mf365fa956cdf2081\|requireOpen]] | 호출자가 잠금 후 읽은 DB 시각으로 08시 포함·다음 자정 미포함을 판정한다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.domain.ConversationServiceHours#isOpen maab448b57fa699a5\|isOpen]] | 시간 밖에도 종료 정리를 커밋한 뒤 403을 반환할 수 있도록 순수 시간 판정을 제공한다. |

> [!example]- 생성자·자동 생성 멤버 1개
>
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.domain.ConversationServiceHours#생성자 ma84082f40ab94f4b|private ConversationServiceHours()]]

## requireOpen mf365fa956cdf2081

호출자가 잠금 후 읽은 DB 시각으로 08시 포함·다음 자정 미포함을 판정한다.

```java
public static com.dodamdodam.backend.conversation.domain.ConversationServiceHours.ServiceWindow requireOpen(java.time.OffsetDateTime databaseNow)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationService#decideStart m074dea56e9276dfa|ConversationService.decideStart]] — 호출 · L105,121,127
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationService#replay m8a9b848e75e75181|ConversationService.replay]] — 호출 · L142
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationService#verifyStartResponse macdb3aca10ad50df|ConversationService.verifyStartResponse]] — 호출 · L73

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.domain.ConversationServiceHours#isOpen maab448b57fa699a5|ConversationServiceHours.isOpen]] — 호출 · L22
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.domain.ConversationServiceHours.ServiceWindow#생성자 m03e71a943c961a54|ServiceWindow.생성자]] — 생성 · L27
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.exception.ConversationException#serviceHoursClosed mdd01dc4ac5506f44|ConversationException.serviceHoursClosed]] — 호출 · L23

### 외부 라이브러리 호출

- `java.time.LocalDate#atStartOfDay(java.time.ZoneId)`
- `java.time.LocalDate#plusDays(long)`
- `java.time.OffsetDateTime#atZoneSameInstant(java.time.ZoneId)`
- `java.time.ZonedDateTime#toLocalDate()`
- `java.time.ZonedDateTime#toOffsetDateTime()`
- `java.util.Objects#requireNonNull(T,java.lang.String)`

### 소스 코드

`src/main/java/com/dodamdodam/backend/conversation/domain/ConversationServiceHours.java:20`

```java
public static ServiceWindow requireOpen(OffsetDateTime databaseNow) {
    var checkedAt = Objects.requireNonNull(databaseNow, "databaseNow").atZoneSameInstant(KST);
    if (!isOpen(databaseNow)) {
        throw ConversationException.serviceHoursClosed();
    }
    // 자정부터는 다음 날짜의 00시이므로 다시 시간외가 되며, 잠금 전 날짜를 재사용하지 않는다.
    var date = checkedAt.toLocalDate();
    return new ServiceWindow(date, date.plusDays(1).atStartOfDay(KST).toOffsetDateTime());
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## isOpen maab448b57fa699a5

시간 밖에도 종료 정리를 커밋한 뒤 403을 반환할 수 있도록 순수 시간 판정을 제공한다.

```java
public static boolean isOpen(java.time.OffsetDateTime databaseNow)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationService#decideStart m074dea56e9276dfa|ConversationService.decideStart]] — 호출 · L102
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationService#verifyStartResponse macdb3aca10ad50df|ConversationService.verifyStartResponse]] — 호출 · L67
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.domain.ConversationServiceHours#requireOpen mf365fa956cdf2081|ConversationServiceHours.requireOpen]] — 호출 · L22

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 외부 라이브러리 호출

- `java.time.LocalTime#isBefore(java.time.LocalTime)`
- `java.time.OffsetDateTime#atZoneSameInstant(java.time.ZoneId)`
- `java.time.ZonedDateTime#toLocalTime()`
- `java.util.Objects#requireNonNull(T,java.lang.String)`

### 소스 코드

`src/main/java/com/dodamdodam/backend/conversation/domain/ConversationServiceHours.java:31`

```java
public static boolean isOpen(OffsetDateTime databaseNow) {
    return !Objects.requireNonNull(databaseNow, "databaseNow").atZoneSameInstant(KST).toLocalTime().isBefore(OPENS_AT);
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## 생성자 ma84082f40ab94f4b

외부 시계 없이 전달받은 DB 판정 시각만 사용하는 정책이다.

```java
private ConversationServiceHours()
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 외부 라이브러리 호출

- `java.lang.Object#<init>()`

### 소스 코드

`src/main/java/com/dodamdodam/backend/conversation/domain/ConversationServiceHours.java:16`

```java
private ConversationServiceHours() {
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]
