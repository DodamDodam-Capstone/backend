package com.dodamdodam.backend.conversation.api;

import com.dodamdodam.backend.TestcontainersConfiguration;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcPrint;
import org.springframework.context.annotation.Import;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockCookie;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.session.Session;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@Import({TestcontainersConfiguration.class, GuardianConversationDetailContractTests.ExpirePinBeforeSerialization.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class GuardianConversationDetailContractTests {

    private static final String DETAIL_PATH = "/api/v1/children/{childId}/conversations/{conversationId}";

    @Autowired private MockMvc mvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private JdbcIndexedSessionRepository sessions;
    @Autowired private CookieSerializer cookies;

    @Test
    void checksLoginThenChildAndConversationOwnershipBeforePin() throws Exception {
        var locked = parent(false);
        var other = parent(true);
        var ownedConversation = conversation(locked.childId(), "ENDED", "EMPTY");
        var foreignConversation = conversation(other.childId(), "ENDED", "EMPTY");

        error(mvc.perform(get(DETAIL_PATH, "invalid", "invalid")), 401, "AUTHENTICATION_REQUIRED");
        var notFound = notFoundBody(detail(locked, locked.childId(), UUID.randomUUID()));
        assertThat(notFoundBody(detail(locked, locked.childId(), foreignConversation))).isEqualTo(notFound);
        assertThat(notFoundBody(detail(locked, other.childId(), foreignConversation))).isEqualTo(notFound);
        assertThat(notFoundBody(detail(locked, UUID.randomUUID(), ownedConversation))).isEqualTo(notFound);
        error(detail(locked, locked.childId(), ownedConversation), 403, "GUARDIAN_UNLOCK_REQUIRED");
    }

    @Test
    void checksPinBeforeEvaluatingConversationStatus() throws Exception {
        var locked = parent(false);
        var active = conversation(locked.childId(), "ACTIVE", "NOT_STARTED");
        error(detail(locked, locked.childId(), active), 403, "GUARDIAN_UNLOCK_REQUIRED");

        var unlocked = parent(true);
        var visibleOnlyAfterEnd = conversation(unlocked.childId(), "ACTIVE", "NOT_STARTED");
        error(detail(unlocked, unlocked.childId(), visibleOnlyAfterEnd), 404, "RESOURCE_NOT_FOUND");
        jdbc.update("UPDATE conversations SET status='CLOSING', end_reason='MANUAL', end_requested_at=started_at WHERE id=?",
                visibleOnlyAfterEnd);
        error(detail(unlocked, unlocked.childId(), visibleOnlyAfterEnd), 404, "RESOURCE_NOT_FOUND");
    }

    @ParameterizedTest
    @CsvSource({"afterSequence,-1", "afterSequence,1.2", "afterSequence,", "size,0", "size,101", "size,abc", "size, "})
    void validatesCursorAndSizeBeforeCheckingOwnershipOrPin(String field, String value) throws Exception {
        var locked = parent(false);
        mvc.perform(get(DETAIL_PATH, UUID.randomUUID(), UUID.randomUUID()).cookie(locked.cookie())
                        .queryParam(field, value == null ? "" : value))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.error.fields[0].field").value(field));
    }

    @Test
    void rejectsAbbreviatedConversationIdBeforeReadingOwnership() throws Exception {
        var locked = parent(false);
        detail(locked, locked.childId(), "1-1-1-1-1").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields[0].field").value("conversationId"));
    }

    @Test
    void acceptsANonnegativeCursorPastTheDatabaseIntegerRangeAsAnEmptyPage() throws Exception {
        var unlocked = parent(true);
        var ended = conversation(unlocked.childId(), "ENDED", "EMPTY");
        mvc.perform(get(DETAIL_PATH, unlocked.childId(), ended).cookie(unlocked.cookie())
                        .queryParam("afterSequence", "2147483648"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.turns.length()").value(0))
                .andExpect(jsonPath("$.data.hasNext").value(false))
                .andExpect(jsonPath("$.data.nextAfterSequence").value((Object) null));
    }

    @Test
    void failsClosedWhenConversationOwnershipCannotBeVerified() throws Exception {
        var locked = parent(false);
        var conversation = conversation(locked.childId(), "ENDED", "EMPTY");
        jdbc.execute("ALTER TABLE conversations RENAME TO unavailable_conversations");
        try {
            error(detail(locked, locked.childId(), conversation), 503, "AUTH_STATE_UNAVAILABLE");
        }
        finally {
            jdbc.execute("ALTER TABLE unavailable_conversations RENAME TO conversations");
        }
    }

    @Test
    void discardsPreparedPageWhenPinExpiresBeforeWritingResponse() throws Exception {
        var unlocked = parent(true);
        var conversation = conversation(unlocked.childId(), "ENDED", "EMPTY");
        var call = mvc.perform(get(DETAIL_PATH, unlocked.childId(), conversation).cookie(unlocked.cookie())
                .header("X-Test-Expire-Guardian-Context", unlocked.contextId().toString()));

        error(call, 403, "GUARDIAN_UNLOCK_REQUIRED");
        assertThat(call.andReturn().getResponse().getContentAsString()).doesNotContain(conversation.toString());
    }

    @Test
    void readsPermittedFieldsInSequenceAcrossTheDefaultHundredTurnBoundary() throws Exception {
        var unlocked = parent(true);
        var conversation = conversation(unlocked.childId(), "ENDED", "PENDING");
        jdbc.update("""
                INSERT INTO conversation_turns (id, conversation_id, client_request_id, request_hash, sequence,
                    status, child_text, child_text_visibility, reply_text, reply_text_visibility, error_code,
                    created_at, completed_at, processing_deadline_at)
                SELECT gen_random_uuid(), ?, gen_random_uuid(), repeat('a', 64),
                    CASE WHEN seq=101 THEN 103 ELSE seq END,
                    CASE WHEN seq=2 THEN 'FAILED' ELSE 'SUCCEEDED' END,
                    CASE WHEN seq=1 THEN '허용된 대체 텍스트' ELSE NULL END,
                    CASE WHEN seq=1 THEN 'REDACTED' ELSE 'OMITTED' END,
                    CASE WHEN seq=1 THEN '허용된 답변' ELSE NULL END,
                    CASE WHEN seq=1 THEN 'VISIBLE' ELSE 'OMITTED' END,
                    CASE WHEN seq=2 THEN 'AI_TIMEOUT' ELSE NULL END,
                    TIMESTAMPTZ '2026-10-01 10:00:01+09', TIMESTAMPTZ '2026-10-01 10:00:02+09',
                    TIMESTAMPTZ '2026-10-01 10:00:10+09'
                FROM generate_series(101, 1, -1) AS seq
                """, conversation);

        var firstPage = detail(unlocked, unlocked.childId(), conversation)
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.data.conversation.status").value("ENDED"))
                .andExpect(jsonPath("$.data.conversation.summaryStatus").value("PENDING"))
                .andExpect(jsonPath("$.data.conversation.endRequestedAt").value("2026-10-01T10:01:00+09:00"))
                .andExpect(jsonPath("$.data.conversation.summary").value((Object) null))
                .andExpect(jsonPath("$.data.turns.length()").value(100))
                .andExpect(jsonPath("$.data.turns[0].sequence").value(1))
                .andExpect(jsonPath("$.data.turns[0].childText").value("허용된 대체 텍스트"))
                .andExpect(jsonPath("$.data.turns[0].childTextVisibility").value("REDACTED"))
                .andExpect(jsonPath("$.data.turns[0].replyText").value("허용된 답변"))
                .andExpect(jsonPath("$.data.turns[0].replyTextVisibility").value("VISIBLE"))
                .andExpect(jsonPath("$.data.turns[1].sequence").value(2))
                .andExpect(jsonPath("$.data.turns[1].childText").value((Object) null))
                .andExpect(jsonPath("$.data.turns[1].replyTextVisibility").value("OMITTED"))
                .andExpect(jsonPath("$.data.turns[1].errorCode").value("AI_TIMEOUT"))
                .andExpect(jsonPath("$.data.turns[99].sequence").value(100))
                .andExpect(jsonPath("$.data.hasNext").value(true))
                .andExpect(jsonPath("$.data.nextAfterSequence").value(100));
        assertThat(firstPage.andReturn().getResponse().getContentAsString()).doesNotContain(
                "requestHash", "clientRequestId", "processingDeadlineAt", "serviceDate", "summaryDeadlineAt", "audio");

        detail(unlocked, unlocked.childId(), conversation, 100, 1)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.turns.length()").value(1))
                .andExpect(jsonPath("$.data.turns[0].sequence").value(103))
                .andExpect(jsonPath("$.data.hasNext").value(false))
                .andExpect(jsonPath("$.data.nextAfterSequence").value((Object) null));
        detail(unlocked, unlocked.childId(), conversation, 103, 1)
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.turns.length()").value(0));

        jdbc.update("UPDATE guardian_pins SET pin_version=2 WHERE account_id=?", unlocked.accountId());
        error(detail(unlocked, unlocked.childId(), conversation, 100, 1), 403, "GUARDIAN_UNLOCK_REQUIRED");
    }

    @Test
    void returnsEmptyAndFailedSummariesWithoutInventingContent() throws Exception {
        var unlocked = parent(true);
        for (String summary : List.of("EMPTY", "FAILED")) {
            var ended = conversation(unlocked.childId(), "ENDED", summary);
            detail(unlocked, unlocked.childId(), ended)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.conversation.summaryStatus").value(summary))
                    .andExpect(jsonPath("$.data.conversation.title").value((Object) null))
                    .andExpect(jsonPath("$.data.conversation.topic").value((Object) null))
                    .andExpect(jsonPath("$.data.conversation.summary").value((Object) null))
                    .andExpect(jsonPath("$.data.turns.length()").value(0))
                    .andExpect(jsonPath("$.data.hasNext").value(false))
                    .andExpect(jsonPath("$.data.nextAfterSequence").value((Object) null));
        }
    }

    private ResultActions detail(Parent parent, UUID childId, Object conversationId) throws Exception {
        return mvc.perform(get(DETAIL_PATH, childId, conversationId).cookie(parent.cookie()));
    }

    private ResultActions detail(Parent parent, UUID childId, UUID conversationId, int afterSequence, int size) throws Exception {
        return mvc.perform(get(DETAIL_PATH, childId, conversationId).cookie(parent.cookie())
                .queryParam("afterSequence", String.valueOf(afterSequence)).queryParam("size", String.valueOf(size)));
    }

    private void error(ResultActions call, int expectedStatus, String code) throws Exception {
        call.andExpect(status().is(expectedStatus)).andExpect(jsonPath("$.error.code").value(code))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    private Map<String, Object> notFoundBody(ResultActions call) throws Exception {
        error(call, 404, "RESOURCE_NOT_FOUND");
        var response = call.andReturn().getResponse();
        return JsonPath.parse(response.getContentAsString()).delete("$.error.requestId").read("$");
    }

    private Parent parent(boolean unlocked) {
        var account = UUID.randomUUID();
        var child = UUID.randomUUID();
        var context = UUID.randomUUID();
        jdbc.update("INSERT INTO accounts(id,email) VALUES (?,?)", account, account + "@example.invalid");
        jdbc.update("""
                INSERT INTO children (id, account_id, name, nickname, birth_date, gender, character_id)
                VALUES (?, ?, '가나', '별이', DATE '2020-05-12', 'FEMALE', 'test-character')
                """, child, account);
        jdbc.update("INSERT INTO guardian_pins(account_id,pin_hash) VALUES (?,'test-fixture')", account);
        jdbc.update("INSERT INTO session_security(id,account_id,session_version) VALUES (?,?,0)", context, account);
        if (unlocked) {
            jdbc.update("""
                    UPDATE session_security SET guardian_unlocked_until=clock_timestamp() + INTERVAL '10 minutes',
                        guardian_pin_version=1 WHERE id=?
                    """, context);
        }
        Session session = sessions.createSession();
        session.setMaxInactiveInterval(Duration.ofSeconds(-1));
        var security = SecurityContextHolder.createEmptyContext();
        security.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(account.toString(), null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))));
        session.setAttribute("accountId", account);
        session.setAttribute("securityContextId", context);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, security);
        var response = new MockHttpServletResponse();
        cookies.writeCookieValue(new CookieSerializer.CookieValue(new MockHttpServletRequest(), response, session.getId()));
        return new Parent(account, child, context, MockCookie.parse(response.getHeader("Set-Cookie")));
    }

    private UUID conversation(UUID child, String status, String summaryStatus) {
        var id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO conversations (id, child_id, client_request_id, service_date, scheduled_end_at, status,
                    summary_status, started_at)
                VALUES (?, ?, ?, DATE '2026-10-01', TIMESTAMPTZ '2026-10-02 00:00:00+09', 'ACTIVE',
                    'NOT_STARTED', TIMESTAMPTZ '2026-10-01 10:00:00+09')
                """, id, child, UUID.randomUUID());
        if (status.equals("ENDED")) {
            jdbc.update("""
                    UPDATE conversations SET status='ENDED', end_reason='MANUAL', summary_status=?,
                        end_requested_at=started_at + INTERVAL '1 minute', ended_at=started_at + INTERVAL '1 minute',
                        summary_started_at=CASE WHEN ? IN ('PENDING','FAILED') THEN started_at + INTERVAL '1 minute' END,
                        summary_deadline_at=CASE WHEN ? IN ('PENDING','FAILED') THEN started_at + INTERVAL '2 minutes' END,
                        summary_completed_at=CASE WHEN ?='FAILED' THEN started_at + INTERVAL '2 minutes' END,
                        summary_error_code=CASE WHEN ?='FAILED' THEN 'AI_TIMEOUT' END
                    WHERE id=?
                    """, summaryStatus, summaryStatus, summaryStatus, summaryStatus, summaryStatus, id);
        }
        return id;
    }

    private record Parent(UUID accountId, UUID childId, UUID contextId, Cookie cookie) { }

    @ControllerAdvice(assignableTypes = GuardianConversationController.class)
    static class ExpirePinBeforeSerialization implements ResponseBodyAdvice<Object> {

        @Autowired private JdbcTemplate jdbc;

        @Override
        public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
            return "detail".equals(returnType.getMethod().getName());
        }

        @Override
        public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType contentType,
                                      Class<? extends HttpMessageConverter<?>> converterType,
                                      ServerHttpRequest request, ServerHttpResponse response) {
            var context = request.getHeaders().getFirst("X-Test-Expire-Guardian-Context");
            if (context != null) {
                // 서비스 조회가 성공한 뒤 HTTP 응답을 만들기 직전에 기존 권한을 만료시킨다.
                jdbc.update("UPDATE session_security SET guardian_unlocked_until=clock_timestamp() WHERE id=?",
                        UUID.fromString(context));
            }
            return body;
        }
    }
}
