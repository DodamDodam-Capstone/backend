# 전체 클래스·메서드 호출 목록

2026-11-15 현재 작업 파일을 포함한 스냅샷이다. `method-map.json`이 기계 판독 원본이며 `snapshot.json`에 소스별 SHA-256이 있다.

71개 Java 파일 · 106개 타입 · 직접 작성 271개 메서드/생성자 · 컴파일러 생성 295개 멤버 · 내부 호출 쌍 541개.

`호출자 → 현재 메서드 → 호출 대상`으로 읽는다. 메서드 참조는 콜백 등록일 수 있으며 즉시 호출을 뜻하지 않는다. lambda 내부 호출은 포함 메서드에 귀속했다. Spring·Servlet 진입은 별도 표시했다. 호출자가 없다는 이유로 미사용 코드를 뜻하지 않는다.

[구조·요청 흐름](architecture.md) · [Supabase 비교](supabase-assessment.md)

[전체 호출 그림](DodamDodam-Code-Map/Assets/all-class-method-calls.svg) · [Obsidian 탐색 시작](DodamDodam-Code-Map/00-Start.md) · [전체 메서드 Canvas](DodamDodam-Code-Map/02-All-Methods.canvas)

## 클래스 색인

- [DodamDodamBackendApplication](#c1) — 작성 1, 자동 1
- [auth.api.AuthLogoutController](#c2) — 작성 2, 자동 0
- [auth.api.EmailVerificationController](#c3) — 작성 2, 자동 0
- [auth.api.EmailVerificationExceptionHandler](#c4) — 작성 2, 자동 1
- [auth.application.AuthRateLimitService](#c5) — 작성 2, 자동 0
- [auth.application.EmailVerificationCodeGenerator](#c6) — 작성 3, 자동 0
- [auth.application.EmailVerificationDeliveryService](#c7) — 작성 3, 자동 0
- [auth.application.EmailVerificationIssuanceService](#c8) — 작성 5, 자동 0
- [auth.application.EmailVerificationIssuanceService.Prepared](#c9) — 작성 0, 자동 6
- [auth.domain.AuthRateLimitKey](#c10) — 작성 2, 자동 4
- [auth.domain.AuthRateLimitKey.Action](#c11) — 작성 3, 자동 2
- [auth.dto.IssueEmailVerificationRequest](#c12) — 작성 4, 자동 6
- [auth.dto.IssueEmailVerificationRequest.Purpose](#c13) — 작성 0, 자동 3
- [auth.dto.VerificationIssueResponse](#c14) — 작성 0, 자동 6
- [auth.exception.AuthRateLimitException](#c15) — 작성 3, 자동 0
- [auth.exception.EmailVerificationException](#c16) — 작성 11, 자동 0
- [auth.infrastructure.AuthRateLimitKeyHasher](#c17) — 작성 6, 자동 0
- [auth.infrastructure.AuthRateLimitRepository](#c18) — 작성 3, 자동 0
- [auth.infrastructure.AuthRateLimitRepository.RateLimitState](#c19) — 작성 2, 자동 7
- [auth.infrastructure.EmailVerificationAccountRepository](#c20) — 작성 4, 자동 0
- [auth.infrastructure.EmailVerificationAccountRepository.AccountState](#c21) — 작성 0, 자동 7
- [auth.infrastructure.EmailVerificationAccountRepository.PinState](#c22) — 작성 1, 자동 12
- [auth.infrastructure.EmailVerificationCodeHasher](#c23) — 작성 2, 자동 0
- [auth.infrastructure.EmailVerificationKeys](#c24) — 작성 4, 자동 0
- [auth.infrastructure.EmailVerificationRepository](#c25) — 작성 7, 자동 0
- [auth.infrastructure.EmailVerificationRepository.NewChallenge](#c26) — 작성 0, 자동 15
- [auth.infrastructure.EmailVerificationRepository.IssuedChallenge](#c27) — 작성 0, 자동 6
- [auth.infrastructure.SmtpEmailVerificationSender](#c28) — 작성 4, 자동 0
- [auth.session.AuthStateErrorFilter](#c29) — 작성 5, 자동 0
- [auth.session.AuthStateErrorFilter$anonymous@52](#c30) — 작성 1, 자동 1
- [auth.session.AuthStateErrorHandler](#c31) — 작성 3, 자동 0
- [auth.session.AuthStateUnavailableException](#c32) — 작성 2, 자동 0
- [auth.session.AuthenticatedSession](#c33) — 작성 0, 자동 6
- [auth.session.AuthenticatedSessionService](#c34) — 작성 5, 자동 0
- [auth.session.AuthenticatedSessionService.CookieRenewal](#c35) — 작성 0, 자동 6
- [auth.session.LoginSecurityContextRepository](#c36) — 작성 2, 자동 0
- [auth.session.LoginSessionAuthenticationStrategy](#c37) — 작성 2, 자동 0
- [auth.session.SessionCookieRenewalFilter](#c38) — 작성 3, 자동 0
- [auth.session.SessionCookieRenewalFilter$anonymous@41](#c39) — 작성 1, 자동 1
- [auth.session.SessionCookieRenewalFilter.InvalidSessionException](#c40) — 작성 0, 자동 1
- [auth.session.SessionLifecycleService](#c41) — 작성 8, 자동 0
- [auth.session.SessionLifecycleService.PendingLogin](#c42) — 작성 1, 자동 0
- [auth.session.SessionSecurityRepository](#c43) — 작성 7, 자동 0
- [auth.session.SessionSecurityRepository.SessionValidity](#c44) — 작성 1, 자동 9
- [child.api.ChildController](#c45) — 작성 3, 자동 0
- [child.api.ChildExceptionHandler](#c46) — 작성 1, 자동 1
- [child.application.ChildService](#c47) — 작성 6, 자동 0
- [child.dto.ChildContext](#c48) — 작성 0, 자동 11
- [child.dto.ChildView](#c49) — 작성 0, 자동 12
- [child.dto.ChildView.Gender](#c50) — 작성 0, 자동 3
- [child.dto.CreateChildRequest](#c51) — 작성 5, 자동 10
- [child.exception.ChildException](#c52) — 작성 7, 자동 0
- [child.infrastructure.ChildRepository](#c53) — 작성 5, 자동 0
- [conversation.api.ConversationController](#c54) — 작성 3, 자동 0
- [conversation.api.ConversationController.PreparedStart](#c55) — 작성 0, 자동 7
- [conversation.api.ConversationExceptionHandler](#c56) — 작성 2, 자동 1
- [conversation.api.ConversationMediaTypeExceptionHandler](#c57) — 작성 1, 자동 1
- [conversation.api.ConversationStartResponseFilter](#c58) — 작성 8, 자동 0
- [conversation.api.ConversationStartResponseFilter$anonymous@86](#c59) — 작성 1, 자동 1
- [conversation.api.ConversationStartResponseFilter.ResponseAccess](#c60) — 작성 0, 자동 8
- [conversation.api.ConversationStartResponseFilter.ResponseRejected](#c61) — 작성 1, 자동 0
- [conversation.application.ConversationClosureCoordinator](#c62) — 작성 3, 자동 0
- [conversation.application.ConversationClosureCoordinator.Trigger](#c63) — 작성 0, 자동 3
- [conversation.application.ConversationClosureCoordinator.ClosureResult](#c64) — 작성 0, 자동 6
- [conversation.application.ConversationService](#c65) — 작성 6, 자동 0
- [conversation.application.ConversationService.StartOutcome](#c66) — 작성 0, 자동 6
- [conversation.application.ConversationService.StartDecision](#c67) — 작성 0, 자동 6
- [conversation.domain.ConversationServiceHours](#c68) — 작성 3, 자동 0
- [conversation.domain.ConversationServiceHours.ServiceWindow](#c69) — 작성 0, 자동 6
- [conversation.dto.ActiveConversationExistsError](#c70) — 작성 1, 자동 5
- [conversation.dto.ActiveConversationExistsError.ErrorDetail](#c71) — 작성 0, 자동 9
- [conversation.dto.StartConversationRequest](#c72) — 작성 4, 자동 4
- [conversation.dto.StartConversationResult](#c73) — 작성 1, 자동 8
- [conversation.exception.ConversationException](#c74) — 작성 11, 자동 0
- [conversation.infrastructure.ConversationClosureProperties](#c75) — 작성 1, 자동 4
- [conversation.infrastructure.ConversationClosureRepository](#c76) — 작성 12, 자동 0
- [conversation.infrastructure.ConversationClosureRepository.ClosureState](#c77) — 작성 0, 자동 11
- [conversation.infrastructure.ConversationRepository](#c78) — 작성 7, 자동 0
- [conversation.infrastructure.ConversationRepository.StoredConversation](#c79) — 작성 1, 자동 9
- [global.auth.CsrfAccessDeniedHandler](#c80) — 작성 2, 자동 0
- [global.auth.CsrfController](#c81) — 작성 1, 자동 1
- [global.auth.CsrfController.CsrfTokenResponse](#c82) — 작성 0, 자동 7
- [global.config.AppSecurityProperties](#c83) — 작성 1, 자동 5
- [global.config.OAuth2ClientConfig](#c84) — 작성 3, 자동 1
- [global.config.OAuth2ClientProperties](#c85) — 작성 0, 자동 6
- [global.config.OAuth2ClientProperties.Credentials](#c86) — 작성 0, 자동 6
- [global.config.SecurityConfig](#c87) — 작성 4, 자동 1
- [global.config.SecurityConfig$anonymous@120](#c88) — 작성 1, 자동 1
- [global.config.SessionConfig](#c89) — 작성 3, 자동 1
- [global.error.ApiErrorResponse](#c90) — 작성 0, 자동 5
- [global.error.ApiErrorResponse.ErrorDetail](#c91) — 작성 0, 자동 8
- [global.error.ApiErrorResponse.FieldError](#c92) — 작성 0, 자동 6
- [global.error.ApiErrorResponses](#c93) — 작성 3, 자동 0
- [global.error.CommonApiExceptionHandler](#c94) — 작성 2, 자동 0
- [global.web.ApiResponseHeadersFilter](#c95) — 작성 1, 자동 1
- [global.web.ClientAddressResolver](#c96) — 작성 2, 자동 0
- [global.web.GlobalApiRateLimitFilter](#c97) — 작성 3, 자동 0
- [global.web.Part1JsonRequestGuardFilter](#c98) — 작성 4, 자동 0
- [global.web.Part1JsonRequestGuardFilter.CachedBodyRequest](#c99) — 작성 6, 자동 0
- [global.web.Part1JsonRequestGuardFilter.CachedBodyRequest$anonymous@97](#c100) — 작성 5, 자동 1
- [global.web.Part2JsonRequestGuardFilter](#c101) — 작성 4, 자동 0
- [guardian.api.GuardianAccessExceptionHandler](#c102) — 작성 1, 자동 1
- [guardian.application.GuardianAccessService](#c103) — 작성 5, 자동 0
- [guardian.exception.GuardianAccessException](#c104) — 작성 6, 자동 0
- [guardian.infrastructure.GuardianAccessRepository](#c105) — 작성 2, 자동 0
- [guardian.infrastructure.GuardianAccessRepository.GuardianAccessState](#c106) — 작성 2, 자동 12

<a id="c1"></a>
## DodamDodamBackendApplication

정의: [src/main/java/com/dodamdodam/backend/DodamDodamBackendApplication.java:6](../../../src/main/java/com/dodamdodam/backend/DodamDodamBackendApplication.java#L6) · CLASS

설정: `@org.springframework.boot.autoconfigure.SpringBootApplication`

<a id="m1"></a>
### main

```java
public static void main(java.lang.String[] args)
```

정의: [src/main/java/com/dodamdodam/backend/DodamDodamBackendApplication.java:9](../../../src/main/java/com/dodamdodam/backend/DodamDodamBackendApplication.java#L9)

호출자: 정적 Java 호출 없음

프레임워크 진입: JVM 애플리케이션 시작

내부 호출: 없음

외부 라이브러리 호출: `org.springframework.boot.SpringApplication.run(java.lang.Class<?>,java.lang.String[])`

<a id="m2"></a>
### DodamDodamBackendApplication 생성자 — 자동 생성

```java
public void <init>()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

외부 라이브러리 호출: `java.lang.Object.<init>()`

<a id="c2"></a>
## auth.api.AuthLogoutController

정의: [src/main/java/com/dodamdodam/backend/auth/api/AuthLogoutController.java:24](../../../src/main/java/com/dodamdodam/backend/auth/api/AuthLogoutController.java#L24) · CLASS

설정: `@org.springframework.web.bind.annotation.RestController`

<a id="m3"></a>
### AuthLogoutController 생성자

```java
public void <init>(com.dodamdodam.backend.auth.session.AuthenticatedSessionService authenticatedSessions, com.dodamdodam.backend.auth.session.SessionSecurityRepository securityRepository, org.springframework.security.web.context.SecurityContextRepository securityContextRepository, org.springframework.security.web.csrf.CookieCsrfTokenRepository csrfRepository)
```

정의: [src/main/java/com/dodamdodam/backend/auth/api/AuthLogoutController.java:33](../../../src/main/java/com/dodamdodam/backend/auth/api/AuthLogoutController.java#L33)

기존 세션·CSRF 정리 기능으로 로그아웃 API를 구성한다.

호출자: 정적 Java 호출 없음

내부 호출: 없음

외부 라이브러리 호출: `java.lang.Object.<init>()` / `org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler.setSecurityContextRepository(org.springframework.security.web.context.SecurityContextRepository)` / `org.springframework.security.web.csrf.CsrfLogoutHandler.<init>(org.springframework.security.web.csrf.CsrfTokenRepository)`

<a id="m4"></a>
### logout

```java
public org.springframework.http.ResponseEntity<?> logout(jakarta.servlet.http.HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response)
```

정의: [src/main/java/com/dodamdodam/backend/auth/api/AuthLogoutController.java:42](../../../src/main/java/com/dodamdodam/backend/auth/api/AuthLogoutController.java#L42)

인증과 빈 본문을 확인한 뒤 현재 문맥·세션·CSRF 쿠키를 폐기한다.

애너테이션: `@org.springframework.web.bind.annotation.PostMapping({"/api/v1/auth/logout"})`

호출자: 정적 Java 호출 없음

프레임워크 진입: POST /api/v1/auth/logout

내부 호출: [auth.session.AuthStateUnavailableException.AuthStateUnavailableException](#m164) / [auth.session.AuthenticatedSession.accountId](#m166) / [auth.session.AuthenticatedSession.securityContextId](#m169) / [auth.session.AuthenticatedSessionService.requireAuthenticatedSession](#m172) / [auth.session.SessionSecurityRepository.revokeCurrentContext](#m207) / [global.error.ApiErrorResponses.body](#m507)

외부 라이브러리 호출: `jakarta.servlet.ServletRequest.getAttribute(java.lang.String)` / `java.lang.Boolean.equals(java.lang.Object)` / `java.util.List.of()` / `org.springframework.http.ResponseEntity.badRequest()` / `org.springframework.http.ResponseEntity.noContent()` / `org.springframework.http.ResponseEntity.BodyBuilder.body(@org.jspecify.annotations.Nullable T)` / `org.springframework.http.ResponseEntity.HeadersBuilder.build()` / `org.springframework.security.authentication.InsufficientAuthenticationException.<init>(java.lang.String)` / `org.springframework.security.core.context.SecurityContext.getAuthentication()` / `org.springframework.security.core.context.SecurityContextHolder.getContext()` / `org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler.logout(jakarta.servlet.http.HttpServletRequest,jakarta.servlet.http.HttpServletResponse,org.springframework.security.core.@org.jspecify.annotations.Nullable Authentication)` / `org.springframework.security.web.csrf.CsrfLogoutHandler.logout(jakarta.servlet.http.HttpServletRequest,jakarta.servlet.http.HttpServletResponse,org.springframework.security.core.@org.jspecify.annotations.Nullable Authentication)`

<a id="c3"></a>
## auth.api.EmailVerificationController

정의: [src/main/java/com/dodamdodam/backend/auth/api/EmailVerificationController.java:18](../../../src/main/java/com/dodamdodam/backend/auth/api/EmailVerificationController.java#L18) · CLASS

설정: `@org.springframework.web.bind.annotation.RestController`

<a id="m5"></a>
### EmailVerificationController 생성자

```java
public void <init>(com.dodamdodam.backend.auth.session.AuthenticatedSessionService authentication, com.dodamdodam.backend.auth.application.EmailVerificationIssuanceService issuance, tools.jackson.databind.json.JsonMapper mapper)
```

정의: [src/main/java/com/dodamdodam/backend/auth/api/EmailVerificationController.java:25](../../../src/main/java/com/dodamdodam/backend/auth/api/EmailVerificationController.java#L25)

조건부 로그인과 기존 입력·발급 업무를 연결한다.

호출자: 정적 Java 호출 없음

내부 호출: 없음

외부 라이브러리 호출: `java.lang.Object.<init>()` / `tools.jackson.databind.cfg.MapperBuilder.enable(tools.jackson.core.StreamReadFeature[])` / `tools.jackson.databind.cfg.MapperBuilder.enable(tools.jackson.databind.DeserializationFeature[])` / `tools.jackson.databind.json.JsonMapper.rebuild()` / `tools.jackson.databind.json.JsonMapper.Builder.build()`

<a id="m6"></a>
### issue

```java
public java.util.Map<java.lang.String,com.dodamdodam.backend.auth.dto.VerificationIssueResponse> issue(jakarta.servlet.http.HttpServletRequest request)
```

정의: [src/main/java/com/dodamdodam/backend/auth/api/EmailVerificationController.java:34](../../../src/main/java/com/dodamdodam/backend/auth/api/EmailVerificationController.java#L34)

PIN 목적은 구조·필드 검사 전에 현재 로그인을 요구하며 허용한 응답 두 필드만 반환한다.

애너테이션: `@org.springframework.web.bind.annotation.PostMapping({"/api/v1/auth/email-verifications"})`

호출자: 정적 Java 호출 없음

프레임워크 진입: POST /api/v1/auth/email-verifications

내부 호출: [auth.application.EmailVerificationIssuanceService.issue](#m19) / [auth.dto.IssueEmailVerificationRequest.fromJson](#m40) / [auth.exception.EmailVerificationException.validation](#m64) / [auth.session.AuthenticatedSessionService.requireAuthenticatedSession](#m172) / [global.web.ClientAddressResolver.resolve](#m514)

외부 라이브러리 호출: `jakarta.servlet.ServletRequest.getReader()` / `java.lang.String.equals(java.lang.Object)` / `java.util.Map.of(K,V)` / `tools.jackson.databind.JsonNode.asText()` / `tools.jackson.databind.JsonNode.get(java.lang.String)` / `tools.jackson.databind.JsonNode.isObject()` / `tools.jackson.databind.JsonNode.isString()` / `tools.jackson.databind.ObjectMapper.readTree(java.io.Reader)`

<a id="c4"></a>
## auth.api.EmailVerificationExceptionHandler

정의: [src/main/java/com/dodamdodam/backend/auth/api/EmailVerificationExceptionHandler.java:11](../../../src/main/java/com/dodamdodam/backend/auth/api/EmailVerificationExceptionHandler.java#L11) · CLASS

설정: `@org.springframework.core.annotation.Order(-2147483648)` · `@org.springframework.web.bind.annotation.RestControllerAdvice(assignableTypes={com.dodamdodam.backend.auth.api.EmailVerificationController.class})`

<a id="m7"></a>
### handle

```java
public org.springframework.http.ResponseEntity<com.dodamdodam.backend.global.error.ApiErrorResponse> handle(com.dodamdodam.backend.auth.exception.EmailVerificationException failure, jakarta.servlet.http.HttpServletResponse response)
```

정의: [src/main/java/com/dodamdodam/backend/auth/api/EmailVerificationExceptionHandler.java:15](../../../src/main/java/com/dodamdodam/backend/auth/api/EmailVerificationExceptionHandler.java#L15)

이메일 발급 업무 오류를 고정 코드·필드 목록으로만 반환한다.

애너테이션: `@org.springframework.web.bind.annotation.ExceptionHandler({com.dodamdodam.backend.auth.exception.EmailVerificationException.class})`

호출자: 정적 Java 호출 없음

프레임워크 진입: Spring MVC 예외 처리

내부 호출: [auth.exception.EmailVerificationException.code](#m65) / [auth.exception.EmailVerificationException.fields](#m72) / [auth.exception.EmailVerificationException.status](#m67) / [global.error.ApiErrorResponses.body](#m507)

외부 라이브러리 호출: `java.lang.Throwable.getMessage()` / `org.springframework.http.ResponseEntity.status(int)` / `org.springframework.http.ResponseEntity.BodyBuilder.body(@org.jspecify.annotations.Nullable T)`

<a id="m8"></a>
### limited

```java
public org.springframework.http.ResponseEntity<com.dodamdodam.backend.global.error.ApiErrorResponse> limited(com.dodamdodam.backend.auth.exception.AuthRateLimitException failure, jakarta.servlet.http.HttpServletResponse response)
```

정의: [src/main/java/com/dodamdodam/backend/auth/api/EmailVerificationExceptionHandler.java:22](../../../src/main/java/com/dodamdodam/backend/auth/api/EmailVerificationExceptionHandler.java#L22)

적용 제한 중 가장 늦은 재시도까지 계산된 초를 헤더에도 표시한다.

애너테이션: `@org.springframework.web.bind.annotation.ExceptionHandler({com.dodamdodam.backend.auth.exception.AuthRateLimitException.class})`

호출자: 정적 Java 호출 없음

프레임워크 진입: Spring MVC 예외 처리

내부 호출: [auth.exception.AuthRateLimitException.code](#m60) / [auth.exception.AuthRateLimitException.retryAfterSeconds](#m61) / [global.error.ApiErrorResponses.body](#m507)

외부 라이브러리 호출: `jakarta.servlet.http.HttpServletResponse.setHeader(java.lang.String,java.lang.String)` / `java.lang.Long.toString(long)` / `java.lang.Throwable.getMessage()` / `java.util.List.of()` / `org.springframework.http.ResponseEntity.status(int)` / `org.springframework.http.ResponseEntity.BodyBuilder.body(@org.jspecify.annotations.Nullable T)`

<a id="m9"></a>
### EmailVerificationExceptionHandler 생성자 — 자동 생성

```java
public void <init>()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

외부 라이브러리 호출: `java.lang.Object.<init>()`

<a id="c5"></a>
## auth.application.AuthRateLimitService

정의: [src/main/java/com/dodamdodam/backend/auth/application/AuthRateLimitService.java:16](../../../src/main/java/com/dodamdodam/backend/auth/application/AuthRateLimitService.java#L16) · CLASS

설정: `@org.springframework.stereotype.Service`

<a id="m10"></a>
### AuthRateLimitService 생성자

```java
public void <init>(com.dodamdodam.backend.auth.infrastructure.AuthRateLimitRepository repository, org.springframework.transaction.PlatformTransactionManager transactionManager)
```

정의: [src/main/java/com/dodamdodam/backend/auth/application/AuthRateLimitService.java:23](../../../src/main/java/com/dodamdodam/backend/auth/application/AuthRateLimitService.java#L23)

이후 업무 rollback과 독립적으로 예산을 커밋하는 짧은 예약 Tx를 구성한다.

호출자: 정적 Java 호출 없음

내부 호출: 없음

외부 라이브러리 호출: `java.lang.Object.<init>()` / `org.springframework.transaction.support.DefaultTransactionDefinition.setPropagationBehavior(int)` / `org.springframework.transaction.support.TransactionTemplate.<init>(org.springframework.transaction.PlatformTransactionManager)`

<a id="m11"></a>
### reserve

```java
public void reserve(java.util.List<com.dodamdodam.backend.auth.domain.AuthRateLimitKey> keys)
```

정의: [src/main/java/com/dodamdodam/backend/auth/application/AuthRateLimitService.java:30](../../../src/main/java/com/dodamdodam/backend/auth/application/AuthRateLimitService.java#L30)

업무 row lock 전에 호출하며 API_IP 단독 예약 뒤 같은 단계의 업무 키들을 함께 예약한다.

호출자: [auth.application.EmailVerificationIssuanceService.issue](#m19) / [global.web.GlobalApiRateLimitFilter.doFilterInternal](#m517)

내부 호출: [auth.exception.AuthRateLimitException.AuthRateLimitException](#m59) / [auth.infrastructure.AuthRateLimitRepository.reserve](#m80) / [auth.session.AuthStateUnavailableException.AuthStateUnavailableException](#m163) / [auth.session.AuthStateUnavailableException.AuthStateUnavailableException](#m164)

외부 라이브러리 호출: `org.springframework.transaction.support.TransactionTemplate.execute(org.springframework.transaction.support.TransactionCallback<T>)`

<a id="c6"></a>
## auth.application.EmailVerificationCodeGenerator

정의: [src/main/java/com/dodamdodam/backend/auth/application/EmailVerificationCodeGenerator.java:6](../../../src/main/java/com/dodamdodam/backend/auth/application/EmailVerificationCodeGenerator.java#L6) · CLASS

<a id="m12"></a>
### EmailVerificationCodeGenerator 생성자

```java
public void <init>()
```

정의: [src/main/java/com/dodamdodam/backend/auth/application/EmailVerificationCodeGenerator.java:11](../../../src/main/java/com/dodamdodam/backend/auth/application/EmailVerificationCodeGenerator.java#L11)

운영 생성에는 암호학적으로 안전한 난수만 사용한다.

호출자: `auth.application.EmailVerificationIssuanceService.<initializer>`

내부 호출: [auth.application.EmailVerificationCodeGenerator.EmailVerificationCodeGenerator](#m13)

외부 라이브러리 호출: `java.security.SecureRandom.<init>()`

<a id="m13"></a>
### EmailVerificationCodeGenerator 생성자

```java
void <init>(java.security.SecureRandom random)
```

정의: [src/main/java/com/dodamdodam/backend/auth/application/EmailVerificationCodeGenerator.java:16](../../../src/main/java/com/dodamdodam/backend/auth/application/EmailVerificationCodeGenerator.java#L16)

같은 패키지 테스트에서 난수 경계를 재현한다.

호출자: [auth.application.EmailVerificationCodeGenerator.EmailVerificationCodeGenerator](#m12)

내부 호출: 없음

외부 라이브러리 호출: `java.lang.Object.<init>()`

<a id="m14"></a>
### generate

```java
public java.lang.String generate()
```

정의: [src/main/java/com/dodamdodam/backend/auth/application/EmailVerificationCodeGenerator.java:21](../../../src/main/java/com/dodamdodam/backend/auth/application/EmailVerificationCodeGenerator.java#L21)

선행 0과 ASCII 형식을 보존한 정확히 6자리 인증번호를 생성한다.

호출자: [auth.application.EmailVerificationIssuanceService.issue](#m19)

내부 호출: 없음

외부 라이브러리 호출: `java.lang.String.format(java.util.Locale,java.lang.String,java.lang.Object[])` / `java.util.Random.nextInt(int)`

<a id="c7"></a>
## auth.application.EmailVerificationDeliveryService

정의: [src/main/java/com/dodamdodam/backend/auth/application/EmailVerificationDeliveryService.java:14](../../../src/main/java/com/dodamdodam/backend/auth/application/EmailVerificationDeliveryService.java#L14) · CLASS

설정: `@org.springframework.stereotype.Service`

<a id="m15"></a>
### EmailVerificationDeliveryService 생성자

```java
public void <init>(com.dodamdodam.backend.auth.infrastructure.SmtpEmailVerificationSender sender, com.dodamdodam.backend.auth.infrastructure.EmailVerificationRepository repository, org.springframework.transaction.PlatformTransactionManager manager)
```

정의: [src/main/java/com/dodamdodam/backend/auth/application/EmailVerificationDeliveryService.java:21](../../../src/main/java/com/dodamdodam/backend/auth/application/EmailVerificationDeliveryService.java#L21)

발송 실패 시 이미 커밋한 그 발급만 폐기하는 별도 Tx를 구성한다.

호출자: 정적 Java 호출 없음

내부 호출: 없음

외부 라이브러리 호출: `java.lang.Object.<init>()` / `org.springframework.transaction.support.DefaultTransactionDefinition.setPropagationBehavior(int)` / `org.springframework.transaction.support.TransactionTemplate.<init>(org.springframework.transaction.PlatformTransactionManager)`

<a id="m16"></a>
### deliver

```java
public void deliver(java.util.UUID challengeId, java.lang.String email, java.lang.String code, boolean eligible)
```

정의: [src/main/java/com/dodamdodam/backend/auth/application/EmailVerificationDeliveryService.java:30](../../../src/main/java/com/dodamdodam/backend/auth/application/EmailVerificationDeliveryService.java#L30)

업무 Tx 종료 뒤 발송하고 거부·timeout·결과 불명확은 해당 id만 보상 폐기한다.

호출자: [auth.application.EmailVerificationIssuanceService.issue](#m19)

내부 호출: [auth.application.EmailVerificationDeliveryService.invalidate](#m17) / [auth.exception.EmailVerificationException.deliveryUnavailable](#m66) / [auth.infrastructure.SmtpEmailVerificationSender.send](#m150) / [auth.session.AuthStateUnavailableException.AuthStateUnavailableException](#m163)

외부 라이브러리 호출: `org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()`

<a id="m17"></a>
### invalidate

```java
public void invalidate(java.util.UUID challengeId)
```

정의: [src/main/java/com/dodamdodam/backend/auth/application/EmailVerificationDeliveryService.java:45](../../../src/main/java/com/dodamdodam/backend/auth/application/EmailVerificationDeliveryService.java#L45)

늦은 발송 실패나 권한 변경에서도 새 발급·소비 완료 증명을 건드리지 않는다.

호출자: [auth.application.EmailVerificationDeliveryService.deliver](#m16) / [auth.application.EmailVerificationIssuanceService.verifyResponseAccess](#m22)

내부 호출: [auth.infrastructure.EmailVerificationRepository.invalidateUnconsumed](#m123) / [auth.session.AuthStateUnavailableException.AuthStateUnavailableException](#m164)

외부 라이브러리 호출: `org.springframework.transaction.support.TransactionOperations.executeWithoutResult(java.util.function.Consumer<org.springframework.transaction.TransactionStatus>)`

<a id="c8"></a>
## auth.application.EmailVerificationIssuanceService

정의: [src/main/java/com/dodamdodam/backend/auth/application/EmailVerificationIssuanceService.java:22](../../../src/main/java/com/dodamdodam/backend/auth/application/EmailVerificationIssuanceService.java#L22) · CLASS

설정: `@org.springframework.stereotype.Service`

<a id="m18"></a>
### EmailVerificationIssuanceService 생성자

```java
public void <init>(com.dodamdodam.backend.auth.infrastructure.EmailVerificationRepository repository, com.dodamdodam.backend.auth.infrastructure.EmailVerificationAccountRepository accounts, com.dodamdodam.backend.auth.infrastructure.EmailVerificationKeys keys, com.dodamdodam.backend.auth.application.AuthRateLimitService limits, com.dodamdodam.backend.auth.application.EmailVerificationDeliveryService delivery, jakarta.validation.Validator validator, org.springframework.transaction.PlatformTransactionManager manager, com.dodamdodam.backend.auth.session.SessionSecurityRepository sessions)
```

정의: [src/main/java/com/dodamdodam/backend/auth/application/EmailVerificationIssuanceService.java:36](../../../src/main/java/com/dodamdodam/backend/auth/application/EmailVerificationIssuanceService.java#L36)

예산 예약·업무 저장·외부 발송을 서로 다른 경계로 연결한다.

호출자: 정적 Java 호출 없음

내부 호출: 없음

외부 라이브러리 호출: `java.lang.Object.<init>()` / `org.springframework.transaction.support.DefaultTransactionDefinition.setPropagationBehavior(int)` / `org.springframework.transaction.support.TransactionTemplate.<init>(org.springframework.transaction.PlatformTransactionManager)`

<a id="m19"></a>
### issue

```java
public com.dodamdodam.backend.auth.dto.VerificationIssueResponse issue(jakarta.servlet.http.HttpServletRequest request, com.dodamdodam.backend.auth.dto.IssueEmailVerificationRequest input, com.dodamdodam.backend.auth.session.AuthenticatedSession current, java.net.InetAddress address)
```

정의: [src/main/java/com/dodamdodam/backend/auth/application/EmailVerificationIssuanceService.java:51](../../../src/main/java/com/dodamdodam/backend/auth/application/EmailVerificationIssuanceService.java#L51)

구조 검증 뒤 예산을 먼저 커밋하고, challenge 커밋이 끝난 다음에만 메일을 발송한다.

호출자: [auth.api.EmailVerificationController.issue](#m6)

내부 호출: [auth.application.AuthRateLimitService.reserve](#m11) / [auth.application.EmailVerificationCodeGenerator.generate](#m14) / [auth.application.EmailVerificationDeliveryService.deliver](#m16) / [auth.application.EmailVerificationIssuanceService.preparePin](#m21) / [auth.application.EmailVerificationIssuanceService.preparePublic](#m20) / [auth.application.EmailVerificationIssuanceService.verifyResponseAccess](#m22) / [auth.application.EmailVerificationIssuanceService.Prepared.data](#m24) / [auth.application.EmailVerificationIssuanceService.Prepared.issued](#m27) / [auth.dto.IssueEmailVerificationRequest.email](#m45) / [auth.dto.IssueEmailVerificationRequest.normalized](#m41) / [auth.dto.IssueEmailVerificationRequest.purpose](#m48) / [auth.dto.VerificationIssueResponse.VerificationIssueResponse](#m53) / [auth.infrastructure.AuthRateLimitKeyHasher.emailIssueKeys](#m75) / [auth.infrastructure.EmailVerificationCodeHasher.hash](#m116) / [auth.infrastructure.EmailVerificationKeys.codeHasher](#m119) / [auth.infrastructure.EmailVerificationKeys.rateHasher](#m118) / [auth.infrastructure.EmailVerificationRepository.IssuedChallenge.challengeId](#m129) / [auth.infrastructure.EmailVerificationRepository.IssuedChallenge.expiresAt](#m131) / [auth.infrastructure.EmailVerificationRepository.NewChallenge.eligible](#m136) / [auth.infrastructure.EmailVerificationRepository.NewChallenge.email](#m137) / [auth.session.AuthStateUnavailableException.AuthStateUnavailableException](#m163) / [auth.session.AuthStateUnavailableException.AuthStateUnavailableException](#m164) / [auth.session.AuthenticatedSession.accountId](#m166) / [auth.session.AuthenticatedSession.securityContextId](#m169)

외부 라이브러리 호출: `jakarta.servlet.ServletRequest.setAttribute(java.lang.String,java.lang.Object)` / `java.time.OffsetDateTime.withOffsetSameInstant(java.time.ZoneOffset)` / `java.time.ZoneOffset.ofHours(int)` / `org.springframework.security.authentication.InsufficientAuthenticationException.<init>(java.lang.String)` / `org.springframework.transaction.support.TransactionTemplate.execute(org.springframework.transaction.support.TransactionCallback<T>)`

<a id="m20"></a>
### preparePublic

```java
private com.dodamdodam.backend.auth.application.EmailVerificationIssuanceService.Prepared preparePublic(com.dodamdodam.backend.auth.dto.IssueEmailVerificationRequest input, byte[] hash)
```

정의: [src/main/java/com/dodamdodam/backend/auth/application/EmailVerificationIssuanceService.java:80](../../../src/main/java/com/dodamdodam/backend/auth/application/EmailVerificationIssuanceService.java#L80)

같은 이메일·목적을 잠근 뒤 현재 계정으로 normal/decoy와 reset 버전을 판정한다.

호출자: [auth.application.EmailVerificationIssuanceService.issue](#m19)

내부 호출: [auth.application.EmailVerificationIssuanceService.Prepared.Prepared](#m23) / [auth.dto.IssueEmailVerificationRequest.email](#m45) / [auth.dto.IssueEmailVerificationRequest.purpose](#m48) / [auth.infrastructure.EmailVerificationAccountRepository.findForUpdate](#m92) / [auth.infrastructure.EmailVerificationAccountRepository.AccountState.id](#m100) / [auth.infrastructure.EmailVerificationAccountRepository.AccountState.sessionVersion](#m101) / [auth.infrastructure.EmailVerificationRepository.lockScope](#m124) / [auth.infrastructure.EmailVerificationRepository.replace](#m122) / [auth.infrastructure.EmailVerificationRepository.requireResendReady](#m125) / [auth.infrastructure.EmailVerificationRepository.NewChallenge.NewChallenge](#m134)

외부 라이브러리 호출: `java.util.Optional.isEmpty()` / `java.util.Optional.isPresent()` / `java.util.Optional.orElseThrow()` / `java.util.UUID.randomUUID()`

<a id="m21"></a>
### preparePin

```java
private com.dodamdodam.backend.auth.application.EmailVerificationIssuanceService.Prepared preparePin(com.dodamdodam.backend.auth.dto.IssueEmailVerificationRequest input, com.dodamdodam.backend.auth.session.AuthenticatedSession current, byte[] hash)
```

정의: [src/main/java/com/dodamdodam/backend/auth/application/EmailVerificationIssuanceService.java:92](../../../src/main/java/com/dodamdodam/backend/auth/application/EmailVerificationIssuanceService.java#L92)

계정→문맥→PIN→challenge 순서로 잠그고 현재 이메일·generation·PIN 버전을 저장한다.

호출자: [auth.application.EmailVerificationIssuanceService.issue](#m19)

내부 호출: [auth.application.EmailVerificationIssuanceService.Prepared.Prepared](#m23) / [auth.dto.IssueEmailVerificationRequest.IssueEmailVerificationRequest](#m44) / [auth.dto.IssueEmailVerificationRequest.email](#m45) / [auth.dto.IssueEmailVerificationRequest.normalized](#m41) / [auth.dto.IssueEmailVerificationRequest.purpose](#m48) / [auth.exception.EmailVerificationException.authenticationRequired](#m68) / [auth.exception.EmailVerificationException.pinAlreadySet](#m70) / [auth.exception.EmailVerificationException.pinNotSet](#m71) / [auth.exception.EmailVerificationException.validation](#m64) / [auth.infrastructure.EmailVerificationAccountRepository.findPinState](#m94) / [auth.infrastructure.EmailVerificationAccountRepository.lockPin](#m93) / [auth.infrastructure.EmailVerificationAccountRepository.PinState.email](#m107) / [auth.infrastructure.EmailVerificationAccountRepository.PinState.isSessionCurrent](#m95) / [auth.infrastructure.EmailVerificationAccountRepository.PinState.pinVersion](#m111) / [auth.infrastructure.EmailVerificationAccountRepository.PinState.setupGeneration](#m113) / [auth.infrastructure.EmailVerificationRepository.lockScope](#m124) / [auth.infrastructure.EmailVerificationRepository.replace](#m122) / [auth.infrastructure.EmailVerificationRepository.requireResendReady](#m125) / [auth.infrastructure.EmailVerificationRepository.NewChallenge.NewChallenge](#m134) / [auth.session.AuthenticatedSession.accountId](#m166) / [auth.session.AuthenticatedSession.securityContextId](#m169) / [auth.session.SessionSecurityRepository.requireCurrentContextForUpdate](#m206)

외부 라이브러리 호출: `java.lang.String.equals(java.lang.Object)` / `java.util.Optional.orElseThrow(java.util.function.Supplier<? extends X>)` / `java.util.UUID.randomUUID()`

<a id="m22"></a>
### verifyResponseAccess

```java
public void verifyResponseAccess(jakarta.servlet.http.HttpServletRequest request)
```

정의: [src/main/java/com/dodamdodam/backend/auth/application/EmailVerificationIssuanceService.java:119](../../../src/main/java/com/dodamdodam/backend/auth/application/EmailVerificationIssuanceService.java#L119)

전송 직전 현재 DB를 읽고 늦은 폐기·계정/문맥/PIN 변경이면 이 발급만 폐기한다.

호출자: [auth.application.EmailVerificationIssuanceService.issue](#m19) / [auth.session.AuthStateErrorFilter.verifyResponseAccess](#m156)

내부 호출: [auth.application.EmailVerificationDeliveryService.invalidate](#m17) / [auth.application.EmailVerificationIssuanceService.Prepared.data](#m24) / [auth.exception.EmailVerificationException.authenticationRequired](#m68) / [auth.exception.EmailVerificationException.authorizationRequired](#m69) / [auth.exception.EmailVerificationException.deliveryUnavailable](#m66) / [auth.infrastructure.EmailVerificationAccountRepository.findPinState](#m94) / [auth.infrastructure.EmailVerificationAccountRepository.PinState.email](#m107) / [auth.infrastructure.EmailVerificationAccountRepository.PinState.isSessionCurrent](#m95) / [auth.infrastructure.EmailVerificationAccountRepository.PinState.pinVersion](#m111) / [auth.infrastructure.EmailVerificationAccountRepository.PinState.setupGeneration](#m113) / [auth.infrastructure.EmailVerificationRepository.isIssueResponseCurrent](#m126) / [auth.infrastructure.EmailVerificationRepository.NewChallenge.email](#m137) / [auth.infrastructure.EmailVerificationRepository.NewChallenge.id](#m140) / [auth.infrastructure.EmailVerificationRepository.NewChallenge.pinVersionSnapshot](#m141) / [auth.infrastructure.EmailVerificationRepository.NewChallenge.purpose](#m142) / [auth.infrastructure.EmailVerificationRepository.NewChallenge.setupAccountId](#m145) / [auth.infrastructure.EmailVerificationRepository.NewChallenge.setupGeneration](#m146) / [auth.infrastructure.EmailVerificationRepository.NewChallenge.setupSessionBinding](#m147) / [auth.session.AuthStateUnavailableException.AuthStateUnavailableException](#m164)

외부 라이브러리 호출: `jakarta.servlet.ServletRequest.getAttribute(java.lang.String)` / `java.lang.String.equals(java.lang.Object)` / `java.util.Objects.equals(java.lang.Object,java.lang.Object)` / `java.util.Optional.get()` / `java.util.Optional.isEmpty()`

<a id="c9"></a>
## auth.application.EmailVerificationIssuanceService.Prepared

정의: [src/main/java/com/dodamdodam/backend/auth/application/EmailVerificationIssuanceService.java:150](../../../src/main/java/com/dodamdodam/backend/auth/application/EmailVerificationIssuanceService.java#L150) · RECORD

내부 적격 값은 HTTP 응답에 포함하지 않는다.

<a id="m23"></a>
### Prepared 생성자 — 자동 생성

```java
private void <init>(com.dodamdodam.backend.auth.infrastructure.EmailVerificationRepository.NewChallenge data, com.dodamdodam.backend.auth.infrastructure.EmailVerificationRepository.IssuedChallenge issued)
```

호출자: [auth.application.EmailVerificationIssuanceService.preparePin](#m21) / [auth.application.EmailVerificationIssuanceService.preparePublic](#m20)

내부 호출: 없음

외부 라이브러리 호출: `java.lang.Record.<init>()`

<a id="m24"></a>
### data — 자동 생성

```java
public com.dodamdodam.backend.auth.infrastructure.EmailVerificationRepository.NewChallenge data()
```

호출자: [auth.application.EmailVerificationIssuanceService.issue](#m19) / [auth.application.EmailVerificationIssuanceService.verifyResponseAccess](#m22)

내부 호출: 없음

<a id="m25"></a>
### equals — 자동 생성

```java
public final boolean equals(java.lang.Object o)
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m26"></a>
### hashCode — 자동 생성

```java
public final int hashCode()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m27"></a>
### issued — 자동 생성

```java
public com.dodamdodam.backend.auth.infrastructure.EmailVerificationRepository.IssuedChallenge issued()
```

호출자: [auth.application.EmailVerificationIssuanceService.issue](#m19)

내부 호출: 없음

<a id="m28"></a>
### toString — 자동 생성

```java
public final java.lang.String toString()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="c10"></a>
## auth.domain.AuthRateLimitKey

정의: [src/main/java/com/dodamdodam/backend/auth/domain/AuthRateLimitKey.java:4](../../../src/main/java/com/dodamdodam/backend/auth/domain/AuthRateLimitKey.java#L4) · RECORD

원문 식별자 대신 목적과 32바이트 HMAC으로 예약 대상을 구분한다.

<a id="m29"></a>
### AuthRateLimitKey 생성자

```java
public void <init>(com.dodamdodam.backend.auth.domain.AuthRateLimitKey.Action action, byte[] keyHash)
```

정의: [src/main/java/com/dodamdodam/backend/auth/domain/AuthRateLimitKey.java:7](../../../src/main/java/com/dodamdodam/backend/auth/domain/AuthRateLimitKey.java#L7)

DB에 전달할 해시를 검증·복사해 호출자의 배열 변경으로 범위가 바뀌지 않게 한다.

호출자: [auth.infrastructure.AuthRateLimitKeyHasher.hash](#m77)

내부 호출: 없음

외부 라이브러리 호출: `Array.clone()` / `java.lang.IllegalArgumentException.<init>(java.lang.String)` / `java.lang.Record.<init>()`

<a id="m30"></a>
### keyHash

```java
public byte[] keyHash()
```

정의: [src/main/java/com/dodamdodam/backend/auth/domain/AuthRateLimitKey.java:15](../../../src/main/java/com/dodamdodam/backend/auth/domain/AuthRateLimitKey.java#L15)

해시 원본을 노출하지 않아 같은 예약의 식별값을 유지한다.

애너테이션: `@java.lang.Override`

호출자: [auth.infrastructure.AuthRateLimitRepository.reserve](#m80) / [auth.infrastructure.AuthRateLimitRepository.reserveAllowed](#m81)

내부 호출: 없음

외부 라이브러리 호출: `Array.clone()`

<a id="m34"></a>
### action — 자동 생성

```java
public com.dodamdodam.backend.auth.domain.AuthRateLimitKey.Action action()
```

호출자: [auth.infrastructure.AuthRateLimitRepository.reserve](#m80) / [auth.infrastructure.AuthRateLimitRepository.reserveAllowed](#m81)

내부 호출: 없음

<a id="m35"></a>
### equals — 자동 생성

```java
public final boolean equals(java.lang.Object o)
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m36"></a>
### hashCode — 자동 생성

```java
public final int hashCode()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m37"></a>
### toString — 자동 생성

```java
public final java.lang.String toString()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="c11"></a>
## auth.domain.AuthRateLimitKey.Action

정의: [src/main/java/com/dodamdodam/backend/auth/domain/AuthRateLimitKey.java:21](../../../src/main/java/com/dodamdodam/backend/auth/domain/AuthRateLimitKey.java#L21) · ENUM

기존 기술 기준의 고정창 요청량이며 PIN 실제 오답 차단과 구분한다.

<a id="m31"></a>
### Action 생성자

```java
private void <init>(long capacity, long windowSeconds)
```

정의: [src/main/java/com/dodamdodam/backend/auth/domain/AuthRateLimitKey.java:31](../../../src/main/java/com/dodamdodam/backend/auth/domain/AuthRateLimitKey.java#L31)

창 안에서 허용하는 예약 수와 창 길이를 보관한다.

호출자: `auth.domain.AuthRateLimitKey.Action.<initializer>`

내부 호출: 없음

외부 라이브러리 호출: `java.lang.Enum.<init>(java.lang.String,int)`

<a id="m32"></a>
### capacity

```java
public long capacity()
```

정의: [src/main/java/com/dodamdodam/backend/auth/domain/AuthRateLimitKey.java:37](../../../src/main/java/com/dodamdodam/backend/auth/domain/AuthRateLimitKey.java#L37)

한도번째 요청까지 허용하는 예약 수를 반환한다.

호출자: [auth.infrastructure.AuthRateLimitRepository.RateLimitState.retryAfterSeconds](#m82)

내부 호출: 없음

<a id="m33"></a>
### windowSeconds

```java
public long windowSeconds()
```

정의: [src/main/java/com/dodamdodam/backend/auth/domain/AuthRateLimitKey.java:42](../../../src/main/java/com/dodamdodam/backend/auth/domain/AuthRateLimitKey.java#L42)

최초 예약부터 고정하는 요청창 길이를 초로 반환한다.

호출자: [auth.infrastructure.AuthRateLimitRepository.reserveAllowed](#m81) / [auth.infrastructure.AuthRateLimitRepository.RateLimitState.retryAfterSeconds](#m82) / [auth.infrastructure.AuthRateLimitRepository.RateLimitState.windowFinished](#m83)

내부 호출: 없음

<a id="m38"></a>
### valueOf — 자동 생성

```java
public static com.dodamdodam.backend.auth.domain.AuthRateLimitKey.Action valueOf(java.lang.String name)
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m39"></a>
### values — 자동 생성

```java
public static com.dodamdodam.backend.auth.domain.AuthRateLimitKey.Action[] values()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="c12"></a>
## auth.dto.IssueEmailVerificationRequest

정의: [src/main/java/com/dodamdodam/backend/auth/dto/IssueEmailVerificationRequest.java:10](../../../src/main/java/com/dodamdodam/backend/auth/dto/IssueEmailVerificationRequest.java#L10) · RECORD

<a id="m40"></a>
### fromJson

```java
public static com.dodamdodam.backend.auth.dto.IssueEmailVerificationRequest fromJson(tools.jackson.databind.JsonNode body)
```

정의: [src/main/java/com/dodamdodam/backend/auth/dto/IssueEmailVerificationRequest.java:15](../../../src/main/java/com/dodamdodam/backend/auth/dto/IssueEmailVerificationRequest.java#L15)

목적과 email만 파싱하며 PIN 목적의 email 생략과 명시적 null을 구분한다.

호출자: [auth.api.EmailVerificationController.issue](#m6)

내부 호출: [auth.dto.IssueEmailVerificationRequest.IssueEmailVerificationRequest](#m44) / [auth.dto.IssueEmailVerificationRequest.Purpose.valueOf](#m51) / [auth.exception.EmailVerificationException.validation](#m64)

외부 라이브러리 호출: `java.util.Set.containsAll(java.util.Collection<?>)` / `tools.jackson.databind.JsonNode.get(java.lang.String)` / `tools.jackson.databind.JsonNode.isObject()` / `tools.jackson.databind.JsonNode.isString()` / `tools.jackson.databind.JsonNode.propertyNames()` / `tools.jackson.databind.JsonNode.stringValue()`

<a id="m41"></a>
### normalized

```java
public com.dodamdodam.backend.auth.dto.IssueEmailVerificationRequest normalized(jakarta.validation.Validator validator)
```

정의: [src/main/java/com/dodamdodam/backend/auth/dto/IssueEmailVerificationRequest.java:40](../../../src/main/java/com/dodamdodam/backend/auth/dto/IssueEmailVerificationRequest.java#L40)

서비스 처리 전에 호출하며 정규화한 이메일의 형식·길이·목적별 필수 여부를 검사한다.

호출자: [auth.application.EmailVerificationIssuanceService.issue](#m19) / [auth.application.EmailVerificationIssuanceService.preparePin](#m21)

내부 호출: [auth.dto.IssueEmailVerificationRequest.IssueEmailVerificationRequest](#m44) / [auth.dto.IssueEmailVerificationRequest.trimEmail](#m42) / [auth.exception.EmailVerificationException.validation](#m64)

외부 라이브러리 호출: `jakarta.validation.Validator.validateValue(java.lang.Class<T>,java.lang.String,java.lang.Object,java.lang.Class<?>[])` / `java.lang.String.chars()` / `java.lang.String.isEmpty()` / `java.lang.String.length()` / `java.lang.String.toLowerCase(java.util.Locale)` / `java.util.Set.isEmpty()` / `java.util.stream.IntStream.allMatch(java.util.function.IntPredicate)`

<a id="m42"></a>
### trimEmail

```java
private static java.lang.String trimEmail(java.lang.String value)
```

정의: [src/main/java/com/dodamdodam/backend/auth/dto/IssueEmailVerificationRequest.java:62](../../../src/main/java/com/dodamdodam/backend/auth/dto/IssueEmailVerificationRequest.java#L62)

자녀 입력과 같은 Unicode 공백 기준으로 양끝만 제거하며 점·+suffix는 보존한다.

호출자: [auth.dto.IssueEmailVerificationRequest.normalized](#m41)

내부 호출: [auth.dto.IssueEmailVerificationRequest.isSpace](#m43)

외부 라이브러리 호출: `java.lang.Character.charCount(int)` / `java.lang.String.codePointAt(int)` / `java.lang.String.codePointBefore(int)` / `java.lang.String.length()` / `java.lang.String.substring(int,int)`

<a id="m43"></a>
### isSpace

```java
private static boolean isSpace(int codePoint)
```

정의: [src/main/java/com/dodamdodam/backend/auth/dto/IssueEmailVerificationRequest.java:75](../../../src/main/java/com/dodamdodam/backend/auth/dto/IssueEmailVerificationRequest.java#L75)

NBSP를 포함한 바깥 공백을 판정하고 NUL 등 제어 문자는 보정하지 않는다.

호출자: [auth.dto.IssueEmailVerificationRequest.trimEmail](#m42)

내부 호출: 없음

외부 라이브러리 호출: `java.lang.Character.isSpaceChar(int)` / `java.lang.Character.isWhitespace(int)`

<a id="m44"></a>
### IssueEmailVerificationRequest 생성자 — 자동 생성

```java
public void <init>(com.dodamdodam.backend.auth.dto.IssueEmailVerificationRequest.Purpose purpose, java.lang.@jakarta.validation.constraints.Email String email)
```

호출자: [auth.application.EmailVerificationIssuanceService.preparePin](#m21) / [auth.dto.IssueEmailVerificationRequest.fromJson](#m40) / [auth.dto.IssueEmailVerificationRequest.normalized](#m41)

내부 호출: 없음

외부 라이브러리 호출: `java.lang.Record.<init>()`

<a id="m45"></a>
### email — 자동 생성

```java
public java.lang.@jakarta.validation.constraints.Email String email()
```

애너테이션: `@jakarta.validation.constraints.Email`

호출자: [auth.application.EmailVerificationIssuanceService.issue](#m19) / [auth.application.EmailVerificationIssuanceService.preparePin](#m21) / [auth.application.EmailVerificationIssuanceService.preparePublic](#m20)

내부 호출: 없음

<a id="m46"></a>
### equals — 자동 생성

```java
public final boolean equals(java.lang.Object o)
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m47"></a>
### hashCode — 자동 생성

```java
public final int hashCode()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m48"></a>
### purpose — 자동 생성

```java
public com.dodamdodam.backend.auth.dto.IssueEmailVerificationRequest.Purpose purpose()
```

호출자: [auth.application.EmailVerificationIssuanceService.issue](#m19) / [auth.application.EmailVerificationIssuanceService.preparePin](#m21) / [auth.application.EmailVerificationIssuanceService.preparePublic](#m20)

내부 호출: 없음

<a id="m49"></a>
### toString — 자동 생성

```java
public final java.lang.String toString()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="c13"></a>
## auth.dto.IssueEmailVerificationRequest.Purpose

정의: [src/main/java/com/dodamdodam/backend/auth/dto/IssueEmailVerificationRequest.java:79](../../../src/main/java/com/dodamdodam/backend/auth/dto/IssueEmailVerificationRequest.java#L79) · ENUM

<a id="m50"></a>
### Purpose 생성자 — 자동 생성

```java
private void <init>()
```

호출자: `auth.dto.IssueEmailVerificationRequest.Purpose.<initializer>`

내부 호출: 없음

외부 라이브러리 호출: `java.lang.Enum.<init>(java.lang.String,int)`

<a id="m51"></a>
### valueOf — 자동 생성

```java
public static com.dodamdodam.backend.auth.dto.IssueEmailVerificationRequest.Purpose valueOf(java.lang.String name)
```

호출자: [auth.dto.IssueEmailVerificationRequest.fromJson](#m40)

내부 호출: 없음

<a id="m52"></a>
### values — 자동 생성

```java
public static com.dodamdodam.backend.auth.dto.IssueEmailVerificationRequest.Purpose[] values()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="c14"></a>
## auth.dto.VerificationIssueResponse

정의: [src/main/java/com/dodamdodam/backend/auth/dto/VerificationIssueResponse.java:8](../../../src/main/java/com/dodamdodam/backend/auth/dto/VerificationIssueResponse.java#L8) · RECORD

정상·decoy 모두 같은 길이의 식별자·KST 만료 시각만 공개한다.

<a id="m53"></a>
### VerificationIssueResponse 생성자 — 자동 생성

```java
public void <init>(java.util.UUID challengeId, java.time.OffsetDateTime expiresAt)
```

호출자: [auth.application.EmailVerificationIssuanceService.issue](#m19)

내부 호출: 없음

외부 라이브러리 호출: `java.lang.Record.<init>()`

<a id="m54"></a>
### challengeId — 자동 생성

```java
public java.util.UUID challengeId()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m55"></a>
### equals — 자동 생성

```java
public final boolean equals(java.lang.Object o)
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m56"></a>
### expiresAt — 자동 생성

```java
public java.time.OffsetDateTime expiresAt()
```

애너테이션: `@com.fasterxml.jackson.annotation.JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX")`

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m57"></a>
### hashCode — 자동 생성

```java
public final int hashCode()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m58"></a>
### toString — 자동 생성

```java
public final java.lang.String toString()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="c15"></a>
## auth.exception.AuthRateLimitException

정의: [src/main/java/com/dodamdodam/backend/auth/exception/AuthRateLimitException.java:3](../../../src/main/java/com/dodamdodam/backend/auth/exception/AuthRateLimitException.java#L3) · CLASS

<a id="m59"></a>
### AuthRateLimitException 생성자

```java
public void <init>(long retryAfterSeconds)
```

정의: [src/main/java/com/dodamdodam/backend/auth/exception/AuthRateLimitException.java:8](../../../src/main/java/com/dodamdodam/backend/auth/exception/AuthRateLimitException.java#L8)

식별자·DB 상태 없이 최소 재시도 대기시간만 전달한다.

호출자: [auth.application.AuthRateLimitService.reserve](#m11) / [auth.infrastructure.EmailVerificationRepository.requireResendReady](#m125)

내부 호출: 없음

외부 라이브러리 호출: `java.lang.Math.max(long,long)` / `java.lang.RuntimeException.<init>(java.lang.String)`

<a id="m60"></a>
### code

```java
public java.lang.String code()
```

정의: [src/main/java/com/dodamdodam/backend/auth/exception/AuthRateLimitException.java:14](../../../src/main/java/com/dodamdodam/backend/auth/exception/AuthRateLimitException.java#L14)

HTTP 연결부에서 사용할 고정 업무 오류 코드를 반환한다.

호출자: [auth.api.EmailVerificationExceptionHandler.limited](#m8) / [global.web.GlobalApiRateLimitFilter.doFilterInternal](#m517)

내부 호출: 없음

<a id="m61"></a>
### retryAfterSeconds

```java
public long retryAfterSeconds()
```

정의: [src/main/java/com/dodamdodam/backend/auth/exception/AuthRateLimitException.java:19](../../../src/main/java/com/dodamdodam/backend/auth/exception/AuthRateLimitException.java#L19)

가장 늦은 제한이 끝나는 시각까지 올림한 대기 초를 반환한다.

호출자: [auth.api.EmailVerificationExceptionHandler.limited](#m8) / [global.web.GlobalApiRateLimitFilter.doFilterInternal](#m517)

내부 호출: 없음

<a id="c16"></a>
## auth.exception.EmailVerificationException

정의: [src/main/java/com/dodamdodam/backend/auth/exception/EmailVerificationException.java:7](../../../src/main/java/com/dodamdodam/backend/auth/exception/EmailVerificationException.java#L7) · CLASS

<a id="m62"></a>
### EmailVerificationException 생성자

```java
private void <init>(java.lang.String field)
```

정의: [src/main/java/com/dodamdodam/backend/auth/exception/EmailVerificationException.java:14](../../../src/main/java/com/dodamdodam/backend/auth/exception/EmailVerificationException.java#L14)

입력 원문 대신 검증에 실패한 필드만 보관한다.

호출자: [auth.exception.EmailVerificationException.validation](#m64)

내부 호출: [auth.exception.EmailVerificationException.EmailVerificationException](#m63) / [global.error.ApiErrorResponse.FieldError.FieldError](#m500)

외부 라이브러리 호출: `java.util.List.of(E)`

<a id="m63"></a>
### EmailVerificationException 생성자

```java
private void <init>(int status, java.lang.String code, java.lang.String message, java.util.List<com.dodamdodam.backend.global.error.ApiErrorResponse.FieldError> fields)
```

정의: [src/main/java/com/dodamdodam/backend/auth/exception/EmailVerificationException.java:19](../../../src/main/java/com/dodamdodam/backend/auth/exception/EmailVerificationException.java#L19)

외부 실패 원문을 보관하지 않고 허용한 업무 오류만 구성한다.

호출자: [auth.exception.EmailVerificationException.EmailVerificationException](#m62) / [auth.exception.EmailVerificationException.authenticationRequired](#m68) / [auth.exception.EmailVerificationException.authorizationRequired](#m69) / [auth.exception.EmailVerificationException.deliveryUnavailable](#m66) / [auth.exception.EmailVerificationException.pinAlreadySet](#m70) / [auth.exception.EmailVerificationException.pinNotSet](#m71)

내부 호출: 없음

외부 라이브러리 호출: `java.lang.RuntimeException.<init>(java.lang.String)`

<a id="m64"></a>
### validation

```java
public static com.dodamdodam.backend.auth.exception.EmailVerificationException validation(java.lang.String field)
```

정의: [src/main/java/com/dodamdodam/backend/auth/exception/EmailVerificationException.java:27](../../../src/main/java/com/dodamdodam/backend/auth/exception/EmailVerificationException.java#L27)

요청 구조·값 검증에 공통으로 사용할 오류를 만든다.

호출자: [auth.api.EmailVerificationController.issue](#m6) / [auth.application.EmailVerificationIssuanceService.preparePin](#m21) / [auth.dto.IssueEmailVerificationRequest.fromJson](#m40) / [auth.dto.IssueEmailVerificationRequest.normalized](#m41)

내부 호출: [auth.exception.EmailVerificationException.EmailVerificationException](#m62)

<a id="m65"></a>
### code

```java
public java.lang.String code()
```

정의: [src/main/java/com/dodamdodam/backend/auth/exception/EmailVerificationException.java:32](../../../src/main/java/com/dodamdodam/backend/auth/exception/EmailVerificationException.java#L32)

HTTP 오류 연결에 사용할 고정 검증 코드를 반환한다.

호출자: [auth.api.EmailVerificationExceptionHandler.handle](#m7) / [auth.session.AuthStateErrorFilter.doFilterInternal](#m154)

내부 호출: 없음

<a id="m66"></a>
### deliveryUnavailable

```java
public static com.dodamdodam.backend.auth.exception.EmailVerificationException deliveryUnavailable()
```

정의: [src/main/java/com/dodamdodam/backend/auth/exception/EmailVerificationException.java:37](../../../src/main/java/com/dodamdodam/backend/auth/exception/EmailVerificationException.java#L37)

SMTP 실패 보상이 완료된 뒤 일반화한 재발급 안내만 반환한다.

호출자: [auth.application.EmailVerificationDeliveryService.deliver](#m16) / [auth.application.EmailVerificationIssuanceService.verifyResponseAccess](#m22)

내부 호출: [auth.exception.EmailVerificationException.EmailVerificationException](#m63)

외부 라이브러리 호출: `java.util.List.of()`

<a id="m67"></a>
### status

```java
public int status()
```

정의: [src/main/java/com/dodamdodam/backend/auth/exception/EmailVerificationException.java:43](../../../src/main/java/com/dodamdodam/backend/auth/exception/EmailVerificationException.java#L43)

HTTP 연결에 사용할 고정 상태를 반환한다.

호출자: [auth.api.EmailVerificationExceptionHandler.handle](#m7) / [auth.session.AuthStateErrorFilter.doFilterInternal](#m154)

내부 호출: 없음

<a id="m68"></a>
### authenticationRequired

```java
public static com.dodamdodam.backend.auth.exception.EmailVerificationException authenticationRequired()
```

정의: [src/main/java/com/dodamdodam/backend/auth/exception/EmailVerificationException.java:48](../../../src/main/java/com/dodamdodam/backend/auth/exception/EmailVerificationException.java#L48)

현재 문맥이 폐기·만료·교체된 늦은 응답을 거부한다.

호출자: [auth.application.EmailVerificationIssuanceService.preparePin](#m21) / [auth.application.EmailVerificationIssuanceService.verifyResponseAccess](#m22)

내부 호출: [auth.exception.EmailVerificationException.EmailVerificationException](#m63)

외부 라이브러리 호출: `java.util.List.of()`

<a id="m69"></a>
### authorizationRequired

```java
public static com.dodamdodam.backend.auth.exception.EmailVerificationException authorizationRequired(com.dodamdodam.backend.auth.dto.IssueEmailVerificationRequest.Purpose purpose)
```

정의: [src/main/java/com/dodamdodam/backend/auth/exception/EmailVerificationException.java:53](../../../src/main/java/com/dodamdodam/backend/auth/exception/EmailVerificationException.java#L53)

PIN 목적의 현재 이메일·문맥·generation·PIN 상태 결합이 달라졌음을 표현한다.

호출자: [auth.application.EmailVerificationIssuanceService.verifyResponseAccess](#m22)

내부 호출: [auth.exception.EmailVerificationException.EmailVerificationException](#m63)

외부 라이브러리 호출: `java.util.List.of()`

<a id="m70"></a>
### pinAlreadySet

```java
public static com.dodamdodam.backend.auth.exception.EmailVerificationException pinAlreadySet()
```

정의: [src/main/java/com/dodamdodam/backend/auth/exception/EmailVerificationException.java:60](../../../src/main/java/com/dodamdodam/backend/auth/exception/EmailVerificationException.java#L60)

이미 PIN이 있는 계정에는 최초 설정 발급을 허용하지 않는다.

호출자: [auth.application.EmailVerificationIssuanceService.preparePin](#m21)

내부 호출: [auth.exception.EmailVerificationException.EmailVerificationException](#m63)

외부 라이브러리 호출: `java.util.List.of()`

<a id="m71"></a>
### pinNotSet

```java
public static com.dodamdodam.backend.auth.exception.EmailVerificationException pinNotSet()
```

정의: [src/main/java/com/dodamdodam/backend/auth/exception/EmailVerificationException.java:65](../../../src/main/java/com/dodamdodam/backend/auth/exception/EmailVerificationException.java#L65)

PIN이 없는 계정에는 재설정 발급을 허용하지 않는다.

호출자: [auth.application.EmailVerificationIssuanceService.preparePin](#m21)

내부 호출: [auth.exception.EmailVerificationException.EmailVerificationException](#m63)

외부 라이브러리 호출: `java.util.List.of()`

<a id="m72"></a>
### fields

```java
public java.util.List<com.dodamdodam.backend.global.error.ApiErrorResponse.FieldError> fields()
```

정의: [src/main/java/com/dodamdodam/backend/auth/exception/EmailVerificationException.java:70](../../../src/main/java/com/dodamdodam/backend/auth/exception/EmailVerificationException.java#L70)

이메일·번호 원문이 없는 필드 오류를 반환한다.

호출자: [auth.api.EmailVerificationExceptionHandler.handle](#m7)

내부 호출: 없음

<a id="c17"></a>
## auth.infrastructure.AuthRateLimitKeyHasher

정의: [src/main/java/com/dodamdodam/backend/auth/infrastructure/AuthRateLimitKeyHasher.java:17](../../../src/main/java/com/dodamdodam/backend/auth/infrastructure/AuthRateLimitKeyHasher.java#L17) · CLASS

<a id="m73"></a>
### AuthRateLimitKeyHasher 생성자

```java
public void <init>(byte[] key)
```

정의: [src/main/java/com/dodamdodam/backend/auth/infrastructure/AuthRateLimitKeyHasher.java:23](../../../src/main/java/com/dodamdodam/backend/auth/infrastructure/AuthRateLimitKeyHasher.java#L23)

번호 인증 키와 별도로 주입하는 요청 제한 전용 32바이트 키를 받는다.

호출자: [auth.infrastructure.EmailVerificationKeys.EmailVerificationKeys](#m117)

내부 호출: [auth.session.AuthStateUnavailableException.AuthStateUnavailableException](#m163)

외부 라이브러리 호출: `java.lang.Object.<init>()` / `javax.crypto.spec.SecretKeySpec.<init>(byte[],java.lang.String)`

<a id="m74"></a>
### apiIp

```java
public com.dodamdodam.backend.auth.domain.AuthRateLimitKey apiIp(java.net.InetAddress clientAddress)
```

정의: [src/main/java/com/dodamdodam/backend/auth/infrastructure/AuthRateLimitKeyHasher.java:31](../../../src/main/java/com/dodamdodam/backend/auth/infrastructure/AuthRateLimitKeyHasher.java#L31)

상위 진입부가 확인한 실제 IP를 사용하며 전달 헤더를 직접 신뢰하지 않는다.

호출자: [global.web.GlobalApiRateLimitFilter.doFilterInternal](#m517)

내부 호출: [auth.infrastructure.AuthRateLimitKeyHasher.addressScope](#m76) / [auth.infrastructure.AuthRateLimitKeyHasher.hash](#m77)

<a id="m75"></a>
### emailIssueKeys

```java
public java.util.List<com.dodamdodam.backend.auth.domain.AuthRateLimitKey> emailIssueKeys(java.lang.String normalizedEmail, com.dodamdodam.backend.auth.dto.IssueEmailVerificationRequest.Purpose purpose, java.util.UUID accountId, java.util.UUID securityContextId, java.net.InetAddress clientAddress)
```

정의: [src/main/java/com/dodamdodam/backend/auth/infrastructure/AuthRateLimitKeyHasher.java:36](../../../src/main/java/com/dodamdodam/backend/auth/infrastructure/AuthRateLimitKeyHasher.java#L36)

정규화·권한 검사한 범위로 누적 한도와 현재 문맥의 cooldown 키를 만든다.

호출자: [auth.application.EmailVerificationIssuanceService.issue](#m19)

내부 호출: [auth.infrastructure.AuthRateLimitKeyHasher.addressScope](#m76) / [auth.infrastructure.AuthRateLimitKeyHasher.hash](#m77) / [auth.session.AuthStateUnavailableException.AuthStateUnavailableException](#m163)

외부 라이브러리 호출: `java.lang.Enum.name()` / `java.lang.String.isBlank()` / `java.util.List.of(E,E,E)` / `java.util.UUID.toString()`

<a id="m76"></a>
### addressScope

```java
private java.lang.String addressScope(java.net.InetAddress address)
```

정의: [src/main/java/com/dodamdodam/backend/auth/infrastructure/AuthRateLimitKeyHasher.java:56](../../../src/main/java/com/dodamdodam/backend/auth/infrastructure/AuthRateLimitKeyHasher.java#L56)

같은 IP의 축약 표기가 달라도 주소 바이트가 같으면 같은 예산을 사용한다.

호출자: [auth.infrastructure.AuthRateLimitKeyHasher.apiIp](#m74) / [auth.infrastructure.AuthRateLimitKeyHasher.emailIssueKeys](#m75)

내부 호출: [auth.session.AuthStateUnavailableException.AuthStateUnavailableException](#m163)

외부 라이브러리 호출: `java.net.InetAddress.getAddress()` / `java.util.HexFormat.formatHex(byte[])` / `java.util.HexFormat.of()`

<a id="m77"></a>
### hash

```java
private com.dodamdodam.backend.auth.domain.AuthRateLimitKey hash(com.dodamdodam.backend.auth.domain.AuthRateLimitKey.Action action, java.lang.String[] scope)
```

정의: [src/main/java/com/dodamdodam/backend/auth/infrastructure/AuthRateLimitKeyHasher.java:64](../../../src/main/java/com/dodamdodam/backend/auth/infrastructure/AuthRateLimitKeyHasher.java#L64)

action과 각 UTF-8 필드의 길이를 포함해 경계가 모호하지 않은 전용 HMAC을 만든다.

호출자: [auth.infrastructure.AuthRateLimitKeyHasher.apiIp](#m74) / [auth.infrastructure.AuthRateLimitKeyHasher.emailIssueKeys](#m75)

내부 호출: [auth.domain.AuthRateLimitKey.AuthRateLimitKey](#m29) / [auth.infrastructure.AuthRateLimitKeyHasher.updateField](#m78) / [auth.session.AuthStateUnavailableException.AuthStateUnavailableException](#m164)

외부 라이브러리 호출: `java.lang.Enum.name()` / `java.lang.String.getBytes(java.nio.charset.Charset)` / `javax.crypto.Mac.doFinal()` / `javax.crypto.Mac.getInstance(java.lang.String)` / `javax.crypto.Mac.init(java.security.Key)` / `javax.crypto.Mac.update(byte[])`

<a id="m78"></a>
### updateField

```java
private void updateField(javax.crypto.Mac mac, java.lang.String value)
```

정의: [src/main/java/com/dodamdodam/backend/auth/infrastructure/AuthRateLimitKeyHasher.java:81](../../../src/main/java/com/dodamdodam/backend/auth/infrastructure/AuthRateLimitKeyHasher.java#L81)

문자열 구분자 충돌 없이 필드 길이와 원문 바이트를 순서대로 계산에만 사용한다.

호출자: [auth.infrastructure.AuthRateLimitKeyHasher.hash](#m77)

내부 호출: 없음

외부 라이브러리 호출: `java.lang.String.getBytes(java.nio.charset.Charset)` / `java.nio.ByteBuffer.allocate(int)` / `java.nio.ByteBuffer.array()` / `java.nio.ByteBuffer.putInt(int)` / `javax.crypto.Mac.update(byte[])`

<a id="c18"></a>
## auth.infrastructure.AuthRateLimitRepository

정의: [src/main/java/com/dodamdodam/backend/auth/infrastructure/AuthRateLimitRepository.java:17](../../../src/main/java/com/dodamdodam/backend/auth/infrastructure/AuthRateLimitRepository.java#L17) · CLASS

설정: `@org.springframework.stereotype.Repository`

<a id="m79"></a>
### AuthRateLimitRepository 생성자

```java
public void <init>(org.springframework.jdbc.core.JdbcTemplate jdbc)
```

정의: [src/main/java/com/dodamdodam/backend/auth/infrastructure/AuthRateLimitRepository.java:23](../../../src/main/java/com/dodamdodam/backend/auth/infrastructure/AuthRateLimitRepository.java#L23)

동일 DB의 인스턴스들이 요청 예산을 공유하는 저장소를 구성한다.

호출자: 정적 Java 호출 없음

내부 호출: 없음

외부 라이브러리 호출: `java.lang.Object.<init>()`

<a id="m80"></a>
### reserve

```java
public long reserve(java.util.List<com.dodamdodam.backend.auth.domain.AuthRateLimitKey> keys)
```

정의: [src/main/java/com/dodamdodam/backend/auth/infrastructure/AuthRateLimitRepository.java:28](../../../src/main/java/com/dodamdodam/backend/auth/infrastructure/AuthRateLimitRepository.java#L28)

호출자 Tx에서 한 단계의 모든 키를 예약하며 거부 시 쓰지 않고 재시도 초를 반환한다.

애너테이션: `@org.springframework.transaction.annotation.Transactional(propagation=MANDATORY)`

호출자: [auth.application.AuthRateLimitService.reserve](#m11)

내부 호출: [auth.domain.AuthRateLimitKey.action](#m34) / [auth.domain.AuthRateLimitKey.keyHash](#m30) / [auth.infrastructure.AuthRateLimitRepository.reserveAllowed](#m81) / [auth.infrastructure.AuthRateLimitRepository.RateLimitState.RateLimitState](#m84) / [auth.infrastructure.AuthRateLimitRepository.RateLimitState.retryAfterSeconds](#m82) / [auth.session.AuthStateUnavailableException.AuthStateUnavailableException](#m163)

외부 라이브러리 호출: `java.lang.Enum.name()` / `java.lang.Math.max(long,long)` / `java.sql.ResultSet.getLong(java.lang.String)` / `java.sql.ResultSet.getObject(java.lang.String,java.lang.Class<T>)` / `java.util.HashMap.put(K,V)` / `java.util.HexFormat.formatHex(byte[])` / `java.util.HexFormat.of()` / `java.util.LinkedHashMap.<init>()` / `java.util.LinkedHashMap.entrySet()` / `java.util.List.getFirst()` / `java.util.List.isEmpty()` / `java.util.Map.Entry.getKey()` / `java.util.Map.Entry.getValue()` / `java.util.TreeMap.<init>()` / `java.util.TreeMap.entrySet()` / `java.util.TreeMap.put(K,V)` / `org.springframework.jdbc.core.JdbcTemplate.query(java.lang.String,org.springframework.jdbc.core.RowMapper<T>,java.lang.@org.jspecify.annotations.Nullable Object @org.jspecify.annotations.Nullable [])` / `org.springframework.jdbc.core.JdbcTemplate.queryForObject(java.lang.String,java.lang.Class<T>)` / `org.springframework.jdbc.core.JdbcTemplate.queryForObject(java.lang.String,org.springframework.jdbc.core.RowMapper<T>,java.lang.@org.jspecify.annotations.Nullable Object @org.jspecify.annotations.Nullable [])`

<a id="m81"></a>
### reserveAllowed

```java
private void reserveAllowed(com.dodamdodam.backend.auth.domain.AuthRateLimitKey key, com.dodamdodam.backend.auth.infrastructure.AuthRateLimitRepository.RateLimitState state, java.time.OffsetDateTime checkedAt)
```

정의: [src/main/java/com/dodamdodam/backend/auth/infrastructure/AuthRateLimitRepository.java:73](../../../src/main/java/com/dodamdodam/backend/auth/infrastructure/AuthRateLimitRepository.java#L73)

모든 제한이 허용된 뒤에만 count를 쓰며 기존 창·차단을 임의로 연장하지 않는다.

호출자: [auth.infrastructure.AuthRateLimitRepository.reserve](#m80)

내부 호출: [auth.domain.AuthRateLimitKey.action](#m34) / [auth.domain.AuthRateLimitKey.keyHash](#m30) / [auth.domain.AuthRateLimitKey.Action.windowSeconds](#m33) / [auth.infrastructure.AuthRateLimitRepository.RateLimitState.windowFinished](#m83)

외부 라이브러리 호출: `java.lang.Enum.name()` / `java.time.OffsetDateTime.plusSeconds(long)` / `org.springframework.jdbc.core.JdbcTemplate.update(java.lang.String,java.lang.@org.jspecify.annotations.Nullable Object @org.jspecify.annotations.Nullable [])`

<a id="c19"></a>
## auth.infrastructure.AuthRateLimitRepository.RateLimitState

정의: [src/main/java/com/dodamdodam/backend/auth/infrastructure/AuthRateLimitRepository.java:92](../../../src/main/java/com/dodamdodam/backend/auth/infrastructure/AuthRateLimitRepository.java#L92) · RECORD

<a id="m82"></a>
### retryAfterSeconds

```java
long retryAfterSeconds(com.dodamdodam.backend.auth.domain.AuthRateLimitKey.Action action, java.time.OffsetDateTime checkedAt)
```

정의: [src/main/java/com/dodamdodam/backend/auth/infrastructure/AuthRateLimitRepository.java:95](../../../src/main/java/com/dodamdodam/backend/auth/infrastructure/AuthRateLimitRepository.java#L95)

차단과 예산이 소진된 창 중 최늦은 재시도 시각까지 올림 초를 계산한다.

호출자: [auth.infrastructure.AuthRateLimitRepository.reserve](#m80)

내부 호출: [auth.domain.AuthRateLimitKey.Action.capacity](#m32) / [auth.domain.AuthRateLimitKey.Action.windowSeconds](#m33)

외부 라이브러리 호출: `java.lang.Math.max(long,long)` / `java.time.Duration.between(java.time.temporal.Temporal,java.time.temporal.Temporal)` / `java.time.Duration.getNano()` / `java.time.Duration.getSeconds()` / `java.time.OffsetDateTime.isAfter(java.time.OffsetDateTime)` / `java.time.OffsetDateTime.isBefore(java.time.OffsetDateTime)` / `java.time.OffsetDateTime.plusSeconds(long)`

<a id="m83"></a>
### windowFinished

```java
boolean windowFinished(com.dodamdodam.backend.auth.domain.AuthRateLimitKey.Action action, java.time.OffsetDateTime checkedAt)
```

정의: [src/main/java/com/dodamdodam/backend/auth/infrastructure/AuthRateLimitRepository.java:110](../../../src/main/java/com/dodamdodam/backend/auth/infrastructure/AuthRateLimitRepository.java#L110)

창과 차단 모두 equality부터 끝나며 그 전에는 기존 count를 유지한다.

호출자: [auth.infrastructure.AuthRateLimitRepository.reserveAllowed](#m81)

내부 호출: [auth.domain.AuthRateLimitKey.Action.windowSeconds](#m33)

외부 라이브러리 호출: `java.time.OffsetDateTime.isBefore(java.time.OffsetDateTime)` / `java.time.OffsetDateTime.plusSeconds(long)`

<a id="m84"></a>
### RateLimitState 생성자 — 자동 생성

```java
void <init>(java.time.OffsetDateTime windowStartedAt, long attemptCount, java.time.OffsetDateTime blockedUntil)
```

호출자: [auth.infrastructure.AuthRateLimitRepository.reserve](#m80)

내부 호출: 없음

외부 라이브러리 호출: `java.lang.Record.<init>()`

<a id="m85"></a>
### attemptCount — 자동 생성

```java
public long attemptCount()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m86"></a>
### blockedUntil — 자동 생성

```java
public java.time.OffsetDateTime blockedUntil()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m87"></a>
### equals — 자동 생성

```java
public final boolean equals(java.lang.Object o)
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m88"></a>
### hashCode — 자동 생성

```java
public final int hashCode()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m89"></a>
### toString — 자동 생성

```java
public final java.lang.String toString()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m90"></a>
### windowStartedAt — 자동 생성

```java
public java.time.OffsetDateTime windowStartedAt()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="c20"></a>
## auth.infrastructure.EmailVerificationAccountRepository

정의: [src/main/java/com/dodamdodam/backend/auth/infrastructure/EmailVerificationAccountRepository.java:10](../../../src/main/java/com/dodamdodam/backend/auth/infrastructure/EmailVerificationAccountRepository.java#L10) · CLASS

설정: `@org.springframework.stereotype.Repository`

<a id="m91"></a>
### EmailVerificationAccountRepository 생성자

```java
public void <init>(org.springframework.jdbc.core.JdbcTemplate jdbc)
```

정의: [src/main/java/com/dodamdodam/backend/auth/infrastructure/EmailVerificationAccountRepository.java:15](../../../src/main/java/com/dodamdodam/backend/auth/infrastructure/EmailVerificationAccountRepository.java#L15)

발급에 필요한 이메일·계정 버전만 읽으며 비밀번호·PIN hash는 조회하지 않는다.

호출자: 정적 Java 호출 없음

내부 호출: 없음

외부 라이브러리 호출: `java.lang.Object.<init>()`

<a id="m92"></a>
### findForUpdate

```java
public java.util.Optional<com.dodamdodam.backend.auth.infrastructure.EmailVerificationAccountRepository.AccountState> findForUpdate(java.lang.String email)
```

정의: [src/main/java/com/dodamdodam/backend/auth/infrastructure/EmailVerificationAccountRepository.java:20](../../../src/main/java/com/dodamdodam/backend/auth/infrastructure/EmailVerificationAccountRepository.java#L20)

공개 발급 scope 잠금 뒤 현재 계정을 잠그고 적격 여부와 reset snapshot의 근거를 읽는다.

애너테이션: `@org.springframework.transaction.annotation.Transactional(propagation=MANDATORY)`

호출자: [auth.application.EmailVerificationIssuanceService.preparePublic](#m20)

내부 호출: [auth.infrastructure.EmailVerificationAccountRepository.AccountState.AccountState](#m96)

외부 라이브러리 호출: `java.sql.ResultSet.getLong(java.lang.String)` / `java.sql.ResultSet.getObject(java.lang.String,java.lang.Class<T>)` / `java.sql.ResultSet.getString(java.lang.String)` / `java.util.Collection.stream()` / `java.util.stream.Stream.findFirst()` / `org.springframework.jdbc.core.JdbcTemplate.query(java.lang.String,org.springframework.jdbc.core.RowMapper<T>,java.lang.@org.jspecify.annotations.Nullable Object @org.jspecify.annotations.Nullable [])`

<a id="m93"></a>
### lockPin

```java
public void lockPin(java.util.UUID accountId)
```

정의: [src/main/java/com/dodamdodam/backend/auth/infrastructure/EmailVerificationAccountRepository.java:31](../../../src/main/java/com/dodamdodam/backend/auth/infrastructure/EmailVerificationAccountRepository.java#L31)

계정·세션 잠금을 얻은 호출자 Tx에서 현재 PIN 행을 마지막 상위 row로 잠근다.

애너테이션: `@org.springframework.transaction.annotation.Transactional(propagation=MANDATORY)`

호출자: [auth.application.EmailVerificationIssuanceService.preparePin](#m21)

내부 호출: 없음

외부 라이브러리 호출: `org.springframework.jdbc.core.JdbcTemplate.queryForList(java.lang.String,java.lang.Class<T>,java.lang.@org.jspecify.annotations.Nullable Object @org.jspecify.annotations.Nullable [])`

<a id="m94"></a>
### findPinState

```java
public java.util.Optional<com.dodamdodam.backend.auth.infrastructure.EmailVerificationAccountRepository.PinState> findPinState(java.util.UUID accountId, java.util.UUID contextId)
```

정의: [src/main/java/com/dodamdodam/backend/auth/infrastructure/EmailVerificationAccountRepository.java:37](../../../src/main/java/com/dodamdodam/backend/auth/infrastructure/EmailVerificationAccountRepository.java#L37)

PIN 원문 없이 현재 세션·이메일·generation·PIN 버전을 같은 DB 조회로 읽는다.

호출자: [auth.application.EmailVerificationIssuanceService.preparePin](#m21) / [auth.application.EmailVerificationIssuanceService.verifyResponseAccess](#m22)

내부 호출: [auth.infrastructure.EmailVerificationAccountRepository.PinState.PinState](#m103)

외부 라이브러리 호출: `java.sql.ResultSet.getLong(java.lang.String)` / `java.sql.ResultSet.getObject(java.lang.String,java.lang.Class<T>)` / `java.sql.ResultSet.getString(java.lang.String)` / `java.util.Collection.stream()` / `java.util.stream.Stream.findFirst()` / `org.springframework.jdbc.core.JdbcTemplate.query(java.lang.String,org.springframework.jdbc.core.RowMapper<T>,java.lang.@org.jspecify.annotations.Nullable Object @org.jspecify.annotations.Nullable [])`

<a id="c21"></a>
## auth.infrastructure.EmailVerificationAccountRepository.AccountState

정의: [src/main/java/com/dodamdodam/backend/auth/infrastructure/EmailVerificationAccountRepository.java:28](../../../src/main/java/com/dodamdodam/backend/auth/infrastructure/EmailVerificationAccountRepository.java#L28) · RECORD

공개 목적 적격 판정에 필요한 최소 계정 상태다.

<a id="m96"></a>
### AccountState 생성자 — 자동 생성

```java
public void <init>(java.util.UUID id, java.lang.String email, long sessionVersion)
```

호출자: [auth.infrastructure.EmailVerificationAccountRepository.findForUpdate](#m92)

내부 호출: 없음

외부 라이브러리 호출: `java.lang.Record.<init>()`

<a id="m97"></a>
### email — 자동 생성

```java
public java.lang.String email()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m98"></a>
### equals — 자동 생성

```java
public final boolean equals(java.lang.Object o)
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m99"></a>
### hashCode — 자동 생성

```java
public final int hashCode()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m100"></a>
### id — 자동 생성

```java
public java.util.UUID id()
```

호출자: [auth.application.EmailVerificationIssuanceService.preparePublic](#m20)

내부 호출: 없음

<a id="m101"></a>
### sessionVersion — 자동 생성

```java
public long sessionVersion()
```

호출자: [auth.application.EmailVerificationIssuanceService.preparePublic](#m20)

내부 호출: 없음

<a id="m102"></a>
### toString — 자동 생성

```java
public final java.lang.String toString()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="c22"></a>
## auth.infrastructure.EmailVerificationAccountRepository.PinState

정의: [src/main/java/com/dodamdodam/backend/auth/infrastructure/EmailVerificationAccountRepository.java:50](../../../src/main/java/com/dodamdodam/backend/auth/infrastructure/EmailVerificationAccountRepository.java#L50) · RECORD

<a id="m95"></a>
### isSessionCurrent

```java
public boolean isSessionCurrent()
```

정의: [src/main/java/com/dodamdodam/backend/auth/infrastructure/EmailVerificationAccountRepository.java:53](../../../src/main/java/com/dodamdodam/backend/auth/infrastructure/EmailVerificationAccountRepository.java#L53)

NULL expiry에서도 폐기·계정 버전 검사를 생략하지 않는다.

호출자: [auth.application.EmailVerificationIssuanceService.preparePin](#m21) / [auth.application.EmailVerificationIssuanceService.verifyResponseAccess](#m22)

내부 호출: 없음

외부 라이브러리 호출: `java.time.OffsetDateTime.isBefore(java.time.OffsetDateTime)`

<a id="m103"></a>
### PinState 생성자 — 자동 생성

```java
public void <init>(java.lang.String email, long accountVersion, long contextVersion, long setupGeneration, java.time.OffsetDateTime revokedAt, java.time.OffsetDateTime expiresAt, java.lang.Long pinVersion, java.time.OffsetDateTime checkedAt)
```

호출자: [auth.infrastructure.EmailVerificationAccountRepository.findPinState](#m94)

내부 호출: 없음

외부 라이브러리 호출: `java.lang.Record.<init>()`

<a id="m104"></a>
### accountVersion — 자동 생성

```java
public long accountVersion()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m105"></a>
### checkedAt — 자동 생성

```java
public java.time.OffsetDateTime checkedAt()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m106"></a>
### contextVersion — 자동 생성

```java
public long contextVersion()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m107"></a>
### email — 자동 생성

```java
public java.lang.String email()
```

호출자: [auth.application.EmailVerificationIssuanceService.preparePin](#m21) / [auth.application.EmailVerificationIssuanceService.verifyResponseAccess](#m22)

내부 호출: 없음

<a id="m108"></a>
### equals — 자동 생성

```java
public final boolean equals(java.lang.Object o)
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m109"></a>
### expiresAt — 자동 생성

```java
public java.time.OffsetDateTime expiresAt()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m110"></a>
### hashCode — 자동 생성

```java
public final int hashCode()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m111"></a>
### pinVersion — 자동 생성

```java
public java.lang.Long pinVersion()
```

호출자: [auth.application.EmailVerificationIssuanceService.preparePin](#m21) / [auth.application.EmailVerificationIssuanceService.verifyResponseAccess](#m22)

내부 호출: 없음

<a id="m112"></a>
### revokedAt — 자동 생성

```java
public java.time.OffsetDateTime revokedAt()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m113"></a>
### setupGeneration — 자동 생성

```java
public long setupGeneration()
```

호출자: [auth.application.EmailVerificationIssuanceService.preparePin](#m21) / [auth.application.EmailVerificationIssuanceService.verifyResponseAccess](#m22)

내부 호출: 없음

<a id="m114"></a>
### toString — 자동 생성

```java
public final java.lang.String toString()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="c23"></a>
## auth.infrastructure.EmailVerificationCodeHasher

정의: [src/main/java/com/dodamdodam/backend/auth/infrastructure/EmailVerificationCodeHasher.java:10](../../../src/main/java/com/dodamdodam/backend/auth/infrastructure/EmailVerificationCodeHasher.java#L10) · CLASS

<a id="m115"></a>
### EmailVerificationCodeHasher 생성자

```java
public void <init>(byte[] key)
```

정의: [src/main/java/com/dodamdodam/backend/auth/infrastructure/EmailVerificationCodeHasher.java:17](../../../src/main/java/com/dodamdodam/backend/auth/infrastructure/EmailVerificationCodeHasher.java#L17)

외부 주입한 이메일 인증 전용 32바이트 키만 받으며 임의 대체 키를 만들지 않는다.

호출자: [auth.infrastructure.EmailVerificationKeys.EmailVerificationKeys](#m117)

내부 호출: [auth.session.AuthStateUnavailableException.AuthStateUnavailableException](#m163)

외부 라이브러리 호출: `java.lang.Object.<init>()` / `javax.crypto.spec.SecretKeySpec.<init>(byte[],java.lang.String)`

<a id="m116"></a>
### hash

```java
public byte[] hash(java.lang.String code)
```

정의: [src/main/java/com/dodamdodam/backend/auth/infrastructure/EmailVerificationCodeHasher.java:26](../../../src/main/java/com/dodamdodam/backend/auth/infrastructure/EmailVerificationCodeHasher.java#L26)

코드 원문을 보정·보관하지 않고 DB code_hash용 32바이트 HMAC을 반환한다.

호출자: [auth.application.EmailVerificationIssuanceService.issue](#m19)

내부 호출: [auth.session.AuthStateUnavailableException.AuthStateUnavailableException](#m164)

외부 라이브러리 호출: `java.lang.IllegalArgumentException.<init>(java.lang.String)` / `java.lang.String.getBytes(java.nio.charset.Charset)` / `java.util.regex.Matcher.matches()` / `java.util.regex.Pattern.matcher(java.lang.CharSequence)` / `javax.crypto.Mac.doFinal(byte[])` / `javax.crypto.Mac.getInstance(java.lang.String)` / `javax.crypto.Mac.init(java.security.Key)`

<a id="c24"></a>
## auth.infrastructure.EmailVerificationKeys

정의: [src/main/java/com/dodamdodam/backend/auth/infrastructure/EmailVerificationKeys.java:12](../../../src/main/java/com/dodamdodam/backend/auth/infrastructure/EmailVerificationKeys.java#L12) · CLASS

설정: `@org.springframework.stereotype.Component`

<a id="m117"></a>
### EmailVerificationKeys 생성자

```java
public void <init>(org.springframework.core.env.Environment environment)
```

정의: [src/main/java/com/dodamdodam/backend/auth/infrastructure/EmailVerificationKeys.java:18](../../../src/main/java/com/dodamdodam/backend/auth/infrastructure/EmailVerificationKeys.java#L18)

프로세스 시작 시 전용 파일을 한 번 읽고 미설정 기능만 안전하게 닫는다.

호출자: 정적 Java 호출 없음

내부 호출: [auth.infrastructure.AuthRateLimitKeyHasher.AuthRateLimitKeyHasher](#m73) / [auth.infrastructure.EmailVerificationCodeHasher.EmailVerificationCodeHasher](#m115) / [auth.infrastructure.EmailVerificationKeys.readKey](#m120)

외부 라이브러리 호출: `java.lang.Object.<init>()` / `java.security.MessageDigest.isEqual(byte[],byte[])` / `java.util.Arrays.fill(byte[],byte)` / `org.springframework.core.env.PropertyResolver.getProperty(java.lang.String,java.lang.String)`

<a id="m118"></a>
### rateHasher

```java
public com.dodamdodam.backend.auth.infrastructure.AuthRateLimitKeyHasher rateHasher()
```

정의: [src/main/java/com/dodamdodam/backend/auth/infrastructure/EmailVerificationKeys.java:33](../../../src/main/java/com/dodamdodam/backend/auth/infrastructure/EmailVerificationKeys.java#L33)

전역 제한 키가 없으면 무제한 처리를 허용하지 않는다.

호출자: [auth.application.EmailVerificationIssuanceService.issue](#m19) / [global.web.GlobalApiRateLimitFilter.doFilterInternal](#m517)

내부 호출: [auth.session.AuthStateUnavailableException.AuthStateUnavailableException](#m163)

<a id="m119"></a>
### codeHasher

```java
public com.dodamdodam.backend.auth.infrastructure.EmailVerificationCodeHasher codeHasher()
```

정의: [src/main/java/com/dodamdodam/backend/auth/infrastructure/EmailVerificationKeys.java:41](../../../src/main/java/com/dodamdodam/backend/auth/infrastructure/EmailVerificationKeys.java#L41)

번호 키가 없거나 제한 키와 같으면 발급을 거부한다.

호출자: [auth.application.EmailVerificationIssuanceService.issue](#m19)

내부 호출: [auth.session.AuthStateUnavailableException.AuthStateUnavailableException](#m163)

<a id="m120"></a>
### readKey

```java
private static byte[] readKey(java.lang.String file)
```

정의: [src/main/java/com/dodamdodam/backend/auth/infrastructure/EmailVerificationKeys.java:49](../../../src/main/java/com/dodamdodam/backend/auth/infrastructure/EmailVerificationKeys.java#L49)

파일 경로·내용을 예외에 복사하지 않고 raw 32바이트만 읽는다.

호출자: [auth.infrastructure.EmailVerificationKeys.EmailVerificationKeys](#m117)

내부 호출: 없음

외부 라이브러리 호출: `java.io.InputStream.readNBytes(int)` / `java.lang.String.isBlank()` / `java.nio.file.Files.newInputStream(java.nio.file.Path,java.nio.file.OpenOption[])` / `java.nio.file.Path.of(java.lang.String,java.lang.String[])`

<a id="c25"></a>
## auth.infrastructure.EmailVerificationRepository

정의: [src/main/java/com/dodamdodam/backend/auth/infrastructure/EmailVerificationRepository.java:17](../../../src/main/java/com/dodamdodam/backend/auth/infrastructure/EmailVerificationRepository.java#L17) · CLASS

설정: `@org.springframework.stereotype.Repository`

<a id="m121"></a>
### EmailVerificationRepository 생성자

```java
public void <init>(org.springframework.jdbc.core.JdbcTemplate jdbc)
```

정의: [src/main/java/com/dodamdodam/backend/auth/infrastructure/EmailVerificationRepository.java:23](../../../src/main/java/com/dodamdodam/backend/auth/infrastructure/EmailVerificationRepository.java#L23)

기존 email_verifications에 해시와 목적별 권한 snapshot을 저장한다.

호출자: 정적 Java 호출 없음

내부 호출: 없음

외부 라이브러리 호출: `java.lang.Object.<init>()`

<a id="m122"></a>
### replace

```java
public com.dodamdodam.backend.auth.infrastructure.EmailVerificationRepository.IssuedChallenge replace(com.dodamdodam.backend.auth.infrastructure.EmailVerificationRepository.NewChallenge challenge)
```

정의: [src/main/java/com/dodamdodam/backend/auth/infrastructure/EmailVerificationRepository.java:28](../../../src/main/java/com/dodamdodam/backend/auth/infrastructure/EmailVerificationRepository.java#L28)

호출자가 계정·문맥·PIN 검사와 필요한 잠금을 마친 Tx에서 이전 권한을 교체한다.

애너테이션: `@org.springframework.transaction.annotation.Transactional(propagation=MANDATORY)`

호출자: [auth.application.EmailVerificationIssuanceService.preparePin](#m21) / [auth.application.EmailVerificationIssuanceService.preparePublic](#m20)

내부 호출: [auth.infrastructure.EmailVerificationRepository.databaseNow](#m127) / [auth.infrastructure.EmailVerificationRepository.lockScope](#m124) / [auth.infrastructure.EmailVerificationRepository.IssuedChallenge.IssuedChallenge](#m128) / [auth.infrastructure.EmailVerificationRepository.NewChallenge.codeHash](#m135) / [auth.infrastructure.EmailVerificationRepository.NewChallenge.eligible](#m136) / [auth.infrastructure.EmailVerificationRepository.NewChallenge.email](#m137) / [auth.infrastructure.EmailVerificationRepository.NewChallenge.id](#m140) / [auth.infrastructure.EmailVerificationRepository.NewChallenge.pinVersionSnapshot](#m141) / [auth.infrastructure.EmailVerificationRepository.NewChallenge.purpose](#m142) / [auth.infrastructure.EmailVerificationRepository.NewChallenge.resetAccountId](#m143) / [auth.infrastructure.EmailVerificationRepository.NewChallenge.resetSessionVersion](#m144) / [auth.infrastructure.EmailVerificationRepository.NewChallenge.setupAccountId](#m145) / [auth.infrastructure.EmailVerificationRepository.NewChallenge.setupGeneration](#m146) / [auth.infrastructure.EmailVerificationRepository.NewChallenge.setupSessionBinding](#m147) / [auth.session.AuthStateUnavailableException.AuthStateUnavailableException](#m164)

외부 라이브러리 호출: `java.lang.Enum.name()` / `java.time.OffsetDateTime.plusSeconds(long)` / `java.util.ArrayList.<init>(java.util.Collection<? extends E>)` / `java.util.ArrayList.addFirst(E)` / `java.util.ArrayList.toArray()` / `java.util.List.of(E,E)` / `java.util.List.of(E,E,E)` / `java.util.List.toArray()` / `org.springframework.jdbc.core.JdbcTemplate.queryForList(java.lang.String,java.lang.Class<T>,java.lang.@org.jspecify.annotations.Nullable Object @org.jspecify.annotations.Nullable [])` / `org.springframework.jdbc.core.JdbcTemplate.update(java.lang.String,java.lang.@org.jspecify.annotations.Nullable Object @org.jspecify.annotations.Nullable [])`

<a id="m123"></a>
### invalidateUnconsumed

```java
public void invalidateUnconsumed(java.util.UUID challengeId)
```

정의: [src/main/java/com/dodamdodam/backend/auth/infrastructure/EmailVerificationRepository.java:69](../../../src/main/java/com/dodamdodam/backend/auth/infrastructure/EmailVerificationRepository.java#L69)

발송 실패 보상 등의 호출자 Tx에서 해당 발급만 폐기하며 재호출·소비 완료는 그대로 둔다.

애너테이션: `@org.springframework.transaction.annotation.Transactional(propagation=MANDATORY)`

호출자: [auth.application.EmailVerificationDeliveryService.invalidate](#m17)

내부 호출: [auth.infrastructure.EmailVerificationRepository.databaseNow](#m127) / [auth.session.AuthStateUnavailableException.AuthStateUnavailableException](#m164)

외부 라이브러리 호출: `org.springframework.jdbc.core.JdbcTemplate.queryForList(java.lang.String,java.lang.Class<T>,java.lang.@org.jspecify.annotations.Nullable Object @org.jspecify.annotations.Nullable [])` / `org.springframework.jdbc.core.JdbcTemplate.update(java.lang.String,java.lang.@org.jspecify.annotations.Nullable Object @org.jspecify.annotations.Nullable [])`

<a id="m124"></a>
### lockScope

```java
public void lockScope(java.lang.String email, com.dodamdodam.backend.auth.dto.IssueEmailVerificationRequest.Purpose purpose, java.util.UUID accountId, java.util.UUID contextId)
```

정의: [src/main/java/com/dodamdodam/backend/auth/infrastructure/EmailVerificationRepository.java:85](../../../src/main/java/com/dodamdodam/backend/auth/infrastructure/EmailVerificationRepository.java#L85)

최초 발급도 잠근 뒤 계정 판정과 실제 발급 간격을 검사할 수 있게 한다.

애너테이션: `@org.springframework.transaction.annotation.Transactional(propagation=MANDATORY)`

호출자: [auth.application.EmailVerificationIssuanceService.preparePin](#m21) / [auth.application.EmailVerificationIssuanceService.preparePublic](#m20) / [auth.infrastructure.EmailVerificationRepository.replace](#m122)

내부 호출: 없음

외부 라이브러리 호출: `java.lang.Enum.name()` / `org.springframework.jdbc.core.JdbcTemplate.queryForObject(java.lang.String,org.springframework.jdbc.core.RowMapper<T>,java.lang.@org.jspecify.annotations.Nullable Object @org.jspecify.annotations.Nullable [])`

<a id="m125"></a>
### requireResendReady

```java
public void requireResendReady(java.lang.String email, com.dodamdodam.backend.auth.dto.IssueEmailVerificationRequest.Purpose purpose, java.util.UUID accountId, java.util.UUID contextId)
```

정의: [src/main/java/com/dodamdodam/backend/auth/infrastructure/EmailVerificationRepository.java:95](../../../src/main/java/com/dodamdodam/backend/auth/infrastructure/EmailVerificationRepository.java#L95)

scope와 상위 업무 row 잠금 뒤 최신 발급+60초를 재검사해 사전 예약 대기시간을 보완한다.

애너테이션: `@org.springframework.transaction.annotation.Transactional(propagation=MANDATORY)`

호출자: [auth.application.EmailVerificationIssuanceService.preparePin](#m21) / [auth.application.EmailVerificationIssuanceService.preparePublic](#m20)

내부 호출: [auth.exception.AuthRateLimitException.AuthRateLimitException](#m59) / [auth.infrastructure.EmailVerificationRepository.databaseNow](#m127)

외부 라이브러리 호출: `java.lang.Enum.name()` / `java.time.Duration.between(java.time.temporal.Temporal,java.time.temporal.Temporal)` / `java.time.Duration.getNano()` / `java.time.Duration.getSeconds()` / `java.time.OffsetDateTime.isBefore(java.time.OffsetDateTime)` / `java.time.OffsetDateTime.plusSeconds(long)` / `java.util.List.getFirst()` / `java.util.List.isEmpty()` / `org.springframework.jdbc.core.JdbcTemplate.queryForList(java.lang.String,java.lang.Class<T>,java.lang.@org.jspecify.annotations.Nullable Object @org.jspecify.annotations.Nullable [])`

<a id="m126"></a>
### isIssueResponseCurrent

```java
public boolean isIssueResponseCurrent(java.util.UUID challengeId)
```

정의: [src/main/java/com/dodamdodam/backend/auth/infrastructure/EmailVerificationRepository.java:113](../../../src/main/java/com/dodamdodam/backend/auth/infrastructure/EmailVerificationRepository.java#L113)

응답용 최신 상태만 검사하며 eligible·번호 검증 권한을 대신하지 않는다.

호출자: [auth.application.EmailVerificationIssuanceService.verifyResponseAccess](#m22)

내부 호출: 없음

외부 라이브러리 호출: `java.lang.Boolean.equals(java.lang.Object)` / `java.util.List.getFirst()` / `java.util.List.isEmpty()` / `org.springframework.jdbc.core.JdbcTemplate.queryForList(java.lang.String,java.lang.Class<T>,java.lang.@org.jspecify.annotations.Nullable Object @org.jspecify.annotations.Nullable [])`

<a id="m127"></a>
### databaseNow

```java
private java.time.OffsetDateTime databaseNow()
```

정의: [src/main/java/com/dodamdodam/backend/auth/infrastructure/EmailVerificationRepository.java:122](../../../src/main/java/com/dodamdodam/backend/auth/infrastructure/EmailVerificationRepository.java#L122)

트랜잭션 시작 시각 대신 모든 잠금 대기가 끝난 현재 DB 시각을 읽는다.

호출자: [auth.infrastructure.EmailVerificationRepository.invalidateUnconsumed](#m123) / [auth.infrastructure.EmailVerificationRepository.replace](#m122) / [auth.infrastructure.EmailVerificationRepository.requireResendReady](#m125)

내부 호출: 없음

외부 라이브러리 호출: `org.springframework.jdbc.core.JdbcTemplate.queryForObject(java.lang.String,java.lang.Class<T>)`

<a id="c26"></a>
## auth.infrastructure.EmailVerificationRepository.NewChallenge

정의: [src/main/java/com/dodamdodam/backend/auth/infrastructure/EmailVerificationRepository.java:127](../../../src/main/java/com/dodamdodam/backend/auth/infrastructure/EmailVerificationRepository.java#L127) · RECORD

서비스가 판정한 발급 값이다. 번호 원문 없이 기존 스키마의 목적별 결합 값만 받는다.

<a id="m134"></a>
### NewChallenge 생성자 — 자동 생성

```java
public void <init>(java.util.UUID id, java.lang.String email, com.dodamdodam.backend.auth.dto.IssueEmailVerificationRequest.Purpose purpose, boolean eligible, byte[] codeHash, java.util.UUID setupAccountId, java.util.UUID setupSessionBinding, java.lang.Long setupGeneration, java.lang.Long pinVersionSnapshot, java.util.UUID resetAccountId, java.lang.Long resetSessionVersion)
```

호출자: [auth.application.EmailVerificationIssuanceService.preparePin](#m21) / [auth.application.EmailVerificationIssuanceService.preparePublic](#m20)

내부 호출: 없음

외부 라이브러리 호출: `java.lang.Record.<init>()`

<a id="m135"></a>
### codeHash — 자동 생성

```java
public byte[] codeHash()
```

호출자: [auth.infrastructure.EmailVerificationRepository.replace](#m122)

내부 호출: 없음

<a id="m136"></a>
### eligible — 자동 생성

```java
public boolean eligible()
```

호출자: [auth.application.EmailVerificationIssuanceService.issue](#m19) / [auth.infrastructure.EmailVerificationRepository.replace](#m122)

내부 호출: 없음

<a id="m137"></a>
### email — 자동 생성

```java
public java.lang.String email()
```

호출자: [auth.application.EmailVerificationIssuanceService.issue](#m19) / [auth.application.EmailVerificationIssuanceService.verifyResponseAccess](#m22) / [auth.infrastructure.EmailVerificationRepository.replace](#m122)

내부 호출: 없음

<a id="m138"></a>
### equals — 자동 생성

```java
public final boolean equals(java.lang.Object o)
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m139"></a>
### hashCode — 자동 생성

```java
public final int hashCode()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m140"></a>
### id — 자동 생성

```java
public java.util.UUID id()
```

호출자: [auth.application.EmailVerificationIssuanceService.verifyResponseAccess](#m22) / [auth.infrastructure.EmailVerificationRepository.replace](#m122)

내부 호출: 없음

<a id="m141"></a>
### pinVersionSnapshot — 자동 생성

```java
public java.lang.Long pinVersionSnapshot()
```

호출자: [auth.application.EmailVerificationIssuanceService.verifyResponseAccess](#m22) / [auth.infrastructure.EmailVerificationRepository.replace](#m122)

내부 호출: 없음

<a id="m142"></a>
### purpose — 자동 생성

```java
public com.dodamdodam.backend.auth.dto.IssueEmailVerificationRequest.Purpose purpose()
```

호출자: [auth.application.EmailVerificationIssuanceService.verifyResponseAccess](#m22) / [auth.infrastructure.EmailVerificationRepository.replace](#m122)

내부 호출: 없음

<a id="m143"></a>
### resetAccountId — 자동 생성

```java
public java.util.UUID resetAccountId()
```

호출자: [auth.infrastructure.EmailVerificationRepository.replace](#m122)

내부 호출: 없음

<a id="m144"></a>
### resetSessionVersion — 자동 생성

```java
public java.lang.Long resetSessionVersion()
```

호출자: [auth.infrastructure.EmailVerificationRepository.replace](#m122)

내부 호출: 없음

<a id="m145"></a>
### setupAccountId — 자동 생성

```java
public java.util.UUID setupAccountId()
```

호출자: [auth.application.EmailVerificationIssuanceService.verifyResponseAccess](#m22) / [auth.infrastructure.EmailVerificationRepository.replace](#m122)

내부 호출: 없음

<a id="m146"></a>
### setupGeneration — 자동 생성

```java
public java.lang.Long setupGeneration()
```

호출자: [auth.application.EmailVerificationIssuanceService.verifyResponseAccess](#m22) / [auth.infrastructure.EmailVerificationRepository.replace](#m122)

내부 호출: 없음

<a id="m147"></a>
### setupSessionBinding — 자동 생성

```java
public java.util.UUID setupSessionBinding()
```

호출자: [auth.application.EmailVerificationIssuanceService.verifyResponseAccess](#m22) / [auth.infrastructure.EmailVerificationRepository.replace](#m122)

내부 호출: 없음

<a id="m148"></a>
### toString — 자동 생성

```java
public final java.lang.String toString()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="c27"></a>
## auth.infrastructure.EmailVerificationRepository.IssuedChallenge

정의: [src/main/java/com/dodamdodam/backend/auth/infrastructure/EmailVerificationRepository.java:132](../../../src/main/java/com/dodamdodam/backend/auth/infrastructure/EmailVerificationRepository.java#L132) · RECORD

응답·메일 처리에 필요한 공개 challenge 식별자와 번호 만료 시각이다.

<a id="m128"></a>
### IssuedChallenge 생성자 — 자동 생성

```java
public void <init>(java.util.UUID challengeId, java.time.OffsetDateTime expiresAt)
```

호출자: [auth.infrastructure.EmailVerificationRepository.replace](#m122)

내부 호출: 없음

외부 라이브러리 호출: `java.lang.Record.<init>()`

<a id="m129"></a>
### challengeId — 자동 생성

```java
public java.util.UUID challengeId()
```

호출자: [auth.application.EmailVerificationIssuanceService.issue](#m19)

내부 호출: 없음

<a id="m130"></a>
### equals — 자동 생성

```java
public final boolean equals(java.lang.Object o)
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m131"></a>
### expiresAt — 자동 생성

```java
public java.time.OffsetDateTime expiresAt()
```

호출자: [auth.application.EmailVerificationIssuanceService.issue](#m19)

내부 호출: 없음

<a id="m132"></a>
### hashCode — 자동 생성

```java
public final int hashCode()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m133"></a>
### toString — 자동 생성

```java
public final java.lang.String toString()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="c28"></a>
## auth.infrastructure.SmtpEmailVerificationSender

정의: [src/main/java/com/dodamdodam/backend/auth/infrastructure/SmtpEmailVerificationSender.java:13](../../../src/main/java/com/dodamdodam/backend/auth/infrastructure/SmtpEmailVerificationSender.java#L13) · CLASS

설정: `@org.springframework.stereotype.Component`

<a id="m149"></a>
### SmtpEmailVerificationSender 생성자

```java
public void <init>(org.springframework.core.env.Environment environment)
```

정의: [src/main/java/com/dodamdodam/backend/auth/infrastructure/SmtpEmailVerificationSender.java:19](../../../src/main/java/com/dodamdodam/backend/auth/infrastructure/SmtpEmailVerificationSender.java#L19)

유한 timeout과 TLS를 적용하며 메일 설정 오류는 다른 API의 기동을 막지 않는다.

호출자: 정적 Java 호출 없음

내부 호출: [auth.infrastructure.SmtpEmailVerificationSender.bool](#m151) / [auth.infrastructure.SmtpEmailVerificationSender.loopback](#m152)

외부 라이브러리 호출: `jakarta.mail.internet.InternetAddress.<init>(java.lang.String,boolean)` / `jakarta.mail.internet.InternetAddress.validate()` / `java.lang.Boolean.toString(boolean)` / `java.lang.IllegalArgumentException.<init>()` / `java.lang.Integer.parseInt(java.lang.String)` / `java.lang.Integer.toString(int)` / `java.lang.Object.<init>()` / `java.lang.String.contains(java.lang.CharSequence)` / `java.lang.String.isBlank()` / `java.lang.String.isEmpty()` / `java.nio.file.Files.readString(java.nio.file.Path,java.nio.charset.Charset)` / `java.nio.file.Path.of(java.lang.String,java.lang.String[])` / `java.util.Properties.setProperty(java.lang.String,java.lang.String)` / `org.springframework.core.env.PropertyResolver.getProperty(java.lang.String,java.lang.String)` / `org.springframework.mail.javamail.JavaMailSenderImpl.<init>()` / `org.springframework.mail.javamail.JavaMailSenderImpl.getJavaMailProperties()` / `org.springframework.mail.javamail.JavaMailSenderImpl.setDefaultEncoding(java.lang.@org.jspecify.annotations.Nullable String)` / `org.springframework.mail.javamail.JavaMailSenderImpl.setHost(java.lang.@org.jspecify.annotations.Nullable String)` / `org.springframework.mail.javamail.JavaMailSenderImpl.setPassword(java.lang.@org.jspecify.annotations.Nullable String)` / `org.springframework.mail.javamail.JavaMailSenderImpl.setPort(int)` / `org.springframework.mail.javamail.JavaMailSenderImpl.setUsername(java.lang.@org.jspecify.annotations.Nullable String)`

<a id="m150"></a>
### send

```java
public void send(java.lang.String email, java.lang.String code, boolean eligible)
```

정의: [src/main/java/com/dodamdodam/backend/auth/infrastructure/SmtpEmailVerificationSender.java:69](../../../src/main/java/com/dodamdodam/backend/auth/infrastructure/SmtpEmailVerificationSender.java#L69)

정상·decoy 모두 같은 SMTP 경로를 사용하며 decoy에는 번호를 넣지 않는다.

호출자: [auth.application.EmailVerificationDeliveryService.deliver](#m16)

내부 호출: 없음

외부 라이브러리 호출: `java.lang.IllegalStateException.<init>(java.lang.String)` / `org.springframework.mail.MailSender.send(org.springframework.mail.SimpleMailMessage)` / `org.springframework.mail.SimpleMailMessage.<init>()` / `org.springframework.mail.SimpleMailMessage.setFrom(java.lang.@org.jspecify.annotations.Nullable String)` / `org.springframework.mail.SimpleMailMessage.setSubject(java.lang.@org.jspecify.annotations.Nullable String)` / `org.springframework.mail.SimpleMailMessage.setText(java.lang.@org.jspecify.annotations.Nullable String)` / `org.springframework.mail.SimpleMailMessage.setTo(java.lang.@org.jspecify.annotations.Nullable String)`

<a id="m151"></a>
### bool

```java
private static boolean bool(org.springframework.core.env.Environment environment, java.lang.String name, boolean fallback)
```

정의: [src/main/java/com/dodamdodam/backend/auth/infrastructure/SmtpEmailVerificationSender.java:83](../../../src/main/java/com/dodamdodam/backend/auth/infrastructure/SmtpEmailVerificationSender.java#L83)

문자열 설정 오류를 false로 바꿔 TLS를 끄지 않는다.

호출자: [auth.infrastructure.SmtpEmailVerificationSender.SmtpEmailVerificationSender](#m149)

내부 호출: 없음

외부 라이브러리 호출: `java.lang.Boolean.parseBoolean(java.lang.String)` / `java.lang.Boolean.toString(boolean)` / `java.lang.IllegalArgumentException.<init>()` / `java.lang.String.equalsIgnoreCase(java.lang.String)` / `org.springframework.core.env.PropertyResolver.getProperty(java.lang.String,java.lang.String)`

<a id="m152"></a>
### loopback

```java
private static boolean loopback(java.lang.String host)
```

정의: [src/main/java/com/dodamdodam/backend/auth/infrastructure/SmtpEmailVerificationSender.java:92](../../../src/main/java/com/dodamdodam/backend/auth/infrastructure/SmtpEmailVerificationSender.java#L92)

외부 DNS 조회 없이 명시적인 loopback 주소만 테스트용 평문 전송으로 인정한다.

호출자: [auth.infrastructure.SmtpEmailVerificationSender.SmtpEmailVerificationSender](#m149)

내부 호출: 없음

외부 라이브러리 호출: `java.lang.String.equals(java.lang.Object)` / `java.net.InetAddress.isLoopbackAddress()` / `java.net.InetAddress.ofLiteral(java.lang.String)`

<a id="c29"></a>
## auth.session.AuthStateErrorFilter

정의: [src/main/java/com/dodamdodam/backend/auth/session/AuthStateErrorFilter.java:27](../../../src/main/java/com/dodamdodam/backend/auth/session/AuthStateErrorFilter.java#L27) · CLASS

설정: `@org.springframework.stereotype.Component` · `@org.springframework.core.annotation.Order(-2147483647)`

<a id="m153"></a>
### AuthStateErrorFilter 생성자

```java
public void <init>(com.dodamdodam.backend.auth.session.AuthStateErrorHandler errorHandler, com.dodamdodam.backend.auth.session.SessionLifecycleService lifecycle, com.dodamdodam.backend.guardian.application.GuardianAccessService guardian, tools.jackson.databind.json.JsonMapper jsonMapper, com.dodamdodam.backend.auth.application.EmailVerificationIssuanceService email)
```

정의: [src/main/java/com/dodamdodam/backend/auth/session/AuthStateErrorFilter.java:38](../../../src/main/java/com/dodamdodam/backend/auth/session/AuthStateErrorFilter.java#L38)

로그인 보상과 보호자·이메일 발급의 응답 재검사·공통 오류 경로를 구성한다.

호출자: 정적 Java 호출 없음

내부 호출: 없음

외부 라이브러리 호출: `org.springframework.web.filter.OncePerRequestFilter.<init>()`

<a id="m154"></a>
### doFilterInternal

```java
protected void doFilterInternal(jakarta.servlet.http.HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response, jakarta.servlet.FilterChain filterChain)
```

정의: [src/main/java/com/dodamdodam/backend/auth/session/AuthStateErrorFilter.java:48](../../../src/main/java/com/dodamdodam/backend/auth/session/AuthStateErrorFilter.java#L48)

실제 전송 전과 미커밋 종료 시 권한을 확인하고 실패한 성공 응답을 비운다.

애너테이션: `@java.lang.Override`

호출자: 정적 Java 호출 없음

프레임워크 진입: Servlet / Spring 필터 실행

내부 호출: [auth.exception.EmailVerificationException.code](#m65) / [auth.exception.EmailVerificationException.status](#m67) / [auth.session.AuthStateErrorFilter.findAuthStateFailure](#m157) / [auth.session.AuthStateErrorFilter.resetForError](#m158) / [auth.session.AuthStateErrorFilter.verifyResponseAccess](#m156) / [auth.session.AuthStateErrorFilter$anonymous@52.AuthStateErrorFilter$anonymous@52](#m159) / [auth.session.AuthStateErrorHandler.commence](#m161) / [auth.session.AuthStateErrorHandler.unavailable](#m162) / [auth.session.SessionLifecycleService.failLogin](#m198) / [auth.session.SessionLifecycleService.hasPendingLogin](#m197) / [global.error.ApiErrorResponses.write](#m508) / [guardian.exception.GuardianAccessException.code](#m550) / [guardian.exception.GuardianAccessException.status](#m549)

외부 라이브러리 호출: `jakarta.servlet.FilterChain.doFilter(jakarta.servlet.ServletRequest,jakarta.servlet.ServletResponse)` / `jakarta.servlet.ServletResponse.isCommitted()` / `java.lang.Throwable.addSuppressed(java.lang.Throwable)` / `java.lang.Throwable.getMessage()` / `org.springframework.security.authentication.InsufficientAuthenticationException.<init>(java.lang.String)`

<a id="m156"></a>
### verifyResponseAccess

```java
private void verifyResponseAccess(jakarta.servlet.http.HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response)
```

정의: [src/main/java/com/dodamdodam/backend/auth/session/AuthStateErrorFilter.java:94](../../../src/main/java/com/dodamdodam/backend/auth/session/AuthStateErrorFilter.java#L94)

로그인 최종 검증을 유지하고 성공 상태인 보호자·이메일 발급 응답을 DB에서 재검사한다.

호출자: [auth.session.AuthStateErrorFilter.doFilterInternal](#m154) / [auth.session.AuthStateErrorFilter$anonymous@52.onResponseCommitted](#m155)

내부 호출: [auth.application.EmailVerificationIssuanceService.verifyResponseAccess](#m22) / [auth.session.SessionLifecycleService.verifyLogin](#m196) / [guardian.application.GuardianAccessService.verifyResponseAccess](#m543)

외부 라이브러리 호출: `jakarta.servlet.http.HttpServletResponse.getStatus()`

<a id="m157"></a>
### findAuthStateFailure

```java
private java.lang.Throwable findAuthStateFailure(java.lang.Throwable failure)
```

정의: [src/main/java/com/dodamdodam/backend/auth/session/AuthStateErrorFilter.java:104](../../../src/main/java/com/dodamdodam/backend/auth/session/AuthStateErrorFilter.java#L104)

명시적인 인증·보호자 오류와 MVC 밖의 프레임워크 세션 장애를 찾는다.

호출자: [auth.session.AuthStateErrorFilter.doFilterInternal](#m154)

내부 호출: 없음

외부 라이브러리 호출: `java.lang.Throwable.getCause()`

<a id="m158"></a>
### resetForError

```java
private void resetForError(jakarta.servlet.http.HttpServletResponse response)
```

정의: [src/main/java/com/dodamdodam/backend/auth/session/AuthStateErrorFilter.java:118](../../../src/main/java/com/dodamdodam/backend/auth/session/AuthStateErrorFilter.java#L118)

아직 전송하지 않은 성공 본문·전송 길이·위치·인증 쿠키를 공통 헤더와 분리해 제거한다.

호출자: [auth.session.AuthStateErrorFilter.doFilterInternal](#m154)

내부 호출: 없음

외부 라이브러리 호출: `jakarta.servlet.ServletResponse.reset()` / `jakarta.servlet.http.HttpServletResponse.addHeader(java.lang.String,java.lang.String)` / `jakarta.servlet.http.HttpServletResponse.getHeaderNames()` / `jakarta.servlet.http.HttpServletResponse.getHeaders(java.lang.String)` / `java.lang.Iterable.forEach(java.util.function.Consumer<? super T>)` / `java.lang.String.equalsIgnoreCase(java.lang.String)` / `java.lang.String.startsWith(java.lang.String)` / `java.util.Collection.stream()` / `java.util.HashMap.put(K,V)` / `java.util.LinkedHashMap.<init>()` / `java.util.LinkedHashMap.forEach(java.util.function.BiConsumer<? super K,? super V>)` / `java.util.stream.Stream.filter(java.util.function.Predicate<? super T>)` / `java.util.stream.Stream.toList()`

<a id="c30"></a>
## auth.session.AuthStateErrorFilter$anonymous@52

정의: [src/main/java/com/dodamdodam/backend/auth/session/AuthStateErrorFilter.java:52](../../../src/main/java/com/dodamdodam/backend/auth/session/AuthStateErrorFilter.java#L52) · CLASS

<a id="m155"></a>
### onResponseCommitted

```java
protected void onResponseCommitted()
```

정의: [src/main/java/com/dodamdodam/backend/auth/session/AuthStateErrorFilter.java:54](../../../src/main/java/com/dodamdodam/backend/auth/session/AuthStateErrorFilter.java#L54)

native 저장·쿠키 처리 뒤 실제 전송 전에 현재 권한을 확인한다.

애너테이션: `@java.lang.Override`

호출자: 정적 Java 호출 없음

프레임워크 진입: 응답 commit 콜백

내부 호출: [auth.session.AuthStateErrorFilter.verifyResponseAccess](#m156)

<a id="m159"></a>
### 익명 클래스@52 생성자 — 자동 생성

```java
void <init>(jakarta.servlet.http.HttpServletResponse arg0)
```

호출자: [auth.session.AuthStateErrorFilter.doFilterInternal](#m154)

내부 호출: 없음

외부 라이브러리 호출: `org.springframework.security.web.util.OnCommittedResponseWrapper.<init>(jakarta.servlet.http.HttpServletResponse)`

<a id="c31"></a>
## auth.session.AuthStateErrorHandler

정의: [src/main/java/com/dodamdodam/backend/auth/session/AuthStateErrorHandler.java:12](../../../src/main/java/com/dodamdodam/backend/auth/session/AuthStateErrorHandler.java#L12) · CLASS

설정: `@org.springframework.stereotype.Component`

<a id="m160"></a>
### AuthStateErrorHandler 생성자

```java
public void <init>(tools.jackson.databind.json.JsonMapper jsonMapper)
```

정의: [src/main/java/com/dodamdodam/backend/auth/session/AuthStateErrorHandler.java:17](../../../src/main/java/com/dodamdodam/backend/auth/session/AuthStateErrorHandler.java#L17)

호출자: 정적 Java 호출 없음

내부 호출: 없음

외부 라이브러리 호출: `java.lang.Object.<init>()`

<a id="m161"></a>
### commence

```java
public void commence(jakarta.servlet.http.HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response, org.springframework.security.core.AuthenticationException exception)
```

정의: [src/main/java/com/dodamdodam/backend/auth/session/AuthStateErrorHandler.java:22](../../../src/main/java/com/dodamdodam/backend/auth/session/AuthStateErrorHandler.java#L22)

인증이 필요한 요청에 기존 401 오류를 반환한다.

애너테이션: `@java.lang.Override`

호출자: [auth.session.AuthStateErrorFilter.doFilterInternal](#m154)

프레임워크 진입: Spring Security 인증 실패

내부 호출: [global.error.ApiErrorResponses.write](#m508)

<a id="m162"></a>
### unavailable

```java
public void unavailable(jakarta.servlet.http.HttpServletResponse response)
```

정의: [src/main/java/com/dodamdodam/backend/auth/session/AuthStateErrorHandler.java:29](../../../src/main/java/com/dodamdodam/backend/auth/session/AuthStateErrorHandler.java#L29)

인증 상태를 확인할 수 없을 때 기존 503 오류를 반환한다.

호출자: [auth.session.AuthStateErrorFilter.doFilterInternal](#m154)

내부 호출: [global.error.ApiErrorResponses.write](#m508)

<a id="c32"></a>
## auth.session.AuthStateUnavailableException

정의: [src/main/java/com/dodamdodam/backend/auth/session/AuthStateUnavailableException.java:3](../../../src/main/java/com/dodamdodam/backend/auth/session/AuthStateUnavailableException.java#L3) · CLASS

<a id="m163"></a>
### AuthStateUnavailableException 생성자

```java
public void <init>()
```

정의: [src/main/java/com/dodamdodam/backend/auth/session/AuthStateUnavailableException.java:6](../../../src/main/java/com/dodamdodam/backend/auth/session/AuthStateUnavailableException.java#L6)

로그인 저장 결과를 확인할 수 없는 상태를 인증 장애로 표시한다.

호출자: [auth.application.AuthRateLimitService.reserve](#m11) / [auth.application.EmailVerificationDeliveryService.deliver](#m16) / [auth.application.EmailVerificationIssuanceService.issue](#m19) / [auth.infrastructure.AuthRateLimitKeyHasher.AuthRateLimitKeyHasher](#m73) / [auth.infrastructure.AuthRateLimitKeyHasher.addressScope](#m76) / [auth.infrastructure.AuthRateLimitKeyHasher.emailIssueKeys](#m75) / [auth.infrastructure.AuthRateLimitRepository.reserve](#m80) / [auth.infrastructure.EmailVerificationCodeHasher.EmailVerificationCodeHasher](#m115) / [auth.infrastructure.EmailVerificationKeys.codeHasher](#m119) / [auth.infrastructure.EmailVerificationKeys.rateHasher](#m118) / [auth.session.SessionLifecycleService.verifyLogin](#m196) / [global.web.ClientAddressResolver.resolve](#m514)

내부 호출: [auth.session.AuthStateUnavailableException.AuthStateUnavailableException](#m164)

<a id="m164"></a>
### AuthStateUnavailableException 생성자

```java
public void <init>(java.lang.Throwable cause)
```

정의: [src/main/java/com/dodamdodam/backend/auth/session/AuthStateUnavailableException.java:11](../../../src/main/java/com/dodamdodam/backend/auth/session/AuthStateUnavailableException.java#L11)

원인은 보존하되 외부 응답에 사용할 메시지는 고정한다.

호출자: [auth.api.AuthLogoutController.logout](#m4) / [auth.application.AuthRateLimitService.reserve](#m11) / [auth.application.EmailVerificationDeliveryService.invalidate](#m17) / [auth.application.EmailVerificationIssuanceService.issue](#m19) / [auth.application.EmailVerificationIssuanceService.verifyResponseAccess](#m22) / [auth.infrastructure.AuthRateLimitKeyHasher.hash](#m77) / [auth.infrastructure.EmailVerificationCodeHasher.hash](#m116) / [auth.infrastructure.EmailVerificationRepository.invalidateUnconsumed](#m123) / [auth.infrastructure.EmailVerificationRepository.replace](#m122) / [auth.session.AuthStateUnavailableException.AuthStateUnavailableException](#m163) / [auth.session.AuthenticatedSessionService.requireAuthenticatedSession](#m172) / [auth.session.AuthenticatedSessionService.requireStoredSession](#m173) / [auth.session.SessionCookieRenewalFilter.renew](#m189) / [auth.session.SessionLifecycleService.beginLogin](#m193) / [auth.session.SessionLifecycleService.failLogin](#m198) / [auth.session.SessionLifecycleService.verifyLogin](#m196) / [auth.session.SessionSecurityRepository.requireCurrentContextForUpdate](#m206)

내부 호출: 없음

외부 라이브러리 호출: `java.lang.RuntimeException.<init>(java.lang.String,java.lang.Throwable)`

<a id="c33"></a>
## auth.session.AuthenticatedSession

정의: [src/main/java/com/dodamdodam/backend/auth/session/AuthenticatedSession.java:5](../../../src/main/java/com/dodamdodam/backend/auth/session/AuthenticatedSession.java#L5) · RECORD

<a id="m165"></a>
### AuthenticatedSession 생성자 — 자동 생성

```java
public void <init>(java.util.UUID accountId, java.util.UUID securityContextId)
```

호출자: [auth.session.AuthenticatedSessionService.validate](#m175) / [auth.session.SessionLifecycleService.beginLogin](#m193) / [auth.session.SessionSecurityRepository.createLoginContext](#m202)

내부 호출: 없음

외부 라이브러리 호출: `java.lang.Record.<init>()`

<a id="m166"></a>
### accountId — 자동 생성

```java
public java.util.UUID accountId()
```

호출자: [auth.api.AuthLogoutController.logout](#m4) / [auth.application.EmailVerificationIssuanceService.issue](#m19) / [auth.application.EmailVerificationIssuanceService.preparePin](#m21) / [auth.session.LoginSessionAuthenticationStrategy.onAuthentication](#m185) / [auth.session.SessionSecurityRepository.createLoginContext](#m202) / [auth.session.SessionSecurityRepository.discardLoginContext](#m203) / [child.application.ChildService.create](#m225) / [conversation.api.ConversationController.prepareStart](#m284) / [conversation.application.ConversationService.decideStart](#m331) / [conversation.application.ConversationService.replay](#m332) / [conversation.application.ConversationService.verifyStartResponse](#m330) / [guardian.application.GuardianAccessService.requireCurrentAccess](#m544)

내부 호출: 없음

<a id="m167"></a>
### equals — 자동 생성

```java
public final boolean equals(java.lang.Object o)
```

호출자: [auth.session.AuthenticatedSessionService.requireStoredSession](#m173) / [conversation.api.ConversationStartResponseFilter.verify](#m303)

내부 호출: 없음

<a id="m168"></a>
### hashCode — 자동 생성

```java
public final int hashCode()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m169"></a>
### securityContextId — 자동 생성

```java
public java.util.UUID securityContextId()
```

호출자: [auth.api.AuthLogoutController.logout](#m4) / [auth.application.EmailVerificationIssuanceService.issue](#m19) / [auth.application.EmailVerificationIssuanceService.preparePin](#m21) / [auth.session.LoginSessionAuthenticationStrategy.onAuthentication](#m185) / [auth.session.SessionSecurityRepository.createLoginContext](#m202) / [auth.session.SessionSecurityRepository.discardLoginContext](#m203) / [child.application.ChildService.create](#m225) / [conversation.application.ConversationService.decideStart](#m331) / [conversation.application.ConversationService.replay](#m332) / [conversation.application.ConversationService.verifyStartResponse](#m330) / [guardian.application.GuardianAccessService.requireCurrentAccess](#m544)

내부 호출: 없음

<a id="m170"></a>
### toString — 자동 생성

```java
public final java.lang.String toString()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="c34"></a>
## auth.session.AuthenticatedSessionService

정의: [src/main/java/com/dodamdodam/backend/auth/session/AuthenticatedSessionService.java:17](../../../src/main/java/com/dodamdodam/backend/auth/session/AuthenticatedSessionService.java#L17) · CLASS

설정: `@org.springframework.stereotype.Service`

<a id="m171"></a>
### AuthenticatedSessionService 생성자

```java
public void <init>(com.dodamdodam.backend.auth.session.SessionSecurityRepository repository)
```

정의: [src/main/java/com/dodamdodam/backend/auth/session/AuthenticatedSessionService.java:27](../../../src/main/java/com/dodamdodam/backend/auth/session/AuthenticatedSessionService.java#L27)

호출자: 정적 Java 호출 없음

내부 호출: 없음

외부 라이브러리 호출: `java.lang.Object.<init>()`

<a id="m172"></a>
### requireAuthenticatedSession

```java
public com.dodamdodam.backend.auth.session.AuthenticatedSession requireAuthenticatedSession(jakarta.servlet.http.HttpServletRequest request)
```

정의: [src/main/java/com/dodamdodam/backend/auth/session/AuthenticatedSessionService.java:32](../../../src/main/java/com/dodamdodam/backend/auth/session/AuthenticatedSessionService.java#L32)

실제 세션과 업무 문맥을 확인하고 저장소 장애를 인증 무효와 구분한다.

호출자: [auth.api.AuthLogoutController.logout](#m4) / [auth.api.EmailVerificationController.issue](#m6) / [child.api.ChildController.create](#m219) / [conversation.api.ConversationController.prepareStart](#m284) / [conversation.api.ConversationStartResponseFilter.verify](#m303) / [global.config.SecurityConfig.applicationSecurityFilterChain](#m476)

내부 호출: [auth.session.AuthStateUnavailableException.AuthStateUnavailableException](#m164) / [auth.session.AuthenticatedSessionService.markCookieRenewal](#m174) / [auth.session.AuthenticatedSessionService.validate](#m175)

외부 라이브러리 호출: `jakarta.servlet.http.HttpServletRequest.getSession(boolean)` / `jakarta.servlet.http.HttpSession.getAttribute(java.lang.String)` / `jakarta.servlet.http.HttpSession.getId()` / `org.springframework.security.authentication.InsufficientAuthenticationException.<init>(java.lang.String)` / `org.springframework.security.core.context.SecurityContext.getAuthentication()` / `org.springframework.security.core.context.SecurityContextHolder.getContext()`

<a id="m173"></a>
### requireStoredSession

```java
void requireStoredSession(org.springframework.session.Session stored, com.dodamdodam.backend.auth.session.AuthenticatedSession expected)
```

정의: [src/main/java/com/dodamdodam/backend/auth/session/AuthenticatedSessionService.java:50](../../../src/main/java/com/dodamdodam/backend/auth/session/AuthenticatedSessionService.java#L50)

저장된 세션을 다시 검증하며 인증 저장소 확인 실패를 명시적으로 전달한다.

호출자: [auth.session.SessionCookieRenewalFilter.renew](#m189) / [auth.session.SessionLifecycleService.verifyLogin](#m196)

내부 호출: [auth.session.AuthStateUnavailableException.AuthStateUnavailableException](#m164) / [auth.session.AuthenticatedSession.equals](#m167) / [auth.session.AuthenticatedSessionService.validate](#m175)

외부 라이브러리 호출: `org.springframework.security.authentication.InsufficientAuthenticationException.<init>(java.lang.String)` / `org.springframework.security.core.context.SecurityContext.getAuthentication()` / `org.springframework.session.Session.getAttribute(java.lang.String)`

<a id="m174"></a>
### markCookieRenewal

```java
static void markCookieRenewal(jakarta.servlet.http.HttpServletRequest request, java.lang.String sessionId, com.dodamdodam.backend.auth.session.AuthenticatedSession context)
```

정의: [src/main/java/com/dodamdodam/backend/auth/session/AuthenticatedSessionService.java:68](../../../src/main/java/com/dodamdodam/backend/auth/session/AuthenticatedSessionService.java#L68)

호출자: [auth.session.AuthenticatedSessionService.requireAuthenticatedSession](#m172) / [auth.session.SessionLifecycleService.verifyLogin](#m196)

내부 호출: [auth.session.AuthenticatedSessionService.CookieRenewal.CookieRenewal](#m176)

외부 라이브러리 호출: `jakarta.servlet.ServletRequest.setAttribute(java.lang.String,java.lang.Object)`

<a id="m175"></a>
### validate

```java
private com.dodamdodam.backend.auth.session.AuthenticatedSession validate(org.springframework.security.core.Authentication authentication, java.lang.Object account, java.lang.Object context, java.lang.Object security)
```

정의: [src/main/java/com/dodamdodam/backend/auth/session/AuthenticatedSessionService.java:81](../../../src/main/java/com/dodamdodam/backend/auth/session/AuthenticatedSessionService.java#L81)

전달받은 인증·세션 저장 정보·DB 문맥을 대조하고 하나라도 불일치하면 인증을 거부한다.

호출자: [auth.session.AuthenticatedSessionService.requireAuthenticatedSession](#m172) / [auth.session.AuthenticatedSessionService.requireStoredSession](#m173)

내부 호출: [auth.session.AuthenticatedSession.AuthenticatedSession](#m165) / [auth.session.SessionSecurityRepository.isCurrentContext](#m204)

외부 라이브러리 호출: `java.lang.String.equals(java.lang.Object)` / `java.security.Principal.getName()` / `java.util.UUID.toString()` / `org.springframework.security.authentication.AuthenticationTrustResolver.isAuthenticated(org.springframework.security.core.@org.jspecify.annotations.Nullable Authentication)` / `org.springframework.security.authentication.InsufficientAuthenticationException.<init>(java.lang.String)` / `org.springframework.security.core.context.SecurityContext.getAuthentication()`

<a id="c35"></a>
## auth.session.AuthenticatedSessionService.CookieRenewal

정의: [src/main/java/com/dodamdodam/backend/auth/session/AuthenticatedSessionService.java:104](../../../src/main/java/com/dodamdodam/backend/auth/session/AuthenticatedSessionService.java#L104) · RECORD

<a id="m176"></a>
### CookieRenewal 생성자 — 자동 생성

```java
void <init>(java.lang.String sessionId, com.dodamdodam.backend.auth.session.AuthenticatedSession context)
```

호출자: [auth.session.AuthenticatedSessionService.markCookieRenewal](#m174)

내부 호출: 없음

외부 라이브러리 호출: `java.lang.Record.<init>()`

<a id="m177"></a>
### context — 자동 생성

```java
public com.dodamdodam.backend.auth.session.AuthenticatedSession context()
```

호출자: [auth.session.SessionCookieRenewalFilter.renew](#m189)

내부 호출: 없음

<a id="m178"></a>
### equals — 자동 생성

```java
public final boolean equals(java.lang.Object o)
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m179"></a>
### hashCode — 자동 생성

```java
public final int hashCode()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m180"></a>
### sessionId — 자동 생성

```java
public java.lang.String sessionId()
```

호출자: [auth.session.SessionCookieRenewalFilter.renew](#m189)

내부 호출: 없음

<a id="m181"></a>
### toString — 자동 생성

```java
public final java.lang.String toString()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="c36"></a>
## auth.session.LoginSecurityContextRepository

정의: [src/main/java/com/dodamdodam/backend/auth/session/LoginSecurityContextRepository.java:8](../../../src/main/java/com/dodamdodam/backend/auth/session/LoginSecurityContextRepository.java#L8) · CLASS

<a id="m182"></a>
### LoginSecurityContextRepository 생성자

```java
public void <init>(com.dodamdodam.backend.auth.session.SessionLifecycleService lifecycle)
```

정의: [src/main/java/com/dodamdodam/backend/auth/session/LoginSecurityContextRepository.java:12](../../../src/main/java/com/dodamdodam/backend/auth/session/LoginSecurityContextRepository.java#L12)

호출자: [global.config.SessionConfig.securityContextRepository](#m485)

내부 호출: 없음

외부 라이브러리 호출: `org.springframework.security.web.context.HttpSessionSecurityContextRepository.<init>()`

<a id="m183"></a>
### saveContext

```java
public void saveContext(org.springframework.security.core.context.SecurityContext context, jakarta.servlet.http.HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response)
```

정의: [src/main/java/com/dodamdodam/backend/auth/session/LoginSecurityContextRepository.java:16](../../../src/main/java/com/dodamdodam/backend/auth/session/LoginSecurityContextRepository.java#L16)

애너테이션: `@java.lang.Override`

호출자: 정적 Java 호출 없음

프레임워크 진입: Spring Security 문맥 저장

내부 호출: [auth.session.SessionLifecycleService.contextSaved](#m195)

외부 라이브러리 호출: `org.springframework.security.web.context.HttpSessionSecurityContextRepository.saveContext(org.springframework.security.core.context.SecurityContext,jakarta.servlet.http.HttpServletRequest,jakarta.servlet.http.HttpServletResponse)`

<a id="c37"></a>
## auth.session.LoginSessionAuthenticationStrategy

정의: [src/main/java/com/dodamdodam/backend/auth/session/LoginSessionAuthenticationStrategy.java:14](../../../src/main/java/com/dodamdodam/backend/auth/session/LoginSessionAuthenticationStrategy.java#L14) · CLASS

<a id="m184"></a>
### LoginSessionAuthenticationStrategy 생성자

```java
public void <init>(com.dodamdodam.backend.auth.session.SessionLifecycleService lifecycle, org.springframework.security.web.csrf.CookieCsrfTokenRepository csrfRepository, org.springframework.security.web.csrf.CsrfTokenRequestHandler csrfRequestHandler)
```

정의: [src/main/java/com/dodamdodam/backend/auth/session/LoginSessionAuthenticationStrategy.java:20](../../../src/main/java/com/dodamdodam/backend/auth/session/LoginSessionAuthenticationStrategy.java#L20)

호출자: [global.config.SessionConfig.loginSessionAuthenticationStrategy](#m484)

내부 호출: 없음

외부 라이브러리 호출: `java.lang.Object.<init>()` / `org.springframework.security.web.authentication.session.SessionFixationProtectionStrategy.setMigrateSessionAttributes(boolean)` / `org.springframework.security.web.csrf.CsrfAuthenticationStrategy.<init>(org.springframework.security.web.csrf.CsrfTokenRepository)` / `org.springframework.security.web.csrf.CsrfAuthenticationStrategy.setRequestHandler(org.springframework.security.web.csrf.CsrfTokenRequestHandler)`

<a id="m185"></a>
### onAuthentication

```java
public void onAuthentication(org.springframework.security.core.Authentication authentication, jakarta.servlet.http.HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response)
```

정의: [src/main/java/com/dodamdodam/backend/auth/session/LoginSessionAuthenticationStrategy.java:28](../../../src/main/java/com/dodamdodam/backend/auth/session/LoginSessionAuthenticationStrategy.java#L28)

애너테이션: `@java.lang.Override`

호출자: 정적 Java 호출 없음

프레임워크 진입: Spring Security 로그인 성공 전략

내부 호출: [auth.session.AuthenticatedSession.accountId](#m166) / [auth.session.AuthenticatedSession.securityContextId](#m169) / [auth.session.SessionLifecycleService.beginLogin](#m193) / [auth.session.SessionLifecycleService.nativeSessionPrepared](#m194)

외부 라이브러리 호출: `jakarta.servlet.http.HttpServletRequest.getSession(boolean)` / `jakarta.servlet.http.HttpSession.getId()` / `jakarta.servlet.http.HttpSession.removeAttribute(java.lang.String)` / `jakarta.servlet.http.HttpSession.setAttribute(java.lang.String,java.lang.Object)` / `jakarta.servlet.http.HttpSession.setMaxInactiveInterval(int)` / `org.springframework.dao.DataAccessResourceFailureException.<init>(java.lang.@org.jspecify.annotations.Nullable String,java.lang.@org.jspecify.annotations.Nullable Throwable)` / `org.springframework.security.web.authentication.session.AbstractSessionFixationProtectionStrategy.onAuthentication(org.springframework.security.core.Authentication,jakarta.servlet.http.HttpServletRequest,jakarta.servlet.http.HttpServletResponse)` / `org.springframework.security.web.csrf.CsrfAuthenticationStrategy.onAuthentication(org.springframework.security.core.@org.jspecify.annotations.Nullable Authentication,jakarta.servlet.http.HttpServletRequest,jakarta.servlet.http.HttpServletResponse)`

<a id="c38"></a>
## auth.session.SessionCookieRenewalFilter

정의: [src/main/java/com/dodamdodam/backend/auth/session/SessionCookieRenewalFilter.java:22](../../../src/main/java/com/dodamdodam/backend/auth/session/SessionCookieRenewalFilter.java#L22) · CLASS

설정: `@org.springframework.stereotype.Component` · `@org.springframework.core.annotation.Order(-2147483646)`

<a id="m186"></a>
### SessionCookieRenewalFilter 생성자

```java
public void <init>(org.springframework.session.web.http.CookieSerializer cookies, org.springframework.session.jdbc.JdbcIndexedSessionRepository sessions, com.dodamdodam.backend.auth.session.AuthenticatedSessionService authentication)
```

정의: [src/main/java/com/dodamdodam/backend/auth/session/SessionCookieRenewalFilter.java:30](../../../src/main/java/com/dodamdodam/backend/auth/session/SessionCookieRenewalFilter.java#L30)

호출자: 정적 Java 호출 없음

내부 호출: 없음

외부 라이브러리 호출: `org.springframework.web.filter.OncePerRequestFilter.<init>()`

<a id="m187"></a>
### doFilterInternal

```java
protected void doFilterInternal(jakarta.servlet.http.HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response, jakarta.servlet.FilterChain filterChain)
```

정의: [src/main/java/com/dodamdodam/backend/auth/session/SessionCookieRenewalFilter.java:37](../../../src/main/java/com/dodamdodam/backend/auth/session/SessionCookieRenewalFilter.java#L37)

애너테이션: `@java.lang.Override`

호출자: 정적 Java 호출 없음

프레임워크 진입: Servlet / Spring 필터 실행

내부 호출: [auth.session.SessionCookieRenewalFilter.renew](#m189) / [auth.session.SessionCookieRenewalFilter$anonymous@41.SessionCookieRenewalFilter$anonymous@41](#m190)

외부 라이브러리 호출: `jakarta.servlet.FilterChain.doFilter(jakarta.servlet.ServletRequest,jakarta.servlet.ServletResponse)` / `jakarta.servlet.ServletResponse.isCommitted()`

<a id="m189"></a>
### renew

```java
private void renew(jakarta.servlet.http.HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response)
```

정의: [src/main/java/com/dodamdodam/backend/auth/session/SessionCookieRenewalFilter.java:51](../../../src/main/java/com/dodamdodam/backend/auth/session/SessionCookieRenewalFilter.java#L51)

native 세션·업무 문맥 재검사 뒤 쿠키를 갱신하며 확인 장애는 인증 오류로 전달한다.

호출자: [auth.session.SessionCookieRenewalFilter.doFilterInternal](#m187) / [auth.session.SessionCookieRenewalFilter$anonymous@41.onResponseCommitted](#m188)

내부 호출: [auth.session.AuthStateUnavailableException.AuthStateUnavailableException](#m164) / [auth.session.AuthenticatedSessionService.requireStoredSession](#m173) / [auth.session.AuthenticatedSessionService.CookieRenewal.context](#m177) / [auth.session.AuthenticatedSessionService.CookieRenewal.sessionId](#m180) / [auth.session.SessionCookieRenewalFilter.InvalidSessionException.InvalidSessionException](#m191)

외부 라이브러리 호출: `jakarta.servlet.ServletRequest.getAttribute(java.lang.String)` / `jakarta.servlet.ServletRequest.removeAttribute(java.lang.String)` / `jakarta.servlet.http.HttpServletResponse.addHeader(java.lang.String,java.lang.String)` / `jakarta.servlet.http.HttpServletResponse.getHeaders(java.lang.String)` / `jakarta.servlet.http.HttpServletResponse.getStatus()` / `jakarta.servlet.http.HttpServletResponse.setHeader(java.lang.String,java.lang.String)` / `java.lang.Iterable.forEach(java.util.function.Consumer<? super T>)` / `java.lang.String.startsWith(java.lang.String)` / `java.net.HttpCookie.getMaxAge()` / `java.net.HttpCookie.parse(java.lang.String)` / `java.time.Duration.isNegative()` / `java.util.Collection.stream()` / `java.util.stream.Stream.anyMatch(java.util.function.Predicate<? super T>)` / `java.util.stream.Stream.filter(java.util.function.Predicate<? super T>)` / `java.util.stream.Stream.flatMap(java.util.function.Function<? super T,? extends java.util.stream.Stream<? extends R>>)` / `java.util.stream.Stream.toList()` / `org.springframework.session.Session.getMaxInactiveInterval()` / `org.springframework.session.jdbc.JdbcIndexedSessionRepository.findById(java.lang.String)` / `org.springframework.session.web.http.CookieSerializer.writeCookieValue(org.springframework.session.web.http.CookieSerializer.CookieValue)` / `org.springframework.session.web.http.CookieSerializer.CookieValue.<init>(jakarta.servlet.http.HttpServletRequest,jakarta.servlet.http.HttpServletResponse,java.lang.String)` / `org.springframework.session.web.http.CookieSerializer.CookieValue.setCookieMaxAge(int)`

<a id="c39"></a>
## auth.session.SessionCookieRenewalFilter$anonymous@41

정의: [src/main/java/com/dodamdodam/backend/auth/session/SessionCookieRenewalFilter.java:41](../../../src/main/java/com/dodamdodam/backend/auth/session/SessionCookieRenewalFilter.java#L41) · CLASS

<a id="m188"></a>
### onResponseCommitted

```java
protected void onResponseCommitted()
```

정의: [src/main/java/com/dodamdodam/backend/auth/session/SessionCookieRenewalFilter.java:42](../../../src/main/java/com/dodamdodam/backend/auth/session/SessionCookieRenewalFilter.java#L42)

애너테이션: `@java.lang.Override`

호출자: 정적 Java 호출 없음

프레임워크 진입: 응답 commit 콜백

내부 호출: [auth.session.SessionCookieRenewalFilter.renew](#m189)

<a id="m190"></a>
### 익명 클래스@41 생성자 — 자동 생성

```java
void <init>(jakarta.servlet.http.HttpServletResponse arg0)
```

호출자: [auth.session.SessionCookieRenewalFilter.doFilterInternal](#m187)

내부 호출: 없음

외부 라이브러리 호출: `org.springframework.security.web.util.OnCommittedResponseWrapper.<init>(jakarta.servlet.http.HttpServletResponse)`

<a id="c40"></a>
## auth.session.SessionCookieRenewalFilter.InvalidSessionException

정의: [src/main/java/com/dodamdodam/backend/auth/session/SessionCookieRenewalFilter.java:83](../../../src/main/java/com/dodamdodam/backend/auth/session/SessionCookieRenewalFilter.java#L83) · CLASS

MVC가 소비하지 않고 외곽 필터의 본문 초기화·401 변환으로 넘겨야 하는 재검사 실패다.

<a id="m191"></a>
### InvalidSessionException 생성자 — 자동 생성

```java
public void <init>()
```

호출자: [auth.session.SessionCookieRenewalFilter.renew](#m189) / [conversation.api.ConversationStartResponseFilter.doFilterInternal](#m301)

내부 호출: 없음

외부 라이브러리 호출: `java.lang.RuntimeException.<init>()`

<a id="c41"></a>
## auth.session.SessionLifecycleService

정의: [src/main/java/com/dodamdodam/backend/auth/session/SessionLifecycleService.java:17](../../../src/main/java/com/dodamdodam/backend/auth/session/SessionLifecycleService.java#L17) · CLASS

설정: `@org.springframework.stereotype.Service`

<a id="m192"></a>
### SessionLifecycleService 생성자

```java
public void <init>(com.dodamdodam.backend.auth.session.SessionSecurityRepository securityRepository, org.springframework.session.jdbc.JdbcIndexedSessionRepository sessionRepository, com.dodamdodam.backend.auth.session.AuthenticatedSessionService authenticatedSessions)
```

정의: [src/main/java/com/dodamdodam/backend/auth/session/SessionLifecycleService.java:26](../../../src/main/java/com/dodamdodam/backend/auth/session/SessionLifecycleService.java#L26)

호출자: 정적 Java 호출 없음

내부 호출: 없음

외부 라이브러리 호출: `java.lang.Object.<init>()`

<a id="m193"></a>
### beginLogin

```java
com.dodamdodam.backend.auth.session.AuthenticatedSession beginLogin(org.springframework.security.core.Authentication authentication, jakarta.servlet.http.HttpServletRequest request)
```

정의: [src/main/java/com/dodamdodam/backend/auth/session/SessionLifecycleService.java:34](../../../src/main/java/com/dodamdodam/backend/auth/session/SessionLifecycleService.java#L34)

이전 세션을 확인하고 새 로그인 문맥의 생성·커밋 장애를 인증 실패와 구분한다.

호출자: [auth.session.LoginSessionAuthenticationStrategy.onAuthentication](#m185)

내부 호출: [auth.session.AuthStateUnavailableException.AuthStateUnavailableException](#m164) / [auth.session.AuthenticatedSession.AuthenticatedSession](#m165) / [auth.session.SessionLifecycleService.PendingLogin.PendingLogin](#m200) / [auth.session.SessionSecurityRepository.createLoginContext](#m202)

외부 라이브러리 호출: `jakarta.servlet.ServletRequest.setAttribute(java.lang.String,java.lang.Object)` / `jakarta.servlet.http.HttpServletRequest.getSession(boolean)` / `jakarta.servlet.http.HttpSession.getAttribute(java.lang.String)` / `java.lang.IllegalArgumentException.<init>()` / `java.lang.String.equals(java.lang.Object)` / `java.security.Principal.getName()` / `java.util.UUID.fromString(java.lang.String)` / `java.util.UUID.toString()` / `org.springframework.security.authentication.AuthenticationTrustResolver.isAuthenticated(org.springframework.security.core.@org.jspecify.annotations.Nullable Authentication)` / `org.springframework.security.authentication.InsufficientAuthenticationException.<init>(java.lang.String)`

<a id="m194"></a>
### nativeSessionPrepared

```java
void nativeSessionPrepared(jakarta.servlet.http.HttpServletRequest request, java.lang.String sessionId)
```

정의: [src/main/java/com/dodamdodam/backend/auth/session/SessionLifecycleService.java:65](../../../src/main/java/com/dodamdodam/backend/auth/session/SessionLifecycleService.java#L65)

호출자: [auth.session.LoginSessionAuthenticationStrategy.onAuthentication](#m185)

내부 호출: [auth.session.SessionLifecycleService.pending](#m199)

<a id="m195"></a>
### contextSaved

```java
void contextSaved(jakarta.servlet.http.HttpServletRequest request)
```

정의: [src/main/java/com/dodamdodam/backend/auth/session/SessionLifecycleService.java:69](../../../src/main/java/com/dodamdodam/backend/auth/session/SessionLifecycleService.java#L69)

호출자: [auth.session.LoginSecurityContextRepository.saveContext](#m183)

내부 호출: [auth.session.SessionLifecycleService.pending](#m199) / [auth.session.SessionLifecycleService.verifyLogin](#m196)

<a id="m196"></a>
### verifyLogin

```java
public void verifyLogin(jakarta.servlet.http.HttpServletRequest request)
```

정의: [src/main/java/com/dodamdodam/backend/auth/session/SessionLifecycleService.java:78](../../../src/main/java/com/dodamdodam/backend/auth/session/SessionLifecycleService.java#L78)

응답 전에 실제 로그인 저장 결과를 확인하고 불완전한 저장을 503 대상으로 표시한다.

호출자: [auth.session.AuthStateErrorFilter.verifyResponseAccess](#m156) / [auth.session.SessionLifecycleService.contextSaved](#m195)

내부 호출: [auth.session.AuthStateUnavailableException.AuthStateUnavailableException](#m163) / [auth.session.AuthStateUnavailableException.AuthStateUnavailableException](#m164) / [auth.session.AuthenticatedSessionService.markCookieRenewal](#m174) / [auth.session.AuthenticatedSessionService.requireStoredSession](#m173) / [auth.session.SessionLifecycleService.pending](#m199)

외부 라이브러리 호출: `java.time.Duration.isNegative()` / `org.springframework.session.Session.getMaxInactiveInterval()` / `org.springframework.session.jdbc.JdbcIndexedSessionRepository.findById(java.lang.String)`

<a id="m197"></a>
### hasPendingLogin

```java
public boolean hasPendingLogin(jakarta.servlet.http.HttpServletRequest request)
```

정의: [src/main/java/com/dodamdodam/backend/auth/session/SessionLifecycleService.java:99](../../../src/main/java/com/dodamdodam/backend/auth/session/SessionLifecycleService.java#L99)

호출자: [auth.session.AuthStateErrorFilter.doFilterInternal](#m154) / [global.error.CommonApiExceptionHandler.handle](#m510)

내부 호출: [auth.session.SessionLifecycleService.pending](#m199)

<a id="m198"></a>
### failLogin

```java
public void failLogin(jakarta.servlet.http.HttpServletRequest request)
```

정의: [src/main/java/com/dodamdodam/backend/auth/session/SessionLifecycleService.java:104](../../../src/main/java/com/dodamdodam/backend/auth/session/SessionLifecycleService.java#L104)

실패한 새 문맥을 보상 폐기하고 프레임워크 세션도 반드시 정리한다.

호출자: [auth.session.AuthStateErrorFilter.doFilterInternal](#m154)

내부 호출: [auth.session.AuthStateUnavailableException.AuthStateUnavailableException](#m164) / [auth.session.SessionLifecycleService.pending](#m199) / [auth.session.SessionSecurityRepository.discardLoginContext](#m203)

외부 라이브러리 호출: `jakarta.servlet.ServletRequest.removeAttribute(java.lang.String)` / `org.springframework.security.core.context.SecurityContextHolder.clearContext()` / `org.springframework.session.jdbc.JdbcIndexedSessionRepository.deleteById(java.lang.String)`

<a id="m199"></a>
### pending

```java
private com.dodamdodam.backend.auth.session.SessionLifecycleService.PendingLogin pending(jakarta.servlet.http.HttpServletRequest request)
```

정의: [src/main/java/com/dodamdodam/backend/auth/session/SessionLifecycleService.java:126](../../../src/main/java/com/dodamdodam/backend/auth/session/SessionLifecycleService.java#L126)

호출자: [auth.session.SessionLifecycleService.contextSaved](#m195) / [auth.session.SessionLifecycleService.failLogin](#m198) / [auth.session.SessionLifecycleService.hasPendingLogin](#m197) / [auth.session.SessionLifecycleService.nativeSessionPrepared](#m194) / [auth.session.SessionLifecycleService.verifyLogin](#m196)

내부 호출: 없음

외부 라이브러리 호출: `jakarta.servlet.ServletRequest.getAttribute(java.lang.String)`

<a id="c42"></a>
## auth.session.SessionLifecycleService.PendingLogin

정의: [src/main/java/com/dodamdodam/backend/auth/session/SessionLifecycleService.java:130](../../../src/main/java/com/dodamdodam/backend/auth/session/SessionLifecycleService.java#L130) · CLASS

<a id="m200"></a>
### PendingLogin 생성자

```java
void <init>(com.dodamdodam.backend.auth.session.AuthenticatedSession context)
```

정의: [src/main/java/com/dodamdodam/backend/auth/session/SessionLifecycleService.java:135](../../../src/main/java/com/dodamdodam/backend/auth/session/SessionLifecycleService.java#L135)

호출자: [auth.session.SessionLifecycleService.beginLogin](#m193)

내부 호출: 없음

외부 라이브러리 호출: `java.lang.Object.<init>()`

<a id="c43"></a>
## auth.session.SessionSecurityRepository

정의: [src/main/java/com/dodamdodam/backend/auth/session/SessionSecurityRepository.java:14](../../../src/main/java/com/dodamdodam/backend/auth/session/SessionSecurityRepository.java#L14) · CLASS

설정: `@org.springframework.stereotype.Repository`

<a id="m201"></a>
### SessionSecurityRepository 생성자

```java
public void <init>(org.springframework.jdbc.core.JdbcTemplate jdbcTemplate)
```

정의: [src/main/java/com/dodamdodam/backend/auth/session/SessionSecurityRepository.java:19](../../../src/main/java/com/dodamdodam/backend/auth/session/SessionSecurityRepository.java#L19)

호출자: 정적 Java 호출 없음

내부 호출: 없음

외부 라이브러리 호출: `java.lang.Object.<init>()`

<a id="m202"></a>
### createLoginContext

```java
public com.dodamdodam.backend.auth.session.AuthenticatedSession createLoginContext(java.util.UUID accountId, com.dodamdodam.backend.auth.session.AuthenticatedSession previous)
```

정의: [src/main/java/com/dodamdodam/backend/auth/session/SessionSecurityRepository.java:23](../../../src/main/java/com/dodamdodam/backend/auth/session/SessionSecurityRepository.java#L23)

애너테이션: `@org.springframework.transaction.annotation.Transactional(propagation=REQUIRES_NEW)`

호출자: [auth.session.SessionLifecycleService.beginLogin](#m193)

내부 호출: [auth.session.AuthenticatedSession.AuthenticatedSession](#m165) / [auth.session.AuthenticatedSession.accountId](#m166) / [auth.session.AuthenticatedSession.securityContextId](#m169) / [auth.session.SessionSecurityRepository.revokeCurrentContext](#m207)

외부 라이브러리 호출: `java.util.Collection.stream()` / `java.util.Map.get(java.lang.Object)` / `java.util.Optional.orElseThrow(java.util.function.Supplier<? extends X>)` / `java.util.UUID.equals(java.lang.Object)` / `java.util.UUID.randomUUID()` / `java.util.stream.Stream.filter(java.util.function.Predicate<? super T>)` / `java.util.stream.Stream.findFirst()` / `java.util.stream.Stream.map(java.util.function.Function<? super T,? extends R>)` / `org.springframework.jdbc.core.JdbcTemplate.queryForList(java.lang.String,java.lang.@org.jspecify.annotations.Nullable Object @org.jspecify.annotations.Nullable [])` / `org.springframework.jdbc.core.JdbcTemplate.update(java.lang.String,java.lang.@org.jspecify.annotations.Nullable Object @org.jspecify.annotations.Nullable [])` / `org.springframework.security.authentication.InsufficientAuthenticationException.<init>(java.lang.String)`

<a id="m203"></a>
### discardLoginContext

```java
public void discardLoginContext(com.dodamdodam.backend.auth.session.AuthenticatedSession context)
```

정의: [src/main/java/com/dodamdodam/backend/auth/session/SessionSecurityRepository.java:42](../../../src/main/java/com/dodamdodam/backend/auth/session/SessionSecurityRepository.java#L42)

애너테이션: `@org.springframework.transaction.annotation.Transactional(propagation=REQUIRES_NEW)`

호출자: [auth.session.SessionLifecycleService.failLogin](#m198)

내부 호출: [auth.session.AuthenticatedSession.accountId](#m166) / [auth.session.AuthenticatedSession.securityContextId](#m169) / [auth.session.SessionSecurityRepository.invalidatePinCredentials](#m208) / [auth.session.SessionSecurityRepository.revokeCurrentContext](#m207)

외부 라이브러리 호출: `java.sql.ResultSet.getObject(int,java.lang.Class<T>)` / `org.springframework.jdbc.core.JdbcTemplate.queryForObject(java.lang.String,org.springframework.jdbc.core.RowMapper<T>)` / `org.springframework.jdbc.core.JdbcTemplate.update(java.lang.String,java.lang.@org.jspecify.annotations.Nullable Object @org.jspecify.annotations.Nullable [])`

<a id="m204"></a>
### isCurrentContext

```java
public boolean isCurrentContext(java.util.UUID accountId, java.util.UUID securityContextId)
```

정의: [src/main/java/com/dodamdodam/backend/auth/session/SessionSecurityRepository.java:58](../../../src/main/java/com/dodamdodam/backend/auth/session/SessionSecurityRepository.java#L58)

호출자: [auth.session.AuthenticatedSessionService.validate](#m175) / [auth.session.SessionSecurityRepository.requireCurrentContextForUpdate](#m206)

내부 호출: [auth.session.SessionSecurityRepository.SessionValidity.SessionValidity](#m209) / [auth.session.SessionSecurityRepository.SessionValidity.isValid](#m205)

외부 라이브러리 호출: `java.sql.ResultSet.getLong(java.lang.String)` / `java.sql.ResultSet.getObject(java.lang.String,java.lang.Class<T>)` / `java.util.List.getFirst()` / `java.util.List.isEmpty()` / `org.springframework.jdbc.core.JdbcTemplate.query(java.lang.String,org.springframework.jdbc.core.RowMapper<T>,java.lang.@org.jspecify.annotations.Nullable Object @org.jspecify.annotations.Nullable [])`

<a id="m206"></a>
### requireCurrentContextForUpdate

```java
public void requireCurrentContextForUpdate(java.util.UUID accountId, java.util.UUID securityContextId)
```

정의: [src/main/java/com/dodamdodam/backend/auth/session/SessionSecurityRepository.java:81](../../../src/main/java/com/dodamdodam/backend/auth/session/SessionSecurityRepository.java#L81)

호출자의 트랜잭션에서 계정→문맥 순서로 잠그고 현재 유효성을 재검사한다.

애너테이션: `@org.springframework.transaction.annotation.Transactional(propagation=MANDATORY)`

호출자: [auth.application.EmailVerificationIssuanceService.preparePin](#m21) / [child.application.ChildService.create](#m225) / [conversation.application.ConversationService.decideStart](#m331) / [conversation.application.ConversationService.replay](#m332) / [conversation.application.ConversationService.verifyStartResponse](#m330)

내부 호출: [auth.session.AuthStateUnavailableException.AuthStateUnavailableException](#m164) / [auth.session.SessionSecurityRepository.isCurrentContext](#m204)

외부 라이브러리 호출: `java.util.List.isEmpty()` / `org.springframework.jdbc.core.JdbcTemplate.queryForList(java.lang.String,java.lang.Class<T>,java.lang.@org.jspecify.annotations.Nullable Object @org.jspecify.annotations.Nullable [])` / `org.springframework.security.authentication.InsufficientAuthenticationException.<init>(java.lang.String)`

<a id="m207"></a>
### revokeCurrentContext

```java
public boolean revokeCurrentContext(java.util.UUID accountId, java.util.UUID securityContextId)
```

정의: [src/main/java/com/dodamdodam/backend/auth/session/SessionSecurityRepository.java:102](../../../src/main/java/com/dodamdodam/backend/auth/session/SessionSecurityRepository.java#L102)

애너테이션: `@org.springframework.transaction.annotation.Transactional`

호출자: [auth.api.AuthLogoutController.logout](#m4) / [auth.session.SessionSecurityRepository.createLoginContext](#m202) / [auth.session.SessionSecurityRepository.discardLoginContext](#m203)

내부 호출: [auth.session.SessionSecurityRepository.invalidatePinCredentials](#m208)

외부 라이브러리 호출: `java.sql.ResultSet.getObject(int,java.lang.Class<T>)` / `java.util.List.getFirst()` / `java.util.List.isEmpty()` / `org.springframework.jdbc.core.JdbcTemplate.queryForList(java.lang.String,java.lang.Class<T>,java.lang.@org.jspecify.annotations.Nullable Object @org.jspecify.annotations.Nullable [])` / `org.springframework.jdbc.core.JdbcTemplate.queryForObject(java.lang.String,org.springframework.jdbc.core.RowMapper<T>)` / `org.springframework.jdbc.core.JdbcTemplate.update(java.lang.String,java.lang.@org.jspecify.annotations.Nullable Object @org.jspecify.annotations.Nullable [])`

<a id="m208"></a>
### invalidatePinCredentials

```java
private void invalidatePinCredentials(java.util.UUID accountId, java.util.UUID securityContextId, java.time.OffsetDateTime revokedAt)
```

정의: [src/main/java/com/dodamdodam/backend/auth/session/SessionSecurityRepository.java:139](../../../src/main/java/com/dodamdodam/backend/auth/session/SessionSecurityRepository.java#L139)

호출자: [auth.session.SessionSecurityRepository.discardLoginContext](#m203) / [auth.session.SessionSecurityRepository.revokeCurrentContext](#m207)

내부 호출: 없음

외부 라이브러리 호출: `org.springframework.jdbc.core.JdbcTemplate.update(java.lang.String,java.lang.@org.jspecify.annotations.Nullable Object @org.jspecify.annotations.Nullable [])`

<a id="c44"></a>
## auth.session.SessionSecurityRepository.SessionValidity

정의: [src/main/java/com/dodamdodam/backend/auth/session/SessionSecurityRepository.java:72](../../../src/main/java/com/dodamdodam/backend/auth/session/SessionSecurityRepository.java#L72) · RECORD

<a id="m205"></a>
### isValid

```java
boolean isValid()
```

정의: [src/main/java/com/dodamdodam/backend/auth/session/SessionSecurityRepository.java:74](../../../src/main/java/com/dodamdodam/backend/auth/session/SessionSecurityRepository.java#L74)

호출자: [auth.session.SessionSecurityRepository.isCurrentContext](#m204)

내부 호출: 없음

외부 라이브러리 호출: `java.time.OffsetDateTime.isBefore(java.time.OffsetDateTime)`

<a id="m209"></a>
### SessionValidity 생성자 — 자동 생성

```java
void <init>(long sessionVersion, long accountVersion, java.time.OffsetDateTime revokedAt, java.time.OffsetDateTime expiresAt, java.time.OffsetDateTime checkedAt)
```

호출자: [auth.session.SessionSecurityRepository.isCurrentContext](#m204)

내부 호출: 없음

외부 라이브러리 호출: `java.lang.Record.<init>()`

<a id="m210"></a>
### accountVersion — 자동 생성

```java
public long accountVersion()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m211"></a>
### checkedAt — 자동 생성

```java
public java.time.OffsetDateTime checkedAt()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m212"></a>
### equals — 자동 생성

```java
public final boolean equals(java.lang.Object o)
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m213"></a>
### expiresAt — 자동 생성

```java
public java.time.OffsetDateTime expiresAt()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m214"></a>
### hashCode — 자동 생성

```java
public final int hashCode()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m215"></a>
### revokedAt — 자동 생성

```java
public java.time.OffsetDateTime revokedAt()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m216"></a>
### sessionVersion — 자동 생성

```java
public long sessionVersion()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m217"></a>
### toString — 자동 생성

```java
public final java.lang.String toString()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="c45"></a>
## child.api.ChildController

정의: [src/main/java/com/dodamdodam/backend/child/api/ChildController.java:22](../../../src/main/java/com/dodamdodam/backend/child/api/ChildController.java#L22) · CLASS

설정: `@org.springframework.web.bind.annotation.RestController`

<a id="m218"></a>
### ChildController 생성자

```java
public void <init>(com.dodamdodam.backend.auth.session.AuthenticatedSessionService authentication, com.dodamdodam.backend.child.application.ChildService children)
```

정의: [src/main/java/com/dodamdodam/backend/child/api/ChildController.java:29](../../../src/main/java/com/dodamdodam/backend/child/api/ChildController.java#L29)

기존 세션 검증과 자녀 등록 서비스를 연결한다.

호출자: 정적 Java 호출 없음

내부 호출: 없음

외부 라이브러리 호출: `java.lang.Object.<init>()`

<a id="m219"></a>
### create

```java
public org.springframework.http.ResponseEntity<java.util.Map<java.lang.String,com.dodamdodam.backend.child.dto.ChildView>> create(jakarta.servlet.http.HttpServletRequest request, tools.jackson.databind.JsonNode body)
```

정의: [src/main/java/com/dodamdodam/backend/child/api/ChildController.java:35](../../../src/main/java/com/dodamdodam/backend/child/api/ChildController.java#L35)

로그인 계정에 자녀를 등록하고 공개 프로필과 식별 URI를 반환한다.

애너테이션: `@org.springframework.web.bind.annotation.PostMapping({"/api/v1/children"})`

호출자: 정적 Java 호출 없음

프레임워크 진입: POST /api/v1/children

내부 호출: [auth.session.AuthenticatedSessionService.requireAuthenticatedSession](#m172) / [child.application.ChildService.create](#m225) / [child.dto.ChildView.childId](#m243) / [child.dto.CreateChildRequest.fromJson](#m255)

외부 라이브러리 호출: `java.net.URI.create(java.lang.String)` / `java.util.Map.of(K,V)` / `org.springframework.http.ResponseEntity.created(java.net.URI)` / `org.springframework.http.ResponseEntity.BodyBuilder.body(@org.jspecify.annotations.Nullable T)`

<a id="m220"></a>
### invalidJson

```java
public org.springframework.http.ResponseEntity<com.dodamdodam.backend.global.error.ApiErrorResponse> invalidJson(jakarta.servlet.http.HttpServletResponse response)
```

정의: [src/main/java/com/dodamdodam/backend/child/api/ChildController.java:45](../../../src/main/java/com/dodamdodam/backend/child/api/ChildController.java#L45)

원문을 노출하지 않고 이 컨트롤러의 JSON 읽기 오류만 400으로 처리한다.

애너테이션: `@org.springframework.web.bind.annotation.ExceptionHandler({org.springframework.http.converter.HttpMessageNotReadableException.class})`

호출자: 정적 Java 호출 없음

프레임워크 진입: Spring MVC 예외 처리

내부 호출: [child.exception.ChildException.code](#m275) / [child.exception.ChildException.fields](#m276) / [child.exception.ChildException.validation](#m273) / [global.error.ApiErrorResponses.body](#m507)

외부 라이브러리 호출: `java.lang.Throwable.getMessage()` / `org.springframework.http.ResponseEntity.badRequest()` / `org.springframework.http.ResponseEntity.BodyBuilder.body(@org.jspecify.annotations.Nullable T)`

<a id="c46"></a>
## child.api.ChildExceptionHandler

정의: [src/main/java/com/dodamdodam/backend/child/api/ChildExceptionHandler.java:14](../../../src/main/java/com/dodamdodam/backend/child/api/ChildExceptionHandler.java#L14) · CLASS

설정: `@org.springframework.core.annotation.Order(-2147483648)` · `@org.springframework.web.bind.annotation.RestControllerAdvice`

<a id="m221"></a>
### handle

```java
public org.springframework.http.ResponseEntity<com.dodamdodam.backend.global.error.ApiErrorResponse> handle(com.dodamdodam.backend.child.exception.ChildException exception, jakarta.servlet.http.HttpServletResponse response)
```

정의: [src/main/java/com/dodamdodam/backend/child/api/ChildExceptionHandler.java:19](../../../src/main/java/com/dodamdodam/backend/child/api/ChildExceptionHandler.java#L19)

자녀 업무 예외만 공통 오류 응답으로 변환해 다른 기능의 인증 장애와 구분한다.

애너테이션: `@org.springframework.web.bind.annotation.ExceptionHandler({com.dodamdodam.backend.child.exception.ChildException.class})`

호출자: [conversation.api.ConversationStartResponseFilter.doFilterInternal](#m301)

프레임워크 진입: Spring MVC 예외 처리

내부 호출: [child.exception.ChildException.code](#m275) / [child.exception.ChildException.fields](#m276) / [global.error.ApiErrorResponses.body](#m507)

외부 라이브러리 호출: `java.lang.Throwable.getMessage()` / `org.springframework.http.ResponseEntity.status(org.springframework.http.HttpStatusCode)` / `org.springframework.http.ResponseEntity.BodyBuilder.body(@org.jspecify.annotations.Nullable T)`

<a id="m222"></a>
### ChildExceptionHandler 생성자 — 자동 생성

```java
public void <init>()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

외부 라이브러리 호출: `java.lang.Object.<init>()`

<a id="c47"></a>
## child.application.ChildService

정의: [src/main/java/com/dodamdodam/backend/child/application/ChildService.java:18](../../../src/main/java/com/dodamdodam/backend/child/application/ChildService.java#L18) · CLASS

설정: `@org.springframework.stereotype.Service`

<a id="m223"></a>
### ChildService 생성자

```java
public void <init>(com.dodamdodam.backend.child.infrastructure.ChildRepository repository, com.dodamdodam.backend.auth.session.SessionSecurityRepository securityRepository)
```

정의: [src/main/java/com/dodamdodam/backend/child/application/ChildService.java:30](../../../src/main/java/com/dodamdodam/backend/child/application/ChildService.java#L30)

운영 시계와 현재 지원 목록으로 서비스를 구성한다.

애너테이션: `@org.springframework.beans.factory.annotation.Autowired`

호출자: 정적 Java 호출 없음

내부 호출: [child.application.ChildService.ChildService](#m224)

외부 라이브러리 호출: `java.time.Clock.systemUTC()`

<a id="m224"></a>
### ChildService 생성자

```java
void <init>(com.dodamdodam.backend.child.infrastructure.ChildRepository repository, com.dodamdodam.backend.auth.session.SessionSecurityRepository securityRepository, java.time.Clock clock, java.util.Set<java.lang.String> supportedCharacterIds)
```

정의: [src/main/java/com/dodamdodam/backend/child/application/ChildService.java:36](../../../src/main/java/com/dodamdodam/backend/child/application/ChildService.java#L36)

날짜 경계와 지원 목록을 테스트에서 고정할 수 있도록 의존성을 주입한다.

호출자: [child.application.ChildService.ChildService](#m223)

내부 호출: 없음

외부 라이브러리 호출: `java.lang.Object.<init>()` / `java.util.Set.copyOf(java.util.Collection<? extends E>)`

<a id="m225"></a>
### create

```java
public com.dodamdodam.backend.child.dto.ChildView create(com.dodamdodam.backend.auth.session.AuthenticatedSession session, com.dodamdodam.backend.child.dto.CreateChildRequest request)
```

정의: [src/main/java/com/dodamdodam/backend/child/application/ChildService.java:45](../../../src/main/java/com/dodamdodam/backend/child/application/ChildService.java#L45)

입력 검증·현재 문맥 확인·등록을 같은 트랜잭션에서 수행한다.

애너테이션: `@org.springframework.transaction.annotation.Transactional`

호출자: [child.api.ChildController.create](#m219)

내부 호출: [auth.session.AuthenticatedSession.accountId](#m166) / [auth.session.AuthenticatedSession.securityContextId](#m169) / [auth.session.SessionSecurityRepository.requireCurrentContextForUpdate](#m206) / [child.dto.CreateChildRequest.normalized](#m256) / [child.exception.ChildException.limitReached](#m274) / [child.infrastructure.ChildRepository.insert](#m278)

외부 라이브러리 호출: `java.util.Optional.orElseThrow(java.util.function.Supplier<? extends X>)`

<a id="m226"></a>
### requireChildAccess

```java
public com.dodamdodam.backend.child.dto.ChildView requireChildAccess(java.util.UUID accountId, java.util.UUID childId)
```

정의: [src/main/java/com/dodamdodam/backend/child/application/ChildService.java:55](../../../src/main/java/com/dodamdodam/backend/child/application/ChildService.java#L55)

소유 자녀를 반환하며, 미소유와 미존재를 같은 오류로 처리한다.

호출자: [conversation.api.ConversationController.prepareStart](#m284)

내부 호출: [child.exception.ChildException.notFound](#m272) / [child.infrastructure.ChildRepository.findOwned](#m279)

외부 라이브러리 호출: `java.util.Optional.orElseThrow(java.util.function.Supplier<? extends X>)`

<a id="m227"></a>
### requireChildAccessForUpdate

```java
public com.dodamdodam.backend.child.dto.ChildView requireChildAccessForUpdate(java.util.UUID accountId, java.util.UUID childId)
```

정의: [src/main/java/com/dodamdodam/backend/child/application/ChildService.java:60](../../../src/main/java/com/dodamdodam/backend/child/application/ChildService.java#L60)

계정·보안 문맥 잠금을 먼저 획득한 호출자의 트랜잭션에서 소유 자녀를 잠근다.

애너테이션: `@org.springframework.transaction.annotation.Transactional(propagation=MANDATORY)`

호출자: [conversation.application.ConversationClosureCoordinator.reconcile](#m317) / [conversation.application.ConversationService.decideStart](#m331) / [conversation.application.ConversationService.verifyStartResponse](#m330)

내부 호출: [child.exception.ChildException.notFound](#m272) / [child.infrastructure.ChildRepository.findOwnedForUpdate](#m280)

외부 라이브러리 호출: `java.util.Optional.orElseThrow(java.util.function.Supplier<? extends X>)`

<a id="m228"></a>
### getChildContext

```java
public com.dodamdodam.backend.child.dto.ChildContext getChildContext(java.util.UUID childId)
```

정의: [src/main/java/com/dodamdodam/backend/child/application/ChildService.java:66](../../../src/main/java/com/dodamdodam/backend/child/application/ChildService.java#L66)

requireAuthenticatedSession → requireChildAccess 순서로 검증한 뒤 호출하는 내부 조회다.

호출자: 정적 Java 호출 없음

내부 호출: [child.dto.ChildContext.ChildContext](#m229) / [child.dto.ChildView.birthDate](#m241) / [child.dto.ChildView.characterId](#m242) / [child.dto.ChildView.childId](#m243) / [child.dto.ChildView.createdAt](#m244) / [child.dto.ChildView.interests](#m248) / [child.dto.ChildView.name](#m249) / [child.dto.ChildView.nickname](#m250) / [child.exception.ChildException.notFound](#m272) / [child.infrastructure.ChildRepository.findById](#m281)

외부 라이브러리 호출: `java.util.Optional.orElseThrow(java.util.function.Supplier<? extends X>)`

<a id="c48"></a>
## child.dto.ChildContext

정의: [src/main/java/com/dodamdodam/backend/child/dto/ChildContext.java:9](../../../src/main/java/com/dodamdodam/backend/child/dto/ChildContext.java#L9) · RECORD

내부 자녀 문맥. AI 호칭에는 nickname을 매핑하며 name을 자동 전송하지 않는다.

<a id="m229"></a>
### ChildContext 생성자 — 자동 생성

```java
public void <init>(java.util.UUID childId, java.lang.String name, java.lang.String nickname, java.time.LocalDate birthDate, java.util.List<java.lang.String> interests, java.lang.String characterId, java.time.OffsetDateTime createdAt)
```

호출자: [child.application.ChildService.getChildContext](#m228)

내부 호출: 없음

외부 라이브러리 호출: `java.lang.Record.<init>()`

<a id="m230"></a>
### birthDate — 자동 생성

```java
public java.time.LocalDate birthDate()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m231"></a>
### characterId — 자동 생성

```java
public java.lang.String characterId()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m232"></a>
### childId — 자동 생성

```java
public java.util.UUID childId()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m233"></a>
### createdAt — 자동 생성

```java
public java.time.OffsetDateTime createdAt()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m234"></a>
### equals — 자동 생성

```java
public final boolean equals(java.lang.Object o)
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m235"></a>
### hashCode — 자동 생성

```java
public final int hashCode()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m236"></a>
### interests — 자동 생성

```java
public java.util.List<java.lang.String> interests()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m237"></a>
### name — 자동 생성

```java
public java.lang.String name()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m238"></a>
### nickname — 자동 생성

```java
public java.lang.String nickname()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m239"></a>
### toString — 자동 생성

```java
public final java.lang.String toString()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="c49"></a>
## child.dto.ChildView

정의: [src/main/java/com/dodamdodam/backend/child/dto/ChildView.java:9](../../../src/main/java/com/dodamdodam/backend/child/dto/ChildView.java#L9) · RECORD

API에 공개하는 자녀 프로필이며 계정·세션 식별자는 포함하지 않는다.

<a id="m240"></a>
### ChildView 생성자 — 자동 생성

```java
public void <init>(java.util.UUID childId, java.lang.String name, java.lang.String nickname, java.time.LocalDate birthDate, com.dodamdodam.backend.child.dto.ChildView.Gender gender, java.util.List<java.lang.String> interests, java.lang.String characterId, java.time.OffsetDateTime createdAt)
```

호출자: `child.infrastructure.ChildRepository.<initializer>`

내부 호출: 없음

외부 라이브러리 호출: `java.lang.Record.<init>()`

<a id="m241"></a>
### birthDate — 자동 생성

```java
public java.time.LocalDate birthDate()
```

호출자: [child.application.ChildService.getChildContext](#m228)

내부 호출: 없음

<a id="m242"></a>
### characterId — 자동 생성

```java
public java.lang.String characterId()
```

호출자: [child.application.ChildService.getChildContext](#m228)

내부 호출: 없음

<a id="m243"></a>
### childId — 자동 생성

```java
public java.util.UUID childId()
```

호출자: [child.api.ChildController.create](#m219) / [child.application.ChildService.getChildContext](#m228) / [conversation.api.ConversationController.prepareStart](#m284)

내부 호출: 없음

<a id="m244"></a>
### createdAt — 자동 생성

```java
public java.time.OffsetDateTime createdAt()
```

호출자: [child.application.ChildService.getChildContext](#m228)

내부 호출: 없음

<a id="m245"></a>
### equals — 자동 생성

```java
public final boolean equals(java.lang.Object o)
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m246"></a>
### gender — 자동 생성

```java
public com.dodamdodam.backend.child.dto.ChildView.Gender gender()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m247"></a>
### hashCode — 자동 생성

```java
public final int hashCode()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m248"></a>
### interests — 자동 생성

```java
public java.util.List<java.lang.String> interests()
```

호출자: [child.application.ChildService.getChildContext](#m228)

내부 호출: 없음

<a id="m249"></a>
### name — 자동 생성

```java
public java.lang.String name()
```

호출자: [child.application.ChildService.getChildContext](#m228)

내부 호출: 없음

<a id="m250"></a>
### nickname — 자동 생성

```java
public java.lang.String nickname()
```

호출자: [child.application.ChildService.getChildContext](#m228)

내부 호출: 없음

<a id="m251"></a>
### toString — 자동 생성

```java
public final java.lang.String toString()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="c50"></a>
## child.dto.ChildView.Gender

정의: [src/main/java/com/dodamdodam/backend/child/dto/ChildView.java:12](../../../src/main/java/com/dodamdodam/backend/child/dto/ChildView.java#L12) · ENUM

<a id="m252"></a>
### Gender 생성자 — 자동 생성

```java
private void <init>()
```

호출자: `child.dto.ChildView.Gender.<initializer>`

내부 호출: 없음

외부 라이브러리 호출: `java.lang.Enum.<init>(java.lang.String,int)`

<a id="m253"></a>
### valueOf — 자동 생성

```java
public static com.dodamdodam.backend.child.dto.ChildView.Gender valueOf(java.lang.String name)
```

호출자: [child.dto.CreateChildRequest.fromJson](#m255) / `child.infrastructure.ChildRepository.<initializer>`

내부 호출: 없음

<a id="m254"></a>
### values — 자동 생성

```java
public static com.dodamdodam.backend.child.dto.ChildView.Gender[] values()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="c51"></a>
## child.dto.CreateChildRequest

정의: [src/main/java/com/dodamdodam/backend/child/dto/CreateChildRequest.java:15](../../../src/main/java/com/dodamdodam/backend/child/dto/CreateChildRequest.java#L15) · RECORD

<a id="m255"></a>
### fromJson

```java
public static com.dodamdodam.backend.child.dto.CreateChildRequest fromJson(tools.jackson.databind.JsonNode body)
```

정의: [src/main/java/com/dodamdodam/backend/child/dto/CreateChildRequest.java:23](../../../src/main/java/com/dodamdodam/backend/child/dto/CreateChildRequest.java#L23)

JSON 구조와 타입을 검사해 요청을 만들며, 값 정규화는 normalized에서 수행한다.

호출자: [child.api.ChildController.create](#m219)

내부 호출: [child.dto.ChildView.Gender.valueOf](#m253) / [child.dto.CreateChildRequest.CreateChildRequest](#m260) / [child.dto.CreateChildRequest.requiredString](#m257) / [child.exception.ChildException.validation](#m273)

외부 라이브러리 호출: `java.time.LocalDate.parse(java.lang.CharSequence)` / `java.util.ArrayList.<init>()` / `java.util.ArrayList.add(E)` / `java.util.List.copyOf(java.util.Collection<? extends E>)` / `java.util.Set.containsAll(java.util.Collection<?>)` / `java.util.regex.Matcher.matches()` / `java.util.regex.Pattern.matcher(java.lang.CharSequence)` / `tools.jackson.databind.JsonNode.get(int)` / `tools.jackson.databind.JsonNode.get(java.lang.String)` / `tools.jackson.databind.JsonNode.isArray()` / `tools.jackson.databind.JsonNode.isObject()` / `tools.jackson.databind.JsonNode.propertyNames()` / `tools.jackson.databind.JsonNode.size()`

<a id="m256"></a>
### normalized

```java
public com.dodamdodam.backend.child.dto.CreateChildRequest normalized(java.time.Clock clock, java.util.Set<java.lang.String> supportedCharacterIds)
```

정의: [src/main/java/com/dodamdodam/backend/child/dto/CreateChildRequest.java:66](../../../src/main/java/com/dodamdodam/backend/child/dto/CreateChildRequest.java#L66)

생성자로 직접 만든 요청도 검증하며, 저장에 사용할 정규화된 새 객체를 반환한다.

호출자: [child.application.ChildService.create](#m225)

내부 호출: [child.dto.CreateChildRequest.CreateChildRequest](#m260) / [child.dto.CreateChildRequest.normalizedText](#m258) / [child.exception.ChildException.validation](#m273)

외부 라이브러리 호출: `java.time.Clock.withZone(java.time.ZoneId)` / `java.time.LocalDate.isAfter(java.time.chrono.ChronoLocalDate)` / `java.time.LocalDate.now(java.time.Clock)` / `java.time.ZoneId.of(java.lang.String)` / `java.util.ArrayList.<init>()` / `java.util.ArrayList.add(E)` / `java.util.HashSet.<init>()` / `java.util.HashSet.add(E)` / `java.util.List.copyOf(java.util.Collection<? extends E>)` / `java.util.List.size()` / `java.util.Set.contains(java.lang.Object)` / `java.util.regex.Matcher.matches()` / `java.util.regex.Pattern.matcher(java.lang.CharSequence)`

<a id="m257"></a>
### requiredString

```java
private static java.lang.String requiredString(tools.jackson.databind.JsonNode node, java.lang.String field)
```

정의: [src/main/java/com/dodamdodam/backend/child/dto/CreateChildRequest.java:100](../../../src/main/java/com/dodamdodam/backend/child/dto/CreateChildRequest.java#L100)

필수 JSON 문자열을 읽고 누락·null·타입 오류를 해당 필드의 검증 예외로 처리한다.

호출자: [child.dto.CreateChildRequest.fromJson](#m255)

내부 호출: [child.exception.ChildException.validation](#m273)

외부 라이브러리 호출: `tools.jackson.databind.JsonNode.isString()` / `tools.jackson.databind.JsonNode.stringValue()`

<a id="m258"></a>
### normalizedText

```java
private static java.lang.String normalizedText(java.lang.String value, int maximum, java.lang.String field)
```

정의: [src/main/java/com/dodamdodam/backend/child/dto/CreateChildRequest.java:109](../../../src/main/java/com/dodamdodam/backend/child/dto/CreateChildRequest.java#L109)

바깥 Unicode 공백을 제거하고 코드포인트 길이와 저장 가능한 문자 여부를 검사한다.

호출자: [child.dto.CreateChildRequest.normalized](#m256)

내부 호출: [child.dto.CreateChildRequest.isSpace](#m259) / [child.exception.ChildException.validation](#m273)

외부 라이브러리 호출: `java.lang.Character.charCount(int)` / `java.lang.String.codePointAt(int)` / `java.lang.String.codePointBefore(int)` / `java.lang.String.codePointCount(int,int)` / `java.lang.String.codePoints()` / `java.lang.String.length()` / `java.lang.String.substring(int,int)` / `java.util.stream.IntStream.anyMatch(java.util.function.IntPredicate)`

<a id="m259"></a>
### isSpace

```java
private static boolean isSpace(int codePoint)
```

정의: [src/main/java/com/dodamdodam/backend/child/dto/CreateChildRequest.java:137](../../../src/main/java/com/dodamdodam/backend/child/dto/CreateChildRequest.java#L137)

양끝 공백 제거에 사용할 Unicode 공백 기준이다.

호출자: [child.dto.CreateChildRequest.normalizedText](#m258)

내부 호출: 없음

외부 라이브러리 호출: `java.lang.Character.isSpaceChar(int)` / `java.lang.Character.isWhitespace(int)`

<a id="m260"></a>
### CreateChildRequest 생성자 — 자동 생성

```java
public void <init>(java.lang.String name, java.lang.String nickname, java.time.LocalDate birthDate, com.dodamdodam.backend.child.dto.ChildView.Gender gender, java.util.List<java.lang.String> interests, java.lang.String characterId)
```

호출자: [child.dto.CreateChildRequest.fromJson](#m255) / [child.dto.CreateChildRequest.normalized](#m256)

내부 호출: 없음

외부 라이브러리 호출: `java.lang.Record.<init>()`

<a id="m261"></a>
### birthDate — 자동 생성

```java
public java.time.LocalDate birthDate()
```

호출자: [child.infrastructure.ChildRepository.insert](#m278)

내부 호출: 없음

<a id="m262"></a>
### characterId — 자동 생성

```java
public java.lang.String characterId()
```

호출자: [child.infrastructure.ChildRepository.insert](#m278)

내부 호출: 없음

<a id="m263"></a>
### equals — 자동 생성

```java
public final boolean equals(java.lang.Object o)
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m264"></a>
### gender — 자동 생성

```java
public com.dodamdodam.backend.child.dto.ChildView.Gender gender()
```

호출자: [child.infrastructure.ChildRepository.insert](#m278)

내부 호출: 없음

<a id="m265"></a>
### hashCode — 자동 생성

```java
public final int hashCode()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m266"></a>
### interests — 자동 생성

```java
public java.util.List<java.lang.String> interests()
```

호출자: [child.infrastructure.ChildRepository.insert](#m278)

내부 호출: 없음

<a id="m267"></a>
### name — 자동 생성

```java
public java.lang.String name()
```

호출자: [child.infrastructure.ChildRepository.insert](#m278)

내부 호출: 없음

<a id="m268"></a>
### nickname — 자동 생성

```java
public java.lang.String nickname()
```

호출자: [child.infrastructure.ChildRepository.insert](#m278)

내부 호출: 없음

<a id="m269"></a>
### toString — 자동 생성

```java
public final java.lang.String toString()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="c52"></a>
## child.exception.ChildException

정의: [src/main/java/com/dodamdodam/backend/child/exception/ChildException.java:6](../../../src/main/java/com/dodamdodam/backend/child/exception/ChildException.java#L6) · CLASS

<a id="m270"></a>
### ChildException 생성자

```java
public void <init>(java.lang.Throwable cause)
```

정의: [src/main/java/com/dodamdodam/backend/child/exception/ChildException.java:12](../../../src/main/java/com/dodamdodam/backend/child/exception/ChildException.java#L12)

저장 장애의 원인을 보존하고 응답에는 고정된 업무 오류만 전달한다.

호출자: [child.infrastructure.ChildRepository.findById](#m281) / [child.infrastructure.ChildRepository.findOwned](#m279) / [child.infrastructure.ChildRepository.findOwnedForUpdate](#m280) / [child.infrastructure.ChildRepository.insert](#m278)

내부 호출: [child.exception.ChildException.ChildException](#m271)

외부 라이브러리 호출: `java.util.List.of()`

<a id="m271"></a>
### ChildException 생성자

```java
private void <init>(java.lang.String code, java.lang.String message, java.util.List<com.dodamdodam.backend.global.error.ApiErrorResponse.FieldError> fields, java.lang.Throwable cause)
```

정의: [src/main/java/com/dodamdodam/backend/child/exception/ChildException.java:16](../../../src/main/java/com/dodamdodam/backend/child/exception/ChildException.java#L16)

호출자: [child.exception.ChildException.ChildException](#m270) / [child.exception.ChildException.limitReached](#m274) / [child.exception.ChildException.notFound](#m272) / [child.exception.ChildException.validation](#m273)

내부 호출: 없음

외부 라이브러리 호출: `java.lang.RuntimeException.<init>(java.lang.String,java.lang.Throwable)`

<a id="m272"></a>
### notFound

```java
public static com.dodamdodam.backend.child.exception.ChildException notFound()
```

정의: [src/main/java/com/dodamdodam/backend/child/exception/ChildException.java:23](../../../src/main/java/com/dodamdodam/backend/child/exception/ChildException.java#L23)

미소유·미존재 자녀를 같은 오류로 처리한다.

호출자: [child.application.ChildService.getChildContext](#m228) / [child.application.ChildService.requireChildAccess](#m226) / [child.application.ChildService.requireChildAccessForUpdate](#m227) / [conversation.application.ConversationClosureCoordinator.reconcile](#m317) / [conversation.application.ConversationClosureCoordinator.requireState](#m318) / [conversation.application.ConversationService.verifyStartResponse](#m330)

내부 호출: [child.exception.ChildException.ChildException](#m271)

외부 라이브러리 호출: `java.util.List.of()`

<a id="m273"></a>
### validation

```java
public static com.dodamdodam.backend.child.exception.ChildException validation(java.lang.String field)
```

정의: [src/main/java/com/dodamdodam/backend/child/exception/ChildException.java:28](../../../src/main/java/com/dodamdodam/backend/child/exception/ChildException.java#L28)

원본 입력을 노출하지 않고 검증에 실패한 필드만 전달한다.

호출자: [child.api.ChildController.invalidJson](#m220) / [child.dto.CreateChildRequest.fromJson](#m255) / [child.dto.CreateChildRequest.normalized](#m256) / [child.dto.CreateChildRequest.normalizedText](#m258) / [child.dto.CreateChildRequest.requiredString](#m257)

내부 호출: [child.exception.ChildException.ChildException](#m271) / [global.error.ApiErrorResponse.FieldError.FieldError](#m500)

외부 라이브러리 호출: `java.util.List.of(E)`

<a id="m274"></a>
### limitReached

```java
public static com.dodamdodam.backend.child.exception.ChildException limitReached()
```

정의: [src/main/java/com/dodamdodam/backend/child/exception/ChildException.java:34](../../../src/main/java/com/dodamdodam/backend/child/exception/ChildException.java#L34)

계정당 자녀 1명 제한에 해당하는 업무 오류를 만든다.

호출자: [child.application.ChildService.create](#m225)

내부 호출: [child.exception.ChildException.ChildException](#m271)

외부 라이브러리 호출: `java.util.List.of()`

<a id="m275"></a>
### code

```java
public java.lang.String code()
```

정의: [src/main/java/com/dodamdodam/backend/child/exception/ChildException.java:39](../../../src/main/java/com/dodamdodam/backend/child/exception/ChildException.java#L39)

HTTP 처리 계층이 사용할 업무 오류 코드를 반환한다.

호출자: [child.api.ChildController.invalidJson](#m220) / [child.api.ChildExceptionHandler.handle](#m221)

내부 호출: 없음

<a id="m276"></a>
### fields

```java
public java.util.List<com.dodamdodam.backend.global.error.ApiErrorResponse.FieldError> fields()
```

정의: [src/main/java/com/dodamdodam/backend/child/exception/ChildException.java:44](../../../src/main/java/com/dodamdodam/backend/child/exception/ChildException.java#L44)

원본 입력을 포함하지 않는 필드 오류 정보를 반환한다.

호출자: [child.api.ChildController.invalidJson](#m220) / [child.api.ChildExceptionHandler.handle](#m221)

내부 호출: 없음

<a id="c53"></a>
## child.infrastructure.ChildRepository

정의: [src/main/java/com/dodamdodam/backend/child/infrastructure/ChildRepository.java:19](../../../src/main/java/com/dodamdodam/backend/child/infrastructure/ChildRepository.java#L19) · CLASS

설정: `@org.springframework.stereotype.Repository`

<a id="m277"></a>
### ChildRepository 생성자

```java
public void <init>(org.springframework.jdbc.core.JdbcTemplate jdbc)
```

정의: [src/main/java/com/dodamdodam/backend/child/infrastructure/ChildRepository.java:40](../../../src/main/java/com/dodamdodam/backend/child/infrastructure/ChildRepository.java#L40)

기존 JDBC 연결과 예외 변환을 사용한다.

호출자: 정적 Java 호출 없음

내부 호출: 없음

외부 라이브러리 호출: `java.lang.Object.<init>()`

<a id="m278"></a>
### insert

```java
public java.util.Optional<com.dodamdodam.backend.child.dto.ChildView> insert(java.util.UUID accountId, com.dodamdodam.backend.child.dto.CreateChildRequest normalized)
```

정의: [src/main/java/com/dodamdodam/backend/child/infrastructure/ChildRepository.java:45](../../../src/main/java/com/dodamdodam/backend/child/infrastructure/ChildRepository.java#L45)

정규화된 프로필을 저장하며 계정당 중복만 빈 결과로 반환한다.

호출자: [child.application.ChildService.create](#m225)

내부 호출: [child.dto.CreateChildRequest.birthDate](#m261) / [child.dto.CreateChildRequest.characterId](#m262) / [child.dto.CreateChildRequest.gender](#m264) / [child.dto.CreateChildRequest.interests](#m266) / [child.dto.CreateChildRequest.name](#m267) / [child.dto.CreateChildRequest.nickname](#m268) / [child.exception.ChildException.ChildException](#m270)

외부 라이브러리 호출: `java.lang.Enum.name()` / `java.util.Collection.stream()` / `java.util.List.toArray()` / `java.util.UUID.randomUUID()` / `java.util.stream.Stream.findFirst()` / `org.springframework.jdbc.core.JdbcTemplate.query(java.lang.String,org.springframework.jdbc.core.RowMapper<T>,java.lang.@org.jspecify.annotations.Nullable Object @org.jspecify.annotations.Nullable [])` / `org.springframework.jdbc.support.SqlArrayValue.<init>(java.lang.String,java.lang.Object[])`

<a id="m279"></a>
### findOwned

```java
public java.util.Optional<com.dodamdodam.backend.child.dto.ChildView> findOwned(java.util.UUID accountId, java.util.UUID childId)
```

정의: [src/main/java/com/dodamdodam/backend/child/infrastructure/ChildRepository.java:63](../../../src/main/java/com/dodamdodam/backend/child/infrastructure/ChildRepository.java#L63)

계정과 자녀를 함께 조회해 미소유·미존재를 같은 빈 결과로 처리한다.

호출자: [child.application.ChildService.requireChildAccess](#m226)

내부 호출: [child.exception.ChildException.ChildException](#m270)

외부 라이브러리 호출: `java.util.Collection.stream()` / `java.util.stream.Stream.findFirst()` / `org.springframework.jdbc.core.JdbcTemplate.query(java.lang.String,org.springframework.jdbc.core.RowMapper<T>,java.lang.@org.jspecify.annotations.Nullable Object @org.jspecify.annotations.Nullable [])`

<a id="m280"></a>
### findOwnedForUpdate

```java
public java.util.Optional<com.dodamdodam.backend.child.dto.ChildView> findOwnedForUpdate(java.util.UUID accountId, java.util.UUID childId)
```

정의: [src/main/java/com/dodamdodam/backend/child/infrastructure/ChildRepository.java:74](../../../src/main/java/com/dodamdodam/backend/child/infrastructure/ChildRepository.java#L74)

현재 업무 트랜잭션에서 소유 자녀 행을 잠가 자녀별 변경을 직렬화한다.

호출자: [child.application.ChildService.requireChildAccessForUpdate](#m227)

내부 호출: [child.exception.ChildException.ChildException](#m270)

외부 라이브러리 호출: `java.util.Collection.stream()` / `java.util.stream.Stream.findFirst()` / `org.springframework.jdbc.core.JdbcTemplate.query(java.lang.String,org.springframework.jdbc.core.RowMapper<T>,java.lang.@org.jspecify.annotations.Nullable Object @org.jspecify.annotations.Nullable [])`

<a id="m281"></a>
### findById

```java
public java.util.Optional<com.dodamdodam.backend.child.dto.ChildView> findById(java.util.UUID childId)
```

정의: [src/main/java/com/dodamdodam/backend/child/infrastructure/ChildRepository.java:85](../../../src/main/java/com/dodamdodam/backend/child/infrastructure/ChildRepository.java#L85)

선행 소유권 검사를 마친 내부 문맥 조회에 사용할 프로필을 읽는다.

호출자: [child.application.ChildService.getChildContext](#m228)

내부 호출: [child.exception.ChildException.ChildException](#m270)

외부 라이브러리 호출: `java.util.Collection.stream()` / `java.util.stream.Stream.findFirst()` / `org.springframework.jdbc.core.JdbcTemplate.query(java.lang.String,org.springframework.jdbc.core.RowMapper<T>,java.lang.@org.jspecify.annotations.Nullable Object @org.jspecify.annotations.Nullable [])`

<a id="c54"></a>
## conversation.api.ConversationController

정의: [src/main/java/com/dodamdodam/backend/conversation/api/ConversationController.java:21](../../../src/main/java/com/dodamdodam/backend/conversation/api/ConversationController.java#L21) · CLASS

공통 접근 검증을 거친 대화 시작 요청을 실제 생성 트랜잭션에 연결한다.

설정: `@org.springframework.web.bind.annotation.RestController`

<a id="m282"></a>
### ConversationController 생성자

```java
public void <init>(com.dodamdodam.backend.auth.session.AuthenticatedSessionService authentication, com.dodamdodam.backend.child.application.ChildService children, com.dodamdodam.backend.conversation.application.ConversationService conversations)
```

정의: [src/main/java/com/dodamdodam/backend/conversation/api/ConversationController.java:29](../../../src/main/java/com/dodamdodam/backend/conversation/api/ConversationController.java#L29)

기존 로그인·자녀 소유권 정책을 재사용한다.

호출자: 정적 Java 호출 없음

내부 호출: 없음

외부 라이브러리 호출: `java.lang.Object.<init>()`

<a id="m283"></a>
### start

```java
public org.springframework.http.ResponseEntity<java.util.Map<java.lang.String,com.dodamdodam.backend.conversation.dto.StartConversationResult>> start(jakarta.servlet.http.HttpServletRequest request, java.lang.String childId, tools.jackson.databind.JsonNode body)
```

정의: [src/main/java/com/dodamdodam/backend/conversation/api/ConversationController.java:37](../../../src/main/java/com/dodamdodam/backend/conversation/api/ConversationController.java#L37)

신규 생성은 201과 Location, 같은 키 재전송은 기존 시작 정보만 200으로 반환한다.

애너테이션: `@org.springframework.web.bind.annotation.PostMapping({"/api/v1/children/{childId}/conversations"})`

호출자: 정적 Java 호출 없음

프레임워크 진입: POST /api/v1/children/{childId}/conversations

내부 호출: [conversation.api.ConversationController.prepareStart](#m284) / [conversation.api.ConversationController.PreparedStart.childId](#m286) / [conversation.api.ConversationController.PreparedStart.input](#m289) / [conversation.api.ConversationController.PreparedStart.session](#m290) / [conversation.api.ConversationStartResponseFilter.protect](#m299) / [conversation.application.ConversationService.create](#m329) / [conversation.application.ConversationService.StartOutcome.created](#m341) / [conversation.application.ConversationService.StartOutcome.result](#m344) / [conversation.dto.StartConversationRequest.clientRequestId](#m374) / [conversation.dto.StartConversationResult.conversationId](#m379)

외부 라이브러리 호출: `java.net.URI.create(java.lang.String)` / `java.util.Map.of(K,V)` / `org.springframework.http.ResponseEntity.created(java.net.URI)` / `org.springframework.http.ResponseEntity.ok(@org.jspecify.annotations.Nullable T)` / `org.springframework.http.ResponseEntity.BodyBuilder.body(@org.jspecify.annotations.Nullable T)`

<a id="m284"></a>
### prepareStart

```java
public com.dodamdodam.backend.conversation.api.ConversationController.PreparedStart prepareStart(jakarta.servlet.http.HttpServletRequest request, java.lang.String childId, tools.jackson.databind.JsonNode body)
```

정의: [src/main/java/com/dodamdodam/backend/conversation/api/ConversationController.java:53](../../../src/main/java/com/dodamdodam/backend/conversation/api/ConversationController.java#L53)

인증→구조→소유권 순서로 검증한 내부 입력을 준비하며 성공 응답이나 ID를 만들지 않는다.

호출자: [conversation.api.ConversationController.start](#m283)

내부 호출: [auth.session.AuthenticatedSession.accountId](#m166) / [auth.session.AuthenticatedSessionService.requireAuthenticatedSession](#m172) / [child.application.ChildService.requireChildAccess](#m226) / [child.dto.ChildView.childId](#m243) / [conversation.api.ConversationController.PreparedStart.PreparedStart](#m285) / [conversation.dto.StartConversationRequest.fromJson](#m371) / [conversation.dto.StartConversationRequest.parseChildId](#m372)

<a id="c55"></a>
## conversation.api.ConversationController.PreparedStart

정의: [src/main/java/com/dodamdodam/backend/conversation/api/ConversationController.java:63](../../../src/main/java/com/dodamdodam/backend/conversation/api/ConversationController.java#L63) · RECORD

생성 서비스에 전달할 내부 문맥이며 공개 API 응답으로 직렬화하지 않는다.

<a id="m285"></a>
### PreparedStart 생성자 — 자동 생성

```java
public void <init>(com.dodamdodam.backend.auth.session.AuthenticatedSession session, java.util.UUID childId, com.dodamdodam.backend.conversation.dto.StartConversationRequest input)
```

호출자: [conversation.api.ConversationController.prepareStart](#m284)

내부 호출: 없음

외부 라이브러리 호출: `java.lang.Record.<init>()`

<a id="m286"></a>
### childId — 자동 생성

```java
public java.util.UUID childId()
```

호출자: [conversation.api.ConversationController.start](#m283)

내부 호출: 없음

<a id="m287"></a>
### equals — 자동 생성

```java
public final boolean equals(java.lang.Object o)
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m288"></a>
### hashCode — 자동 생성

```java
public final int hashCode()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m289"></a>
### input — 자동 생성

```java
public com.dodamdodam.backend.conversation.dto.StartConversationRequest input()
```

호출자: [conversation.api.ConversationController.start](#m283)

내부 호출: 없음

<a id="m290"></a>
### session — 자동 생성

```java
public com.dodamdodam.backend.auth.session.AuthenticatedSession session()
```

호출자: [conversation.api.ConversationController.start](#m283)

내부 호출: 없음

<a id="m291"></a>
### toString — 자동 생성

```java
public final java.lang.String toString()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="c56"></a>
## conversation.api.ConversationExceptionHandler

정의: [src/main/java/com/dodamdodam/backend/conversation/api/ConversationExceptionHandler.java:14](../../../src/main/java/com/dodamdodam/backend/conversation/api/ConversationExceptionHandler.java#L14) · CLASS

설정: `@org.springframework.core.annotation.Order(-2147483648)` · `@org.springframework.web.bind.annotation.RestControllerAdvice(basePackages={"com.dodamdodam.backend"})`

<a id="m292"></a>
### propagateResponseRejection

```java
void propagateResponseRejection(com.dodamdodam.backend.conversation.api.ConversationStartResponseFilter.ResponseRejected rejection)
```

정의: [src/main/java/com/dodamdodam/backend/conversation/api/ConversationExceptionHandler.java:19](../../../src/main/java/com/dodamdodam/backend/conversation/api/ConversationExceptionHandler.java#L19)

응답 전 재검사 실패는 MVC에서 출력하지 않고 전용 필터가 성공 응답부터 비우게 한다.

애너테이션: `@org.springframework.web.bind.annotation.ExceptionHandler({com.dodamdodam.backend.conversation.api.ConversationStartResponseFilter.ResponseRejected.class})`

호출자: 정적 Java 호출 없음

프레임워크 진입: Spring MVC 예외 처리

내부 호출: 없음

<a id="m293"></a>
### handle

```java
public org.springframework.http.ResponseEntity<?> handle(com.dodamdodam.backend.conversation.exception.ConversationException exception, jakarta.servlet.http.HttpServletResponse response)
```

정의: [src/main/java/com/dodamdodam/backend/conversation/api/ConversationExceptionHandler.java:25](../../../src/main/java/com/dodamdodam/backend/conversation/api/ConversationExceptionHandler.java#L25)

대화 업무 오류만 변환하고 인증·소유권·JSON 읽기 오류는 기존 처리기에 맡긴다.

애너테이션: `@org.springframework.web.bind.annotation.ExceptionHandler({com.dodamdodam.backend.conversation.exception.ConversationException.class})`

호출자: [conversation.api.ConversationStartResponseFilter.doFilterInternal](#m301)

프레임워크 진입: Spring MVC 예외 처리

내부 호출: [conversation.dto.ActiveConversationExistsError.from](#m355) / [conversation.exception.ConversationException.activeConversationId](#m397) / [conversation.exception.ConversationException.code](#m395) / [conversation.exception.ConversationException.fields](#m396) / [global.error.ApiErrorResponses.body](#m507)

외부 라이브러리 호출: `java.lang.Throwable.getMessage()` / `org.springframework.http.ResponseEntity.status(org.springframework.http.HttpStatusCode)` / `org.springframework.http.ResponseEntity.BodyBuilder.body(@org.jspecify.annotations.Nullable T)`

<a id="m294"></a>
### ConversationExceptionHandler 생성자 — 자동 생성

```java
public void <init>()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

외부 라이브러리 호출: `java.lang.Object.<init>()`

<a id="c57"></a>
## conversation.api.ConversationMediaTypeExceptionHandler

정의: [src/main/java/com/dodamdodam/backend/conversation/api/ConversationMediaTypeExceptionHandler.java:15](../../../src/main/java/com/dodamdodam/backend/conversation/api/ConversationMediaTypeExceptionHandler.java#L15) · CLASS

설정: `@org.springframework.core.annotation.Order(-2147483648)` · `@org.springframework.web.bind.annotation.RestControllerAdvice(assignableTypes={com.dodamdodam.backend.conversation.api.ConversationController.class})`

<a id="m295"></a>
### handle

```java
public org.springframework.http.ResponseEntity<com.dodamdodam.backend.global.error.ApiErrorResponse> handle(jakarta.servlet.http.HttpServletResponse response)
```

정의: [src/main/java/com/dodamdodam/backend/conversation/api/ConversationMediaTypeExceptionHandler.java:20](../../../src/main/java/com/dodamdodam/backend/conversation/api/ConversationMediaTypeExceptionHandler.java#L20)

시작 컨트롤러의 MVC 미디어 타입 오류만 공통 500 처리보다 먼저 변환한다.

애너테이션: `@org.springframework.web.bind.annotation.ExceptionHandler({org.springframework.web.HttpMediaTypeNotSupportedException.class})`

호출자: 정적 Java 호출 없음

프레임워크 진입: Spring MVC 예외 처리

내부 호출: [global.error.ApiErrorResponses.body](#m507)

외부 라이브러리 호출: `java.util.List.of()` / `org.springframework.http.ResponseEntity.status(org.springframework.http.HttpStatusCode)` / `org.springframework.http.ResponseEntity.BodyBuilder.body(@org.jspecify.annotations.Nullable T)`

<a id="m296"></a>
### ConversationMediaTypeExceptionHandler 생성자 — 자동 생성

```java
public void <init>()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

외부 라이브러리 호출: `java.lang.Object.<init>()`

<a id="c58"></a>
## conversation.api.ConversationStartResponseFilter

정의: [src/main/java/com/dodamdodam/backend/conversation/api/ConversationStartResponseFilter.java:36](../../../src/main/java/com/dodamdodam/backend/conversation/api/ConversationStartResponseFilter.java#L36) · CLASS

인증 문맥이 유지되는 보안 체인의 하위에서 시작 성공 응답만 전송 전에 보호한다.

설정: `@org.springframework.stereotype.Component` · `@org.jspecify.annotations.NullMarked`

<a id="m297"></a>
### ConversationStartResponseFilter 생성자

```java
public void <init>(org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterProperties security, com.dodamdodam.backend.auth.session.AuthenticatedSessionService authentication, com.dodamdodam.backend.conversation.application.ConversationService conversations, com.dodamdodam.backend.conversation.api.ConversationExceptionHandler conversationErrors, com.dodamdodam.backend.child.api.ChildExceptionHandler childErrors, tools.jackson.databind.json.JsonMapper json)
```

정의: [src/main/java/com/dodamdodam/backend/conversation/api/ConversationStartResponseFilter.java:52](../../../src/main/java/com/dodamdodam/backend/conversation/api/ConversationStartResponseFilter.java#L52)

기존 필터 순서는 유지하고 Security 위임 필터 바로 뒤에 한 번 등록한다.

호출자: 정적 Java 호출 없음

내부 호출: 없음

외부 라이브러리 호출: `java.lang.Math.addExact(int,int)` / `org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterProperties.getOrder()` / `org.springframework.web.filter.OncePerRequestFilter.<init>()`

<a id="m298"></a>
### getOrder

```java
public int getOrder()
```

정의: [src/main/java/com/dodamdodam/backend/conversation/api/ConversationStartResponseFilter.java:64](../../../src/main/java/com/dodamdodam/backend/conversation/api/ConversationStartResponseFilter.java#L64)

보안 체인이 다음 서블릿 필터를 호출하는 동안에만 현재 인증 문맥을 사용한다.

애너테이션: `@java.lang.Override`

호출자: 정적 Java 호출 없음

프레임워크 진입: Servlet / Spring 필터 실행

내부 호출: 없음

<a id="m299"></a>
### protect

```java
static void protect(jakarta.servlet.http.HttpServletRequest request, com.dodamdodam.backend.auth.session.AuthenticatedSession session, java.util.UUID childId, java.util.UUID clientRequestId, java.util.UUID conversationId)
```

정의: [src/main/java/com/dodamdodam/backend/conversation/api/ConversationStartResponseFilter.java:70](../../../src/main/java/com/dodamdodam/backend/conversation/api/ConversationStartResponseFilter.java#L70)

시작 성공 후 서버가 검증한 문맥과 반환할 대화만 요청 범위에 기록한다.

호출자: [conversation.api.ConversationController.start](#m283)

내부 호출: [conversation.api.ConversationStartResponseFilter.ResponseAccess.ResponseAccess](#m308)

외부 라이브러리 호출: `jakarta.servlet.ServletRequest.setAttribute(java.lang.String,java.lang.Object)`

<a id="m300"></a>
### shouldNotFilter

```java
protected boolean shouldNotFilter(jakarta.servlet.http.HttpServletRequest request)
```

정의: [src/main/java/com/dodamdodam/backend/conversation/api/ConversationStartResponseFilter.java:76](../../../src/main/java/com/dodamdodam/backend/conversation/api/ConversationStartResponseFilter.java#L76)

Part 1과 다른 Part 2 경로의 응답은 감싸거나 재검사하지 않는다.

애너테이션: `@java.lang.Override`

호출자: 정적 Java 호출 없음

프레임워크 진입: Servlet / Spring 필터 실행

내부 호출: 없음

외부 라이브러리 호출: `org.springframework.security.web.util.matcher.RequestMatcher.matches(jakarta.servlet.http.HttpServletRequest)`

<a id="m301"></a>
### doFilterInternal

```java
protected void doFilterInternal(jakarta.servlet.http.HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response, jakarta.servlet.FilterChain filterChain)
```

정의: [src/main/java/com/dodamdodam/backend/conversation/api/ConversationStartResponseFilter.java:82](../../../src/main/java/com/dodamdodam/backend/conversation/api/ConversationStartResponseFilter.java#L82)

첫 전송 및 미커밋 반환 직전에 검사하고 실패한 성공 본문·헤더를 원자적으로 비운다.

애너테이션: `@java.lang.Override`

호출자: 정적 Java 호출 없음

프레임워크 진입: Servlet / Spring 필터 실행

내부 호출: [auth.session.SessionCookieRenewalFilter.InvalidSessionException.InvalidSessionException](#m191) / [child.api.ChildExceptionHandler.handle](#m221) / [conversation.api.ConversationExceptionHandler.handle](#m293) / [conversation.api.ConversationStartResponseFilter.clearSuccess](#m305) / [conversation.api.ConversationStartResponseFilter.findRejection](#m304) / [conversation.api.ConversationStartResponseFilter.verify](#m303) / [conversation.api.ConversationStartResponseFilter$anonymous@86.ConversationStartResponseFilter$anonymous@86](#m307) / [conversation.exception.ConversationException.ConversationException](#m388)

외부 라이브러리 호출: `jakarta.servlet.FilterChain.doFilter(jakarta.servlet.ServletRequest,jakarta.servlet.ServletResponse)` / `jakarta.servlet.ServletResponse.getOutputStream()` / `jakarta.servlet.ServletResponse.isCommitted()` / `jakarta.servlet.ServletResponse.setCharacterEncoding(java.lang.String)` / `jakarta.servlet.ServletResponse.setContentType(java.lang.String)` / `jakarta.servlet.http.HttpServletResponse.setStatus(int)` / `java.nio.charset.Charset.name()` / `org.springframework.http.HttpEntity.getBody()` / `org.springframework.http.HttpStatusCode.value()` / `org.springframework.http.ResponseEntity.getStatusCode()` / `tools.jackson.databind.ObjectMapper.writeValue(java.io.OutputStream,java.lang.Object)`

<a id="m303"></a>
### verify

```java
private void verify(jakarta.servlet.http.HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response)
```

정의: [src/main/java/com/dodamdodam/backend/conversation/api/ConversationStartResponseFilter.java:123](../../../src/main/java/com/dodamdodam/backend/conversation/api/ConversationStartResponseFilter.java#L123)

기존 인증을 먼저 재사용하고 성공 대상의 현재 DB 권한·업무 상태를 확인한다.

호출자: [conversation.api.ConversationStartResponseFilter.doFilterInternal](#m301) / [conversation.api.ConversationStartResponseFilter$anonymous@86.onResponseCommitted](#m302)

내부 호출: [auth.session.AuthenticatedSession.equals](#m167) / [auth.session.AuthenticatedSessionService.requireAuthenticatedSession](#m172) / [conversation.api.ConversationStartResponseFilter.ResponseAccess.childId](#m309) / [conversation.api.ConversationStartResponseFilter.ResponseAccess.clientRequestId](#m310) / [conversation.api.ConversationStartResponseFilter.ResponseAccess.conversationId](#m311) / [conversation.api.ConversationStartResponseFilter.ResponseAccess.session](#m314) / [conversation.api.ConversationStartResponseFilter.ResponseRejected.ResponseRejected](#m306) / [conversation.application.ConversationService.verifyStartResponse](#m330)

외부 라이브러리 호출: `jakarta.servlet.ServletRequest.getAttribute(java.lang.String)` / `jakarta.servlet.http.HttpServletResponse.getStatus()` / `org.springframework.security.authentication.InsufficientAuthenticationException.<init>(java.lang.String)`

<a id="m304"></a>
### findRejection

```java
private java.lang.@org.jspecify.annotations.Nullable RuntimeException findRejection(java.lang.Throwable failure)
```

정의: [src/main/java/com/dodamdodam/backend/conversation/api/ConversationStartResponseFilter.java:142](../../../src/main/java/com/dodamdodam/backend/conversation/api/ConversationStartResponseFilter.java#L142)

MVC/서블릿 포장 안의 전송 거부만 찾고 무관한 기존 예외 처리에는 관여하지 않는다.

호출자: [conversation.api.ConversationStartResponseFilter.doFilterInternal](#m301)

내부 호출: 없음

외부 라이브러리 호출: `java.lang.Throwable.getCause()`

<a id="m305"></a>
### clearSuccess

```java
private void clearSuccess(jakarta.servlet.http.HttpServletResponse response)
```

정의: [src/main/java/com/dodamdodam/backend/conversation/api/ConversationStartResponseFilter.java:152](../../../src/main/java/com/dodamdodam/backend/conversation/api/ConversationStartResponseFilter.java#L152)

requestId·no-store·CORS는 유지하고 성공 길이·Location·인증 갱신 쿠키를 제거한다.

호출자: [conversation.api.ConversationStartResponseFilter.doFilterInternal](#m301)

내부 호출: 없음

외부 라이브러리 호출: `jakarta.servlet.ServletResponse.reset()` / `jakarta.servlet.http.HttpServletResponse.addHeader(java.lang.String,java.lang.String)` / `jakarta.servlet.http.HttpServletResponse.getHeaderNames()` / `jakarta.servlet.http.HttpServletResponse.getHeaders(java.lang.String)` / `java.lang.Iterable.forEach(java.util.function.Consumer<? super T>)` / `java.lang.String.equalsIgnoreCase(java.lang.String)` / `java.lang.String.startsWith(java.lang.String)` / `java.util.Collection.stream()` / `java.util.HashMap.put(K,V)` / `java.util.LinkedHashMap.<init>()` / `java.util.LinkedHashMap.forEach(java.util.function.BiConsumer<? super K,? super V>)` / `java.util.stream.Stream.filter(java.util.function.Predicate<? super T>)` / `java.util.stream.Stream.toList()`

<a id="c59"></a>
## conversation.api.ConversationStartResponseFilter$anonymous@86

정의: [src/main/java/com/dodamdodam/backend/conversation/api/ConversationStartResponseFilter.java:86](../../../src/main/java/com/dodamdodam/backend/conversation/api/ConversationStartResponseFilter.java#L86) · CLASS

<a id="m302"></a>
### onResponseCommitted

```java
protected void onResponseCommitted()
```

정의: [src/main/java/com/dodamdodam/backend/conversation/api/ConversationStartResponseFilter.java:88](../../../src/main/java/com/dodamdodam/backend/conversation/api/ConversationStartResponseFilter.java#L88)

MVC 직렬화·flush가 시작해도 거부된 성공 바이트를 그대로 내보내지 않는다.

애너테이션: `@java.lang.Override`

호출자: 정적 Java 호출 없음

프레임워크 진입: 응답 commit 콜백

내부 호출: [conversation.api.ConversationStartResponseFilter.verify](#m303)

외부 라이브러리 호출: `org.springframework.security.web.util.OnCommittedResponseWrapper.disableOnResponseCommitted()`

<a id="m307"></a>
### 익명 클래스@86 생성자 — 자동 생성

```java
void <init>(jakarta.servlet.http.HttpServletResponse arg0)
```

호출자: [conversation.api.ConversationStartResponseFilter.doFilterInternal](#m301)

내부 호출: 없음

외부 라이브러리 호출: `org.springframework.security.web.util.OnCommittedResponseWrapper.<init>(jakarta.servlet.http.HttpServletResponse)`

<a id="c60"></a>
## conversation.api.ConversationStartResponseFilter.ResponseAccess

정의: [src/main/java/com/dodamdodam/backend/conversation/api/ConversationStartResponseFilter.java:167](../../../src/main/java/com/dodamdodam/backend/conversation/api/ConversationStartResponseFilter.java#L167) · RECORD

<a id="m308"></a>
### ResponseAccess 생성자 — 자동 생성

```java
private void <init>(com.dodamdodam.backend.auth.session.AuthenticatedSession session, java.util.UUID childId, java.util.UUID clientRequestId, java.util.UUID conversationId)
```

호출자: [conversation.api.ConversationStartResponseFilter.protect](#m299)

내부 호출: 없음

외부 라이브러리 호출: `java.lang.Record.<init>()`

<a id="m309"></a>
### childId — 자동 생성

```java
public java.util.UUID childId()
```

호출자: [conversation.api.ConversationStartResponseFilter.verify](#m303)

내부 호출: 없음

<a id="m310"></a>
### clientRequestId — 자동 생성

```java
public java.util.UUID clientRequestId()
```

호출자: [conversation.api.ConversationStartResponseFilter.verify](#m303)

내부 호출: 없음

<a id="m311"></a>
### conversationId — 자동 생성

```java
public java.util.UUID conversationId()
```

호출자: [conversation.api.ConversationStartResponseFilter.verify](#m303)

내부 호출: 없음

<a id="m312"></a>
### equals — 자동 생성

```java
public final boolean equals(java.lang.Object o)
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m313"></a>
### hashCode — 자동 생성

```java
public final int hashCode()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m314"></a>
### session — 자동 생성

```java
public com.dodamdodam.backend.auth.session.AuthenticatedSession session()
```

호출자: [conversation.api.ConversationStartResponseFilter.verify](#m303)

내부 호출: 없음

<a id="m315"></a>
### toString — 자동 생성

```java
public final java.lang.String toString()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="c61"></a>
## conversation.api.ConversationStartResponseFilter.ResponseRejected

정의: [src/main/java/com/dodamdodam/backend/conversation/api/ConversationStartResponseFilter.java:170](../../../src/main/java/com/dodamdodam/backend/conversation/api/ConversationStartResponseFilter.java#L170) · CLASS

업무 예외를 MVC가 먼저 출력하지 못하도록 원인을 별도 보관하는 내부 전송 신호다.

<a id="m306"></a>
### ResponseRejected 생성자

```java
private void <init>(java.lang.RuntimeException rejection)
```

정의: [src/main/java/com/dodamdodam/backend/conversation/api/ConversationStartResponseFilter.java:174](../../../src/main/java/com/dodamdodam/backend/conversation/api/ConversationStartResponseFilter.java#L174)

고정 메시지만 가지며 실제 오류 변환은 성공 응답 초기화 뒤 수행한다.

호출자: [conversation.api.ConversationStartResponseFilter.verify](#m303)

내부 호출: 없음

외부 라이브러리 호출: `java.lang.RuntimeException.<init>(java.lang.String,java.lang.Throwable,boolean,boolean)`

<a id="c62"></a>
## conversation.application.ConversationClosureCoordinator

정의: [src/main/java/com/dodamdodam/backend/conversation/application/ConversationClosureCoordinator.java:14](../../../src/main/java/com/dodamdodam/backend/conversation/application/ConversationClosureCoordinator.java#L14) · CLASS

설정: `@org.springframework.stereotype.Service` · `@org.springframework.boot.context.properties.EnableConfigurationProperties({com.dodamdodam.backend.conversation.infrastructure.ConversationClosureProperties.class})`

<a id="m316"></a>
### ConversationClosureCoordinator 생성자

```java
public void <init>(com.dodamdodam.backend.child.application.ChildService children, com.dodamdodam.backend.conversation.infrastructure.ConversationRepository clock, com.dodamdodam.backend.conversation.infrastructure.ConversationClosureRepository repository, com.dodamdodam.backend.conversation.infrastructure.ConversationClosureProperties properties)
```

정의: [src/main/java/com/dodamdodam/backend/conversation/application/ConversationClosureCoordinator.java:24](../../../src/main/java/com/dodamdodam/backend/conversation/application/ConversationClosureCoordinator.java#L24)

시작·수동 종료·타이머·발화 완료가 같은 DB 전이와 명시적 요약 기한을 사용한다.

호출자: 정적 Java 호출 없음

내부 호출: 없음

외부 라이브러리 호출: `java.lang.Object.<init>()`

<a id="m317"></a>
### reconcile

```java
public com.dodamdodam.backend.conversation.application.ConversationClosureCoordinator.ClosureResult reconcile(java.util.UUID accountId, java.util.UUID childId, java.util.UUID conversationId, com.dodamdodam.backend.conversation.application.ConversationClosureCoordinator.Trigger trigger)
```

정의: [src/main/java/com/dodamdodam/backend/conversation/application/ConversationClosureCoordinator.java:33](../../../src/main/java/com/dodamdodam/backend/conversation/application/ConversationClosureCoordinator.java#L33)

신뢰된 내부 호출용이며 사용자 경로의 로그인·연결 검사는 호출자가 같은 Tx에서 선행한다.

애너테이션: `@org.springframework.transaction.annotation.Transactional`

호출자: [conversation.application.ConversationService.decideStart](#m331) / [conversation.application.ConversationService.verifyStartResponse](#m330)

내부 호출: [child.application.ChildService.requireChildAccessForUpdate](#m227) / [child.exception.ChildException.notFound](#m272) / [conversation.application.ConversationClosureCoordinator.requireState](#m318) / [conversation.application.ConversationClosureCoordinator.ClosureResult.ClosureResult](#m319) / [conversation.infrastructure.ConversationClosureProperties.summaryProcessingTimeout](#m401) / [conversation.infrastructure.ConversationClosureRepository.beginClosing](#m406) / [conversation.infrastructure.ConversationClosureRepository.expireProcessing](#m408) / [conversation.infrastructure.ConversationClosureRepository.finishClosing](#m412) / [conversation.infrastructure.ConversationClosureRepository.grantRecovery](#m413) / [conversation.infrastructure.ConversationClosureRepository.hasProcessing](#m409) / [conversation.infrastructure.ConversationClosureRepository.hasSummaryInput](#m410) / [conversation.infrastructure.ConversationClosureRepository.lockAccount](#m404) / [conversation.infrastructure.ConversationClosureRepository.lockLinks](#m411) / [conversation.infrastructure.ConversationClosureRepository.lockProcessingTurns](#m407) / [conversation.infrastructure.ConversationClosureRepository.ClosureState.endRequestedAt](#m418) / [conversation.infrastructure.ConversationClosureRepository.ClosureState.scheduledEndAt](#m422) / [conversation.infrastructure.ConversationClosureRepository.ClosureState.status](#m423) / [conversation.infrastructure.ConversationRepository.currentTime](#m427)

외부 라이브러리 호출: `java.lang.String.equals(java.lang.Object)` / `java.time.OffsetDateTime.isBefore(java.time.OffsetDateTime)` / `java.time.OffsetDateTime.plus(java.time.temporal.TemporalAmount)`

<a id="m318"></a>
### requireState

```java
private com.dodamdodam.backend.conversation.infrastructure.ConversationClosureRepository.ClosureState requireState(java.util.UUID childId, java.util.UUID conversationId)
```

정의: [src/main/java/com/dodamdodam/backend/conversation/application/ConversationClosureCoordinator.java:68](../../../src/main/java/com/dodamdodam/backend/conversation/application/ConversationClosureCoordinator.java#L68)

미존재·다른 자녀의 대화는 같은 404로 처리하고 부모-자식 관계가 없는 상태는 반환하지 않는다.

호출자: [conversation.application.ConversationClosureCoordinator.reconcile](#m317)

내부 호출: [child.exception.ChildException.notFound](#m272) / [conversation.infrastructure.ConversationClosureRepository.findForUpdate](#m405)

외부 라이브러리 호출: `java.util.Optional.orElseThrow(java.util.function.Supplier<? extends X>)`

<a id="c63"></a>
## conversation.application.ConversationClosureCoordinator.Trigger

정의: [src/main/java/com/dodamdodam/backend/conversation/application/ConversationClosureCoordinator.java:73](../../../src/main/java/com/dodamdodam/backend/conversation/application/ConversationClosureCoordinator.java#L73) · ENUM

자동 관측과 명시적 종료 요청을 구분하며 해당 API나 worker 자체를 구현하지 않는다.

<a id="m325"></a>
### Trigger 생성자 — 자동 생성

```java
private void <init>()
```

호출자: `conversation.application.ConversationClosureCoordinator.Trigger.<initializer>`

내부 호출: 없음

외부 라이브러리 호출: `java.lang.Enum.<init>(java.lang.String,int)`

<a id="m326"></a>
### valueOf — 자동 생성

```java
public static com.dodamdodam.backend.conversation.application.ConversationClosureCoordinator.Trigger valueOf(java.lang.String name)
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m327"></a>
### values — 자동 생성

```java
public static com.dodamdodam.backend.conversation.application.ConversationClosureCoordinator.Trigger[] values()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="c64"></a>
## conversation.application.ConversationClosureCoordinator.ClosureResult

정의: [src/main/java/com/dodamdodam/backend/conversation/application/ConversationClosureCoordinator.java:76](../../../src/main/java/com/dodamdodam/backend/conversation/application/ConversationClosureCoordinator.java#L76) · RECORD

endedNow는 최초 전이 실행자 표시이며 요약 AI 실행·성공을 의미하지 않는다.

<a id="m319"></a>
### ClosureResult 생성자 — 자동 생성

```java
public void <init>(com.dodamdodam.backend.conversation.infrastructure.ConversationClosureRepository.ClosureState state, boolean endedNow)
```

호출자: [conversation.application.ConversationClosureCoordinator.reconcile](#m317)

내부 호출: 없음

외부 라이브러리 호출: `java.lang.Record.<init>()`

<a id="m320"></a>
### endedNow — 자동 생성

```java
public boolean endedNow()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m321"></a>
### equals — 자동 생성

```java
public final boolean equals(java.lang.Object o)
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m322"></a>
### hashCode — 자동 생성

```java
public final int hashCode()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m323"></a>
### state — 자동 생성

```java
public com.dodamdodam.backend.conversation.infrastructure.ConversationClosureRepository.ClosureState state()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m324"></a>
### toString — 자동 생성

```java
public final java.lang.String toString()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="c65"></a>
## conversation.application.ConversationService

정의: [src/main/java/com/dodamdodam/backend/conversation/application/ConversationService.java:20](../../../src/main/java/com/dodamdodam/backend/conversation/application/ConversationService.java#L20) · CLASS

설정: `@org.springframework.stereotype.Service`

<a id="m328"></a>
### ConversationService 생성자

```java
public void <init>(com.dodamdodam.backend.auth.session.SessionSecurityRepository security, com.dodamdodam.backend.child.application.ChildService children, com.dodamdodam.backend.conversation.infrastructure.ConversationRepository repository, com.dodamdodam.backend.conversation.application.ConversationClosureCoordinator closure, org.springframework.transaction.PlatformTransactionManager transactions)
```

정의: [src/main/java/com/dodamdodam/backend/conversation/application/ConversationService.java:30](../../../src/main/java/com/dodamdodam/backend/conversation/application/ConversationService.java#L30)

공통 보안·자녀 정책과 대화 저장소를 같은 업무 트랜잭션에서 사용한다.

호출자: 정적 Java 호출 없음

내부 호출: 없음

외부 라이브러리 호출: `java.lang.Object.<init>()` / `org.springframework.transaction.support.TransactionTemplate.<init>(org.springframework.transaction.PlatformTransactionManager)`

<a id="m329"></a>
### create

```java
public com.dodamdodam.backend.conversation.application.ConversationService.StartOutcome create(com.dodamdodam.backend.auth.session.AuthenticatedSession session, java.util.UUID childId, com.dodamdodam.backend.conversation.dto.StartConversationRequest request)
```

정의: [src/main/java/com/dodamdodam/backend/conversation/application/ConversationService.java:40](../../../src/main/java/com/dodamdodam/backend/conversation/application/ConversationService.java#L40)

HTTP 진입에서 외부 업무 Tx 없이 호출하며 종료 조정 커밋 후 정상적인 시작 거부를 전달한다.

호출자: [conversation.api.ConversationController.start](#m283)

내부 호출: [conversation.application.ConversationService.decideStart](#m331) / [conversation.application.ConversationService.StartDecision.outcome](#m337) / [conversation.application.ConversationService.StartDecision.rejection](#m338)

외부 라이브러리 호출: `java.util.Objects.requireNonNull(T)` / `org.springframework.transaction.support.TransactionTemplate.execute(org.springframework.transaction.support.TransactionCallback<T>)`

<a id="m330"></a>
### verifyStartResponse

```java
public void verifyStartResponse(com.dodamdodam.backend.auth.session.AuthenticatedSession session, java.util.UUID childId, java.util.UUID clientRequestId, java.util.UUID conversationId)
```

정의: [src/main/java/com/dodamdodam/backend/conversation/application/ConversationService.java:50](../../../src/main/java/com/dodamdodam/backend/conversation/application/ConversationService.java#L50)

생성 Tx 이후 새 Tx에서 반환 대상만 재검사하며 대화 생성·암묵적 연결은 수행하지 않는다.

호출자: [conversation.api.ConversationStartResponseFilter.verify](#m303)

내부 호출: [auth.session.AuthenticatedSession.accountId](#m166) / [auth.session.AuthenticatedSession.securityContextId](#m169) / [auth.session.SessionSecurityRepository.requireCurrentContextForUpdate](#m206) / [child.application.ChildService.requireChildAccessForUpdate](#m227) / [child.exception.ChildException.notFound](#m272) / [conversation.application.ConversationClosureCoordinator.reconcile](#m317) / [conversation.application.ConversationService.conflictFor](#m333) / [conversation.application.ConversationService.replay](#m332) / [conversation.domain.ConversationServiceHours.isOpen](#m348) / [conversation.domain.ConversationServiceHours.requireOpen](#m347) / [conversation.exception.ConversationException.serviceHoursClosed](#m390) / [conversation.infrastructure.ConversationRepository.currentTime](#m427) / [conversation.infrastructure.ConversationRepository.findRequestForUpdate](#m428) / [conversation.infrastructure.ConversationRepository.StoredConversation.conversationId](#m435) / [conversation.infrastructure.ConversationRepository.StoredConversation.scheduledEndAt](#m438) / [conversation.infrastructure.ConversationRepository.StoredConversation.status](#m441)

외부 라이브러리 호출: `java.lang.String.equals(java.lang.Object)` / `java.time.OffsetDateTime.isBefore(java.time.OffsetDateTime)` / `java.util.Optional.filter(java.util.function.Predicate<? super T>)` / `java.util.Optional.orElseThrow(java.util.function.Supplier<? extends X>)` / `java.util.UUID.equals(java.lang.Object)` / `org.springframework.transaction.support.TransactionTemplate.execute(org.springframework.transaction.support.TransactionCallback<T>)`

<a id="m331"></a>
### decideStart

```java
private com.dodamdodam.backend.conversation.application.ConversationService.StartDecision decideStart(com.dodamdodam.backend.auth.session.AuthenticatedSession session, java.util.UUID childId, com.dodamdodam.backend.conversation.dto.StartConversationRequest request)
```

정의: [src/main/java/com/dodamdodam/backend/conversation/application/ConversationService.java:82](../../../src/main/java/com/dodamdodam/backend/conversation/application/ConversationService.java#L82)

자녀 잠금 아래 이전 날짜 종료 조정과 시작 판정을 같은 업무 트랜잭션에서 수행한다.

호출자: [conversation.application.ConversationService.create](#m329)

내부 호출: [auth.session.AuthenticatedSession.accountId](#m166) / [auth.session.AuthenticatedSession.securityContextId](#m169) / [auth.session.SessionSecurityRepository.requireCurrentContextForUpdate](#m206) / [child.application.ChildService.requireChildAccessForUpdate](#m227) / [conversation.application.ConversationClosureCoordinator.reconcile](#m317) / [conversation.application.ConversationService.conflictFor](#m333) / [conversation.application.ConversationService.replay](#m332) / [conversation.application.ConversationService.StartDecision.StartDecision](#m334) / [conversation.application.ConversationService.StartOutcome.StartOutcome](#m340) / [conversation.domain.ConversationServiceHours.isOpen](#m348) / [conversation.domain.ConversationServiceHours.requireOpen](#m347) / [conversation.domain.ConversationServiceHours.ServiceWindow.serviceDate](#m353) / [conversation.dto.StartConversationRequest.clientRequestId](#m374) / [conversation.dto.StartConversationResult.conversationId](#m379) / [conversation.exception.ConversationException.ConversationException](#m388) / [conversation.exception.ConversationException.serviceHoursClosed](#m390) / [conversation.infrastructure.ConversationRepository.currentTime](#m427) / [conversation.infrastructure.ConversationRepository.findRequestForUpdate](#m428) / [conversation.infrastructure.ConversationRepository.findUnendedForUpdate](#m429) / [conversation.infrastructure.ConversationRepository.insertActive](#m431) / [conversation.infrastructure.ConversationRepository.insertLink](#m432) / [conversation.infrastructure.ConversationRepository.StoredConversation.conversationId](#m435) / [conversation.infrastructure.ConversationRepository.StoredConversation.scheduledEndAt](#m438) / [conversation.infrastructure.ConversationRepository.StoredConversation.status](#m441)

외부 라이브러리 호출: `java.lang.IllegalStateException.<init>(java.lang.String)` / `java.lang.String.equals(java.lang.Object)` / `java.time.LocalDate.equals(java.lang.Object)` / `java.time.OffsetDateTime.isBefore(java.time.OffsetDateTime)` / `java.util.Optional.get()` / `java.util.Optional.isEmpty()` / `java.util.Optional.isPresent()` / `java.util.Optional.orElseThrow()` / `java.util.Optional.orElseThrow(java.util.function.Supplier<? extends X>)` / `java.util.UUID.randomUUID()`

<a id="m332"></a>
### replay

```java
private com.dodamdodam.backend.conversation.application.ConversationService.StartOutcome replay(com.dodamdodam.backend.auth.session.AuthenticatedSession session, com.dodamdodam.backend.conversation.infrastructure.ConversationRepository.StoredConversation conversation, com.dodamdodam.backend.conversation.domain.ConversationServiceHours.ServiceWindow window)
```

정의: [src/main/java/com/dodamdodam/backend/conversation/application/ConversationService.java:135](../../../src/main/java/com/dodamdodam/backend/conversation/application/ConversationService.java#L135)

당일 ACTIVE의 현재 연결이 있을 때만 기존 시작 정보를 반환하며 암묵적으로 연결하지 않는다.

호출자: [conversation.application.ConversationService.decideStart](#m331) / [conversation.application.ConversationService.verifyStartResponse](#m330)

내부 호출: [auth.session.AuthenticatedSession.accountId](#m166) / [auth.session.AuthenticatedSession.securityContextId](#m169) / [auth.session.SessionSecurityRepository.requireCurrentContextForUpdate](#m206) / [conversation.application.ConversationService.conflictFor](#m333) / [conversation.application.ConversationService.StartOutcome.StartOutcome](#m340) / [conversation.domain.ConversationServiceHours.requireOpen](#m347) / [conversation.domain.ConversationServiceHours.ServiceWindow.serviceDate](#m353) / [conversation.exception.ConversationException.childSessionRequired](#m391) / [conversation.exception.ConversationException.serviceHoursClosed](#m390) / [conversation.infrastructure.ConversationRepository.currentTime](#m427) / [conversation.infrastructure.ConversationRepository.hasLinkForUpdate](#m430) / [conversation.infrastructure.ConversationRepository.StoredConversation.conversationId](#m435) / [conversation.infrastructure.ConversationRepository.StoredConversation.serviceDate](#m439) / [conversation.infrastructure.ConversationRepository.StoredConversation.startResult](#m433) / [conversation.infrastructure.ConversationRepository.StoredConversation.status](#m441)

외부 라이브러리 호출: `java.lang.String.equals(java.lang.Object)` / `java.time.LocalDate.equals(java.lang.Object)`

<a id="m333"></a>
### conflictFor

```java
private com.dodamdodam.backend.conversation.exception.ConversationException conflictFor(com.dodamdodam.backend.conversation.infrastructure.ConversationRepository.StoredConversation conversation)
```

정의: [src/main/java/com/dodamdodam/backend/conversation/application/ConversationService.java:153](../../../src/main/java/com/dodamdodam/backend/conversation/application/ConversationService.java#L153)

다른 시작 키의 미종료 대화와 종료된 기존 키를 각각의 업무 충돌로 분류한다.

호출자: [conversation.application.ConversationService.decideStart](#m331) / [conversation.application.ConversationService.replay](#m332) / [conversation.application.ConversationService.verifyStartResponse](#m330)

내부 호출: [conversation.exception.ConversationException.ConversationException](#m388) / [conversation.exception.ConversationException.activeExists](#m392) / [conversation.exception.ConversationException.closing](#m393) / [conversation.exception.ConversationException.ended](#m394) / [conversation.infrastructure.ConversationRepository.StoredConversation.conversationId](#m435) / [conversation.infrastructure.ConversationRepository.StoredConversation.status](#m441)

외부 라이브러리 호출: `java.lang.IllegalStateException.<init>(java.lang.String)`

<a id="c66"></a>
## conversation.application.ConversationService.StartOutcome

정의: [src/main/java/com/dodamdodam/backend/conversation/application/ConversationService.java:163](../../../src/main/java/com/dodamdodam/backend/conversation/application/ConversationService.java#L163) · RECORD

HTTP 상태 선택용 내부 결과이며 created 표시는 공개 data에 포함하지 않는다.

<a id="m340"></a>
### StartOutcome 생성자 — 자동 생성

```java
public void <init>(com.dodamdodam.backend.conversation.dto.StartConversationResult result, boolean created)
```

호출자: [conversation.application.ConversationService.decideStart](#m331) / [conversation.application.ConversationService.replay](#m332)

내부 호출: 없음

외부 라이브러리 호출: `java.lang.Record.<init>()`

<a id="m341"></a>
### created — 자동 생성

```java
public boolean created()
```

호출자: [conversation.api.ConversationController.start](#m283)

내부 호출: 없음

<a id="m342"></a>
### equals — 자동 생성

```java
public final boolean equals(java.lang.Object o)
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m343"></a>
### hashCode — 자동 생성

```java
public final int hashCode()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m344"></a>
### result — 자동 생성

```java
public com.dodamdodam.backend.conversation.dto.StartConversationResult result()
```

호출자: [conversation.api.ConversationController.start](#m283)

내부 호출: 없음

<a id="m345"></a>
### toString — 자동 생성

```java
public final java.lang.String toString()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="c67"></a>
## conversation.application.ConversationService.StartDecision

정의: [src/main/java/com/dodamdodam/backend/conversation/application/ConversationService.java:166](../../../src/main/java/com/dodamdodam/backend/conversation/application/ConversationService.java#L166) · RECORD

HTTP 거부와 저장 장애를 분리해 종료 조정의 정상 커밋을 보존한다.

<a id="m334"></a>
### StartDecision 생성자 — 자동 생성

```java
private void <init>(com.dodamdodam.backend.conversation.application.ConversationService.StartOutcome outcome, com.dodamdodam.backend.conversation.exception.ConversationException rejection)
```

호출자: [conversation.application.ConversationService.decideStart](#m331)

내부 호출: 없음

외부 라이브러리 호출: `java.lang.Record.<init>()`

<a id="m335"></a>
### equals — 자동 생성

```java
public final boolean equals(java.lang.Object o)
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m336"></a>
### hashCode — 자동 생성

```java
public final int hashCode()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m337"></a>
### outcome — 자동 생성

```java
public com.dodamdodam.backend.conversation.application.ConversationService.StartOutcome outcome()
```

호출자: [conversation.application.ConversationService.create](#m329)

내부 호출: 없음

<a id="m338"></a>
### rejection — 자동 생성

```java
public com.dodamdodam.backend.conversation.exception.ConversationException rejection()
```

호출자: [conversation.application.ConversationService.create](#m329)

내부 호출: 없음

<a id="m339"></a>
### toString — 자동 생성

```java
public final java.lang.String toString()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="c68"></a>
## conversation.domain.ConversationServiceHours

정의: [src/main/java/com/dodamdodam/backend/conversation/domain/ConversationServiceHours.java:10](../../../src/main/java/com/dodamdodam/backend/conversation/domain/ConversationServiceHours.java#L10) · CLASS

<a id="m346"></a>
### ConversationServiceHours 생성자

```java
private void <init>()
```

정의: [src/main/java/com/dodamdodam/backend/conversation/domain/ConversationServiceHours.java:16](../../../src/main/java/com/dodamdodam/backend/conversation/domain/ConversationServiceHours.java#L16)

외부 시계 없이 전달받은 DB 판정 시각만 사용하는 정책이다.

호출자: 정적 Java 호출 없음

내부 호출: 없음

외부 라이브러리 호출: `java.lang.Object.<init>()`

<a id="m347"></a>
### requireOpen

```java
public static com.dodamdodam.backend.conversation.domain.ConversationServiceHours.ServiceWindow requireOpen(java.time.OffsetDateTime databaseNow)
```

정의: [src/main/java/com/dodamdodam/backend/conversation/domain/ConversationServiceHours.java:20](../../../src/main/java/com/dodamdodam/backend/conversation/domain/ConversationServiceHours.java#L20)

호출자가 잠금 후 읽은 DB 시각으로 08시 포함·다음 자정 미포함을 판정한다.

호출자: [conversation.application.ConversationService.decideStart](#m331) / [conversation.application.ConversationService.replay](#m332) / [conversation.application.ConversationService.verifyStartResponse](#m330)

내부 호출: [conversation.domain.ConversationServiceHours.isOpen](#m348) / [conversation.domain.ConversationServiceHours.ServiceWindow.ServiceWindow](#m349) / [conversation.exception.ConversationException.serviceHoursClosed](#m390)

외부 라이브러리 호출: `java.time.LocalDate.atStartOfDay(java.time.ZoneId)` / `java.time.LocalDate.plusDays(long)` / `java.time.OffsetDateTime.atZoneSameInstant(java.time.ZoneId)` / `java.time.ZonedDateTime.toLocalDate()` / `java.time.ZonedDateTime.toOffsetDateTime()` / `java.util.Objects.requireNonNull(T,java.lang.String)`

<a id="m348"></a>
### isOpen

```java
public static boolean isOpen(java.time.OffsetDateTime databaseNow)
```

정의: [src/main/java/com/dodamdodam/backend/conversation/domain/ConversationServiceHours.java:31](../../../src/main/java/com/dodamdodam/backend/conversation/domain/ConversationServiceHours.java#L31)

시간 밖에도 종료 정리를 커밋한 뒤 403을 반환할 수 있도록 순수 시간 판정을 제공한다.

호출자: [conversation.application.ConversationService.decideStart](#m331) / [conversation.application.ConversationService.verifyStartResponse](#m330) / [conversation.domain.ConversationServiceHours.requireOpen](#m347)

내부 호출: 없음

외부 라이브러리 호출: `java.time.LocalTime.isBefore(java.time.LocalTime)` / `java.time.OffsetDateTime.atZoneSameInstant(java.time.ZoneId)` / `java.time.ZonedDateTime.toLocalTime()` / `java.util.Objects.requireNonNull(T,java.lang.String)`

<a id="c69"></a>
## conversation.domain.ConversationServiceHours.ServiceWindow

정의: [src/main/java/com/dodamdodam/backend/conversation/domain/ConversationServiceHours.java:36](../../../src/main/java/com/dodamdodam/backend/conversation/domain/ConversationServiceHours.java#L36) · RECORD

동일한 DB 판정 시각에서 계산한 서비스 날짜와 예정 종료 시각이다.

<a id="m349"></a>
### ServiceWindow 생성자 — 자동 생성

```java
public void <init>(java.time.LocalDate serviceDate, java.time.OffsetDateTime scheduledEndAt)
```

호출자: [conversation.domain.ConversationServiceHours.requireOpen](#m347)

내부 호출: 없음

외부 라이브러리 호출: `java.lang.Record.<init>()`

<a id="m350"></a>
### equals — 자동 생성

```java
public final boolean equals(java.lang.Object o)
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m351"></a>
### hashCode — 자동 생성

```java
public final int hashCode()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m352"></a>
### scheduledEndAt — 자동 생성

```java
public java.time.OffsetDateTime scheduledEndAt()
```

호출자: [conversation.infrastructure.ConversationRepository.insertActive](#m431)

내부 호출: 없음

<a id="m353"></a>
### serviceDate — 자동 생성

```java
public java.time.LocalDate serviceDate()
```

호출자: [conversation.application.ConversationService.decideStart](#m331) / [conversation.application.ConversationService.replay](#m332) / [conversation.infrastructure.ConversationRepository.insertActive](#m431)

내부 호출: 없음

<a id="m354"></a>
### toString — 자동 생성

```java
public final java.lang.String toString()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="c70"></a>
## conversation.dto.ActiveConversationExistsError

정의: [src/main/java/com/dodamdodam/backend/conversation/dto/ActiveConversationExistsError.java:9](../../../src/main/java/com/dodamdodam/backend/conversation/dto/ActiveConversationExistsError.java#L9) · RECORD

일반 오류 스키마를 바꾸지 않고 ACTIVE 충돌에서만 기존 대화 ID를 추가한다.

<a id="m355"></a>
### from

```java
public static com.dodamdodam.backend.conversation.dto.ActiveConversationExistsError from(com.dodamdodam.backend.global.error.ApiErrorResponse base, java.util.UUID conversationId)
```

정의: [src/main/java/com/dodamdodam/backend/conversation/dto/ActiveConversationExistsError.java:12](../../../src/main/java/com/dodamdodam/backend/conversation/dto/ActiveConversationExistsError.java#L12)

공통 오류 작성기가 만든 메타데이터를 그대로 재사용한다.

호출자: [conversation.api.ConversationExceptionHandler.handle](#m293)

내부 호출: [conversation.dto.ActiveConversationExistsError.ActiveConversationExistsError](#m356) / [conversation.dto.ActiveConversationExistsError.ErrorDetail.ErrorDetail](#m361) / [global.error.ApiErrorResponse.error](#m489) / [global.error.ApiErrorResponse.ErrorDetail.code](#m493) / [global.error.ApiErrorResponse.ErrorDetail.fields](#m495) / [global.error.ApiErrorResponse.ErrorDetail.message](#m497) / [global.error.ApiErrorResponse.ErrorDetail.requestId](#m498)

<a id="m356"></a>
### ActiveConversationExistsError 생성자 — 자동 생성

```java
public void <init>(com.dodamdodam.backend.conversation.dto.ActiveConversationExistsError.ErrorDetail error)
```

호출자: [conversation.dto.ActiveConversationExistsError.from](#m355)

내부 호출: 없음

외부 라이브러리 호출: `java.lang.Record.<init>()`

<a id="m357"></a>
### equals — 자동 생성

```java
public final boolean equals(java.lang.Object o)
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m358"></a>
### error — 자동 생성

```java
public com.dodamdodam.backend.conversation.dto.ActiveConversationExistsError.ErrorDetail error()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m359"></a>
### hashCode — 자동 생성

```java
public final int hashCode()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m360"></a>
### toString — 자동 생성

```java
public final java.lang.String toString()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="c71"></a>
## conversation.dto.ActiveConversationExistsError.ErrorDetail

정의: [src/main/java/com/dodamdodam/backend/conversation/dto/ActiveConversationExistsError.java:18](../../../src/main/java/com/dodamdodam/backend/conversation/dto/ActiveConversationExistsError.java#L18) · RECORD

<a id="m361"></a>
### ErrorDetail 생성자 — 자동 생성

```java
public void <init>(java.lang.String code, java.lang.String message, java.lang.String requestId, java.util.List<com.dodamdodam.backend.global.error.ApiErrorResponse.FieldError> fields, java.util.UUID activeConversationId)
```

호출자: [conversation.dto.ActiveConversationExistsError.from](#m355)

내부 호출: 없음

외부 라이브러리 호출: `java.lang.Record.<init>()`

<a id="m362"></a>
### activeConversationId — 자동 생성

```java
public java.util.UUID activeConversationId()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m363"></a>
### code — 자동 생성

```java
public java.lang.String code()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m364"></a>
### equals — 자동 생성

```java
public final boolean equals(java.lang.Object o)
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m365"></a>
### fields — 자동 생성

```java
public java.util.List<com.dodamdodam.backend.global.error.ApiErrorResponse.FieldError> fields()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m366"></a>
### hashCode — 자동 생성

```java
public final int hashCode()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m367"></a>
### message — 자동 생성

```java
public java.lang.String message()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m368"></a>
### requestId — 자동 생성

```java
public java.lang.String requestId()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m369"></a>
### toString — 자동 생성

```java
public final java.lang.String toString()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="c72"></a>
## conversation.dto.StartConversationRequest

정의: [src/main/java/com/dodamdodam/backend/conversation/dto/StartConversationRequest.java:9](../../../src/main/java/com/dodamdodam/backend/conversation/dto/StartConversationRequest.java#L9) · RECORD

<a id="m370"></a>
### StartConversationRequest 생성자

```java
public void <init>(java.util.UUID clientRequestId)
```

정의: [src/main/java/com/dodamdodam/backend/conversation/dto/StartConversationRequest.java:16](../../../src/main/java/com/dodamdodam/backend/conversation/dto/StartConversationRequest.java#L16)

직접 생성한 요청도 필수 시작 키를 가져야 한다.

호출자: [conversation.dto.StartConversationRequest.fromJson](#m371)

내부 호출: [conversation.exception.ConversationException.validation](#m389)

외부 라이브러리 호출: `java.lang.Record.<init>()`

<a id="m371"></a>
### fromJson

```java
public static com.dodamdodam.backend.conversation.dto.StartConversationRequest fromJson(tools.jackson.databind.JsonNode body)
```

정의: [src/main/java/com/dodamdodam/backend/conversation/dto/StartConversationRequest.java:23](../../../src/main/java/com/dodamdodam/backend/conversation/dto/StartConversationRequest.java#L23)

추가 필드·타입 강제 변환 없이 명세의 단일 UUID 문자열을 읽는다.

호출자: [conversation.api.ConversationController.prepareStart](#m284)

내부 호출: [conversation.dto.StartConversationRequest.StartConversationRequest](#m370) / [conversation.dto.StartConversationRequest.parseUuid](#m373) / [conversation.exception.ConversationException.validation](#m389)

외부 라이브러리 호출: `java.util.Set.containsAll(java.util.Collection<?>)` / `tools.jackson.databind.JsonNode.get(java.lang.String)` / `tools.jackson.databind.JsonNode.isObject()` / `tools.jackson.databind.JsonNode.isString()` / `tools.jackson.databind.JsonNode.propertyNames()` / `tools.jackson.databind.JsonNode.stringValue()`

<a id="m372"></a>
### parseChildId

```java
public static java.util.UUID parseChildId(java.lang.String childId)
```

정의: [src/main/java/com/dodamdodam/backend/conversation/dto/StartConversationRequest.java:36](../../../src/main/java/com/dodamdodam/backend/conversation/dto/StartConversationRequest.java#L36)

경로의 자녀 UUID에도 본문과 같은 완전한 문자열 형식을 적용한다.

호출자: [conversation.api.ConversationController.prepareStart](#m284)

내부 호출: [conversation.dto.StartConversationRequest.parseUuid](#m373)

<a id="m373"></a>
### parseUuid

```java
private static java.util.UUID parseUuid(java.lang.String value, java.lang.String field)
```

정의: [src/main/java/com/dodamdodam/backend/conversation/dto/StartConversationRequest.java:41](../../../src/main/java/com/dodamdodam/backend/conversation/dto/StartConversationRequest.java#L41)

UUID.fromString이 허용하는 축약형과 공백·개행을 먼저 거부한다.

호출자: [conversation.dto.StartConversationRequest.fromJson](#m371) / [conversation.dto.StartConversationRequest.parseChildId](#m372)

내부 호출: [conversation.exception.ConversationException.validation](#m389)

외부 라이브러리 호출: `java.util.UUID.fromString(java.lang.String)` / `java.util.regex.Matcher.matches()` / `java.util.regex.Pattern.matcher(java.lang.CharSequence)`

<a id="m374"></a>
### clientRequestId — 자동 생성

```java
public java.util.UUID clientRequestId()
```

호출자: [conversation.api.ConversationController.start](#m283) / [conversation.application.ConversationService.decideStart](#m331)

내부 호출: 없음

<a id="m375"></a>
### equals — 자동 생성

```java
public final boolean equals(java.lang.Object o)
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m376"></a>
### hashCode — 자동 생성

```java
public final int hashCode()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m377"></a>
### toString — 자동 생성

```java
public final java.lang.String toString()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="c73"></a>
## conversation.dto.StartConversationResult

정의: [src/main/java/com/dodamdodam/backend/conversation/dto/StartConversationResult.java:11](../../../src/main/java/com/dodamdodam/backend/conversation/dto/StartConversationResult.java#L11) · RECORD

저장된 시작 정보만 공개하며, 재전송 시 새 시각이나 대화 ID를 만들지 않는다.

<a id="m378"></a>
### StartConversationResult 생성자

```java
public void <init>(java.util.UUID conversationId, java.lang.String status, java.time.OffsetDateTime startedAt, java.time.LocalDate serviceDate, java.time.OffsetDateTime scheduledEndAt)
```

정의: [src/main/java/com/dodamdodam/backend/conversation/dto/StartConversationResult.java:15](../../../src/main/java/com/dodamdodam/backend/conversation/dto/StartConversationResult.java#L15)

필수 공개 필드를 확인하고 같은 순간을 KST 초 정밀도로 표현한다.

호출자: [conversation.infrastructure.ConversationRepository.insertActive](#m431) / [conversation.infrastructure.ConversationRepository.StoredConversation.startResult](#m433)

내부 호출: 없음

외부 라이브러리 호출: `java.lang.IllegalArgumentException.<init>(java.lang.String)` / `java.lang.Record.<init>()` / `java.lang.String.equals(java.lang.Object)` / `java.time.OffsetDateTime.truncatedTo(java.time.temporal.TemporalUnit)` / `java.time.OffsetDateTime.withOffsetSameInstant(java.time.ZoneOffset)` / `java.time.ZoneOffset.ofHours(int)` / `java.util.Objects.requireNonNull(T,java.lang.String)`

<a id="m379"></a>
### conversationId — 자동 생성

```java
public java.util.UUID conversationId()
```

호출자: [conversation.api.ConversationController.start](#m283) / [conversation.application.ConversationService.decideStart](#m331)

내부 호출: 없음

<a id="m380"></a>
### equals — 자동 생성

```java
public final boolean equals(java.lang.Object o)
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m381"></a>
### hashCode — 자동 생성

```java
public final int hashCode()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m382"></a>
### scheduledEndAt — 자동 생성

```java
public java.time.OffsetDateTime scheduledEndAt()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m383"></a>
### serviceDate — 자동 생성

```java
public java.time.LocalDate serviceDate()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m384"></a>
### startedAt — 자동 생성

```java
public java.time.OffsetDateTime startedAt()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m385"></a>
### status — 자동 생성

```java
public java.lang.String status()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m386"></a>
### toString — 자동 생성

```java
public final java.lang.String toString()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="c74"></a>
## conversation.exception.ConversationException

정의: [src/main/java/com/dodamdodam/backend/conversation/exception/ConversationException.java:8](../../../src/main/java/com/dodamdodam/backend/conversation/exception/ConversationException.java#L8) · CLASS

<a id="m387"></a>
### ConversationException 생성자

```java
private void <init>(java.lang.String code, java.lang.String message, java.util.List<com.dodamdodam.backend.global.error.ApiErrorResponse.FieldError> fields, java.util.UUID activeConversationId, java.lang.Throwable cause)
```

정의: [src/main/java/com/dodamdodam/backend/conversation/exception/ConversationException.java:15](../../../src/main/java/com/dodamdodam/backend/conversation/exception/ConversationException.java#L15)

입력 원문이나 내부 식별자를 포함하지 않는 고정 오류를 구성한다.

호출자: [conversation.exception.ConversationException.ConversationException](#m388) / [conversation.exception.ConversationException.activeExists](#m392) / [conversation.exception.ConversationException.childSessionRequired](#m391) / [conversation.exception.ConversationException.closing](#m393) / [conversation.exception.ConversationException.ended](#m394) / [conversation.exception.ConversationException.serviceHoursClosed](#m390) / [conversation.exception.ConversationException.validation](#m389)

내부 호출: 없음

외부 라이브러리 호출: `java.lang.RuntimeException.<init>(java.lang.String,java.lang.Throwable)` / `java.util.List.copyOf(java.util.Collection<? extends E>)`

<a id="m388"></a>
### ConversationException 생성자

```java
public void <init>(java.lang.Throwable cause)
```

정의: [src/main/java/com/dodamdodam/backend/conversation/exception/ConversationException.java:24](../../../src/main/java/com/dodamdodam/backend/conversation/exception/ConversationException.java#L24)

업무 DB 오류는 원인을 보존하되 공통 500 응답에 원문을 노출하지 않는다.

호출자: [conversation.api.ConversationStartResponseFilter.doFilterInternal](#m301) / [conversation.application.ConversationService.conflictFor](#m333) / [conversation.application.ConversationService.decideStart](#m331) / [conversation.infrastructure.ConversationClosureRepository.beginClosing](#m406) / [conversation.infrastructure.ConversationClosureRepository.expireProcessing](#m408) / [conversation.infrastructure.ConversationClosureRepository.findForUpdate](#m405) / [conversation.infrastructure.ConversationClosureRepository.finishClosing](#m412) / [conversation.infrastructure.ConversationClosureRepository.grantRecovery](#m413) / [conversation.infrastructure.ConversationClosureRepository.hasProcessing](#m409) / [conversation.infrastructure.ConversationClosureRepository.hasSummaryInput](#m410) / [conversation.infrastructure.ConversationClosureRepository.lockAccount](#m404) / [conversation.infrastructure.ConversationClosureRepository.lockLinks](#m411) / [conversation.infrastructure.ConversationClosureRepository.lockProcessingTurns](#m407) / [conversation.infrastructure.ConversationClosureRepository.requireTransition](#m414) / [conversation.infrastructure.ConversationRepository.currentTime](#m427) / [conversation.infrastructure.ConversationRepository.findRequestForUpdate](#m428) / [conversation.infrastructure.ConversationRepository.findUnendedForUpdate](#m429) / [conversation.infrastructure.ConversationRepository.hasLinkForUpdate](#m430) / [conversation.infrastructure.ConversationRepository.insertActive](#m431) / [conversation.infrastructure.ConversationRepository.insertLink](#m432)

내부 호출: [conversation.exception.ConversationException.ConversationException](#m387)

외부 라이브러리 호출: `java.util.List.of()`

<a id="m389"></a>
### validation

```java
public static com.dodamdodam.backend.conversation.exception.ConversationException validation(java.lang.String field)
```

정의: [src/main/java/com/dodamdodam/backend/conversation/exception/ConversationException.java:29](../../../src/main/java/com/dodamdodam/backend/conversation/exception/ConversationException.java#L29)

정의된 필드 이름만 공개하고 잘못된 입력값은 오류에 복사하지 않는다.

호출자: [conversation.dto.StartConversationRequest.StartConversationRequest](#m370) / [conversation.dto.StartConversationRequest.fromJson](#m371) / [conversation.dto.StartConversationRequest.parseUuid](#m373)

내부 호출: [conversation.exception.ConversationException.ConversationException](#m387) / [global.error.ApiErrorResponse.FieldError.FieldError](#m500)

외부 라이브러리 호출: `java.util.List.of(E)`

<a id="m390"></a>
### serviceHoursClosed

```java
public static com.dodamdodam.backend.conversation.exception.ConversationException serviceHoursClosed()
```

정의: [src/main/java/com/dodamdodam/backend/conversation/exception/ConversationException.java:35](../../../src/main/java/com/dodamdodam/backend/conversation/exception/ConversationException.java#L35)

아이 대화 이용시간 밖의 요청을 공통 403 업무 오류로 표현한다.

호출자: [conversation.application.ConversationService.decideStart](#m331) / [conversation.application.ConversationService.replay](#m332) / [conversation.application.ConversationService.verifyStartResponse](#m330) / [conversation.domain.ConversationServiceHours.requireOpen](#m347)

내부 호출: [conversation.exception.ConversationException.ConversationException](#m387)

외부 라이브러리 호출: `java.util.List.of()`

<a id="m391"></a>
### childSessionRequired

```java
public static com.dodamdodam.backend.conversation.exception.ConversationException childSessionRequired()
```

정의: [src/main/java/com/dodamdodam/backend/conversation/exception/ConversationException.java:40](../../../src/main/java/com/dodamdodam/backend/conversation/exception/ConversationException.java#L40)

같은 시작 키라도 현재 보안 문맥에 연결되지 않았다면 명시적 이어하기가 필요하다.

호출자: [conversation.application.ConversationService.replay](#m332)

내부 호출: [conversation.exception.ConversationException.ConversationException](#m387)

외부 라이브러리 호출: `java.util.List.of()`

<a id="m392"></a>
### activeExists

```java
public static com.dodamdodam.backend.conversation.exception.ConversationException activeExists(java.util.UUID conversationId)
```

정의: [src/main/java/com/dodamdodam/backend/conversation/exception/ConversationException.java:45](../../../src/main/java/com/dodamdodam/backend/conversation/exception/ConversationException.java#L45)

현재 ACTIVE가 있는 경우에만 명세가 허용하는 기존 대화 ID를 공개한다.

호출자: [conversation.application.ConversationService.conflictFor](#m333)

내부 호출: [conversation.exception.ConversationException.ConversationException](#m387)

외부 라이브러리 호출: `java.util.List.of()` / `java.util.Objects.requireNonNull(T,java.lang.String)`

<a id="m393"></a>
### closing

```java
public static com.dodamdodam.backend.conversation.exception.ConversationException closing()
```

정의: [src/main/java/com/dodamdodam/backend/conversation/exception/ConversationException.java:51](../../../src/main/java/com/dodamdodam/backend/conversation/exception/ConversationException.java#L51)

종료 조정 중에는 새 대화를 만들지 않는다.

호출자: [conversation.application.ConversationService.conflictFor](#m333)

내부 호출: [conversation.exception.ConversationException.ConversationException](#m387)

외부 라이브러리 호출: `java.util.List.of()`

<a id="m394"></a>
### ended

```java
public static com.dodamdodam.backend.conversation.exception.ConversationException ended()
```

정의: [src/main/java/com/dodamdodam/backend/conversation/exception/ConversationException.java:56](../../../src/main/java/com/dodamdodam/backend/conversation/exception/ConversationException.java#L56)

종료된 시작 키의 재사용으로 새로운 행을 만들지 않는다.

호출자: [conversation.application.ConversationService.conflictFor](#m333)

내부 호출: [conversation.exception.ConversationException.ConversationException](#m387)

외부 라이브러리 호출: `java.util.List.of()`

<a id="m395"></a>
### code

```java
public java.lang.String code()
```

정의: [src/main/java/com/dodamdodam/backend/conversation/exception/ConversationException.java:61](../../../src/main/java/com/dodamdodam/backend/conversation/exception/ConversationException.java#L61)

HTTP 처리 계층에서 사용할 업무 오류 코드를 반환한다.

호출자: [conversation.api.ConversationExceptionHandler.handle](#m293)

내부 호출: 없음

<a id="m396"></a>
### fields

```java
public java.util.List<com.dodamdodam.backend.global.error.ApiErrorResponse.FieldError> fields()
```

정의: [src/main/java/com/dodamdodam/backend/conversation/exception/ConversationException.java:66](../../../src/main/java/com/dodamdodam/backend/conversation/exception/ConversationException.java#L66)

공통 오류 응답에 담을 검증 오류 목록을 반환한다.

호출자: [conversation.api.ConversationExceptionHandler.handle](#m293)

내부 호출: 없음

<a id="m397"></a>
### activeConversationId

```java
public java.util.UUID activeConversationId()
```

정의: [src/main/java/com/dodamdodam/backend/conversation/exception/ConversationException.java:71](../../../src/main/java/com/dodamdodam/backend/conversation/exception/ConversationException.java#L71)

ACTIVE 충돌 전용 응답에서만 사용할 기존 대화 ID다.

호출자: [conversation.api.ConversationExceptionHandler.handle](#m293)

내부 호출: 없음

<a id="c75"></a>
## conversation.infrastructure.ConversationClosureProperties

정의: [src/main/java/com/dodamdodam/backend/conversation/infrastructure/ConversationClosureProperties.java:8](../../../src/main/java/com/dodamdodam/backend/conversation/infrastructure/ConversationClosureProperties.java#L8) · RECORD

미확정 요약 처리 기한은 운영 기본값 없이 명시적으로 주입한다.

설정: `@org.springframework.boot.context.properties.ConfigurationProperties("app.conversation")`

<a id="m398"></a>
### ConversationClosureProperties 생성자

```java
public void <init>(java.time.Duration summaryProcessingTimeout)
```

정의: [src/main/java/com/dodamdodam/backend/conversation/infrastructure/ConversationClosureProperties.java:12](../../../src/main/java/com/dodamdodam/backend/conversation/infrastructure/ConversationClosureProperties.java#L12)

NULL·0·음수와 PostgreSQL 마이크로초보다 세밀한 정밀도를 거부한다.

호출자: 정적 Java 호출 없음

내부 호출: 없음

외부 라이브러리 호출: `java.lang.IllegalArgumentException.<init>(java.lang.String)` / `java.lang.Record.<init>()` / `java.time.Duration.getNano()` / `java.time.Duration.isNegative()` / `java.time.Duration.isZero()` / `java.util.Objects.requireNonNull(T,java.lang.String)`

<a id="m399"></a>
### equals — 자동 생성

```java
public final boolean equals(java.lang.Object o)
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m400"></a>
### hashCode — 자동 생성

```java
public final int hashCode()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m401"></a>
### summaryProcessingTimeout — 자동 생성

```java
public java.time.Duration summaryProcessingTimeout()
```

호출자: [conversation.application.ConversationClosureCoordinator.reconcile](#m317)

내부 호출: 없음

<a id="m402"></a>
### toString — 자동 생성

```java
public final java.lang.String toString()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="c76"></a>
## conversation.infrastructure.ConversationClosureRepository

정의: [src/main/java/com/dodamdodam/backend/conversation/infrastructure/ConversationClosureRepository.java:13](../../../src/main/java/com/dodamdodam/backend/conversation/infrastructure/ConversationClosureRepository.java#L13) · CLASS

설정: `@org.springframework.stereotype.Repository` · `@org.springframework.transaction.annotation.Transactional(propagation=MANDATORY)`

<a id="m403"></a>
### ConversationClosureRepository 생성자

```java
public void <init>(org.springframework.jdbc.core.JdbcTemplate jdbc)
```

정의: [src/main/java/com/dodamdodam/backend/conversation/infrastructure/ConversationClosureRepository.java:20](../../../src/main/java/com/dodamdodam/backend/conversation/infrastructure/ConversationClosureRepository.java#L20)

종료 전이·발화 만료·복구 링크를 호출자의 동일 업무 트랜잭션에 묶는다.

호출자: 정적 Java 호출 없음

내부 호출: 없음

외부 라이브러리 호출: `java.lang.Object.<init>()`

<a id="m404"></a>
### lockAccount

```java
public boolean lockAccount(java.util.UUID accountId)
```

정의: [src/main/java/com/dodamdodam/backend/conversation/infrastructure/ConversationClosureRepository.java:25](../../../src/main/java/com/dodamdodam/backend/conversation/infrastructure/ConversationClosureRepository.java#L25)

계정을 먼저 잠가 다른 문맥의 폐기·버전 변경과 복구 자격 확정이 엇갈리지 않게 한다.

호출자: [conversation.application.ConversationClosureCoordinator.reconcile](#m317)

내부 호출: [conversation.exception.ConversationException.ConversationException](#m388)

외부 라이브러리 호출: `java.util.List.isEmpty()` / `org.springframework.jdbc.core.JdbcTemplate.queryForList(java.lang.String,java.lang.Class<T>,java.lang.@org.jspecify.annotations.Nullable Object @org.jspecify.annotations.Nullable [])`

<a id="m405"></a>
### findForUpdate

```java
public java.util.Optional<com.dodamdodam.backend.conversation.infrastructure.ConversationClosureRepository.ClosureState> findForUpdate(java.util.UUID childId, java.util.UUID conversationId)
```

정의: [src/main/java/com/dodamdodam/backend/conversation/infrastructure/ConversationClosureRepository.java:35](../../../src/main/java/com/dodamdodam/backend/conversation/infrastructure/ConversationClosureRepository.java#L35)

자녀 잠금 이후 부모-자식 조합을 확인하며 종료 대상 대화를 잠근다.

호출자: [conversation.application.ConversationClosureCoordinator.requireState](#m318)

내부 호출: [conversation.exception.ConversationException.ConversationException](#m388) / [conversation.infrastructure.ConversationClosureRepository.ClosureState.ClosureState](#m415)

외부 라이브러리 호출: `java.sql.ResultSet.getObject(java.lang.String,java.lang.Class<T>)` / `java.sql.ResultSet.getString(java.lang.String)` / `java.util.Collection.stream()` / `java.util.stream.Stream.findFirst()` / `org.springframework.jdbc.core.JdbcTemplate.query(java.lang.String,org.springframework.jdbc.core.RowMapper<T>,java.lang.@org.jspecify.annotations.Nullable Object @org.jspecify.annotations.Nullable [])`

<a id="m406"></a>
### beginClosing

```java
public void beginClosing(java.util.UUID conversationId, java.time.OffsetDateTime boundary, java.lang.String reason)
```

정의: [src/main/java/com/dodamdodam/backend/conversation/infrastructure/ConversationClosureRepository.java:51](../../../src/main/java/com/dodamdodam/backend/conversation/infrastructure/ConversationClosureRepository.java#L51)

ACTIVE의 최초 경계만 접수하며 이미 저장된 MANUAL/MIDNIGHT는 덮어쓰지 않는다.

호출자: [conversation.application.ConversationClosureCoordinator.reconcile](#m317)

내부 호출: [conversation.exception.ConversationException.ConversationException](#m388) / [conversation.infrastructure.ConversationClosureRepository.requireTransition](#m414)

외부 라이브러리 호출: `org.springframework.jdbc.core.JdbcTemplate.update(java.lang.String,java.lang.@org.jspecify.annotations.Nullable Object @org.jspecify.annotations.Nullable [])`

<a id="m407"></a>
### lockProcessingTurns

```java
public void lockProcessingTurns(java.util.UUID conversationId)
```

정의: [src/main/java/com/dodamdodam/backend/conversation/infrastructure/ConversationClosureRepository.java:64](../../../src/main/java/com/dodamdodam/backend/conversation/infrastructure/ConversationClosureRepository.java#L64)

진행 중 발화를 먼저 잠그며 완료 처리와 기다린 뒤의 시각 판정을 직렬화한다.

호출자: [conversation.application.ConversationClosureCoordinator.reconcile](#m317)

내부 호출: [conversation.exception.ConversationException.ConversationException](#m388)

외부 라이브러리 호출: `org.springframework.jdbc.core.JdbcTemplate.queryForList(java.lang.String,java.lang.Class<T>,java.lang.@org.jspecify.annotations.Nullable Object @org.jspecify.annotations.Nullable [])`

<a id="m408"></a>
### expireProcessing

```java
public void expireProcessing(java.util.UUID conversationId, java.time.OffsetDateTime checkedAt)
```

정의: [src/main/java/com/dodamdodam/backend/conversation/infrastructure/ConversationClosureRepository.java:76](../../../src/main/java/com/dodamdodam/backend/conversation/infrastructure/ConversationClosureRepository.java#L76)

원래 기한에 도달한 PROCESSING만 실패시키고 terminal 결과·기한·텍스트는 변경하지 않는다.

호출자: [conversation.application.ConversationClosureCoordinator.reconcile](#m317)

내부 호출: [conversation.exception.ConversationException.ConversationException](#m388)

외부 라이브러리 호출: `org.springframework.jdbc.core.JdbcTemplate.update(java.lang.String,java.lang.@org.jspecify.annotations.Nullable Object @org.jspecify.annotations.Nullable [])`

<a id="m409"></a>
### hasProcessing

```java
public boolean hasProcessing(java.util.UUID conversationId)
```

정의: [src/main/java/com/dodamdodam/backend/conversation/infrastructure/ConversationClosureRepository.java:89](../../../src/main/java/com/dodamdodam/backend/conversation/infrastructure/ConversationClosureRepository.java#L89)

모든 발화 terminal 여부는 실제 DB의 남은 PROCESSING으로 판단한다.

호출자: [conversation.application.ConversationClosureCoordinator.reconcile](#m317)

내부 호출: [conversation.exception.ConversationException.ConversationException](#m388)

외부 라이브러리 호출: `java.lang.Boolean.equals(java.lang.Object)` / `org.springframework.jdbc.core.JdbcTemplate.queryForObject(java.lang.String,java.lang.Class<T>,java.lang.@org.jspecify.annotations.Nullable Object @org.jspecify.annotations.Nullable [])`

<a id="m410"></a>
### hasSummaryInput

```java
public boolean hasSummaryInput(java.util.UUID conversationId)
```

정의: [src/main/java/com/dodamdodam/backend/conversation/infrastructure/ConversationClosureRepository.java:101](../../../src/main/java/com/dodamdodam/backend/conversation/infrastructure/ConversationClosureRepository.java#L101)

실패 발화를 포함해 저장·표시가 허용된 nonnull 텍스트가 하나라도 있는지 확인한다.

호출자: [conversation.application.ConversationClosureCoordinator.reconcile](#m317)

내부 호출: [conversation.exception.ConversationException.ConversationException](#m388)

외부 라이브러리 호출: `java.lang.Boolean.equals(java.lang.Object)` / `org.springframework.jdbc.core.JdbcTemplate.queryForObject(java.lang.String,java.lang.Class<T>,java.lang.@org.jspecify.annotations.Nullable Object @org.jspecify.annotations.Nullable [])`

<a id="m411"></a>
### lockLinks

```java
public void lockLinks(java.util.UUID conversationId)
```

정의: [src/main/java/com/dodamdodam/backend/conversation/infrastructure/ConversationClosureRepository.java:116](../../../src/main/java/com/dodamdodam/backend/conversation/infrastructure/ConversationClosureRepository.java#L116)

복구 대상 확정 전 연결들을 일정한 순서로 잠근다.

호출자: [conversation.application.ConversationClosureCoordinator.reconcile](#m317)

내부 호출: [conversation.exception.ConversationException.ConversationException](#m388)

외부 라이브러리 호출: `org.springframework.jdbc.core.JdbcTemplate.queryForList(java.lang.String,java.lang.Class<T>,java.lang.@org.jspecify.annotations.Nullable Object @org.jspecify.annotations.Nullable [])`

<a id="m412"></a>
### finishClosing

```java
public void finishClosing(java.util.UUID conversationId, java.time.OffsetDateTime endedAt, java.time.OffsetDateTime summaryDeadline)
```

정의: [src/main/java/com/dodamdodam/backend/conversation/infrastructure/ConversationClosureRepository.java:129](../../../src/main/java/com/dodamdodam/backend/conversation/infrastructure/ConversationClosureRepository.java#L129)

최초 ENDED 전이에서만 실제 완료 시각과 EMPTY/PENDING의 필수 NULL 조합을 기록한다.

호출자: [conversation.application.ConversationClosureCoordinator.reconcile](#m317)

내부 호출: [conversation.exception.ConversationException.ConversationException](#m388) / [conversation.infrastructure.ConversationClosureRepository.requireTransition](#m414)

외부 라이브러리 호출: `org.springframework.jdbc.core.JdbcTemplate.update(java.lang.String,java.lang.@org.jspecify.annotations.Nullable Object @org.jspecify.annotations.Nullable [])`

<a id="m413"></a>
### grantRecovery

```java
public void grantRecovery(java.util.UUID childId, java.util.UUID conversationId, java.time.OffsetDateTime boundary, java.time.OffsetDateTime endedAt)
```

정의: [src/main/java/com/dodamdodam/backend/conversation/infrastructure/ConversationClosureRepository.java:146](../../../src/main/java/com/dodamdodam/backend/conversation/infrastructure/ConversationClosureRepository.java#L146)

같은 소유 계정의 경계 이전 유효 연결에만 최초 endedAt+600초를 원자적으로 기록한다.

호출자: [conversation.application.ConversationClosureCoordinator.reconcile](#m317)

내부 호출: [conversation.exception.ConversationException.ConversationException](#m388)

외부 라이브러리 호출: `java.time.OffsetDateTime.plusSeconds(long)` / `org.springframework.jdbc.core.JdbcTemplate.update(java.lang.String,java.lang.@org.jspecify.annotations.Nullable Object @org.jspecify.annotations.Nullable [])`

<a id="m414"></a>
### requireTransition

```java
private void requireTransition(int count)
```

정의: [src/main/java/com/dodamdodam/backend/conversation/infrastructure/ConversationClosureRepository.java:164](../../../src/main/java/com/dodamdodam/backend/conversation/infrastructure/ConversationClosureRepository.java#L164)

잠근 행의 조건부 전이가 유실되면 성공으로 추정하지 않고 종료 전체를 롤백한다.

호출자: [conversation.infrastructure.ConversationClosureRepository.beginClosing](#m406) / [conversation.infrastructure.ConversationClosureRepository.finishClosing](#m412)

내부 호출: [conversation.exception.ConversationException.ConversationException](#m388)

외부 라이브러리 호출: `java.lang.IllegalStateException.<init>(java.lang.String)`

<a id="c77"></a>
## conversation.infrastructure.ConversationClosureRepository.ClosureState

정의: [src/main/java/com/dodamdodam/backend/conversation/infrastructure/ConversationClosureRepository.java:171](../../../src/main/java/com/dodamdodam/backend/conversation/infrastructure/ConversationClosureRepository.java#L171) · RECORD

종료 확인 API가 재사용할 저장 상태이며 아이 내용·발화는 포함하지 않는다.

<a id="m415"></a>
### ClosureState 생성자 — 자동 생성

```java
public void <init>(java.util.UUID conversationId, java.lang.String status, java.time.OffsetDateTime scheduledEndAt, java.time.OffsetDateTime endRequestedAt, java.lang.String endReason, java.time.OffsetDateTime endedAt, java.lang.String summaryStatus)
```

호출자: [conversation.infrastructure.ConversationClosureRepository.findForUpdate](#m405)

내부 호출: 없음

외부 라이브러리 호출: `java.lang.Record.<init>()`

<a id="m416"></a>
### conversationId — 자동 생성

```java
public java.util.UUID conversationId()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m417"></a>
### endReason — 자동 생성

```java
public java.lang.String endReason()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m418"></a>
### endRequestedAt — 자동 생성

```java
public java.time.OffsetDateTime endRequestedAt()
```

호출자: [conversation.application.ConversationClosureCoordinator.reconcile](#m317)

내부 호출: 없음

<a id="m419"></a>
### endedAt — 자동 생성

```java
public java.time.OffsetDateTime endedAt()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m420"></a>
### equals — 자동 생성

```java
public final boolean equals(java.lang.Object o)
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m421"></a>
### hashCode — 자동 생성

```java
public final int hashCode()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m422"></a>
### scheduledEndAt — 자동 생성

```java
public java.time.OffsetDateTime scheduledEndAt()
```

호출자: [conversation.application.ConversationClosureCoordinator.reconcile](#m317)

내부 호출: 없음

<a id="m423"></a>
### status — 자동 생성

```java
public java.lang.String status()
```

호출자: [conversation.application.ConversationClosureCoordinator.reconcile](#m317)

내부 호출: 없음

<a id="m424"></a>
### summaryStatus — 자동 생성

```java
public java.lang.String summaryStatus()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m425"></a>
### toString — 자동 생성

```java
public final java.lang.String toString()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="c78"></a>
## conversation.infrastructure.ConversationRepository

정의: [src/main/java/com/dodamdodam/backend/conversation/infrastructure/ConversationRepository.java:17](../../../src/main/java/com/dodamdodam/backend/conversation/infrastructure/ConversationRepository.java#L17) · CLASS

설정: `@org.springframework.stereotype.Repository`

<a id="m426"></a>
### ConversationRepository 생성자

```java
public void <init>(org.springframework.jdbc.core.JdbcTemplate jdbc)
```

정의: [src/main/java/com/dodamdodam/backend/conversation/infrastructure/ConversationRepository.java:28](../../../src/main/java/com/dodamdodam/backend/conversation/infrastructure/ConversationRepository.java#L28)

기존 업무 DB 연결과 트랜잭션에 참여하는 JDBC 저장소다.

호출자: 정적 Java 호출 없음

내부 호출: 없음

외부 라이브러리 호출: `java.lang.Object.<init>()`

<a id="m427"></a>
### currentTime

```java
public java.time.OffsetDateTime currentTime()
```

정의: [src/main/java/com/dodamdodam/backend/conversation/infrastructure/ConversationRepository.java:33](../../../src/main/java/com/dodamdodam/backend/conversation/infrastructure/ConversationRepository.java#L33)

트랜잭션 시작 시각이 아닌 호출 시점의 DB 시각을 읽는다.

호출자: [conversation.application.ConversationClosureCoordinator.reconcile](#m317) / [conversation.application.ConversationService.decideStart](#m331) / [conversation.application.ConversationService.replay](#m332) / [conversation.application.ConversationService.verifyStartResponse](#m330)

내부 호출: [conversation.exception.ConversationException.ConversationException](#m388)

외부 라이브러리 호출: `java.sql.ResultSet.getObject(int,java.lang.Class<T>)` / `org.springframework.jdbc.core.JdbcTemplate.queryForObject(java.lang.String,org.springframework.jdbc.core.RowMapper<T>)`

<a id="m428"></a>
### findRequestForUpdate

```java
public java.util.Optional<com.dodamdodam.backend.conversation.infrastructure.ConversationRepository.StoredConversation> findRequestForUpdate(java.util.UUID childId, java.util.UUID clientRequestId)
```

정의: [src/main/java/com/dodamdodam/backend/conversation/infrastructure/ConversationRepository.java:43](../../../src/main/java/com/dodamdodam/backend/conversation/infrastructure/ConversationRepository.java#L43)

자녀 잠금 뒤 시작 키에 해당하는 대화 상태를 잠가 확인한다.

애너테이션: `@org.springframework.transaction.annotation.Transactional(propagation=MANDATORY)`

호출자: [conversation.application.ConversationService.decideStart](#m331) / [conversation.application.ConversationService.verifyStartResponse](#m330)

내부 호출: [conversation.exception.ConversationException.ConversationException](#m388)

외부 라이브러리 호출: `java.util.Collection.stream()` / `java.util.stream.Stream.findFirst()` / `org.springframework.jdbc.core.JdbcTemplate.query(java.lang.String,org.springframework.jdbc.core.RowMapper<T>,java.lang.@org.jspecify.annotations.Nullable Object @org.jspecify.annotations.Nullable [])`

<a id="m429"></a>
### findUnendedForUpdate

```java
public java.util.Optional<com.dodamdodam.backend.conversation.infrastructure.ConversationRepository.StoredConversation> findUnendedForUpdate(java.util.UUID childId)
```

정의: [src/main/java/com/dodamdodam/backend/conversation/infrastructure/ConversationRepository.java:56](../../../src/main/java/com/dodamdodam/backend/conversation/infrastructure/ConversationRepository.java#L56)

같은 자녀의 미종료 행을 잠그며, 종료 조정기와의 상태 변경도 직렬화한다.

애너테이션: `@org.springframework.transaction.annotation.Transactional(propagation=MANDATORY)`

호출자: [conversation.application.ConversationService.decideStart](#m331)

내부 호출: [conversation.exception.ConversationException.ConversationException](#m388)

외부 라이브러리 호출: `java.util.Collection.stream()` / `java.util.stream.Stream.findFirst()` / `org.springframework.jdbc.core.JdbcTemplate.query(java.lang.String,org.springframework.jdbc.core.RowMapper<T>,java.lang.@org.jspecify.annotations.Nullable Object @org.jspecify.annotations.Nullable [])`

<a id="m430"></a>
### hasLinkForUpdate

```java
public boolean hasLinkForUpdate(java.util.UUID securityContextId, java.util.UUID conversationId)
```

정의: [src/main/java/com/dodamdodam/backend/conversation/infrastructure/ConversationRepository.java:69](../../../src/main/java/com/dodamdodam/backend/conversation/infrastructure/ConversationRepository.java#L69)

대화 잠금 뒤 현재 보안 문맥의 기존 연결만 잠가 확인하며 새 연결은 만들지 않는다.

애너테이션: `@org.springframework.transaction.annotation.Transactional(propagation=MANDATORY)`

호출자: [conversation.application.ConversationService.replay](#m332)

내부 호출: [conversation.exception.ConversationException.ConversationException](#m388)

외부 라이브러리 호출: `java.util.List.isEmpty()` / `org.springframework.jdbc.core.JdbcTemplate.queryForList(java.lang.String,java.lang.Class<T>,java.lang.@org.jspecify.annotations.Nullable Object @org.jspecify.annotations.Nullable [])`

<a id="m431"></a>
### insertActive

```java
public java.util.Optional<com.dodamdodam.backend.conversation.dto.StartConversationResult> insertActive(java.util.UUID id, java.util.UUID childId, java.util.UUID clientRequestId, java.time.OffsetDateTime startedAt, com.dodamdodam.backend.conversation.domain.ConversationServiceHours.ServiceWindow window)
```

정의: [src/main/java/com/dodamdodam/backend/conversation/infrastructure/ConversationRepository.java:83](../../../src/main/java/com/dodamdodam/backend/conversation/infrastructure/ConversationRepository.java#L83)

미종료 부분 UNIQUE 충돌만 빈 결과로 구분하고, 다른 제약·저장 장애는 500 원인을 보존한다.

호출자: [conversation.application.ConversationService.decideStart](#m331)

내부 호출: [conversation.domain.ConversationServiceHours.ServiceWindow.scheduledEndAt](#m352) / [conversation.domain.ConversationServiceHours.ServiceWindow.serviceDate](#m353) / [conversation.dto.StartConversationResult.StartConversationResult](#m378) / [conversation.exception.ConversationException.ConversationException](#m388)

외부 라이브러리 호출: `java.sql.ResultSet.getObject(java.lang.String,java.lang.Class<T>)` / `java.sql.ResultSet.getString(java.lang.String)` / `java.util.Collection.stream()` / `java.util.stream.Stream.findFirst()` / `org.springframework.jdbc.core.JdbcTemplate.query(java.lang.String,org.springframework.jdbc.core.RowMapper<T>,java.lang.@org.jspecify.annotations.Nullable Object @org.jspecify.annotations.Nullable [])`

<a id="m432"></a>
### insertLink

```java
public void insertLink(java.util.UUID securityContextId, java.util.UUID conversationId, java.time.OffsetDateTime boundAt)
```

정의: [src/main/java/com/dodamdodam/backend/conversation/infrastructure/ConversationRepository.java:104](../../../src/main/java/com/dodamdodam/backend/conversation/infrastructure/ConversationRepository.java#L104)

생성과 같은 트랜잭션에서 현재 보안 문맥의 최초 연결 시각을 저장한다.

호출자: [conversation.application.ConversationService.decideStart](#m331)

내부 호출: [conversation.exception.ConversationException.ConversationException](#m388)

외부 라이브러리 호출: `org.springframework.jdbc.core.JdbcTemplate.update(java.lang.String,java.lang.@org.jspecify.annotations.Nullable Object @org.jspecify.annotations.Nullable [])`

<a id="c79"></a>
## conversation.infrastructure.ConversationRepository.StoredConversation

정의: [src/main/java/com/dodamdodam/backend/conversation/infrastructure/ConversationRepository.java:117](../../../src/main/java/com/dodamdodam/backend/conversation/infrastructure/ConversationRepository.java#L117) · RECORD

상태 판정 전용 내부 행이며 그대로 API에 노출하지 않는다.

<a id="m433"></a>
### startResult

```java
public com.dodamdodam.backend.conversation.dto.StartConversationResult startResult()
```

정의: [src/main/java/com/dodamdodam/backend/conversation/infrastructure/ConversationRepository.java:121](../../../src/main/java/com/dodamdodam/backend/conversation/infrastructure/ConversationRepository.java#L121)

새 시각이나 ID를 만들지 않고 저장된 ACTIVE 시작 정보만 공개한다.

호출자: [conversation.application.ConversationService.replay](#m332)

내부 호출: [conversation.dto.StartConversationResult.StartConversationResult](#m378)

<a id="m434"></a>
### StoredConversation 생성자 — 자동 생성

```java
public void <init>(java.util.UUID conversationId, java.lang.String status, java.time.OffsetDateTime startedAt, java.time.LocalDate serviceDate, java.time.OffsetDateTime scheduledEndAt)
```

호출자: `conversation.infrastructure.ConversationRepository.<initializer>`

내부 호출: 없음

외부 라이브러리 호출: `java.lang.Record.<init>()`

<a id="m435"></a>
### conversationId — 자동 생성

```java
public java.util.UUID conversationId()
```

호출자: [conversation.application.ConversationService.conflictFor](#m333) / [conversation.application.ConversationService.decideStart](#m331) / [conversation.application.ConversationService.replay](#m332) / [conversation.application.ConversationService.verifyStartResponse](#m330)

내부 호출: 없음

<a id="m436"></a>
### equals — 자동 생성

```java
public final boolean equals(java.lang.Object o)
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m437"></a>
### hashCode — 자동 생성

```java
public final int hashCode()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m438"></a>
### scheduledEndAt — 자동 생성

```java
public java.time.OffsetDateTime scheduledEndAt()
```

호출자: [conversation.application.ConversationService.decideStart](#m331) / [conversation.application.ConversationService.verifyStartResponse](#m330)

내부 호출: 없음

<a id="m439"></a>
### serviceDate — 자동 생성

```java
public java.time.LocalDate serviceDate()
```

호출자: [conversation.application.ConversationService.replay](#m332)

내부 호출: 없음

<a id="m440"></a>
### startedAt — 자동 생성

```java
public java.time.OffsetDateTime startedAt()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m441"></a>
### status — 자동 생성

```java
public java.lang.String status()
```

호출자: [conversation.application.ConversationService.conflictFor](#m333) / [conversation.application.ConversationService.decideStart](#m331) / [conversation.application.ConversationService.replay](#m332) / [conversation.application.ConversationService.verifyStartResponse](#m330)

내부 호출: 없음

<a id="m442"></a>
### toString — 자동 생성

```java
public final java.lang.String toString()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="c80"></a>
## global.auth.CsrfAccessDeniedHandler

정의: [src/main/java/com/dodamdodam/backend/global/auth/CsrfAccessDeniedHandler.java:12](../../../src/main/java/com/dodamdodam/backend/global/auth/CsrfAccessDeniedHandler.java#L12) · CLASS

설정: `@org.springframework.stereotype.Component`

<a id="m443"></a>
### CsrfAccessDeniedHandler 생성자

```java
public void <init>(tools.jackson.databind.json.JsonMapper jsonMapper)
```

정의: [src/main/java/com/dodamdodam/backend/global/auth/CsrfAccessDeniedHandler.java:17](../../../src/main/java/com/dodamdodam/backend/global/auth/CsrfAccessDeniedHandler.java#L17)

호출자: 정적 Java 호출 없음

내부 호출: 없음

외부 라이브러리 호출: `java.lang.Object.<init>()`

<a id="m444"></a>
### handle

```java
public void handle(jakarta.servlet.http.HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response, org.springframework.security.access.AccessDeniedException exception)
```

정의: [src/main/java/com/dodamdodam/backend/global/auth/CsrfAccessDeniedHandler.java:22](../../../src/main/java/com/dodamdodam/backend/global/auth/CsrfAccessDeniedHandler.java#L22)

CSRF 거부를 기존 고정 메시지와 공통 JSON 형식으로 반환한다.

애너테이션: `@java.lang.Override`

호출자: 정적 Java 호출 없음

프레임워크 진입: Spring Security CSRF 거부

내부 호출: [global.error.ApiErrorResponses.write](#m508)

<a id="c81"></a>
## global.auth.CsrfController

정의: [src/main/java/com/dodamdodam/backend/global/auth/CsrfController.java:8](../../../src/main/java/com/dodamdodam/backend/global/auth/CsrfController.java#L8) · CLASS

설정: `@org.springframework.web.bind.annotation.RestController` · `@org.springframework.web.bind.annotation.RequestMapping({"/api/v1/auth"})`

<a id="m445"></a>
### csrf

```java
com.dodamdodam.backend.global.auth.CsrfController.CsrfTokenResponse csrf(org.springframework.security.web.csrf.CsrfToken csrfToken)
```

정의: [src/main/java/com/dodamdodam/backend/global/auth/CsrfController.java:12](../../../src/main/java/com/dodamdodam/backend/global/auth/CsrfController.java#L12)

애너테이션: `@org.springframework.web.bind.annotation.GetMapping({"/csrf"})`

호출자: 정적 Java 호출 없음

프레임워크 진입: GET /api/v1/auth/csrf

내부 호출: [global.auth.CsrfController.CsrfTokenResponse.CsrfTokenResponse](#m447)

외부 라이브러리 호출: `org.springframework.security.web.csrf.CsrfToken.getHeaderName()` / `org.springframework.security.web.csrf.CsrfToken.getParameterName()` / `org.springframework.security.web.csrf.CsrfToken.getToken()`

<a id="m446"></a>
### CsrfController 생성자 — 자동 생성

```java
public void <init>()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

외부 라이브러리 호출: `java.lang.Object.<init>()`

<a id="c82"></a>
## global.auth.CsrfController.CsrfTokenResponse

정의: [src/main/java/com/dodamdodam/backend/global/auth/CsrfController.java:21](../../../src/main/java/com/dodamdodam/backend/global/auth/CsrfController.java#L21) · RECORD

<a id="m447"></a>
### CsrfTokenResponse 생성자 — 자동 생성

```java
void <init>(java.lang.String headerName, java.lang.String parameterName, java.lang.String token)
```

호출자: [global.auth.CsrfController.csrf](#m445)

내부 호출: 없음

외부 라이브러리 호출: `java.lang.Record.<init>()`

<a id="m448"></a>
### equals — 자동 생성

```java
public final boolean equals(java.lang.Object o)
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m449"></a>
### hashCode — 자동 생성

```java
public final int hashCode()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m450"></a>
### headerName — 자동 생성

```java
public java.lang.String headerName()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m451"></a>
### parameterName — 자동 생성

```java
public java.lang.String parameterName()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m452"></a>
### toString — 자동 생성

```java
public final java.lang.String toString()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m453"></a>
### token — 자동 생성

```java
public java.lang.String token()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="c83"></a>
## global.config.AppSecurityProperties

정의: [src/main/java/com/dodamdodam/backend/global/config/AppSecurityProperties.java:9](../../../src/main/java/com/dodamdodam/backend/global/config/AppSecurityProperties.java#L9) · RECORD

설정: `@org.springframework.validation.annotation.Validated` · `@org.springframework.boot.context.properties.ConfigurationProperties("app.security")`

<a id="m454"></a>
### AppSecurityProperties 생성자

```java
public void <init>(java.util.@jakarta.validation.constraints.NotEmpty List<java.lang.@jakarta.validation.constraints.NotBlank String> allowedOrigins, java.lang.@jakarta.validation.constraints.NotBlank String loginSuccessUrl)
```

정의: [src/main/java/com/dodamdodam/backend/global/config/AppSecurityProperties.java:16](../../../src/main/java/com/dodamdodam/backend/global/config/AppSecurityProperties.java#L16)

호출자: 정적 Java 호출 없음

내부 호출: 없음

외부 라이브러리 호출: `java.lang.Record.<init>()` / `java.util.List.copyOf(java.util.Collection<? extends E>)`

<a id="m455"></a>
### allowedOrigins — 자동 생성

```java
public java.util.@jakarta.validation.constraints.NotEmpty List<java.lang.@jakarta.validation.constraints.NotBlank String> allowedOrigins()
```

애너테이션: `@jakarta.validation.constraints.NotEmpty`

호출자: [global.config.SecurityConfig.corsConfigurationSource](#m480)

내부 호출: 없음

<a id="m456"></a>
### equals — 자동 생성

```java
public final boolean equals(java.lang.Object o)
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m457"></a>
### hashCode — 자동 생성

```java
public final int hashCode()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m458"></a>
### loginSuccessUrl — 자동 생성

```java
public java.lang.@jakarta.validation.constraints.NotBlank String loginSuccessUrl()
```

애너테이션: `@jakarta.validation.constraints.NotBlank`

호출자: [global.config.SecurityConfig.applicationSecurityFilterChain](#m476)

내부 호출: 없음

<a id="m459"></a>
### toString — 자동 생성

```java
public final java.lang.String toString()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="c84"></a>
## global.config.OAuth2ClientConfig

정의: [src/main/java/com/dodamdodam/backend/global/config/OAuth2ClientConfig.java:14](../../../src/main/java/com/dodamdodam/backend/global/config/OAuth2ClientConfig.java#L14) · CLASS

설정: `@org.springframework.context.annotation.Configuration(proxyBeanMethods=false)` · `@org.springframework.boot.context.properties.EnableConfigurationProperties({com.dodamdodam.backend.global.config.OAuth2ClientProperties.class})`

<a id="m460"></a>
### clientRegistrationRepository

```java
org.springframework.security.oauth2.client.registration.ClientRegistrationRepository clientRegistrationRepository(com.dodamdodam.backend.global.config.OAuth2ClientProperties properties)
```

정의: [src/main/java/com/dodamdodam/backend/global/config/OAuth2ClientConfig.java:18](../../../src/main/java/com/dodamdodam/backend/global/config/OAuth2ClientConfig.java#L18)

애너테이션: `@org.springframework.context.annotation.Bean`

호출자: 정적 Java 호출 없음

프레임워크 진입: Spring 컨테이너 Bean 생성

내부 호출: [global.config.OAuth2ClientConfig.google](#m461) / [global.config.OAuth2ClientConfig.kakao](#m462) / [global.config.OAuth2ClientProperties.google](#m466) / [global.config.OAuth2ClientProperties.kakao](#m468)

외부 라이브러리 호출: `org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository.<init>(org.springframework.security.oauth2.client.registration.ClientRegistration[])`

<a id="m461"></a>
### google

```java
private org.springframework.security.oauth2.client.registration.ClientRegistration google(com.dodamdodam.backend.global.config.OAuth2ClientProperties.Credentials credentials)
```

정의: [src/main/java/com/dodamdodam/backend/global/config/OAuth2ClientConfig.java:26](../../../src/main/java/com/dodamdodam/backend/global/config/OAuth2ClientConfig.java#L26)

호출자: [global.config.OAuth2ClientConfig.clientRegistrationRepository](#m460)

내부 호출: [global.config.OAuth2ClientProperties.Credentials.clientId](#m471) / [global.config.OAuth2ClientProperties.Credentials.clientSecret](#m472)

외부 라이브러리 호출: `org.springframework.security.config.oauth2.client.CommonOAuth2Provider.getBuilder(java.lang.String)` / `org.springframework.security.oauth2.client.registration.ClientRegistration.Builder.build()` / `org.springframework.security.oauth2.client.registration.ClientRegistration.Builder.clientId(java.lang.String)` / `org.springframework.security.oauth2.client.registration.ClientRegistration.Builder.clientSecret(java.lang.@org.jspecify.annotations.Nullable String)` / `org.springframework.security.oauth2.client.registration.ClientRegistration.Builder.scope(java.lang.String @org.jspecify.annotations.Nullable [])`

<a id="m462"></a>
### kakao

```java
private org.springframework.security.oauth2.client.registration.ClientRegistration kakao(com.dodamdodam.backend.global.config.OAuth2ClientProperties.Credentials credentials)
```

정의: [src/main/java/com/dodamdodam/backend/global/config/OAuth2ClientConfig.java:34](../../../src/main/java/com/dodamdodam/backend/global/config/OAuth2ClientConfig.java#L34)

호출자: [global.config.OAuth2ClientConfig.clientRegistrationRepository](#m460)

내부 호출: [global.config.OAuth2ClientProperties.Credentials.clientId](#m471) / [global.config.OAuth2ClientProperties.Credentials.clientSecret](#m472)

외부 라이브러리 호출: `org.springframework.security.oauth2.client.registration.ClientRegistration.withRegistrationId(java.lang.String)` / `org.springframework.security.oauth2.client.registration.ClientRegistration.Builder.authorizationGrantType(org.springframework.security.oauth2.core.AuthorizationGrantType)` / `org.springframework.security.oauth2.client.registration.ClientRegistration.Builder.authorizationUri(java.lang.@org.jspecify.annotations.Nullable String)` / `org.springframework.security.oauth2.client.registration.ClientRegistration.Builder.build()` / `org.springframework.security.oauth2.client.registration.ClientRegistration.Builder.clientAuthenticationMethod(org.springframework.security.oauth2.core.ClientAuthenticationMethod)` / `org.springframework.security.oauth2.client.registration.ClientRegistration.Builder.clientId(java.lang.String)` / `org.springframework.security.oauth2.client.registration.ClientRegistration.Builder.clientName(java.lang.@org.jspecify.annotations.Nullable String)` / `org.springframework.security.oauth2.client.registration.ClientRegistration.Builder.clientSecret(java.lang.@org.jspecify.annotations.Nullable String)` / `org.springframework.security.oauth2.client.registration.ClientRegistration.Builder.issuerUri(java.lang.@org.jspecify.annotations.Nullable String)` / `org.springframework.security.oauth2.client.registration.ClientRegistration.Builder.jwkSetUri(java.lang.@org.jspecify.annotations.Nullable String)` / `org.springframework.security.oauth2.client.registration.ClientRegistration.Builder.redirectUri(java.lang.@org.jspecify.annotations.Nullable String)` / `org.springframework.security.oauth2.client.registration.ClientRegistration.Builder.scope(java.lang.String @org.jspecify.annotations.Nullable [])` / `org.springframework.security.oauth2.client.registration.ClientRegistration.Builder.tokenUri(java.lang.String)` / `org.springframework.security.oauth2.client.registration.ClientRegistration.Builder.userInfoUri(java.lang.@org.jspecify.annotations.Nullable String)` / `org.springframework.security.oauth2.client.registration.ClientRegistration.Builder.userNameAttributeName(java.lang.@org.jspecify.annotations.Nullable String)`

<a id="m463"></a>
### OAuth2ClientConfig 생성자 — 자동 생성

```java
public void <init>()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

외부 라이브러리 호출: `java.lang.Object.<init>()`

<a id="c85"></a>
## global.config.OAuth2ClientProperties

정의: [src/main/java/com/dodamdodam/backend/global/config/OAuth2ClientProperties.java:9](../../../src/main/java/com/dodamdodam/backend/global/config/OAuth2ClientProperties.java#L9) · RECORD

설정: `@org.springframework.validation.annotation.Validated` · `@org.springframework.boot.context.properties.ConfigurationProperties("app.oauth2")`

<a id="m464"></a>
### OAuth2ClientProperties 생성자 — 자동 생성

```java
public void <init>(com.dodamdodam.backend.global.config.OAuth2ClientProperties.@jakarta.validation.constraints.NotNull @jakarta.validation.Valid Credentials google, com.dodamdodam.backend.global.config.OAuth2ClientProperties.@jakarta.validation.constraints.NotNull @jakarta.validation.Valid Credentials kakao)
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

외부 라이브러리 호출: `java.lang.Record.<init>()`

<a id="m465"></a>
### equals — 자동 생성

```java
public final boolean equals(java.lang.Object o)
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m466"></a>
### google — 자동 생성

```java
public com.dodamdodam.backend.global.config.OAuth2ClientProperties.@jakarta.validation.constraints.NotNull @jakarta.validation.Valid Credentials google()
```

애너테이션: `@jakarta.validation.constraints.NotNull` · `@jakarta.validation.Valid`

호출자: [global.config.OAuth2ClientConfig.clientRegistrationRepository](#m460)

내부 호출: 없음

<a id="m467"></a>
### hashCode — 자동 생성

```java
public final int hashCode()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m468"></a>
### kakao — 자동 생성

```java
public com.dodamdodam.backend.global.config.OAuth2ClientProperties.@jakarta.validation.constraints.NotNull @jakarta.validation.Valid Credentials kakao()
```

애너테이션: `@jakarta.validation.constraints.NotNull` · `@jakarta.validation.Valid`

호출자: [global.config.OAuth2ClientConfig.clientRegistrationRepository](#m460)

내부 호출: 없음

<a id="m469"></a>
### toString — 자동 생성

```java
public final java.lang.String toString()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="c86"></a>
## global.config.OAuth2ClientProperties.Credentials

정의: [src/main/java/com/dodamdodam/backend/global/config/OAuth2ClientProperties.java:16](../../../src/main/java/com/dodamdodam/backend/global/config/OAuth2ClientProperties.java#L16) · RECORD

<a id="m470"></a>
### Credentials 생성자 — 자동 생성

```java
public void <init>(java.lang.@jakarta.validation.constraints.NotBlank String clientId, java.lang.@jakarta.validation.constraints.NotBlank String clientSecret)
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

외부 라이브러리 호출: `java.lang.Record.<init>()`

<a id="m471"></a>
### clientId — 자동 생성

```java
public java.lang.@jakarta.validation.constraints.NotBlank String clientId()
```

애너테이션: `@jakarta.validation.constraints.NotBlank`

호출자: [global.config.OAuth2ClientConfig.google](#m461) / [global.config.OAuth2ClientConfig.kakao](#m462)

내부 호출: 없음

<a id="m472"></a>
### clientSecret — 자동 생성

```java
public java.lang.@jakarta.validation.constraints.NotBlank String clientSecret()
```

애너테이션: `@jakarta.validation.constraints.NotBlank`

호출자: [global.config.OAuth2ClientConfig.google](#m461) / [global.config.OAuth2ClientConfig.kakao](#m462)

내부 호출: 없음

<a id="m473"></a>
### equals — 자동 생성

```java
public final boolean equals(java.lang.Object o)
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m474"></a>
### hashCode — 자동 생성

```java
public final int hashCode()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m475"></a>
### toString — 자동 생성

```java
public final java.lang.String toString()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="c87"></a>
## global.config.SecurityConfig

정의: [src/main/java/com/dodamdodam/backend/global/config/SecurityConfig.java:44](../../../src/main/java/com/dodamdodam/backend/global/config/SecurityConfig.java#L44) · CLASS

설정: `@org.springframework.context.annotation.Configuration(proxyBeanMethods=false)` · `@org.springframework.security.config.annotation.web.configuration.EnableWebSecurity` · `@org.springframework.boot.context.properties.EnableConfigurationProperties({com.dodamdodam.backend.global.config.AppSecurityProperties.class})`

<a id="m476"></a>
### applicationSecurityFilterChain

```java
org.springframework.security.web.SecurityFilterChain applicationSecurityFilterChain(org.springframework.security.config.annotation.web.builders.HttpSecurity http, com.dodamdodam.backend.global.config.AppSecurityProperties properties, com.dodamdodam.backend.global.auth.CsrfAccessDeniedHandler csrfAccessDeniedHandler, org.springframework.security.web.csrf.CookieCsrfTokenRepository csrfTokenRepository, com.dodamdodam.backend.auth.session.AuthenticatedSessionService authenticatedSessionService, com.dodamdodam.backend.auth.session.AuthStateErrorHandler authStateErrorHandler, com.dodamdodam.backend.auth.session.LoginSessionAuthenticationStrategy loginSessionAuthenticationStrategy, org.springframework.security.web.context.SecurityContextRepository securityContextRepository, org.springframework.security.web.csrf.CsrfTokenRequestHandler csrfRequestHandler, tools.jackson.databind.json.JsonMapper jsonMapper, com.dodamdodam.backend.auth.application.AuthRateLimitService rateLimits, com.dodamdodam.backend.auth.infrastructure.EmailVerificationKeys verificationKeys)
```

정의: [src/main/java/com/dodamdodam/backend/global/config/SecurityConfig.java:50](../../../src/main/java/com/dodamdodam/backend/global/config/SecurityConfig.java#L50)

공통 인증·CSRF와 Part 1·2 전송 검사를 순서대로 연결한다.

애너테이션: `@org.springframework.context.annotation.Bean`

호출자: 정적 Java 호출 없음

프레임워크 진입: Spring 컨테이너 Bean 생성

내부 호출: [auth.session.AuthenticatedSessionService.requireAuthenticatedSession](#m172) / [global.config.AppSecurityProperties.loginSuccessUrl](#m458) / [global.web.GlobalApiRateLimitFilter.GlobalApiRateLimitFilter](#m515) / [global.web.Part1JsonRequestGuardFilter.Part1JsonRequestGuardFilter](#m518) / [global.web.Part2JsonRequestGuardFilter.Part2JsonRequestGuardFilter](#m534)

외부 라이브러리 호출: `java.util.HashMap.put(K,V)` / `java.util.LinkedHashMap.<init>()` / `org.springframework.security.authorization.AuthorizationDecision.<init>(boolean)` / `org.springframework.security.config.Customizer.withDefaults()` / `org.springframework.security.config.annotation.AbstractSecurityBuilder.build()` / `org.springframework.security.config.annotation.web.AbstractRequestMatcherRegistry.anyRequest()` / `org.springframework.security.config.annotation.web.AbstractRequestMatcherRegistry.requestMatchers(java.lang.String[])` / `org.springframework.security.config.annotation.web.AbstractRequestMatcherRegistry.requestMatchers(org.springframework.http.HttpMethod,java.lang.String[])` / `org.springframework.security.config.annotation.web.builders.HttpSecurity.addFilterBefore(jakarta.servlet.Filter,java.lang.Class<? extends jakarta.servlet.Filter>)` / `org.springframework.security.config.annotation.web.builders.HttpSecurity.authorizeHttpRequests(org.springframework.security.config.Customizer<org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer<org.springframework.security.config.annotation.web.builders.HttpSecurity>.AuthorizationManagerRequestMatcherRegistry>)` / `org.springframework.security.config.annotation.web.builders.HttpSecurity.cors(org.springframework.security.config.Customizer<org.springframework.security.config.annotation.web.configurers.CorsConfigurer<org.springframework.security.config.annotation.web.builders.HttpSecurity>>)` / `org.springframework.security.config.annotation.web.builders.HttpSecurity.csrf(org.springframework.security.config.Customizer<org.springframework.security.config.annotation.web.configurers.CsrfConfigurer<org.springframework.security.config.annotation.web.builders.HttpSecurity>>)` / `org.springframework.security.config.annotation.web.builders.HttpSecurity.exceptionHandling(org.springframework.security.config.Customizer<org.springframework.security.config.annotation.web.configurers.ExceptionHandlingConfigurer<org.springframework.security.config.annotation.web.builders.HttpSecurity>>)` / `org.springframework.security.config.annotation.web.builders.HttpSecurity.logout(org.springframework.security.config.Customizer<org.springframework.security.config.annotation.web.configurers.LogoutConfigurer<org.springframework.security.config.annotation.web.builders.HttpSecurity>>)` / `org.springframework.security.config.annotation.web.builders.HttpSecurity.oauth2Login(org.springframework.security.config.Customizer<org.springframework.security.config.annotation.web.configurers.oauth2.client.OAuth2LoginConfigurer<org.springframework.security.config.annotation.web.builders.HttpSecurity>>)` / `org.springframework.security.config.annotation.web.builders.HttpSecurity.requestCache(org.springframework.security.config.Customizer<org.springframework.security.config.annotation.web.configurers.RequestCacheConfigurer<org.springframework.security.config.annotation.web.builders.HttpSecurity>>)` / `org.springframework.security.config.annotation.web.builders.HttpSecurity.securityContext(org.springframework.security.config.Customizer<org.springframework.security.config.annotation.web.configurers.SecurityContextConfigurer<org.springframework.security.config.annotation.web.builders.HttpSecurity>>)` / `org.springframework.security.config.annotation.web.builders.HttpSecurity.sessionManagement(org.springframework.security.config.Customizer<org.springframework.security.config.annotation.web.configurers.SessionManagementConfigurer<org.springframework.security.config.annotation.web.builders.HttpSecurity>>)` / `org.springframework.security.config.annotation.web.configurers.AbstractAuthenticationFilterConfigurer.defaultSuccessUrl(java.lang.String,boolean)` / `org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer.disable()` / `org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer.AuthorizedUrl.access(org.springframework.security.authorization.AuthorizationManager<? super org.springframework.security.web.access.intercept.RequestAuthorizationContext>)` / `org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer.AuthorizedUrl.permitAll()` / `org.springframework.security.config.annotation.web.configurers.CsrfConfigurer.csrfTokenRepository(org.springframework.security.web.csrf.CsrfTokenRepository)` / `org.springframework.security.config.annotation.web.configurers.CsrfConfigurer.csrfTokenRequestHandler(org.springframework.security.web.csrf.CsrfTokenRequestHandler)` / `org.springframework.security.config.annotation.web.configurers.CsrfConfigurer.sessionAuthenticationStrategy(org.springframework.security.web.authentication.session.SessionAuthenticationStrategy)` / `org.springframework.security.config.annotation.web.configurers.ExceptionHandlingConfigurer.accessDeniedHandler(org.springframework.security.web.access.AccessDeniedHandler)` / `org.springframework.security.config.annotation.web.configurers.ExceptionHandlingConfigurer.authenticationEntryPoint(org.springframework.security.web.AuthenticationEntryPoint)` / `org.springframework.security.config.annotation.web.configurers.RequestCacheConfigurer.disable()` / `org.springframework.security.config.annotation.web.configurers.SecurityContextConfigurer.securityContextRepository(org.springframework.security.web.context.SecurityContextRepository)` / `org.springframework.security.config.annotation.web.configurers.SessionManagementConfigurer.sessionAuthenticationStrategy(org.springframework.security.web.authentication.session.SessionAuthenticationStrategy)` / `org.springframework.security.web.access.AccessDeniedHandlerImpl.<init>()` / `org.springframework.security.web.access.DelegatingAccessDeniedHandler.<init>(java.util.LinkedHashMap<java.lang.Class<? extends org.springframework.security.access.AccessDeniedException>,org.springframework.security.web.access.AccessDeniedHandler>,org.springframework.security.web.access.AccessDeniedHandler)` / `org.springframework.security.web.access.intercept.RequestAuthorizationContext.getRequest()` / `org.springframework.security.web.authentication.session.NullAuthenticatedSessionStrategy.<init>()`

<a id="m477"></a>
### csrfTokenRequestHandler

```java
org.springframework.security.web.csrf.CsrfTokenRequestHandler csrfTokenRequestHandler()
```

정의: [src/main/java/com/dodamdodam/backend/global/config/SecurityConfig.java:118](../../../src/main/java/com/dodamdodam/backend/global/config/SecurityConfig.java#L118)

애너테이션: `@org.springframework.context.annotation.Bean`

호출자: 정적 Java 호출 없음

프레임워크 진입: Spring 컨테이너 Bean 생성

내부 호출: [global.config.SecurityConfig$anonymous@120.SecurityConfig$anonymous@120](#m482)

<a id="m479"></a>
### csrfTokenRepository

```java
org.springframework.security.web.csrf.CookieCsrfTokenRepository csrfTokenRepository(org.springframework.boot.web.server.autoconfigure.ServerProperties serverProperties)
```

정의: [src/main/java/com/dodamdodam/backend/global/config/SecurityConfig.java:128](../../../src/main/java/com/dodamdodam/backend/global/config/SecurityConfig.java#L128)

애너테이션: `@org.springframework.context.annotation.Bean`

호출자: 정적 Java 호출 없음

프레임워크 진입: Spring 컨테이너 Bean 생성

내부 호출: 없음

외부 라이브러리 호출: `java.lang.Boolean.equals(java.lang.Object)` / `java.lang.IllegalArgumentException.<init>(java.lang.String)` / `org.springframework.boot.web.server.Cookie.getSameSite()` / `org.springframework.boot.web.server.Cookie.getSecure()` / `org.springframework.boot.web.server.Cookie.SameSite.attributeValue()` / `org.springframework.boot.web.server.autoconfigure.ServerProperties.getServlet()` / `org.springframework.boot.web.server.autoconfigure.ServerProperties.Servlet.getSession()` / `org.springframework.boot.web.server.servlet.Session.getCookie()` / `org.springframework.http.ResponseCookie.ResponseCookieBuilder.path(java.lang.@org.jspecify.annotations.Nullable String)` / `org.springframework.http.ResponseCookie.ResponseCookieBuilder.sameSite(java.lang.@org.jspecify.annotations.Nullable String)` / `org.springframework.http.ResponseCookie.ResponseCookieBuilder.secure(boolean)` / `org.springframework.security.web.csrf.CookieCsrfTokenRepository.setCookieCustomizer(java.util.function.Consumer<org.springframework.http.ResponseCookie.ResponseCookieBuilder>)` / `org.springframework.security.web.csrf.CookieCsrfTokenRepository.withHttpOnlyFalse()`

<a id="m480"></a>
### corsConfigurationSource

```java
org.springframework.web.cors.CorsConfigurationSource corsConfigurationSource(com.dodamdodam.backend.global.config.AppSecurityProperties properties)
```

정의: [src/main/java/com/dodamdodam/backend/global/config/SecurityConfig.java:143](../../../src/main/java/com/dodamdodam/backend/global/config/SecurityConfig.java#L143)

애너테이션: `@org.springframework.context.annotation.Bean`

호출자: 정적 Java 호출 없음

프레임워크 진입: Spring 컨테이너 Bean 생성

내부 호출: [global.config.AppSecurityProperties.allowedOrigins](#m455)

외부 라이브러리 호출: `java.lang.IllegalArgumentException.<init>(java.lang.String)` / `java.lang.String.contains(java.lang.CharSequence)` / `java.lang.String.equalsIgnoreCase(java.lang.String)` / `java.util.List.of(E)` / `java.util.List.of(E,E,E)` / `java.util.List.of(E,E,E,E,E,E)` / `org.springframework.web.cors.CorsConfiguration.<init>()` / `org.springframework.web.cors.CorsConfiguration.getAllowedOrigins()` / `org.springframework.web.cors.CorsConfiguration.setAllowCredentials(java.lang.@org.jspecify.annotations.Nullable Boolean)` / `org.springframework.web.cors.CorsConfiguration.setAllowedHeaders(java.util.@org.jspecify.annotations.Nullable List<java.lang.String>)` / `org.springframework.web.cors.CorsConfiguration.setAllowedMethods(java.util.@org.jspecify.annotations.Nullable List<java.lang.String>)` / `org.springframework.web.cors.CorsConfiguration.setAllowedOrigins(java.util.@org.jspecify.annotations.Nullable List<java.lang.String>)` / `org.springframework.web.cors.CorsConfiguration.setExposedHeaders(java.util.@org.jspecify.annotations.Nullable List<java.lang.String>)` / `org.springframework.web.cors.CorsConfiguration.setMaxAge(java.lang.@org.jspecify.annotations.Nullable Long)` / `org.springframework.web.cors.UrlBasedCorsConfigurationSource.<init>()` / `org.springframework.web.cors.UrlBasedCorsConfigurationSource.registerCorsConfiguration(java.lang.String,org.springframework.web.cors.CorsConfiguration)`

<a id="m481"></a>
### SecurityConfig 생성자 — 자동 생성

```java
public void <init>()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

외부 라이브러리 호출: `java.lang.Object.<init>()`

<a id="c88"></a>
## global.config.SecurityConfig$anonymous@120

정의: [src/main/java/com/dodamdodam/backend/global/config/SecurityConfig.java:120](../../../src/main/java/com/dodamdodam/backend/global/config/SecurityConfig.java#L120) · CLASS

<a id="m478"></a>
### resolveCsrfTokenValue

```java
public java.lang.String resolveCsrfTokenValue(jakarta.servlet.http.HttpServletRequest request, org.springframework.security.web.csrf.CsrfToken csrfToken)
```

정의: [src/main/java/com/dodamdodam/backend/global/config/SecurityConfig.java:121](../../../src/main/java/com/dodamdodam/backend/global/config/SecurityConfig.java#L121)

애너테이션: `@java.lang.Override`

호출자: 정적 Java 호출 없음

내부 호출: 없음

외부 라이브러리 호출: `jakarta.servlet.http.HttpServletRequest.getHeader(java.lang.String)` / `org.springframework.security.web.csrf.CsrfToken.getHeaderName()`

<a id="m482"></a>
### 익명 클래스@120 생성자 — 자동 생성

```java
void <init>()
```

호출자: [global.config.SecurityConfig.csrfTokenRequestHandler](#m477)

내부 호출: 없음

외부 라이브러리 호출: `org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler.<init>()`

<a id="c89"></a>
## global.config.SessionConfig

정의: [src/main/java/com/dodamdodam/backend/global/config/SessionConfig.java:16](../../../src/main/java/com/dodamdodam/backend/global/config/SessionConfig.java#L16) · CLASS

설정: `@org.springframework.context.annotation.Configuration(proxyBeanMethods=false)`

<a id="m483"></a>
### explicitSessionDeletion

```java
org.springframework.session.config.SessionRepositoryCustomizer<org.springframework.session.jdbc.JdbcIndexedSessionRepository> explicitSessionDeletion()
```

정의: [src/main/java/com/dodamdodam/backend/global/config/SessionConfig.java:19](../../../src/main/java/com/dodamdodam/backend/global/config/SessionConfig.java#L19)

애너테이션: `@org.springframework.context.annotation.Bean`

호출자: 정적 Java 호출 없음

프레임워크 진입: Spring 컨테이너 Bean 생성

내부 호출: 없음

외부 라이브러리 호출: `org.springframework.session.jdbc.JdbcIndexedSessionRepository.setDeleteSessionQuery(java.lang.String)`

<a id="m484"></a>
### loginSessionAuthenticationStrategy

```java
com.dodamdodam.backend.auth.session.LoginSessionAuthenticationStrategy loginSessionAuthenticationStrategy(com.dodamdodam.backend.auth.session.SessionLifecycleService lifecycle, org.springframework.security.web.csrf.CookieCsrfTokenRepository csrfRepository, org.springframework.security.web.csrf.CsrfTokenRequestHandler csrfRequestHandler)
```

정의: [src/main/java/com/dodamdodam/backend/global/config/SessionConfig.java:25](../../../src/main/java/com/dodamdodam/backend/global/config/SessionConfig.java#L25)

애너테이션: `@org.springframework.context.annotation.Bean`

호출자: 정적 Java 호출 없음

프레임워크 진입: Spring 컨테이너 Bean 생성

내부 호출: [auth.session.LoginSessionAuthenticationStrategy.LoginSessionAuthenticationStrategy](#m184)

<a id="m485"></a>
### securityContextRepository

```java
org.springframework.security.web.context.SecurityContextRepository securityContextRepository(com.dodamdodam.backend.auth.session.SessionLifecycleService lifecycle)
```

정의: [src/main/java/com/dodamdodam/backend/global/config/SessionConfig.java:31](../../../src/main/java/com/dodamdodam/backend/global/config/SessionConfig.java#L31)

애너테이션: `@org.springframework.context.annotation.Bean`

호출자: 정적 Java 호출 없음

프레임워크 진입: Spring 컨테이너 Bean 생성

내부 호출: [auth.session.LoginSecurityContextRepository.LoginSecurityContextRepository](#m182)

외부 라이브러리 호출: `org.springframework.security.web.context.DelegatingSecurityContextRepository.<init>(org.springframework.security.web.context.SecurityContextRepository[])` / `org.springframework.security.web.context.RequestAttributeSecurityContextRepository.<init>()`

<a id="m486"></a>
### SessionConfig 생성자 — 자동 생성

```java
public void <init>()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

외부 라이브러리 호출: `java.lang.Object.<init>()`

<a id="c90"></a>
## global.error.ApiErrorResponse

정의: [src/main/java/com/dodamdodam/backend/global/error/ApiErrorResponse.java:5](../../../src/main/java/com/dodamdodam/backend/global/error/ApiErrorResponse.java#L5) · RECORD

<a id="m487"></a>
### ApiErrorResponse 생성자 — 자동 생성

```java
public void <init>(com.dodamdodam.backend.global.error.ApiErrorResponse.ErrorDetail error)
```

호출자: [global.error.ApiErrorResponses.body](#m507)

내부 호출: 없음

외부 라이브러리 호출: `java.lang.Record.<init>()`

<a id="m488"></a>
### equals — 자동 생성

```java
public final boolean equals(java.lang.Object o)
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m489"></a>
### error — 자동 생성

```java
public com.dodamdodam.backend.global.error.ApiErrorResponse.ErrorDetail error()
```

호출자: [conversation.dto.ActiveConversationExistsError.from](#m355)

내부 호출: 없음

<a id="m490"></a>
### hashCode — 자동 생성

```java
public final int hashCode()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m491"></a>
### toString — 자동 생성

```java
public final java.lang.String toString()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="c91"></a>
## global.error.ApiErrorResponse.ErrorDetail

정의: [src/main/java/com/dodamdodam/backend/global/error/ApiErrorResponse.java:7](../../../src/main/java/com/dodamdodam/backend/global/error/ApiErrorResponse.java#L7) · RECORD

<a id="m492"></a>
### ErrorDetail 생성자 — 자동 생성

```java
public void <init>(java.lang.String code, java.lang.String message, java.lang.String requestId, java.util.List<com.dodamdodam.backend.global.error.ApiErrorResponse.FieldError> fields)
```

호출자: [global.error.ApiErrorResponses.body](#m507)

내부 호출: 없음

외부 라이브러리 호출: `java.lang.Record.<init>()`

<a id="m493"></a>
### code — 자동 생성

```java
public java.lang.String code()
```

호출자: [conversation.dto.ActiveConversationExistsError.from](#m355)

내부 호출: 없음

<a id="m494"></a>
### equals — 자동 생성

```java
public final boolean equals(java.lang.Object o)
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m495"></a>
### fields — 자동 생성

```java
public java.util.List<com.dodamdodam.backend.global.error.ApiErrorResponse.FieldError> fields()
```

호출자: [conversation.dto.ActiveConversationExistsError.from](#m355)

내부 호출: 없음

<a id="m496"></a>
### hashCode — 자동 생성

```java
public final int hashCode()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m497"></a>
### message — 자동 생성

```java
public java.lang.String message()
```

호출자: [conversation.dto.ActiveConversationExistsError.from](#m355)

내부 호출: 없음

<a id="m498"></a>
### requestId — 자동 생성

```java
public java.lang.String requestId()
```

호출자: [conversation.dto.ActiveConversationExistsError.from](#m355)

내부 호출: 없음

<a id="m499"></a>
### toString — 자동 생성

```java
public final java.lang.String toString()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="c92"></a>
## global.error.ApiErrorResponse.FieldError

정의: [src/main/java/com/dodamdodam/backend/global/error/ApiErrorResponse.java:10](../../../src/main/java/com/dodamdodam/backend/global/error/ApiErrorResponse.java#L10) · RECORD

<a id="m500"></a>
### FieldError 생성자 — 자동 생성

```java
public void <init>(java.lang.String field, java.lang.String reason)
```

호출자: [auth.exception.EmailVerificationException.EmailVerificationException](#m62) / [child.exception.ChildException.validation](#m273) / [conversation.exception.ConversationException.validation](#m389) / [global.error.CommonApiExceptionHandler.handle](#m510)

내부 호출: 없음

외부 라이브러리 호출: `java.lang.Record.<init>()`

<a id="m501"></a>
### equals — 자동 생성

```java
public final boolean equals(java.lang.Object o)
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m502"></a>
### field — 자동 생성

```java
public java.lang.String field()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m503"></a>
### hashCode — 자동 생성

```java
public final int hashCode()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m504"></a>
### reason — 자동 생성

```java
public java.lang.String reason()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m505"></a>
### toString — 자동 생성

```java
public final java.lang.String toString()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="c93"></a>
## global.error.ApiErrorResponses

정의: [src/main/java/com/dodamdodam/backend/global/error/ApiErrorResponses.java:11](../../../src/main/java/com/dodamdodam/backend/global/error/ApiErrorResponses.java#L11) · CLASS

<a id="m506"></a>
### ApiErrorResponses 생성자

```java
private void <init>()
```

정의: [src/main/java/com/dodamdodam/backend/global/error/ApiErrorResponses.java:14](../../../src/main/java/com/dodamdodam/backend/global/error/ApiErrorResponses.java#L14)

요청별 상태 없이 사용하는 공통 함수의 인스턴스 생성을 막는다.

호출자: 정적 Java 호출 없음

내부 호출: 없음

외부 라이브러리 호출: `java.lang.Object.<init>()`

<a id="m507"></a>
### body

```java
public static com.dodamdodam.backend.global.error.ApiErrorResponse body(jakarta.servlet.http.HttpServletResponse response, java.lang.String code, java.lang.String message, java.util.List<com.dodamdodam.backend.global.error.ApiErrorResponse.FieldError> fields)
```

정의: [src/main/java/com/dodamdodam/backend/global/error/ApiErrorResponses.java:18](../../../src/main/java/com/dodamdodam/backend/global/error/ApiErrorResponses.java#L18)

현재 응답의 요청 ID와 검증 오류를 공통 오류 본문에 담는다.

호출자: [auth.api.AuthLogoutController.logout](#m4) / [auth.api.EmailVerificationExceptionHandler.handle](#m7) / [auth.api.EmailVerificationExceptionHandler.limited](#m8) / [child.api.ChildController.invalidJson](#m220) / [child.api.ChildExceptionHandler.handle](#m221) / [conversation.api.ConversationExceptionHandler.handle](#m293) / [conversation.api.ConversationMediaTypeExceptionHandler.handle](#m295) / [global.error.ApiErrorResponses.write](#m508) / [global.error.CommonApiExceptionHandler.handle](#m510) / [guardian.api.GuardianAccessExceptionHandler.handle](#m538)

내부 호출: [global.error.ApiErrorResponse.ApiErrorResponse](#m487) / [global.error.ApiErrorResponse.ErrorDetail.ErrorDetail](#m492)

외부 라이브러리 호출: `jakarta.servlet.http.HttpServletResponse.getHeader(java.lang.String)`

<a id="m508"></a>
### write

```java
public static void write(tools.jackson.databind.json.JsonMapper mapper, jakarta.servlet.http.HttpServletResponse response, int status, java.lang.String code, java.lang.String message)
```

정의: [src/main/java/com/dodamdodam/backend/global/error/ApiErrorResponses.java:25](../../../src/main/java/com/dodamdodam/backend/global/error/ApiErrorResponses.java#L25)

MVC 밖의 오류를 호출자가 결정한 상태 코드와 UTF-8 JSON으로 출력한다.

호출자: [auth.session.AuthStateErrorFilter.doFilterInternal](#m154) / [auth.session.AuthStateErrorHandler.commence](#m161) / [auth.session.AuthStateErrorHandler.unavailable](#m162) / [global.auth.CsrfAccessDeniedHandler.handle](#m444) / [global.web.GlobalApiRateLimitFilter.doFilterInternal](#m517) / [global.web.Part1JsonRequestGuardFilter.doFilterInternal](#m520) / [global.web.Part2JsonRequestGuardFilter.doFilterInternal](#m536)

내부 호출: [global.error.ApiErrorResponses.body](#m507)

외부 라이브러리 호출: `jakarta.servlet.ServletResponse.getOutputStream()` / `jakarta.servlet.ServletResponse.setCharacterEncoding(java.lang.String)` / `jakarta.servlet.ServletResponse.setContentType(java.lang.String)` / `jakarta.servlet.http.HttpServletResponse.setStatus(int)` / `java.nio.charset.Charset.name()` / `java.util.List.of()` / `tools.jackson.databind.ObjectMapper.writeValue(java.io.OutputStream,java.lang.Object)`

<a id="c94"></a>
## global.error.CommonApiExceptionHandler

정의: [src/main/java/com/dodamdodam/backend/global/error/CommonApiExceptionHandler.java:21](../../../src/main/java/com/dodamdodam/backend/global/error/CommonApiExceptionHandler.java#L21) · CLASS

설정: `@org.springframework.core.annotation.Order(2147483647)` · `@org.springframework.web.bind.annotation.RestControllerAdvice(basePackages={"com.dodamdodam.backend"})`

<a id="m509"></a>
### CommonApiExceptionHandler 생성자

```java
public void <init>(com.dodamdodam.backend.auth.session.SessionLifecycleService lifecycle)
```

정의: [src/main/java/com/dodamdodam/backend/global/error/CommonApiExceptionHandler.java:28](../../../src/main/java/com/dodamdodam/backend/global/error/CommonApiExceptionHandler.java#L28)

진행 중 로그인에 필요한 외곽 필터의 실패 보상 경로를 확인한다.

호출자: 정적 Java 호출 없음

내부 호출: 없음

외부 라이브러리 호출: `java.lang.Object.<init>()`

<a id="m510"></a>
### handle

```java
public org.springframework.http.ResponseEntity<com.dodamdodam.backend.global.error.ApiErrorResponse> handle(java.lang.Exception exception, jakarta.servlet.http.HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response)
```

정의: [src/main/java/com/dodamdodam/backend/global/error/CommonApiExceptionHandler.java:33](../../../src/main/java/com/dodamdodam/backend/global/error/CommonApiExceptionHandler.java#L33)

기능별 처리 뒤에 남은 컨트롤러 오류를 원문 없는 공통 응답으로 변환한다.

애너테이션: `@org.springframework.web.bind.annotation.ExceptionHandler({java.lang.Exception.class})`

호출자: 정적 Java 호출 없음

프레임워크 진입: Spring MVC 예외 처리

내부 호출: [auth.session.SessionLifecycleService.hasPendingLogin](#m197) / [global.error.ApiErrorResponse.FieldError.FieldError](#m500) / [global.error.ApiErrorResponses.body](#m507)

외부 라이브러리 호출: `java.lang.Throwable.getCause()` / `java.util.List.of()` / `java.util.List.of(E)` / `org.springframework.http.ResponseEntity.badRequest()` / `org.springframework.http.ResponseEntity.internalServerError()` / `org.springframework.http.ResponseEntity.BodyBuilder.body(@org.jspecify.annotations.Nullable T)` / `org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.getName()`

<a id="c95"></a>
## global.web.ApiResponseHeadersFilter

정의: [src/main/java/com/dodamdodam/backend/global/web/ApiResponseHeadersFilter.java:15](../../../src/main/java/com/dodamdodam/backend/global/web/ApiResponseHeadersFilter.java#L15) · CLASS

설정: `@org.springframework.stereotype.Component` · `@org.springframework.core.annotation.Order(-2147483648)`

<a id="m511"></a>
### doFilterInternal

```java
protected void doFilterInternal(jakarta.servlet.http.HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response, jakarta.servlet.FilterChain filterChain)
```

정의: [src/main/java/com/dodamdodam/backend/global/web/ApiResponseHeadersFilter.java:19](../../../src/main/java/com/dodamdodam/backend/global/web/ApiResponseHeadersFilter.java#L19)

애너테이션: `@java.lang.Override`

호출자: 정적 Java 호출 없음

프레임워크 진입: Servlet / Spring 필터 실행

내부 호출: 없음

외부 라이브러리 호출: `jakarta.servlet.FilterChain.doFilter(jakarta.servlet.ServletRequest,jakarta.servlet.ServletResponse)` / `jakarta.servlet.http.HttpServletResponse.setHeader(java.lang.String,java.lang.String)` / `java.util.UUID.randomUUID()` / `java.util.UUID.toString()`

<a id="m512"></a>
### ApiResponseHeadersFilter 생성자 — 자동 생성

```java
public void <init>()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

외부 라이브러리 호출: `org.springframework.web.filter.OncePerRequestFilter.<init>()`

<a id="c96"></a>
## global.web.ClientAddressResolver

정의: [src/main/java/com/dodamdodam/backend/global/web/ClientAddressResolver.java:9](../../../src/main/java/com/dodamdodam/backend/global/web/ClientAddressResolver.java#L9) · CLASS

<a id="m513"></a>
### ClientAddressResolver 생성자

```java
private void <init>()
```

정의: [src/main/java/com/dodamdodam/backend/global/web/ClientAddressResolver.java:11](../../../src/main/java/com/dodamdodam/backend/global/web/ClientAddressResolver.java#L11)

상태 없이 공유하는 주소 변환 함수만 제공한다.

호출자: 정적 Java 호출 없음

내부 호출: 없음

외부 라이브러리 호출: `java.lang.Object.<init>()`

<a id="m514"></a>
### resolve

```java
public static java.net.InetAddress resolve(jakarta.servlet.http.HttpServletRequest request)
```

정의: [src/main/java/com/dodamdodam/backend/global/web/ClientAddressResolver.java:14](../../../src/main/java/com/dodamdodam/backend/global/web/ClientAddressResolver.java#L14)

Forwarded 헤더 wrapper의 주소 대신 서블릿 컨테이너가 받은 실제 연결 주소를 읽는다.

호출자: [auth.api.EmailVerificationController.issue](#m6) / [global.web.GlobalApiRateLimitFilter.doFilterInternal](#m517)

내부 호출: [auth.session.AuthStateUnavailableException.AuthStateUnavailableException](#m163)

외부 라이브러리 호출: `jakarta.servlet.ServletRequest.getRemoteAddr()` / `jakarta.servlet.ServletRequestWrapper.getRequest()` / `java.net.InetAddress.ofLiteral(java.lang.String)`

<a id="c97"></a>
## global.web.GlobalApiRateLimitFilter

정의: [src/main/java/com/dodamdodam/backend/global/web/GlobalApiRateLimitFilter.java:16](../../../src/main/java/com/dodamdodam/backend/global/web/GlobalApiRateLimitFilter.java#L16) · CLASS

<a id="m515"></a>
### GlobalApiRateLimitFilter 생성자

```java
public void <init>(com.dodamdodam.backend.auth.application.AuthRateLimitService limits, com.dodamdodam.backend.auth.infrastructure.EmailVerificationKeys keys, tools.jackson.databind.json.JsonMapper mapper)
```

정의: [src/main/java/com/dodamdodam/backend/global/web/GlobalApiRateLimitFilter.java:25](../../../src/main/java/com/dodamdodam/backend/global/web/GlobalApiRateLimitFilter.java#L25)

CORS 뒤 API·OAuth에 한 번만 적용할 전역 예산 예약 경로를 구성한다.

호출자: [global.config.SecurityConfig.applicationSecurityFilterChain](#m476)

내부 호출: 없음

외부 라이브러리 호출: `org.springframework.web.filter.OncePerRequestFilter.<init>()`

<a id="m516"></a>
### shouldNotFilter

```java
protected boolean shouldNotFilter(jakarta.servlet.http.HttpServletRequest request)
```

정의: [src/main/java/com/dodamdodam/backend/global/web/GlobalApiRateLimitFilter.java:32](../../../src/main/java/com/dodamdodam/backend/global/web/GlobalApiRateLimitFilter.java#L32)

health·정적 리소스는 제외하며 정상 CORS preflight는 앞선 CORS 필터가 종료한다.

애너테이션: `@java.lang.Override`

호출자: 정적 Java 호출 없음

프레임워크 진입: Servlet / Spring 필터 실행

내부 호출: 없음

외부 라이브러리 호출: `org.springframework.security.web.util.matcher.RequestMatcher.matches(jakarta.servlet.http.HttpServletRequest)`

<a id="m517"></a>
### doFilterInternal

```java
protected void doFilterInternal(jakarta.servlet.http.HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response, jakarta.servlet.FilterChain chain)
```

정의: [src/main/java/com/dodamdodam/backend/global/web/GlobalApiRateLimitFilter.java:38](../../../src/main/java/com/dodamdodam/backend/global/web/GlobalApiRateLimitFilter.java#L38)

업무/전송 실패와 무관하게 전역 IP 예약을 먼저 커밋하고 소진 시 공통429를 반환한다.

애너테이션: `@java.lang.Override`

호출자: 정적 Java 호출 없음

프레임워크 진입: Servlet / Spring 필터 실행

내부 호출: [auth.application.AuthRateLimitService.reserve](#m11) / [auth.exception.AuthRateLimitException.code](#m60) / [auth.exception.AuthRateLimitException.retryAfterSeconds](#m61) / [auth.infrastructure.AuthRateLimitKeyHasher.apiIp](#m74) / [auth.infrastructure.EmailVerificationKeys.rateHasher](#m118) / [global.error.ApiErrorResponses.write](#m508) / [global.web.ClientAddressResolver.resolve](#m514)

외부 라이브러리 호출: `jakarta.servlet.FilterChain.doFilter(jakarta.servlet.ServletRequest,jakarta.servlet.ServletResponse)` / `jakarta.servlet.http.HttpServletResponse.setHeader(java.lang.String,java.lang.String)` / `java.lang.Long.toString(long)` / `java.lang.Throwable.getMessage()` / `java.util.List.of(E)`

<a id="c98"></a>
## global.web.Part1JsonRequestGuardFilter

정의: [src/main/java/com/dodamdodam/backend/global/web/Part1JsonRequestGuardFilter.java:24](../../../src/main/java/com/dodamdodam/backend/global/web/Part1JsonRequestGuardFilter.java#L24) · CLASS

<a id="m518"></a>
### Part1JsonRequestGuardFilter 생성자

```java
public void <init>(tools.jackson.databind.json.JsonMapper jsonMapper)
```

정의: [src/main/java/com/dodamdodam/backend/global/web/Part1JsonRequestGuardFilter.java:37](../../../src/main/java/com/dodamdodam/backend/global/web/Part1JsonRequestGuardFilter.java#L37)

공통 오류 응답에 사용할 JSON 직렬화기를 받는다.

호출자: [global.config.SecurityConfig.applicationSecurityFilterChain](#m476)

내부 호출: 없음

외부 라이브러리 호출: `org.springframework.web.filter.OncePerRequestFilter.<init>()`

<a id="m519"></a>
### shouldNotFilter

```java
protected boolean shouldNotFilter(jakarta.servlet.http.HttpServletRequest request)
```

정의: [src/main/java/com/dodamdodam/backend/global/web/Part1JsonRequestGuardFilter.java:42](../../../src/main/java/com/dodamdodam/backend/global/web/Part1JsonRequestGuardFilter.java#L42)

로그아웃·자녀 등록·이메일 발급 POST에 전송 크기·형식 검사를 적용한다.

애너테이션: `@java.lang.Override`

호출자: 정적 Java 호출 없음

프레임워크 진입: Servlet / Spring 필터 실행

내부 호출: 없음

외부 라이브러리 호출: `org.springframework.security.web.util.matcher.RequestMatcher.matches(jakarta.servlet.http.HttpServletRequest)`

<a id="m520"></a>
### doFilterInternal

```java
protected void doFilterInternal(jakarta.servlet.http.HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response, jakarta.servlet.FilterChain filterChain)
```

정의: [src/main/java/com/dodamdodam/backend/global/web/Part1JsonRequestGuardFilter.java:48](../../../src/main/java/com/dodamdodam/backend/global/web/Part1JsonRequestGuardFilter.java#L48)

CSRF 전에 전송 규칙을 검사하고, 후속 MVC가 읽을 수 있도록 본문을 보관한다.

애너테이션: `@java.lang.Override`

호출자: 정적 Java 호출 없음

프레임워크 진입: Servlet / Spring 필터 실행

내부 호출: [global.error.ApiErrorResponses.write](#m508) / [global.web.Part1JsonRequestGuardFilter.supportsContentType](#m521) / [global.web.Part1JsonRequestGuardFilter.CachedBodyRequest.CachedBodyRequest](#m522)

외부 라이브러리 호출: `jakarta.servlet.FilterChain.doFilter(jakarta.servlet.ServletRequest,jakarta.servlet.ServletResponse)` / `jakarta.servlet.ServletInputStream.readNBytes(int)` / `jakarta.servlet.ServletRequest.getInputStream()` / `jakarta.servlet.ServletRequest.setAttribute(java.lang.String,java.lang.Object)`

<a id="m521"></a>
### supportsContentType

```java
private boolean supportsContentType(jakarta.servlet.http.HttpServletRequest request, boolean emptyBody)
```

정의: [src/main/java/com/dodamdodam/backend/global/web/Part1JsonRequestGuardFilter.java:69](../../../src/main/java/com/dodamdodam/backend/global/web/Part1JsonRequestGuardFilter.java#L69)

JSON·UTF-8을 허용하고, Content-Type 없는 빈 본문은 로그아웃에만 허용한다.

호출자: [global.web.Part1JsonRequestGuardFilter.doFilterInternal](#m520)

내부 호출: 없음

외부 라이브러리 호출: `jakarta.servlet.ServletRequest.getContentType()` / `java.lang.String.equals(java.lang.Object)` / `java.nio.charset.Charset.equals(java.lang.Object)` / `org.springframework.http.MediaType.parseMediaType(java.lang.String)` / `org.springframework.security.web.util.matcher.RequestMatcher.matches(jakarta.servlet.http.HttpServletRequest)` / `org.springframework.util.MimeType.getCharset()` / `org.springframework.util.MimeType.getSubtype()` / `org.springframework.util.MimeType.getType()`

<a id="c99"></a>
## global.web.Part1JsonRequestGuardFilter.CachedBodyRequest

정의: [src/main/java/com/dodamdodam/backend/global/web/Part1JsonRequestGuardFilter.java:84](../../../src/main/java/com/dodamdodam/backend/global/web/Part1JsonRequestGuardFilter.java#L84) · CLASS

크기 검사를 마친 본문을 동기 MVC 처리에서 다시 읽을 수 있게 제공한다.

<a id="m522"></a>
### CachedBodyRequest 생성자

```java
private void <init>(jakarta.servlet.http.HttpServletRequest request, byte[] body)
```

정의: [src/main/java/com/dodamdodam/backend/global/web/Part1JsonRequestGuardFilter.java:88](../../../src/main/java/com/dodamdodam/backend/global/web/Part1JsonRequestGuardFilter.java#L88)

원래 요청의 세션·헤더·속성은 위임하고 검사한 본문만 보관한다.

호출자: [global.web.Part1JsonRequestGuardFilter.doFilterInternal](#m520)

내부 호출: 없음

외부 라이브러리 호출: `jakarta.servlet.http.HttpServletRequestWrapper.<init>(jakarta.servlet.http.HttpServletRequest)`

<a id="m523"></a>
### getInputStream

```java
public jakarta.servlet.ServletInputStream getInputStream()
```

정의: [src/main/java/com/dodamdodam/backend/global/web/Part1JsonRequestGuardFilter.java:94](../../../src/main/java/com/dodamdodam/backend/global/web/Part1JsonRequestGuardFilter.java#L94)

소비한 원본 스트림 대신 매번 본문 처음부터 읽는 스트림을 반환한다.

애너테이션: `@java.lang.Override`

호출자: [global.web.Part1JsonRequestGuardFilter.CachedBodyRequest.getReader](#m529)

내부 호출: [global.web.Part1JsonRequestGuardFilter.CachedBodyRequest$anonymous@97.CachedBodyRequest$anonymous@97](#m533)

외부 라이브러리 호출: `java.io.ByteArrayInputStream.<init>(byte[])`

<a id="m529"></a>
### getReader

```java
public java.io.BufferedReader getReader()
```

정의: [src/main/java/com/dodamdodam/backend/global/web/Part1JsonRequestGuardFilter.java:132](../../../src/main/java/com/dodamdodam/backend/global/web/Part1JsonRequestGuardFilter.java#L132)

허용한 JSON 인코딩인 UTF-8로 본문을 다시 읽는다.

애너테이션: `@java.lang.Override`

호출자: 정적 Java 호출 없음

내부 호출: [global.web.Part1JsonRequestGuardFilter.CachedBodyRequest.getInputStream](#m523)

외부 라이브러리 호출: `java.io.BufferedReader.<init>(java.io.Reader)` / `java.io.InputStreamReader.<init>(java.io.InputStream,java.nio.charset.Charset)`

<a id="m530"></a>
### getContentLength

```java
public int getContentLength()
```

정의: [src/main/java/com/dodamdodam/backend/global/web/Part1JsonRequestGuardFilter.java:138](../../../src/main/java/com/dodamdodam/backend/global/web/Part1JsonRequestGuardFilter.java#L138)

헤더의 주장 대신 실제 보관한 본문 길이를 제공한다.

애너테이션: `@java.lang.Override`

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m531"></a>
### getContentLengthLong

```java
public long getContentLengthLong()
```

정의: [src/main/java/com/dodamdodam/backend/global/web/Part1JsonRequestGuardFilter.java:144](../../../src/main/java/com/dodamdodam/backend/global/web/Part1JsonRequestGuardFilter.java#L144)

long 길이를 사용하는 소비자에게도 같은 실제 본문 길이를 제공한다.

애너테이션: `@java.lang.Override`

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m532"></a>
### getCharacterEncoding

```java
public java.lang.String getCharacterEncoding()
```

정의: [src/main/java/com/dodamdodam/backend/global/web/Part1JsonRequestGuardFilter.java:150](../../../src/main/java/com/dodamdodam/backend/global/web/Part1JsonRequestGuardFilter.java#L150)

Reader와 인코딩 메타데이터를 UTF-8로 일치시킨다.

애너테이션: `@java.lang.Override`

호출자: 정적 Java 호출 없음

내부 호출: 없음

외부 라이브러리 호출: `java.nio.charset.Charset.name()`

<a id="c100"></a>
## global.web.Part1JsonRequestGuardFilter.CachedBodyRequest$anonymous@97

정의: [src/main/java/com/dodamdodam/backend/global/web/Part1JsonRequestGuardFilter.java:97](../../../src/main/java/com/dodamdodam/backend/global/web/Part1JsonRequestGuardFilter.java#L97) · CLASS

<a id="m524"></a>
### read

```java
public int read()
```

정의: [src/main/java/com/dodamdodam/backend/global/web/Part1JsonRequestGuardFilter.java:99](../../../src/main/java/com/dodamdodam/backend/global/web/Part1JsonRequestGuardFilter.java#L99)

보관한 본문에서 한 바이트를 읽는다.

애너테이션: `@java.lang.Override`

호출자: 정적 Java 호출 없음

내부 호출: 없음

외부 라이브러리 호출: `java.io.ByteArrayInputStream.read()`

<a id="m525"></a>
### read

```java
public int read(byte[] bytes, int offset, int length)
```

정의: [src/main/java/com/dodamdodam/backend/global/web/Part1JsonRequestGuardFilter.java:105](../../../src/main/java/com/dodamdodam/backend/global/web/Part1JsonRequestGuardFilter.java#L105)

JSON 파서의 버퍼 읽기를 보관한 본문으로 위임한다.

애너테이션: `@java.lang.Override`

호출자: 정적 Java 호출 없음

내부 호출: 없음

외부 라이브러리 호출: `java.io.ByteArrayInputStream.read(byte[],int,int)`

<a id="m526"></a>
### isFinished

```java
public boolean isFinished()
```

정의: [src/main/java/com/dodamdodam/backend/global/web/Part1JsonRequestGuardFilter.java:111](../../../src/main/java/com/dodamdodam/backend/global/web/Part1JsonRequestGuardFilter.java#L111)

해당 스트림의 본문이 모두 소비됐는지 반환한다.

애너테이션: `@java.lang.Override`

호출자: 정적 Java 호출 없음

내부 호출: 없음

외부 라이브러리 호출: `java.io.ByteArrayInputStream.available()`

<a id="m527"></a>
### isReady

```java
public boolean isReady()
```

정의: [src/main/java/com/dodamdodam/backend/global/web/Part1JsonRequestGuardFilter.java:117](../../../src/main/java/com/dodamdodam/backend/global/web/Part1JsonRequestGuardFilter.java#L117)

본문이 메모리에 있어 네트워크 입력 대기가 필요하지 않다.

애너테이션: `@java.lang.Override`

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m528"></a>
### setReadListener

```java
public void setReadListener(jakarta.servlet.ReadListener listener)
```

정의: [src/main/java/com/dodamdodam/backend/global/web/Part1JsonRequestGuardFilter.java:123](../../../src/main/java/com/dodamdodam/backend/global/web/Part1JsonRequestGuardFilter.java#L123)

현재 동기 MVC 계약에서 비동기 읽기 등록을 허용하지 않는다.

애너테이션: `@java.lang.Override`

호출자: 정적 Java 호출 없음

내부 호출: 없음

외부 라이브러리 호출: `java.lang.IllegalStateException.<init>(java.lang.String)` / `java.util.Objects.requireNonNull(T,java.lang.String)`

<a id="m533"></a>
### 익명 클래스@97 생성자 — 자동 생성

```java
void <init>()
```

호출자: [global.web.Part1JsonRequestGuardFilter.CachedBodyRequest.getInputStream](#m523)

내부 호출: 없음

외부 라이브러리 호출: `jakarta.servlet.ServletInputStream.<init>()`

<a id="c101"></a>
## global.web.Part2JsonRequestGuardFilter

정의: [src/main/java/com/dodamdodam/backend/global/web/Part2JsonRequestGuardFilter.java:19](../../../src/main/java/com/dodamdodam/backend/global/web/Part2JsonRequestGuardFilter.java#L19) · CLASS

설정: `@org.jspecify.annotations.NullMarked`

<a id="m534"></a>
### Part2JsonRequestGuardFilter 생성자

```java
public void <init>(tools.jackson.databind.json.JsonMapper jsonMapper)
```

정의: [src/main/java/com/dodamdodam/backend/global/web/Part2JsonRequestGuardFilter.java:27](../../../src/main/java/com/dodamdodam/backend/global/web/Part2JsonRequestGuardFilter.java#L27)

Security 체인에서만 등록하며 공통 오류 직렬화기를 재사용한다.

호출자: [global.config.SecurityConfig.applicationSecurityFilterChain](#m476)

내부 호출: 없음

외부 라이브러리 호출: `org.springframework.web.filter.OncePerRequestFilter.<init>()`

<a id="m535"></a>
### shouldNotFilter

```java
protected boolean shouldNotFilter(jakarta.servlet.http.HttpServletRequest request)
```

정의: [src/main/java/com/dodamdodam/backend/global/web/Part2JsonRequestGuardFilter.java:32](../../../src/main/java/com/dodamdodam/backend/global/web/Part2JsonRequestGuardFilter.java#L32)

현재 구현된 Part 2 JSON 본문 경로인 대화 시작에만 적용한다.

애너테이션: `@java.lang.Override`

호출자: 정적 Java 호출 없음

프레임워크 진입: Servlet / Spring 필터 실행

내부 호출: 없음

외부 라이브러리 호출: `org.springframework.security.web.util.matcher.RequestMatcher.matches(jakarta.servlet.http.HttpServletRequest)`

<a id="m536"></a>
### doFilterInternal

```java
protected void doFilterInternal(jakarta.servlet.http.HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response, jakarta.servlet.FilterChain filterChain)
```

정의: [src/main/java/com/dodamdodam/backend/global/web/Part2JsonRequestGuardFilter.java:38](../../../src/main/java/com/dodamdodam/backend/global/web/Part2JsonRequestGuardFilter.java#L38)

CSRF·인증 전에 형식을 거부하고 본문·구조 검증은 후속 처리에 남긴다.

애너테이션: `@java.lang.Override`

호출자: 정적 Java 호출 없음

프레임워크 진입: Servlet / Spring 필터 실행

내부 호출: [global.error.ApiErrorResponses.write](#m508) / [global.web.Part2JsonRequestGuardFilter.supportsContentType](#m537)

외부 라이브러리 호출: `jakarta.servlet.FilterChain.doFilter(jakarta.servlet.ServletRequest,jakarta.servlet.ServletResponse)` / `jakarta.servlet.ServletRequest.getContentType()`

<a id="m537"></a>
### supportsContentType

```java
private boolean supportsContentType(java.lang.@org.jspecify.annotations.Nullable String contentType)
```

정의: [src/main/java/com/dodamdodam/backend/global/web/Part2JsonRequestGuardFilter.java:51](../../../src/main/java/com/dodamdodam/backend/global/web/Part2JsonRequestGuardFilter.java#L51)

명세의 application/json과 생략 또는 UTF-8인 charset만 허용한다.

호출자: [global.web.Part2JsonRequestGuardFilter.doFilterInternal](#m536)

내부 호출: 없음

외부 라이브러리 호출: `java.lang.String.equals(java.lang.Object)` / `java.nio.charset.Charset.equals(java.lang.Object)` / `org.springframework.http.MediaType.parseMediaType(java.lang.String)` / `org.springframework.util.MimeType.getCharset()` / `org.springframework.util.MimeType.getSubtype()` / `org.springframework.util.MimeType.getType()`

<a id="c102"></a>
## guardian.api.GuardianAccessExceptionHandler

정의: [src/main/java/com/dodamdodam/backend/guardian/api/GuardianAccessExceptionHandler.java:16](../../../src/main/java/com/dodamdodam/backend/guardian/api/GuardianAccessExceptionHandler.java#L16) · CLASS

설정: `@org.springframework.core.annotation.Order(-2147483648)` · `@org.springframework.web.bind.annotation.RestControllerAdvice(basePackages={"com.dodamdodam.backend"})`

<a id="m538"></a>
### handle

```java
public org.springframework.http.ResponseEntity<com.dodamdodam.backend.global.error.ApiErrorResponse> handle(com.dodamdodam.backend.guardian.exception.GuardianAccessException exception, jakarta.servlet.http.HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response)
```

정의: [src/main/java/com/dodamdodam/backend/guardian/api/GuardianAccessExceptionHandler.java:21](../../../src/main/java/com/dodamdodam/backend/guardian/api/GuardianAccessExceptionHandler.java#L21)

최초 권한 오류를 공통 응답으로 바꾸고 응답 직전 실패는 외곽 필터로 넘긴다.

애너테이션: `@org.springframework.web.bind.annotation.ExceptionHandler({com.dodamdodam.backend.guardian.exception.GuardianAccessException.class})`

호출자: 정적 Java 호출 없음

프레임워크 진입: Spring MVC 예외 처리

내부 호출: [global.error.ApiErrorResponses.body](#m507) / [guardian.application.GuardianAccessService.requiresResponseVerification](#m542) / [guardian.exception.GuardianAccessException.code](#m550) / [guardian.exception.GuardianAccessException.status](#m549)

외부 라이브러리 호출: `java.lang.Throwable.getMessage()` / `java.util.List.of()` / `org.springframework.http.ResponseEntity.status(int)` / `org.springframework.http.ResponseEntity.BodyBuilder.body(@org.jspecify.annotations.Nullable T)`

<a id="m539"></a>
### GuardianAccessExceptionHandler 생성자 — 자동 생성

```java
public void <init>()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

외부 라이브러리 호출: `java.lang.Object.<init>()`

<a id="c103"></a>
## guardian.application.GuardianAccessService

정의: [src/main/java/com/dodamdodam/backend/guardian/application/GuardianAccessService.java:10](../../../src/main/java/com/dodamdodam/backend/guardian/application/GuardianAccessService.java#L10) · CLASS

설정: `@org.springframework.stereotype.Service`

<a id="m540"></a>
### GuardianAccessService 생성자

```java
public void <init>(com.dodamdodam.backend.guardian.infrastructure.GuardianAccessRepository repository)
```

정의: [src/main/java/com/dodamdodam/backend/guardian/application/GuardianAccessService.java:18](../../../src/main/java/com/dodamdodam/backend/guardian/application/GuardianAccessService.java#L18)

현재 DB 권한을 조회할 저장소를 구성한다.

호출자: 정적 Java 호출 없음

내부 호출: 없음

외부 라이브러리 호출: `java.lang.Object.<init>()`

<a id="m541"></a>
### requireGuardianAccess

```java
public java.time.OffsetDateTime requireGuardianAccess(jakarta.servlet.http.HttpServletRequest request, com.dodamdodam.backend.auth.session.AuthenticatedSession authenticated)
```

정의: [src/main/java/com/dodamdodam/backend/guardian/application/GuardianAccessService.java:23](../../../src/main/java/com/dodamdodam/backend/guardian/application/GuardianAccessService.java#L23)

같은 요청의 인증·소유권 검사 뒤 보호자 확인 기한을 요구한다.

호출자: 정적 Java 호출 없음

내부 호출: [guardian.application.GuardianAccessService.requireCurrentAccess](#m544)

외부 라이브러리 호출: `jakarta.servlet.ServletRequest.setAttribute(java.lang.String,java.lang.Object)`

<a id="m542"></a>
### requiresResponseVerification

```java
public static boolean requiresResponseVerification(jakarta.servlet.http.HttpServletRequest request)
```

정의: [src/main/java/com/dodamdodam/backend/guardian/application/GuardianAccessService.java:31](../../../src/main/java/com/dodamdodam/backend/guardian/application/GuardianAccessService.java#L31)

재검사 대상 요청인지만 반환하며, 표식 자체가 현재 권한을 보증하지는 않는다.

호출자: [guardian.api.GuardianAccessExceptionHandler.handle](#m538)

내부 호출: 없음

외부 라이브러리 호출: `jakarta.servlet.ServletRequest.getAttribute(java.lang.String)`

<a id="m543"></a>
### verifyResponseAccess

```java
public void verifyResponseAccess(jakarta.servlet.http.HttpServletRequest request)
```

정의: [src/main/java/com/dodamdodam/backend/guardian/application/GuardianAccessService.java:36](../../../src/main/java/com/dodamdodam/backend/guardian/application/GuardianAccessService.java#L36)

보호자 권한 검사에 성공한 요청의 현재 DB 권한을 다시 확인한다.

호출자: [auth.session.AuthStateErrorFilter.verifyResponseAccess](#m156)

내부 호출: [guardian.application.GuardianAccessService.requireCurrentAccess](#m544)

외부 라이브러리 호출: `jakarta.servlet.ServletRequest.getAttribute(java.lang.String)`

<a id="m544"></a>
### requireCurrentAccess

```java
private java.time.OffsetDateTime requireCurrentAccess(com.dodamdodam.backend.auth.session.AuthenticatedSession authenticated)
```

정의: [src/main/java/com/dodamdodam/backend/guardian/application/GuardianAccessService.java:44](../../../src/main/java/com/dodamdodam/backend/guardian/application/GuardianAccessService.java#L44)

로그인 문맥 유효성을 먼저 확인한 뒤 같은 문맥의 최신 보호자 권한을 요구한다.

호출자: [guardian.application.GuardianAccessService.requireGuardianAccess](#m541) / [guardian.application.GuardianAccessService.verifyResponseAccess](#m543)

내부 호출: [auth.session.AuthenticatedSession.accountId](#m166) / [auth.session.AuthenticatedSession.securityContextId](#m169) / [guardian.exception.GuardianAccessException.authenticationRequired](#m546) / [guardian.exception.GuardianAccessException.unlockRequired](#m547) / [guardian.infrastructure.GuardianAccessRepository.findState](#m552) / [guardian.infrastructure.GuardianAccessRepository.GuardianAccessState.guardianUntil](#m563) / [guardian.infrastructure.GuardianAccessRepository.GuardianAccessState.isGuardianCurrent](#m554) / [guardian.infrastructure.GuardianAccessRepository.GuardianAccessState.isSessionCurrent](#m553)

외부 라이브러리 호출: `java.util.Optional.orElseThrow(java.util.function.Supplier<? extends X>)`

<a id="c104"></a>
## guardian.exception.GuardianAccessException

정의: [src/main/java/com/dodamdodam/backend/guardian/exception/GuardianAccessException.java:3](../../../src/main/java/com/dodamdodam/backend/guardian/exception/GuardianAccessException.java#L3) · CLASS

<a id="m545"></a>
### GuardianAccessException 생성자

```java
private void <init>(int status, java.lang.String code, java.lang.String message, java.lang.Throwable cause)
```

정의: [src/main/java/com/dodamdodam/backend/guardian/exception/GuardianAccessException.java:9](../../../src/main/java/com/dodamdodam/backend/guardian/exception/GuardianAccessException.java#L9)

정해진 권한 오류만 만들 수 있도록 상태·코드·원인을 내부에서 구성한다.

호출자: [guardian.exception.GuardianAccessException.authenticationRequired](#m546) / [guardian.exception.GuardianAccessException.unavailable](#m548) / [guardian.exception.GuardianAccessException.unlockRequired](#m547)

내부 호출: 없음

외부 라이브러리 호출: `java.lang.RuntimeException.<init>(java.lang.String,java.lang.Throwable)`

<a id="m546"></a>
### authenticationRequired

```java
public static com.dodamdodam.backend.guardian.exception.GuardianAccessException authenticationRequired()
```

정의: [src/main/java/com/dodamdodam/backend/guardian/exception/GuardianAccessException.java:16](../../../src/main/java/com/dodamdodam/backend/guardian/exception/GuardianAccessException.java#L16)

현재 로그인 문맥이 없거나 유효하지 않은 경우를 표시한다.

호출자: [guardian.application.GuardianAccessService.requireCurrentAccess](#m544)

내부 호출: [guardian.exception.GuardianAccessException.GuardianAccessException](#m545)

<a id="m547"></a>
### unlockRequired

```java
public static com.dodamdodam.backend.guardian.exception.GuardianAccessException unlockRequired()
```

정의: [src/main/java/com/dodamdodam/backend/guardian/exception/GuardianAccessException.java:21](../../../src/main/java/com/dodamdodam/backend/guardian/exception/GuardianAccessException.java#L21)

현재 세션에 유효한 보호자 확인이 필요한 경우를 표시한다.

호출자: [guardian.application.GuardianAccessService.requireCurrentAccess](#m544)

내부 호출: [guardian.exception.GuardianAccessException.GuardianAccessException](#m545)

<a id="m548"></a>
### unavailable

```java
public static com.dodamdodam.backend.guardian.exception.GuardianAccessException unavailable(java.lang.Throwable cause)
```

정의: [src/main/java/com/dodamdodam/backend/guardian/exception/GuardianAccessException.java:26](../../../src/main/java/com/dodamdodam/backend/guardian/exception/GuardianAccessException.java#L26)

저장소 확인 실패의 원인은 보존하고 응답 메시지는 고정한다.

호출자: [guardian.infrastructure.GuardianAccessRepository.findState](#m552)

내부 호출: [guardian.exception.GuardianAccessException.GuardianAccessException](#m545)

<a id="m549"></a>
### status

```java
public int status()
```

정의: [src/main/java/com/dodamdodam/backend/guardian/exception/GuardianAccessException.java:32](../../../src/main/java/com/dodamdodam/backend/guardian/exception/GuardianAccessException.java#L32)

API와 응답 재검사 경로가 사용할 HTTP 상태를 반환한다.

호출자: [auth.session.AuthStateErrorFilter.doFilterInternal](#m154) / [guardian.api.GuardianAccessExceptionHandler.handle](#m538)

내부 호출: 없음

<a id="m550"></a>
### code

```java
public java.lang.String code()
```

정의: [src/main/java/com/dodamdodam/backend/guardian/exception/GuardianAccessException.java:37](../../../src/main/java/com/dodamdodam/backend/guardian/exception/GuardianAccessException.java#L37)

외부 응답에 사용할 고정 오류 코드를 반환한다.

호출자: [auth.session.AuthStateErrorFilter.doFilterInternal](#m154) / [guardian.api.GuardianAccessExceptionHandler.handle](#m538)

내부 호출: 없음

<a id="c105"></a>
## guardian.infrastructure.GuardianAccessRepository

정의: [src/main/java/com/dodamdodam/backend/guardian/infrastructure/GuardianAccessRepository.java:13](../../../src/main/java/com/dodamdodam/backend/guardian/infrastructure/GuardianAccessRepository.java#L13) · CLASS

설정: `@org.springframework.stereotype.Repository`

<a id="m551"></a>
### GuardianAccessRepository 생성자

```java
public void <init>(org.springframework.jdbc.core.JdbcTemplate jdbc)
```

정의: [src/main/java/com/dodamdodam/backend/guardian/infrastructure/GuardianAccessRepository.java:19](../../../src/main/java/com/dodamdodam/backend/guardian/infrastructure/GuardianAccessRepository.java#L19)

계정·문맥·PIN 버전을 같은 DB 조회로 확인할 저장소를 구성한다.

호출자: 정적 Java 호출 없음

내부 호출: 없음

외부 라이브러리 호출: `java.lang.Object.<init>()`

<a id="m552"></a>
### findState

```java
public java.util.Optional<com.dodamdodam.backend.guardian.infrastructure.GuardianAccessRepository.GuardianAccessState> findState(java.util.UUID accountId, java.util.UUID securityContextId)
```

정의: [src/main/java/com/dodamdodam/backend/guardian/infrastructure/GuardianAccessRepository.java:24](../../../src/main/java/com/dodamdodam/backend/guardian/infrastructure/GuardianAccessRepository.java#L24)

프레임워크 인증 검사 뒤 사용할 현재 문맥·보호자 권한과 DB 시각을 조회한다.

호출자: [guardian.application.GuardianAccessService.requireCurrentAccess](#m544)

내부 호출: [guardian.exception.GuardianAccessException.unavailable](#m548) / [guardian.infrastructure.GuardianAccessRepository.GuardianAccessState.GuardianAccessState](#m555)

외부 라이브러리 호출: `java.sql.ResultSet.getLong(java.lang.String)` / `java.sql.ResultSet.getObject(java.lang.String,java.lang.Class<T>)` / `java.util.Collection.stream()` / `java.util.stream.Stream.findFirst()` / `org.springframework.jdbc.core.JdbcTemplate.query(java.lang.String,org.springframework.jdbc.core.RowMapper<T>,java.lang.@org.jspecify.annotations.Nullable Object @org.jspecify.annotations.Nullable [])`

<a id="c106"></a>
## guardian.infrastructure.GuardianAccessRepository.GuardianAccessState

정의: [src/main/java/com/dodamdodam/backend/guardian/infrastructure/GuardianAccessRepository.java:50](../../../src/main/java/com/dodamdodam/backend/guardian/infrastructure/GuardianAccessRepository.java#L50) · RECORD

<a id="m553"></a>
### isSessionCurrent

```java
public boolean isSessionCurrent()
```

정의: [src/main/java/com/dodamdodam/backend/guardian/infrastructure/GuardianAccessRepository.java:56](../../../src/main/java/com/dodamdodam/backend/guardian/infrastructure/GuardianAccessRepository.java#L56)

DB의 세션 버전·폐기·만료 기준으로 문맥 유효성을 판정한다.

호출자: [guardian.application.GuardianAccessService.requireCurrentAccess](#m544) / [guardian.infrastructure.GuardianAccessRepository.GuardianAccessState.isGuardianCurrent](#m554)

내부 호출: 없음

외부 라이브러리 호출: `java.time.OffsetDateTime.isBefore(java.time.OffsetDateTime)`

<a id="m554"></a>
### isGuardianCurrent

```java
public boolean isGuardianCurrent()
```

정의: [src/main/java/com/dodamdodam/backend/guardian/infrastructure/GuardianAccessRepository.java:63](../../../src/main/java/com/dodamdodam/backend/guardian/infrastructure/GuardianAccessRepository.java#L63)

같은 문맥의 미만료 보호자 확인과 현재 PIN 버전 일치를 함께 요구한다.

호출자: [guardian.application.GuardianAccessService.requireCurrentAccess](#m544)

내부 호출: [guardian.infrastructure.GuardianAccessRepository.GuardianAccessState.isSessionCurrent](#m553)

외부 라이브러리 호출: `java.lang.Long.equals(java.lang.Object)` / `java.time.OffsetDateTime.isBefore(java.time.OffsetDateTime)`

<a id="m555"></a>
### GuardianAccessState 생성자 — 자동 생성

```java
public void <init>(long contextSessionVersion, long accountSessionVersion, java.time.OffsetDateTime revokedAt, java.time.OffsetDateTime expiresAt, java.time.OffsetDateTime guardianUntil, java.lang.Long guardianPinVersion, java.lang.Long currentPinVersion, java.time.OffsetDateTime checkedAt)
```

호출자: [guardian.infrastructure.GuardianAccessRepository.findState](#m552)

내부 호출: 없음

외부 라이브러리 호출: `java.lang.Record.<init>()`

<a id="m556"></a>
### accountSessionVersion — 자동 생성

```java
public long accountSessionVersion()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m557"></a>
### checkedAt — 자동 생성

```java
public java.time.OffsetDateTime checkedAt()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m558"></a>
### contextSessionVersion — 자동 생성

```java
public long contextSessionVersion()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m559"></a>
### currentPinVersion — 자동 생성

```java
public java.lang.Long currentPinVersion()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m560"></a>
### equals — 자동 생성

```java
public final boolean equals(java.lang.Object o)
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m561"></a>
### expiresAt — 자동 생성

```java
public java.time.OffsetDateTime expiresAt()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m562"></a>
### guardianPinVersion — 자동 생성

```java
public java.lang.Long guardianPinVersion()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m563"></a>
### guardianUntil — 자동 생성

```java
public java.time.OffsetDateTime guardianUntil()
```

호출자: [guardian.application.GuardianAccessService.requireCurrentAccess](#m544)

내부 호출: 없음

<a id="m564"></a>
### hashCode — 자동 생성

```java
public final int hashCode()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m565"></a>
### revokedAt — 자동 생성

```java
public java.time.OffsetDateTime revokedAt()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음

<a id="m566"></a>
### toString — 자동 생성

```java
public final java.lang.String toString()
```

호출자: 정적 Java 호출 없음

내부 호출: 없음
