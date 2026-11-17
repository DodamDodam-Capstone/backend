---
tags: [backend, code-map, auth]
aliases: ["SessionSecurityRepository"]
---
# SessionSecurityRepository

`com.dodamdodam.backend.auth.session.SessionSecurityRepository`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|시작]] → [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요]] → 이 클래스 · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/90-Reference|전체 참조 자료]]

## 클래스 요약

업무 로그인 문맥 생성·유효성 확인·잠금·폐기를 담당한다.

- 원본: `src/main/java/com/dodamdodam/backend/auth/session/SessionSecurityRepository.java:14`
- 작성 메서드/생성자 7개 · 자동 생성 0개

## 메서드 목록

| 메서드 | 하는 일 |
| --- | --- |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.SessionSecurityRepository#createLoginContext m7e32b11c78d3f326\|createLoginContext]] | 아래 구현과 호출 관계에서 확인 |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.SessionSecurityRepository#discardLoginContext m53a371370ce09898\|discardLoginContext]] | 아래 구현과 호출 관계에서 확인 |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.SessionSecurityRepository#isCurrentContext mda2203671fdb2db3\|isCurrentContext]] | 아래 구현과 호출 관계에서 확인 |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.SessionSecurityRepository#requireCurrentContextForUpdate m950184451aa631db\|requireCurrentContextForUpdate]] | 호출자의 트랜잭션에서 계정→문맥 순서로 잠그고 현재 유효성을 재검사한다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.SessionSecurityRepository#revokeCurrentContext ma61a7dc8bfc5d117\|revokeCurrentContext]] | 아래 구현과 호출 관계에서 확인 |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.SessionSecurityRepository#invalidatePinCredentials m1d9b3770323eb783\|invalidatePinCredentials]] | 아래 구현과 호출 관계에서 확인 |

> [!example]- 생성자·자동 생성 멤버 1개
>
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.SessionSecurityRepository#생성자 m0a442ad1078b9ee5|public SessionSecurityRepository(JdbcTemplate jdbcTemplate)]]

## createLoginContext m7e32b11c78d3f326

아래 원본 코드에서 구현을 확인한다.

```java
public com.dodamdodam.backend.auth.session.AuthenticatedSession createLoginContext(java.util.UUID accountId, com.dodamdodam.backend.auth.session.AuthenticatedSession previous)
```

설정: `@org.springframework.transaction.annotation.Transactional(propagation=REQUIRES_NEW)`

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.SessionLifecycleService#beginLogin m86b3ae016950c512|SessionLifecycleService.beginLogin]] — 호출 · L56

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthenticatedSession#생성자 mf3197381afe1fc7a|AuthenticatedSession.생성자]] — 생성 · L39
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthenticatedSession#accountId m23b42fff4f806999|AuthenticatedSession.accountId]] — 호출 · L28,33
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthenticatedSession#securityContextId me6d8be795777e536|AuthenticatedSession.securityContextId]] — 호출 · L33
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.SessionSecurityRepository#revokeCurrentContext ma61a7dc8bfc5d117|SessionSecurityRepository.revokeCurrentContext]] — 호출 · L33

### 외부 라이브러리 호출

- `java.util.Collection#stream()`
- `java.util.Map#get(java.lang.Object)`
- `java.util.Optional#orElseThrow(java.util.function.Supplier<? extends X>)`
- `java.util.UUID#equals(java.lang.Object)`
- `java.util.UUID#randomUUID()`
- `java.util.stream.Stream#filter(java.util.function.Predicate<? super T>)`
- `java.util.stream.Stream#findFirst()`
- `java.util.stream.Stream#map(java.util.function.Function<? super T,? extends R>)`
- `org.springframework.jdbc.core.JdbcTemplate#queryForList(java.lang.String,java.lang.@org.jspecify.annotations.Nullable Object @org.jspecify.annotations.Nullable [])`
- `org.springframework.jdbc.core.JdbcTemplate#update(java.lang.String,java.lang.@org.jspecify.annotations.Nullable Object @org.jspecify.annotations.Nullable [])`
- `org.springframework.security.authentication.InsufficientAuthenticationException#<init>(java.lang.String)`

### 소스 코드

`src/main/java/com/dodamdodam/backend/auth/session/SessionSecurityRepository.java:23`

```java
@Transactional(propagation = Propagation.REQUIRES_NEW)
public AuthenticatedSession createLoginContext(UUID accountId, AuthenticatedSession previous) {
    // 계정 간 재로그인도 같은 순서로 잠가 서로 역순으로 기다리지 않게 한다.
    var accounts = jdbcTemplate.queryForList("""
            SELECT id, session_version FROM accounts WHERE id IN (?, ?) ORDER BY id FOR UPDATE
            """, accountId, previous == null ? accountId : previous.accountId());
    var version = accounts.stream().filter(row -> accountId.equals(row.get("id")))
            .map(row -> (Long) row.get("session_version")).findFirst()
            .orElseThrow(() -> new InsufficientAuthenticationException("로그인 계정을 확인할 수 없습니다."));
    if (previous != null) {
        revokeCurrentContext(previous.accountId(), previous.securityContextId());
    }
    var contextId = UUID.randomUUID();
    jdbcTemplate.update("""
            INSERT INTO session_security (id, account_id, session_version, expires_at) VALUES (?, ?, ?, NULL)
            """, contextId, accountId, version);
    return new AuthenticatedSession(accountId, contextId);
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## discardLoginContext m53a371370ce09898

아래 원본 코드에서 구현을 확인한다.

```java
public void discardLoginContext(com.dodamdodam.backend.auth.session.AuthenticatedSession context)
```

설정: `@org.springframework.transaction.annotation.Transactional(propagation=REQUIRES_NEW)`

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.SessionLifecycleService#failLogin m8a418625ae91ad24|SessionLifecycleService.failLogin]] — 호출 · L112

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthenticatedSession#accountId m23b42fff4f806999|AuthenticatedSession.accountId]] — 호출 · L44,54,55
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthenticatedSession#securityContextId me6d8be795777e536|AuthenticatedSession.securityContextId]] — 호출 · L44,54,55
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.SessionSecurityRepository#invalidatePinCredentials m1d9b3770323eb783|SessionSecurityRepository.invalidatePinCredentials]] — 호출 · L55
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.SessionSecurityRepository#revokeCurrentContext ma61a7dc8bfc5d117|SessionSecurityRepository.revokeCurrentContext]] — 호출 · L44

### 외부 라이브러리 호출

- `java.sql.ResultSet#getObject(int,java.lang.Class<T>)`
- `org.springframework.jdbc.core.JdbcTemplate#queryForObject(java.lang.String,org.springframework.jdbc.core.RowMapper<T>)`
- `org.springframework.jdbc.core.JdbcTemplate#update(java.lang.String,java.lang.@org.jspecify.annotations.Nullable Object @org.jspecify.annotations.Nullable [])`

### 소스 코드

`src/main/java/com/dodamdodam/backend/auth/session/SessionSecurityRepository.java:42`

```java
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void discardLoginContext(AuthenticatedSession context) {
    if (revokeCurrentContext(context.accountId(), context.securityContextId())) {
        return;
    }
    // 저장 중 버전이 바뀐 문맥도 보상 폐기한다. 이미 기록된 폐기 시각은 유지한다.
    var revokedAt = jdbcTemplate.queryForObject("SELECT clock_timestamp()",
            (result, row) -> result.getObject(1, OffsetDateTime.class));
    jdbcTemplate.update("""
            UPDATE session_security
            SET revoked_at = COALESCE(revoked_at, ?), guardian_unlocked_until = NULL, guardian_pin_version = NULL
            WHERE id = ? AND account_id = ?
            """, revokedAt, context.securityContextId(), context.accountId());
    invalidatePinCredentials(context.accountId(), context.securityContextId(), revokedAt);
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## isCurrentContext mda2203671fdb2db3

아래 원본 코드에서 구현을 확인한다.

```java
public boolean isCurrentContext(java.util.UUID accountId, java.util.UUID securityContextId)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthenticatedSessionService#validate m72fbb73c9b868baa|AuthenticatedSessionService.validate]] — 호출 · L97
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.SessionSecurityRepository#requireCurrentContextForUpdate m950184451aa631db|SessionSecurityRepository.requireCurrentContextForUpdate]] — 호출 · L92

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.SessionSecurityRepository.SessionValidity#생성자 m630c889182c65473|SessionValidity.생성자]] — 생성 · L64
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.SessionSecurityRepository.SessionValidity#isValid mc39155fcbb025abe|SessionValidity.isValid]] — 호출 · L69

### 외부 라이브러리 호출

- `java.sql.ResultSet#getLong(java.lang.String)`
- `java.sql.ResultSet#getObject(java.lang.String,java.lang.Class<T>)`
- `java.util.List#getFirst()`
- `java.util.List#isEmpty()`
- `org.springframework.jdbc.core.JdbcTemplate#query(java.lang.String,org.springframework.jdbc.core.RowMapper<T>,java.lang.@org.jspecify.annotations.Nullable Object @org.jspecify.annotations.Nullable [])`

### 소스 코드

`src/main/java/com/dodamdodam/backend/auth/session/SessionSecurityRepository.java:58`

```java
public boolean isCurrentContext(UUID accountId, UUID securityContextId) {
    var states = jdbcTemplate.query("""
            SELECT s.session_version, a.session_version AS account_version,
                   s.revoked_at, s.expires_at, clock_timestamp() AS checked_at
            FROM session_security s JOIN accounts a ON a.id = s.account_id
            WHERE s.id = ? AND s.account_id = ?
            """, (result, row) -> new SessionValidity(
            result.getLong("session_version"), result.getLong("account_version"),
            result.getObject("revoked_at", OffsetDateTime.class),
            result.getObject("expires_at", OffsetDateTime.class),
            result.getObject("checked_at", OffsetDateTime.class)), securityContextId, accountId);
    return !states.isEmpty() && states.getFirst().isValid();
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## requireCurrentContextForUpdate m950184451aa631db

호출자의 트랜잭션에서 계정→문맥 순서로 잠그고 현재 유효성을 재검사한다.

```java
public void requireCurrentContextForUpdate(java.util.UUID accountId, java.util.UUID securityContextId)
```

설정: `@org.springframework.transaction.annotation.Transactional(propagation=MANDATORY)`

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.EmailVerificationIssuanceService#preparePin m330c48cfa5c13034|EmailVerificationIssuanceService.preparePin]] — 호출 · L93
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.application.ChildService#create m022c1fb522a0efea|ChildService.create]] — 호출 · L49
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationService#decideStart m074dea56e9276dfa|ConversationService.decideStart]] — 호출 · L84,91,99
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationService#replay m8a9b848e75e75181|ConversationService.replay]] — 호출 · L141
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationService#verifyStartResponse macdb3aca10ad50df|ConversationService.verifyStartResponse]] — 호출 · L53,57,63

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthStateUnavailableException#생성자 m2ecef5ced435fdbf|AuthStateUnavailableException.생성자]] — 생성 · L98
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.SessionSecurityRepository#isCurrentContext mda2203671fdb2db3|SessionSecurityRepository.isCurrentContext]] — 호출 · L92

### 외부 라이브러리 호출

- `java.util.List#isEmpty()`
- `org.springframework.jdbc.core.JdbcTemplate#queryForList(java.lang.String,java.lang.Class<T>,java.lang.@org.jspecify.annotations.Nullable Object @org.jspecify.annotations.Nullable [])`
- `org.springframework.security.authentication.InsufficientAuthenticationException#<init>(java.lang.String)`

### 소스 코드

`src/main/java/com/dodamdodam/backend/auth/session/SessionSecurityRepository.java:81`

```java
@Transactional(propagation = Propagation.MANDATORY)
public void requireCurrentContextForUpdate(UUID accountId, UUID securityContextId) {
    try {
        var accounts = jdbcTemplate.queryForList("SELECT id FROM accounts WHERE id = ? FOR UPDATE", UUID.class, accountId);
        if (accounts.isEmpty()) {
            throw new InsufficientAuthenticationException("로그인이 필요합니다.");
        }
        var contexts = jdbcTemplate.queryForList("""
                SELECT id FROM session_security WHERE id = ? AND account_id = ? FOR UPDATE
                """, UUID.class, securityContextId, accountId);
        // 잠금 대기가 끝난 뒤 기존 규칙과 새 DB 시각으로 다시 확인한다.
        if (contexts.isEmpty() || !isCurrentContext(accountId, securityContextId)) {
            throw new InsufficientAuthenticationException("로그인이 필요합니다.");
        }
    }
    catch (DataAccessException | TransactionException | ConversionException failure) {
        // 자녀 업무 트랜잭션 전체가 아니라 인증 검사 실패만 구분한다.
        throw new AuthStateUnavailableException(failure);
    }
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## revokeCurrentContext ma61a7dc8bfc5d117

아래 원본 코드에서 구현을 확인한다.

```java
public boolean revokeCurrentContext(java.util.UUID accountId, java.util.UUID securityContextId)
```

설정: `@org.springframework.transaction.annotation.Transactional`

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.api.AuthLogoutController#logout m43cbd6436794ba0f|AuthLogoutController.logout]] — 호출 · L50
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.SessionSecurityRepository#createLoginContext m7e32b11c78d3f326|SessionSecurityRepository.createLoginContext]] — 호출 · L33
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.SessionSecurityRepository#discardLoginContext m53a371370ce09898|SessionSecurityRepository.discardLoginContext]] — 호출 · L44

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.SessionSecurityRepository#invalidatePinCredentials m1d9b3770323eb783|SessionSecurityRepository.invalidatePinCredentials]] — 호출 · L135

### 외부 라이브러리 호출

- `java.sql.ResultSet#getObject(int,java.lang.Class<T>)`
- `java.util.List#getFirst()`
- `java.util.List#isEmpty()`
- `org.springframework.jdbc.core.JdbcTemplate#queryForList(java.lang.String,java.lang.Class<T>,java.lang.@org.jspecify.annotations.Nullable Object @org.jspecify.annotations.Nullable [])`
- `org.springframework.jdbc.core.JdbcTemplate#queryForObject(java.lang.String,org.springframework.jdbc.core.RowMapper<T>)`
- `org.springframework.jdbc.core.JdbcTemplate#update(java.lang.String,java.lang.@org.jspecify.annotations.Nullable Object @org.jspecify.annotations.Nullable [])`

### 소스 코드

`src/main/java/com/dodamdodam/backend/auth/session/SessionSecurityRepository.java:102`

```java
@Transactional
public boolean revokeCurrentContext(UUID accountId, UUID securityContextId) {
    var accountVersions = jdbcTemplate.queryForList(
            "SELECT session_version FROM accounts WHERE id = ? FOR UPDATE", Long.class, accountId);
    if (accountVersions.isEmpty()) {
        return false;
    }
    var contexts = jdbcTemplate.queryForList("""
            SELECT id FROM session_security WHERE id = ? AND account_id = ? FOR UPDATE
            """, UUID.class, securityContextId, accountId);
    if (contexts.isEmpty()) {
        return false;
    }
    jdbcTemplate.queryForList("""
            SELECT id FROM email_verifications
            WHERE setup_account_id = ? AND setup_session_binding = ?
              AND purpose IN ('PIN_SETUP', 'PIN_RESET')
              AND token_consumed_at IS NULL AND invalidated_at IS NULL
            ORDER BY id FOR UPDATE
            """, UUID.class, accountId, securityContextId);

    // 잠금을 기다린 시간을 반영하도록 모든 잠금 뒤 DB 시각을 읽는다.
    var revokedAt = jdbcTemplate.queryForObject("SELECT clock_timestamp()",
            (result, row) -> result.getObject(1, OffsetDateTime.class));
    int updated = jdbcTemplate.update("""
            UPDATE session_security
            SET revoked_at = ?, guardian_unlocked_until = NULL, guardian_pin_version = NULL
            WHERE id = ? AND account_id = ? AND session_version = ? AND revoked_at IS NULL
              AND (expires_at IS NULL OR expires_at > ?)
            """, revokedAt, securityContextId, accountId, accountVersions.getFirst(), revokedAt);
    if (updated == 0) {
        return false;
    }
    invalidatePinCredentials(accountId, securityContextId, revokedAt);
    return true;
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## invalidatePinCredentials m1d9b3770323eb783

아래 원본 코드에서 구현을 확인한다.

```java
private void invalidatePinCredentials(java.util.UUID accountId, java.util.UUID securityContextId, java.time.OffsetDateTime revokedAt)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.SessionSecurityRepository#discardLoginContext m53a371370ce09898|SessionSecurityRepository.discardLoginContext]] — 호출 · L55
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.SessionSecurityRepository#revokeCurrentContext ma61a7dc8bfc5d117|SessionSecurityRepository.revokeCurrentContext]] — 호출 · L135

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 외부 라이브러리 호출

- `org.springframework.jdbc.core.JdbcTemplate#update(java.lang.String,java.lang.@org.jspecify.annotations.Nullable Object @org.jspecify.annotations.Nullable [])`

### 소스 코드

`src/main/java/com/dodamdodam/backend/auth/session/SessionSecurityRepository.java:139`

```java
private void invalidatePinCredentials(UUID accountId, UUID securityContextId, OffsetDateTime revokedAt) {
    jdbcTemplate.update("""
            UPDATE email_verifications SET invalidated_at = ?
            WHERE setup_account_id = ? AND setup_session_binding = ?
              AND purpose IN ('PIN_SETUP', 'PIN_RESET')
              AND token_consumed_at IS NULL AND invalidated_at IS NULL
            """, revokedAt, accountId, securityContextId);
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## 생성자 m0a442ad1078b9ee5

아래 원본 코드에서 구현을 확인한다.

```java
public SessionSecurityRepository(org.springframework.jdbc.core.JdbcTemplate jdbcTemplate)
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 외부 라이브러리 호출

- `java.lang.Object#<init>()`

### 소스 코드

`src/main/java/com/dodamdodam/backend/auth/session/SessionSecurityRepository.java:19`

```java
public SessionSecurityRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]
