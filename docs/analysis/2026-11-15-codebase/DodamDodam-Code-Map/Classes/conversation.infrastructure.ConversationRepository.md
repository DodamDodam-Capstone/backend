---
tags: [backend, code-map, conversation]
aliases: ["ConversationRepository"]
---
# ConversationRepository

`com.dodamdodam.backend.conversation.infrastructure.ConversationRepository`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|시작]] → [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요]] → 이 클래스 · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/90-Reference|전체 참조 자료]]

## 클래스 요약

대화 시작·중복 조회·세션 연결과 관련된 SQL 및 잠금을 담당한다.

- 원본: `src/main/java/com/dodamdodam/backend/conversation/infrastructure/ConversationRepository.java:17`
- 작성 메서드/생성자 7개 · 자동 생성 0개

## 메서드 목록

| 메서드 | 하는 일 |
| --- | --- |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.infrastructure.ConversationRepository#currentTime m7f615951fbdd6a0f\|currentTime]] | 트랜잭션 시작 시각이 아닌 호출 시점의 DB 시각을 읽는다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.infrastructure.ConversationRepository#findRequestForUpdate m8f65c4eacd8961ee\|findRequestForUpdate]] | 자녀 잠금 뒤 시작 키에 해당하는 대화 상태를 잠가 확인한다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.infrastructure.ConversationRepository#findUnendedForUpdate m395989c791432963\|findUnendedForUpdate]] | 같은 자녀의 미종료 행을 잠그며, 종료 조정기와의 상태 변경도 직렬화한다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.infrastructure.ConversationRepository#hasLinkForUpdate m72ad3628685c5f36\|hasLinkForUpdate]] | 대화 잠금 뒤 현재 보안 문맥의 기존 연결만 잠가 확인하며 새 연결은 만들지 않는다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.infrastructure.ConversationRepository#insertActive mff8e74c752863f8c\|insertActive]] | 미종료 부분 UNIQUE 충돌만 빈 결과로 구분하고, 다른 제약·저장 장애는 500 원인을 보존한다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.infrastructure.ConversationRepository#insertLink m5d92b93bc37cc1e5\|insertLink]] | 생성과 같은 트랜잭션에서 현재 보안 문맥의 최초 연결 시각을 저장한다. |

> [!example]- 생성자·자동 생성 멤버 1개
>
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.infrastructure.ConversationRepository#생성자 m0d8d2b475ae45c4c|public ConversationRepository(JdbcTemplate jdbc)]]

## currentTime m7f615951fbdd6a0f

트랜잭션 시작 시각이 아닌 호출 시점의 DB 시각을 읽는다.

```java
public java.time.OffsetDateTime currentTime()
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationClosureCoordinator#reconcile m5c7d3b9b2dd0cb88|ConversationClosureCoordinator.reconcile]] — 호출 · L41,55,60
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationService#decideStart m074dea56e9276dfa|ConversationService.decideStart]] — 호출 · L87,92,100,121,127
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationService#replay m8a9b848e75e75181|ConversationService.replay]] — 호출 · L142
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationService#verifyStartResponse macdb3aca10ad50df|ConversationService.verifyStartResponse]] — 호출 · L58,64

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.exception.ConversationException#생성자 mdf07793b291a512a|ConversationException.생성자]] — 생성 · L38

### 외부 라이브러리 호출

- `java.sql.ResultSet#getObject(int,java.lang.Class<T>)`
- `org.springframework.jdbc.core.JdbcTemplate#queryForObject(java.lang.String,org.springframework.jdbc.core.RowMapper<T>)`

### 소스 코드

`src/main/java/com/dodamdodam/backend/conversation/infrastructure/ConversationRepository.java:33`

```java
public OffsetDateTime currentTime() {
    try {
        return jdbc.queryForObject("SELECT clock_timestamp()", (row, _) -> row.getObject(1, OffsetDateTime.class));
    }
    catch (DataAccessException failure) {
        throw new ConversationException(failure);
    }
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## findRequestForUpdate m8f65c4eacd8961ee

자녀 잠금 뒤 시작 키에 해당하는 대화 상태를 잠가 확인한다.

```java
public java.util.Optional<com.dodamdodam.backend.conversation.infrastructure.ConversationRepository.StoredConversation> findRequestForUpdate(java.util.UUID childId, java.util.UUID clientRequestId)
```

설정: `@org.springframework.transaction.annotation.Transactional(propagation=MANDATORY)`

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationService#decideStart m074dea56e9276dfa|ConversationService.decideStart]] — 호출 · L88,97
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationService#verifyStartResponse macdb3aca10ad50df|ConversationService.verifyStartResponse]] — 호출 · L55,61

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.exception.ConversationException#생성자 mdf07793b291a512a|ConversationException.생성자]] — 생성 · L51

### 외부 라이브러리 호출

- `java.util.Collection#stream()`
- `java.util.stream.Stream#findFirst()`
- `org.springframework.jdbc.core.JdbcTemplate#query(java.lang.String,org.springframework.jdbc.core.RowMapper<T>,java.lang.@org.jspecify.annotations.Nullable Object @org.jspecify.annotations.Nullable [])`

### 소스 코드

`src/main/java/com/dodamdodam/backend/conversation/infrastructure/ConversationRepository.java:43`

```java
@Transactional(propagation = Propagation.MANDATORY)
public Optional<StoredConversation> findRequestForUpdate(UUID childId, UUID clientRequestId) {
    try {
        return jdbc.query("SELECT " + COLUMNS + " FROM conversations WHERE child_id = ? AND client_request_id = ? FOR UPDATE",
                STORED_ROW,
                childId, clientRequestId).stream().findFirst();
    }
    catch (DataAccessException failure) {
        throw new ConversationException(failure);
    }
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## findUnendedForUpdate m395989c791432963

같은 자녀의 미종료 행을 잠그며, 종료 조정기와의 상태 변경도 직렬화한다.

```java
public java.util.Optional<com.dodamdodam.backend.conversation.infrastructure.ConversationRepository.StoredConversation> findUnendedForUpdate(java.util.UUID childId)
```

설정: `@org.springframework.transaction.annotation.Transactional(propagation=MANDATORY)`

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationService#decideStart m074dea56e9276dfa|ConversationService.decideStart]] — 호출 · L89,98,119

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.exception.ConversationException#생성자 mdf07793b291a512a|ConversationException.생성자]] — 생성 · L64

### 외부 라이브러리 호출

- `java.util.Collection#stream()`
- `java.util.stream.Stream#findFirst()`
- `org.springframework.jdbc.core.JdbcTemplate#query(java.lang.String,org.springframework.jdbc.core.RowMapper<T>,java.lang.@org.jspecify.annotations.Nullable Object @org.jspecify.annotations.Nullable [])`

### 소스 코드

`src/main/java/com/dodamdodam/backend/conversation/infrastructure/ConversationRepository.java:56`

```java
@Transactional(propagation = Propagation.MANDATORY)
public Optional<StoredConversation> findUnendedForUpdate(UUID childId) {
    try {
        return jdbc.query("SELECT " + COLUMNS + " FROM conversations WHERE child_id = ? AND status IN ('ACTIVE','CLOSING') FOR UPDATE",
                STORED_ROW,
                childId).stream().findFirst();
    }
    catch (DataAccessException failure) {
        throw new ConversationException(failure);
    }
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## hasLinkForUpdate m72ad3628685c5f36

대화 잠금 뒤 현재 보안 문맥의 기존 연결만 잠가 확인하며 새 연결은 만들지 않는다.

```java
public boolean hasLinkForUpdate(java.util.UUID securityContextId, java.util.UUID conversationId)
```

설정: `@org.springframework.transaction.annotation.Transactional(propagation=MANDATORY)`

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationService#replay m8a9b848e75e75181|ConversationService.replay]] — 호출 · L139

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.exception.ConversationException#생성자 mdf07793b291a512a|ConversationException.생성자]] — 생성 · L78

### 외부 라이브러리 호출

- `java.util.List#isEmpty()`
- `org.springframework.jdbc.core.JdbcTemplate#queryForList(java.lang.String,java.lang.Class<T>,java.lang.@org.jspecify.annotations.Nullable Object @org.jspecify.annotations.Nullable [])`

### 소스 코드

`src/main/java/com/dodamdodam/backend/conversation/infrastructure/ConversationRepository.java:69`

```java
@Transactional(propagation = Propagation.MANDATORY)
public boolean hasLinkForUpdate(UUID securityContextId, UUID conversationId) {
    try {
        return !jdbc.queryForList("""
                SELECT session_security_id FROM conversation_session_links
                WHERE session_security_id = ? AND conversation_id = ? FOR UPDATE
                """, UUID.class, securityContextId, conversationId).isEmpty();
    }
    catch (DataAccessException failure) {
        throw new ConversationException(failure);
    }
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## insertActive mff8e74c752863f8c

미종료 부분 UNIQUE 충돌만 빈 결과로 구분하고, 다른 제약·저장 장애는 500 원인을 보존한다.

```java
public java.util.Optional<com.dodamdodam.backend.conversation.dto.StartConversationResult> insertActive(java.util.UUID id, java.util.UUID childId, java.util.UUID clientRequestId, java.time.OffsetDateTime startedAt, com.dodamdodam.backend.conversation.domain.ConversationServiceHours.ServiceWindow window)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationService#decideStart m074dea56e9276dfa|ConversationService.decideStart]] — 호출 · L116

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.domain.ConversationServiceHours.ServiceWindow#scheduledEndAt m62fcc5985e3dc7b2|ServiceWindow.scheduledEndAt]] — 호출 · L95
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.domain.ConversationServiceHours.ServiceWindow#serviceDate m935e99412829c2a1|ServiceWindow.serviceDate]] — 호출 · L95
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.dto.StartConversationResult#생성자 m9ffc5c7b78e8b45c|StartConversationResult.생성자]] — 생성 · L92
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.exception.ConversationException#생성자 mdf07793b291a512a|ConversationException.생성자]] — 생성 · L99

### 외부 라이브러리 호출

- `java.sql.ResultSet#getObject(java.lang.String,java.lang.Class<T>)`
- `java.sql.ResultSet#getString(java.lang.String)`
- `java.util.Collection#stream()`
- `java.util.stream.Stream#findFirst()`
- `org.springframework.jdbc.core.JdbcTemplate#query(java.lang.String,org.springframework.jdbc.core.RowMapper<T>,java.lang.@org.jspecify.annotations.Nullable Object @org.jspecify.annotations.Nullable [])`

### 소스 코드

`src/main/java/com/dodamdodam/backend/conversation/infrastructure/ConversationRepository.java:83`

```java
public Optional<StartConversationResult> insertActive(UUID id, UUID childId, UUID clientRequestId,
                                                     OffsetDateTime startedAt, ServiceWindow window) {
    try {
        return jdbc.query("""
                INSERT INTO conversations (id, child_id, client_request_id, service_date, scheduled_end_at,
                    status, summary_status, started_at)
                VALUES (?, ?, ?, ?, ?, 'ACTIVE', 'NOT_STARTED', ?)
                ON CONFLICT (child_id) WHERE status IN ('ACTIVE','CLOSING') DO NOTHING
                RETURNING id, status, started_at, service_date, scheduled_end_at
                """, (row, _) -> new StartConversationResult(row.getObject("id", UUID.class), row.getString("status"),
                row.getObject("started_at", OffsetDateTime.class), row.getObject("service_date", LocalDate.class),
                row.getObject("scheduled_end_at", OffsetDateTime.class)),
                id, childId, clientRequestId, window.serviceDate(), window.scheduledEndAt(), startedAt)
                .stream().findFirst();
    }
    catch (DataAccessException failure) {
        throw new ConversationException(failure);
    }
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## insertLink m5d92b93bc37cc1e5

생성과 같은 트랜잭션에서 현재 보안 문맥의 최초 연결 시각을 저장한다.

```java
public void insertLink(java.util.UUID securityContextId, java.util.UUID conversationId, java.time.OffsetDateTime boundAt)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationService#decideStart m074dea56e9276dfa|ConversationService.decideStart]] — 호출 · L125

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.exception.ConversationException#생성자 mdf07793b291a512a|ConversationException.생성자]] — 생성 · L112

### 외부 라이브러리 호출

- `org.springframework.jdbc.core.JdbcTemplate#update(java.lang.String,java.lang.@org.jspecify.annotations.Nullable Object @org.jspecify.annotations.Nullable [])`

### 소스 코드

`src/main/java/com/dodamdodam/backend/conversation/infrastructure/ConversationRepository.java:104`

```java
public void insertLink(UUID securityContextId, UUID conversationId, OffsetDateTime boundAt) {
    try {
        jdbc.update("""
                INSERT INTO conversation_session_links (session_security_id, conversation_id, bound_at)
                VALUES (?, ?, ?)
                """, securityContextId, conversationId, boundAt);
    }
    catch (DataAccessException failure) {
        throw new ConversationException(failure);
    }
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## 생성자 m0d8d2b475ae45c4c

기존 업무 DB 연결과 트랜잭션에 참여하는 JDBC 저장소다.

```java
public ConversationRepository(org.springframework.jdbc.core.JdbcTemplate jdbc)
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 외부 라이브러리 호출

- `java.lang.Object#<init>()`

### 소스 코드

`src/main/java/com/dodamdodam/backend/conversation/infrastructure/ConversationRepository.java:28`

```java
public ConversationRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]
