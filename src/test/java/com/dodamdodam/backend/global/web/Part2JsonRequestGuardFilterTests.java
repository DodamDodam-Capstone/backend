package com.dodamdodam.backend.global.web;

import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

class Part2JsonRequestGuardFilterTests {

    private static final String START = "/api/v1/children/33333333-3333-4333-8333-333333333333/conversations";
    private final Part2JsonRequestGuardFilter guard = new Part2JsonRequestGuardFilter(JsonMapper.builder().build());

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"text/plain", "application/xml", "application/octet-stream", "application/problem+json",
            "application/*", "*/*", "not a media type", "", "application/json;charset=ISO-8859-1",
            "application/json;charset=unknown-charset"})
    void rejectsUnsupportedContentTypesBeforeCallingNextFilter(String type) throws Exception {
        var request = request("POST", START, "{}");
        request.setContentType(type);
        var response = new MockHttpServletResponse();
        response.setHeader("X-Request-ID", "test-request-id");

        guard.doFilter(request, response, (nextRequest, nextResponse) -> {
            throw new AssertionError("미지원 형식은 CSRF·인증 단계에 도달하면 안 됩니다.");
        });

        assertThat(response.getStatus()).isEqualTo(415);
        Map<String, Object> error = JsonPath.read(response.getContentAsString(), "$.error");
        assertThat(error).containsOnlyKeys("code", "message", "requestId", "fields")
                .containsEntry("code", "UNSUPPORTED_MEDIA_TYPE")
                .containsEntry("requestId", "test-request-id").containsEntry("fields", List.of());
    }

    @ParameterizedTest
    @ValueSource(strings = {"application/json", "application/json;charset=UTF-8", "APPLICATION/JSON;charset=utf-8"})
    void acceptsUtf8JsonWithoutReadingOrApplyingPart1SizeLimit(String type) throws Exception {
        // Part 1의 16 KiB보다 큰 본문도 Part 2 필터에서 소비하거나 크기로 거부하지 않는다.
        var body = " ".repeat(16385) + "{\"nickname\":\"별이\"}";
        var request = request("POST", START, body);
        request.setContentType(type);

        guard.doFilter(request, new MockHttpServletResponse(), (forwarded, response) -> {
            assertThat(forwarded).isSameAs(request);
            assertThat(forwarded.getInputStream().readAllBytes()).isEqualTo(body.getBytes(StandardCharsets.UTF_8));
        });
    }

    @Test
    void leavesMalformedJsonForValidationAfterCsrfAndAuthentication() throws Exception {
        var request = request("POST", START, "{]");
        request.setContentType("application/json");
        guard.doFilter(request, new MockHttpServletResponse(), (forwarded, response) ->
                assertThat(forwarded.getInputStream().readAllBytes()).isEqualTo("{]".getBytes(StandardCharsets.UTF_8)));
    }

    @ParameterizedTest
    @CsvSource({"GET,/api/v1/children/child/conversations", "OPTIONS,/api/v1/children/child/conversations",
            "POST,/api/v1/children", "POST,/api/v1/auth/logout", "POST,/api/v1/auth/login",
            "POST,/api/v1/children/child/conversations/conversation/resume",
            "POST,/api/v1/children/child/conversations/conversation/end",
            "POST,/api/v1/children/child/conversations/conversation/turns"})
    void leavesPart1AndOtherPart2RoutesUntouched(String method, String path) throws Exception {
        var request = request(method, path, "original-body");
        request.setContentType("text/plain");

        guard.doFilter(request, new MockHttpServletResponse(), (forwarded, response) -> {
            assertThat(forwarded).isSameAs(request);
            assertThat(forwarded.getInputStream().readAllBytes()).isEqualTo("original-body".getBytes(StandardCharsets.UTF_8));
        });
    }

    private MockHttpServletRequest request(String method, String path, String body) {
        var request = new MockHttpServletRequest(method, path);
        request.setContent(body.getBytes(StandardCharsets.UTF_8));
        return request;
    }
}
