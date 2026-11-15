-- 시작 키와 아이당 미종료 1개를 보장하며, 같은 날 종료된 대화는 여러 개 보존한다.
CREATE TABLE conversations (
    id uuid PRIMARY KEY,
    child_id uuid NOT NULL REFERENCES children(id) ON DELETE RESTRICT,
    client_request_id uuid NOT NULL,
    service_date date NOT NULL,
    scheduled_end_at timestamptz NOT NULL,
    status text NOT NULL,
    end_requested_at timestamptz,
    end_reason text,
    title text,
    topic text,
    summary text,
    summary_status text NOT NULL,
    started_at timestamptz NOT NULL,
    ended_at timestamptz,
    summary_started_at timestamptz,
    summary_deadline_at timestamptz,
    summary_completed_at timestamptz,
    summary_error_code text,
    CONSTRAINT uq_conversation_request UNIQUE (child_id,client_request_id),
    CONSTRAINT ck_conversation_service_window CHECK (
        isfinite(service_date) AND isfinite(scheduled_end_at)
        AND scheduled_end_at=((service_date+1)::timestamp AT TIME ZONE 'Asia/Seoul')
        AND started_at>=((service_date+time '08:00') AT TIME ZONE 'Asia/Seoul')
        AND started_at<scheduled_end_at),
    CONSTRAINT ck_conversation_status CHECK (status IN ('ACTIVE','CLOSING','ENDED')),
    CONSTRAINT ck_conversation_end_reason CHECK (end_reason IS NULL OR end_reason IN ('MANUAL','MIDNIGHT')),
    CONSTRAINT ck_conversation_end_boundary CHECK (
        (end_requested_at IS NULL AND end_reason IS NULL) OR
        (end_requested_at IS NOT NULL AND end_reason IS NOT NULL AND end_requested_at>=started_at AND
         ((end_reason='MANUAL' AND end_requested_at<scheduled_end_at) OR
          (end_reason='MIDNIGHT' AND end_requested_at=scheduled_end_at)))),
    CONSTRAINT ck_conversation_summary_enum CHECK (
        summary_status IN ('NOT_STARTED','PENDING','READY','FAILED','EMPTY')),
    CONSTRAINT ck_conversation_lifecycle CHECK (
        (status='ACTIVE' AND end_requested_at IS NULL AND end_reason IS NULL
         AND ended_at IS NULL AND summary_status='NOT_STARTED') OR
        (status='CLOSING' AND end_requested_at IS NOT NULL AND end_reason IS NOT NULL
         AND ended_at IS NULL AND summary_status='NOT_STARTED') OR
        (status='ENDED' AND end_requested_at IS NOT NULL AND end_reason IS NOT NULL
         AND ended_at IS NOT NULL AND ended_at>=end_requested_at
         AND summary_status IN ('PENDING','READY','FAILED','EMPTY'))),
    CONSTRAINT ck_conversation_summary CHECK (
        (summary_status IN ('NOT_STARTED','EMPTY') AND summary_started_at IS NULL
         AND summary_deadline_at IS NULL AND summary_completed_at IS NULL
         AND summary_error_code IS NULL AND title IS NULL AND topic IS NULL AND summary IS NULL) OR
        (summary_status='PENDING' AND summary_started_at IS NOT NULL AND summary_deadline_at IS NOT NULL
         AND summary_completed_at IS NULL AND summary_error_code IS NULL
         AND title IS NULL AND topic IS NULL AND summary IS NULL) OR
        (summary_status='READY' AND summary_started_at IS NOT NULL AND summary_deadline_at IS NOT NULL
         AND summary_completed_at IS NOT NULL AND summary_error_code IS NULL
         AND title IS NOT NULL AND topic IS NOT NULL AND summary IS NOT NULL) OR
        (summary_status='FAILED' AND summary_started_at IS NOT NULL AND summary_deadline_at IS NOT NULL
         AND summary_completed_at IS NOT NULL AND summary_error_code IS NOT NULL
         AND title IS NULL AND topic IS NULL AND summary IS NULL)),
    CONSTRAINT ck_conversation_summary_order CHECK (
        (summary_started_at IS NULL OR summary_deadline_at>summary_started_at) AND
        (summary_completed_at IS NULL OR summary_completed_at>=summary_started_at)),
    CONSTRAINT ck_conversation_error CHECK (summary_error_code IS NULL OR char_length(summary_error_code)>0),
    CONSTRAINT ck_conversation_ready_deadline CHECK (
        summary_status<>'READY' OR summary_completed_at<summary_deadline_at),
    CONSTRAINT ck_conversation_time CHECK (
        isfinite(started_at) AND (end_requested_at IS NULL OR isfinite(end_requested_at)) AND
        (ended_at IS NULL OR isfinite(ended_at)) AND
        (summary_started_at IS NULL OR isfinite(summary_started_at)) AND
        (summary_deadline_at IS NULL OR isfinite(summary_deadline_at)) AND
        (summary_completed_at IS NULL OR isfinite(summary_completed_at)))
);
CREATE UNIQUE INDEX uq_conversations_unended_child ON conversations(child_id) WHERE status IN ('ACTIVE','CLOSING');
CREATE INDEX ix_conversations_child_started ON conversations(child_id,started_at DESC,id DESC);
CREATE INDEX ix_conversations_summary_deadline ON conversations(summary_deadline_at) WHERE summary_status='PENDING';

-- SCRUM-88이 재사용할 전체 발화 스키마이며, 처리 중 발화와 허용 텍스트의 조합을 제한한다.
CREATE TABLE conversation_turns (
    id uuid PRIMARY KEY,
    conversation_id uuid NOT NULL REFERENCES conversations(id) ON DELETE RESTRICT,
    client_request_id uuid NOT NULL,
    request_hash text NOT NULL,
    sequence integer NOT NULL,
    status text NOT NULL,
    child_text text,
    child_text_visibility text NOT NULL,
    reply_text text,
    reply_text_visibility text NOT NULL,
    error_code text,
    created_at timestamptz NOT NULL,
    processing_deadline_at timestamptz NOT NULL,
    completed_at timestamptz,
    CONSTRAINT uq_turn_request UNIQUE (conversation_id,client_request_id),
    CONSTRAINT uq_turn_sequence UNIQUE (conversation_id,sequence),
    CONSTRAINT ck_turn_sequence CHECK (sequence>=1),
    CONSTRAINT ck_turn_hash CHECK (request_hash ~ '^[0-9a-f]{64}$'),
    CONSTRAINT ck_turn_status CHECK (status IN ('PROCESSING','SUCCEEDED','FAILED')),
    CONSTRAINT ck_turn_child_visibility CHECK (
        (child_text_visibility='OMITTED' AND child_text IS NULL) OR
        (child_text_visibility IN ('VISIBLE','REDACTED') AND child_text IS NOT NULL)),
    CONSTRAINT ck_turn_reply_visibility CHECK (
        (reply_text_visibility='OMITTED' AND reply_text IS NULL) OR
        (reply_text_visibility IN ('VISIBLE','REDACTED') AND reply_text IS NOT NULL)),
    CONSTRAINT ck_turn_lifecycle CHECK (
        (status='PROCESSING' AND completed_at IS NULL AND error_code IS NULL
         AND child_text IS NULL AND reply_text IS NULL
         AND child_text_visibility='OMITTED' AND reply_text_visibility='OMITTED') OR
        (status='SUCCEEDED' AND completed_at IS NOT NULL AND error_code IS NULL) OR
        (status='FAILED' AND completed_at IS NOT NULL AND error_code IS NOT NULL)),
    CONSTRAINT ck_turn_error CHECK (error_code IS NULL OR char_length(error_code)>0),
    CONSTRAINT ck_turn_success_deadline CHECK (status<>'SUCCEEDED' OR completed_at<processing_deadline_at),
    CONSTRAINT ck_turn_time_order CHECK (
        processing_deadline_at>created_at AND (completed_at IS NULL OR completed_at>=created_at)),
    CONSTRAINT ck_turn_time CHECK (
        isfinite(created_at) AND isfinite(processing_deadline_at) AND
        (completed_at IS NULL OR isfinite(completed_at)))
);
CREATE UNIQUE INDEX uq_turns_processing_conversation ON conversation_turns(conversation_id) WHERE status='PROCESSING';
CREATE INDEX ix_turns_processing_deadline ON conversation_turns(processing_deadline_at) WHERE status='PROCESSING';

-- 종료 요청자 한 명에 제한하지 않고 여러 기존 연결의 복구 기한을 기록한다.
CREATE TABLE conversation_session_links (
    session_security_id uuid NOT NULL REFERENCES session_security(id) ON DELETE RESTRICT,
    conversation_id uuid NOT NULL REFERENCES conversations(id) ON DELETE RESTRICT,
    bound_at timestamptz NOT NULL,
    end_recovery_until timestamptz,
    PRIMARY KEY (session_security_id,conversation_id),
    CONSTRAINT ck_link_time CHECK (
        isfinite(bound_at) AND (end_recovery_until IS NULL OR isfinite(end_recovery_until)))
);
CREATE INDEX ix_links_conversation ON conversation_session_links(conversation_id);
