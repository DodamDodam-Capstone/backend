---
tags: [backend, code-map, global]
aliases: ["Credentials"]
---
# Credentials

`com.dodamdodam.backend.global.config.OAuth2ClientProperties.Credentials`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|시작]] → [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/global|기능 개요]] → 이 클래스 · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/90-Reference|전체 참조 자료]]

## 클래스 요약

값을 전달하는 데이터 타입이다.

- 원본: `src/main/java/com/dodamdodam/backend/global/config/OAuth2ClientProperties.java:16`
- 작성 메서드/생성자 0개 · 자동 생성 6개

## 메서드 목록

데이터·설정 타입이다. 아래 목록을 펼쳐 생성자와 접근자를 확인한다.

> [!example]- 생성자·자동 생성 멤버 6개
>
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.config.OAuth2ClientProperties.Credentials#clientId m6719e57d88fe927c|public String clientId() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.config.OAuth2ClientProperties.Credentials#clientSecret m730c0c58b8b57ef9|public String clientSecret() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.config.OAuth2ClientProperties.Credentials#equals m7e1e3605303ae437|public final boolean equals(Object o) [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.config.OAuth2ClientProperties.Credentials#hashCode m840fbf63ceec344b|public final int hashCode() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.config.OAuth2ClientProperties.Credentials#toString m2ef90c7ade8003d9|public final String toString() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.config.OAuth2ClientProperties.Credentials#생성자 mf048ed3b85d9635b|public Credentials(String clientId, String clientSecret) [자동]]]

## clientId m6719e57d88fe927c

컴파일러가 생성한 멤버다.

```java
public java.lang.@jakarta.validation.constraints.NotBlank String clientId()
```

설정: `@jakarta.validation.constraints.NotBlank`

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.config.OAuth2ClientConfig#google m056c827ec1dc042e|OAuth2ClientConfig.google]] — 호출 · L28
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.config.OAuth2ClientConfig#kakao mc0c9909ebaa65b36|OAuth2ClientConfig.kakao]] — 호출 · L36

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/global|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## clientSecret m730c0c58b8b57ef9

컴파일러가 생성한 멤버다.

```java
public java.lang.@jakarta.validation.constraints.NotBlank String clientSecret()
```

설정: `@jakarta.validation.constraints.NotBlank`

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.config.OAuth2ClientConfig#google m056c827ec1dc042e|OAuth2ClientConfig.google]] — 호출 · L29
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.config.OAuth2ClientConfig#kakao mc0c9909ebaa65b36|OAuth2ClientConfig.kakao]] — 호출 · L37

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/global|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## equals m7e1e3605303ae437

컴파일러가 생성한 멤버다.

```java
public final boolean equals(java.lang.Object o)
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/global|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## hashCode m840fbf63ceec344b

컴파일러가 생성한 멤버다.

```java
public final int hashCode()
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/global|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## toString m2ef90c7ade8003d9

컴파일러가 생성한 멤버다.

```java
public final java.lang.String toString()
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/global|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## 생성자 mf048ed3b85d9635b

컴파일러가 생성한 멤버다.

```java
public Credentials(java.lang.@jakarta.validation.constraints.NotBlank String clientId, java.lang.@jakarta.validation.constraints.NotBlank String clientSecret)
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 외부 라이브러리 호출

- `java.lang.Record#<init>()`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/global|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]
