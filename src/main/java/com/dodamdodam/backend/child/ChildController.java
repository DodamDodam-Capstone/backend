package com.dodamdodam.backend.child;

import com.dodamdodam.backend.auth.session.AuthenticatedSessionService;
import com.dodamdodam.backend.global.error.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.net.URI;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

@RestController
public class ChildController {

    private final AuthenticatedSessionService authentication;
    private final ChildService children;

    /** 기존 세션 검증과 자녀 등록 서비스를 연결한다. */
    public ChildController(AuthenticatedSessionService authentication, ChildService children) {
        this.authentication = authentication;
        this.children = children;
    }

    /** 로그인 계정에 자녀를 등록하고 공개 프로필과 식별 URI를 반환한다. */
    @PostMapping("/api/v1/children")
    public ResponseEntity<Map<String, ChildView>> create(HttpServletRequest request, @RequestBody JsonNode body) {
        // accountId는 본문 대신 검증한 세션에서만 가져온다.
        var current = authentication.requireAuthenticatedSession(request);
        var child = children.create(current, CreateChildRequest.fromJson(body));
        // Location은 리소스 식별자이며 단건 GET 경로의 추가를 뜻하지 않는다.
        return ResponseEntity.created(URI.create("/api/v1/children/" + child.childId())).body(Map.of("data", child));
    }

    /** 원문을 노출하지 않고 이 컨트롤러의 JSON 읽기 오류만 400으로 처리한다. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> invalidJson(HttpServletResponse response) {
        var invalid = ChildException.validation("body");
        return ResponseEntity.badRequest().body(new ApiErrorResponse(new ApiErrorResponse.ErrorDetail(
                invalid.code(), invalid.getMessage(), response.getHeader("X-Request-ID"), invalid.fields())));
    }
}
