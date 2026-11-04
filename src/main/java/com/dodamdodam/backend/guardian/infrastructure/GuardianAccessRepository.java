package com.dodamdodam.backend.guardian.infrastructure;

import com.dodamdodam.backend.guardian.exception.GuardianAccessException;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.core.convert.ConversionException;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.TransactionException;

@Repository
public class GuardianAccessRepository {

    private final JdbcTemplate jdbc;

    /** 계정·문맥·PIN 버전을 같은 DB 조회로 확인할 저장소를 구성한다. */
    public GuardianAccessRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** 프레임워크 인증 검사 뒤 사용할 현재 문맥·보호자 권한과 DB 시각을 조회한다. */
    public Optional<GuardianAccessState> findState(UUID accountId, UUID securityContextId) {
        try {
            // PIN 존재와 버전만 읽으며 PIN이 없어도 로그인 문맥은 반환한다.
            var states = jdbc.query("""
                    SELECT s.session_version AS context_version, a.session_version AS account_version,
                           s.revoked_at, s.expires_at, s.guardian_unlocked_until, s.guardian_pin_version,
                           p.pin_version AS current_pin_version, clock_timestamp() AS checked_at
                    FROM session_security s JOIN accounts a ON a.id = s.account_id
                    LEFT JOIN guardian_pins p ON p.account_id = a.id
                    WHERE s.id = ? AND s.account_id = ?
                    """, (result, row) -> new GuardianAccessState(
                    result.getLong("context_version"), result.getLong("account_version"),
                    result.getObject("revoked_at", OffsetDateTime.class),
                    result.getObject("expires_at", OffsetDateTime.class),
                    result.getObject("guardian_unlocked_until", OffsetDateTime.class),
                    result.getObject("guardian_pin_version", Long.class),
                    result.getObject("current_pin_version", Long.class),
                    result.getObject("checked_at", OffsetDateTime.class)), securityContextId, accountId);
            return states.stream().findFirst();
        }
        catch (DataAccessException | TransactionException | ConversionException failure) {
            // 확인 장애를 행 없음이나 PIN 미설정으로 해석해 권한을 우회하지 않는다.
            throw GuardianAccessException.unavailable(failure);
        }
    }

    public record GuardianAccessState(long contextSessionVersion, long accountSessionVersion,
                                      OffsetDateTime revokedAt, OffsetDateTime expiresAt,
                                      OffsetDateTime guardianUntil, Long guardianPinVersion,
                                      Long currentPinVersion, OffsetDateTime checkedAt) {

        /** DB의 세션 버전·폐기·만료 기준으로 문맥 유효성을 판정한다. */
        public boolean isSessionCurrent() {
            // NULL 만료는 허용하지만 계정 버전과 폐기 여부 검사는 항상 유지한다.
            return contextSessionVersion == accountSessionVersion && revokedAt == null
                    && (expiresAt == null || checkedAt.isBefore(expiresAt));
        }

        /** 같은 문맥의 미만료 보호자 확인과 현재 PIN 버전 일치를 함께 요구한다. */
        public boolean isGuardianCurrent() {
            return isSessionCurrent()
                    // 기한과 DB 시각이 같아지는 순간부터 보호자 확인을 거부한다.
                    && guardianUntil != null && checkedAt.isBefore(guardianUntil)
                    // 현재 PIN이 존재하고 확인 당시 버전과 같아야 한다.
                    && currentPinVersion != null && guardianPinVersion != null
                    && guardianPinVersion.equals(currentPinVersion);
        }
    }
}
