package com.dodamdodam.backend.global.web;

import com.dodamdodam.backend.global.error.ApiErrorResponses;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.json.JsonMapper;

@NullMarked
public class Part2JsonRequestGuardFilter extends OncePerRequestFilter {

    private static final RequestMatcher START = PathPatternRequestMatcher.withDefaults()
            .matcher(HttpMethod.POST, "/api/v1/children/{childId}/conversations");
    private final JsonMapper jsonMapper;

    /** Security 체인에서만 등록하며 공통 오류 직렬화기를 재사용한다. */
    public Part2JsonRequestGuardFilter(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    /** 현재 구현된 Part 2 JSON 본문 경로인 대화 시작에만 적용한다. */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !START.matches(request);
    }

    /** CSRF·인증 전에 형식을 거부하고 본문·구조 검증은 후속 처리에 남긴다. */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!supportsContentType(request.getContentType())) {
            ApiErrorResponses.write(jsonMapper, response, 415,
                    "UNSUPPORTED_MEDIA_TYPE", "지원하지 않는 Content-Type입니다.");
            return;
        }
        // Part 2 크기 정책은 미정이므로 스트림을 읽거나 Part 1 제한을 적용하지 않는다.
        filterChain.doFilter(request, response);
    }

    /** 명세의 application/json과 생략 또는 UTF-8인 charset만 허용한다. */
    private boolean supportsContentType(@Nullable String contentType) {
        if (contentType == null) {
            return false;
        }
        try {
            var type = MediaType.parseMediaType(contentType);
            return type.getType().equals("application") && type.getSubtype().equals("json")
                    && (type.getCharset() == null || StandardCharsets.UTF_8.equals(type.getCharset()));
        }
        catch (IllegalArgumentException invalid) {
            return false;
        }
    }
}
