package com.dodamdodam.backend.conversation.infrastructure;

import com.dodamdodam.backend.conversation.application.AllowedTextPolicy.AllowedText;
import com.dodamdodam.backend.conversation.domain.TextVisibility;
import com.dodamdodam.backend.conversation.domain.TurnStatus;
import com.dodamdodam.backend.conversation.dto.TurnView;
import com.dodamdodam.backend.conversation.exception.ConversationException;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(propagation = Propagation.MANDATORY)
public class ConversationTurnRepository {

    private static final String COLUMNS = """
            id, sequence, status, child_text, child_text_visibility, reply_text, reply_text_visibility,
            created_at, completed_at, error_code, request_hash, processing_deadline_at
            """;
    private static final RowMapper<StoredTurn> STORED_ROW = (row, _) -> new StoredTurn(
            new TurnView(row.getObject("id", UUID.class), row.getInt("sequence"), TurnStatus.valueOf(row.getString("status")),
                    row.getString("child_text"), TextVisibility.valueOf(row.getString("child_text_visibility")),
                    row.getString("reply_text"), TextVisibility.valueOf(row.getString("reply_text_visibility")),
                    row.getObject("created_at", OffsetDateTime.class), row.getObject("completed_at", OffsetDateTime.class),
                    row.getString("error_code")), row.getString("request_hash"),
            row.getObject("processing_deadline_at", OffsetDateTime.class));

    private final JdbcTemplate jdbc;

    /** V6 발화 테이블을 기존 업무 트랜잭션에서만 사용한다. */
    public ConversationTurnRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** 대화 잠금 뒤 같은 키의 저장 상태를 읽으며 terminal 발화도 재사용한다. */
    public Optional<StoredTurn> findRequestForUpdate(UUID conversationId, UUID clientRequestId) {
        try {
            return jdbc.query("SELECT " + COLUMNS
                    + " FROM conversation_turns WHERE conversation_id = ? AND client_request_id = ? FOR UPDATE",
                    STORED_ROW, conversationId, clientRequestId).stream().findFirst();
        }
        catch (DataAccessException failure) {
            throw new ConversationException(failure);
        }
    }

    /** 결과 문맥에 묶인 발화만 조회하며 대화 잠금 이후에 호출한다. */
    public Optional<StoredTurn> findForUpdate(UUID conversationId, UUID turnId) {
        try {
            return jdbc.query("SELECT " + COLUMNS + " FROM conversation_turns WHERE conversation_id = ? AND id = ? FOR UPDATE",
                    STORED_ROW, conversationId, turnId).stream().findFirst();
        }
        catch (DataAccessException failure) {
            throw new ConversationException(failure);
        }
    }

    /** 계정 잠금 뒤 원래 문맥을 먼저 잠근다. 만료·폐기·정리 여부는 기접수 결과의 저장 권한을 취소하지 않는다. */
    public void lockAcceptedContext(UUID accountId, UUID securityContextId) {
        try {
            jdbc.queryForList("SELECT id FROM session_security WHERE account_id = ? AND id = ? FOR UPDATE",
                    UUID.class, accountId, securityContextId);
        }
        catch (DataAccessException failure) {
            throw new ConversationException(failure);
        }
    }

    /** 허용 정책을 통과한 결과만 원래 기한 내 PROCESSING에 반영하며 terminal은 덮어쓰지 않는다. */
    public int completeProcessing(UUID conversationId, UUID turnId, TurnStatus status, String errorCode,
                                  AllowedText child, AllowedText reply, OffsetDateTime checkedAt) {
        try {
            return jdbc.update("""
                    UPDATE conversation_turns SET status = ?, completed_at = ?, error_code = ?,
                        child_text = ?, child_text_visibility = ?, reply_text = ?, reply_text_visibility = ?
                    WHERE conversation_id = ? AND id = ? AND status = 'PROCESSING' AND completed_at IS NULL
                        AND processing_deadline_at > ?
                    """, status.name(), checkedAt, errorCode, child.text(), child.visibility().name(), reply.text(), reply.visibility().name(),
                    conversationId, turnId, checkedAt);
        }
        catch (DataAccessException failure) {
            throw new ConversationException(failure);
        }
    }

    /** 종료 조정기와 같은 순서로 PROCESSING을 잠그며 새 키의 충돌 여부를 확인한다. */
    public boolean hasProcessingForUpdate(UUID conversationId) {
        try {
            return !jdbc.queryForList("""
                    SELECT id FROM conversation_turns WHERE conversation_id = ? AND status = 'PROCESSING'
                    ORDER BY id FOR UPDATE
                    """, UUID.class, conversationId).isEmpty();
        }
        catch (DataAccessException failure) {
            throw new ConversationException(failure);
        }
    }

    /** 호출자가 대화 행을 잠근 상태에서 실패 발화까지 포함한 마지막 순번 다음을 구한다. */
    public int nextSequence(UUID conversationId) {
        try {
            var sequence = jdbc.queryForObject("SELECT COALESCE(MAX(sequence), 0) + 1 FROM conversation_turns WHERE conversation_id = ?",
                    Integer.class, conversationId);
            // 집계 결과가 없으면 임의 순번으로 대체하지 않고 저장을 중단한다.
            if (sequence == null) {
                throw new ConversationException(new IllegalStateException("다음 발화 순번을 확인할 수 없습니다."));
            }
            return sequence;
        }
        catch (DataAccessException failure) {
            throw new ConversationException(failure);
        }
    }

    /** 잠근 대화에만 신규 PROCESSING을 저장하며 무관한 제약 위반을 업무 충돌로 바꾸지 않는다. */
    public StoredTurn insertProcessing(UUID id, UUID conversationId, UUID clientRequestId, String requestHash,
                                      int sequence, OffsetDateTime createdAt, OffsetDateTime processingDeadlineAt) {
        try {
            return jdbc.queryForObject("""
                    INSERT INTO conversation_turns (id, conversation_id, client_request_id, request_hash, sequence,
                        status, child_text_visibility, reply_text_visibility, created_at, processing_deadline_at)
                    VALUES (?, ?, ?, ?, ?, 'PROCESSING', 'OMITTED', 'OMITTED', ?, ?)
                    RETURNING
                    """ + COLUMNS, STORED_ROW, id, conversationId, clientRequestId, requestHash, sequence, createdAt, processingDeadlineAt);
        }
        catch (DataAccessException failure) {
            throw new ConversationException(failure);
        }
    }

    /** 해시·원래 기한은 내부 판정에만 사용하며 TurnView의 공개 필드와 분리한다. */
    public record StoredTurn(TurnView view, String requestHash, OffsetDateTime processingDeadlineAt) { }
}
