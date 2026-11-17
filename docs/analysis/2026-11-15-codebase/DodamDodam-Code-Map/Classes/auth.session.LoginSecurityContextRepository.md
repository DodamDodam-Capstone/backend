---
tags: [backend, code-map, auth]
aliases: ["LoginSecurityContextRepository"]
---
# LoginSecurityContextRepository

`com.dodamdodam.backend.auth.session.LoginSecurityContextRepository`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|시작]] → [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요]] → 이 클래스 · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/90-Reference|전체 참조 자료]]

## 클래스 요약

프레임워크 설정 또는 보조 타입이다. 아래 메서드와 소스에서 연결 위치를 확인한다.

- 원본: `src/main/java/com/dodamdodam/backend/auth/session/LoginSecurityContextRepository.java:8`
- 작성 메서드/생성자 2개 · 자동 생성 0개

## 메서드 목록

| 메서드 | 하는 일 |
| --- | --- |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.LoginSecurityContextRepository#saveContext m554bf261f42a0b0a\|saveContext]] | 아래 구현과 호출 관계에서 확인 |

> [!example]- 생성자·자동 생성 멤버 1개
>
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.LoginSecurityContextRepository#생성자 mddb28a1afb0bf2df|public LoginSecurityContextRepository(SessionLifecycleService lifecycle)]]

## saveContext m554bf261f42a0b0a

아래 원본 코드에서 구현을 확인한다.

```java
public void saveContext(org.springframework.security.core.context.SecurityContext context, jakarta.servlet.http.HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response)
```

설정: `@java.lang.Override`

### 이 메서드를 호출하는 곳

- 프레임워크 진입: Spring Security 문맥 저장

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.SessionLifecycleService#contextSaved mf9c9c28ed7751e2c|SessionLifecycleService.contextSaved]] — 호출 · L19

### 외부 라이브러리 호출

- `org.springframework.security.web.context.HttpSessionSecurityContextRepository#saveContext(org.springframework.security.core.context.SecurityContext,jakarta.servlet.http.HttpServletRequest,jakarta.servlet.http.HttpServletResponse)`

### 소스 코드

`src/main/java/com/dodamdodam/backend/auth/session/LoginSecurityContextRepository.java:16`

```java
@Override
public void saveContext(SecurityContext context, HttpServletRequest request, HttpServletResponse response) {
    super.saveContext(context, request, response);
    lifecycle.contextSaved(request);
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## 생성자 mddb28a1afb0bf2df

아래 원본 코드에서 구현을 확인한다.

```java
public LoginSecurityContextRepository(com.dodamdodam.backend.auth.session.SessionLifecycleService lifecycle)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.config.SessionConfig#securityContextRepository m165f44738512b8d5|SessionConfig.securityContextRepository]] — 생성 · L34

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 외부 라이브러리 호출

- `org.springframework.security.web.context.HttpSessionSecurityContextRepository#<init>()`

### 소스 코드

`src/main/java/com/dodamdodam/backend/auth/session/LoginSecurityContextRepository.java:12`

```java
public LoginSecurityContextRepository(SessionLifecycleService lifecycle) {
    this.lifecycle = lifecycle;
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]
