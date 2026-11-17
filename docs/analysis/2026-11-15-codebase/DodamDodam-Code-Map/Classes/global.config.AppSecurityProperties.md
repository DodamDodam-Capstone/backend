---
tags: [backend, code-map, global]
aliases: ["AppSecurityProperties"]
---
# AppSecurityProperties

`com.dodamdodam.backend.global.config.AppSecurityProperties`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|시작]] → [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/global|기능 개요]] → 이 클래스 · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/90-Reference|전체 참조 자료]]

## 클래스 요약

값을 전달하는 데이터 타입이다.

- 원본: `src/main/java/com/dodamdodam/backend/global/config/AppSecurityProperties.java:9`
- 작성 메서드/생성자 1개 · 자동 생성 5개

## 메서드 목록

데이터·설정 타입이다. 아래 목록을 펼쳐 생성자와 접근자를 확인한다.

> [!example]- 생성자·자동 생성 멤버 6개
>
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.config.AppSecurityProperties#생성자 mbdf07f60f41fd15f|public AppSecurityProperties(List<String> allowedOrigins, String loginSuccessUrl)]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.config.AppSecurityProperties#allowedOrigins m66ee7b0d586a2a9d|public List<String> allowedOrigins() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.config.AppSecurityProperties#equals m88a96f5fa65c8dcc|public final boolean equals(Object o) [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.config.AppSecurityProperties#hashCode mb79a6581cbdad6ea|public final int hashCode() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.config.AppSecurityProperties#loginSuccessUrl m3702e77020b91861|public String loginSuccessUrl() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.config.AppSecurityProperties#toString m3ab0ecc57087c6bc|public final String toString() [자동]]]

## 생성자 mbdf07f60f41fd15f

아래 원본 코드에서 구현을 확인한다.

```java
public AppSecurityProperties(java.util.@jakarta.validation.constraints.NotEmpty List<java.lang.@jakarta.validation.constraints.NotBlank String> allowedOrigins, java.lang.@jakarta.validation.constraints.NotBlank String loginSuccessUrl)
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 외부 라이브러리 호출

- `java.lang.Record#<init>()`
- `java.util.List#copyOf(java.util.Collection<? extends E>)`

### 소스 코드

`src/main/java/com/dodamdodam/backend/global/config/AppSecurityProperties.java:16`

```java
public AppSecurityProperties {
    allowedOrigins = List.copyOf(allowedOrigins);
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/global|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## allowedOrigins m66ee7b0d586a2a9d

컴파일러가 생성한 멤버다.

```java
public java.util.@jakarta.validation.constraints.NotEmpty List<java.lang.@jakarta.validation.constraints.NotBlank String> allowedOrigins()
```

설정: `@jakarta.validation.constraints.NotEmpty`

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.config.SecurityConfig#corsConfigurationSource mf369b7a9aa69415d|SecurityConfig.corsConfigurationSource]] — 호출 · L146

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/global|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## equals m88a96f5fa65c8dcc

컴파일러가 생성한 멤버다.

```java
public final boolean equals(java.lang.Object o)
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/global|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## hashCode mb79a6581cbdad6ea

컴파일러가 생성한 멤버다.

```java
public final int hashCode()
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/global|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## loginSuccessUrl m3702e77020b91861

컴파일러가 생성한 멤버다.

```java
public java.lang.@jakarta.validation.constraints.NotBlank String loginSuccessUrl()
```

설정: `@jakarta.validation.constraints.NotBlank`

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.config.SecurityConfig#applicationSecurityFilterChain m8a28e16ae80b5e1e|SecurityConfig.applicationSecurityFilterChain]] — 호출 · L111

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/global|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## toString m3ab0ecc57087c6bc

컴파일러가 생성한 멤버다.

```java
public final java.lang.String toString()
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/global|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]
