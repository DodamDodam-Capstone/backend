---
tags: [backend, code-map, guardian]
aliases: ["GuardianAccessException"]
---
# GuardianAccessException

`com.dodamdodam.backend.guardian.exception.GuardianAccessException`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|시작]] → [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/guardian|기능 개요]] → 이 클래스 · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/90-Reference|전체 참조 자료]]

## 클래스 요약

업무 실패의 상태 코드와 공개 오류 정보를 전달한다.

- 원본: `src/main/java/com/dodamdodam/backend/guardian/exception/GuardianAccessException.java:3`
- 작성 메서드/생성자 6개 · 자동 생성 0개

## 메서드 목록

| 메서드 | 하는 일 |
| --- | --- |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/guardian.exception.GuardianAccessException#authenticationRequired m71e62d186a76be15\|authenticationRequired]] | 현재 로그인 문맥이 없거나 유효하지 않은 경우를 표시한다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/guardian.exception.GuardianAccessException#unlockRequired md7ff80a32bab27b1\|unlockRequired]] | 현재 세션에 유효한 보호자 확인이 필요한 경우를 표시한다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/guardian.exception.GuardianAccessException#unavailable m0cb219e13da52ebb\|unavailable]] | 저장소 확인 실패의 원인은 보존하고 응답 메시지는 고정한다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/guardian.exception.GuardianAccessException#status md61668cf15a2751a\|status]] | API와 응답 재검사 경로가 사용할 HTTP 상태를 반환한다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/guardian.exception.GuardianAccessException#code m7dd5b7198c3b7a07\|code]] | 외부 응답에 사용할 고정 오류 코드를 반환한다. |

> [!example]- 생성자·자동 생성 멤버 1개
>
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/guardian.exception.GuardianAccessException#생성자 m4e6fce2d05e4a8d9|private GuardianAccessException(int status, String code, String message, Throwable cause)]]

## authenticationRequired m71e62d186a76be15

현재 로그인 문맥이 없거나 유효하지 않은 경우를 표시한다.

```java
public static com.dodamdodam.backend.guardian.exception.GuardianAccessException authenticationRequired()
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/guardian.application.GuardianAccessService#requireCurrentAccess mef6c2129fa7e5585|GuardianAccessService.requireCurrentAccess]] — 호출/참조 · L46,48

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/guardian.exception.GuardianAccessException#생성자 m4e6fce2d05e4a8d9|GuardianAccessException.생성자]] — 생성 · L17

### 소스 코드

`src/main/java/com/dodamdodam/backend/guardian/exception/GuardianAccessException.java:16`

```java
public static GuardianAccessException authenticationRequired() {
    return new GuardianAccessException(401, "AUTHENTICATION_REQUIRED", "로그인이 필요합니다.", null);
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/guardian|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## unlockRequired md7ff80a32bab27b1

현재 세션에 유효한 보호자 확인이 필요한 경우를 표시한다.

```java
public static com.dodamdodam.backend.guardian.exception.GuardianAccessException unlockRequired()
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/guardian.application.GuardianAccessService#requireCurrentAccess mef6c2129fa7e5585|GuardianAccessService.requireCurrentAccess]] — 호출 · L51

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/guardian.exception.GuardianAccessException#생성자 m4e6fce2d05e4a8d9|GuardianAccessException.생성자]] — 생성 · L22

### 소스 코드

`src/main/java/com/dodamdodam/backend/guardian/exception/GuardianAccessException.java:21`

```java
public static GuardianAccessException unlockRequired() {
    return new GuardianAccessException(403, "GUARDIAN_UNLOCK_REQUIRED", "보호자 PIN 확인이 필요합니다.", null);
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/guardian|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## unavailable m0cb219e13da52ebb

저장소 확인 실패의 원인은 보존하고 응답 메시지는 고정한다.

```java
public static com.dodamdodam.backend.guardian.exception.GuardianAccessException unavailable(java.lang.Throwable cause)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/guardian.infrastructure.GuardianAccessRepository#findState mb1e4bd2e6cd4fcaf|GuardianAccessRepository.findState]] — 호출 · L46

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/guardian.exception.GuardianAccessException#생성자 m4e6fce2d05e4a8d9|GuardianAccessException.생성자]] — 생성 · L27

### 소스 코드

`src/main/java/com/dodamdodam/backend/guardian/exception/GuardianAccessException.java:26`

```java
public static GuardianAccessException unavailable(Throwable cause) {
    return new GuardianAccessException(503, "AUTH_STATE_UNAVAILABLE",
            "인증 상태를 확인할 수 없습니다. 잠시 후 다시 시도해 주세요.", cause);
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/guardian|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## status md61668cf15a2751a

API와 응답 재검사 경로가 사용할 HTTP 상태를 반환한다.

```java
public int status()
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthStateErrorFilter#doFilterInternal m039987860b5cf49f|AuthStateErrorFilter.doFilterInternal]] — 호출 · L82
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/guardian.api.GuardianAccessExceptionHandler#handle mc83c0587f4b275ce|GuardianAccessExceptionHandler.handle]] — 호출 · L29

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 소스 코드

`src/main/java/com/dodamdodam/backend/guardian/exception/GuardianAccessException.java:32`

```java
public int status() {
    return status;
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/guardian|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## code m7dd5b7198c3b7a07

외부 응답에 사용할 고정 오류 코드를 반환한다.

```java
public java.lang.String code()
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthStateErrorFilter#doFilterInternal m039987860b5cf49f|AuthStateErrorFilter.doFilterInternal]] — 호출 · L82
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/guardian.api.GuardianAccessExceptionHandler#handle mc83c0587f4b275ce|GuardianAccessExceptionHandler.handle]] — 호출 · L30

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 소스 코드

`src/main/java/com/dodamdodam/backend/guardian/exception/GuardianAccessException.java:37`

```java
public String code() {
    return code;
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/guardian|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## 생성자 m4e6fce2d05e4a8d9

정해진 권한 오류만 만들 수 있도록 상태·코드·원인을 내부에서 구성한다.

```java
private GuardianAccessException(int status, java.lang.String code, java.lang.String message, java.lang.Throwable cause)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/guardian.exception.GuardianAccessException#authenticationRequired m71e62d186a76be15|GuardianAccessException.authenticationRequired]] — 생성 · L17
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/guardian.exception.GuardianAccessException#unavailable m0cb219e13da52ebb|GuardianAccessException.unavailable]] — 생성 · L27
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/guardian.exception.GuardianAccessException#unlockRequired md7ff80a32bab27b1|GuardianAccessException.unlockRequired]] — 생성 · L22

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 외부 라이브러리 호출

- `java.lang.RuntimeException#<init>(java.lang.String,java.lang.Throwable)`

### 소스 코드

`src/main/java/com/dodamdodam/backend/guardian/exception/GuardianAccessException.java:9`

```java
private GuardianAccessException(int status, String code, String message, Throwable cause) {
    super(message, cause);
    this.status = status;
    this.code = code;
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/guardian|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]
