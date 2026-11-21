package com.dodamdodam.backend.home.api;

import com.dodamdodam.backend.auth.session.AuthStateUnavailableException;
import com.dodamdodam.backend.auth.session.AuthenticatedSession;
import com.dodamdodam.backend.auth.session.AuthenticatedSessionService;
import com.dodamdodam.backend.auth.session.SessionCookieRenewalFilter.InvalidSessionException;
import com.dodamdodam.backend.child.api.ChildExceptionHandler;
import com.dodamdodam.backend.child.exception.ChildException;
import com.dodamdodam.backend.conversation.api.ConversationExceptionHandler;
import com.dodamdodam.backend.conversation.exception.ConversationException;
import com.dodamdodam.backend.home.application.ChildHomeService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jspecify.annotations.NullMarked;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterProperties;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;
import tools.jackson.databind.json.JsonMapper;

/** 홈 성공 본문을 보류하고 현재 인증·상태로 다시 만든 응답만 전달한다. */
@Component
@NullMarked
public class ChildHomeResponseFilter extends OncePerRequestFilter implements Ordered {

    private static final String RESPONSE_ACCESS = ChildHomeResponseFilter.class.getName() + ".access";
    private static final RequestMatcher HOME = PathPatternRequestMatcher.withDefaults()
            .matcher(HttpMethod.GET, "/api/v1/children/{childId}/home");
    private final int order;
    private final AuthenticatedSessionService authentication;
    private final ChildHomeService homes;
    private final ChildExceptionHandler childErrors;
    private final ConversationExceptionHandler conversationErrors;
    private final JsonMapper json;

    /** Security 아래에 한 번 등록하며 기존 시작 필터와 보안 체인의 순서는 유지한다. */
    public ChildHomeResponseFilter(SecurityFilterProperties security, AuthenticatedSessionService authentication,
            ChildHomeService homes, ChildExceptionHandler childErrors, ConversationExceptionHandler conversationErrors,
            JsonMapper json) {
        this.order = Math.addExact(security.getOrder(), 2);
        this.authentication = authentication;
        this.homes = homes;
        this.childErrors = childErrors;
        this.conversationErrors = conversationErrors;
        this.json = json;
    }

    /** 보안 체인이 유지되는 하위 서블릿 필터 위치를 사용한다. */
    @Override
    public int getOrder() {
        return order;
    }

    /** Controller의 조회가 성공한 경우에만 서버가 검증한 문맥을 요청 범위에 기록한다. */
    static void protect(HttpServletRequest request, AuthenticatedSession session, UUID childId) {
        request.setAttribute(RESPONSE_ACCESS, new ResponseAccess(session, childId));
    }

    /** 정확한 홈 GET 이외의 응답은 감싸지 않는다. */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !HOME.matches(request);
    }

    /** 동기 MVC 본문을 보류하고 성공 표식이 있는 200만 최신 JSON으로 교체한다. */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        var cached = new ContentCachingResponseWrapper(response);
        try {
            chain.doFilter(request, cached);
            var access = request.getAttribute(RESPONSE_ACCESS);
            // 표식은 재조회 전에 소비하며 서비스를 Java로 호출하므로 요청을 재디스패치하지 않는다.
            request.removeAttribute(RESPONSE_ACCESS);
            if (cached.getStatus() == 200 && access instanceof ResponseAccess expected) {
                try {
                    var body = latestBody(request, expected);
                    cached.resetBuffer();
                    cached.setCharacterEncoding(StandardCharsets.UTF_8.name());
                    cached.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    cached.setContentLength(body.length);
                    cached.getOutputStream().write(body);
                }
                catch (RuntimeException rejection) {
                    reject(cached, rejection);
                }
            }
            cached.copyBodyToResponse();
        }
        catch (ServletException | IOException | RuntimeException failure) {
            if (!response.isCommitted()) {
                clearSuccess(cached);
            }
            throw failure;
        }
        finally {
            request.removeAttribute(RESPONSE_ACCESS);
        }
    }

    /** Controller 조회 Tx 종료 뒤 서비스 프록시를 다시 호출하여 별도 Tx의 최신 상태를 직렬화한다. */
    private byte[] latestBody(HttpServletRequest request, ResponseAccess expected) {
        var current = authentication.requireAuthenticatedSession(request);
        if (!expected.session().equals(current)) {
            throw new InsufficientAuthenticationException("로그인이 필요합니다.");
        }
        return json.writeValueAsBytes(Map.of("data", homes.getHome(current, expected.childId())));
    }

    /** 인증 실패는 기존 외곽 필터로 넘기고 업무·직렬화 실패는 기존 오류 envelope로 처리한다. */
    private void reject(ContentCachingResponseWrapper response, RuntimeException rejection) throws IOException {
        clearSuccess(response);
        if (rejection instanceof AuthenticationException) {
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

    /** 공통 헤더·다른 쿠키는 유지하고 성공 본문·길이·Location·인증 갱신 쿠키는 비운다. */
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
        response.reset();
        // 캐시 wrapper의 길이 필드는 reset과 별도로 비워 이전 길이를 복원하지 않게 한다.
        response.setHeader("Content-Length", null);
        headers.forEach((name, values) -> values.forEach(value -> response.addHeader(name, value)));
    }

    private record ResponseAccess(AuthenticatedSession session, UUID childId) { }
}
