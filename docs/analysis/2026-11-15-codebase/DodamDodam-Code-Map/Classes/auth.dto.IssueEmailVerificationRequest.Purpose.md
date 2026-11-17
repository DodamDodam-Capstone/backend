---
tags: [backend, code-map, auth]
aliases: ["Purpose"]
---
# Purpose

`com.dodamdodam.backend.auth.dto.IssueEmailVerificationRequest.Purpose`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|시작]] → [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요]] → 이 클래스 · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/90-Reference|전체 참조 자료]]

## 클래스 요약

허용 상태·종류를 정의하는 열거형이다.

- 원본: `src/main/java/com/dodamdodam/backend/auth/dto/IssueEmailVerificationRequest.java:79`
- 작성 메서드/생성자 0개 · 자동 생성 3개

## 메서드 목록

데이터·설정 타입이다. 아래 목록을 펼쳐 생성자와 접근자를 확인한다.

> [!example]- 생성자·자동 생성 멤버 3개
>
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.dto.IssueEmailVerificationRequest.Purpose#valueOf m4718c6a857663d83|public static IssueEmailVerificationRequest.Purpose valueOf(String name) [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.dto.IssueEmailVerificationRequest.Purpose#values m326400bd226fa678|public static IssueEmailVerificationRequest.Purpose[] values() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.dto.IssueEmailVerificationRequest.Purpose#생성자 mb15ef6a2ea374bcc|private Purpose() [자동]]]

## valueOf m4718c6a857663d83

컴파일러가 생성한 멤버다.

```java
public static com.dodamdodam.backend.auth.dto.IssueEmailVerificationRequest.Purpose valueOf(java.lang.String name)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.dto.IssueEmailVerificationRequest#fromJson m94a43db6e5d8c7eb|IssueEmailVerificationRequest.fromJson]] — 호출 · L26

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## values m326400bd226fa678

컴파일러가 생성한 멤버다.

```java
public static com.dodamdodam.backend.auth.dto.IssueEmailVerificationRequest.Purpose[] values()
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## 생성자 mb15ef6a2ea374bcc

컴파일러가 생성한 멤버다.

```java
private Purpose()
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.dto.IssueEmailVerificationRequest.Purpose|필드/정적 초기화]] — 생성 · L80

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 외부 라이브러리 호출

- `java.lang.Enum#<init>(java.lang.String,int)`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]
