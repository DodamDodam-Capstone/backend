package com.dodamdodam.backend.conversation.infrastructure;

import com.dodamdodam.backend.auth.session.AuthStateUnavailableException;
import com.dodamdodam.backend.conversation.dto.ConversationView;
import java.time.LocalDate;
import java.util.List;
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

    private final JdbcTemplate jdbc;

    /** JDBC로 보호자에게 공개할 대화 열을 읽는다. */
    public GuardianConversationRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** 계정·아이·종료 상태와 KST 시작 날짜 범위를 함께 적용한다. */
    public List<ConversationView> listEnded(UUID accountId, UUID childId, LocalDate from, LocalDate to, long offset, int limit) {
        try {
            return jdbc.query("SELECT " + CONVERSATION_COLUMNS + """
                    FROM conversations c JOIN children ch ON ch.id = c.child_id
                    WHERE ch.account_id = ? AND c.child_id = ? AND c.status = 'ENDED'
                      AND (?::date IS NULL OR c.service_date >= ?::date)
                      AND (?::date IS NULL OR c.service_date <= ?::date)
                    ORDER BY c.started_at DESC, c.id DESC
                    LIMIT ? OFFSET ?
                    """, CONVERSATION_ROW, accountId, childId, from, from, to, to, limit, offset);
        }
        catch (DataAccessException | ConversionException failure) {
            throw new AuthStateUnavailableException(failure);
        }
    }
}
