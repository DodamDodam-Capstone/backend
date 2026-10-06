# 보호자 대화 기록

[전체 API 목차](../Integrated_API_Spec.md) · **담당: Part 1 API / Part 2 읽기 모델** · 계약 v3.1 / 문서 분할 2026-10-06

절 번호는 기존 통합 명세의 번호를 유지합니다. 다른 절은 전체 목차에서 찾을 수 있습니다.

## 이 문서에서 찾기

- [6.15 날짜별 대화 기록 및 키워드 검색](#section-6-15)
- [6.16 대화 상세와 전체 발화 페이지](#section-6-16)

<a id="section-6-15"></a>
<a id="operation-listchildconversations"></a>
### 6.15 날짜별 대화 기록 및 키워드 검색

`GET /api/v1/children/{childId}/conversations` · `listChildConversations` · **담당 Part 1** · A-06 · 권한 G+O+P

로그인→아이 소유권→guardian 확인. B-07.1 공유안 채택(2026-10-01 KST), 목록·검색·상세 모두 ENDED만 포함·ACTIVE/CLOSING 제외. startedAt DESC, conversationId DESC. 제목/주제/요약/각 허용 텍스트 부분 일치, EXISTS로 대화 중복 없음. size+1로 hasNext. q/page 검증 실패 400, 타인 아이는 PIN 상태와 무관하게 404. PIN 만료는 403. OFFSET 페이지는 동시 추가 시 snapshot 안정성 보장 없음. D05A: 보호자기록은ENDED만,목록page/size 및상세100턴cursor유지. 수동/자정 CLOSING은 종료 완료 전 제외. 실제 ENDED 이후 요약 완료를 기다리지 않는다. 빈 EMPTY도 기록하며 같은 날 여러 대화는 startedAt+conversationId 정렬을 유지한다. 날짜필터는startedAt의KST날짜(=service_date),endedAt이새벽이어도전날기록에포함.

| 위치 | 필드 | 필수 | 타입·기본값·검증 |
| --- | --- | --- | --- |
| path | childId | 예 | type="string"; format="uuid" 현재 계정 소유 아이, 아니면 404. |
| query | from | 아니오 | type="string"; format="date" KST 시작 날짜 포함, 생략 시 하한 없음. |
| query | to | 아니오 | type="string"; format="date" KST 마지막 날짜 포함, 다음날 00시 미만. from보다 이전이면 400. 다음날00시 경계를 표현할 수 없는 날짜는400 VALIDATION_FAILED. |
| query | q | 아니오 | type="string"; 정규화 후 type="string"; maxLength=100 trim 후 최대 100자. 내부 공백 보존, 빈 값=검색 없음. %, _, !는 리터럴. 영문 대소문자 무시. SQL 바인딩과 별개로 %, _, 선택 escape 문자를 모두 escape하여 리터럴 부분 일치 검색. |
| query | page | 아니오 | type="integer"; minimum=0; default=0 0부터 시작. page*size 오버플로 거부. |
| query | size | 아니오 | type="integer"; minimum=1; maximum=100; default=20 대화 개수. |

요청 body: 없음.

| HTTP | 본문·오류 코드 |
| --- | --- |
| 200 | [ConversationPageResponse](schemas/records.md#schema-conversationpageresponse) · 성공.  |
| 401 | AUTHENTICATION_REQUIRED · AUTHENTICATION_REQUIRED; 발생 조건 및 재시도는 operation 설명과 기능 명세 참조. |
| 503 | AUTH_STATE_UNAVAILABLE · AUTH_STATE_UNAVAILABLE; 발생 조건 및 재시도는 operation 설명과 기능 명세 참조. |
| 400 | VALIDATION_FAILED · VALIDATION_FAILED; 발생 조건 및 재시도는 operation 설명과 기능 명세 참조. |
| 403 | GUARDIAN_UNLOCK_REQUIRED · GUARDIAN_UNLOCK_REQUIRED; 발생 조건 및 재시도는 operation 설명과 기능 명세 참조. |
| 404 | RESOURCE_NOT_FOUND · RESOURCE_NOT_FOUND; 발생 조건 및 재시도는 operation 설명과 기능 명세 참조. |
| 429 | RATE_LIMITED · RATE_LIMITED; 발생 조건 및 재시도는 operation 설명과 기능 명세 참조. |

응답 추가 헤더: 429 `Retry-After`: 최소 재시도 대기 초. 실제 남은 차단시간으로 계산.

트랜잭션·재시도: 읽기 전용. 동일 요청 반복 가능.

DB 읽기: `accounts, children, guardian_pins, session_security, conversations, conversation_turns`; 쓰기: ``. 공통 제한 예약과 프레임워크 세션 저장은 별도 공통 처리다.

200 응답 예시 (normal):

<!-- json-example: ConversationPageResponse -->
```json
{
  "data": {
    "items": [
      {
        "conversationId": "33333333-3333-4333-8333-333333333333",
        "childId": "22222222-2222-4222-8222-222222222222",
        "status": "ENDED",
        "startedAt": "2026-09-30T14:00:00+09:00",
        "endedAt": "2026-10-01T00:00:03+09:00",
        "title": "친구와 함께 놀았어요",
        "topic": "친구",
        "summary": "친구와 함께 놀았던 경험을 이야기했어요.",
        "summaryStatus": "READY",
        "endRequestedAt": "2026-10-01T00:00:00+09:00",
        "endReason": "MIDNIGHT"
      }
    ],
    "page": 0,
    "size": 20,
    "hasNext": false
  }
}
```

나머지 4개 상태 예시는 OpenAPI의 이 operation에 함께 제공한다.

<a id="section-6-16"></a>
<a id="operation-getchildconversation"></a>
### 6.16 대화 상세와 전체 발화 페이지

`GET /api/v1/children/{childId}/conversations/{conversationId}` · `getChildConversation` · **담당 Part 1** · A-06 · 권한 G+O+P

아이 및 대화 연결/소유권→PIN 검사. B-07.1 공유안 ENDED만/ACTIVE/CLOSING 제외, 소유권·PIN 통과 후 대상 밖 상세404. B-07.2 공유안 size1\~100/default100/max100·sequence ASC·size+1, afterSequence 초기0/마지막 반환 sequence 다음부터 이어 읽기. 끝은 hasNext=false/nextAfterSequence=null, 전체 내용 임의 잘림 없음. 비공개 원문/음성 없음·요약 PENDING/FAILED/EMPTY와 visibility 유지. 각 페이지 소유권·PIN 재검사, 만료403.100턴은 D05A의 기본/최대 페이지 크기이며 실제 성능 검증은 별도다. D05A: 보호자기록은ENDED만,목록page/size 및상세100턴cursor유지. 수동/자정 CLOSING은 종료 완료 전 제외. 실제 ENDED 이후 요약 완료를 기다리지 않는다. 빈 EMPTY도 기록하며 같은 날 여러 대화는 startedAt+conversationId 정렬을 유지한다. 날짜필터는startedAt의KST날짜(=service_date),endedAt이새벽이어도전날기록에포함.

| 위치 | 필드 | 필수 | 타입·기본값·검증 |
| --- | --- | --- | --- |
| path | childId | 예 | type="string"; format="uuid" 현재 계정 소유 아이, 아니면 404. |
| path | conversationId | 예 | type="string"; format="uuid" childId에 속한 대화, 조합 불일치 404. |
| query | afterSequence | 아니오 | type="integer"; minimum=0; default=0 B-07.2 공유안 채택. 초기0, 다음 요청은 마지막 반환 sequence를 전달하며 그 번호 초과 발화를 sequence ASC로 조회. |
| query | size | 아니오 | type="integer"; minimum=1; maximum=100; default=100 B-07.2 공유안 채택.1\~100턴·기본/최대100, 이어 읽기로 전체 내용을 임의로 자르지 않음. 실측 전 초기값. |

요청 body: 없음.

| HTTP | 본문·오류 코드 |
| --- | --- |
| 200 | [ConversationDetailResponse](schemas/records.md#schema-conversationdetailresponse) · 성공.  |
| 401 | AUTHENTICATION_REQUIRED · AUTHENTICATION_REQUIRED; 발생 조건 및 재시도는 operation 설명과 기능 명세 참조. |
| 503 | AUTH_STATE_UNAVAILABLE · AUTH_STATE_UNAVAILABLE; 발생 조건 및 재시도는 operation 설명과 기능 명세 참조. |
| 400 | VALIDATION_FAILED · VALIDATION_FAILED; 발생 조건 및 재시도는 operation 설명과 기능 명세 참조. |
| 403 | GUARDIAN_UNLOCK_REQUIRED · GUARDIAN_UNLOCK_REQUIRED; 발생 조건 및 재시도는 operation 설명과 기능 명세 참조. |
| 404 | RESOURCE_NOT_FOUND · RESOURCE_NOT_FOUND; 발생 조건 및 재시도는 operation 설명과 기능 명세 참조. |
| 429 | RATE_LIMITED · RATE_LIMITED; 발생 조건 및 재시도는 operation 설명과 기능 명세 참조. |

응답 추가 헤더: 429 `Retry-After`: 최소 재시도 대기 초. 실제 남은 차단시간으로 계산.

트랜잭션·재시도: 읽기 전용. 동일 요청 반복 가능.

DB 읽기: `accounts, children, guardian_pins, session_security, conversations, conversation_turns`; 쓰기: ``. 공통 제한 예약과 프레임워크 세션 저장은 별도 공통 처리다.

200 응답 예시 (normalAndVisibility):

<!-- json-example: ConversationDetailResponse -->
```json
{
  "data": {
    "conversation": {
      "conversationId": "33333333-3333-4333-8333-333333333333",
      "childId": "22222222-2222-4222-8222-222222222222",
      "status": "ENDED",
      "startedAt": "2026-09-30T14:00:00+09:00",
      "endedAt": "2026-10-01T00:00:03+09:00",
      "title": "친구와 함께 놀았어요",
      "topic": "친구",
      "summary": "친구와 함께 놀았던 경험을 이야기했어요.",
      "summaryStatus": "READY",
      "endRequestedAt": "2026-10-01T00:00:00+09:00",
      "endReason": "MIDNIGHT"
    },
    "turns": [
      {
        "turnId": "55555555-5555-4555-8555-555555555555",
        "sequence": 1,
        "status": "SUCCEEDED",
        "childText": "오늘 친구랑 놀았어.",
        "childTextVisibility": "VISIBLE",
        "replyText": "어떤 놀이를 했니?",
        "replyTextVisibility": "VISIBLE",
        "createdAt": "2026-09-30T14:01:00+09:00",
        "completedAt": "2026-09-30T14:01:03+09:00",
        "errorCode": null
      },
      {
        "turnId": "66666666-6666-4666-8666-666666666666",
        "sequence": 2,
        "status": "FAILED",
        "childText": null,
        "childTextVisibility": "OMITTED",
        "replyText": null,
        "replyTextVisibility": "OMITTED",
        "createdAt": "2026-09-30T14:02:00+09:00",
        "completedAt": "2026-09-30T14:02:03+09:00",
        "errorCode": "AI_TIMEOUT"
      },
      {
        "turnId": "77777777-7777-4777-8777-777777777777",
        "sequence": 3,
        "status": "SUCCEEDED",
        "childText": null,
        "childTextVisibility": "OMITTED",
        "replyText": "잠시 쉬었다가 다시 이야기해도 괜찮아.",
        "replyTextVisibility": "VISIBLE",
        "createdAt": "2026-09-30T14:03:00+09:00",
        "completedAt": "2026-09-30T14:03:03+09:00",
        "errorCode": null
      },
      {
        "turnId": "99999999-9999-4999-8999-999999999999",
        "sequence": 4,
        "status": "SUCCEEDED",
        "childText": "친구와 있었던 일을 이야기했어요.",
        "childTextVisibility": "REDACTED",
        "replyText": "어떤 놀이를 했니?",
        "replyTextVisibility": "VISIBLE",
        "createdAt": "2026-09-30T14:04:00+09:00",
        "completedAt": "2026-09-30T14:04:03+09:00",
        "errorCode": null
      }
    ],
    "hasNext": false,
    "nextAfterSequence": null
  }
}
```

나머지 2개 상태 예시는 OpenAPI의 이 operation에 함께 제공한다.
