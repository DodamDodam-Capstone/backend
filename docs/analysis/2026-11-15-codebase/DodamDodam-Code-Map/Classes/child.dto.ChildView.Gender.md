---
tags: [backend, code-map, child]
aliases: ["Gender"]
---
# Gender

`com.dodamdodam.backend.child.dto.ChildView.Gender`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|시작]] → [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/child|기능 개요]] → 이 클래스 · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/90-Reference|전체 참조 자료]]

## 클래스 요약

허용 상태·종류를 정의하는 열거형이다.

- 원본: `src/main/java/com/dodamdodam/backend/child/dto/ChildView.java:12`
- 작성 메서드/생성자 0개 · 자동 생성 3개

## 메서드 목록

데이터·설정 타입이다. 아래 목록을 펼쳐 생성자와 접근자를 확인한다.

> [!example]- 생성자·자동 생성 멤버 3개
>
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.dto.ChildView.Gender#valueOf mae04a68330eefb58|public static ChildView.Gender valueOf(String name) [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.dto.ChildView.Gender#values mdb3726c097096c43|public static ChildView.Gender[] values() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.dto.ChildView.Gender#생성자 me28961aaaab896a5|private Gender() [자동]]]

## valueOf mae04a68330eefb58

컴파일러가 생성한 멤버다.

```java
public static com.dodamdodam.backend.child.dto.ChildView.Gender valueOf(java.lang.String name)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.dto.CreateChildRequest#fromJson mf8c618d868acf95b|CreateChildRequest.fromJson]] — 호출 · L45
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.infrastructure.ChildRepository|필드/정적 초기화]] — 호출 · L28

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/child|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## values mdb3726c097096c43

컴파일러가 생성한 멤버다.

```java
public static com.dodamdodam.backend.child.dto.ChildView.Gender[] values()
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/child|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## 생성자 me28961aaaab896a5

컴파일러가 생성한 멤버다.

```java
private Gender()
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.dto.ChildView.Gender|필드/정적 초기화]] — 생성 · L12

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 외부 라이브러리 호출

- `java.lang.Enum#<init>(java.lang.String,int)`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/child|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]
