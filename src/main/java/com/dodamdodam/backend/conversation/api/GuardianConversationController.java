package com.dodamdodam.backend.conversation.api;

import com.dodamdodam.backend.auth.session.AuthenticatedSessionService;
import com.dodamdodam.backend.conversation.application.GuardianConversationService;
import com.dodamdodam.backend.conversation.dto.ConversationDetailView;
import com.dodamdodam.backend.conversation.dto.ConversationPageView;
import com.dodamdodam.backend.conversation.dto.GuardianConversationListRequest;
import com.dodamdodam.backend.conversation.dto.StartConversationRequest;
import com.dodamdodam.backend.conversation.exception.ConversationException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GuardianConversationController {

    private static final Pattern NONNEGATIVE_INTEGER = Pattern.compile("[0-9]+");

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
                request.getParameter("page"), request.getParameter("size"), request.getParameter("q"));
        return Map.of("data", conversations.list(request, authenticated, child, query));
    }

    /** 로그인 뒤 경로와 커서를 검증하고 종료된 대화의 다음 발화 페이지를 반환한다. */
    @GetMapping("/api/v1/children/{childId}/conversations/{conversationId}")
    public Map<String, ConversationDetailView> detail(HttpServletRequest request,
            @PathVariable String childId, @PathVariable String conversationId,
            @RequestParam(required = false) String afterSequence, @RequestParam(required = false) String size) {
        var authenticated = authentication.requireAuthenticatedSession(request);
        var parsedChildId = parseUuid(childId, "childId");
        var parsedConversationId = parseUuid(conversationId, "conversationId");
        var cursor = parseDetailNumber(afterSequence, "afterSequence", 0);
        var pageSize = parseDetailNumber(size, "size", 100);
        if (pageSize < 1 || pageSize > 100) {
            throw ConversationException.validation("size");
        }
        return Map.of("data", conversations.detail(request, authenticated, parsedChildId, parsedConversationId,
                cursor, pageSize));
    }

    /** 생략한 값에만 기본값을 쓰고 순번 상한 뒤의 커서는 빈 페이지로 이어지게 한다. */
    private static int parseDetailNumber(String value, String field, int defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (!NONNEGATIVE_INTEGER.matcher(value).matches()) {
            throw ConversationException.validation(field);
        }
        try {
            return Integer.parseInt(value);
        }
        catch (NumberFormatException overflow) {
            // 입력은 이미 0 이상의 정수다. DB가 저장할 수 있는 마지막 순번도 이 커서에는 포함되지 않는다.
            return Integer.MAX_VALUE;
        }
    }

    private static UUID parseUuid(String value, String field) {
        return StartConversationRequest.parseUuid(value, field);
    }
}
