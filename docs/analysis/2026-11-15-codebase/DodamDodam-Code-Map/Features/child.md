# 아이 등록 · 소유권 · child

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|시작]] → 기능 개요 → 요청 흐름 → 메서드 정의

로그인 계정에 아이를 등록하고 해당 아이의 소유자인지 확인한다.

> [!info] 현재 범위
> 선행 조건 · 실제 캐릭터 목록 미정

## 먼저 볼 흐름

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Flows/03-child|아이 등록과 소유권]] — 등록 성공 시 201 · data + Location

## 주요 진입점

| 코드 | 책임 |
| --- | --- |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.api.ChildController#create m85a3864c9b94be48\|ChildController.create]] | 아이 등록 HTTP 진입점. 세션의 계정으로 등록하고 201 응답을 만든다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.application.ChildService#create m022c1fb522a0efea\|ChildService.create]] | 아이 입력 검증·등록과 소유권 검사를 수행한다. 등록 시 현재 보안 문맥을 잠근다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.application.ChildService#requireChildAccess mbb6af419b1ceab47\|ChildService.requireChildAccess]] | 아이 입력 검증·등록과 소유권 검사를 수행한다. 등록 시 현재 보안 문맥을 잠근다. |

## 수정할 때 찾을 위치

| 계층 | 담당 책임 | 클래스 |
| --- | --- | --- |
| `api` | HTTP 입력·응답·기능 오류 | [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.api.ChildController\|ChildController]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.api.ChildExceptionHandler\|ChildExceptionHandler]] |
| `application` | 업무 규칙·실행 순서·트랜잭션 | [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.application.ChildService\|ChildService]] |
| `dto` | 요청 검증·전달 데이터 | [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.dto.ChildContext\|ChildContext]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.dto.ChildView\|ChildView]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.dto.CreateChildRequest\|CreateChildRequest]] |
| `exception` | 공개 업무 오류 | [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.exception.ChildException\|ChildException]] |
| `infrastructure` | DB·잠금·외부 시스템 | [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.infrastructure.ChildRepository\|ChildRepository]] |

> [!example]- 전체 타입 9개 — 중첩 타입·데이터 포함
>
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.api.ChildController|child.api.ChildController]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.api.ChildExceptionHandler|child.api.ChildExceptionHandler]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.application.ChildService|child.application.ChildService]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.dto.ChildContext|child.dto.ChildContext]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.dto.ChildView|child.dto.ChildView]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.dto.ChildView.Gender|child.dto.ChildView.Gender]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.dto.CreateChildRequest|child.dto.CreateChildRequest]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.exception.ChildException|child.exception.ChildException]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.infrastructure.ChildRepository|child.infrastructure.ChildRepository]]

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Canvases/child.canvas|전체 호출선 보기 — 세부 조사용]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/90-Reference|전체 자료와 분석 한계]]
