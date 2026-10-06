# 아이 프로필 JSON 스키마

[전체 API 목차](../../Integrated_API_Spec.md) · **담당: 각 API 담당 기준** · 계약 v3.1 / 문서 분할 2026-10-06

절 번호는 기존 통합 명세의 번호를 유지합니다. 다른 절은 전체 목차에서 찾을 수 있습니다.

61개 전체 스키마 중 이 기능에 해당하는 정의입니다. JSON `$ref`는 통합 OpenAPI의 components를 참조합니다.

## 이 문서에서 찾기

- [14.4 ChildView](#section-14-4)
- [14.11 ChildrenList](#section-14-11)
- [14.21 CreateChildRequest](#section-14-21)
- [14.23 ChildViewResponse](#section-14-23)
- [14.28 ChildrenListResponse](#section-14-28)

<a id="section-14-4"></a>
<a id="schema-childview"></a>
### 14.4 ChildView

<!-- json-schema: ChildView -->
```json
{
  "description": "name은 보호자용 이름, nickname은 아이용 호칭으로 별도 저장. 기존 name trim 후1~5 및 생일/성별/관심사/캐릭터 기준 유지. migration은 nullable nickname→name 복사 backfill→검증→NOT NULL; 이름 임의 절단/성별 임의 지정 금지.",
  "type": "object",
  "properties": {
    "childId": {
      "type": "string",
      "format": "uuid"
    },
    "name": {
      "type": "string",
      "minLength": 1,
      "maxLength": 5,
      "description": "trim 후1~5 Unicode 코드포인트·내부 공백 보존. B-04.1 D06 입력 기준 확정."
    },
    "birthDate": {
      "type": "string",
      "format": "date",
      "description": "유효 YYYY-MM-DD·KST 오늘 이하, 최저 연령 추가 없음. B-04.1 D06 입력 기준 확정."
    },
    "gender": {
      "type": "string",
      "description": "MALE/FEMALE 2종만 허용, 미지정값 없음. B-04.1 D06 입력 기준 확정.",
      "enum": [
        "MALE",
        "FEMALE"
      ]
    },
    "interests": {
      "type": "array",
      "description": "자유 입력0~10개·각 trim 후1~30 Unicode 코드포인트·동일값 중복 거부·순서 보존. 요청 생략 시[]·text[] 유지. B-04.2 D06 입력 기준 확정.",
      "items": {
        "type": "string",
        "minLength": 1,
        "maxLength": 30
      },
      "maxItems": 10,
      "uniqueItems": true
    },
    "characterId": {
      "type": "string",
      "description": "영문·숫자·_·-로1~64자, 지원 목록에 없는 ID400 VALIDATION_FAILED. B-04.3 D06 입력 기준 확정. 실제 목록/기본값/폐기 ID/카탈로그 소유·AI 대응은 J-04 미결, dodam은 예시.",
      "minLength": 1,
      "maxLength": 64,
      "pattern": "^[A-Za-z0-9_-]+(?![\\s\\S])"
    },
    "createdAt": {
      "type": "string",
      "format": "date-time",
      "pattern": "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\+09:00(?![\\s\\S])",
      "description": "KST 오프셋 +09:00. 초 정밀도."
    },
    "nickname": {
      "type": "string",
      "minLength": 1,
      "maxLength": 20,
      "description": "trim 후1~20 Unicode 코드포인트. 아이 화면·인사·AI 호칭용 애칭, null/빈 문자열 거부. 유일성/실명 인증 없음. 20자는 통합 기술 기준."
    }
  },
  "required": [
    "childId",
    "name",
    "birthDate",
    "gender",
    "interests",
    "characterId",
    "createdAt",
    "nickname"
  ],
  "additionalProperties": false,
  "x-owner": "Part 1"
}
```

<a id="section-14-11"></a>
<a id="schema-childrenlist"></a>
### 14.11 ChildrenList

<!-- json-schema: ChildrenList -->
```json
{
  "type": "object",
  "properties": {
    "items": {
      "type": "array",
      "items": {
        "$ref": "#/components/schemas/ChildView"
      },
      "maxItems": 1
    }
  },
  "required": [
    "items"
  ],
  "additionalProperties": false,
  "x-owner": "Part 1"
}
```

<a id="section-14-21"></a>
<a id="schema-createchildrequest"></a>
### 14.21 CreateChildRequest

<!-- json-schema: CreateChildRequest -->
```json
{
  "type": "object",
  "properties": {
    "name": {
      "type": "string",
      "description": "전송 문자열. trim 후1~5 Unicode 코드포인트·내부 공백 보존, 빈 이름 거부. B-04.1 D06 입력 기준 확정.",
      "x-normalization": "trim-before-validation",
      "x-normalized-schema": {
        "type": "string",
        "minLength": 1,
        "maxLength": 5
      }
    },
    "birthDate": {
      "type": "string",
      "format": "date",
      "description": "유효 YYYY-MM-DD·KST 오늘 이하, 최저 연령 추가 없음. B-04.1 D06 입력 기준 확정."
    },
    "gender": {
      "type": "string",
      "description": "MALE/FEMALE 2종만 허용, 미지정값 없음. B-04.1 D06 입력 기준 확정.",
      "enum": [
        "MALE",
        "FEMALE"
      ]
    },
    "interests": {
      "type": "array",
      "description": "자유 입력0~10개·각 trim 후1~30 Unicode 코드포인트·동일값 중복 거부·순서 보존. 요청 생략 시[]·text[] 유지. B-04.2 D06 입력 기준 확정.",
      "items": {
        "type": "string",
        "description": "각 원소 trim 후 1~30자. trim 결과 기준 중복을 거부한다.",
        "x-normalization": "trim-before-validation",
        "x-normalized-schema": {
          "type": "string",
          "minLength": 1,
          "maxLength": 30
        }
      },
      "maxItems": 10,
      "uniqueItems": true,
      "default": [],
      "x-normalized-schema": {
        "type": "array",
        "items": {
          "type": "string",
          "minLength": 1,
          "maxLength": 30
        },
        "maxItems": 10,
        "uniqueItems": true
      },
      "x-normalization": "trim-each-item-then-check-duplicates"
    },
    "characterId": {
      "type": "string",
      "description": "영문·숫자·_·-로1~64자, 지원 목록에 없는 ID400 VALIDATION_FAILED. B-04.3 D06 입력 기준 확정. 실제 목록/기본값/폐기 ID/카탈로그 소유·AI 대응은 J-04 미결, dodam은 예시.",
      "minLength": 1,
      "maxLength": 64,
      "pattern": "^[A-Za-z0-9_-]+(?![\\s\\S])"
    },
    "nickname": {
      "type": "string",
      "minLength": 1,
      "pattern": "\\S",
      "description": "trim 후1~20 Unicode 코드포인트. 아이 화면·인사·AI 호칭용 애칭, null/빈 문자열 거부. 유일성/실명 인증 없음. 20자는 통합 기술 기준.",
      "x-normalization": "trim-before-validation",
      "x-normalized-schema": {
        "type": "string",
        "minLength": 1,
        "maxLength": 20
      }
    }
  },
  "required": [
    "name",
    "birthDate",
    "gender",
    "characterId",
    "nickname"
  ],
  "additionalProperties": false,
  "description": "이름 name(보호자용)과 애칭 nickname(아이용)을 별도 필수 입력. name trim 후1~5, nickname trim 후1~20 Unicode 코드포인트, 빈 값/null 거부. 관심사 trim, birthDate는 KST 오늘 이하, gender 및 characterId 카탈로그 기준 유지. 별도 애칭 유일성·실명 인증 없음.",
  "x-owner": "Part 1"
}
```

<a id="section-14-23"></a>
<a id="schema-childviewresponse"></a>
### 14.23 ChildViewResponse

<!-- json-schema: ChildViewResponse -->
```json
{
  "type": "object",
  "properties": {
    "data": {
      "$ref": "#/components/schemas/ChildView"
    }
  },
  "required": [
    "data"
  ],
  "additionalProperties": false,
  "x-owner": "Part 1"
}
```

<a id="section-14-28"></a>
<a id="schema-childrenlistresponse"></a>
### 14.28 ChildrenListResponse

<!-- json-schema: ChildrenListResponse -->
```json
{
  "type": "object",
  "properties": {
    "data": {
      "$ref": "#/components/schemas/ChildrenList"
    }
  },
  "required": [
    "data"
  ],
  "additionalProperties": false,
  "x-owner": "Part 1"
}
```
