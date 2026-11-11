package com.dodamdodam.backend.conversation.infrastructure;

import com.dodamdodam.backend.TestcontainersConfiguration;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.function.BiConsumer;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 서비스 구현 없이 실제 PostgreSQL에서 공통 스키마의 허용·거부 경계를 검증한다. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = {"spring.session.jdbc.cleanup-cron=-"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Transactional
class ConversationSchemaTests {

    private static final LocalDate SERVICE_DATE = LocalDate.of(2026, 10, 3);
    private static final ZoneOffset KST = ZoneOffset.ofHours(9);
    private static final OffsetDateTime TURN_CREATED_AT = SERVICE_DATE.atTime(9, 0).atOffset(KST);
    @Autowired private JdbcTemplate jdbc;
    @Autowired private DataSource dataSource;

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void flywayMigratesEmptySchemaAndRepeatsWithoutChanges() throws SQLException {
        var originalSchema = jdbc.queryForObject("SELECT current_schema()", String.class);
        withIsolatedSchema((schema, isolated) -> {
            var migration = migration(schema);

            assertThat(migration.migrate().migrationsExecuted).isPositive();
            assertThat(isolated.queryForList("""
                    SELECT table_name FROM information_schema.tables
                    WHERE table_schema = ? AND table_name IN
                        ('conversations', 'conversation_turns', 'conversation_session_links')
                    """, String.class, schema)).containsExactlyInAnyOrder(
                    "conversations", "conversation_turns", "conversation_session_links");
            migration.validate();
            assertThat(migration.migrate().migrationsExecuted).isZero();
        });
        assertThat(jdbc.queryForObject("SELECT current_schema()", String.class)).isEqualTo(originalSchema);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM accounts", Integer.class)).isNotNull();
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void flywayUpgradesV5WithoutLosingExistingChildOrSession() throws SQLException {
        withIsolatedSchema((schema, isolated) -> {
            Flyway.configure().dataSource(dataSource).defaultSchema(schema).schemas(schema)
                    .target("5").load().migrate();
            var childId = child(isolated);
            var contextId = context(isolated, childId);

            var migration = migration(schema);
            assertThat(migration.migrate().migrationsExecuted).isPositive();
            migration.validate();
            var conversationId = conversation(isolated, childId, UUID.randomUUID(), "ACTIVE", SERVICE_DATE);
            isolated.update("""
                    INSERT INTO conversation_session_links (session_security_id, conversation_id, bound_at)
                    VALUES (?, ?, ?)
                    """, contextId, conversationId, TURN_CREATED_AT);

            assertThat(isolated.queryForObject("SELECT id FROM children WHERE id = ?", UUID.class, childId))
                    .isEqualTo(childId);
            assertThat(isolated.queryForObject("SELECT id FROM session_security WHERE id = ?", UUID.class, contextId))
                    .isEqualTo(contextId);
        });
    }

    @ParameterizedTest
    @CsvSource({"ACTIVE,ACTIVE", "ACTIVE,CLOSING", "CLOSING,ACTIVE", "CLOSING,CLOSING"})
    void rejectsAnyTwoUnendedConversationsEvenAcrossDates(String existing, String next) {
        var childId = child();
        conversation(jdbc, childId, UUID.randomUUID(), existing, SERVICE_DATE);

        assertViolation(() -> conversation(jdbc, childId, UUID.randomUUID(), next, SERVICE_DATE.plusDays(1)), "23505");
    }

    @Test
    void allowsSeveralEndedConversationsAndNewActiveOnSameDateWhileSummaryIsPending() {
        var childId = child();
        var first = conversation(childId);
        end(first);
        jdbc.update("""
                UPDATE conversations SET summary_status = 'PENDING', summary_started_at = ended_at,
                    summary_deadline_at = ended_at + INTERVAL '5 minutes' WHERE id = ?
                """, first);
        var second = conversation(childId);
        end(second);
        var third = conversation(childId);

        assertThat(jdbc.queryForObject("SELECT count(*) FROM conversations WHERE child_id = ? AND service_date = ?",
                Integer.class, childId, SERVICE_DATE)).isEqualTo(3);
        assertThat(jdbc.queryForObject("SELECT status FROM conversations WHERE id = ?", String.class, third))
                .isEqualTo("ACTIVE");
        assertThat(jdbc.queryForObject("SELECT summary_status FROM conversations WHERE id = ?", String.class, first))
                .isEqualTo("PENDING");
    }

    @Test
    void rejectsReuseOfEndedConversationRequestKey() {
        var childId = child();
        var key = UUID.randomUUID();
        end(conversation(jdbc, childId, key, "ACTIVE", SERVICE_DATE));

        assertViolation(() -> conversation(jdbc, childId, key, "ACTIVE", SERVICE_DATE.plusDays(1)), "23505");
    }

    @Test
    void scopesStartKeyAndUnendedLimitToEachChild() {
        var key = UUID.randomUUID();
        conversation(jdbc, child(), key, "ACTIVE", SERVICE_DATE);
        conversation(jdbc, child(), key, "CLOSING", SERVICE_DATE);
    }

    @ParameterizedTest
    @ValueSource(strings = {"2024-02-29", "2026-10-31", "2026-12-31"})
    void acceptsKstOpeningAndLastMicrosecondRegardlessOfDatabaseTimezone(String date) {
        jdbc.execute("SET LOCAL TIME ZONE 'America/New_York'");
        var serviceDate = LocalDate.parse(date);
        var id = conversation(jdbc, child(), UUID.randomUUID(), "ACTIVE", serviceDate);
        jdbc.update("UPDATE conversations SET started_at = scheduled_end_at - INTERVAL '1 microsecond' WHERE id = ?", id);

        var scheduledEnd = jdbc.queryForObject("SELECT scheduled_end_at FROM conversations WHERE id = ?",
                (row, index) -> row.getObject(1, OffsetDateTime.class).toInstant(), id);
        assertThat(scheduledEnd)
                .isEqualTo(serviceDate.plusDays(1).atStartOfDay().atOffset(KST).toInstant());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "id", "child_id", "client_request_id", "service_date", "scheduled_end_at",
            "status", "summary_status", "started_at"
    })
    void rejectsMissingRequiredConversationColumn(String column) {
        rejectUpdate("conversations", conversation(child()), column + " = NULL", "23502");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "status = 'UNKNOWN'", "summary_status = 'UNKNOWN'", "end_reason = 'UNKNOWN'",
            "status = 'CLOSING'", "status = 'ENDED'", "summary_status = 'EMPTY'",
            "end_reason = 'MANUAL'", "end_requested_at = started_at", "ended_at = started_at",
            "title = '허용 전 제목'", "topic = '허용 전 주제'", "summary = '허용 전 요약'",
            "summary_started_at = started_at", "summary_deadline_at = scheduled_end_at",
            "summary_completed_at = started_at", "summary_error_code = 'ERROR'",
            "service_date = 'infinity'::date", "service_date = '-infinity'::date",
            "service_date = service_date + 1", "scheduled_end_at = 'infinity'::timestamptz",
            "scheduled_end_at = '-infinity'::timestamptz",
            "scheduled_end_at = scheduled_end_at + INTERVAL '1 second'",
            "started_at = started_at - INTERVAL '1 microsecond'", "started_at = scheduled_end_at",
            "started_at = 'infinity'::timestamptz", "started_at = '-infinity'::timestamptz"
    })
    void rejectsInvalidConversationWindowOrActiveState(String assignment) {
        rejectUpdate("conversations", conversation(child()), assignment, "23514");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "end_requested_at = NULL", "end_reason = NULL", "ended_at = NULL",
            "end_requested_at = started_at - INTERVAL '1 microsecond'",
            "end_requested_at = scheduled_end_at", "end_reason = 'MIDNIGHT'",
            "ended_at = end_requested_at - INTERVAL '1 microsecond'",
            "end_requested_at = 'infinity'::timestamptz", "end_requested_at = '-infinity'::timestamptz",
            "ended_at = 'infinity'::timestamptz", "ended_at = '-infinity'::timestamptz"
    })
    void rejectsInvalidEndedBoundary(String assignment) {
        var id = conversation(child());
        end(id);
        rejectUpdate("conversations", id, assignment, "23514");
    }

    @Test
    void acceptsMidnightBoundaryAndActualEndAfterMidnight() {
        var id = conversation(child());
        jdbc.update("""
                UPDATE conversations SET status = 'CLOSING', end_reason = 'MIDNIGHT',
                    end_requested_at = scheduled_end_at WHERE id = ?
                """, id);
        jdbc.update("""
                UPDATE conversations SET status = 'ENDED', summary_status = 'EMPTY',
                    ended_at = scheduled_end_at + INTERVAL '30 seconds' WHERE id = ?
                """, id);
    }

    @ParameterizedTest
    @ValueSource(strings = {"EMPTY", "PENDING", "READY", "FAILED"})
    void acceptsEachEndedSummaryState(String state) {
        var id = conversation(child());
        end(id);
        if (!state.equals("EMPTY")) {
            pendingSummary(id);
        }
        if (state.equals("READY")) {
            jdbc.update("""
                    UPDATE conversations SET summary_status = 'READY',
                        summary_completed_at = summary_deadline_at - INTERVAL '1 microsecond',
                        title = '제목', topic = '주제', summary = '허용된 요약' WHERE id = ?
                    """, id);
        }
        if (state.equals("FAILED")) {
            jdbc.update("""
                    UPDATE conversations SET summary_status = 'FAILED', summary_completed_at = summary_deadline_at,
                        summary_error_code = 'AI_TIMEOUT' WHERE id = ?
                    """, id);
        }
        assertThat(jdbc.queryForObject("SELECT summary_status FROM conversations WHERE id = ?", String.class, id))
                .isEqualTo(state);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "summary_started_at = NULL", "summary_deadline_at = NULL",
            "summary_deadline_at = summary_started_at", "summary_deadline_at = summary_started_at - INTERVAL '1 second'",
            "summary_completed_at = summary_started_at", "summary_error_code = 'ERROR'",
            "title = '제목'", "topic = '주제'", "summary = '요약'",
            "summary_started_at = '-infinity'::timestamptz", "summary_deadline_at = 'infinity'::timestamptz",
            "summary_status = 'READY', summary_completed_at = summary_started_at, title = '제목', topic = '주제'",
            "summary_status = 'FAILED', summary_completed_at = summary_started_at",
            "summary_status = 'FAILED', summary_completed_at = summary_started_at, summary_error_code = ''"
    })
    void rejectsInvalidPendingOrTerminalSummary(String assignment) {
        var id = conversation(child());
        end(id);
        pendingSummary(id);
        rejectUpdate("conversations", id, assignment, "23514");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "summary_completed_at = NULL", "summary_completed_at = summary_deadline_at",
            "summary_completed_at = summary_deadline_at + INTERVAL '1 microsecond'",
            "summary_completed_at = summary_started_at - INTERVAL '1 microsecond'",
            "summary_completed_at = 'infinity'::timestamptz", "summary_completed_at = '-infinity'::timestamptz",
            "title = NULL", "topic = NULL", "summary = NULL", "summary_error_code = 'ERROR'"
    })
    void rejectsReadySummaryWithMissingFieldsOrInvalidCompletionTime(String assignment) {
        var id = conversation(child());
        end(id);
        pendingSummary(id);
        jdbc.update("""
                UPDATE conversations SET summary_status = 'READY', summary_completed_at = summary_started_at,
                    title = '제목', topic = '주제', summary = '허용된 요약' WHERE id = ?
                """, id);
        rejectUpdate("conversations", id, assignment, "23514");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "id", "conversation_id", "client_request_id", "request_hash", "sequence", "status",
            "child_text_visibility", "reply_text_visibility", "created_at", "processing_deadline_at"
    })
    void rejectsMissingRequiredTurnColumn(String column) {
        rejectUpdate("conversation_turns", processing(conversation(child()), 1, UUID.randomUUID()), column + " = NULL", "23502");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "sequence = 0", "sequence = -1", "request_hash = ''", "request_hash = repeat('a', 63)",
            "request_hash = repeat('a', 65)", "request_hash = repeat('A', 64)", "request_hash = repeat('g', 64)",
            "request_hash = repeat('a', 64) || chr(10)", "status = 'UNKNOWN'",
            "child_text_visibility = 'UNKNOWN'", "reply_text_visibility = 'UNKNOWN'",
            "child_text = '금지 원문'", "reply_text = '금지 원문'",
            "child_text_visibility = 'VISIBLE'", "reply_text_visibility = 'REDACTED'",
            "child_text = '허용문', child_text_visibility = 'VISIBLE'",
            "reply_text = '가공문', reply_text_visibility = 'REDACTED'",
            "completed_at = created_at", "error_code = 'ERROR'", "status = 'SUCCEEDED'", "status = 'FAILED'",
            "processing_deadline_at = created_at", "processing_deadline_at = created_at - INTERVAL '1 second'",
            "created_at = '-infinity'::timestamptz", "created_at = 'infinity'::timestamptz",
            "processing_deadline_at = 'infinity'::timestamptz", "processing_deadline_at = '-infinity'::timestamptz",
            "status = 'SUCCEEDED', completed_at = processing_deadline_at",
            "status = 'SUCCEEDED', completed_at = processing_deadline_at + INTERVAL '1 microsecond'",
            "status = 'SUCCEEDED', completed_at = created_at - INTERVAL '1 microsecond'",
            "status = 'SUCCEEDED', completed_at = created_at, error_code = 'ERROR'",
            "status = 'FAILED', completed_at = processing_deadline_at, error_code = ''",
            "status = 'FAILED', completed_at = '-infinity'::timestamptz, error_code = 'ERROR'",
            "status = 'FAILED', completed_at = 'infinity'::timestamptz, error_code = 'ERROR'"
    })
    void rejectsInvalidTurnStateOrDeadline(String assignment) {
        rejectUpdate("conversation_turns", processing(conversation(child()), 1, UUID.randomUUID()), assignment, "23514");
    }

    @ParameterizedTest
    @CsvSource({
            "OMITTED,OMITTED", "VISIBLE,OMITTED", "REDACTED,OMITTED", "OMITTED,VISIBLE", "OMITTED,REDACTED",
            "VISIBLE,VISIBLE", "VISIBLE,REDACTED", "REDACTED,VISIBLE", "REDACTED,REDACTED"
    })
    void acceptsIndependentTextVisibilityForSuccessfulTurn(String childVisibility, String replyVisibility) {
        var id = processing(conversation(child()), 1, UUID.randomUUID());
        jdbc.update("""
                UPDATE conversation_turns SET status = 'SUCCEEDED',
                    completed_at = processing_deadline_at - INTERVAL '1 microsecond',
                    child_text = ?, child_text_visibility = ?, reply_text = ?, reply_text_visibility = ? WHERE id = ?
                """, childVisibility.equals("OMITTED") ? null : "허용된 아이 말", childVisibility,
                replyVisibility.equals("OMITTED") ? null : "허용된 답변", replyVisibility, id);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "child_text = '금지 원문'", "reply_text = '금지 원문'",
            "child_text_visibility = 'VISIBLE'", "child_text_visibility = 'REDACTED'",
            "reply_text_visibility = 'VISIBLE'", "reply_text_visibility = 'REDACTED'",
            "child_text_visibility = 'UNKNOWN'", "reply_text_visibility = 'UNKNOWN'"
    })
    void rejectsInvalidVisibilityOnTerminalTurnIndependentlyOfProcessingRules(String assignment) {
        var id = processing(conversation(child()), 1, UUID.randomUUID());
        succeed(id);
        rejectUpdate("conversation_turns", id, assignment, "23514");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    void acceptsFailedTurnAtOrAfterDeadlineWithIndividuallyAllowedText(int secondsAfterDeadline) {
        var id = processing(conversation(child()), 1, UUID.randomUUID());
        jdbc.update("""
                UPDATE conversation_turns SET status = 'FAILED',
                    completed_at = processing_deadline_at + ? * INTERVAL '1 second', error_code = 'AI_TIMEOUT',
                    child_text = '허용된 아이 말', child_text_visibility = 'REDACTED' WHERE id = ?
                """, secondsAfterDeadline, id);
    }

    @Test
    void rejectsSecondProcessingTurnInSameConversation() {
        var conversationId = conversation(child());
        processing(conversationId, 1, UUID.randomUUID());
        assertViolation(() -> processing(conversationId, 2, UUID.randomUUID()), "23505");
    }

    @ParameterizedTest
    @ValueSource(strings = {"REQUEST", "SEQUENCE"})
    void preservesTurnRequestAndSequenceUniquenessAfterCompletion(String duplicate) {
        var conversationId = conversation(child());
        var key = UUID.randomUUID();
        succeed(processing(conversationId, 1, key));

        assertViolation(() -> processing(conversationId, duplicate.equals("SEQUENCE") ? 1 : 2,
                duplicate.equals("REQUEST") ? key : UUID.randomUUID()), "23505");
    }

    @Test
    void allowsTerminalRowsAndOneProcessingPerConversationAndScopesTurnKeys() {
        var first = conversation(child());
        var key = UUID.randomUUID();
        succeed(processing(first, 1, key));
        succeed(processing(first, 2, UUID.randomUUID()));
        processing(first, 3, UUID.randomUUID());
        processing(conversation(child()), 1, key);
    }

    @Test
    void linksMultipleContextsAndConversationsWithoutSingleRecoveryWinner() {
        var childId = child();
        var first = conversation(childId);
        end(first);
        var second = conversation(childId);
        var firstContext = context(jdbc, childId);
        var secondContext = context(jdbc, childId);
        link(firstContext, first);
        link(secondContext, first);
        link(firstContext, second);
        jdbc.update("""
                UPDATE conversation_session_links SET end_recovery_until =
                    (SELECT ended_at + INTERVAL '600 seconds' FROM conversations WHERE id = ?)
                WHERE conversation_id = ?
                """, first, first);

        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM conversation_session_links
                WHERE conversation_id = ? AND end_recovery_until IS NOT NULL
                """, Integer.class, first)).isEqualTo(2);
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM conversation_session_links
                WHERE session_security_id = ?
                """, Integer.class, firstContext)).isEqualTo(2);
    }

    @Test
    void rejectsDuplicateSessionConversationPair() {
        var childId = child();
        var conversationId = conversation(childId);
        var contextId = context(jdbc, childId);
        link(contextId, conversationId);
        assertViolation(() -> link(contextId, conversationId), "23505");
    }

    @ParameterizedTest
    @CsvSource(value = {
            "session_security_id = NULL|23502", "conversation_id = NULL|23502", "bound_at = NULL|23502",
            "bound_at = 'infinity'::timestamptz|23514", "bound_at = '-infinity'::timestamptz|23514",
            "end_recovery_until = 'infinity'::timestamptz|23514", "end_recovery_until = '-infinity'::timestamptz|23514"
    }, delimiter = '|')
    void rejectsMissingLinkKeysOrNonFiniteTimestamps(String assignment, String sqlState) {
        var childId = child();
        var id = conversation(childId);
        link(context(jdbc, childId), id);
        assertViolation(() -> jdbc.update("UPDATE conversation_session_links SET " + assignment + " WHERE conversation_id = ?", id), sqlState);
    }

    @ParameterizedTest
    @ValueSource(strings = {"CONVERSATION_CHILD", "TURN_CONVERSATION", "LINK_CONTEXT", "LINK_CONVERSATION"})
    void rejectsMissingForeignKeyTarget(String relationship) {
        var childId = child();
        var id = conversation(childId);
        switch (relationship) {
            case "CONVERSATION_CHILD" -> assertViolation(() -> conversation(UUID.randomUUID()), "23503");
            case "TURN_CONVERSATION" -> assertViolation(() -> processing(UUID.randomUUID(), 1, UUID.randomUUID()), "23503");
            case "LINK_CONTEXT" -> assertViolation(() -> link(UUID.randomUUID(), id), "23503");
            case "LINK_CONVERSATION" -> assertViolation(() -> link(context(jdbc, childId), UUID.randomUUID()), "23503");
            default -> throw new AssertionError(relationship);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"CHILD", "CONVERSATION_WITH_TURN", "CONVERSATION_WITH_LINK", "CONTEXT"})
    void restrictsDeletionOfReferencedRows(String target) {
        var childId = child();
        var id = conversation(childId);
        var contextId = context(jdbc, childId);
        // 참조 대상 누락(23503)과 RESTRICT 삭제 거부(23001)를 구분한다.
        switch (target) {
            case "CHILD" -> assertViolation(() -> jdbc.update("DELETE FROM children WHERE id = ?", childId), "23001");
            case "CONVERSATION_WITH_TURN" -> {
                processing(id, 1, UUID.randomUUID());
                assertViolation(() -> jdbc.update("DELETE FROM conversations WHERE id = ?", id), "23001");
            }
            case "CONVERSATION_WITH_LINK" -> {
                link(contextId, id);
                assertViolation(() -> jdbc.update("DELETE FROM conversations WHERE id = ?", id), "23001");
            }
            case "CONTEXT" -> {
                link(contextId, id);
                assertViolation(() -> jdbc.update("DELETE FROM session_security WHERE id = ?", contextId), "23001");
            }
            default -> throw new AssertionError(target);
        }
    }

    private UUID child() {
        return child(jdbc);
    }

    private UUID child(JdbcTemplate target) {
        var accountId = UUID.randomUUID();
        var childId = UUID.randomUUID();
        target.update("INSERT INTO accounts (id, email) VALUES (?, ?)", accountId, accountId + "@example.invalid");
        target.update("""
                INSERT INTO children (id, account_id, name, nickname, birth_date, gender, character_id)
                VALUES (?, ?, '가나', '별이', DATE '2020-05-12', 'FEMALE', 'test-character')
                """, childId, accountId);
        return childId;
    }

    private UUID conversation(UUID childId) {
        return conversation(jdbc, childId, UUID.randomUUID(), "ACTIVE", SERVICE_DATE);
    }

    private UUID conversation(JdbcTemplate target, UUID childId, UUID key, String status, LocalDate date) {
        var id = UUID.randomUUID();
        target.update("""
                INSERT INTO conversations (id, child_id, client_request_id, service_date, scheduled_end_at,
                    status, summary_status, started_at, end_requested_at, end_reason)
                VALUES (?, ?, ?, ?, ?, ?, 'NOT_STARTED', ?, ?, ?)
                """, id, childId, key, date, date.plusDays(1).atStartOfDay().atOffset(KST), status,
                date.atTime(8, 0).atOffset(KST), status.equals("CLOSING") ? date.atTime(10, 0).atOffset(KST) : null,
                status.equals("CLOSING") ? "MANUAL" : null);
        return id;
    }

    private void end(UUID id) {
        jdbc.update("""
                UPDATE conversations SET status = 'ENDED', end_reason = 'MANUAL', summary_status = 'EMPTY',
                    end_requested_at = started_at + INTERVAL '2 hours',
                    ended_at = started_at + INTERVAL '2 hours' WHERE id = ?
                """, id);
    }

    private void pendingSummary(UUID id) {
        // 합성 기한은 DB 제약 시험용이며 운영 처리시간을 정하지 않는다.
        jdbc.update("""
                UPDATE conversations SET summary_status = 'PENDING', summary_started_at = ended_at,
                    summary_deadline_at = ended_at + INTERVAL '5 minutes' WHERE id = ?
                """, id);
    }

    private UUID processing(UUID conversationId, int sequence, UUID key) {
        var id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO conversation_turns (id, conversation_id, client_request_id, request_hash, sequence,
                    status, child_text_visibility, reply_text_visibility, created_at, processing_deadline_at)
                VALUES (?, ?, ?, ?, ?, 'PROCESSING', 'OMITTED', 'OMITTED', ?, ?)
                """, id, conversationId, key, "a".repeat(64), sequence, TURN_CREATED_AT, TURN_CREATED_AT.plusMinutes(1));
        return id;
    }

    private void succeed(UUID id) {
        jdbc.update("""
                UPDATE conversation_turns SET status = 'SUCCEEDED', completed_at = created_at + INTERVAL '30 seconds'
                WHERE id = ?
                """, id);
    }

    private UUID context(JdbcTemplate target, UUID childId) {
        var id = UUID.randomUUID();
        target.update("""
                INSERT INTO session_security (id, account_id, session_version)
                SELECT ?, account_id, 0 FROM children WHERE id = ?
                """, id, childId);
        return id;
    }

    private void link(UUID contextId, UUID conversationId) {
        jdbc.update("""
                INSERT INTO conversation_session_links (session_security_id, conversation_id, bound_at)
                VALUES (?, ?, ?)
                """, contextId, conversationId, TURN_CREATED_AT);
    }

    private void rejectUpdate(String table, UUID id, String assignment, String sqlState) {
        // 고정된 테스트 SQL만 사용하며, 제약 위반으로 중단된 트랜잭션은 테스트 종료 시 롤백한다.
        assertViolation(() -> jdbc.update("UPDATE " + table + " SET " + assignment + " WHERE id = ?", id), sqlState);
    }

    private void assertViolation(Runnable operation, String sqlState) {
        assertThatThrownBy(operation::run).isInstanceOfSatisfying(DataIntegrityViolationException.class,
                failure -> assertThat(failure.getMostSpecificCause()).isInstanceOfSatisfying(SQLException.class,
                        cause -> assertThat(cause.getSQLState()).isEqualTo(sqlState)));
    }

    private Flyway migration(String schema) {
        return Flyway.configure().dataSource(dataSource).defaultSchema(schema).schemas(schema).load();
    }

    private void withIsolatedSchema(BiConsumer<String, JdbcTemplate> operation) throws SQLException {
        // Testcontainers 안의 고유 스키마만 생성·정리하여 기존 데이터와 Flyway 이력을 건드리지 않는다.
        var schema = "conversation_migration_" + UUID.randomUUID().toString().replace("-", "");
        jdbc.execute("CREATE SCHEMA " + schema);
        try (Connection connection = dataSource.getConnection()) {
            var originalSchema = connection.getSchema();
            try {
                connection.setSchema(schema);
                operation.accept(schema, new JdbcTemplate(new SingleConnectionDataSource(connection, true)));
            }
            finally {
                // 풀에서 재사용될 연결에 삭제된 시험 스키마가 남지 않도록 명시적으로 복원한다.
                connection.setSchema(originalSchema);
            }
        }
        finally {
            jdbc.execute("DROP SCHEMA " + schema + " CASCADE");
        }
    }
}
