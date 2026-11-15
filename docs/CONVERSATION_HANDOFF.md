# 대화 시작·종료 공통 기반 구현 계약

SCRUM-86에서 구현한 대화 기반을 SCRUM-87/88/101/102/103이 재사용하기 위한 인계서다.
현재 구현과 후속 작업의 책임을 이 문서에서 함께 관리한다.

## 1. 기준과 구현 지도

- 코드 기준: `feature/SCRUM-86-conversation-start`, HEAD `10e0053a70f1f6bb3e1628dc56373c92da3ba131`.
- 검증일: 2026-11-11. 응답 직전 재검사와 415 수정까지 포함한다.
- 계약 원본: [대화 API §7.1·§8](p0/api/06-conversations.md), [JSON 스키마](p0/api/schemas/conversations.md), [공통 API](p0/api/01-common.md), [ERD §5·§8·§12.2](p0/Integrated_ERD_Design.md).
- 이 문서는 구현 사용법과 검증 범위를 기록한다. 통합 API/ERD v3.1이나 미확정 정책을 변경하지 않는다.

| 구현 위치 | 제공하는 계약 |
| --- | --- |
| [ConversationController](../src/main/java/com/dodamdodam/backend/conversation/api/ConversationController.java) | 시작 요청·공통 접근 검증·HTTP 상태/Location·응답 보호 표식 |
| [ConversationService](../src/main/java/com/dodamdodam/backend/conversation/application/ConversationService.java) | `create`, `verifyStartResponse`, 생성·멱등성·최종 업무 상태 검사 |
| [ConversationRepository](../src/main/java/com/dodamdodam/backend/conversation/infrastructure/ConversationRepository.java) | DB 시각·시작 키/미종료 조회·부분 UNIQUE 대응·대화/link 저장 |
| [ConversationStartResponseFilter](../src/main/java/com/dodamdodam/backend/conversation/api/ConversationStartResponseFilter.java) | 실제 전송 전 재검사·실패한 성공 응답 제거 |
| [ConversationClosureCoordinator](../src/main/java/com/dodamdodam/backend/conversation/application/ConversationClosureCoordinator.java) | 재사용 가능한 최초 종료 경계·drain·ENDED 확정 |
| [ConversationClosureRepository](../src/main/java/com/dodamdodam/backend/conversation/infrastructure/ConversationClosureRepository.java) | 조건부 timeout/종료 갱신·요약 입력 판정·복구 links |
| [V6 migration](../src/main/resources/db/migration/V6__create_conversation_tables.sql) | 공통 3개 테이블의 전체 v3.1 스키마 |

## 2. 대화 시작 API

`POST /api/v1/children/{childId}/conversations`

기존 로그인 쿠키와 `X-XSRF-TOKEN`을 사용하며, Content-Type은 `application/json`이다.
[StartConversationRequest](../src/main/java/com/dodamdodam/backend/conversation/dto/StartConversationRequest.java)는
`clientRequestId` UUID 문자열 하나만 받는다. 경로 childId도 완전한 36자리 UUID로 검사한다.
null·축약형·공백·타입 강제 변환·미정의 필드는 허용하지 않는다. 계정·보안 문맥은 서버에서 얻는다.

```json
{
  "clientRequestId": "22222222-2222-4222-8222-222222222222"
}
```

성공 응답은 [StartConversationResult](../src/main/java/com/dodamdodam/backend/conversation/dto/StartConversationResult.java)의 다섯 필드만 `data`로 감싼다.
DB 시각은 같은 순간을 KST `+09:00`, 초 정밀도로 변환해 공개하며 DB의 마이크로초 정밀도는 유지한다.
내부 `StartOutcome.created`와 인증·저장소 객체는 공개하지 않는다.

```json
{
  "data": {
    "conversationId": "33333333-3333-4333-8333-333333333333",
    "status": "ACTIVE",
    "startedAt": "2026-10-03T08:00:00+09:00",
    "serviceDate": "2026-10-03",
    "scheduledEndAt": "2026-10-04T00:00:00+09:00"
  }
}
```

### 상태·멱등성·오류

아래 업무 판정은 인증·구조·소유권 확인과 아이 잠금 이후에 수행한다.
필요한 종료 정리를 거친 뒤 이용시간을 먼저 검사하고, 기존 시작 키를 다른 미종료 대화보다 먼저 판단한다.

| 조건 | 응답 |
| --- | --- |
| 새 키, 미종료 없음 | 201, 새 conversationId와 현재 문맥 link 생성 |
| 동일 키, 당일 ACTIVE, 현재 문맥의 기존 link 있음 | 200, 최초 ID/시작/예정 종료 시각 그대로 반환 |
| 동일 키, 당일 ACTIVE, 현재 문맥 link 없음 | 403 CHILD_SESSION_REQUIRED. link를 자동 추가하지 않음 |
| 동일 키 CLOSING 또는 다른 키의 미종료 CLOSING | 409 CONVERSATION_CLOSING |
| 동일 키 ENDED | 409 CONVERSATION_ENDED. 이후 다른 ACTIVE가 생겨도 이 키로 새 행을 만들지 않음 |
| 다른 키 ACTIVE 존재 | 409 ACTIVE_CONVERSATION_EXISTS, `error.activeConversationId` 포함 |
| KST 00:00 이상 08:00 미만 | 403 SERVICE_HOURS_CLOSED. start의 키·상태·link 판정보다 우선 |

당일 ENDED 및 이전 요약 PENDING은 새 키의 시작을 막지 않는다. 화면 이탈·새로고침은 대화를 종료하거나 새로 생성하지 않는다.
현재 문맥이 이미 연결된 ACTIVE의 재전송만 복구하며, 새 로그인에서의 명시적 resume은 SCRUM-101의 책임이다.

201에만 `Location: /api/v1/children/{childId}/conversations/{conversationId}`를 추가한다.
200과 오류에는 Location을 추가하지 않는다. Location이나 ACTIVE 충돌 ID는 세션 연결·기록 조회 권한을 부여하지 않는다.
공통 오류는 `error.code/message/requestId/fields`, 검증 항목이 없으면 fields=`[]`이며,
ACTIVE 충돌에만 `activeConversationId`가 추가된다. requestId는 서버 `X-Request-ID`와 일치하고 `no-store`를 유지한다.

전송 형식 → CSRF → 로그인 → 구조 → 소유권 → 업무 판정 순서를 재사용한다.
비로그인 401, CSRF 누락/불일치 403, 미존재/타인 아이 404, 잘못된 JSON·UUID·추가 필드 400이다.
[Part2JsonRequestGuardFilter](../src/main/java/com/dodamdodam/backend/global/web/Part2JsonRequestGuardFilter.java)는 시작 POST에만 JSON·생략/UTF-8 charset을 허용하며 미지원/누락 형식은 415로 거부한다.
시작 컨트롤러의 MVC 미디어 타입 예외도 [전용 처리기](../src/main/java/com/dodamdodam/backend/conversation/api/ConversationMediaTypeExceptionHandler.java)가 415로 처리한다.
Part 1의 크기 제한을 Part 2에 적용하지 않는다. 미확정 413 및 공통 429의 상태는 §7에 구분한다.

### 응답 직전 재검사

1. Controller가 생성/재전송 성공 후 검증된 로그인 문맥·childId·시작 키·반환 대화 ID를 요청 표식으로 남긴다.
2. `ConversationStartResponseFilter`는 Security 위임 필터 order+1(기본 -99)에 한 번 등록된다. 보안 체인의 하위 실행 중이므로 현재 인증 문맥을 재사용한다.
3. 첫 전송/flush/Content-Length 충족 직전 및 미커밋 반환 시 현재 로그인을 먼저 확인하고 `ConversationService.verifyStartResponse(session, childId, clientRequestId, conversationId)`를 호출한다.
4. 생성과 분리된 업무 Tx에서 같은 상위 잠금 순서로 **해당 키와 반환 ID의 대화만** 다시 읽고, 필요한 종료 정리·현재 DB 시각·상태·link를 확인한다. 새 대화나 암묵적 link를 생성하지 않는다.
5. 실패하면 아직 전송하지 않은 성공 본문·Content-Length·Location·JSESSIONID 갱신 쿠키를 제거한다. requestId·no-store·CORS는 보존한다. 업무 오류는 기존 처리기로 403/404/409/500, 인증 무효/확인 장애는 기존 외곽 필터로 401/503 처리한다.

재검사 실패를 MVC가 일반 500으로 소비하지 않도록 내부 `ResponseRejected`를 전용 필터까지 전달한다.
이미 커밋된 생성 Tx는 응답 거부로 되돌리지 않는다. 원래 대화가 종료된 사이 새 ACTIVE가 생겨도 새 대화 정보를 대신 반환하지 않는다.
이 보호는 현재 시작 API의 동기 201/200에 한정된다. Part 1·다른 Part 2 경로·400 이상 오류 응답은 재검사하지 않는다.
후속 API가 자동으로 이 필터의 보호를 받는다고 가정하지 않는다. 이미 전달한 바이트의 회수나 DB와 네트워크의 원자 커밋을 보장하지 않는다.
필터 전체 순서는 [개발환경 §10.6](DEVELOPMENT_SETUP.md#106-필터-실행-순서와-변경-기준)을 따른다.

## 3. DB 스키마와 소유 범위

Flyway의 기존 V1~V5 이력을 유지하고 V6를 적용한다. V6의 직접 FK 참조는
[V2의 session_security](../src/main/resources/db/migration/V2__create_accounts_and_session_security.sql)와
[V4의 children](../src/main/resources/db/migration/V4__create_children.sql)이다. 두 선행 테이블은 V2의 accounts를 참조한다.
세 테이블의 DDL·인덱스를 통합 ERD §12.2와 주석/공백을 제외해 비교했으며 모두 일치했다.
전체 컬럼/SQL은 V6와 ERD를 기준으로 하며 적용된 migration을 수정하지 않는다.

| 테이블 | 현재 제공하는 키·제약 | 구현 책임 |
| --- | --- | --- |
| conversations, 18컬럼 | PK(id), children FK/RESTRICT. `uq_conversation_request(child_id, client_request_id)`. `uq_conversations_unended_child`: child_id WHERE ACTIVE/CLOSING. 일별 UNIQUE 없음 | SCRUM-86: 생성·시작 상태·공통 종료. 후속 Task는 같은 행의 수명주기 재사용 |
| conversation_turns, 14컬럼 | PK(id), conversations FK/RESTRICT. `(conversation_id, client_request_id)`와 `(conversation_id, sequence)` UNIQUE. `uq_turns_processing_conversation`: conversation_id WHERE PROCESSING | SCRUM-86: 전체 공통 스키마와 기존 PROCESSING 만료. SCRUM-88: 신규 발화 접수·저장·정상/실패 결과·허용 텍스트 |
| conversation_session_links, 4컬럼 | `(session_security_id, conversation_id)` 복합 PK, session_security/conversations FK 모두 RESTRICT. conversation_id 조회 인덱스. 다중 적격 문맥 허용 | SCRUM-86: 시작 link·종료 복구기한. SCRUM-101: 명시 resume 연결. SCRUM-103: 기존 연결의 현재 복구 권한 판정 |

`session_security_id`는 `AuthenticatedSession.securityContextId`다. JSESSIONID나 Spring Session의 native ID를 넣지 않는다.
bound_at은 최초 연결 시각이며 start 재전송으로 변경하지 않는다. 현재 start는 INSERT만 수행하고 resume UPSERT는 미구현이다.
후속 resume도 최초 bound_at을 보존하고 CLOSING/ENDED에 새 link를 만들지 않아야 한다.

| CHECK·서비스 제약 | 현재 구현 |
| --- | --- |
| conversations 이용시간 | 유한 service_date, 다음 KST 자정인 scheduled_end_at, 그날 08:00≤started_at<scheduled_end_at |
| conversations 상태 | ACTIVE의 종료 필드 NULL/NOT_STARTED, CLOSING의 경계·사유 필수/ended_at NULL, ENDED의 ended_at 필수 및 시각 순서 |
| 종료 경계 | MANUAL: started_at≤end_requested_at<scheduled_end_at. MIDNIGHT: end_requested_at=scheduled_end_at |
| 요약 상태 | NOT_STARTED/EMPTY/PENDING/READY/FAILED별 시각·오류·title/topic/summary의 NULL 조합. deadline>시작, 완료≥시작, READY 완료<deadline. 현재 READY/요약 FAILED 생성 서비스는 없음 |
| turns 요청·순번 | request_hash 소문자 hex64, sequence≥1, 시작 키와 별도인 발화 요청 키의 대화별 UNIQUE |
| turns visibility | OMITTED이면 텍스트 NULL, VISIBLE/REDACTED이면 nonnull. child/reply를 독립 검사 |
| turns 상태 | PROCESSING은 completed/error/두 텍스트 NULL 및 양쪽 OMITTED. SUCCEEDED는 completed 필수/error NULL, FAILED는 completed와 비어 있지 않은 error 필수 |
| turns 시각 | 유한 시각, processing_deadline_at>created_at, 완료≥created_at, SUCCEEDED 완료<deadline |
| links 시각 | bound_at과 nonnull end_recovery_until은 유한 시각. 소유 계정 일치·적격성·600초 산식은 서비스에서 검사 |

DB UNIQUE/FK/CHECK와 함께 권한·원래 deadline·허용 입력·모든 발화 terminal 등 교차 조건을 서비스가 보장한다.
`turn_audio_assets`는 이번 migration에 없으며 음성 자산/삭제는 후속 작업이다.
SCRUM-88은 기존 conversation_turns를 재사용하고 중복 CREATE migration이나 스키마 작업량을 추가하지 않는다.

## 4. 잠금·DB 시각·트랜잭션

기존 권한 잠금은 필요한 경우 `accounts → session_security → guardian_pins → email_verifications → PIN_FAILURE_ACCOUNT` 순이며 같은 종류의 다건은 ID 순이다.
현재 시작/응답 재검사는 `accounts → 현재 session_security → 소유 children → conversations → 필요한 turns/links` 순서를 사용한다.
아이 row의 SELECT FOR UPDATE로 같은 아이 시작을 직렬화한다. 조정기는 PROCESSING을 ID 순, links를 session_security_id 순으로 잠근다.

`ConversationRepository.currentTime()`은 `SELECT clock_timestamp()`를 실행한다.
아이/대화/link 잠금 대기 뒤 권한·시각을 다시 확인한다. 생성 직후 저장 중 자정 경과도 검사한다.
조정기는 대화 잠금 후, PROCESSING 잠금 후, link 잠금 후 종료 확정 전에 DB 시각을 다시 읽는다.
`now()`/CURRENT_TIMESTAMP, 클라이언트·AI 시각, JVM 시계로 이를 대체하지 않는다. endedAt은 DB 업무 판정 시각이며 물리 commit timestamp가 아니다.

| 호출·경계 | 반드시 유지할 동작 |
| --- | --- |
| `ConversationService.create(session, childId, request)` | HTTP 진입에서 외부 업무 Tx 없이 호출. 내부 TransactionTemplate으로 종료 정리·대화·현재 link를 처리하며 `StartOutcome(result, created)` 반환 |
| 생성 업무 Tx | ACTIVE/NOT_STARTED, 종료·요약 결과 필드 NULL로 생성. serviceDate는 시작 KST 날짜, scheduledEndAt은 다음 KST 자정. link 저장 실패·저장 중 자정 경과·저장 장애는 같은 Tx 전체 rollback |
| 정상 시작 거부 | 종료 정리 뒤 시간외/상태 거부는 StartDecision으로 정리를 커밋한 다음 403/409를 전달. 예외 변환 때문에 정리를 되돌리지 않음 |
| `verifyStartResponse(...)` | 생성 Tx 이후 응답 필터가 호출. 새 Tx로 반환 대상만 검사하고 필요한 정리 수행. 정상 종료 정리의 403/409는 정리 커밋 후 전달하며 생성 Tx는 보존 |
| 종료 조정 Tx | 최초 경계·기한 만료·ENDED·요약 접수·복구기한은 함께 성공/rollback. 내부 조정은 caller Tx에 REQUIRED로 참여 |
| DB 최종 안전장치 | insertActive의 ON CONFLICT는 미종료 부분 UNIQUE만 지정. 충돌 시 실제 현재 상태 확인. 무관한 UNIQUE/FK/CHECK·저장 장애를 일괄 409로 바꾸지 않음 |

현재 두 public 서비스 메서드의 HTTP 호출부에는 외부 업무 Tx가 없다. 임의로 REQUIRED 외부 Tx에 감싸면 정상 거부의 커밋 의미가 달라질 수 있다.
잠금 조회와 종료 저장소는 기존 업무 Tx가 필수(MANDATORY)다. insertActive/insertLink를 호출자 Tx 밖에서 개별 호출해 원자성을 잃지 않게 한다.
Spring Session 저장·쿠키 갱신·외부 AI/파일 작업은 업무 DB Tx와 별도다. 외부 I/O 중 업무 잠금을 유지하지 않는다.

## 5. 공통 종료 조정기 사용법

공개 진입점은 `ConversationClosureCoordinator.reconcile(UUID accountId, UUID childId, UUID conversationId, Trigger trigger)`다.
`@Transactional` REQUIRED이며 이미 존재하는 caller Tx에 참여한다.

| 입력·반환 | 현재 의미 |
| --- | --- |
| accountId/childId/conversationId | 신뢰된 내부 ID. 소유 아이와 대상 대화를 확인하며 잘못된 조합은 404 |
| OBSERVE | 예정 자정 전 ACTIVE는 유지. 자정에 도달한 ACTIVE 또는 기존 CLOSING을 정리 |
| REQUEST_END | 자정 전 최초 종료를 MANUAL로 접수. 자정 이상이면 MIDNIGHT. 호출 가능한 내부 기능이며 수동 HTTP API는 아직 없음 |
| ClosureResult.state | conversationId/status/scheduledEndAt/endRequestedAt/endReason/endedAt/summaryStatus의 내부 상태 |
| ClosureResult.endedNow | 최초 ENDED 전이 수행 여부. 외부 Tx 커밋 완료나 요약 AI 실행을 뜻하지 않음 |

사용자 경로의 로그인·CSRF·현재 연결 검사는 호출자가 선행해야 한다. 조정기가 이 검사를 대신하지 않는다.
독립 호출은 account → 소유 child → conversation → PROCESSING turns → links를 잠근다.
이미 같은 부모 잠금을 보유한 start 호출은 재사용한다. 반대로 conversation/turn만 먼저 잠근 뒤 조정기로 상위 잠금을 요청하면 역순 대기 위험이 있다.
후속 turn 완료·worker는 처음부터 정순서 Tx를 설계하거나 기존 Tx 종료 후 정순서의 별도 Tx에서 호출해야 한다.

### 종료 경계·PROCESSING·ENDED

1. 지난 날짜 ACTIVE를 발견하면 최초 경계가 없을 때 MIDNIGHT/end_requested_at=기존 scheduled_end_at으로 CLOSING 전이한다. 자정 전 REQUEST_END는 MANUAL/잠금 후 DB 시각이다.
2. 기존 MANUAL/MIDNIGHT 경계·사유는 재호출·자정 도달로 변경하지 않는다.
3. 실제 PROCESSING을 조회·잠그고 `now < processing_deadline_at`이면 원래 기한까지 CLOSING을 유지한다. start는 409로 막는다.
4. equality부터 `status='PROCESSING' AND completed_at IS NULL AND processing_deadline_at<=판정시각` 조건으로만 FAILED/AI_TIMEOUT 처리한다. 정상 완료된 terminal이나 원래 deadline을 덮어쓰지 않는다.
5. 모든 turn이 terminal이면 최초 ENDED에서 ended_at·PENDING/EMPTY·적격 links의 복구기한을 같은 Tx로 확정한다. 저장 실패는 모두 rollback한다.
6. ENDED 재호출은 기존 상태를 반환한다. 경계·ended_at·복구기한·요약 접수 시각을 연장하지 않는다. 정리 후 미종료가 없어야 새 키로 새 시작을 허용한다.

생산 호출부는 현재 start와 응답 직전 재검사다. timer/catch-up/sweep/turn 완료 자동 호출은 아직 없다.
OBSERVE는 자정 전 ACTIVE에서 즉시 반환하므로 일반 ACTIVE 발화 timeout worker를 대신하지 않는다.
일반 timeout을 위해 REQUEST_END를 사용해 대화를 수동 종료하지 않는다. 후속 발화 수명주기에서 조건부 만료와 조정 호출을 연동해야 한다.

### PENDING/EMPTY와 요약 실행 경계

허용 요약 입력은 SUCCEEDED/FAILED 중 적어도 한쪽 텍스트가 nonnull이고 해당 visibility가 VISIBLE/REDACTED인 발화다.
FAILED의 허용 텍스트도 포함하고 반대편 OMITTED는 배제 사유가 아니다. 행 수·성공 수만으로 판정하지 않는다.
FE 고정 첫 인사는 turn/발화 수/요약 입력으로 저장하지 않는다. 필드별 저장·표시 허용 및 REDACTED 처리는 SCRUM-88에서 구현한다.

| 입력 | 최초 ENDED에서 저장 |
| --- | --- |
| 빈 대화 또는 허용 입력 없음 | EMPTY. 요약 시각·오류·title/topic/summary 모두 NULL |
| 허용 입력 있음 | PENDING. summary_started_at=ended_at, summary_deadline_at=ended_at+명시 설정값. completed/error/세 텍스트는 NULL |

[ConversationClosureProperties](../src/main/java/com/dodamdodam/backend/conversation/infrastructure/ConversationClosureProperties.java)는
`app.conversation.summary-processing-timeout` / `SUMMARY_PROCESSING_TIMEOUT`을 기본값 없이 요구한다.
누락·0·음수·마이크로초보다 세밀한 정밀도를 거부한다. 테스트 60초는 운영 정책이 아니다.
운영 수치와 AI 자료는 D13/D14 대기이며 [환경변수 설정](DEVELOPMENT_SETUP.md#6-환경변수와-비밀값)을 따른다.

실제 요약 AI 호출·READY/FAILED 결과 저장·요약 timeout/sweep는 미구현이다.
후속 요약 구현은 최초 전이 **커밋 이후** 실행 시도 계약을 start와 응답 재검사 경유 종료에도 연결해야 한다.
현재 create/verifyStartResponse는 ClosureResult로 AI 작업을 예약하지 않으므로 end/timer만 연결하면 이 경로의 PENDING을 빠뜨릴 수 있다.
외부 AI 호출의 exactly-once나 crash 후 자동 재시도를 현재 보장하지 않는다.

### 적격 links와 600초 복구

최초 ENDED 시 같은 아이 소유 계정, session_version 일치, 미폐기, 미만료인 session_security의 기존 links 중
`bound_at < 최초 end_requested_at`에만 `end_recovery_until = ended_at + 600초`를 저장한다.
경계 equality·이후 연결·타인 계정·폐기·만료·버전 불일치는 제외한다. 적격 연결은 여러 개이며 승자 1개로 제한하지 않는다.
기존 nonnull 기한과 ENDED 재호출은 갱신하지 않는다. 문맥 만료 시각에 맞춰 저장 기한을 새로 계산하거나 연장하지 않는다.

저장된 복구기한만으로 현재 접근 권한이 생기는 것은 아니다. SCRUM-103은 현재 framework 로그인·소유권·문맥·기존 link를 재검사하고
`now < end_recovery_until`만 허용해야 한다. equality부터 403, 무효 로그인은 401이다.
미래 공개 응답은 CLOSING 202의 conversationId/status 두 필드와 Retry-After≥1,
ENDED 200의 conversationId/status/endedAt/summaryStatus 네 필드만이다. 내부 ClosureState 전체나 발화·음성·요약 본문을 반환하지 않는다.
이 HTTP 접근 판정·receipt는 현재 미구현이다.

## 6. 후속 Task별 구현 책임

| Task | 현재 재사용할 것 | 후속 구현·인수 대상 |
| --- | --- | --- |
| [SCRUM-87](https://dodamdodam.atlassian.net/browse/SCRUM-87) | 대화 상태·현재 문맥 연결·인증/소유권·정순서 잠금 | 음성 업로드·AI 요청, 접수 커밋 후 잠금 밖 외부 호출, 음성 허용/자산/삭제 연동. 실제 AI wire·용량/시간 수치 확정 |
| [SCRUM-88](https://dodamdodam.atlassian.net/browse/SCRUM-88) | V6 전체 conversation_turns, 상태/visibility/원래 deadline/부분 UNIQUE | 신규 접수·원본 audio 바이트 SHA-256·대화 잠금 아래 sequence 부여·정상/실패 조건부 저장·허용 텍스트 처리. 공통 스키마 중복 생성·중복 작업량 산정 금지 |
| [SCRUM-101](https://dodamdodam.atlassian.net/browse/SCRUM-101) | ACTIVE/당일/이용시간·현재 보안 문맥·links PK | 명시 resume, 기존 허용 turns 복원·현재 문맥 연결 및 최초 bound_at 보존. CLOSING/ENDED 새 연결 차단 |
| [SCRUM-102](https://dodamdodam.atlassian.net/browse/SCRUM-102) | reconcile/REQUEST_END·최초 경계·원래 deadline drain·원자적 ENDED | 수동 /end API, 발화 접수와 같은 대화 잠금/CAS 경합, timer 등 실제 조정 호출. 응답·음성 삭제·후속 작업 연동 |
| [SCRUM-103](https://dodamdodam.atlassian.net/browse/SCRUM-103) | 기존 적격 links의 고정 end_recovery_until·endedAt | 종료 확인 권한·600초 equality·내용 없는 receipt·응답 유실 복구·현재 로그인 무효화 우선·기한 불변 테스트 |

API별 전송·인증·응답 보호를 직접 확인해야 한다. start의 전용 guard/응답 필터가 후속 경로에 자동 적용되는 것은 아니다.
후속 작업이 스키마나 종료 규칙을 확장하면 이 인계서를 함께 갱신하고 API/ERD와의 일치 여부를 확인한다.

## 7. 구현 상태와 남은 의존성

| 구분 | 코드 기준 현재 상태 |
| --- | --- |
| 구현 완료 | 3개 공통 스키마, 시작 입력/접근/시간/멱등성/동시성/원자성, 415, 응답 직전 보호, 호출 가능한 공통 종료 최소 기반 |
| 부분 구현 | 대화 수명주기는 공통 조정기·PENDING/EMPTY·복구기한 기록까지. 정상 turn 결과·요약 실행·HTTP 종료/복구 접근·자동 실행자는 아직 없음 |
| 후속 API·worker 미구현 | 음성/발화 접수·정상 AI 결과·resume·/end·receipt·timer/catch-up/sweep·요약 실행/timeout·음성 자산 삭제 |
| Part 2 413 | 명세에는 REQUEST_TOO_LARGE가 있으나 JSON/음성 크기 정책 미확정으로 현재 크기 검사는 없음. 승인된 수치·범위 확정 후 별도 연결, Part 1 16 KiB를 복사하지 않음 |
| 공통 429 | 현재 시작 경로에 API_IP/auth_rate_limits 요청량 예약 실행부가 없음. 공통 담당 범위·연계 기준 확인 후 429/Retry-After/별도 예약 커밋을 검증해야 함 |
| 실제 아이 등록·AI·운영 | 기존 캐릭터 목록 연결, 실제 AI 자료·운영 timeout/OAuth/HTTPS 등은 별도 기존 범위. 합성 fixture 테스트가 신규 사용자부터 전체 운영 흐름의 인수를 대신하지 않음 |

413/429나 후속 API·worker를 SCRUM-86에서 이미 완료한 기능으로 기록하지 않는다.
이 의존성의 담당·완료 범위는 명시적으로 인계하며 정책·Part 1 코드를 문서 작업에서 임의 변경하지 않는다.

## 8. 이번 실행 검증과 완료 기준

코드 HEAD `10e0053`에서 2026-11-11 아래 관련 테스트를 실행했다. JDK 25 및 기존 PostgreSQL `postgres:18.6-alpine` Testcontainers 구성을 사용했다.

```powershell
.\gradlew.bat test --tests 'com.dodamdodam.backend.conversation.*' --tests '*Part2JsonRequestGuardFilterTests' --tests '*Part1JsonRequestGuardTests' --tests '*CsrfLifecycleContractTests' --tests '*SecurityConfigTests' --tests '*CommonApiErrorContractTests' --tests '*LogoutContractTests' --tests '*ChildRegistrationContractTests' --no-daemon --no-build-cache --console=plain
```

**BUILD SUCCESSFUL: 17 suites / 572 tests / failures 0 / errors 0 / skipped 0**.
이번 문서 작업에서는 위 관련 검증을 재실행했으며 전체 clean check를 재실행한 결과로 확대하지 않는다.

테스트 파일은 아래에서 바로 찾을 수 있다. 개수는 이번 XML 결과 기준이다.

| 테스트 위치 | 개수 | 증명 범위 |
| --- | ---: | --- |
| [ConversationSchemaTests](../src/test/java/com/dodamdodam/backend/conversation/infrastructure/ConversationSchemaTests.java) | 170 | 깨끗한 스키마 Flyway·재실행·V5 업그레이드·실제 FK/CHECK/UNIQUE·같은 날 ENDED 다건 |
| [ConversationCreationContractTests](../src/test/java/com/dodamdodam/backend/conversation/api/ConversationCreationContractTests.java) | 70 | 실제 시작 계약·행 수·Location·HTTP 동시 요청·응답 유실 재요청·커밋 뒤 인증/시간/상태/연결/소유권/저장소 경합. 실제 Tomcat/HttpClient 자정·종료 검사 2개 포함 |
| [ConversationCreationTests](../src/test/java/com/dodamdodam/backend/conversation/application/ConversationCreationTests.java) | 23 | 생성/link rollback·KST·아이 잠금·대기 뒤 시간/상태·부분 UNIQUE·무관한 저장 오류 |
| [ConversationClosureCoordinatorTests](../src/test/java/com/dodamdodam/backend/conversation/application/ConversationClosureCoordinatorTests.java) | 27 | deadline equality·허용 FAILED 입력·경계/terminal 불변·다중 links·동시 조정·rollback·종료 후 재시작 |
| [ConversationStartResponseFilterTests](../src/test/java/com/dodamdodam/backend/conversation/api/ConversationStartResponseFilterTests.java) | 22 | stream/Writer/flush/미커밋 반환·성공 제거·기존 오류/다른 경로 보존·401/503 위임·필터 순서 |
| [ConversationStartContractTests](../src/test/java/com/dodamdodam/backend/conversation/api/ConversationStartContractTests.java) | 32 | 입력 검증 probe의 공통 보안·DTO 계약. 실제 저장 근거는 생성 테스트에서 확인 |
| [StartConversationDtoTests](../src/test/java/com/dodamdodam/backend/conversation/dto/StartConversationDtoTests.java) | 47 | UUID·필수/추가 필드·공개 JSON 필드/정밀도 |
| [ConversationServiceHoursTests](../src/test/java/com/dodamdodam/backend/conversation/domain/ConversationServiceHoursTests.java) | 14 | KST 08:00 포함·자정 제외·날짜 경계 |
| [ConversationClosurePropertiesTests](../src/test/java/com/dodamdodam/backend/conversation/infrastructure/ConversationClosurePropertiesTests.java) | 8 | 기본값 없는 설정·양수/정밀도 |
| [Part2JsonRequestGuardFilterTests](../src/test/java/com/dodamdodam/backend/global/web/Part2JsonRequestGuardFilterTests.java) | 23 | JSON/UTF-8·415·본문 미소비·경로 격리·Part 1 크기 제한 미전용 |
| [ConversationMediaTypeExceptionHandlerTests](../src/test/java/com/dodamdodam/backend/conversation/api/ConversationMediaTypeExceptionHandlerTests.java) | 4 | MVC 미디어 타입 415·다른 컨트롤러 처리 보존 |
| 관련 기존 보안/자녀 테스트 6 suites | 132 | Part1JsonRequestGuard 33, CsrfLifecycle 7, SecurityConfig 13, CommonApiErrorContract 14, Logout 33, ChildRegistrationContract 32 |

동시성은 독립 Tx·Executor/latch·실제 pg_blocking_pids로 확인한다. 일부 시간 경계는 DB 시각 조회 반환값을 고정/이동한다.
실제 DB 시계 테스트는 한 Tx 안의 clock_timestamp 증가와 CURRENT_TIMESTAMP 고정을 별도로 확인한다.
기존 pg_sleep 두 곳은 실제 문맥 만료 도달과 20ms DB 시계 진행용이다. 실제 브라우저 여러 탭·다중 서버·AI·운영 네트워크 유실은 미검증이다.

| SCRUM-86 완료 기준 | 구현 위치 | 관련 테스트 | 충족 여부 |
| --- | --- | --- | --- |
| 공통 3개 테이블·FK/UNIQUE/CHECK·migration 재현성 | V6, 기존 V2/V4 FK | ConversationSchemaTests | 충족 |
| 로그인·CSRF·소유권·UUID/추가 필드·KST | Controller, Request DTO, ServiceHours, 기존 공통 서비스/필터 | Start/CreationContract, DTO, ServiceHours | 충족 |
| 새 키 201·동일 키 연결 ACTIVE 200·미연결 403 | Service.create/replay, Repository, Controller | CreationContract/CreationTests | 충족 |
| 다른 키 ACTIVE의 409+ID·CLOSING/ENDED 키 충돌 | Service.conflictFor, ConversationExceptionHandler, ActiveConversationExistsError | CreationContract | 충족 |
| 이전 날짜 정리·원래 deadline의 CLOSING 유지 | Service/Coordinator.reconcile | ClosureCoordinatorTests, CreationContract | 충족 |
| 당일 ENDED·요약 PENDING 뒤 새 시작 | Service, 미종료 부분 UNIQUE | Schema/Closure/CreationContract | 충족 |
| 두 탭·같은 키 응답 유실·잠금 대기 재판정·원자성 | Service/Repository/기존 Child row lock | CreationTests, CreationContract | 충족 |
| 응답 직전 로그인·시간·상태·link 재검사와 성공 제거 | ResponseFilter, Service.verifyStartResponse, Controller 표식 | ResponseFilterTests, CreationContract의 delayedResponseRechecksCommittedConversationBeforeSending/realHttpResponseDiscardsDelayedSuccessWithoutTruncatingError | 충족 |
| 최초 MANUAL/MIDNIGHT·조건부 AI_TIMEOUT·ENDED·PENDING/EMPTY·600초 기록 | ClosureCoordinator/ClosureRepository/ClosureProperties | ClosureCoordinatorTests, ClosurePropertiesTests | 충족: 공통 최소 기반 |
| 후속 Task 구현 계약 문서 | 본 인계서, README, 개발환경 문서 연결 | 로컬 링크·내용·결과 개수 대조 | 충족 |
| PROCESSING 중 resume·실제 /end 경합·내용 없는 종료 확인과 600초 HTTP 접근 | SCRUM-101/102/103 및 87/88 연동 대상 | 해당 API 인수 테스트는 후속 필요 | 상세 API 미구현: 후속 범위 |
| 실제 AI/요약·음성 자산·자동 worker | 후속 구현 대상 | 후속 통합 검증 필요 | 미구현: 후속 범위 |
| 413·429 전체 공통 계약 | §7의 미확정/미연결 의존성 | 해당 경계 검증 미완료 | 미완료: 범위/정책·공통 연계 확인 필요 |
| 필수 CI·리뷰·PR 병합·development 반영·Jira Done | 팀 실행 절차 | 이번 문서 작업에서 원격 상태 확인/변경하지 않음 | 미검증 |

SCRUM-86의 구현·로컬 검증 충족과 Jira Task의 최종 Done은 구분한다.
후속 API·공통 의존성의 책임과 인수 범위를 명시하고, 최종 완료는 [팀 실행 규칙](AGENT_WORKFLOW.md#3-상태-전환)의 실제 CI·리뷰·병합·Jira 결과로 확인한다.
