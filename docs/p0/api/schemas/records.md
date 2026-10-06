# 공유 기록·발화 읽기 모델 JSON 스키마

[전체 API 목차](../../Integrated_API_Spec.md) · **담당: 각 API 담당 기준** · 계약 v3.1 / 문서 분할 2026-10-06

절 번호는 기존 통합 명세의 번호를 유지합니다. 다른 절은 전체 목차에서 찾을 수 있습니다.

61개 전체 스키마 중 이 기능에 해당하는 정의입니다. JSON `$ref`는 통합 OpenAPI의 components를 참조합니다.

## 이 문서에서 찾기

- [14.5 ConversationView](#section-14-5)
- [14.6 TurnView](#section-14-6)
- [14.12 ConversationPage](#section-14-12)
- [14.13 ConversationDetail](#section-14-13)
- [14.29 ConversationPageResponse](#section-14-29)
- [14.30 ConversationDetailResponse](#section-14-30)

<a id="section-14-5"></a>
<a id="schema-conversationview"></a>
### 14.5 ConversationView

<!-- json-schema: ConversationView -->
```json
{
  "type": "object",
  "properties": {
    "conversationId": {
      "type": "string",
      "format": "uuid"
    },
    "childId": {
      "type": "string",
      "format": "uuid"
    },
    "status": {
      "type": "string",
      "enum": [
        "ACTIVE",
        "CLOSING",
        "ENDED"
      ]
    },
    "startedAt": {
      "type": "string",
      "format": "date-time",
      "pattern": "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\+09:00(?![\\s\\S])",
      "description": "KST 오프셋 +09:00. 초 정밀도."
    },
    "endedAt": {
      "type": "string",
      "format": "date-time",
      "pattern": "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\+09:00(?![\\s\\S])",
      "description": "KST 오프셋 +09:00. 초 정밀도.",
      "nullable": true
    },
    "title": {
      "type": "string",
      "nullable": true,
      "x-policy": "READ_MODEL_TEXT_MAX_LENGTH: Part 2와 합의 필요, 임의 절단 금지"
    },
    "topic": {
      "type": "string",
      "nullable": true,
      "x-policy": "READ_MODEL_TEXT_MAX_LENGTH: Part 2와 합의 필요, 임의 절단 금지"
    },
    "summary": {
      "type": "string",
      "nullable": true,
      "x-policy": "READ_MODEL_TEXT_MAX_LENGTH: Part 2와 합의 필요, 임의 절단 금지"
    },
    "summaryStatus": {
      "type": "string",
      "enum": [
        "NOT_STARTED",
        "PENDING",
        "READY",
        "FAILED",
        "EMPTY"
      ]
    },
    "endRequestedAt": {
      "type": "string",
      "format": "date-time",
      "pattern": "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\+09:00(?![\\s\\S])",
      "description": "최초 종료 경계. MANUAL은 DB 현재시각, MIDNIGHT는 scheduledEndAt. 중복 end/자정 도달로 변경하지 않음.",
      "nullable": true
    },
    "endReason": {
      "type": "string",
      "nullable": true,
      "enum": [
        null,
        "MANUAL",
        "MIDNIGHT"
      ]
    }
  },
  "required": [
    "conversationId",
    "childId",
    "status",
    "startedAt",
    "endedAt",
    "title",
    "topic",
    "summary",
    "summaryStatus",
    "endRequestedAt",
    "endReason"
  ],
  "additionalProperties": false,
  "description": "공통11필드. ACTIVE는 endRequestedAt/endReason/endedAt=null, CLOSING은 종료경계/사유 필수·endedAt=null, ENDED는 모두 필수. ACTIVE/CLOSING의 summaryStatus=NOT_STARTED. ENDED의 endedAt>=endRequestedAt>=startedAt; MANUAL 경계<scheduledEndAt, MIDNIGHT 경계=scheduledEndAt. READY만 title/topic/summary 모두 허용 문자열, 다른 요약 상태는 null. 보호자 목록·상세는 ENDED만, CLOSING/ACTIVE 제외. 같은 날 여러 ENDED 및 빈 EMPTY 기록 허용.",
  "allOf": [
    {
      "oneOf": [
        {
          "properties": {
            "status": {
              "enum": [
                "ACTIVE"
              ]
            },
            "endedAt": {
              "type": "string",
              "nullable": true,
              "enum": [
                null
              ]
            },
            "summaryStatus": {
              "enum": [
                "NOT_STARTED"
              ]
            },
            "endRequestedAt": {
              "type": "string",
              "nullable": true,
              "enum": [
                null
              ]
            },
            "endReason": {
              "type": "string",
              "nullable": true,
              "enum": [
                null
              ]
            }
          }
        },
        {
          "properties": {
            "status": {
              "enum": [
                "CLOSING"
              ]
            },
            "endedAt": {
              "type": "string",
              "nullable": true,
              "enum": [
                null
              ]
            },
            "endRequestedAt": {
              "type": "string",
              "format": "date-time",
              "pattern": "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\+09:00(?![\\s\\S])",
              "description": "KST 오프셋 +09:00. 초 정밀도."
            },
            "endReason": {
              "type": "string",
              "enum": [
                "MANUAL",
                "MIDNIGHT"
              ]
            },
            "summaryStatus": {
              "enum": [
                "NOT_STARTED"
              ]
            }
          }
        },
        {
          "properties": {
            "status": {
              "enum": [
                "ENDED"
              ]
            },
            "endedAt": {
              "type": "string",
              "format": "date-time",
              "pattern": "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\+09:00(?![\\s\\S])",
              "description": "KST 오프셋 +09:00. 초 정밀도."
            },
            "summaryStatus": {
              "enum": [
                "PENDING",
                "READY",
                "FAILED",
                "EMPTY"
              ]
            },
            "endRequestedAt": {
              "type": "string",
              "format": "date-time",
              "pattern": "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\+09:00(?![\\s\\S])",
              "description": "KST 오프셋 +09:00. 초 정밀도."
            },
            "endReason": {
              "type": "string",
              "enum": [
                "MANUAL",
                "MIDNIGHT"
              ]
            }
          }
        }
      ]
    },
    {
      "oneOf": [
        {
          "properties": {
            "summaryStatus": {
              "enum": [
                "READY"
              ]
            },
            "title": {
              "type": "string"
            },
            "topic": {
              "type": "string"
            },
            "summary": {
              "type": "string"
            }
          }
        },
        {
          "properties": {
            "summaryStatus": {
              "enum": [
                "NOT_STARTED",
                "PENDING",
                "FAILED",
                "EMPTY"
              ]
            },
            "title": {
              "type": "string",
              "nullable": true,
              "enum": [
                null
              ]
            },
            "topic": {
              "type": "string",
              "nullable": true,
              "enum": [
                null
              ]
            },
            "summary": {
              "type": "string",
              "nullable": true,
              "enum": [
                null
              ]
            }
          }
        }
      ]
    }
  ],
  "x-owner": "Shared: Part 2 storage / Part 1 guardian read"
}
```

<a id="section-14-6"></a>
<a id="schema-turnview"></a>
### 14.6 TurnView

<!-- json-schema: TurnView -->
```json
{
  "type": "object",
  "properties": {
    "turnId": {
      "type": "string",
      "format": "uuid"
    },
    "sequence": {
      "type": "integer",
      "minimum": 1
    },
    "status": {
      "type": "string",
      "enum": [
        "PROCESSING",
        "SUCCEEDED",
        "FAILED"
      ]
    },
    "childText": {
      "type": "string",
      "nullable": true,
      "x-policy": "READ_MODEL_TEXT_MAX_LENGTH: Part 2와 합의 필요, 임의 절단 금지"
    },
    "childTextVisibility": {
      "type": "string",
      "enum": [
        "VISIBLE",
        "REDACTED",
        "OMITTED"
      ]
    },
    "replyText": {
      "type": "string",
      "nullable": true,
      "x-policy": "READ_MODEL_TEXT_MAX_LENGTH: Part 2와 합의 필요, 임의 절단 금지"
    },
    "replyTextVisibility": {
      "type": "string",
      "enum": [
        "VISIBLE",
        "REDACTED",
        "OMITTED"
      ]
    },
    "createdAt": {
      "type": "string",
      "format": "date-time",
      "pattern": "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\+09:00(?![\\s\\S])",
      "description": "KST 오프셋 +09:00. 초 정밀도."
    },
    "completedAt": {
      "type": "string",
      "format": "date-time",
      "pattern": "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\+09:00(?![\\s\\S])",
      "description": "KST 오프셋 +09:00. 초 정밀도.",
      "nullable": true
    },
    "errorCode": {
      "type": "string",
      "nullable": true
    }
  },
  "required": [
    "turnId",
    "sequence",
    "status",
    "childText",
    "childTextVisibility",
    "replyText",
    "replyTextVisibility",
    "createdAt",
    "completedAt",
    "errorCode"
  ],
  "additionalProperties": false,
  "description": "각 텍스트: OMITTED=null, VISIBLE/REDACTED=nonnull. PROCESSING은 완료시각/오류/텍스트 null 및 OMITTED. SUCCEEDED는 완료시각 필수, 오류 null. FAILED는 완료시각과 오류 필수. clientRequestId/음성/내부 위험 신호 제외.",
  "allOf": [
    {
      "oneOf": [
        {
          "properties": {
            "childTextVisibility": {
              "enum": [
                "OMITTED"
              ]
            },
            "childText": {
              "type": "string",
              "nullable": true,
              "enum": [
                null
              ]
            }
          }
        },
        {
          "properties": {
            "childTextVisibility": {
              "enum": [
                "VISIBLE",
                "REDACTED"
              ]
            },
            "childText": {
              "type": "string"
            }
          }
        }
      ]
    },
    {
      "oneOf": [
        {
          "properties": {
            "replyTextVisibility": {
              "enum": [
                "OMITTED"
              ]
            },
            "replyText": {
              "type": "string",
              "nullable": true,
              "enum": [
                null
              ]
            }
          }
        },
        {
          "properties": {
            "replyTextVisibility": {
              "enum": [
                "VISIBLE",
                "REDACTED"
              ]
            },
            "replyText": {
              "type": "string"
            }
          }
        }
      ]
    },
    {
      "oneOf": [
        {
          "properties": {
            "status": {
              "enum": [
                "PROCESSING"
              ]
            },
            "completedAt": {
              "type": "string",
              "nullable": true,
              "enum": [
                null
              ]
            },
            "errorCode": {
              "type": "string",
              "nullable": true,
              "enum": [
                null
              ]
            },
            "childText": {
              "type": "string",
              "nullable": true,
              "enum": [
                null
              ]
            },
            "replyText": {
              "type": "string",
              "nullable": true,
              "enum": [
                null
              ]
            },
            "childTextVisibility": {
              "enum": [
                "OMITTED"
              ]
            },
            "replyTextVisibility": {
              "enum": [
                "OMITTED"
              ]
            }
          }
        },
        {
          "properties": {
            "status": {
              "enum": [
                "SUCCEEDED"
              ]
            },
            "completedAt": {
              "type": "string",
              "format": "date-time",
              "pattern": "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\+09:00(?![\\s\\S])",
              "description": "KST 오프셋 +09:00. 초 정밀도."
            },
            "errorCode": {
              "type": "string",
              "nullable": true,
              "enum": [
                null
              ]
            }
          }
        },
        {
          "properties": {
            "status": {
              "enum": [
                "FAILED"
              ]
            },
            "completedAt": {
              "type": "string",
              "format": "date-time",
              "pattern": "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\+09:00(?![\\s\\S])",
              "description": "KST 오프셋 +09:00. 초 정밀도."
            },
            "errorCode": {
              "type": "string",
              "minLength": 1
            }
          }
        }
      ]
    }
  ],
  "x-owner": "Shared: Part 2 storage / Part 1 guardian read"
}
```

<a id="section-14-12"></a>
<a id="schema-conversationpage"></a>
### 14.12 ConversationPage

<!-- json-schema: ConversationPage -->
```json
{
  "type": "object",
  "properties": {
    "items": {
      "type": "array",
      "items": {
        "allOf": [
          {
            "$ref": "#/components/schemas/ConversationView"
          },
          {
            "properties": {
              "status": {
                "enum": [
                  "ENDED"
                ]
              }
            }
          }
        ]
      }
    },
    "page": {
      "type": "integer",
      "minimum": 0
    },
    "size": {
      "type": "integer",
      "minimum": 1,
      "maximum": 100
    },
    "hasNext": {
      "type": "boolean"
    }
  },
  "required": [
    "items",
    "page",
    "size",
    "hasNext"
  ],
  "additionalProperties": false,
  "x-owner": "Part 1"
}
```

<a id="section-14-13"></a>
<a id="schema-conversationdetail"></a>
### 14.13 ConversationDetail

<!-- json-schema: ConversationDetail -->
```json
{
  "type": "object",
  "properties": {
    "conversation": {
      "allOf": [
        {
          "$ref": "#/components/schemas/ConversationView"
        },
        {
          "properties": {
            "status": {
              "enum": [
                "ENDED"
              ]
            }
          }
        }
      ]
    },
    "turns": {
      "type": "array",
      "items": {
        "$ref": "#/components/schemas/TurnView"
      },
      "maxItems": 100
    },
    "hasNext": {
      "type": "boolean"
    },
    "nextAfterSequence": {
      "type": "integer",
      "minimum": 1,
      "nullable": true
    }
  },
  "required": [
    "conversation",
    "turns",
    "hasNext",
    "nextAfterSequence"
  ],
  "additionalProperties": false,
  "description": "D05A 보호자 ENDED 상세. sequence ASC, afterSequence 초과, 기본/최대100턴. hasNext=true이면 nextAfterSequence는 마지막 반환sequence, false이면null. 권한은 매페이지 재검사.",
  "allOf": [
    {
      "oneOf": [
        {
          "properties": {
            "hasNext": {
              "enum": [
                true
              ]
            },
            "nextAfterSequence": {
              "type": "integer",
              "minimum": 1
            },
            "turns": {
              "type": "array",
              "minItems": 1,
              "items": {
                "$ref": "#/components/schemas/TurnView"
              }
            }
          }
        },
        {
          "properties": {
            "hasNext": {
              "enum": [
                false
              ]
            },
            "nextAfterSequence": {
              "type": "integer",
              "nullable": true,
              "enum": [
                null
              ]
            }
          }
        }
      ]
    }
  ],
  "x-owner": "Part 1"
}
```

<a id="section-14-29"></a>
<a id="schema-conversationpageresponse"></a>
### 14.29 ConversationPageResponse

<!-- json-schema: ConversationPageResponse -->
```json
{
  "type": "object",
  "properties": {
    "data": {
      "$ref": "#/components/schemas/ConversationPage"
    }
  },
  "required": [
    "data"
  ],
  "additionalProperties": false,
  "x-owner": "Part 1"
}
```

<a id="section-14-30"></a>
<a id="schema-conversationdetailresponse"></a>
### 14.30 ConversationDetailResponse

<!-- json-schema: ConversationDetailResponse -->
```json
{
  "type": "object",
  "properties": {
    "data": {
      "$ref": "#/components/schemas/ConversationDetail"
    }
  },
  "required": [
    "data"
  ],
  "additionalProperties": false,
  "x-owner": "Part 1"
}
```
