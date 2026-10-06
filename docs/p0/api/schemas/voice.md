# 음성 발화·처리 상태 JSON 스키마

[전체 API 목차](../../Integrated_API_Spec.md) · **담당: 각 API 담당 기준** · 계약 v3.1 / 문서 분할 2026-10-06

절 번호는 기존 통합 명세의 번호를 유지합니다. 다른 절은 전체 목차에서 찾을 수 있습니다.

61개 전체 스키마 중 이 기능에 해당하는 정의입니다. JSON `$ref`는 통합 OpenAPI의 components를 참조합니다.

## 이 문서에서 찾기

- [14.32 VoiceTurnRequest](#section-14-32)
- [14.35 P2ProcessingTurn](#section-14-35)
- [14.36 P2SucceededTurn](#section-14-36)
- [14.37 P2FailedTurn](#section-14-37)
- [14.38 P2TerminalTurn](#section-14-38)
- [14.40 TemporaryAudio](#section-14-40)
- [14.41 P2TemporaryAudioOrNull](#section-14-41)
- [14.42 ProcessingChildTurnResult](#section-14-42)
- [14.43 SucceededChildTurnResult](#section-14-43)
- [14.44 FailedChildTurnResult](#section-14-44)
- [14.45 ChildTurnResult](#section-14-45)
- [14.46 TerminalChildTurnResult](#section-14-46)
- [14.47 TurnAccepted](#section-14-47)
- [14.52 ChildTurnResultResponse](#section-14-52)
- [14.53 TerminalChildTurnResultResponse](#section-14-53)
- [14.54 SucceededChildTurnResultResponse](#section-14-54)
- [14.55 TurnAcceptedResponse](#section-14-55)

<a id="section-14-32"></a>
<a id="schema-voiceturnrequest"></a>
### 14.32 VoiceTurnRequest

<!-- json-schema: VoiceTurnRequest -->
```json
{
  "type": "object",
  "properties": {
    "clientRequestId": {
      "type": "string",
      "format": "uuid",
      "description": "multipart text part. 전송 재시도는 같은 키·같은 파일, 새 녹음은 새 UUID."
    },
    "audio": {
      "type": "string",
      "format": "binary",
      "description": "필수 음성 파일. 텍스트/버튼 발화는 받지 않는다."
    }
  },
  "required": [
    "clientRequestId",
    "audio"
  ],
  "additionalProperties": false,
  "x-owner": "Part 2",
  "description": "multipart audio+clientRequestId 두필드만. 실제 audio바이트 SHA-256 소문자hex64; 파일명/boundary/clientRequestId 제외. 실제파일디코딩검사. 의미있는추가처리옵션은P0없음. MIME/codec/용량/시간한도는D13/14대기."
}
```

<a id="section-14-35"></a>
<a id="schema-p2processingturn"></a>
### 14.35 P2ProcessingTurn

<!-- json-schema: P2ProcessingTurn -->
```json
{
  "allOf": [
    {
      "$ref": "#/components/schemas/TurnView"
    },
    {
      "properties": {
        "status": {
          "type": "string",
          "enum": [
            "PROCESSING"
          ]
        }
      }
    }
  ],
  "description": "Part1 공통 TurnView의 폐쇄객체·필드별 visibility/NULL·완료시각/errorCode oneOf를 그대로 재사용한다.",
  "x-owner": "Part 2"
}
```

<a id="section-14-36"></a>
<a id="schema-p2succeededturn"></a>
### 14.36 P2SucceededTurn

<!-- json-schema: P2SucceededTurn -->
```json
{
  "allOf": [
    {
      "$ref": "#/components/schemas/TurnView"
    },
    {
      "properties": {
        "status": {
          "type": "string",
          "enum": [
            "SUCCEEDED"
          ]
        }
      }
    }
  ],
  "description": "Part1 공통 TurnView의 폐쇄객체·필드별 visibility/NULL·완료시각/errorCode oneOf를 그대로 재사용한다.",
  "x-owner": "Part 2"
}
```

<a id="section-14-37"></a>
<a id="schema-p2failedturn"></a>
### 14.37 P2FailedTurn

<!-- json-schema: P2FailedTurn -->
```json
{
  "allOf": [
    {
      "$ref": "#/components/schemas/TurnView"
    },
    {
      "properties": {
        "status": {
          "type": "string",
          "enum": [
            "FAILED"
          ]
        }
      }
    }
  ],
  "description": "Part1 공통 TurnView의 폐쇄객체·필드별 visibility/NULL·완료시각/errorCode oneOf를 그대로 재사용한다.",
  "x-owner": "Part 2"
}
```

<a id="section-14-38"></a>
<a id="schema-p2terminalturn"></a>
### 14.38 P2TerminalTurn

<!-- json-schema: P2TerminalTurn -->
```json
{
  "oneOf": [
    {
      "$ref": "#/components/schemas/P2SucceededTurn"
    },
    {
      "$ref": "#/components/schemas/P2FailedTurn"
    }
  ],
  "x-owner": "Part 2"
}
```

<a id="section-14-40"></a>
<a id="schema-temporaryaudio"></a>
### 14.40 TemporaryAudio

<!-- json-schema: TemporaryAudio -->
```json
{
  "type": "object",
  "properties": {
    "url": {
      "type": "string",
      "description": "인증된 상대 GET /api/v1/children/{childId}/conversations/{conversationId}/turns/{turnId}/audio. 공개 영구 URL 아님.",
      "pattern": "^/api/v1/children/[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}/conversations/[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}/turns/[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}/audio(?![\\s\\S])"
    },
    "contentType": {
      "type": "string",
      "description": "합의할 실제 음성 MIME. 예시 audio/wav는 확정값 아님.",
      "minLength": 1,
      "pattern": "^audio/[^\\s]+(?:[ \\t]*;[^\\r\\n]*)?(?![\\s\\S])"
    },
    "expiresAt": {
      "type": "string",
      "format": "date-time",
      "pattern": "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\+09:00(?![\\s\\S])",
      "description": "공통 KST(+09:00), 초 정밀도."
    }
  },
  "required": [
    "url",
    "contentType",
    "expiresAt"
  ],
  "additionalProperties": false,
  "description": "별도 음성 허용·당일 ACTIVE·08~24·현재 연결·AVAILABLE·now<expiresAt에서만 제공. CLOSING 또는 자정 이후 drain 결과도 아이에게 URL/바이너리 전달 금지. 금지/미생성/만료는 JSON audio=null. MIME/TTL은 D13/D14 대기.",
  "x-owner": "Part 2"
}
```

<a id="section-14-41"></a>
<a id="schema-p2temporaryaudioornull"></a>
### 14.41 P2TemporaryAudioOrNull

<!-- json-schema: P2TemporaryAudioOrNull -->
```json
{
  "oneOf": [
    {
      "$ref": "#/components/schemas/TemporaryAudio"
    },
    {
      "type": "object",
      "nullable": true,
      "enum": [
        null
      ]
    }
  ],
  "x-owner": "Part 2"
}
```

<a id="section-14-42"></a>
<a id="schema-processingchildturnresult"></a>
### 14.42 ProcessingChildTurnResult

<!-- json-schema: ProcessingChildTurnResult -->
```json
{
  "type": "object",
  "properties": {
    "turn": {
      "$ref": "#/components/schemas/P2ProcessingTurn"
    },
    "audio": {
      "type": "object",
      "nullable": true,
      "enum": [
        null
      ]
    },
    "topicSuggestions": {
      "type": "array",
      "items": {
        "type": "string"
      },
      "description": "D09: P0 주제추천미지원, 항상빈배열[]. DB저장없음. AI자체대화진행과FE추천선택UI는별개.",
      "maxItems": 0
    }
  },
  "required": [
    "turn",
    "audio",
    "topicSuggestions"
  ],
  "additionalProperties": false,
  "x-owner": "Part 2"
}
```

<a id="section-14-43"></a>
<a id="schema-succeededchildturnresult"></a>
### 14.43 SucceededChildTurnResult

<!-- json-schema: SucceededChildTurnResult -->
```json
{
  "type": "object",
  "properties": {
    "turn": {
      "$ref": "#/components/schemas/P2SucceededTurn"
    },
    "audio": {
      "$ref": "#/components/schemas/P2TemporaryAudioOrNull"
    },
    "topicSuggestions": {
      "type": "array",
      "items": {
        "type": "string"
      },
      "description": "D09: P0 주제추천미지원, 항상빈배열[]. DB저장없음. AI자체대화진행과FE추천선택UI는별개.",
      "maxItems": 0
    }
  },
  "required": [
    "turn",
    "audio",
    "topicSuggestions"
  ],
  "additionalProperties": false,
  "x-owner": "Part 2",
  "description": "처리성공. 음성금지/미생성/만료는audio=null가능하나TTS실제장애는FAILED로분류(D10A)."
}
```

<a id="section-14-44"></a>
<a id="schema-failedchildturnresult"></a>
### 14.44 FailedChildTurnResult

<!-- json-schema: FailedChildTurnResult -->
```json
{
  "type": "object",
  "properties": {
    "turn": {
      "$ref": "#/components/schemas/P2FailedTurn"
    },
    "audio": {
      "type": "object",
      "nullable": true,
      "enum": [
        null
      ],
      "description": "D10: 실패결과는음성미제공. TTS실패시허용된텍스트만보존."
    },
    "topicSuggestions": {
      "type": "array",
      "items": {
        "type": "string"
      },
      "description": "D09: P0 주제추천미지원, 항상빈배열[]. DB저장없음. AI자체대화진행과FE추천선택UI는별개.",
      "maxItems": 0
    }
  },
  "required": [
    "turn",
    "audio",
    "topicSuggestions"
  ],
  "additionalProperties": false,
  "x-owner": "Part 2",
  "description": "STT무음STT_NO_SPEECH,AI/TTS장애AI_UPSTREAM_FAILED,기한초과AI_TIMEOUT 등실패. 허용텍스트는실패단계에따라보존, audio=null,topicSuggestions=[]."
}
```

<a id="section-14-45"></a>
<a id="schema-childturnresult"></a>
### 14.45 ChildTurnResult

<!-- json-schema: ChildTurnResult -->
```json
{
  "oneOf": [
    {
      "$ref": "#/components/schemas/ProcessingChildTurnResult"
    },
    {
      "$ref": "#/components/schemas/SucceededChildTurnResult"
    },
    {
      "$ref": "#/components/schemas/FailedChildTurnResult"
    }
  ],
  "x-owner": "Part 2"
}
```

<a id="section-14-46"></a>
<a id="schema-terminalchildturnresult"></a>
### 14.46 TerminalChildTurnResult

<!-- json-schema: TerminalChildTurnResult -->
```json
{
  "oneOf": [
    {
      "$ref": "#/components/schemas/SucceededChildTurnResult"
    },
    {
      "$ref": "#/components/schemas/FailedChildTurnResult"
    }
  ],
  "x-owner": "Part 2"
}
```

<a id="section-14-47"></a>
<a id="schema-turnaccepted"></a>
### 14.47 TurnAccepted

<!-- json-schema: TurnAccepted -->
```json
{
  "type": "object",
  "properties": {
    "turnId": {
      "type": "string",
      "format": "uuid"
    },
    "clientRequestId": {
      "type": "string",
      "format": "uuid"
    },
    "status": {
      "type": "string",
      "enum": [
        "PROCESSING"
      ]
    },
    "statusUrl": {
      "type": "string",
      "description": "현재 turnId의 인증된 상태 GET 상대 경로. POST 응답에서만202; 같은 PROCESSING을 GET하면200 ChildTurnResult.",
      "pattern": "^/api/v1/children/[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}/conversations/[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}/turns/[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}(?![\\s\\S])"
    }
  },
  "required": [
    "turnId",
    "clientRequestId",
    "status",
    "statusUrl"
  ],
  "additionalProperties": false,
  "x-owner": "Part 2"
}
```

<a id="section-14-52"></a>
<a id="schema-childturnresultresponse"></a>
### 14.52 ChildTurnResultResponse

<!-- json-schema: ChildTurnResultResponse -->
```json
{
  "type": "object",
  "properties": {
    "data": {
      "$ref": "#/components/schemas/ChildTurnResult"
    }
  },
  "required": [
    "data"
  ],
  "additionalProperties": false,
  "x-owner": "Part 2"
}
```

<a id="section-14-53"></a>
<a id="schema-terminalchildturnresultresponse"></a>
### 14.53 TerminalChildTurnResultResponse

<!-- json-schema: TerminalChildTurnResultResponse -->
```json
{
  "type": "object",
  "properties": {
    "data": {
      "$ref": "#/components/schemas/TerminalChildTurnResult"
    }
  },
  "required": [
    "data"
  ],
  "additionalProperties": false,
  "x-owner": "Part 2"
}
```

<a id="section-14-54"></a>
<a id="schema-succeededchildturnresultresponse"></a>
### 14.54 SucceededChildTurnResultResponse

<!-- json-schema: SucceededChildTurnResultResponse -->
```json
{
  "type": "object",
  "properties": {
    "data": {
      "$ref": "#/components/schemas/SucceededChildTurnResult"
    }
  },
  "required": [
    "data"
  ],
  "additionalProperties": false,
  "x-owner": "Part 2"
}
```

<a id="section-14-55"></a>
<a id="schema-turnacceptedresponse"></a>
### 14.55 TurnAcceptedResponse

<!-- json-schema: TurnAcceptedResponse -->
```json
{
  "type": "object",
  "properties": {
    "data": {
      "$ref": "#/components/schemas/TurnAccepted"
    }
  },
  "required": [
    "data"
  ],
  "additionalProperties": false,
  "x-owner": "Part 2"
}
```
