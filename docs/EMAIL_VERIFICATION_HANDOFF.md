# 이메일 인증번호 발급 구현과 후속 인계

[SCRUM-77](https://dodamdodam.atlassian.net/browse/SCRUM-77)의 로컬 구현·검증 기준이다.
2026-11-17 전체 검증을 마쳤으며 원격 push·PR·병합은 별도 단계다.

## 구현된 요청 흐름

`POST /api/v1/auth/email-verifications`는 성공 시 200과
`data.challengeId`, `data.expiresAt`만 반환한다. 인증번호·계정 적격 여부는 응답하지 않는다.

| 목적 | 사용할 이메일과 권한 |
| --- | --- |
| SIGNUP | 정규화한 입력 이메일. 가입 가능한 계정에만 사용 가능한 번호 발급 |
| RESET_PASSWORD | 정규화한 입력 이메일. 존재하는 계정의 현재 상태를 발급에 결합 |
| PIN_SETUP | 현재 로그인 계정 이메일·보안 문맥·설정 세대. 기존 PIN이 있으면 거부 |
| PIN_RESET | 현재 로그인 계정 이메일·보안 문맥·설정 세대·PIN 버전. PIN이 없으면 거부 |

1. 공통 전송 검사·CSRF 검사 후 목적별 인증과 입력 구조를 확인한다.
2. 전역 IP 예산과 이메일 발급 예산을 각각 독립 트랜잭션으로 예약한다.
3. 목적별 잠금 아래 기존 미소비 증명을 폐기하고 새 challenge를 저장·커밋한다.
4. 업무 DB 잠금 밖에서 SMTP를 호출한다. 전송 실패·결과 불명확이면 해당 발급만 별도 트랜잭션으로 폐기한다.
5. 발송 뒤와 응답 전송 직전에 현재 DB 권한을 재확인한다.

공개 목적의 부적격 요청도 같은 응답 형식과 SMTP 경로를 거친다.
사용 가능한 번호를 보내지 않는 안내 메일을 사용하며 계정 존재 여부를 공개하지 않는다.

## SCRUM-90에서 재사용할 기준

| 코드 / 데이터 | 후속 작업에서 지킬 계약 |
| --- | --- |
| [IssueEmailVerificationRequest](../src/main/java/com/dodamdodam/backend/auth/dto/IssueEmailVerificationRequest.java) | 이메일·목적의 정규화와 기존 입력 규칙 유지 |
| [EmailVerificationCodeHasher](../src/main/java/com/dodamdodam/backend/auth/infrastructure/EmailVerificationCodeHasher.java) | raw 32바이트 전용 키와 ASCII 6자리 번호의 HMAC-SHA-256. 선행 0 보존 |
| [EmailVerificationRepository](../src/main/java/com/dodamdodam/backend/auth/infrastructure/EmailVerificationRepository.java) | 재발급의 번호·토큰 폐기, 호출자 트랜잭션, 범위 잠금과 잠금 후 DB 시각 사용 |
| [EmailVerificationIssuanceService](../src/main/java/com/dodamdodam/backend/auth/application/EmailVerificationIssuanceService.java) | 공개 목적의 eligible·비밀번호 재설정 계정 버전, PIN 목적의 계정·이메일·문맥·세대·버전 결합 |
| [EmailVerificationDeliveryService](../src/main/java/com/dodamdodam/backend/auth/application/EmailVerificationDeliveryService.java) | 실패한 challengeId만 폐기. 이후 새 발급이나 소비 완료 증명 보존 |
| [AuthRateLimitService](../src/main/java/com/dodamdodam/backend/auth/application/AuthRateLimitService.java) | 상위 IP 예산을 다시 예약하지 않으며, 뒤 단계 실패로 이미 커밋한 예산을 되돌리지 않음 |
| [V3 이메일 증명 스키마](../src/main/resources/db/migration/V3__create_email_verifications.sql) | 적용된 migration 수정 금지. 후속 검증·토큰 상태는 기존 컬럼과 계약을 먼저 확인 |

[SCRUM-90](https://dodamdodam.atlassian.net/browse/SCRUM-90)은 번호 검증·일회성 토큰 발급을 구현한다.
발급된 challenge의 현재 적격 여부·목적·만료·폐기·소비 상태와 필요한 세션 결합을 검증해야 한다.
번호 검증과 토큰 발급은 원자적으로 한 번만 허용하고, 실제 토큰 소비는 가입·비밀번호·PIN 변경과 같은
업무 트랜잭션에서 처리한다. 토큰 secret의 검증값은 해당 명세의 SHA-256이며 번호 HMAC과 구분한다.

PIN 변경 경로는 계정 → 보안 문맥 → PIN → challenge의 잠금 순서를 유지한다.
재발급·로그아웃·보호자 잠금·PIN 변경과의 경합, 만료 시각 equality, 동시 검증·소비를 후속 검증에 포함한다.
세부 계약은 [인증 API 명세](p0/api/02-auth.md)와 [통합 ERD](p0/Integrated_ERD_Design.md)를 따른다.

## 검증과 운영 확인

2026-11-17 실행한 `./gradlew clean check --no-daemon --console=plain --rerun-tasks`에서
50개 테스트 묶음의 1,131개 테스트가 통과했다. 실패·오류·건너뛰기는 모두 0개다.
네 목적·재발급·요청 제한·동시 잠금·SMTP 실패 보상·실제 HTTP·응답 전 권한 변경과 기존 계약을 포함한다.
에이전트 정책 검사, CI 계약 검사와 Compose 구성 검사도 통과했다.

실제 메일 공급자의 자격 증명·받은편지함 도착·스팸 처리, 운영 키 배포·교체와 실제 배포 부하는
검증하지 않았다. 테스트의 SMTP는 loopback 서버이며 테스트 키는 합성 fixture다.
배포 전 확인은 [개발 환경 문서](DEVELOPMENT_SETUP.md#이메일-발급과-전역-요청-제한-키)의
비밀 파일·SMTP·요청 제한 설정을 따른다. 실제 `.env`나 비밀 파일은 이 작업에서 읽거나 수정하지 않았다.
