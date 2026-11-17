# 이메일 인증번호 발급

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|시작]] → [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요]] → 이 흐름 → 메서드 정의

**진입:** `POST /api/v1/auth/email-verifications`

**결과:** 200 · data.challengeId / expiresAt

> [!important] 먼저 알아둘 점
> challenge를 커밋한 뒤 SMTP를 호출한다. 발급 API이며 인증번호 검증·회원가입 API는 후속이다.

![[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Assets/02-email.svg]]

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Flows/02-email.canvas|같은 흐름을 Canvas에서 열기]] · 아래 코드 링크로 실제 메서드 이동

번호와 화살표는 **주요 처리 순서**다. 조건에 따른 조기 반환·실패와 중간 helper 호출은 생략했다. 직접 Java 호출 관계는 연결된 메서드 노트에서 확인한다.

## 단계별 코드

### 1. 요청 · 목적 검사

JSON을 읽고 허용 필드 확인 · PIN 목적은 로그인 먼저 확인

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.api.EmailVerificationController#issue m90c67bb20e0ca690|EmailVerificationController.issue]]

### 2. 정규화 · 예산 예약

이메일·목적별 제한 예약 · 6자리 번호 생성과 HMAC

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.EmailVerificationIssuanceService#issue mcb0a83ce2cd55ce0|EmailVerificationIssuanceService.issue]]
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.AuthRateLimitService#reserve m63e1737a4e336ba7|AuthRateLimitService.reserve]]

### 3. 목적별 현재 상태 확인

공개 목적: 계정 적격 여부 · PIN 목적: 현재 문맥·PIN 잠금

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.EmailVerificationIssuanceService#preparePublic m8348ca544b936e88|EmailVerificationIssuanceService.preparePublic]]
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.EmailVerificationIssuanceService#preparePin m330c48cfa5c13034|EmailVerificationIssuanceService.preparePin]]

### 4. challenge 저장 · 커밋

이전 증명을 무효화 · 새 발급과 만료 시각 저장

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.EmailVerificationRepository#replace m14c7e6b25ddbfbd4|EmailVerificationRepository.replace]]

### 5. 메일 전달

업무 트랜잭션 밖에서 SMTP · 실패하면 이 발급만 폐기

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.EmailVerificationDeliveryService#deliver mce05e4112cdb91d4|EmailVerificationDeliveryService.deliver]]
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.infrastructure.SmtpEmailVerificationSender#send mbb7d859c7dfa3045|SmtpEmailVerificationSender.send]]

### 6. 응답 권한 재확인

현재 proof·문맥·PIN 재검사 · 보낼 때도 최신 상태 확인

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.EmailVerificationIssuanceService#verifyResponseAccess me8292ce668aadbdf|EmailVerificationIssuanceService.verifyResponseAccess]]
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthStateErrorFilter#verifyResponseAccess m1098528f38d3ff11|AuthStateErrorFilter.verifyResponseAccess]]

## 실패하거나 달라지는 경우

| 조건 | 결과 / 주의 | 확인할 코드 |
| --- | --- | --- |
| 발급 예산 / 재전송 제한 | 429 · Retry-After | [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.api.EmailVerificationExceptionHandler#limited m77259e0b6f3fe9c9\|EmailVerificationExceptionHandler.limited]] |
| SMTP 실패 / 응답 유실 | 해당 발급을 보상 폐기 후 오류 반환 | [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.EmailVerificationDeliveryService#invalidate m2641bd7bf8eedcbb\|EmailVerificationDeliveryService.invalidate]] |
| 전송 사이 권한 변경 | 해당 발급 폐기·성공 응답 거부 | [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.application.EmailVerificationIssuanceService#verifyResponseAccess me8292ce668aadbdf\|EmailVerificationIssuanceService.verifyResponseAccess]] |

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|관련 클래스 찾기]]
