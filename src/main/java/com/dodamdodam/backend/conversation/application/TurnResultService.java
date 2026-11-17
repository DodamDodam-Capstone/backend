package com.dodamdodam.backend.conversation.application;

import com.dodamdodam.backend.child.application.ChildService;
import com.dodamdodam.backend.child.exception.ChildException;
import com.dodamdodam.backend.conversation.application.AllowedTextPolicy.AllowedText;
import com.dodamdodam.backend.conversation.application.ConversationClosureCoordinator.ClosureResult;
import com.dodamdodam.backend.conversation.application.ConversationClosureCoordinator.Trigger;
import com.dodamdodam.backend.conversation.application.TurnAcceptanceService.AcceptedTurn;
import com.dodamdodam.backend.conversation.domain.TurnStatus;
import com.dodamdodam.backend.conversation.exception.ConversationException;
import com.dodamdodam.backend.conversation.infrastructure.ConversationClosureRepository;
import com.dodamdodam.backend.conversation.infrastructure.ConversationRepository;
import com.dodamdodam.backend.conversation.infrastructure.ConversationRepository.StoredConversation;
import com.dodamdodam.backend.conversation.infrastructure.ConversationTurnRepository;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class TurnResultService {

    private static final Set<String> FAILURE_CODES = Set.of("STT_NO_SPEECH", "AI_UPSTREAM_FAILED");
    private static final AllowedText OMITTED = new AllowedTextPolicy().select(null, null, null, null);
    private final ChildService children;
    private final ConversationRepository conversations;
    private final ConversationTurnRepository turns;
    private final ConversationClosureRepository closureRepository;
    private final ConversationClosureCoordinator closure;
    private final TransactionTemplate transaction;

    /** 기접수 결과·ACTIVE 만료·종료 정리를 같은 업무 트랜잭션에 연결한다. */
    public TurnResultService(ChildService children, ConversationRepository conversations, ConversationTurnRepository turns,
            ConversationClosureRepository closureRepository, ConversationClosureCoordinator closure, PlatformTransactionManager transactions) {
        this.children = children;
        this.conversations = conversations;
        this.turns = turns;
        this.closureRepository = closureRepository;
        this.closure = closure;
        this.transaction = new TransactionTemplate(transactions);
    }

    /** 최초 접수 문맥으로 성공을 확정하며 반환값은 아이에게 내용을 전달할 권한이 아니다. */
    @Transactional(propagation = Propagation.NEVER)
    public Settlement succeed(AcceptedTurn permit, AllowedText child, AllowedText reply) {
        return complete(permit, TurnStatus.SUCCEEDED, null, child, reply);
    }

    /** 명세의 STT/AI/TTS 실패 코드만 받으며 전체 기한 만료는 DB 시각으로 별도 판정한다. */
    @Transactional(propagation = Propagation.NEVER)
    public Settlement fail(AcceptedTurn permit, String errorCode, AllowedText child, AllowedText reply) {
        if (errorCode == null || !FAILURE_CODES.contains(errorCode)) {
            throw ConversationException.validation("errorCode");
        }
        return complete(permit, TurnStatus.FAILED, errorCode, child, reply);
    }

    /** 신뢰된 DB 후보의 상위 ID만 받는 내부 정리 진입점이며 sweep·AI 재호출·아이 내용 조회를 수행하지 않는다. */
    @Transactional(propagation = Propagation.NEVER)
    public Expiration expireOverdue(UUID accountId, UUID childId, UUID conversationId) {
        Objects.requireNonNull(accountId, "정리 대상 계정이 필요합니다.");
        Objects.requireNonNull(childId, "정리 대상 아이가 필요합니다.");
        Objects.requireNonNull(conversationId, "정리 대상 대화가 필요합니다.");
        return Objects.requireNonNull(transaction.execute(_ -> {
            lockParents(accountId, null, childId, conversationId);
            closureRepository.lockProcessingTurns(conversationId);
            int expired = closureRepository.expireProcessing(conversationId, conversations.currentTime());
            var reconciled = closure.reconcile(accountId, childId, conversationId, Trigger.OBSERVE);
            return new Expiration(expired, reconciled);
        }));
    }

    /** 공개 결과 대신 상태·변경 여부만 반환하여 저장과 HTTP 전달을 분리한다. */
    private Settlement complete(AcceptedTurn permit, TurnStatus status, String errorCode, AllowedText child, AllowedText reply) {
        Objects.requireNonNull(permit, "최초 신규 접수에서 발급한 결과 저장 문맥이 필요합니다.");
        return Objects.requireNonNull(transaction.execute(_ -> {
            var conversation = lockParents(permit.accountId(), permit.securityContextId(), permit.childId(), permit.conversationId());
            var stored = turns.findForUpdate(permit.conversationId(), permit.turnId()).orElseThrow(ChildException::notFound);
            // 모든 상위·발화 잠금 대기가 끝난 실제 DB 시각만 사용한다.
            var checkedAt = conversations.currentTime();
            int updated = 0;
            if (stored.view().status() == TurnStatus.PROCESSING && stored.view().completedAt() == null
                    && !"ENDED".equals(conversation.status())) {
                if (checkedAt.isBefore(stored.processingDeadlineAt())) {
                    updated = turns.completeProcessing(permit.conversationId(), permit.turnId(), status, errorCode,
                            child == null ? OMITTED : child, reply == null ? OMITTED : reply, checkedAt);
                } else {
                    updated = closureRepository.expireProcessing(permit.conversationId(), checkedAt);
                }
            }
            var result = turns.findForUpdate(permit.conversationId(), permit.turnId()).orElseThrow(ChildException::notFound);
            // 계정·문맥·아이·대화를 이미 보유하므로 조정기가 상위 잠금을 새로 역순 획득하지 않는다.
            var reconciled = closure.reconcile(permit.accountId(), permit.childId(), permit.conversationId(), Trigger.OBSERVE);
            return new Settlement(result.view().turnId(), result.view().status(), updated == 1, reconciled);
        }));
    }

    /** 계정→원래 문맥(있는 경우)→소유 아이→대화 순서이며 현재 로그인 유효성을 저장 조건으로 사용하지 않는다. */
    private StoredConversation lockParents(UUID accountId, UUID securityContextId, UUID childId, UUID conversationId) {
        if (!closureRepository.lockAccount(accountId)) {
            throw ChildException.notFound();
        }
        if (securityContextId != null) {
            turns.lockAcceptedContext(accountId, securityContextId);
        }
        children.requireChildAccessForUpdate(accountId, childId);
        return conversations.findForUpdate(childId, conversationId).orElseThrow(ChildException::notFound);
    }

    /** 저장 확정 메타데이터이며 TurnView·텍스트·현재 전달 권한을 포함하지 않는다. */
    public record Settlement(UUID turnId, TurnStatus status, boolean changed, ClosureResult closure) { }

    /** 정리 결과의 영향 행 수와 종료 확정 표시만 제공한다. */
    public record Expiration(int expired, ClosureResult closure) { }
}
