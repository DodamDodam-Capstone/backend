package com.dodamdodam.backend.conversation.infrastructure;

import com.dodamdodam.backend.auth.session.AuthStateUnavailableException;
import com.dodamdodam.backend.conversation.dto.ConversationView;
import com.dodamdodam.backend.conversation.dto.TurnView;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.core.convert.ConversionException;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class GuardianConversationRepository {

    private static final String CONVERSATION_COLUMNS = """
            c.id AS conversation_id, c.child_id, c.status, c.started_at, c.end_requested_at, c.end_reason,
            c.ended_at, c.title, c.topic, c.summary, c.summary_status
            """;
    private static final RowMapper<ConversationView> CONVERSATION_ROW = DataClassRowMapper.newInstance(ConversationView.class);
    private static final RowMapper<TurnView> TURN_ROW = DataClassRowMapper.newInstance(TurnView.class);

    private final JdbcTemplate jdbc;

    /** JDBC로 보호자에게 공개할 대화 열을 읽는다. */
    public GuardianConversationRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** LIKE 와일드카드와 escape 문자 자체를 먼저 이스케이프한 뒤 부분 일치로 감싼다. */
    private static String literalPattern(String keyword) {
        return "%" + keyword.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
    }

    /** 계정·아이·종료 상태를 제한하고 허용 텍스트의 리터럴 일치를 페이지 분할 전에 적용한다. */
    public List<ConversationView> listEnded(UUID accountId, UUID childId, LocalDate from, LocalDate to,
                                            String keyword, long offset, int limit) {
        if (keyword != null && keyword.indexOf('\0') >= 0) {
            // PostgreSQL text에 저장될 수 없는 NUL 문자는 부분 일치 대상도 없다.
            return List.of();
        }
        var pattern = keyword == null ? null : literalPattern(keyword);
        try {
            return jdbc.query("SELECT " + CONVERSATION_COLUMNS + """
                    FROM conversations c JOIN children ch ON ch.id = c.child_id
                    CROSS JOIN (VALUES (?::text)) AS search(pattern)
                    WHERE ch.account_id = ? AND c.child_id = ? AND c.status = 'ENDED'
                      AND (?::date IS NULL OR c.service_date >= ?::date)
                      AND (?::date IS NULL OR c.service_date <= ?::date)
                      AND (search.pattern IS NULL
                        OR c.title ILIKE search.pattern ESCAPE '!'
                        OR c.topic ILIKE search.pattern ESCAPE '!'
                        OR c.summary ILIKE search.pattern ESCAPE '!'
                        OR EXISTS (
                            SELECT 1 FROM conversation_turns t WHERE t.conversation_id = c.id AND (
                                (t.child_text_visibility IN ('VISIBLE', 'REDACTED')
                                  AND t.child_text ILIKE search.pattern ESCAPE '!')
                                OR (t.reply_text_visibility IN ('VISIBLE', 'REDACTED')
                                  AND t.reply_text ILIKE search.pattern ESCAPE '!')
                            )
                        ))
                    ORDER BY c.started_at DESC, c.id DESC
                    LIMIT ? OFFSET ?
                    """, CONVERSATION_ROW, pattern, accountId, childId, from, from, to, to, limit, offset);
        }
        catch (DataAccessException | ConversionException failure) {
            throw new AuthStateUnavailableException(failure);
        }
    }

    /** PIN 검사보다 먼저 소유 계정·자녀·대화 조합만 확인하며 대화 상태나 내용은 읽지 않는다. */
    public boolean hasConversation(UUID accountId, UUID childId, UUID conversationId) {
        try {
            return Boolean.TRUE.equals(jdbc.queryForObject("""
                    SELECT EXISTS (
                        SELECT 1 FROM conversations c JOIN children ch ON ch.id = c.child_id
                        WHERE ch.account_id = ? AND c.child_id = ? AND c.id = ?
                    )
                    """, Boolean.class, accountId, childId, conversationId));
        }
        catch (DataAccessException | ConversionException failure) {
            throw new AuthStateUnavailableException(failure);
        }
    }

    /** 소유 조합에 속한 ENDED 대화에서 공개 메타정보만 읽는다. */
    public Optional<ConversationView> findEnded(UUID accountId, UUID childId, UUID conversationId) {
        try {
            return jdbc.query("SELECT " + CONVERSATION_COLUMNS + """
                    FROM conversations c JOIN children ch ON ch.id = c.child_id
                    WHERE ch.account_id = ? AND c.child_id = ? AND c.id = ? AND c.status = 'ENDED'
                    """, CONVERSATION_ROW, accountId, childId, conversationId).stream().findFirst();
        }
        catch (DataAccessException | ConversionException failure) {
            throw new AuthStateUnavailableException(failure);
        }
    }

    /** 허용 텍스트와 가시성만 공개 발화 모델로 읽으며 원본 음성·요청 키·해시는 조회하지 않는다. */
    public List<TurnView> listEndedTurns(UUID accountId, UUID childId, UUID conversationId, int afterSequence, int limit) {
        try {
            return jdbc.query("""
                    SELECT t.id AS turn_id, t.sequence, t.status, t.child_text, t.child_text_visibility, t.reply_text,
                           t.reply_text_visibility, t.created_at, t.completed_at, t.error_code
                    FROM conversation_turns t JOIN conversations c ON c.id = t.conversation_id
                    JOIN children ch ON ch.id = c.child_id
                    WHERE ch.account_id = ? AND c.child_id = ? AND c.id = ? AND c.status = 'ENDED'
                      AND t.sequence > ?
                    ORDER BY t.sequence ASC
                    LIMIT ?
                    """, TURN_ROW, accountId, childId, conversationId, afterSequence, limit);
        }
        catch (DataAccessException | ConversionException failure) {
            throw new AuthStateUnavailableException(failure);
        }
    }
}
