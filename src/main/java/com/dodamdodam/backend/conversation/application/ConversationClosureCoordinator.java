package com.dodamdodam.backend.conversation.application;

import com.dodamdodam.backend.child.application.ChildService;
import com.dodamdodam.backend.child.exception.ChildException;
import com.dodamdodam.backend.conversation.infrastructure.ConversationClosureProperties;
import com.dodamdodam.backend.conversation.infrastructure.ConversationClosureRepository;
import com.dodamdodam.backend.conversation.infrastructure.ConversationClosureRepository.ClosureState;
import com.dodamdodam.backend.conversation.infrastructure.ConversationRepository;
import java.util.UUID;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@EnableConfigurationProperties(ConversationClosureProperties.class)
public class ConversationClosureCoordinator {

    private final ChildService children;
    private final ConversationRepository clock;
    private final ConversationClosureRepository repository;
    private final ConversationClosureProperties properties;

    /** 시작·수동 종료·타이머·발화 완료가 같은 DB 전이와 명시적 요약 기한을 사용한다. */
    public ConversationClosureCoordinator(ChildService children, ConversationRepository clock,
            ConversationClosureRepository repository, ConversationClosureProperties properties) {
        this.children = children;
        this.clock = clock;
        this.repository = repository;
        this.properties = properties;
    }

    /** 신뢰된 내부 호출용이며 사용자 경로의 로그인·연결 검사는 호출자가 같은 Tx에서 선행한다. */
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

    /** 미존재·다른 자녀의 대화는 같은 404로 처리하고 부모-자식 관계가 없는 상태는 반환하지 않는다. */
    private ClosureState requireState(UUID childId, UUID conversationId) {
        return repository.findForUpdate(childId, conversationId).orElseThrow(ChildException::notFound);
    }

    /** 자동 관측과 명시적 종료 요청을 구분하며 해당 API나 worker 자체를 구현하지 않는다. */
    public enum Trigger { OBSERVE, REQUEST_END }

    /** endedNow는 최초 전이 실행자 표시이며 요약 AI 실행·성공을 의미하지 않는다. */
    public record ClosureResult(ClosureState state, boolean endedNow) { }
}
