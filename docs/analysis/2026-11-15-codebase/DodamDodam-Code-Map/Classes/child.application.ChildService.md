---
tags: [backend, code-map, child]
aliases: ["ChildService"]
---
# ChildService

`com.dodamdodam.backend.child.application.ChildService`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|시작]] → [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/child|기능 개요]] → 이 클래스 · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/90-Reference|전체 참조 자료]]

## 클래스 요약

아이 입력 검증·등록과 소유권 검사를 수행한다. 등록 시 현재 보안 문맥을 잠근다.

- 원본: `src/main/java/com/dodamdodam/backend/child/application/ChildService.java:18`
- 작성 메서드/생성자 6개 · 자동 생성 0개

## 메서드 목록

| 메서드 | 하는 일 |
| --- | --- |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.application.ChildService#create m022c1fb522a0efea\|create]] | 입력 검증·현재 문맥 확인·등록을 같은 트랜잭션에서 수행한다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.application.ChildService#requireChildAccess mbb6af419b1ceab47\|requireChildAccess]] | 소유 자녀를 반환하며, 미소유와 미존재를 같은 오류로 처리한다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.application.ChildService#requireChildAccessForUpdate m10ec0868c9e8ded5\|requireChildAccessForUpdate]] | 계정·보안 문맥 잠금을 먼저 획득한 호출자의 트랜잭션에서 소유 자녀를 잠근다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.application.ChildService#getChildContext m6caa8e347baa2700\|getChildContext]] | requireAuthenticatedSession → requireChildAccess 순서로 검증한 뒤 호출하는 내부 조회다. |

> [!example]- 생성자·자동 생성 멤버 2개
>
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.application.ChildService#생성자 m525dcc919ca9a630|public ChildService(ChildRepository repository, SessionSecurityRepository securityRepository)]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.application.ChildService#생성자 ma9397c45f753bcb9|ChildService(ChildRepository repository, SessionSecurityRepository securityRepository, Clock clock, Set<String> supportedCharacterIds)]]

## create m022c1fb522a0efea

입력 검증·현재 문맥 확인·등록을 같은 트랜잭션에서 수행한다.

```java
public com.dodamdodam.backend.child.dto.ChildView create(com.dodamdodam.backend.auth.session.AuthenticatedSession session, com.dodamdodam.backend.child.dto.CreateChildRequest request)
```

설정: `@org.springframework.transaction.annotation.Transactional`

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.api.ChildController#create m85a3864c9b94be48|ChildController.create]] — 호출 · L39

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthenticatedSession#accountId m23b42fff4f806999|AuthenticatedSession.accountId]] — 호출 · L49,51
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthenticatedSession#securityContextId me6d8be795777e536|AuthenticatedSession.securityContextId]] — 호출 · L49
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.SessionSecurityRepository#requireCurrentContextForUpdate m950184451aa631db|SessionSecurityRepository.requireCurrentContextForUpdate]] — 호출 · L49
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.dto.CreateChildRequest#normalized mf6b45ea2162d93f1|CreateChildRequest.normalized]] — 호출 · L47
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.exception.ChildException#limitReached me8321a7d6a24ae8e|ChildException.limitReached]] — 참조 · L51
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.infrastructure.ChildRepository#insert mcdb7b197a0d8c26e|ChildRepository.insert]] — 호출 · L51

### 외부 라이브러리 호출

- `java.util.Optional#orElseThrow(java.util.function.Supplier<? extends X>)`

### 소스 코드

`src/main/java/com/dodamdodam/backend/child/application/ChildService.java:45`

```java
@Transactional
public ChildView create(AuthenticatedSession session, CreateChildRequest request) {
    var normalized = request.normalized(clock, supportedCharacterIds);
    // 입력 검증 후 문맥을 잠가 폐기·계정 버전 변경과 등록이 엇갈리지 않게 한다.
    securityRepository.requireCurrentContextForUpdate(session.accountId(), session.securityContextId());
    // 계정당 UNIQUE 충돌만 등록 제한이며 다른 저장 장애는 원래 업무 예외를 유지한다.
    return repository.insert(session.accountId(), normalized).orElseThrow(ChildException::limitReached);
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/child|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## requireChildAccess mbb6af419b1ceab47

소유 자녀를 반환하며, 미소유와 미존재를 같은 오류로 처리한다.

```java
public com.dodamdodam.backend.child.dto.ChildView requireChildAccess(java.util.UUID accountId, java.util.UUID childId)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.api.ConversationController#prepareStart m1cac5f319ccf317a|ConversationController.prepareStart]] — 호출 · L57

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.exception.ChildException#notFound mda3f586558efa3e2|ChildException.notFound]] — 참조 · L56
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.infrastructure.ChildRepository#findOwned m0c455eef6c80ad12|ChildRepository.findOwned]] — 호출 · L56

### 외부 라이브러리 호출

- `java.util.Optional#orElseThrow(java.util.function.Supplier<? extends X>)`

### 소스 코드

`src/main/java/com/dodamdodam/backend/child/application/ChildService.java:55`

```java
public ChildView requireChildAccess(UUID accountId, UUID childId) {
    return repository.findOwned(accountId, childId).orElseThrow(ChildException::notFound);
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/child|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## requireChildAccessForUpdate m10ec0868c9e8ded5

계정·보안 문맥 잠금을 먼저 획득한 호출자의 트랜잭션에서 소유 자녀를 잠근다.

```java
public com.dodamdodam.backend.child.dto.ChildView requireChildAccessForUpdate(java.util.UUID accountId, java.util.UUID childId)
```

설정: `@org.springframework.transaction.annotation.Transactional(propagation=MANDATORY)`

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationClosureCoordinator#reconcile m5c7d3b9b2dd0cb88|ConversationClosureCoordinator.reconcile]] — 호출 · L39
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationService#decideStart m074dea56e9276dfa|ConversationService.decideStart]] — 호출 · L85
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationService#verifyStartResponse macdb3aca10ad50df|ConversationService.verifyStartResponse]] — 호출 · L54

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.exception.ChildException#notFound mda3f586558efa3e2|ChildException.notFound]] — 참조 · L62
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.infrastructure.ChildRepository#findOwnedForUpdate maf36c957db0ad4a1|ChildRepository.findOwnedForUpdate]] — 호출 · L62

### 외부 라이브러리 호출

- `java.util.Optional#orElseThrow(java.util.function.Supplier<? extends X>)`

### 소스 코드

`src/main/java/com/dodamdodam/backend/child/application/ChildService.java:60`

```java
@Transactional(propagation = Propagation.MANDATORY)
public ChildView requireChildAccessForUpdate(UUID accountId, UUID childId) {
    return repository.findOwnedForUpdate(accountId, childId).orElseThrow(ChildException::notFound);
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/child|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## getChildContext m6caa8e347baa2700

requireAuthenticatedSession → requireChildAccess 순서로 검증한 뒤 호출하는 내부 조회다.

```java
public com.dodamdodam.backend.child.dto.ChildContext getChildContext(java.util.UUID childId)
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.dto.ChildContext#생성자 m536e5b98466722f0|ChildContext.생성자]] — 생성 · L68
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.dto.ChildView#birthDate md248aad07954f7cd|ChildView.birthDate]] — 호출 · L68
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.dto.ChildView#characterId mab784e0a7962babf|ChildView.characterId]] — 호출 · L69
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.dto.ChildView#childId m7990d88dbffa4abf|ChildView.childId]] — 호출 · L68
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.dto.ChildView#createdAt m659786102a254ae8|ChildView.createdAt]] — 호출 · L69
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.dto.ChildView#interests m5a91f7785027eed8|ChildView.interests]] — 호출 · L69
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.dto.ChildView#name mac1fa6635c552e74|ChildView.name]] — 호출 · L68
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.dto.ChildView#nickname mfe2268021c8e5b2b|ChildView.nickname]] — 호출 · L68
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.exception.ChildException#notFound mda3f586558efa3e2|ChildException.notFound]] — 참조 · L67
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.infrastructure.ChildRepository#findById mdfeacb26eaf99e57|ChildRepository.findById]] — 호출 · L67

### 외부 라이브러리 호출

- `java.util.Optional#orElseThrow(java.util.function.Supplier<? extends X>)`

### 소스 코드

`src/main/java/com/dodamdodam/backend/child/application/ChildService.java:66`

```java
public ChildContext getChildContext(UUID childId) {
    var child = repository.findById(childId).orElseThrow(ChildException::notFound);
    return new ChildContext(child.childId(), child.name(), child.nickname(), child.birthDate(),
            child.interests(), child.characterId(), child.createdAt());
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/child|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## 생성자 m525dcc919ca9a630

운영 시계와 현재 지원 목록으로 서비스를 구성한다.

```java
public ChildService(com.dodamdodam.backend.child.infrastructure.ChildRepository repository, com.dodamdodam.backend.auth.session.SessionSecurityRepository securityRepository)
```

설정: `@org.springframework.beans.factory.annotation.Autowired`

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.application.ChildService#생성자 ma9397c45f753bcb9|ChildService.생성자]] — 호출 · L32

### 외부 라이브러리 호출

- `java.time.Clock#systemUTC()`

### 소스 코드

`src/main/java/com/dodamdodam/backend/child/application/ChildService.java:30`

```java
@Autowired
public ChildService(ChildRepository repository, SessionSecurityRepository securityRepository) {
    this(repository, securityRepository, Clock.systemUTC(), SUPPORTED_CHARACTER_IDS);
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/child|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## 생성자 ma9397c45f753bcb9

날짜 경계와 지원 목록을 테스트에서 고정할 수 있도록 의존성을 주입한다.

```java
ChildService(com.dodamdodam.backend.child.infrastructure.ChildRepository repository, com.dodamdodam.backend.auth.session.SessionSecurityRepository securityRepository, java.time.Clock clock, java.util.Set<java.lang.String> supportedCharacterIds)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.application.ChildService#생성자 m525dcc919ca9a630|ChildService.생성자]] — 호출 · L32

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 외부 라이브러리 호출

- `java.lang.Object#<init>()`
- `java.util.Set#copyOf(java.util.Collection<? extends E>)`

### 소스 코드

`src/main/java/com/dodamdodam/backend/child/application/ChildService.java:36`

```java
ChildService(ChildRepository repository, SessionSecurityRepository securityRepository,
             Clock clock, Set<String> supportedCharacterIds) {
    this.repository = repository;
    this.securityRepository = securityRepository;
    this.clock = clock;
    this.supportedCharacterIds = Set.copyOf(supportedCharacterIds);
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/child|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]
