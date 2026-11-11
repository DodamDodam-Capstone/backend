package com.dodamdodam.backend.conversation.api;

import com.dodamdodam.backend.auth.session.AuthenticatedSession;
import com.dodamdodam.backend.auth.session.AuthenticatedSessionService;
import com.dodamdodam.backend.child.application.ChildService;
import com.dodamdodam.backend.conversation.application.ConversationService;
import com.dodamdodam.backend.conversation.dto.StartConversationRequest;
import com.dodamdodam.backend.conversation.dto.StartConversationResult;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

/** 공통 접근 검증을 거친 대화 시작 요청을 실제 생성 트랜잭션에 연결한다. */
@RestController
public class ConversationController {

    private final AuthenticatedSessionService authentication;
    private final ChildService children;
    private final ConversationService conversations;

    /** 기존 로그인·자녀 소유권 정책을 재사용한다. */
    public ConversationController(AuthenticatedSessionService authentication, ChildService children,
                                  ConversationService conversations) {
        this.authentication = authentication;
        this.children = children;
        this.conversations = conversations;
    }

    /** 신규 생성은 201과 Location, 같은 키 재전송은 기존 시작 정보만 200으로 반환한다. */
    @PostMapping("/api/v1/children/{childId}/conversations")
    public ResponseEntity<Map<String, StartConversationResult>> start(HttpServletRequest request,
            @PathVariable String childId, @RequestBody JsonNode body) {
        var prepared = prepareStart(request, childId, body);
        var outcome = conversations.create(prepared.session(), prepared.childId(), prepared.input());
        var bodyData = Map.of("data", outcome.result());
        if (!outcome.created()) {
            return ResponseEntity.ok(bodyData);
        }
        var location = URI.create("/api/v1/children/" + prepared.childId() + "/conversations/" + outcome.result().conversationId());
        return ResponseEntity.created(location).body(bodyData);
    }

    /** 인증→구조→소유권 순서로 검증한 내부 입력을 준비하며 성공 응답이나 ID를 만들지 않는다. */
    public PreparedStart prepareStart(HttpServletRequest request, String childId, JsonNode body) {
        var current = authentication.requireAuthenticatedSession(request);
        var parsedChildId = StartConversationRequest.parseChildId(childId);
        var input = StartConversationRequest.fromJson(body);
        var child = children.requireChildAccess(current.accountId(), parsedChildId);
        // CSRF는 기존 Security 필터가 처리하며, 최종 시간·문맥 확인은 생성 트랜잭션에 남긴다.
        return new PreparedStart(current, child.childId(), input);
    }

    /** 생성 서비스에 전달할 내부 문맥이며 공개 API 응답으로 직렬화하지 않는다. */
    public record PreparedStart(AuthenticatedSession session, UUID childId, StartConversationRequest input) { }
}
