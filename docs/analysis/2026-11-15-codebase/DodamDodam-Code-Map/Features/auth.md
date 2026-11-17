# 인증 · 이메일 · auth

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|시작]] → 기능 개요 → 요청 흐름 → 메서드 정의

현재 세션 권한을 확인하고 이메일 인증번호를 발급한다.

> [!info] 현재 범위
> 부분 구현 · 가입 / OAuth 연동 후속

## 먼저 볼 흐름

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Flows/02-email|이메일 인증번호 발급]] — 200 · data.challengeId / expiresAt
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Flows/05-session|로그인 문맥 검증과 로그아웃]] — 현재 계정·문맥 반환 · 로그아웃 성공은 204

## 주요 진입점

| 코드 | 책임 |
| --- | --- |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.api.EmailVerificationController#issue m90c67bb20e0ca690\|EmailVerificationController.issue]] | 이메일 발급 HTTP 진입점. JSON을 읽고 PIN 목적의 선행 인증을 처리한다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthenticatedSessionService#requireAuthenticatedSession mad23075b25d06a28\|AuthenticatedSessionService.requireAuthenticatedSession]] | 프레임워크 인증·저장된 세션·현재 DB 보안 문맥이 일치하는지 검증한다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.api.AuthLogoutController#logout m43cbd6436794ba0f\|AuthLogoutController.logout]] | 현재 업무 문맥을 폐기하고 네이티브 세션·CSRF 정보를 정리한다. |

## 수정할 때 찾을 위치

| 계층 | 담당 책임 | 클래스 |
| --- | --- | --- |
| `api` | HTTP 입력·응답·기능 오류 | [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.api.AuthLogoutController\|AuthLogoutController]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.api.EmailVerificationController\|EmailVerificationController]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.api.EmailVerificationExceptionHandler\|EmailVerificationExceptionHandler]] |
| `application` | 업무 규칙·실행 순서·트랜잭션 | [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.AuthRateLimitService\|AuthRateLimitService]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.EmailVerificationCodeGenerator\|EmailVerificationCodeGenerator]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.EmailVerificationDeliveryService\|EmailVerificationDeliveryService]] · 외 1개는 아래 전체 목록 |
| `domain` | 보조 타입 | [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.domain.AuthRateLimitKey\|AuthRateLimitKey]] |
| `dto` | 요청 검증·전달 데이터 | [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.dto.IssueEmailVerificationRequest\|IssueEmailVerificationRequest]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.dto.VerificationIssueResponse\|VerificationIssueResponse]] |
| `exception` | 공개 업무 오류 | [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.exception.AuthRateLimitException\|AuthRateLimitException]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.exception.EmailVerificationException\|EmailVerificationException]] |
| `infrastructure` | DB·잠금·외부 시스템 | [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.AuthRateLimitRepository\|AuthRateLimitRepository]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationAccountRepository\|EmailVerificationAccountRepository]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationKeys\|EmailVerificationKeys]] · 외 4개는 아래 전체 목록 |
| `session` | 현재 인증 문맥·세션 수명 | [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthenticatedSessionService\|AuthenticatedSessionService]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthStateErrorFilter\|AuthStateErrorFilter]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthenticatedSession\|AuthenticatedSession]] · 외 7개는 아래 전체 목록 |

> [!example]- 전체 타입 43개 — 중첩 타입·데이터 포함
>
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.api.AuthLogoutController|auth.api.AuthLogoutController]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.api.EmailVerificationController|auth.api.EmailVerificationController]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.api.EmailVerificationExceptionHandler|auth.api.EmailVerificationExceptionHandler]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.AuthRateLimitService|auth.application.AuthRateLimitService]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.EmailVerificationCodeGenerator|auth.application.EmailVerificationCodeGenerator]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.EmailVerificationDeliveryService|auth.application.EmailVerificationDeliveryService]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.EmailVerificationIssuanceService|auth.application.EmailVerificationIssuanceService]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.EmailVerificationIssuanceService.Prepared|auth.application.EmailVerificationIssuanceService.Prepared]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.domain.AuthRateLimitKey|auth.domain.AuthRateLimitKey]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.domain.AuthRateLimitKey.Action|auth.domain.AuthRateLimitKey.Action]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.dto.IssueEmailVerificationRequest|auth.dto.IssueEmailVerificationRequest]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.dto.IssueEmailVerificationRequest.Purpose|auth.dto.IssueEmailVerificationRequest.Purpose]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.dto.VerificationIssueResponse|auth.dto.VerificationIssueResponse]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.exception.AuthRateLimitException|auth.exception.AuthRateLimitException]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.exception.EmailVerificationException|auth.exception.EmailVerificationException]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.AuthRateLimitKeyHasher|auth.infrastructure.AuthRateLimitKeyHasher]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.AuthRateLimitRepository|auth.infrastructure.AuthRateLimitRepository]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.AuthRateLimitRepository.RateLimitState|auth.infrastructure.AuthRateLimitRepository.RateLimitState]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationAccountRepository|auth.infrastructure.EmailVerificationAccountRepository]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationAccountRepository.AccountState|auth.infrastructure.EmailVerificationAccountRepository.AccountState]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationAccountRepository.PinState|auth.infrastructure.EmailVerificationAccountRepository.PinState]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationCodeHasher|auth.infrastructure.EmailVerificationCodeHasher]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationKeys|auth.infrastructure.EmailVerificationKeys]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationRepository|auth.infrastructure.EmailVerificationRepository]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationRepository.NewChallenge|auth.infrastructure.EmailVerificationRepository.NewChallenge]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationRepository.IssuedChallenge|auth.infrastructure.EmailVerificationRepository.IssuedChallenge]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.SmtpEmailVerificationSender|auth.infrastructure.SmtpEmailVerificationSender]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthStateErrorFilter|auth.session.AuthStateErrorFilter]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthStateErrorFilter_anonymous_52|auth.session.AuthStateErrorFilter$anonymous@52]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthStateErrorHandler|auth.session.AuthStateErrorHandler]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthStateUnavailableException|auth.session.AuthStateUnavailableException]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthenticatedSession|auth.session.AuthenticatedSession]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthenticatedSessionService|auth.session.AuthenticatedSessionService]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthenticatedSessionService.CookieRenewal|auth.session.AuthenticatedSessionService.CookieRenewal]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.LoginSecurityContextRepository|auth.session.LoginSecurityContextRepository]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.LoginSessionAuthenticationStrategy|auth.session.LoginSessionAuthenticationStrategy]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.SessionCookieRenewalFilter|auth.session.SessionCookieRenewalFilter]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.SessionCookieRenewalFilter_anonymous_41|auth.session.SessionCookieRenewalFilter$anonymous@41]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.SessionCookieRenewalFilter.InvalidSessionException|auth.session.SessionCookieRenewalFilter.InvalidSessionException]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.SessionLifecycleService|auth.session.SessionLifecycleService]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.SessionLifecycleService.PendingLogin|auth.session.SessionLifecycleService.PendingLogin]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.SessionSecurityRepository|auth.session.SessionSecurityRepository]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.SessionSecurityRepository.SessionValidity|auth.session.SessionSecurityRepository.SessionValidity]]

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Canvases/auth.canvas|전체 호출선 보기 — 세부 조사용]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/90-Reference|전체 자료와 분석 한계]]
