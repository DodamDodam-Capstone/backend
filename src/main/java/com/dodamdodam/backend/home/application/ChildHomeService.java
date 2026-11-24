package com.dodamdodam.backend.home.application;

import com.dodamdodam.backend.auth.session.AuthenticatedSession;
import com.dodamdodam.backend.auth.session.SessionSecurityRepository;
import com.dodamdodam.backend.child.application.ChildService;
import com.dodamdodam.backend.conversation.application.ConversationClosureCoordinator;
import com.dodamdodam.backend.conversation.application.ConversationClosureCoordinator.Trigger;
import com.dodamdodam.backend.conversation.domain.ConversationServiceHours;
import com.dodamdodam.backend.conversation.infrastructure.ConversationClosureRepository;
import com.dodamdodam.backend.conversation.infrastructure.ConversationRepository;
import com.dodamdodam.backend.conversation.infrastructure.ConversationRepository.StoredConversation;
import com.dodamdodam.backend.home.dto.ChildHome;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChildHomeService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private final SessionSecurityRepository security;
    private final ChildService children;
    private final ConversationRepository conversations;
    private final ConversationClosureRepository expiration;
    private final ConversationClosureCoordinator closure;

    /** 기존 접근 검사·대화 조회·조건부 정리만 재사용한다. */
    public ChildHomeService(SessionSecurityRepository security, ChildService children, ConversationRepository conversations,
            ConversationClosureRepository expiration, ConversationClosureCoordinator closure) {
        this.security = security;
        this.children = children;
        this.conversations = conversations;
        this.expiration = expiration;
        this.closure = closure;
    }

    /** 현재 로그인 문맥과 소유 아이를 검증하고 필요한 기존 정리까지 같은 쓰기 가능 Tx에서 수행한다. */
    @Transactional
    public ChildHome getHome(AuthenticatedSession session, UUID childId) {
        // 계정→보안 문맥→아이→대화→발화→연결 순서로 필요한 행만 잠근다.
        security.requireCurrentContextForUpdate(session.accountId(), session.securityContextId());
        children.requireChildAccessForUpdate(session.accountId(), childId);
        var context = children.getChildContext(childId);
        var unended = conversations.findUnendedForUpdate(childId);
        var checkedAt = checkedTime(session);
        if (unended.isPresent()) {
            settle(session, childId, unended.orElseThrow(), checkedAt);
            unended = conversations.findUnendedForUpdate(childId);
            checkedAt = checkedTime(session);
        }

        UUID activeId = null;
        if (unended.isPresent() && isAccessibleActive(unended.orElseThrow(), checkedAt)
                && conversations.hasLinkForUpdate(session.securityContextId(), unended.orElseThrow().conversationId())) {
            activeId = unended.orElseThrow().conversationId();
        }

        // 연결 잠금 대기 뒤에도 문맥 만료·자정을 재검사하며 시간 외를 예외로 바꾸지 않는다.
        checkedAt = checkedTime(session);
        if (unended.isPresent() && "ACTIVE".equals(unended.orElseThrow().status())
                && !checkedAt.isBefore(unended.orElseThrow().scheduledEndAt())) {
            // ACTIVE 경로에서 발화 잠금은 이미 보유하므로 연결 뒤의 자정 정리도 역순 잠금이 아니다.
            closure.reconcile(session.accountId(), childId, unended.orElseThrow().conversationId(), Trigger.OBSERVE);
            unended = conversations.findUnendedForUpdate(childId);
            checkedAt = checkedTime(session);
            activeId = null;
        }
        if (activeId != null && (unended.isEmpty() || !isAccessibleActive(unended.orElseThrow(), checkedAt))) {
            activeId = null;
        }
        boolean closing = unended.isPresent() && "CLOSING".equals(unended.orElseThrow().status());
        return ChildHome.from(context, checkedAt, activeId, closing);
    }

    /** 종료 대상의 만료는 조정기에 맡기고, 아직 ACTIVE인 대화의 조건부 만료만 직접 재사용한다. */
    private void settle(AuthenticatedSession session, UUID childId, StoredConversation conversation, OffsetDateTime checkedAt) {
        if ("CLOSING".equals(conversation.status()) || !checkedAt.isBefore(conversation.scheduledEndAt())) {
            closure.reconcile(session.accountId(), childId, conversation.conversationId(), Trigger.OBSERVE);
            return;
        }
        if ("ACTIVE".equals(conversation.status())) {
            expiration.lockProcessingTurns(conversation.conversationId());
            checkedAt = checkedTime(session);
            if (!checkedAt.isBefore(conversation.scheduledEndAt())) {
                closure.reconcile(session.accountId(), childId, conversation.conversationId(), Trigger.OBSERVE);
                return;
            }
            expiration.expireProcessing(conversation.conversationId(), checkedAt);
        }
    }

    /** 잠금 대기가 끝난 현재 문맥을 재검사한 뒤 DB 시각을 새로 읽는다. */
    private OffsetDateTime checkedTime(AuthenticatedSession session) {
        security.requireCurrentContextForUpdate(session.accountId(), session.securityContextId());
        return conversations.currentTime();
    }

    /** 당일 서비스 시간의 미종료 ACTIVE만 기존 세션 연결 확인 대상으로 삼는다. */
    private boolean isAccessibleActive(StoredConversation conversation, OffsetDateTime checkedAt) {
        return "ACTIVE".equals(conversation.status()) && ConversationServiceHours.isOpen(checkedAt)
                && conversation.serviceDate().equals(checkedAt.atZoneSameInstant(KST).toLocalDate())
                && checkedAt.isBefore(conversation.scheduledEndAt());
    }
}
