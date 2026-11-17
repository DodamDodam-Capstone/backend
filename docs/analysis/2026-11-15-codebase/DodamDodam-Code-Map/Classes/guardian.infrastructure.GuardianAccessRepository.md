---
tags: [backend, code-map, guardian]
aliases: ["GuardianAccessRepository"]
---
# GuardianAccessRepository

`com.dodamdodam.backend.guardian.infrastructure.GuardianAccessRepository`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|시작]] → [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/guardian|기능 개요]] → 이 클래스 · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/90-Reference|전체 참조 자료]]

## 클래스 요약

DB 시각을 기준으로 현재 문맥과 보호자 권한 상태를 함께 조회한다.

- 원본: `src/main/java/com/dodamdodam/backend/guardian/infrastructure/GuardianAccessRepository.java:13`
- 작성 메서드/생성자 2개 · 자동 생성 0개

## 메서드 목록

| 메서드 | 하는 일 |
| --- | --- |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/guardian.infrastructure.GuardianAccessRepository#findState mb1e4bd2e6cd4fcaf\|findState]] | 프레임워크 인증 검사 뒤 사용할 현재 문맥·보호자 권한과 DB 시각을 조회한다. |

> [!example]- 생성자·자동 생성 멤버 1개
>
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/guardian.infrastructure.GuardianAccessRepository#생성자 mb0f0cf87359de769|public GuardianAccessRepository(JdbcTemplate jdbc)]]

## findState mb1e4bd2e6cd4fcaf

프레임워크 인증 검사 뒤 사용할 현재 문맥·보호자 권한과 DB 시각을 조회한다.

```java
public java.util.Optional<com.dodamdodam.backend.guardian.infrastructure.GuardianAccessRepository.GuardianAccessState> findState(java.util.UUID accountId, java.util.UUID securityContextId)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/guardian.application.GuardianAccessService#requireCurrentAccess mef6c2129fa7e5585|GuardianAccessService.requireCurrentAccess]] — 호출 · L45

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/guardian.exception.GuardianAccessException#unavailable m0cb219e13da52ebb|GuardianAccessException.unavailable]] — 호출 · L46
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/guardian.infrastructure.GuardianAccessRepository.GuardianAccessState#생성자 m565381b90003821f|GuardianAccessState.생성자]] — 생성 · L34

### 외부 라이브러리 호출

- `java.sql.ResultSet#getLong(java.lang.String)`
- `java.sql.ResultSet#getObject(java.lang.String,java.lang.Class<T>)`
- `java.util.Collection#stream()`
- `java.util.stream.Stream#findFirst()`
- `org.springframework.jdbc.core.JdbcTemplate#query(java.lang.String,org.springframework.jdbc.core.RowMapper<T>,java.lang.@org.jspecify.annotations.Nullable Object @org.jspecify.annotations.Nullable [])`

### 소스 코드

`src/main/java/com/dodamdodam/backend/guardian/infrastructure/GuardianAccessRepository.java:24`

```java
public Optional<GuardianAccessState> findState(UUID accountId, UUID securityContextId) {
    try {
        // PIN 존재와 버전만 읽으며 PIN이 없어도 로그인 문맥은 반환한다.
        var states = jdbc.query("""
                SELECT s.session_version AS context_version, a.session_version AS account_version,
                       s.revoked_at, s.expires_at, s.guardian_unlocked_until, s.guardian_pin_version,
                       p.pin_version AS current_pin_version, clock_timestamp() AS checked_at
                FROM session_security s JOIN accounts a ON a.id = s.account_id
                LEFT JOIN guardian_pins p ON p.account_id = a.id
                WHERE s.id = ? AND s.account_id = ?
                """, (result, row) -> new GuardianAccessState(
                result.getLong("context_version"), result.getLong("account_version"),
                result.getObject("revoked_at", OffsetDateTime.class),
                result.getObject("expires_at", OffsetDateTime.class),
                result.getObject("guardian_unlocked_until", OffsetDateTime.class),
                result.getObject("guardian_pin_version", Long.class),
                result.getObject("current_pin_version", Long.class),
                result.getObject("checked_at", OffsetDateTime.class)), securityContextId, accountId);
        return states.stream().findFirst();
    }
    catch (DataAccessException | TransactionException | ConversionException failure) {
        // 확인 장애를 행 없음이나 PIN 미설정으로 해석해 권한을 우회하지 않는다.
        throw GuardianAccessException.unavailable(failure);
    }
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/guardian|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## 생성자 mb0f0cf87359de769

계정·문맥·PIN 버전을 같은 DB 조회로 확인할 저장소를 구성한다.

```java
public GuardianAccessRepository(org.springframework.jdbc.core.JdbcTemplate jdbc)
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 외부 라이브러리 호출

- `java.lang.Object#<init>()`

### 소스 코드

`src/main/java/com/dodamdodam/backend/guardian/infrastructure/GuardianAccessRepository.java:19`

```java
public GuardianAccessRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/guardian|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]
