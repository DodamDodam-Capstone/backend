package com.dodamdodam.backend.auth.infrastructure;

import com.dodamdodam.backend.auth.session.AuthStateUnavailableException;
import com.dodamdodam.backend.guardian.infrastructure.GuardianAccessRepository.GuardianAccessState;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.core.convert.ConversionException;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.TransactionException;

@Repository
public class CurrentAccountRepository {

    private final JdbcTemplate jdbc;

    /** 기존 JDBC 연결로 현재 계정의 읽기 전용 상태를 조회한다. */
    public CurrentAccountRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** 동일 계정·문맥의 아이·PIN·권한을 한 SQL 스냅샷으로 읽고 원문 해시는 조회하지 않는다. */
    public Optional<AccountState> findState(UUID accountId, UUID securityContextId) {
        try {
            // LEFT JOIN으로 아이/PIN 미설정을 정상 상태로 보존하며 문맥의 계정 결합은 필수다.
            var states = jdbc.query("""
                    SELECT a.id AS account_id, a.email, c.id AS child_id,
                           s.session_version AS context_version, a.session_version AS account_version,
                           s.revoked_at, s.expires_at, s.guardian_unlocked_until, s.guardian_pin_version,
                           p.pin_version AS current_pin_version, clock_timestamp() AS checked_at
                    FROM accounts a JOIN session_security s ON s.account_id = a.id
                    LEFT JOIN children c ON c.account_id = a.id
                    LEFT JOIN guardian_pins p ON p.account_id = a.id
                    WHERE a.id = ? AND s.id = ?
                    """, (result, row) -> new AccountState(
                    result.getObject("account_id", UUID.class), result.getString("email"),
                    result.getObject("child_id", UUID.class),
                    new GuardianAccessState(result.getLong("context_version"), result.getLong("account_version"),
                            result.getObject("revoked_at", OffsetDateTime.class),
                            result.getObject("expires_at", OffsetDateTime.class),
                            result.getObject("guardian_unlocked_until", OffsetDateTime.class),
                            result.getObject("guardian_pin_version", Long.class),
                            result.getObject("current_pin_version", Long.class),
                            result.getObject("checked_at", OffsetDateTime.class))), accountId, securityContextId);
            return states.stream().findFirst();
        }
        catch (DataAccessException | TransactionException | ConversionException failure) {
            // 조회 장애를 아이/PIN 없음이나 로그아웃으로 해석하지 않는다.
            throw new AuthStateUnavailableException(failure);
        }
    }

    /** 공통 권한 판정과 공개 프로필을 같은 조회 시점으로 서비스에 전달한다. */
    public record AccountState(UUID accountId, String email, UUID childId, GuardianAccessState access) { }
}
