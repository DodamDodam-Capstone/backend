package com.dodamdodam.backend.auth.infrastructure;

import com.dodamdodam.backend.auth.dto.IssueEmailVerificationRequest.Purpose;
import com.dodamdodam.backend.auth.session.AuthStateUnavailableException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
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
            String lockKey = pinPurpose
                    ? "pin:" + challenge.purpose().name() + ":" + challenge.setupAccountId() + ":" + challenge.setupSessionBinding()
                    : "public:" + challenge.purpose().name() + ":" + challenge.email();
            jdbc.queryForObject("SELECT pg_advisory_xact_lock(hashtextextended('email-issuance:' || ?, 0))",
                    (result, row) -> true, lockKey);

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

    /** 트랜잭션 시작 시각 대신 모든 잠금 대기가 끝난 현재 DB 시각을 읽는다. */
    private OffsetDateTime databaseNow() {
        return jdbc.queryForObject("SELECT clock_timestamp()", OffsetDateTime.class);
    }

    /** 서비스가 판정한 발급 값이다. 번호 원문 없이 기존 스키마의 목적별 결합 값만 받는다. */
    public record NewChallenge(UUID id, String email, Purpose purpose, boolean eligible, byte[] codeHash,
                               UUID setupAccountId, UUID setupSessionBinding, Long setupGeneration, Long pinVersionSnapshot,
                               UUID resetAccountId, Long resetSessionVersion) { }

    /** 응답·메일 처리에 필요한 공개 challenge 식별자와 번호 만료 시각이다. */
    public record IssuedChallenge(UUID challengeId, OffsetDateTime expiresAt) { }
}
