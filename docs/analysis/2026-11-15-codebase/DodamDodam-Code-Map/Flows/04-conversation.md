# 대화 시작과 재전송

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|시작]] → [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요]] → 이 흐름 → 메서드 정의

**진입:** `POST /api/v1/children/{childId}/conversations`

**결과:** 신규 201 + Location · 허용된 같은 키 재전송 200

> [!important] 먼저 알아둘 점
> clientRequestId 재전송은 현재 연결된 당일 ACTIVE 대화만 허용한다. 응답 재검사에서는 새 연결을 만들지 않는다.

![[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Assets/04-conversation.svg]]

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Flows/04-conversation.canvas|같은 흐름을 Canvas에서 열기]] · 아래 코드 링크로 실제 메서드 이동

번호와 화살표는 **주요 처리 순서**다. 조건에 따른 조기 반환·실패와 중간 helper 호출은 생략했다. 직접 Java 호출 관계는 연결된 메서드 노트에서 확인한다.

## 단계별 코드

### 1. 인증 · 입력 · 소유권

로그인 → UUID / JSON 검사 · 해당 아이의 소유권 확인

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.api.ConversationController#prepareStart m1cac5f319ccf317a|ConversationController.prepareStart]]

### 2. 상위 데이터 잠금

계정 → 문맥 → 아이 → 대화 · 대기 후 DB 시각 재확인

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationService#create mf9ff06c04e6d7fad|ConversationService.create]]
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationService#decideStart m074dea56e9276dfa|ConversationService.decideStart]]

### 3. 이전 대화 · 시간 판단

필요하면 기존 종료 상태 정리 · 이용시간과 시작 키를 판단

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationService#decideStart m074dea56e9276dfa|ConversationService.decideStart]]
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationClosureCoordinator#reconcile m5c7d3b9b2dd0cb88|ConversationClosureCoordinator.reconcile]]

### 4. 신규 또는 재전송

신규: 대화와 세션 연결 저장 · 같은 키: 기존 연결 재검사

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.infrastructure.ConversationRepository#insertActive mff8e74c752863f8c|ConversationRepository.insertActive]]
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationService#replay m8a9b848e75e75181|ConversationService.replay]]

### 5. 응답 대상 기록

검증한 대화 ID를 요청에 기록 · 신규 / 재전송 응답 구분

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.api.ConversationController#start m255287826e69c022|ConversationController.start]]
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.api.ConversationStartResponseFilter#protect m24d9b8b621ce0746|ConversationStartResponseFilter.protect]]

### 6. 현재 상태 다시 확인

응답 직전 권한·시간·상태 확인 · 실패하면 성공 응답을 비움

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.api.ConversationStartResponseFilter#verify mbaf83885c918b297|ConversationStartResponseFilter.verify]]
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationService#verifyStartResponse macdb3aca10ad50df|ConversationService.verifyStartResponse]]

## 실패하거나 달라지는 경우

| 조건 | 결과 / 주의 | 확인할 코드 |
| --- | --- | --- |
| 소유하지 않은 아이 | 404 · 미존재와 동일 | [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.application.ChildService#requireChildAccess mbb6af419b1ceab47\|ChildService.requireChildAccess]] |
| 미종료 대화 / 종료된 키 충돌 | 409 · 업무 상태별 분류 | [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationService#conflictFor m1c136cdb5700e953\|ConversationService.conflictFor]] |
| 응답 전 종료 / 권한 변경 | 기존 성공 본문·Location 제거 | [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.api.ConversationStartResponseFilter#clearSuccess me633fd53fe2379f2\|ConversationStartResponseFilter.clearSuccess]] |

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|관련 클래스 찾기]]
