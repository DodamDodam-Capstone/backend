# SCRUM-95 CORS·Cookie Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 명시적 credentialed CORS와 환경별 세션·CSRF 쿠키 속성을 계약 v3.1에 맞춘다.

**Architecture:** 기존 SecurityConfig, AppSecurityProperties와 Spring Boot ServerProperties를 재사용한다. CORS는 기존 SecurityFilterChain에서 처리하고 CookieCsrfTokenRepository는 서버 세션 쿠키의 Secure·SameSite 설정을 공유한다.

**Tech Stack:** Java 25, Spring Boot 4.1.1, Spring Security 7.1.1, JUnit 5, MockMvc, PostgreSQL Testcontainers, JDK HttpClient.

**Spec:** [SCRUM-95](https://dodamdodam.atlassian.net/browse/SCRUM-95), [공통 API](../../p0/api/01-common.md), [D17](../../p0/Decision_Record.md).

## Global Constraints

- 한 번 승인받은 이번 스텝은 아래 테스트·구현·검증을 수행한 뒤 사용자에게 결과를 확인받는다. 다음 스텝을 자동 실행하지 않는다.
- 현재 브랜치 feature/SCRUM-95-csrf-cors와 기존 사용자 변경을 보존한다. 구현 스텝에서는 commit을 보류했으며, 2026-10-09 사용자 후속 요청에 따라 현재까지의 변경과 기록을 로컬 commit으로 보관한다. push·PR·Jira 상태 전환은 하지 않는다.
- 공개 인증 POST도 CSRF를 검사하며 기존 응답 형식과 no-store·서버 생성 X-Request-ID를 유지한다.
- D17의 실제 운영 origin·SameSite는 미확정이다. 로컬 기본값은 localhost:3000·localhost:5173, Secure=false, SameSite=Lax다.
- 세션 쿠키는 HttpOnly, Path=/, Domain 미설정(host-only)이며 HTTPS에서는 Secure=true다. XSRF-TOKEN은 기존 HttpOnly=false를 유지한다.
- FE가 JSON 토큰만 사용하는지 확인 후 CSRF HttpOnly=true 전환을 별도로 판단한다. 이번 스텝에서는 기존 false를 유지한다.
- 새 라이브러리와 실제 인증 API를 추가하지 않는다. 토큰·raw session ID·실제 .env를 읽거나 출력하지 않는다.
- JDBC 세션 저장·365일 갱신(SCRUM-94), 인증 후 토큰 정리·재발급, 실배포 브라우저 연동(SCRUM-129)은 후속 범위다.

## Review Focus

- 허용 origin과 비슷한 공격 origin / Origin:null → CORS 허용 없음.
- 쿠키 없는 preflight → 인증·CSRF에 막히지 않고 허용 origin만 200.
- 허용 origin의 CSRF 실패 → 403 JSON과 no-store·requestId·노출 헤더 유지.
- SameSite=None + Secure=false → 브라우저가 사용할 수 없는 설정이므로 기동 실패.
- 실제 서블릿 세션 생성 → MockMvc만으로 판단하지 않고 embedded server Set-Cookie의 속성을 검사. 실패 메시지에 쿠키 값은 출력하지 않음.

## Task 1: CORS 계약 보완

**Files:**
- Modify: src/main/java/com/dodamdodam/backend/global/config/SecurityConfig.java
- Create: src/test/java/com/dodamdodam/backend/CorsContractTests.java
- Create: src/test/java/com/dodamdodam/backend/global/config/SecurityConfigTests.java

**Interfaces:** 기존 corsConfigurationSource(AppSecurityProperties) → CorsConfigurationSource와 applicationSecurityFilterChain을 사용한다. 기존 CsrfContractTests.AuthProbe를 테스트에서 재사용한다.

- [x] **Step 1: 실패 테스트 작성.** CorsContractTests는 실제 SecurityFilterChain과 TestcontainersConfiguration을 사용한다. 다음 입력·응답을 고정한다.
  - OPTIONS /api/v1/auth/login, Origin=http://localhost:5173, 요청 method=POST, 요청 headers=Content-Type,X-XSRF-TOKEN, 쿠키 없음 → 200, Allow-Origin은 요청 origin, Allow-Credentials=true, POST·두 요청 헤더 허용.
  - GET /api/v1/auth/csrf, Origin=http://localhost:5173 → 200, Expose-Headers에 Location·X-Request-ID·Retry-After.
  - POST /api/v1/auth/login, 허용 origin, CSRF 없음 → 403 CSRF_INVALID, CORS 허용·노출 헤더, no-store와 오류/헤더 동일 requestId.
  - 비허용 origin https://untrusted.example / http://localhost:5173.untrusted.example / null의 preflight 및 실제 POST → 403, Allow-Origin 없음, no-store·UUID 요청 ID 유지.
  - GET /api/v1/auth/csrf, Origin 없음 → 기존 공개 200 유지.
  - SecurityConfigTests: origin 목록의 *·https://*.example.com·null·NULL·Null·null/은 IllegalArgumentException으로 CORS 설정 생성 실패; localhost 두 origin은 생성 성공.
- [x] **Step 2: red 확인.** Java 25로 `./gradlew test --tests '*CorsContractTests' --tests '*SecurityConfigTests' --no-build-cache`. 새 헤더 노출·잘못된 설정 거부가 없어 실패하는지 확인한다. 이미 동작하는 preflight/거절은 회귀 검사다.
- [x] **Step 3: 최소 구현.** 기존 allowedOrigins·allowCredentials=true·허용 method/header·/api/** 범위를 재사용한다. 노출 목록을 Location, X-Request-ID, Retry-After로 보완한다. CorsConfiguration이 정규화한 허용 origin에서 wildcard와 대소문자 무관 null을 거부하여 CORS bean 생성 시 기동을 중단한다.
- [x] **Step 4: 위 테스트 green 확인.** OPTIONS 전체에 permitAll을 추가하거나 CSRF를 면제해 검증을 통과시키지 않는다.

## Task 2: 쿠키 정책·환경변수 연결

**Files:**
- Modify: src/main/java/com/dodamdodam/backend/global/config/SecurityConfig.java
- Modify: src/main/resources/application.yml
- Modify: .env.example
- Modify: compose.yaml
- Modify: docs/DEVELOPMENT_SETUP.md
- Create: src/test/java/com/dodamdodam/backend/CookieContractTests.java
- Modify: src/test/java/com/dodamdodam/backend/global/config/SecurityConfigTests.java

**Interfaces:** SecurityConfig에 `CookieCsrfTokenRepository csrfTokenRepository(ServerProperties serverProperties)` bean을 추가한다. ServerProperties는 `org.springframework.boot.web.server.autoconfigure.ServerProperties`이며 `getServlet().getSession().getCookie()`를 읽는다. applicationSecurityFilterChain은 해당 repository bean을 주입받는다.

- [x] **Step 1: 실패 테스트 작성.** CookieContractTests에서 로컬/보안 설정을 각각 RANDOM_PORT embedded server로 실행한다. PostgreSQL Testcontainers와 테스트 전용 /login/session-cookie-probe GET을 사용하고 해당 probe에서만 request.getSession()을 호출한다. JDK HttpClient로 probe와 GET /api/v1/auth/csrf의 Set-Cookie를 검사한다.
  - Secure=false, SameSite=lax: JSESSIONID의 속성만 추출해 Path=/·HttpOnly·SameSite=Lax, Secure 없음·Domain 없음 검사. XSRF-TOKEN은 Path=/·SameSite=Lax, HttpOnly·Secure·Domain 없음 검사.
  - Secure=true, SameSite=none: 두 쿠키 모두 Secure·SameSite=None·Path=/·Domain 없음; HttpOnly는 JSESSIONID에만 존재.
  - factory 설정 검사: SameSite=None + Secure=false는 IllegalArgumentException, SameSite=None + Secure=true 및 SameSite=Lax + Secure=false는 성공. SameSite=Strict + Secure=true의 설정 생성도 검사하지만 Google OAuth 호환성을 보증하거나 운영 기본값으로 권장하지 않는다.
  - 쿠키 발급 값은 assertion 대상/로그에 넣지 않고 세미콜론 뒤 속성만 검사한다.
- [x] **Step 2: red 확인.** Java 25로 `./gradlew test --tests '*CookieContractTests' --tests '*SecurityConfigTests' --no-build-cache`. CSRF 쿠키의 SameSite 설정·잘못된 조합 거부 누락으로 실패하는지 확인한다.
- [x] **Step 3: 최소 구현.** application.yml의 세션 cookie.path를 /로 명시하고 same-site를 `${SESSION_COOKIE_SAME_SITE:lax}`로 연결한다. 기존 SESSION_COOKIE_SECURE를 유지한다. CSRF repository cookie customizer에 Path=/, HttpOnly=false, 세션 cookie의 Secure·SameSite를 적용한다. SameSite=None에는 Secure=true를 요구한다.
- [x] **Step 4: 환경·문서 반영.** .env.example에 SESSION_COOKIE_SAME_SITE=lax를 추가한다. Compose의 Secure 고정 false를 `${SESSION_COOKIE_SECURE:-false}`로 바꾸고 SameSite도 전달한다. DEVELOPMENT_SETUP에 변수, 두 쿠키의 HttpOnly 차이, 로컬/HTTPS 예시와 D17 미확정을 적는다.
- [x] **Step 5: 위 테스트 green 확인.** 실제 HTTPS나 브라우저의 cookie 저장·전송이 검증됐다고 주장하지 않는다. embedded HTTP 서버에서도 Secure=true 응답 속성은 검사할 수 있다.

## Task 3: 이번 스텝 검증·확인

- [x] Java 25에서 `./gradlew clean check --no-build-cache`를 실행하고 기존 CSRF 14개·기존 smoke 2개와 새 테스트의 실제 결과를 보고한다.
- [x] `docker compose config --quiet`와 변경 파일의 `git diff --check`를 확인한다. 실제 .env 값이나 확장된 Compose 설정을 출력하지 않는다.
- [x] 변경 파일·테스트 수·실패/skip·사용자 변경 보존 여부를 확인한다. 운영 origin·SameSite와 실제 로그인·브라우저 통합 미검증을 명시한다.
- [x] 사용자 후속 요청으로 현재 결과를 기록하고 로컬 commit으로 보관한다. 다음 토큰 수명주기 스텝은 별도 승인 후 진행한다.

참고: [Spring Security CORS](https://docs.spring.io/spring-security/reference/servlet/integrations/cors.html), [MDN Set-Cookie](https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Headers/Set-Cookie).

## 실행 결과 — 2026-10-09 KST

- Java 25 clean check --no-build-cache: 41개 통과, 실패·오류·skip 0.
- docker compose config --quiet 및 full 프로필 환경변수 전달 검사 통과.
- 별도 리뷰의 NULL/Null/null/ origin 검증 우회를 회귀 테스트 RED→GREEN으로 수정.
- 기존 사용자 변경을 보존하고 현재 구현·테스트·문서를 로컬 commit으로 보관한다. push·PR 없음. 다음 토큰 수명주기 스텝은 사용자 확인 대기.

## 현재까지 작업 기록 — SCRUM-95

작업 브랜치: `feature/SCRUM-95-csrf-cors`. [SCRUM-95](https://dodamdodam.atlassian.net/browse/SCRUM-95)는 일부 구현·검증을 마친 진행 중 작업이며 전체 완료로 처리하지 않는다.

| 구현 범위 | 현재 결과 |
| --- | --- |
| CSRF 발급 API | 기존 `{headerName, parameterName, token}` 응답과 공개 GET 유지. `data` wrapper 없음 |
| 공개 인증 POST | email-verifications·verify·signup·login·password-resets의 정확한 경로에 비로그인 접근 허용. CSRF는 면제하지 않음 |
| CSRF 검증 | `X-XSRF-TOKEN` 헤더만 검증. query/form `_csrf` 대체 거부. 헤더 누락·빈 값·불일치와 쿠키 누락은 403 |
| CSRF 오류 | `CSRF_INVALID`와 `{error:{code,message,requestId,fields}}` 반환. fields는 빈 배열 |
| 공통 응답 헤더 | 서버 생성 UUID `X-Request-ID`, 오류 본문의 동일 requestId, `Cache-Control: no-store` 적용 |
| CORS | 기존 명시적 origin·credentials 유지. Location·X-Request-ID·Retry-After 노출. 허용 preflight·POST 통과와 비허용 origin 거부 검증 |
| origin 설정 검증 | Spring 정규화 후 wildcard와 null을 대소문자 무관하게 거부. NULL·Null·null/ 우회도 회귀 테스트로 수정 |
| 쿠키 | 두 쿠키 Path=/·host-only. 세션 HttpOnly=true, CSRF HttpOnly=false 유지. Secure·SameSite 환경 설정 공유, None+Secure=false 기동 거부 |
| 환경·문서 | SESSION_COOKIE_SAME_SITE 추가, Compose Secure 고정값 제거, 개발 환경 안내 갱신 |

### 검증 기록

Java 25와 PostgreSQL 18.6 Testcontainers에서 `./gradlew clean check --no-build-cache`를 실행했다. 전체 41개 통과, 실패·오류·skip 0이다.

| 테스트 | 사례 수 | 확인 범위 |
| --- | ---: | --- |
| CsrfContractTests | 14 | 실제 발급 토큰·쿠키, 공개 POST, 잘못된 헤더·파라미터, JSON 오류·공통 헤더 |
| CorsContractTests | 8 | preflight·정상 204·CSRF 403, origin 허용/거부, credentials·노출 헤더 |
| CookieContractTests | 4 | embedded server의 실제 JSESSIONID·XSRF-TOKEN Set-Cookie 속성, 로컬 Lax·Secure None |
| SecurityConfigTests | 13 | 위험 origin과 잘못된 SameSite/Secure 설정 거부, 정상 설정 생성 |
| DodamDodamBackendApplicationTests | 2 | health·CSRF 발급 기본 동작 |

새 동작의 실패를 먼저 확인하고 구현 후 통과를 확인했다. 공개 인증 업무는 테스트 전용 컨트롤러로 SecurityFilterChain을 검사했으며 실제 회원가입·로그인 업무가 구현됐다는 의미는 아니다. 쿠키 테스트도 브라우저 저장·전송 검증과 구분한다.

`docker compose config --quiet`, full 프로필의 Secure=true·SameSite=none 환경변수 전달 검사, `git diff --check`도 통과했다. 별도 코드 검토에서 발견한 null origin 정규화 우회는 실패 테스트 3개를 추가한 뒤 수정했으며 최종 전체 검사로 확인했다.

### 남은 작업과 결정

- [ ] 인증 상태 변경 후 CSRF 정리·GET 재발급 및 실제 로그인 소비자 연동 검증. 다음 스텝 승인 후 진행한다.
- [ ] FE가 JSON 토큰만 사용하는지 또는 쿠키를 직접 읽는지 확인한 뒤 CSRF HttpOnly=true 전환 여부를 결정한다. 현재는 기존 false를 유지한다.
- [ ] D17 운영 origin·SameSite 확정과 SCRUM-129 실제 브라우저·HTTPS·OAuth 검증. Strict 설정 생성 테스트가 Google 로그인 호환성을 보증하지 않는다.
- [ ] SCRUM-94의 JDBC 세션 저장·로그아웃·365일 쿠키 갱신과 연동한다. 이번 구현에서는 기존 세션 timeout을 변경하지 않았다.
- [ ] Teamboard 계획 날짜 동기화는 확인하지 못했다. Jira 시작일 2026-10-09·예상 기한 2026-10-12와는 별도로 확인이 필요하다.

사용자 `.gitignore`·`gradlew.bat` 변경은 보존하고 이번 commit에서 제외한다. 실제 `.env`, 비밀값, 토큰·raw session ID, 테스트 실행 산출물과 내부 임시 작업 기록은 commit하지 않는다.
