---
tags: [backend, code-map, global]
aliases: ["CsrfController"]
---
# CsrfController

`com.dodamdodam.backend.global.auth.CsrfController`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|시작]] → [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/global|기능 개요]] → 이 클래스 · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/90-Reference|전체 참조 자료]]

## 클래스 요약

클라이언트가 변경 요청에 사용할 CSRF 토큰 정보를 반환한다.

- 원본: `src/main/java/com/dodamdodam/backend/global/auth/CsrfController.java:8`
- 작성 메서드/생성자 1개 · 자동 생성 1개

## 메서드 목록

| 메서드 | 하는 일 |
| --- | --- |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.auth.CsrfController#csrf meef142dc53c5563f\|csrf]] | 아래 구현과 호출 관계에서 확인 |

> [!example]- 생성자·자동 생성 멤버 1개
>
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.auth.CsrfController#생성자 mebbc4c30969ef44e|public CsrfController() [자동]]]

## csrf meef142dc53c5563f

아래 원본 코드에서 구현을 확인한다.

```java
com.dodamdodam.backend.global.auth.CsrfController.CsrfTokenResponse csrf(org.springframework.security.web.csrf.CsrfToken csrfToken)
```

설정: `@org.springframework.web.bind.annotation.GetMapping({"/csrf"})`

### 이 메서드를 호출하는 곳

- 프레임워크 진입: GET /api/v1/auth/csrf

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.auth.CsrfController.CsrfTokenResponse#생성자 mb05e8ad25cf16db5|CsrfTokenResponse.생성자]] — 생성 · L14

### 외부 라이브러리 호출

- `org.springframework.security.web.csrf.CsrfToken#getHeaderName()`
- `org.springframework.security.web.csrf.CsrfToken#getParameterName()`
- `org.springframework.security.web.csrf.CsrfToken#getToken()`

### 소스 코드

`src/main/java/com/dodamdodam/backend/global/auth/CsrfController.java:12`

```java
@GetMapping("/csrf")
CsrfTokenResponse csrf(CsrfToken csrfToken) {
    return new CsrfTokenResponse(
            csrfToken.getHeaderName(),
            csrfToken.getParameterName(),
            csrfToken.getToken()
    );
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/global|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## 생성자 mebbc4c30969ef44e

컴파일러가 생성한 멤버다.

```java
public CsrfController()
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 외부 라이브러리 호출

- `java.lang.Object#<init>()`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/global|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]
