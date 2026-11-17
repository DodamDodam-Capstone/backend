---
tags: [backend, code-map, conversation]
aliases: ["ConversationService"]
---
# ConversationService

`com.dodamdodam.backend.conversation.application.ConversationService`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|시작]] → [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요]] → 이 클래스 · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/90-Reference|전체 참조 자료]]

## 클래스 요약

잠금 아래 신규 시작·같은 키 재전송·충돌을 판단하고 응답 대상도 재검사한다.

- 원본: `src/main/java/com/dodamdodam/backend/conversation/application/ConversationService.java:20`
- 작성 메서드/생성자 6개 · 자동 생성 0개

## 메서드 목록

| 메서드 | 하는 일 |
| --- | --- |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationService#create mf9ff06c04e6d7fad\|create]] | HTTP 진입에서 외부 업무 Tx 없이 호출하며 종료 조정 커밋 후 정상적인 시작 거부를 전달한다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationService#verifyStartResponse macdb3aca10ad50df\|verifyStartResponse]] | 생성 Tx 이후 새 Tx에서 반환 대상만 재검사하며 대화 생성·암묵적 연결은 수행하지 않는다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationService#decideStart m074dea56e9276dfa\|decideStart]] | 자녀 잠금 아래 이전 날짜 종료 조정과 시작 판정을 같은 업무 트랜잭션에서 수행한다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationService#replay m8a9b848e75e75181\|replay]] | 당일 ACTIVE의 현재 연결이 있을 때만 기존 시작 정보를 반환하며 암묵적으로 연결하지 않는다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationService#conflictFor m1c136cdb5700e953\|conflictFor]] | 다른 시작 키의 미종료 대화와 종료된 기존 키를 각각의 업무 충돌로 분류한다. |

> [!example]- 생성자·자동 생성 멤버 1개
>
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationService#생성자 m0fd6b20b047e6ffe|public ConversationService(SessionSecurityRepository security, ChildService children, ConversationRepository repository, ConversationClosureCoordinator closure, PlatformTransactionManager transactions)]]

## create mf9ff06c04e6d7fad

HTTP 진입에서 외부 업무 Tx 없이 호출하며 종료 조정 커밋 후 정상적인 시작 거부를 전달한다.

```java
public com.dodamdodam.backend.conversation.application.ConversationService.StartOutcome create(com.dodamdodam.backend.auth.session.AuthenticatedSession session, java.util.UUID childId, com.dodamdodam.backend.conversation.dto.StartConversationRequest request)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.api.ConversationController#start m255287826e69c022|ConversationController.start]] — 호출 · L41

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationService#decideStart m074dea56e9276dfa|ConversationService.decideStart]] — 호출 · L41
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationService.StartDecision#outcome m96072c60c5023629|StartDecision.outcome]] — 호출 · L46
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationService.StartDecision#rejection m6035e6f5576e1b67|StartDecision.rejection]] — 호출 · L43,44

### 외부 라이브러리 호출

- `java.util.Objects#requireNonNull(T)`
- `org.springframework.transaction.support.TransactionTemplate#execute(org.springframework.transaction.support.TransactionCallback<T>)`

### 소스 코드

`src/main/java/com/dodamdodam/backend/conversation/application/ConversationService.java:40`

```java
public StartOutcome create(AuthenticatedSession session, UUID childId, StartConversationRequest request) {
    var decision = Objects.requireNonNull(transaction.execute(_ -> decideStart(session, childId, request)));
    // 정상적인 409 거부는 종료 조정을 커밋한 뒤 전달하고, 저장 장애는 execute 안에서 전체 롤백한다.
    if (decision.rejection() != null) {
        throw decision.rejection();
    }
    return decision.outcome();
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## verifyStartResponse macdb3aca10ad50df

생성 Tx 이후 새 Tx에서 반환 대상만 재검사하며 대화 생성·암묵적 연결은 수행하지 않는다.

```java
public void verifyStartResponse(com.dodamdodam.backend.auth.session.AuthenticatedSession session, java.util.UUID childId, java.util.UUID clientRequestId, java.util.UUID conversationId)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.api.ConversationStartResponseFilter#verify mbaf83885c918b297|ConversationStartResponseFilter.verify]] — 호출 · L133

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthenticatedSession#accountId m23b42fff4f806999|AuthenticatedSession.accountId]] — 호출 · L53,54,57,60,63
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthenticatedSession#securityContextId me6d8be795777e536|AuthenticatedSession.securityContextId]] — 호출 · L53,57,63
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.SessionSecurityRepository#requireCurrentContextForUpdate m950184451aa631db|SessionSecurityRepository.requireCurrentContextForUpdate]] — 호출 · L53,57,63
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.application.ChildService#requireChildAccessForUpdate m10ec0868c9e8ded5|ChildService.requireChildAccessForUpdate]] — 호출 · L54
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.exception.ChildException#notFound mda3f586558efa3e2|ChildException.notFound]] — 참조 · L56,62
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationClosureCoordinator#reconcile m5c7d3b9b2dd0cb88|ConversationClosureCoordinator.reconcile]] — 호출 · L60
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationService#conflictFor m1c136cdb5700e953|ConversationService.conflictFor]] — 호출 · L71
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationService#replay m8a9b848e75e75181|ConversationService.replay]] — 호출 · L73
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.domain.ConversationServiceHours#isOpen maab448b57fa699a5|ConversationServiceHours.isOpen]] — 호출 · L67
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.domain.ConversationServiceHours#requireOpen mf365fa956cdf2081|ConversationServiceHours.requireOpen]] — 호출 · L73
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.exception.ConversationException#serviceHoursClosed mdd01dc4ac5506f44|ConversationException.serviceHoursClosed]] — 호출 · L68
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.infrastructure.ConversationRepository#currentTime m7f615951fbdd6a0f|ConversationRepository.currentTime]] — 호출 · L58,64
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.infrastructure.ConversationRepository#findRequestForUpdate m8f65c4eacd8961ee|ConversationRepository.findRequestForUpdate]] — 호출 · L55,61
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.infrastructure.ConversationRepository.StoredConversation#conversationId mb76d5928e19ce55a|StoredConversation.conversationId]] — 호출 · L56,62
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.infrastructure.ConversationRepository.StoredConversation#scheduledEndAt m0fa068722b276f2b|StoredConversation.scheduledEndAt]] — 호출 · L59
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.infrastructure.ConversationRepository.StoredConversation#status maab6d0a3b6d2a4ee|StoredConversation.status]] — 호출 · L59,70

### 외부 라이브러리 호출

- `java.lang.String#equals(java.lang.Object)`
- `java.time.OffsetDateTime#isBefore(java.time.OffsetDateTime)`
- `java.util.Optional#filter(java.util.function.Predicate<? super T>)`
- `java.util.Optional#orElseThrow(java.util.function.Supplier<? extends X>)`
- `java.util.UUID#equals(java.lang.Object)`
- `org.springframework.transaction.support.TransactionTemplate#execute(org.springframework.transaction.support.TransactionCallback<T>)`

### 소스 코드

`src/main/java/com/dodamdodam/backend/conversation/application/ConversationService.java:50`

```java
public void verifyStartResponse(AuthenticatedSession session, UUID childId, UUID clientRequestId, UUID conversationId) {
    ConversationException rejection = transaction.execute(_ -> {
        // 기존 변경 경로와 같은 상위 잠금 순서로 재조회해 종료·소유권 변경과 직렬화한다.
        security.requireCurrentContextForUpdate(session.accountId(), session.securityContextId());
        children.requireChildAccessForUpdate(session.accountId(), childId);
        var current = repository.findRequestForUpdate(childId, clientRequestId)
                .filter(row -> row.conversationId().equals(conversationId)).orElseThrow(ChildException::notFound);
        security.requireCurrentContextForUpdate(session.accountId(), session.securityContextId());
        var checkedAt = repository.currentTime();
        if (current.status().equals("CLOSING") || !checkedAt.isBefore(current.scheduledEndAt())) {
            closure.reconcile(session.accountId(), childId, conversationId, ConversationClosureCoordinator.Trigger.OBSERVE);
            current = repository.findRequestForUpdate(childId, clientRequestId)
                    .filter(row -> row.conversationId().equals(conversationId)).orElseThrow(ChildException::notFound);
            security.requireCurrentContextForUpdate(session.accountId(), session.securityContextId());
            checkedAt = repository.currentTime();
        }
        // start의 이용시간 우선순위를 유지하며 종료 정리는 정상 거부 전에 커밋한다.
        if (!ConversationServiceHours.isOpen(checkedAt)) {
            return ConversationException.serviceHoursClosed();
        }
        if (!current.status().equals("ACTIVE")) {
            return conflictFor(current);
        }
        replay(session, current, ConversationServiceHours.requireOpen(checkedAt));
        return null;
    });
    if (rejection != null) {
        throw rejection;
    }
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## decideStart m074dea56e9276dfa

자녀 잠금 아래 이전 날짜 종료 조정과 시작 판정을 같은 업무 트랜잭션에서 수행한다.

```java
private com.dodamdodam.backend.conversation.application.ConversationService.StartDecision decideStart(com.dodamdodam.backend.auth.session.AuthenticatedSession session, java.util.UUID childId, com.dodamdodam.backend.conversation.dto.StartConversationRequest request)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationService#create mf9ff06c04e6d7fad|ConversationService.create]] — 호출 · L41

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthenticatedSession#accountId m23b42fff4f806999|AuthenticatedSession.accountId]] — 호출 · L84,85,91,95,99
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthenticatedSession#securityContextId me6d8be795777e536|AuthenticatedSession.securityContextId]] — 호출 · L84,91,99,125
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.SessionSecurityRepository#requireCurrentContextForUpdate m950184451aa631db|SessionSecurityRepository.requireCurrentContextForUpdate]] — 호출 · L84,91,99
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.application.ChildService#requireChildAccessForUpdate m10ec0868c9e8ded5|ChildService.requireChildAccessForUpdate]] — 호출 · L85
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationClosureCoordinator#reconcile m5c7d3b9b2dd0cb88|ConversationClosureCoordinator.reconcile]] — 호출 · L95
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationService#conflictFor m1c136cdb5700e953|ConversationService.conflictFor]] — 호출 · L108,113,122
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationService#replay m8a9b848e75e75181|ConversationService.replay]] — 호출 · L110
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationService.StartDecision#생성자 ma623afa442cedb87|StartDecision.생성자]] — 생성 · L103,108,110,113,122,131
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationService.StartOutcome#생성자 mb897e2535fd3e442|StartOutcome.생성자]] — 생성 · L131
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.domain.ConversationServiceHours#isOpen maab448b57fa699a5|ConversationServiceHours.isOpen]] — 호출 · L102
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.domain.ConversationServiceHours#requireOpen mf365fa956cdf2081|ConversationServiceHours.requireOpen]] — 호출 · L105,121,127
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.domain.ConversationServiceHours.ServiceWindow#serviceDate m935e99412829c2a1|ServiceWindow.serviceDate]] — 호출 · L128
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.dto.StartConversationRequest#clientRequestId m6d3b72a72eb6f686|StartConversationRequest.clientRequestId]] — 호출 · L88,97,116
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.dto.StartConversationResult#conversationId m3dca6e4445701a55|StartConversationResult.conversationId]] — 호출 · L125
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.exception.ConversationException#생성자 mdf07793b291a512a|ConversationException.생성자]] — 생성 · L120
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.exception.ConversationException#serviceHoursClosed mdd01dc4ac5506f44|ConversationException.serviceHoursClosed]] — 호출 · L103,129
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.infrastructure.ConversationRepository#currentTime m7f615951fbdd6a0f|ConversationRepository.currentTime]] — 호출 · L87,92,100,121,127
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.infrastructure.ConversationRepository#findRequestForUpdate m8f65c4eacd8961ee|ConversationRepository.findRequestForUpdate]] — 호출 · L88,97
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.infrastructure.ConversationRepository#findUnendedForUpdate m395989c791432963|ConversationRepository.findUnendedForUpdate]] — 호출 · L89,98,119
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.infrastructure.ConversationRepository#insertActive mff8e74c752863f8c|ConversationRepository.insertActive]] — 호출 · L116
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.infrastructure.ConversationRepository#insertLink m5d92b93bc37cc1e5|ConversationRepository.insertLink]] — 호출 · L125
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.infrastructure.ConversationRepository.StoredConversation#conversationId mb76d5928e19ce55a|StoredConversation.conversationId]] — 호출 · L95
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.infrastructure.ConversationRepository.StoredConversation#scheduledEndAt m0fa068722b276f2b|StoredConversation.scheduledEndAt]] — 호출 · L94
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.infrastructure.ConversationRepository.StoredConversation#status maab6d0a3b6d2a4ee|StoredConversation.status]] — 호출 · L93,107

### 외부 라이브러리 호출

- `java.lang.IllegalStateException#<init>(java.lang.String)`
- `java.lang.String#equals(java.lang.Object)`
- `java.time.LocalDate#equals(java.lang.Object)`
- `java.time.OffsetDateTime#isBefore(java.time.OffsetDateTime)`
- `java.util.Optional#get()`
- `java.util.Optional#isEmpty()`
- `java.util.Optional#isPresent()`
- `java.util.Optional#orElseThrow()`
- `java.util.Optional#orElseThrow(java.util.function.Supplier<? extends X>)`
- `java.util.UUID#randomUUID()`

### 소스 코드

`src/main/java/com/dodamdodam/backend/conversation/application/ConversationService.java:82`

```java
private StartDecision decideStart(AuthenticatedSession session, UUID childId, StartConversationRequest request) {
    // 계정→보안 문맥→자녀→대화 순으로 필요한 행을 잠근다.
    security.requireCurrentContextForUpdate(session.accountId(), session.securityContextId());
    children.requireChildAccessForUpdate(session.accountId(), childId);
    // 시간 밖에도 자정 정리는 수행하되 시작의 공개 오류는 이용시간을 우선한다.
    var checkedAt = repository.currentTime();
    var requested = repository.findRequestForUpdate(childId, request.clientRequestId());
    var unended = repository.findUnendedForUpdate(childId);
    // 이미 보유한 권한 잠금은 유지하고 자녀·대화 잠금 대기 중 발생한 시간 만료를 다시 검사한다.
    security.requireCurrentContextForUpdate(session.accountId(), session.securityContextId());
    var startedAt = repository.currentTime();
    if (unended.isPresent() && (unended.get().status().equals("CLOSING")
            || !checkedAt.isBefore(unended.get().scheduledEndAt()) || !startedAt.isBefore(unended.get().scheduledEndAt()))) {
        closure.reconcile(session.accountId(), childId, unended.get().conversationId(), ConversationClosureCoordinator.Trigger.OBSERVE);
        // 조정 후 같은 키 ENDED/CLOSING을 우선 판정하고, 실제 미종료가 사라졌을 때만 생성한다.
        requested = repository.findRequestForUpdate(childId, request.clientRequestId());
        unended = repository.findUnendedForUpdate(childId);
        security.requireCurrentContextForUpdate(session.accountId(), session.securityContextId());
        startedAt = repository.currentTime();
    }
    if (!ConversationServiceHours.isOpen(startedAt)) {
        return new StartDecision(null, ConversationException.serviceHoursClosed());
    }
    var window = ConversationServiceHours.requireOpen(startedAt);
    if (requested.isPresent()) {
        if (!requested.get().status().equals("ACTIVE")) {
            return new StartDecision(null, conflictFor(requested.get()));
        }
        return new StartDecision(replay(session, requested.get(), window), null);
    }
    if (unended.isPresent()) {
        return new StartDecision(null, conflictFor(unended.get()));
    }

    var created = repository.insertActive(UUID.randomUUID(), childId, request.clientRequestId(), startedAt, window);
    if (created.isEmpty()) {
        // 명시한 부분 UNIQUE가 막은 경우에만 실제 현재 행을 확인해 업무 충돌로 분류한다.
        var conflict = repository.findUnendedForUpdate(childId)
                .orElseThrow(() -> new ConversationException(new IllegalStateException("미종료 충돌 상태를 확인할 수 없습니다.")));
        ConversationServiceHours.requireOpen(repository.currentTime());
        return new StartDecision(null, conflictFor(conflict));
    }
    var result = created.orElseThrow();
    repository.insertLink(session.securityContextId(), result.conversationId(), startedAt);
    // 저장 도중 자정을 지났다면 커밋하지 않고 대화와 연결을 함께 롤백한다.
    var finishedWindow = ConversationServiceHours.requireOpen(repository.currentTime());
    if (!finishedWindow.serviceDate().equals(window.serviceDate())) {
        throw ConversationException.serviceHoursClosed();
    }
    return new StartDecision(new StartOutcome(result, true), null);
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## replay m8a9b848e75e75181

당일 ACTIVE의 현재 연결이 있을 때만 기존 시작 정보를 반환하며 암묵적으로 연결하지 않는다.

```java
private com.dodamdodam.backend.conversation.application.ConversationService.StartOutcome replay(com.dodamdodam.backend.auth.session.AuthenticatedSession session, com.dodamdodam.backend.conversation.infrastructure.ConversationRepository.StoredConversation conversation, com.dodamdodam.backend.conversation.domain.ConversationServiceHours.ServiceWindow window)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationService#decideStart m074dea56e9276dfa|ConversationService.decideStart]] — 호출 · L110
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationService#verifyStartResponse macdb3aca10ad50df|ConversationService.verifyStartResponse]] — 호출 · L73

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthenticatedSession#accountId m23b42fff4f806999|AuthenticatedSession.accountId]] — 호출 · L141
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthenticatedSession#securityContextId me6d8be795777e536|AuthenticatedSession.securityContextId]] — 호출 · L139,141
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.SessionSecurityRepository#requireCurrentContextForUpdate m950184451aa631db|SessionSecurityRepository.requireCurrentContextForUpdate]] — 호출 · L141
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationService#conflictFor m1c136cdb5700e953|ConversationService.conflictFor]] — 호출 · L137
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationService.StartOutcome#생성자 mb897e2535fd3e442|StartOutcome.생성자]] — 생성 · L149
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.domain.ConversationServiceHours#requireOpen mf365fa956cdf2081|ConversationServiceHours.requireOpen]] — 호출 · L142
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.domain.ConversationServiceHours.ServiceWindow#serviceDate m935e99412829c2a1|ServiceWindow.serviceDate]] — 호출 · L136,143
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.exception.ConversationException#childSessionRequired mfac9ec20240efe61|ConversationException.childSessionRequired]] — 호출 · L147
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.exception.ConversationException#serviceHoursClosed mdd01dc4ac5506f44|ConversationException.serviceHoursClosed]] — 호출 · L144
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.infrastructure.ConversationRepository#currentTime m7f615951fbdd6a0f|ConversationRepository.currentTime]] — 호출 · L142
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.infrastructure.ConversationRepository#hasLinkForUpdate m72ad3628685c5f36|ConversationRepository.hasLinkForUpdate]] — 호출 · L139
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.infrastructure.ConversationRepository.StoredConversation#conversationId mb76d5928e19ce55a|StoredConversation.conversationId]] — 호출 · L139
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.infrastructure.ConversationRepository.StoredConversation#serviceDate m256662d118c9915c|StoredConversation.serviceDate]] — 호출 · L136,143
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.infrastructure.ConversationRepository.StoredConversation#startResult m2a1a09c7685f09a4|StoredConversation.startResult]] — 호출 · L149
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.infrastructure.ConversationRepository.StoredConversation#status maab6d0a3b6d2a4ee|StoredConversation.status]] — 호출 · L136

### 외부 라이브러리 호출

- `java.lang.String#equals(java.lang.Object)`
- `java.time.LocalDate#equals(java.lang.Object)`

### 소스 코드

`src/main/java/com/dodamdodam/backend/conversation/application/ConversationService.java:135`

```java
private StartOutcome replay(AuthenticatedSession session, StoredConversation conversation, ServiceWindow window) {
    if (!"ACTIVE".equals(conversation.status()) || !conversation.serviceDate().equals(window.serviceDate())) {
        throw conflictFor(conversation);
    }
    var linked = repository.hasLinkForUpdate(session.securityContextId(), conversation.conversationId());
    // 연결 행 잠금 대기 중의 문맥 만료·자정 경계도 성공 응답 전에 다시 검사한다.
    security.requireCurrentContextForUpdate(session.accountId(), session.securityContextId());
    var finalWindow = ConversationServiceHours.requireOpen(repository.currentTime());
    if (!finalWindow.serviceDate().equals(conversation.serviceDate())) {
        throw ConversationException.serviceHoursClosed();
    }
    if (!linked) {
        throw ConversationException.childSessionRequired();
    }
    return new StartOutcome(conversation.startResult(), false);
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## conflictFor m1c136cdb5700e953

다른 시작 키의 미종료 대화와 종료된 기존 키를 각각의 업무 충돌로 분류한다.

```java
private com.dodamdodam.backend.conversation.exception.ConversationException conflictFor(com.dodamdodam.backend.conversation.infrastructure.ConversationRepository.StoredConversation conversation)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationService#decideStart m074dea56e9276dfa|ConversationService.decideStart]] — 호출 · L108,113,122
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationService#replay m8a9b848e75e75181|ConversationService.replay]] — 호출 · L137
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationService#verifyStartResponse macdb3aca10ad50df|ConversationService.verifyStartResponse]] — 호출 · L71

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.exception.ConversationException#생성자 mdf07793b291a512a|ConversationException.생성자]] — 생성 · L158
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.exception.ConversationException#activeExists m4697f9606f9c286b|ConversationException.activeExists]] — 호출 · L155
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.exception.ConversationException#closing mf4eb5930af2bafa7|ConversationException.closing]] — 호출 · L156
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.exception.ConversationException#ended m7095f7ecea180249|ConversationException.ended]] — 호출 · L157
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.infrastructure.ConversationRepository.StoredConversation#conversationId mb76d5928e19ce55a|StoredConversation.conversationId]] — 호출 · L155
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.infrastructure.ConversationRepository.StoredConversation#status maab6d0a3b6d2a4ee|StoredConversation.status]] — 호출 · L154

### 외부 라이브러리 호출

- `java.lang.IllegalStateException#<init>(java.lang.String)`

### 소스 코드

`src/main/java/com/dodamdodam/backend/conversation/application/ConversationService.java:153`

```java
private ConversationException conflictFor(StoredConversation conversation) {
    return switch (conversation.status()) {
        case "ACTIVE" -> ConversationException.activeExists(conversation.conversationId());
        case "CLOSING" -> ConversationException.closing();
        case "ENDED" -> ConversationException.ended();
        default -> new ConversationException(new IllegalStateException("지원하지 않는 대화 상태입니다."));
    };
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## 생성자 m0fd6b20b047e6ffe

공통 보안·자녀 정책과 대화 저장소를 같은 업무 트랜잭션에서 사용한다.

```java
public ConversationService(com.dodamdodam.backend.auth.session.SessionSecurityRepository security, com.dodamdodam.backend.child.application.ChildService children, com.dodamdodam.backend.conversation.infrastructure.ConversationRepository repository, com.dodamdodam.backend.conversation.application.ConversationClosureCoordinator closure, org.springframework.transaction.PlatformTransactionManager transactions)
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 외부 라이브러리 호출

- `java.lang.Object#<init>()`
- `org.springframework.transaction.support.TransactionTemplate#<init>(org.springframework.transaction.PlatformTransactionManager)`

### 소스 코드

`src/main/java/com/dodamdodam/backend/conversation/application/ConversationService.java:30`

```java
public ConversationService(SessionSecurityRepository security, ChildService children, ConversationRepository repository,
        ConversationClosureCoordinator closure, PlatformTransactionManager transactions) {
    this.security = security;
    this.children = children;
    this.repository = repository;
    this.closure = closure;
    this.transaction = new TransactionTemplate(transactions);
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]
