# 인증·계정 JSON 스키마

[전체 API 목차](../../Integrated_API_Spec.md) · **담당: 각 API 담당 기준** · 계약 v3.1 / 문서 분할 2026-10-06

절 번호는 기존 통합 명세의 번호를 유지합니다. 다른 절은 전체 목차에서 찾을 수 있습니다.

61개 전체 스키마 중 이 기능에 해당하는 정의입니다. JSON `$ref`는 통합 OpenAPI의 components를 참조합니다.

## 이 문서에서 찾기

- [14.2 CsrfTokenResponse](#section-14-2)
- [14.3 AccountView](#section-14-3)
- [14.7 VerificationIssue](#section-14-7)
- [14.8 VerificationGrant](#section-14-8)
- [14.9 SignupResult](#section-14-9)
- [14.14 IssueEmailVerificationRequest](#section-14-14)
- [14.15 VerifyEmailCodeRequest](#section-14-15)
- [14.16 SignupRequest](#section-14-16)
- [14.17 LoginRequest](#section-14-17)
- [14.18 PasswordResetRequest](#section-14-18)
- [14.22 AccountViewResponse](#section-14-22)
- [14.24 VerificationIssueResponse](#section-14-24)
- [14.25 VerificationGrantResponse](#section-14-25)
- [14.26 SignupResultResponse](#section-14-26)

<a id="section-14-2"></a>
<a id="schema-csrftokenresponse"></a>
### 14.2 CsrfTokenResponse

<!-- json-schema: CsrfTokenResponse -->
```json
{
  "type": "object",
  "properties": {
    "headerName": {
      "type": "string",
      "enum": [
        "X-XSRF-TOKEN"
      ]
    },
    "parameterName": {
      "type": "string",
      "enum": [
        "_csrf"
      ]
    },
    "token": {
      "type": "string",
      "minLength": 1
    }
  },
  "required": [
    "headerName",
    "parameterName",
    "token"
  ],
  "additionalProperties": false,
  "x-owner": "Part 1"
}
```

<a id="section-14-3"></a>
<a id="schema-accountview"></a>
### 14.3 AccountView

<!-- json-schema: AccountView -->
```json
{
  "type": "object",
  "properties": {
    "accountId": {
      "type": "string",
      "format": "uuid"
    },
    "email": {
      "type": "string",
      "format": "email",
      "maxLength": 254,
      "pattern": "^[\\x21-\\x7E]+(?![\\s\\S])",
      "description": "B-01.1 원문 입력 기준: trim+전체 소문자·유효 ASCII 이메일·최대254자, Gmail 점/+suffix 보존. RFC 로컬 파트 예외와 달리 서비스 식별 정책. v3에서 유지한 입력 제약으로 유지."
    },
    "childId": {
      "type": "string",
      "format": "uuid",
      "nullable": true
    },
    "hasPin": {
      "type": "boolean"
    },
    "guardianUnlockedUntil": {
      "type": "string",
      "format": "date-time",
      "pattern": "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\+09:00(?![\\s\\S])",
      "description": "D04 확정 PIN 성공 시각+1800초, 현재 세션 최대30분·활동/조회 연장 없음. now>=until 거부 및 실제 로그인 유효 검사 유지. KST +09:00·초 정밀도, 실제 세션 저장·만료 동작은 구현 후 시험한다.",
      "nullable": true
    }
  },
  "required": [
    "accountId",
    "email",
    "childId",
    "hasPin",
    "guardianUnlockedUntil"
  ],
  "additionalProperties": false,
  "x-owner": "Part 1"
}
```

<a id="section-14-7"></a>
<a id="schema-verificationissue"></a>
### 14.7 VerificationIssue

<!-- json-schema: VerificationIssue -->
```json
{
  "type": "object",
  "properties": {
    "challengeId": {
      "type": "string",
      "format": "uuid"
    },
    "expiresAt": {
      "type": "string",
      "format": "date-time",
      "pattern": "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\+09:00(?![\\s\\S])",
      "description": "B-02.3 원문 입력 기준으로 발급 시각+600초, now>=expiry 거부. 발송 지연/재요청으로 임의 연장 없음. KST 오프셋 +09:00, 초 정밀도. 운영 세부값은 D17 보류."
    }
  },
  "required": [
    "challengeId",
    "expiresAt"
  ],
  "additionalProperties": false,
  "x-owner": "Part 1"
}
```

<a id="section-14-8"></a>
<a id="schema-verificationgrant"></a>
### 14.8 VerificationGrant

<!-- json-schema: VerificationGrant -->
```json
{
  "type": "object",
  "properties": {
    "verificationToken": {
      "type": "string",
      "writeOnly": false,
      "pattern": "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\\.[A-Za-z0-9_-]{43}(?![\\s\\S])",
      "description": "challengeId.secret 형태의 일회성 권한. PIN_SETUP/PIN_RESET은 로그인 계정·보안 문맥·generation에 결합하고 PIN_RESET은 발급 시 pin_version에도 결합. 원문token을 URL·로그·DB에 저장하지 않는다.",
      "readOnly": true
    },
    "tokenExpiresAt": {
      "type": "string",
      "format": "date-time",
      "pattern": "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\+09:00(?![\\s\\S])",
      "description": "B-02.3 원문 입력 기준으로 검증 성공 시각+600초, now>=expiry 거부. 번호 만료와 별도이며 소비/조회로 연장하지 않음. KST 오프셋 +09:00, 초 정밀도. 운영 세부값은 D17 보류."
    }
  },
  "required": [
    "verificationToken",
    "tokenExpiresAt"
  ],
  "additionalProperties": false,
  "x-owner": "Part 1"
}
```

<a id="section-14-9"></a>
<a id="schema-signupresult"></a>
### 14.9 SignupResult

<!-- json-schema: SignupResult -->
```json
{
  "type": "object",
  "properties": {
    "accountId": {
      "type": "string",
      "format": "uuid"
    }
  },
  "required": [
    "accountId"
  ],
  "additionalProperties": false,
  "x-owner": "Part 1"
}
```

<a id="section-14-14"></a>
<a id="schema-issueemailverificationrequest"></a>
### 14.14 IssueEmailVerificationRequest

<!-- json-schema: IssueEmailVerificationRequest -->
```json
{
  "oneOf": [
    {
      "type": "object",
      "properties": {
        "purpose": {
          "type": "string",
          "enum": [
            "SIGNUP",
            "RESET_PASSWORD"
          ]
        },
        "email": {
          "type": "string",
          "description": "전송 문자열. 서버가 trim+lower 후 x-normalized-schema의 이메일 형식/254자 제한을 검사한다.",
          "x-normalization": "trim-and-lower-before-validation",
          "x-normalized-schema": {
            "type": "string",
            "format": "email",
            "maxLength": 254,
            "pattern": "^[\\x21-\\x7E]+(?![\\s\\S])",
            "description": "B-01.1 원문 입력 기준: trim+전체 소문자·유효 ASCII 이메일·최대254자, Gmail 점/+suffix 보존. RFC 로컬 파트 예외와 달리 서비스 식별 정책. v3에서 유지한 입력 제약으로 유지."
          }
        }
      },
      "required": [
        "purpose",
        "email"
      ],
      "additionalProperties": false
    },
    {
      "type": "object",
      "properties": {
        "purpose": {
          "type": "string",
          "enum": [
            "PIN_SETUP",
            "PIN_RESET"
          ]
        },
        "email": {
          "type": "string",
          "description": "전송 문자열. 서버가 trim+lower 후 x-normalized-schema의 이메일 형식/254자 제한을 검사한다.",
          "x-normalization": "trim-and-lower-before-validation",
          "x-normalized-schema": {
            "type": "string",
            "format": "email",
            "maxLength": 254,
            "pattern": "^[\\x21-\\x7E]+(?![\\s\\S])",
            "description": "B-01.1 원문 입력 기준: trim+전체 소문자·유효 ASCII 이메일·최대254자, Gmail 점/+suffix 보존. RFC 로컬 파트 예외와 달리 서비스 식별 정책. v3에서 유지한 입력 제약으로 유지."
          }
        }
      },
      "required": [
        "purpose"
      ],
      "additionalProperties": false,
      "description": "PIN_SETUP/PIN_RESET은 유효 로그인 필수. 서버 현재 계정 이메일 사용; email 생략 가능, 명시적null 불가, 있으면 정규화 후 계정 이메일과 일치해야 한다."
    }
  ],
  "x-owner": "Part 1"
}
```

<a id="section-14-15"></a>
<a id="schema-verifyemailcoderequest"></a>
### 14.15 VerifyEmailCodeRequest

<!-- json-schema: VerifyEmailCodeRequest -->
```json
{
  "type": "object",
  "properties": {
    "challengeId": {
      "type": "string",
      "format": "uuid"
    },
    "code": {
      "type": "string",
      "pattern": "^[0-9]{6}(?![\\s\\S])",
      "minLength": 6,
      "maxLength": 6,
      "writeOnly": true,
      "description": "B-02.3 원문 입력 기준의 정확히6자리 ASCII 숫자 문자열, 선행0 보존. v3에서 유지한 입력 제약으로 유지.",
      "x-policy": "EMAIL_CODE_LENGTH=6, B-02.3 Part 1 공유안·기존 기술 기준안·D17 실운영값/검증 보류"
    }
  },
  "required": [
    "challengeId",
    "code"
  ],
  "additionalProperties": false,
  "x-owner": "Part 1"
}
```

<a id="section-14-16"></a>
<a id="schema-signuprequest"></a>
### 14.16 SignupRequest

<!-- json-schema: SignupRequest -->
```json
{
  "type": "object",
  "properties": {
    "verificationToken": {
      "type": "string",
      "writeOnly": true,
      "pattern": "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\\.[A-Za-z0-9_-]{43}(?![\\s\\S])",
      "description": "A-02.1 확정: challengeId(UUID) + 점 + CSPRNG 32바이트 secret의 패딩 없는 Base64url 43자. 서버는 secret 문자열의 UTF-8 바이트에 대한 SHA-256 검증값만 DB에 저장·상수 시간 비교하며 목적·만료·1회 소비를 검사. PIN_SETUP은 계정·세션·generation도 결합. 토큰 원문은 DB·로그에 저장하지 않음."
    },
    "password": {
      "type": "string",
      "writeOnly": true,
      "minLength": 15,
      "maxLength": 128,
      "description": "B-01.2 원문 입력 기준의 새 비밀번호15~128 Unicode 코드포인트. trim/정규화/절단 없음, 최대128은 프로젝트 선택. 유효 Unicode만 허용하며 단독 surrogate 등은400 VALIDATION_FAILED·조용한 대체 없음. UTF-8 최대512바이트는128×4 계산 결과이며 별도 설정값이 아님. 실제 고정 라이브러리 버전 입력 호환성은 A-01.10 검증 항목. v3에서 유지한 입력 제약으로 유지.",
      "x-policy": [
        "PASSWORD_MIN_LENGTH=15(새 비밀번호 생성, B-01.2 공유안)",
        "PASSWORD_MAX_LENGTH=128(새 비밀번호 생성, B-01.2 공유안)"
      ]
    }
  },
  "required": [
    "verificationToken",
    "password"
  ],
  "additionalProperties": false,
  "description": "P0 가입 요청은 SIGNUP verificationToken과 password 두 필드. 동의·추가 보호자 정보는 D15에 따라 P0 요청/저장에서 제외. 가입 후 별도 로그인.",
  "x-owner": "Part 1"
}
```

<a id="section-14-17"></a>
<a id="schema-loginrequest"></a>
### 14.17 LoginRequest

<!-- json-schema: LoginRequest -->
```json
{
  "type": "object",
  "properties": {
    "email": {
      "type": "string",
      "description": "전송 문자열. 서버가 trim+lower 후 x-normalized-schema의 이메일 형식/254자 제한을 검사한다.",
      "x-normalization": "trim-and-lower-before-validation",
      "x-normalized-schema": {
        "type": "string",
        "format": "email",
        "maxLength": 254,
        "pattern": "^[\\x21-\\x7E]+(?![\\s\\S])",
        "description": "B-01.1 원문 입력 기준: trim+전체 소문자·유효 ASCII 이메일·최대254자, Gmail 점/+suffix 보존. RFC 로컬 파트 예외와 달리 서비스 식별 정책. v3에서 유지한 입력 제약으로 유지."
      }
    },
    "password": {
      "type": "string",
      "writeOnly": true,
      "minLength": 1,
      "description": "기존 비밀번호 문자열 검증, minLength=1 유지. trim/정규화/절단 금지, 새 생성15~128자 규칙 소급 적용 없음. Part 1 JSON 본문16KiB(B-02.9 공유안)로 제한. 유효 Unicode 입력만 허용, 단독 surrogate 등은400 VALIDATION_FAILED로 거부·조용한 대체 없음.128/512 생성 상한 소급 없음, 실제 고정 라이브러리 버전 입력 호환성은 A-01.10 검증 항목.",
      "x-policy": [
        "PART1_JSON_MAX_BYTES"
      ]
    }
  },
  "required": [
    "email",
    "password"
  ],
  "additionalProperties": false,
  "description": "B-05.4 별도 rememberMe 미도입. 요청 필드/체크박스/자동 복원 없음, unknown field로 거부.",
  "x-owner": "Part 1"
}
```

<a id="section-14-18"></a>
<a id="schema-passwordresetrequest"></a>
### 14.18 PasswordResetRequest

<!-- json-schema: PasswordResetRequest -->
```json
{
  "type": "object",
  "properties": {
    "verificationToken": {
      "type": "string",
      "writeOnly": true,
      "pattern": "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\\.[A-Za-z0-9_-]{43}(?![\\s\\S])",
      "description": "A-02.1 확정: challengeId(UUID) + 점 + CSPRNG 32바이트 secret의 패딩 없는 Base64url 43자. 서버는 secret 문자열의 UTF-8 바이트에 대한 SHA-256 검증값만 DB에 저장·상수 시간 비교하며 목적·만료·1회 소비를 검사. PIN_SETUP은 계정·세션·generation도 결합. 토큰 원문은 DB·로그에 저장하지 않음."
    },
    "newPassword": {
      "type": "string",
      "writeOnly": true,
      "minLength": 15,
      "maxLength": 128,
      "description": "B-01.2 원문 입력 기준의 새 비밀번호15~128 Unicode 코드포인트. trim/정규화/절단 없음, 최대128은 프로젝트 선택. 유효 Unicode만 허용하며 단독 surrogate 등은400 VALIDATION_FAILED·조용한 대체 없음. UTF-8 최대512바이트는128×4 계산 결과이며 별도 설정값이 아님. 실제 고정 라이브러리 버전 입력 호환성은 A-01.10 검증 항목. v3에서 유지한 입력 제약으로 유지.",
      "x-policy": [
        "PASSWORD_MIN_LENGTH=15(새 비밀번호 생성, B-01.2 공유안)",
        "PASSWORD_MAX_LENGTH=128(새 비밀번호 생성, B-01.2 공유안)"
      ]
    }
  },
  "required": [
    "verificationToken",
    "newPassword"
  ],
  "additionalProperties": false,
  "x-owner": "Part 1"
}
```

<a id="section-14-22"></a>
<a id="schema-accountviewresponse"></a>
### 14.22 AccountViewResponse

<!-- json-schema: AccountViewResponse -->
```json
{
  "type": "object",
  "properties": {
    "data": {
      "$ref": "#/components/schemas/AccountView"
    }
  },
  "required": [
    "data"
  ],
  "additionalProperties": false,
  "x-owner": "Part 1"
}
```

<a id="section-14-24"></a>
<a id="schema-verificationissueresponse"></a>
### 14.24 VerificationIssueResponse

<!-- json-schema: VerificationIssueResponse -->
```json
{
  "type": "object",
  "properties": {
    "data": {
      "$ref": "#/components/schemas/VerificationIssue"
    }
  },
  "required": [
    "data"
  ],
  "additionalProperties": false,
  "x-owner": "Part 1"
}
```

<a id="section-14-25"></a>
<a id="schema-verificationgrantresponse"></a>
### 14.25 VerificationGrantResponse

<!-- json-schema: VerificationGrantResponse -->
```json
{
  "type": "object",
  "properties": {
    "data": {
      "$ref": "#/components/schemas/VerificationGrant"
    }
  },
  "required": [
    "data"
  ],
  "additionalProperties": false,
  "x-owner": "Part 1"
}
```

<a id="section-14-26"></a>
<a id="schema-signupresultresponse"></a>
### 14.26 SignupResultResponse

<!-- json-schema: SignupResultResponse -->
```json
{
  "type": "object",
  "properties": {
    "data": {
      "$ref": "#/components/schemas/SignupResult"
    }
  },
  "required": [
    "data"
  ],
  "additionalProperties": false,
  "x-owner": "Part 1"
}
```
