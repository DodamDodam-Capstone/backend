# 이메일·비밀번호 로그인 구현

[SCRUM-78](https://dodamdodam.atlassian.net/browse/SCRUM-78)의 구현 설명이다.
HTTP 계약은 [인증 API §6.5](p0/api/02-auth.md#operation-login)와
[LoginRequest](p0/api/schemas/auth.md#schema-loginrequest)를 따른다.

## 요청과 응답

**POST /api/v1/auth/login**은 공개 경로지만 CSRF 검증을 수행한다.
먼저 GET /api/v1/auth/csrf에서 받은 쿠키와 응답 token을 함께 사용한다.

~~~http
POST /api/v1/auth/login
Content-Type: application/json
X-XSRF-TOKEN: <발급받은 token>
~~~

~~~json
{
  "email": "guardian@example.com",
  "password": "synthetic-example-password"
}
~~~

예시는 합성 값이다. email, password만 받으며 rememberMe 등 추가 필드는 거부한다.
실제 비밀번호·세션 쿠키를 문서·이슈·로그에 복사하지 않는다.

성공은 HTTP 200과 [AccountView](CURRENT_ACCOUNT.md)의 5개 필드다.
아이·PIN 미설정도 정상 로그인이며, PIN이 있어도 로그인만으로 보호자 확인을 해제하지 않는다.

~~~json
{
  "data": {
    "accountId": "11111111-1111-4111-8111-111111111111",
    "email": "guardian@example.com",
    "childId": null,
    "hasPin": false,
    "guardianUnlockedUntil": null
  }
}
~~~

로그인 후에는 새 JSESSIONID를 사용한다. 기존 CSRF 토큰은 폐기되므로 다음 쓰기 요청 전에
GET /api/v1/auth/csrf로 다시 발급받는다. 브라우저는 credentials: include가 필요하다.

## 입력과 비밀번호 포맷

- 이메일은 양끝 Unicode 공백 제거와 Locale.ROOT 소문자 변환 뒤 유효 ASCII·최대254자를 검사한다.
  Gmail 점·+suffix를 지우지 않으며 이메일 발급도 같은 EmailAddress 정책을 사용한다.
- 로그인 비밀번호는 비어 있지 않은 유효 Unicode 문자열을 그대로 비교한다.
  공백·Unicode 정규화·길이 절단을 하지 않으며, 신규 비밀번호의 15~128자 규칙을 소급 적용하지 않는다.
- 전체 JSON은 실제 UTF-8 16,384바이트 제한을 따른다. 중복 필드·후행 JSON·잘못된 UTF-8·단독 surrogate는 400이다.
  표준 UTF-8 decoder를 REPORT로 적용하며 overlong·CESU-8 및 UTF-16/32 자동 감지를 허용하지 않는다.
- Controller는 비밀번호 원문을 프레임워크 요청 인자 로그에 전달하지 않는다.
  입력·자격·공개 계정 DTO의 로그용 문자열도 개인정보를 숨긴다.

공통 PasswordEncoder는 **{pbkdf2-sha256-v1}** 식별자의 PBKDF2-HMAC-SHA256을 사용한다.
salt는16바이트, 반복은600,000회, 결과는256비트이며 새 의존성을 추가하지 않는다.
현재 저장 포맷은 식별자와 소문자 hex96자다. 알 수 없는 포맷·손상된 해시는 503으로 처리하며
평문·다른 해시로 자동 해석하지 않는다. 기존 데이터의 해시 변환이나 재해시는 이번 작업에서 수행하지 않는다.

구현 API는 [Spring Security Password Storage](https://docs.spring.io/spring-security/reference/features/authentication/password-storage.html)를,
선택한 PBKDF2 비용은 [OWASP Password Storage](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html)의
PBKDF2-HMAC-SHA256 기준을 참고했다. 운영 처리량·지연 측정과 비용 조정은 별도 검증 사항이다.

SCRUM-91 가입·SCRUM-79 비밀번호 재설정도 이 PasswordEncoder bean을 사용해야 한다.
검증된 원문을 encode에 전달하고 반환 문자열 전체를 accounts.password_hash에 저장한다.
encoder 식별자·포맷을 바꿀 때는 생성과 검증 호환성을 함께 변경해야 한다.

## 처리 순서와 경합

1. 기존 글로벌 API_IP 예산 예약 → 전송 크기/형식 → CSRF → JSON·입력 검증을 수행한다.
2. 로그인 이메일·IP 예산을 같은 짧은 독립 트랜잭션에서 예약한다.
3. 계정 자격을 읽어 실제 비밀번호 해시를 비교한다. 없는 계정·소셜 전용도 동일 encoder의 dummy 해시를 비교한다.
   이는 계정 열거 방어이며 정시간 실행을 보장하지 않는다.
4. 새 계정과 이전 계정을 UUID 순으로 잠근 뒤 비교했던 해시와 session_version을 다시 확인한다.
   reset·자격 변경이 먼저 커밋됐으면 401로 거부하고 로그인 로직이 이전 문맥을 교체하지 않는다.
5. 기존 문맥과 미소비 PIN_SETUP/PIN_RESET 증명을 폐기하고 새 session_security를 등록한다.
6. 기존 세션 고정 방어로 native 세션을 교체하고 새 인증 문맥을 저장한다.
   실제 저장과 현재 유효성을 확인한 뒤에만 AccountView를 반환한다.

해시 비교 중에는 업무 DB 잠금을 유지하지 않는다. 업무 문맥 변경과 Spring Session 저장을
하나의 원자적 트랜잭션으로 가정하지 않으며, native 저장 실패 시 새 문맥을 보상 폐기한다.
이미 폐기한 이전 권한은 복원하지 않는다. 응답 직전 재검사 실패도 성공 본문·로그인 쿠키를 제거하고 503으로 반환한다.

일반 로그인은 업무 기한 NULL, native 음수 수명과365일 지속 쿠키를 유지한다.
이전 guardian 기한·PIN 버전·setup generation을 새 로그인으로 복사하지 않는다.

## 시도 제한과 오류

| 범위 | 기준 |
| --- | --- |
| 전역 API_IP | 기존300회/60초, 다른 업무 예약과 독립 |
| LOGIN_EMAIL | 정규화 이메일10회/900초 |
| LOGIN_IP | 실제 연결 IP100회/900초 |

로그인 두 예산은 성공·실패를 모두 포함한다. 한도번째는 처리하고 다음 요청부터429다.
한 키가 거부되면 그 로그인 예약 단계 전체를 되돌리며 앞선 글로벌 예산은 유지한다.
새 세션·성공 로그인·업무 rollback으로 이미 예약한 횟수를 초기화하지 않는다.
최초 요청 기준 고정창이며 반복 요청으로 창을 연장하지 않는다.
IP는 기존 실제 연결 주소 판정을 사용한다. 임의 Forwarded·X-Forwarded-For를 신뢰하지 않는다.

| HTTP / code | 의미 |
| --- | --- |
| 400 VALIDATION_FAILED | 구조·타입·이메일·빈 비밀번호·Unicode 오류. 필드명과 고정 사유만 반환 |
| 401 LOGIN_FAILED | 없는 계정·오답·소셜 전용 또는 세션 생성 전 자격 변경 |
| 403 CSRF_INVALID | CSRF 쿠키·헤더 없음/불일치 |
| 413 REQUEST_TOO_LARGE | 실제 UTF-8 본문16KiB 초과 |
| 415 UNSUPPORTED_MEDIA_TYPE | application/json·UTF-8 이외 또는 Content-Type 없음 |
| 429 RATE_LIMITED | 적용 예산 소진. Retry-After는 실제 남은 제한을 올림한 초, 최소1 |
| 503 AUTH_STATE_UNAVAILABLE | 자격/상태 저장소·해시 검증·세션 저장/최종 확인 실패 |

모든 응답은 X-Request-ID와 Cache-Control: no-store를 유지한다.
LOGIN_FAILED는 제출한 자격의 실패다. 잘못된 비밀번호만으로 이전의 유효한 로그인 세션을 폐기하지 않는다.

## Postman 확인 순서

1. PostgreSQL과 기존 요청 제한 키 설정을 준비한다. 설정은 [개발 환경 문서](DEVELOPMENT_SETUP.md)를 따른다.
2. GET /api/v1/auth/csrf를 실행하고 응답 token을 변수에 보관한다. Postman 쿠키 저장을 유지한다.
3. 위 POST를 JSON으로 보내고 X-XSRF-TOKEN에 token을 넣는다.
4. 성공 후 GET /api/v1/auth/me로 새 로그인 세션과 AccountView를 확인한다.
5. 다음 쓰기 요청 전 CSRF를 재발급받는다. 틀린 비밀번호401과 한도 초과429도 각각 확인한다.

성공 검증에는 위 해시 포맷으로 생성된 이메일 계정이 필요하다. 가입 API는 SCRUM-91 범위다.
이메일 로그인에는 SMTP 발송을 사용하지 않는다. 자동화 검증은 합성 계정·PostgreSQL·실제 JDBC 세션/쿠키를 사용하며,
운영 Controller에 테스트용 로그인 우회 경로는 추가하지 않는다.

## 코드와 검증

- [PasswordLoginController](../src/main/java/com/dodamdodam/backend/auth/api/PasswordLoginController.java): 엄격 JSON 파싱·입력과 HTTP 연결.
- [PasswordLoginService](../src/main/java/com/dodamdodam/backend/auth/application/PasswordLoginService.java): 예산·실제/dummy 비교·세션 저장·계정 응답.
- [PasswordLoginRepository](../src/main/java/com/dodamdodam/backend/auth/infrastructure/PasswordLoginRepository.java): 최소 자격 조회.
- [SessionSecurityRepository](../src/main/java/com/dodamdodam/backend/auth/session/SessionSecurityRepository.java): 잠금 내 자격 재검사·문맥 교체.
- [PasswordConfig](../src/main/java/com/dodamdodam/backend/auth/config/PasswordConfig.java): 버전 해시 포맷과 KDF 설정.

~~~sh
./gradlew test --tests '*LoginRequestTests' --tests '*PasswordConfigTests' --tests '*PasswordLogin*Tests' --no-daemon --console=plain
./gradlew clean check --no-daemon --console=plain --rerun-tasks
~~~

실제 HTTP로 CSRF·전송 경계·Unicode 원문·요청량·세션 교체·저장 실패 보상·응답 직전 무효화와
DEBUG/직렬화 자격 비노출을 확인한다. 기존 OAuth·쿠키 수명·이메일 발급 계약도 함께 회귀 검사한다.
