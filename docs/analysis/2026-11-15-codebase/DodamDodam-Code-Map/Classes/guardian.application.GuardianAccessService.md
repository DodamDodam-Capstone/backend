---
tags: [backend, code-map, guardian]
aliases: ["GuardianAccessService"]
---
# GuardianAccessService

`com.dodamdodam.backend.guardian.application.GuardianAccessService`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|시작]] → [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/guardian|기능 개요]] → 이 클래스 · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/90-Reference|전체 참조 자료]]

## 클래스 요약

로그인 문맥·보호자 확인 기한·PIN 버전을 확인하고 응답 재검사를 연결한다.

- 원본: `src/main/java/com/dodamdodam/backend/guardian/application/GuardianAccessService.java:10`
- 작성 메서드/생성자 5개 · 자동 생성 0개

## 메서드 목록

| 메서드 | 하는 일 |
| --- | --- |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/guardian.application.GuardianAccessService#requireGuardianAccess mae5bd54dbbf05b63\|requireGuardianAccess]] | 같은 요청의 인증·소유권 검사 뒤 보호자 확인 기한을 요구한다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/guardian.application.GuardianAccessService#requiresResponseVerification m41add87d49066ff2\|requiresResponseVerification]] | 재검사 대상 요청인지만 반환하며, 표식 자체가 현재 권한을 보증하지는 않는다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/guardian.application.GuardianAccessService#verifyResponseAccess m08ab0b3e274d6cb9\|verifyResponseAccess]] | 보호자 권한 검사에 성공한 요청의 현재 DB 권한을 다시 확인한다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/guardian.application.GuardianAccessService#requireCurrentAccess mef6c2129fa7e5585\|requireCurrentAccess]] | 로그인 문맥 유효성을 먼저 확인한 뒤 같은 문맥의 최신 보호자 권한을 요구한다. |

> [!example]- 생성자·자동 생성 멤버 1개
>
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/guardian.application.GuardianAccessService#생성자 m64c6cf8d92037a4d|public GuardianAccessService(GuardianAccessRepository repository)]]

## requireGuardianAccess mae5bd54dbbf05b63

같은 요청의 인증·소유권 검사 뒤 보호자 확인 기한을 요구한다.

```java
public java.time.OffsetDateTime requireGuardianAccess(jakarta.servlet.http.HttpServletRequest request, com.dodamdodam.backend.auth.session.AuthenticatedSession authenticated)
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/guardian.application.GuardianAccessService#requireCurrentAccess mef6c2129fa7e5585|GuardianAccessService.requireCurrentAccess]] — 호출 · L24

### 외부 라이브러리 호출

- `jakarta.servlet.ServletRequest#setAttribute(java.lang.String,java.lang.Object)`

### 소스 코드

`src/main/java/com/dodamdodam/backend/guardian/application/GuardianAccessService.java:23`

```java
public OffsetDateTime requireGuardianAccess(HttpServletRequest request, AuthenticatedSession authenticated) {
    var until = requireCurrentAccess(authenticated);
    // 성공한 계정·문맥만 요청에 남기며 HTTP 세션이나 DB 권한을 연장하지 않는다.
    request.setAttribute(RESPONSE_ACCESS, authenticated);
    return until;
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/guardian|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## requiresResponseVerification m41add87d49066ff2

재검사 대상 요청인지만 반환하며, 표식 자체가 현재 권한을 보증하지는 않는다.

```java
public static boolean requiresResponseVerification(jakarta.servlet.http.HttpServletRequest request)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/guardian.api.GuardianAccessExceptionHandler#handle mc83c0587f4b275ce|GuardianAccessExceptionHandler.handle]] — 호출 · L24

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 외부 라이브러리 호출

- `jakarta.servlet.ServletRequest#getAttribute(java.lang.String)`

### 소스 코드

`src/main/java/com/dodamdodam/backend/guardian/application/GuardianAccessService.java:31`

```java
public static boolean requiresResponseVerification(HttpServletRequest request) {
    return request.getAttribute(RESPONSE_ACCESS) instanceof AuthenticatedSession;
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/guardian|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## verifyResponseAccess m08ab0b3e274d6cb9

보호자 권한 검사에 성공한 요청의 현재 DB 권한을 다시 확인한다.

```java
public void verifyResponseAccess(jakarta.servlet.http.HttpServletRequest request)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthStateErrorFilter#verifyResponseAccess m1098528f38d3ff11|AuthStateErrorFilter.verifyResponseAccess]] — 호출 · L98

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/guardian.application.GuardianAccessService#requireCurrentAccess mef6c2129fa7e5585|GuardianAccessService.requireCurrentAccess]] — 호출 · L39

### 외부 라이브러리 호출

- `jakarta.servlet.ServletRequest#getAttribute(java.lang.String)`

### 소스 코드

`src/main/java/com/dodamdodam/backend/guardian/application/GuardianAccessService.java:36`

```java
public void verifyResponseAccess(HttpServletRequest request) {
    if (request.getAttribute(RESPONSE_ACCESS) instanceof AuthenticatedSession authenticated) {
        // 표식을 유지해 반복 검사에서도 저장 당시 기한 대신 현재 DB 상태를 사용한다.
        requireCurrentAccess(authenticated);
    }
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/guardian|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## requireCurrentAccess mef6c2129fa7e5585

로그인 문맥 유효성을 먼저 확인한 뒤 같은 문맥의 최신 보호자 권한을 요구한다.

```java
private java.time.OffsetDateTime requireCurrentAccess(com.dodamdodam.backend.auth.session.AuthenticatedSession authenticated)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/guardian.application.GuardianAccessService#requireGuardianAccess mae5bd54dbbf05b63|GuardianAccessService.requireGuardianAccess]] — 호출 · L24
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/guardian.application.GuardianAccessService#verifyResponseAccess m08ab0b3e274d6cb9|GuardianAccessService.verifyResponseAccess]] — 호출 · L39

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthenticatedSession#accountId m23b42fff4f806999|AuthenticatedSession.accountId]] — 호출 · L45
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthenticatedSession#securityContextId me6d8be795777e536|AuthenticatedSession.securityContextId]] — 호출 · L45
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/guardian.exception.GuardianAccessException#authenticationRequired m71e62d186a76be15|GuardianAccessException.authenticationRequired]] — 호출/참조 · L46,48
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/guardian.exception.GuardianAccessException#unlockRequired md7ff80a32bab27b1|GuardianAccessException.unlockRequired]] — 호출 · L51
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/guardian.infrastructure.GuardianAccessRepository#findState mb1e4bd2e6cd4fcaf|GuardianAccessRepository.findState]] — 호출 · L45
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/guardian.infrastructure.GuardianAccessRepository.GuardianAccessState#guardianUntil m91cf4fd36a77eee4|GuardianAccessState.guardianUntil]] — 호출 · L53
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/guardian.infrastructure.GuardianAccessRepository.GuardianAccessState#isGuardianCurrent m9b272566953e54cb|GuardianAccessState.isGuardianCurrent]] — 호출 · L50
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/guardian.infrastructure.GuardianAccessRepository.GuardianAccessState#isSessionCurrent m51366fdd7d44c0f0|GuardianAccessState.isSessionCurrent]] — 호출 · L47

### 외부 라이브러리 호출

- `java.util.Optional#orElseThrow(java.util.function.Supplier<? extends X>)`

### 소스 코드

`src/main/java/com/dodamdodam/backend/guardian/application/GuardianAccessService.java:44`

```java
private OffsetDateTime requireCurrentAccess(AuthenticatedSession authenticated) {
    var state = repository.findState(authenticated.accountId(), authenticated.securityContextId())
            .orElseThrow(GuardianAccessException::authenticationRequired);
    if (!state.isSessionCurrent()) {
        throw GuardianAccessException.authenticationRequired();
    }
    if (!state.isGuardianCurrent()) {
        throw GuardianAccessException.unlockRequired();
    }
    return state.guardianUntil();
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/guardian|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## 생성자 m64c6cf8d92037a4d

현재 DB 권한을 조회할 저장소를 구성한다.

```java
public GuardianAccessService(com.dodamdodam.backend.guardian.infrastructure.GuardianAccessRepository repository)
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 외부 라이브러리 호출

- `java.lang.Object#<init>()`

### 소스 코드

`src/main/java/com/dodamdodam/backend/guardian/application/GuardianAccessService.java:18`

```java
public GuardianAccessService(GuardianAccessRepository repository) {
    this.repository = repository;
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/guardian|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]
