---
tags: [backend, code-map, auth]
aliases: ["PendingLogin"]
---
# PendingLogin

`com.dodamdodam.backend.auth.session.SessionLifecycleService.PendingLogin`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|시작]] → [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요]] → 이 클래스 · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/90-Reference|전체 참조 자료]]

## 클래스 요약

프레임워크 설정 또는 보조 타입이다. 아래 메서드와 소스에서 연결 위치를 확인한다.

- 원본: `src/main/java/com/dodamdodam/backend/auth/session/SessionLifecycleService.java:130`
- 작성 메서드/생성자 1개 · 자동 생성 0개

## 메서드 목록

데이터·설정 타입이다. 아래 목록을 펼쳐 생성자와 접근자를 확인한다.

> [!example]- 생성자·자동 생성 멤버 1개
>
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.SessionLifecycleService.PendingLogin#생성자 m28a9766683ed8489|PendingLogin(AuthenticatedSession context)]]

## 생성자 m28a9766683ed8489

아래 원본 코드에서 구현을 확인한다.

```java
PendingLogin(com.dodamdodam.backend.auth.session.AuthenticatedSession context)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.SessionLifecycleService#beginLogin m86b3ae016950c512|SessionLifecycleService.beginLogin]] — 생성 · L57

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 외부 라이브러리 호출

- `java.lang.Object#<init>()`

### 소스 코드

`src/main/java/com/dodamdodam/backend/auth/session/SessionLifecycleService.java:135`

```java
PendingLogin(AuthenticatedSession context) {
    this.context = context;
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]
