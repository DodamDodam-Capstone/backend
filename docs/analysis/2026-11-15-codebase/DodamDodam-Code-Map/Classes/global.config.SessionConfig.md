---
tags: [backend, code-map, global]
aliases: ["SessionConfig"]
---
# SessionConfig

`com.dodamdodam.backend.global.config.SessionConfig`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|시작]] → [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/global|기능 개요]] → 이 클래스 · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/90-Reference|전체 참조 자료]]

## 클래스 요약

프레임워크 설정 또는 보조 타입이다. 아래 메서드와 소스에서 연결 위치를 확인한다.

- 원본: `src/main/java/com/dodamdodam/backend/global/config/SessionConfig.java:16`
- 작성 메서드/생성자 3개 · 자동 생성 1개

## 메서드 목록

| 메서드 | 하는 일 |
| --- | --- |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.config.SessionConfig#explicitSessionDeletion md775db20b496637a\|explicitSessionDeletion]] | 아래 구현과 호출 관계에서 확인 |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.config.SessionConfig#loginSessionAuthenticationStrategy m869f6655de6b7c46\|loginSessionAuthenticationStrategy]] | 아래 구현과 호출 관계에서 확인 |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.config.SessionConfig#securityContextRepository m165f44738512b8d5\|securityContextRepository]] | 아래 구현과 호출 관계에서 확인 |

> [!example]- 생성자·자동 생성 멤버 1개
>
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.config.SessionConfig#생성자 m066988239b6c9a00|public SessionConfig() [자동]]]

## explicitSessionDeletion md775db20b496637a

아래 원본 코드에서 구현을 확인한다.

```java
org.springframework.session.config.SessionRepositoryCustomizer<org.springframework.session.jdbc.JdbcIndexedSessionRepository> explicitSessionDeletion()
```

설정: `@org.springframework.context.annotation.Bean`

### 이 메서드를 호출하는 곳

- 프레임워크 진입: Spring 컨테이너 Bean 생성

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 외부 라이브러리 호출

- `org.springframework.session.jdbc.JdbcIndexedSessionRepository#setDeleteSessionQuery(java.lang.String)`

### 소스 코드

`src/main/java/com/dodamdodam/backend/global/config/SessionConfig.java:19`

```java
@Bean
SessionRepositoryCustomizer<JdbcIndexedSessionRepository> explicitSessionDeletion() {
    // 4.1.1 기본 deleteById SQL은 음수 timeout을 제외하므로 명시적 폐기는 ID만으로 삭제한다.
    return repository -> repository.setDeleteSessionQuery("DELETE FROM %TABLE_NAME% WHERE SESSION_ID = ?");
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/global|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## loginSessionAuthenticationStrategy m869f6655de6b7c46

아래 원본 코드에서 구현을 확인한다.

```java
com.dodamdodam.backend.auth.session.LoginSessionAuthenticationStrategy loginSessionAuthenticationStrategy(com.dodamdodam.backend.auth.session.SessionLifecycleService lifecycle, org.springframework.security.web.csrf.CookieCsrfTokenRepository csrfRepository, org.springframework.security.web.csrf.CsrfTokenRequestHandler csrfRequestHandler)
```

설정: `@org.springframework.context.annotation.Bean`

### 이 메서드를 호출하는 곳

- 프레임워크 진입: Spring 컨테이너 Bean 생성

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.LoginSessionAuthenticationStrategy#생성자 mc69a76ba15c64801|LoginSessionAuthenticationStrategy.생성자]] — 생성 · L28

### 소스 코드

`src/main/java/com/dodamdodam/backend/global/config/SessionConfig.java:25`

```java
@Bean
LoginSessionAuthenticationStrategy loginSessionAuthenticationStrategy(SessionLifecycleService lifecycle,
        CookieCsrfTokenRepository csrfRepository, CsrfTokenRequestHandler csrfRequestHandler) {
    return new LoginSessionAuthenticationStrategy(lifecycle, csrfRepository, csrfRequestHandler);
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/global|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## securityContextRepository m165f44738512b8d5

아래 원본 코드에서 구현을 확인한다.

```java
org.springframework.security.web.context.SecurityContextRepository securityContextRepository(com.dodamdodam.backend.auth.session.SessionLifecycleService lifecycle)
```

설정: `@org.springframework.context.annotation.Bean`

### 이 메서드를 호출하는 곳

- 프레임워크 진입: Spring 컨테이너 Bean 생성

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.LoginSecurityContextRepository#생성자 mddb28a1afb0bf2df|LoginSecurityContextRepository.생성자]] — 생성 · L34

### 외부 라이브러리 호출

- `org.springframework.security.web.context.DelegatingSecurityContextRepository#<init>(org.springframework.security.web.context.SecurityContextRepository[])`
- `org.springframework.security.web.context.RequestAttributeSecurityContextRepository#<init>()`

### 소스 코드

`src/main/java/com/dodamdodam/backend/global/config/SessionConfig.java:31`

```java
@Bean
SecurityContextRepository securityContextRepository(SessionLifecycleService lifecycle) {
    return new DelegatingSecurityContextRepository(new RequestAttributeSecurityContextRepository(),
            new LoginSecurityContextRepository(lifecycle));
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/global|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## 생성자 m066988239b6c9a00

컴파일러가 생성한 멤버다.

```java
public SessionConfig()
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 외부 라이브러리 호출

- `java.lang.Object#<init>()`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/global|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]
