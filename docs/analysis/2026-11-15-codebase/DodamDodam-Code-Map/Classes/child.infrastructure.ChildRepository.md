---
tags: [backend, code-map, child]
aliases: ["ChildRepository"]
---
# ChildRepository

`com.dodamdodam.backend.child.infrastructure.ChildRepository`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|시작]] → [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/child|기능 개요]] → 이 클래스 · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/90-Reference|전체 참조 자료]]

## 클래스 요약

children 테이블의 등록·소유권 조회·잠금 조회를 담당한다.

- 원본: `src/main/java/com/dodamdodam/backend/child/infrastructure/ChildRepository.java:19`
- 작성 메서드/생성자 5개 · 자동 생성 0개

## 메서드 목록

| 메서드 | 하는 일 |
| --- | --- |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.infrastructure.ChildRepository#insert mcdb7b197a0d8c26e\|insert]] | 정규화된 프로필을 저장하며 계정당 중복만 빈 결과로 반환한다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.infrastructure.ChildRepository#findOwned m0c455eef6c80ad12\|findOwned]] | 계정과 자녀를 함께 조회해 미소유·미존재를 같은 빈 결과로 처리한다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.infrastructure.ChildRepository#findOwnedForUpdate maf36c957db0ad4a1\|findOwnedForUpdate]] | 현재 업무 트랜잭션에서 소유 자녀 행을 잠가 자녀별 변경을 직렬화한다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.infrastructure.ChildRepository#findById mdfeacb26eaf99e57\|findById]] | 선행 소유권 검사를 마친 내부 문맥 조회에 사용할 프로필을 읽는다. |

> [!example]- 생성자·자동 생성 멤버 1개
>
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.infrastructure.ChildRepository#생성자 m41ae0dedcce4349b|public ChildRepository(JdbcTemplate jdbc)]]

## insert mcdb7b197a0d8c26e

정규화된 프로필을 저장하며 계정당 중복만 빈 결과로 반환한다.

```java
public java.util.Optional<com.dodamdodam.backend.child.dto.ChildView> insert(java.util.UUID accountId, com.dodamdodam.backend.child.dto.CreateChildRequest normalized)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.application.ChildService#create m022c1fb522a0efea|ChildService.create]] — 호출 · L51

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.dto.CreateChildRequest#birthDate m521a841c1b562159|CreateChildRequest.birthDate]] — 호출 · L53
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.dto.CreateChildRequest#characterId m947b336024310718|CreateChildRequest.characterId]] — 호출 · L54
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.dto.CreateChildRequest#gender m0bb25172a292801f|CreateChildRequest.gender]] — 호출 · L53
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.dto.CreateChildRequest#interests m3ec099adee591128|CreateChildRequest.interests]] — 호출 · L54
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.dto.CreateChildRequest#name mea66a08e5a7112b1|CreateChildRequest.name]] — 호출 · L52
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.dto.CreateChildRequest#nickname me107b28c9a6d3bad|CreateChildRequest.nickname]] — 호출 · L52
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.exception.ChildException#생성자 m1309e1738b206625|ChildException.생성자]] — 생성 · L58

### 외부 라이브러리 호출

- `java.lang.Enum#name()`
- `java.util.Collection#stream()`
- `java.util.List#toArray()`
- `java.util.UUID#randomUUID()`
- `java.util.stream.Stream#findFirst()`
- `org.springframework.jdbc.core.JdbcTemplate#query(java.lang.String,org.springframework.jdbc.core.RowMapper<T>,java.lang.@org.jspecify.annotations.Nullable Object @org.jspecify.annotations.Nullable [])`
- `org.springframework.jdbc.support.SqlArrayValue#<init>(java.lang.String,java.lang.Object[])`

### 소스 코드

`src/main/java/com/dodamdodam/backend/child/infrastructure/ChildRepository.java:45`

```java
public Optional<ChildView> insert(UUID accountId, CreateChildRequest normalized) {
    try {
        return jdbc.query("""
                INSERT INTO children (id, account_id, name, nickname, birth_date, gender, interests, character_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (account_id) DO NOTHING
                RETURNING
                """ + COLUMNS, CHILD_ROW, UUID.randomUUID(), accountId, normalized.name(), normalized.nickname(),
                normalized.birthDate(), normalized.gender().name(),
                new SqlArrayValue("text", normalized.interests().toArray()), normalized.characterId())
                .stream().findFirst();
    }
    catch (DataAccessException failure) {
        throw new ChildException(failure);
    }
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/child|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## findOwned m0c455eef6c80ad12

계정과 자녀를 함께 조회해 미소유·미존재를 같은 빈 결과로 처리한다.

```java
public java.util.Optional<com.dodamdodam.backend.child.dto.ChildView> findOwned(java.util.UUID accountId, java.util.UUID childId)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.application.ChildService#requireChildAccess mbb6af419b1ceab47|ChildService.requireChildAccess]] — 호출 · L56

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.exception.ChildException#생성자 m1309e1738b206625|ChildException.생성자]] — 생성 · L69

### 외부 라이브러리 호출

- `java.util.Collection#stream()`
- `java.util.stream.Stream#findFirst()`
- `org.springframework.jdbc.core.JdbcTemplate#query(java.lang.String,org.springframework.jdbc.core.RowMapper<T>,java.lang.@org.jspecify.annotations.Nullable Object @org.jspecify.annotations.Nullable [])`

### 소스 코드

`src/main/java/com/dodamdodam/backend/child/infrastructure/ChildRepository.java:63`

```java
public Optional<ChildView> findOwned(UUID accountId, UUID childId) {
    try {
        return jdbc.query("SELECT " + COLUMNS + " FROM children WHERE id = ? AND account_id = ?",
                CHILD_ROW, childId, accountId).stream().findFirst();
    }
    catch (DataAccessException failure) {
        throw new ChildException(failure);
    }
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/child|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## findOwnedForUpdate maf36c957db0ad4a1

현재 업무 트랜잭션에서 소유 자녀 행을 잠가 자녀별 변경을 직렬화한다.

```java
public java.util.Optional<com.dodamdodam.backend.child.dto.ChildView> findOwnedForUpdate(java.util.UUID accountId, java.util.UUID childId)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.application.ChildService#requireChildAccessForUpdate m10ec0868c9e8ded5|ChildService.requireChildAccessForUpdate]] — 호출 · L62

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.exception.ChildException#생성자 m1309e1738b206625|ChildException.생성자]] — 생성 · L80

### 외부 라이브러리 호출

- `java.util.Collection#stream()`
- `java.util.stream.Stream#findFirst()`
- `org.springframework.jdbc.core.JdbcTemplate#query(java.lang.String,org.springframework.jdbc.core.RowMapper<T>,java.lang.@org.jspecify.annotations.Nullable Object @org.jspecify.annotations.Nullable [])`

### 소스 코드

`src/main/java/com/dodamdodam/backend/child/infrastructure/ChildRepository.java:74`

```java
public Optional<ChildView> findOwnedForUpdate(UUID accountId, UUID childId) {
    try {
        return jdbc.query("SELECT " + COLUMNS + " FROM children WHERE id = ? AND account_id = ? FOR UPDATE",
                CHILD_ROW, childId, accountId).stream().findFirst();
    }
    catch (DataAccessException failure) {
        throw new ChildException(failure);
    }
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/child|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## findById mdfeacb26eaf99e57

선행 소유권 검사를 마친 내부 문맥 조회에 사용할 프로필을 읽는다.

```java
public java.util.Optional<com.dodamdodam.backend.child.dto.ChildView> findById(java.util.UUID childId)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.application.ChildService#getChildContext m6caa8e347baa2700|ChildService.getChildContext]] — 호출 · L67

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.exception.ChildException#생성자 m1309e1738b206625|ChildException.생성자]] — 생성 · L91

### 외부 라이브러리 호출

- `java.util.Collection#stream()`
- `java.util.stream.Stream#findFirst()`
- `org.springframework.jdbc.core.JdbcTemplate#query(java.lang.String,org.springframework.jdbc.core.RowMapper<T>,java.lang.@org.jspecify.annotations.Nullable Object @org.jspecify.annotations.Nullable [])`

### 소스 코드

`src/main/java/com/dodamdodam/backend/child/infrastructure/ChildRepository.java:85`

```java
public Optional<ChildView> findById(UUID childId) {
    try {
        return jdbc.query("SELECT " + COLUMNS + " FROM children WHERE id = ?", CHILD_ROW, childId)
                .stream().findFirst();
    }
    catch (DataAccessException failure) {
        throw new ChildException(failure);
    }
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/child|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## 생성자 m41ae0dedcce4349b

기존 JDBC 연결과 예외 변환을 사용한다.

```java
public ChildRepository(org.springframework.jdbc.core.JdbcTemplate jdbc)
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 외부 라이브러리 호출

- `java.lang.Object#<init>()`

### 소스 코드

`src/main/java/com/dodamdodam/backend/child/infrastructure/ChildRepository.java:40`

```java
public ChildRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/child|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]
