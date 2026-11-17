# 실행 · 저장 · 외부 연결 · app

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|시작]] → 기능 개요 → 요청 흐름 → 메서드 정의

Spring을 시작하고 DB·SMTP·후속 AI 연결의 경계를 확인한다.

> [!info] 현재 범위
> 현재 연결과 후속 연결을 구분

## 먼저 볼 흐름

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Flows/01-request|요청은 어디서 검사되는가]] — 요청이 검사를 통과하면 Controller로 전달한다. 실패는 해당 경계에서 중단한다.

## 주요 진입점

| 코드 | 책임 |
| --- | --- |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/DodamDodamBackendApplication#main m4511768b7ac61b27\|DodamDodamBackendApplication.main]] | Spring Boot 애플리케이션을 시작하는 main 진입점이다. |

## 현재 연결과 후속 연결

| 영역 | 현재 상태 |
| --- | --- |
| 실행 | Java 25 · Spring Boot · Servlet / Spring Security |
| DB | PostgreSQL · JdbcTemplate · Flyway V1~V7 · 현재 11개 테이블 |
| 메일 | SMTP 구현. 실제 공급자·환경 설정은 별도 확인 |
| AI | 이 backend에 STT / TTS / FastAPI 연결 구현 없음. AI 팀 저장소는 분석 범위 밖 |
| Supabase | 도입 검토 단계. 현재 연결로 그리지 않음 |

[DB 테이블·현재 구성](../../architecture.md#8-실제-db와-후속-설계의-차이) · [Supabase 검토](../../supabase-assessment.md)

## 수정할 때 찾을 위치

| 계층 | 담당 책임 | 클래스 |
| --- | --- | --- |
| `bootstrap` | 애플리케이션 시작 | [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/DodamDodamBackendApplication\|DodamDodamBackendApplication]] |

> [!example]- 전체 타입 1개 — 중첩 타입·데이터 포함
>
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/DodamDodamBackendApplication|DodamDodamBackendApplication]]

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Canvases/app.canvas|전체 호출선 보기 — 세부 조사용]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/90-Reference|전체 자료와 분석 한계]]
