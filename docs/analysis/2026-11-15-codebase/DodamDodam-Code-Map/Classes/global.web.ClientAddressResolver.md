---
tags: [backend, code-map, global]
aliases: ["ClientAddressResolver"]
---
# ClientAddressResolver

`com.dodamdodam.backend.global.web.ClientAddressResolver`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|시작]] → [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/global|기능 개요]] → 이 클래스 · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/90-Reference|전체 참조 자료]]

## 클래스 요약

Forwarded 헤더 wrapper의 주소 대신 서블릿 컨테이너가 받은 실제 연결 주소를 읽는다.

- 원본: `src/main/java/com/dodamdodam/backend/global/web/ClientAddressResolver.java:9`
- 작성 메서드/생성자 2개 · 자동 생성 0개

## 메서드 목록

| 메서드 | 하는 일 |
| --- | --- |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.web.ClientAddressResolver#resolve m53c50e28b5cfa314\|resolve]] | Forwarded 헤더 wrapper의 주소 대신 서블릿 컨테이너가 받은 실제 연결 주소를 읽는다. |

> [!example]- 생성자·자동 생성 멤버 1개
>
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.web.ClientAddressResolver#생성자 mb5debcb91ab7fd90|private ClientAddressResolver()]]

## resolve m53c50e28b5cfa314

Forwarded 헤더 wrapper의 주소 대신 서블릿 컨테이너가 받은 실제 연결 주소를 읽는다.

```java
public static java.net.InetAddress resolve(jakarta.servlet.http.HttpServletRequest request)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.api.EmailVerificationController#issue m90c67bb20e0ca690|EmailVerificationController.issue]] — 호출 · L50
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.web.GlobalApiRateLimitFilter#doFilterInternal me8918f88c454eb77|GlobalApiRateLimitFilter.doFilterInternal]] — 호출 · L42

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthStateUnavailableException#생성자 m03e9c81ea8cf5421|AuthStateUnavailableException.생성자]] — 생성 · L24

### 외부 라이브러리 호출

- `jakarta.servlet.ServletRequest#getRemoteAddr()`
- `jakarta.servlet.ServletRequestWrapper#getRequest()`
- `java.net.InetAddress#ofLiteral(java.lang.String)`

### 소스 코드

`src/main/java/com/dodamdodam/backend/global/web/ClientAddressResolver.java:14`

```java
public static InetAddress resolve(HttpServletRequest request) {
    ServletRequest original = request;
    while (original instanceof ServletRequestWrapper wrapper) {
        original = wrapper.getRequest();
    }
    try {
        // 문자열 DNS 조회를 사용하지 않아 요청값으로 외부 이름을 조회하지 않는다.
        return InetAddress.ofLiteral(original.getRemoteAddr());
    }
    catch (IllegalArgumentException invalid) {
        throw new AuthStateUnavailableException();
    }
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/global|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## 생성자 mb5debcb91ab7fd90

상태 없이 공유하는 주소 변환 함수만 제공한다.

```java
private ClientAddressResolver()
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 외부 라이브러리 호출

- `java.lang.Object#<init>()`

### 소스 코드

`src/main/java/com/dodamdodam/backend/global/web/ClientAddressResolver.java:11`

```java
private ClientAddressResolver() { }
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/global|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]
