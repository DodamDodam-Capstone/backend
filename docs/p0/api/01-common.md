# 공통 HTTP·세션·공유 응답

[전체 API 목차](../Integrated_API_Spec.md) · **담당: 공통 / Part 1 제공, Part 2 적용** · 계약 v3.1 / 문서 분할 2026-10-06

절 번호는 기존 통합 명세의 번호를 유지합니다. 다른 절은 전체 목차에서 찾을 수 있습니다.

## 이 문서에서 찾기

- [3 공통 HTTP와 세션 계약](#section-3)
- [3.1 내부 함수와 무효화](#section-3-1)
- [3.2 원문 구현 계약의 공통 보완](#section-3-2)
- [3.3 PIN_RESET 권한과 오류 경계](#section-3-3)
- [5 응답 객체](#section-5)
- [5.1 Part 1 인증과 프로필](#section-5-1)
- [5.2 공유 ConversationView](#section-5-2)
- [5.3 공유 TurnView](#section-5-3)
- [5.4 보호자와 아이 응답 wrapper](#section-5-4)
- [5.5 상태와 NULL 조합](#section-5-5)

<a id="section-3"></a>
## 3. 공통 HTTP와 세션 계약

| 항목 | 통합 계약 |
| --- | --- |
| 경로·형식 | `/api/v1`, JSON UTF-8. OAuth 두 경로는 framework 경로. 음성 POST는 multipart |
| 인증 | HttpOnly 세션 쿠키. JSESSIONID·Spring Session JDBC를 공통 기준으로 사용. FE `credentials` 포함, 운영 Secure 및 실제 SameSite/CORS/origin은 배포 환경과 확정 |
| CSRF | 공개 인증을 포함한 모든 POST는 `X-XSRF-TOKEN`. 최초 진입·로그인·로그아웃 후 GET auth/csrf로 재획득. OAuth GET은 state/nonce |
| 성공 | `{ "data": ... }`. 201은 Location, 204는 본문 없음. CSRF 응답·OAuth 302·음성 GET 바이너리만 예외 |
| 오류 | `{ "error": { "code", "message", "requestId", "fields" } }`. fields는 `{field,reason}` 배열, 없으면 `[]`. ACTIVE_CONVERSATION_EXISTS 오류만 activeConversationId 확장 |
| 헤더 | 명세된 모든 응답에 `Cache-Control: no-store`와 `X-Request-ID` 적용(공개 인증·실패·204·302·바이너리 포함). error.requestId는 같은 추적 ID. 429는 초 단위 `Retry-After`(최소 1) |
| ID·시각 | 모든 업무 ID·clientRequestId UUID 문자열. 날짜 YYYY-MM-DD. 시각 YYYY-MM-DDTHH:mm:ss+09:00, DB timestamptz. 외부 시각은 같은 순간을 KST로 변환 |
| NULL | 응답 명세에 있는 null 필드를 생략하지 않음. 요청은 허용한다고 명시한 경우만 null. 현재 요청 DTO에는 null 허용 필드 없음 |
| 추가 필드 | Part 1 요청은 미정의 body 필드 400. Part 2 JSON·multipart도 명세 필드만 허용(D01). 응답은 정해진 DTO만 반환 |
| body 없는 POST | Part 1 logout/lock은 0바이트, 내용이 있으면 400. Part 2 resume/end에도 같은 규칙 적용(D01). Content-Type 없는 0바이트 허용 |
| 전송 제한 | Part 1 JSON 16,384바이트 공유안: 실제 UTF-8 body(공백·escape 포함), 압축 허용 시 해제 후에도 검사. Part 2 음성/JSON 제한은 별도 미정 |
| 정규화 | 이름·검색어·관심사 trim. 이메일 trim+전체 lower(Part 1 공유안). 비밀번호·PIN·코드·token은 trim/숫자변환/절단하지 않음 |
| 재시도 | 조회 재시도와 업무 재실행 구분. 401·CSRF 실패·네트워크 오류 후 POST를 무조건 자동 재실행하지 않음. 음성은 아래 키 복구 계약 사용 |
| 로그 | 비밀번호·PIN·code·token·원본 음성·금지 텍스트·raw session ID·내부 binding 미기록. fields/message에도 원문 복사 금지 |

**오류 우선순위:** 전역 요청량 예약은 진입 단계이며 한도 소진 시 429가 먼저 날 수 있다. 전송 크기/Content-Type → CSRF → 로그인 → 구조 검증 → 소유권·리소스 연결 → 보호자 확인(해당 경로) → 업무 상태. `PIN_SETUP`/`PIN_RESET` 검증은 challenge를 읽어 목적을 확인한 후 조건부 로그인을 강제한다. 타인/잘못된 child–conversation–turn 조합은 404로 상태·텍스트를 숨긴다. 유효한 소유자의 아이 대화에서는 ACTIVE/ENDED 확인 후 연결·중복 키를 판정한다.

<a id="section-3-1"></a>
### 3.1 내부 함수와 무효화

| 제공자 → 소비자 | 계약 |
| --- | --- |
| Part 1 → 양쪽 | `requireAuthenticatedSession(request)` → accountId, securityContextId. framework session 존재·principal/account/context 일치·미폐기·계정 session_version 일치·유한 expiry가 있을 때만 기한 검사. NULL expiry도 framework·폐기·버전 검사를 생략하지 않음. 저장소 확인 불가 503 |
| Part 1 → Part 2 | `requireChildAccess(accountId, childId)` → 소유 ChildView. 미소유 404 |
| Part 1 → Part 2 | `getChildContext(childId)` → childId/name/nickname/birthDate/interests/characterId/createdAt. 소유권 검사 후만 호출. AI 호칭은 nickname 매핑, 실명 name 자동 전송 금지. 실제 wire 필드명은 D13 대기 |
| Part 1 → 기록 API | `requireGuardianAccess` → until>now, 현재 pin_version 일치. 실패 403 |
| Part 2 → Part 1 | ConversationView·TurnView 읽기 모델. 허용 텍스트만 저장·검색·요약. query 및 조회 DTO는 Part 1, 저장 스키마·인덱스 migration은 Part 2 |

일반 로그인 시간 만료 없음(`expires_at=null`), 쿠키 365일 유효 사용 갱신, 별도 rememberMe 미도입은 **D04 확정 기준**이다. 프레임워크 실제 수명·저장·쿠키 동작은 운영 전 검증한다. NULL expiry도 로그아웃·폐기·계정 버전 검사를 생략하지 못한다. 보호자 권한은 PIN 성공부터 최대 1,800초 확정값이며 활동으로 연장하지 않는다. 유한 로그인 상한이 있으면 그보다 길게 허용하지 않는다. 종료 복구의 유한 TTL은 로그인과 별개다.

| 사건 | 일반 로그인 | 보호자/PIN_SETUP·PIN_RESET | Part 2 연결 |
| --- | --- | --- | --- |
| 새로고침 | 유효 세션 유지 | 각 권한의 서버 기한 검사 | ACTIVE 연결 유지, 미연결은 resume |
| guardian/lock | 유지 | 현재 guardian 해제·setup_generation 증가·현재 미소비 PIN_SETUP/PIN_RESET 폐기 | ACTIVE 연결 유지 |
| 로그아웃·세션 교체·만료 | 이전 문맥 무효 | 이전 권한 무효 | 이전 연결·종료 복구 무효. 새 로그인은 ACTIVE만 resume |
| 비밀번호 reset | 계정 session_version 증가, 모든 이전 세션 무효 | 모든 기존 권한 무효 | 이전 접근 차단. 허용 AI 결과 저장은 완료 가능 |
| PIN reset 성공 후 | 유지하는 원문 계약 | pin_version 증가로 모든 기존 guardian 무효, 승자를 제외한 모든 미소비 PIN_SETUP/PIN_RESET challenge/token은 같은 트랜잭션에서 폐기 | ACTIVE 연결 유지. 이메일 PIN_RESET 권한으로204 성공; 자동 unlock 없음 |

프레임워크 세션 저장과 업무 DB 변경을 하나의 원자적 트랜잭션이라고 가정하지 않는다. DB 기준의 폐기·버전 검사를 모든 요청 및 긴 AI 처리의 **응답 직전**에 다시 적용한다. 확인 불가이면 결과를 노출하지 않는다.

<a id="section-3-2"></a>
### 3.2 원문 구현 계약의 공통 보완

다음은 새 회의 선택지가 아니라 Part 1 기능 명세(F1)의 보안·동시성 계약을 복원한 것이다. 원문에서 수치 중 D04·D06 확정 범위와 D17 운영 보류 기준을 구분한다.

| 영역 | 반드시 지킬 구현 계약 |
| --- | --- |
| 추적·CORS | X-Request-ID는 서버 생성 UUID이며 error.requestId와 같다. 업무 중복키 clientRequestId와 별개다. 운영 세션 쿠키는 HttpOnly·Secure·Path=/·host-only. credentials=true와 origin `*`를 함께 허용하지 않고, FE에 `Location`, `X-Request-ID`, `Retry-After`를 노출한다. 실제 origin/SameSite만 D-17에서 정한다 |
| 로그인 실패 보상 | 이전 문맥 폐기 후 새 session_security를 등록하고 framework 세션 저장까지 성공한 뒤200. 업무 DB만 성공하고 framework 저장이 실패하면 새 보조 문맥을 폐기하고503. 이전 폐기 권한을 되살려 보상하지 않는다 |
| 보호자 권한 원본 | guardian 시각·PIN 버전·setup generation은 DB session_security가 원본이다. framework에는 인증 principal/색인과 context ID를 두며, 늦게 저장된 serialized HttpSession의 guardian 값으로 DB를 덮어쓰지 않는다 |
| 계정 열거 방지 | 비존재 계정 로그인에도 dummy password hash 비교를 수행한다. 가입된 이메일의 SIGNUP/없는 이메일의 RESET_PASSWORD는 decoy를 저장하고 동일200 구조·크기 및 같은 발송 경로의 일반화 안내를 사용한다. 사용 가능한 번호를 보내지 않으며 decoy 검증은 VERIFICATION_INVALID. 정시간 실행을 보장한다는 의미는 아님 |
| 검증 token | `challengeId.secret`에서 secret 문자열 UTF-8의 SHA-256을 상수 시간 비교한다. token 원문은 DB·URL·query·localStorage·로그에 저장하지 않는다. challengeId만으로 권한을 인정하지 않고 목적·기한·미소비·미폐기 및 PIN_SETUP 계정/context/generation을 검사한다 |
| 최초 PIN 오류 순서 | 기본 권한→구조 검증→기존 PIN→token 판정. 실제 PIN_SETUP을 찾았으면 계정·세션·폐기 판정이 만료·소비보다 우선한다. 둘 다 실패하는 경우 권한 결합 오류403을 먼저 반환한다. token 누락403과 명시적null400을 구분한다 |
| PIN unlock 시각 | 최초 PIN_FAILURE_ACCOUNT row도 account lock 아래 생성한다. 잠금 획득 후 DB `clock_timestamp()`로 차단을 확인하고, 해시 비교 후 유효 세션을 다시 검사한 다음 새 DB 시각으로 성공/오답/guardian 기한을 정한다. 대기 전 Tx 시작 시각으로 기한을 늘리지 않는다 |
| 제한 예약·rollback | API_IP는 별도 커밋; 번호 검증 EMAIL_VERIFY_IP도 challenge 조회 전 별도 커밋. 나머지 키는 정렬해 짧은 Tx로 함께 예약하며 한 키 거부 시 그 단계만 rollback한다. 앞선 IP 예산은 되돌리지 않는다. 모든 예약 뒤 업무 row lock을 잡고 역순 대기를 만들지 않는다 |
| 제한키·재개 | PIN_SETUP/PIN_RESET 각 목적 누적 subject는 accountId+purpose로 세션을 제외하고 cooldown만 accountId+securityContextId+purpose. blocked_until이 미래면 거부; 차단 종료 후에도 유효 window의 예산을 소진했으면 window 종료까지 거부한다. 예산이 남은 유효 window는 기존 count를 이어 사용하고, window와 차단이 모두 끝나면 count=0부터 시작한다. 반복 차단 요청으로 기한을 연장하지 않는다. Retry-After는 응답 생성 시 실제 적용 제한의 최늦은 해제까지 올림한 초, 최소1 |
| Google 식별·경쟁 | 서명·iss·aud·exp·state·nonce와 canonical issuer를 검증한다. 기존 identity의 이메일이 달라도 accounts.email을 자동 변경하지 않는다. 신규 생성 UNIQUE 충돌은 동일(GOOGLE,issuer,subject) 승자만 재조회하고, 다른 identity/같은 email은 ACCOUNT_LINK_REQUIRED; 이메일 자동 연결 금지 |

§6의 해당 operation과 이 표를 함께 구현한다. 아이용 Part 2의 모든 데이터 응답도 전달 직전 유효 세션·소유권·대화 상태·필요한 연결을 재검사하며, 무효화 이후 결과를 노출하지 않는다. 읽기 모델의 `FAILED.errorCode`는 nonnull뿐 아니라 **빈 문자열도 금지**한다. AccountView.email은 정규화된 유효 ASCII 이메일·최대254자다. 모든 비밀번호 경로에서 Unicode 정규화·trim·절단을 금지한다.

<a id="section-3-3"></a>
### 3.3 PIN_RESET 권한과 오류 경계

이메일 재인증은 로그인한 계정의 **현재 이메일**로만 발급한다. Google-only 계정도 이미 PIN이 있다면 동일 경로를 쓴다. PIN_SETUP과 PIN_RESET은 목적을 분리하고, 계정·현재 securityContext·setup_generation 및 PIN_RESET 발급 당시 pin_version에 결합한다. 기존 setup_* 컬럼명은 두 PIN 목적에 공용으로 쓰며 새 계정 복구 서비스를 만들지 않는다.

잠금 순서는 accounts → session_security → guardian_pins → challenge다. 각 단계에서 정규화된 현재 accounts.email과 email_verifications.email도 비교한다. 최종 token 소비는 PIN hash 변경·pin_version 증가·다른 미소비 PIN 권한 폐기와 같은 Tx다. 소비한 승자는 token_consumed_at만 기록하고 invalidated_at을 함께 넣지 않는다. 전체 업무 실패 시 모두 rollback한다.

최초 PIN_SETUP은 기존 누락403 계약을 유지한다. PIN_RESET의 verificationToken은 required이므로 누락/null400 VALIDATION_FAILED다. 잘못된 목적·해시·만료·소비는400 VERIFICATION_INVALID. 실제 해당 PIN 목적의 계정·문맥·generation·폐기·PIN 버전 결합이 틀리면403이며 만료/소비보다 우선한다. PIN_RESET 발급/최종 소비 시 PIN이 없으면409 PIN_NOT_SET. 재설정 성공은 일반 로그인·당일 대화 연결을 유지하고, 이전 모든 guardian을 버전으로 무효화한다. 기존 오답·차단·요청량은 유지하며 별도로 새 PIN을 확인해야 한다.

lock/logout/세션 교체/password reset/키 교체/cleanup은 PIN_RESET에도 기존 PIN_SETUP과 같은 폐기·참조 규칙을 적용한다. 재발급은 해당 목적의 이전 미소비 번호/token을 폐기한다. 발급·번호 확인·최종 소비는 메일/AI 호출 중 업무 DB 잠금을 유지하지 않는다.


<a id="section-5"></a>
## 5. 응답 객체

별도 표시가 없으면 표의 필드는 모두 필수이며 null도 생략하지 않는다. `data` 내부를 설명한다. 공통 객체를 두 파트가 별도로 변형하지 않는다.

<a id="section-5-1"></a>
### 5.1 Part 1 인증과 프로필

| 객체 | 필드와 타입 | 조건 |
| --- | --- | --- |
| CsrfTokenResponse | headerName:string=`X-XSRF-TOKEN`, parameterName:string=`_csrf`, token:nonempty string | 유일한 인증 JSON wrapper 예외, data 없음 |
| AccountView | accountId:UUID, email:string, childId:UUID/null, hasPin:boolean, guardianUnlockedUntil:KST/null | childId=null이면 아이 등록. 만료/무효 guardian 시각은 null. 별도 hasChild 없음 |
| ChildView | childId:UUID, name:string, nickname:string, birthDate:date, gender:MALE/FEMALE, interests:string[], characterId:string, createdAt:KST | 생성 요청의 정규화 후 제약. interests는 생략 대신 []. 필드 모두 nonnull |
| VerificationIssue | challengeId:UUID, expiresAt:KST | 가입 유무를 드러내지 않는 decoy도 동일 형태 |
| VerificationGrant | verificationToken:string, tokenExpiresAt:KST | token은 `challengeId.secret`; secret=CSPRNG 32바이트를 패딩 없는 Base64url 43자로 표현. 응답 유실 시 재발급 |
| SignupResult | accountId:UUID | 가입 후 별도 로그인 |
| GuardianUnlock | guardianUnlockedUntil:KST | 현재 세션만, 실제 로그인 상한과 PIN 버전 검사 |
| ChildrenList | items:ChildView[] | P0 0\~1개, 빈 목록도 200 |

<a id="section-5-2"></a>
### 5.2 공유 ConversationView

| 필드 | 타입 | NULL | 의미 |
| --- | --- | --- | --- |
| conversationId | UUID string | 불가 | 대화 ID |
| childId | UUID string | 불가 | 소유 아이 |
| status | ACTIVE / CLOSING / ENDED | 불가 | 대화 상태 |
| startedAt | KST timestamp | 불가 | 날짜 검색 기준 |
| endRequestedAt | KST timestamp | 허용 | ACTIVE null, CLOSING/ENDED는 최초 종료 경계 필수·불변 |
| endReason | MANUAL / MIDNIGHT | 허용 | ACTIVE null, CLOSING/ENDED 필수·최초 사유 불변 |
| endedAt | KST timestamp | 허용 | ACTIVE/CLOSING null, ENDED 실제 완료시각 필수 |
| title | string | 허용 | READY에서만 허용된 제목 |
| topic | string | 허용 | READY에서만 허용된 주제 |
| summary | string | 허용 | READY에서만 허용된 요약 |
| summaryStatus | NOT_STARTED / PENDING / READY / FAILED / EMPTY | 불가 | 아래 상태표 |

<a id="section-5-3"></a>
### 5.3 공유 TurnView

| 필드 | 타입 | NULL | 의미 |
| --- | --- | --- | --- |
| turnId | UUID string | 불가 | 발화 ID |
| sequence | integer ≥1 | 불가 | 서버 순번, 실패 번호 재사용 없음 |
| status | PROCESSING / SUCCEEDED / FAILED | 불가 | 처리 상태 |
| childText | string | 허용 | 저장·표시 모두 허용된 아이 텍스트 |
| childTextVisibility | VISIBLE / REDACTED / OMITTED | 불가 | 아이 텍스트의 개별 판정 |
| replyText | string | 허용 | 저장·표시 모두 허용된 답변 |
| replyTextVisibility | VISIBLE / REDACTED / OMITTED | 불가 | 답변의 개별 판정 |
| createdAt | KST timestamp | 불가 | 접수 시각 |
| completedAt | KST timestamp | 허용 | PROCESSING null, 완료 시 필수 |
| errorCode | string | 허용 | FAILED는 고정 오류 코드, 나머지 null |

TurnView에는 clientRequestId, audio, topicSuggestions, 내부 위험 신호, 해시·처리 기한을 넣지 않는다. 텍스트 길이 한도는 AI/Part 2와 미정이며 저장 단계에서 검사하고 임의 절단하지 않는다.

<a id="section-5-4"></a>
### 5.4 보호자와 아이 응답 wrapper

| 객체 | 필드와 타입 | 범위·조건 |
| --- | --- | --- |
| ConversationPage | items:ConversationView[], page:integer≥0, size:integer1\~100, hasNext:boolean | Part 1. D05A: ENDED만 포함. 빈 items 허용 |
| ConversationDetail | conversation:ConversationView, turns:TurnView[], hasNext:boolean, nextAfterSequence:integer≥1/null | Part 1. ENDED만. turns 최대100, sequence ASC. hasNext=true이면 마지막 반환 sequence, false이면 null. 빈 turns면 false/null |
| ChildHome | child:{childId:UUID,name:string,nickname:string,characterId:string}, greeting:string, joinedAt:KST, daysTogether:integer≥1, activeConversationId:UUID/null, menus:{code,status}[], availability:ConversationAvailability | Part 2. TALK은 canEnter에 따라 AVAILABLE/UNAVAILABLE, PLAY/COMING_SOON. 과거 텍스트 없음 |
| StartConversationResult | conversationId:UUID, status:ACTIVE, startedAt:KST, serviceDate:date, scheduledEndAt:KST | Part 2. 최초·동일 키 모두 같은 형태, 과거 발화 없음 |
| ResumeConversationResult | conversationId:UUID, status:ACTIVE, startedAt:KST, serviceDate:date, scheduledEndAt:KST, turns:TurnView[], processingTurnId:UUID/null | Part 2. turns sequence ASC, 없으면 []. PROCESSING 최대1. 음성·주제 제안은 발화 POST 완료/GET wrapper에만 포함하며 resume에는 없음 |
| ChildTurnResult | turn:TurnView, audio:TemporaryAudio/null, topicSuggestions:string[] | Part 2. POST 완료 및 두 GET 공통. 처리 중은 turn PROCESSING, audio=null, topicSuggestions=[] |
| TurnAccepted | turnId:UUID, clientRequestId:UUID, status:PROCESSING, statusUrl:string | Part 2 POST 202. statusUrl은 해당 turn GET 상대 경로 |
| TemporaryAudio | url:string, contentType:string, expiresAt:KST | Part 2. 인증된 audio GET 상대 경로, 별도 음성 허용·수명 내에서만. MIME D13/D14 확정 전 예시는 계약값 아님 |
| EndPendingReceipt | conversationId:UUID, status:CLOSING | Part 2. end202, Retry-After≥1 후 동일 end 재확인. 과거 내용·음성·요약 없음 |
| EndReceipt | conversationId:UUID, status:ENDED, endedAt:KST, summaryStatus:PENDING/READY/FAILED/EMPTY | Part 2. 제목·주제·요약 본문·발화·음성 없음. endedAt 불변, summaryStatus 변경 가능 |

Home의 joinedAt=children.createdAt, daysTogether=KST 오늘 날짜와 등록 날짜 차이+1, greeting=backend 템플릿은 **D06 기준**이다. Home.child는 이름·애칭·캐릭터를 포함하며 아이 표시·Home.greeting은 nickname을 사용한다. 새 대화의 첫 인사는 FE 고정 템플릿 “{nickname}, 오늘 만나서 반가워!”이며 AI/TTS/발화 저장·개수·요약 입력에 포함하지 않는다. 실명 인증을 추가하지 않는다.

`topicSuggestions`는 모든 응답 상태에서 항상 빈 배열[]이다(D09). 저장 컬럼과 추천 UI·입력은 제외한다. 요약의 topic은 별개의 기존 필드이며 유지한다. TTS 장애는 FAILED/audio=null이고 허용 텍스트만 보존한다(D10).

<a id="section-5-5"></a>
### 5.5 상태와 NULL 조합

| 대상 상태 | 반드시 만족할 조건 |
| --- | --- |
| 대화 ACTIVE | endRequestedAt/endReason/endedAt=null, summaryStatus=NOT_STARTED, title/topic/summary=null |
| 대화 CLOSING | endRequestedAt/endReason 필수, endedAt=null, summaryStatus=NOT_STARTED, title/topic/summary=null |
| 대화 ENDED | endRequestedAt/endReason/endedAt 필수, endedAt≥endRequestedAt≥startedAt, summaryStatus≠NOT_STARTED |
| summary READY | title/topic/summary 모두 저장·표시 허용 nonnull 문자열 |
| summary NOT_STARTED/PENDING/FAILED/EMPTY | title/topic/summary 모두 null |
| turn PROCESSING | completedAt/errorCode/childText/replyText=null, 두 visibility=OMITTED |
| turn SUCCEEDED | completedAt 필수, errorCode=null. 텍스트별 공개 여부 별도 |
| turn FAILED | completedAt와 errorCode 필수. 허용 텍스트 존재 여부는 실패 단계/AI 계약에 따름 |
| visibility VISIBLE | 해당 저장·표시 허용 원문 nonnull |
| visibility REDACTED | AI 계약이 허용한 가공 텍스트만 nonnull, 가공 전 원문 미보관 |
| visibility OMITTED | 해당 텍스트=null. 반대편 허용 텍스트는 유지 가능 |

저장만 허용되거나 허용 정보가 없으면 그 텍스트는 OMITTED/null이다. title/topic/summary도 모두 허용 검사를 통과해야 READY다. 텍스트 허용만으로 음성 허용을 추정하지 않는다. 금지 원문은 DB·검색·요약·AI 이전 문맥·일반 로그에 재보관하지 않는다.

Home availability는 timeZone, serverTime, serviceDate, opensAt, closesAt, canEnter, reason, nextOpensAt의8필드다. serverTime의 KST 날짜가 serviceDate, opensAt은 당일08시, closesAt은 다음날00시다. 00\~08에는 canEnter=false/OUTSIDE_SERVICE_HOURS/nextOpensAt=당일08시다. 운영시간 중 CLOSING(이전 날짜 포함)은 false/CONVERSATION_CLOSING/null이며, 정상 진입은 true/null/null이다. 메뉴는 TALK,PLAY 순서로 각1개만 반환한다. canEnter=false이면 activeConversationId=null이다.

Start/Resume의 serviceDate와 scheduledEndAt은 같은 대화에 불변이다. ConversationView는 endRequestedAt/endReason을 더한11필드이며 serviceDate/scheduledEndAt은 추가하지 않는다. MANUAL 경계는 scheduledEndAt 미만, MIDNIGHT 경계는 scheduledEndAt과 같다. endedAt은 drain 뒤 실제 종료시각으로 경계 이상이며 수동 종료는 자정 이전일 수 있다. 마무리 중 DB/API 상태는 CLOSING이다. 요약 완료는 ENDED나 새 대화 시작의 조건이 아니다.
