package com.dodamdodam.backend.conversation.application;

import com.dodamdodam.backend.auth.session.AuthenticatedSession;
import com.dodamdodam.backend.auth.session.AuthStateUnavailableException;
import com.dodamdodam.backend.child.application.ChildService;
import com.dodamdodam.backend.child.exception.ChildException;
import com.dodamdodam.backend.conversation.dto.ConversationDetailView;
import com.dodamdodam.backend.conversation.dto.ConversationPageView;
import com.dodamdodam.backend.conversation.dto.GuardianConversationListRequest;
import com.dodamdodam.backend.conversation.exception.ConversationException;
import com.dodamdodam.backend.conversation.infrastructure.GuardianConversationRepository;
import com.dodamdodam.backend.guardian.application.GuardianAccessService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

@Service
public class GuardianConversationService {

    private final ChildService children;
    private final GuardianAccessService guardianAccess;
    private final GuardianConversationRepository repository;

    /** 기존 소유권·보호자 확인 정책과 읽기 저장소를 연결한다. */
    public GuardianConversationService(ChildService children, GuardianAccessService guardianAccess,
                                       GuardianConversationRepository repository) {
        this.children = children;
        this.guardianAccess = guardianAccess;
        this.repository = repository;
    }

    /** 소유권 오류를 보호자 확인보다 먼저 판정하고 한 행을 더 조회해 다음 페이지를 판단한다. */
    public ConversationPageView list(HttpServletRequest request, AuthenticatedSession authenticated, UUID childId,
                                     GuardianConversationListRequest query) {
        requireOwnedChild(authenticated, childId);
        requireGuardianAccess(request, authenticated);
        var items = repository.listEnded(authenticated.accountId(), childId, query.from(), query.to(),
                query.keyword(), query.offset(), query.size() + 1);
        return new ConversationPageView(items.subList(0, Math.min(items.size(), query.size())),
                query.page(), query.size(), items.size() > query.size());
    }

    /** 부재·미소유·조합 불일치는 같은 404로 처리하고 PIN 확인 뒤 종료 기록을 조회한다. */
    public ConversationDetailView detail(HttpServletRequest request, AuthenticatedSession authenticated, UUID childId,
                                         UUID conversationId, int afterSequence, int size) {
        try {
            requireOwnedChild(authenticated, childId);
        }
        catch (ChildException failure) {
            if ("RESOURCE_NOT_FOUND".equals(failure.code())) {
                throw ConversationException.notFound();
            }
            throw failure;
        }
        var accountId = authenticated.accountId();
        if (!repository.hasConversation(accountId, childId, conversationId)) {
            throw ConversationException.notFound();
        }
        requireGuardianAccess(request, authenticated);
        var conversation = repository.findEnded(accountId, childId, conversationId)
                .orElseThrow(ConversationException::notFound);
        // ENDED 이후 발화는 바뀌지 않으므로 대화나 세션 행에 쓰기 잠금을 잡지 않는다.
        var candidates = repository.listEndedTurns(accountId, childId, conversationId, afterSequence, size + 1);
        // 추가 행은 다음 페이지 유무만 나타내므로 커서는 실제 반환한 마지막 발화를 가리킨다.
        boolean hasNext = candidates.size() > size;
        var turns = hasNext ? candidates.subList(0, size) : candidates;
        return new ConversationDetailView(conversation, List.copyOf(turns), hasNext,
                hasNext ? turns.getLast().sequence() : null);
    }

    /** 미소유와 부재를 공통 404로 처리한다. */
    public void requireOwnedChild(AuthenticatedSession authenticated, UUID childId) {
        try {
            children.requireChildAccess(authenticated.accountId(), childId);
        }
        catch (ChildException failure) {
            if (failure.getCause() instanceof DataAccessException unavailable) {
                // 기록 접근에 필요한 소유권을 확인할 수 없는 저장소 장애는 503으로 전달한다.
                throw new AuthStateUnavailableException(unavailable);
            }
            throw failure;
        }
    }

    /** 현재 문맥의 PIN 확인을 요구하고 기존 응답 직전 재검사에 등록한다. */
    public void requireGuardianAccess(HttpServletRequest request, AuthenticatedSession authenticated) {
        guardianAccess.requireGuardianAccess(request, authenticated);
    }
}
