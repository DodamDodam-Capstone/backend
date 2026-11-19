package com.dodamdodam.backend.auth.infrastructure;

import com.dodamdodam.backend.auth.dto.IssueEmailVerificationRequest.Purpose;
import com.dodamdodam.backend.auth.exception.AuthRateLimitException;
import java.time.Duration;
import com.dodamdodam.backend.auth.session.AuthStateUnavailableException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class EmailVerificationRepository {

    private final JdbcTemplate jdbc;

    /** 기존 email_verifications에 해시와 목적별 권한 snapshot을 저장한다. */
    public EmailVerificationRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** 호출자가 계정·문맥·PIN 검사와 필요한 잠금을 마친 Tx에서 이전 권한을 교체한다. */
    @Transactional(propagation = Propagation.MANDATORY)
    public IssuedChallenge replace(NewChallenge challenge) {
        try {
            boolean pinPurpose = challenge.purpose() == Purpose.PIN_SETUP || challenge.purpose() == Purpose.PIN_RESET;
            String predicate = pinPurpose ? "purpose = ? AND setup_account_id = ? AND setup_session_binding = ?"
                    : "purpose = ? AND email = ?";
            List<Object> scopeParameters = pinPurpose
                    ? List.of(challenge.purpose().name(), challenge.setupAccountId(), challenge.setupSessionBinding())
                    : List.of(challenge.purpose().name(), challenge.email());

            // 아직 row가 없는 최초 발급도 같은 범위에서 하나씩 진행한다.
            lockScope(challenge.email(), challenge.purpose(), challenge.setupAccountId(), challenge.setupSessionBinding());

            // 기존 번호와 검증 완료 토큰을 함께 잠그며 소비·기폐기 행은 보존한다.
            String unconsumedScope = predicate + " AND token_consumed_at IS NULL AND invalidated_at IS NULL";
            jdbc.queryForList("SELECT id FROM email_verifications WHERE " + unconsumedScope + " ORDER BY id FOR UPDATE",
                    UUID.class, scopeParameters.toArray());
            var issuedAt = databaseNow();
            var expiresAt = issuedAt.plusSeconds(600);
            var invalidationParameters = new ArrayList<>(scopeParameters);
            invalidationParameters.addFirst(issuedAt);
            jdbc.update("UPDATE email_verifications SET invalidated_at = ? WHERE " + unconsumedScope,
                    invalidationParameters.toArray());

            jdbc.update("""
                    INSERT INTO email_verifications
                        (id, email, purpose, eligible, code_hash, created_at, expires_at,
                         setup_account_id, setup_session_binding, setup_generation, pin_version_snapshot,
                         reset_account_id, reset_session_version)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, challenge.id(), challenge.email(), challenge.purpose().name(), challenge.eligible(),
                    challenge.codeHash(), issuedAt, expiresAt, challenge.setupAccountId(), challenge.setupSessionBinding(),
                    challenge.setupGeneration(), challenge.pinVersionSnapshot(), challenge.resetAccountId(), challenge.resetSessionVersion());
            return new IssuedChallenge(challenge.id(), expiresAt);
        }
        catch (DataAccessException failure) {
            throw new AuthStateUnavailableException(failure);
        }
    }

    /** 발송 실패 보상 등의 호출자 Tx에서 해당 발급만 폐기하며 재호출·소비 완료는 그대로 둔다. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void invalidateUnconsumed(UUID challengeId) {
        try {
            jdbc.queryForList("SELECT id FROM email_verifications WHERE id = ? FOR UPDATE", UUID.class, challengeId);
            var invalidatedAt = databaseNow();
            jdbc.update("""
                    UPDATE email_verifications SET invalidated_at = ?
                    WHERE id = ? AND token_consumed_at IS NULL AND invalidated_at IS NULL
                    """, invalidatedAt, challengeId);
        }
        catch (DataAccessException failure) {
            throw new AuthStateUnavailableException(failure);
        }
    }

    /** 최초 발급도 잠근 뒤 계정 판정과 실제 발급 간격을 검사할 수 있게 한다. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void lockScope(String email, Purpose purpose, UUID accountId, UUID contextId) {
        boolean pin = purpose == Purpose.PIN_SETUP || purpose == Purpose.PIN_RESET;
        String key = pin ? "pin:" + purpose.name() + ":" + accountId + ":" + contextId
                : "public:" + purpose.name() + ":" + email;
        jdbc.queryForObject("SELECT pg_advisory_xact_lock(hashtextextended('email-issuance:' || ?, 0))",
                (result, row) -> true, key);
    }

    /** scope와 상위 업무 row 잠금 뒤 최신 발급+60초를 재검사해 사전 예약 대기시간을 보완한다. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void requireResendReady(String email, Purpose purpose, UUID accountId, UUID contextId) {
        boolean pin = purpose == Purpose.PIN_SETUP || purpose == Purpose.PIN_RESET;
        String predicate = pin ? "purpose=? AND setup_account_id=? AND setup_session_binding=?" : "purpose=? AND email=?";
        Object[] arguments = pin ? new Object[]{purpose.name(), accountId, contextId} : new Object[]{purpose.name(), email};
        var times = jdbc.queryForList("SELECT created_at FROM email_verifications WHERE " + predicate
                + " ORDER BY created_at DESC LIMIT 1", OffsetDateTime.class, arguments);
        if (!times.isEmpty()) {
            var now = databaseNow();
            var until = times.getFirst().plusSeconds(60);
            if (now.isBefore(until)) {
                var remaining = Duration.between(now, until);
                throw new AuthRateLimitException(remaining.getSeconds() + (remaining.getNano() == 0 ? 0 : 1));
            }
        }
    }

    /** 응답용 최신 상태만 검사하며 eligible·번호 검증 권한을 대신하지 않는다. */
    public boolean isIssueResponseCurrent(UUID challengeId) {
        var states = jdbc.queryForList("""
                SELECT invalidated_at IS NULL AND token_consumed_at IS NULL AND expires_at > clock_timestamp()
                FROM email_verifications WHERE id=?
                """, Boolean.class, challengeId);
        return !states.isEmpty() && Boolean.TRUE.equals(states.getFirst());
    }

    /** 검증 예산과 상위 잠금 대상을 정할 때 challenge를 잠금 없이 읽는다. */
    public Optional<Challenge> findChallenge(UUID id) {
        return readChallenge(id, false);
    }

    /** 최초 PIN 설정에서 소비하거나 폐기할 행을 ID 순서로 잠근 뒤 기한을 판정한다. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void lockPinSetupProofs(UUID accountId, UUID presentedId) {
        jdbc.queryForList("""
                SELECT id FROM email_verifications
                WHERE setup_account_id = ? AND purpose IN ('PIN_SETUP', 'PIN_RESET')
                  AND (id = ? OR (token_consumed_at IS NULL AND invalidated_at IS NULL))
                ORDER BY id FOR UPDATE
                """, UUID.class, accountId, presentedId);
    }

    /** 같은 번호 검증·토큰 소비를 직렬화하며 PIN의 상위 잠금 뒤에 호출한다. */
    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<Challenge> lockChallenge(UUID id) {
        return readChallenge(id, true);
    }

    /** 목적과 발급 snapshot은 불변이며 갱신 가능한 검증 상태는 잠금 뒤 다시 읽는다. */
    private Optional<Challenge> readChallenge(UUID id, boolean lock) {
        return jdbc.query("""
                SELECT email, purpose, eligible, code_hash, created_at, expires_at, attempt_count,
                       verified_at, token_hash, token_expires_at, token_consumed_at, invalidated_at,
                       setup_account_id, setup_session_binding, setup_generation, pin_version_snapshot,
                       reset_account_id, reset_session_version
                FROM email_verifications WHERE id = ?
                """ + (lock ? " FOR UPDATE" : ""), (row, index) -> new Challenge(
            row.getString("email"), Purpose.valueOf(row.getString("purpose")), row.getBoolean("eligible"),
            row.getBytes("code_hash"), row.getObject("created_at", OffsetDateTime.class),
            row.getObject("expires_at", OffsetDateTime.class), row.getInt("attempt_count"),
            row.getObject("verified_at", OffsetDateTime.class), row.getBytes("token_hash"),
            row.getObject("token_expires_at", OffsetDateTime.class),
            row.getObject("token_consumed_at", OffsetDateTime.class),
            row.getObject("invalidated_at", OffsetDateTime.class), row.getObject("setup_account_id", UUID.class),
            row.getObject("setup_session_binding", UUID.class), row.getObject("setup_generation", Long.class),
            row.getObject("pin_version_snapshot", Long.class), row.getObject("reset_account_id", UUID.class),
            row.getObject("reset_session_version", Long.class)), id).stream().findFirst();
    }

    /** 잠근 행에만 오답과 한도 도달 시각을 기록한다. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void recordWrongCode(UUID id, int attemptCount, OffsetDateTime invalidatedAt) {
        jdbc.update("UPDATE email_verifications SET attempt_count=?, invalidated_at=? WHERE id=?",
            attemptCount, invalidatedAt, id);
    }

    /** 잠근 행에서 번호 사용 완료와 토큰 해시·기한을 한 번에 기록한다. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void storeGrant(UUID id, OffsetDateTime verifiedAt, byte[] hash, OffsetDateTime expiresAt) {
        jdbc.update("""
                UPDATE email_verifications SET verified_at=?, token_hash=?, token_expires_at=? WHERE id=?
                """, verifiedAt, hash, expiresAt, id);
    }

    /** 업무 트랜잭션이 잠근 유효 토큰의 소비 시각만 기록한다. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void markConsumed(UUID id, OffsetDateTime now) {
        jdbc.update("UPDATE email_verifications SET token_consumed_at=? WHERE id=?", now, id);
    }

    /** 번호 해시와 토큰 해시, 발급 당시 권한 결합을 함께 읽는다. */
    public record Challenge(String email, Purpose purpose, boolean eligible, byte[] codeHash, OffsetDateTime createdAt,
                            OffsetDateTime expiresAt, int attemptCount, OffsetDateTime verifiedAt, byte[] tokenHash,
                            OffsetDateTime tokenExpiresAt, OffsetDateTime tokenConsumedAt, OffsetDateTime invalidatedAt,
                            UUID setupAccountId, UUID setupSessionBinding, Long setupGeneration, Long pinVersionSnapshot,
                            UUID resetAccountId, Long resetSessionVersion) { }

    /** 트랜잭션 시작 시각 대신 모든 잠금 대기가 끝난 현재 DB 시각을 읽는다. */
    public OffsetDateTime databaseNow() {
        return jdbc.queryForObject("SELECT clock_timestamp()", OffsetDateTime.class);
    }

    /** 서비스가 판정한 발급 값이다. 번호 원문 없이 기존 스키마의 목적별 결합 값만 받는다. */
    public record NewChallenge(UUID id, String email, Purpose purpose, boolean eligible, byte[] codeHash,
                               UUID setupAccountId, UUID setupSessionBinding, Long setupGeneration, Long pinVersionSnapshot,
                               UUID resetAccountId, Long resetSessionVersion) { }

    /** 응답·메일 처리에 필요한 공개 challenge 식별자와 번호 만료 시각이다. */
    public record IssuedChallenge(UUID challengeId, OffsetDateTime expiresAt) { }
}
