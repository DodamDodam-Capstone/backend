# 보호자 PIN JSON 스키마

[전체 API 목차](../../Integrated_API_Spec.md) · **담당: 각 API 담당 기준** · 계약 v3.1 / 문서 분할 2026-10-06

절 번호는 기존 통합 명세의 번호를 유지합니다. 다른 절은 전체 목차에서 찾을 수 있습니다.

61개 전체 스키마 중 이 기능에 해당하는 정의입니다. JSON `$ref`는 통합 OpenAPI의 components를 참조합니다.

## 이 문서에서 찾기

- [14.10 GuardianUnlock](#section-14-10)
- [14.19 GuardianPinSetupRequest](#section-14-19)
- [14.20 GuardianUnlockRequest](#section-14-20)
- [14.27 GuardianUnlockResponse](#section-14-27)
- [14.57 GuardianPinResetRequest](#section-14-57)

<a id="section-14-10"></a>
<a id="schema-guardianunlock"></a>
### 14.10 GuardianUnlock

<!-- json-schema: GuardianUnlock -->
```json
{
  "type": "object",
  "properties": {
    "guardianUnlockedUntil": {
      "type": "string",
      "format": "date-time",
      "pattern": "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\+09:00(?![\\s\\S])",
      "description": "D04 확정 PIN 성공 시각+1800초, 현재 세션 최대30분·활동/조회 연장 없음. now>=until 거부 및 실제 로그인 유효 검사 유지. KST +09:00·초 정밀도, 실제 세션 저장·만료 동작은 구현 후 시험한다."
    }
  },
  "required": [
    "guardianUnlockedUntil"
  ],
  "additionalProperties": false,
  "x-owner": "Part 1"
}
```

<a id="section-14-19"></a>
<a id="schema-guardianpinsetuprequest"></a>
### 14.19 GuardianPinSetupRequest

<!-- json-schema: GuardianPinSetupRequest -->
```json
{
  "type": "object",
  "properties": {
    "pin": {
      "type": "string",
      "pattern": "^[0-9]{4}(?![\\s\\S])",
      "writeOnly": true,
      "description": "정확히 ASCII 숫자 4자리 문자열. 0000 포함 앞자리 0 보존."
    },
    "verificationToken": {
      "type": "string",
      "writeOnly": true,
      "pattern": "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\\.[A-Za-z0-9_-]{43}(?![\\s\\S])",
      "description": "A-02.1 확정: challengeId(UUID) + 점 + CSPRNG 32바이트 secret의 패딩 없는 Base64url 43자. 서버는 secret 문자열의 UTF-8 바이트에 대한 SHA-256 검증값만 DB에 저장·상수 시간 비교하며 목적·만료·1회 소비를 검사. PIN_SETUP은 계정·세션·generation도 결합. 토큰 원문은 DB·로그에 저장하지 않음."
    }
  },
  "required": [
    "pin"
  ],
  "additionalProperties": false,
  "description": "verificationToken은 업무적으로 필수. 누락을 403 PIN_SETUP_AUTHORIZATION_REQUIRED로 표현하려고 구조 required에서는 제외. 명시적 null은 400.",
  "x-owner": "Part 1"
}
```

<a id="section-14-20"></a>
<a id="schema-guardianunlockrequest"></a>
### 14.20 GuardianUnlockRequest

<!-- json-schema: GuardianUnlockRequest -->
```json
{
  "type": "object",
  "properties": {
    "pin": {
      "type": "string",
      "pattern": "^[0-9]{4}(?![\\s\\S])",
      "writeOnly": true,
      "description": "정확히 ASCII 숫자 4자리 문자열. 0000 포함 앞자리 0 보존."
    }
  },
  "required": [
    "pin"
  ],
  "additionalProperties": false,
  "x-owner": "Part 1"
}
```

<a id="section-14-27"></a>
<a id="schema-guardianunlockresponse"></a>
### 14.27 GuardianUnlockResponse

<!-- json-schema: GuardianUnlockResponse -->
```json
{
  "type": "object",
  "properties": {
    "data": {
      "$ref": "#/components/schemas/GuardianUnlock"
    }
  },
  "required": [
    "data"
  ],
  "additionalProperties": false,
  "x-owner": "Part 1"
}
```

<a id="section-14-57"></a>
<a id="schema-guardianpinresetrequest"></a>
### 14.57 GuardianPinResetRequest

<!-- json-schema: GuardianPinResetRequest -->
```json
{
  "type": "object",
  "properties": {
    "newPin": {
      "type": "string",
      "pattern": "^[0-9]{4}(?![\\s\\S])",
      "writeOnly": true,
      "description": "정확히 ASCII 숫자 4자리 문자열. 0000 포함 앞자리 0 보존."
    },
    "verificationToken": {
      "type": "string",
      "writeOnly": true,
      "pattern": "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\\.[A-Za-z0-9_-]{43}(?![\\s\\S])",
      "description": "A-02.1 확정: challengeId(UUID) + 점 + CSPRNG 32바이트 secret의 패딩 없는 Base64url 43자. 서버는 secret 문자열의 UTF-8 바이트에 대한 SHA-256 검증값만 DB에 저장·상수 시간 비교하며 목적·만료·1회 소비를 검사. PIN_SETUP은 계정·세션·generation도 결합. 토큰 원문은 DB·로그에 저장하지 않음."
    }
  },
  "required": [
    "newPin",
    "verificationToken"
  ],
  "additionalProperties": false,
  "description": "D16 이메일 PIN_RESET 재인증의 일회성 verificationToken과 새PIN. 유효 로그인·동일계정/보안문맥/generation/발급시PIN버전 필수. PIN_SETUP 토큰 재사용 불가.",
  "x-owner": "Part 1"
}
```
