package com.dodamdodam.backend.global.web;

import com.dodamdodam.backend.global.error.ApiErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.json.JsonMapper;

public class Part1JsonRequestGuardFilter extends OncePerRequestFilter {

    public static final String EMPTY_BODY = Part1JsonRequestGuardFilter.class.getName() + ".emptyBody";
    private static final int MAX_BODY_BYTES = 16384;
    private static final RequestMatcher LOGOUT = PathPatternRequestMatcher.withDefaults()
            .matcher(HttpMethod.POST, "/api/v1/auth/logout");
    private static final RequestMatcher CHILDREN = PathPatternRequestMatcher.withDefaults()
            .matcher(HttpMethod.POST, "/api/v1/children");
    private final JsonMapper jsonMapper;

    /** 공통 오류 응답에 사용할 JSON 직렬화기를 받는다. */
    public Part1JsonRequestGuardFilter(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    /** 로그아웃과 자녀 등록 POST에만 전송 크기·형식 검사를 적용한다. */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !LOGOUT.matches(request) && !CHILDREN.matches(request);
    }

    /** CSRF 전에 전송 규칙을 검사하고, 후속 MVC가 읽을 수 있도록 본문을 보관한다. */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // Content-Length를 신뢰하지 않고 최대 크기보다 한 바이트만 더 읽어 초과 여부를 판정한다.
        byte[] body = request.getInputStream().readNBytes(MAX_BODY_BYTES + 1);
        if (body.length > MAX_BODY_BYTES) {
            reject(response, 413, "REQUEST_TOO_LARGE", "요청 본문이 허용 크기를 초과했습니다.");
            return;
        }
        if (!supportsContentType(request, body.length == 0)) {
            reject(response, 415, "UNSUPPORTED_MEDIA_TYPE", "지원하지 않는 Content-Type입니다.");
            return;
        }
        // 로그아웃의 0바이트 판정과 자녀 JSON 구조 검증은 CSRF·로그인 검사 이후에 수행한다.
        request.setAttribute(EMPTY_BODY, body.length == 0);
        filterChain.doFilter(new CachedBodyRequest(request, body), response);
    }

    /** JSON·UTF-8을 허용하고, Content-Type 없는 빈 본문은 로그아웃에만 허용한다. */
    private boolean supportsContentType(HttpServletRequest request, boolean emptyBody) {
        if (request.getContentType() == null) {
            return emptyBody && LOGOUT.matches(request);
        }
        try {
            var type = MediaType.parseMediaType(request.getContentType());
            return type.getType().equals("application") && type.getSubtype().equals("json")
                    && (type.getCharset() == null || StandardCharsets.UTF_8.equals(type.getCharset()));
        }
        catch (IllegalArgumentException invalid) {
            return false;
        }
    }

    /** 앞단에서 발급한 요청 ID를 유지하며 입력 원문 없는 공통 오류를 반환한다. */
    private void reject(HttpServletResponse response, int status, String code, String message) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        jsonMapper.writeValue(response.getOutputStream(), new ApiErrorResponse(new ApiErrorResponse.ErrorDetail(
                code, message, response.getHeader("X-Request-ID"), List.of())));
    }

    /** 크기 검사를 마친 본문을 동기 MVC 처리에서 다시 읽을 수 있게 제공한다. */
    private static final class CachedBodyRequest extends HttpServletRequestWrapper {
        private final byte[] body;

        /** 원래 요청의 세션·헤더·속성은 위임하고 검사한 본문만 보관한다. */
        private CachedBodyRequest(HttpServletRequest request, byte[] body) {
            super(request);
            this.body = body;
        }

        /** 소비한 원본 스트림 대신 매번 본문 처음부터 읽는 스트림을 반환한다. */
        @Override
        public ServletInputStream getInputStream() {
            var input = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                /** 보관한 본문에서 한 바이트를 읽는다. */
                @Override
                public int read() {
                    return input.read();
                }

                /** JSON 파서의 버퍼 읽기를 보관한 본문으로 위임한다. */
                @Override
                public int read(byte[] bytes, int offset, int length) {
                    return input.read(bytes, offset, length);
                }

                /** 해당 스트림의 본문이 모두 소비됐는지 반환한다. */
                @Override
                public boolean isFinished() {
                    return input.available() == 0;
                }

                /** 본문이 메모리에 있어 네트워크 입력 대기가 필요하지 않다. */
                @Override
                public boolean isReady() {
                    return true;
                }

                /** 현재 동기 MVC 계약에서 비동기 읽기 등록을 허용하지 않는다. */
                @Override
                public void setReadListener(ReadListener listener) {
                    Objects.requireNonNull(listener, "readListener");
                    throw new IllegalStateException("동기 MVC 본문 읽기만 지원합니다.");
                }
            };
        }

        /** 허용한 JSON 인코딩인 UTF-8로 본문을 다시 읽는다. */
        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }

        /** 헤더의 주장 대신 실제 보관한 본문 길이를 제공한다. */
        @Override
        public int getContentLength() {
            return body.length;
        }

        /** long 길이를 사용하는 소비자에게도 같은 실제 본문 길이를 제공한다. */
        @Override
        public long getContentLengthLong() {
            return body.length;
        }

        /** Reader와 인코딩 메타데이터를 UTF-8로 일치시킨다. */
        @Override
        public String getCharacterEncoding() {
            return StandardCharsets.UTF_8.name();
        }
    }
}
