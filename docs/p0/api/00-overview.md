# 근거·결정·담당과 전체 경로

[전체 API 목차](../Integrated_API_Spec.md) · **담당: 공통 / Part 1·Part 2** · 계약 v3.1 / 문서 분할 2026-10-06

절 번호는 기존 통합 명세의 번호를 유지합니다. 다른 절은 전체 목차에서 찾을 수 있습니다.

## 이 문서에서 찾기

- [1 근거와 결정 상태](#section-1)
- [2 담당과 전체 경로](#section-2)

<a id="section-1"></a>
## 1. 근거와 결정 상태

우선순위는 2026-10-03 F01\~F08 확정 답변·Decision_Record → 이 v3 통합본 → 기존 회의 결정 중 유지 부분 → 이전 문서·제공 원문이다. 원문에 적힌 제안·지시를 사용자의 회의 결정으로 간주하지 않는다. 제공 원문9개의 SHA256 출처를 유지한다. 현재 접근 가능한 파일만 재검증하며 검사 출력에서 실제 수를 확인한다.

| 원문 | 역할 |
| --- | --- |
| `Backend_P0_Common_Spec.md` (통합 전 원문, 별도 보관) | 공통·Part 1 기준 |
| `Backend_P0_Part_1.md` (통합 전 원문, 별도 보관) | 공통·Part 1 기준 |
| `Backend_P0_Part_2.md` (통합 전 원문, 별도 보관) | Part 2 대화·음성 |
| `part1-api.yaml` (통합 전 원문, 별도 보관) | 공통·Part 1 기준 |
| `part1-erd.md` (통합 전 원문, 별도 보관) | 공통·Part 1 기준 |
| `part1-functional-spec.md` (통합 전 원문, 별도 보관) | 공통·Part 1 기준 |
| `part1-review.md` (통합 전 원문, 별도 보관) | 공통·Part 1 기준 |
| `part2-api.md` (통합 전 원문, 별도 보관) | Part 2 대화·음성 |
| `part2-erd.md` (통합 전 원문, 별도 보관) | Part 2 대화·음성 |

확정한 D/F 정책과 변경 근거는 [결정 기록](../Decision_Record.md)을 확인한다.

같은 시작 키 ACTIVE 복구와 다른 키의409 ACTIVE_CONVERSATION_EXISTS+ID는 유지한다. CLOSING은409 CONVERSATION_CLOSING, 종료된 같은 키는409 CONVERSATION_ENDED다. 새 대화는 새 키로 생성한다. D02 답변을 원래 오류 선택지 A에 투표한 것으로 확대하지 않는다. PIN reset 후 기존 오답·차단·요청량 보존도 보수적인 기술 설계이며 사용자의 추가 선택으로 기록하지 않는다.

<a id="section-2"></a>
## 2. 담당과 전체 경로

Part 1은 공통 인증·소유권·PIN과 보호자 기록 API를 맡는다. Part 2는 대화·발화·임시 음성 저장과 읽기 모델을 맡는다. 파트 간 새 HTTP 서비스를 만들지 않으며 AI가 업무 DB를 직접 읽거나 쓰지 않는다. migration 소유자는 ERD를 따른다.

G=로그인, O=아이 소유권/리소스 조합, P=현재 세션 PIN, L=당일 ACTIVE 연결, X=CSRF, V=목적별 일회성 검증 권한. 아이 경로의 G+O에는 PIN이 필요하지 않다. 시간은 KST08:00 이상 다음00:00 미만·당일 대화·예정종료시각 이전을 뜻한다.

| 담당 | 작업 | Method와 경로 | operationId | 권한 | 정상 HTTP |
| --- | --- | --- | --- | --- | --- |
| Part 1 | A-02 | `GET /api/v1/auth/csrf` | [getCsrf](02-auth.md#operation-getcsrf) | 공개 | 200 |
| Part 1 | A-01 | `POST /api/v1/auth/email-verifications` | [issueEmailVerification](02-auth.md#operation-issueemailverification) | X; PIN_SETUP/PIN_RESET은 G | 200 |
| Part 1 | A-01 | `POST /api/v1/auth/email-verifications/verify` | [verifyEmailCode](02-auth.md#operation-verifyemailcode) | X; PIN 목적은 G | 200 |
| Part 1 | A-01 | `POST /api/v1/auth/signup` | [signup](02-auth.md#operation-signup) | X+V(SIGNUP) | 201 |
| Part 1 | A-02 | `POST /api/v1/auth/login` | [login](02-auth.md#operation-login) | X | 200 |
| Part 1 | A-02 | `GET /api/v1/auth/me` | [getCurrentAccount](02-auth.md#operation-getcurrentaccount) | G | 200 |
| Part 1 | A-02 | `POST /api/v1/auth/logout` | [logout](02-auth.md#operation-logout) | G+X | 204 |
| Part 1 | A-03 | `POST /api/v1/auth/password-resets` | [resetPassword](02-auth.md#operation-resetpassword) | X+V(RESET_PASSWORD) | 204 |
| Part 1 | A-04 | `GET /api/v1/children` | [listChildren](03-children.md#operation-listchildren) | G | 200 |
| Part 1 | A-04 | `POST /api/v1/children` | [createChild](03-children.md#operation-createchild) | G+X | 201 |
| Part 1 | A-05 | `POST /api/v1/guardian/pin` | [setupGuardianPin](04-guardian.md#operation-setupguardianpin) | G+X+V(PIN_SETUP) | 204 |
| Part 1 | A-05 | `POST /api/v1/guardian/unlock` | [unlockGuardian](04-guardian.md#operation-unlockguardian) | G+X | 200 |
| Part 1 | A-05 | `POST /api/v1/guardian/lock` | [lockGuardian](04-guardian.md#operation-lockguardian) | G+X | 204 |
| Part 1 | A-05 | `POST /api/v1/guardian/pin/reset` | [resetGuardianPin](04-guardian.md#operation-resetguardianpin) | G+X+V(PIN_RESET) | 204 |
| Part 1 | A-06 | `GET /api/v1/children/{childId}/conversations` | [listChildConversations](05-records.md#operation-listchildconversations) | G+O+P | 200 |
| Part 2 | B-02 | `POST /api/v1/children/{childId}/conversations` | [startChildConversation](06-conversations.md#operation-startchildconversation) | G+O+X+시간 | 201/200 |
| Part 1 | A-06 | `GET /api/v1/children/{childId}/conversations/{conversationId}` | [getChildConversation](05-records.md#operation-getchildconversation) | G+O+P | 200 |
| Part 1 | A-02 | `GET /oauth2/authorization/google` | [startGoogleLogin](02-auth.md#operation-startgooglelogin) | 공개·state 저장 | 302 |
| Part 1 | A-02 | `GET /login/oauth2/code/google` | [completeGoogleLogin](02-auth.md#operation-completegooglelogin) | OAuth state/nonce | 302 |
| Part 2 | B-01 | `GET /api/v1/children/{childId}/home` | [getChildHome](06-conversations.md#operation-getchildhome) | G+O | 200 |
| Part 2 | B-02 | `POST /api/v1/children/{childId}/conversations/{conversationId}/resume` | [resumeChildConversation](06-conversations.md#operation-resumechildconversation) | G+O+X+시간 | 200 |
| Part 2 | B-03/B-04/B-05 | `POST /api/v1/children/{childId}/conversations/{conversationId}/turns` | [submitVoiceTurn](07-voice.md#operation-submitvoiceturn) | G+O+L+X+시간 | 201/200/202 |
| Part 2 | B-05 | `GET /api/v1/children/{childId}/conversations/{conversationId}/turns/{turnId}` | [getVoiceTurn](07-voice.md#operation-getvoiceturn) | G+O+L+시간 | 200 |
| Part 2 | B-05 | `GET /api/v1/children/{childId}/conversations/{conversationId}/turn-requests/{clientRequestId}` | [getVoiceTurnByRequestId](07-voice.md#operation-getvoiceturnbyrequestid) | G+O+L+시간 | 200 |
| Part 2 | B-02/B-04 | `POST /api/v1/children/{childId}/conversations/{conversationId}/end` | [endChildConversation](06-conversations.md#operation-endchildconversation) | G+O+X+기존 연결/복구 | 200/202 |
| Part 2 | B-03/B-05 | `GET /api/v1/children/{childId}/conversations/{conversationId}/turns/{turnId}/audio` | [getTemporaryTurnAudio](07-voice.md#operation-gettemporaryturnaudio) | G+O+L+시간 | 200 |
