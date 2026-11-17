---
tags: [backend, code-map, auth]
aliases: ["AuthRateLimitRepository"]
---
# AuthRateLimitRepository

`com.dodamdodam.backend.auth.infrastructure.AuthRateLimitRepository`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|시작]] → [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요]] → 이 클래스 · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/90-Reference|전체 참조 자료]]

## 클래스 요약

요청 제한 키를 잠그고 시간 창별 사용량을 저장한다.

- 원본: `src/main/java/com/dodamdodam/backend/auth/infrastructure/AuthRateLimitRepository.java:17`
- 작성 메서드/생성자 3개 · 자동 생성 0개

## 메서드 목록

| 메서드 | 하는 일 |
| --- | --- |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.AuthRateLimitRepository#reserve m5db21fe66e21f71b\|reserve]] | 호출자 Tx에서 한 단계의 모든 키를 예약하며 거부 시 쓰지 않고 재시도 초를 반환한다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.AuthRateLimitRepository#reserveAllowed m4983e5d4a84a95d9\|reserveAllowed]] | 모든 제한이 허용된 뒤에만 count를 쓰며 기존 창·차단을 임의로 연장하지 않는다. |

> [!example]- 생성자·자동 생성 멤버 1개
>
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.AuthRateLimitRepository#생성자 mcc5c9bb86255b02d|public AuthRateLimitRepository(JdbcTemplate jdbc)]]

## reserve m5db21fe66e21f71b

호출자 Tx에서 한 단계의 모든 키를 예약하며 거부 시 쓰지 않고 재시도 초를 반환한다.

```java
public long reserve(java.util.List<com.dodamdodam.backend.auth.domain.AuthRateLimitKey> keys)
```

설정: `@org.springframework.transaction.annotation.Transactional(propagation=MANDATORY)`

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.AuthRateLimitService#reserve m63e1737a4e336ba7|AuthRateLimitService.reserve]] — 호출 · L34

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.domain.AuthRateLimitKey#action m3bb673cd9501adf1|AuthRateLimitKey.action]] — 호출 · L38,51,60
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.domain.AuthRateLimitKey#keyHash m1c77dca4d2e2810f|AuthRateLimitKey.keyHash]] — 호출 · L38,51
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.AuthRateLimitRepository#reserveAllowed m4983e5d4a84a95d9|AuthRateLimitRepository.reserveAllowed]] — 호출 · L67
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.AuthRateLimitRepository.RateLimitState#생성자 m0aed0339d96e0b77|RateLimitState.생성자]] — 생성 · L49
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.AuthRateLimitRepository.RateLimitState#retryAfterSeconds ma0bf3b2a05b8fb05|RateLimitState.retryAfterSeconds]] — 호출 · L60
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthStateUnavailableException#생성자 m03e9c81ea8cf5421|AuthStateUnavailableException.생성자]] — 생성 · L31,36

### 외부 라이브러리 호출

- `java.lang.Enum#name()`
- `java.lang.Math#max(long,long)`
- `java.sql.ResultSet#getLong(java.lang.String)`
- `java.sql.ResultSet#getObject(java.lang.String,java.lang.Class<T>)`
- `java.util.HashMap#put(K,V)`
- `java.util.HexFormat#formatHex(byte[])`
- `java.util.HexFormat#of()`
- `java.util.LinkedHashMap#<init>()`
- `java.util.LinkedHashMap#entrySet()`
- `java.util.List#getFirst()`
- `java.util.List#isEmpty()`
- `java.util.Map.Entry#getKey()`
- `java.util.Map.Entry#getValue()`
- `java.util.TreeMap#<init>()`
- `java.util.TreeMap#entrySet()`
- `java.util.TreeMap#put(K,V)`
- `org.springframework.jdbc.core.JdbcTemplate#query(java.lang.String,org.springframework.jdbc.core.RowMapper<T>,java.lang.@org.jspecify.annotations.Nullable Object @org.jspecify.annotations.Nullable [])`
- `org.springframework.jdbc.core.JdbcTemplate#queryForObject(java.lang.String,java.lang.Class<T>)`
- `org.springframework.jdbc.core.JdbcTemplate#queryForObject(java.lang.String,org.springframework.jdbc.core.RowMapper<T>,java.lang.@org.jspecify.annotations.Nullable Object @org.jspecify.annotations.Nullable [])`

### 소스 코드

`src/main/java/com/dodamdodam/backend/auth/infrastructure/AuthRateLimitRepository.java:28`

```java
@Transactional(propagation = Propagation.MANDATORY)
public long reserve(List<AuthRateLimitKey> keys) {
    if (keys == null || keys.isEmpty()) {
        throw new AuthStateUnavailableException();
    }
    var ordered = new TreeMap<String, AuthRateLimitKey>();
    for (var key : keys) {
        if (key == null) {
            throw new AuthStateUnavailableException();
        }
        ordered.put(key.action().name() + ":" + HexFormat.of().formatHex(key.keyHash()), key);
    }
    var states = new LinkedHashMap<AuthRateLimitKey, RateLimitState>();
    for (var entry : ordered.entrySet()) {
        // 최초 row 생성도 직렬화하고 모든 요청이 같은 순서로 잠금을 얻는다.
        jdbc.queryForObject("SELECT pg_advisory_xact_lock(hashtextextended('auth-rate:' || ?, 0))",
                (result, row) -> true, entry.getKey());
        var key = entry.getValue();
        var rows = jdbc.query("""
                SELECT window_started_at, attempt_count, blocked_until FROM auth_rate_limits
                WHERE action = ? AND key_hash = ? FOR UPDATE
                """, (result, row) -> new RateLimitState(
                result.getObject("window_started_at", OffsetDateTime.class), result.getLong("attempt_count"),
                result.getObject("blocked_until", OffsetDateTime.class)), key.action().name(), key.keyHash());
        states.put(key, rows.isEmpty() ? null : rows.getFirst());
    }

    // 모든 잠금 대기 뒤 한 DB 시각으로 판정해 여러 키의 경계를 일치시킨다.
    var checkedAt = jdbc.queryForObject("SELECT clock_timestamp()", OffsetDateTime.class);
    long retryAfter = 0;
    for (var entry : states.entrySet()) {
        if (entry.getValue() != null) {
            retryAfter = Math.max(retryAfter, entry.getValue().retryAfterSeconds(entry.getKey().action(), checkedAt));
        }
    }
    if (retryAfter > 0) {
        return retryAfter;
    }
    for (var entry : states.entrySet()) {
        reserveAllowed(entry.getKey(), entry.getValue(), checkedAt);
    }
    return 0;
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## reserveAllowed m4983e5d4a84a95d9

모든 제한이 허용된 뒤에만 count를 쓰며 기존 창·차단을 임의로 연장하지 않는다.

```java
private void reserveAllowed(com.dodamdodam.backend.auth.domain.AuthRateLimitKey key, com.dodamdodam.backend.auth.infrastructure.AuthRateLimitRepository.RateLimitState state, java.time.OffsetDateTime checkedAt)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.AuthRateLimitRepository#reserve m5db21fe66e21f71b|AuthRateLimitRepository.reserve]] — 호출 · L67

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.domain.AuthRateLimitKey#action m3bb673cd9501adf1|AuthRateLimitKey.action]] — 호출 · L78,80,84,88
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.domain.AuthRateLimitKey#keyHash m1c77dca4d2e2810f|AuthRateLimitKey.keyHash]] — 호출 · L78,84,88
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.domain.AuthRateLimitKey.Action#windowSeconds ma2abc09be0cef384|Action.windowSeconds]] — 호출 · L78,84
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.AuthRateLimitRepository.RateLimitState#windowFinished md9f40a5f486b9ab3|RateLimitState.windowFinished]] — 호출 · L80

### 외부 라이브러리 호출

- `java.lang.Enum#name()`
- `java.time.OffsetDateTime#plusSeconds(long)`
- `org.springframework.jdbc.core.JdbcTemplate#update(java.lang.String,java.lang.@org.jspecify.annotations.Nullable Object @org.jspecify.annotations.Nullable [])`

### 소스 코드

`src/main/java/com/dodamdodam/backend/auth/infrastructure/AuthRateLimitRepository.java:73`

```java
private void reserveAllowed(AuthRateLimitKey key, RateLimitState state, OffsetDateTime checkedAt) {
    if (state == null) {
        jdbc.update("""
                INSERT INTO auth_rate_limits (action, key_hash, window_started_at, attempt_count, expires_at)
                VALUES (?, ?, ?, 1, ?)
                """, key.action().name(), key.keyHash(), checkedAt, checkedAt.plusSeconds(key.action().windowSeconds()));
    }
    else if (state.windowFinished(key.action(), checkedAt)) {
        jdbc.update("""
                UPDATE auth_rate_limits SET window_started_at = ?, attempt_count = 1,
                    blocked_until = NULL, expires_at = ? WHERE action = ? AND key_hash = ?
                """, checkedAt, checkedAt.plusSeconds(key.action().windowSeconds()), key.action().name(), key.keyHash());
    }
    else {
        jdbc.update("UPDATE auth_rate_limits SET attempt_count = attempt_count + 1 WHERE action = ? AND key_hash = ?",
                key.action().name(), key.keyHash());
    }
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## 생성자 mcc5c9bb86255b02d

동일 DB의 인스턴스들이 요청 예산을 공유하는 저장소를 구성한다.

```java
public AuthRateLimitRepository(org.springframework.jdbc.core.JdbcTemplate jdbc)
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 외부 라이브러리 호출

- `java.lang.Object#<init>()`

### 소스 코드

`src/main/java/com/dodamdodam/backend/auth/infrastructure/AuthRateLimitRepository.java:23`

```java
public AuthRateLimitRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]
