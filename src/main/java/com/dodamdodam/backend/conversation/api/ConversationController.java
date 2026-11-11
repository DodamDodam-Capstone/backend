package com.dodamdodam.backend.conversation.api;

import com.dodamdodam.backend.auth.session.AuthenticatedSession;
import com.dodamdodam.backend.auth.session.AuthenticatedSessionService;
import com.dodamdodam.backend.child.application.ChildService;
import com.dodamdodam.backend.conversation.dto.StartConversationRequest;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

/** 생성 서비스 연결 전에는 운영 POST 매핑을 등록하지 않는 시작 요청 검증 기반이다. */
@RestController
public class ConversationController {

    private final AuthenticatedSessionService authentication;
    private final ChildService children;

    /** 기존 로그인·자녀 소유권 정책을 재사용한다. */
    public ConversationController(AuthenticatedSessionService authentication, ChildService children) {
        this.authentication = authentication;
        this.children = children;
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
