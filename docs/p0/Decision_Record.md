# P0 확정 정책 및 문서 변경 기록

2026-10-03 KST · v3.1 · 사용자 F-01\~F-08 답변 반영

## 1. 확정 정책

| 결정 | 반영 내용 |
| --- | --- |
| 대화 기본 | KST 08:00\~24:00. 화면 이탈은 종료가 아니며 기존 ACTIVE 대화와 허용 기록을 유지한다. 이야기 마치기는 종료 후 홈으로 이동한다. 같은 날 다시 시작하면 새 ID의 대화다. |
| F-01 | 수동 종료 접수 즉시 새 입력을 막는다. 접수한 발화만 원래 처리기한까지 마무리하고 허용 결과를 저장한 뒤 종료한다. |
| F-02 | 종료 처리까지 새 대화를 막되 요약 완료는 기다리지 않는다. 아이당 미종료 대화는 하나다. |
| F-03 | 종료 경계 이전에 연결된 유효 로그인만 실제 endedAt부터 600초 동안 종료 확인 정보만 복구한다. 수동·자정 종료 모두 적용하며 재요청은 기한을 연장하지 않는다. 새 로그인에는 부여하지 않는다. |
| F-04 | 발화가 없는 종료 대화도 EMPTY 기록으로 남긴다. 요약 AI를 호출하지 않는다. |
| F-05 A | 첫 인사는 FE 고정 문구다. AI 호출·TTS·발화 저장·요약 입력·발화 수에 포함하지 않는다. |
| F-06 | 아이 이름과 애칭을 별도 입력·저장한다. 이름은 보호자용 정보, 애칭은 아이 화면·인사·AI 호칭으로 사용한다. |
| F-07 | 동의와 추가 보호자 정보 수집은 P0 제외 유지. 동의 API·테이블·필수 가입 단계·완료 여부를 이번 계약에 추가하지 않는다. 법적 보호자 확인을 완료했다고 표시하지 않는다. |
| F-08 | 기존 P0 범위 유지: 이메일·Google, 아이 1명 등록/조회, 음성 대화, PIN 보호 대화 기록. Kakao·다자녀·프로필 수정/삭제·선택지/문자 입력·사진·놀이·보상·꾸미기·감정 분석·알림·리포트 내보내기는 제외한다. |

기존 D01\~D18 중 위 결정으로 대체된 부분 외에는 유지한다. D13/D14 AI 실제 wire·필드별 허용·시간/용량과 D17 운영 환경값은 여전히 자료 대기이며 임의 확정하지 않는다. 최초 문서 작성 단계에서는 GitHub 게시를 제외했으며, 이후 사용자 승인으로 문서만 저장소에 게시한다.

## 2. 통합 구현 계약

다음은 확정 정책을 구현하기 위한 기술 설계이며 사용자가 직접 선택한 수치로 기록하지 않는다.

- `conversations.status`: ACTIVE → CLOSING → ENDED. 처리 중 발화가 없으면 짧은 트랜잭션에서 곧바로 ENDED 확정 가능.
- `end_requested_at timestamptz NULL`, `end_reason`(MANUAL/MIDNIGHT, 초기 NULL). 최초 종료 경계를 고정하며 중복 end나 자정 도달로 덮어쓰지 않는다. 수동 종료는 잠금 후 DB 시각, 자정 종료는 scheduled_end_at을 경계로 사용한다. 자정 이후 처음 종료를 관측하면 MIDNIGHT로 처리한다.
- ACTIVE의 종료 필드는 NULL. CLOSING은 end_requested_at/end_reason 필수, ended_at NULL. ENDED는 모두 필수이며 ended_at >= end_requested_at >= started_at. MANUAL의 경계는 scheduled_end_at 미만, MIDNIGHT의 경계는 scheduled_end_at과 같다.
- `(child_id, service_date)` UNIQUE 제거. `(child_id, client_request_id)` 유지. `status IN ('ACTIVE','CLOSING')`인 행에 child_id 부분 UNIQUE를 적용한다. 지난 날짜의 미종료 행도 정리하기 전 새 대화를 만들지 않는다.
- 수동 종료와 음성 접수는 같은 conversation 잠금/조건부 갱신으로 직렬화. 종료가 먼저면 접수 거부; 발화가 먼저면 원래 deadline까지 drain. CLOSING은 아이용 본문·음성·resume 차단, 기존 작업의 허용 결과 저장만 허용한다. late callback은 terminal 결과를 덮어쓰지 않는다.
- `POST .../end`: body 0바이트·CSRF·로그인·소유권·기존 연결. 종료 진행 중 202 `EndPendingReceipt {conversationId,status:"CLOSING"}` + Retry-After(1 이상). 종료 완료 200 기존 네 필드 `EndReceipt {conversationId,status:"ENDED",endedAt,summaryStatus}`. 별도 poll 경로 없이 동일 end를 재확인하며 경계/복구기한을 늘리지 않는다. PIN 불필요.
- 자정/수동 종료 경계 이전의 `bound_at < end_requested_at`인 유효 연결만 CLOSING 확인 및 ENDED 600초 확인 가능. CLOSING에는 새 연결을 만들지 않는다. 로그인 무효화는 항상 우선한다. 확인 응답에 과거 내용·음성·요약 본문 없음.
- `/start`: 서비스 시간 안에 기존 ACTIVE이면 기존 ACTIVE_CONVERSATION_EXISTS 계약, CLOSING이면 409 CONVERSATION_CLOSING. 기존 PREVIOUS_CONVERSATION_CLOSING은 CONVERSATION_CLOSING으로 통합하고 DAILY_CONVERSATION_CLOSED는 제거한다. 당일 ENDED 존재만으로 거부하지 않는다. 새 대화는 새 clientRequestId, 재전송은 기존 키를 유지한다. 동일 키의 ENDED 재전송은 CONVERSATION_ENDED로 거부하며 새 대화를 만들지 않는다.
- `/home`: 접근 가능한 당일 ACTIVE만 activeConversationId에 반환. 서비스 시간의 CLOSING은 ID=null, canEnter=false, reason=CONVERSATION_CLOSING. 시간 밖은 OUTSIDE_SERVICE_HOURS 우선. 새 로그인은 복구권한 없이 Home으로 이용 가능 여부만 확인한다. 시간 밖 종료 완료 추정은 금지하며 opensAt부터 재확인한다. closingConversationId는 추가하지 않는다.
- `/resume`, 발화 POST/GET, 임시 음성 GET은 CLOSING에 409 CONVERSATION_CLOSING. 자정 이후 시간 차단 규칙도 유지한다. 조회·장시간 요청의 응답 직전 상태/권한을 재검사한다.
- 최초 ENDED 전이만 PENDING/EMPTY를 확정하고 요약 최대 1회 시도. EMPTY는 허용 요약 입력이 없을 때이며 빈 대화도 포함. 이전 대화 요약 중 새 대화 가능, 결과는 원래 conversationId에만 저장. 보호자 기록은 ENDED만, 시간+ID 정렬 유지.
- 아이 이름 `name`: 기존 trim 후 1\~5 Unicode 코드포인트 유지. 신규 애칭 `nickname`: 필수, trim 후 1\~20 코드포인트(통합 기술 기준), null/빈 문자열 거부. 별도 유일성·실명 인증 없음. ChildView/CreateChildRequest/Home.child/내부 ChildContext에 nickname 포함. AI에는 호칭 nickname을 매핑하며 실명 name을 자동 전송하지 않는다. 실제 AI 필드명은 D13에 따름.
- FE 고정 시작 인사: "{nickname}, 오늘 만나서 반가워!" 등 고정 템플릿. Home.greeting도 nickname을 사용하는 서버 고정 문구다.
- 기존 데이터 migration은 nullable nickname 추가→name 복사 backfill→검증→NOT NULL. 기존 ENDED는 end_reason=MIDNIGHT/end_requested_at=scheduled_end_at으로 현재 v2 데이터 전제에 맞춰 backfill하되 실제 데이터가 전제에 맞지 않으면 중단·분류하고 임의 시각을 만들지 않는다.

## 3. 최종 검토 범위

API·OpenAPI JSON 스키마와 예시, ERD 사전·DDL·migration·검증, Common/Part1/Part2, FE/AI/FE·BE 전달서, Mermaid·SVG·HTML·PDF를 함께 맞춘다. 기존 하루 1대화·자정 전 종료 거부·애칭 없음 규칙은 대체된다. Figma 파일은 이번에 편집하지 않으며 화면 변경 요청은 FE 전달서에 기록한다.

Jira는 기존 업무의 관련 범위와 수용 기준을 우선 보완하고 새 작업은 실제 공백이 있을 때만 생성한다. 담당자·상태·스프린트·일정은 임의 변경하지 않는다. 후속 승인에 따른 문서 게시 PR은 Jira를 연결하지 않으며, 구현 업무 완료로 취급하지 않는다.

## 4. v3.1 전면 재검토 결과

2026-10-03에 10개 문서·OpenAPI·DDL·JSON·다이어그램을 교차 검토했다. 새 정책 선택을 요구하지 않는 계약 오류 5건을 수정했다.

| 검토 항목 | 반영 |
| --- | --- |
| 시간 밖 Home | CLOSING과 ENDED를 구분할 수 없으므로 종료 완료로 추정하지 않음. 현재 이용 가능 여부만 안내하고 opensAt부터 재확인 |
| 발화 응답 유실 | 원래 POST 접수보다 복구 GET 404가 먼저 올 수 있음. 같은 키·파일 보존 및 한도 내 확인, 자동 새 키·재녹음 전환 금지 |
| 입력·응답 스키마 | 44개 정규식의 끝 개행 허용을 엄격한 끝 검사로 수정. MIME의 중간 CRLF도 거부. 기존 비밀번호·일반 텍스트 정책은 유지 |
| 기록 상세 예시 | sequence 1\~4의 처리 시각을 순차 배치하여 PROCESSING 최대 1개 계약과 일치 |
| 오류 코드 | CHILD_ALREADY_EXISTS 오기를 실제 CHILD_LIMIT_REACHED로 정정 |

ERD 구조·마이그레이션·잠금/종료 계약은 추가 결함이 발견되지 않아 유지했다. 스키마·JSON·경계 회귀 검사를 다시 통과했으며 SQL DDL은 이전 검증본과 동일하다. 실제 HTTP/FE/AI 통합 시험 및 운영 DB 이관은 별도 개발 작업이다. 원문 9개 중 현재 접근 가능한 3개는 해시 검증했고, 부재한 6개는 원문 재대조를 했다고 주장하지 않는다. 기존 v2 스키마 비교는 위 정규식 오류 수정만 정규화해 나머지 구조 보존을 검사했다.
