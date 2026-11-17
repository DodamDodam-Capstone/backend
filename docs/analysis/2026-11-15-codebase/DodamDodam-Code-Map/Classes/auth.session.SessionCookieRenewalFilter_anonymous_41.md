---
tags: [backend, code-map, auth]
aliases: ["익명 클래스@41"]
---
# 익명 클래스@41

`com.dodamdodam.backend.auth.session.SessionCookieRenewalFilter$anonymous@41`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|시작]] → [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요]] → 이 클래스 · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/90-Reference|전체 참조 자료]]

## 클래스 요약

프레임워크 설정 또는 보조 타입이다. 아래 메서드와 소스에서 연결 위치를 확인한다.

- 원본: `src/main/java/com/dodamdodam/backend/auth/session/SessionCookieRenewalFilter.java:41`
- 작성 메서드/생성자 1개 · 자동 생성 1개

## 메서드 목록

| 메서드 | 하는 일 |
| --- | --- |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.SessionCookieRenewalFilter_anonymous_41#onResponseCommitted m55ab85a891497417\|onResponseCommitted]] | 아래 구현과 호출 관계에서 확인 |

> [!example]- 생성자·자동 생성 멤버 1개
>
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.SessionCookieRenewalFilter_anonymous_41#생성자 m894f8d670cee2a2a|익명 클래스 [자동]]]

## onResponseCommitted m55ab85a891497417

아래 원본 코드에서 구현을 확인한다.

```java
protected void onResponseCommitted()
```

설정: `@java.lang.Override`

### 이 메서드를 호출하는 곳

- 프레임워크 진입: 응답 commit 콜백

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.SessionCookieRenewalFilter#renew me6ea84e6a538169a|SessionCookieRenewalFilter.renew]] — 호출 · L44

### 소스 코드

`src/main/java/com/dodamdodam/backend/auth/session/SessionCookieRenewalFilter.java:42`

```java
@Override
protected void onResponseCommitted() {
    renew(request, response);
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## 생성자 m894f8d670cee2a2a

컴파일러가 생성한 멤버다.

```java
익명 클래스@41(jakarta.servlet.http.HttpServletResponse arg0)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.SessionCookieRenewalFilter#doFilterInternal m5b3b8f1a080501e9|SessionCookieRenewalFilter.doFilterInternal]] — 생성 · L41

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 외부 라이브러리 호출

- `org.springframework.security.web.util.OnCommittedResponseWrapper#<init>(jakarta.servlet.http.HttpServletResponse)`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]
