-- 계정당 PIN 해시와 재설정 버전을 한 행으로 관리한다.
CREATE TABLE guardian_pins (
    account_id uuid PRIMARY KEY REFERENCES accounts(id) ON DELETE RESTRICT,
    pin_hash text NOT NULL,
    pin_version bigint NOT NULL DEFAULT 1,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_pin_hash CHECK (char_length(pin_hash)>0),
    CONSTRAINT ck_pin_version CHECK (pin_version>=1),
    CONSTRAINT ck_pin_time CHECK (isfinite(created_at) AND isfinite(updated_at))
);
