# 이메일·Google 인증과 계정

[전체 API 목차](../Integrated_API_Spec.md) · **담당: Part 1** · 계약 v3.1 / 문서 분할 2026-10-06

절 번호는 기존 통합 명세의 번호를 유지합니다. 다른 절은 전체 목차에서 찾을 수 있습니다.

## 이 문서에서 찾기

- [4.1 IssueEmailVerificationRequest](#section-4-1)
- [4.2 VerifyEmailCodeRequest](#section-4-2)
- [4.3 SignupRequest](#section-4-3)
- [4.4 LoginRequest](#section-4-4)
- [4.5 PasswordResetRequest](#section-4-5)
- [6.1 CSRF 토큰 획득](#section-6-1)
- [6.2 목적별 인증번호 발급 및 재발급](#section-6-2)
- [6.3 인증번호 검증 및 토큰 발급](#section-6-3)
- [6.4 이메일 회원가입](#section-6-4)
- [6.5 이메일 로그인](#section-6-5)
- [6.6 현재 계정 및 진입 상태](#section-6-6)
- [6.7 로그아웃](#section-6-7)
- [6.8 비밀번호 재설정](#section-6-8)
- [6.17 Google 로그인 시작](#section-6-17)
- [6.18 Google 로그인 콜백](#section-6-18)

<a id="section-4-1"></a>
### 4.1 IssueEmailVerificationRequest

[IssueEmailVerificationRequest](schemas/auth.md#schema-issueemailverificationrequest) · `application/json`



분기 1:

| 필드 | 필수 | 타입·검증 |
| --- | --- | --- |
| purpose | 예 | type="string"; enum=["SIGNUP", "RESET_PASSWORD"]  |
| email | 예 | type="string"; 정규화 후 type="string"; format="email"; maxLength=254; pattern="^[\\x21-\\x7E]+(?![\\s\\S])" 전송 문자열. 서버가 trim+lower 후 x-normalized-schema의 이메일 형식/254자 제한을 검사한다. |

분기 2: PIN_SETUP/PIN_RESET은 유효 로그인 필수. 서버 현재 계정 이메일 사용; email 생략 가능, 명시적null 불가, 있으면 정규화 후 계정 이메일과 일치해야 한다.

| 필드 | 필수 | 타입·검증 |
| --- | --- | --- |
| purpose | 예 | type="string"; enum=["PIN_SETUP", "PIN_RESET"]  |
| email | 아니오 | type="string"; 정규화 후 type="string"; format="email"; maxLength=254; pattern="^[\\x21-\\x7E]+(?![\\s\\S])" 전송 문자열. 서버가 trim+lower 후 x-normalized-schema의 이메일 형식/254자 제한을 검사한다. |

signup 요청 예시:

<!-- json-example: IssueEmailVerificationRequest -->
```json
{
  "purpose": "SIGNUP",
  "email": "guardian@example.com"
}
```

reset 요청 예시:

<!-- json-example: IssueEmailVerificationRequest -->
```json
{
  "purpose": "RESET_PASSWORD",
  "email": "guardian@example.com"
}
```

pinSetup 요청 예시:

<!-- json-example: IssueEmailVerificationRequest -->
```json
{
  "purpose": "PIN_SETUP"
}
```

pinReset 요청 예시:

<!-- json-example: IssueEmailVerificationRequest -->
```json
{
  "purpose": "PIN_RESET"
}
```

<a id="section-4-2"></a>
### 4.2 VerifyEmailCodeRequest

[VerifyEmailCodeRequest](schemas/auth.md#schema-verifyemailcoderequest) · `application/json`



| 필드 | 필수 | 타입·검증 |
| --- | --- | --- |
| challengeId | 예 | type="string"; format="uuid"  |
| code | 예 | type="string"; minLength=6; maxLength=6; pattern="^[0-9]{6}(?![\\s\\S])" B-02.3 원문 입력 기준의 정확히6자리 ASCII 숫자 문자열, 선행0 보존. v3에서 유지한 입력 제약으로 유지. |

normal 요청 예시:

<!-- json-example: VerifyEmailCodeRequest -->
```json
{
  "challengeId": "88888888-8888-4888-8888-888888888888",
  "code": "012345"
}
```

<a id="section-4-3"></a>
### 4.3 SignupRequest

[SignupRequest](schemas/auth.md#schema-signuprequest) · `application/json`

P0 가입 요청은 SIGNUP verificationToken과 password 두 필드. 동의·추가 보호자 정보는 D15에 따라 P0 요청/저장에서 제외. 가입 후 별도 로그인.

| 필드 | 필수 | 타입·검증 |
| --- | --- | --- |
| verificationToken | 예 | type="string"; pattern="^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\\.[A-Za-z0-9_-]{43}(?![\\s\\S])" A-02.1 확정: challengeId(UUID) + 점 + CSPRNG 32바이트 secret의 패딩 없는 Base64url 43자. 서버는 secret 문자열의 UTF-8 바이트에 대한 SHA-256 검증값만 DB에 저장·상수 시간 비교하며 목적·만료·1회 소비를 검사. PIN_SETUP은 계정·세션·generation도 결합. 토큰 원문은 DB·로그에 저장하지 않음. |
| password | 예 | type="string"; minLength=15; maxLength=128 B-01.2 원문 입력 기준의 새 비밀번호15\~128 Unicode 코드포인트. trim/정규화/절단 없음, 최대128은 프로젝트 선택. 유효 Unicode만 허용하며 단독 surrogate 등은400 VALIDATION_FAILED·조용한 대체 없음. UTF-8 최대512바이트는128×4 계산 결과이며 별도 설정값이 아님. 실제 고정 라이브러리 버전 입력 호환성은 A-01.10 검증 항목. v3에서 유지한 입력 제약으로 유지. |

normal 요청 예시:

<!-- json-example: SignupRequest -->
```json
{
  "verificationToken": "88888888-8888-4888-8888-888888888888.AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
  "password": "Example-Password-Only!"
}
```

<a id="section-4-4"></a>
### 4.4 LoginRequest

[LoginRequest](schemas/auth.md#schema-loginrequest) · `application/json`

B-05.4 별도 rememberMe 미도입. 요청 필드/체크박스/자동 복원 없음, unknown field로 거부.

| 필드 | 필수 | 타입·검증 |
| --- | --- | --- |
| email | 예 | type="string"; 정규화 후 type="string"; format="email"; maxLength=254; pattern="^[\\x21-\\x7E]+(?![\\s\\S])" 전송 문자열. 서버가 trim+lower 후 x-normalized-schema의 이메일 형식/254자 제한을 검사한다. |
| password | 예 | type="string"; minLength=1 기존 비밀번호 문자열 검증, minLength=1 유지. trim/정규화/절단 금지, 새 생성15\~128자 규칙 소급 적용 없음. Part 1 JSON 본문16KiB(B-02.9 공유안)로 제한. 유효 Unicode 입력만 허용, 단독 surrogate 등은400 VALIDATION_FAILED로 거부·조용한 대체 없음.128/512 생성 상한 소급 없음, 실제 고정 라이브러리 버전 입력 호환성은 A-01.10 검증 항목. |

normal 요청 예시:

<!-- json-example: LoginRequest -->
```json
{
  "email": "guardian@example.com",
  "password": "Example-Password-Only!"
}
```

<a id="section-4-5"></a>
### 4.5 PasswordResetRequest

[PasswordResetRequest](schemas/auth.md#schema-passwordresetrequest) · `application/json`



| 필드 | 필수 | 타입·검증 |
| --- | --- | --- |
| verificationToken | 예 | type="string"; pattern="^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\\.[A-Za-z0-9_-]{43}(?![\\s\\S])" A-02.1 확정: challengeId(UUID) + 점 + CSPRNG 32바이트 secret의 패딩 없는 Base64url 43자. 서버는 secret 문자열의 UTF-8 바이트에 대한 SHA-256 검증값만 DB에 저장·상수 시간 비교하며 목적·만료·1회 소비를 검사. PIN_SETUP은 계정·세션·generation도 결합. 토큰 원문은 DB·로그에 저장하지 않음. |
| newPassword | 예 | type="string"; minLength=15; maxLength=128 B-01.2 원문 입력 기준의 새 비밀번호15\~128 Unicode 코드포인트. trim/정규화/절단 없음, 최대128은 프로젝트 선택. 유효 Unicode만 허용하며 단독 surrogate 등은400 VALIDATION_FAILED·조용한 대체 없음. UTF-8 최대512바이트는128×4 계산 결과이며 별도 설정값이 아님. 실제 고정 라이브러리 버전 입력 호환성은 A-01.10 검증 항목. v3에서 유지한 입력 제약으로 유지. |

normal 요청 예시:

<!-- json-example: PasswordResetRequest -->
```json
{
  "verificationToken": "88888888-8888-4888-8888-888888888888.AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
  "newPassword": "New-Example-Password!"
}
```

<a id="section-6-1"></a>
<a id="operation-getcsrf"></a>
### 6.1 CSRF 토큰 획득

`GET /api/v1/auth/csrf` · `getCsrf` · **담당 Part 1** · A-02 · 권한 공개

기존 CsrfController 형식 유지. data wrapper 예외. 최초 진입/로그인/로그아웃 뒤 재획득. 인증 쿠키가 아니라 XSRF-TOKEN 쿠키를 갱신할 수 있다.

요청 body: 없음.

| HTTP | 본문·오류 코드 |
| --- | --- |
| 200 | [CsrfTokenResponse](schemas/auth.md#schema-csrftokenresponse) · 성공.  |
| 429 | RATE_LIMITED · RATE_LIMITED; 발생 조건 및 재시도는 operation 설명과 기능 명세 참조. |

응답 추가 헤더: 429 `Retry-After`: 최소 재시도 대기 초. 실제 남은 차단시간으로 계산.

트랜잭션·재시도: 읽기 전용. 동일 요청 반복 가능.

DB 읽기: ``; 쓰기: ``. 공통 제한 예약과 프레임워크 세션 저장은 별도 공통 처리다.

200 응답 예시 (normal):

<!-- json-example: CsrfTokenResponse -->
```json
{
  "headerName": "X-XSRF-TOKEN",
  "parameterName": "_csrf",
  "token": "opaque-csrf-token"
}
```

<a id="section-6-2"></a>
<a id="operation-issueemailverification"></a>
### 6.2 목적별 인증번호 발급 및 재발급

`POST /api/v1/auth/email-verifications` · `issueEmailVerification` · **담당 Part 1** · A-01 · 권한 X; PIN_SETUP/PIN_RESET은 G

SIGNUP/RESET_PASSWORD는 email 필수. PIN_SETUP/PIN_RESET은 로그인 계정 이메일 사용. PIN_SETUP은 기존PIN이면409, PIN_RESET은 PIN없으면409. PIN_RESET은 계정/현재securityContext/setup_generation/pin_version_snapshot에 결합하며 PIN unlock 오답 차단 중에도 이메일 재인증 요청 자체는 별도 이메일·IP 제한을 적용해 허용한다. 공개 목적 계정 존재는 decoy+동일200로 숨긴다. 유효한 재발급은 동일범위 기존 미소비 번호/토큰 폐기. 메일 발송 실패·timeout·불명확은 해당 challenge 폐기 후503. 인증번호HMAC키교체는 기존권한폐기. 번호6자리·번호/토큰 각600초·재발급60초 및 요청제한 수치는 D17 운영보류인 기존기술기준을 재사용한다.

| 위치 | 필드 | 필수 | 타입·기본값·검증 |
| --- | --- | --- | --- |
| header | X-XSRF-TOKEN | 예 | type="string"; minLength=1 GET auth/csrf 응답 token. 누락/불일치는 403. |

요청 body: `application/json` [IssueEmailVerificationRequest](schemas/auth.md#schema-issueemailverificationrequest)

| HTTP | 본문·오류 코드 |
| --- | --- |
| 200 | [VerificationIssueResponse](schemas/auth.md#schema-verificationissueresponse) · 성공.  |
| 400 | VALIDATION_FAILED · VALIDATION_FAILED; 발생 조건 및 재시도는 operation 설명과 기능 명세 참조. |
| 403 | CSRF_INVALID, PIN_RESET_AUTHORIZATION_REQUIRED, PIN_SETUP_AUTHORIZATION_REQUIRED · CSRF_INVALID / PIN_SETUP_AUTHORIZATION_REQUIRED / PIN_RESET_AUTHORIZATION_REQUIRED |
| 413 | REQUEST_TOO_LARGE · REQUEST_TOO_LARGE; B-02.9 공유안의 모든 Part 1 JSON 본문 최대16384바이트(16KiB), 공백/escape 포함 UTF-8 실제 본문 기준.16384는 크기 검사 통과·16385부터413이며 CSRF/로그인 검사보다 먼저 판정. 기존 기술 기준안·D17 실운영값/검증 보류. |
| 415 | UNSUPPORTED_MEDIA_TYPE · UNSUPPORTED_MEDIA_TYPE; 발생 조건 및 재시도는 operation 설명과 기능 명세 참조. |
| 503 | AUTH_STATE_UNAVAILABLE, EMAIL_DELIVERY_UNAVAILABLE · AUTH_STATE_UNAVAILABLE / EMAIL_DELIVERY_UNAVAILABLE; A-05.3 채택으로 발송 실패/timeout/결과 불명확은 challenge 폐기 후 새 발급 복구. 발생 조건 및 재시도는 operation 설명과 기능 명세 참조. |
| 401 | AUTHENTICATION_REQUIRED · AUTHENTICATION_REQUIRED; 발생 조건 및 재시도는 operation 설명과 기능 명세 참조. |
| 409 | PIN_ALREADY_SET, PIN_NOT_SET · PIN_ALREADY_SET / PIN_NOT_SET |
| 429 | RATE_LIMITED · RATE_LIMITED; 발생 조건 및 재시도는 operation 설명과 기능 명세 참조. |

응답 추가 헤더: 429 `Retry-After`: 최소 재시도 대기 초. 실제 남은 차단시간으로 계산.

트랜잭션·재시도: 계정(로그인 PIN 목적)→session_security→guardian_pins→challenge 순서. 최초 발급도 email+purpose 또는 account+context+purpose advisory lock으로 직렬화. IP/목적별 요청량 사전 예약 후 업무 lock, 커밋 뒤 SMTP 호출(업무 DB lock 없음). 목적별 최신권한·PIN상태·기한 재검사.

DB 읽기: `accounts, guardian_pins, session_security`; 쓰기: `email_verifications, auth_rate_limits`. 공통 제한 예약과 프레임워크 세션 저장은 별도 공통 처리다.

200 응답 예시 (normal):

<!-- json-example: VerificationIssueResponse -->
```json
{
  "data": {
    "challengeId": "88888888-8888-4888-8888-888888888888",
    "expiresAt": "2026-09-30T10:10:00+09:00"
  }
}
```

<a id="section-6-3"></a>
<a id="operation-verifyemailcode"></a>
### 6.3 인증번호 검증 및 토큰 발급

`POST /api/v1/auth/email-verifications/verify` · `verifyEmailCode` · **담당 Part 1** · A-01 · 권한 X; PIN 목적은 G

purpose는 challenge에서 결정한다. 두 PIN 목적은 현재로그인/계정/문맥/generation을 확인하고 PIN_RESET은 현재pin_version과 발급snapshot도 일치해야 한다. PIN 목적의 실제 challenge 계정·문맥·폐기·generation/version 불일치는403이며 만료/소비보다 우선한다. 코드오답/형식상정상인잘못된토큰·만료/소비는400. 실제오답은 상태를커밋하며 기존기술기준5번째429. 성공 시번호재사용금지+token발급, 응답유실은재발급부터복구. 번호만료와token만료는독립이다.

| 위치 | 필드 | 필수 | 타입·기본값·검증 |
| --- | --- | --- | --- |
| header | X-XSRF-TOKEN | 예 | type="string"; minLength=1 GET auth/csrf 응답 token. 누락/불일치는 403. |

요청 body: `application/json` [VerifyEmailCodeRequest](schemas/auth.md#schema-verifyemailcoderequest)

| HTTP | 본문·오류 코드 |
| --- | --- |
| 200 | [VerificationGrantResponse](schemas/auth.md#schema-verificationgrantresponse) · 성공.  |
| 400 | VALIDATION_FAILED, VERIFICATION_INVALID · VALIDATION_FAILED / VERIFICATION_INVALID; 발생 조건 및 재시도는 operation 설명과 기능 명세 참조. |
| 403 | CSRF_INVALID, PIN_RESET_AUTHORIZATION_REQUIRED, PIN_SETUP_AUTHORIZATION_REQUIRED · CSRF_INVALID / PIN_SETUP_AUTHORIZATION_REQUIRED / PIN_RESET_AUTHORIZATION_REQUIRED |
| 413 | REQUEST_TOO_LARGE · REQUEST_TOO_LARGE; B-02.9 공유안의 모든 Part 1 JSON 본문 최대16384바이트(16KiB), 공백/escape 포함 UTF-8 실제 본문 기준.16384는 크기 검사 통과·16385부터413이며 CSRF/로그인 검사보다 먼저 판정. 기존 기술 기준안·D17 실운영값/검증 보류. |
| 415 | UNSUPPORTED_MEDIA_TYPE · UNSUPPORTED_MEDIA_TYPE; 발생 조건 및 재시도는 operation 설명과 기능 명세 참조. |
| 503 | AUTH_STATE_UNAVAILABLE · AUTH_STATE_UNAVAILABLE; 발생 조건 및 재시도는 operation 설명과 기능 명세 참조. |
| 401 | AUTHENTICATION_REQUIRED · AUTHENTICATION_REQUIRED; 발생 조건 및 재시도는 operation 설명과 기능 명세 참조. |
| 409 | PIN_ALREADY_SET, PIN_NOT_SET · PIN_ALREADY_SET / PIN_NOT_SET |
| 429 | RATE_LIMITED · RATE_LIMITED; 발생 조건 및 재시도는 operation 설명과 기능 명세 참조. |

응답 추가 헤더: 429 `Retry-After`: 최소 재시도 대기 초. 실제 남은 차단시간으로 계산.

트랜잭션·재시도: IP 예산은 challenge 조회 전 별도 예약. PIN 목적 accounts→session_security→guardian_pins→challenge 잠금 순서; 상태 최종 재검사 후 token해시/기한 원자 기록. 실제 오답·폐기 상태는400/429응답 때문에 rollback하지 않음.

DB 읽기: `accounts, session_security, guardian_pins`; 쓰기: `email_verifications, auth_rate_limits`. 공통 제한 예약과 프레임워크 세션 저장은 별도 공통 처리다.

200 응답 예시 (normal):

<!-- json-example: VerificationGrantResponse -->
```json
{
  "data": {
    "verificationToken": "88888888-8888-4888-8888-888888888888.AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
    "tokenExpiresAt": "2026-09-30T10:15:00+09:00"
  }
}
```

<a id="section-6-4"></a>
<a id="operation-signup"></a>
### 6.4 이메일 회원가입

`POST /api/v1/auth/signup` · `signup` · **담당 Part 1** · A-01 · 권한 X+V(SIGNUP)

SIGNUP 토큰에 결합된 이메일로만 계정을 생성한다. 요청은 verificationToken/password 두 필드이며 추가 동의·보호자 정보는 P0 제외(D15). 유효권한 소비와 계정생성을 한 Tx로 처리한다. email중복409, 가입201 후 자동로그인 없이 명시적로그인. 응답유실시로그인으로확인.

| 위치 | 필드 | 필수 | 타입·기본값·검증 |
| --- | --- | --- | --- |
| header | X-XSRF-TOKEN | 예 | type="string"; minLength=1 GET auth/csrf 응답 token. 누락/불일치는 403. |

요청 body: `application/json` [SignupRequest](schemas/auth.md#schema-signuprequest)

| HTTP | 본문·오류 코드 |
| --- | --- |
| 201 | [SignupResultResponse](schemas/auth.md#schema-signupresultresponse) · 성공.  |
| 400 | VALIDATION_FAILED, VERIFICATION_INVALID · VALIDATION_FAILED / VERIFICATION_INVALID; 발생 조건 및 재시도는 operation 설명과 기능 명세 참조. |
| 403 | CSRF_INVALID · CSRF_INVALID; 발생 조건 및 재시도는 operation 설명과 기능 명세 참조. |
| 413 | REQUEST_TOO_LARGE · REQUEST_TOO_LARGE; B-02.9 공유안의 모든 Part 1 JSON 본문 최대16384바이트(16KiB), 공백/escape 포함 UTF-8 실제 본문 기준.16384는 크기 검사 통과·16385부터413이며 CSRF/로그인 검사보다 먼저 판정. 기존 기술 기준안·D17 실운영값/검증 보류. |
| 415 | UNSUPPORTED_MEDIA_TYPE · UNSUPPORTED_MEDIA_TYPE; 발생 조건 및 재시도는 operation 설명과 기능 명세 참조. |
| 503 | AUTH_STATE_UNAVAILABLE · AUTH_STATE_UNAVAILABLE; 발생 조건 및 재시도는 operation 설명과 기능 명세 참조. |
| 409 | ACCOUNT_ALREADY_EXISTS · ACCOUNT_ALREADY_EXISTS; 발생 조건 및 재시도는 operation 설명과 기능 명세 참조. |
| 429 | RATE_LIMITED · RATE_LIMITED; 발생 조건 및 재시도는 operation 설명과 기능 명세 참조. |

응답 추가 헤더: 201 `Location`: 생성 리소스 식별 URI. 별도 GET 지원을 의미하지 않는다.; 429 `Retry-After`: 최소 재시도 대기 초. 실제 남은 차단시간으로 계산.

트랜잭션·재시도: accounts 생성과 challenge 검증/소비 원자 커밋; 일반업무실패시소비도rollback.

DB 읽기: `email_verifications`; 쓰기: `accounts, email_verifications, auth_rate_limits`. 공통 제한 예약과 프레임워크 세션 저장은 별도 공통 처리다.

201 응답 예시 (normal):

<!-- json-example: SignupResultResponse -->
```json
{
  "data": {
    "accountId": "11111111-1111-4111-8111-111111111111"
  }
}
```

<a id="section-6-5"></a>
<a id="operation-login"></a>
### 6.5 이메일 로그인

`POST /api/v1/auth/login` · `login` · **담당 Part 1** · A-02 · 권한 X

없는 email/틀린 password/소셜 전용은 동일 401 LOGIN_FAILED. session fixation 방어로 세션 교체, PIN 확인 복사 없음. reset과 경쟁 시 account session_version 및 password hash 재검사. 새 CSRF 획득 필요. B-05.4 별도 rememberMe 필드/체크박스/자동 복원 미도입. 비밀번호 로그인은 정규화 email10회/IP100회 각900초 고정창(B-02.7 공유안), 처리 예약된 성공/실패 포함·한도번째 처리/다음부터429. 기존 키·예약 단계 유지, Google OAuth는 별도 API_IP 정책. 비존재 계정에도 dummy password hash 비교를 수행하며 요청량에 포함한다.

| 위치 | 필드 | 필수 | 타입·기본값·검증 |
| --- | --- | --- | --- |
| header | X-XSRF-TOKEN | 예 | type="string"; minLength=1 GET auth/csrf 응답 token. 누락/불일치는 403. |

요청 body: `application/json` [LoginRequest](schemas/auth.md#schema-loginrequest)

| HTTP | 본문·오류 코드 |
| --- | --- |
| 200 | [AccountViewResponse](schemas/auth.md#schema-accountviewresponse) · 성공.  |
| 400 | VALIDATION_FAILED · VALIDATION_FAILED; 발생 조건 및 재시도는 operation 설명과 기능 명세 참조. |
| 403 | CSRF_INVALID · CSRF_INVALID; 발생 조건 및 재시도는 operation 설명과 기능 명세 참조. |
| 413 | REQUEST_TOO_LARGE · REQUEST_TOO_LARGE; B-02.9 공유안의 모든 Part 1 JSON 본문 최대16384바이트(16KiB), 공백/escape 포함 UTF-8 실제 본문 기준.16384는 크기 검사 통과·16385부터413이며 CSRF/로그인 검사보다 먼저 판정. 기존 기술 기준안·D17 실운영값/검증 보류. |
| 415 | UNSUPPORTED_MEDIA_TYPE · UNSUPPORTED_MEDIA_TYPE; 발생 조건 및 재시도는 operation 설명과 기능 명세 참조. |
| 503 | AUTH_STATE_UNAVAILABLE · AUTH_STATE_UNAVAILABLE; 발생 조건 및 재시도는 operation 설명과 기능 명세 참조. |
| 401 | LOGIN_FAILED · LOGIN_FAILED; 발생 조건 및 재시도는 operation 설명과 기능 명세 참조. |
| 429 | RATE_LIMITED · RATE_LIMITED; 발생 조건 및 재시도는 operation 설명과 기능 명세 참조. |

응답 추가 헤더: 200 `Set-Cookie`: B-05.4 인증된 일반 로그인 JSESSIONID 지속 쿠키365일(Max-Age=31536000), 유효 사용 확인 후 응답 재발급/갱신. cookieMaxAge 설정만으로 갱신되지 않음. HttpOnly; 운영 Secure; Path=/; SameSite는 배포 구성에 맞춤. 로그 금지.; 429 `Retry-After`: 최소 재시도 대기 초. 실제 남은 차단시간으로 계산.

트랜잭션·재시도: 현재 자격 재검사+새 session_security 생성. B-05.1 Spring Session JDBC 저장은 기본 REQUIRES_NEW로 업무 트랜잭션과 자동 원자적이지 않음. 세션 저장 실패503·성공 응답 금지 및 버전/폐기 판정 유지. 이전 session_security 폐기 후 새 문맥 등록. framework 세션 저장까지 성공한 뒤200; DB만 성공하고 저장 실패 시 새 보조 문맥 폐기 후503. 이전 권한 복구 금지.

DB 읽기: `accounts, children, guardian_pins`; 쓰기: `session_security, email_verifications, auth_rate_limits`. 공통 제한 예약과 프레임워크 세션 저장은 별도 공통 처리다.

200 응답 예시 (normal):

<!-- json-example: AccountViewResponse -->
```json
{
  "data": {
    "accountId": "11111111-1111-4111-8111-111111111111",
    "email": "guardian@example.com",
    "childId": null,
    "hasPin": false,
    "guardianUnlockedUntil": null
  }
}
```

<a id="section-6-6"></a>
<a id="operation-getcurrentaccount"></a>
### 6.6 현재 계정 및 진입 상태

`GET /api/v1/auth/me` · `getCurrentAccount` · **담당 Part 1** · A-02 · 권한 G

로그인 없음/만료는 401. 유효하지 않은 guardianUnlockedUntil은 null. 5xx를 로그아웃으로 간주하지 않는다. 각 업무 API는 별도 권한 재검사.

요청 body: 없음.

| HTTP | 본문·오류 코드 |
| --- | --- |
| 200 | [AccountViewResponse](schemas/auth.md#schema-accountviewresponse) · 성공.  |
| 401 | AUTHENTICATION_REQUIRED · AUTHENTICATION_REQUIRED; 발생 조건 및 재시도는 operation 설명과 기능 명세 참조. |
| 503 | AUTH_STATE_UNAVAILABLE · AUTH_STATE_UNAVAILABLE; 발생 조건 및 재시도는 operation 설명과 기능 명세 참조. |
| 429 | RATE_LIMITED · RATE_LIMITED; 발생 조건 및 재시도는 operation 설명과 기능 명세 참조. |

응답 추가 헤더: 429 `Retry-After`: 최소 재시도 대기 초. 실제 남은 차단시간으로 계산.

트랜잭션·재시도: 읽기 전용. 동일 요청 반복 가능.

DB 읽기: `accounts, children, guardian_pins, session_security`; 쓰기: ``. 공통 제한 예약과 프레임워크 세션 저장은 별도 공통 처리다.

200 응답 예시 (beforeChild):

<!-- json-example: AccountViewResponse -->
```json
{
  "data": {
    "accountId": "11111111-1111-4111-8111-111111111111",
    "email": "guardian@example.com",
    "childId": null,
    "hasPin": false,
    "guardianUnlockedUntil": null
  }
}
```

나머지 4개 상태 예시는 OpenAPI의 이 operation에 함께 제공한다.

<a id="section-6-7"></a>
<a id="operation-logout"></a>
### 6.7 로그아웃

`POST /api/v1/auth/logout` · `logout` · **담당 Part 1** · A-02 · 권한 G+X

B-06.2 Part 1 공유안 채택(2026-10-01 KST). POST /api/v1/auth/logout만 진입 경로로 사용하고 기본 /logout 별도 매핑 제거. 기존 CSRF/204·현재 session_security/보호자 확인/PIN_SETUP/PIN_RESET 폐기 뒤 프레임워크 session/쿠키/CSRF 삭제를 유지. 이미 비로그인이면401, DB 무효화 실패는503. 성공 후 CSRF 재획득. 기존 프론트 호출 확인·경로 전환은 J-08 후속, 실제 설정 변경 없음.

| 위치 | 필드 | 필수 | 타입·기본값·검증 |
| --- | --- | --- | --- |
| header | X-XSRF-TOKEN | 예 | type="string"; minLength=1 GET auth/csrf 응답 token. 누락/불일치는 403. |

요청 body: 없음. POST는 실제0바이트.

| HTTP | 본문·오류 코드 |
| --- | --- |
| 204 | 본문 없음 · 성공. 본문 없음. |
| 401 | AUTHENTICATION_REQUIRED · AUTHENTICATION_REQUIRED; 발생 조건 및 재시도는 operation 설명과 기능 명세 참조. |
| 400 | VALIDATION_FAILED · VALIDATION_FAILED; 발생 조건 및 재시도는 operation 설명과 기능 명세 참조. |
| 403 | CSRF_INVALID · CSRF_INVALID; 발생 조건 및 재시도는 operation 설명과 기능 명세 참조. |
| 413 | REQUEST_TOO_LARGE · REQUEST_TOO_LARGE; B-02.9 공유안의 모든 Part 1 JSON 본문 최대16384바이트(16KiB), 공백/escape 포함 UTF-8 실제 본문 기준.16384는 크기 검사 통과·16385부터413이며 CSRF/로그인 검사보다 먼저 판정. 기존 기술 기준안·D17 실운영값/검증 보류. |
| 415 | UNSUPPORTED_MEDIA_TYPE · UNSUPPORTED_MEDIA_TYPE; 발생 조건 및 재시도는 operation 설명과 기능 명세 참조. |
| 503 | AUTH_STATE_UNAVAILABLE · AUTH_STATE_UNAVAILABLE; 발생 조건 및 재시도는 operation 설명과 기능 명세 참조. |
| 429 | RATE_LIMITED · RATE_LIMITED; 발생 조건 및 재시도는 operation 설명과 기능 명세 참조. |

응답 추가 헤더: 204 `Set-Cookie`: 세션 및 CSRF 쿠키를 기존 Path/Domain과 맞춰 만료 처리.; 429 `Retry-After`: 최소 재시도 대기 초. 실제 남은 차단시간으로 계산.

트랜잭션·재시도: 현재 보안 문맥/권한 폐기 커밋. 프레임워크 물리 session 삭제는 이어서 처리.

DB 읽기: `accounts`; 쓰기: `session_security, email_verifications`. 공통 제한 예약과 프레임워크 세션 저장은 별도 공통 처리다.

<a id="section-6-8"></a>
<a id="operation-resetpassword"></a>
### 6.8 비밀번호 재설정

`POST /api/v1/auth/password-resets` · `resetPassword` · **담당 Part 1** · A-03 · 권한 X+V(RESET_PASSWORD)

RESET_PASSWORD 토큰만. 이전 version 토큰도 거부. 새 해시·token 소비·account session_version 증가·모든 이전 로그인/PIN_SETUP/PIN_RESET 무효화. 204는 재로그인 필요, 자동 로그인 없음. Google-only는 유효 RESET_PASSWORD 토큰 검증 후409 PASSWORD_RESET_NOT_AVAILABLE·Google 안내, 일반 업무 실패 롤백으로 토큰 미소비(B-03.3 공유안·v3에서 유지한 입력·흐름 계약). 새 password 수단·identity 연결·로그인 권한 생성 없음, 접근 상실 복구 별도. 토큰 재사용 400. 응답 유실 시 새 password 로그인으로 확인.

| 위치 | 필드 | 필수 | 타입·기본값·검증 |
| --- | --- | --- | --- |
| header | X-XSRF-TOKEN | 예 | type="string"; minLength=1 GET auth/csrf 응답 token. 누락/불일치는 403. |

요청 body: `application/json` [PasswordResetRequest](schemas/auth.md#schema-passwordresetrequest)

| HTTP | 본문·오류 코드 |
| --- | --- |
| 204 | 본문 없음 · 성공. 본문 없음. |
| 400 | VALIDATION_FAILED, VERIFICATION_INVALID · VALIDATION_FAILED / VERIFICATION_INVALID; 발생 조건 및 재시도는 operation 설명과 기능 명세 참조. |
| 403 | CSRF_INVALID · CSRF_INVALID; 발생 조건 및 재시도는 operation 설명과 기능 명세 참조. |
| 413 | REQUEST_TOO_LARGE · REQUEST_TOO_LARGE; B-02.9 공유안의 모든 Part 1 JSON 본문 최대16384바이트(16KiB), 공백/escape 포함 UTF-8 실제 본문 기준.16384는 크기 검사 통과·16385부터413이며 CSRF/로그인 검사보다 먼저 판정. 기존 기술 기준안·D17 실운영값/검증 보류. |
| 415 | UNSUPPORTED_MEDIA_TYPE · UNSUPPORTED_MEDIA_TYPE; 발생 조건 및 재시도는 operation 설명과 기능 명세 참조. |
| 503 | AUTH_STATE_UNAVAILABLE · AUTH_STATE_UNAVAILABLE; 발생 조건 및 재시도는 operation 설명과 기능 명세 참조. |
| 409 | PASSWORD_RESET_NOT_AVAILABLE · PASSWORD_RESET_NOT_AVAILABLE; 발생 조건 및 재시도는 operation 설명과 기능 명세 참조. |
| 429 | RATE_LIMITED · RATE_LIMITED; 발생 조건 및 재시도는 operation 설명과 기능 명세 참조. |

응답 추가 헤더: 429 `Retry-After`: 최소 재시도 대기 초. 실제 남은 차단시간으로 계산.

트랜잭션·재시도: 계정+challenge lock; 해시/버전/소비/다른 reset 및 PIN_SETUP/PIN_RESET 폐기 단일 트랜잭션. 물리 세션 삭제 지연은 version 검사로 차단. Google-only409는 일반 업무 실패 롤백으로 토큰 미소비·기존 만료 유지(B-03.3 공유안).

DB 읽기: `accounts, email_verifications`; 쓰기: `accounts, email_verifications, auth_rate_limits`. 공통 제한 예약과 프레임워크 세션 저장은 별도 공통 처리다.

<a id="section-6-17"></a>
<a id="operation-startgooglelogin"></a>
### 6.17 Google 로그인 시작

`GET /oauth2/authorization/google` · `startGoogleLogin` · **담당 Part 1** · A-02 · 권한 공개·state 저장

Google OAuth/OIDC 로그인 시작. 서버state 저장이 성공한 경우에만302. 저장 불가/정상설정없음503 AUTH_STATE_UNAVAILABLE, 요청량초과429. state/nonce 검증과허용redirect설정을사용한다.

요청 body: 없음.

| HTTP | 본문·오류 코드 |
| --- | --- |
| 302 | 본문 없음 · OAuth 브라우저 리다이렉트. JSON envelope 예외. callback은 성공 또는 실패 고정 허용 URL. |
| 429 | RATE_LIMITED · RATE_LIMITED; 발생 조건 및 재시도는 operation 설명과 기능 명세 참조. |
| 503 | AUTH_STATE_UNAVAILABLE · AUTH_STATE_UNAVAILABLE |

응답 추가 헤더: 302 `Location`: 서버 allowlist URL만 사용. 외부 임의 redirect 금지.; 302 `Set-Cookie`: 필요 시 세션 교체/생성, 운영 HttpOnly·Secure. 비밀값 로그 금지.; 429 `Retry-After`: 최소 재시도 대기 초. 실제 남은 차단시간으로 계산.

트랜잭션·재시도: 프레임워크 임시 OAuth state/nonce 저장. 업무 테이블 쓰기 없음.

DB 읽기: ``; 쓰기: ``. 공통 제한 예약과 프레임워크 세션 저장은 별도 공통 처리다.

<a id="section-6-18"></a>
<a id="operation-completegooglelogin"></a>
### 6.18 Google 로그인 콜백

`GET /login/oauth2/code/google` · `completeGoogleLogin` · **담당 Part 1** · A-02 · 권한 OAuth state/nonce

Google 서명·iss·aud·exp·state·nonce 및 canonical issuer를검증한다. 기존(GOOGLE,issuer,subject)identity 우선,이메일자동연결/변경금지. 신규는검증된email이필수이며동의·추가보호자정보수집은P0제외. 동일identity생성경쟁만승자재조회하고다른identity동일email은실패화면으로안내한다. 성공로그인세션까지저장한후고정성공URL302. 실패는허용된고정실패URL로302. 사용자취소OAUTH_CANCELLED,검증이메일없음OAUTH_PROFILE_INCOMPLETE,다른identity의동일email은ACCOUNT_LINK_REQUIRED,그외인증/세션저장장애OAUTH_LOGIN_FAILED;URL에비밀값·내부원인금지. allowlist설정자체없음503. 별도미가입정보보완화면없음.

| 위치 | 필드 | 필수 | 타입·기본값·검증 |
| --- | --- | --- | --- |
| query | code | 아니오 | type="string" 성공 콜백에 필수인 authorization code, 로그 금지. |
| query | state | 아니오 | type="string" 프레임워크가 저장한 값과 비교. 성공 콜백 필수. |
| query | error | 아니오 | type="string" 사용자 취소/공급자 오류. 외부 값 그대로 프론트에 전달 금지. |

요청 body: 없음.

| HTTP | 본문·오류 코드 |
| --- | --- |
| 302 | 본문 없음 · OAuth 브라우저 리다이렉트. JSON envelope 예외. callback은 성공 또는 실패 고정 허용 URL. |
| 429 | RATE_LIMITED · RATE_LIMITED; 발생 조건 및 재시도는 operation 설명과 기능 명세 참조. |
| 503 | AUTH_STATE_UNAVAILABLE · AUTH_STATE_UNAVAILABLE |

응답 추가 헤더: 302 `Location`: 서버 allowlist URL만 사용. 외부 임의 redirect 금지.; 302 `Set-Cookie`: 필요 시 세션 교체/생성, 운영 HttpOnly·Secure. 비밀값 로그 금지.; 429 `Retry-After`: 최소 재시도 대기 초. 실제 남은 차단시간으로 계산.

트랜잭션·재시도: accounts+auth_identities 생성 원자성, session_security와framework저장은별도검증. 세션저장실패시새문맥폐기·이전폐기권한복원금지. 성공세션미발급으로실패redirect.

DB 읽기: `accounts, auth_identities`; 쓰기: `accounts, auth_identities, session_security, email_verifications`. 공통 제한 예약과 프레임워크 세션 저장은 별도 공통 처리다.

허용 redirect reason: OAUTH_CANCELLED, OAUTH_LOGIN_FAILED, OAUTH_PROFILE_INCOMPLETE, ACCOUNT_LINK_REQUIRED. 외부 error 원문을 복사하지 않는다. 성공: code와 state. 오류: error와 검증 가능한 state. 누락/불일치 시 성공 금지, 일반화 실패 redirect. 공급자 error는 로그/URL에 그대로 복사하지 않는다.
