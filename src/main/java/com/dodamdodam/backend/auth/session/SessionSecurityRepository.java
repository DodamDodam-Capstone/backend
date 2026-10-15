package com.dodamdodam.backend.auth.session;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class SessionSecurityRepository {

    private final JdbcTemplate jdbcTemplate;

    public SessionSecurityRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean isCurrentContext(UUID accountId, UUID securityContextId) {
        var states = jdbcTemplate.query("""
                SELECT s.session_version, a.session_version AS account_version,
                       s.revoked_at, s.expires_at, clock_timestamp() AS checked_at
                FROM session_security s JOIN accounts a ON a.id = s.account_id
                WHERE s.id = ? AND s.account_id = ?
                """, (result, row) -> new SessionValidity(
                result.getLong("session_version"), result.getLong("account_version"),
                result.getObject("revoked_at", OffsetDateTime.class),
                result.getObject("expires_at", OffsetDateTime.class),
                result.getObject("checked_at", OffsetDateTime.class)), securityContextId, accountId);
        return !states.isEmpty() && states.getFirst().isValid();
    }

    record SessionValidity(long sessionVersion, long accountVersion, OffsetDateTime revokedAt,
                           OffsetDateTime expiresAt, OffsetDateTime checkedAt) {
        boolean isValid() {
            return sessionVersion == accountVersion && revokedAt == null
                    && (expiresAt == null || checkedAt.isBefore(expiresAt));
        }
    }

    @Transactional
    public boolean revokeCurrentContext(UUID accountId, UUID securityContextId) {
        var accountVersions = jdbcTemplate.queryForList(
                "SELECT session_version FROM accounts WHERE id = ? FOR UPDATE", Long.class, accountId);
        if (accountVersions.isEmpty()) {
            return false;
        }
        var contexts = jdbcTemplate.queryForList("""
                SELECT id FROM session_security WHERE id = ? AND account_id = ? FOR UPDATE
                """, UUID.class, securityContextId, accountId);
        if (contexts.isEmpty()) {
            return false;
        }
        jdbcTemplate.queryForList("""
                SELECT id FROM email_verifications
                WHERE setup_account_id = ? AND setup_session_binding = ?
                  AND purpose IN ('PIN_SETUP', 'PIN_RESET')
                  AND token_consumed_at IS NULL AND invalidated_at IS NULL
                ORDER BY id FOR UPDATE
                """, UUID.class, accountId, securityContextId);

        // 잠금을 기다린 시간을 반영하도록 모든 잠금 뒤 DB 시각을 읽는다.
        var revokedAt = jdbcTemplate.queryForObject("SELECT clock_timestamp()",
                (result, row) -> result.getObject(1, OffsetDateTime.class));
        int updated = jdbcTemplate.update("""
                UPDATE session_security
                SET revoked_at = ?, guardian_unlocked_until = NULL, guardian_pin_version = NULL
                WHERE id = ? AND account_id = ? AND session_version = ? AND revoked_at IS NULL
                  AND (expires_at IS NULL OR expires_at > ?)
                """, revokedAt, securityContextId, accountId, accountVersions.getFirst(), revokedAt);
        if (updated == 0) {
            return false;
        }
        jdbcTemplate.update("""
                UPDATE email_verifications SET invalidated_at = ?
                WHERE setup_account_id = ? AND setup_session_binding = ?
                  AND purpose IN ('PIN_SETUP', 'PIN_RESET')
                  AND token_consumed_at IS NULL AND invalidated_at IS NULL
                """, revokedAt, accountId, securityContextId);
        return true;
    }
}
