# 보호자 권한 확인

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|시작]] → [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/guardian|기능 개요]] → 이 흐름 → 메서드 정의

**진입:** `인증·아이 소유권 검사를 마친 내부 호출자`

**결과:** 유효한 guardianUntil 반환 · 요청에 재검사 대상 기록

> [!important] 먼저 알아둘 점
> 공통 검증 서비스의 사용 계약이다. 이를 호출하는 PIN 설정·해제·기록 HTTP API까지 구현됐다는 뜻은 아니다.

![[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Assets/06-guardian.svg]]

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Flows/06-guardian.canvas|같은 흐름을 Canvas에서 열기]] · 아래 코드 링크로 실제 메서드 이동

번호와 화살표는 **주요 처리 순서**다. 조건에 따른 조기 반환·실패와 중간 helper 호출은 생략했다. 직접 Java 호출 관계는 연결된 메서드 노트에서 확인한다.

## 단계별 코드

### 1. 보호자 권한 요구

호출자가 인증·소유권 선행 · 현재 계정과 문맥 전달

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/guardian.application.GuardianAccessService#requireGuardianAccess mae5bd54dbbf05b63|GuardianAccessService.requireGuardianAccess]]

### 2. 현재 상태 함께 조회

문맥·계정·PIN 버전 조회 · DB 시각을 기준으로 검사

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/guardian.infrastructure.GuardianAccessRepository#findState mb1e4bd2e6cd4fcaf|GuardianAccessRepository.findState]]

### 3. 로그인 문맥 검사

세션 버전·폐기·만료 확인 · 무효면 인증 필요 오류

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/guardian.infrastructure.GuardianAccessRepository.GuardianAccessState#isSessionCurrent m51366fdd7d44c0f0|GuardianAccessState.isSessionCurrent]]

### 4. 보호자 확인 검사

guardianUntil과 PIN 버전 · 현재 권한인지 함께 확인

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/guardian.infrastructure.GuardianAccessRepository.GuardianAccessState#isGuardianCurrent m9b272566953e54cb|GuardianAccessState.isGuardianCurrent]]

### 5. 재검사 대상 기록

성공한 계정·문맥만 보관 · 권한 기한 자체는 연장 안 함

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/guardian.application.GuardianAccessService#requireGuardianAccess mae5bd54dbbf05b63|GuardianAccessService.requireGuardianAccess]]

### 6. 응답 전 다시 조회

AuthStateErrorFilter에서 호출 · 현재 DB 권한으로 재확인

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/guardian.application.GuardianAccessService#verifyResponseAccess m08ab0b3e274d6cb9|GuardianAccessService.verifyResponseAccess]]
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthStateErrorFilter#verifyResponseAccess m1098528f38d3ff11|AuthStateErrorFilter.verifyResponseAccess]]

## 실패하거나 달라지는 경우

| 조건 | 결과 / 주의 | 확인할 코드 |
| --- | --- | --- |
| 로그인 문맥 무효 | 401 · 인증 필요 | [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/guardian.application.GuardianAccessService#requireCurrentAccess mef6c2129fa7e5585\|GuardianAccessService.requireCurrentAccess]] |
| 보호자 확인 만료 / PIN 버전 변경 | 403 · 보호자 확인 필요 | [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/guardian.application.GuardianAccessService#requireCurrentAccess mef6c2129fa7e5585\|GuardianAccessService.requireCurrentAccess]] |
| 전송 전 권한 상실 | 성공 응답을 현재 오류로 교체 | [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/guardian.api.GuardianAccessExceptionHandler#handle mc83c0587f4b275ce\|GuardianAccessExceptionHandler.handle]] |

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/guardian|관련 클래스 찾기]]
