---
tags: [backend, code-map, app]
aliases: ["DodamDodamBackendApplication"]
---
# DodamDodamBackendApplication

`com.dodamdodam.backend.DodamDodamBackendApplication`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|시작]] → [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/app|기능 개요]] → 이 클래스 · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/90-Reference|전체 참조 자료]]

## 클래스 요약

Spring Boot 애플리케이션을 시작하는 main 진입점이다.

- 원본: `src/main/java/com/dodamdodam/backend/DodamDodamBackendApplication.java:6`
- 작성 메서드/생성자 1개 · 자동 생성 1개

## 메서드 목록

| 메서드 | 하는 일 |
| --- | --- |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/DodamDodamBackendApplication#main m4511768b7ac61b27\|main]] | 아래 구현과 호출 관계에서 확인 |

> [!example]- 생성자·자동 생성 멤버 1개
>
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/DodamDodamBackendApplication#생성자 m5e5b5ef1c635c365|public DodamDodamBackendApplication() [자동]]]

## main m4511768b7ac61b27

아래 원본 코드에서 구현을 확인한다.

```java
public static void main(java.lang.String[] args)
```

### 이 메서드를 호출하는 곳

- 프레임워크 진입: JVM 애플리케이션 시작

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 외부 라이브러리 호출

- `org.springframework.boot.SpringApplication#run(java.lang.Class<?>,java.lang.String[])`

### 소스 코드

`src/main/java/com/dodamdodam/backend/DodamDodamBackendApplication.java:9`

```java
public static void main(String[] args) {
    SpringApplication.run(DodamDodamBackendApplication.class, args);
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/app|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## 생성자 m5e5b5ef1c635c365

컴파일러가 생성한 멤버다.

```java
public DodamDodamBackendApplication()
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 외부 라이브러리 호출

- `java.lang.Object#<init>()`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/app|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]
