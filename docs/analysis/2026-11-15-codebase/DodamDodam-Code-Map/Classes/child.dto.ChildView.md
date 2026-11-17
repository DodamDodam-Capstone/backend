---
tags: [backend, code-map, child]
aliases: ["ChildView"]
---
# ChildView

`com.dodamdodam.backend.child.dto.ChildView`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|시작]] → [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/child|기능 개요]] → 이 클래스 · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/90-Reference|전체 참조 자료]]

## 클래스 요약

아이의 공개 프로필을 반환하는 응답 데이터다.

- 원본: `src/main/java/com/dodamdodam/backend/child/dto/ChildView.java:9`
- 작성 메서드/생성자 0개 · 자동 생성 12개

## 메서드 목록

데이터·설정 타입이다. 아래 목록을 펼쳐 생성자와 접근자를 확인한다.

> [!example]- 생성자·자동 생성 멤버 12개
>
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.dto.ChildView#birthDate md248aad07954f7cd|public LocalDate birthDate() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.dto.ChildView#characterId mab784e0a7962babf|public String characterId() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.dto.ChildView#childId m7990d88dbffa4abf|public UUID childId() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.dto.ChildView#createdAt m659786102a254ae8|public OffsetDateTime createdAt() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.dto.ChildView#equals me7f9b6d7e73c4930|public final boolean equals(Object o) [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.dto.ChildView#gender mf0df961456b30e95|public ChildView.Gender gender() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.dto.ChildView#hashCode m43c759216ee7545f|public final int hashCode() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.dto.ChildView#interests m5a91f7785027eed8|public List<String> interests() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.dto.ChildView#name mac1fa6635c552e74|public String name() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.dto.ChildView#nickname mfe2268021c8e5b2b|public String nickname() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.dto.ChildView#toString maeddd37313c40abf|public final String toString() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.dto.ChildView#생성자 m7920af371985807d|public ChildView(UUID childId, String name, String nickname, LocalDate birthDate, ChildView.Gender gender, List<String> interests, String characterId, OffsetDateTime createdAt) [자동]]]

## birthDate md248aad07954f7cd

컴파일러가 생성한 멤버다.

```java
public java.time.LocalDate birthDate()
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.application.ChildService#getChildContext m6caa8e347baa2700|ChildService.getChildContext]] — 호출 · L68

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/child|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## characterId mab784e0a7962babf

컴파일러가 생성한 멤버다.

```java
public java.lang.String characterId()
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.application.ChildService#getChildContext m6caa8e347baa2700|ChildService.getChildContext]] — 호출 · L69

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/child|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## childId m7990d88dbffa4abf

컴파일러가 생성한 멤버다.

```java
public java.util.UUID childId()
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.api.ChildController#create m85a3864c9b94be48|ChildController.create]] — 호출 · L41
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.application.ChildService#getChildContext m6caa8e347baa2700|ChildService.getChildContext]] — 호출 · L68
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.api.ConversationController#prepareStart m1cac5f319ccf317a|ConversationController.prepareStart]] — 호출 · L59

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/child|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## createdAt m659786102a254ae8

컴파일러가 생성한 멤버다.

```java
public java.time.OffsetDateTime createdAt()
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.application.ChildService#getChildContext m6caa8e347baa2700|ChildService.getChildContext]] — 호출 · L69

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/child|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## equals me7f9b6d7e73c4930

컴파일러가 생성한 멤버다.

```java
public final boolean equals(java.lang.Object o)
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/child|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## gender mf0df961456b30e95

컴파일러가 생성한 멤버다.

```java
public com.dodamdodam.backend.child.dto.ChildView.Gender gender()
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/child|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## hashCode m43c759216ee7545f

컴파일러가 생성한 멤버다.

```java
public final int hashCode()
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/child|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## interests m5a91f7785027eed8

컴파일러가 생성한 멤버다.

```java
public java.util.List<java.lang.String> interests()
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.application.ChildService#getChildContext m6caa8e347baa2700|ChildService.getChildContext]] — 호출 · L69

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/child|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## name mac1fa6635c552e74

컴파일러가 생성한 멤버다.

```java
public java.lang.String name()
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.application.ChildService#getChildContext m6caa8e347baa2700|ChildService.getChildContext]] — 호출 · L68

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/child|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## nickname mfe2268021c8e5b2b

컴파일러가 생성한 멤버다.

```java
public java.lang.String nickname()
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.application.ChildService#getChildContext m6caa8e347baa2700|ChildService.getChildContext]] — 호출 · L68

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/child|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## toString maeddd37313c40abf

컴파일러가 생성한 멤버다.

```java
public final java.lang.String toString()
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/child|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## 생성자 m7920af371985807d

컴파일러가 생성한 멤버다.

```java
public ChildView(java.util.UUID childId, java.lang.String name, java.lang.String nickname, java.time.LocalDate birthDate, com.dodamdodam.backend.child.dto.ChildView.Gender gender, java.util.List<java.lang.String> interests, java.lang.String characterId, java.time.OffsetDateTime createdAt)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.infrastructure.ChildRepository|필드/정적 초기화]] — 생성 · L26

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 외부 라이브러리 호출

- `java.lang.Record#<init>()`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/child|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]
