package com.dodamdodam.backend.conversation.api;

import com.dodamdodam.backend.TestcontainersConfiguration;
import jakarta.servlet.http.Cookie;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcPrint;
import org.springframework.context.annotation.Import;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@SpringBootTest(properties = "spring.session.jdbc.cleanup-cron=-")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class GuardianConversationListHttpTests {

    private static final String PATH = "/api/v1/children/%s/conversations";
    private static final LocalDate SERVICE_DATE = LocalDate.of(2026, 10, 3);
    private static final ZoneOffset KST = ZoneOffset.ofHours(9);

    @Autowired private MockMvc mvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private JdbcIndexedSessionRepository sessions;
    @Autowired private CookieSerializer cookies;

    @AfterEach
    void cleanDatabase() {
        jdbc.update("DELETE FROM conversation_turns");
        jdbc.update("DELETE FROM conversations");
        jdbc.update("DELETE FROM SPRING_SESSION");
        jdbc.update("DELETE FROM guardian_pins");
        jdbc.update("DELETE FROM children");
        jdbc.update("DELETE FROM session_security");
        jdbc.update("DELETE FROM accounts");
    }

    @Test
    void filtersByKstStartDateAndOwnerAndUsesStableSizePlusOnePaging() throws Exception {
        var owner = fixture(true);
        var sameDayLast = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");
        var sameTimeFirst = UUID.fromString("00000000-0000-0000-0000-000000000001");
        conversation(owner.childId(), sameDayLast, SERVICE_DATE, 15, "ENDED", "EMPTY");
        conversation(owner.childId(), sameTimeFirst, SERVICE_DATE, 15, "ENDED", "PENDING");
        var older = conversation(owner.childId(), UUID.randomUUID(), SERVICE_DATE, 8, "ENDED", "EMPTY");
        conversation(owner.childId(), UUID.randomUUID(), SERVICE_DATE.plusDays(1), 8, "ENDED", "EMPTY");
        conversation(fixture(true).childId(), UUID.randomUUID(), SERVICE_DATE, 23, "ENDED", "EMPTY");
        String day = SERVICE_DATE.toString();

        mvc.perform(list(owner).param("from", day).param("to", day).param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.items[0].conversationId").value(sameDayLast.toString()))
                .andExpect(jsonPath("$.data.items[0].status").value("ENDED"))
                .andExpect(jsonPath("$.data.items[0].summaryStatus").value("EMPTY"))
                .andExpect(jsonPath("$.data.items[0].startedAt").value("2026-10-03T15:00:00+09:00"))
                .andExpect(jsonPath("$.data.items[0].endedAt").value("2026-10-04T00:00:01+09:00"))
                .andExpect(jsonPath("$.data.items[0].clientRequestId").doesNotHaveJsonPath())
                .andExpect(jsonPath("$.data.items[0].summaryStartedAt").doesNotHaveJsonPath())
                .andExpect(jsonPath("$.data.items[1].conversationId").value(sameTimeFirst.toString()))
                .andExpect(jsonPath("$.data.items[1].summaryStatus").value("PENDING"))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(2))
                .andExpect(jsonPath("$.data.hasNext").value(true));

        mvc.perform(list(owner).param("from", day).param("to", day).param("page", "1").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].conversationId").value(older.toString()))
                .andExpect(jsonPath("$.data.hasNext").value(false));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ACTIVE", "CLOSING"})
    void excludesUnfinishedConversationsAndDefaultsToFirstTwenty(String state) throws Exception {
        var owner = fixture(true);
        conversation(owner.childId(), UUID.randomUUID(), SERVICE_DATE, 13, state, "NOT_STARTED");
        mvc.perform(list(owner)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isEmpty())
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.data.hasNext").value(false));
    }

    @Test
    void authenticatesBeforeInputAndChecksOwnershipBeforeGuardianGrant() throws Exception {
        var owner = fixture(false);
        var stranger = fixture(false);
        assertError(get(PATH.formatted("bad-id")).param("page", "x"), 401, "AUTHENTICATION_REQUIRED");
        assertError(get(PATH.formatted("bad-id")).cookie(owner.cookie()), 400, "VALIDATION_FAILED")
                .andExpect(jsonPath("$.error.fields[0].field").value("childId"));
        assertError(get(PATH.formatted(owner.childId())).cookie(stranger.cookie()), 404, "RESOURCE_NOT_FOUND");
        assertError(get(PATH.formatted(UUID.randomUUID())).cookie(stranger.cookie()), 404, "RESOURCE_NOT_FOUND");
        assertError(list(owner), 403, "GUARDIAN_UNLOCK_REQUIRED");

        var expired = fixture(true);
        jdbc.update("UPDATE session_security SET guardian_unlocked_until=clock_timestamp() WHERE id=?", expired.contextId());
        assertError(list(expired), 403, "GUARDIAN_UNLOCK_REQUIRED");
    }

    @ParameterizedTest
    @CsvSource({"page,-1", "page,9223372036854775808", "size,0", "size,101", "size,1.5",
            "from,2026-02-30", "from,2026-1-03", "to,+999999999-12-31"})
    void rejectsInvalidFieldsBeforeGuardianCheck(String name, String value) throws Exception {
        assertError(list(fixture(false)).param(name, value), 400, "VALIDATION_FAILED")
                .andExpect(jsonPath("$.error.fields[0].field").value(name));
    }

    @Test
    void rejectsReversedDatesAndTrueOffsetOverflowButAcceptsBigintOffset() throws Exception {
        var owner = fixture(true);
        assertError(list(owner).param("from", "2026-10-04").param("to", "2026-10-03"), 400, "VALIDATION_FAILED")
                .andExpect(jsonPath("$.error.fields[0].field").value("to"));
        assertError(list(owner).param("page", Long.toString(Long.MAX_VALUE)).param("size", "2"), 400, "VALIDATION_FAILED")
                .andExpect(jsonPath("$.error.fields[0].field").value("page"));
        mvc.perform(list(owner).param("page", "2147483648").param("size", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.items").isEmpty())
                .andExpect(jsonPath("$.data.hasNext").value(false));
    }

    @ParameterizedTest
    @ValueSource(strings = {"children", "conversations"})
    void readFailureReturnsCommon503(String table) throws Exception {
        var owner = fixture(true);
        jdbc.execute("ALTER TABLE " + table + " RENAME TO list_test_unavailable");
        try {
            assertError(list(owner), 503, "AUTH_STATE_UNAVAILABLE");
        }
        finally {
            jdbc.execute("ALTER TABLE list_test_unavailable RENAME TO " + table);
        }
    }

    @Test
    void searchMatchesSummaryAndAllowedTurnFieldsWithoutDuplicates() throws Exception {
        Fixture owned = fixture(true);
        Fixture unrelated = fixture(true);
        LocalDate date = LocalDate.of(2026, 10, 2);
        UUID withSummary = readyConversation(owned.childId(), date, 10);
        jdbc.update("UPDATE conversations SET summary = ? WHERE id = ?", "The DODAM  story", withSummary);
        UUID withTurns = conversation(owned.childId(), UUID.randomUUID(), date, 11, "ENDED", "PENDING");
        insertTurn(withTurns, 1, "Someone said Dodam  story", "REDACTED", null, "OMITTED");
        insertTurn(withTurns, 2, null, "OMITTED", "Dodam  story reply", "VISIBLE");
        UUID childTextOnly = conversation(owned.childId(), UUID.randomUUID(), date, 12, "ENDED", "PENDING");
        insertTurn(childTextOnly, 1, "Dodam  story from child", "VISIBLE", "How did you feel?", "VISIBLE");
        UUID replyTextOnly = conversation(owned.childId(), UUID.randomUUID(), date, 9, "ENDED", "PENDING");
        insertTurn(replyTextOnly, 1, "Can you tell me more?", "VISIBLE", "Another Dodam  story", "VISIBLE");
        UUID redactedTextOnly = conversation(owned.childId(), UUID.randomUUID(), date, 8, "ENDED", "PENDING");
        insertTurn(redactedTextOnly, 1, "Dodam  story from [child]", "REDACTED", null, "OMITTED");
        UUID active = conversation(owned.childId(), UUID.randomUUID(), date, 13, "ACTIVE", "NOT_STARTED");
        insertTurn(active, 1, "DODAM  story", "VISIBLE", null, "OMITTED");
        UUID other = readyConversation(unrelated.childId(), date, 14);
        jdbc.update("UPDATE conversations SET topic = ? WHERE id = ?", "Dodam  story", other);
        UUID withSingleSpace = readyConversation(owned.childId(), date, 15);
        jdbc.update("UPDATE conversations SET title = ? WHERE id = ?", "Dodam story", withSingleSpace);

        mvc.perform(get("/api/v1/children/{childId}/conversations", owned.childId())
                        .cookie(owned.cookie()).queryParam("q", "  dodam  STORY  ").queryParam("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(5))
                .andExpect(jsonPath("$.data.items[0].conversationId").value(childTextOnly.toString()))
                .andExpect(jsonPath("$.data.items[1].conversationId").value(withTurns.toString()))
                .andExpect(jsonPath("$.data.items[2].conversationId").value(withSummary.toString()))
                .andExpect(jsonPath("$.data.items[3].conversationId").value(replyTextOnly.toString()))
                .andExpect(jsonPath("$.data.items[4].conversationId").value(redactedTextOnly.toString()))
                .andExpect(jsonPath("$.data.hasNext").value(false));
    }

    @Test
    void searchTreatsWildcardsAndEscapeCharacterAsLiteral() throws Exception {
        Fixture owned = fixture(true);
        LocalDate date = LocalDate.of(2026, 10, 2);
        UUID expected = readyConversation(owned.childId(), date, 10);
        UUID wildcard = readyConversation(owned.childId(), date, 11);
        jdbc.update("UPDATE conversations SET title = ? WHERE id = ?", "%,_! is literal", expected);
        jdbc.update("UPDATE conversations SET title = ? WHERE id = ?", "anything,x! is not a match", wildcard);

        mvc.perform(get("/api/v1/children/{childId}/conversations", owned.childId())
                        .cookie(owned.cookie()).queryParam("q", "%,_!"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].conversationId").value(expected.toString()));
    }

    @Test
    void searchTrimsUnicodeSpacesBeforeCheckingLengthOrBlank() throws Exception {
        Fixture owned = fixture(true);
        UUID expected = readyConversation(owned.childId(), LocalDate.of(2026, 10, 2), 10);
        String title = "😀".repeat(100);
        jdbc.update("UPDATE conversations SET title = ? WHERE id = ?", title, expected);

        mvc.perform(get("/api/v1/children/{childId}/conversations", owned.childId())
                        .cookie(owned.cookie()).queryParam("q", "\u00A0 " + title + "\u202F\u00A0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].conversationId").value(expected.toString()));
        mvc.perform(get("/api/v1/children/{childId}/conversations", owned.childId())
                        .cookie(owned.cookie()).queryParam("q", "😀" + title))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
        mvc.perform(get("/api/v1/children/{childId}/conversations", owned.childId())
                        .cookie(owned.cookie()).queryParam("q", " \u00A0\u202F "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].conversationId").value(expected.toString()));
        mvc.perform(list(owned).param("q", "a\0b"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isEmpty());
    }

    private UUID readyConversation(UUID childId, LocalDate date, int startedHour) {
        UUID id = conversation(childId, UUID.randomUUID(), date, startedHour, "ENDED", "PENDING");
        jdbc.update("""
                UPDATE conversations SET summary_status = 'READY', title = '제목', topic = '주제', summary = '요약',
                    summary_completed_at = summary_started_at + interval '1 second' WHERE id = ?
                """, id);
        return id;
    }

    private void insertTurn(UUID conversationId, int sequence, String childText, String childVisibility,
                            String replyText, String replyVisibility) {
        jdbc.update("""
                INSERT INTO conversation_turns (id, conversation_id, client_request_id, request_hash,
                    sequence, status, child_text, child_text_visibility, reply_text, reply_text_visibility,
                    created_at, processing_deadline_at, completed_at)
                SELECT ?, c.id, ?, repeat('a', 64), ?, 'SUCCEEDED', ?, ?, ?, ?,
                    c.started_at, c.started_at + interval '1 minute', c.started_at + interval '30 seconds'
                FROM conversations c WHERE c.id = ?
                """, UUID.randomUUID(), UUID.randomUUID(), sequence,
                childText, childVisibility, replyText, replyVisibility, conversationId);
    }


    private MockHttpServletRequestBuilder list(Fixture fixture) {
        return get(PATH.formatted(fixture.childId())).cookie(fixture.cookie());
    }

    private ResultActions assertError(MockHttpServletRequestBuilder request, int code, String error) throws Exception {
        return mvc.perform(request).andExpect(status().is(code)).andExpect(jsonPath("$.error.code").value(error))
                .andExpect(jsonPath("$.data").doesNotHaveJsonPath());
    }

    private Fixture fixture(boolean unlocked) {
        var accountId = UUID.randomUUID();
        var childId = UUID.randomUUID();
        var contextId = UUID.randomUUID();
        jdbc.update("INSERT INTO accounts(id,email) VALUES (?,?)", accountId, accountId + "@example.invalid");
        jdbc.update("""
                INSERT INTO children(id,account_id,name,nickname,birth_date,gender,character_id)
                VALUES (?,?,'가나','별이','2020-05-12','FEMALE','test-character')
                """, childId, accountId);
        jdbc.update("INSERT INTO session_security(id,account_id,session_version) VALUES (?,?,0)", contextId, accountId);
        if (unlocked) {
            jdbc.update("INSERT INTO guardian_pins(account_id,pin_hash) VALUES (?,'synthetic-hash')", accountId);
            jdbc.update("""
                    UPDATE session_security SET guardian_unlocked_until=clock_timestamp()+interval '15 minutes',
                        guardian_pin_version=1 WHERE id=?
                    """, contextId);
        }
        Session session = sessions.createSession();
        session.setMaxInactiveInterval(Duration.ofSeconds(-1));
        var security = SecurityContextHolder.createEmptyContext();
        security.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(accountId.toString(), null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))));
        session.setAttribute("accountId", accountId);
        session.setAttribute("securityContextId", contextId);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, security);
        var response = new MockHttpServletResponse();
        cookies.writeCookieValue(new CookieSerializer.CookieValue(new MockHttpServletRequest(), response, session.getId()));
        return new Fixture(accountId, childId, contextId, MockCookie.parse(response.getHeader("Set-Cookie")));
    }

    private UUID conversation(UUID childId, UUID id, LocalDate serviceDate, int startedHour, String state, String summaryStatus) {
        var scheduled = serviceDate.plusDays(1).atStartOfDay().atOffset(KST);
        var start = serviceDate.atTime(startedHour, 0).atOffset(KST);
        var requested = "ACTIVE".equals(state) ? null : scheduled;
        var ended = "ENDED".equals(state) ? scheduled.plusSeconds(1) : null;
        boolean pending = "PENDING".equals(summaryStatus);
        jdbc.update("""
                INSERT INTO conversations(id,child_id,client_request_id,service_date,scheduled_end_at,started_at,status,
                    end_requested_at,end_reason,ended_at,summary_status,summary_started_at,summary_deadline_at)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)
                """, id, childId, UUID.randomUUID(), serviceDate, scheduled, start, state, requested,
                requested == null ? null : "MIDNIGHT", ended, summaryStatus,
                pending ? ended : null, pending ? ended.plusMinutes(1) : null);
        return id;
    }

    private record Fixture(UUID accountId, UUID childId, UUID contextId, Cookie cookie) { }
}
