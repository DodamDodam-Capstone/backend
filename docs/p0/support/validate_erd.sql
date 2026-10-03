-- 최종 설계 v3.0 13테이블/115컬럼 검증. 권위 있는 DDL은 Integrated_ERD_Design.md §12.2다.
-- 검증 대상은 DB row/구조 제약이다. 실제 현재 권한·교차 테이블 업무 상태·HTTP/AI 실행은 별도 서비스 검사다.
-- 독립 임시 DB에서 BEGIN; CREATE SCHEMA ...; SET LOCAL search_path=...;
-- 본문 §12.2 DDL; 이 파일; ROLLBACK; 순으로 실행한다. 운영 DB 사용 금지.
\set ON_ERROR_STOP on
\o /dev/null
CREATE TEMP TABLE erd_audit_results(kind text NOT NULL, label text NOT NULL);
CREATE FUNCTION pg_temp.uid(n integer) RETURNS uuid LANGUAGE sql IMMUTABLE AS $$
  SELECT ('00000000-0000-4000-8000-' || lpad(n::text,12,'0'))::uuid
$$;
CREATE FUNCTION pg_temp.expect_true(value boolean, label text) RETURNS void LANGUAGE plpgsql AS $$
BEGIN
  IF value IS DISTINCT FROM true THEN RAISE EXCEPTION 'FAIL: %',label; END IF;
  INSERT INTO erd_audit_results VALUES ('positive',label);
END $$;
CREATE FUNCTION pg_temp.expect_reject(statement text, expected text, label text)
RETURNS void LANGUAGE plpgsql AS $$
DECLARE actual text;
BEGIN
  BEGIN EXECUTE statement;
  EXCEPTION WHEN OTHERS THEN GET STACKED DIAGNOSTICS actual=RETURNED_SQLSTATE;
  END;
  IF actual IS NULL OR NOT (actual=expected OR (expected='23503' AND actual='23001')) THEN
    RAISE EXCEPTION 'FAIL: %, expected %, got %',label,expected,coalesce(actual,'accepted');
  END IF;
  INSERT INTO erd_audit_results VALUES ('rejected',label);
END $$;

SELECT pg_temp.expect_true((SELECT count(*)=13 FROM information_schema.tables
  WHERE table_schema=current_schema() AND table_type='BASE TABLE'),'13 tables');
SELECT pg_temp.expect_true((SELECT count(*)=115 FROM information_schema.columns
  WHERE table_schema=current_schema()),'115 columns');

-- 세션 timezone을 UTC로 두어 날짜 CHECK가 Asia/Seoul을 명시적으로 사용하는지 확인한다.
SET LOCAL TIME ZONE 'UTC';
SELECT pg_temp.expect_true(NOT EXISTS (SELECT 1 FROM information_schema.columns
  WHERE table_schema=current_schema() AND table_name='conversation_turns' AND column_name='topic_suggestions'), 'topic storage column absent');
SELECT pg_temp.expect_true(NOT EXISTS (SELECT 1 FROM pg_indexes
  WHERE schemaname=current_schema() AND indexname='uq_links_end_winner'), 'single end winner index absent');
SELECT pg_temp.expect_true(NOT EXISTS (SELECT 1 FROM pg_constraint
  WHERE conrelid='conversations'::regclass AND conname='uq_conversation_service_date'), 'daily unique constraint removed');
SELECT pg_temp.expect_true((SELECT indexdef LIKE '%ACTIVE%' AND indexdef LIKE '%CLOSING%'
  FROM pg_indexes WHERE schemaname=current_schema() AND indexname='uq_conversations_unended_child'), 'unended index includes ACTIVE and CLOSING');
SELECT pg_temp.expect_true((SELECT indexdef LIKE '%PIN_RESET%' FROM pg_indexes
  WHERE schemaname=current_schema() AND indexname='ix_verification_setup'), 'PIN purpose index includes reset');

INSERT INTO accounts(id,email) VALUES (pg_temp.uid(1),'a@example.test'),(pg_temp.uid(2),'b@example.test');
INSERT INTO auth_identities(id,account_id,provider,issuer,subject)
  VALUES (pg_temp.uid(3),pg_temp.uid(1),'GOOGLE','https://accounts.google.com','subject-1');
INSERT INTO children(id,account_id,name,nickname,birth_date,gender,character_id)
  VALUES (pg_temp.uid(11),pg_temp.uid(1),'도담','도담이','2020-01-01','MALE','dodam'),
         (pg_temp.uid(12),pg_temp.uid(2),'다솜','다솜이','2021-01-01','FEMALE','dodam');
INSERT INTO guardian_pins(account_id,pin_hash) VALUES (pg_temp.uid(1),'fixture-encoded-hash');
INSERT INTO session_security(id,account_id,session_version,created_at)
  VALUES (pg_temp.uid(21),pg_temp.uid(1),0,'2026-10-02 10:00+09'),
         (pg_temp.uid(22),pg_temp.uid(1),0,'2026-10-02 10:00+09'),
         (pg_temp.uid(23),pg_temp.uid(2),0,'2026-10-02 10:00+09'),
         (pg_temp.uid(24),pg_temp.uid(1),0,'2026-10-02 10:00+09');

SELECT pg_temp.expect_true((SELECT password_hash IS NULL AND session_version=0 FROM accounts WHERE id=pg_temp.uid(1)), 'account defaults');
SELECT pg_temp.expect_true((SELECT interests='{}'::text[] FROM children WHERE id=pg_temp.uid(11)), 'interests default empty');
SELECT pg_temp.expect_true((SELECT pin_version=1 FROM guardian_pins WHERE account_id=pg_temp.uid(1)), 'PIN version default');
SELECT pg_temp.expect_true((SELECT expires_at IS NULL AND setup_generation=0 FROM session_security WHERE id=pg_temp.uid(21)), 'nullable login expiry');
SELECT pg_temp.expect_reject($s$INSERT INTO accounts(id,email) VALUES (pg_temp.uid(99),'a@example.test')$s$,'23505','canonical email unique');
SELECT pg_temp.expect_reject($s$UPDATE accounts SET email=' A@EXAMPLE.TEST ' WHERE id=pg_temp.uid(1)$s$,'23514','email canonical check');
SELECT pg_temp.expect_reject($s$UPDATE accounts SET password_hash='' WHERE id=pg_temp.uid(1)$s$,'23514','empty password hash');
SELECT pg_temp.expect_reject($s$UPDATE accounts SET session_version=-1 WHERE id=pg_temp.uid(1)$s$,'23514','negative session version');
SELECT pg_temp.expect_reject($s$UPDATE accounts SET email=NULL WHERE id=pg_temp.uid(1)$s$,'23502','email not null');
SELECT pg_temp.expect_reject($s$INSERT INTO auth_identities(id,account_id,provider,issuer,subject) VALUES (pg_temp.uid(99),pg_temp.uid(2),'GOOGLE','https://accounts.google.com','subject-1')$s$,'23505','external identity unique');
SELECT pg_temp.expect_reject($s$UPDATE auth_identities SET provider='OTHER' WHERE id=pg_temp.uid(3)$s$,'23514','provider enum');
SELECT pg_temp.expect_reject($s$UPDATE auth_identities SET account_id=pg_temp.uid(99) WHERE id=pg_temp.uid(3)$s$,'23503','identity account FK');
SELECT pg_temp.expect_reject($s$UPDATE children SET account_id=pg_temp.uid(1) WHERE id=pg_temp.uid(12)$s$,'23505','one child per account');
SELECT pg_temp.expect_reject($s$UPDATE children SET name='' WHERE id=pg_temp.uid(11)$s$,'23514','empty child name');
SELECT pg_temp.expect_reject($s$UPDATE children SET name='가나다라마바' WHERE id=pg_temp.uid(11)$s$,'22001','child name maximum length');
SELECT pg_temp.expect_reject($s$UPDATE children SET nickname=NULL WHERE id=pg_temp.uid(11)$s$,'23502','nickname required');
SELECT pg_temp.expect_reject($s$UPDATE children SET nickname='' WHERE id=pg_temp.uid(11)$s$,'23514','empty nickname rejected');
SELECT pg_temp.expect_reject($s$UPDATE children SET nickname=' 도담이 ' WHERE id=pg_temp.uid(11)$s$,'23514','nickname must be trimmed');
SELECT pg_temp.expect_reject($s$UPDATE children SET nickname=repeat('가',21) WHERE id=pg_temp.uid(11)$s$,'22001','nickname maximum 20 codepoints');
UPDATE children SET nickname=repeat('🐣',20) WHERE id=pg_temp.uid(11);
SELECT pg_temp.expect_true((SELECT char_length(nickname)=20 FROM children WHERE id=pg_temp.uid(11)), '20 Unicode codepoints nickname accepted');
UPDATE children SET nickname='도담이' WHERE id=pg_temp.uid(11);
SELECT pg_temp.expect_true((SELECT name='도담' AND nickname='도담이' FROM children WHERE id=pg_temp.uid(11)), 'name and nickname stored independently');
SELECT pg_temp.expect_reject($s$UPDATE children SET gender='UNSPECIFIED' WHERE id=pg_temp.uid(11)$s$,'23514','gender enum');
SELECT pg_temp.expect_reject($s$UPDATE children SET interests=ARRAY['one',NULL] WHERE id=pg_temp.uid(11)$s$,'23514','interests null element');
SELECT pg_temp.expect_reject($s$UPDATE children SET interests=ARRAY[['one','two']] WHERE id=pg_temp.uid(11)$s$,'23514','interests multidimensional rejected by CHECK');
SELECT pg_temp.expect_reject($s$UPDATE children SET interests=array_fill('a'::text,ARRAY[11]) WHERE id=pg_temp.uid(11)$s$,'23514','interests count limit');
SELECT pg_temp.expect_reject($s$UPDATE children SET character_id='invalid id' WHERE id=pg_temp.uid(11)$s$,'23514','character alphabet');
SELECT pg_temp.expect_reject($s$UPDATE children SET birth_date='infinity' WHERE id=pg_temp.uid(11)$s$,'23514','finite birth date');
SELECT pg_temp.expect_reject($s$UPDATE guardian_pins SET pin_hash='' WHERE account_id=pg_temp.uid(1)$s$,'23514','empty PIN hash');
SELECT pg_temp.expect_reject($s$UPDATE guardian_pins SET pin_version=0 WHERE account_id=pg_temp.uid(1)$s$,'23514','PIN version positive');
UPDATE session_security SET guardian_unlocked_until='2026-10-02 10:30+09',guardian_pin_version=1 WHERE id=pg_temp.uid(21);
SELECT pg_temp.expect_true((SELECT guardian_unlocked_until IS NOT NULL AND expires_at IS NULL FROM session_security WHERE id=pg_temp.uid(21)), 'finite guardian with null login upper bound');
SELECT pg_temp.expect_reject($s$UPDATE session_security SET guardian_pin_version=NULL WHERE id=pg_temp.uid(21)$s$,'23514','guardian pair null consistency');
SELECT pg_temp.expect_reject($s$UPDATE session_security SET guardian_pin_version=0 WHERE id=pg_temp.uid(21)$s$,'23514','guardian version positive');
SELECT pg_temp.expect_reject($s$UPDATE session_security SET expires_at=created_at WHERE id=pg_temp.uid(21)$s$,'23514','login expiry after creation');
SELECT pg_temp.expect_reject($s$UPDATE session_security SET expires_at='2026-10-02 10:20+09' WHERE id=pg_temp.uid(21)$s$,'23514','guardian cannot exceed finite login bound');
SELECT pg_temp.expect_reject($s$UPDATE session_security SET expires_at='infinity' WHERE id=pg_temp.uid(22)$s$,'23514','infinity cannot replace null login bound');
SELECT pg_temp.expect_reject($s$UPDATE session_security SET revoked_at=created_at-interval '1 second' WHERE id=pg_temp.uid(22)$s$,'23514','revocation after creation');

INSERT INTO email_verifications(id,email,purpose,eligible,code_hash,created_at,expires_at)
  VALUES (pg_temp.uid(31),'new@example.test','SIGNUP',true,decode(repeat('aa',32),'hex'),'2026-10-02 10:00+09','2026-10-02 10:10+09'),
         (pg_temp.uid(32),'unknown@example.test','RESET_PASSWORD',false,decode(repeat('aa',32),'hex'),'2026-10-02 10:00+09','2026-10-02 10:10+09');
INSERT INTO email_verifications(id,email,purpose,eligible,code_hash,created_at,expires_at,setup_account_id,setup_session_binding,setup_generation)
  VALUES (pg_temp.uid(33),'a@example.test','PIN_SETUP',true,decode(repeat('aa',32),'hex'),'2026-10-02 10:00+09','2026-10-02 10:10+09',pg_temp.uid(1),pg_temp.uid(21),0);
INSERT INTO email_verifications(id,email,purpose,eligible,code_hash,created_at,expires_at,reset_account_id,reset_session_version)
  VALUES (pg_temp.uid(34),'a@example.test','RESET_PASSWORD',true,decode(repeat('aa',32),'hex'),'2026-10-02 10:00+09','2026-10-02 10:10+09',pg_temp.uid(1),0);
SELECT pg_temp.expect_true((SELECT reset_account_id IS NULL AND NOT eligible FROM email_verifications WHERE id=pg_temp.uid(32)), 'decoy without account binding');
SELECT pg_temp.expect_reject($s$UPDATE email_verifications SET verified_at='2026-10-02 10:01+09' WHERE id=pg_temp.uid(31)$s$,'23514','verified triple cannot be partial');
SELECT pg_temp.expect_reject($s$UPDATE email_verifications SET verified_at='2026-10-02 10:01+09',token_hash=decode(repeat('aa',32),'hex'),token_expires_at='2026-10-02 10:11+09' WHERE id=pg_temp.uid(32)$s$,'23514','decoy cannot verify');
SELECT pg_temp.expect_reject($s$UPDATE email_verifications SET code_hash=decode('aa','hex') WHERE id=pg_temp.uid(31)$s$,'23514','code hash length');
SELECT pg_temp.expect_reject($s$UPDATE email_verifications SET setup_session_binding=pg_temp.uid(23) WHERE id=pg_temp.uid(33)$s$,'23503','PIN_SETUP composite account session FK');
SELECT pg_temp.expect_reject($s$UPDATE email_verifications SET setup_generation=NULL WHERE id=pg_temp.uid(33)$s$,'23514','PIN_SETUP required generation');
SELECT pg_temp.expect_reject($s$UPDATE email_verifications SET eligible=false WHERE id=pg_temp.uid(33)$s$,'23514','PIN_SETUP cannot be decoy');
SELECT pg_temp.expect_reject($s$UPDATE email_verifications SET reset_account_id=NULL WHERE id=pg_temp.uid(34)$s$,'23514','eligible reset requires account');
SELECT pg_temp.expect_reject($s$UPDATE email_verifications SET reset_session_version=-1 WHERE id=pg_temp.uid(34)$s$,'23514','reset version nonnegative');
SELECT pg_temp.expect_reject($s$UPDATE email_verifications SET setup_account_id=pg_temp.uid(1) WHERE id=pg_temp.uid(31)$s$,'23514','SIGNUP cannot have PIN_SETUP binding');
SELECT pg_temp.expect_reject($s$UPDATE email_verifications SET reset_account_id=pg_temp.uid(1),reset_session_version=0 WHERE id=pg_temp.uid(32)$s$,'23514','reset decoy cannot bind account');
UPDATE email_verifications SET verified_at='2026-10-02 10:09+09',token_hash=decode(repeat('bb',32),'hex'),token_expires_at='2026-10-02 10:19+09' WHERE id=pg_temp.uid(31);
SELECT pg_temp.expect_true((SELECT token_expires_at>expires_at FROM email_verifications WHERE id=pg_temp.uid(31)), 'token may outlive code');
SELECT pg_temp.expect_reject($s$UPDATE email_verifications SET token_hash=decode('aa','hex') WHERE id=pg_temp.uid(31)$s$,'23514','token hash length');
SELECT pg_temp.expect_reject($s$UPDATE email_verifications SET token_expires_at=verified_at WHERE id=pg_temp.uid(31)$s$,'23514','token expiry after verification');
SELECT pg_temp.expect_reject($s$UPDATE email_verifications SET token_consumed_at='2026-10-02 10:10+09',invalidated_at='2026-10-02 10:11+09' WHERE id=pg_temp.uid(31)$s$,'23514','consumed invalidated mutual exclusion');
SELECT pg_temp.expect_reject($s$UPDATE email_verifications SET token_consumed_at='2026-10-02 10:08+09' WHERE id=pg_temp.uid(31)$s$,'23514','consumption after verification');
SELECT pg_temp.expect_reject($s$DELETE FROM session_security WHERE id=pg_temp.uid(21)$s$,'23503','challenge blocks session physical deletion');

-- PIN_RESET은 기존 PIN 목적 결합을 재사용하며 snapshot만 추가한다.
INSERT INTO email_verifications(id,email,purpose,eligible,code_hash,created_at,expires_at,
  setup_account_id,setup_session_binding,setup_generation,pin_version_snapshot)
  VALUES (pg_temp.uid(35),'a@example.test','PIN_RESET',true,decode(repeat('aa',32),'hex'),
  '2026-10-02 10:00+09','2026-10-02 10:10+09',pg_temp.uid(1),pg_temp.uid(24),0,1);
SELECT pg_temp.expect_true((SELECT pin_version_snapshot=1 AND setup_account_id=pg_temp.uid(1)
  AND reset_account_id IS NULL FROM email_verifications WHERE id=pg_temp.uid(35)), 'PIN_RESET valid account context generation snapshot');
SELECT pg_temp.expect_true((SELECT bool_and(pin_version_snapshot IS NULL) FROM email_verifications
  WHERE purpose<>'PIN_RESET'), 'non-reset purposes default snapshot null');
SELECT pg_temp.expect_reject($s$UPDATE email_verifications SET pin_version_snapshot=NULL WHERE id=pg_temp.uid(35)$s$,'23514','PIN_RESET snapshot required');
SELECT pg_temp.expect_reject($s$UPDATE email_verifications SET pin_version_snapshot=0 WHERE id=pg_temp.uid(35)$s$,'23514','PIN_RESET snapshot positive');
SELECT pg_temp.expect_reject($s$UPDATE email_verifications SET pin_version_snapshot=-1 WHERE id=pg_temp.uid(35)$s$,'23514','PIN_RESET snapshot negative');
SELECT pg_temp.expect_reject($s$UPDATE email_verifications SET setup_account_id=NULL WHERE id=pg_temp.uid(35)$s$,'23514','PIN_RESET account required');
SELECT pg_temp.expect_reject($s$UPDATE email_verifications SET setup_session_binding=NULL WHERE id=pg_temp.uid(35)$s$,'23514','PIN_RESET context required');
SELECT pg_temp.expect_reject($s$UPDATE email_verifications SET setup_generation=NULL WHERE id=pg_temp.uid(35)$s$,'23514','PIN_RESET generation required');
SELECT pg_temp.expect_reject($s$UPDATE email_verifications SET setup_session_binding=pg_temp.uid(23) WHERE id=pg_temp.uid(35)$s$,'23503','PIN_RESET composite account session FK');
SELECT pg_temp.expect_reject($s$UPDATE email_verifications SET eligible=false WHERE id=pg_temp.uid(35)$s$,'23514','PIN_RESET cannot be decoy');
SELECT pg_temp.expect_reject($s$UPDATE email_verifications SET pin_version_snapshot=1 WHERE id=pg_temp.uid(31)$s$,'23514','SIGNUP snapshot forbidden');
SELECT pg_temp.expect_reject($s$UPDATE email_verifications SET pin_version_snapshot=1 WHERE id=pg_temp.uid(33)$s$,'23514','PIN_SETUP snapshot forbidden');
SELECT pg_temp.expect_reject($s$UPDATE email_verifications SET pin_version_snapshot=1 WHERE id=pg_temp.uid(34)$s$,'23514','RESET_PASSWORD snapshot forbidden');
UPDATE email_verifications SET verified_at='2026-10-02 10:01+09',token_hash=decode(repeat('bb',32),'hex'),
  token_expires_at='2026-10-02 10:11+09',token_consumed_at='2026-10-02 10:02+09' WHERE id=pg_temp.uid(35);
SELECT pg_temp.expect_true((SELECT token_consumed_at IS NOT NULL AND invalidated_at IS NULL
  FROM email_verifications WHERE id=pg_temp.uid(35)), 'PIN_RESET consumed proof keeps invalidation null');
SELECT pg_temp.expect_reject($s$UPDATE email_verifications SET invalidated_at='2026-10-02 10:02+09' WHERE id=pg_temp.uid(35)$s$,'23514','PIN_RESET consumed proof cannot also invalidate');

INSERT INTO auth_rate_limits(action,key_hash,window_started_at,expires_at)
  VALUES ('API_IP',decode(repeat('cc',32),'hex'),'2026-10-02 10:00+09','2026-10-02 10:01+09');
INSERT INTO auth_rate_limits(action,key_hash,window_started_at,expires_at,failure_times)
  VALUES ('PIN_FAILURE_ACCOUNT',decode(repeat('dd',32),'hex'),'2026-10-02 10:00+09','2026-10-02 10:15+09','{}');
SELECT pg_temp.expect_true((SELECT attempt_count=0 AND failure_times='{}'::timestamptz[] FROM auth_rate_limits WHERE action='PIN_FAILURE_ACCOUNT'), 'empty PIN failure array allowed');
SELECT pg_temp.expect_reject($s$UPDATE auth_rate_limits SET key_hash=decode('cc','hex') WHERE action='API_IP'$s$,'23514','rate key hash length');
SELECT pg_temp.expect_reject($s$UPDATE auth_rate_limits SET failure_times='{}' WHERE action='API_IP'$s$,'23514','non-PIN action must not have failures');
SELECT pg_temp.expect_reject($s$UPDATE auth_rate_limits SET failure_times=NULL WHERE action='PIN_FAILURE_ACCOUNT'$s$,'23514','PIN failure array required');
SELECT pg_temp.expect_reject($s$UPDATE auth_rate_limits SET failure_times=ARRAY[NULL::timestamptz],attempt_count=1 WHERE action='PIN_FAILURE_ACCOUNT'$s$,'23514','PIN failure null element');
SELECT pg_temp.expect_reject($s$UPDATE auth_rate_limits SET failure_times=ARRAY[['2026-10-02 10:00+09'::timestamptz]],attempt_count=1 WHERE action='PIN_FAILURE_ACCOUNT'$s$,'23514','PIN failure multidimensional rejected by CHECK');
SELECT pg_temp.expect_reject($s$UPDATE auth_rate_limits SET failure_times=array_fill('2026-10-02 10:00+09'::timestamptz,ARRAY[6]),attempt_count=6 WHERE action='PIN_FAILURE_ACCOUNT'$s$,'23514','PIN failure count ceiling');
SELECT pg_temp.expect_reject($s$UPDATE auth_rate_limits SET attempt_count=1 WHERE action='PIN_FAILURE_ACCOUNT'$s$,'23514','PIN failure count matches array');
SELECT pg_temp.expect_reject($s$UPDATE auth_rate_limits SET blocked_until=expires_at+interval '1 second' WHERE action='API_IP'$s$,'23514','cleanup cannot precede active block');

INSERT INTO conversations(id,child_id,client_request_id,service_date,scheduled_end_at,status,summary_status,started_at)
  VALUES (pg_temp.uid(41),pg_temp.uid(11),pg_temp.uid(141),'2026-10-02','2026-10-03 00:00+09','ACTIVE','NOT_STARTED','2026-10-02 10:00+09'),
         (pg_temp.uid(42),pg_temp.uid(12),pg_temp.uid(142),'2026-10-02','2026-10-03 00:00+09','ACTIVE','NOT_STARTED','2026-10-02 10:00+09');
SELECT pg_temp.expect_reject($s$INSERT INTO conversations(id,child_id,client_request_id,service_date,scheduled_end_at,status,summary_status,started_at) VALUES (pg_temp.uid(49),pg_temp.uid(11),pg_temp.uid(149),'2026-10-03','2026-10-04 00:00+09','ACTIVE','NOT_STARTED','2026-10-03 08:00+09')$s$,'23505','one ACTIVE per child');
-- 날짜/시각은 한 행 안에서 DB가 보장한다. 현재시간 접근·모든 turn terminal은 서비스 검사다.
SELECT pg_temp.expect_true((SELECT service_date='2026-10-02'::date AND scheduled_end_at='2026-10-02 15:00Z'::timestamptz FROM conversations WHERE id=pg_temp.uid(41)), 'KST next midnight independent of UTC session timezone');
UPDATE conversations SET started_at='2026-10-02 08:00+09' WHERE id=pg_temp.uid(41);
SELECT pg_temp.expect_true((SELECT started_at='2026-10-01 23:00Z'::timestamptz FROM conversations WHERE id=pg_temp.uid(41)), 'start exactly 08 KST allowed');
SELECT pg_temp.expect_reject($s$UPDATE conversations SET started_at='2026-10-02 07:59:59.999999+09' WHERE id=pg_temp.uid(41)$s$,'23514','start before 08 KST rejected');
UPDATE conversations SET started_at='2026-10-02 23:59:59.999999+09' WHERE id=pg_temp.uid(41);
SELECT pg_temp.expect_true((SELECT started_at<scheduled_end_at FROM conversations WHERE id=pg_temp.uid(41)), 'start immediately before midnight allowed');
SELECT pg_temp.expect_reject($s$UPDATE conversations SET started_at=scheduled_end_at WHERE id=pg_temp.uid(41)$s$,'23514','start at midnight rejected');
SELECT pg_temp.expect_reject($s$UPDATE conversations SET started_at=scheduled_end_at+interval '1 second' WHERE id=pg_temp.uid(41)$s$,'23514','start after midnight rejected');
UPDATE conversations SET started_at='2026-10-02 10:00+09' WHERE id=pg_temp.uid(41);
SELECT pg_temp.expect_reject($s$UPDATE conversations SET scheduled_end_at=scheduled_end_at-interval '1 second' WHERE id=pg_temp.uid(41)$s$,'23514','scheduled end one second before exact KST midnight rejected');
SELECT pg_temp.expect_reject($s$UPDATE conversations SET scheduled_end_at=scheduled_end_at+interval '1 second' WHERE id=pg_temp.uid(41)$s$,'23514','scheduled end one second after exact KST midnight rejected');
SELECT pg_temp.expect_reject($s$UPDATE conversations SET service_date=service_date-1 WHERE id=pg_temp.uid(41)$s$,'23514','service date and midnight must agree');
SELECT pg_temp.expect_reject($s$UPDATE conversations SET service_date=NULL WHERE id=pg_temp.uid(41)$s$,'23502','service date required');
SELECT pg_temp.expect_reject($s$UPDATE conversations SET scheduled_end_at=NULL WHERE id=pg_temp.uid(41)$s$,'23502','scheduled end required');
SELECT pg_temp.expect_reject($s$UPDATE conversations SET scheduled_end_at='infinity' WHERE id=pg_temp.uid(41)$s$,'23514','scheduled end finite');
SELECT pg_temp.expect_reject($s$UPDATE conversations SET service_date='infinity' WHERE id=pg_temp.uid(41)$s$,'23514','service date finite');
SELECT pg_temp.expect_reject($s$UPDATE conversations SET status='CLOSING' WHERE id=pg_temp.uid(41)$s$,'23514','CLOSING requires boundary and reason');
SELECT pg_temp.expect_reject($s$UPDATE conversations SET status='ENDED',end_requested_at=scheduled_end_at,end_reason='MIDNIGHT',summary_status='EMPTY',ended_at=scheduled_end_at-interval '1 microsecond' WHERE id=pg_temp.uid(42)$s$,'23514','MIDNIGHT ENDED cannot precede end boundary');

-- 수동 종료 경계와 명시 CLOSING; 자정과 달리 즉시 끝나지 않아도 새 대화는 차단한다.
SELECT pg_temp.expect_reject($s$UPDATE conversations SET end_reason='UNKNOWN' WHERE id=pg_temp.uid(42)$s$,'23514','end reason enum');
SELECT pg_temp.expect_reject($s$UPDATE conversations SET end_requested_at='2026-10-02 10:05+09',end_reason='MANUAL' WHERE id=pg_temp.uid(42)$s$,'23514','ACTIVE end fields must be null');
SELECT pg_temp.expect_reject($s$UPDATE conversations SET status='CLOSING',end_reason='MANUAL' WHERE id=pg_temp.uid(42)$s$,'23514','CLOSING missing boundary rejected');
SELECT pg_temp.expect_reject($s$UPDATE conversations SET status='CLOSING',end_requested_at='2026-10-02 10:05+09' WHERE id=pg_temp.uid(42)$s$,'23514','CLOSING missing reason rejected');
SELECT pg_temp.expect_reject($s$UPDATE conversations SET status='CLOSING',end_requested_at=scheduled_end_at,end_reason='MANUAL' WHERE id=pg_temp.uid(42)$s$,'23514','manual boundary must precede midnight');
SELECT pg_temp.expect_reject($s$UPDATE conversations SET status='CLOSING',end_requested_at='2026-10-02 10:05+09',end_reason='MIDNIGHT' WHERE id=pg_temp.uid(42)$s$,'23514','midnight boundary must equal scheduled end');
SELECT pg_temp.expect_reject($s$UPDATE conversations SET status='CLOSING',end_requested_at=started_at-interval '1 second',end_reason='MANUAL' WHERE id=pg_temp.uid(42)$s$,'23514','end boundary cannot precede start');
SELECT pg_temp.expect_reject($s$UPDATE conversations SET status='CLOSING',end_requested_at='infinity',end_reason='MANUAL' WHERE id=pg_temp.uid(42)$s$,'23514','end boundary finite');
UPDATE conversations SET status='CLOSING',end_requested_at='2026-10-02 10:05+09',end_reason='MANUAL' WHERE id=pg_temp.uid(42);
SELECT pg_temp.expect_true((SELECT ended_at IS NULL AND summary_status='NOT_STARTED' FROM conversations WHERE id=pg_temp.uid(42)), 'CLOSING stores boundary without completion or summary');
SELECT pg_temp.expect_reject($s$UPDATE conversations SET ended_at=end_requested_at WHERE id=pg_temp.uid(42)$s$,'23514','CLOSING ended time must be null');
SELECT pg_temp.expect_reject($s$UPDATE conversations SET summary_status='EMPTY' WHERE id=pg_temp.uid(42)$s$,'23514','CLOSING summary cannot start');
SELECT pg_temp.expect_reject($s$UPDATE conversations SET status='ENDED',summary_status='EMPTY',ended_at=end_requested_at-interval '1 microsecond' WHERE id=pg_temp.uid(42)$s$,'23514','manual ENDED cannot precede end boundary');
SELECT pg_temp.expect_reject($s$INSERT INTO conversations(id,child_id,client_request_id,service_date,scheduled_end_at,status,summary_status,started_at) VALUES (pg_temp.uid(49),pg_temp.uid(12),pg_temp.uid(149),'2026-10-02','2026-10-03 00:00+09','ACTIVE','NOT_STARTED','2026-10-02 11:00+09')$s$,'23505','CLOSING blocks same-day ACTIVE');
SELECT pg_temp.expect_reject($s$INSERT INTO conversations(id,child_id,client_request_id,service_date,scheduled_end_at,status,summary_status,started_at) VALUES (pg_temp.uid(49),pg_temp.uid(12),pg_temp.uid(149),'2026-10-03','2026-10-04 00:00+09','ACTIVE','NOT_STARTED','2026-10-03 08:00+09')$s$,'23505','previous-day CLOSING blocks new ACTIVE');
SELECT pg_temp.expect_reject($s$INSERT INTO conversations(id,child_id,client_request_id,service_date,scheduled_end_at,status,summary_status,started_at,end_requested_at,end_reason) VALUES (pg_temp.uid(49),pg_temp.uid(12),pg_temp.uid(149),'2026-10-02','2026-10-03 00:00+09','CLOSING','NOT_STARTED','2026-10-02 11:00+09','2026-10-02 11:01+09','MANUAL')$s$,'23505','one CLOSING per child');
SELECT pg_temp.expect_reject($s$UPDATE conversations SET title='not ready' WHERE id=pg_temp.uid(41)$s$,'23514','ACTIVE cannot expose summary text');
SELECT pg_temp.expect_reject($s$UPDATE conversations SET status='ENDED' WHERE id=pg_temp.uid(41)$s$,'23514','ENDED needs time and summary state');
SELECT pg_temp.expect_reject($s$UPDATE conversations SET summary_status='PENDING' WHERE id=pg_temp.uid(41)$s$,'23514','ACTIVE summary state');
INSERT INTO conversation_turns(id,conversation_id,client_request_id,request_hash,sequence,status,child_text_visibility,reply_text_visibility,created_at,processing_deadline_at)
  VALUES (pg_temp.uid(51),pg_temp.uid(41),pg_temp.uid(151),repeat('a',64),1,'PROCESSING','OMITTED','OMITTED','2026-10-02 10:00+09','2026-10-02 10:01+09');
SELECT pg_temp.expect_reject($s$INSERT INTO conversation_turns(id,conversation_id,client_request_id,request_hash,sequence,status,child_text_visibility,reply_text_visibility,created_at,processing_deadline_at) VALUES (pg_temp.uid(59),pg_temp.uid(41),pg_temp.uid(159),repeat('b',64),2,'PROCESSING','OMITTED','OMITTED','2026-10-02 10:00+09','2026-10-02 10:01+09')$s$,'23505','one PROCESSING per conversation');
SELECT pg_temp.expect_reject($s$UPDATE conversation_turns SET sequence=0 WHERE id=pg_temp.uid(51)$s$,'23514','sequence positive');
SELECT pg_temp.expect_reject($s$UPDATE conversation_turns SET request_hash='' WHERE id=pg_temp.uid(51)$s$,'23514','request hash nonempty');
SELECT pg_temp.expect_true((SELECT request_hash ~ '^[0-9a-f]{64}$' FROM conversation_turns WHERE id=pg_temp.uid(51)), 'lowercase 64 hex request hash allowed');
SELECT pg_temp.expect_reject($s$UPDATE conversation_turns SET request_hash=repeat('a',63) WHERE id=pg_temp.uid(51)$s$,'23514','request hash 63 chars rejected');
SELECT pg_temp.expect_reject($s$UPDATE conversation_turns SET request_hash=repeat('a',65) WHERE id=pg_temp.uid(51)$s$,'23514','request hash 65 chars rejected');
SELECT pg_temp.expect_reject($s$UPDATE conversation_turns SET request_hash=repeat('A',64) WHERE id=pg_temp.uid(51)$s$,'23514','request hash uppercase rejected');
SELECT pg_temp.expect_reject($s$UPDATE conversation_turns SET request_hash=repeat('g',64) WHERE id=pg_temp.uid(51)$s$,'23514','request hash nonhex rejected');
SELECT pg_temp.expect_reject($s$UPDATE conversation_turns SET status='SUCCEEDED' WHERE id=pg_temp.uid(51)$s$,'23514','terminal needs completed time');
SELECT pg_temp.expect_reject($s$UPDATE conversation_turns SET child_text='secret' WHERE id=pg_temp.uid(51)$s$,'23514','OMITTED requires null');
SELECT pg_temp.expect_reject($s$UPDATE conversation_turns SET child_text_visibility='VISIBLE' WHERE id=pg_temp.uid(51)$s$,'23514','VISIBLE requires text');
SELECT pg_temp.expect_reject($s$UPDATE conversation_turns SET processing_deadline_at='infinity' WHERE id=pg_temp.uid(51)$s$,'23514','finite processing deadline');
UPDATE conversation_turns SET status='SUCCEEDED',completed_at='2026-10-02 10:00:30+09',reply_text='허용된 가상 응답',reply_text_visibility='VISIBLE' WHERE id=pg_temp.uid(51);
SELECT pg_temp.expect_true((SELECT child_text IS NULL AND reply_text IS NOT NULL FROM conversation_turns WHERE id=pg_temp.uid(51)), 'independent permitted text visibility');
SELECT pg_temp.expect_reject($s$UPDATE conversation_turns SET error_code='AI_TIMEOUT' WHERE id=pg_temp.uid(51)$s$,'23514','SUCCEEDED error null');
SELECT pg_temp.expect_reject($s$UPDATE conversation_turns SET completed_at=processing_deadline_at WHERE id=pg_temp.uid(51)$s$,'23514','SUCCEEDED cannot be recorded at deadline');
INSERT INTO conversation_turns(id,conversation_id,client_request_id,request_hash,sequence,status,child_text_visibility,reply_text_visibility,created_at,processing_deadline_at,completed_at,error_code)
  VALUES (pg_temp.uid(52),pg_temp.uid(41),pg_temp.uid(152),repeat('b',64),2,'FAILED','OMITTED','OMITTED','2026-10-02 10:01+09','2026-10-02 10:02+09','2026-10-02 10:02+09','AI_TIMEOUT');
SELECT pg_temp.expect_true((SELECT completed_at=processing_deadline_at FROM conversation_turns WHERE id=pg_temp.uid(52)), 'FAILED may be recorded at deadline');
SELECT pg_temp.expect_reject($s$UPDATE conversation_turns SET sequence=1 WHERE id=pg_temp.uid(52)$s$,'23505','unique turn sequence');
SELECT pg_temp.expect_reject($s$UPDATE conversation_turns SET client_request_id=pg_temp.uid(151) WHERE id=pg_temp.uid(52)$s$,'23505','unique turn request');
SELECT pg_temp.expect_reject($s$UPDATE conversation_turns SET error_code='' WHERE id=pg_temp.uid(52)$s$,'23514','FAILED error nonempty');
SELECT pg_temp.expect_reject($s$UPDATE conversation_turns SET error_code=NULL WHERE id=pg_temp.uid(52)$s$,'23514','FAILED error required');

-- 자정 전 접수된 결과는 기존 deadline 안이면 자정 뒤 저장할 수 있다.
INSERT INTO conversation_turns(id,conversation_id,client_request_id,request_hash,sequence,status,
  child_text_visibility,reply_text_visibility,created_at,processing_deadline_at,completed_at)
  VALUES (pg_temp.uid(53),pg_temp.uid(41),pg_temp.uid(153),repeat('c',64),5,'SUCCEEDED',
  'OMITTED','OMITTED','2026-10-02 23:59:59+09','2026-10-03 00:01+09','2026-10-03 00:00:02+09');
SELECT pg_temp.expect_true((SELECT t.created_at<c.scheduled_end_at AND t.completed_at>c.scheduled_end_at
  AND t.completed_at<t.processing_deadline_at FROM conversation_turns t JOIN conversations c ON c.id=t.conversation_id
  WHERE t.id=pg_temp.uid(53)), 'accepted turn can complete after midnight before original deadline');
INSERT INTO conversation_turns(id,conversation_id,client_request_id,request_hash,sequence,status,
  child_text,child_text_visibility,reply_text,reply_text_visibility,created_at,processing_deadline_at,completed_at,error_code)
  VALUES (pg_temp.uid(54),pg_temp.uid(41),pg_temp.uid(154),repeat('d',64),3,'FAILED',
  '허용된 아이 텍스트','VISIBLE','허용된 답변','VISIBLE','2026-10-02 10:04+09','2026-10-02 10:05+09','2026-10-02 10:04:20+09','AI_UPSTREAM_FAILED');
SELECT pg_temp.expect_true((SELECT status='FAILED' AND child_text IS NOT NULL AND reply_text IS NOT NULL
  FROM conversation_turns WHERE id=pg_temp.uid(54)), 'TTS FAILED can retain separately permitted texts');
INSERT INTO conversation_turns(id,conversation_id,client_request_id,request_hash,sequence,status,
  child_text_visibility,reply_text_visibility,created_at,processing_deadline_at,completed_at,error_code)
  VALUES (pg_temp.uid(55),pg_temp.uid(41),pg_temp.uid(155),repeat('e',64),4,'FAILED',
  'OMITTED','OMITTED','2026-10-02 10:05+09','2026-10-02 10:06+09','2026-10-02 10:05:10+09','STT_NO_SPEECH');
SELECT pg_temp.expect_true((SELECT status='FAILED' AND child_text IS NULL AND reply_text IS NULL
  FROM conversation_turns WHERE id=pg_temp.uid(55)), 'STT_NO_SPEECH stored FAILED with omitted texts');

WITH updated AS (
  UPDATE conversation_turns SET reply_text='late overwrite',reply_text_visibility='VISIBLE'
  WHERE id=pg_temp.uid(51) AND status='PROCESSING' RETURNING id)
SELECT pg_temp.expect_true((SELECT count(*)=0 FROM updated), 'terminal callback predicate prevents overwrite');

INSERT INTO conversation_session_links(session_security_id,conversation_id,bound_at)
  VALUES (pg_temp.uid(21),pg_temp.uid(41),'2026-10-02 10:00+09'),
         (pg_temp.uid(22),pg_temp.uid(41),'2026-10-02 10:00+09');
SELECT pg_temp.expect_true((SELECT count(*)=2 FROM conversation_session_links WHERE conversation_id=pg_temp.uid(41)), 'multiple ACTIVE sessions allowed');
SELECT pg_temp.expect_reject($s$INSERT INTO conversation_session_links VALUES (pg_temp.uid(21),pg_temp.uid(41),'2026-10-02 10:01+09',NULL)$s$,'23505','link composite primary key');
UPDATE conversations SET status='ENDED',end_requested_at=scheduled_end_at,end_reason='MIDNIGHT',ended_at='2026-10-03 00:00:03+09',summary_status='PENDING',summary_started_at='2026-10-03 00:00:03+09',summary_deadline_at='2026-10-03 00:01:03+09' WHERE id=pg_temp.uid(41);
UPDATE conversation_session_links SET end_recovery_until='2026-10-03 00:10:03+09' WHERE session_security_id=pg_temp.uid(21) AND conversation_id=pg_temp.uid(41);
UPDATE conversation_session_links SET end_recovery_until='2026-10-03 00:10:03+09' WHERE session_security_id=pg_temp.uid(22) AND conversation_id=pg_temp.uid(41);
SELECT pg_temp.expect_true((SELECT count(*)=2 AND bool_and(l.end_recovery_until=c.ended_at+interval '600 seconds') FROM conversation_session_links l JOIN conversations c ON c.id=l.conversation_id WHERE c.id=pg_temp.uid(41)), 'multiple existing sessions hold fixed 600-second recovery');
SELECT pg_temp.expect_reject($s$UPDATE conversation_session_links SET end_recovery_until='infinity' WHERE session_security_id=pg_temp.uid(21)$s$,'23514','end recovery finite despite null login upper bound');
SELECT pg_temp.expect_reject($s$DELETE FROM session_security WHERE id=pg_temp.uid(22)$s$,'23503','link blocks session physical deletion');
SELECT pg_temp.expect_reject($s$UPDATE conversations SET summary_status='READY',summary_completed_at='2026-10-03 00:00:30+09',title='제목',topic='주제' WHERE id=pg_temp.uid(41)$s$,'23514','READY requires all three texts');
SELECT pg_temp.expect_reject($s$UPDATE conversations SET summary_deadline_at=summary_started_at WHERE id=pg_temp.uid(41)$s$,'23514','summary deadline after start');
SELECT pg_temp.expect_reject($s$UPDATE conversations SET summary_status='FAILED',summary_completed_at='2026-10-03 00:01:03+09',summary_error_code='' WHERE id=pg_temp.uid(41)$s$,'23514','summary error nonempty');
UPDATE conversations SET summary_status='READY',summary_completed_at='2026-10-03 00:00:30+09',title='가상 제목',topic='가상 주제',summary='허용된 가상 요약' WHERE id=pg_temp.uid(41);
SELECT pg_temp.expect_true((SELECT summary_status='READY' AND summary IS NOT NULL FROM conversations WHERE id=pg_temp.uid(41)), 'READY summary complete');
SELECT pg_temp.expect_reject($s$UPDATE conversations SET summary_completed_at=summary_deadline_at WHERE id=pg_temp.uid(41)$s$,'23514','READY cannot be recorded at deadline');
UPDATE conversations SET status='ENDED',ended_at='2026-10-02 10:05+09',summary_status='EMPTY' WHERE id=pg_temp.uid(42);
SELECT pg_temp.expect_true((SELECT summary_started_at IS NULL AND summary IS NULL FROM conversations WHERE id=pg_temp.uid(42)), 'EMPTY has no work or generated values');
SELECT pg_temp.expect_reject($s$INSERT INTO conversations(id,child_id,client_request_id,service_date,scheduled_end_at,status,summary_status,started_at) VALUES (pg_temp.uid(49),pg_temp.uid(11),pg_temp.uid(141),'2026-10-03','2026-10-04 00:00+09','ACTIVE','NOT_STARTED','2026-10-03 08:00+09')$s$,'23505','ended start request key cannot be reused');

SELECT pg_temp.expect_true((SELECT ended_at=end_requested_at AND ended_at<scheduled_end_at AND end_reason='MANUAL' FROM conversations WHERE id=pg_temp.uid(42)), 'MANUAL empty conversation can end before midnight');
INSERT INTO conversations(id,child_id,client_request_id,service_date,scheduled_end_at,status,summary_status,started_at)
  VALUES (pg_temp.uid(43),pg_temp.uid(11),pg_temp.uid(143),'2026-10-03','2026-10-04 00:00+09','ACTIVE','NOT_STARTED','2026-10-03 08:00+09');
SELECT pg_temp.expect_true((SELECT count(*)=2 FROM conversations WHERE child_id=pg_temp.uid(11)), 'next service date allowed once previous conversation ENDED');
INSERT INTO conversations(id,child_id,client_request_id,service_date,scheduled_end_at,status,summary_status,started_at,ended_at,end_requested_at,end_reason)
  VALUES (pg_temp.uid(44),pg_temp.uid(11),pg_temp.uid(144),'2026-09-30','2026-10-01 00:00+09','ENDED','EMPTY','2026-09-30 08:00+09','2026-10-01 00:00+09','2026-10-01 00:00+09','MIDNIGHT'),
         (pg_temp.uid(45),pg_temp.uid(11),pg_temp.uid(145),'2024-02-29','2024-03-01 00:00+09','ENDED','EMPTY','2024-02-29 08:00+09','2024-03-01 00:00+09','2024-03-01 00:00+09','MIDNIGHT');
SELECT pg_temp.expect_true((SELECT scheduled_end_at='2026-09-30 15:00Z'::timestamptz FROM conversations WHERE id=pg_temp.uid(44)), 'month-end KST midnight correct');
SELECT pg_temp.expect_true((SELECT scheduled_end_at='2024-02-29 15:00Z'::timestamptz FROM conversations WHERE id=pg_temp.uid(45)), 'leap-day KST midnight correct');

-- 같은날 EMPTY 종료 후 새 대화, 이전 요약 PENDING 중에도 또 새 대화를 시작한다.
INSERT INTO conversations(id,child_id,client_request_id,service_date,scheduled_end_at,status,summary_status,started_at)
  VALUES (pg_temp.uid(46),pg_temp.uid(12),pg_temp.uid(146),'2026-10-02','2026-10-03 00:00+09','ACTIVE','NOT_STARTED','2026-10-02 11:00+09');
SELECT pg_temp.expect_true((SELECT count(*)=2 FROM conversations WHERE child_id=pg_temp.uid(12) AND service_date='2026-10-02'), 'same-day new ID after EMPTY end');
INSERT INTO conversation_turns(id,conversation_id,client_request_id,request_hash,sequence,status,
  child_text_visibility,reply_text_visibility,created_at,processing_deadline_at)
  VALUES (pg_temp.uid(56),pg_temp.uid(46),pg_temp.uid(156),repeat('f',64),1,'PROCESSING','OMITTED','OMITTED','2026-10-02 11:00:05+09','2026-10-02 11:01:05+09');
UPDATE conversations SET status='CLOSING',end_requested_at='2026-10-02 11:00:10+09',end_reason='MANUAL' WHERE id=pg_temp.uid(46) AND status='ACTIVE';
WITH accepted AS (
  INSERT INTO conversation_turns(id,conversation_id,client_request_id,request_hash,sequence,status,
    child_text_visibility,reply_text_visibility,created_at,processing_deadline_at)
  SELECT pg_temp.uid(57),id,pg_temp.uid(157),repeat('a',64),2,'PROCESSING','OMITTED','OMITTED','2026-10-02 11:00:11+09'::timestamptz,'2026-10-02 11:01:11+09'::timestamptz
  FROM conversations WHERE id=pg_temp.uid(46) AND status='ACTIVE'
  RETURNING id)
SELECT pg_temp.expect_true((SELECT count(*)=0 FROM accepted), 'end-first admission predicate refuses new turn');
UPDATE conversation_turns SET status='SUCCEEDED',completed_at='2026-10-02 11:00:20+09',reply_text='허용된 응답',reply_text_visibility='VISIBLE'
  WHERE id=pg_temp.uid(56) AND status='PROCESSING' AND '2026-10-02 11:00:20+09'::timestamptz<processing_deadline_at;
SELECT pg_temp.expect_true((SELECT t.completed_at>c.end_requested_at AND t.completed_at<t.processing_deadline_at
  FROM conversation_turns t JOIN conversations c ON c.id=t.conversation_id WHERE t.id=pg_temp.uid(56)), 'accepted turn drains after manual boundary before original deadline');
WITH changed AS (UPDATE conversations SET end_requested_at=scheduled_end_at,end_reason='MIDNIGHT'
  WHERE id=pg_temp.uid(46) AND status='ACTIVE' RETURNING id)
SELECT pg_temp.expect_true((SELECT count(*)=0 FROM changed), 'repeat end or midnight predicate preserves first boundary');
UPDATE conversations SET status='ENDED',ended_at='2026-10-02 11:00:20+09',summary_status='PENDING',
  summary_started_at='2026-10-02 11:00:20+09',summary_deadline_at='2026-10-02 11:01:20+09' WHERE id=pg_temp.uid(46);
INSERT INTO conversations(id,child_id,client_request_id,service_date,scheduled_end_at,status,summary_status,started_at)
  VALUES (pg_temp.uid(47),pg_temp.uid(12),pg_temp.uid(147),'2026-10-02','2026-10-03 00:00+09','ACTIVE','NOT_STARTED','2026-10-02 11:00:21+09');
SELECT pg_temp.expect_true((SELECT status='ENDED' AND summary_status='PENDING' FROM conversations WHERE id=pg_temp.uid(46))
  AND (SELECT status='ACTIVE' FROM conversations WHERE id=pg_temp.uid(47)), 'new same-day conversation does not wait for previous summary');
SELECT pg_temp.expect_true((SELECT NOT EXISTS (SELECT 1 FROM conversation_turns WHERE conversation_id=c.id)
  AND summary_status='EMPTY' FROM conversations c WHERE id=pg_temp.uid(42)), 'zero-turn end remains EMPTY record; fixed greeting has no turn');

-- 복구 선별의 서비스 SQL을 가상 시각으로 검증한다. 실제 framework 로그인 검사는 별도다.
INSERT INTO session_security(id,account_id,session_version,created_at,revoked_at)
  VALUES (pg_temp.uid(25),pg_temp.uid(2),0,'2026-10-02 10:00+09',NULL),
         (pg_temp.uid(26),pg_temp.uid(2),0,'2026-10-02 10:00+09',NULL),
         (pg_temp.uid(27),pg_temp.uid(2),0,'2026-10-02 10:00+09',NULL),
         (pg_temp.uid(28),pg_temp.uid(2),0,'2026-10-02 10:00+09','2026-10-02 11:00:09+09');
INSERT INTO conversation_session_links(session_security_id,conversation_id,bound_at)
  VALUES (pg_temp.uid(25),pg_temp.uid(46),'2026-10-02 11:00:09+09'),
         (pg_temp.uid(26),pg_temp.uid(46),'2026-10-02 11:00:10+09'),
         (pg_temp.uid(27),pg_temp.uid(46),'2026-10-02 11:00:11+09'),
         (pg_temp.uid(28),pg_temp.uid(46),'2026-10-02 11:00:09+09');
UPDATE conversation_session_links l SET end_recovery_until=c.ended_at+interval '600 seconds'
  FROM conversations c JOIN children ch ON ch.id=c.child_id
  JOIN accounts a ON a.id=ch.account_id, session_security ss
  WHERE l.conversation_id=c.id AND l.session_security_id=ss.id AND c.id=pg_temp.uid(46)
    AND ss.account_id=a.id AND ss.session_version=a.session_version AND ss.revoked_at IS NULL
    AND (ss.expires_at IS NULL OR ss.expires_at>c.ended_at)
    AND l.bound_at<c.end_requested_at AND l.end_recovery_until IS NULL;
SELECT pg_temp.expect_true((SELECT count(*)=1 FROM conversation_session_links WHERE conversation_id=pg_temp.uid(46) AND end_recovery_until IS NOT NULL), 'strict pre-bound and valid-context recovery filter');
SELECT pg_temp.expect_true((SELECT end_recovery_until='2026-10-02 11:10:20+09'::timestamptz FROM conversation_session_links WHERE session_security_id=pg_temp.uid(25)), 'manual recovery uses actual endedAt plus 600 seconds');
SELECT pg_temp.expect_true((SELECT '2026-10-02 11:10:19.999999+09'::timestamptz<end_recovery_until
  AND NOT ('2026-10-02 11:10:20+09'::timestamptz<end_recovery_until) FROM conversation_session_links WHERE session_security_id=pg_temp.uid(25)), 'receipt recovery rejects exact 600-second boundary');
WITH changed AS (UPDATE conversation_session_links SET end_recovery_until='2026-10-02 11:11+09'
  WHERE session_security_id=pg_temp.uid(25) AND end_recovery_until IS NULL RETURNING session_security_id)
SELECT pg_temp.expect_true((SELECT count(*)=0 FROM changed), 'retry predicate cannot extend stored recovery');
SELECT pg_temp.expect_true(NOT EXISTS (SELECT 1 FROM conversation_session_links WHERE conversation_id=pg_temp.uid(46) AND session_security_id=pg_temp.uid(23)), 'new unbound login has no receipt recovery');

INSERT INTO turn_audio_assets(turn_id,storage_key,content_type,byte_size,status,created_at,expires_at)
  VALUES (pg_temp.uid(51),'fixture/internal-key','audio/wav',32,'AVAILABLE','2026-10-02 10:00:30+09','2026-10-02 10:05+09');
SELECT pg_temp.expect_reject($s$INSERT INTO turn_audio_assets SELECT * FROM turn_audio_assets WHERE turn_id=pg_temp.uid(51)$s$,'23505','one audio asset per turn');
SELECT pg_temp.expect_reject($s$UPDATE turn_audio_assets SET byte_size=0 WHERE turn_id=pg_temp.uid(51)$s$,'23514','audio size positive');
SELECT pg_temp.expect_reject($s$UPDATE turn_audio_assets SET expires_at=created_at WHERE turn_id=pg_temp.uid(51)$s$,'23514','audio TTL positive');
SELECT pg_temp.expect_reject($s$UPDATE turn_audio_assets SET status='DELETED' WHERE turn_id=pg_temp.uid(51)$s$,'23514','DELETED needs deletion time');
SELECT pg_temp.expect_reject($s$UPDATE turn_audio_assets SET deleted_at='2026-10-02 10:04+09' WHERE turn_id=pg_temp.uid(51)$s$,'23514','AVAILABLE deletion time null');
SELECT pg_temp.expect_reject($s$UPDATE turn_audio_assets SET expires_at='infinity' WHERE turn_id=pg_temp.uid(51)$s$,'23514','audio TTL finite');
UPDATE turn_audio_assets SET status='DELETE_PENDING' WHERE turn_id=pg_temp.uid(51);
UPDATE turn_audio_assets SET status='DELETED',deleted_at='2026-10-02 10:05+09' WHERE turn_id=pg_temp.uid(51);
SELECT pg_temp.expect_true((SELECT status='DELETED' AND deleted_at IS NOT NULL FROM turn_audio_assets WHERE turn_id=pg_temp.uid(51)), 'audio delete lifecycle');
SELECT pg_temp.expect_reject($s$DELETE FROM conversation_turns WHERE id=pg_temp.uid(51)$s$,'23503','audio prevents turn cascade');
SELECT pg_temp.expect_reject($s$DELETE FROM conversations WHERE id=pg_temp.uid(41)$s$,'23503','conversation deletion RESTRICT');
SELECT pg_temp.expect_reject($s$DELETE FROM children WHERE id=pg_temp.uid(11)$s$,'23503','child deletion RESTRICT');
SELECT pg_temp.expect_reject($s$DELETE FROM accounts WHERE id=pg_temp.uid(1)$s$,'23503','account deletion RESTRICT');

INSERT INTO SPRING_SESSION VALUES (pg_temp.uid(81)::text,pg_temp.uid(82)::text,0,0,60,60000,pg_temp.uid(1)::text);
INSERT INTO SPRING_SESSION_ATTRIBUTES VALUES (pg_temp.uid(81)::text,'fixture',decode('00','hex'));
SELECT pg_temp.expect_reject($s$INSERT INTO SPRING_SESSION SELECT pg_temp.uid(83)::text,SESSION_ID,CREATION_TIME,LAST_ACCESS_TIME,MAX_INACTIVE_INTERVAL,EXPIRY_TIME,PRINCIPAL_NAME FROM SPRING_SESSION$s$,'23505','native session ID unique');
SELECT pg_temp.expect_reject($s$UPDATE SPRING_SESSION SET EXPIRY_TIME=NULL$s$,'23502','native expiry remains not null');
DELETE FROM SPRING_SESSION WHERE PRIMARY_ID=pg_temp.uid(81)::text;
SELECT pg_temp.expect_true((SELECT count(*)=0 FROM SPRING_SESSION_ATTRIBUTES), 'framework attribute cascade only');
SELECT pg_temp.expect_true((SELECT count(*)=2 FROM accounts), 'framework delete leaves business accounts');

DO $$ BEGIN
  IF (SELECT count(*) FROM erd_audit_results)<>184 THEN RAISE EXCEPTION 'Unexpected test count'; END IF;
END $$;
\o
SELECT kind,count(*) AS passed FROM erd_audit_results GROUP BY kind ORDER BY kind;
SELECT 'PASS '||count(*)||' / 184' AS result FROM erd_audit_results;
