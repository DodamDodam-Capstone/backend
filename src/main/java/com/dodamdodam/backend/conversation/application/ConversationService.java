package com.dodamdodam.backend.conversation.application;

import com.dodamdodam.backend.auth.session.AuthenticatedSession;
import com.dodamdodam.backend.auth.session.SessionSecurityRepository;
import com.dodamdodam.backend.child.application.ChildService;
import com.dodamdodam.backend.child.exception.ChildException;
import com.dodamdodam.backend.conversation.domain.ConversationServiceHours;
import com.dodamdodam.backend.conversation.domain.ConversationServiceHours.ServiceWindow;
import com.dodamdodam.backend.conversation.dto.StartConversationRequest;
import com.dodamdodam.backend.conversation.dto.StartConversationResult;
import com.dodamdodam.backend.conversation.exception.ConversationException;
import com.dodamdodam.backend.conversation.infrastructure.ConversationRepository;
import com.dodamdodam.backend.conversation.infrastructure.ConversationRepository.StoredConversation;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class ConversationService {

    private final SessionSecurityRepository security;
    private final ChildService children;
    private final ConversationRepository repository;
    private final ConversationClosureCoordinator closure;
    private final TransactionTemplate transaction;

    /** 공통 보안·자녀 정책과 대화 저장소를 같은 업무 트랜잭션에서 사용한다. */
    public ConversationService(SessionSecurityRepository security, ChildService children, ConversationRepository repository,
            ConversationClosureCoordinator closure, PlatformTransactionManager transactions) {
        this.security = security;
        this.children = children;
        this.repository = repository;
        this.closure = closure;
        this.transaction = new TransactionTemplate(transactions);
    }

    /** HTTP 진입에서 외부 업무 Tx 없이 호출하며 종료 조정 커밋 후 정상적인 시작 거부를 전달한다. */
    public StartOutcome create(AuthenticatedSession session, UUID childId, StartConversationRequest request) {
        var decision = Objects.requireNonNull(transaction.execute(_ -> decideStart(session, childId, request)));
        // 정상적인 409 거부는 종료 조정을 커밋한 뒤 전달하고, 저장 장애는 execute 안에서 전체 롤백한다.
        if (decision.rejection() != null) {
            throw decision.rejection();
        }
        return decision.outcome();
    }

    /** 생성 Tx 이후 새 Tx에서 반환 대상만 재검사하며 대화 생성·암묵적 연결은 수행하지 않는다. */
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

    /** 자녀 잠금 아래 이전 날짜 종료 조정과 시작 판정을 같은 업무 트랜잭션에서 수행한다. */
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

    /** 당일 ACTIVE의 현재 연결이 있을 때만 기존 시작 정보를 반환하며 암묵적으로 연결하지 않는다. */
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

    /** 다른 시작 키의 미종료 대화와 종료된 기존 키를 각각의 업무 충돌로 분류한다. */
    private ConversationException conflictFor(StoredConversation conversation) {
        return switch (conversation.status()) {
            case "ACTIVE" -> ConversationException.activeExists(conversation.conversationId());
            case "CLOSING" -> ConversationException.closing();
            case "ENDED" -> ConversationException.ended();
            default -> new ConversationException(new IllegalStateException("지원하지 않는 대화 상태입니다."));
        };
    }

    /** HTTP 상태 선택용 내부 결과이며 created 표시는 공개 data에 포함하지 않는다. */
    public record StartOutcome(StartConversationResult result, boolean created) { }

    /** HTTP 거부와 저장 장애를 분리해 종료 조정의 정상 커밋을 보존한다. */
    private record StartDecision(StartOutcome outcome, ConversationException rejection) { }
}
