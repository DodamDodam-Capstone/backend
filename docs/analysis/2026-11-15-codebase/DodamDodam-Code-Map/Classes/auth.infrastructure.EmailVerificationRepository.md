---
tags: [backend, code-map, auth]
aliases: ["EmailVerificationRepository"]
---
# EmailVerificationRepository

`com.dodamdodam.backend.auth.infrastructure.EmailVerificationRepository`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|시작]] → [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요]] → 이 클래스 · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/90-Reference|전체 참조 자료]]

## 클래스 요약

이메일 challenge의 잠금·재발급·검증 기반 상태를 저장한다.

- 원본: `src/main/java/com/dodamdodam/backend/auth/infrastructure/EmailVerificationRepository.java:17`
- 작성 메서드/생성자 7개 · 자동 생성 0개

## 메서드 목록

| 메서드 | 하는 일 |
| --- | --- |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationRepository#replace m14c7e6b25ddbfbd4\|replace]] | 호출자가 계정·문맥·PIN 검사와 필요한 잠금을 마친 Tx에서 이전 권한을 교체한다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationRepository#invalidateUnconsumed md4f33dbac51270fb\|invalidateUnconsumed]] | 발송 실패 보상 등의 호출자 Tx에서 해당 발급만 폐기하며 재호출·소비 완료는 그대로 둔다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationRepository#lockScope ma7c40b57ecd4ef2d\|lockScope]] | 최초 발급도 잠근 뒤 계정 판정과 실제 발급 간격을 검사할 수 있게 한다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationRepository#requireResendReady m1cc9b17629706b6b\|requireResendReady]] | scope와 상위 업무 row 잠금 뒤 최신 발급+60초를 재검사해 사전 예약 대기시간을 보완한다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationRepository#isIssueResponseCurrent m1f11b5692acde05b\|isIssueResponseCurrent]] | 응답용 최신 상태만 검사하며 eligible·번호 검증 권한을 대신하지 않는다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationRepository#databaseNow m17bff71a4c1b2e90\|databaseNow]] | 트랜잭션 시작 시각 대신 모든 잠금 대기가 끝난 현재 DB 시각을 읽는다. |

> [!example]- 생성자·자동 생성 멤버 1개
>
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationRepository#생성자 m342a1eae5eb8517f|public EmailVerificationRepository(JdbcTemplate jdbc)]]

## replace m14c7e6b25ddbfbd4

호출자가 계정·문맥·PIN 검사와 필요한 잠금을 마친 Tx에서 이전 권한을 교체한다.

```java
public com.dodamdodam.backend.auth.infrastructure.EmailVerificationRepository.IssuedChallenge replace(com.dodamdodam.backend.auth.infrastructure.EmailVerificationRepository.NewChallenge challenge)
```

설정: `@org.springframework.transaction.annotation.Transactional(propagation=MANDATORY)`

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.EmailVerificationIssuanceService#preparePin m330c48cfa5c13034|EmailVerificationIssuanceService.preparePin]] — 호출 · L115
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.EmailVerificationIssuanceService#preparePublic m8348ca544b936e88|EmailVerificationIssuanceService.preparePublic]] — 호출 · L88

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationRepository#databaseNow m17bff71a4c1b2e90|EmailVerificationRepository.databaseNow]] — 호출 · L45
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationRepository#lockScope ma7c40b57ecd4ef2d|EmailVerificationRepository.lockScope]] — 호출 · L39
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationRepository.IssuedChallenge#생성자 med80a24a306fec04|IssuedChallenge.생성자]] — 생성 · L61
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationRepository.NewChallenge#codeHash mf24c18fb044290af|NewChallenge.codeHash]] — 호출 · L59
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationRepository.NewChallenge#eligible m63887491899caa79|NewChallenge.eligible]] — 호출 · L58
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationRepository.NewChallenge#email m133eec4c2423ba35|NewChallenge.email]] — 호출 · L36,39,58
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationRepository.NewChallenge#id m5a9d6c1170252280|NewChallenge.id]] — 호출 · L58,61
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationRepository.NewChallenge#pinVersionSnapshot mecbbbc9252ef6bfe|NewChallenge.pinVersionSnapshot]] — 호출 · L60
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationRepository.NewChallenge#purpose m1ea1c554e5f2c6ea|NewChallenge.purpose]] — 호출 · L31,35,36,39,58
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationRepository.NewChallenge#resetAccountId m03d8680785b3ffe8|NewChallenge.resetAccountId]] — 호출 · L60
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationRepository.NewChallenge#resetSessionVersion mec616719547a4627|NewChallenge.resetSessionVersion]] — 호출 · L60
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationRepository.NewChallenge#setupAccountId m4e9c4faacdb5993f|NewChallenge.setupAccountId]] — 호출 · L35,39,59
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationRepository.NewChallenge#setupGeneration m82bdbdf8a59a4bb8|NewChallenge.setupGeneration]] — 호출 · L60
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationRepository.NewChallenge#setupSessionBinding m2c0efae75740ba3b|NewChallenge.setupSessionBinding]] — 호출 · L35,39,59
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthStateUnavailableException#생성자 m2ecef5ced435fdbf|AuthStateUnavailableException.생성자]] — 생성 · L64

### 외부 라이브러리 호출

- `java.lang.Enum#name()`
- `java.time.OffsetDateTime#plusSeconds(long)`
- `java.util.ArrayList#<init>(java.util.Collection<? extends E>)`
- `java.util.ArrayList#addFirst(E)`
- `java.util.ArrayList#toArray()`
- `java.util.List#of(E,E)`
- `java.util.List#of(E,E,E)`
- `java.util.List#toArray()`
- `org.springframework.jdbc.core.JdbcTemplate#queryForList(java.lang.String,java.lang.Class<T>,java.lang.@org.jspecify.annotations.Nullable Object @org.jspecify.annotations.Nullable [])`
- `org.springframework.jdbc.core.JdbcTemplate#update(java.lang.String,java.lang.@org.jspecify.annotations.Nullable Object @org.jspecify.annotations.Nullable [])`

### 소스 코드

`src/main/java/com/dodamdodam/backend/auth/infrastructure/EmailVerificationRepository.java:28`

```java
@Transactional(propagation = Propagation.MANDATORY)
public IssuedChallenge replace(NewChallenge challenge) {
    try {
        boolean pinPurpose = challenge.purpose() == Purpose.PIN_SETUP || challenge.purpose() == Purpose.PIN_RESET;
        String predicate = pinPurpose ? "purpose = ? AND setup_account_id = ? AND setup_session_binding = ?"
                : "purpose = ? AND email = ?";
        List<Object> scopeParameters = pinPurpose
                ? List.of(challenge.purpose().name(), challenge.setupAccountId(), challenge.setupSessionBinding())
                : List.of(challenge.purpose().name(), challenge.email());

        // 아직 row가 없는 최초 발급도 같은 범위에서 하나씩 진행한다.
        lockScope(challenge.email(), challenge.purpose(), challenge.setupAccountId(), challenge.setupSessionBinding());

        // 기존 번호와 검증 완료 토큰을 함께 잠그며 소비·기폐기 행은 보존한다.
        String unconsumedScope = predicate + " AND token_consumed_at IS NULL AND invalidated_at IS NULL";
        jdbc.queryForList("SELECT id FROM email_verifications WHERE " + unconsumedScope + " ORDER BY id FOR UPDATE",
                UUID.class, scopeParameters.toArray());
        var issuedAt = databaseNow();
        var expiresAt = issuedAt.plusSeconds(600);
        var invalidationParameters = new ArrayList<>(scopeParameters);
        invalidationParameters.addFirst(issuedAt);
        jdbc.update("UPDATE email_verifications SET invalidated_at = ? WHERE " + unconsumedScope,
                invalidationParameters.toArray());

        jdbc.update("""
                INSERT INTO email_verifications
                    (id, email, purpose, eligible, code_hash, created_at, expires_at,
                     setup_account_id, setup_session_binding, setup_generation, pin_version_snapshot,
                     reset_account_id, reset_session_version)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, challenge.id(), challenge.email(), challenge.purpose().name(), challenge.eligible(),
                challenge.codeHash(), issuedAt, expiresAt, challenge.setupAccountId(), challenge.setupSessionBinding(),
                challenge.setupGeneration(), challenge.pinVersionSnapshot(), challenge.resetAccountId(), challenge.resetSessionVersion());
        return new IssuedChallenge(challenge.id(), expiresAt);
    }
    catch (DataAccessException failure) {
        throw new AuthStateUnavailableException(failure);
    }
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## invalidateUnconsumed md4f33dbac51270fb

발송 실패 보상 등의 호출자 Tx에서 해당 발급만 폐기하며 재호출·소비 완료는 그대로 둔다.

```java
public void invalidateUnconsumed(java.util.UUID challengeId)
```

설정: `@org.springframework.transaction.annotation.Transactional(propagation=MANDATORY)`

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.EmailVerificationDeliveryService#invalidate m2641bd7bf8eedcbb|EmailVerificationDeliveryService.invalidate]] — 호출 · L47

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationRepository#databaseNow m17bff71a4c1b2e90|EmailVerificationRepository.databaseNow]] — 호출 · L73
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthStateUnavailableException#생성자 m2ecef5ced435fdbf|AuthStateUnavailableException.생성자]] — 생성 · L80

### 외부 라이브러리 호출

- `org.springframework.jdbc.core.JdbcTemplate#queryForList(java.lang.String,java.lang.Class<T>,java.lang.@org.jspecify.annotations.Nullable Object @org.jspecify.annotations.Nullable [])`
- `org.springframework.jdbc.core.JdbcTemplate#update(java.lang.String,java.lang.@org.jspecify.annotations.Nullable Object @org.jspecify.annotations.Nullable [])`

### 소스 코드

`src/main/java/com/dodamdodam/backend/auth/infrastructure/EmailVerificationRepository.java:69`

```java
@Transactional(propagation = Propagation.MANDATORY)
public void invalidateUnconsumed(UUID challengeId) {
    try {
        jdbc.queryForList("SELECT id FROM email_verifications WHERE id = ? FOR UPDATE", UUID.class, challengeId);
        var invalidatedAt = databaseNow();
        jdbc.update("""
                UPDATE email_verifications SET invalidated_at = ?
                WHERE id = ? AND token_consumed_at IS NULL AND invalidated_at IS NULL
                """, invalidatedAt, challengeId);
    }
    catch (DataAccessException failure) {
        throw new AuthStateUnavailableException(failure);
    }
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## lockScope ma7c40b57ecd4ef2d

최초 발급도 잠근 뒤 계정 판정과 실제 발급 간격을 검사할 수 있게 한다.

```java
public void lockScope(java.lang.String email, com.dodamdodam.backend.auth.dto.IssueEmailVerificationRequest.Purpose purpose, java.util.UUID accountId, java.util.UUID contextId)
```

설정: `@org.springframework.transaction.annotation.Transactional(propagation=MANDATORY)`

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.EmailVerificationIssuanceService#preparePin m330c48cfa5c13034|EmailVerificationIssuanceService.preparePin]] — 호출 · L110
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.EmailVerificationIssuanceService#preparePublic m8348ca544b936e88|EmailVerificationIssuanceService.preparePublic]] — 호출 · L81
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationRepository#replace m14c7e6b25ddbfbd4|EmailVerificationRepository.replace]] — 호출 · L39

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 외부 라이브러리 호출

- `java.lang.Enum#name()`
- `org.springframework.jdbc.core.JdbcTemplate#queryForObject(java.lang.String,org.springframework.jdbc.core.RowMapper<T>,java.lang.@org.jspecify.annotations.Nullable Object @org.jspecify.annotations.Nullable [])`

### 소스 코드

`src/main/java/com/dodamdodam/backend/auth/infrastructure/EmailVerificationRepository.java:85`

```java
@Transactional(propagation = Propagation.MANDATORY)
public void lockScope(String email, Purpose purpose, UUID accountId, UUID contextId) {
    boolean pin = purpose == Purpose.PIN_SETUP || purpose == Purpose.PIN_RESET;
    String key = pin ? "pin:" + purpose.name() + ":" + accountId + ":" + contextId
            : "public:" + purpose.name() + ":" + email;
    jdbc.queryForObject("SELECT pg_advisory_xact_lock(hashtextextended('email-issuance:' || ?, 0))",
            (result, row) -> true, key);
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## requireResendReady m1cc9b17629706b6b

scope와 상위 업무 row 잠금 뒤 최신 발급+60초를 재검사해 사전 예약 대기시간을 보완한다.

```java
public void requireResendReady(java.lang.String email, com.dodamdodam.backend.auth.dto.IssueEmailVerificationRequest.Purpose purpose, java.util.UUID accountId, java.util.UUID contextId)
```

설정: `@org.springframework.transaction.annotation.Transactional(propagation=MANDATORY)`

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.EmailVerificationIssuanceService#preparePin m330c48cfa5c13034|EmailVerificationIssuanceService.preparePin]] — 호출 · L111
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.EmailVerificationIssuanceService#preparePublic m8348ca544b936e88|EmailVerificationIssuanceService.preparePublic]] — 호출 · L84

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.exception.AuthRateLimitException#생성자 m1856a1b1e165196d|AuthRateLimitException.생성자]] — 생성 · L107
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationRepository#databaseNow m17bff71a4c1b2e90|EmailVerificationRepository.databaseNow]] — 호출 · L103

### 외부 라이브러리 호출

- `java.lang.Enum#name()`
- `java.time.Duration#between(java.time.temporal.Temporal,java.time.temporal.Temporal)`
- `java.time.Duration#getNano()`
- `java.time.Duration#getSeconds()`
- `java.time.OffsetDateTime#isBefore(java.time.OffsetDateTime)`
- `java.time.OffsetDateTime#plusSeconds(long)`
- `java.util.List#getFirst()`
- `java.util.List#isEmpty()`
- `org.springframework.jdbc.core.JdbcTemplate#queryForList(java.lang.String,java.lang.Class<T>,java.lang.@org.jspecify.annotations.Nullable Object @org.jspecify.annotations.Nullable [])`

### 소스 코드

`src/main/java/com/dodamdodam/backend/auth/infrastructure/EmailVerificationRepository.java:95`

```java
@Transactional(propagation = Propagation.MANDATORY)
public void requireResendReady(String email, Purpose purpose, UUID accountId, UUID contextId) {
    boolean pin = purpose == Purpose.PIN_SETUP || purpose == Purpose.PIN_RESET;
    String predicate = pin ? "purpose=? AND setup_account_id=? AND setup_session_binding=?" : "purpose=? AND email=?";
    Object[] arguments = pin ? new Object[]{purpose.name(), accountId, contextId} : new Object[]{purpose.name(), email};
    var times = jdbc.queryForList("SELECT created_at FROM email_verifications WHERE " + predicate
            + " ORDER BY created_at DESC LIMIT 1", OffsetDateTime.class, arguments);
    if (!times.isEmpty()) {
        var now = databaseNow();
        var until = times.getFirst().plusSeconds(60);
        if (now.isBefore(until)) {
            var remaining = Duration.between(now, until);
            throw new AuthRateLimitException(remaining.getSeconds() + (remaining.getNano() == 0 ? 0 : 1));
        }
    }
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## isIssueResponseCurrent m1f11b5692acde05b

응답용 최신 상태만 검사하며 eligible·번호 검증 권한을 대신하지 않는다.

```java
public boolean isIssueResponseCurrent(java.util.UUID challengeId)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.EmailVerificationIssuanceService#verifyResponseAccess me8292ce668aadbdf|EmailVerificationIssuanceService.verifyResponseAccess]] — 호출 · L139

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 외부 라이브러리 호출

- `java.lang.Boolean#equals(java.lang.Object)`
- `java.util.List#getFirst()`
- `java.util.List#isEmpty()`
- `org.springframework.jdbc.core.JdbcTemplate#queryForList(java.lang.String,java.lang.Class<T>,java.lang.@org.jspecify.annotations.Nullable Object @org.jspecify.annotations.Nullable [])`

### 소스 코드

`src/main/java/com/dodamdodam/backend/auth/infrastructure/EmailVerificationRepository.java:113`

```java
public boolean isIssueResponseCurrent(UUID challengeId) {
    var states = jdbc.queryForList("""
            SELECT invalidated_at IS NULL AND token_consumed_at IS NULL AND expires_at > clock_timestamp()
            FROM email_verifications WHERE id=?
            """, Boolean.class, challengeId);
    return !states.isEmpty() && Boolean.TRUE.equals(states.getFirst());
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## databaseNow m17bff71a4c1b2e90

트랜잭션 시작 시각 대신 모든 잠금 대기가 끝난 현재 DB 시각을 읽는다.

```java
private java.time.OffsetDateTime databaseNow()
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationRepository#invalidateUnconsumed md4f33dbac51270fb|EmailVerificationRepository.invalidateUnconsumed]] — 호출 · L73
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationRepository#replace m14c7e6b25ddbfbd4|EmailVerificationRepository.replace]] — 호출 · L45
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationRepository#requireResendReady m1cc9b17629706b6b|EmailVerificationRepository.requireResendReady]] — 호출 · L103

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 외부 라이브러리 호출

- `org.springframework.jdbc.core.JdbcTemplate#queryForObject(java.lang.String,java.lang.Class<T>)`

### 소스 코드

`src/main/java/com/dodamdodam/backend/auth/infrastructure/EmailVerificationRepository.java:122`

```java
private OffsetDateTime databaseNow() {
    return jdbc.queryForObject("SELECT clock_timestamp()", OffsetDateTime.class);
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## 생성자 m342a1eae5eb8517f

기존 email_verifications에 해시와 목적별 권한 snapshot을 저장한다.

```java
public EmailVerificationRepository(org.springframework.jdbc.core.JdbcTemplate jdbc)
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 외부 라이브러리 호출

- `java.lang.Object#<init>()`

### 소스 코드

`src/main/java/com/dodamdodam/backend/auth/infrastructure/EmailVerificationRepository.java:23`

```java
public EmailVerificationRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]
