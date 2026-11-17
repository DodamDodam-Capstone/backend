---
tags: [backend, code-map, global]
aliases: ["OAuth2ClientProperties"]
---
# OAuth2ClientProperties

`com.dodamdodam.backend.global.config.OAuth2ClientProperties`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|시작]] → [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/global|기능 개요]] → 이 클래스 · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/90-Reference|전체 참조 자료]]

## 클래스 요약

값을 전달하는 데이터 타입이다.

- 원본: `src/main/java/com/dodamdodam/backend/global/config/OAuth2ClientProperties.java:9`
- 작성 메서드/생성자 0개 · 자동 생성 6개

## 메서드 목록

데이터·설정 타입이다. 아래 목록을 펼쳐 생성자와 접근자를 확인한다.

> [!example]- 생성자·자동 생성 멤버 6개
>
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.config.OAuth2ClientProperties#equals m04e5576b1bfd3c8b|public final boolean equals(Object o) [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.config.OAuth2ClientProperties#google m7640b32867dd7941|public OAuth2ClientProperties.Credentials google() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.config.OAuth2ClientProperties#hashCode mdcd9274422c78822|public final int hashCode() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.config.OAuth2ClientProperties#kakao m087d0539ef392b14|public OAuth2ClientProperties.Credentials kakao() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.config.OAuth2ClientProperties#toString mdf91aba9e2ff8140|public final String toString() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.config.OAuth2ClientProperties#생성자 mc182a1415ab3da82|public OAuth2ClientProperties(OAuth2ClientProperties.Credentials google, OAuth2ClientProperties.Credentials kakao) [자동]]]

## equals m04e5576b1bfd3c8b

컴파일러가 생성한 멤버다.

```java
public final boolean equals(java.lang.Object o)
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/global|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## google m7640b32867dd7941

컴파일러가 생성한 멤버다.

```java
public com.dodamdodam.backend.global.config.OAuth2ClientProperties.@jakarta.validation.constraints.NotNull @jakarta.validation.Valid Credentials google()
```

설정: `@jakarta.validation.constraints.NotNull` / `@jakarta.validation.Valid`

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.config.OAuth2ClientConfig#clientRegistrationRepository m3ccfcf9a87b4677f|OAuth2ClientConfig.clientRegistrationRepository]] — 호출 · L21

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/global|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## hashCode mdcd9274422c78822

컴파일러가 생성한 멤버다.

```java
public final int hashCode()
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/global|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## kakao m087d0539ef392b14

컴파일러가 생성한 멤버다.

```java
public com.dodamdodam.backend.global.config.OAuth2ClientProperties.@jakarta.validation.constraints.NotNull @jakarta.validation.Valid Credentials kakao()
```

설정: `@jakarta.validation.constraints.NotNull` / `@jakarta.validation.Valid`

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.config.OAuth2ClientConfig#clientRegistrationRepository m3ccfcf9a87b4677f|OAuth2ClientConfig.clientRegistrationRepository]] — 호출 · L22

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/global|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## toString mdf91aba9e2ff8140

컴파일러가 생성한 멤버다.

```java
public final java.lang.String toString()
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/global|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## 생성자 mc182a1415ab3da82

컴파일러가 생성한 멤버다.

```java
public OAuth2ClientProperties(com.dodamdodam.backend.global.config.OAuth2ClientProperties.@jakarta.validation.constraints.NotNull @jakarta.validation.Valid Credentials google, com.dodamdodam.backend.global.config.OAuth2ClientProperties.@jakarta.validation.constraints.NotNull @jakarta.validation.Valid Credentials kakao)
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 외부 라이브러리 호출

- `java.lang.Record#<init>()`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/global|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]
