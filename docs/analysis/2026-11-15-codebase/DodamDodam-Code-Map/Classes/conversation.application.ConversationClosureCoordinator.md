---
tags: [backend, code-map, conversation]
aliases: ["ConversationClosureCoordinator"]
---
# ConversationClosureCoordinator

`com.dodamdodam.backend.conversation.application.ConversationClosureCoordinator`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|시작]] → [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요]] → 이 클래스 · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/90-Reference|전체 참조 자료]]

## 클래스 요약

진행 중 발화 정리를 기다리며 대화를 ACTIVE → CLOSING → ENDED로 조정한다.

- 원본: `src/main/java/com/dodamdodam/backend/conversation/application/ConversationClosureCoordinator.java:14`
- 작성 메서드/생성자 3개 · 자동 생성 0개

## 메서드 목록

| 메서드 | 하는 일 |
| --- | --- |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationClosureCoordinator#reconcile m5c7d3b9b2dd0cb88\|reconcile]] | 신뢰된 내부 호출용이며 사용자 경로의 로그인·연결 검사는 호출자가 같은 Tx에서 선행한다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationClosureCoordinator#requireState mbbd5c374803feeec\|requireState]] | 미존재·다른 자녀의 대화는 같은 404로 처리하고 부모-자식 관계가 없는 상태는 반환하지 않는다. |

> [!example]- 생성자·자동 생성 멤버 1개
>
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationClosureCoordinator#생성자 mcb19985dfca463dc|public ConversationClosureCoordinator(ChildService children, ConversationRepository clock, ConversationClosureRepository repository, ConversationClosureProperties properties)]]

## reconcile m5c7d3b9b2dd0cb88

신뢰된 내부 호출용이며 사용자 경로의 로그인·연결 검사는 호출자가 같은 Tx에서 선행한다.

```java
public com.dodamdodam.backend.conversation.application.ConversationClosureCoordinator.ClosureResult reconcile(java.util.UUID accountId, java.util.UUID childId, java.util.UUID conversationId, com.dodamdodam.backend.conversation.application.ConversationClosureCoordinator.Trigger trigger)
```

설정: `@org.springframework.transaction.annotation.Transactional`

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationService#decideStart m074dea56e9276dfa|ConversationService.decideStart]] — 호출 · L95
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationService#verifyStartResponse macdb3aca10ad50df|ConversationService.verifyStartResponse]] — 호출 · L60

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.application.ChildService#requireChildAccessForUpdate m10ec0868c9e8ded5|ChildService.requireChildAccessForUpdate]] — 호출 · L39
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.exception.ChildException#notFound mda3f586558efa3e2|ChildException.notFound]] — 호출 · L37
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationClosureCoordinator#requireState mbbd5c374803feeec|ConversationClosureCoordinator.requireState]] — 호출 · L40,51,64
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationClosureCoordinator.ClosureResult#생성자 mc1db2d4930566bf2|ClosureResult.생성자]] — 생성 · L43,48,57,64
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.infrastructure.ConversationClosureProperties#summaryProcessingTimeout mffb02d95482b61bd|ConversationClosureProperties.summaryProcessingTimeout]] — 호출 · L61
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.infrastructure.ConversationClosureRepository#beginClosing m8c1904cc945fbe62|ConversationClosureRepository.beginClosing]] — 호출 · L50
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.infrastructure.ConversationClosureRepository#expireProcessing m013d6a27bd537b45|ConversationClosureRepository.expireProcessing]] — 호출 · L55
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.infrastructure.ConversationClosureRepository#finishClosing mabcd7eabe63e07b8|ConversationClosureRepository.finishClosing]] — 호출 · L62
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.infrastructure.ConversationClosureRepository#grantRecovery m7ac5ec5179a9bacb|ConversationClosureRepository.grantRecovery]] — 호출 · L63
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.infrastructure.ConversationClosureRepository#hasProcessing mbb01ff6c85dec5a2|ConversationClosureRepository.hasProcessing]] — 호출 · L56
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.infrastructure.ConversationClosureRepository#hasSummaryInput mc52240ff470ce37e|ConversationClosureRepository.hasSummaryInput]] — 호출 · L61
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.infrastructure.ConversationClosureRepository#lockAccount me75547411e9e6805|ConversationClosureRepository.lockAccount]] — 호출 · L36
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.infrastructure.ConversationClosureRepository#lockLinks m1f2723814c4ce03e|ConversationClosureRepository.lockLinks]] — 호출 · L59
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.infrastructure.ConversationClosureRepository#lockProcessingTurns macac7a26cd3dcb0a|ConversationClosureRepository.lockProcessingTurns]] — 호출 · L54
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.infrastructure.ConversationClosureRepository.ClosureState#endRequestedAt m68ff0976ba9e6035|ClosureState.endRequestedAt]] — 호출 · L63
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.infrastructure.ConversationClosureRepository.ClosureState#scheduledEndAt m0301ef381d8056d8|ClosureState.scheduledEndAt]] — 호출 · L46,50
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.infrastructure.ConversationClosureRepository.ClosureState#status mc61770c44b1d7c22|ClosureState.status]] — 호출 · L42,45
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.infrastructure.ConversationRepository#currentTime m7f615951fbdd6a0f|ConversationRepository.currentTime]] — 호출 · L41,55,60

### 외부 라이브러리 호출

- `java.lang.String#equals(java.lang.Object)`
- `java.time.OffsetDateTime#isBefore(java.time.OffsetDateTime)`
- `java.time.OffsetDateTime#plus(java.time.temporal.TemporalAmount)`

### 소스 코드

`src/main/java/com/dodamdodam/backend/conversation/application/ConversationClosureCoordinator.java:33`

```java
@Transactional
public ClosureResult reconcile(UUID accountId, UUID childId, UUID conversationId, Trigger trigger) {
    // 이미 보유한 부모 잠금은 재사용하며 새 잠금은 계정→자녀→대화→발화/연결 순서다.
    if (!repository.lockAccount(accountId)) {
        throw ChildException.notFound();
    }
    children.requireChildAccessForUpdate(accountId, childId);
    var state = requireState(childId, conversationId);
    var checkedAt = clock.currentTime();
    if (state.status().equals("ENDED")) {
        return new ClosureResult(state, false);
    }
    if (state.status().equals("ACTIVE")) {
        boolean midnight = !checkedAt.isBefore(state.scheduledEndAt());
        if (!midnight && trigger != Trigger.REQUEST_END) {
            return new ClosureResult(state, false);
        }
        repository.beginClosing(conversationId, midnight ? state.scheduledEndAt() : checkedAt, midnight ? "MIDNIGHT" : "MANUAL");
        state = requireState(childId, conversationId);
    }

    repository.lockProcessingTurns(conversationId);
    repository.expireProcessing(conversationId, clock.currentTime());
    if (repository.hasProcessing(conversationId)) {
        return new ClosureResult(state, false);
    }
    repository.lockLinks(conversationId);
    var endedAt = clock.currentTime();
    var summaryDeadline = repository.hasSummaryInput(conversationId) ? endedAt.plus(properties.summaryProcessingTimeout()) : null;
    repository.finishClosing(conversationId, endedAt, summaryDeadline);
    repository.grantRecovery(childId, conversationId, state.endRequestedAt(), endedAt);
    return new ClosureResult(requireState(childId, conversationId), true);
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## requireState mbbd5c374803feeec

미존재·다른 자녀의 대화는 같은 404로 처리하고 부모-자식 관계가 없는 상태는 반환하지 않는다.

```java
private com.dodamdodam.backend.conversation.infrastructure.ConversationClosureRepository.ClosureState requireState(java.util.UUID childId, java.util.UUID conversationId)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationClosureCoordinator#reconcile m5c7d3b9b2dd0cb88|ConversationClosureCoordinator.reconcile]] — 호출 · L40,51,64

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.exception.ChildException#notFound mda3f586558efa3e2|ChildException.notFound]] — 참조 · L69
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.infrastructure.ConversationClosureRepository#findForUpdate m31b37bc252579185|ConversationClosureRepository.findForUpdate]] — 호출 · L69

### 외부 라이브러리 호출

- `java.util.Optional#orElseThrow(java.util.function.Supplier<? extends X>)`

### 소스 코드

`src/main/java/com/dodamdodam/backend/conversation/application/ConversationClosureCoordinator.java:68`

```java
private ClosureState requireState(UUID childId, UUID conversationId) {
    return repository.findForUpdate(childId, conversationId).orElseThrow(ChildException::notFound);
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## 생성자 mcb19985dfca463dc

시작·수동 종료·타이머·발화 완료가 같은 DB 전이와 명시적 요약 기한을 사용한다.

```java
public ConversationClosureCoordinator(com.dodamdodam.backend.child.application.ChildService children, com.dodamdodam.backend.conversation.infrastructure.ConversationRepository clock, com.dodamdodam.backend.conversation.infrastructure.ConversationClosureRepository repository, com.dodamdodam.backend.conversation.infrastructure.ConversationClosureProperties properties)
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 외부 라이브러리 호출

- `java.lang.Object#<init>()`

### 소스 코드

`src/main/java/com/dodamdodam/backend/conversation/application/ConversationClosureCoordinator.java:24`

```java
public ConversationClosureCoordinator(ChildService children, ConversationRepository clock,
        ConversationClosureRepository repository, ConversationClosureProperties properties) {
    this.children = children;
    this.clock = clock;
    this.repository = repository;
    this.properties = properties;
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]
