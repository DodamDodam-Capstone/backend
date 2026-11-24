package com.dodamdodam.backend.auth.infrastructure;

import java.util.Optional;
import java.util.UUID;
import java.time.OffsetDateTime;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.*;

@Repository
public class EmailVerificationAccountRepository {
    private final JdbcTemplate jdbc;

    /** 발급에 필요한 이메일·계정 버전만 읽으며 비밀번호·PIN hash는 조회하지 않는다. */
    public EmailVerificationAccountRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** 공개 발급 scope 잠금 뒤 현재 계정을 잠그고 적격 여부와 reset snapshot의 근거를 읽는다. */
    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<AccountState> findForUpdate(String email) {
        return jdbc.query("SELECT id, email, session_version FROM accounts WHERE email = ? FOR UPDATE",
                (result, row) -> new AccountState(result.getObject("id", UUID.class), result.getString("email"),
                        result.getLong("session_version")), email).stream().findFirst();
    }

    /** 공개 목적 적격 판정에 필요한 최소 계정 상태다. */
    public record AccountState(UUID id, String email, long sessionVersion) { }

    /** 계정 잠금 상태에서 비밀번호를 교체한다. 버전 증가로 이전 로그인과 RESET_PASSWORD 증명을 폐기한다. */
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean replacePassword(UUID accountId, String hash) {
        return jdbc.update("""
                UPDATE accounts SET password_hash = ?, session_version = session_version + 1, updated_at = clock_timestamp()
                WHERE id = ? AND password_hash IS NOT NULL
                """, hash, accountId) == 1;
    }

    /** 계정·세션 잠금을 얻은 호출자 Tx에서 현재 PIN 행을 잠그고 존재 여부를 반환한다. */
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean lockPin(UUID accountId) {
        return !jdbc.queryForList("SELECT account_id FROM guardian_pins WHERE account_id = ? FOR UPDATE",
                UUID.class, accountId).isEmpty();
    }

    /** PIN 원문 없이 현재 세션·이메일·generation·PIN 버전을 같은 DB 조회로 읽는다. */
    public Optional<PinState> findPinState(UUID accountId, UUID contextId) {
        return jdbc.query("""
                SELECT a.email, a.session_version AS account_version, s.session_version AS context_version,
                       s.setup_generation, s.revoked_at, s.expires_at, p.pin_version, clock_timestamp() AS checked_at
                FROM accounts a JOIN session_security s ON s.account_id=a.id
                LEFT JOIN guardian_pins p ON p.account_id=a.id WHERE a.id=? AND s.id=?
                """, (result, row) -> new PinState(result.getString("email"), result.getLong("account_version"),
                result.getLong("context_version"), result.getLong("setup_generation"),
                result.getObject("revoked_at", OffsetDateTime.class), result.getObject("expires_at", OffsetDateTime.class),
                result.getObject("pin_version", Long.class), result.getObject("checked_at", OffsetDateTime.class)),
                accountId, contextId).stream().findFirst();
    }

    public record PinState(String email, long accountVersion, long contextVersion, long setupGeneration,
                           OffsetDateTime revokedAt, OffsetDateTime expiresAt, Long pinVersion, OffsetDateTime checkedAt) {
        /** NULL expiry에서도 폐기·계정 버전 검사를 생략하지 않는다. */
        public boolean isSessionCurrent() {
            return accountVersion == contextVersion && revokedAt == null && (expiresAt == null || checkedAt.isBefore(expiresAt));
        }
    }
}
