# 음성 발화·비동기 복구·임시 음성

[전체 API 목차](../Integrated_API_Spec.md) · **담당: Part 2** · 계약 v3.1 / 문서 분할 2026-10-06

절 번호는 기존 통합 명세의 번호를 유지합니다. 다른 절은 전체 목차에서 찾을 수 있습니다.

## 이 문서에서 찾기

- [4.11 VoiceTurnRequest](#section-4-11)
- [7.4 음성 발화 전송](#section-7-4)
- [7.5 발화 ID로 상태 조회](#section-7-5)
- [7.6 요청 ID로 상태 조회](#section-7-6)
- [7.8 임시 응답 음성 재접근](#section-7-8)

<a id="section-4-11"></a>
### 4.11 VoiceTurnRequest

[VoiceTurnRequest](schemas/voice.md#schema-voiceturnrequest) · `multipart/form-data`

multipart audio+clientRequestId 두필드만. 실제 audio바이트 SHA-256 소문자hex64; 파일명/boundary/clientRequestId 제외. 실제파일디코딩검사. 의미있는추가처리옵션은P0없음. MIME/codec/용량/시간한도는D13/14대기.

| 필드 | 필수 | 타입·검증 |
| --- | --- | --- |
| clientRequestId | 예 | type="string"; format="uuid" multipart text part. 전송 재시도는 같은 키·같은 파일, 새 녹음은 새 UUID. |
| audio | 예 | type="string"; format="binary" 필수 음성 파일. 텍스트/버튼 발화는 받지 않는다. |

`audio`는 실제 파일 바이너리 파트, `clientRequestId`는 UUID 문자열 파트다. audio를 JSON/Base64 문자열로 바꾸지 않는다. 파일명·boundary·키는 해시에서 제외한다. MIME/codec/크기/길이 수치는 D13·D14 자료 대기다.

<a id="section-7-4"></a>
<a id="operation-submitvoiceturn"></a>
### 7.4 음성 발화 전송

`POST /api/v1/children/{childId}/conversations/{conversationId}/turns` · `submitVoiceTurn` · **담당 Part 2** · B-03/B-04/B-05 · 권한 G+O+L+X+시간

당일ACTIVE·08\~24·현재연결·CSRF필수. 대화잠금후최신DB시각이scheduled_end_at미만인때만접수;23:59업로드라도접수판정이00:00이면거부. 기존키+같은해시는PROCESSING202/terminal200,다른해시409,새키만다른PROCESSING409. 신규완전성공201,접수후미완료202. STT무음첫동기422 STT_NO_SPEECH/FAILED,AI또는TTS장애502 AI_UPSTREAM_FAILED/FAILED,전체기한초과504 AI_TIMEOUT/FAILED. 이미202이면GET200의FAILED로확인. TTS실패도허용텍스트만보존/audio=null/topics=[]. 종료 경계 전에 접수한 작업만 CLOSING에서도 원래 deadline까지 허용 결과 저장 가능. 수동 종료와 자정 이후 모든 아이 본문·음성 전달은 차단한다. CLOSING은409 CONVERSATION_CLOSING. 응답 직전 현재 상태/시간/로그인/소유권/연결 재검사; GET·동일 키 재전송도 우회 불가.

| 위치 | 필드 | 필수 | 타입·기본값·검증 |
| --- | --- | --- | --- |
| path | childId | 예 | type="string"; format="uuid" 필수 UUID. null/형식 오류400. 서버에서 로그인 계정 소유권과 부모-자식 리소스 연결 검사. |
| path | conversationId | 예 | type="string"; format="uuid" 필수 UUID. null/형식 오류400. 서버에서 로그인 계정 소유권과 부모-자식 리소스 연결 검사. |
| header | X-XSRF-TOKEN | 예 | type="string"; minLength=1 로그인 쿠키와 함께 모든 POST에 전송. 누락/불일치403 CSRF_INVALID. GET은 변경용 CSRF 검사 없음. |

요청 body: `multipart/form-data` [VoiceTurnRequest](schemas/voice.md#schema-voiceturnrequest)

| HTTP | 본문·오류 코드 |
| --- | --- |
| 400 | VALIDATION_FAILED · VALIDATION_FAILED; 발생 조건 및 재시도는 operation 설명과 기능 명세 참조. |
| 401 | AUTHENTICATION_REQUIRED · AUTHENTICATION_REQUIRED; 발생 조건 및 재시도는 operation 설명과 기능 명세 참조. |
| 404 | RESOURCE_NOT_FOUND · RESOURCE_NOT_FOUND; 발생 조건 및 재시도는 operation 설명과 기능 명세 참조. |
| 429 | RATE_LIMITED · RATE_LIMITED; 발생 조건 및 재시도는 operation 설명과 기능 명세 참조. |
| 503 | AUTH_STATE_UNAVAILABLE · AUTH_STATE_UNAVAILABLE; 발생 조건 및 재시도는 operation 설명과 기능 명세 참조. |
| 403 | CHILD_SESSION_REQUIRED, CSRF_INVALID, SERVICE_HOURS_CLOSED · SERVICE_HOURS_CLOSED / CHILD_SESSION_REQUIRED / CSRF_INVALID |
| 409 | CONVERSATION_CLOSING, CONVERSATION_ENDED, IDEMPOTENCY_CONFLICT, TURN_IN_PROGRESS · CONVERSATION_ENDED / CONVERSATION_CLOSING / IDEMPOTENCY_CONFLICT / TURN_IN_PROGRESS |
| 413 | AUDIO_TOO_LARGE · 음성 최대 업로드 크기를 넘었습니다. 수치는 D13/D14 미정. |
| 415 | AUDIO_FORMAT_UNSUPPORTED · 미지원/잘못된 음성 형식입니다. MIME/codec와 파일 검증 범위 D13/D14 미정. |
| 502 | AI_UPSTREAM_FAILED · D10A AI/TTS장애. 허용텍스트보존,FAILED/audio=null/topics=[]. |
| 504 | AI_TIMEOUT · AI_TIMEOUT |
| 201 | [SucceededChildTurnResultResponse](schemas/voice.md#schema-succeededchildturnresultresponse) · 최초 대기 구간 내 완전 성공. audio/wav 및 예시4분은 MIME/TTL 확정 아님. |
| 200 | [TerminalChildTurnResultResponse](schemas/voice.md#schema-terminalchildturnresultresponse) · 동일 키·같은 해시의 저장 완료결과. 이전 실패도 조회 성공200. |
| 202 | [TurnAcceptedResponse](schemas/voice.md#schema-turnacceptedresponse) · 신규 접수 또는 동일키 PROCESSING. statusUrl로 조회. |
| 422 | STT_NO_SPEECH · D10A 실제 AI 무음 판정의 최초 동기 오류. 접수된 발화FAILED 저장. 이미202전달후는GET200 FAILED로확인. |

응답 추가 헤더: 429 `Retry-After`: 최소 재시도 대기 초. 실제 남은 차단시간으로 계산.; 201 `Location`: /api/v1/children/{childId}/conversations/{conversationId}/turns/{turnId}

트랜잭션·재시도: 기존키검사→새키만유효음성/PROCESSING충돌→sequence MAX+1·created_at·deadline·PROCESSING커밋→잠금없이AI1회시도→잠금후현재PROCESSING을검사: now<deadline이면허용결과로terminal변경,now>=deadline이면AI_TIMEOUT/FAILED정리. 늦은결과terminal덮어쓰기금지. 최종응답권한검사와저장완료는별개. 이후 CLOSING/자정 대화는 공통 종료 조정기 실행. 수동 end와 동일 conversation 잠금/CAS 사용; end가 먼저면 신규 접수 거부, turn이 먼저면 기존 기한까지 drain. terminal 늦은 callback 덮어쓰기 금지.

DB 기본 업무 읽기: `accounts, session_security, children, conversations, conversation_session_links, conversation_turns, turn_audio_assets`; 기본 업무 쓰기: `conversation_turns, turn_audio_assets`.

조건부·후속 DB 작업: x-tables-write는 기본 업무 쓰기 목록이며 아래 공통 조정기/후속 worker의 조건부 쓰기는 별도다. 모든 요청의 auth_rate_limits 예산 예약과 SPRING_SESSION/SPRING_SESSION_ATTRIBUTES 프레임워크 저장은 별도 공통 처리이며 단일 업무 Tx와의 원자성을 가정하지 않는다. 기본 접수 Tx는 conversation_turns; 허용 결과 Tx는 conversation_turns+turn_audio_assets다. terminal 이후 수동/자정 종료 정리는 공통 조정기를 경유한다. 공통 종료·기한 조정기/후속 worker의 조건부 쓰기: conversations(최초 종료 경계·CLOSING/ENDED·요약 상태/결과), conversation_turns(기존 PROCESSING의 만료 FAILED 또는 기한 내 허용 terminal 결과), conversation_session_links(적격 기존 연결의 종료 복구기한), turn_audio_assets(수동 종료 접수/자정/만료의 DELETE_PENDING 및 파일 삭제 확인 후 DELETED). 요청이 이 작업을 필요로 할 때만 실행하며 네 테이블을 매번 같은 Tx로 쓴다는 뜻은 아니다. 요약/AI/파일 삭제는 잠금 밖 후속 작업이고, CLOSING에서 늦게 생성된 음성도 아이에게 노출하지 않고 삭제 대상으로 정리한다.

201 응답 예시 (COMPLETE_SUCCESS):

<!-- json-example: SucceededChildTurnResultResponse -->
```json
{
  "data": {
    "turn": {
      "turnId": "55555555-5555-4555-8555-555555555555",
      "sequence": 1,
      "status": "SUCCEEDED",
      "childText": "오늘 친구랑 놀았어.",
      "childTextVisibility": "VISIBLE",
      "replyText": "어떤 놀이를 했니?",
      "replyTextVisibility": "VISIBLE",
      "createdAt": "2026-10-02T13:00:00+09:00",
      "completedAt": "2026-10-02T13:00:03+09:00",
      "errorCode": null
    },
    "audio": {
      "url": "/api/v1/children/22222222-2222-4222-8222-222222222222/conversations/33333333-3333-4333-8333-333333333333/turns/55555555-5555-4555-8555-555555555555/audio",
      "contentType": "audio/wav",
      "expiresAt": "2026-10-02T13:04:00+09:00"
    },
    "topicSuggestions": []
  }
}
```

나머지 1개 상태 예시는 OpenAPI의 이 operation에 함께 제공한다.

200 응답 예시 (SUCCEEDED):

<!-- json-example: TerminalChildTurnResultResponse -->
```json
{
  "data": {
    "turn": {
      "turnId": "55555555-5555-4555-8555-555555555555",
      "sequence": 1,
      "status": "SUCCEEDED",
      "childText": "오늘 친구랑 놀았어.",
      "childTextVisibility": "VISIBLE",
      "replyText": "어떤 놀이를 했니?",
      "replyTextVisibility": "VISIBLE",
      "createdAt": "2026-10-02T13:00:00+09:00",
      "completedAt": "2026-10-02T13:00:03+09:00",
      "errorCode": null
    },
    "audio": {
      "url": "/api/v1/children/22222222-2222-4222-8222-222222222222/conversations/33333333-3333-4333-8333-333333333333/turns/55555555-5555-4555-8555-555555555555/audio",
      "contentType": "audio/wav",
      "expiresAt": "2026-10-02T13:04:00+09:00"
    },
    "topicSuggestions": []
  }
}
```

나머지 2개 상태 예시는 OpenAPI의 이 operation에 함께 제공한다.

202 응답 예시 (ACCEPTED):

<!-- json-example: TurnAcceptedResponse -->
```json
{
  "data": {
    "turnId": "55555555-5555-4555-8555-555555555555",
    "clientRequestId": "88888888-8888-4888-8888-888888888888",
    "status": "PROCESSING",
    "statusUrl": "/api/v1/children/22222222-2222-4222-8222-222222222222/conversations/33333333-3333-4333-8333-333333333333/turns/55555555-5555-4555-8555-555555555555"
  }
}
```

<a id="section-7-5"></a>
<a id="operation-getvoiceturn"></a>
### 7.5 발화 ID로 상태 조회

`GET /api/v1/children/{childId}/conversations/{conversationId}/turns/{turnId}` · `getVoiceTurn` · **담당 Part 2** · B-05 · 권한 G+O+L+시간

turnId로저장된발화상태조회. 응답은PROCESSING/SUCCEEDED/FAILED모두200 ChildTurnResult. 유효로그인→아이소유권/대화/turn조합→ENDED409→서비스시간403→CLOSING 또는 당일/기한409→현재연결403순서. 자정후에도처리중인발화내용은보내지않음. GET은AI재호출하지않음. 응답유실은clientRequestId조회로복구하며404만으로동시POST미접수를단정하지않음. CLOSING은409 CONVERSATION_CLOSING. 응답 직전 현재 상태/시간/로그인/소유권/연결 재검사; GET·동일 키 재전송도 우회 불가.

| 위치 | 필드 | 필수 | 타입·기본값·검증 |
| --- | --- | --- | --- |
| path | childId | 예 | type="string"; format="uuid" 필수 UUID. null/형식 오류400. 서버에서 로그인 계정 소유권과 부모-자식 리소스 연결 검사. |
| path | conversationId | 예 | type="string"; format="uuid" 필수 UUID. null/형식 오류400. 서버에서 로그인 계정 소유권과 부모-자식 리소스 연결 검사. |
| path | turnId | 예 | type="string"; format="uuid" 필수 UUID. null/형식 오류400. 서버에서 로그인 계정 소유권과 부모-자식 리소스 연결 검사. |

요청 body: 없음.

| HTTP | 본문·오류 코드 |
| --- | --- |
| 400 | VALIDATION_FAILED · VALIDATION_FAILED; 발생 조건 및 재시도는 operation 설명과 기능 명세 참조. |
| 401 | AUTHENTICATION_REQUIRED · AUTHENTICATION_REQUIRED; 발생 조건 및 재시도는 operation 설명과 기능 명세 참조. |
| 404 | RESOURCE_NOT_FOUND · RESOURCE_NOT_FOUND; 발생 조건 및 재시도는 operation 설명과 기능 명세 참조. |
| 429 | RATE_LIMITED · RATE_LIMITED; 발생 조건 및 재시도는 operation 설명과 기능 명세 참조. |
| 503 | AUTH_STATE_UNAVAILABLE · AUTH_STATE_UNAVAILABLE; 발생 조건 및 재시도는 operation 설명과 기능 명세 참조. |
| 403 | CHILD_SESSION_REQUIRED, SERVICE_HOURS_CLOSED · SERVICE_HOURS_CLOSED / CHILD_SESSION_REQUIRED |
| 409 | CONVERSATION_CLOSING, CONVERSATION_ENDED · CONVERSATION_ENDED / CONVERSATION_CLOSING |
| 200 | [ChildTurnResultResponse](schemas/voice.md#schema-childturnresultresponse) · 저장 상태 조회 성공(PROCESSING/SUCCEEDED/FAILED). |

응답 추가 헤더: 429 `Retry-After`: 최소 재시도 대기 초. 실제 남은 차단시간으로 계산.

트랜잭션·재시도: 읽기/최종권한재검사. 기한정리실행은기존PROCESSING 조건부실패만; AI재실행없음. 수동 end와 동일 conversation 잠금/CAS 사용; end가 먼저면 신규 접수 거부, turn이 먼저면 기존 기한까지 drain. terminal 늦은 callback 덮어쓰기 금지.

DB 기본 업무 읽기: `accounts, session_security, children, conversations, conversation_session_links, conversation_turns, turn_audio_assets`; 기본 업무 쓰기: ``.

조건부·후속 DB 작업: x-tables-write는 기본 업무 쓰기 목록이며 아래 공통 조정기/후속 worker의 조건부 쓰기는 별도다. 모든 요청의 auth_rate_limits 예산 예약과 SPRING_SESSION/SPRING_SESSION_ATTRIBUTES 프레임워크 저장은 별도 공통 처리이며 단일 업무 Tx와의 원자성을 가정하지 않는다. 기본 상태 projection은 읽기 전용이다. 발견한 만료 PROCESSING의 조건부 FAILED 및 그 terminal 전이에 따른 종료 정리는 공통 조정기/worker를 경유한다. 공통 종료·기한 조정기/후속 worker의 조건부 쓰기: conversations(최초 종료 경계·CLOSING/ENDED·요약 상태/결과), conversation_turns(기존 PROCESSING의 만료 FAILED 또는 기한 내 허용 terminal 결과), conversation_session_links(적격 기존 연결의 종료 복구기한), turn_audio_assets(수동 종료 접수/자정/만료의 DELETE_PENDING 및 파일 삭제 확인 후 DELETED). 요청이 이 작업을 필요로 할 때만 실행하며 네 테이블을 매번 같은 Tx로 쓴다는 뜻은 아니다. 요약/AI/파일 삭제는 잠금 밖 후속 작업이고, CLOSING에서 늦게 생성된 음성도 아이에게 노출하지 않고 삭제 대상으로 정리한다.

200 응답 예시 (PROCESSING):

<!-- json-example: ChildTurnResultResponse -->
```json
{
  "data": {
    "turn": {
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
    },
    "audio": null,
    "topicSuggestions": []
  }
}
```

나머지 4개 상태 예시는 OpenAPI의 이 operation에 함께 제공한다.

<a id="section-7-6"></a>
<a id="operation-getvoiceturnbyrequestid"></a>
### 7.6 요청 ID로 상태 조회

`GET /api/v1/children/{childId}/conversations/{conversationId}/turn-requests/{clientRequestId}` · `getVoiceTurnByRequestId` · **담당 Part 2** · B-05 · 권한 G+O+L+시간

clientRequestId로저장된발화상태조회. 응답은PROCESSING/SUCCEEDED/FAILED모두200 ChildTurnResult. 유효로그인→아이소유권/대화/turn조합→ENDED409→서비스시간403→CLOSING 또는 당일/기한409→현재연결403순서. 자정후에도처리중인발화내용은보내지않음. GET은AI재호출하지않음. 응답유실은clientRequestId조회로복구하며404만으로동시POST미접수를단정하지않음. CLOSING은409 CONVERSATION_CLOSING. 응답 직전 현재 상태/시간/로그인/소유권/연결 재검사; GET·동일 키 재전송도 우회 불가.

| 위치 | 필드 | 필수 | 타입·기본값·검증 |
| --- | --- | --- | --- |
| path | childId | 예 | type="string"; format="uuid" 필수 UUID. null/형식 오류400. 서버에서 로그인 계정 소유권과 부모-자식 리소스 연결 검사. |
| path | conversationId | 예 | type="string"; format="uuid" 필수 UUID. null/형식 오류400. 서버에서 로그인 계정 소유권과 부모-자식 리소스 연결 검사. |
| path | clientRequestId | 예 | type="string"; format="uuid" 필수 UUID. null/형식 오류400. 서버에서 로그인 계정 소유권과 부모-자식 리소스 연결 검사. |

요청 body: 없음.

| HTTP | 본문·오류 코드 |
| --- | --- |
| 400 | VALIDATION_FAILED · VALIDATION_FAILED; 발생 조건 및 재시도는 operation 설명과 기능 명세 참조. |
| 401 | AUTHENTICATION_REQUIRED · AUTHENTICATION_REQUIRED; 발생 조건 및 재시도는 operation 설명과 기능 명세 참조. |
| 404 | RESOURCE_NOT_FOUND · RESOURCE_NOT_FOUND; 발생 조건 및 재시도는 operation 설명과 기능 명세 참조. |
| 429 | RATE_LIMITED · RATE_LIMITED; 발생 조건 및 재시도는 operation 설명과 기능 명세 참조. |
| 503 | AUTH_STATE_UNAVAILABLE · AUTH_STATE_UNAVAILABLE; 발생 조건 및 재시도는 operation 설명과 기능 명세 참조. |
| 403 | CHILD_SESSION_REQUIRED, SERVICE_HOURS_CLOSED · SERVICE_HOURS_CLOSED / CHILD_SESSION_REQUIRED |
| 409 | CONVERSATION_CLOSING, CONVERSATION_ENDED · CONVERSATION_ENDED / CONVERSATION_CLOSING |
| 200 | [ChildTurnResultResponse](schemas/voice.md#schema-childturnresultresponse) · 저장 상태 조회 성공(PROCESSING/SUCCEEDED/FAILED). |

응답 추가 헤더: 429 `Retry-After`: 최소 재시도 대기 초. 실제 남은 차단시간으로 계산.

트랜잭션·재시도: 읽기/최종권한재검사. 기한정리실행은기존PROCESSING 조건부실패만; AI재실행없음. 수동 end와 동일 conversation 잠금/CAS 사용; end가 먼저면 신규 접수 거부, turn이 먼저면 기존 기한까지 drain. terminal 늦은 callback 덮어쓰기 금지.

DB 기본 업무 읽기: `accounts, session_security, children, conversations, conversation_session_links, conversation_turns, turn_audio_assets`; 기본 업무 쓰기: ``.

조건부·후속 DB 작업: x-tables-write는 기본 업무 쓰기 목록이며 아래 공통 조정기/후속 worker의 조건부 쓰기는 별도다. 모든 요청의 auth_rate_limits 예산 예약과 SPRING_SESSION/SPRING_SESSION_ATTRIBUTES 프레임워크 저장은 별도 공통 처리이며 단일 업무 Tx와의 원자성을 가정하지 않는다. 기본 상태 projection은 읽기 전용이다. 발견한 만료 PROCESSING의 조건부 FAILED 및 그 terminal 전이에 따른 종료 정리는 공통 조정기/worker를 경유한다. 공통 종료·기한 조정기/후속 worker의 조건부 쓰기: conversations(최초 종료 경계·CLOSING/ENDED·요약 상태/결과), conversation_turns(기존 PROCESSING의 만료 FAILED 또는 기한 내 허용 terminal 결과), conversation_session_links(적격 기존 연결의 종료 복구기한), turn_audio_assets(수동 종료 접수/자정/만료의 DELETE_PENDING 및 파일 삭제 확인 후 DELETED). 요청이 이 작업을 필요로 할 때만 실행하며 네 테이블을 매번 같은 Tx로 쓴다는 뜻은 아니다. 요약/AI/파일 삭제는 잠금 밖 후속 작업이고, CLOSING에서 늦게 생성된 음성도 아이에게 노출하지 않고 삭제 대상으로 정리한다.

200 응답 예시 (PROCESSING):

<!-- json-example: ChildTurnResultResponse -->
```json
{
  "data": {
    "turn": {
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
    },
    "audio": null,
    "topicSuggestions": []
  }
}
```

나머지 4개 상태 예시는 OpenAPI의 이 operation에 함께 제공한다.

<a id="section-7-8"></a>
<a id="operation-gettemporaryturnaudio"></a>
### 7.8 임시 응답 음성 재접근

`GET /api/v1/children/{childId}/conversations/{conversationId}/turns/{turnId}/audio` · `getTemporaryTurnAudio` · **담당 Part 2** · B-03/B-05 · 권한 G+O+L+시간

D07 인증된임시음성GET. 로그인/소유권/리소스조합후ENDED409,시간외403 SERVICE_HOURS_CLOSED,CLOSING 또는 어제/자정 경계409 CONVERSATION_CLOSING,미연결403. 별도음성허용·AVAILABLE·now<expiresAt만200 binary/no-store/X-Request-ID. 금지·미생성404,기존자산만료410 AUDIO_EXPIRED. 물리삭제지연에도재생거부;AI/TTS재생성없음. 텍스트허용으로음성허용추론금지;실제MIME/codec/최대크기/TTL은D13/14대기. CLOSING은409 CONVERSATION_CLOSING. 응답 직전 현재 상태/시간/로그인/소유권/연결 재검사; GET·동일 키 재전송도 우회 불가.

| 위치 | 필드 | 필수 | 타입·기본값·검증 |
| --- | --- | --- | --- |
| path | childId | 예 | type="string"; format="uuid" 필수 UUID. null/형식 오류400. 서버에서 로그인 계정 소유권과 부모-자식 리소스 연결 검사. |
| path | conversationId | 예 | type="string"; format="uuid" 필수 UUID. null/형식 오류400. 서버에서 로그인 계정 소유권과 부모-자식 리소스 연결 검사. |
| path | turnId | 예 | type="string"; format="uuid" 필수 UUID. null/형식 오류400. 서버에서 로그인 계정 소유권과 부모-자식 리소스 연결 검사. |

요청 body: 없음.

| HTTP | 본문·오류 코드 |
| --- | --- |
| 400 | VALIDATION_FAILED · VALIDATION_FAILED; 발생 조건 및 재시도는 operation 설명과 기능 명세 참조. |
| 401 | AUTHENTICATION_REQUIRED · AUTHENTICATION_REQUIRED; 발생 조건 및 재시도는 operation 설명과 기능 명세 참조. |
| 404 | RESOURCE_NOT_FOUND · RESOURCE_NOT_FOUND; 발생 조건 및 재시도는 operation 설명과 기능 명세 참조. |
| 429 | RATE_LIMITED · RATE_LIMITED; 발생 조건 및 재시도는 operation 설명과 기능 명세 참조. |
| 503 | AUTH_STATE_UNAVAILABLE · AUTH_STATE_UNAVAILABLE; 발생 조건 및 재시도는 operation 설명과 기능 명세 참조. |
| 403 | CHILD_SESSION_REQUIRED, SERVICE_HOURS_CLOSED · SERVICE_HOURS_CLOSED / CHILD_SESSION_REQUIRED |
| 409 | CONVERSATION_CLOSING, CONVERSATION_ENDED · CONVERSATION_ENDED / CONVERSATION_CLOSING |
| 410 | AUDIO_EXPIRED · AUDIO_EXPIRED |
| 200 | audio/* · 허용된 임시 바이너리 음성. audio/*는 미정 MIME을 나타내는 초안 와일드카드이며 확정 후 실제 지원 MIME으로 교체. no-store/X-Request-ID. |

응답 추가 헤더: 429 `Retry-After`: 최소 재시도 대기 초. 실제 남은 차단시간으로 계산.

트랜잭션·재시도: 메타데이터권한검사와파일전달직전현재시간/로그인/연결/수명재검사. 이미전달한바이트는회수불가하므로FE도자정타이머로재생중단·Blob/화면데이터정리. 수동 end와 동일 conversation 잠금/CAS 사용; end가 먼저면 신규 접수 거부, turn이 먼저면 기존 기한까지 drain. terminal 늦은 callback 덮어쓰기 금지.

DB 기본 업무 읽기: `accounts, session_security, children, conversations, conversation_session_links, conversation_turns, turn_audio_assets`; 기본 업무 쓰기: ``.

조건부·후속 DB 작업: x-tables-write는 기본 업무 쓰기 목록이며 아래 공통 조정기/후속 worker의 조건부 쓰기는 별도다. 모든 요청의 auth_rate_limits 예산 예약과 SPRING_SESSION/SPRING_SESSION_ATTRIBUTES 프레임워크 저장은 별도 공통 처리이며 단일 업무 Tx와의 원자성을 가정하지 않는다. 기본 음성 GET의 권한/메타데이터 검사와 파일 전달은 읽기 전용이다. 만료/수동 종료/자정 정리 worker는 turn_audio_assets에 DELETE_PENDING을 기록하고 실제 파일 삭제 확인 후 DELETED로 갱신한다. 파일 삭제와 DB 변경은 원자적이지 않으며 물리 정리 지연과 무관하게 접근을 먼저 거부한다. GET으로 AI/TTS를 재실행하지 않는다.
