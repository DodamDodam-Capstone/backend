# SCRUM-95 CSRF Lifecycle Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 현재 인증 필터에서 로그인·로그아웃 후 CSRF 쿠키 정리와 GET 재획득을 검증하고, 후속 인증 API의 연결 규칙을 기록한다.

**Architecture:** 기존 CookieCsrfTokenRepository, 헤더 전용 request handler, CsrfController와 Spring Security 인증·로그아웃 처리를 재사용한다. 실제 OAuth 인증 필터를 통과하되 외부 인증 결과만 테스트 대역으로 제공한다. 기본 처리로 만족하면 운영 코드를 추가하지 않는다.

**Tech Stack:** Java 25, Spring Boot 4.1.1, Spring Security 7.1.1, MockMvc, PostgreSQL Testcontainers.

**Spec:** [SCRUM-95](https://dodamdodam.atlassian.net/browse/SCRUM-95), [공통 계약](../../p0/api/01-common.md), [인증 계약 6.1·6.5·6.7](../../p0/api/02-auth.md), [이전 스텝 기록](2026-10-09-SCRUM-95-cors-cookie.md).

## Global Constraints

- 사용자에게 한 스텝씩 결과를 제시하고 다음 스텝은 확인 후 진행한다. Step 1은 dce99c4, Step 2는 f19fa53로 사용자 승인 후 로컬 commit했다. Step 3도 진행안 설명 후 승인을 받아 전체 검증·최종 검토·결과 기록을 진행하고, 설명 후 결과 기록의 로컬 commit 승인을 받았다. 다음 작업은 진행안 설명 후 별도 확인을 받는다.
- 기존 feature/SCRUM-95-csrf-cors와 사용자 .gitignore·gradlew.bat 변경을 보존한다. 구현 승인만으로 commit·push·PR·Jira 상태 변경을 진행하지 않는다.
- 발급 응답은 `{headerName, parameterName, token}`, POST 헤더는 X-XSRF-TOKEN, 실패는 403 CSRF_INVALID를 유지한다. 공통 no-store·서버 생성 X-Request-ID도 유지한다.
- 최초 진입·로그인 성공·로그아웃 성공 후 FE는 credentials를 포함한 GET /api/v1/auth/csrf로 token을 획득한다. 인증 성공 후 GET 전에는 이전 메모리 토큰을 사용하지 않는다.
- 공개 POST의 CSRF 면제, 파라미터 대체, 매 GET마다 강제 회전, POST 자동 재실행을 추가하지 않는다. 현재 쿠키 HttpOnly·Secure·SameSite 정책은 유지한다.
- 이메일 로그인 구현과 API 로그아웃의 DB 폐기·401/503·204 계약은 후속 인증/세션 작업에서 연결한다. 현재 기본 /logout의 동작 검증은 POST /api/v1/auth/logout 구현 완료를 뜻하지 않는다.
- 실제 OAuth 제공자·브라우저·HTTPS, JDBC 세션 저장·365일 갱신은 이 스텝의 검증 범위 밖이다. 토큰·쿠키 값·raw session ID는 출력하지 않는다.

## Review Focus

- 인증 성공 후 삭제 응답을 적용한 클라이언트가 이전 헤더를 전송하면 403이어야 한다. 재획득한 쿠키와 새 헤더로 POST가 성공해야 한다.
- 인증 실패에는 성공 시 토큰 정리 처리가 실행되지 않아야 한다.
- CSRF가 틀린 로그아웃 요청은 인증·세션을 종료하지 않아야 한다.
- 쿠키 만료는 발급과 동일 Path=/·host-only 및 환경별 속성을 사용해야 한다.
- CookieCsrfTokenRepository는 요청 쿠키에서 기대 토큰을 읽는다. 이전 쿠키·동일 헤더의 수동 재전송까지 서버에서 폐기했다고 주장하지 않는다. 세션에 결합한 서버 측 폐기가 필요하면 별도 저장 방식 결정이 필요하다.

## Task 1: 현재 필터의 수명주기 검증과 연결 기록

**Files:**
- Create: src/test/java/com/dodamdodam/backend/CsrfLifecycleContractTests.java
- Modify if a production defect is proven: src/main/java/com/dodamdodam/backend/global/config/SecurityConfig.java
- Modify: docs/DEVELOPMENT_SETUP.md
- Update: 이 계획 문서의 실행 결과

**Interfaces:** 기존 GET /api/v1/auth/csrf와 실제 SecurityFilterChain을 소비한다. 후속 이메일 로그인은 인증 완료·응답 확정 전에 구성된 SessionAuthenticationStrategy.onAuthentication(Authentication, HttpServletRequest, HttpServletResponse)를 실행해야 한다. 후속 API 로그아웃은 업무 DB 폐기가 성공한 뒤 현재 쿠키 repository를 사용하는 CsrfLogoutHandler의 정리를 실행해야 한다. 이 연결을 위한 미사용 운영 helper는 만들지 않는다.

- [x] **Step 1: 회귀 테스트 작성·현재 동작 확인.** @SpringBootTest, @AutoConfigureMockMvc(print=NONE), TestcontainersConfiguration과 기존 CsrfContractTests.AuthProbe를 사용한다. 새 테스트만 사용하는 보호 POST `/api/v1/csrf-lifecycle/probe`는 204를 반환한다.
  - 실제 FilterChainProxy의 OAuth2LoginAuthenticationFilter에서 AuthenticationManager만 테스트 대역으로 교체한다. 성공은 authenticated OAuth2LoginAuthenticationToken, 실패는 OAuth2AuthenticationException으로 제공한다. OAuth 시작 GET으로 authorization request/session을 만든 뒤 대응하는 callback GET을 수행한다. SessionAuthenticationStrategy·성공 처리·LogoutFilter·CSRF repository는 실제 설정을 사용한다. @DirtiesContext(AFTER_CLASS)로 대역 설정의 다른 테스트 전파를 막는다.
  - `repeatedGetKeepsExistingToken`: 기존 쿠키를 보낸 반복 GET은 같은 token을 반환한다. GET마다 강제 회전하지 않는다.
  - `oauthSuccessClearsTokenAndAllowsRefresh`: 성공 302에서 XSRF-TOKEN 만료와 공통 헤더 확인 → 삭제를 적용해 CSRF 쿠키 없이 이전 헤더로 보호 POST → 403 → 같은 로그인 세션으로 GET 재획득 → 이전 값과 다른 token → 새 쿠키·헤더로 보호 POST 204.
  - `logoutClearsTokenAndAllowsAnonymousRefresh`: OAuth 로그인 뒤 재획득한 token으로 현재 POST /logout → 성공 302·CSRF 쿠키 만료·세션 무효 → 이전 헤더만 보낸 공개 POST 403 → 익명 GET 재획득 → 새 쿠키·헤더로 기존 공개 AuthProbe POST 204.
  - `oauthFailureKeepsExistingCsrf`: 인증 실패 뒤 기존 쿠키를 보낸 GET의 token이 유지되고, 해당 쿠키·헤더로 공개 AuthProbe POST가 통과한다.
  - `invalidLogoutKeepsAuthenticatedSession`: 헤더 누락·불일치 두 사례는 각각 403 CSRF_INVALID이며 쿠키 만료 없음. 이어서 기존 세션·정상 token으로 보호 POST 204.
  - 토큰 비교는 boolean 결과만 assertion에 전달하고 쿠키는 속성만 검사한다. `.with(user())`, `.with(oauth2Login())`, `.with(csrf())`로 수명주기 자체를 대신하지 않는다.
  - Run: Java 25에서 `./gradlew test --tests '*CsrfLifecycleContractTests' --no-build-cache`. 기본 처리로 이미 통과한 사례는 회귀 검증으로 기록한다. 실제 실패는 테스트 대역 오류와 구분해 원인·결과를 보고하고 사용자 확인을 받는다.

- [x] **Step 2: 필요한 수정과 연동 안내.** Step 1 확인 후 진행한다. 운영 설정 누락으로 실패한 경우에만 기존 Spring Security 처리 연결을 최소 수정하고 해당 테스트의 실패→통과를 확인한다. 모두 통과했다면 운영 코드 수정 없이 DEVELOPMENT_SETUP에 FE 호출 순서와 후속 인증 API의 전략/정리 호출 시점을 기록한다. 실제 이메일 로그인·API 로그아웃 구현은 추가하지 않는다.

- [x] **Step 3: 전체 검증·결과 기록.** Step 2 확인 후 진행한다. Java 25에서 `./gradlew clean check --no-build-cache`와 `git diff --check`를 실행한다. 기존 41개와 새 사례의 통과·실패·skip을 실제 결과로 기록하고 변경 파일·연동 미완료 항목을 제시한다. commit은 사용자 요청 후 진행한다.

## 근거와 현재 확인 결과 — 2026-10-10 KST

- 현재 SecurityConfig는 oauth2Login과 기본 logout을 설정한다. 이메일 로그인 컨트롤러와 계약 경로의 API 로그아웃은 현재 코드에 없다.
- Spring Security는 인증 성공과 로그아웃 성공에서 기존 CSRF를 정리하며, 명시적 GET endpoint로 이후 재획득하는 흐름을 안내한다. [Spring Security CSRF](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html)
- 구성된 인증 전략은 실제 인증 필터에서 인증 성공 직후 호출된다. [Spring Security 7.1.1 인증 필터](https://github.com/spring-projects/spring-security/blob/7.1.1/web/src/main/java/org/springframework/security/web/authentication/AbstractAuthenticationProcessingFilter.java)
- 현재 cookie repository는 서버 측 토큰 폐기 목록을 두지 않는다. 삭제 응답 이후의 정상 클라이언트 흐름과 수동 쿠키 재전송의 보장을 구분한다. [Spring Security 7.1.1 cookie repository](https://github.com/spring-projects/spring-security/blob/7.1.1/web/src/main/java/org/springframework/security/web/csrf/CookieCsrfTokenRepository.java)
- 계획 설명 후 Step 1 구현 요청을 받았고, 구현·검증 결과를 설명한 뒤 기록과 테스트의 로컬 commit 요청을 받아 dce99c4로 보관했다. 이후 Step 2 연동 문서를 작성하고 설명한 뒤 승인받아 f19fa53로 보관했다. Step 3 전체 검증도 승인받아 진행했으며 결과 설명 후 로컬 commit 요청을 받았다.

## Step 1 실행 결과 — 2026-10-10 KST

- CsrfLifecycleContractTests의 6개 사례가 실제 인증·로그아웃 필터와 기존 CSRF API에서 통과했다. 외부 인증 결과만 대체했으며 운영 소스는 변경하지 않았다.
- 최초 테스트 실행은 6개 중 3개 실패했다. OAuth state의 URL 인코딩을 그대로 callback 파라미터로 넘긴 테스트 입력 오류였다. 디코딩과 실제 인증 성공/실패 검사로 바로잡았으며 운영 결함으로 기록하지 않는다.
- 테스트 환경에서 인증 필터의 세션 전략을 잠시 NullAuthenticatedSessionStrategy로 교체했을 때 로그인 쿠키 정리 테스트가 실패했다(1개 실행·1개 실패). 이 변경을 제거한 뒤 전체 검증을 실행했다.
- Java 25에서 `./gradlew clean check --no-build-cache`: 기존 41개 + 새 6개 = 47개 통과, 실패·오류·skip 0. `git diff --check` 통과.
- 별도 읽기 전용 검토에서 Critical·Important·Minor 지적 없음. 검토자는 테스트 결과 XML과 성공 로그를 확인했으며 테스트를 재실행하지 않았다.
- CSRF 오류 응답이 새 쿠키를 발급할 수 있으므로, 403 뒤 GET 재획득 요청은 해당 응답 쿠키도 반영한다. 재획득 토큰은 JSON·쿠키가 일치하며 새 헤더로 POST가 통과한다.
- 현재 /logout의 302·세션 무효화와 로컬 Secure=false/SameSite=Lax의 CSRF 쿠키 만료 속성을 검증했다. 계약 경로의 API 로그아웃 204나 Secure=true/SameSite=None 환경의 실제 브라우저 동작을 구현·검증한 것은 아니다.
- 사용자 .gitignore·gradlew.bat 변경을 보존하고 commit에서 제외한다. 이번 후속 요청에 따라 테스트 파일과 이 기록만 로컬 commit으로 보관한다. push·PR·Jira 상태 변경은 진행하지 않는다.

## Step 2 실행 결과 — 2026-10-10 KST

- 사용자 요청에 따라 DEVELOPMENT_SETUP의 React 연동 기준에 FE 호출 순서·오류 복구·후속 Backend 연결 규칙을 반영했다. Step 1에서 운영 결함이 발견되지 않아 운영 소스·테스트는 수정하지 않았다.
- 최초 진입·로그인 성공·로그아웃 성공 후 이전 메모리 token을 비우고 credentials 포함 GET으로 재획득하도록 기록했다. 반복 GET의 token 재사용과 인증 실패 시 성공 정리를 적용하지 않는 동작도 명시했다.
- JavaScript 예시는 GET HTTP 상태·JSON headerName/token을 확인한 뒤 POST를 실행한다. GET 실패 시 이전 token을 복원하지 않고 POST를 보류한다. 예시 경로가 실제 구현 API가 아님을 표시했다.
- CSRF_INVALID·인증 필요·로그인 실패·네트워크 오류를 구분하고, 재획득과 업무 POST 재실행을 별도로 처리하도록 안내했다. 기존 쿠키·헤더 수동 재전송에 대한 서버 측 폐기 보장은 추가하지 않았다.
- 후속 이메일 로그인에는 세션 전략의 인증 성공 호출과 SecurityContextRepository 명시적 저장, 실제 세션 저장 성공 전 200 금지를 기록했다. 후속 API 로그아웃에는 업무 DB 폐기 커밋 후 인증 문맥·세션·CSRF·쿠키 정리 순서를 기록했다.
- 현재 /logout의 302와 향후 /api/v1/auth/logout의 204·401·503 계약을 구분했다. CORS가 /api/**에만 등록되어 있어 현재 /logout의 다른 origin FE fetch 사용을 전제로 하지 않도록 명시했다. 실제 인증 API·FE 코드·JDBC 세션·365일 갱신과 운영 쿠키 정책 변경은 추가하지 않았다.
- 문서 JavaScript 2개 블록 구문 검사와 네트워크 없는 7개 예시 확인을 통과했다: 정상 GET→POST, GET 503, data로 감싼 응답, 빈 token, GET 네트워크 실패, POST 403 후 자동 재실행 없음, 후속 재획득 실패 시 기존 메모리 token 비움.
- 변경 문서 2개의 로컬 링크·anchor 7개와 코드 블록 경계를 확인했고 `git diff --check`를 통과했다. .gitignore·gradlew.bat diff는 작업 전과 일치한다. 문서만 변경한 이번 스텝에서는 Gradle 테스트를 재실행하지 않았으며, Step 1의 47개 통과는 이전 검증 결과다.
- 사용자 승인에 따라 이번 스텝의 두 문서를 f19fa53 로컬 commit으로 보관했다. push·PR·Jira 상태 변경은 진행하지 않았다. 다음 Step 3은 진행안 설명 후 사용자 승인을 받아 실행했다.

## Step 3 실행 결과 — 2026-10-10 KST

- Java 25와 실행 중인 Docker daemon에서 `./gradlew clean check --no-build-cache`를 실행했다. 19:29 KST 확인 시 BUILD SUCCESSFUL이며 clean 이후 운영·테스트 컴파일과 test·check가 실행됐다. 새 검증은 한 번만 수행했고 최종 검토자는 테스트를 재실행하지 않았다.
- JUnit XML의 tests·failures·errors·skipped 속성을 직접 집계했다. 기존 41개와 수명주기 6개를 합한 47개가 모두 통과했으며 실패·오류·skip은 0이다.

| 테스트 | 실행 | 실패 | 오류 | skip |
| --- | ---: | ---: | ---: | ---: |
| CsrfContractTests | 14 | 0 | 0 | 0 |
| CsrfLifecycleContractTests | 6 | 0 | 0 | 0 |
| CorsContractTests | 8 | 0 | 0 | 0 |
| CookieContractTests: LocalHttp·SecureCookies | 4 | 0 | 0 | 0 |
| SecurityConfigTests | 13 | 0 | 0 | 0 |
| DodamDodamBackendApplicationTests | 2 | 0 | 0 | 0 |
| **합계** | **47** | **0** | **0** | **0** |

- 별도 읽기 전용 최종 검토에서 브랜치 전체 `d048b97..f19fa53`의 3개 commit과 관련 운영 설정·테스트·계약·연동 문서를 확인했다. Critical·Important·Minor 지적은 없었다. 검토자는 최종 XML 47개와 BUILD SUCCESSFUL 로그, 해당 commit 범위의 `git diff --check`를 직접 확인했다. 이는 기술 검토 결과이며 GitHub 팀 승인·필수 CI·Jira 완료를 대신하지 않는다.
- 현재 필터의 로그인 성공/실패·로그아웃·재획득 동작과 FE 안내가 일치한다. 후속 이메일 로그인에는 세션 전략·명시적 인증 문맥 저장, API 로그아웃에는 업무 DB 폐기 후 framework 정리가 필요하다는 연결 규칙도 확인했다.
- 이번 Step 3에서 운영 코드·테스트·설정은 수정하지 않았다. 현재 변경은 이 결과 기록뿐이며 사용자 .gitignore·gradlew.bat 변경은 그대로 보존한다. 사용자 요청으로 결과 기록만 로컬 commit한다. push·PR·Jira 상태 변경은 진행하지 않는다.

### 최종 검토의 후속 범위와 남은 영향

| 항목 | 결정과 남은 영향 |
| --- | --- |
| 이메일 로그인·API 로그아웃 | 실제 업무 검증·200/204/401/503·DB 실패 보상은 후속 인증 구현에서 연결한다. 현재 필터 검증으로 해당 기능이 제공된 것은 아니다. |
| 현재 /logout의 다른 origin FE fetch | CORS가 /api/**에만 적용되므로 지원을 전제로 하지 않는다. FE의 API 로그아웃은 후속 경로 구현 뒤 연결해야 한다. |
| 이전 쿠키·동일 헤더 수동 재전송 | 현재 cookie repository를 유지한다. 서버 측 폐기 목록에 의한 재전송 차단은 제공하지 않는다. |
| 실제 OAuth 공급자 검증 | 외부 인증 결과 대역을 사용했으므로 공급자의 자격·서명·state/nonce 검증은 실제 연동에서 확인한다. 외부 연동 오류가 남아 있을 수 있다. |
| 실제 HTTPS 브라우저 쿠키 | 로컬 Lax 수명주기와 Secure/None 발급 속성 테스트만 확인했다. HTTPS 브라우저 저장·전송·삭제와 Secure/None 만료는 별도 검증이 필요하다. |
| 운영 origin·SameSite·CSRF HttpOnly | 기존 정책을 유지한다. D17·실제 FE 소비 방식 확인 전 운영 값을 확정하거나 CSRF HttpOnly를 전환하지 않는다. 배포 호환성은 미확인이다. |
| JDBC 세션·365일 갱신·운영 부하 | SCRUM-94·운영 후속 범위로 유지한다. 현재 결과로 영속 세션·갱신·부하 계약이 구현됐다고 판단하지 않는다. |
| 실제 FE·rate limit·업무 오류 전체 구현 | 이번 연동 안내에서 기능 완료를 주장하지 않는다. 해당 구현과 호출 흐름의 별도 검증이 필요하다. |
| 사용자 변경·이번 결과 기록 | 사용자 .gitignore·gradlew.bat는 검토 commit 밖이며 보존한다. 최종 검토 이후 추가한 이 기록은 문서 검증 후 사용자 승인으로 별도 commit한다. |

이 계획의 Step 1~3 검증 범위는 완료했다. SCRUM-95 전체 이슈의 완료 조건·후속 인증 연결·GitHub 필수 CI와 팀 리뷰·PR 병합·Jira 자동 완료는 별도로 확인해야 한다. 다음 외부 작업은 사용자 확인 없이 진행하지 않는다.
