---
tags: [backend, code-map, auth]
aliases: ["EmailVerificationAccountRepository"]
---
# EmailVerificationAccountRepository

`com.dodamdodam.backend.auth.infrastructure.EmailVerificationAccountRepository`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|시작]] → [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요]] → 이 클래스 · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/90-Reference|전체 참조 자료]]

## 클래스 요약

발급 적격 여부와 PIN 목적에 필요한 현재 계정·PIN 상태를 조회한다.

- 원본: `src/main/java/com/dodamdodam/backend/auth/infrastructure/EmailVerificationAccountRepository.java:10`
- 작성 메서드/생성자 4개 · 자동 생성 0개

## 메서드 목록

| 메서드 | 하는 일 |
| --- | --- |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationAccountRepository#findForUpdate m335c95532feb01f2\|findForUpdate]] | 공개 발급 scope 잠금 뒤 현재 계정을 잠그고 적격 여부와 reset snapshot의 근거를 읽는다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationAccountRepository#lockPin mcfbdc90d46d30c44\|lockPin]] | 계정·세션 잠금을 얻은 호출자 Tx에서 현재 PIN 행을 마지막 상위 row로 잠근다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationAccountRepository#findPinState maa86816375106a0c\|findPinState]] | PIN 원문 없이 현재 세션·이메일·generation·PIN 버전을 같은 DB 조회로 읽는다. |

> [!example]- 생성자·자동 생성 멤버 1개
>
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationAccountRepository#생성자 mbca596d3d1a02c96|public EmailVerificationAccountRepository(JdbcTemplate jdbc)]]

## findForUpdate m335c95532feb01f2

공개 발급 scope 잠금 뒤 현재 계정을 잠그고 적격 여부와 reset snapshot의 근거를 읽는다.

```java
public java.util.Optional<com.dodamdodam.backend.auth.infrastructure.EmailVerificationAccountRepository.AccountState> findForUpdate(java.lang.String email)
```

설정: `@org.springframework.transaction.annotation.Transactional(propagation=MANDATORY)`

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.EmailVerificationIssuanceService#preparePublic m8348ca544b936e88|EmailVerificationIssuanceService.preparePublic]] — 호출 · L82

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationAccountRepository.AccountState#생성자 mffddf04fa94a42b2|AccountState.생성자]] — 생성 · L23

### 외부 라이브러리 호출

- `java.sql.ResultSet#getLong(java.lang.String)`
- `java.sql.ResultSet#getObject(java.lang.String,java.lang.Class<T>)`
- `java.sql.ResultSet#getString(java.lang.String)`
- `java.util.Collection#stream()`
- `java.util.stream.Stream#findFirst()`
- `org.springframework.jdbc.core.JdbcTemplate#query(java.lang.String,org.springframework.jdbc.core.RowMapper<T>,java.lang.@org.jspecify.annotations.Nullable Object @org.jspecify.annotations.Nullable [])`

### 소스 코드

`src/main/java/com/dodamdodam/backend/auth/infrastructure/EmailVerificationAccountRepository.java:20`

```java
@Transactional(propagation = Propagation.MANDATORY)
public Optional<AccountState> findForUpdate(String email) {
    return jdbc.query("SELECT id, email, session_version FROM accounts WHERE email = ? FOR UPDATE",
            (result, row) -> new AccountState(result.getObject("id", UUID.class), result.getString("email"),
                    result.getLong("session_version")), email).stream().findFirst();
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## lockPin mcfbdc90d46d30c44

계정·세션 잠금을 얻은 호출자 Tx에서 현재 PIN 행을 마지막 상위 row로 잠근다.

```java
public void lockPin(java.util.UUID accountId)
```

설정: `@org.springframework.transaction.annotation.Transactional(propagation=MANDATORY)`

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.EmailVerificationIssuanceService#preparePin m330c48cfa5c13034|EmailVerificationIssuanceService.preparePin]] — 호출 · L94

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 외부 라이브러리 호출

- `org.springframework.jdbc.core.JdbcTemplate#queryForList(java.lang.String,java.lang.Class<T>,java.lang.@org.jspecify.annotations.Nullable Object @org.jspecify.annotations.Nullable [])`

### 소스 코드

`src/main/java/com/dodamdodam/backend/auth/infrastructure/EmailVerificationAccountRepository.java:31`

```java
@Transactional(propagation = Propagation.MANDATORY)
public void lockPin(UUID accountId) {
    jdbc.queryForList("SELECT account_id FROM guardian_pins WHERE account_id=? FOR UPDATE", UUID.class, accountId);
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## findPinState maa86816375106a0c

PIN 원문 없이 현재 세션·이메일·generation·PIN 버전을 같은 DB 조회로 읽는다.

```java
public java.util.Optional<com.dodamdodam.backend.auth.infrastructure.EmailVerificationAccountRepository.PinState> findPinState(java.util.UUID accountId, java.util.UUID contextId)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.EmailVerificationIssuanceService#preparePin m330c48cfa5c13034|EmailVerificationIssuanceService.preparePin]] — 호출 · L95
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.EmailVerificationIssuanceService#verifyResponseAccess me8292ce668aadbdf|EmailVerificationIssuanceService.verifyResponseAccess]] — 호출 · L126

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationAccountRepository.PinState#생성자 m70affc768552d379|PinState.생성자]] — 생성 · L43

### 외부 라이브러리 호출

- `java.sql.ResultSet#getLong(java.lang.String)`
- `java.sql.ResultSet#getObject(java.lang.String,java.lang.Class<T>)`
- `java.sql.ResultSet#getString(java.lang.String)`
- `java.util.Collection#stream()`
- `java.util.stream.Stream#findFirst()`
- `org.springframework.jdbc.core.JdbcTemplate#query(java.lang.String,org.springframework.jdbc.core.RowMapper<T>,java.lang.@org.jspecify.annotations.Nullable Object @org.jspecify.annotations.Nullable [])`

### 소스 코드

`src/main/java/com/dodamdodam/backend/auth/infrastructure/EmailVerificationAccountRepository.java:37`

```java
public Optional<PinState> findPinState(UUID accountId, UUID contextId) {
    return jdbc.query("""
            SELECT a.email, a.session_version AS account_version, s.session_version AS context_version,
                   s.setup_generation, s.revoked_at, s.expires_at, p.pin_version, clock_timestamp() AS checked_at
            FROM accounts a JOIN session_security s ON s.account_id=a.id
            LEFT JOIN guardian_pins p ON p.account_id=a.id WHERE a.id=? AND s.id=?
            """, (result, row) -> new PinState(result.getString("email"), result.getLong("account_version"),
            result.getLong("context_version"), result.getLong("setup_generation"),
            result.getObject("revoked_at", OffsetDateTime.class), result.getObject("expires_at", OffsetDateTime.class),
            result.getObject("pin_version", Long.class), result.getObject("checked_at", OffsetDateTime.class)),
            accountId, contextId).stream().findFirst();
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## 생성자 mbca596d3d1a02c96

발급에 필요한 이메일·계정 버전만 읽으며 비밀번호·PIN hash는 조회하지 않는다.

```java
public EmailVerificationAccountRepository(org.springframework.jdbc.core.JdbcTemplate jdbc)
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 외부 라이브러리 호출

- `java.lang.Object#<init>()`

### 소스 코드

`src/main/java/com/dodamdodam/backend/auth/infrastructure/EmailVerificationAccountRepository.java:15`

```java
public EmailVerificationAccountRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]
