package com.dodamdodam.backend.conversation.api;

import com.dodamdodam.backend.auth.session.AuthStateUnavailableException;
import com.dodamdodam.backend.auth.session.AuthenticatedSession;
import com.dodamdodam.backend.auth.session.AuthenticatedSessionService;
import com.dodamdodam.backend.auth.session.SessionCookieRenewalFilter.InvalidSessionException;
import com.dodamdodam.backend.child.api.ChildExceptionHandler;
import com.dodamdodam.backend.child.exception.ChildException;
import com.dodamdodam.backend.conversation.application.ConversationService;
import com.dodamdodam.backend.conversation.exception.ConversationException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterProperties;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.OnCommittedResponseWrapper;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.json.JsonMapper;

/** 인증 문맥이 유지되는 보안 체인의 하위에서 시작 성공 응답만 전송 전에 보호한다. */
@Component
@NullMarked
public class ConversationStartResponseFilter extends OncePerRequestFilter implements Ordered {

    private static final String RESPONSE_ACCESS = ConversationStartResponseFilter.class.getName() + ".access";
    private static final RequestMatcher START = PathPatternRequestMatcher.withDefaults()
            .matcher(HttpMethod.POST, "/api/v1/children/{childId}/conversations");

    private final int order;
    private final AuthenticatedSessionService authentication;
    private final ConversationService conversations;
    private final ConversationExceptionHandler conversationErrors;
    private final ChildExceptionHandler childErrors;
    private final JsonMapper json;

    /** 기존 필터 순서는 유지하고 Security 위임 필터 바로 뒤에 한 번 등록한다. */
    public ConversationStartResponseFilter(SecurityFilterProperties security,
            AuthenticatedSessionService authentication, ConversationService conversations,
            ConversationExceptionHandler conversationErrors, ChildExceptionHandler childErrors, JsonMapper json) {
        this.order = Math.addExact(security.getOrder(), 1);
        this.authentication = authentication;
        this.conversations = conversations;
        this.conversationErrors = conversationErrors;
        this.childErrors = childErrors;
        this.json = json;
    }

    /** 보안 체인이 다음 서블릿 필터를 호출하는 동안에만 현재 인증 문맥을 사용한다. */
    @Override
    public int getOrder() {
        return order;
    }

    /** 시작 성공 후 서버가 검증한 문맥과 반환할 대화만 요청 범위에 기록한다. */
    static void protect(HttpServletRequest request, AuthenticatedSession session, UUID childId,
            UUID clientRequestId, UUID conversationId) {
        request.setAttribute(RESPONSE_ACCESS, new ResponseAccess(session, childId, clientRequestId, conversationId));
    }

    /** Part 1과 다른 Part 2 경로의 응답은 감싸거나 재검사하지 않는다. */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !START.matches(request);
    }

    /** 첫 전송 및 미커밋 반환 직전에 검사하고 실패한 성공 본문·헤더를 원자적으로 비운다. */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        try {
            filterChain.doFilter(request, new OnCommittedResponseWrapper(response) {
                /** MVC 직렬화·flush가 시작해도 거부된 성공 바이트를 그대로 내보내지 않는다. */
                @Override
                protected void onResponseCommitted() {
                    disableOnResponseCommitted();
                    verify(request, response);
                }
            });
            if (!response.isCommitted()) {
                verify(request, response);
            }
        }
        catch (ServletException | IOException | RuntimeException failure) {
            var rejection = findRejection(failure);
            if (rejection == null || response.isCommitted()) {
                throw failure;
            }
            clearSuccess(response);
            if (rejection instanceof AuthenticationException) {
                // 실제 세션 폐기·저장 확인과 401/503 변환은 기존 외곽 인증 필터가 수행한다.
                throw new InvalidSessionException();
            }
            if (rejection instanceof AuthStateUnavailableException unavailable) {
                throw unavailable;
            }
            var error = rejection instanceof ChildException child
                    ? childErrors.handle(child, response)
                    : conversationErrors.handle(rejection instanceof ConversationException conversation
                            ? conversation : new ConversationException(rejection), response);
            response.setStatus(error.getStatusCode().value());
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            json.writeValue(response.getOutputStream(), error.getBody());
        }
    }

    /** 기존 인증을 먼저 재사용하고 성공 대상의 현재 DB 권한·업무 상태를 확인한다. */
    private void verify(HttpServletRequest request, HttpServletResponse response) {
        if ((response.getStatus() != 200 && response.getStatus() != 201)
                || !(request.getAttribute(RESPONSE_ACCESS) instanceof ResponseAccess expected)) {
            return;
        }
        try {
            var current = authentication.requireAuthenticatedSession(request);
            if (!expected.session().equals(current)) {
                throw new InsufficientAuthenticationException("로그인이 필요합니다.");
            }
            conversations.verifyStartResponse(current, expected.childId(), expected.clientRequestId(), expected.conversationId());
        }
        catch (RuntimeException rejection) {
            // MVC가 부분 직렬화 실패를 일반 500으로 소비하기 전에 이 필터까지 되돌린다.
            throw new ResponseRejected(rejection);
        }
    }

    /** MVC/서블릿 포장 안의 전송 거부만 찾고 무관한 기존 예외 처리에는 관여하지 않는다. */
    private @Nullable RuntimeException findRejection(Throwable failure) {
        for (@Nullable Throwable current = failure; current != null; current = current.getCause()) {
            if (current instanceof ResponseRejected rejected) {
                return rejected.rejection;
            }
        }
        return null;
    }

    /** requestId·no-store·CORS는 유지하고 성공 길이·Location·인증 갱신 쿠키를 제거한다. */
    private void clearSuccess(HttpServletResponse response) {
        var headers = new LinkedHashMap<String, List<String>>();
        for (var name : response.getHeaderNames()) {
            if (name.equalsIgnoreCase("Content-Length") || name.equalsIgnoreCase("Content-Type")
                    || name.equalsIgnoreCase("Location")) {
                continue;
            }
            headers.put(name, response.getHeaders(name).stream()
                    .filter(value -> !name.equalsIgnoreCase("Set-Cookie") || !value.startsWith("JSESSIONID=")).toList());
        }
        // Writer/OutputStream 선택도 초기화해 오류 JSON에 성공 본문이 섞이지 않게 한다.
        response.reset();
        headers.forEach((name, values) -> values.forEach(value -> response.addHeader(name, value)));
    }

    private record ResponseAccess(AuthenticatedSession session, UUID childId, UUID clientRequestId, UUID conversationId) { }

    /** 업무 예외를 MVC가 먼저 출력하지 못하도록 원인을 별도 보관하는 내부 전송 신호다. */
    static final class ResponseRejected extends RuntimeException {
        private final RuntimeException rejection;

        /** 고정 메시지만 가지며 실제 오류 변환은 성공 응답 초기화 뒤 수행한다. */
        private ResponseRejected(RuntimeException rejection) {
            super("대화 시작 응답을 다시 검증해야 합니다.", null, false, false);
            this.rejection = rejection;
        }
    }
}
