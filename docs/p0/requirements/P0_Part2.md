# DodamDodam P0 Part 2 — Home·음성 대화·종료·요약

계약 v3.1 · 문서 정리 2026-10-06 · 구현·실제 통합 시험은 별도

[공통 요구사항](P0_Common_Spec.md)과 [확정 정책](../Decision_Record.md)을 적용합니다. 이 문서는 작업 책임과 인수 기준을 관리하고 상세 계약은 아래 링크에서 한 번만 관리합니다.

## 담당 범위

Part 2는 아이 Home, 음성 대화의 수명, AI 연동, 허용 결과 저장, 임시 음성, 수동/자정 종료와 요약을 맡는다. STT·LLM·TTS 엔진·안전 판정은 AI 팀, 기록 목록/검색은 Part 1이다. backend 내부는 단일 애플리케이션·DB이며 공통 인증/아이 소유권 함수를 재사용한다. AI가 업무 DB를 직접 읽거나 쓰지 않는다.

| 작업 | 구현 책임 | 공유 산출물 |
| --- | --- | --- |
| B-01 아이 Home | 최소 프로필·nickname 인사·joinedAt·daysTogether·메뉴·서버 이용시간·접근 가능한 ACTIVE ID | ChildHome, availability |
| B-02 시작·이어하기·종료 | 아이당 미종료 1개·당일 ACTIVE 복원·수동/자정 CLOSING·ENDED·기존 연결 receipt | 상태 전이·시작 중복키·세션 연결·경계/복구 |
| B-03 음성·AI | multipart 음성만 접수, STT/생성/TTS 연결·필드별 허용·인증된 임시 음성 GET | 실제 AI 매핑·발화 결과·실패 코드 |
| B-04 저장·요약 | 발화 순서·성공/실패·허용 텍스트, 최초 ENDED 후 대화별 요약 최대 1회 | ConversationView/TurnView·NULL·검색 가능한 읽기 모델 |
| B-05 실패·복구 | 파일 바이트 해시·응답 유실 조회·원래 deadline·재시작 정리·late callback | 중복·경합·202/실패·기한 경계 사례 |

텍스트/버튼 선택 발화·추천 UI·추천 생성/저장·사진·실제 놀이·보상·꾸미기·감정 분석·알림·보호자 음성 다시 듣기는 제외한다. 기존 안전 결과를 우회하지 않는다. 초기 인사는 FE 고정 템플릿이며 BE의 새 발화·AI 호출·TTS·요약 입력·발화 수로 만들지 않는다.

## 상세 계약 찾아가기

| 업무 | 기준 위치 |
| --- | --- |
| B-01 Home·아이 문맥·고정 첫 인사 | [Home API](../api/06-conversations.md#section-7-2) |
| B-02 시작·resume·수동/자정 종료·receipt | [대화 API와 상태 규칙](../api/06-conversations.md) |
| B-03·B-05 음성·중복·응답 유실·기한·재시작 | [음성 API](../api/07-voice.md) |
| B-04 허용 결과·요약·읽기 모델 | [공유 응답](../api/01-common.md#section-5), [종료 규칙](../api/06-conversations.md#section-8-3), [ERD](../Integrated_ERD_Design.md) |
| Part 1·FE·AI 연계와 실제 AI 자료 | [공통 함수](../api/01-common.md#section-3-1), [AI·인수 기준](../api/08-errors-and-validation.md#section-11) |
| 입력·성공/실패·JSON 스키마 | [API 목차](../Integrated_API_Spec.md), [OpenAPI](../Integrated_OpenAPI.yaml) |
| DB·제약·데이터 이관 | [통합 ERD](../Integrated_ERD_Design.md) |
| 공통 보안·오류·제한 수치 | [공통 API](../api/01-common.md), [오류·검증](../api/08-errors-and-validation.md) |

## 인수 기준

- [ ] 07:59:59/08:00:00/23:59:59/00:00:00 경계, 탭 복귀·재로그인·당일 ACTIVE 전체 resume을 확인한다.
- [ ] 수동 종료와 새 발화 경합의 두 순서, end 202→200, 원래 deadline 유지, CLOSING 새 입력/연결/내용 차단을 확인한다.
- [ ] ENDED 후 같은 날 새 ID·새 키, 이전 요약 PENDING과 새 대화 분리, CLOSING 중 새 시작 거부를 확인한다.
- [ ] 빈 대화 EMPTY 기록·요약 미호출, 첫 인사 AI/TTS/저장/요약/발화 수 제외·nickname 사용을 확인한다.
- [ ] 두 탭·여러 서버 시작/종료 경합·처리 실패·재시작·late callback에 중복 행/AI 실행/요약 시도가 없는지 확인한다.
- [ ] 복수 기존 유효 연결만 CLOSING 확인 및 endedAt+600초 미만 receipt, 새 로그인·정확한 만료 equality·경계 재설정 거부를 확인한다.
- [ ] 동일 파일/동일 키·다른 파일·202 이후 FAILED·STT 무음·TTS 전체 실패·별도 음성 허용과 404/410을 확인한다.
- [ ] 응답 직전 수동 종료·자정·로그아웃·password reset으로 무효화돼도 허용 저장과 아이 결과 전달을 분리한다.
- [ ] 실제 FE 녹음→BE 디코딩→AI→브라우저 재생, D13·D14 형식/시간/용량/허용 계약과 D17 운영·보관을 별도 시험한다.

이 요구사항 정리는 제품 코드·DB migration 실행·실제 통합 시험 완료를 뜻하지 않는다. 남은 자료를 임의 0/무제한/가상 endpoint로 채우지 않는다.

## 담당 흐름

### Home·대화 시작

<!-- diagram: req-part2-home-start -->
```mermaid
flowchart TD
    Home[Home 서버 시간과 availability] --> Time{서비스 시간}
    Time -->|아니오| Closed[00시부터 08시까지 진입 차단]
    Time -->|예| Closing{미종료 CLOSING 존재}
    Closing -->|예| Wait[종료 마무리 안내 Home 재조회]
    Closing -->|아니오| Active{접근 가능한 ACTIVE}
    Active -->|있음| Resume[현재 세션 resume 전체 허용 발화]
    Active -->|없음| Start[새 clientRequestId로 시작]
    Start --> New[201 새 ACTIVE 및 연결]
    New --> Greet[FE 고정 첫 인사 AI 호출 없음]
    Greet --> Mic[음성 입력]
    Resume --> Busy{PROCESSING 존재}
    Busy -->|있음| Poll[기존 발화 상태 조회]
    Busy -->|없음| Mic
    Poll --> Mic
```

### 음성 처리·응답 복구

<!-- diagram: req-part2-voice-flow -->
```mermaid
flowchart TD
    Audio[음성과 clientRequestId] --> Access[로그인 CSRF 소유권 시간 상태 연결 검사]
    Access -->|거부| Error[공통 권한 또는 종료 오류]
    Access -->|허용| Key{기존 키}
    Key -->|있음| Hash{같은 파일 바이트 hash}
    Hash -->|아니오| Conflict[409 IDEMPOTENCY_CONFLICT]
    Hash -->|예| Previous[기존 PROCESSING 202 또는 완료 200]
    Key -->|없음| Busy{다른 PROCESSING}
    Busy -->|있음| Wait[409 TURN_IN_PROGRESS]
    Busy -->|없음| Save[음성 검증 및 PROCESSING 접수 커밋]
    Save --> AI[잠금 밖 AI 1회 시도]
    AI --> Filter[필드별 저장 AND 표시 허용 검사]
    Filter --> Commit[PROCESSING 및 원래 deadline 조건부 저장]
    Commit --> End{종료 경계 도달}
    End -->|예| Drain[CLOSING 마무리 및 ENDED 조정]
    End -->|아니오| Recheck[전달 직전 시간 권한 ACTIVE 재검사]
    Recheck --> Reply[허용 결과 또는 접근 오류]
```

### 허용 텍스트·AI·기록 연결

<!-- diagram: req-part2-ai-record-contract -->
```mermaid
sequenceDiagram
    participant UI as 아이 FE
    participant P2 as Part 2
    participant AI as AI 서비스
    participant DB as 공통 DB
    participant P1 as Part 1
    UI->>UI: nickname 고정 첫 인사 표시
    UI->>P2: 음성과 clientRequestId
    P2->>P1: 공통 세션 및 소유권 검사
    P2->>DB: PROCESSING과 원래 deadline 접수
    P2->>AI: 음성 및 허용 문맥 nickname 호칭
    AI-->>P2: 결과 및 개별 허용 정보
    P2->>DB: 기한 내 허용 결과 조건부 저장
    P2->>P2: 응답 직전 상태 시간 권한 검사
    P2-->>UI: 접근 가능할 때 허용 결과
    UI->>P2: 이야기 마치기 end
    P2->>DB: 최초 종료 경계 CLOSING
    P2-->>UI: 진행 중 202 또는 완료 200
    P2->>DB: 기존 작업 terminal 후 ENDED와 PENDING 또는 EMPTY
    Note over UI,P2: 동일 end 완료 확인 후 홈 새 대화 가능
    P2->>AI: 허용 입력이 있으면 원래 대화 요약 1회
    P1->>DB: PIN 확인 후 ENDED 기록 읽기
    DB-->>P1: 허용 텍스트와 요약 상태
```

### 종료·요약

<!-- diagram: req-part2-end-summary -->
```mermaid
flowchart TD
    Trigger[연결된 end 또는 자정 조정] --> Boundary[동일 잠금으로 최초 종료 경계 고정]
    Boundary --> Closing[CLOSING 새 입력과 아이 내용 차단]
    Closing --> Busy{기접수 PROCESSING}
    Busy -->|있음| Drain[원래 deadline까지 처리 202 receipt]
    Drain --> Busy
    Busy -->|없음| Text{허용 요약 입력}
    Text -->|없음| Empty[ENDED 및 EMPTY 기록 보존]
    Text -->|있음| Pending[ENDED 및 PENDING]
    Empty --> Receipt[기존 적격 연결 200 EndReceipt]
    Pending --> Receipt
    Receipt --> Home[FE 홈 같은 날 새 ID 가능]
    Pending --> Summary[최초 전이 실행자 요약 1회 시도]
    Summary -->|허용 출력과 기한 내| Ready[조건부 READY]
    Summary -->|실패 또는 deadline| Failed[FAILED 기존 기록 유지]
    Empty --> Record[Part 1 ENDED 기록]
    Pending --> Record
    Ready --> Record
    Failed --> Record
```
