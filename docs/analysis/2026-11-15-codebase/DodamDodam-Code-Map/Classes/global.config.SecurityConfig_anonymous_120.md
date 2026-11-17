---
tags: [backend, code-map, global]
aliases: ["익명 클래스@120"]
---
# 익명 클래스@120

`com.dodamdodam.backend.global.config.SecurityConfig$anonymous@120`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|시작]] → [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/global|기능 개요]] → 이 클래스 · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/90-Reference|전체 참조 자료]]

## 클래스 요약

프레임워크 설정 또는 보조 타입이다. 아래 메서드와 소스에서 연결 위치를 확인한다.

- 원본: `src/main/java/com/dodamdodam/backend/global/config/SecurityConfig.java:120`
- 작성 메서드/생성자 1개 · 자동 생성 1개

## 메서드 목록

| 메서드 | 하는 일 |
| --- | --- |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.config.SecurityConfig_anonymous_120#resolveCsrfTokenValue m95c2271cdb566188\|resolveCsrfTokenValue]] | 아래 구현과 호출 관계에서 확인 |

> [!example]- 생성자·자동 생성 멤버 1개
>
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.config.SecurityConfig_anonymous_120#생성자 m239f4bb31547a80d|익명 클래스 [자동]]]

## resolveCsrfTokenValue m95c2271cdb566188

아래 원본 코드에서 구현을 확인한다.

```java
public java.lang.String resolveCsrfTokenValue(jakarta.servlet.http.HttpServletRequest request, org.springframework.security.web.csrf.CsrfToken csrfToken)
```

설정: `@java.lang.Override`

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 외부 라이브러리 호출

- `jakarta.servlet.http.HttpServletRequest#getHeader(java.lang.String)`
- `org.springframework.security.web.csrf.CsrfToken#getHeaderName()`

### 소스 코드

`src/main/java/com/dodamdodam/backend/global/config/SecurityConfig.java:121`

```java
@Override
public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken csrfToken) {
    return request.getHeader(csrfToken.getHeaderName());
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/global|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## 생성자 m239f4bb31547a80d

컴파일러가 생성한 멤버다.

```java
익명 클래스@120()
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.config.SecurityConfig#csrfTokenRequestHandler mbc54a3db2ec31eab|SecurityConfig.csrfTokenRequestHandler]] — 생성 · L120

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 외부 라이브러리 호출

- `org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler#<init>()`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/global|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]
