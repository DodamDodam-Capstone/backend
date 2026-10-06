# 통합 Mermaid 다이어그램

[전체 API 목차](../Integrated_API_Spec.md) · **담당: 공통 / Part 1·Part 2** · 계약 v3.1 / 문서 분할 2026-10-06

절 번호는 기존 통합 명세의 번호를 유지합니다. 다른 절은 전체 목차에서 찾을 수 있습니다.

## 이 문서에서 찾기

- [13 통합 API 다이어그램](#section-13)
- [13.1 담당과 데이터 경계](#section-13-1)
- [13.2 요청 검사와 공통 오류 우선순위](#section-13-2)
- [13.3 이메일 번호 확인과 일회성 업무 권한](#section-13-3)
- [13.4 로그인·프로필·아이 홈 진입과 로그아웃](#section-13-4)
- [13.5 PIN 설정·확인·잠금과 이메일 재설정](#section-13-5)
- [13.6 비밀번호 재설정과 모든 이전 로그인 무효화](#section-13-6)
- [13.7 새 대화 시작과 화면 재진입 복원](#section-13-7)
- [13.8 음성 발화 접수와 동기·비동기 결과](#section-13-8)
- [13.9 발화 중복 키와 응답 유실 복구](#section-13-9)
- [13.10 수동·자정 종료와 기존 세션의 종료 확인](#section-13-10)
- [13.11 임시 응답 음성의 시간·권한·수명 검사](#section-13-11)
- [13.12 텍스트·요약·음성의 개별 공개 허용](#section-13-12)
- [13.13 보호자 목록 검색과 전체 발화 이어 읽기](#section-13-13)
- [13.14 26개 API 동작과 도식 매핑](#section-13-14)

<a id="section-13"></a>
## 13. 통합 API 다이어그램

Part 1과 Part 2를 연결하는 13개 Mermaid 다이어그램이다. OpenAPI3.0.3(문서 v3.1.0)의 담당, 권한, 대화 시작·종료, AI 처리, 종료 복구 흐름을 설명한다. 마지막 표는 26개 API 동작을 각 도식에 연결한다.

공통 전제: 요청 구조, 요청 제한, 로그인, 소유권 검사를 통과해야 업무를 처리한다. 아이용 내용과 음성을 전달하기 직전에도 현재 시간, 세션, 대화 상태와 연결을 다시 확인한다. 도식은 필드별 required/null 규칙이나 오류 스키마를 대신하지 않는다. FE 적용과 남은 AI·운영 연동 사항은 FE·BE 인계서(별도 전달)를 함께 확인한다.

<a id="section-13-1"></a>
### 13.1. 담당과 데이터 경계

<!-- diagram: api-integrated-ownership -->
```mermaid
flowchart TD
    FE["아이 화면 / 보호자 대시보드"] --> SEC["Part 1 공통 검사<br/>로그인 · CSRF · 소유권 · 요청 제한"]
    SEC --> AUTH["Part 1 인증·프로필<br/>이메일 · Google · 아이 · PIN"]
    SEC --> REC["Part 1 보호자 기록 GET<br/>현재 PIN 권한 + ENDED"]
    SEC --> TALK["Part 2 아이 대화<br/>Home · start · resume · turn · end 확인 · audio"]
    AUTH --> ADB["Part 1 소유 데이터<br/>계정 · 아이 · 인증 · 보안 문맥"]
    ADB -->|"검증된 계정과 보안 문맥"| TALK
    TALK --> TDB["Part 2 소유 데이터<br/>일일 대화 · 발화 · 세션 연결 · 임시 음성"]
    TDB -->|"허용된 ConversationView / TurnView"| REC
    TALK --> TIME["KST 08:00부터 자정 전까지<br/>접수·연결·내용 전달 시간 검사"]
    TIMER["수동 end · 자정 · 결과 완료 · 재시작 정리"] --> CLOSE["수동/자정 공통 종료 조정<br/>CLOSING · 기존 발화 정리 후 ENDED"]
    CLOSE --> TDB
    TALK -->|"커밋 후 1회 실행 시도<br/>DB 잠금 없음"| AI["AI 서비스"]
    AI -->|"결과 + 필드별 허용 정보"| TALK
```

같은 conversations 경로도 GET은 Part 1 보호자 기록, POST는 Part 2 아이 대화 시작이다. 아이 대화에는 보호자 PIN을 요구하지 않는다. 파트 간 새 HTTP 서비스를 추가하지 않으며 AI가 backend DB를 직접 읽고 쓰지 않는다. 화면 이탈은 대화 종료나 기록 삭제가 아니다.

연결: API §2, §3.1, ERD 담당표 · FE·BE 인계서(별도 전달).

<a id="section-13-2"></a>
### 13.2. 요청 검사와 공통 오류 우선순위

<!-- diagram: api-integrated-request-checks -->
```mermaid
flowchart TD
    R["요청 진입"] --> IP["전역 IP 예산 선행 예약<br/>한도 초과: 429 + Retry-After"]
    IP --> TR["전송 크기 / Content-Type<br/>초과 413 · 미지원 415"]
    TR --> CS["POST의 CSRF 검사<br/>실패: 403 CSRF_INVALID"]
    CS --> AU["필요한 유효 로그인 검사<br/>무효 401 · 확인 불가 503"]
    AU --> SH["body / path / query 구조 검사<br/>실패: 400 VALIDATION_FAILED"]
    SH --> OW["아이 소유권 · 리소스 조합<br/>부재·타인·불일치: 404"]
    OW --> MODE{"요청 종류"}
    MODE -->|"보호자 기록"| PIN["현재 guardian 권한<br/>없음·만료: 403 GUARDIAN_UNLOCK_REQUIRED"]
    PIN --> REC["ENDED만 조회<br/>ACTIVE/CLOSING 상세: 404"]
    MODE -->|"아이 발화 / 음성 / resume"| ST{"이미 ENDED?"}
    ST -->|"예"| EN["409 CONVERSATION_ENDED"]
    ST -->|"아니오"| HR{"KST 08:00 이상<br/>다음 자정 미만?"}
    HR -->|"아니오"| CLOSED["403 SERVICE_HOURS_CLOSED"]
    HR -->|"예"| DAY{"당일 serviceDate이며<br/>now가 scheduledEndAt 미만?"}
    DAY -->|"아니오"| OLD["409 CONVERSATION_CLOSING"]
    DAY -->|"예"| ACTIVE{"status=ACTIVE?"}
    ACTIVE -->|"아니오: CLOSING"| OLD
    ACTIVE -->|"예"| KIND{"resume 요청?"}
    KIND -->|"예"| BIND["현재 세션을 명시적으로 연결<br/>도식07"]
    KIND -->|"아니오"| LINK["현재 세션 연결 검사<br/>미연결: 403 CHILD_SESSION_REQUIRED"]
    LINK --> WORK["중복 키 · 발화 상태 · 음성 수명 검사"]
    MODE -->|"Home / start / end 확인 / 기타"| SPEC["각 API의 예외와 업무 규칙<br/>도식04 · 07 · 10"]
    CF["GET auth/csrf<br/>초기 진입·로그인·로그아웃 후 획득"] -.-> CS
```

아이 내용 접근의 업무 검사 순서는 **ENDED 409 → 이용 시간 외 403 → CLOSING 또는 지난 일자409 → 미연결 403**이다. 다음 날 08시가 되어도 전날 대화가 다시 열리지 않는다. Home과 보호자 기록은 대화 이용 시간 제한 대상이 아니며, POST/end는 내용 없는 수동 종료·종료 확인에 한해 시간 외 예외를 적용한다. start의 기존 키와 새 키 분기는 도식07을 따른다.

이메일 인증의 PIN_SETUP/PIN_RESET은 입력 목적 또는 challenge를 식별한 뒤 유효 로그인을 강제한다. 타인 리소스의 상태를 먼저 검사해 409로 노출하지 않는다. 별도 인증 예산도 업무 전에 예약하며 앞서 커밋한 IP 예산을 후속 실패로 되돌리지 않는다.

연결: API §3, §3.2, §6.1, §8, §9 · FE·BE 인계서(별도 전달).

<a id="section-13-3"></a>
### 13.3. 이메일 번호 확인과 일회성 업무 권한

<!-- diagram: api-integrated-email-verification -->
```mermaid
sequenceDiagram
    participant FE as 앱
    participant P1 as Part 1
    participant DB as PostgreSQL
    participant MAIL as SMTP
    FE->>P1: POST email-verifications, purpose와 필요한 email
    P1->>P1: 공개 목적 또는 로그인 PIN 목적의 권한 검사
    Note over P1,DB: PIN 목적은 계정·현재 문맥·generation 결합, PIN_RESET은 발급 시 PIN 버전도 저장
    P1->>DB: 예산 예약과 범위 잠금, 기존 미소비 권한 폐기 및 새 challenge 커밋
    P1->>MAIL: 커밋 뒤 직접 발송, DB 잠금 없음
    alt 발송 수락
        P1-->>FE: 200 challengeId와 expiresAt
    else 발송 실패 또는 timeout 또는 결과 불명확
        P1->>DB: 해당 challenge 폐기
        P1-->>FE: 503 EMAIL_DELIVERY_UNAVAILABLE
    end
    Note over FE,P1: 다음 단계는 유효한 번호를 받은 경우만 진행
    FE->>P1: POST email-verifications/verify, challengeId와 code
    P1->>DB: 예산 예약과 challenge 잠금, 목적별 권한·기한·번호 검사
    alt 번호와 권한이 유효
        P1->>DB: 번호 재사용 금지, token hash와 별도 만료 시각 저장
        P1-->>FE: 200 verificationToken과 tokenExpiresAt
    else 실제 오답
        P1->>DB: 오답과 한도 도달 시 폐기 상태를 커밋
        P1-->>FE: 400 VERIFICATION_INVALID 또는 한도 도달 429
    end
    Note over FE,DB: 검증 토큰은 발급 목적에 맞는 업무에서만 한 번 소비
    FE->>P1: signup / password-resets / guardian/pin / guardian/pin/reset
    P1->>DB: 최신 권한 재검사, token 소비와 업무 변경을 같은 Tx로 커밋
    P1-->>FE: signup 201, 나머지 성공 204
```

SIGNUP/RESET_PASSWORD는 공개 목적이고 PIN_SETUP/PIN_RESET은 유효 로그인이 필요하다. 두 PIN 목적은 현재 계정·securityContextId·setup_generation에 결합하며 PIN_RESET은 현재 pin_version이 발급 snapshot과 일치해야 한다. 실제 PIN challenge의 계정·문맥·폐기·generation/version 오류는 403이며 만료·소비 오류보다 먼저 판정한다. 목적·번호·토큰 검증 실패는 계약에 따라 400이다.

가입의 추가 동의·보호자 정보 수집은 P0에서 제외한다. signup 201은 자동 로그인을 포함하지 않는다. 번호/토큰 각 600초, 재발급 60초, 실제 오답 5회는 기존 기술 기준을 재사용하며 D-17 운영 검증은 남아 있다. 공개 목적 decoy는 사용 가능한 번호나 토큰을 만들지 않는다. 토큰 원문을 URL, query, localStorage, 일반 로그에 넣지 않는다.

연결: API §4.1\~4.3, §6.2\~6.4, §6.8, §6.11, §6.14 · FE·BE 인계서(별도 전달).

<a id="section-13-4"></a>
### 13.4. 로그인·프로필·아이 홈 진입과 로그아웃

<!-- diagram: api-integrated-login-entry -->
```mermaid
flowchart TD
    EM["POST auth/login<br/>email + password"] --> EC{"자격 일치?"}
    EC -->|"아니오"| LF["401 LOGIN_FAILED<br/>비존재 계정도 dummy hash 비교"]
    EC -->|"예"| CT["이전 문맥 폐기 · 세션 교체<br/>새 보안 문맥과 framework 저장"]
    GS["GET oauth2/authorization/google"] --> STATE{"state 저장과 설정 정상?"}
    STATE -->|"아니오"| S503["503 AUTH_STATE_UNAVAILABLE"]
    STATE -->|"예"| GOOGLE["302 Google 로그인 화면"]
    GOOGLE --> GC["GET login/oauth2/code/google<br/>서명 · iss · aud · exp · state · nonce 검사"]
    GC --> ID{"기존 identity 또는<br/>검증된 신규 email로 가입 가능?"}
    ID -->|"아니오"| GF["고정 실패 URL로 302<br/>사유별 허용 reason은 §6.18"]
    ID -->|"예"| CT
    CT --> SAVE{"로그인 저장 성공?"}
    SAVE -->|"예"| OK["이메일 200 / Google 성공 302<br/>CSRF 재획득"]
    SAVE -->|"아니오"| BAD["새 보조 문맥 폐기<br/>이메일 503 / Google 실패 302"]
    OK --> ME["GET auth/me · GET children<br/>계정 진입 상태와 아이 0~1개"]
    ME --> CHILD{"아이 등록됨?"}
    CHILD -->|"아니오"| CREATE["POST children · name/nickname 별도<br/>201 · 아이 1명 동시 중복409"]
    CREATE --> LOCK["아이 화면 전환 전 guardian/lock 완료"]
    CHILD -->|"예"| LOCK
    LOCK --> HOME["GET children/childId/home<br/>availability · 당일 ACTIVE ID · 과거 내용 없음"]
    HOME --> NEXT["진입 가능하면 Home → start 또는 resume<br/>시간 외에는 대화 버튼 비활성"]
    ME --> LOGOUT["POST auth/logout<br/>현재 문맥과 PIN 목적 권한 폐기"]
    LOGOUT --> OUT["204 · 쿠키와 CSRF 정리<br/>CSRF 재획득"]
```

일반 로그인에는 서비스 차원의 idle/absolute 상한을 두지 않고, 쿠키는 365일로 유효한 사용 시 갱신한다. 이 설정이 logout·세션 폐기·비밀번호 변경을 무시하는 뜻은 아니다. 유효한 framework 세션과 DB 보안 문맥이 모두 필요하며 DB 행만으로 로그인을 복원하지 않는다.

Google 기존 identity는 이메일이 달라도 같은 identity로 식별하며 계정 이메일을 자동 변경하거나 이메일만으로 다른 identity에 연결하지 않는다. 신규 가입은 검증된 email을 요구하고, 추가 동의·보호자 정보 및 별도 가입 정보 보완 화면은 P0에 넣지 않는다. OAuth 콜백의 인증·계정·세션 저장 실패는 허용된 고정 실패 URL로 302이며, 허용 URL 설정 자체가 없으면 503이다. URL에는 토큰이나 내부 오류 원인을 넣지 않는다.

연결: API §6.1, §6.5\~6.10, §6.17\~6.18, §7.1 · FE·BE 인계서(별도 전달).

<a id="section-13-5"></a>
### 13.5. PIN 설정·확인·잠금과 이메일 재설정

<!-- diagram: api-integrated-guardian-access -->
```mermaid
flowchart TD
    IN["유효 로그인"] --> HAS{"PIN 존재?"}
    HAS -->|"아니오"| SETAUTH["PIN_SETUP 이메일 번호 확인<br/>현재 계정·문맥·generation의 토큰"]
    SETAUTH --> SET["POST guardian/pin<br/>PIN 생성 + token 소비 + 남은 설정 권한 폐기"]
    SET --> LOCKED["guardian 잠김<br/>설정·재설정 뒤 자동 unlock 없음"]
    HAS -->|"예"| LOCKED
    LOCKED --> UN["POST guardian/unlock<br/>PIN과 기존 오답 차단 상태 검사"]
    UN --> BLOCK{"차단 중?"}
    BLOCK -->|"예"| RATE["429 RATE_LIMITED<br/>PIN 비교와 차단 연장 없음"]
    BLOCK -->|"아니오"| MATCH{"PIN 일치와<br/>현재 세션 유효성 확인?"}
    MATCH -->|"실제 오답"| FAIL["오답 상태 커밋<br/>400 PIN_INVALID / 한도 도달 429"]
    MATCH -->|"세션 무효 / 확인 불가"| DENY["401 / 503<br/>guardian 권한 부여 금지"]
    MATCH -->|"예"| OPEN["200 guardianUnlockedUntil<br/>현재 세션만 1,800초 · 활동 연장 없음"]
    OPEN -->|"기한 도달"| LOCKED
    OPEN --> GL["POST guardian/lock"]
    GL --> CLEAR["guardian clear + generation 증가<br/>현재 PIN_SETUP / PIN_RESET 폐기"]
    CLEAR --> LOCKED
    LOCKED -->|"PIN을 잊음"| REAUTH["PIN_RESET 이메일 번호 확인<br/>계정·문맥·generation·PIN 버전 snapshot 결합"]
    REAUTH --> RESET["POST guardian/pin/reset<br/>verificationToken + newPin"]
    RESET --> CHECK["잠금 후 현재 권한·목적·버전·기한 재검사"]
    CHECK --> TX["같은 Tx: token 소비 + 새 PIN hash + pin_version 증가<br/>다른 미소비 PIN_SETUP / PIN_RESET 폐기"]
    TX --> DONE["204 · 기존 guardian 권한 모두 무효<br/>일반 로그인·대화 연결·오답 차단 유지"]
    DONE --> LOCKED
```

PIN_RESET은 PIN_SETUP 토큰으로 대신할 수 없다. PIN이 없으면 409 PIN_NOT_SET, 토큰 누락/null은 400이다. 실제 PIN_RESET의 계정·문맥·generation·PIN 버전·폐기 불일치는 403 PIN_RESET_AUTHORIZATION_REQUIRED, 그 밖의 목적·해시·기한·소비 검증 실패는 400이다. 성공 토큰은 consumed로 남기고 같은 토큰에 폐기 상태를 덧붙이지 않는다. hash 저장이나 DB 변경 실패 시 전체 업무를 롤백한다.

PIN 재설정은 일반 로그인과 당일 대화 연결을 유지하며 자동 unlock을 하지 않는다. 오답 차단과 요청 예산도 유지하는 기술 기준을 적용한다. 오답 차단 중에도 PIN_RESET 이메일 인증은 별도 이메일/IP 제한 안에서 가능하다. 응답 유실 시 새 PIN으로 unlock을 확인하되 남아 있는 차단을 준수한다. guardian/lock은 현재 PIN 목적 권한도 폐기하므로 재인증 화면에서 무조건 호출하지 않는다.

연결: API §3.1\~3.2, §6.11\~6.14 · FE·BE 인계서(별도 전달).

<a id="section-13-6"></a>
### 13.6. 비밀번호 재설정과 모든 이전 로그인 무효화

<!-- diagram: api-integrated-password-reset -->
```mermaid
flowchart TD
    REQ["POST auth/password-resets<br/>RESET_PASSWORD token + 새 password"] --> CK["잠금 후 token 목적·기한·소비·계정 version 검사"]
    CK --> PW{"비밀번호 로그인 수단 있음?"}
    PW -->|"아니오"| SOCIAL["409 PASSWORD_RESET_NOT_AVAILABLE<br/>Google 안내 · token 미소비"]
    PW -->|"예"| TX["같은 업무 Tx<br/>hash 변경 + token 소비 + session_version 증가<br/>다른 RESET_PASSWORD / PIN_SETUP / PIN_RESET 폐기"]
    TX --> COMMIT{"커밋 성공?"}
    COMMIT -->|"아니오"| ROLLBACK["업무 변경과 token 소비 모두 rollback"]
    COMMIT -->|"예"| INVALID["모든 이전 로그인·guardian·대화 접근 무효<br/>매 요청 DB version 검사"]
    INVALID --> DONE["204 · 자동 로그인 없음<br/>새 password로 명시적 로그인"]
    INVALID --> DATA["대화·발화 데이터는 유지<br/>이미 접수된 허용 AI 결과 저장 가능"]
    DATA --> HIDE["무효 세션에는 결과 전달 금지<br/>당일 ACTIVE·이용 시간 안에서만 새 세션 resume"]
```

본인 확인 전에 계정 존재나 Google-only 여부를 구분해 알려주지 않는다. 물리 세션 삭제가 늦어져도 DB version 검사로 이전 접근을 거부한다. 비밀번호 재설정은 저장된 대화를 삭제하지 않지만, 새 로그인으로 전날 대화나 종료 복구 권한을 얻을 수는 없다. PIN 재설정이 일반 로그인을 유지하는 규칙과 구분한다.

연결: API §6.8, §3.1, §8.1 · FE·BE 인계서(별도 전달).

<a id="section-13-7"></a>
### 13.7. 새 대화 시작과 화면 재진입 복원

<!-- diagram: api-integrated-start-resume -->
```mermaid
flowchart TD
    HOME["GET Home · availability 확인"] --> AVAILABLE{"08~24이며 미종료 상태 허용?"}
    AVAILABLE -->|"아니오"| WAIT["버튼 비활성 · ID=null<br/>CLOSING은 CONVERSATION_CLOSING<br/>시간 밖 OUTSIDE_SERVICE_HOURS 우선"]
    AVAILABLE -->|"예"| ID{"접근 가능한 ACTIVE ID?"}
    ID -->|"예"| RESUME["POST resume · 0바이트 + CSRF"]
    ID -->|"아니오"| START["POST start · clientRequestId + CSRF"]
    START --> LOCK["아이 잠금 · 시간 검사<br/>이전 날짜 미종료도 종료 조정"]
    LOCK --> KEY{"기존 시작 키 상태?"}
    KEY -->|"ENDED"| ENDED["409 CONVERSATION_ENDED"]
    KEY -->|"CLOSING"| CLOSING["409 CONVERSATION_CLOSING"]
    KEY -->|"ACTIVE"| BOUND{"현재 연결?"}
    BOUND -->|"예"| SAME["200 기존 시작 정보"]
    BOUND -->|"아니오"| NEED["403 CHILD_SESSION_REQUIRED → resume"]
    KEY -->|"없음"| OLD{"아이의 다른 미종료 상태?"}
    OLD -->|"ACTIVE"| EXISTS["409 ACTIVE_CONVERSATION_EXISTS + ID"]
    EXISTS --> RESUME
    OLD -->|"CLOSING"| CLOSING
    OLD -->|"없음"| NEW["새 키 · 새 ID 201<br/>같은 날 ENDED/요약 중이어도 가능"]
    NEW --> GREET["FE nickname 고정 인사<br/>AI/TTS/발화 저장·개수·요약 제외"]
    RESUME --> CHECK["당일 ACTIVE만 연결<br/>종료 상태·시간·권한 재검사"]
    CHECK --> RESTORE["200 전체 허용 turns · sequence ASC<br/>최초 bound_at 유지"]
    RESTORE --> LEAVE["화면 이탈은 ACTIVE 유지"]
    LEAVE --> HOME
    GREET --> LEAVE
```

대화는 KST08\~24 동안 같은 날 여러 번 가능하며 ACTIVE/CLOSING은 아이당 하나다. ENDED 키 재전송은409, 새 대화는 새 키다. serviceDate/scheduledEndAt은 대화별 불변이고, 이전 날짜 미종료도 정리 전 새 시작을 막는다. ACTIVE는 새 유효 세션도 resume할 수 있으나 CLOSING은 새 연결·내용이 없다. 응답 직전 상태·시간·권한을 재검사한다.

연결: API §7.1\~7.3, §8 · FE·BE 인계서(별도 전달).

<a id="section-13-8"></a>
### 13.8. 음성 발화 접수와 동기·비동기 결과

<!-- diagram: api-integrated-voice-async -->
```mermaid
sequenceDiagram
    participant FE as 아이 화면
    participant P2 as Part 2
    participant DB as PostgreSQL
    participant AI as AI 서비스
    FE->>P2: POST turns, multipart audio + clientRequestId
    P2->>P2: CSRF·유효 로그인·소유권·전송 제한 검사
    P2->>DB: 대화 잠금 후 최신 시각·상태·당일·연결·기존 키 검사
    Note over P2,DB: 23:59 업로드라도 잠금 후 판정이 00:00이면 접수 거부
    Note over P2,DB: 아래는 새 키이며 음성 검증과 PROCESSING 충돌 검사를 통과한 경우
    P2->>DB: sequence·created_at·최초 deadline·PROCESSING을 저장하고 커밋
    P2->>AI: 접수 커밋 실행자만 1회 호출 시도, DB 잠금 없음
    alt 최초 HTTP 대기 구간 안에 처리 완료
        AI-->>P2: 결과 또는 실패와 필드별 허용 정보
        P2->>DB: 잠금 후 PROCESSING·기한 검사, 허용 텍스트와 terminal 상태 저장
        P2->>P2: CLOSING/자정이면 공통 종료 조정
        P2->>P2: 응답 직전 시간·세션·상태·연결 재검사
        alt 현재 전달 권한 유효
            P2-->>FE: 완전 성공 201 또는 STT 422 / AI·TTS 502 / timeout 504
        else CLOSING/자정 경과 또는 권한 상실
            P2-->>FE: 해당 401 / 403 / 409, 대화 내용과 음성 비노출
        end
    else 접수 뒤 HTTP 대기 구간 경과
        P2->>P2: 응답 직전 시간과 접근 권한 재검사
        P2-->>FE: 전달 조건이 유효하면 202 TurnAccepted와 statusUrl
        AI-->>P2: 이후 결과 또는 실패
        P2->>DB: 기존 deadline 안의 PROCESSING만 결과 반영, timeout은 FAILED
        P2->>P2: CLOSING/자정이면 공통 종료 조정
        FE->>P2: GET turn 또는 GET turn-request
        P2->>P2: 시간·로그인·소유권·대화·연결 재검사
        P2-->>FE: 접근 가능하면 200 PROCESSING / SUCCEEDED / FAILED, 아니면 접근 오류
    end
```

| 결과 | 저장 상태 | 아직 응답하지 않은 최초 POST | 이미 202를 보낸 뒤 |
| --- | --- | --- | --- |
| 정상 완료 | SUCCEEDED, errorCode=null | 201 ChildTurnResult | 접근 가능한 상태 GET은 200 SUCCEEDED |
| STT 무음·인식할 말 없음 | FAILED, STT_NO_SPEECH | 422 ApiError | 접근 가능한 상태 GET은 200 FAILED |
| AI 또는 TTS 실패 | FAILED, AI_UPSTREAM_FAILED | 502 ApiError | 접근 가능한 상태 GET은 200 FAILED |
| 전체 처리 기한 초과 | FAILED, AI_TIMEOUT | 504 ApiError | 접근 가능한 상태 GET은 200 FAILED |

TTS 실패에서도 저장·표시가 모두 허용된 childText/replyText는 보존하며 audio=null, topicSuggestions=[]이다. 최초 오류 응답에는 ApiError만 넣고 보존 텍스트는 허용 시간의 상태 GET으로 읽는다. 파일 손상·미지원 415와 용량 초과 413을 STT 무음으로 바꾸지 않는다.

이미 보낸 202를 나중에 422/502/504로 바꾸지 않는다. 종료 경계 전 접수 작업은 최초 deadline까지 저장할 수 있지만 CLOSING/자정 뒤 아이에게 내용을 전달하지 않는다. 성공은 `now < deadline`인 PROCESSING에만 반영하고 늦은 결과가 terminal 상태를 덮어쓰지 못한다. 접수 커밋 뒤 호출 전 중단되면 기한 후 FAILED로 정리하며 자동 재호출하지 않는다. MIME·대기시간·deadline 수치는 D-13/D-14 연동 대기다.

연결: API §7.4\~7.6, §8.1 · FE·BE 인계서(별도 전달).

<a id="section-13-9"></a>
### 13.9. 발화 중복 키와 응답 유실 복구

<!-- diagram: api-integrated-idempotency-recovery -->
```mermaid
flowchart TD
    POST["발화 POST<br/>도식02의 시간·권한 검사 통과"] --> KEY{"같은 clientRequestId의 turn 있음?"}
    KEY -->|"예"| HASH{"실제 audio 바이트의<br/>SHA-256 해시가 같음?"}
    HASH -->|"아니오"| CONFLICT["409 IDEMPOTENCY_CONFLICT"]
    HASH -->|"예"| STATUS{"기존 turn 상태"}
    STATUS -->|"PROCESSING"| ACCEPTED["202 TurnAccepted<br/>기존 turnId와 statusUrl"]
    STATUS -->|"SUCCEEDED / FAILED"| RESULT["200 저장된 ChildTurnResult<br/>AI 재호출 없음"]
    KEY -->|"아니오"| VALID["새 음성 유효성 검사<br/>크기·형식 오류 413 / 415"]
    VALID --> BUSY{"다른 PROCESSING 있음?"}
    BUSY -->|"예"| WAIT["409 TURN_IN_PROGRESS"]
    BUSY -->|"아니오"| NEW["새 발화 접수<br/>도식08"]
    LOST["응답 유실"] --> ID{"turnId를 받았음?"}
    ID -->|"예"| GET["GET turns/turnId"]
    ID -->|"아니오"| REQUEST["GET turn-requests/clientRequestId"]
    GET --> GUARD["도식02의 시간·권한 검사<br/>응답 직전에도 재검사"]
    REQUEST --> GUARD
    GUARD --> FIND{"저장된 turn 있음?"}
    FIND -->|"예"| VIEW["200 PROCESSING / SUCCEEDED / FAILED<br/>topicSuggestions는 항상 빈 배열"]
    FIND -->|"아니오"| MISSING["404 RESOURCE_NOT_FOUND<br/>진행 중 POST와의 경쟁 고려"]
```

기존 키/해시를 새 PROCESSING 충돌보다 먼저 검사한다. 해시는 실제 파일 바이트의 SHA-256을 소문자 64자리 hex로 저장하며 multipart boundary, 파일명, clientRequestId를 제외한다. Content-Type만 믿지 않고 실제 파일을 디코딩해 검사한다. 전송량 제한은 중복 요청에도 적용한다.

동일 업로드 재시도에는 같은 키와 같은 파일을 보존하고 다시 녹음한 발화는 새 키를 쓴다. requestId는 서버 추적 ID이며 복구 키는 clientRequestId다. GET 404만으로 진행 중인 POST가 미접수라고 단정하지 않는다. 기존 결과도 CLOSING 또는 자정 이후 시간·상태 검사를 우회해 읽을 수 없다.

연결: API §7.4\~7.6, §8, §9 · FE·BE 인계서(별도 전달).

<a id="section-13-10"></a>
### 13.10. 수동·자정 종료와 기존 세션의 종료 확인

<!-- diagram: api-integrated-end-recovery -->
```mermaid
flowchart TD
    END["POST end · 0바이트 + CSRF<br/>로그인·소유권·기존 연결 · PIN 불필요"] --> AUTH["현재 유효 문맥·기존 연결 확인<br/>무효401 · 미연결403"]
    TIMER["자정 · catch-up · sweep<br/>turn 완료 · Home/start 정리"] --> LOCK["공통 conversation 잠금/CAS"]
    AUTH --> LOCK
    LOCK --> STATE{"현재 상태?"}
    STATE -->|"ACTIVE"| CUT["최초 경계/사유 고정<br/>자정 전 MANUAL=DB now<br/>자정 이후 MIDNIGHT=scheduledEndAt"]
    CUT --> CLOSE["CLOSING · 새 입력/내용/음성/resume 차단"]
    STATE -->|"CLOSING"| CLOSE
    CLOSE --> PROCESS{"기존 PROCESSING 남음?"}
    PROCESS -->|"기한 안"| DRAIN["원래 deadline까지 drain<br/>경계 전 기존 유효 연결의 end만202<br/>Retry-After ≥ 1"]
    DRAIN --> RETRY["동일 end 재확인 · 경계/기한 연장 없음"]
    RETRY --> AUTH
    PROCESS -->|"만료"| FAIL["조건부 AI_TIMEOUT/FAILED<br/>terminal 덮어쓰기 금지"]
    FAIL --> TX["최초 ENDED Tx · 실제 endedAt<br/>경계 전 적격 연결에 +600초 고정"]
    PROCESS -->|"없음"| TX
    TX --> INPUT{"허용 요약 입력 있음?"}
    INPUT -->|"없음 · 빈 대화 포함"| EMPTY["EMPTY 기록 · 요약 호출0회"]
    INPUT -->|"있음"| SUMMARY["PENDING · 최초 실행자만 요약1회<br/>원래 conversationId에만 결과"]
    TX --> NEW["새 대화 시작 가능<br/>요약 완료 대기 없음"]
    STATE -->|"ENDED"| TTL["bound_at이 최초 end_requested_at 미만<br/>현재 로그인·저장권한 유효<br/>now가 endedAt+600초 미만?"]
    TX --> TTL
    TTL -->|"예"| RECEIPT["200 EndReceipt 네 필드<br/>FE 홈 이동"]
    TTL -->|"아니오"| DENY["403 · 새 로그인은 Home 재조회"]
```

수동/자정 종료 최초 경계에서 CLOSING으로 입력·내용·음성을 차단한다. endedAt은 기존 발화 정리 후 실제 시각이며 경계보다 늦을 수 있다. 최초 endRequestedAt/endReason은 재요청과 자정 도달로 바꾸지 않는다. 처리 중 발화가 없으면 바로 ENDED로 확정할 수 있다.

복구 대상은 `bound_at<end_requested_at`의 기존 유효 연결이며 CLOSING202와 ENDED200 확인정보만 제공한다. 로그인 무효401, 미적격/새 로그인/기한 equality403. 반복 요청은 경계·복구기한 연장 또는 요약 재호출을 하지 않는다. 새 로그인은 Home 재조회로 현재 진입 가능 상태를 확인한다. 시간 밖에는 종료 완료를 추정하지 않고 시간외 안내 후 opensAt부터 재확인한다. 요약 crash 후 PENDING 재실행은 없고 기한 후 FAILED로 정리한다.

연결: API §7.7, §8, §8.1 · FE·BE 인계서(별도 전달).

<a id="section-13-11"></a>
### 13.11. 임시 응답 음성의 시간·권한·수명 검사

<!-- diagram: api-integrated-temporary-audio -->
```mermaid
flowchart TD
    GET["GET turns/turnId/audio"] --> AUTH["유효 로그인 · 아이 소유권 · 리소스 조합<br/>무효 401 · 확인 불가 503 · 타인/부재 404"]
    AUTH --> ENDED{"이미 ENDED?"}
    ENDED -->|"예"| E409["409 CONVERSATION_ENDED"]
    ENDED -->|"아니오"| HOURS{"KST 08~24 이용 시간?"}
    HOURS -->|"아니오"| H403["403 SERVICE_HOURS_CLOSED"]
    HOURS -->|"예"| DAY{"당일이며 now가<br/>scheduledEndAt 미만?"}
    DAY -->|"아니오"| C409["409 CONVERSATION_CLOSING"]
    DAY -->|"예"| ACTIVE{"status=ACTIVE?"}
    ACTIVE -->|"아니오: CLOSING"| C409
    ACTIVE -->|"예"| BOUND{"현재 세션 연결됨?"}
    BOUND -->|"아니오"| L403["403 CHILD_SESSION_REQUIRED"]
    BOUND -->|"예"| ASSET{"별도 허용된 음성 자산 존재?"}
    ASSET -->|"금지 / 미생성"| NOTFOUND["404 RESOURCE_NOT_FOUND"]
    ASSET -->|"예"| EXPIRED{"now가 expiresAt 이상?"}
    EXPIRED -->|"예"| EX410["410 AUDIO_EXPIRED"]
    EXPIRED -->|"아니오"| READY{"AVAILABLE이며 접근 가능?"}
    READY -->|"아니오"| NOTFOUND
    READY -->|"예"| RECHECK["파일 전달 직전<br/>시간·로그인·연결·수명 재검사"]
    RECHECK --> OK["200 실제 Content-Type의 바이너리<br/>Cache-Control: no-store · X-Request-ID"]
```

텍스트 허용만으로 음성 허용을 추정하지 않는다. 파일 삭제가 늦어도 자정·대화 종료·세션 무효·음성 만료부터 접근을 거부한다. 이미 전달한 바이트를 회수할 수는 없으므로 FE도 수동 종료 접수 및 자정에 재생을 중단하고 Blob과 화면의 대화 데이터를 정리한다. 화면 정리가 서버 기록 삭제를 뜻하지는 않는다.

만료 음성을 위해 AI/TTS를 재호출하지 않는다. 보호자 기록과 EndReceipt에는 음성 URL이 없다. 구체 MIME·codec·최대 크기·TEMP_AUDIO_TTL·410 판정용 메타데이터 수명은 D-13/D-14 및 운영 연동 사항이며 Range/206 지원을 추가로 약속하지 않는다.

연결: API §7.8, §8, §5.4 · FE·BE 인계서(별도 전달).

<a id="section-13-12"></a>
### 13.12. 텍스트·요약·음성의 개별 공개 허용

<!-- diagram: api-integrated-visibility -->
```mermaid
flowchart TD
    AI["AI 결과 + 필드별 허용 정보"] --> CHILD["childText 독립 판정"]
    AI --> REPLY["replyText 독립 판정"]
    CHILD --> C{"저장과 표시 모두 허용?"}
    REPLY --> R{"저장과 표시 모두 허용?"}
    C -->|"아니오 / 허용 정보 없음"| CN["childText=null<br/>childTextVisibility=OMITTED"]
    R -->|"아니오 / 허용 정보 없음"| RN["replyText=null<br/>replyTextVisibility=OMITTED"]
    C -->|"예"| CY["허용 원문은 VISIBLE<br/>AI가 허용한 가공 문자열만 REDACTED"]
    R -->|"예"| RY["허용 원문은 VISIBLE<br/>AI가 허용한 가공 문자열만 REDACTED"]
    CY --> TEXT["허용된 nonnull 텍스트만<br/>저장·검색·요약 입력·AI 이전 문맥"]
    RY --> TEXT
    TEXT --> DELIVERY["아이에게 표시할 때<br/>이용 시간·현재 세션·대화 상태 재검사"]
    AI --> SUMMARY["title / topic / summary 각각 허용 검사<br/>모두 허용된 문자열일 때만 READY"]
    AI --> AUDIO["음성은 별도 허용·접근·수명 검사<br/>금지·미생성·만료 또는 실패면 audio=null"]
    AI --> TOPICS["P0 주제 추천 보류<br/>topicSuggestions는 항상 빈 배열 · 저장 컬럼 없음"]
    REPLY --> NATURAL["AI가 답변 문장으로<br/>자연스럽게 대화를 이어감"]
```

한쪽이 OMITTED여도 반대쪽 허용 텍스트는 유지한다. PROCESSING은 두 텍스트·completedAt·errorCode가 null이고 두 visibility는 OMITTED다. TTS 실패의 FAILED에도 허용된 텍스트가 남으며 보호자 검색·요약 입력에서 실패 상태만으로 제외하지 않는다. 금지 원문과 허용 정보가 없는 텍스트를 DB·로그·검색·요약·이전 AI 문맥에 남기지 않는다.

주제 추천 보류는 ConversationView의 요약용 topic을 없애는 결정이 아니다. title/topic/summary는 허용 입력으로 생성하고 출력도 각각 검사한다. audio와 topicSuggestions는 발화 POST/GET의 wrapper 필드이며 공유 TurnView에 넣지 않는다. resume은 TurnView 전체만 반환하고 보호자 기록에는 audio·추천을 추가하지 않는다. AI wire와 문자열 한도는 D-13/D-14 연동 대기다.

연결: API §5.2\~5.5, §7.4\~7.8, §11 · FE·BE 인계서(별도 전달).

<a id="section-13-13"></a>
### 13.13. 보호자 목록 검색과 전체 발화 이어 읽기

<!-- diagram: api-integrated-record-pagination -->
```mermaid
sequenceDiagram
    participant FE as 보호자 대시보드
    participant P1 as Part 1 기록 API
    participant DB as Part 2 소유 읽기 모델
    FE->>P1: GET conversations, from / to / q / page / size
    P1->>P1: 로그인·입력·소유권·현재 guardian 권한 검사
    P1->>DB: ENDED, startedAt의 KST 날짜 범위, 허용 텍스트 리터럴 검색
    Note over P1,DB: startedAt DESC, id DESC, size+1 및 발화 EXISTS로 대화 중복 제거
    DB-->>P1: 목록과 현재 요약 상태
    P1-->>FE: 200 ConversationPage, 빈 목록도 허용
    FE->>P1: GET conversation 상세, afterSequence=0, size=100
    P1->>P1: 소유권·PIN·ENDED 검사, 타인 404 / PIN 없음 403 / ACTIVE 404
    P1->>DB: cursor 초과 turn을 sequence ASC, size+1로 조회
    DB-->>P1: 허용된 turn과 conversation
    P1-->>FE: conversation + turns + hasNext + nextAfterSequence
    loop hasNext가 true인 동안
        FE->>P1: nextAfterSequence로 다음 페이지 요청
        P1->>P1: 매 페이지 로그인·소유권·PIN 재검사
        P1->>DB: cursor 초과 turn을 sequence ASC로 조회
        DB-->>P1: 다음 페이지와 현재 요약 상태
        P1-->>FE: 200 페이지, 끝이면 hasNext=false / nextAfterSequence=null
    end
```

보호자 기록은 ENDED만 대상으로 하며 아이 대화의 00\~08 제한은 적용하지 않는다. 수동/자정 경계 후 기존 발화가 처리 중이면 CLOSING이므로 목록에서 제외한다. 빈 대화도 EMPTY 기록으로 남긴다. 종료가 끝났다면 요약 PENDING/FAILED/EMPTY도 기록 조회를 막지 않는다. 날짜는 startedAt의 KST 날짜, 즉 serviceDate를 기준으로 하므로 새벽에 종료된 대화도 전날 기록에 속한다.

목록은 기본 page=0, size=20, 최대 100개다. 상세는 기본·최대 100턴을 cursor로 이어 읽으며 sequence의 빈 번호를 건너뛴다. q는 trim 후 최대 100자이고 `%`, `_`, escape 문자도 리터럴로 검색한다. 잘못된 날짜 범위나 page×size overflow는 400이다. 빈 상세 페이지는 hasNext=false, nextAfterSequence=null이다. 종료 발화는 불변이지만 요약은 페이지 사이에 완료될 수 있다. 비공개 내용의 일치 개수·스니펫·음성을 반환하지 않는다.

연결: API §6.15\~6.16, §5.4\~5.5, §8 · FE·BE 인계서(별도 전달).

<a id="section-13-14"></a>
### 13.14. 26개 API 동작과 도식 매핑

각 operation의 실제 처리 또는 응답 복구가 나타난 주 도식과 보조 도식이다. 기존 operationId 26개를 유지하며 경로는 OAuth를 제외하고 `/api/v1`부터 표기한다.

| 담당 | operationId | Method / 경로 | 주 도식 | 보조 도식 |
| --- | --- | --- | --- | --- |
| Part 1 | getCsrf | GET /api/v1/auth/csrf | 02 요청 검사 | 04 로그인 진입 |
| Part 1 | issueEmailVerification | POST /api/v1/auth/email-verifications | 03 이메일 검증 | 05 PIN |
| Part 1 | verifyEmailCode | POST /api/v1/auth/email-verifications/verify | 03 이메일 검증 | 05 PIN |
| Part 1 | signup | POST /api/v1/auth/signup | 03 이메일 검증 | 04 로그인 진입 |
| Part 1 | login | POST /api/v1/auth/login | 04 로그인 진입 | 06 비밀번호 재설정 |
| Part 1 | getCurrentAccount | GET /api/v1/auth/me | 04 로그인 진입 | 05 PIN |
| Part 1 | logout | POST /api/v1/auth/logout | 04 로그인 진입 | 08 응답 직전 권한 |
| Part 1 | resetPassword | POST /api/v1/auth/password-resets | 06 비밀번호 재설정 | 03 이메일 검증 |
| Part 1 | listChildren | GET /api/v1/children | 04 로그인 진입 | 01 담당 |
| Part 1 | createChild | POST /api/v1/children | 04 로그인 진입 | 01 담당 |
| Part 1 | setupGuardianPin | POST /api/v1/guardian/pin | 05 PIN | 03 이메일 검증 |
| Part 1 | unlockGuardian | POST /api/v1/guardian/unlock | 05 PIN | 13 보호자 기록 |
| Part 1 | lockGuardian | POST /api/v1/guardian/lock | 05 PIN | 04 로그인 진입 |
| Part 1 | resetGuardianPin | POST /api/v1/guardian/pin/reset | 05 이메일 PIN 재설정 | 03 이메일 검증 |
| Part 1 | listChildConversations | GET /api/v1/children/{childId}/conversations | 13 보호자 기록 | 12 공개 허용 |
| Part 1 | getChildConversation | GET /api/v1/children/{childId}/conversations/{conversationId} | 13 보호자 기록 | 12 공개 허용 |
| Part 1 | startGoogleLogin | GET /oauth2/authorization/google | 04 로그인 진입 | 02 요청 검사 |
| Part 1 | completeGoogleLogin | GET /login/oauth2/code/google | 04 로그인 진입 | 02 요청 검사 |
| Part 2 | getChildHome | GET /api/v1/children/{childId}/home | 07 대화 시작·종료·복원 | 04 로그인 진입 |
| Part 2 | startChildConversation | POST /api/v1/children/{childId}/conversations | 07 대화 시작·종료·복원 | 02 시간·권한 검사 |
| Part 2 | resumeChildConversation | POST /api/v1/children/{childId}/conversations/{conversationId}/resume | 07 대화 시작·종료·복원 | 12 공개 허용 |
| Part 2 | submitVoiceTurn | POST /api/v1/children/{childId}/conversations/{conversationId}/turns | 08 음성 비동기 | 09 중복·복구, 12 공개 허용 |
| Part 2 | getVoiceTurn | GET /api/v1/children/{childId}/conversations/{conversationId}/turns/{turnId} | 09 중복·복구 | 08 음성 비동기, 12 공개 허용 |
| Part 2 | getVoiceTurnByRequestId | GET /api/v1/children/{childId}/conversations/{conversationId}/turn-requests/{clientRequestId} | 09 중복·복구 | 08 음성 비동기, 12 공개 허용 |
| Part 2 | endChildConversation | POST /api/v1/children/{childId}/conversations/{conversationId}/end | 10 수동/자정 종료·복구 | 02 시간 외 예외 |
| Part 2 | getTemporaryTurnAudio | GET /api/v1/children/{childId}/conversations/{conversationId}/turns/{turnId}/audio | 11 임시 음성 | 02 시간·권한 검사, 12 공개 허용 |

반영 범위: 수동/자정 종료·CLOSING·같은 날 새 대화·미종료1개, FE 고정 첫 인사·이름/애칭 분리·빈 EMPTY 기록, 기존 세션들의 종료 후 600초 확인, 전체 resume, 보호자 100턴 cursor, 이메일 PIN 재설정, OAuth 실패 처리, STT/TTS 실패 매핑, 빈 주제 추천 배열을 최종 계약으로 적용했다. D-13/D-14의 AI 실제 연동·수치와 D-17 운영 조건은 남아 있으며 도식 작성이 서버 구현이나 통합 시험 완료를 의미하지 않는다.
