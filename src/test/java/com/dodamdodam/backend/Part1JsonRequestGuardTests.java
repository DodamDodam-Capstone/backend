package com.dodamdodam.backend;

import com.dodamdodam.backend.global.web.Part1JsonRequestGuardFilter;
import jakarta.servlet.http.HttpServletRequest;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.DelegatingServletInputStream;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

class Part1JsonRequestGuardTests {

    private static final String CHILDREN = "/api/v1/children";
    private static final String LOGOUT = "/api/v1/auth/logout";
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private final Part1JsonRequestGuardFilter guard = new Part1JsonRequestGuardFilter(JSON);

    @ParameterizedTest
    @ValueSource(strings = {CHILDREN, LOGOUT})
    void oversizedBodyRejected(String path) throws Exception {
        assertRejected(request(path, " ".repeat(16385)), 413, "REQUEST_TOO_LARGE");
    }

    @ParameterizedTest
    @ValueSource(strings = {CHILDREN, LOGOUT})
    void exactUtf8ByteLimitReachesNextFilter(String path) throws Exception {
        var body = ("가".repeat(5461) + " ").getBytes(StandardCharsets.UTF_8);
        var request = request(path, "");
        request.setContent(body);

        guard.doFilter(request, new MockHttpServletResponse(), (forwarded, response) ->
                assertThat(forwarded.getInputStream().readAllBytes()).isEqualTo(body));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"text/plain", "application/problem+json", "application/*", "*/*", "not a media type",
            "application/json;charset=ISO-8859-1", "application/json;charset=unknown-charset", ""})
    void unsupportedMediaTypeRejected(String type) throws Exception {
        for (var path : new String[]{CHILDREN, LOGOUT}) {
            var request = request(path, "{}");
            request.setContentType(type);
            assertRejected(request, 415, "UNSUPPORTED_MEDIA_TYPE");
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"application/json", "application/json;charset=UTF-8", "APPLICATION/JSON;charset=utf-8"})
    void acceptsJsonWithImplicitOrExplicitUtf8(String type) throws Exception {
        var request = request(CHILDREN, "{}");
        request.setContentType(type);

        guard.doFilter(request, new MockHttpServletResponse(), (forwarded, response) ->
                assertThat(forwarded.getInputStream().readAllBytes()).isEqualTo("{}".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void bodyRemainsReadableAfterGuard() throws Exception {
        var body = "{\"nickname\":\"별이\"}";
        var request = request(CHILDREN, body);

        guard.doFilter(request, new MockHttpServletResponse(), (forwarded, response) -> {
            var replay = (HttpServletRequest) forwarded;
            assertThat(replay.getInputStream().readAllBytes()).isEqualTo(body.getBytes(StandardCharsets.UTF_8));
            assertThat(replay.getReader().readLine()).isEqualTo(body);
            assertThat(JSON.readTree(replay.getInputStream()).get("nickname").stringValue()).isEqualTo("별이");
            assertThat(replay.getCharacterEncoding()).isEqualTo("UTF-8");
            assertThat(replay.getContentLengthLong()).isEqualTo(body.getBytes(StandardCharsets.UTF_8).length);
        });
    }

    @Test
    void jsonStructureValidationRemainsAfterCsrfAndAuthentication() throws Exception {
        guard.doFilter(request(CHILDREN, "{]"), new MockHttpServletResponse(), (forwarded, response) ->
                assertThat(forwarded.getInputStream().readAllBytes()).isEqualTo("{]".getBytes(StandardCharsets.UTF_8)));
    }

    @ParameterizedTest
    @CsvSource({"0,true", "1,false", "2,false"})
    void logoutKeepsActualEmptyBodyAttribute(int size, boolean empty) throws Exception {
        guard.doFilter(request(LOGOUT, " ".repeat(size)), new MockHttpServletResponse(), (forwarded, response) ->
                assertThat(forwarded.getAttribute(Part1JsonRequestGuardFilter.EMPTY_BODY)).isEqualTo(empty));
    }

    @Test
    void onlyLogoutAllowsAnUntypedEmptyBody() throws Exception {
        var logout = request(LOGOUT, "");
        logout.setContentType(null);
        guard.doFilter(logout, new MockHttpServletResponse(), (forwarded, response) ->
                assertThat(forwarded.getAttribute(Part1JsonRequestGuardFilter.EMPTY_BODY)).isEqualTo(true));

        var children = request(CHILDREN, "");
        children.setContentType(null);
        assertRejected(children, 415, "UNSUPPORTED_MEDIA_TYPE");
    }

    @Test
    void emptyJsonBodyRemainsForLaterValidation() throws Exception {
        guard.doFilter(request(CHILDREN, ""), new MockHttpServletResponse(), (forwarded, response) ->
                assertThat(forwarded.getInputStream().readAllBytes()).isEmpty());
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 1, 20000})
    void actualBytesDetermineLimitEvenWithMissingOrFalseContentLength(int declared) throws Exception {
        var oversized = declaredLengthRequest(declared, " ".repeat(16385));
        assertRejected(oversized, 413, "REQUEST_TOO_LARGE");
        var small = declaredLengthRequest(declared, "{}");
        guard.doFilter(small, new MockHttpServletResponse(), (forwarded, response) -> {
            assertThat(forwarded.getInputStream().readAllBytes()).isEqualTo("{}".getBytes(StandardCharsets.UTF_8));
            assertThat(forwarded.getContentLength()).isEqualTo(2);
        });
    }

    @Test
    void readsOnlyOneByteBeyondLimitAndSizePrecedesMediaType() throws Exception {
        var input = new ByteArrayInputStream(new byte[32768]);
        var request = new MockHttpServletRequest("POST", CHILDREN) {
            @Override
            public DelegatingServletInputStream getInputStream() {
                return new DelegatingServletInputStream(input);
            }
        };
        request.setContentType("text/plain");

        assertRejected(request, 413, "REQUEST_TOO_LARGE");
        assertThat(input.available()).isEqualTo(32768 - 16385);
    }

    @ParameterizedTest
    @MethodSource("unrelatedRequests")
    void unrelatedRoutesAndMethodsKeepTheirOriginalBody(String method, String path) throws Exception {
        var request = new MockHttpServletRequest(method, path);
        request.setContentType("text/plain");
        request.setContent("original-body".getBytes(StandardCharsets.UTF_8));

        guard.doFilter(request, new MockHttpServletResponse(), (forwarded, response) -> {
            assertThat(forwarded).isSameAs(request);
            assertThat(forwarded.getInputStream().readAllBytes()).isEqualTo("original-body".getBytes(StandardCharsets.UTF_8));
        });
    }

    static Stream<Arguments> unrelatedRequests() {
        return Stream.of(Arguments.of("GET", CHILDREN), Arguments.of("OPTIONS", CHILDREN),
                Arguments.of("GET", LOGOUT), Arguments.of("POST", "/api/v1/auth/login"),
                Arguments.of("POST", "/api/v1/children/another-resource"));
    }

    private MockHttpServletRequest declaredLengthRequest(int declared, String body) {
        var request = new MockHttpServletRequest("POST", CHILDREN) {
            @Override
            public int getContentLength() {
                return declared;
            }

            @Override
            public long getContentLengthLong() {
                return declared;
            }
        };
        request.setContentType("application/json");
        request.setContent(body.getBytes(StandardCharsets.UTF_8));
        return request;
    }

    private MockHttpServletRequest request(String path, String body) {
        var request = new MockHttpServletRequest("POST", path);
        request.setContentType("application/json");
        request.setContent(body.getBytes(StandardCharsets.UTF_8));
        return request;
    }

    private void assertRejected(MockHttpServletRequest request, int status, String code) throws Exception {
        var response = new MockHttpServletResponse();
        response.setHeader("X-Request-ID", "11111111-1111-4111-8111-111111111111");
        guard.doFilter(request, response, (forwarded, outgoing) -> {
            throw new AssertionError("거부한 본문이 다음 필터로 전달됐습니다.");
        });

        assertThat(response.getStatus()).isEqualTo(status);
        var error = JSON.readTree(response.getContentAsByteArray()).get("error");
        assertThat(error.get("code").stringValue()).isEqualTo(code);
        assertThat(error.get("requestId").stringValue()).isEqualTo(response.getHeader("X-Request-ID"));
        assertThat(error.get("fields").isArray()).isTrue();
        assertThat(error.get("fields").size()).isZero();
    }
}
