# 오류·수치·미정 항목·검증

[전체 API 목차](../Integrated_API_Spec.md) · **담당: 공통 / Part 1·Part 2** · 계약 v3.1 / 문서 분할 2026-10-06

절 번호는 기존 통합 명세의 번호를 유지합니다. 다른 절은 전체 목차에서 찾을 수 있습니다.

## 이 문서에서 찾기

- [9 오류와 FE 복구](#section-9)
- [10 수치와 미정 계약](#section-10)
- [11 AI 자료와 구현 후 인수 기준](#section-11)
- [12 스키마 관리와 검증 기록](#section-12)
- [12.1 실행 기록과 재실행](#section-12-1)

<a id="section-9"></a>
## 9. 오류와 FE 복구

오류 envelope는 closed ApiError다. ACTIVE_CONVERSATION_EXISTS만 별도 closed schema로 activeConversationId를 추가한다. 일반 ApiError에 임의 필드를 넣지 않는다. requestId는 서버 추적 UUID이며 clientRequestId와 다르다. 메시지/fields에 민감 원문을 복사하지 않는다.

| 상태·코드 | 처리 |
| --- | --- |
|400 VALIDATION_FAILED / VERIFICATION_INVALID | 입력·purpose·token 상태를 바로잡음. 구조 오류와 일회성 권한 실패 구분 |
|401 AUTHENTICATION_REQUIRED | 로그인 복귀. 이전 변경 요청을 무조건 재실행하지 않음 |
|403 CSRF_INVALID | 토큰 재획득 후 사용자가 의도한 업무의 현재 상태부터 확인 |
|403 PIN_SETUP_AUTHORIZATION_REQUIRED / PIN_RESET_AUTHORIZATION_REQUIRED | 해당 목적의 현재 세션 이메일 발급부터 재인증 |
|403 GUARDIAN_UNLOCK_REQUIRED | 현재 세션 PIN 확인. 다른 세션 성공을 복사하지 않음 |
|403 CHILD_SESSION_REQUIRED | 당일08\~24 ACTIVE만 명시 resume. ENDED는 새 세션 복구 불가 |
|403 SERVICE_HOURS_CLOSED | 아이 입력/내용/음성 차단·Home 이용시간 표시 |
|404 RESOURCE_NOT_FOUND | 부재·타인·리소스 조합 오류를 구분 노출하지 않음 |
|409 ACTIVE_CONVERSATION_EXISTS | activeConversationId로 당일 resume |
|409 CONVERSATION_ENDED | 해당 종료 대화의 내용 복원·같은 시작 키 재사용 불가. 서비스 시간 안 새 키로 새 대화 가능 |
|409 CONVERSATION_CLOSING | 입력·본문·음성 차단, 새 시작 대기. 기존 적격 연결은 동일 end 재확인, 새 로그인은 Home 재조회 |
|202 EndPendingReceipt | Retry-After≥1 후 동일 end를 재확인. 완료200 뒤 홈으로 이동, 본문 복원 없음 |
|409 TURN_IN_PROGRESS | 새 발화 키 접수와 기존 PROCESSING 충돌. 이미 접수된 키 유지 |
|409 IDEMPOTENCY_CONFLICT | 같은 키에 다른 파일. 기존 요청 복구 또는 새 녹음에 새 키 |
|409 PIN_ALREADY_SET / PIN_NOT_SET / CHILD_LIMIT_REACHED / ACCOUNT_ALREADY_EXISTS | 실제 계정 상태에 맞는 설정·로그인·목록 흐름 사용 |
|409 PASSWORD_RESET_NOT_AVAILABLE | Google-only는 Google 로그인 안내, 새 password credential 자동 생성 없음 |
|410 AUDIO_EXPIRED | 재생 불가 안내. 재생성 요청 없음 |
|413 REQUEST_TOO_LARGE / AUDIO_TOO_LARGE | 해당 크기 기준에 맞춘 입력·새 녹음. 업로드 한도 D13 대기 |
|415 UNSUPPORTED_MEDIA_TYPE / AUDIO_FORMAT_UNSUPPORTED | 요청 Content-Type과 실제 디코딩 가능한 형식 확인 |
|422 STT_NO_SPEECH | 이미 FAILED 저장된 최초 동기 결과. 다시 녹음하면 새 키 |
|429 RATE_LIMITED | Retry-After 이후 재시도. 폐기된 challenge는 대기로 살아나지 않음 |
|502 AI_UPSTREAM_FAILED /504 AI_TIMEOUT | 접수 turn은FAILED. TTS실패도audio=null·허용텍스트만. 같은 키는 저장된 결과 |
|503 AUTH_STATE_UNAVAILABLE / EMAIL_DELIVERY_UNAVAILABLE | 보안 상태 확인/메일 전달 장애. 권한 우회 없이 복구 안내 |

발화 접수202이면 나중에 HTTP를422/502/504로 바꾸지 않는다. 이용시간·권한 안의 결과 GET200에서 turn.status=FAILED와 errorCode를 읽는다. response를 잃은 FE는 clientRequestId로 상태를 확인하되404만으로 아직 진행 중인 POST의 미접수를 단정하지 않는다. 실제 endpoint별 허용 오류 목록은 §6\~7/OpenAPI가 기준이다.


<a id="section-10"></a>
## 10. 수치와 미정 계약

공통의 목록 page=0/size=20/max100/q≤100을 유지한다. 아래 표에서 D04·D06·D03·D05로 표시한 값은 확정했다. 나머지 인증/운영 공유안은 D17에 따라 **초기 기술 기준**으로 유지하며 실운영 확정과 구분한다. 필요한 값이 비어 있으면 무제한·0으로 대체하지 않고 해당 기능 운영 설정을 완료한다.

| 설정 | 값·기산점 | 원문 상태·주체 |
| --- | --- | --- |
| EMAIL_CODE_LENGTH | 6자리 ASCII 숫자 | Part 1 B-02.3 공유안 |
| EMAIL_CODE_TTL / VERIFICATION_TOKEN_TTL | 각각600초, 발급/검증 성공 시각부터; now≥expiry 거부 | Part 1 B-02.3 공유안 |
| EMAIL_RESEND_INTERVAL | 이전 발급부터60초, 정확히60초부터 다른 조건 충족 시 허용 | Part 1 B-02.3 공유안 |
| EMAIL_SEND_SUBJECT / IP | 5회/20회, 각900초 고정창, IP는 모든 목적 합산 | Part 1 B-02.5 공유안 |
| EMAIL_VERIFY_SUBJECT / IP | 10회/100회, 각900초 고정창, IP는 challenge 조회 전 예약 | Part 1 B-02.6 공유안 |
| EMAIL_CODE_MAX_ATTEMPTS | 실제 오답5회,1\~4회400,5회 폐기 커밋+429; 기다려도 challenge 복구 없음 | Part 1 B-02.4 공유안 |
| LOGIN_EMAIL / IP | 10회/100회, 각900초 고정창, 성공/실패 사전 예약 | Part 1 B-02.7 공유안, Google에는 별도 전역 제한 |
| PIN_UNLOCK_ACCOUNT / IP | 20회/100회, 각900초 고정창, 성공 포함 | Part 1 B-02.8 공유안 |
| PIN_FAILURE_LIMIT/WINDOW/BLOCK_TTL | 최근900초 실제 오답5회,5번째 판정부터900초 차단; 집계 (t-900초,t] | Part 1 B-02.2 공유안, 성공/재로그인 초기화 없음 |
| API_IP_LIMIT / WINDOW | 300회/60초 고정창, Part1/Part2/OAuth/auth-me/polling/재시도 합산 | Part 1 B-08.2 초기 공유안, FE 호출량·공유 IP·운영 실측 대기 |
| PART1_JSON_MAX_BYTES | 16,384바이트 통과·16,385부터413 REQUEST_TOO_LARGE | Part 1 B-02.9 공유안, 음성 제한과 별개 |
| 일반 로그인 / JSESSIONID | 서비스 idle/absolute 없음, DB expiry=null; 쿠키365일=31,536,000초 유효 사용 갱신 | D04 확정, 실제 framework 검증 필요 |
| GUARDIAN_UNLOCK_TTL | PIN 성공부터 최대1,800초, 조회/활동 연장 없음 | D04 확정, 대시보드 이탈 lock |
| END_RECOVERY_TTL | 실제 종료 커밋의 ended_at부터600초; equality 거부 | F03 확정·수동/자정 동일·기존 저장기한 재계산 없음 |
| PASSWORD_MIN/MAX_LENGTH | 새 생성15\~128 Unicode 코드포인트, 로그인 소급 없음 | Part 1 B-01.2 공유안 |
| name / nickname / gender / interests / characterId | name1\~5·nickname1\~20 trim 후 코드포인트 / MALE,FEMALE / 0\~10개 각1\~30 / ASCII ID1\~64+카탈로그 | F06 별도 입력 확정·nickname20자는 통합 기술 기준, 나머지 D06 유지 |
| 보호자 상세 size | 기본/최대100턴, afterSequence 기본0 | D05A 확정; 실제 성능 시험 별도 |
| CHALLENGE_RETENTION / RATE_LIMIT_RETENTION / SESSION_SECURITY_RETENTION | 권한·효력 종료 후 추가 보관0초, 공통 정리 주기600초 | Part 1 B-08.3\~.5 공유안.0초는 정리 후보 시점, 즉시 삭제/삭제 SLA 아님 |
| SMTP connect/read/write timeout | 양의 유한 수치 미정 | Part 1/메일 운영 측정 필요 |
| READ_MODEL_TEXT_MAX_LENGTH | 문자열별 실제 한도 미정, 임의 절단 없음 | Part 2/AI·Part 1 |


D13·D14 자료 대기: TURN_PROCESSING_TIMEOUT, SUMMARY_PROCESSING_TIMEOUT, TEMP_AUDIO_TTL, 동기 대기시간, 음성 MIME/codec/sample rate/채널/길이/바이트, AI 문맥·텍스트·요약 한도, 실제 AI 오류 매핑, 임시 음성 만료 메타데이터 보관. 시간값은 양의 유한 값으로 확정하며 미정은0/무제한이 아니다. STT/TTS의 FE-facing 상태는 D10으로 이미 확정했고, AI 제공자 wire를 그 상태에 매핑하는 자료만 남았다. 원본 음성·영구 텍스트·AI 문맥의 보관기간은 인증 자료0초/600초 정책과 별개다.

<a id="section-11"></a>
## 11. AI 자료와 구현 후 인수 기준

Part 2가 AI 팀의 실제 endpoint/auth·정상/오류 schema·필드별 저장/표시 허용·음성 별도 허용·시간/용량 자료를 받아 매핑한다. requestId(추적), conversationId, turnId(논리 작업), locale, 최소 아이 문맥과 허용된 이전 텍스트만 연계한다. ChildContext는 nickname을 포함하며 AI 호칭은 nickname을 매핑하고 실명 name을 자동 전송하지 않는다. 실제 wire 필드명은 D13에 따른다. FE 고정 시작 인사는 AI·TTS·발화 저장/개수·요약 입력에 넣지 않는다. AI 내부 위험 신호를 FE DTO에 그대로 넣지 않는다. 허용 여부 누락은 비노출이며 성공 mock을 실제 AI 계약 확정으로 취급하지 않는다.

구현 후 시험은 FE·BE 전달서 §6(별도 전달)의 시간 경계·두 탭 동시 생성·동일 날짜 재시작·화면 이탈/재로그인·수동/자정 접수 경쟁·다중 세션 복구·PIN reset 경합·100턴 pagination·음성404/410·실제 브라우저/AI·세션 저장 장애를 따른다. 특히 로그인/PIN 무효화와 수동 종료/자정이 AI 대기 도중 발생해도 결과를 전달하지 않는지 확인한다. DB 제약과 문서 검증만으로 애플리케이션 동시성·보안 필터를 검증했다고 주장하지 않는다.

<a id="section-12"></a>
## 12. 스키마 관리와 검증 기록

OpenAPI3.0.3의61개 Schema Object와 §14 JSON 정의는 같은 계약이다. nullable/oneOf/allOf·닫힌 객체·서버 정규화 검사를 함께 적용한다. 26개 operation의 담당·업무·입출력·오류·읽기/쓰기·Tx를 명시하며 실제 배포 주소는 미정이다.

스키마 밖에서도 로그인·소유권·PIN·시각, 상태/경계/사유 조합, sequence 정렬·cursor·processingTurnId·Home 메뉴/availability·URL/turnId 일치가 필수다. 검증 스크립트는 수동 종료·drain·기한 불변·늦은 callback·미종료1개·내용 차단·600초 equality·새 로그인·EMPTY·첫 인사 제외를 문서 fixture로 검사한다. 실제 서버 잠금·HTTP·AI 호출 구현 시험은 아니다.

<a id="section-12-1"></a>
### 12.1 실행 기록과 재실행

v3 OpenAPI 구조·예시·정상/거부·서비스 의미·정규화·종료 trace 검사는 `support/validate_contract.py --api-only`로 실행했다. 문서/DB 사전·DTO·도식까지 포함한 최종 수치는 전체 검증 출력과 [변경 기록](../Decision_Record.md)을 확인한다. 과거 v2 결과를 v3 통과 기록으로 재사용하지 않는다. 원문9개 중 현재 접근 가능한3개 SHA256을 검증했으며 나머지6개와 원문 Part1 schema 대조는 경로 부재로 미실행이다.

재실행: [문서·스키마 검사](../support/validate_contract.py), [DDL 검사](../support/validate_erd.sql), [검증 및 도식 안내](../README.md). PyYAML/openapi-spec-validator/openapi-schema-validator 및 Playwright/Chrome/Mermaid는 검증 환경용이며 제품 의존성을 추가하지 않는다.

```sh
python support/validate_contract.py --api-only
python support/validate_contract.py
```

Mermaid를 바꾸면 렌더 후 manifest의 원문 해시도 확인한다. 실제 서버/운영 DB 이관·다중 인스턴스·세션 저장·FE 녹음/재생·AI 허용/오류·파일 삭제·부하·운영 배포는 별도 검증이다. D13/D14 AI 형식·필드별 허용·수치와 D17 운영값은 자료를 받은 뒤 관련 schema/config/경계 예제를 함께 갱신한다.
