# 아이 프로필

[전체 API 목차](../Integrated_API_Spec.md) · **담당: Part 1** · 계약 v3.1 / 문서 분할 2026-10-06

절 번호는 기존 통합 명세의 번호를 유지합니다. 다른 절은 전체 목차에서 찾을 수 있습니다.

## 이 문서에서 찾기

- [4.6 CreateChildRequest](#section-4-6)
- [6.9 아이 프로필 목록](#section-6-9)
- [6.10 아이 등록](#section-6-10)

<a id="section-4-6"></a>
### 4.6 CreateChildRequest

[CreateChildRequest](schemas/children.md#schema-createchildrequest) · `application/json`

이름 name(보호자용)과 애칭 nickname(아이용)을 별도 필수 입력. name trim 후1\~5, nickname trim 후1\~20 Unicode 코드포인트, 빈 값/null 거부. 관심사 trim, birthDate는 KST 오늘 이하, gender 및 characterId 카탈로그 기준 유지. 별도 애칭 유일성·실명 인증 없음.

| 필드 | 필수 | 타입·검증 |
| --- | --- | --- |
| name | 예 | type="string"; 정규화 후 type="string"; minLength=1; maxLength=5 전송 문자열. trim 후1\~5 Unicode 코드포인트·내부 공백 보존, 빈 이름 거부. B-04.1 D06 입력 기준 확정. |
| birthDate | 예 | type="string"; format="date" 유효 YYYY-MM-DD·KST 오늘 이하, 최저 연령 추가 없음. B-04.1 D06 입력 기준 확정. |
| gender | 예 | type="string"; enum=["MALE", "FEMALE"] MALE/FEMALE 2종만 허용, 미지정값 없음. B-04.1 D06 입력 기준 확정. |
| interests | 아니오 | type="array"; maxItems=10; default=[]; 정규화 후 type="array"; maxItems=10 자유 입력0\~10개·각 trim 후1\~30 Unicode 코드포인트·동일값 중복 거부·순서 보존. 요청 생략 시[]·text[] 유지. B-04.2 D06 입력 기준 확정. |
| characterId | 예 | type="string"; minLength=1; maxLength=64; pattern="^[A-Za-z0-9_-]+(?![\\s\\S])" 영문·숫자·_·-로1\~64자, 지원 목록에 없는 ID400 VALIDATION_FAILED. B-04.3 D06 입력 기준 확정. 실제 목록/기본값/폐기 ID/카탈로그 소유·AI 대응은 J-04 미결, dodam은 예시. |
| nickname | 예 | type="string"; minLength=1; pattern="\\S"; 정규화 후 type="string"; minLength=1; maxLength=20 trim 후1\~20 Unicode 코드포인트. 아이 화면·인사·AI 호칭용 애칭, null/빈 문자열 거부. 유일성/실명 인증 없음. 20자는 통합 기술 기준. |

normal 요청 예시:

<!-- json-example: CreateChildRequest -->
```json
{
  "name": "도담",
  "birthDate": "2020-05-12",
  "gender": "FEMALE",
  "interests": [
    "공룡",
    "그림"
  ],
  "characterId": "dodam",
  "nickname": "도담이"
}
```

<a id="section-6-9"></a>
<a id="operation-listchildren"></a>
### 6.9 아이 프로필 목록

`GET /api/v1/children` · `listChildren` · **담당 Part 1** · A-04 · 권한 G

세션 계정 기준 0\~1개. accountId 입력 없음. 빈 결과는 200. PIN 불필요.

요청 body: 없음.

| HTTP | 본문·오류 코드 |
| --- | --- |
| 200 | [ChildrenListResponse](schemas/children.md#schema-childrenlistresponse) · 성공.  |
| 401 | AUTHENTICATION_REQUIRED · AUTHENTICATION_REQUIRED; 발생 조건 및 재시도는 operation 설명과 기능 명세 참조. |
| 503 | AUTH_STATE_UNAVAILABLE · AUTH_STATE_UNAVAILABLE; 발생 조건 및 재시도는 operation 설명과 기능 명세 참조. |
| 429 | RATE_LIMITED · RATE_LIMITED; 발생 조건 및 재시도는 operation 설명과 기능 명세 참조. |

응답 추가 헤더: 429 `Retry-After`: 최소 재시도 대기 초. 실제 남은 차단시간으로 계산.

트랜잭션·재시도: 읽기 전용. 동일 요청 반복 가능.

DB 읽기: `children, session_security, accounts`; 쓰기: ``. 공통 제한 예약과 프레임워크 세션 저장은 별도 공통 처리다.

200 응답 예시 (normal):

<!-- json-example: ChildrenListResponse -->
```json
{
  "data": {
    "items": [
      {
        "childId": "22222222-2222-4222-8222-222222222222",
        "name": "도담",
        "birthDate": "2020-05-12",
        "gender": "FEMALE",
        "interests": [
          "공룡",
          "그림"
        ],
        "characterId": "dodam",
        "createdAt": "2026-09-30T10:00:00+09:00",
        "nickname": "도담이"
      }
    ]
  }
}
```

나머지 1개 상태 예시는 OpenAPI의 이 operation에 함께 제공한다.

<a id="section-6-10"></a>
<a id="operation-createchild"></a>
### 6.10 아이 등록

`POST /api/v1/children` · `createChild` · **담당 Part 1** · A-04 · 권한 G+X

name은 보호자용 이름, nickname은 아이용 호칭으로 별도 필수 입력·저장. 각각 trim 후1\~5/1\~20 Unicode 코드포인트. null/빈 애칭 거부, 별도 유일성·실명 인증 없음. interests trim, birthDate 미래 거부, gender/character 카탈로그 검증. accountId는 세션에서. 아이1명 동시 등록 UNIQUE 충돌409. Location은 식별 URI이며 단건 GET 추가 아님. 응답 유실은 목록 조회.

| 위치 | 필드 | 필수 | 타입·기본값·검증 |
| --- | --- | --- | --- |
| header | X-XSRF-TOKEN | 예 | type="string"; minLength=1 GET auth/csrf 응답 token. 누락/불일치는 403. |

요청 body: `application/json` [CreateChildRequest](schemas/children.md#schema-createchildrequest)

| HTTP | 본문·오류 코드 |
| --- | --- |
| 201 | [ChildViewResponse](schemas/children.md#schema-childviewresponse) · 성공.  |
| 401 | AUTHENTICATION_REQUIRED · AUTHENTICATION_REQUIRED; 발생 조건 및 재시도는 operation 설명과 기능 명세 참조. |
| 400 | VALIDATION_FAILED · VALIDATION_FAILED; 발생 조건 및 재시도는 operation 설명과 기능 명세 참조. |
| 403 | CSRF_INVALID · CSRF_INVALID; 발생 조건 및 재시도는 operation 설명과 기능 명세 참조. |
| 413 | REQUEST_TOO_LARGE · REQUEST_TOO_LARGE; B-02.9 공유안의 모든 Part 1 JSON 본문 최대16384바이트(16KiB), 공백/escape 포함 UTF-8 실제 본문 기준.16384는 크기 검사 통과·16385부터413이며 CSRF/로그인 검사보다 먼저 판정. 기존 기술 기준안·D17 실운영값/검증 보류. |
| 415 | UNSUPPORTED_MEDIA_TYPE · UNSUPPORTED_MEDIA_TYPE; 발생 조건 및 재시도는 operation 설명과 기능 명세 참조. |
| 503 | AUTH_STATE_UNAVAILABLE · AUTH_STATE_UNAVAILABLE; 발생 조건 및 재시도는 operation 설명과 기능 명세 참조. |
| 409 | CHILD_LIMIT_REACHED · CHILD_LIMIT_REACHED; 발생 조건 및 재시도는 operation 설명과 기능 명세 참조. |
| 429 | RATE_LIMITED · RATE_LIMITED; 발생 조건 및 재시도는 operation 설명과 기능 명세 참조. |

응답 추가 헤더: 201 `Location`: 생성 리소스 식별 URI. 별도 GET 지원을 의미하지 않는다.; 429 `Retry-After`: 최소 재시도 대기 초. 실제 남은 차단시간으로 계산.

트랜잭션·재시도: children insert, UNIQUE(account_id)로 동시 제한.

DB 읽기: `accounts, session_security`; 쓰기: `children`. 공통 제한 예약과 프레임워크 세션 저장은 별도 공통 처리다.

201 응답 예시 (normal):

<!-- json-example: ChildViewResponse -->
```json
{
  "data": {
    "childId": "22222222-2222-4222-8222-222222222222",
    "name": "도담",
    "birthDate": "2020-05-12",
    "gender": "FEMALE",
    "interests": [
      "공룡",
      "그림"
    ],
    "characterId": "dodam",
    "createdAt": "2026-09-30T10:00:00+09:00",
    "nickname": "도담이"
  }
}
```
