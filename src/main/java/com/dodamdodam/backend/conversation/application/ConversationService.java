package com.dodamdodam.backend.conversation.application;

import com.dodamdodam.backend.auth.session.AuthenticatedSession;
import com.dodamdodam.backend.auth.session.SessionSecurityRepository;
import com.dodamdodam.backend.child.application.ChildService;
import com.dodamdodam.backend.conversation.domain.ConversationServiceHours;
import com.dodamdodam.backend.conversation.domain.ConversationServiceHours.ServiceWindow;
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

    /** 신규 생성과 현재 세션에 연결된 같은 키의 재전송을 구분하며 기존 행은 덮어쓰지 않는다. */
    @Transactional
    public StartOutcome create(AuthenticatedSession session, UUID childId, StartConversationRequest request) {
        // 계정→보안 문맥→자녀→대화 순으로 필요한 행을 잠근다.
        security.requireCurrentContextForUpdate(session.accountId(), session.securityContextId());
        children.requireChildAccessForUpdate(session.accountId(), childId);
        // 시간 밖에서는 시작 키·대화 상태보다 이용시간 오류가 우선한다.
        ConversationServiceHours.requireOpen(repository.currentTime());
        var requested = repository.findRequestForUpdate(childId, request.clientRequestId());
        var unended = requested.isPresent() ? Optional.<StoredConversation>empty() : repository.findUnendedForUpdate(childId);
        // 이미 보유한 권한 잠금은 유지하고 자녀·대화 잠금 대기 중 발생한 시간 만료를 다시 검사한다.
        security.requireCurrentContextForUpdate(session.accountId(), session.securityContextId());
        var startedAt = repository.currentTime();
        var window = ConversationServiceHours.requireOpen(startedAt);
        if (requested.isPresent()) {
            return replay(session, requested.get(), window);
        }
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
        return new StartOutcome(result, true);
    }

    /** 당일 ACTIVE의 현재 연결이 있을 때만 기존 시작 정보를 반환하며 암묵적으로 연결하지 않는다. */
    private StartOutcome replay(AuthenticatedSession session, StoredConversation conversation, ServiceWindow window) {
        if (!"ACTIVE".equals(conversation.status()) || !conversation.serviceDate().equals(window.serviceDate())) {
            // 이전 날짜 ACTIVE의 실제 종료 조정은 Commit 5에서 연결하며 이 단계에서는 복구하지 않는다.
            rejectExisting(conversation);
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
    private void rejectExisting(StoredConversation conversation) {
        throw switch (conversation.status()) {
            case "ACTIVE" -> ConversationException.activeExists(conversation.conversationId());
            case "CLOSING" -> ConversationException.closing();
            case "ENDED" -> ConversationException.ended();
            default -> new ConversationException(new IllegalStateException("지원하지 않는 대화 상태입니다."));
        };
    }

    /** HTTP 상태 선택용 내부 결과이며 created 표시는 공개 data에 포함하지 않는다. */
    public record StartOutcome(StartConversationResult result, boolean created) { }
}
