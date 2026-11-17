# 로그인 문맥 검증과 로그아웃

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|시작]] → [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요]] → 이 흐름 → 메서드 정의

**진입:** `로그인이 필요한 요청 · POST /api/v1/auth/logout`

**결과:** 현재 계정·문맥 반환 · 로그아웃 성공은 204

> [!important] 먼저 알아둘 점
> 1~3단계는 인증 공통 경로다. 4~6단계는 로그아웃 요청에서만 수행하며 로그인·OAuth 계정 연동 전체 흐름은 아니다.

![[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Assets/05-session.svg]]

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Flows/05-session.canvas|같은 흐름을 Canvas에서 열기]] · 아래 코드 링크로 실제 메서드 이동

번호와 화살표는 **주요 처리 순서**다. 조건에 따른 조기 반환·실패와 중간 helper 호출은 생략했다. 직접 Java 호출 관계는 연결된 메서드 노트에서 확인한다.

## 단계별 코드

### 1. 현재 인증 요청

실제 요청의 세션을 확인 · 없거나 무효면 인증 거부

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthenticatedSessionService#requireAuthenticatedSession mad23075b25d06a28|AuthenticatedSessionService.requireAuthenticatedSession]]

### 2. 저장된 정보 대조

Authentication과 저장 세션 · SecurityContext 일치 검사

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthenticatedSessionService#validate m72fbb73c9b868baa|AuthenticatedSessionService.validate]]

### 3. 업무 문맥 검증

계정·세션 버전·폐기·만료 · DB의 현재 상태로 판단

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.SessionSecurityRepository#isCurrentContext mda2203671fdb2db3|SessionSecurityRepository.isCurrentContext]]

### 4. 로그아웃 요청 검사

인증 성공과 빈 본문 확인 · 이후는 로그아웃 전용 경로

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.api.AuthLogoutController#logout m43cbd6436794ba0f|AuthLogoutController.logout]]

### 5. 업무 문맥 폐기

현재 문맥과 PIN 증명 정리 · 뒤이은 요청에서 사용 불가

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.SessionSecurityRepository#revokeCurrentContext ma61a7dc8bfc5d117|SessionSecurityRepository.revokeCurrentContext]]

### 6. 세션 · CSRF 정리

Spring 로그아웃 핸들러 호출 · 완료 시 204 응답

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.api.AuthLogoutController#logout m43cbd6436794ba0f|AuthLogoutController.logout]]

## 실패하거나 달라지는 경우

| 조건 | 결과 / 주의 | 확인할 코드 |
| --- | --- | --- |
| 문맥 만료 / 폐기 / 불일치 | 401 · 인증 필요 | [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthenticatedSessionService#validate m72fbb73c9b868baa\|AuthenticatedSessionService.validate]] |
| 인증 저장소 확인 실패 | 503 · 미로그인과 구별 | [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthenticatedSessionService#requireAuthenticatedSession mad23075b25d06a28\|AuthenticatedSessionService.requireAuthenticatedSession]] |
| 로그아웃 본문이 비어 있지 않음 | 400 · 입력 오류 | [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.api.AuthLogoutController#logout m43cbd6436794ba0f\|AuthLogoutController.logout]] |

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|관련 클래스 찾기]]
