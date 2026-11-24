package com.dodamdodam.backend.home.api;

import com.dodamdodam.backend.auth.session.AuthenticatedSessionService;
import com.dodamdodam.backend.child.application.ChildService;
import com.dodamdodam.backend.conversation.dto.StartConversationRequest;
import com.dodamdodam.backend.home.application.ChildHomeService;
import com.dodamdodam.backend.home.dto.ChildHome;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** 기존 인증·아이 소유권 검증을 홈 조회 서비스에 연결한다. */
@RestController
public class ChildHomeController {

    private final AuthenticatedSessionService authentication;
    private final ChildService children;
    private final ChildHomeService homes;

    /** 공통 인증과 자녀 정책·홈 서비스를 그대로 재사용한다. */
    public ChildHomeController(AuthenticatedSessionService authentication, ChildService children, ChildHomeService homes) {
        this.authentication = authentication;
        this.children = children;
        this.homes = homes;
    }

    /** 인증→UUID 형식→소유권을 확인하고 시간 외·CLOSING도 현재 홈 상태를 200으로 반환한다. */
    @GetMapping("/api/v1/children/{childId}/home")
    public ResponseEntity<Map<String, ChildHome>> getHome(HttpServletRequest request, @PathVariable String childId) {
        var current = authentication.requireAuthenticatedSession(request);
        var parsedChildId = StartConversationRequest.parseChildId(childId);
        children.requireChildAccess(current.accountId(), parsedChildId);
        var home = homes.getHome(current, parsedChildId);
        ChildHomeResponseFilter.protect(request, current, parsedChildId);
        return ResponseEntity.ok(Map.of("data", home));
    }
}
