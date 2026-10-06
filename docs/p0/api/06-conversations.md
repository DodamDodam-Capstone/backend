# 홈·대화 시작·이어하기·종료

[전체 API 목차](../Integrated_API_Spec.md) · **담당: Part 2** · 계약 v3.1 / 문서 분할 2026-10-06

절 번호는 기존 통합 명세의 번호를 유지합니다. 다른 절은 전체 목차에서 찾을 수 있습니다.

## 이 문서에서 찾기

- [4.10 StartConversationRequest](#section-4-10)
- [7.1 대화 시작](#section-7-1)
- [7.2 아이 홈 조회](#section-7-2)
- [7.3 활성 대화 이어하기](#section-7-3)
- [7.7 이야기 마치기 및 종료 결과 재확인](#section-7-7)
- [8 수동·자정 종료와 같은 날 재시작 규칙](#section-8)
- [8.1 시간 판정과 상태별 접근](#section-8-1)
- [8.2 생성·재진입·시작 키](#section-8-2)
- [8.3 종료 접수·기존 발화 정리·요약](#section-8-3)
- [8.4 종료 확인 복구와 음성](#section-8-4)

<a id="section-4-10"></a>
### 4.10 StartConversationRequest

[StartConversationRequest](schemas/conversations.md#schema-startconversationrequest) · `application/json`

아이별 시작 중복 식별. 같은 키 재전송은 ACTIVE·현재 연결에 한해 기존 시작 정보. ENDED 키 재사용 금지.

| 필드 | 필수 | 타입·검증 |
| --- | --- | --- |
| clientRequestId | 예 | type="string"; format="uuid"  |

<a id="section-7-1"></a>
<a id="operation-startchildconversation"></a>
### 7.1 대화 시작

`POST /api/v1/children/{childId}/conversations` · `startChildConversation` · **담당 Part 2** · B-02 · 권한 G+O+X+시간

로그인·소유권 후 아이 잠금 아래 서비스 시간08\~24를 먼저 검사하고 기존 시작 키를 확인한다. 기존 키 ENDED는409 CONVERSATION_ENDED, CLOSING은409 CONVERSATION_CLOSING. 기존 키 당일 ACTIVE/현재 연결은200, 미연결은403 CHILD_SESSION_REQUIRED 후 명시 resume. 이전 날짜 ACTIVE는 MIDNIGHT 경계를 고정하고 공통 조정기로 정리; CLOSING이 남으면409, 종료된 같은 키면 CONVERSATION_ENDED. 다른 키로 당일 ACTIVE가 있으면409 ACTIVE_CONVERSATION_EXISTS+ID. CLOSING이 있으면409 CONVERSATION_CLOSING. 이전 날짜 포함 미종료가 없으면 같은 날 ENDED가 있어도 새 clientRequestId로 새 conversationId 생성201. 재전송은 기존 키 유지. serviceDate는 시작 KST 날짜, scheduledEndAt은 다음00시. 화면 이탈은 새 대화 생성 사유가 아니다.

| 위치 | 필드 | 필수 | 타입·기본값·검증 |
| --- | --- | --- | --- |
| path | childId | 예 | type="string"; format="uuid" 필수 UUID. null/형식 오류400. 서버에서 로그인 계정 소유권과 부모-자식 리소스 연결 검사. |
| header | X-XSRF-TOKEN | 예 | type="string"; minLength=1 로그인 쿠키와 함께 모든 POST에 전송. 누락/불일치403 CSRF_INVALID. GET은 변경용 CSRF 검사 없음. |

요청 body: `application/json` [StartConversationRequest](schemas/conversations.md#schema-startconversationrequest)

| HTTP | 본문·오류 코드 |
| --- | --- |
| 400 | VALIDATION_FAILED · VALIDATION_FAILED; 발생 조건 및 재시도는 operation 설명과 기능 명세 참조. |
| 401 | AUTHENTICATION_REQUIRED · AUTHENTICATION_REQUIRED; 발생 조건 및 재시도는 operation 설명과 기능 명세 참조. |
| 404 | RESOURCE_NOT_FOUND · RESOURCE_NOT_FOUND; 발생 조건 및 재시도는 operation 설명과 기능 명세 참조. |
| 429 | RATE_LIMITED · RATE_LIMITED; 발생 조건 및 재시도는 operation 설명과 기능 명세 참조. |
| 503 | AUTH_STATE_UNAVAILABLE · AUTH_STATE_UNAVAILABLE; 발생 조건 및 재시도는 operation 설명과 기능 명세 참조. |
| 403 | CHILD_SESSION_REQUIRED, CSRF_INVALID, SERVICE_HOURS_CLOSED · SERVICE_HOURS_CLOSED / CHILD_SESSION_REQUIRED / CSRF_INVALID |
| 409 | ACTIVE_CONVERSATION_EXISTS, CONVERSATION_CLOSING, CONVERSATION_ENDED · 기존 시작 키 ENDED, 미종료 CLOSING 또는 다른 키 ACTIVE 경합. |
| 415 | UNSUPPORTED_MEDIA_TYPE · UNSUPPORTED_MEDIA_TYPE; 발생 조건 및 재시도는 operation 설명과 기능 명세 참조. |
| 201 | [StartConversationResultResponse](schemas/conversations.md#schema-startconversationresultresponse) · 최초 생성 후201, 과거 발화 없음. |
| 200 | [StartConversationResultResponse](schemas/conversations.md#schema-startconversationresultresponse) · 연결된 ACTIVE의 같은 키 복구200. |
| 413 | REQUEST_TOO_LARGE · REQUEST_TOO_LARGE |

응답 추가 헤더: 429 `Retry-After`: 최소 재시도 대기 초. 실제 남은 차단시간으로 계산.; 201 `Location`: /api/v1/children/{childId}/conversations/{conversationId}

트랜잭션·재시도: children 잠금으로 생성 직렬화. UNIQUE(child_id,client_request_id) 유지, UNIQUE(child_id,service_date) 제거, child_id WHERE status IN (ACTIVE,CLOSING) 부분 UNIQUE. 이전 날짜 미종료 정리 전 신규 생성 금지. 대화와 현재 세션 link 한 Tx. 응답 직전 로그인/시간/상태/연결 재검사. Location은 PIN 기록 조회 권한이 아니다.

DB 기본 업무 읽기: `accounts, session_security, children, conversations, conversation_session_links`; 기본 업무 쓰기: `conversations, conversation_session_links`.

조건부·후속 DB 작업: x-tables-write는 기본 업무 쓰기 목록이며 아래 공통 조정기/후속 worker의 조건부 쓰기는 별도다. 모든 요청의 auth_rate_limits 예산 예약과 SPRING_SESSION/SPRING_SESSION_ATTRIBUTES 프레임워크 저장은 별도 공통 처리이며 단일 업무 Tx와의 원자성을 가정하지 않는다. 기본 생성 Tx는 conversations+conversation_session_links, 생성 전 지난 날짜 미종료 정리는 공통 조정기를 경유한다. 공통 종료·기한 조정기/후속 worker의 조건부 쓰기: conversations(최초 종료 경계·CLOSING/ENDED·요약 상태/결과), conversation_turns(기존 PROCESSING의 만료 FAILED 또는 기한 내 허용 terminal 결과), conversation_session_links(적격 기존 연결의 종료 복구기한), turn_audio_assets(수동 종료 접수/자정/만료의 DELETE_PENDING 및 파일 삭제 확인 후 DELETED). 요청이 이 작업을 필요로 할 때만 실행하며 네 테이블을 매번 같은 Tx로 쓴다는 뜻은 아니다. 요약/AI/파일 삭제는 잠금 밖 후속 작업이고, CLOSING에서 늦게 생성된 음성도 아이에게 노출하지 않고 삭제 대상으로 정리한다.

201 응답 예시 (CREATED):

<!-- json-example: StartConversationResultResponse -->
```json
{
  "data": {
    "conversationId": "33333333-3333-4333-8333-333333333333",
    "status": "ACTIVE",
    "startedAt": "2026-10-02T13:00:00+09:00",
    "serviceDate": "2026-10-02",
    "scheduledEndAt": "2026-10-03T00:00:00+09:00"
  }
}
```

200 응답 예시 (REPLAY):

<!-- json-example: StartConversationResultResponse -->
```json
{
  "data": {
    "conversationId": "33333333-3333-4333-8333-333333333333",
    "status": "ACTIVE",
    "startedAt": "2026-10-02T13:00:00+09:00",
    "serviceDate": "2026-10-02",
    "scheduledEndAt": "2026-10-03T00:00:00+09:00"
  }
}
```

<a id="section-7-2"></a>
<a id="operation-getchildhome"></a>
### 7.2 아이 홈 조회

`GET /api/v1/children/{childId}/home` · `getChildHome` · **담당 Part 2** · B-01 · 권한 G+O

TALK은 availability.canEnter에 따라 AVAILABLE/UNAVAILABLE, PLAY는 COMING_SOON. TALK,PLAY 순서 각1개. activeConversationId는 접근 가능한 당일 ACTIVE만, CLOSING은 null. 서비스 시간 안 CLOSING이면 false/CONVERSATION_CLOSING, 시간 밖 OUTSIDE_SERVICE_HOURS 우선. 새 closingConversationId 필드 없음. Home은 로그인/소유권 통과 시 시간 밖에도200; 과거 내용·음성 없음. joinedAt=아이 등록 시각, daysTogether=KST 등록일 포함. name/nickname 별도이며 아이 화면과 greeting은 nickname 사용. 새 로그인은 종료 복구권한을 얻지 못하므로 Home 재조회로 현재 진입 가능 상태를 확인한다. 시간 밖에는 종료 완료를 추정하지 않고 시간외 안내 후 opensAt부터 재확인한다.

| 위치 | 필드 | 필수 | 타입·기본값·검증 |
| --- | --- | --- | --- |
| path | childId | 예 | type="string"; format="uuid" 필수 UUID. null/형식 오류400. 서버에서 로그인 계정 소유권과 부모-자식 리소스 연결 검사. |

요청 body: 없음.

| HTTP | 본문·오류 코드 |
| --- | --- |
| 400 | VALIDATION_FAILED · VALIDATION_FAILED; 발생 조건 및 재시도는 operation 설명과 기능 명세 참조. |
| 401 | AUTHENTICATION_REQUIRED · AUTHENTICATION_REQUIRED; 발생 조건 및 재시도는 operation 설명과 기능 명세 참조. |
| 404 | RESOURCE_NOT_FOUND · RESOURCE_NOT_FOUND; 발생 조건 및 재시도는 operation 설명과 기능 명세 참조. |
| 429 | RATE_LIMITED · RATE_LIMITED; 발생 조건 및 재시도는 operation 설명과 기능 명세 참조. |
| 503 | AUTH_STATE_UNAVAILABLE · AUTH_STATE_UNAVAILABLE; 발생 조건 및 재시도는 operation 설명과 기능 명세 참조. |
| 200 | [ChildHomeResponse](schemas/conversations.md#schema-childhomeresponse) · Home 조회 성공. 활성 대화가 없으면 null. |

응답 추가 헤더: 429 `Retry-After`: 최소 재시도 대기 초. 실제 남은 차단시간으로 계산.

트랜잭션·재시도: 조회 시 이전 날짜 미종료 행을 공통 종료 조정기로 정리. 만료 PROCESSING은 조건부 FAILED; 원래 기한 안이면 CLOSING 유지. 배치 지연이 시간 밖 내용 접근을 허용하지 않는다. 응답 시 현재 시간/상태/권한 재검사.

DB 기본 업무 읽기: `accounts, session_security, children, conversations`; 기본 업무 쓰기: ``.

조건부·후속 DB 작업: x-tables-write는 기본 업무 쓰기 목록이며 아래 공통 조정기/후속 worker의 조건부 쓰기는 별도다. 모든 요청의 auth_rate_limits 예산 예약과 SPRING_SESSION/SPRING_SESSION_ATTRIBUTES 프레임워크 저장은 별도 공통 처리이며 단일 업무 Tx와의 원자성을 가정하지 않는다. 기본 Home projection은 읽기 전용이다. 이전 날짜 미종료/기한 만료를 발견했을 때의 조건부 정리는 공통 조정기를 경유하므로 요청 전체가 항상 무쓰기라는 뜻은 아니다. 공통 종료·기한 조정기/후속 worker의 조건부 쓰기: conversations(최초 종료 경계·CLOSING/ENDED·요약 상태/결과), conversation_turns(기존 PROCESSING의 만료 FAILED 또는 기한 내 허용 terminal 결과), conversation_session_links(적격 기존 연결의 종료 복구기한), turn_audio_assets(수동 종료 접수/자정/만료의 DELETE_PENDING 및 파일 삭제 확인 후 DELETED). 요청이 이 작업을 필요로 할 때만 실행하며 네 테이블을 매번 같은 Tx로 쓴다는 뜻은 아니다. 요약/AI/파일 삭제는 잠금 밖 후속 작업이고, CLOSING에서 늦게 생성된 음성도 아이에게 노출하지 않고 삭제 대상으로 정리한다.

200 응답 예시 (NO_ACTIVE):

<!-- json-example: ChildHomeResponse -->
```json
{
  "data": {
    "child": {
      "childId": "22222222-2222-4222-8222-222222222222",
      "name": "도담친구",
      "characterId": "dodam",
      "nickname": "도담이"
    },
    "greeting": "도담이, 오늘도 반가워!",
    "joinedAt": "2026-10-02T09:00:00+09:00",
    "daysTogether": 1,
    "activeConversationId": null,
    "menus": [
      {
        "code": "TALK",
        "status": "AVAILABLE"
      },
      {
        "code": "PLAY",
        "status": "COMING_SOON"
      }
    ],
    "availability": {
      "timeZone": "Asia/Seoul",
      "serverTime": "2026-10-02T13:00:00+09:00",
      "serviceDate": "2026-10-02",
      "opensAt": "2026-10-02T08:00:00+09:00",
      "closesAt": "2026-10-03T00:00:00+09:00",
      "canEnter": true,
      "reason": null,
      "nextOpensAt": null
    }
  }
}
```

나머지 1개 상태 예시는 OpenAPI의 이 operation에 함께 제공한다.

<a id="section-7-3"></a>
<a id="operation-resumechildconversation"></a>
### 7.3 활성 대화 이어하기

`POST /api/v1/children/{childId}/conversations/{conversationId}/resume` · `resumeChildConversation` · **담당 Part 2** · B-02 · 권한 G+O+X+시간

서비스 시간 안 당일 ACTIVE만 명시 연결하고 전체 허용 발화를 sequence ASC 반환. 화면 이탈·새로고침·재로그인 복원. PROCESSING 중 가능. CLOSING/ENDED는 새 연결과 본문 반환 금지. 페이지화/절단 없음. audio/추천/FE 첫 인사는 발화 목록에 없음. body0바이트. CLOSING은409 CONVERSATION_CLOSING, ENDED409, 시간 밖403의 기존 우선순위 유지. 최초 bound_at 보존.

| 위치 | 필드 | 필수 | 타입·기본값·검증 |
| --- | --- | --- | --- |
| path | childId | 예 | type="string"; format="uuid" 필수 UUID. null/형식 오류400. 서버에서 로그인 계정 소유권과 부모-자식 리소스 연결 검사. |
| path | conversationId | 예 | type="string"; format="uuid" 필수 UUID. null/형식 오류400. 서버에서 로그인 계정 소유권과 부모-자식 리소스 연결 검사. |
| header | X-XSRF-TOKEN | 예 | type="string"; minLength=1 로그인 쿠키와 함께 모든 POST에 전송. 누락/불일치403 CSRF_INVALID. GET은 변경용 CSRF 검사 없음. |

요청 body: 없음. POST는 실제0바이트.

| HTTP | 본문·오류 코드 |
| --- | --- |
| 400 | VALIDATION_FAILED · VALIDATION_FAILED; 발생 조건 및 재시도는 operation 설명과 기능 명세 참조. |
| 401 | AUTHENTICATION_REQUIRED · AUTHENTICATION_REQUIRED; 발생 조건 및 재시도는 operation 설명과 기능 명세 참조. |
| 404 | RESOURCE_NOT_FOUND · RESOURCE_NOT_FOUND; 발생 조건 및 재시도는 operation 설명과 기능 명세 참조. |
| 429 | RATE_LIMITED · RATE_LIMITED; 발생 조건 및 재시도는 operation 설명과 기능 명세 참조. |
| 503 | AUTH_STATE_UNAVAILABLE · AUTH_STATE_UNAVAILABLE; 발생 조건 및 재시도는 operation 설명과 기능 명세 참조. |
| 403 | CHILD_SESSION_REQUIRED, CSRF_INVALID, SERVICE_HOURS_CLOSED · SERVICE_HOURS_CLOSED / CHILD_SESSION_REQUIRED / CSRF_INVALID |
| 409 | CONVERSATION_CLOSING, CONVERSATION_ENDED · CONVERSATION_ENDED / CONVERSATION_CLOSING |
| 200 | [ResumeConversationResultResponse](schemas/conversations.md#schema-resumeconversationresultresponse) · 현재 세션 ACTIVE 연결 후 허용 발화/처리 상태. 빈 배열도200. |

응답 추가 헤더: 429 `Retry-After`: 최소 재시도 대기 초. 실제 남은 차단시간으로 계산.

트랜잭션·재시도: 대화잠금후최신시간·상태검사,linkUPSERT로최초bound_at보존. 응답직전시간/세션/소유권/연결재검사;DB단일snapshot 또는잠금으로turns와processingTurnId일치. CLOSING 전이와 같은 대화 잠금으로 직렬화하여 종료 이후 link 생성 금지.

DB 기본 업무 읽기: `accounts, session_security, children, conversations, conversation_turns, conversation_session_links`; 기본 업무 쓰기: `conversation_session_links`.

조건부·후속 DB 작업: x-tables-write는 기본 업무 쓰기 목록이며 아래 공통 조정기/후속 worker의 조건부 쓰기는 별도다. 모든 요청의 auth_rate_limits 예산 예약과 SPRING_SESSION/SPRING_SESSION_ATTRIBUTES 프레임워크 저장은 별도 공통 처리이며 단일 업무 Tx와의 원자성을 가정하지 않는다. 이 요청의 직접 쓰기는 conversation_session_links의 명시 연결 UPSERT뿐이다. CLOSING/자정은 새 연결 없이 거부한다. 별도 timer/sweep/종료 worker는 conversations, conversation_turns, conversation_session_links, turn_audio_assets를 위 종료·기한·복구·삭제 계약에 따라 변경할 수 있으나 resume이 그 작업을 매번 실행하는 계약은 아니다.

200 응답 예시 (PROCESSING_NEW_SESSION):

<!-- json-example: ResumeConversationResultResponse -->
```json
{
  "data": {
    "conversationId": "33333333-3333-4333-8333-333333333333",
    "status": "ACTIVE",
    "startedAt": "2026-10-02T13:00:00+09:00",
    "turns": [
      {
        "turnId": "55555555-5555-4555-8555-555555555555",
        "sequence": 1,
        "status": "PROCESSING",
        "childText": null,
        "childTextVisibility": "OMITTED",
        "replyText": null,
        "replyTextVisibility": "OMITTED",
        "createdAt": "2026-10-02T13:00:00+09:00",
        "completedAt": null,
        "errorCode": null
      }
    ],
    "processingTurnId": "55555555-5555-4555-8555-555555555555",
    "serviceDate": "2026-10-02",
    "scheduledEndAt": "2026-10-03T00:00:00+09:00"
  }
}
```

나머지 1개 상태 예시는 OpenAPI의 이 operation에 함께 제공한다.

<a id="section-7-7"></a>
<a id="operation-endchildconversation"></a>
### 7.7 이야기 마치기 및 종료 결과 재확인

`POST /api/v1/children/{childId}/conversations/{conversationId}/end` · `endChildConversation` · **담당 Part 2** · B-02/B-04 · 권한 G+O+X+기존 연결/복구

body0바이트·CSRF·현재 로그인·아이 소유권·기존 연결 필수, PIN 불필요. ACTIVE에서 수동 종료를 접수하면 잠금 후 DB 시각을 MANUAL/end_requested_at으로 고정하고 즉시 CLOSING으로 새 입력·본문·음성·resume을 차단한다. 자정 이후 처음 관측하면 MIDNIGHT/scheduled_end_at 사용. 이미 CLOSING/ENDED인 경계·사유는 덮어쓰지 않는다. bound_at<end_requested_at인 기존 유효 연결만 확인 가능하며 CLOSING에서 새 연결을 만들지 않는다. 기존 PROCESSING은 원래 deadline까지 drain하고 남으면202 EndPendingReceipt(2필드)+Retry-After>=1. terminal 정리 후 ENDED이면200 EndReceipt(4필드). 별도 poll 경로 없이 같은 end 재확인. ENDED 확인은 실제 endedAt부터600초 미만의 저장된 복구권한만, 새 로그인403·무효 로그인401·기한 equality/초과403. 반복 end는 경계/복구기한 연장·요약 재호출 없음. 시간 밖에도 확인 정보만 예외 허용, 내용·음성·요약 본문 없음.

| 위치 | 필드 | 필수 | 타입·기본값·검증 |
| --- | --- | --- | --- |
| path | childId | 예 | type="string"; format="uuid" 필수 UUID. null/형식 오류400. 서버에서 로그인 계정 소유권과 부모-자식 리소스 연결 검사. |
| path | conversationId | 예 | type="string"; format="uuid" 필수 UUID. null/형식 오류400. 서버에서 로그인 계정 소유권과 부모-자식 리소스 연결 검사. |
| header | X-XSRF-TOKEN | 예 | type="string"; minLength=1 로그인 쿠키와 함께 모든 POST에 전송. 누락/불일치403 CSRF_INVALID. GET은 변경용 CSRF 검사 없음. |

요청 body: 없음. POST는 실제0바이트.

| HTTP | 본문·오류 코드 |
| --- | --- |
| 400 | VALIDATION_FAILED · VALIDATION_FAILED; 발생 조건 및 재시도는 operation 설명과 기능 명세 참조. |
| 401 | AUTHENTICATION_REQUIRED · AUTHENTICATION_REQUIRED; 발생 조건 및 재시도는 operation 설명과 기능 명세 참조. |
| 404 | RESOURCE_NOT_FOUND · RESOURCE_NOT_FOUND; 발생 조건 및 재시도는 operation 설명과 기능 명세 참조. |
| 429 | RATE_LIMITED · RATE_LIMITED; 발생 조건 및 재시도는 operation 설명과 기능 명세 참조. |
| 503 | AUTH_STATE_UNAVAILABLE · AUTH_STATE_UNAVAILABLE; 발생 조건 및 재시도는 operation 설명과 기능 명세 참조. |
| 403 | CHILD_SESSION_REQUIRED, CSRF_INVALID · CSRF_INVALID / CHILD_SESSION_REQUIRED |
| 200 | [EndReceiptResponse](schemas/conversations.md#schema-endreceiptresponse) · 종료 완료 확인. 현재 적격 연결과 실제 endedAt+600초 복구기한 안에서만200; 본문은 네 필드. |
| 202 | [EndPendingReceiptResponse](schemas/conversations.md#schema-endpendingreceiptresponse) · 종료 접수·기존 발화 정리 중. 최소1초 Retry-After 뒤 같은 end 재확인, 내용 없음. |

응답 추가 헤더: 429 `Retry-After`: 최소 재시도 대기 초. 실제 남은 차단시간으로 계산.; 202 `Retry-After`: 동일 end 재확인 전 대기 초, 최소1.

트랜잭션·재시도: end/timer/turn 완료/기동 catch-up/sweep/Home/start 정리는 동일 conversations 잠금/CAS 조정기. ACTIVE→CLOSING 최초 cutoff/reason 고정; 처리 중 발화 없으면 같은 짧은 Tx에서 바로 ENDED 가능. 기존 deadline 만료는 조건부 FAILED. 최초 ENDED에서 실제 ended_at·적격 links의 ended_at+600초·PENDING 또는 EMPTY 원자 기록. 허용 요약 입력 있으면 summary_started_at을 이 최초 ENDED 전이 시각으로 기록하고 최초 커밋 실행자만 잠금 밖 요약1회 시도; 빈 대화 포함 입력 없으면 EMPTY/요약 미호출. 요약 중 새 대화 가능, 결과는 원래 conversationId에만 반영.

DB 기본 업무 읽기: `accounts, session_security, children, conversations, conversation_turns, conversation_session_links`; 기본 업무 쓰기: `conversations, conversation_session_links`.

조건부·후속 DB 작업: x-tables-write는 기본 업무 쓰기 목록이며 아래 공통 조정기/후속 worker의 조건부 쓰기는 별도다. 모든 요청의 auth_rate_limits 예산 예약과 SPRING_SESSION/SPRING_SESSION_ATTRIBUTES 프레임워크 저장은 별도 공통 처리이며 단일 업무 Tx와의 원자성을 가정하지 않는다. 종료 경계/상태와 적격 복구기한은 conversations+conversation_session_links 업무 Tx다. 기존 turn timeout 정리와 음성 DELETE_PENDING 표시는 같은 종료 흐름의 조건부 쓰기이며 아래 공통 조정기/worker 범위에 포함한다. 공통 종료·기한 조정기/후속 worker의 조건부 쓰기: conversations(최초 종료 경계·CLOSING/ENDED·요약 상태/결과), conversation_turns(기존 PROCESSING의 만료 FAILED 또는 기한 내 허용 terminal 결과), conversation_session_links(적격 기존 연결의 종료 복구기한), turn_audio_assets(수동 종료 접수/자정/만료의 DELETE_PENDING 및 파일 삭제 확인 후 DELETED). 요청이 이 작업을 필요로 할 때만 실행하며 네 테이블을 매번 같은 Tx로 쓴다는 뜻은 아니다. 요약/AI/파일 삭제는 잠금 밖 후속 작업이고, CLOSING에서 늦게 생성된 음성도 아이에게 노출하지 않고 삭제 대상으로 정리한다.

200 응답 예시 (COMMITTED_PENDING):

<!-- json-example: EndReceiptResponse -->
```json
{
  "data": {
    "conversationId": "33333333-3333-4333-8333-333333333333",
    "status": "ENDED",
    "endedAt": "2026-10-03T00:00:03+09:00",
    "summaryStatus": "PENDING"
  }
}
```

나머지 4개 상태 예시는 OpenAPI의 이 operation에 함께 제공한다.

202 응답 예시 (pending):

<!-- json-example: EndPendingReceiptResponse -->
```json
{
  "data": {
    "conversationId": "33333333-3333-4333-8333-333333333333",
    "status": "CLOSING"
  }
}
```

<a id="section-8"></a>
## 8. 수동·자정 종료와 같은 날 재시작 규칙

<a id="section-8-1"></a>
### 8.1 시간 판정과 상태별 접근

잠금 후 DB 현재시각을 기준으로 한다. 클라이언트/AI 시각이나 오래된 Tx 시작시각으로 경계를 늘리지 않는다. 아이 데이터 경로는 로그인 → 리소스/소유권 → ENDED409 → 이용시간403 → CLOSING/당일·예정종료 경계409 → 현재 연결403 순서다. 타인·잘못된 조합은404, POST의 CSRF/구조 검사는 공통 우선순위를 따른다. start는 서비스 시간부터 검사하며 end는 확인정보에 한해 시간 밖 예외다.

| 조건 | Home | start/resume/turn/audio | end | 보호자 기록 |
| --- | --- | --- | --- | --- |
| 당일08\~24 ACTIVE | canEnter=true, 접근 가능한 activeConversationId | 권한·연결 충족 시 허용 | 기존 연결에서 수동 종료 접수, 202 또는 바로200 | ACTIVE 제외 |
| 서비스 시간 안 CLOSING | false/CONVERSATION_CLOSING, activeConversationId=null | 409 CONVERSATION_CLOSING, 새 연결·내용 금지 | 경계 전 기존 유효 연결만202·Retry-After≥1, 완료하면200 | CLOSING 제외 |
| 00\~08 미종료 | false/OUTSIDE_SERVICE_HOURS 우선, ID=null | 403 SERVICE_HOURS_CLOSED, 자정 경계 고정·정리 | 적격 기존 연결만202 또는200 | ENDED 전 제외 |
| ENDED | 다른 접근 가능한 ACTIVE만 ID 반환, 없으면null | 해당 대화409 CONVERSATION_ENDED; 서비스 시간 안 새 키 start는 새 대화 가능 | 적격 기존 연결+실제 endedAt+600초 미만만200 | PIN 확인 후 조회, EMPTY 포함 |
| 무효 로그인/저장소 확인 불가 | 401/503 | 401/503 | 401/503 | 401/503 |

08:00은 포함, 자정은 제외한다. 23:59 업로드라도 잠금 후 접수 판정이00:00이면 거부한다. FE는 이야기 마치기 접수 및 자정에 입력·마이크·재생을 멈추고 Blob/화면 본문을 정리한다. 이미 전달한 바이트는 서버가 회수하지 못한다. 화면 이탈 자체는 종료나 기록 삭제가 아니다.

<a id="section-8-2"></a>
### 8.2 생성·재진입·시작 키

`UNIQUE(child_id,service_date)`를 제거하고 `UNIQUE(child_id,client_request_id)`를 유지한다. `child_id WHERE status IN ('ACTIVE','CLOSING')` 부분 UNIQUE로 아이당 미종료1개를 보장한다. 아이 잠금 아래 이전 날짜 미종료도 먼저 정리한다. 기한 안 발화가 남으면 CLOSING으로 새 시작을 막고 원래 기한을 단축하지 않는다.

서비스 시간 안 같은 키·당일 ACTIVE·현재 연결은200, 미연결은403 후 명시 resume. 같은 키 CLOSING은409 CONVERSATION_CLOSING, ENDED는409 CONVERSATION_ENDED이며 새로운 행을 만들지 않는다. 다른 키 ACTIVE는409 ACTIVE_CONVERSATION_EXISTS+ID, CLOSING은409 CONVERSATION_CLOSING. 미종료가 없으면 당일 ENDED·요약 PENDING 여부와 무관하게 새 키로 새 ID를201 생성한다. 재전송은 기존 키, 새 대화는 새 키다.

resume은 당일 ACTIVE에서만 유효 로그인과 소유권을 확인해 새 문맥도 연결하며 최초 bound_at을 유지한다. PROCESSING 중에도 전체 허용 turns를 sequence ASC 복원한다. CLOSING에서는 새 연결·resume·본문을 금지한다. 새 로그인은 이전 종료 복구권한을 얻지 못하며 Home 재조회로 현재 진입 가능 상태를 확인한다. 시간 밖에는 종료 완료를 추정하지 않고 시간외 안내 후 opensAt부터 재확인한다. Home에 closingConversationId는 추가하지 않는다.

<a id="section-8-3"></a>
### 8.3 종료 접수·기존 발화 정리·요약

1. 음성 접수와 수동 end는 동일 conversation 잠금/CAS로 직렬화한다. turn 접수가 먼저면 created_at·sequence·원래 deadline을 커밋하고 drain 대상이 된다. end가 먼저면 CLOSING으로 이후 접수와 모든 아이 내용·음성·resume을 차단한다. 같은 키/파일 재전송도 상태 게이트를 우회하지 못한다.
2. 최초 수동 종료는 잠금 후 DB 현재시각을 `end_requested_at`, MANUAL을 `end_reason`으로 기록한다. DB 현재시각이 scheduled_end_at 이상이면 MIDNIGHT/scheduled_end_at을 쓴다. 최초 경계·사유는 중복 end나 자정 도달로 변경하지 않는다. ACTIVE 종료 필드는 null, CLOSING은 경계/사유 필수·ended_at null이다.
3. 접수 커밋 실행자만 잠금 밖 AI1회 시도. CLOSING에서도 기존 발화의 허용 결과만 원래 deadline까지 저장한다. `now<deadline`인 PROCESSING만 성공 가능하며 equality부터 조건부 AI_TIMEOUT/FAILED다. 늦은 callback은 terminal을 덮어쓰지 않는다. 응답 직전에 시간·로그인·소유권·상태·연결을 재검사하고 저장 허용과 내용 전달 허용을 분리한다.
4. 수동 end, 자정 timer, 기동 catch-up, sweep, turn 완료, Home/start 정리는 같은 종료 조정기를 사용한다. ACTIVE→CLOSING→ENDED이며 처리 중 발화가 없으면 짧은 Tx에서 곧바로 ENDED 가능하다. 기한 안 PROCESSING이 남으면 end202 EndPendingReceipt+Retry-After≥1을 반환한다. 별도 polling 경로 없이 같은 end를 재확인한다.
5. 최초 ENDED Tx만 실제 ended_at, 적격 연결의 ended_at+600초, PENDING/EMPTY를 확정한다. `ended_at>=end_requested_at>=started_at`, MANUAL 경계<scheduled_end_at, MIDNIGHT 경계=scheduled_end_at이다. 빈 대화나 허용 요약 입력이 없으면 EMPTY 기록으로 남기고 요약 AI를 호출하지 않는다. 입력이 있으면 이 ENDED 전이 때 summary_started_at을 기록하고 최초 커밋 실행자만 잠금 밖 요약1회 시도한다.
6. 요약은 PENDING이고 `now<summary deadline`일 때만 READY. 실패/기한 초과는 FAILED, crash 재실행 없음. 이전 요약 처리 중 새 대화를 시작할 수 있으며 결과는 원래 conversationId에만 저장한다. 보호자 목록·상세는 ENDED만, 빈 EMPTY도 포함하고 startedAt DESC/ID DESC를 유지한다. AI 외부 호출의 정확히1회 실행 보장은 아니다.

<a id="section-8-4"></a>
### 8.4 종료 확인 복구와 음성

최초 종료 경계보다 이른 `bound_at<end_requested_at`의 기존 유효 문맥만 CLOSING 확인과 ENDED 복구 대상이다. 수동/자정 종료 모두 적용한다. 로그인 무효화가 항상 우선하며 현재 소유권·securityContextId·연결을 매번 확인한다. 여러 기존 세션을 허용하고 첫 확인 세션1개로 제한하지 않는다. 새 로그인·새 연결에는 권한을 주지 않는다.

CLOSING 확인은202의 conversationId/status 두 필드만이다. ENDED 확인은200의 conversationId/status/endedAt/summaryStatus 네 필드만이다. 발화·과거 내용·음성·요약 본문을 보내지 않는다. 기간은 **실제 ended_at+600초**이며 `now>=end_recovery_until`부터403, 무효 로그인401. 재요청·쿠키 갱신·재접속·요약 완료·설정 변경으로 경계나 저장 기한을 늘리지 않는다.

임시 음성 GET은 별도 음성 허용·당일ACTIVE·서비스 시간·현재 연결·AVAILABLE·만료 전을 모두 만족해야 한다. CLOSING409/시간 밖403이 자산 판정보다 우선한다. 금지/미생성404, 기존 자산 만료410이며 삭제 지연에도 논리 접근은 거부한다. AI/TTS 재생성·영구 음성 URL·보호자 다시 듣기·Range/206은 추가하지 않는다.
