package com.dodamdodam.backend.conversation.infrastructure;

import com.dodamdodam.backend.conversation.domain.ConversationServiceHours.ServiceWindow;
import com.dodamdodam.backend.conversation.dto.StartConversationResult;
import com.dodamdodam.backend.conversation.exception.ConversationException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class ConversationRepository {

    private final JdbcTemplate jdbc;

    /** 기존 업무 DB 연결과 트랜잭션에 참여하는 JDBC 저장소다. */
    public ConversationRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** 트랜잭션 시작 시각이 아닌 호출 시점의 DB 시각을 읽는다. */
    public OffsetDateTime currentTime() {
        try {
            return jdbc.queryForObject("SELECT clock_timestamp()", (row, _) -> row.getObject(1, OffsetDateTime.class));
        }
        catch (DataAccessException failure) {
            throw new ConversationException(failure);
        }
    }

    /** 자녀 잠금 뒤 시작 키에 해당하는 대화 상태를 잠가 확인한다. */
    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<StoredConversation> findRequestForUpdate(UUID childId, UUID clientRequestId) {
        try {
            return jdbc.query("""
                    SELECT id, status FROM conversations WHERE child_id = ? AND client_request_id = ? FOR UPDATE
                    """, (row, _) -> new StoredConversation(row.getObject("id", UUID.class), row.getString("status")),
                    childId, clientRequestId).stream().findFirst();
        }
        catch (DataAccessException failure) {
            throw new ConversationException(failure);
        }
    }

    /** 같은 자녀의 미종료 행을 잠그며, 종료 조정기와의 상태 변경도 직렬화한다. */
    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<StoredConversation> findUnendedForUpdate(UUID childId) {
        try {
            return jdbc.query("""
                    SELECT id, status FROM conversations WHERE child_id = ? AND status IN ('ACTIVE','CLOSING') FOR UPDATE
                    """, (row, _) -> new StoredConversation(row.getObject("id", UUID.class), row.getString("status")),
                    childId).stream().findFirst();
        }
        catch (DataAccessException failure) {
            throw new ConversationException(failure);
        }
    }

    /** 미종료 부분 UNIQUE 충돌만 빈 결과로 구분하고, 다른 제약·저장 장애는 500 원인을 보존한다. */
    public Optional<StartConversationResult> insertActive(UUID id, UUID childId, UUID clientRequestId,
                                                         OffsetDateTime startedAt, ServiceWindow window) {
        try {
            return jdbc.query("""
                    INSERT INTO conversations (id, child_id, client_request_id, service_date, scheduled_end_at,
                        status, summary_status, started_at)
                    VALUES (?, ?, ?, ?, ?, 'ACTIVE', 'NOT_STARTED', ?)
                    ON CONFLICT (child_id) WHERE status IN ('ACTIVE','CLOSING') DO NOTHING
                    RETURNING id, status, started_at, service_date, scheduled_end_at
                    """, (row, _) -> new StartConversationResult(row.getObject("id", UUID.class), row.getString("status"),
                    row.getObject("started_at", OffsetDateTime.class), row.getObject("service_date", LocalDate.class),
                    row.getObject("scheduled_end_at", OffsetDateTime.class)),
                    id, childId, clientRequestId, window.serviceDate(), window.scheduledEndAt(), startedAt)
                    .stream().findFirst();
        }
        catch (DataAccessException failure) {
            throw new ConversationException(failure);
        }
    }

    /** 생성과 같은 트랜잭션에서 현재 보안 문맥의 최초 연결 시각을 저장한다. */
    public void insertLink(UUID securityContextId, UUID conversationId, OffsetDateTime boundAt) {
        try {
            jdbc.update("""
                    INSERT INTO conversation_session_links (session_security_id, conversation_id, bound_at)
                    VALUES (?, ?, ?)
                    """, securityContextId, conversationId, boundAt);
        }
        catch (DataAccessException failure) {
            throw new ConversationException(failure);
        }
    }

    /** 상태 판정 전용 내부 행이며 그대로 API에 노출하지 않는다. */
    public record StoredConversation(UUID conversationId, String status) { }
}
