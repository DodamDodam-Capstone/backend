-- 이전 발급기에서 초 단위로 안내한 토큰과 소수 초까지 안내하는 새 토큰을 구별한다.
ALTER TABLE email_verifications
    ADD COLUMN token_expiry_includes_fraction boolean NOT NULL DEFAULT false;
