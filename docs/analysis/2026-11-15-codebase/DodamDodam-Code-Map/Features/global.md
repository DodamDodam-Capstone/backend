# 공통 요청 · 오류 · global

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|시작]] → 기능 개요 → 요청 흐름 → 메서드 정의

요청을 받기 전 검사하고 같은 오류 형식으로 응답한다.

> [!info] 현재 범위
> 구현됨 · 공통 진입 경계

## 먼저 볼 흐름

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Flows/01-request|요청은 어디서 검사되는가]] — 요청이 검사를 통과하면 Controller로 전달한다. 실패는 해당 경계에서 중단한다.

## 주요 진입점

| 코드 | 책임 |
| --- | --- |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.config.SecurityConfig#applicationSecurityFilterChain m8a28e16ae80b5e1e\|SecurityConfig.applicationSecurityFilterChain]] | CORS·CSRF·OAuth와 사용자 정의 필터를 Spring Security 체인에 연결한다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.web.GlobalApiRateLimitFilter#doFilterInternal me8918f88c454eb77\|GlobalApiRateLimitFilter.doFilterInternal]] | API·OAuth 요청의 IP 예산을 업무 처리 전에 예약하고 초과 요청을 거부한다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.error.ApiErrorResponses#write m1b723e7027f0ade4\|ApiErrorResponses.write]] | 공통 오류 본문을 만들고 필터 등 MVC 밖에서도 JSON 오류를 쓴다. |

## 수정할 때 찾을 위치

| 계층 | 담당 책임 | 클래스 |
| --- | --- | --- |
| `auth` | CSRF·보안 응답 | [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.auth.CsrfController\|CsrfController]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.auth.CsrfAccessDeniedHandler\|CsrfAccessDeniedHandler]] |
| `config` | 프레임워크 구성 | [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.config.SecurityConfig\|SecurityConfig]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.config.AppSecurityProperties\|AppSecurityProperties]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.config.OAuth2ClientConfig\|OAuth2ClientConfig]] · 외 2개는 아래 전체 목록 |
| `error` | 공통 오류 형식 | [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.error.ApiErrorResponses\|ApiErrorResponses]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.error.CommonApiExceptionHandler\|CommonApiExceptionHandler]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.error.ApiErrorResponse\|ApiErrorResponse]] |
| `web` | 서블릿 요청 필터 | [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.web.GlobalApiRateLimitFilter\|GlobalApiRateLimitFilter]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.web.ApiResponseHeadersFilter\|ApiResponseHeadersFilter]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.web.Part1JsonRequestGuardFilter\|Part1JsonRequestGuardFilter]] · 외 2개는 아래 전체 목록 |

> [!example]- 전체 타입 22개 — 중첩 타입·데이터 포함
>
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.auth.CsrfAccessDeniedHandler|global.auth.CsrfAccessDeniedHandler]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.auth.CsrfController|global.auth.CsrfController]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.auth.CsrfController.CsrfTokenResponse|global.auth.CsrfController.CsrfTokenResponse]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.config.AppSecurityProperties|global.config.AppSecurityProperties]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.config.OAuth2ClientConfig|global.config.OAuth2ClientConfig]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.config.OAuth2ClientProperties|global.config.OAuth2ClientProperties]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.config.OAuth2ClientProperties.Credentials|global.config.OAuth2ClientProperties.Credentials]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.config.SecurityConfig|global.config.SecurityConfig]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.config.SecurityConfig_anonymous_120|global.config.SecurityConfig$anonymous@120]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.config.SessionConfig|global.config.SessionConfig]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.error.ApiErrorResponse|global.error.ApiErrorResponse]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.error.ApiErrorResponse.ErrorDetail|global.error.ApiErrorResponse.ErrorDetail]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.error.ApiErrorResponse.FieldError|global.error.ApiErrorResponse.FieldError]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.error.ApiErrorResponses|global.error.ApiErrorResponses]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.error.CommonApiExceptionHandler|global.error.CommonApiExceptionHandler]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.web.ApiResponseHeadersFilter|global.web.ApiResponseHeadersFilter]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.web.ClientAddressResolver|global.web.ClientAddressResolver]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.web.GlobalApiRateLimitFilter|global.web.GlobalApiRateLimitFilter]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.web.Part1JsonRequestGuardFilter|global.web.Part1JsonRequestGuardFilter]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.web.Part1JsonRequestGuardFilter.CachedBodyRequest|global.web.Part1JsonRequestGuardFilter.CachedBodyRequest]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.web.Part1JsonRequestGuardFilter.CachedBodyRequest_anonymous_97|global.web.Part1JsonRequestGuardFilter.CachedBodyRequest$anonymous@97]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.web.Part2JsonRequestGuardFilter|global.web.Part2JsonRequestGuardFilter]]

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Canvases/global.canvas|전체 호출선 보기 — 세부 조사용]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/90-Reference|전체 자료와 분석 한계]]
