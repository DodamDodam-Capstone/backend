package com.dodamdodam.backend.auth.infrastructure;

import com.dodamdodam.backend.auth.domain.AuthRateLimitKey;
import com.dodamdodam.backend.auth.domain.AuthRateLimitKey.Action;
import com.dodamdodam.backend.auth.session.AuthStateUnavailableException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.TreeMap;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class AuthRateLimitRepository {

    private final JdbcTemplate jdbc;

    /** 동일 DB의 인스턴스들이 요청 예산을 공유하는 저장소를 구성한다. */
    public AuthRateLimitRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** 호출자 Tx에서 한 단계의 모든 키를 예약하며 거부 시 쓰지 않고 재시도 초를 반환한다. */
    @Transactional(propagation = Propagation.MANDATORY)
    public long reserve(List<AuthRateLimitKey> keys) {
        if (keys == null || keys.isEmpty()) {
            throw new AuthStateUnavailableException();
        }
        var ordered = new TreeMap<String, AuthRateLimitKey>();
        for (var key : keys) {
            if (key == null) {
                throw new AuthStateUnavailableException();
            }
            ordered.put(key.action().name() + ":" + HexFormat.of().formatHex(key.keyHash()), key);
        }
        var states = new LinkedHashMap<AuthRateLimitKey, RateLimitState>();
        for (var entry : ordered.entrySet()) {
            // 최초 row 생성도 직렬화하고 모든 요청이 같은 순서로 잠금을 얻는다.
            jdbc.queryForObject("SELECT pg_advisory_xact_lock(hashtextextended('auth-rate:' || ?, 0))",
                (result, row) -> true, entry.getKey());
            var key = entry.getValue();
            states.put(key, findState(key, true));
        }

        // 모든 잠금 대기 뒤 한 DB 시각으로 판정해 여러 키의 경계를 일치시킨다.
        var checkedAt = jdbc.queryForObject("SELECT clock_timestamp()", OffsetDateTime.class);
        long retryAfter = 0;
        for (var entry : states.entrySet()) {
            if (entry.getValue() != null) {
                retryAfter = Math.max(retryAfter, entry.getValue().retryAfterSeconds(entry.getKey().action(), checkedAt));
            }
        }
        if (retryAfter > 0) {
            return retryAfter;
        }
        for (var entry : states.entrySet()) {
            reserveAllowed(entry.getKey(), entry.getValue(), checkedAt);
        }
        return 0;
    }

    /** 이미 예약한 예산은 건드리지 않고 해당 요청에 적용되는 최늦은 제한 해제 시간을 읽는다. */
    public long retryAfterSeconds(List<AuthRateLimitKey> keys) {
        var checkedAt = jdbc.queryForObject("SELECT clock_timestamp()", OffsetDateTime.class);
        long retryAfter = 0;
        for (var key : keys) {
            var state = findState(key, false);
            if (state != null) {
                retryAfter = Math.max(retryAfter, state.retryAfterSeconds(key.action(), checkedAt));
            }
        }
        return retryAfter;
    }

    /** 오답 행을 잠그고 차단 시각을 읽는다. 행이 없을 때는 상위 계정 잠금이 직렬화를 보장한다. */
    @Transactional(propagation = Propagation.MANDATORY)
    public OffsetDateTime lockPinFailures(byte[] keyHash) {
        var blocks = jdbc.query("""
                SELECT blocked_until FROM auth_rate_limits
                WHERE action = 'PIN_FAILURE_ACCOUNT' AND key_hash = ? FOR UPDATE
                """, (row, index) -> row.getObject(1, OffsetDateTime.class), keyHash);
        return blocks.isEmpty() ? null : blocks.getFirst();
    }

    /** 현재 차단을 연장하지 않고 응답에 적용할 남은 초를 계산한다. */
    public long pinBlockRetryAfterSeconds(byte[] keyHash) {
        var remaining = jdbc.query("""
                SELECT GREATEST(0, ceil(extract(epoch FROM blocked_until - clock_timestamp())))::bigint
                FROM auth_rate_limits WHERE action = 'PIN_FAILURE_ACCOUNT' AND key_hash = ?
                """, (row, index) -> row.getLong(1), keyHash);
        return remaining.isEmpty() ? 0 : remaining.getFirst();
    }

    /** 계정 잠금 아래 최근 900초 오답만 유지하고 다섯 번째 시도에 차단을 기록한다. */
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean recordPinFailure(byte[] keyHash, OffsetDateTime checkedAt) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                WITH attempt AS (SELECT ?::timestamptz AS at, ?::bytea AS key_hash),
                recent AS (
                    SELECT a.at, a.key_hash, ARRAY(
                        SELECT failed_at
                        FROM auth_rate_limits r CROSS JOIN LATERAL unnest(r.failure_times) AS failures(failed_at)
                        WHERE r.action = 'PIN_FAILURE_ACCOUNT' AND r.key_hash = a.key_hash
                            AND failed_at > a.at - interval '900 seconds' AND failed_at <= a.at
                        ORDER BY failed_at
                    ) || ARRAY[a.at] AS failures FROM attempt a
                )
                INSERT INTO auth_rate_limits
                    (action, key_hash, window_started_at, attempt_count, failure_times, blocked_until, expires_at)
                SELECT 'PIN_FAILURE_ACCOUNT', key_hash, failures[1], cardinality(failures), failures,
                    CASE WHEN cardinality(failures) = 5 THEN at + interval '900 seconds' END,
                    at + interval '900 seconds'
                FROM recent
                ON CONFLICT (action, key_hash) DO UPDATE SET
                    window_started_at = EXCLUDED.window_started_at,
                    attempt_count = EXCLUDED.attempt_count,
                    failure_times = EXCLUDED.failure_times,
                    blocked_until = EXCLUDED.blocked_until,
                    expires_at = EXCLUDED.expires_at
                RETURNING blocked_until IS NOT NULL
                """, Boolean.class, checkedAt, keyHash));
    }

    /** 예산을 예약하는 경로만 행을 잠그고, 429 응답 생성 중에는 상태를 조회한다. */
    private RateLimitState findState(AuthRateLimitKey key, boolean lock) {
        var states = jdbc.query("""
                SELECT window_started_at, attempt_count, blocked_until FROM auth_rate_limits
                WHERE action = ? AND key_hash = ?
                """ + (lock ? " FOR UPDATE" : ""), (result, row) -> new RateLimitState(
            result.getObject("window_started_at", OffsetDateTime.class), result.getLong("attempt_count"),
            result.getObject("blocked_until", OffsetDateTime.class)), key.action().name(), key.keyHash());
        return states.isEmpty() ? null : states.getFirst();
    }

    /** 모든 제한이 허용된 뒤에만 count를 쓰며 기존 창·차단을 임의로 연장하지 않는다. */
    private void reserveAllowed(AuthRateLimitKey key, RateLimitState state, OffsetDateTime checkedAt) {
        if (state == null) {
            jdbc.update("""
                    INSERT INTO auth_rate_limits (action, key_hash, window_started_at, attempt_count, expires_at)
                    VALUES (?, ?, ?, 1, ?)
                    """, key.action().name(), key.keyHash(), checkedAt, checkedAt.plusSeconds(key.action().windowSeconds()));
        }
        else if (state.windowFinished(key.action(), checkedAt)) {
            jdbc.update("""
                    UPDATE auth_rate_limits SET window_started_at = ?, attempt_count = 1,
                        blocked_until = NULL, expires_at = ? WHERE action = ? AND key_hash = ?
                    """, checkedAt, checkedAt.plusSeconds(key.action().windowSeconds()), key.action().name(), key.keyHash());
        }
        else {
            jdbc.update("UPDATE auth_rate_limits SET attempt_count = attempt_count + 1 WHERE action = ? AND key_hash = ?",
                    key.action().name(), key.keyHash());
        }
    }

    record RateLimitState(OffsetDateTime windowStartedAt, long attemptCount, OffsetDateTime blockedUntil) {

        /** 차단과 예산이 소진된 창 중 최늦은 재시도 시각까지 올림 초를 계산한다. */
        long retryAfterSeconds(Action action, OffsetDateTime checkedAt) {
            var windowEnd = windowStartedAt.plusSeconds(action.windowSeconds());
            OffsetDateTime retryAt = attemptCount >= action.capacity() && checkedAt.isBefore(windowEnd) ? windowEnd : null;
            if (blockedUntil != null && checkedAt.isBefore(blockedUntil)
                    && (retryAt == null || blockedUntil.isAfter(retryAt))) {
                retryAt = blockedUntil;
            }
            if (retryAt == null) {
                return 0;
            }
            var remaining = Duration.between(checkedAt, retryAt);
            return Math.max(1, remaining.getSeconds() + (remaining.getNano() == 0 ? 0 : 1));
        }

        /** 창과 차단 모두 equality부터 끝나며 그 전에는 기존 count를 유지한다. */
        boolean windowFinished(Action action, OffsetDateTime checkedAt) {
            return !checkedAt.isBefore(windowStartedAt.plusSeconds(action.windowSeconds()))
                    && (blockedUntil == null || !checkedAt.isBefore(blockedUntil));
        }
    }
}
