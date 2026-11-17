package com.dodamdodam.backend.global.web;

import com.dodamdodam.backend.auth.application.AuthRateLimitService;
import com.dodamdodam.backend.auth.infrastructure.EmailVerificationKeys;
import com.dodamdodam.backend.auth.exception.AuthRateLimitException;
import com.dodamdodam.backend.global.error.ApiErrorResponses;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.List;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.json.JsonMapper;

public class GlobalApiRateLimitFilter extends OncePerRequestFilter {
    private static final RequestMatcher API = PathPatternRequestMatcher.withDefaults().matcher("/api/v1/**");
    private static final RequestMatcher OAUTH_START = PathPatternRequestMatcher.withDefaults().matcher("/oauth2/**");
    private static final RequestMatcher OAUTH_CALLBACK = PathPatternRequestMatcher.withDefaults().matcher("/login/oauth2/**");
    private final AuthRateLimitService limits;
    private final EmailVerificationKeys keys;
    private final JsonMapper mapper;

    /** CORS 뒤 API·OAuth에 한 번만 적용할 전역 예산 예약 경로를 구성한다. */
    public GlobalApiRateLimitFilter(AuthRateLimitService limits, EmailVerificationKeys keys, JsonMapper mapper) {
        this.limits = limits;
        this.keys = keys;
        this.mapper = mapper;
    }

    /** health·정적 리소스는 제외하며 정상 CORS preflight는 앞선 CORS 필터가 종료한다. */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !API.matches(request) && !OAUTH_START.matches(request) && !OAUTH_CALLBACK.matches(request);
    }

    /** 업무/전송 실패와 무관하게 전역 IP 예약을 먼저 커밋하고 소진 시 공통429를 반환한다. */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            limits.reserve(List.of(keys.rateHasher().apiIp(ClientAddressResolver.resolve(request))));
        }
        catch (AuthRateLimitException exceeded) {
            response.setHeader("Retry-After", Long.toString(exceeded.retryAfterSeconds()));
            ApiErrorResponses.write(mapper, response, 429, exceeded.code(), exceeded.getMessage());
            return;
        }
        chain.doFilter(request, response);
    }
}
