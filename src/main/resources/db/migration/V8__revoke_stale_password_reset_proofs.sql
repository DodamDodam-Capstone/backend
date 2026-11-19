-- 계정 버전을 변경한 트랜잭션에서 사용하지 않은 이전 재설정 번호와 토큰을 함께 폐기한다.
CREATE FUNCTION revoke_stale_password_reset_proofs() RETURNS trigger
LANGUAGE plpgsql SET search_path FROM CURRENT AS $$
BEGIN
    WITH stale AS (
        SELECT id FROM email_verifications
        WHERE reset_account_id = NEW.id AND purpose = 'RESET_PASSWORD'
          AND reset_session_version <> NEW.session_version
          AND token_consumed_at IS NULL AND invalidated_at IS NULL
        ORDER BY id FOR UPDATE
    )
    UPDATE email_verifications AS proof
    SET invalidated_at = GREATEST(clock_timestamp(), proof.created_at)
    FROM stale WHERE proof.id = stale.id;
    RETURN NEW;
END;
$$;

CREATE TRIGGER revoke_password_reset_proofs_on_version_change
AFTER UPDATE OF session_version ON accounts
FOR EACH ROW WHEN (OLD.session_version IS DISTINCT FROM NEW.session_version)
EXECUTE FUNCTION revoke_stale_password_reset_proofs();

-- 배포 전에 이미 버전이 바뀐 계정에도 적용하고 소비·폐기 이력은 보존한다.
UPDATE email_verifications AS proof
SET invalidated_at = GREATEST(clock_timestamp(), proof.created_at)
FROM accounts AS account
WHERE proof.reset_account_id = account.id AND proof.purpose = 'RESET_PASSWORD'
  AND proof.reset_session_version <> account.session_version
  AND proof.token_consumed_at IS NULL AND proof.invalidated_at IS NULL;
