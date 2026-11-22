package com.dodamdodam.backend.home.api;

import com.dodamdodam.backend.auth.application.EmailVerificationIssuanceService;
import com.dodamdodam.backend.auth.application.EmailVerificationService;
import com.dodamdodam.backend.auth.session.AuthStateErrorFilter;
import com.dodamdodam.backend.auth.session.AuthStateErrorHandler;
import com.dodamdodam.backend.auth.session.AuthStateUnavailableException;
import com.dodamdodam.backend.auth.session.AuthenticatedSession;
import com.dodamdodam.backend.auth.session.AuthenticatedSessionService;
import com.dodamdodam.backend.auth.session.SessionCookieRenewalFilter.InvalidSessionException;
import com.dodamdodam.backend.auth.session.SessionLifecycleService;
import com.dodamdodam.backend.child.api.ChildExceptionHandler;
import com.dodamdodam.backend.child.application.ChildService;
import com.dodamdodam.backend.child.dto.ChildContext;
import com.dodamdodam.backend.child.exception.ChildException;
import com.dodamdodam.backend.conversation.api.ConversationExceptionHandler;
import com.dodamdodam.backend.conversation.exception.ConversationException;
import com.dodamdodam.backend.guardian.application.GuardianAccessService;
import com.dodamdodam.backend.home.application.ChildHomeService;
import com.dodamdodam.backend.home.dto.ChildHome;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterProperties;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.session.MapSession;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.web.util.ContentCachingResponseWrapper;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/** 본문 작성 뒤 상태 변경을 끼워 넣어 캐시·오류·외곽 인증 처리를 결정적으로 검증한다. */
class ChildHomeResponseFilterTests {

    private static final AuthenticatedSession SESSION = new AuthenticatedSession(UUID.randomUUID(), UUID.randomUUID());
    private static final String SESSION_ID = "synthetic-home-session";
    private static final UUID CHILD = UUID.fromString("22222222-2222-4222-8222-222222222222");
    private static final UUID ACTIVE = UUID.fromString("55555555-5555-4555-8555-555555555555");
    private static final String PATH = "/api/v1/children/" + CHILD + "/home";
    private static final OffsetDateTime BEFORE = OffsetDateTime.parse("2026-10-03T23:59:59.999999+09:00");
    private static final ChildContext CHILD_CONTEXT = new ChildContext(CHILD, "가나", "별이", LocalDate.of(2020, 5, 12),
            List.of("공룡"), "test-character", OffsetDateTime.parse("2026-10-01T06:00:00+09:00"));
    private final AuthenticatedSessionService authentication = mock(AuthenticatedSessionService.class);
    private final ChildHomeService homes = mock(ChildHomeService.class);
    private final JsonMapper json = JsonMapper.builder().build();
    private final SecurityFilterProperties properties = new SecurityFilterProperties();
    private final ChildHome original = ChildHome.from(CHILD_CONTEXT, BEFORE, ACTIVE, false);
    private ChildHomeResponseFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void prepare() {
        filter = filter(homes, json);
        request = new MockHttpServletRequest("GET", PATH);
        // 요청 캐시의 세션 ID와 실제 저장 세션 검증의 성공 결과를 각각 구성한다.
        request.setSession(new MockHttpSession(null, SESSION_ID));
        response = new MockHttpServletResponse();
        response.setHeader("X-Request-ID", "test-request-id");
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Access-Control-Allow-Origin", "https://example.invalid");
        response.setHeader("Access-Control-Allow-Credentials", "true");
        response.addHeader("Vary", "Origin");
        when(authentication.requireAuthenticatedSession(any())).thenReturn(SESSION);
        when(authentication.requireCurrentStoredSession(SESSION_ID, SESSION)).thenReturn(new MapSession(SESSION_ID));
        when(homes.getHome(SESSION, CHILD)).thenReturn(original);
    }

    @Test
    void usesTheConfiguredSecurityOrderPlusTwo() {
        properties.setOrder(-170);
        assertThat(filter(homes, json).getOrder()).isEqualTo(-168);
    }

    @ParameterizedTest
    @ValueSource(strings = {"MIDNIGHT", "CLOSING", "ENDED", "LINK_REMOVED"})
    void refreshesStateChangedAfterTheOriginalBodyWasPrepared(String change) throws Exception {
        var latest = latest(change);
        filter.doFilter(request, response, successfulResponse("FLUSH_BUFFER",
                () -> when(homes.getHome(SESSION, CHILD)).thenReturn(latest)));

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getContentAsByteArray()).isEqualTo(encoded(latest));
        assertThat(response.getContentAsString()).doesNotContain(ACTIVE.toString());
        assertLengthAndCommonHeaders();
        var order = inOrder(authentication, homes);
        order.verify(authentication).requireAuthenticatedSession(request);
        order.verify(authentication).requireCurrentStoredSession(SESSION_ID, SESSION);
        order.verify(homes).getHome(SESSION, CHILD);
        verifyNoMoreInteractions(authentication, homes);
    }

    @ParameterizedTest
    @ValueSource(strings = {"WRITER", "STREAM", "FLUSH_BUFFER", "CONTENT_LENGTH", "CONTENT_LENGTH_LONG", "HEADER", "NO_FLUSH"})
    void replacesAllCachedWriteModesWithoutMixingOldBytes(String mode) throws Exception {
        var latest = latest("CLOSING");
        filter.doFilter(request, response, successfulResponse(mode,
                () -> when(homes.getHome(SESSION, CHILD)).thenReturn(latest)));

        assertThat(response.getContentAsByteArray()).isEqualTo(encoded(latest));
        assertThat(response.getHeader("Set-Cookie")).isEqualTo("JSESSIONID=synthetic-renewal; Path=/; HttpOnly");
        assertThat(response.getHeaders("Set-Cookie")).contains("XSRF-TOKEN=preserved; Path=/");
        assertLengthAndCommonHeaders();
    }

    @ParameterizedTest
    @ValueSource(strings = {"WRITER", "STREAM", "FLUSH_BUFFER", "CONTENT_LENGTH", "CONTENT_LENGTH_LONG", "HEADER", "NO_FLUSH"})
    void rejectedAuthenticationDiscardsSuccessAndRenewalCookiesForEveryWriteMode(String mode) throws Exception {
        response.setHeader("Location", "/synthetic-old-location");
        doThrow(new InsufficientAuthenticationException("로그인이 필요합니다."))
                .when(authentication).requireAuthenticatedSession(any());

        assertThatThrownBy(() -> filter.doFilter(request, response, successfulResponse(mode, () -> { })))
                .isInstanceOf(InvalidSessionException.class);

        assertThat(response.getContentAsByteArray()).isEmpty();
        assertThat(response.getHeader("Content-Length")).isNull();
        assertThat(response.getHeader("Location")).isNull();
        assertThat(response.getHeaders("Set-Cookie")).containsExactly("XSRF-TOKEN=preserved; Path=/");
        assertCommonHeaders();
        verify(authentication).requireAuthenticatedSession(request);
        verifyNoMoreInteractions(authentication);
        verifyNoInteractions(homes);
    }

    @ParameterizedTest
    @ValueSource(strings = {"REVOKED", "REPLACED", "STORE_FAILED"})
    void usesTheExistingOuterAuthenticationErrorEnvelope(String failure) throws Exception {
        if (failure.equals("STORE_FAILED")) {
            doThrow(new AuthStateUnavailableException()).when(authentication).requireAuthenticatedSession(any());
        } else if (failure.equals("REPLACED")) {
            when(authentication.requireAuthenticatedSession(any()))
                    .thenReturn(new AuthenticatedSession(SESSION.accountId(), UUID.randomUUID()));
        } else {
            doThrow(new InsufficientAuthenticationException("로그인이 필요합니다."))
                    .when(authentication).requireAuthenticatedSession(any());
        }
        outerAuthenticationFilter().doFilter(request, response,
                (req, res) -> filter.doFilter(req, res, successfulResponse("HEADER", () -> { })));

        assertThat(response.getStatus()).isEqualTo(failure.equals("STORE_FAILED") ? 503 : 401);
        assertError(failure.equals("STORE_FAILED") ? "AUTH_STATE_UNAVAILABLE" : "AUTHENTICATION_REQUIRED");
        assertThat(response.getHeader("Content-Length")).isNull();
        assertThat(response.getHeaders("Set-Cookie")).containsExactly("XSRF-TOKEN=preserved; Path=/");
        verify(authentication).requireAuthenticatedSession(request);
        verifyNoMoreInteractions(authentication);
        verifyNoInteractions(homes);
    }

    @ParameterizedTest
    @CsvSource({"INVALID,WRITER", "INVALID,STREAM", "INVALID,HEADER",
            "UNAVAILABLE,WRITER", "UNAVAILABLE,STREAM", "UNAVAILABLE,HEADER"})
    void storedSessionFailureRejectsTheResponseBeforeHomeLookup(String failure, String mode) throws Exception {
        // 저장 세션 무효와 조회 장애 모두 재조회 Tx 전에 실패해야 한다.
        boolean unavailable = failure.equals("UNAVAILABLE");
        RuntimeException rejection = unavailable
                ? new AuthStateUnavailableException(new DataAccessResourceFailureException("synthetic-store-failure"))
                : new InsufficientAuthenticationException("로그인이 필요합니다.");
        doThrow(rejection).when(authentication).requireCurrentStoredSession(SESSION_ID, SESSION);
        response.setHeader("Location", "/synthetic-old-location");

        outerAuthenticationFilter().doFilter(request, response,
                (req, res) -> filter.doFilter(req, res, successfulResponse(mode, () -> { })));

        assertThat(response.getStatus()).isEqualTo(unavailable ? 503 : 401);
        assertError(unavailable ? "AUTH_STATE_UNAVAILABLE" : "AUTHENTICATION_REQUIRED");
        assertThat(response.getHeader("Content-Length")).isNull();
        assertThat(response.getHeader("Location")).isNull();
        assertThat(response.getHeaders("Set-Cookie")).containsExactly("XSRF-TOKEN=preserved; Path=/");
        assertThat(response.getContentAsString()).doesNotContain("synthetic-store-failure");
        var order = inOrder(authentication);
        order.verify(authentication).requireAuthenticatedSession(request);
        order.verify(authentication).requireCurrentStoredSession(SESSION_ID, SESSION);
        verifyNoMoreInteractions(authentication);
        verifyNoInteractions(homes);
    }

    @Test
    void missingRequestSessionRejectsBeforeStoredSessionLookupAndHomeLookup() throws Exception {
        request.setSession(null);

        outerAuthenticationFilter().doFilter(request, response,
                (req, res) -> filter.doFilter(req, res, successfulResponse("HEADER", () -> { })));

        assertThat(response.getStatus()).isEqualTo(401);
        assertError("AUTHENTICATION_REQUIRED");
        assertThat(response.getHeaders("Set-Cookie")).containsExactly("XSRF-TOKEN=preserved; Path=/");
        verify(authentication).requireAuthenticatedSession(request);
        verifyNoMoreInteractions(authentication);
        verifyNoInteractions(homes);
    }

    @Test
    void validatesTheCurrentSessionIdEvenWhenTheRequestedCookieIdIsStale() throws Exception {
        request.setRequestedSessionId("synthetic-old-session");

        filter.doFilter(request, response, successfulResponse("WRITER", () -> { }));

        var order = inOrder(authentication, homes);
        order.verify(authentication).requireAuthenticatedSession(request);
        order.verify(authentication).requireCurrentStoredSession(SESSION_ID, SESSION);
        order.verify(homes).getHome(SESSION, CHILD);
        verifyNoMoreInteractions(authentication, homes);
        assertThat(response.getContentAsByteArray()).isEqualTo(encoded(original));
        assertLengthAndCommonHeaders();
    }

    @Test
    void invalidationFoundByTheServiceAlsoUsesTheOuter401Path() throws Exception {
        doThrow(new InsufficientAuthenticationException("로그인이 필요합니다.")).when(homes).getHome(SESSION, CHILD);
        outerAuthenticationFilter().doFilter(request, response,
                (req, res) -> filter.doFilter(req, res, successfulResponse("WRITER", () -> { })));

        assertThat(response.getStatus()).isEqualTo(401);
        assertError("AUTHENTICATION_REQUIRED");
        assertThat(response.getHeader("Content-Length")).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"MISSING", "CHILD_STORE", "CONVERSATION_STORE", "UNEXPECTED"})
    void businessFailuresKeepTheirOwnErrorCodeInsteadOfBecomingAuthentication503(String kind) throws Exception {
        var storeFailure = new DataAccessResourceFailureException("synthetic-sensitive-store-detail");
        RuntimeException failure = switch (kind) {
            case "MISSING" -> ChildException.notFound();
            case "CHILD_STORE" -> new ChildException(storeFailure);
            case "CONVERSATION_STORE" -> new ConversationException(storeFailure);
            default -> new IllegalStateException("synthetic-sensitive-store-detail");
        };
        doThrow(failure).when(homes).getHome(SESSION, CHILD);
        outerAuthenticationFilter().doFilter(request, response,
                (req, res) -> filter.doFilter(req, res, successfulResponse("WRITER", () -> { })));

        assertThat(response.getStatus()).isEqualTo(kind.equals("MISSING") ? 404 : 500);
        assertError(kind.equals("MISSING") ? "RESOURCE_NOT_FOUND" : "INTERNAL_ERROR");
        assertLengthAndCommonHeaders();
        assertThat(response.getContentAsString()).doesNotContain("synthetic-sensitive-store-detail");
        assertThat(response.getHeaders("Set-Cookie")).containsExactly("XSRF-TOKEN=preserved; Path=/");
    }

    @Test
    void failedReserializationDoesNotReleaseThePreviouslySuccessfulBody() throws Exception {
        var brokenJson = spy(json);
        doThrow(new IllegalStateException("synthetic-serialization-failure")).when(brokenJson).writeValueAsBytes(any());
        filter(homes, brokenJson).doFilter(request, response, successfulResponse("STREAM", () -> { }));

        assertThat(response.getStatus()).isEqualTo(500);
        assertError("INTERNAL_ERROR");
        assertLengthAndCommonHeaders();
        assertThat(response.getContentAsString()).doesNotContain("synthetic-serialization-failure");
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 401, 403, 404, 409, 415, 429, 500, 503})
    void existingErrorResponsesAreCopiedWithoutRechecking(int status) throws Exception {
        byte[] error = "{\"error\":{\"code\":\"existing-error\"}}".getBytes(StandardCharsets.UTF_8);
        filter.doFilter(request, response, (req, res) -> {
            ChildHomeResponseFilter.protect(request, SESSION, CHILD);
            ((HttpServletResponse) res).setStatus(status);
            res.getOutputStream().write(error);
            res.flushBuffer();
        });

        assertThat(response.getStatus()).isEqualTo(status);
        assertThat(response.getContentAsByteArray()).isEqualTo(error);
        assertLengthAndCommonHeaders();
        verifyNoInteractions(authentication, homes);
    }

    @Test
    void a200WithoutTheControllerSuccessMarkerIsNotRechecked() throws Exception {
        filter.doFilter(request, response, (req, res) -> res.getOutputStream().write(encoded(original)));

        assertThat(response.getContentAsByteArray()).isEqualTo(encoded(original));
        verifyNoInteractions(authentication, homes);
    }

    @ParameterizedTest
    @CsvSource({"POST,/home", "HEAD,/home", "GET,/home/", "GET,/home/extra", "GET,/conversations", "GET,/homebrew"})
    void otherMethodsAndPathsAreNotWrapped(String method, String suffix) throws Exception {
        var other = new MockHttpServletRequest(method, "/api/v1/children/" + CHILD + suffix);
        filter.doFilter(other, response, (req, res) -> assertThat(res).isSameAs(response));

        verifyNoInteractions(authentication, homes);
    }

    @Test
    void consumesTheMarkerAndNeverRedispatchesTheRequest() throws Exception {
        var calls = new AtomicInteger();
        var first = successfulResponse("NO_FLUSH", calls::incrementAndGet);
        filter.doFilter(request, response, first);
        var nextResponse = new MockHttpServletResponse();
        filter.doFilter(request, nextResponse, (req, res) -> {
            calls.incrementAndGet();
            res.getOutputStream().write("unmarked".getBytes(StandardCharsets.UTF_8));
        });

        assertThat(calls.get()).isEqualTo(2);
        assertThat(nextResponse.getContentAsString()).isEqualTo("unmarked");
        verify(homes).getHome(SESSION, CHILD);
        verifyNoMoreInteractions(homes);
    }

    @Test
    void escapingChainFailureDoesNotFlushPartialSuccessOrRenewalCookies() {
        var failure = new IOException("synthetic transport failure");
        assertThatThrownBy(() -> filter.doFilter(request, response, (req, res) -> {
            successfulResponse("WRITER", () -> { }).doFilter(req, res);
            throw failure;
        })).isSameAs(failure);

        assertThat(response.getContentAsByteArray()).isEmpty();
        assertThat(response.getHeader("Content-Length")).isNull();
        assertThat(response.getHeaders("Set-Cookie")).containsExactly("XSRF-TOKEN=preserved; Path=/");
        verifyNoInteractions(authentication, homes);
    }

    @Test
    void refreshesThroughTheServiceProxyAfterTheControllerTransactionHasCommitted() throws Exception {
        var transactions = mock(PlatformTransactionManager.class);
        var first = mock(TransactionStatus.class);
        var second = mock(TransactionStatus.class);
        when(transactions.getTransaction(any())).thenReturn(first, second);
        var interceptor = new TransactionInterceptor();
        interceptor.setTransactionManager(transactions);
        interceptor.setTransactionAttributeSource(new AnnotationTransactionAttributeSource());
        var proxyFactory = new ProxyFactory(homes);
        proxyFactory.addAdvice(interceptor);
        var transactionalHomes = (ChildHomeService) proxyFactory.getProxy();
        var latest = latest("ENDED");
        when(homes.getHome(SESSION, CHILD)).thenReturn(original, latest);
        var controller = new ChildHomeController(authentication, mock(ChildService.class), transactionalHomes);

        filter(transactionalHomes, json).doFilter(request, response, (req, res) -> {
            var body = controller.getHome((HttpServletRequest) req, CHILD.toString()).getBody();
            verify(transactions).commit(first);
            res.getOutputStream().write(json.writeValueAsBytes(body));
        });

        var order = inOrder(transactions, authentication, homes);
        order.verify(authentication).requireAuthenticatedSession(request);
        order.verify(transactions).getTransaction(any());
        order.verify(homes).getHome(SESSION, CHILD);
        order.verify(transactions).commit(first);
        order.verify(authentication).requireAuthenticatedSession(request);
        order.verify(authentication).requireCurrentStoredSession(SESSION_ID, SESSION);
        order.verify(transactions).getTransaction(any());
        order.verify(homes).getHome(SESSION, CHILD);
        order.verify(transactions).commit(second);
        assertThat(response.getContentAsByteArray()).isEqualTo(encoded(latest));
    }

    /** 기존 바깥 인증 오류 처리기를 그대로 조합하며 테스트용 오류 endpoint는 만들지 않는다. */
    private AuthStateErrorFilter outerAuthenticationFilter() {
        return new AuthStateErrorFilter(new AuthStateErrorHandler(json), mock(SessionLifecycleService.class),
                mock(GuardianAccessService.class), json, mock(EmailVerificationIssuanceService.class),
                mock(EmailVerificationService.class));
    }

    private ChildHomeResponseFilter filter(ChildHomeService service, JsonMapper mapper) {
        return new ChildHomeResponseFilter(properties, authentication, service,
                new ChildExceptionHandler(), new ConversationExceptionHandler(), mapper);
    }

    /** flush·길이 충족·Writer 사용 후에도 원본 응답에 아직 바이트가 전달되지 않았는지 확인한다. */
    private FilterChain successfulResponse(String mode, Runnable afterWrite) {
        return (req, res) -> {
            assertThat(res).isInstanceOf(ContentCachingResponseWrapper.class);
            ChildHomeResponseFilter.protect((HttpServletRequest) req, SESSION, CHILD);
            var http = (HttpServletResponse) res;
            http.setStatus(200);
            http.setCharacterEncoding(StandardCharsets.UTF_8.name());
            http.setContentType("application/json");
            http.addHeader("Set-Cookie", "JSESSIONID=synthetic-renewal; Path=/; HttpOnly");
            http.addHeader("Set-Cookie", "XSRF-TOKEN=preserved; Path=/");
            var body = encoded(original);
            switch (mode) {
                case "WRITER" -> {
                    http.getWriter().write(new String(body, StandardCharsets.UTF_8));
                    http.getWriter().flush();
                }
                case "STREAM" -> {
                    http.getOutputStream().write(body);
                    http.getOutputStream().flush();
                }
                default -> {
                    if (mode.equals("CONTENT_LENGTH")) {
                        http.setContentLength(body.length);
                    } else if (mode.equals("CONTENT_LENGTH_LONG")) {
                        http.setContentLengthLong(body.length);
                    } else if (mode.equals("HEADER")) {
                        http.setHeader("Content-Length", Integer.toString(body.length));
                    }
                    http.getOutputStream().write(body);
                    if (mode.equals("FLUSH_BUFFER")) {
                        http.flushBuffer();
                    }
                }
            }
            assertThat(response.getContentAsByteArray()).isEmpty();
            assertThat(response.isCommitted()).isFalse();
            afterWrite.run();
        };
    }

    private static ChildHome latest(String change) {
        var now = change.equals("MIDNIGHT") ? BEFORE.plusSeconds(1) : BEFORE;
        return ChildHome.from(CHILD_CONTEXT, now, null, change.equals("CLOSING"));
    }

    private byte[] encoded(ChildHome home) {
        return json.writeValueAsBytes(Map.of("data", home));
    }

    private void assertCommonHeaders() {
        assertThat(response.getHeader("X-Request-ID")).isEqualTo("test-request-id");
        assertThat(response.getHeader("Cache-Control")).isEqualTo("no-store");
        assertThat(response.getHeader("Access-Control-Allow-Origin")).isEqualTo("https://example.invalid");
        assertThat(response.getHeader("Access-Control-Allow-Credentials")).isEqualTo("true");
        assertThat(response.getHeaders("Vary")).containsExactly("Origin");
    }

    private void assertLengthAndCommonHeaders() {
        assertThat(response.getContentLength()).isEqualTo(response.getContentAsByteArray().length);
        assertCommonHeaders();
    }

    private void assertError(String code) throws Exception {
        Map<String, Object> envelope = JsonPath.read(response.getContentAsString(), "$");
        Map<String, Object> error = JsonPath.read(response.getContentAsString(), "$.error");
        assertThat(envelope).containsOnlyKeys("error");
        assertThat(error).containsOnlyKeys("code", "message", "requestId", "fields")
                .containsEntry("code", code).containsEntry("requestId", "test-request-id");
        assertThat(response.getContentAsString()).doesNotContain("\"data\"", ACTIVE.toString());
        assertCommonHeaders();
    }
}
