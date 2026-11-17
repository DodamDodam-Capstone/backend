# 보호자 권한 · guardian

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|시작]] → 기능 개요 → 요청 흐름 → 메서드 정의

현재 로그인 문맥의 보호자 확인 기한과 PIN 버전을 검증한다.

> [!info] 현재 범위
> 기반 구현 · PIN / 기록 API 후속

## 먼저 볼 흐름

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Flows/06-guardian|보호자 권한 확인]] — 유효한 guardianUntil 반환 · 요청에 재검사 대상 기록

## 주요 진입점

| 코드 | 책임 |
| --- | --- |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/guardian.application.GuardianAccessService#requireGuardianAccess mae5bd54dbbf05b63\|GuardianAccessService.requireGuardianAccess]] | 로그인 문맥·보호자 확인 기한·PIN 버전을 확인하고 응답 재검사를 연결한다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/guardian.application.GuardianAccessService#verifyResponseAccess m08ab0b3e274d6cb9\|GuardianAccessService.verifyResponseAccess]] | 로그인 문맥·보호자 확인 기한·PIN 버전을 확인하고 응답 재검사를 연결한다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/guardian.infrastructure.GuardianAccessRepository#findState mb1e4bd2e6cd4fcaf\|GuardianAccessRepository.findState]] | DB 시각을 기준으로 현재 문맥과 보호자 권한 상태를 함께 조회한다. |

## 수정할 때 찾을 위치

| 계층 | 담당 책임 | 클래스 |
| --- | --- | --- |
| `api` | HTTP 입력·응답·기능 오류 | [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/guardian.api.GuardianAccessExceptionHandler\|GuardianAccessExceptionHandler]] |
| `application` | 업무 규칙·실행 순서·트랜잭션 | [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/guardian.application.GuardianAccessService\|GuardianAccessService]] |
| `exception` | 공개 업무 오류 | [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/guardian.exception.GuardianAccessException\|GuardianAccessException]] |
| `infrastructure` | DB·잠금·외부 시스템 | [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/guardian.infrastructure.GuardianAccessRepository\|GuardianAccessRepository]] |

> [!example]- 전체 타입 5개 — 중첩 타입·데이터 포함
>
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/guardian.api.GuardianAccessExceptionHandler|guardian.api.GuardianAccessExceptionHandler]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/guardian.application.GuardianAccessService|guardian.application.GuardianAccessService]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/guardian.exception.GuardianAccessException|guardian.exception.GuardianAccessException]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/guardian.infrastructure.GuardianAccessRepository|guardian.infrastructure.GuardianAccessRepository]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/guardian.infrastructure.GuardianAccessRepository.GuardianAccessState|guardian.infrastructure.GuardianAccessRepository.GuardianAccessState]]

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Canvases/guardian.canvas|전체 호출선 보기 — 세부 조사용]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/90-Reference|전체 자료와 분석 한계]]
