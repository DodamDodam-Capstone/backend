---
tags: [backend, code-map, auth]
aliases: ["익명 클래스@52"]
---
# 익명 클래스@52

`com.dodamdodam.backend.auth.session.AuthStateErrorFilter$anonymous@52`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|시작]] → [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요]] → 이 클래스 · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/90-Reference|전체 참조 자료]]

## 클래스 요약

native 저장·쿠키 처리 뒤 실제 전송 전에 현재 권한을 확인한다.

- 원본: `src/main/java/com/dodamdodam/backend/auth/session/AuthStateErrorFilter.java:52`
- 작성 메서드/생성자 1개 · 자동 생성 1개

## 메서드 목록

| 메서드 | 하는 일 |
| --- | --- |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthStateErrorFilter_anonymous_52#onResponseCommitted mada22b8cbf51ec3e\|onResponseCommitted]] | native 저장·쿠키 처리 뒤 실제 전송 전에 현재 권한을 확인한다. |

> [!example]- 생성자·자동 생성 멤버 1개
>
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthStateErrorFilter_anonymous_52#생성자 m0d54bee888c1dbbc|익명 클래스 [자동]]]

## onResponseCommitted mada22b8cbf51ec3e

native 저장·쿠키 처리 뒤 실제 전송 전에 현재 권한을 확인한다.

```java
protected void onResponseCommitted()
```

설정: `@java.lang.Override`

### 이 메서드를 호출하는 곳

- 프레임워크 진입: 응답 commit 콜백

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthStateErrorFilter#verifyResponseAccess m1098528f38d3ff11|AuthStateErrorFilter.verifyResponseAccess]] — 호출 · L56

### 소스 코드

`src/main/java/com/dodamdodam/backend/auth/session/AuthStateErrorFilter.java:54`

```java
@Override
protected void onResponseCommitted() {
    verifyResponseAccess(request, response);
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## 생성자 m0d54bee888c1dbbc

컴파일러가 생성한 멤버다.

```java
익명 클래스@52(jakarta.servlet.http.HttpServletResponse arg0)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthStateErrorFilter#doFilterInternal m039987860b5cf49f|AuthStateErrorFilter.doFilterInternal]] — 생성 · L52

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 외부 라이브러리 호출

- `org.springframework.security.web.util.OnCommittedResponseWrapper#<init>(jakarta.servlet.http.HttpServletResponse)`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]
