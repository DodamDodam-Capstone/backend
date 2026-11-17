---
tags: [backend, code-map, conversation]
aliases: ["ConversationClosureRepository"]
---
# ConversationClosureRepository

`com.dodamdodam.backend.conversation.infrastructure.ConversationClosureRepository`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|시작]] → [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요]] → 이 클래스 · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/90-Reference|전체 참조 자료]]

## 클래스 요약

종료 경계·발화 상태·요약 대기 상태·기존 연결의 복구 기한을 저장한다.

- 원본: `src/main/java/com/dodamdodam/backend/conversation/infrastructure/ConversationClosureRepository.java:13`
- 작성 메서드/생성자 12개 · 자동 생성 0개

## 메서드 목록

| 메서드 | 하는 일 |
| --- | --- |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.infrastructure.ConversationClosureRepository#lockAccount me75547411e9e6805\|lockAccount]] | 계정을 먼저 잠가 다른 문맥의 폐기·버전 변경과 복구 자격 확정이 엇갈리지 않게 한다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.infrastructure.ConversationClosureRepository#findForUpdate m31b37bc252579185\|findForUpdate]] | 자녀 잠금 이후 부모-자식 조합을 확인하며 종료 대상 대화를 잠근다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.infrastructure.ConversationClosureRepository#beginClosing m8c1904cc945fbe62\|beginClosing]] | ACTIVE의 최초 경계만 접수하며 이미 저장된 MANUAL/MIDNIGHT는 덮어쓰지 않는다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.infrastructure.ConversationClosureRepository#lockProcessingTurns macac7a26cd3dcb0a\|lockProcessingTurns]] | 진행 중 발화를 먼저 잠그며 완료 처리와 기다린 뒤의 시각 판정을 직렬화한다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.infrastructure.ConversationClosureRepository#expireProcessing m013d6a27bd537b45\|expireProcessing]] | 원래 기한에 도달한 PROCESSING만 실패시키고 terminal 결과·기한·텍스트는 변경하지 않는다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.infrastructure.ConversationClosureRepository#hasProcessing mbb01ff6c85dec5a2\|hasProcessing]] | 모든 발화 terminal 여부는 실제 DB의 남은 PROCESSING으로 판단한다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.infrastructure.ConversationClosureRepository#hasSummaryInput mc52240ff470ce37e\|hasSummaryInput]] | 실패 발화를 포함해 저장·표시가 허용된 nonnull 텍스트가 하나라도 있는지 확인한다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.infrastructure.ConversationClosureRepository#lockLinks m1f2723814c4ce03e\|lockLinks]] | 복구 대상 확정 전 연결들을 일정한 순서로 잠근다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.infrastructure.ConversationClosureRepository#finishClosing mabcd7eabe63e07b8\|finishClosing]] | 최초 ENDED 전이에서만 실제 완료 시각과 EMPTY/PENDING의 필수 NULL 조합을 기록한다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.infrastructure.ConversationClosureRepository#grantRecovery m7ac5ec5179a9bacb\|grantRecovery]] | 같은 소유 계정의 경계 이전 유효 연결에만 최초 endedAt+600초를 원자적으로 기록한다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.infrastructure.ConversationClosureRepository#requireTransition m3fbf937b913ee91d\|requireTransition]] | 잠근 행의 조건부 전이가 유실되면 성공으로 추정하지 않고 종료 전체를 롤백한다. |

> [!example]- 생성자·자동 생성 멤버 1개
>
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.infrastructure.ConversationClosureRepository#생성자 ma2de0fd1d5104f8b|public ConversationClosureRepository(JdbcTemplate jdbc)]]

## lockAccount me75547411e9e6805

계정을 먼저 잠가 다른 문맥의 폐기·버전 변경과 복구 자격 확정이 엇갈리지 않게 한다.

```java
public boolean lockAccount(java.util.UUID accountId)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationClosureCoordinator#reconcile m5c7d3b9b2dd0cb88|ConversationClosureCoordinator.reconcile]] — 호출 · L36

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.exception.ConversationException#생성자 mdf07793b291a512a|ConversationException.생성자]] — 생성 · L30

### 외부 라이브러리 호출

- `java.util.List#isEmpty()`
- `org.springframework.jdbc.core.JdbcTemplate#queryForList(java.lang.String,java.lang.Class<T>,java.lang.@org.jspecify.annotations.Nullable Object @org.jspecify.annotations.Nullable [])`

### 소스 코드

`src/main/java/com/dodamdodam/backend/conversation/infrastructure/ConversationClosureRepository.java:25`

```java
public boolean lockAccount(UUID accountId) {
    try {
        return !jdbc.queryForList("SELECT id FROM accounts WHERE id = ? FOR UPDATE", UUID.class, accountId).isEmpty();
    }
    catch (DataAccessException failure) {
        throw new ConversationException(failure);
    }
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## findForUpdate m31b37bc252579185

자녀 잠금 이후 부모-자식 조합을 확인하며 종료 대상 대화를 잠근다.

```java
public java.util.Optional<com.dodamdodam.backend.conversation.infrastructure.ConversationClosureRepository.ClosureState> findForUpdate(java.util.UUID childId, java.util.UUID conversationId)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationClosureCoordinator#requireState mbbd5c374803feeec|ConversationClosureCoordinator.requireState]] — 호출 · L69

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.exception.ConversationException#생성자 mdf07793b291a512a|ConversationException.생성자]] — 생성 · L46
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.infrastructure.ConversationClosureRepository.ClosureState#생성자 m5699b272bd502772|ClosureState.생성자]] — 생성 · L40

### 외부 라이브러리 호출

- `java.sql.ResultSet#getObject(java.lang.String,java.lang.Class<T>)`
- `java.sql.ResultSet#getString(java.lang.String)`
- `java.util.Collection#stream()`
- `java.util.stream.Stream#findFirst()`
- `org.springframework.jdbc.core.JdbcTemplate#query(java.lang.String,org.springframework.jdbc.core.RowMapper<T>,java.lang.@org.jspecify.annotations.Nullable Object @org.jspecify.annotations.Nullable [])`

### 소스 코드

`src/main/java/com/dodamdodam/backend/conversation/infrastructure/ConversationClosureRepository.java:35`

```java
public Optional<ClosureState> findForUpdate(UUID childId, UUID conversationId) {
    try {
        return jdbc.query("""
                SELECT id, status, scheduled_end_at, end_requested_at, end_reason, ended_at, summary_status
                FROM conversations WHERE child_id = ? AND id = ? FOR UPDATE
                """, (row, _) -> new ClosureState(row.getObject("id", UUID.class), row.getString("status"),
                row.getObject("scheduled_end_at", OffsetDateTime.class), row.getObject("end_requested_at", OffsetDateTime.class),
                row.getString("end_reason"), row.getObject("ended_at", OffsetDateTime.class), row.getString("summary_status")),
                childId, conversationId).stream().findFirst();
    }
    catch (DataAccessException failure) {
        throw new ConversationException(failure);
    }
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## beginClosing m8c1904cc945fbe62

ACTIVE의 최초 경계만 접수하며 이미 저장된 MANUAL/MIDNIGHT는 덮어쓰지 않는다.

```java
public void beginClosing(java.util.UUID conversationId, java.time.OffsetDateTime boundary, java.lang.String reason)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationClosureCoordinator#reconcile m5c7d3b9b2dd0cb88|ConversationClosureCoordinator.reconcile]] — 호출 · L50

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.exception.ConversationException#생성자 mdf07793b291a512a|ConversationException.생성자]] — 생성 · L59
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.infrastructure.ConversationClosureRepository#requireTransition m3fbf937b913ee91d|ConversationClosureRepository.requireTransition]] — 호출 · L53

### 외부 라이브러리 호출

- `org.springframework.jdbc.core.JdbcTemplate#update(java.lang.String,java.lang.@org.jspecify.annotations.Nullable Object @org.jspecify.annotations.Nullable [])`

### 소스 코드

`src/main/java/com/dodamdodam/backend/conversation/infrastructure/ConversationClosureRepository.java:51`

```java
public void beginClosing(UUID conversationId, OffsetDateTime boundary, String reason) {
    try {
        requireTransition(jdbc.update("""
                UPDATE conversations SET status = 'CLOSING', end_requested_at = ?, end_reason = ?
                WHERE id = ? AND status = 'ACTIVE' AND end_requested_at IS NULL AND end_reason IS NULL
                """, boundary, reason, conversationId));
    }
    catch (DataAccessException failure) {
        throw new ConversationException(failure);
    }
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## lockProcessingTurns macac7a26cd3dcb0a

진행 중 발화를 먼저 잠그며 완료 처리와 기다린 뒤의 시각 판정을 직렬화한다.

```java
public void lockProcessingTurns(java.util.UUID conversationId)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationClosureCoordinator#reconcile m5c7d3b9b2dd0cb88|ConversationClosureCoordinator.reconcile]] — 호출 · L54

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.exception.ConversationException#생성자 mdf07793b291a512a|ConversationException.생성자]] — 생성 · L71

### 외부 라이브러리 호출

- `org.springframework.jdbc.core.JdbcTemplate#queryForList(java.lang.String,java.lang.Class<T>,java.lang.@org.jspecify.annotations.Nullable Object @org.jspecify.annotations.Nullable [])`

### 소스 코드

`src/main/java/com/dodamdodam/backend/conversation/infrastructure/ConversationClosureRepository.java:64`

```java
public void lockProcessingTurns(UUID conversationId) {
    try {
        jdbc.queryForList("""
                SELECT id FROM conversation_turns WHERE conversation_id = ? AND status = 'PROCESSING' ORDER BY id FOR UPDATE
                """, UUID.class, conversationId);
    }
    catch (DataAccessException failure) {
        throw new ConversationException(failure);
    }
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## expireProcessing m013d6a27bd537b45

원래 기한에 도달한 PROCESSING만 실패시키고 terminal 결과·기한·텍스트는 변경하지 않는다.

```java
public void expireProcessing(java.util.UUID conversationId, java.time.OffsetDateTime checkedAt)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationClosureCoordinator#reconcile m5c7d3b9b2dd0cb88|ConversationClosureCoordinator.reconcile]] — 호출 · L55

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.exception.ConversationException#생성자 mdf07793b291a512a|ConversationException.생성자]] — 생성 · L84

### 외부 라이브러리 호출

- `org.springframework.jdbc.core.JdbcTemplate#update(java.lang.String,java.lang.@org.jspecify.annotations.Nullable Object @org.jspecify.annotations.Nullable [])`

### 소스 코드

`src/main/java/com/dodamdodam/backend/conversation/infrastructure/ConversationClosureRepository.java:76`

```java
public void expireProcessing(UUID conversationId, OffsetDateTime checkedAt) {
    try {
        jdbc.update("""
                UPDATE conversation_turns SET status = 'FAILED', error_code = 'AI_TIMEOUT', completed_at = ?
                WHERE conversation_id = ? AND status = 'PROCESSING' AND completed_at IS NULL AND processing_deadline_at <= ?
                """, checkedAt, conversationId, checkedAt);
    }
    catch (DataAccessException failure) {
        throw new ConversationException(failure);
    }
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## hasProcessing mbb01ff6c85dec5a2

모든 발화 terminal 여부는 실제 DB의 남은 PROCESSING으로 판단한다.

```java
public boolean hasProcessing(java.util.UUID conversationId)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationClosureCoordinator#reconcile m5c7d3b9b2dd0cb88|ConversationClosureCoordinator.reconcile]] — 호출 · L56

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.exception.ConversationException#생성자 mdf07793b291a512a|ConversationException.생성자]] — 생성 · L96

### 외부 라이브러리 호출

- `java.lang.Boolean#equals(java.lang.Object)`
- `org.springframework.jdbc.core.JdbcTemplate#queryForObject(java.lang.String,java.lang.Class<T>,java.lang.@org.jspecify.annotations.Nullable Object @org.jspecify.annotations.Nullable [])`

### 소스 코드

`src/main/java/com/dodamdodam/backend/conversation/infrastructure/ConversationClosureRepository.java:89`

```java
public boolean hasProcessing(UUID conversationId) {
    try {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM conversation_turns WHERE conversation_id = ? AND status = 'PROCESSING')
                """, Boolean.class, conversationId));
    }
    catch (DataAccessException failure) {
        throw new ConversationException(failure);
    }
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## hasSummaryInput mc52240ff470ce37e

실패 발화를 포함해 저장·표시가 허용된 nonnull 텍스트가 하나라도 있는지 확인한다.

```java
public boolean hasSummaryInput(java.util.UUID conversationId)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationClosureCoordinator#reconcile m5c7d3b9b2dd0cb88|ConversationClosureCoordinator.reconcile]] — 호출 · L61

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.exception.ConversationException#생성자 mdf07793b291a512a|ConversationException.생성자]] — 생성 · L111

### 외부 라이브러리 호출

- `java.lang.Boolean#equals(java.lang.Object)`
- `org.springframework.jdbc.core.JdbcTemplate#queryForObject(java.lang.String,java.lang.Class<T>,java.lang.@org.jspecify.annotations.Nullable Object @org.jspecify.annotations.Nullable [])`

### 소스 코드

`src/main/java/com/dodamdodam/backend/conversation/infrastructure/ConversationClosureRepository.java:101`

```java
public boolean hasSummaryInput(UUID conversationId) {
    try {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM conversation_turns WHERE conversation_id = ?
                    AND status IN ('SUCCEEDED', 'FAILED')
                    AND ((child_text IS NOT NULL AND child_text_visibility IN ('VISIBLE', 'REDACTED'))
                      OR (reply_text IS NOT NULL AND reply_text_visibility IN ('VISIBLE', 'REDACTED'))))
                """, Boolean.class, conversationId));
    }
    catch (DataAccessException failure) {
        throw new ConversationException(failure);
    }
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## lockLinks m1f2723814c4ce03e

복구 대상 확정 전 연결들을 일정한 순서로 잠근다.

```java
public void lockLinks(java.util.UUID conversationId)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationClosureCoordinator#reconcile m5c7d3b9b2dd0cb88|ConversationClosureCoordinator.reconcile]] — 호출 · L59

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.exception.ConversationException#생성자 mdf07793b291a512a|ConversationException.생성자]] — 생성 · L124

### 외부 라이브러리 호출

- `org.springframework.jdbc.core.JdbcTemplate#queryForList(java.lang.String,java.lang.Class<T>,java.lang.@org.jspecify.annotations.Nullable Object @org.jspecify.annotations.Nullable [])`

### 소스 코드

`src/main/java/com/dodamdodam/backend/conversation/infrastructure/ConversationClosureRepository.java:116`

```java
public void lockLinks(UUID conversationId) {
    try {
        jdbc.queryForList("""
                SELECT session_security_id FROM conversation_session_links
                WHERE conversation_id = ? ORDER BY session_security_id FOR UPDATE
                """, UUID.class, conversationId);
    }
    catch (DataAccessException failure) {
        throw new ConversationException(failure);
    }
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## finishClosing mabcd7eabe63e07b8

최초 ENDED 전이에서만 실제 완료 시각과 EMPTY/PENDING의 필수 NULL 조합을 기록한다.

```java
public void finishClosing(java.util.UUID conversationId, java.time.OffsetDateTime endedAt, java.time.OffsetDateTime summaryDeadline)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationClosureCoordinator#reconcile m5c7d3b9b2dd0cb88|ConversationClosureCoordinator.reconcile]] — 호출 · L62

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.exception.ConversationException#생성자 mdf07793b291a512a|ConversationException.생성자]] — 생성 · L141
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.infrastructure.ConversationClosureRepository#requireTransition m3fbf937b913ee91d|ConversationClosureRepository.requireTransition]] — 호출 · L132

### 외부 라이브러리 호출

- `org.springframework.jdbc.core.JdbcTemplate#update(java.lang.String,java.lang.@org.jspecify.annotations.Nullable Object @org.jspecify.annotations.Nullable [])`

### 소스 코드

`src/main/java/com/dodamdodam/backend/conversation/infrastructure/ConversationClosureRepository.java:129`

```java
public void finishClosing(UUID conversationId, OffsetDateTime endedAt, OffsetDateTime summaryDeadline) {
    try {
        boolean pending = summaryDeadline != null;
        requireTransition(jdbc.update("""
                UPDATE conversations SET status = 'ENDED', ended_at = ?, summary_status = ?,
                    summary_started_at = ?, summary_deadline_at = ?
                WHERE id = ? AND status = 'CLOSING' AND ended_at IS NULL
                    AND NOT EXISTS (SELECT 1 FROM conversation_turns WHERE conversation_id = ? AND status = 'PROCESSING')
                """, endedAt, pending ? "PENDING" : "EMPTY", pending ? endedAt : null, summaryDeadline,
                conversationId, conversationId));
    }
    catch (DataAccessException failure) {
        throw new ConversationException(failure);
    }
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## grantRecovery m7ac5ec5179a9bacb

같은 소유 계정의 경계 이전 유효 연결에만 최초 endedAt+600초를 원자적으로 기록한다.

```java
public void grantRecovery(java.util.UUID childId, java.util.UUID conversationId, java.time.OffsetDateTime boundary, java.time.OffsetDateTime endedAt)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationClosureCoordinator#reconcile m5c7d3b9b2dd0cb88|ConversationClosureCoordinator.reconcile]] — 호출 · L63

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.exception.ConversationException#생성자 mdf07793b291a512a|ConversationException.생성자]] — 생성 · L159

### 외부 라이브러리 호출

- `java.time.OffsetDateTime#plusSeconds(long)`
- `org.springframework.jdbc.core.JdbcTemplate#update(java.lang.String,java.lang.@org.jspecify.annotations.Nullable Object @org.jspecify.annotations.Nullable [])`

### 소스 코드

`src/main/java/com/dodamdodam/backend/conversation/infrastructure/ConversationClosureRepository.java:146`

```java
public void grantRecovery(UUID childId, UUID conversationId, OffsetDateTime boundary, OffsetDateTime endedAt) {
    try {
        jdbc.update("""
                UPDATE conversation_session_links link SET end_recovery_until = ?
                FROM session_security context JOIN accounts account ON account.id = context.account_id
                    JOIN children child ON child.account_id = account.id AND child.id = ?
                WHERE link.conversation_id = ? AND link.session_security_id = context.id
                    AND link.bound_at < ? AND link.end_recovery_until IS NULL
                    AND context.session_version = account.session_version AND context.revoked_at IS NULL
                    AND (context.expires_at IS NULL OR (isfinite(context.expires_at) AND context.expires_at > ?))
                """, endedAt.plusSeconds(600), childId, conversationId, boundary, endedAt);
    }
    catch (DataAccessException failure) {
        throw new ConversationException(failure);
    }
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## requireTransition m3fbf937b913ee91d

잠근 행의 조건부 전이가 유실되면 성공으로 추정하지 않고 종료 전체를 롤백한다.

```java
private void requireTransition(int count)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.infrastructure.ConversationClosureRepository#beginClosing m8c1904cc945fbe62|ConversationClosureRepository.beginClosing]] — 호출 · L53
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.infrastructure.ConversationClosureRepository#finishClosing mabcd7eabe63e07b8|ConversationClosureRepository.finishClosing]] — 호출 · L132

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.exception.ConversationException#생성자 mdf07793b291a512a|ConversationException.생성자]] — 생성 · L166

### 외부 라이브러리 호출

- `java.lang.IllegalStateException#<init>(java.lang.String)`

### 소스 코드

`src/main/java/com/dodamdodam/backend/conversation/infrastructure/ConversationClosureRepository.java:164`

```java
private void requireTransition(int count) {
    if (count != 1) {
        throw new ConversationException(new IllegalStateException("종료 상태 전이를 확정할 수 없습니다."));
    }
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## 생성자 ma2de0fd1d5104f8b

종료 전이·발화 만료·복구 링크를 호출자의 동일 업무 트랜잭션에 묶는다.

```java
public ConversationClosureRepository(org.springframework.jdbc.core.JdbcTemplate jdbc)
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 외부 라이브러리 호출

- `java.lang.Object#<init>()`

### 소스 코드

`src/main/java/com/dodamdodam/backend/conversation/infrastructure/ConversationClosureRepository.java:20`

```java
public ConversationClosureRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]
