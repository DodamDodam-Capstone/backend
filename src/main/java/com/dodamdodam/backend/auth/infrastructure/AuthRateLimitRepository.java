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
            var rows = jdbc.query("""
                    SELECT window_started_at, attempt_count, blocked_until FROM auth_rate_limits
                    WHERE action = ? AND key_hash = ? FOR UPDATE
                    """, (result, row) -> new RateLimitState(
                    result.getObject("window_started_at", OffsetDateTime.class), result.getLong("attempt_count"),
                    result.getObject("blocked_until", OffsetDateTime.class)), key.action().name(), key.keyHash());
            states.put(key, rows.isEmpty() ? null : rows.getFirst());
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
