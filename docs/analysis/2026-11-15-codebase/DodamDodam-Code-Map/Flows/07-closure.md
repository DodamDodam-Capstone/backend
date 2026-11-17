# 대화 종료 상태 조정

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|시작]] → [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|기능 개요]] → 이 흐름 → 메서드 정의

**진입:** `ConversationClosureCoordinator.reconcile 내부 호출`

**결과:** CLOSING 유지 또는 ENDED 전환 · 기존 종료 상태 반환

> [!important] 먼저 알아둘 점
> 타이머·수동 종료 HTTP API·AI 요약 worker 자체는 아니다. PENDING 기록이 요약 실행을 의미하지 않는다.

![[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Assets/07-closure.svg]]

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Flows/07-closure.canvas|같은 흐름을 Canvas에서 열기]] · 아래 코드 링크로 실제 메서드 이동

번호와 화살표는 **주요 처리 순서**다. 조건에 따른 조기 반환·실패와 중간 helper 호출은 생략했다. 직접 Java 호출 관계는 연결된 메서드 노트에서 확인한다.

## 단계별 코드

### 1. 상위 잠금 · 상태 조회

인증·연결 검사는 호출자 책임 · 아이·대화 관계와 상태 조회

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationClosureCoordinator#reconcile m5c7d3b9b2dd0cb88|ConversationClosureCoordinator.reconcile]]

### 2. 종료 경계 확정

이미 ENDED면 기존 상태 반환 · 최초 종료 경계만 기록

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.infrastructure.ConversationClosureRepository#beginClosing m8c1904cc945fbe62|ConversationClosureRepository.beginClosing]]

### 3. 처리 중 발화 정리

PROCESSING 발화를 잠금 · 만료한 처리 상태를 정리

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.infrastructure.ConversationClosureRepository#lockProcessingTurns macac7a26cd3dcb0a|ConversationClosureRepository.lockProcessingTurns]]
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.infrastructure.ConversationClosureRepository#expireProcessing m013d6a27bd537b45|ConversationClosureRepository.expireProcessing]]

### 4. 대기 또는 종료 진행

PROCESSING이 남으면 CLOSING · 없으면 기존 연결을 잠금

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationClosureCoordinator#reconcile m5c7d3b9b2dd0cb88|ConversationClosureCoordinator.reconcile]]
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.infrastructure.ConversationClosureRepository#lockLinks m1f2723814c4ce03e|ConversationClosureRepository.lockLinks]]

### 5. ENDED · 요약 상태 저장

요약 입력 유무를 확인 · EMPTY 또는 PENDING 기록

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.infrastructure.ConversationClosureRepository#finishClosing mabcd7eabe63e07b8|ConversationClosureRepository.finishClosing]]

### 6. 기존 연결 복구 기한

적격 연결에 600초 기한 · 새 연결을 만들지는 않음

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.infrastructure.ConversationClosureRepository#grantRecovery m7ac5ec5179a9bacb|ConversationClosureRepository.grantRecovery]]

## 실패하거나 달라지는 경우

| 조건 | 결과 / 주의 | 확인할 코드 |
| --- | --- | --- |
| 대화가 없거나 아이 불일치 | 404 · 같은 오류로 처리 | [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationClosureCoordinator#requireState mbbd5c374803feeec\|ConversationClosureCoordinator.requireState]] |
| PROCESSING이 아직 남아 있음 | 실패가 아니라 CLOSING 대기 | [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationClosureCoordinator#reconcile m5c7d3b9b2dd0cb88\|ConversationClosureCoordinator.reconcile]] |
| 이미 ENDED인 재호출 | 기존 상태 반환 | [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationClosureCoordinator#reconcile m5c7d3b9b2dd0cb88\|ConversationClosureCoordinator.reconcile]] |

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|관련 클래스 찾기]]
