package com.dodamdodam.backend.conversation.application;

import com.dodamdodam.backend.auth.session.AuthenticatedSession;
import com.dodamdodam.backend.auth.session.SessionSecurityRepository;
import com.dodamdodam.backend.child.application.ChildService;
import com.dodamdodam.backend.conversation.domain.ConversationServiceHours;
import com.dodamdodam.backend.conversation.dto.StartConversationRequest;
import com.dodamdodam.backend.conversation.dto.StartConversationResult;
import com.dodamdodam.backend.conversation.exception.ConversationException;
import com.dodamdodam.backend.conversation.infrastructure.ConversationRepository;
import com.dodamdodam.backend.conversation.infrastructure.ConversationRepository.StoredConversation;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConversationService {

    private final SessionSecurityRepository security;
    private final ChildService children;
    private final ConversationRepository repository;

    /** 공통 보안·자녀 정책과 대화 저장소를 같은 업무 트랜잭션에서 사용한다. */
    public ConversationService(SessionSecurityRepository security, ChildService children, ConversationRepository repository) {
        this.security = security;
        this.children = children;
        this.repository = repository;
    }

    /** 새 대화와 현재 세션 연결을 원자적으로 저장하며 기존 대화는 덮어쓰지 않는다. */
    @Transactional
    public StartConversationResult create(AuthenticatedSession session, UUID childId, StartConversationRequest request) {
        // 계정→보안 문맥→자녀→대화 순으로 필요한 행을 잠근다.
        security.requireCurrentContextForUpdate(session.accountId(), session.securityContextId());
        children.requireChildAccessForUpdate(session.accountId(), childId);
        var requested = repository.findRequestForUpdate(childId, request.clientRequestId());
        var unended = requested.isPresent() ? Optional.<StoredConversation>empty() : repository.findUnendedForUpdate(childId);
        // 이미 보유한 권한 잠금은 유지하고 자녀·대화 잠금 대기 중 발생한 시간 만료를 다시 검사한다.
        security.requireCurrentContextForUpdate(session.accountId(), session.securityContextId());
        var startedAt = repository.currentTime();
        var window = ConversationServiceHours.requireOpen(startedAt);
        requested.ifPresent(this::rejectExisting);
        unended.ifPresent(this::rejectExisting);

        var created = repository.insertActive(UUID.randomUUID(), childId, request.clientRequestId(), startedAt, window);
        if (created.isEmpty()) {
            // 명시한 부분 UNIQUE가 막은 경우에만 실제 현재 행을 확인해 업무 충돌로 분류한다.
            var conflict = repository.findUnendedForUpdate(childId)
                    .orElseThrow(() -> new ConversationException(new IllegalStateException("미종료 충돌 상태를 확인할 수 없습니다.")));
            ConversationServiceHours.requireOpen(repository.currentTime());
            rejectExisting(conflict);
        }
        var result = created.orElseThrow();
        repository.insertLink(session.securityContextId(), result.conversationId(), startedAt);
        // 저장 도중 자정을 지났다면 커밋하지 않고 대화와 연결을 함께 롤백한다.
        var finishedWindow = ConversationServiceHours.requireOpen(repository.currentTime());
        if (!finishedWindow.serviceDate().equals(window.serviceDate())) {
            throw ConversationException.serviceHoursClosed();
        }
        return result;
    }

    /** 이 단계에서는 미종료를 거부하며 동일 키 ACTIVE 재전송의 복구는 후속 멱등성 구현에서 처리한다. */
    private void rejectExisting(StoredConversation conversation) {
        throw switch (conversation.status()) {
            case "ACTIVE" -> ConversationException.activeExists(conversation.conversationId());
            case "CLOSING" -> ConversationException.closing();
            case "ENDED" -> ConversationException.ended();
            default -> new ConversationException(new IllegalStateException("지원하지 않는 대화 상태입니다."));
        };
    }
}
