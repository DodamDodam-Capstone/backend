# 대화 시작 · 상태 · conversation

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|시작]] → 기능 개요 → 요청 흐름 → 메서드 정의

아이의 대화를 시작하고 중복 시작과 종료 상태를 조정한다.

> [!info] 현재 범위
> 부분 구현 · 음성 / 종료 API 후속

## 먼저 볼 흐름

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Flows/04-conversation|대화 시작과 재전송]] — 신규 201 + Location · 허용된 같은 키 재전송 200
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Flows/07-closure|대화 종료 상태 조정]] — CLOSING 유지 또는 ENDED 전환 · 기존 종료 상태 반환

## 주요 진입점

| 코드 | 책임 |
| --- | --- |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.api.ConversationController#start m255287826e69c022\|ConversationController.start]] | 대화 시작 요청을 받아 인증·입력·소유권을 검사하고 201 또는 200을 반환한다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationService#create mf9ff06c04e6d7fad\|ConversationService.create]] | 잠금 아래 신규 시작·같은 키 재전송·충돌을 판단하고 응답 대상도 재검사한다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationClosureCoordinator#reconcile m5c7d3b9b2dd0cb88\|ConversationClosureCoordinator.reconcile]] | 진행 중 발화 정리를 기다리며 대화를 ACTIVE → CLOSING → ENDED로 조정한다. |

## 수정할 때 찾을 위치

| 계층 | 담당 책임 | 클래스 |
| --- | --- | --- |
| `api` | HTTP 입력·응답·기능 오류 | [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.api.ConversationController\|ConversationController]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.api.ConversationStartResponseFilter\|ConversationStartResponseFilter]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.api.ConversationExceptionHandler\|ConversationExceptionHandler]] · 외 1개는 아래 전체 목록 |
| `application` | 업무 규칙·실행 순서·트랜잭션 | [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationClosureCoordinator\|ConversationClosureCoordinator]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationService\|ConversationService]] |
| `domain` | 보조 타입 | [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.domain.ConversationServiceHours\|ConversationServiceHours]] |
| `dto` | 요청 검증·전달 데이터 | [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.dto.ActiveConversationExistsError\|ActiveConversationExistsError]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.dto.StartConversationRequest\|StartConversationRequest]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.dto.StartConversationResult\|StartConversationResult]] |
| `exception` | 공개 업무 오류 | [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.exception.ConversationException\|ConversationException]] |
| `infrastructure` | DB·잠금·외부 시스템 | [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.infrastructure.ConversationClosureRepository\|ConversationClosureRepository]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.infrastructure.ConversationRepository\|ConversationRepository]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.infrastructure.ConversationClosureProperties\|ConversationClosureProperties]] |

> [!example]- 전체 타입 26개 — 중첩 타입·데이터 포함
>
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.api.ConversationController|conversation.api.ConversationController]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.api.ConversationController.PreparedStart|conversation.api.ConversationController.PreparedStart]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.api.ConversationExceptionHandler|conversation.api.ConversationExceptionHandler]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.api.ConversationMediaTypeExceptionHandler|conversation.api.ConversationMediaTypeExceptionHandler]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.api.ConversationStartResponseFilter|conversation.api.ConversationStartResponseFilter]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.api.ConversationStartResponseFilter_anonymous_86|conversation.api.ConversationStartResponseFilter$anonymous@86]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.api.ConversationStartResponseFilter.ResponseAccess|conversation.api.ConversationStartResponseFilter.ResponseAccess]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.api.ConversationStartResponseFilter.ResponseRejected|conversation.api.ConversationStartResponseFilter.ResponseRejected]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationClosureCoordinator|conversation.application.ConversationClosureCoordinator]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationClosureCoordinator.Trigger|conversation.application.ConversationClosureCoordinator.Trigger]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationClosureCoordinator.ClosureResult|conversation.application.ConversationClosureCoordinator.ClosureResult]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationService|conversation.application.ConversationService]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationService.StartOutcome|conversation.application.ConversationService.StartOutcome]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.application.ConversationService.StartDecision|conversation.application.ConversationService.StartDecision]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.domain.ConversationServiceHours|conversation.domain.ConversationServiceHours]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.domain.ConversationServiceHours.ServiceWindow|conversation.domain.ConversationServiceHours.ServiceWindow]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.dto.ActiveConversationExistsError|conversation.dto.ActiveConversationExistsError]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.dto.ActiveConversationExistsError.ErrorDetail|conversation.dto.ActiveConversationExistsError.ErrorDetail]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.dto.StartConversationRequest|conversation.dto.StartConversationRequest]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.dto.StartConversationResult|conversation.dto.StartConversationResult]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.exception.ConversationException|conversation.exception.ConversationException]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.infrastructure.ConversationClosureProperties|conversation.infrastructure.ConversationClosureProperties]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.infrastructure.ConversationClosureRepository|conversation.infrastructure.ConversationClosureRepository]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.infrastructure.ConversationClosureRepository.ClosureState|conversation.infrastructure.ConversationClosureRepository.ClosureState]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.infrastructure.ConversationRepository|conversation.infrastructure.ConversationRepository]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.infrastructure.ConversationRepository.StoredConversation|conversation.infrastructure.ConversationRepository.StoredConversation]]

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Canvases/conversation.canvas|전체 호출선 보기 — 세부 조사용]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/90-Reference|전체 자료와 분석 한계]]
