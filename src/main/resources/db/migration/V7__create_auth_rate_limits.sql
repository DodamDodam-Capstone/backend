-- 인증 요청 예산을 계정 생성 전에도 HMAC 식별자로 보관한다.
CREATE TABLE auth_rate_limits (
    action varchar(40) NOT NULL,
    key_hash bytea NOT NULL,
    window_started_at timestamptz NOT NULL,
    attempt_count bigint NOT NULL DEFAULT 0,
    failure_times timestamptz[] DEFAULT NULL,
    blocked_until timestamptz DEFAULT NULL,
    expires_at timestamptz NOT NULL,
    PRIMARY KEY (action, key_hash),
    CONSTRAINT ck_rate_hash CHECK (octet_length(key_hash) = 32),
    CONSTRAINT ck_rate_count CHECK (attempt_count >= 0),
    CONSTRAINT ck_rate_expiry CHECK (expires_at > window_started_at),
    CONSTRAINT ck_rate_block CHECK (blocked_until IS NULL OR expires_at >= blocked_until),
    CONSTRAINT ck_rate_failures CHECK (
        (action = 'PIN_FAILURE_ACCOUNT' AND failure_times IS NOT NULL
         AND cardinality(failure_times) BETWEEN 0 AND 5
         AND attempt_count = cardinality(failure_times)
         AND CASE WHEN coalesce(array_ndims(failure_times), 1) = 1
                  THEN array_position(failure_times, NULL) IS NULL ELSE false END)
        OR (action <> 'PIN_FAILURE_ACCOUNT' AND failure_times IS NULL)),
    CONSTRAINT ck_rate_time CHECK (
        isfinite(window_started_at) AND isfinite(expires_at)
        AND (blocked_until IS NULL OR isfinite(blocked_until)))
);

CREATE INDEX ix_rate_expiry ON auth_rate_limits(expires_at);
