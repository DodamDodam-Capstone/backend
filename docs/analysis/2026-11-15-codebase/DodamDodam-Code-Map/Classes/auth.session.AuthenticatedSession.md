---
tags: [backend, code-map, auth]
aliases: ["AuthenticatedSession"]
---
# AuthenticatedSession

`com.dodamdodam.backend.auth.session.AuthenticatedSession`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|시작]] → [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요]] → 이 클래스 · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/90-Reference|전체 참조 자료]]

## 클래스 요약

검증한 계정 ID와 업무 보안 문맥 ID를 전달한다. 객체 자체가 현재 권한을 보증하지 않는다.

- 원본: `src/main/java/com/dodamdodam/backend/auth/session/AuthenticatedSession.java:5`
- 작성 메서드/생성자 0개 · 자동 생성 6개

## 메서드 목록

데이터·설정 타입이다. 아래 목록을 펼쳐 생성자와 접근자를 확인한다.

> [!example]- 생성자·자동 생성 멤버 6개
>
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthenticatedSession#accountId m23b42fff4f806999|public UUID accountId() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthenticatedSession#equals mbf17f3450c6bf2bd|public final boolean equals(Object o) [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthenticatedSession#hashCode m76b81b65a577ccda|public final int hashCode() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthenticatedSession#securityContextId me6d8be795777e536|public UUID securityContextId() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthenticatedSession#toString m7eb85d752ac19a2d|public final String toString() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthenticatedSession#생성자 mf3197381afe1fc7a|public AuthenticatedSession(UUID accountId, UUID securityContextId) [자동]]]

## accountId m23b42fff4f806999

컴파일러가 생성한 멤버다.

```java
public java.util.UUID accountId()
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.api.AuthLogoutController#logout m43cbd6436794ba0f|AuthLogoutController.logout]] — 호출 · L50
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.EmailVerificationIssuanceService#issue mcb0a83ce2cd55ce0|EmailVerificationIssuanceService.issue]] — 호출 · L59
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.EmailVerificationIssuanceService#preparePin m330c48cfa5c13034|EmailVerificationIssuanceService.preparePin]] — 호출 · L93,94,95,110,111,112
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.LoginSessionAuthenticationStrategy#onAuthentication m62579b298c019e97|LoginSessionAuthenticationStrategy.onAuthentication]] — 호출 · L41
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.SessionSecurityRepository#createLoginContext m7e32b11c78d3f326|SessionSecurityRepository.createLoginContext]] — 호출 · L28,33
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.SessionSecurityRepository#discardLoginContext m53a371370ce09898|SessionSecurityRepository.discardLoginContext]] — 호출 · L44,54,55
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.application.ChildService#create m022c1fb522a0efea|ChildService.create]] — 호출 · L49,51
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.api.ConversationController#prepareStart m1cac5f319ccf317a|ConversationController.prepareStart]] — 호출 · L57
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationService#decideStart m074dea56e9276dfa|ConversationService.decideStart]] — 호출 · L84,85,91,95,99
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationService#replay m8a9b848e75e75181|ConversationService.replay]] — 호출 · L141
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationService#verifyStartResponse macdb3aca10ad50df|ConversationService.verifyStartResponse]] — 호출 · L53,54,57,60,63
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/guardian.application.GuardianAccessService#requireCurrentAccess mef6c2129fa7e5585|GuardianAccessService.requireCurrentAccess]] — 호출 · L45

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## equals mbf17f3450c6bf2bd

컴파일러가 생성한 멤버다.

```java
public final boolean equals(java.lang.Object o)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthenticatedSessionService#requireStoredSession mc7d6b5fbad4be98b|AuthenticatedSessionService.requireStoredSession]] — 호출 · L58
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.api.ConversationStartResponseFilter#verify mbaf83885c918b297|ConversationStartResponseFilter.verify]] — 호출 · L130

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## hashCode m76b81b65a577ccda

컴파일러가 생성한 멤버다.

```java
public final int hashCode()
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## securityContextId me6d8be795777e536

컴파일러가 생성한 멤버다.

```java
public java.util.UUID securityContextId()
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.api.AuthLogoutController#logout m43cbd6436794ba0f|AuthLogoutController.logout]] — 호출 · L50
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.EmailVerificationIssuanceService#issue mcb0a83ce2cd55ce0|EmailVerificationIssuanceService.issue]] — 호출 · L59
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.EmailVerificationIssuanceService#preparePin m330c48cfa5c13034|EmailVerificationIssuanceService.preparePin]] — 호출 · L93,95,110,111,113
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.LoginSessionAuthenticationStrategy#onAuthentication m62579b298c019e97|LoginSessionAuthenticationStrategy.onAuthentication]] — 호출 · L42
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.SessionSecurityRepository#createLoginContext m7e32b11c78d3f326|SessionSecurityRepository.createLoginContext]] — 호출 · L33
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.SessionSecurityRepository#discardLoginContext m53a371370ce09898|SessionSecurityRepository.discardLoginContext]] — 호출 · L44,54,55
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.application.ChildService#create m022c1fb522a0efea|ChildService.create]] — 호출 · L49
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationService#decideStart m074dea56e9276dfa|ConversationService.decideStart]] — 호출 · L84,91,99,125
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationService#replay m8a9b848e75e75181|ConversationService.replay]] — 호출 · L139,141
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationService#verifyStartResponse macdb3aca10ad50df|ConversationService.verifyStartResponse]] — 호출 · L53,57,63
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/guardian.application.GuardianAccessService#requireCurrentAccess mef6c2129fa7e5585|GuardianAccessService.requireCurrentAccess]] — 호출 · L45

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## toString m7eb85d752ac19a2d

컴파일러가 생성한 멤버다.

```java
public final java.lang.String toString()
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## 생성자 mf3197381afe1fc7a

컴파일러가 생성한 멤버다.

```java
public AuthenticatedSession(java.util.UUID accountId, java.util.UUID securityContextId)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthenticatedSessionService#validate m72fbb73c9b868baa|AuthenticatedSessionService.validate]] — 생성 · L101
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.SessionLifecycleService#beginLogin m86b3ae016950c512|SessionLifecycleService.beginLogin]] — 생성 · L53
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.SessionSecurityRepository#createLoginContext m7e32b11c78d3f326|SessionSecurityRepository.createLoginContext]] — 생성 · L39

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 외부 라이브러리 호출

- `java.lang.Record#<init>()`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]
