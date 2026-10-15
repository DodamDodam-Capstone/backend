package com.dodamdodam.backend.auth.session;

import com.dodamdodam.backend.global.error.ApiErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.json.JsonMapper;

public class LogoutRequestGuardFilter extends OncePerRequestFilter {

    public static final String EMPTY_BODY = LogoutRequestGuardFilter.class.getName() + ".emptyBody";
    private static final RequestMatcher LOGOUT = PathPatternRequestMatcher.withDefaults()
            .matcher(HttpMethod.POST, "/api/v1/auth/logout");
    private final JsonMapper jsonMapper;

    public LogoutRequestGuardFilter(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !LOGOUT.matches(request);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        byte[] body = request.getInputStream().readNBytes(16385);
        if (body.length > 16384) {
            reject(response, 413, "REQUEST_TOO_LARGE", "요청 본문이 허용 크기를 초과했습니다.");
            return;
        }
        boolean supported = body.length == 0 && request.getContentType() == null;
        try {
            if (request.getContentType() != null) {
                var type = MediaType.parseMediaType(request.getContentType());
                supported = type.getType().equals("application") && type.getSubtype().equals("json")
                        && (type.getCharset() == null || StandardCharsets.UTF_8.equals(type.getCharset()));
            }
        }
        catch (IllegalArgumentException exception) {
            supported = false;
        }
        if (!supported) {
            reject(response, 415, "UNSUPPORTED_MEDIA_TYPE", "지원하지 않는 Content-Type입니다.");
            return;
        }
        // 본문 유무의 400 판정은 CSRF와 로그인 검사가 끝난 뒤 수행한다.
        request.setAttribute(EMPTY_BODY, body.length == 0);
        filterChain.doFilter(request, response);
    }

    private void reject(HttpServletResponse response, int status, String code, String message) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        jsonMapper.writeValue(response.getOutputStream(), new ApiErrorResponse(new ApiErrorResponse.ErrorDetail(
                code, message, response.getHeader("X-Request-ID"), List.of())));
    }
}
