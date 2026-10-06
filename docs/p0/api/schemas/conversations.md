# 홈·대화·종료 JSON 스키마

[전체 API 목차](../../Integrated_API_Spec.md) · **담당: 각 API 담당 기준** · 계약 v3.1 / 문서 분할 2026-10-06

절 번호는 기존 통합 명세의 번호를 유지합니다. 다른 절은 전체 목차에서 찾을 수 있습니다.

61개 전체 스키마 중 이 기능에 해당하는 정의입니다. JSON `$ref`는 통합 OpenAPI의 components를 참조합니다.

## 이 문서에서 찾기

- [14.31 StartConversationRequest](#section-14-31)
- [14.33 ChildHome](#section-14-33)
- [14.34 StartConversationResult](#section-14-34)
- [14.39 ResumeConversationResult](#section-14-39)
- [14.48 EndReceipt](#section-14-48)
- [14.49 ChildHomeResponse](#section-14-49)
- [14.50 StartConversationResultResponse](#section-14-50)
- [14.51 ResumeConversationResultResponse](#section-14-51)
- [14.56 EndReceiptResponse](#section-14-56)
- [14.58 ActiveConversationExistsError](#section-14-58)
- [14.59 ConversationAvailability](#section-14-59)
- [14.60 EndPendingReceipt](#section-14-60)
- [14.61 EndPendingReceiptResponse](#section-14-61)

<a id="section-14-31"></a>
<a id="schema-startconversationrequest"></a>
### 14.31 StartConversationRequest

<!-- json-schema: StartConversationRequest -->
```json
{
  "type": "object",
  "properties": {
    "clientRequestId": {
      "type": "string",
      "format": "uuid"
    }
  },
  "required": [
    "clientRequestId"
  ],
  "additionalProperties": false,
  "description": "아이별 시작 중복 식별. 같은 키 재전송은 ACTIVE·현재 연결에 한해 기존 시작 정보. ENDED 키 재사용 금지.",
  "x-owner": "Part 2"
}
```

<a id="section-14-33"></a>
<a id="schema-childhome"></a>
### 14.33 ChildHome

<!-- json-schema: ChildHome -->
```json
{
  "type": "object",
  "properties": {
    "child": {
      "type": "object",
      "properties": {
        "childId": {
          "type": "string",
          "format": "uuid"
        },
        "name": {
          "type": "string"
        },
        "characterId": {
          "type": "string"
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
        "characterId",
        "nickname"
      ],
      "additionalProperties": false
    },
    "greeting": {
      "type": "string",
      "description": "서버 고정 nickname 템플릿. 대화 시작의 FE 고정 첫 인사와 구분하며 AI 생성/TTS가 아니다."
    },
    "joinedAt": {
      "type": "string",
      "format": "date-time",
      "pattern": "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\+09:00(?![\\s\\S])",
      "description": "공통 KST(+09:00), 초 정밀도."
    },
    "daysTogether": {
      "type": "integer",
      "minimum": 1
    },
    "activeConversationId": {
      "type": "string",
      "format": "uuid",
      "nullable": true
    },
    "menus": {
      "type": "array",
      "items": {
        "oneOf": [
          {
            "type": "object",
            "properties": {
              "code": {
                "type": "string",
                "enum": [
                  "TALK"
                ]
              },
              "status": {
                "type": "string",
                "enum": [
                  "AVAILABLE",
                  "UNAVAILABLE"
                ]
              }
            },
            "required": [
              "code",
              "status"
            ],
            "additionalProperties": false
          },
          {
            "type": "object",
            "properties": {
              "code": {
                "type": "string",
                "enum": [
                  "PLAY"
                ]
              },
              "status": {
                "type": "string",
                "enum": [
                  "COMING_SOON"
                ]
              }
            },
            "required": [
              "code",
              "status"
            ],
            "additionalProperties": false
          }
        ]
      },
      "minItems": 2,
      "maxItems": 2
    },
    "availability": {
      "$ref": "#/components/schemas/ConversationAvailability"
    }
  },
  "required": [
    "child",
    "greeting",
    "joinedAt",
    "daysTogether",
    "activeConversationId",
    "menus",
    "availability"
  ],
  "additionalProperties": false,
  "x-owner": "Part 2",
  "description": "TALK은 availability.canEnter에 따라 AVAILABLE/UNAVAILABLE, PLAY는 COMING_SOON. TALK,PLAY 순서 각1개. activeConversationId는 접근 가능한 당일 ACTIVE만, CLOSING은 null. 서비스 시간 안 CLOSING이면 false/CONVERSATION_CLOSING, 시간 밖 OUTSIDE_SERVICE_HOURS 우선. 새 closingConversationId 필드 없음. Home은 로그인/소유권 통과 시 시간 밖에도200; 과거 내용·음성 없음. joinedAt=아이 등록 시각, daysTogether=KST 등록일 포함. name/nickname 별도이며 아이 화면과 greeting은 nickname 사용."
}
```

<a id="section-14-34"></a>
<a id="schema-startconversationresult"></a>
### 14.34 StartConversationResult

<!-- json-schema: StartConversationResult -->
```json
{
  "type": "object",
  "properties": {
    "conversationId": {
      "type": "string",
      "format": "uuid"
    },
    "status": {
      "type": "string",
      "enum": [
        "ACTIVE"
      ]
    },
    "startedAt": {
      "type": "string",
      "format": "date-time",
      "pattern": "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\+09:00(?![\\s\\S])",
      "description": "공통 KST(+09:00), 초 정밀도."
    },
    "serviceDate": {
      "type": "string",
      "format": "date"
    },
    "scheduledEndAt": {
      "type": "string",
      "format": "date-time",
      "pattern": "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\+09:00(?![\\s\\S])",
      "description": "공통 KST(+09:00), 초 정밀도."
    }
  },
  "required": [
    "conversationId",
    "status",
    "startedAt",
    "serviceDate",
    "scheduledEndAt"
  ],
  "additionalProperties": false,
  "x-owner": "Part 2",
  "description": "새 대화 시작 정보. 같은 날 ENDED 이후 새 키로 새 대화 가능. serviceDate는 시작 KST 날짜, scheduledEndAt은 다음00시로 불변. 동일 키 ACTIVE는 기존 정보, CLOSING은409, ENDED는409 CONVERSATION_ENDED. 새 대화 첫 인사는 FE nickname 고정 문구로 AI/TTS/발화 저장·개수·요약에서 제외."
}
```

<a id="section-14-39"></a>
<a id="schema-resumeconversationresult"></a>
### 14.39 ResumeConversationResult

<!-- json-schema: ResumeConversationResult -->
```json
{
  "type": "object",
  "properties": {
    "conversationId": {
      "type": "string",
      "format": "uuid"
    },
    "status": {
      "type": "string",
      "enum": [
        "ACTIVE"
      ]
    },
    "startedAt": {
      "type": "string",
      "format": "date-time",
      "pattern": "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\+09:00(?![\\s\\S])",
      "description": "공통 KST(+09:00), 초 정밀도."
    },
    "turns": {
      "type": "array",
      "items": {
        "$ref": "#/components/schemas/TurnView"
      }
    },
    "processingTurnId": {
      "type": "string",
      "format": "uuid",
      "nullable": true
    },
    "serviceDate": {
      "type": "string",
      "format": "date"
    },
    "scheduledEndAt": {
      "type": "string",
      "format": "date-time",
      "pattern": "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\+09:00(?![\\s\\S])",
      "description": "공통 KST(+09:00), 초 정밀도."
    }
  },
  "required": [
    "conversationId",
    "status",
    "startedAt",
    "turns",
    "processingTurnId",
    "serviceDate",
    "scheduledEndAt"
  ],
  "additionalProperties": false,
  "allOf": [
    {
      "oneOf": [
        {
          "properties": {
            "processingTurnId": {
              "type": "string",
              "format": "uuid",
              "nullable": true,
              "enum": [
                null
              ]
            },
            "turns": {
              "type": "array",
              "items": {
                "$ref": "#/components/schemas/P2TerminalTurn"
              }
            }
          }
        },
        {
          "properties": {
            "processingTurnId": {
              "type": "string",
              "format": "uuid"
            }
          }
        }
      ]
    }
  ],
  "description": "서비스 시간 안 당일 ACTIVE만 명시 연결하고 전체 허용 발화를 sequence ASC 반환. 화면 이탈·새로고침·재로그인 복원. PROCESSING 중 가능. CLOSING/ENDED는 새 연결과 본문 반환 금지. 페이지화/절단 없음. audio/추천/FE 첫 인사는 발화 목록에 없음.",
  "x-service-invariants": [
    "turnId/sequence고유,sequence오름차순,PROCESSING최대1,processingTurnId와실제turn일치",
    "now<scheduledEndAt, 당일 serviceDate, status=ACTIVE. 응답 직전 시간/유효 로그인/소유권/상태/연결 재검사. 수동 종료 또는 자정 이후 내용 노출 금지."
  ],
  "x-owner": "Part 2"
}
```

<a id="section-14-48"></a>
<a id="schema-endreceipt"></a>
### 14.48 EndReceipt

<!-- json-schema: EndReceipt -->
```json
{
  "type": "object",
  "properties": {
    "conversationId": {
      "type": "string",
      "format": "uuid"
    },
    "status": {
      "type": "string",
      "enum": [
        "ENDED"
      ]
    },
    "endedAt": {
      "type": "string",
      "format": "date-time",
      "pattern": "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\+09:00(?![\\s\\S])",
      "description": "공통 KST(+09:00), 초 정밀도."
    },
    "summaryStatus": {
      "type": "string",
      "enum": [
        "PENDING",
        "READY",
        "FAILED",
        "EMPTY"
      ]
    }
  },
  "required": [
    "conversationId",
    "status",
    "endedAt",
    "summaryStatus"
  ],
  "additionalProperties": false,
  "description": "수동/자정 종료 완료 확인4필드. endRequestedAt 이전 bound_at<end_requested_at인 기존 유효 세션 연결에만 실제 endedAt+600초 복구권한을 기록. 현재 로그인·소유권·연결을 항상 재검사. 새 로그인/새 연결 복구 불가, now>=기한 거부, 반복 요청 연장 없음. endedAt은 drain 뒤 실제 종료시각이며 소급 없음. summaryStatus만 후속 변경 가능. 과거 내용·음성·요약 본문 없음.",
  "x-owner": "Part 2"
}
```

<a id="section-14-49"></a>
<a id="schema-childhomeresponse"></a>
### 14.49 ChildHomeResponse

<!-- json-schema: ChildHomeResponse -->
```json
{
  "type": "object",
  "properties": {
    "data": {
      "$ref": "#/components/schemas/ChildHome"
    }
  },
  "required": [
    "data"
  ],
  "additionalProperties": false,
  "x-owner": "Part 2"
}
```

<a id="section-14-50"></a>
<a id="schema-startconversationresultresponse"></a>
### 14.50 StartConversationResultResponse

<!-- json-schema: StartConversationResultResponse -->
```json
{
  "type": "object",
  "properties": {
    "data": {
      "$ref": "#/components/schemas/StartConversationResult"
    }
  },
  "required": [
    "data"
  ],
  "additionalProperties": false,
  "x-owner": "Part 2"
}
```

<a id="section-14-51"></a>
<a id="schema-resumeconversationresultresponse"></a>
### 14.51 ResumeConversationResultResponse

<!-- json-schema: ResumeConversationResultResponse -->
```json
{
  "type": "object",
  "properties": {
    "data": {
      "$ref": "#/components/schemas/ResumeConversationResult"
    }
  },
  "required": [
    "data"
  ],
  "additionalProperties": false,
  "x-owner": "Part 2"
}
```

<a id="section-14-56"></a>
<a id="schema-endreceiptresponse"></a>
### 14.56 EndReceiptResponse

<!-- json-schema: EndReceiptResponse -->
```json
{
  "type": "object",
  "properties": {
    "data": {
      "$ref": "#/components/schemas/EndReceipt"
    }
  },
  "required": [
    "data"
  ],
  "additionalProperties": false,
  "x-owner": "Part 2"
}
```

<a id="section-14-58"></a>
<a id="schema-activeconversationexistserror"></a>
### 14.58 ActiveConversationExistsError

<!-- json-schema: ActiveConversationExistsError -->
```json
{
  "type": "object",
  "properties": {
    "error": {
      "type": "object",
      "properties": {
        "code": {
          "type": "string",
          "enum": [
            "ACTIVE_CONVERSATION_EXISTS"
          ]
        },
        "message": {
          "type": "string"
        },
        "requestId": {
          "type": "string",
          "format": "uuid"
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
        },
        "activeConversationId": {
          "type": "string",
          "format": "uuid"
        }
      },
      "required": [
        "code",
        "message",
        "requestId",
        "fields",
        "activeConversationId"
      ],
      "additionalProperties": false
    }
  },
  "required": [
    "error"
  ],
  "additionalProperties": false,
  "description": "기존통합의시작경합응답유지. 다른키로당일ACTIVE가이미존재하면409+activeConversationId. 이ID만으로권한을주지않고명시resume한다. D02의회의답변은이용시간변경이므로오류선택A투표로해석하지않음.",
  "x-owner": "Part 2"
}
```

<a id="section-14-59"></a>
<a id="schema-conversationavailability"></a>
### 14.59 ConversationAvailability

<!-- json-schema: ConversationAvailability -->
```json
{
  "type": "object",
  "properties": {
    "timeZone": {
      "type": "string",
      "enum": [
        "Asia/Seoul"
      ]
    },
    "serverTime": {
      "type": "string",
      "format": "date-time",
      "pattern": "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\+09:00(?![\\s\\S])",
      "description": "공통 KST(+09:00), 초 정밀도."
    },
    "serviceDate": {
      "type": "string",
      "format": "date"
    },
    "opensAt": {
      "type": "string",
      "format": "date-time",
      "pattern": "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\+09:00(?![\\s\\S])",
      "description": "공통 KST(+09:00), 초 정밀도."
    },
    "closesAt": {
      "type": "string",
      "format": "date-time",
      "pattern": "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\+09:00(?![\\s\\S])",
      "description": "공통 KST(+09:00), 초 정밀도."
    },
    "canEnter": {
      "type": "boolean"
    },
    "reason": {
      "type": "string",
      "nullable": true,
      "enum": [
        null,
        "OUTSIDE_SERVICE_HOURS",
        "CONVERSATION_CLOSING"
      ]
    },
    "nextOpensAt": {
      "type": "string",
      "format": "date-time",
      "pattern": "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\+09:00(?![\\s\\S])",
      "description": "공통 KST(+09:00), 초 정밀도.",
      "nullable": true
    }
  },
  "required": [
    "timeZone",
    "serverTime",
    "serviceDate",
    "opensAt",
    "closesAt",
    "canEnter",
    "reason",
    "nextOpensAt"
  ],
  "additionalProperties": false,
  "description": "Home 시간표: serviceDate=serverTime의 KST 날짜, opensAt=당일08시, closesAt=다음00시. 00~08에는 canEnter=false, reason=OUTSIDE_SERVICE_HOURS, nextOpensAt=당일08시가 우선. 서비스 시간 안 CLOSING(이전 날짜 포함)이면 false/CONVERSATION_CLOSING/null. 진입 가능은 true/null/null. FE 시계만으로 권한을 허용하지 않는다.",
  "x-owner": "Part 2",
  "allOf": [
    {
      "oneOf": [
        {
          "properties": {
            "canEnter": {
              "enum": [
                true
              ]
            },
            "reason": {
              "nullable": true,
              "enum": [
                null
              ]
            },
            "nextOpensAt": {
              "nullable": true,
              "enum": [
                null
              ]
            }
          }
        },
        {
          "properties": {
            "canEnter": {
              "enum": [
                false
              ]
            },
            "reason": {
              "enum": [
                "OUTSIDE_SERVICE_HOURS"
              ]
            },
            "nextOpensAt": {
              "type": "string",
              "nullable": false
            }
          }
        },
        {
          "properties": {
            "canEnter": {
              "enum": [
                false
              ]
            },
            "reason": {
              "enum": [
                "CONVERSATION_CLOSING"
              ]
            },
            "nextOpensAt": {
              "nullable": true,
              "enum": [
                null
              ]
            }
          }
        }
      ]
    }
  ]
}
```

<a id="section-14-60"></a>
<a id="schema-endpendingreceipt"></a>
### 14.60 EndPendingReceipt

<!-- json-schema: EndPendingReceipt -->
```json
{
  "type": "object",
  "properties": {
    "conversationId": {
      "type": "string",
      "format": "uuid"
    },
    "status": {
      "type": "string",
      "enum": [
        "CLOSING"
      ]
    }
  },
  "required": [
    "conversationId",
    "status"
  ],
  "additionalProperties": false,
  "description": "종료 접수 후 기존 발화 정리 중인202의 확인정보2필드. Retry-After(1 이상) 후 동일 end를 재확인한다. 종료 경계 이전의 기존 유효 연결만 허용, 내용/음성/요약 없음.",
  "x-owner": "Part 2"
}
```

<a id="section-14-61"></a>
<a id="schema-endpendingreceiptresponse"></a>
### 14.61 EndPendingReceiptResponse

<!-- json-schema: EndPendingReceiptResponse -->
```json
{
  "type": "object",
  "properties": {
    "data": {
      "$ref": "#/components/schemas/EndPendingReceipt"
    }
  },
  "required": [
    "data"
  ],
  "additionalProperties": false,
  "x-owner": "Part 2"
}
```
