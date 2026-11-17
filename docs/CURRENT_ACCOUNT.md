# 현재 계정·앱 진입 상태 조회

[SCRUM-76](https://dodamdodam.atlassian.net/browse/SCRUM-76)의 구현 설명이다.
공통 계약은 [인증 API §6.6](p0/api/02-auth.md#operation-getcurrentaccount)와
[AccountView 스키마](p0/api/schemas/auth.md#schema-accountview)를 따른다.

## 요청과 응답

`GET /api/v1/auth/me`는 현재 유효한 `JSESSIONID`로 인증한다. GET이므로 CSRF 헤더와 요청 본문은 필요하지 않다.
브라우저는 `credentials: include`를 사용하며, 인증되지 않은 요청은 로그인 화면으로 302 이동하지 않고 401을 받는다.

성공은 HTTP 200과 `data` 아래 다음 5개 필드다. nullable 필드도 생략하지 않는다.

| 필드 | 의미 |
| --- | --- |
| accountId | 현재 로그인 계정 UUID |
| email | 정규화된 유효 ASCII 이메일, 최대 254자 |
| childId | 현재 계정의 아이 UUID. 미등록이면 null |
| hasPin | 현재 계정에 PIN이 설정되어 있는지. 보호자 잠금 여부와 별개 |
| guardianUnlockedUntil | 현재 문맥과 PIN 버전에서 유효한 보호자 확인 기한. 잠김·만료·버전 불일치면 null |

모든 응답은 `X-Request-ID`와 `Cache-Control: no-store`를 유지한다. 기존 필터가 로그인 쿠키와 응답 직전
실제 세션 유효성을 관리한다. 조회는 보호자 권한을 생성하거나 기한을 연장하지 않는다.

## 상태별 성공 예시

아래 이메일·UUID는 설명용 합성 값이다. 보호자 시각은 예시의 요청 시점보다 미래라고 가정한다.

### 1. 아이 등록 전

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

FE는 아이 등록 흐름으로 이동한다. 별도 `hasChild` 필드를 추가하지 않는다.

### 2. 아이 등록 후, PIN 설정 전

```json
{
  "data": {
    "accountId": "11111111-1111-4111-8111-111111111111",
    "email": "guardian@example.com",
    "childId": "22222222-2222-4222-8222-222222222222",
    "hasPin": false,
    "guardianUnlockedUntil": null
  }
}
```

### 3. PIN 설정 후, 보호자 잠금

```json
{
  "data": {
    "accountId": "11111111-1111-4111-8111-111111111111",
    "email": "guardian@example.com",
    "childId": "22222222-2222-4222-8222-222222222222",
    "hasPin": true,
    "guardianUnlockedUntil": null
  }
}
```

잠긴 상태도 정상 조회다. 이 요청에서 보호자 확인을 강제하거나 403을 반환하지 않는다.

### 4. 현재 문맥의 보호자 확인 유효

```json
{
  "data": {
    "accountId": "11111111-1111-4111-8111-111111111111",
    "email": "guardian@example.com",
    "childId": "22222222-2222-4222-8222-222222222222",
    "hasPin": true,
    "guardianUnlockedUntil": "2026-11-17T16:30:00+09:00"
  }
}
```

표시 시각은 KST 초 정밀도다. 실제 DB 기한보다 길어지지 않게 소수초를 내리며,
내린 표시 기한도 이미 지났다면 null을 반환한다.

### 5. 보호자 확인 만료 또는 PIN 재설정으로 기존 버전 무효

```json
{
  "data": {
    "accountId": "11111111-1111-4111-8111-111111111111",
    "email": "guardian@example.com",
    "childId": "22222222-2222-4222-8222-222222222222",
    "hasPin": true,
    "guardianUnlockedUntil": null
  }
}
```

다른 로그인 문맥의 보호자 확인도 상속하지 않는다. 일반 로그인까지 무효가 되었으면 이 예시 대신 401을 반환한다.

## 오류와 확인 방법

| HTTP / code | 조건과 FE 처리 |
| --- | --- |
| 401 AUTHENTICATION_REQUIRED | 세션 없음·만료·폐기·계정 버전 불일치·계정/문맥 불일치. 인증 상태 정리 및 로그인 안내 |
| 503 AUTH_STATE_UNAVAILABLE | 계정·아이·PIN·문맥 저장소 확인 불가 또는 잘못된 저장 이메일. 자동 로그아웃으로 해석하지 않고 오류 안내·조회 재시도 |
| 429 RATE_LIMITED | 전역 요청 제한의 공통 오류. SCRUM-77 필터 통합 시 적용하며 Retry-After 준수 |

로그인 없이 `curl -i http://localhost:8080/api/v1/auth/me`로 401과 공통 오류 헤더를 확인할 수 있다.
성공 응답은 이미 생성된 유효 로그인 세션으로 확인한다. 이메일 로그인 API 자체는 SCRUM-78 범위이며,
자동화 검증은 테스트 전용 PostgreSQL·JDBC 세션을 사용한다. 공개 테스트 로그인 경로는 추가하지 않는다.

```sh
./gradlew test --tests '*CurrentAccountRepositoryTests' --tests '*CurrentAccountServiceTests' --tests '*CurrentAccountHttpTests' --no-daemon --console=plain
./gradlew clean check --no-daemon --console=plain
```

## 코드와 조회 경계

- [AuthMeController](../src/main/java/com/dodamdodam/backend/auth/api/AuthMeController.java): 기존 세션 인증 → 서비스 → 성공 envelope.
- [CurrentAccountService](../src/main/java/com/dodamdodam/backend/auth/application/CurrentAccountService.java): 공통 세션·보호자 판정, KST 표시와 출력 검증.
- [CurrentAccountRepository](../src/main/java/com/dodamdodam/backend/auth/infrastructure/CurrentAccountRepository.java): 4개 테이블을 한 SQL로 읽고 DB 장애를 인증 상태 503으로 변환.
- [AccountView](../src/main/java/com/dodamdodam/backend/auth/dto/AccountView.java): 외부에 공개할 5개 필드만 전달.

업무 데이터는 읽기 전용이며 비밀번호·PIN 해시를 조회하지 않는다. 세션 프레임워크의 저장과 쿠키 갱신은 기존 정책을 따른다.
응답은 조회 시점의 화면 이동용 상태다. 실제 보호자·아이·대화 API는 수행 시 자신의 권한을 다시 검증하며,
이 응답을 새 권한 증명으로 사용하지 않는다. 신규 migration이나 OAuth·Supabase 연결은 포함하지 않는다.
