CREATE TABLE email_verifications (
    id uuid PRIMARY KEY,
    email varchar(254) NOT NULL,
    purpose varchar(20) NOT NULL,
    eligible boolean NOT NULL,
    code_hash bytea NOT NULL,
    expires_at timestamptz NOT NULL,
    attempt_count integer NOT NULL DEFAULT 0,
    verified_at timestamptz DEFAULT NULL,
    token_hash bytea DEFAULT NULL,
    token_expires_at timestamptz DEFAULT NULL,
    token_consumed_at timestamptz DEFAULT NULL,
    invalidated_at timestamptz DEFAULT NULL,
    setup_account_id uuid DEFAULT NULL REFERENCES accounts(id) ON DELETE RESTRICT,
    setup_session_binding uuid DEFAULT NULL REFERENCES session_security(id) ON DELETE RESTRICT,
    setup_generation bigint DEFAULT NULL,
    pin_version_snapshot bigint DEFAULT NULL,
    reset_account_id uuid DEFAULT NULL REFERENCES accounts(id) ON DELETE RESTRICT,
    reset_session_version bigint DEFAULT NULL,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_setup_account_session FOREIGN KEY (setup_session_binding,setup_account_id)
        REFERENCES session_security(id,account_id) ON DELETE RESTRICT,
    CONSTRAINT ck_verification_purpose CHECK (purpose IN ('SIGNUP','RESET_PASSWORD','PIN_SETUP','PIN_RESET')),
    CONSTRAINT ck_verification_email CHECK (email=lower(btrim(email)) AND char_length(email)>0),
    CONSTRAINT ck_verification_attempt CHECK (attempt_count>=0 AND expires_at>created_at),
    CONSTRAINT ck_verification_code_hash CHECK (octet_length(code_hash)=32),
    CONSTRAINT ck_verification_token_hash CHECK (token_hash IS NULL OR octet_length(token_hash)=32),
    CONSTRAINT ck_verification_token_group CHECK (
        (verified_at IS NULL AND token_hash IS NULL AND token_expires_at IS NULL) OR
        (verified_at IS NOT NULL AND token_hash IS NOT NULL AND token_expires_at IS NOT NULL
         AND token_expires_at>verified_at)),
    CONSTRAINT ck_verification_consumed CHECK (
        token_consumed_at IS NULL OR (verified_at IS NOT NULL AND token_consumed_at>=verified_at)),
    CONSTRAINT ck_verification_consumed_invalid CHECK (token_consumed_at IS NULL OR invalidated_at IS NULL),
    CONSTRAINT ck_verification_invalid_time CHECK (invalidated_at IS NULL OR invalidated_at>=created_at),
    CONSTRAINT ck_verification_verified_time CHECK (verified_at IS NULL OR verified_at>=created_at),
    CONSTRAINT ck_verification_decoy CHECK (eligible OR verified_at IS NULL),
    CONSTRAINT ck_verification_setup CHECK (
        (purpose IN ('PIN_SETUP','PIN_RESET') AND eligible AND setup_account_id IS NOT NULL
         AND setup_session_binding IS NOT NULL AND setup_generation IS NOT NULL AND setup_generation>=0) OR
        (purpose NOT IN ('PIN_SETUP','PIN_RESET') AND setup_account_id IS NULL
         AND setup_session_binding IS NULL AND setup_generation IS NULL)),
    CONSTRAINT ck_verification_pin_version CHECK (
        (purpose='PIN_RESET' AND pin_version_snapshot IS NOT NULL AND pin_version_snapshot>=1) OR
        (purpose<>'PIN_RESET' AND pin_version_snapshot IS NULL)),
    CONSTRAINT ck_verification_reset CHECK (
        (purpose='RESET_PASSWORD' AND eligible AND reset_account_id IS NOT NULL
         AND reset_session_version IS NOT NULL AND reset_session_version>=0) OR
        ((purpose<>'RESET_PASSWORD' OR NOT eligible)
         AND reset_account_id IS NULL AND reset_session_version IS NULL)),
    CONSTRAINT ck_verification_time CHECK (
        isfinite(created_at) AND isfinite(expires_at) AND
        (verified_at IS NULL OR isfinite(verified_at)) AND
        (token_expires_at IS NULL OR isfinite(token_expires_at)) AND
        (token_consumed_at IS NULL OR isfinite(token_consumed_at)) AND
        (invalidated_at IS NULL OR isfinite(invalidated_at)))
);
CREATE INDEX ix_verification_email_purpose ON email_verifications(email,purpose,created_at DESC);
CREATE INDEX ix_verification_setup ON email_verifications(setup_account_id,setup_session_binding)
    WHERE purpose IN ('PIN_SETUP','PIN_RESET') AND token_consumed_at IS NULL AND invalidated_at IS NULL;
CREATE INDEX ix_verification_reset ON email_verifications(reset_account_id)
    WHERE purpose='RESET_PASSWORD' AND token_consumed_at IS NULL AND invalidated_at IS NULL;
CREATE INDEX ix_verification_cleanup ON email_verifications(created_at);
