# 공통 오류 JSON 스키마

[전체 API 목차](../../Integrated_API_Spec.md) · **담당: 각 API 담당 기준** · 계약 v3.1 / 문서 분할 2026-10-06

절 번호는 기존 통합 명세의 번호를 유지합니다. 다른 절은 전체 목차에서 찾을 수 있습니다.

61개 전체 스키마 중 이 기능에 해당하는 정의입니다. JSON `$ref`는 통합 OpenAPI의 components를 참조합니다.

## 이 문서에서 찾기

- [14.1 ApiError](#section-14-1)

<a id="section-14-1"></a>
<a id="schema-apierror"></a>
### 14.1 ApiError

<!-- json-schema: ApiError -->
```json
{
  "type": "object",
  "properties": {
    "error": {
      "type": "object",
      "properties": {
        "code": {
          "type": "string"
        },
        "message": {
          "type": "string"
        },
        "requestId": {
          "type": "string",
          "format": "uuid",
          "description": "서버 생성 UUID; X-Request-ID와 동일, clientRequestId와 별개."
        },
        "fields": {
          "type": "array",
          "items": {
            "type": "object",
            "properties": {
              "field": {
                "type": "string"
              },
              "reason": {
                "type": "string"
              }
            },
            "required": [
              "field",
              "reason"
            ],
            "additionalProperties": false
          }
        }
      },
      "required": [
        "code",
        "message",
        "requestId",
        "fields"
      ],
      "additionalProperties": false
    }
  },
  "required": [
    "error"
  ],
  "additionalProperties": false,
  "x-owner": "Part 1"
}
```
