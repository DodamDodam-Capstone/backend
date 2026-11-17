# DodamDodam · 처음 보는 담당자를 위한 코드 지도

**먼저 이 한 장으로 책임을 파악하고 → 맡은 요청 흐름을 선택하고 → 필요한 메서드로 들어가세요.**

> [!info] 기준과 범위
> 2026-11-17 확인 · SCRUM-77 작업을 포함한 2026-11-15 소스 스냅샷. 자동 갱신 문서는 아닙니다.

![[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Assets/00-Overview.svg]]

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Overview.canvas|한눈에 보는 구조를 Canvas에서 열기]]

그림은 요약 보기다. **아래 링크 또는 Canvas 카드의 링크**로 들어가면 해당 메서드까지 이동할 수 있다.

## 맡은 작업부터 찾기

| 확인하려는 것 | 먼저 열 문서 |
| --- | --- |
| 요청은 어디서 검사되는가 | [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Flows/01-request\|6단계 흐름 · 실제 메서드 · 실패 조건]] |
| 이메일 인증번호 발급 | [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Flows/02-email\|6단계 흐름 · 실제 메서드 · 실패 조건]] |
| 아이 등록과 소유권 | [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Flows/03-child\|6단계 흐름 · 실제 메서드 · 실패 조건]] |
| 대화 시작과 재전송 | [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Flows/04-conversation\|6단계 흐름 · 실제 메서드 · 실패 조건]] |
| 로그인 문맥 검증과 로그아웃 | [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Flows/05-session\|6단계 흐름 · 실제 메서드 · 실패 조건]] |
| 보호자 권한 확인 | [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Flows/06-guardian\|6단계 흐름 · 실제 메서드 · 실패 조건]] |
| 대화 종료 상태 조정 | [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Flows/07-closure\|6단계 흐름 · 실제 메서드 · 실패 조건]] |

## 꼭 구분할 구현 범위

| 현재 있는 것 | 이어서 필요한 것 |
| --- | --- |
| 이메일 발급·SMTP·발급 실패 보상 | 인증번호 검증·회원가입·OAuth 계정 연동 |
| 아이 등록 API·검증·소유권 검사 | **실제 characterId 목록 확정·연결** — 현재 운영 목록은 비어 있음 |
| 대화 시작·재전송·종료 상태 조정 | 음성 API·수동 종료 API·타이머·요약 worker |
| 보호자 권한 공통 검증 | PIN·기록 등 이를 사용하는 HTTP API |
| PostgreSQL·SMTP 연결 구현 | STT / TTS / FastAPI 연결, Supabase 도입 여부 |

## Obsidian에서 읽는 순서

1. `backend/docs`를 보관소로 연다. 이 노트는 **읽기 보기**에서 보면 그림과 링크가 함께 보인다.
2. 위 표에서 맡은 요청을 선택한다. 그림은 번호 순서대로 읽고, Canvas에서는 카드 안의 메서드 링크를 연다.
3. 메서드 노트에서 `호출하는 곳 / 호출되는 곳`을 따라간다. 상단 `기능 개요`로 돌아올 수 있다.
4. 전체 호출 누락이나 영향 범위를 조사할 때만 [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/90-Reference|전체 클래스·메서드 지도]]를 연다.

## 기능별 담당 코드

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/global|공통 요청 · 오류]] — 요청을 받기 전 검사하고 같은 오류 형식으로 응답한다.
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|인증 · 이메일]] — 현재 세션 권한을 확인하고 이메일 인증번호를 발급한다.
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/child|아이 등록 · 소유권]] — 로그인 계정에 아이를 등록하고 해당 아이의 소유자인지 확인한다.
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/conversation|대화 시작 · 상태]] — 아이의 대화를 시작하고 중복 시작과 종료 상태를 조정한다.
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/guardian|보호자 권한]] — 현재 로그인 문맥의 보호자 확인 기한과 PIN 버전을 검증한다.
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/app|실행 · 저장 · 외부 연결]] — Spring을 시작하고 DB·SMTP·후속 AI 연결의 경계를 확인한다.

[상세 아키텍처](../architecture.md) · [Supabase와 대안 비교](../supabase-assessment.md) · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/90-Reference|분석 범위·재생성·전체 목록]]
