CREATE TABLE children (
    id uuid PRIMARY KEY,
    account_id uuid NOT NULL UNIQUE REFERENCES accounts(id) ON DELETE RESTRICT,
    name varchar(5) NOT NULL,
    nickname varchar(20) NOT NULL,
    birth_date date NOT NULL,
    gender varchar(20) NOT NULL,
    interests text[] NOT NULL DEFAULT '{}'::text[],
    character_id varchar(64) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_child_name CHECK (char_length(name) BETWEEN 1 AND 5 AND name=btrim(name)),
    CONSTRAINT ck_child_nickname CHECK (char_length(nickname) BETWEEN 1 AND 20 AND nickname=btrim(nickname)),
    CONSTRAINT ck_child_gender CHECK (gender IN ('MALE','FEMALE')),
    CONSTRAINT ck_child_interests CHECK (
        cardinality(interests) BETWEEN 0 AND 10 AND
        CASE WHEN coalesce(array_ndims(interests),1)=1
             THEN array_position(interests,NULL) IS NULL ELSE false END),
    CONSTRAINT ck_child_character CHECK (character_id ~ '^[A-Za-z0-9_-]{1,64}$'),
    CONSTRAINT ck_child_time CHECK (isfinite(birth_date) AND isfinite(created_at))
);
