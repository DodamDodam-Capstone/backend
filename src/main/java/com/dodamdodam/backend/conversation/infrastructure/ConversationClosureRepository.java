package com.dodamdodam.backend.conversation.infrastructure;

import com.dodamdodam.backend.conversation.exception.ConversationException;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(propagation = Propagation.MANDATORY)
public class ConversationClosureRepository {

    private final JdbcTemplate jdbc;

    /** 종료 전이·발화 만료·복구 링크를 호출자의 동일 업무 트랜잭션에 묶는다. */
    public ConversationClosureRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** 계정을 먼저 잠가 다른 문맥의 폐기·버전 변경과 복구 자격 확정이 엇갈리지 않게 한다. */
    public boolean lockAccount(UUID accountId) {
        try {
            return !jdbc.queryForList("SELECT id FROM accounts WHERE id = ? FOR UPDATE", UUID.class, accountId).isEmpty();
        }
        catch (DataAccessException failure) {
            throw new ConversationException(failure);
        }
    }

    /** 자녀 잠금 이후 부모-자식 조합을 확인하며 종료 대상 대화를 잠근다. */
    public Optional<ClosureState> findForUpdate(UUID childId, UUID conversationId) {
        try {
            return jdbc.query("""
                    SELECT id, status, scheduled_end_at, end_requested_at, end_reason, ended_at, summary_status
                    FROM conversations WHERE child_id = ? AND id = ? FOR UPDATE
                    """, (row, _) -> new ClosureState(row.getObject("id", UUID.class), row.getString("status"),
                    row.getObject("scheduled_end_at", OffsetDateTime.class), row.getObject("end_requested_at", OffsetDateTime.class),
                    row.getString("end_reason"), row.getObject("ended_at", OffsetDateTime.class), row.getString("summary_status")),
                    childId, conversationId).stream().findFirst();
        }
        catch (DataAccessException failure) {
            throw new ConversationException(failure);
        }
    }

    /** ACTIVE의 최초 경계만 접수하며 이미 저장된 MANUAL/MIDNIGHT는 덮어쓰지 않는다. */
    public void beginClosing(UUID conversationId, OffsetDateTime boundary, String reason) {
        try {
            requireTransition(jdbc.update("""
                    UPDATE conversations SET status = 'CLOSING', end_requested_at = ?, end_reason = ?
                    WHERE id = ? AND status = 'ACTIVE' AND end_requested_at IS NULL AND end_reason IS NULL
                    """, boundary, reason, conversationId));
        }
        catch (DataAccessException failure) {
            throw new ConversationException(failure);
        }
    }

    /** 진행 중 발화를 먼저 잠그며 완료 처리와 기다린 뒤의 시각 판정을 직렬화한다. */
    public void lockProcessingTurns(UUID conversationId) {
        try {
            jdbc.queryForList("""
                    SELECT id FROM conversation_turns WHERE conversation_id = ? AND status = 'PROCESSING' ORDER BY id FOR UPDATE
                    """, UUID.class, conversationId);
        }
        catch (DataAccessException failure) {
            throw new ConversationException(failure);
        }
    }

    /** 원래 기한에 도달한 PROCESSING만 실패시키고 terminal 결과·기한·텍스트는 변경하지 않는다. */
    public void expireProcessing(UUID conversationId, OffsetDateTime checkedAt) {
        try {
            jdbc.update("""
                    UPDATE conversation_turns SET status = 'FAILED', error_code = 'AI_TIMEOUT', completed_at = ?
                    WHERE conversation_id = ? AND status = 'PROCESSING' AND completed_at IS NULL AND processing_deadline_at <= ?
                    """, checkedAt, conversationId, checkedAt);
        }
        catch (DataAccessException failure) {
            throw new ConversationException(failure);
        }
    }

    /** 모든 발화 terminal 여부는 실제 DB의 남은 PROCESSING으로 판단한다. */
    public boolean hasProcessing(UUID conversationId) {
        try {
            return Boolean.TRUE.equals(jdbc.queryForObject("""
                    SELECT EXISTS (SELECT 1 FROM conversation_turns WHERE conversation_id = ? AND status = 'PROCESSING')
                    """, Boolean.class, conversationId));
        }
        catch (DataAccessException failure) {
            throw new ConversationException(failure);
        }
    }

    /** 실패 발화를 포함해 저장·표시가 허용된 nonnull 텍스트가 하나라도 있는지 확인한다. */
    public boolean hasSummaryInput(UUID conversationId) {
        try {
            return Boolean.TRUE.equals(jdbc.queryForObject("""
                    SELECT EXISTS (SELECT 1 FROM conversation_turns WHERE conversation_id = ?
                        AND status IN ('SUCCEEDED', 'FAILED')
                        AND ((child_text IS NOT NULL AND child_text_visibility IN ('VISIBLE', 'REDACTED'))
                          OR (reply_text IS NOT NULL AND reply_text_visibility IN ('VISIBLE', 'REDACTED'))))
                    """, Boolean.class, conversationId));
        }
        catch (DataAccessException failure) {
            throw new ConversationException(failure);
        }
    }

    /** 복구 대상 확정 전 연결들을 일정한 순서로 잠근다. */
    public void lockLinks(UUID conversationId) {
        try {
            jdbc.queryForList("""
                    SELECT session_security_id FROM conversation_session_links
                    WHERE conversation_id = ? ORDER BY session_security_id FOR UPDATE
                    """, UUID.class, conversationId);
        }
        catch (DataAccessException failure) {
            throw new ConversationException(failure);
        }
    }

    /** 최초 ENDED 전이에서만 실제 완료 시각과 EMPTY/PENDING의 필수 NULL 조합을 기록한다. */
    public void finishClosing(UUID conversationId, OffsetDateTime endedAt, OffsetDateTime summaryDeadline) {
        try {
            boolean pending = summaryDeadline != null;
            requireTransition(jdbc.update("""
                    UPDATE conversations SET status = 'ENDED', ended_at = ?, summary_status = ?,
                        summary_started_at = ?, summary_deadline_at = ?
                    WHERE id = ? AND status = 'CLOSING' AND ended_at IS NULL
                        AND NOT EXISTS (SELECT 1 FROM conversation_turns WHERE conversation_id = ? AND status = 'PROCESSING')
                    """, endedAt, pending ? "PENDING" : "EMPTY", pending ? endedAt : null, summaryDeadline,
                    conversationId, conversationId));
        }
        catch (DataAccessException failure) {
            throw new ConversationException(failure);
        }
    }

    /** 같은 소유 계정의 경계 이전 유효 연결에만 최초 endedAt+600초를 원자적으로 기록한다. */
    public void grantRecovery(UUID childId, UUID conversationId, OffsetDateTime boundary, OffsetDateTime endedAt) {
        try {
            jdbc.update("""
                    UPDATE conversation_session_links link SET end_recovery_until = ?
                    FROM session_security context JOIN accounts account ON account.id = context.account_id
                        JOIN children child ON child.account_id = account.id AND child.id = ?
                    WHERE link.conversation_id = ? AND link.session_security_id = context.id
                        AND link.bound_at < ? AND link.end_recovery_until IS NULL
                        AND context.session_version = account.session_version AND context.revoked_at IS NULL
                        AND (context.expires_at IS NULL OR (isfinite(context.expires_at) AND context.expires_at > ?))
                    """, endedAt.plusSeconds(600), childId, conversationId, boundary, endedAt);
        }
        catch (DataAccessException failure) {
            throw new ConversationException(failure);
        }
    }

    /** 잠근 행의 조건부 전이가 유실되면 성공으로 추정하지 않고 종료 전체를 롤백한다. */
    private void requireTransition(int count) {
        if (count != 1) {
            throw new ConversationException(new IllegalStateException("종료 상태 전이를 확정할 수 없습니다."));
        }
    }

    /** 종료 확인 API가 재사용할 저장 상태이며 아이 내용·발화는 포함하지 않는다. */
    public record ClosureState(UUID conversationId, String status, OffsetDateTime scheduledEndAt,
                               OffsetDateTime endRequestedAt, String endReason, OffsetDateTime endedAt, String summaryStatus) { }
}
