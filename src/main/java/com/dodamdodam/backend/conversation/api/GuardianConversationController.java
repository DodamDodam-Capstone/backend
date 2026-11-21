package com.dodamdodam.backend.conversation.api;

import com.dodamdodam.backend.auth.session.AuthenticatedSessionService;
import com.dodamdodam.backend.conversation.application.GuardianConversationService;
import com.dodamdodam.backend.conversation.dto.ConversationPageView;
import com.dodamdodam.backend.conversation.dto.GuardianConversationListRequest;
import com.dodamdodam.backend.conversation.dto.StartConversationRequest;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GuardianConversationController {

    private final AuthenticatedSessionService authentication;
    private final GuardianConversationService conversations;

    /** 현재 로그인과 보호자 기록 조회를 연결한다. */
    public GuardianConversationController(AuthenticatedSessionService authentication,
                                          GuardianConversationService conversations) {
        this.authentication = authentication;
        this.conversations = conversations;
    }

    /** 로그인 확인 뒤 입력을 해석하고 소유권·보호자 확인을 통과한 종료 기록을 반환한다. */
    @GetMapping("/api/v1/children/{childId}/conversations")
    public Map<String, ConversationPageView> list(HttpServletRequest request, @PathVariable String childId) {
        var authenticated = authentication.requireAuthenticatedSession(request);
        var child = parseUuid(childId, "childId");
        var query = GuardianConversationListRequest.fromQuery(request.getParameter("from"), request.getParameter("to"),
                request.getParameter("page"), request.getParameter("size"));
        return Map.of("data", conversations.list(request, authenticated, child, query));
    }

    private static UUID parseUuid(String value, String field) {
        return StartConversationRequest.parseUuid(value, field);
    }
}
