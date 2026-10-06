# DodamDodam P0 통합 API 명세

2026-10-03 KST · v3.1 · F-01\~F-08 확정 답변 반영 최종 설계

**한국시간 08:00부터 자정 전까지 대화하며, 같은 날에도 종료 후 새 대화를 시작할 수 있다.** 화면 이탈·새로고침은 ACTIVE를 유지한다. 이야기 마치기는 수동 종료를 접수하고 새 입력과 모든 아이 내용 접근을 즉시 차단한다. 기존 발화가 있으면 CLOSING에서 원래 기한까지 마무리하고 ENDED 뒤 홈으로 이동한다. 자정도 같은 종료 흐름을 사용한다. 아이당 ACTIVE/CLOSING 합계는 최대1개이며 요약 완료는 새 시작을 막지 않는다. 종료 경계 전에 연결된 유효 로그인만 실제 종료 후600초 동안 확인정보를 복구한다.

이 명세는 Part 1의 인증·아이·PIN·보호자 기록과 Part 2의 Home·음성·대화를 통합한 **24개 경로·26개 operation·61개 스키마**다. 각 API에 담당을 명시한다. 이메일 PIN 재설정과 최소 가입 흐름을 포함하며, 주제 추천은 P0에서 사용하지 않는다.

함께 사용할 문서: [확정 결정 기록](Decision_Record.md), [통합 ERD](Integrated_ERD_Design.md), FE·BE 개발 전달서(별도 전달), [기계 판독 OpenAPI 3.0.3](Integrated_OpenAPI.yaml), [API 다이어그램](api/09-diagrams.md).

최종이라는 말은 **사용자가 결정한 범위의 설계가 반영됐다는 뜻**이다. D13·D14의 AI 실제 형식·수치와 D17 운영 환경값은 자료를 받은 뒤 채운다. 이 문서는 서버 구현·브라우저·AI 통합 시험 완료를 의미하지 않는다. 별도 승인 절차는 추가하지 않는다(D18).

**2026-10-06 문서 구성 변경:** 기존 계약 v3.1을 기능별 문서와 JSON 스키마로 나눴다. API 경로·필드·정책·담당은 유지한다. JSON의 `$ref`는 [통합 OpenAPI](Integrated_OpenAPI.yaml)의 components를 참조한다.

v3.1 재검토: Home 시간외 상태와 종료 receipt 구분, 응답 유실 GET 404 경합, 문자열 끝 개행 거부, 순차 발화 예시, CHILD_LIMIT_REACHED 오류 코드를 바로잡았다. 기존 비밀번호 원문 정책은 변경하지 않았다.

## 기능별 문서

공통 규칙을 확인한 뒤 구현할 기능으로 이동한다. 요청·응답 예시와 상세 스키마는 각 링크로 연결된다.

| 문서 | 담당 |
| --- | --- |
| [근거·결정·담당과 전체 경로](api/00-overview.md) | 공통 / Part 1·Part 2 |
| [공통 HTTP·세션·공유 응답](api/01-common.md) | 공통 / Part 1 제공, Part 2 적용 |
| [이메일·Google 인증과 계정](api/02-auth.md) | Part 1 |
| [아이 프로필](api/03-children.md) | Part 1 |
| [보호자 PIN과 권한](api/04-guardian.md) | Part 1 |
| [보호자 대화 기록](api/05-records.md) | Part 1 API / Part 2 읽기 모델 |
| [홈·대화 시작·이어하기·종료](api/06-conversations.md) | Part 2 |
| [음성 발화·비동기 복구·임시 음성](api/07-voice.md) | Part 2 |
| [오류·수치·미정 항목·검증](api/08-errors-and-validation.md) | 공통 / Part 1·Part 2 |
| [통합 Mermaid 다이어그램](api/09-diagrams.md) | 공통 / Part 1·Part 2 |

## JSON 스키마

| 분류 | 정의 수 |
| --- | --- |
| [공통 오류](api/schemas/common.md) | 1 |
| [인증·계정](api/schemas/auth.md) | 14 |
| [아이 프로필](api/schemas/children.md) | 5 |
| [보호자 PIN](api/schemas/guardian.md) | 5 |
| [공유 기록·발화 읽기 모델](api/schemas/records.md) | 6 |
| [홈·대화·종료](api/schemas/conversations.md) | 13 |
| [음성 발화·처리 상태](api/schemas/voice.md) | 17 |

## 기존 절·직접 링크 찾아가기

기존 `Integrated_API_Spec.md#operation-...` 및 `#schema-...` 링크는 아래에서 새 위치로 이동할 수 있다.

- [1 근거와 결정 상태](api/00-overview.md#section-1)
- [2 담당과 전체 경로](api/00-overview.md#section-2)
- [3 공통 HTTP와 세션 계약](api/01-common.md#section-3)
- [3.1 내부 함수와 무효화](api/01-common.md#section-3-1)
- [3.2 원문 구현 계약의 공통 보완](api/01-common.md#section-3-2)
- [3.3 PIN_RESET 권한과 오류 경계](api/01-common.md#section-3-3)
<a id="section-4"></a>
## 4. 요청 객체와 입력 검증

전체 필드 정의는 §14 JSON 스키마에 있다. `required`는 키 존재 여부, `nullable`은 명시적 null 허용이다. 요청에 선언하지 않은 필드는400이며 아래 요청 DTO는 null을 허용하지 않는다. 전송 문자열과 정규화 후 검증을 구분한다. `x-normalized-schema`는 서버가 실행할 검증이며 생성 도구가 자동으로 수행한다고 가정하지 않는다.

이메일은 trim+lower, 이름·애칭·관심사·검색어는 trim하고 내부 공백을 유지한다. 비밀번호·PIN·코드·token은 trim·숫자변환·Unicode 정규화·절단하지 않는다. 새 비밀번호15\~128 코드포인트 기준은 기존 기술 기준이며 로그인 기존 비밀번호에 소급하지 않는다. 아이 이름1\~5, 애칭 nickname1\~20 코드포인트(별도 필수), 성별MALE/FEMALE, 관심사0\~10개·각1\~30, 캐릭터 ASCII ID1\~64 및 서버 카탈로그 검사(D06). 생일은 실제 날짜이며 KST 오늘 이하다.

- [4.1 IssueEmailVerificationRequest](api/02-auth.md#section-4-1)
- [4.2 VerifyEmailCodeRequest](api/02-auth.md#section-4-2)
- [4.3 SignupRequest](api/02-auth.md#section-4-3)
- [4.4 LoginRequest](api/02-auth.md#section-4-4)
- [4.5 PasswordResetRequest](api/02-auth.md#section-4-5)
- [4.6 CreateChildRequest](api/03-children.md#section-4-6)
- [4.7 GuardianPinSetupRequest](api/04-guardian.md#section-4-7)
- [4.8 GuardianUnlockRequest](api/04-guardian.md#section-4-8)
- [4.9 GuardianPinResetRequest](api/04-guardian.md#section-4-9)
- [4.10 StartConversationRequest](api/06-conversations.md#section-4-10)
- [4.11 VoiceTurnRequest](api/07-voice.md#section-4-11)
- [5 응답 객체](api/01-common.md#section-5)
- [5.1 Part 1 인증과 프로필](api/01-common.md#section-5-1)
- [5.2 공유 ConversationView](api/01-common.md#section-5-2)
- [5.3 공유 TurnView](api/01-common.md#section-5-3)
- [5.4 보호자와 아이 응답 wrapper](api/01-common.md#section-5-4)
- [5.5 상태와 NULL 조합](api/01-common.md#section-5-5)
<a id="section-6"></a>
## 6. Part 1 API 상세

공통 오류·헤더는 §3을 적용한다. 아래 표는 각 경로에 선언된 모든 HTTP 상태다. 예시의 ID·시각은 가상 데이터이며 서로 다른 시나리오는 독립적이다.

<a id="operation-getcsrf"></a>
- [6.1 CSRF 토큰 획득](api/02-auth.md#section-6-1)
<a id="operation-issueemailverification"></a>
- [6.2 목적별 인증번호 발급 및 재발급](api/02-auth.md#section-6-2)
<a id="operation-verifyemailcode"></a>
- [6.3 인증번호 검증 및 토큰 발급](api/02-auth.md#section-6-3)
<a id="operation-signup"></a>
- [6.4 이메일 회원가입](api/02-auth.md#section-6-4)
<a id="operation-login"></a>
- [6.5 이메일 로그인](api/02-auth.md#section-6-5)
<a id="operation-getcurrentaccount"></a>
- [6.6 현재 계정 및 진입 상태](api/02-auth.md#section-6-6)
<a id="operation-logout"></a>
- [6.7 로그아웃](api/02-auth.md#section-6-7)
<a id="operation-resetpassword"></a>
- [6.8 비밀번호 재설정](api/02-auth.md#section-6-8)
<a id="operation-listchildren"></a>
- [6.9 아이 프로필 목록](api/03-children.md#section-6-9)
<a id="operation-createchild"></a>
- [6.10 아이 등록](api/03-children.md#section-6-10)
<a id="operation-setupguardianpin"></a>
- [6.11 최초 보호자 PIN 설정](api/04-guardian.md#section-6-11)
<a id="operation-unlockguardian"></a>
- [6.12 보호자 PIN 확인](api/04-guardian.md#section-6-12)
<a id="operation-lockguardian"></a>
- [6.13 보호자 잠금 및 아이 모드 전환](api/04-guardian.md#section-6-13)
<a id="operation-resetguardianpin"></a>
- [6.14 이메일 재인증으로 보호자 PIN 재설정](api/04-guardian.md#section-6-14)
<a id="operation-listchildconversations"></a>
- [6.15 날짜별 대화 기록 및 키워드 검색](api/05-records.md#section-6-15)
<a id="operation-getchildconversation"></a>
- [6.16 대화 상세와 전체 발화 페이지](api/05-records.md#section-6-16)
<a id="operation-startgooglelogin"></a>
- [6.17 Google 로그인 시작](api/02-auth.md#section-6-17)
<a id="operation-completegooglelogin"></a>
- [6.18 Google 로그인 콜백](api/02-auth.md#section-6-18)
<a id="section-7"></a>
## 7. Part 2 API 상세

공통 오류·헤더는 §3을 적용한다. 아래 표는 각 경로에 선언된 모든 HTTP 상태다. 예시의 ID·시각은 가상 데이터이며 서로 다른 시나리오는 독립적이다.

<a id="operation-startchildconversation"></a>
- [7.1 대화 시작](api/06-conversations.md#section-7-1)
<a id="operation-getchildhome"></a>
- [7.2 아이 홈 조회](api/06-conversations.md#section-7-2)
<a id="operation-resumechildconversation"></a>
- [7.3 활성 대화 이어하기](api/06-conversations.md#section-7-3)
<a id="operation-submitvoiceturn"></a>
- [7.4 음성 발화 전송](api/07-voice.md#section-7-4)
<a id="operation-getvoiceturn"></a>
- [7.5 발화 ID로 상태 조회](api/07-voice.md#section-7-5)
<a id="operation-getvoiceturnbyrequestid"></a>
- [7.6 요청 ID로 상태 조회](api/07-voice.md#section-7-6)
<a id="operation-endchildconversation"></a>
- [7.7 이야기 마치기 및 종료 결과 재확인](api/06-conversations.md#section-7-7)
<a id="operation-gettemporaryturnaudio"></a>
- [7.8 임시 응답 음성 재접근](api/07-voice.md#section-7-8)
- [8 수동·자정 종료와 같은 날 재시작 규칙](api/06-conversations.md#section-8)
- [8.1 시간 판정과 상태별 접근](api/06-conversations.md#section-8-1)
- [8.2 생성·재진입·시작 키](api/06-conversations.md#section-8-2)
- [8.3 종료 접수·기존 발화 정리·요약](api/06-conversations.md#section-8-3)
- [8.4 종료 확인 복구와 음성](api/06-conversations.md#section-8-4)
- [9 오류와 FE 복구](api/08-errors-and-validation.md#section-9)
- [10 수치와 미정 계약](api/08-errors-and-validation.md#section-10)
- [11 AI 자료와 구현 후 인수 기준](api/08-errors-and-validation.md#section-11)
- [12 스키마 관리와 검증 기록](api/08-errors-and-validation.md#section-12)
- [12.1 실행 기록과 재실행](api/08-errors-and-validation.md#section-12-1)
- [13 통합 API 다이어그램](api/09-diagrams.md#section-13)
- [13.1 담당과 데이터 경계](api/09-diagrams.md#section-13-1)
- [13.2 요청 검사와 공통 오류 우선순위](api/09-diagrams.md#section-13-2)
- [13.3 이메일 번호 확인과 일회성 업무 권한](api/09-diagrams.md#section-13-3)
- [13.4 로그인·프로필·아이 홈 진입과 로그아웃](api/09-diagrams.md#section-13-4)
- [13.5 PIN 설정·확인·잠금과 이메일 재설정](api/09-diagrams.md#section-13-5)
- [13.6 비밀번호 재설정과 모든 이전 로그인 무효화](api/09-diagrams.md#section-13-6)
- [13.7 새 대화 시작과 화면 재진입 복원](api/09-diagrams.md#section-13-7)
- [13.8 음성 발화 접수와 동기·비동기 결과](api/09-diagrams.md#section-13-8)
- [13.9 발화 중복 키와 응답 유실 복구](api/09-diagrams.md#section-13-9)
- [13.10 수동·자정 종료와 기존 세션의 종료 확인](api/09-diagrams.md#section-13-10)
- [13.11 임시 응답 음성의 시간·권한·수명 검사](api/09-diagrams.md#section-13-11)
- [13.12 텍스트·요약·음성의 개별 공개 허용](api/09-diagrams.md#section-13-12)
- [13.13 보호자 목록 검색과 전체 발화 이어 읽기](api/09-diagrams.md#section-13-13)
- [13.14 26개 API 동작과 도식 매핑](api/09-diagrams.md#section-13-14)
<a id="section-14"></a>
<a id="integrated-json-schemas"></a>
## 14. 전체 스키마의 JSON 정의

61개 OpenAPI3.0.3 Schema Object다. 요청·응답 예시와 구분하며 같은 OpenAPI components의 `$ref`를 사용한다. required/null/상태/닫힌 객체 및 서버 정규화 제약을 함께 적용한다.

<a id="schema-apierror"></a>
- [14.1 ApiError](api/schemas/common.md#section-14-1)
<a id="schema-csrftokenresponse"></a>
- [14.2 CsrfTokenResponse](api/schemas/auth.md#section-14-2)
<a id="schema-accountview"></a>
- [14.3 AccountView](api/schemas/auth.md#section-14-3)
<a id="schema-childview"></a>
- [14.4 ChildView](api/schemas/children.md#section-14-4)
<a id="schema-conversationview"></a>
- [14.5 ConversationView](api/schemas/records.md#section-14-5)
<a id="schema-turnview"></a>
- [14.6 TurnView](api/schemas/records.md#section-14-6)
<a id="schema-verificationissue"></a>
- [14.7 VerificationIssue](api/schemas/auth.md#section-14-7)
<a id="schema-verificationgrant"></a>
- [14.8 VerificationGrant](api/schemas/auth.md#section-14-8)
<a id="schema-signupresult"></a>
- [14.9 SignupResult](api/schemas/auth.md#section-14-9)
<a id="schema-guardianunlock"></a>
- [14.10 GuardianUnlock](api/schemas/guardian.md#section-14-10)
<a id="schema-childrenlist"></a>
- [14.11 ChildrenList](api/schemas/children.md#section-14-11)
<a id="schema-conversationpage"></a>
- [14.12 ConversationPage](api/schemas/records.md#section-14-12)
<a id="schema-conversationdetail"></a>
- [14.13 ConversationDetail](api/schemas/records.md#section-14-13)
<a id="schema-issueemailverificationrequest"></a>
- [14.14 IssueEmailVerificationRequest](api/schemas/auth.md#section-14-14)
<a id="schema-verifyemailcoderequest"></a>
- [14.15 VerifyEmailCodeRequest](api/schemas/auth.md#section-14-15)
<a id="schema-signuprequest"></a>
- [14.16 SignupRequest](api/schemas/auth.md#section-14-16)
<a id="schema-loginrequest"></a>
- [14.17 LoginRequest](api/schemas/auth.md#section-14-17)
<a id="schema-passwordresetrequest"></a>
- [14.18 PasswordResetRequest](api/schemas/auth.md#section-14-18)
<a id="schema-guardianpinsetuprequest"></a>
- [14.19 GuardianPinSetupRequest](api/schemas/guardian.md#section-14-19)
<a id="schema-guardianunlockrequest"></a>
- [14.20 GuardianUnlockRequest](api/schemas/guardian.md#section-14-20)
<a id="schema-createchildrequest"></a>
- [14.21 CreateChildRequest](api/schemas/children.md#section-14-21)
<a id="schema-accountviewresponse"></a>
- [14.22 AccountViewResponse](api/schemas/auth.md#section-14-22)
<a id="schema-childviewresponse"></a>
- [14.23 ChildViewResponse](api/schemas/children.md#section-14-23)
<a id="schema-verificationissueresponse"></a>
- [14.24 VerificationIssueResponse](api/schemas/auth.md#section-14-24)
<a id="schema-verificationgrantresponse"></a>
- [14.25 VerificationGrantResponse](api/schemas/auth.md#section-14-25)
<a id="schema-signupresultresponse"></a>
- [14.26 SignupResultResponse](api/schemas/auth.md#section-14-26)
<a id="schema-guardianunlockresponse"></a>
- [14.27 GuardianUnlockResponse](api/schemas/guardian.md#section-14-27)
<a id="schema-childrenlistresponse"></a>
- [14.28 ChildrenListResponse](api/schemas/children.md#section-14-28)
<a id="schema-conversationpageresponse"></a>
- [14.29 ConversationPageResponse](api/schemas/records.md#section-14-29)
<a id="schema-conversationdetailresponse"></a>
- [14.30 ConversationDetailResponse](api/schemas/records.md#section-14-30)
<a id="schema-startconversationrequest"></a>
- [14.31 StartConversationRequest](api/schemas/conversations.md#section-14-31)
<a id="schema-voiceturnrequest"></a>
- [14.32 VoiceTurnRequest](api/schemas/voice.md#section-14-32)
<a id="schema-childhome"></a>
- [14.33 ChildHome](api/schemas/conversations.md#section-14-33)
<a id="schema-startconversationresult"></a>
- [14.34 StartConversationResult](api/schemas/conversations.md#section-14-34)
<a id="schema-p2processingturn"></a>
- [14.35 P2ProcessingTurn](api/schemas/voice.md#section-14-35)
<a id="schema-p2succeededturn"></a>
- [14.36 P2SucceededTurn](api/schemas/voice.md#section-14-36)
<a id="schema-p2failedturn"></a>
- [14.37 P2FailedTurn](api/schemas/voice.md#section-14-37)
<a id="schema-p2terminalturn"></a>
- [14.38 P2TerminalTurn](api/schemas/voice.md#section-14-38)
<a id="schema-resumeconversationresult"></a>
- [14.39 ResumeConversationResult](api/schemas/conversations.md#section-14-39)
<a id="schema-temporaryaudio"></a>
- [14.40 TemporaryAudio](api/schemas/voice.md#section-14-40)
<a id="schema-p2temporaryaudioornull"></a>
- [14.41 P2TemporaryAudioOrNull](api/schemas/voice.md#section-14-41)
<a id="schema-processingchildturnresult"></a>
- [14.42 ProcessingChildTurnResult](api/schemas/voice.md#section-14-42)
<a id="schema-succeededchildturnresult"></a>
- [14.43 SucceededChildTurnResult](api/schemas/voice.md#section-14-43)
<a id="schema-failedchildturnresult"></a>
- [14.44 FailedChildTurnResult](api/schemas/voice.md#section-14-44)
<a id="schema-childturnresult"></a>
- [14.45 ChildTurnResult](api/schemas/voice.md#section-14-45)
<a id="schema-terminalchildturnresult"></a>
- [14.46 TerminalChildTurnResult](api/schemas/voice.md#section-14-46)
<a id="schema-turnaccepted"></a>
- [14.47 TurnAccepted](api/schemas/voice.md#section-14-47)
<a id="schema-endreceipt"></a>
- [14.48 EndReceipt](api/schemas/conversations.md#section-14-48)
<a id="schema-childhomeresponse"></a>
- [14.49 ChildHomeResponse](api/schemas/conversations.md#section-14-49)
<a id="schema-startconversationresultresponse"></a>
- [14.50 StartConversationResultResponse](api/schemas/conversations.md#section-14-50)
<a id="schema-resumeconversationresultresponse"></a>
- [14.51 ResumeConversationResultResponse](api/schemas/conversations.md#section-14-51)
<a id="schema-childturnresultresponse"></a>
- [14.52 ChildTurnResultResponse](api/schemas/voice.md#section-14-52)
<a id="schema-terminalchildturnresultresponse"></a>
- [14.53 TerminalChildTurnResultResponse](api/schemas/voice.md#section-14-53)
<a id="schema-succeededchildturnresultresponse"></a>
- [14.54 SucceededChildTurnResultResponse](api/schemas/voice.md#section-14-54)
<a id="schema-turnacceptedresponse"></a>
- [14.55 TurnAcceptedResponse](api/schemas/voice.md#section-14-55)
<a id="schema-endreceiptresponse"></a>
- [14.56 EndReceiptResponse](api/schemas/conversations.md#section-14-56)
<a id="schema-guardianpinresetrequest"></a>
- [14.57 GuardianPinResetRequest](api/schemas/guardian.md#section-14-57)
<a id="schema-activeconversationexistserror"></a>
- [14.58 ActiveConversationExistsError](api/schemas/conversations.md#section-14-58)
<a id="schema-conversationavailability"></a>
- [14.59 ConversationAvailability](api/schemas/conversations.md#section-14-59)
<a id="schema-endpendingreceipt"></a>
- [14.60 EndPendingReceipt](api/schemas/conversations.md#section-14-60)
<a id="schema-endpendingreceiptresponse"></a>
- [14.61 EndPendingReceiptResponse](api/schemas/conversations.md#section-14-61)
