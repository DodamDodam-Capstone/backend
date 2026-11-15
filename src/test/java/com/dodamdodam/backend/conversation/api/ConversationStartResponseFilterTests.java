package com.dodamdodam.backend.conversation.api;

import com.dodamdodam.backend.auth.session.AuthStateUnavailableException;
import com.dodamdodam.backend.auth.session.AuthenticatedSession;
import com.dodamdodam.backend.auth.session.AuthenticatedSessionService;
import com.dodamdodam.backend.auth.session.SessionCookieRenewalFilter.InvalidSessionException;
import com.dodamdodam.backend.child.api.ChildExceptionHandler;
import com.dodamdodam.backend.conversation.application.ConversationService;
import com.dodamdodam.backend.conversation.exception.ConversationException;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterProperties;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ConversationStartResponseFilterTests {

    private static final AuthenticatedSession SESSION = new AuthenticatedSession(UUID.randomUUID(), UUID.randomUUID());
    private static final UUID CHILD = UUID.randomUUID();
    private static final UUID KEY = UUID.randomUUID();
    private static final UUID CONVERSATION = UUID.randomUUID();
    private static final String PATH = "/api/v1/children/" + CHILD + "/conversations";
    private static final String BODY = "{\"data\":{\"conversationId\":\"" + CONVERSATION + "\"}}";
    private final AuthenticatedSessionService authentication = mock(AuthenticatedSessionService.class);
    private final ConversationService conversations = mock(ConversationService.class);
    private final SecurityFilterProperties properties = new SecurityFilterProperties();
    private ConversationStartResponseFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void prepare() {
        filter = new ConversationStartResponseFilter(properties, authentication, conversations,
                new ConversationExceptionHandler(), new ChildExceptionHandler(), JsonMapper.builder().build());
        request = new MockHttpServletRequest("POST", PATH);
        response = new MockHttpServletResponse();
        response.setHeader("X-Request-ID", "test-request-id");
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Access-Control-Allow-Origin", "https://example.invalid");
        when(authentication.requireAuthenticatedSession(request)).thenReturn(SESSION);
    }

    @ParameterizedTest
    @ValueSource(strings = {"CONTENT_LENGTH", "WRITER", "FLUSH_BUFFER", "NO_FLUSH"})
    void rejectsBeforeEveryCommitPathAndBeforeAnUncommittedReturn(String mode) throws Exception {
        doThrow(ConversationException.serviceHoursClosed()).when(conversations).verifyStartResponse(SESSION, CHILD, KEY, CONVERSATION);
        filter.doFilter(request, response, successfulResponse(mode));

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(JsonPath.<String>read(response.getContentAsString(), "$.error.code")).isEqualTo("SERVICE_HOURS_CLOSED");
        assertThat(JsonPath.<String>read(response.getContentAsString(), "$.error.requestId")).isEqualTo("test-request-id");
        assertThat(response.getContentAsString()).doesNotContain("\"data\"", CONVERSATION.toString());
        assertThat(response.getHeader("Location")).isNull();
        assertThat(response.getHeader("Content-Length")).isNull();
        assertThat(response.getHeader("Set-Cookie")).isNull();
        assertThat(response.getHeader("Cache-Control")).isEqualTo("no-store");
        assertThat(response.getHeader("Access-Control-Allow-Origin")).isEqualTo("https://example.invalid");
    }

    @ParameterizedTest
    @ValueSource(strings = {"CONTENT_LENGTH", "WRITER", "FLUSH_BUFFER", "NO_FLUSH"})
    void unchangedResponseKeepsBodyStatusAndLocation(String mode) throws Exception {
        filter.doFilter(request, response, successfulResponse(mode));
        assertThat(response.getStatus()).isEqualTo(201);
        assertThat(response.getContentAsString()).isEqualTo(BODY);
        assertThat(response.getHeader("Location")).isEqualTo(PATH + "/" + CONVERSATION);
        verify(conversations, atLeastOnce()).verifyStartResponse(SESSION, CHILD, KEY, CONVERSATION);
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 401, 403, 404, 409, 415, 500, 503})
    void existingErrorIsNotRevalidatedOrReplaced(int status) throws Exception {
        filter.doFilter(request, response, (req, res) -> {
            ConversationStartResponseFilter.protect(request, SESSION, CHILD, KEY, CONVERSATION);
            ((HttpServletResponse) res).setStatus(status);
            res.getOutputStream().write("existing-error".getBytes(StandardCharsets.UTF_8));
            res.flushBuffer();
        });
        assertThat(response.getStatus()).isEqualTo(status);
        assertThat(response.getContentAsString()).isEqualTo("existing-error");
        verifyNoInteractions(conversations);
        verify(authentication, org.mockito.Mockito.never()).requireAuthenticatedSession(request);
    }

    @ParameterizedTest
    @CsvSource({"POST,/api/v1/children", "POST,/api/v1/auth/logout", "GET,/api/v1/children/child/conversations"})
    void otherRoutesAreNotWrapped(String method, String path) throws Exception {
        var other = new MockHttpServletRequest(method, path);
        filter.doFilter(other, response, (req, res) -> {
            assertThat(req).isSameAs(other);
            assertThat(res).isSameAs(response);
        });
        verifyNoInteractions(conversations);
    }

    @Test
    void changedAuthenticationUsesExistingOuter401PathAndDiscardsSuccess() throws Exception {
        when(authentication.requireAuthenticatedSession(request)).thenReturn(
                new AuthenticatedSession(SESSION.accountId(), UUID.randomUUID()));
        assertThatThrownBy(() -> filter.doFilter(request, response, successfulResponse("CONTENT_LENGTH")))
                .isInstanceOf(InvalidSessionException.class);
        assertThat(response.getContentAsString()).isEmpty();
        assertThat(response.getHeader("Location")).isNull();
        verifyNoInteractions(conversations);
    }

    @Test
    void authenticationStoreFailureUsesExistingOuter503Path() throws Exception {
        var unavailable = new AuthStateUnavailableException(new IllegalStateException("test-only store outage"));
        doThrow(unavailable).when(authentication).requireAuthenticatedSession(request);
        assertThatThrownBy(() -> filter.doFilter(request, response, successfulResponse("CONTENT_LENGTH")))
                .isSameAs(unavailable);
        assertThat(response.getContentAsString()).isEmpty();
        assertThat(response.getHeader("Location")).isNull();
        verifyNoInteractions(conversations);
    }

    @Test
    void servletRegistrationFollowsConfiguredSecurityOrder() {
        properties.setOrder(350);
        var configured = new ConversationStartResponseFilter(properties, authentication, conversations,
                new ConversationExceptionHandler(), new ChildExceptionHandler(), JsonMapper.builder().build());
        assertThat(configured.getOrder()).isEqualTo(351);
    }

    private FilterChain successfulResponse(String mode) {
        return (req, res) -> {
            var outgoing = (HttpServletResponse) res;
            ConversationStartResponseFilter.protect(request, SESSION, CHILD, KEY, CONVERSATION);
            outgoing.setStatus(201);
            outgoing.setCharacterEncoding(StandardCharsets.UTF_8.name());
            outgoing.setHeader("Location", PATH + "/" + CONVERSATION);
            outgoing.setHeader("Set-Cookie", "JSESSIONID=test-session; Max-Age=3600; Path=/");
            if (mode.equals("CONTENT_LENGTH")) {
                outgoing.setContentLength(BODY.getBytes(StandardCharsets.UTF_8).length);
            }
            if (mode.equals("WRITER")) {
                outgoing.getWriter().write(BODY);
                outgoing.getWriter().flush();
            }
            else {
                outgoing.getOutputStream().write(BODY.getBytes(StandardCharsets.UTF_8));
                if (mode.equals("FLUSH_BUFFER")) {
                    outgoing.flushBuffer();
                }
            }
        };
    }
}
