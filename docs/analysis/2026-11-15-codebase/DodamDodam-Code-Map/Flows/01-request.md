# 요청은 어디서 검사되는가

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|시작]] → [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/global|기능 개요]] → 이 흐름 → 메서드 정의

**진입:** `HTTP 요청 → 공통 필터 → 보안 체인 → MVC`

**결과:** 요청이 검사를 통과하면 Controller로 전달한다. 실패는 해당 경계에서 중단한다.

> [!important] 먼저 알아둘 점
> 필터 화살표는 실행 경계의 요약이다. 필터끼리 직접 Java 메서드를 호출한다는 뜻은 아니다.

![[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Assets/01-request.svg]]

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Flows/01-request.canvas|같은 흐름을 Canvas에서 열기]] · 아래 코드 링크로 실제 메서드 이동

번호와 화살표는 **주요 처리 순서**다. 조건에 따른 조기 반환·실패와 중간 helper 호출은 생략했다. 직접 Java 호출 관계는 연결된 메서드 노트에서 확인한다.

## 단계별 코드

### 1. 요청 추적 · 응답 보호

requestId와 no-store 설정 · 인증 오류 처리 경계 구성

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.web.ApiResponseHeadersFilter#doFilterInternal mdb9438ca5f22a128|ApiResponseHeadersFilter.doFilterInternal]]
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthStateErrorFilter#doFilterInternal m039987860b5cf49f|AuthStateErrorFilter.doFilterInternal]]

### 2. 세션 · 보안 체인

세션 쿠키 갱신 경계 · Spring Session 뒤 CORS 처리

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.SessionCookieRenewalFilter#doFilterInternal m5b3b8f1a080501e9|SessionCookieRenewalFilter.doFilterInternal]]
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.config.SecurityConfig#applicationSecurityFilterChain m8a28e16ae80b5e1e|SecurityConfig.applicationSecurityFilterChain]]

### 3. 전역 요청 제한

API / OAuth IP 예산 예약 · 과도한 요청은 여기서 중단

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.web.GlobalApiRateLimitFilter#doFilterInternal me8918f88c454eb77|GlobalApiRateLimitFilter.doFilterInternal]]

### 4. 본문 전송 검사

해당 경로의 크기·형식 검사 · Part 2 → Part 1 필터 순서

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.web.Part2JsonRequestGuardFilter#doFilterInternal m6ee42797d374d4fc|Part2JsonRequestGuardFilter.doFilterInternal]]
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.web.Part1JsonRequestGuardFilter#doFilterInternal m647a0f12b7d38584|Part1JsonRequestGuardFilter.doFilterInternal]]

### 5. CSRF · 인증 · 업무 처리

Spring Security 검사 후 MVC · 예시: 아이 등록 Controller

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.config.SecurityConfig#applicationSecurityFilterChain m8a28e16ae80b5e1e|SecurityConfig.applicationSecurityFilterChain]]
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.api.ChildController#create m85a3864c9b94be48|ChildController.create]]

### 6. 응답 직전 재검사

대상 요청의 최신 권한 확인 · 실패한 성공 본문·쿠키 제거

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthStateErrorFilter#verifyResponseAccess m1098528f38d3ff11|AuthStateErrorFilter.verifyResponseAccess]]
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.api.ConversationStartResponseFilter#verify mbaf83885c918b297|ConversationStartResponseFilter.verify]]

## 실패하거나 달라지는 경우

| 조건 | 결과 / 주의 | 확인할 코드 |
| --- | --- | --- |
| 요청 제한 초과 | 429와 재시도 기한 | [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.web.GlobalApiRateLimitFilter#doFilterInternal me8918f88c454eb77\|GlobalApiRateLimitFilter.doFilterInternal]] |
| 권한 확인 저장소 장애 | 인증 상태 확인 불가 503 | [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthStateErrorFilter#doFilterInternal m039987860b5cf49f\|AuthStateErrorFilter.doFilterInternal]] |
| MVC 처리 오류 | 기능별 처리 후 공통 오류 처리 | [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.error.CommonApiExceptionHandler#handle m58650f47953e9086\|CommonApiExceptionHandler.handle]] |

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/global|관련 클래스 찾기]]
