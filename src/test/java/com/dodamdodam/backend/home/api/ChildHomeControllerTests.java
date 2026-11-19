package com.dodamdodam.backend.home.api;

import com.dodamdodam.backend.auth.application.AuthRateLimitService;
import com.dodamdodam.backend.auth.application.EmailVerificationIssuanceService;
import com.dodamdodam.backend.auth.application.EmailVerificationService;
import com.dodamdodam.backend.auth.exception.AuthRateLimitException;
import com.dodamdodam.backend.auth.infrastructure.EmailVerificationKeys;
import com.dodamdodam.backend.auth.session.AuthStateErrorHandler;
import com.dodamdodam.backend.auth.session.AuthStateUnavailableException;
import com.dodamdodam.backend.auth.session.AuthenticatedSession;
import com.dodamdodam.backend.auth.session.AuthenticatedSessionService;
import com.dodamdodam.backend.auth.session.SessionLifecycleService;
import com.dodamdodam.backend.child.application.ChildService;
import com.dodamdodam.backend.child.dto.ChildContext;
import com.dodamdodam.backend.child.dto.ChildView;
import com.dodamdodam.backend.child.exception.ChildException;
import com.dodamdodam.backend.conversation.application.ConversationService;
import com.dodamdodam.backend.conversation.api.ConversationStartResponseFilter;
import com.dodamdodam.backend.global.auth.CsrfAccessDeniedHandler;
import com.dodamdodam.backend.global.config.SecurityConfig;
import com.dodamdodam.backend.global.config.SessionConfig;
import com.dodamdodam.backend.guardian.application.GuardianAccessService;
import com.dodamdodam.backend.home.application.ChildHomeService;
import com.dodamdodam.backend.home.dto.ChildHome;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.server.autoconfigure.ServerProperties;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcPrint;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.context.ApplicationContext;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.calls;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 기존 보안 체인·필터·예외 처리기를 사용하는 DB 없는 MVC 계약 테스트다. */
@WebMvcTest(controllers = ChildHomeController.class, properties = {
        "app.security.allowed-origins=http://localhost:5173",
        "app.security.login-success-url=http://localhost:5173/oauth/callback"})
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@EnableConfigurationProperties(ServerProperties.class)
@Import({SecurityConfig.class, SessionConfig.class, AuthStateErrorHandler.class,
        CsrfAccessDeniedHandler.class, EmailVerificationKeys.class, ChildHomeControllerTests.ResponseGate.class})
class ChildHomeControllerTests {

    private static final UUID CHILD = UUID.fromString("22222222-2222-4222-8222-222222222222");
    private static final UUID ACTIVE = UUID.fromString("55555555-5555-4555-8555-555555555555");
    private static final AuthenticatedSession CURRENT = new AuthenticatedSession(UUID.randomUUID(), UUID.randomUUID());
    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-10-03T13:00:00+09:00");
    private static final ChildContext CHILD_CONTEXT = new ChildContext(CHILD, "가나", "별이", LocalDate.of(2020, 5, 12),
            List.of("공룡"), "test-character", OffsetDateTime.parse("2026-10-01T06:00:00+09:00"));
    @Autowired private MockMvc mvc;
    @Autowired private ApplicationContext context;
    @Autowired private ResponseGate responseGate;
    @MockitoBean private AuthenticatedSessionService authentication;
    @MockitoBean private ChildService children;
    @MockitoBean private ChildHomeService homes;
    @MockitoBean private AuthRateLimitService limits;
    @MockitoBean private SessionLifecycleService lifecycle;
    @MockitoBean private GuardianAccessService guardian;
    @MockitoBean private EmailVerificationIssuanceService issuance;
    @MockitoBean private EmailVerificationService verification;
    @MockitoBean private ConversationService conversations;
    @MockitoBean private JdbcIndexedSessionRepository sessions;
    @MockitoBean private CookieSerializer cookies;
    @MockitoBean private ClientRegistrationRepository registrations;

    @BeforeEach
    void prepare() {
        responseGate.reset();
        // 실제 인증·소유권·DB 서비스 검증은 별도 계약 테스트에서 수행한다.
        when(authentication.requireAuthenticatedSession(any())).thenAnswer(invocation -> {
            HttpServletRequest request = invocation.getArgument(0);
            if (request.getSession(false) == null) {
                throw new InsufficientAuthenticationException("로그인이 필요합니다.");
            }
            return CURRENT;
        });
        when(children.requireChildAccess(CURRENT.accountId(), CHILD)).thenReturn(new ChildView(CHILD,
                CHILD_CONTEXT.name(), CHILD_CONTEXT.nickname(), CHILD_CONTEXT.birthDate(), ChildView.Gender.FEMALE,
                CHILD_CONTEXT.interests(), CHILD_CONTEXT.characterId(), CHILD_CONTEXT.createdAt()));
        when(homes.getHome(CURRENT, CHILD)).thenReturn(ChildHome.from(CHILD_CONTEXT, NOW, ACTIVE, false));
    }

    @Test
    void returnsOnlyTheHomeContractInADataEnvelopeWithoutACsrfToken() throws Exception {
        var response = mvc.perform(get(path(CHILD.toString())).session(browser())
                        .header("Origin", "http://localhost:5173").header("X-Request-ID", "client-controlled"))
                .andExpect(status().isOk()).andReturn().getResponse();
        var envelope = object(response, "$");
        var data = object(response, "$.data");

        assertThat(envelope).containsOnlyKeys("data");
        assertThat(data).containsOnlyKeys("child", "greeting", "joinedAt", "daysTogether",
                "activeConversationId", "menus", "availability");
        assertThat(object(response, "$.data.child")).containsOnlyKeys("childId", "name", "nickname", "characterId")
                .containsEntry("childId", CHILD.toString()).containsEntry("name", "가나")
                .containsEntry("nickname", "별이").containsEntry("characterId", "test-character");
        assertThat(data).containsEntry("greeting", "별이, 오늘도 반가워!")
                .containsEntry("joinedAt", "2026-10-01T06:00:00+09:00")
                .containsEntry("daysTogether", 3).containsEntry("activeConversationId", ACTIVE.toString());
        assertThat(data.get("menus")).isEqualTo(List.of(Map.of("code", "TALK", "status", "AVAILABLE"),
                Map.of("code", "PLAY", "status", "COMING_SOON")));
        assertThat(object(response, "$.data.availability")).containsOnlyKeys("timeZone", "serverTime", "serviceDate",
                "opensAt", "closesAt", "canEnter", "reason", "nextOpensAt")
                .containsEntry("timeZone", "Asia/Seoul").containsEntry("serverTime", "2026-10-03T13:00:00+09:00")
                .containsEntry("serviceDate", "2026-10-03").containsEntry("opensAt", "2026-10-03T08:00:00+09:00")
                .containsEntry("closesAt", "2026-10-04T00:00:00+09:00").containsEntry("canEnter", true)
                .containsEntry("reason", null).containsEntry("nextOpensAt", null);
        assertHeaders(response);
        assertThat(response.getHeader("Access-Control-Allow-Origin")).isEqualTo("http://localhost:5173");
        assertThat(response.getHeader("Access-Control-Allow-Credentials")).isEqualTo("true");
        assertThat(response.getHeader("Location")).isNull();
    }

    @Test
    void authenticatesBeforeOwnershipAndHomeLookup() throws Exception {
        mvc.perform(get(path(CHILD.toString())).session(browser())).andExpect(status().isOk());

        var order = inOrder(authentication, children, homes);
        // 처음의 Security·Controller 검증과 응답 필터의 후속 검증을 구분한다.
        order.verify(authentication, calls(2)).requireAuthenticatedSession(any());
        order.verify(children).requireChildAccess(CURRENT.accountId(), CHILD);
        order.verify(homes, calls(1)).getHome(CURRENT, CHILD);
        order.verify(authentication).requireAuthenticatedSession(any());
        order.verify(homes).getHome(CURRENT, CHILD);
    }

    @Test
    void registersHomeFilterOnceBelowSecurityWithoutMovingTheStartFilter() {
        var order = context.getBean(SecurityFilterProperties.class).getOrder();
        assertThat(context.getBeansOfType(ChildHomeResponseFilter.class)).hasSize(1);
        assertThat(context.getBean(ChildHomeResponseFilter.class).getOrder()).isEqualTo(order + 2);
        assertThat(context.getBean(ConversationStartResponseFilter.class).getOrder()).isEqualTo(order + 1);
        var security = context.getBean("springSecurityFilterChain", FilterChainProxy.class);
        assertThat(security.getFilterChains().stream().flatMap(chain -> chain.getFilters().stream()))
                .noneMatch(ChildHomeResponseFilter.class::isInstance);
    }

    @ParameterizedTest
    @ValueSource(strings = {"MIDNIGHT", "CLOSING", "ENDED", "LINK_REMOVED"})
    void replacesTheMvcBodyAfterTheControllerSnapshotBecomesStale(String change) throws Exception {
        var latest = ChildHome.from(CHILD_CONTEXT, change.equals("MIDNIGHT") ? NOW.plusDays(1).withHour(0) : NOW,
                null, change.equals("CLOSING"));
        responseGate.afterHome(() -> when(homes.getHome(CURRENT, CHILD)).thenReturn(latest));

        var response = mvc.perform(get(path(CHILD.toString())).session(browser())
                        .header("Origin", "http://localhost:5173"))
                .andExpect(status().isOk()).andReturn().getResponse();

        assertThat(object(response, "$.data")).containsEntry("activeConversationId", null);
        assertThat(object(response, "$.data.availability"))
                .containsEntry("canEnter", latest.availability().canEnter())
                .containsEntry("reason", latest.availability().reason())
                .containsEntry("serviceDate", latest.availability().serviceDate().toString());
        assertThat(response.getContentAsString()).doesNotContain(ACTIVE.toString());
        assertThat(response.getContentLength()).isEqualTo(response.getContentAsByteArray().length);
        assertThat(response.getHeaders("Set-Cookie")).contains("JSESSIONID=synthetic-renewal; Path=/; HttpOnly");
        assertThat(response.getHeader("Access-Control-Allow-Origin")).isEqualTo("http://localhost:5173");
        assertHeaders(response);
    }

    @ParameterizedTest
    @ValueSource(strings = {"REVOKED", "REPLACED", "STORE_FAILED"})
    void lateAuthenticationFailureRemovesTheMvcSuccessBodyAndRenewalCookie(String kind) throws Exception {
        responseGate.afterHome(() -> {
            if (kind.equals("REPLACED")) {
                doReturn(new AuthenticatedSession(CURRENT.accountId(), UUID.randomUUID()))
                        .when(authentication).requireAuthenticatedSession(any());
            } else if (kind.equals("STORE_FAILED")) {
                doThrow(new AuthStateUnavailableException()).when(authentication).requireAuthenticatedSession(any());
            } else {
                doThrow(new InsufficientAuthenticationException("로그인이 필요합니다."))
                        .when(authentication).requireAuthenticatedSession(any());
            }
        });

        var response = mvc.perform(get(path(CHILD.toString())).session(browser())
                        .header("Origin", "http://localhost:5173"))
                .andExpect(status().is(kind.equals("STORE_FAILED") ? 503 : 401)).andReturn().getResponse();

        assertError(response, kind.equals("STORE_FAILED") ? "AUTH_STATE_UNAVAILABLE" : "AUTHENTICATION_REQUIRED");
        assertThat(response.getContentAsString()).doesNotContain("\"data\"", ACTIVE.toString());
        assertThat(response.getHeaders("Set-Cookie")).noneMatch(value -> value.startsWith("JSESSIONID="));
        assertThat(response.getHeaders("Set-Cookie")).contains("XSRF-TOKEN=preserved; Path=/");
        assertThat(response.getHeader("Access-Control-Allow-Origin")).isEqualTo("http://localhost:5173");
        if (response.getHeader("Content-Length") != null) {
            assertThat(response.getContentLength()).isEqualTo(response.getContentAsByteArray().length);
        }
        verify(homes).getHome(CURRENT, CHILD);
    }

    @ParameterizedTest
    @ValueSource(strings = {"2026-10-03T00:00:00+09:00", "2026-10-03T07:59:59+09:00"})
    void outsideServiceHoursStillReturn200(String time) throws Exception {
        when(homes.getHome(CURRENT, CHILD)).thenReturn(ChildHome.from(CHILD_CONTEXT, OffsetDateTime.parse(time), ACTIVE, true));

        var response = mvc.perform(get(path(CHILD.toString())).session(browser()))
                .andExpect(status().isOk()).andReturn().getResponse();

        assertThat(object(response, "$.data")).containsEntry("activeConversationId", null);
        assertThat(object(response, "$.data.availability")).containsEntry("canEnter", false)
                .containsEntry("reason", "OUTSIDE_SERVICE_HOURS")
                .containsEntry("nextOpensAt", "2026-10-03T08:00:00+09:00");
        assertThat(JsonPath.<String>read(response.getContentAsString(), "$.data.menus[0].status")).isEqualTo("UNAVAILABLE");
        assertHeaders(response);
    }

    @Test
    void closingStillReturns200WithAnExplicitNullActiveId() throws Exception {
        when(homes.getHome(CURRENT, CHILD)).thenReturn(ChildHome.from(CHILD_CONTEXT, NOW, ACTIVE, true));

        var response = mvc.perform(get(path(CHILD.toString())).session(browser()))
                .andExpect(status().isOk()).andReturn().getResponse();

        assertThat(object(response, "$.data")).containsEntry("activeConversationId", null);
        assertThat(object(response, "$.data.availability")).containsEntry("canEnter", false)
                .containsEntry("reason", "CONVERSATION_CLOSING").containsEntry("nextOpensAt", null);
        assertHeaders(response);
    }

    @Test
    void anonymousRequestIs401BeforeUuidAndOwnershipValidation() throws Exception {
        var response = mvc.perform(get(path("not-a-uuid")))
                .andExpect(status().isUnauthorized()).andReturn().getResponse();

        assertError(response, "AUTHENTICATION_REQUIRED");
        verifyNoInteractions(children, homes);
    }

    @ParameterizedTest
    @ValueSource(strings = {"not-a-uuid", "1-1-1-1-1", "22222222-2222-4222-8222-22222222222z"})
    void malformedChildIdIs400BeforeOwnershipAndService(String childId) throws Exception {
        var response = mvc.perform(get(path(childId)).session(browser()))
                .andExpect(status().isBadRequest()).andReturn().getResponse();

        assertError(response, "VALIDATION_FAILED");
        assertThat(JsonPath.<String>read(response.getContentAsString(), "$.error.fields[0].field")).isEqualTo("childId");
        verifyNoInteractions(children, homes);
    }

    @ParameterizedTest
    @ValueSource(strings = {"MISSING", "OTHER_OWNER"})
    void missingAndUnownedChildrenShareTheSame404(String scenario) throws Exception {
        var childId = scenario.equals("MISSING") ? UUID.fromString("33333333-3333-4333-8333-333333333333") : CHILD;
        when(children.requireChildAccess(CURRENT.accountId(), childId)).thenThrow(ChildException.notFound());

        var response = mvc.perform(get(path(childId.toString())).session(browser()))
                .andExpect(status().isNotFound()).andReturn().getResponse();

        assertError(response, "RESOURCE_NOT_FOUND");
        assertThat(response.getContentAsString()).doesNotContain(childId.toString(), "가나", "별이");
        verifyNoInteractions(homes);
    }

    @Test
    void globalRateLimitKeepsTheExisting429AndRetryAfterContract() throws Exception {
        doThrow(new AuthRateLimitException(12)).when(limits).reserve(any());

        var response = mvc.perform(get(path(CHILD.toString())).session(browser()))
                .andExpect(status().isTooManyRequests()).andReturn().getResponse();

        assertError(response, "RATE_LIMITED");
        assertThat(response.getHeader("Retry-After")).isEqualTo("12");
        verifyNoInteractions(authentication, children, homes);
    }

    @Test
    void authenticationStoreFailureKeepsTheExisting503Contract() throws Exception {
        doThrow(new AuthStateUnavailableException()).when(authentication).requireAuthenticatedSession(any());

        var response = mvc.perform(get(path(CHILD.toString())).session(browser()))
                .andExpect(status().isServiceUnavailable()).andReturn().getResponse();

        assertError(response, "AUTH_STATE_UNAVAILABLE");
        verifyNoInteractions(children, homes);
    }

    /** 실제 로그인 endpoint 없이 MVC 인증 협력 객체에 전달할 합성 세션만 구성한다. */
    private static MockHttpSession browser() {
        var session = new MockHttpSession();
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(CURRENT.accountId().toString(), null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))));
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        session.setAttribute(AuthenticatedSessionService.ACCOUNT_ID, CURRENT.accountId());
        session.setAttribute(AuthenticatedSessionService.SECURITY_CONTEXT_ID, CURRENT.securityContextId());
        return session;
    }

    private static String path(String childId) {
        return "/api/v1/children/" + childId + "/home";
    }

    private static Map<String, Object> object(MockHttpServletResponse response, String path) throws Exception {
        return JsonPath.read(response.getContentAsString(), path);
    }

    private static void assertHeaders(MockHttpServletResponse response) {
        assertThat(response.getHeader("Cache-Control")).contains("no-store");
        var requestId = response.getHeader("X-Request-ID");
        assertThat(requestId).isNotBlank().isNotEqualTo("client-controlled");
        assertThat(UUID.fromString(requestId).toString()).isEqualTo(requestId);
    }

    private static void assertError(MockHttpServletResponse response, String code) throws Exception {
        assertThat(object(response, "$")).containsOnlyKeys("error");
        assertThat(object(response, "$.error")).containsOnlyKeys("code", "message", "requestId", "fields")
                .containsEntry("code", code).containsEntry("requestId", response.getHeader("X-Request-ID"));
        assertHeaders(response);
    }

    /** Controller 반환과 MVC 직렬화 사이에 변경을 삽입하며 실제 endpoint는 추가하지 않는다. */
    @TestConfiguration(proxyBeanMethods = false)
    @ControllerAdvice
    @NullMarked
    static class ResponseGate implements ResponseBodyAdvice<Object> {
        private final AtomicReference<@Nullable Runnable> next = new AtomicReference<>();
        private final JsonMapper json;

        ResponseGate(JsonMapper json) {
            this.json = json;
        }

        void afterHome(Runnable change) {
            assertThat(next.compareAndSet(null, change)).isTrue();
        }

        void reset() {
            next.set(null);
        }

        @Override
        public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
            return returnType.getContainingClass() == ChildHomeController.class;
        }

        @Override
        public @Nullable Object beforeBodyWrite(@Nullable Object body, MethodParameter returnType, MediaType contentType,
                Class<? extends HttpMessageConverter<?>> converterType, ServerHttpRequest request, ServerHttpResponse response) {
            var change = next.getAndSet(null);
            if (change != null) {
                response.getHeaders().setContentLength(json.writeValueAsBytes(body).length);
                response.getHeaders().add("Set-Cookie", "JSESSIONID=synthetic-renewal; Path=/; HttpOnly");
                response.getHeaders().add("Set-Cookie", "XSRF-TOKEN=preserved; Path=/");
                change.run();
            }
            return body;
        }
    }
}
