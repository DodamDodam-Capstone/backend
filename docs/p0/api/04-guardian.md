# 보호자 PIN과 권한

[전체 API 목차](../Integrated_API_Spec.md) · **담당: Part 1** · 계약 v3.1 / 문서 분할 2026-10-06

절 번호는 기존 통합 명세의 번호를 유지합니다. 다른 절은 전체 목차에서 찾을 수 있습니다.

## 이 문서에서 찾기

- [4.7 GuardianPinSetupRequest](#section-4-7)
- [4.8 GuardianUnlockRequest](#section-4-8)
- [4.9 GuardianPinResetRequest](#section-4-9)
- [6.11 최초 보호자 PIN 설정](#section-6-11)
- [6.12 보호자 PIN 확인](#section-6-12)
- [6.13 보호자 잠금 및 아이 모드 전환](#section-6-13)
- [6.14 이메일 재인증으로 보호자 PIN 재설정](#section-6-14)

<a id="section-4-7"></a>
### 4.7 GuardianPinSetupRequest

[GuardianPinSetupRequest](schemas/guardian.md#schema-guardianpinsetuprequest) · `application/json`

verificationToken은 업무적으로 필수. 누락을 403 PIN_SETUP_AUTHORIZATION_REQUIRED로 표현하려고 구조 required에서는 제외. 명시적 null은 400.

| 필드 | 필수 | 타입·검증 |
| --- | --- | --- |
| pin | 예 | type="string"; pattern="^[0-9]{4}(?![\\s\\S])" 정확히 ASCII 숫자 4자리 문자열. 0000 포함 앞자리 0 보존. |
| verificationToken | 아니오 | type="string"; pattern="^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\\.[A-Za-z0-9_-]{43}(?![\\s\\S])" A-02.1 확정: challengeId(UUID) + 점 + CSPRNG 32바이트 secret의 패딩 없는 Base64url 43자. 서버는 secret 문자열의 UTF-8 바이트에 대한 SHA-256 검증값만 DB에 저장·상수 시간 비교하며 목적·만료·1회 소비를 검사. PIN_SETUP은 계정·세션·generation도 결합. 토큰 원문은 DB·로그에 저장하지 않음. |

normal 요청 예시:

<!-- json-example: GuardianPinSetupRequest -->
```json
{
  "pin": "0123",
  "verificationToken": "88888888-8888-4888-8888-888888888888.AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
}
```

<a id="section-4-8"></a>
### 4.8 GuardianUnlockRequest

[GuardianUnlockRequest](schemas/guardian.md#schema-guardianunlockrequest) · `application/json`



| 필드 | 필수 | 타입·검증 |
| --- | --- | --- |
| pin | 예 | type="string"; pattern="^[0-9]{4}(?![\\s\\S])" 정확히 ASCII 숫자 4자리 문자열. 0000 포함 앞자리 0 보존. |

normal 요청 예시:

<!-- json-example: GuardianUnlockRequest -->
```json
{
  "pin": "0123"
}
```

<a id="section-4-9"></a>
### 4.9 GuardianPinResetRequest

[GuardianPinResetRequest](schemas/guardian.md#schema-guardianpinresetrequest) · `application/json`

D16 이메일 PIN_RESET 재인증의 일회성 verificationToken과 새PIN. 유효 로그인·동일계정/보안문맥/generation/발급시PIN버전 필수. PIN_SETUP 토큰 재사용 불가.

| 필드 | 필수 | 타입·검증 |
| --- | --- | --- |
| newPin | 예 | type="string"; pattern="^[0-9]{4}(?![\\s\\S])" 정확히 ASCII 숫자 4자리 문자열. 0000 포함 앞자리 0 보존. |
| verificationToken | 예 | type="string"; pattern="^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\\.[A-Za-z0-9_-]{43}(?![\\s\\S])" A-02.1 확정: challengeId(UUID) + 점 + CSPRNG 32바이트 secret의 패딩 없는 Base64url 43자. 서버는 secret 문자열의 UTF-8 바이트에 대한 SHA-256 검증값만 DB에 저장·상수 시간 비교하며 목적·만료·1회 소비를 검사. PIN_SETUP은 계정·세션·generation도 결합. 토큰 원문은 DB·로그에 저장하지 않음. |

normal 요청 예시:

<!-- json-example: GuardianPinResetRequest -->
```json
{
  "newPin": "0123",
  "verificationToken": "88888888-8888-4888-8888-888888888888.AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
}
```

<a id="section-6-11"></a>
<a id="operation-setupguardianpin"></a>
### 6.11 최초 보호자 PIN 설정

`POST /api/v1/guardian/pin` · `setupGuardianPin` · **담당 Part 1** · A-05 · 권한 G+X+V(PIN_SETUP)

로그인+PIN 미설정+현재 계정/세션 PIN_SETUP. token 누락은 403, 명시적 null은 400. 기존 PIN 먼저 검사하여 409. 이후 잘못된 목적/해시/만료/소비는 400, PIN_SETUP 계정/세션 불일치·폐기는 403. 설정 뒤 자동 unlock 없음. 기본 권한→구조 검증→기존 PIN→token 판정. 실제 PIN_SETUP의 계정/세션/폐기 검사가 만료/소비보다 우선하여403을 먼저 반환한다.

| 위치 | 필드 | 필수 | 타입·기본값·검증 |
| --- | --- | --- | --- |
| header | X-XSRF-TOKEN | 예 | type="string"; minLength=1 GET auth/csrf 응답 token. 누락/불일치는 403. |

요청 body: `application/json` [GuardianPinSetupRequest](schemas/guardian.md#schema-guardianpinsetuprequest)

| HTTP | 본문·오류 코드 |
| --- | --- |
| 204 | 본문 없음 · 성공. 본문 없음. |
| 401 | AUTHENTICATION_REQUIRED · AUTHENTICATION_REQUIRED; 발생 조건 및 재시도는 operation 설명과 기능 명세 참조. |
| 400 | VALIDATION_FAILED, VERIFICATION_INVALID · VALIDATION_FAILED / VERIFICATION_INVALID; 발생 조건 및 재시도는 operation 설명과 기능 명세 참조. |
| 403 | CSRF_INVALID, PIN_SETUP_AUTHORIZATION_REQUIRED · CSRF_INVALID / PIN_SETUP_AUTHORIZATION_REQUIRED; 발생 조건 및 재시도는 operation 설명과 기능 명세 참조. |
| 413 | REQUEST_TOO_LARGE · REQUEST_TOO_LARGE; B-02.9 공유안의 모든 Part 1 JSON 본문 최대16384바이트(16KiB), 공백/escape 포함 UTF-8 실제 본문 기준.16384는 크기 검사 통과·16385부터413이며 CSRF/로그인 검사보다 먼저 판정. 기존 기술 기준안·D17 실운영값/검증 보류. |
| 415 | UNSUPPORTED_MEDIA_TYPE · UNSUPPORTED_MEDIA_TYPE; 발생 조건 및 재시도는 operation 설명과 기능 명세 참조. |
| 503 | AUTH_STATE_UNAVAILABLE · AUTH_STATE_UNAVAILABLE; 발생 조건 및 재시도는 operation 설명과 기능 명세 참조. |
| 409 | PIN_ALREADY_SET · PIN_ALREADY_SET; 발생 조건 및 재시도는 operation 설명과 기능 명세 참조. |
| 429 | RATE_LIMITED · RATE_LIMITED; 발생 조건 및 재시도는 operation 설명과 기능 명세 참조. |

응답 추가 헤더: 429 `Retry-After`: 최소 재시도 대기 초. 실제 남은 차단시간으로 계산.

트랜잭션·재시도: accounts→session_security→guardian_pins(기존 유무)→challenge 잠금. PIN 생성+승자token 소비+다른 미소비 PIN_SETUP/PIN_RESET 폐기 한 Tx. 소비한 승자에는 invalidated_at을 추가하지 않는다. lock보다 뒤면 generation 재검사 거부.

DB 읽기: `accounts, session_security, email_verifications, guardian_pins`; 쓰기: `guardian_pins, email_verifications, auth_rate_limits`. 공통 제한 예약과 프레임워크 세션 저장은 별도 공통 처리다.

<a id="section-6-12"></a>
<a id="operation-unlockguardian"></a>
### 6.12 보호자 PIN 확인

`POST /api/v1/guardian/unlock` · `unlockGuardian` · **담당 Part 1** · A-05 · 권한 G+X

현재 세션에만 until 및 PIN version 기록. PIN 없음409. 계정 기준 최근900초의 실제 오답5회이면5번째 판정 시점부터900초 차단하는 Part 1 공유안 채택. 1\~4번째 오답400 PIN_INVALID, 5번째 및 차단 중429 RATE_LIMITED와 Retry-After. 성공은 오답 횟수에 포함하거나 기존 오답을 초기화하지 않음. 새 세션·재로그인·다른 기기도 계정 제한 유지. 집계 구간(t-900초,t]의 하한은 제외. 차단 중에는 올바른 PIN도 비교하지 않으며 재요청으로 차단 연장 없음. 차단 만료와 같은 시각부터 다른 제한이 없으면 재검증 가능. 전체 요청량은 별도 계정20회/IP100회 각900초 고정창의 B-02.8 Part 1 공유안·기존 기술 기준안·D17 실운영값/검증 보류. 성공 포함 사전 예약하고 새 로그인으로 초기화하지 않으며 전체 한도 거부는 실제 오답에 추가하지 않음. 오답 차단만으로 로그인·아이 대화·이미 유효한 guardian 권한·PIN을 변경하지 않음. reset과 직렬화. D04 확정으로 현재 로그인 세션의 PIN 성공부터 최대1800초, guardian_unlocked_until은 성공 시각+1800초. 활동/조회 자동 연장 없음·now>=until 거부·실제 로그인 유효 검사 유지. B-05.2 일반 로그인 expires_at=NULL이어도 guardian1800초 및 실제 로그인 유효 검사 유지, 유한 상한이 있는 문맥에는 기존 상한 제약 적용. 만료 뒤 보호자 API403 GUARDIAN_UNLOCK_REQUIRED/PIN 재확인, 이미 전달한 데이터 숨김은 프론트 요구. 재설정은 PIN_RESET 이메일 재인증으로 제공. 오답 차단은 재설정으로 자동 초기화하지 않는다.

| 위치 | 필드 | 필수 | 타입·기본값·검증 |
| --- | --- | --- | --- |
| header | X-XSRF-TOKEN | 예 | type="string"; minLength=1 GET auth/csrf 응답 token. 누락/불일치는 403. |

요청 body: `application/json` [GuardianUnlockRequest](schemas/guardian.md#schema-guardianunlockrequest)

| HTTP | 본문·오류 코드 |
| --- | --- |
| 200 | [GuardianUnlockResponse](schemas/guardian.md#schema-guardianunlockresponse) · 성공.  |
| 401 | AUTHENTICATION_REQUIRED · AUTHENTICATION_REQUIRED; 발생 조건 및 재시도는 operation 설명과 기능 명세 참조. |
| 400 | PIN_INVALID, VALIDATION_FAILED · VALIDATION_FAILED / PIN_INVALID; 발생 조건 및 재시도는 operation 설명과 기능 명세 참조. |
| 403 | CSRF_INVALID · CSRF_INVALID; 발생 조건 및 재시도는 operation 설명과 기능 명세 참조. |
| 413 | REQUEST_TOO_LARGE · REQUEST_TOO_LARGE; B-02.9 공유안의 모든 Part 1 JSON 본문 최대16384바이트(16KiB), 공백/escape 포함 UTF-8 실제 본문 기준.16384는 크기 검사 통과·16385부터413이며 CSRF/로그인 검사보다 먼저 판정. 기존 기술 기준안·D17 실운영값/검증 보류. |
| 415 | UNSUPPORTED_MEDIA_TYPE · UNSUPPORTED_MEDIA_TYPE; 발생 조건 및 재시도는 operation 설명과 기능 명세 참조. |
| 503 | AUTH_STATE_UNAVAILABLE · AUTH_STATE_UNAVAILABLE; 발생 조건 및 재시도는 operation 설명과 기능 명세 참조. |
| 409 | PIN_NOT_SET · PIN_NOT_SET; 발생 조건 및 재시도는 operation 설명과 기능 명세 참조. |
| 429 | RATE_LIMITED · RATE_LIMITED; 발생 조건 및 재시도는 operation 설명과 기능 명세 참조. |

응답 추가 헤더: 429 `Retry-After`: 최소 재시도 대기 초. 실제 남은 차단시간으로 계산.

트랜잭션·재시도: 전체 요청량 먼저 별도 커밋. 계정/session/PIN/PIN_FAILURE_ACCOUNT lock 뒤 차단 재검사. 성공 권한 또는 오답·차단 상태를 같은 업무 트랜잭션에서 커밋. 400/429 오답 응답 때문에 실패 상태를 rollback하지 않음. 최초 오답 row도 account lock 아래 생성. 잠금 후 DB clock_timestamp로 차단 검사; 해시 비교 뒤 유효 세션 재검사 후 새 DB clock_timestamp로 성공/오답/guardian 기한 판정. 대기 전 Tx 시작 시각 금지.

DB 읽기: `accounts, guardian_pins, session_security, auth_rate_limits`; 쓰기: `session_security, auth_rate_limits`. 공통 제한 예약과 프레임워크 세션 저장은 별도 공통 처리다.

200 응답 예시 (normal):

<!-- json-example: GuardianUnlockResponse -->
```json
{
  "data": {
    "guardianUnlockedUntil": "2026-09-30T15:00:00+09:00"
  }
}
```

<a id="section-6-13"></a>
<a id="operation-lockguardian"></a>
### 6.13 보호자 잠금 및 아이 모드 전환

`POST /api/v1/guardian/lock` · `lockGuardian` · **담당 Part 1** · A-05 · 권한 G+X

현재 guardian 권한 null, setup generation 증가, 현재 PIN_SETUP/PIN_RESET challenge/token 폐기. 로그인 및 ACTIVE 대화 연결 유지. B-05.3 공유안으로 대시보드 전체 영역 이탈 시 호출하여 보호자 확인 상태만 즉시 해제, PIN 자체 삭제/변경·로그아웃 아님. 내부 이동은 이탈로 보지 않는 해석/직접 URL·앱 종료 감지/실패 UX/공유 탭은 후속 검토. 모든 내부 이동·background 무차별 lock은 미확정이며 PIN_SETUP/PIN_RESET 흐름 영향 검토. 유효 session에서 반복해도 204.

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

응답 추가 헤더: 429 `Retry-After`: 최소 재시도 대기 초. 실제 남은 차단시간으로 계산.

트랜잭션·재시도: 계정/session lock; 권한 clear+generation 증가+challenge 폐기를 한 트랜잭션.

DB 읽기: `accounts, session_security`; 쓰기: `session_security, email_verifications`. 공통 제한 예약과 프레임워크 세션 저장은 별도 공통 처리다.

<a id="section-6-14"></a>
<a id="operation-resetguardianpin"></a>
### 6.14 이메일 재인증으로 보호자 PIN 재설정

`POST /api/v1/guardian/pin/reset` · `resetGuardianPin` · **담당 Part 1** · A-05 · 권한 G+X+V(PIN_RESET)

D16 로그인한 보호자가 PIN_RESET 이메일 번호를 확인해 받은 verificationToken과 newPin을 제출한다. PIN없으면409 PIN_NOT_SET. 다른목적/해시/기한/소비오류400, 실제PIN_RESET의계정/문맥/generation/pin_version/폐기불일치403 PIN_RESET_AUTHORIZATION_REQUIRED. 구조상token누락/null은400. 성공204 후 자동unlock없음;일반로그인과당일ACTIVE연결은유지. PIN오답/차단 및요청량예산은보수적인기술설계로유지(사용자가초기화선택한것아님). 응답유실이면새PIN으로unlock확인하며동일token재실행성공을보장하지않음.

| 위치 | 필드 | 필수 | 타입·기본값·검증 |
| --- | --- | --- | --- |
| header | X-XSRF-TOKEN | 예 | type="string"; minLength=1 GET auth/csrf 응답 token. 누락/불일치는 403. |

요청 body: `application/json` [GuardianPinResetRequest](schemas/guardian.md#schema-guardianpinresetrequest)

| HTTP | 본문·오류 코드 |
| --- | --- |
| 204 | 본문 없음 · PIN 재설정 완료. 자동 PIN unlock 없음. 본문 없음. |
| 401 | AUTHENTICATION_REQUIRED · AUTHENTICATION_REQUIRED; 발생 조건 및 재시도는 operation 설명과 기능 명세 참조. |
| 400 | VALIDATION_FAILED, VERIFICATION_INVALID · VALIDATION_FAILED / VERIFICATION_INVALID |
| 403 | CSRF_INVALID, PIN_RESET_AUTHORIZATION_REQUIRED · CSRF_INVALID / PIN_RESET_AUTHORIZATION_REQUIRED |
| 413 | REQUEST_TOO_LARGE · REQUEST_TOO_LARGE; B-02.9 공유안의 모든 Part 1 JSON 본문 최대16384바이트(16KiB), 공백/escape 포함 UTF-8 실제 본문 기준.16384는 크기 검사 통과·16385부터413이며 CSRF/로그인 검사보다 먼저 판정. 기존 기술 기준안·D17 실운영값/검증 보류. |
| 415 | UNSUPPORTED_MEDIA_TYPE · UNSUPPORTED_MEDIA_TYPE; 발생 조건 및 재시도는 operation 설명과 기능 명세 참조. |
| 503 | AUTH_STATE_UNAVAILABLE · AUTH_STATE_UNAVAILABLE |
| 429 | RATE_LIMITED · RATE_LIMITED; 발생 조건 및 재시도는 operation 설명과 기능 명세 참조. |
| 409 | PIN_NOT_SET · PIN_NOT_SET |

응답 추가 헤더: 429 `Retry-After`: 최소 재시도 대기 초. 실제 남은 차단시간으로 계산.

트랜잭션·재시도: accounts→session_security→guardian_pins→challenge 잠금. current권한최종검사→새PIN hash·pin_version증가·승자token consumed→다른미소비PIN_SETUP/PIN_RESET 폐기를같은업무Tx로커밋. 승자에는 invalidated_at을추가하지않음. 모든기존guardian은버전으로무효. DB설정/해시실패는전부rollback.

DB 읽기: `accounts, guardian_pins, session_security, email_verifications, auth_rate_limits`; 쓰기: `guardian_pins, email_verifications`. 공통 제한 예약과 프레임워크 세션 저장은 별도 공통 처리다.
