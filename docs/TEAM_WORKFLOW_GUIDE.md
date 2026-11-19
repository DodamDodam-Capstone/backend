# Backend 팀 업무 실행 가이드

이 문서는 Backend 팀원이 Jira 또는 GitHub에서 업무를 시작해
`development` PR을 병합하고 완료 상태를 확인할 때 그대로 따라 하는 가이드입니다.

## 1. 이 저장소를 사용하는 업무

- Jira: `dodamdodam.atlassian.net` / 프로젝트 `SCRUM`
- Jira/GitHub 접두어: `[BE]`
- GitHub 저장소: `DodamDodam-Capstone/backend`
- 일반 PR 대상: `development`
- 필수 CI: `backend-quality`, `gitmoji-conventional-title`, `jira-issue-key`
- 알림 채널: `#backend-actions`

API, domain, database, 인증·인가, 외부 시스템 연결, Backend 테스트와 관련된
업무를 이 저장소에서 처리합니다. Frontend·AI·통합 변경이 함께 필요하면 같은
Epic 아래에 `[FE]`, `[AI]`, `[INT]` Task를 별도로 만듭니다.

업무 표기는 Epic `[EPIC] <사용자 가치 또는 목표>`, Backend Task `[BE] <구현할 결과>`, GitHub Issue `SCRUM-<번호> [BE] <같은 제목>`을 사용합니다. GitHub-first는 Jira 업무와 GitHub Issue를 1:1로 연결합니다.

## 2. 시작 경로를 먼저 선택합니다

| 상황 | 작업 방법 |
| --- | --- |
| Jira `[BE]` Task가 이미 있음 | Jira 키로 바로 branch와 PR을 만듭니다. GitHub Task Form은 열지 않습니다. |
| Jira Task가 없음 | GitHub의 `Backend Task` 또는 `Backend Bug` Form으로 Jira 업무를 자동 생성합니다. |

이미 있는 Jira Task와 GitHub Issue Form을 함께 사용하면 Jira Task가 중복 생성될
수 있습니다. 한 업무에는 한 가지 시작 경로만 사용합니다.

Story/Task의 포인트를 입력하기 전에 [Jira Story Point 입력 기준 공유안](STORY_POINTS.md)을 확인합니다.
점수별 설명, 프로젝트 예시와 중복 집계·완료량 기준은 이 문서에서 관리합니다.

## 3. Jira-first: Jira Task가 이미 있는 경우

예시 계획:

```text
Epic: SCRUM-200 [EPIC] 회원 인증 흐름 제공
Task: SCRUM-202 [BE] 로그인 및 토큰 재발급 API
```

### 3.1 작업 브랜치 생성

```bash
git switch development
git pull --ff-only
git switch -c feature/SCRUM-202-auth-api
```

### 3.2 commit과 PR 작성

```text
commit: ✨ feat(auth): SCRUM-202 [BE] 토큰 재발급 API 추가
PR:     ✨ feat(auth): SCRUM-202 [BE] 토큰 재발급 API 추가
base:   development
```

PR 본문에는 Jira 링크를 적습니다. GitHub Issue를 만들지 않은 Jira-first 업무에는
`Resolves #번호`를 넣지 않습니다.

```text
Jira: https://dodamdodam.atlassian.net/browse/SCRUM-202
관련 GitHub Issue: 없음 (Jira-first 업무)
```

### 3.3 병합 후 결과

1. 필수 CI와 리뷰를 통과합니다.
2. PR을 `development`에 squash merge합니다.
3. Jira의 Development 영역에 branch, commit, PR, build가 표시됩니다.
4. Jira Automation이 `SCRUM-202`를 `완료`로 전환합니다.
5. `#backend-actions`에서 source, target, PR, commit, actor, 결과를 확인합니다.

이 경로에는 GitHub Issue가 없으므로 Jira Task만 완료됩니다.

## 4. GitHub-first: GitHub Issue에서 Jira Task를 만드는 경우

### 4.1 Issue Form 작성

`New issue`에서 `Backend Task`를 선택합니다.

```text
제목: [BE] 로그인 시도 제한 정책 추가
상위 Jira 키: SCRUM-200
완료 목표: 반복 로그인 실패 요청을 제한한다.
완료 조건:
- [ ] 제한 정책과 오류 응답 구현
- [ ] 단위·통합 테스트 통과
- [ ] development 대상 PR 준비
```

Issue를 연 뒤 Jira 키가 바로 붙지 않으면 팀원이 유형과 내용을 검토하고
`jira-sync` 레이블을 추가합니다. 이 레이블만으로 자동화가 Jira Task를 만들고
제목을 다음과 같이 변경합니다.

```text
backend#123 SCRUM-207 [BE] 로그인 시도 제한 정책 추가
```

자동 결과:

- Jira `SCRUM-207` Task 생성 및 `SCRUM-200` Epic의 child로 연결
- GitHub Issue에 Jira 링크 댓글 추가
- `jira-linked` 레이블 추가
- Slack 전송 성공 시 `jira-notified` 레이블 추가
- `#backend-actions`에 GitHub Issue → Jira 연결 결과 전송

### 4.2 자동 생성된 키로 작업

```text
branch: feature/SCRUM-207-login-rate-limit
commit: ✨ feat(auth): SCRUM-207 [BE] 로그인 시도 제한 추가
PR:     ✨ feat(auth): SCRUM-207 [BE] 로그인 시도 제한 추가
base:   development
```

PR 본문:

```text
Jira: https://dodamdodam.atlassian.net/browse/SCRUM-207
Resolves #123
```

`development` 병합 후 `close-linked-issues`가 `backend#123`을 닫고, Jira Automation이
`SCRUM-207`을 `완료`로 전환합니다.

### 4.3 자동화의 승인·중복 방지 기준

- 열린 직후 자동 생성은 GitHub `OWNER`, `MEMBER`, `COLLABORATOR`가 등록한 Issue에 적용합니다. 외부 작성자 및 `CONTRIBUTOR`/`NONE`은 팀원이 내용과 `task`/`bug` 유형을 확인한 뒤 `jira-sync`로 승인합니다.
- `jira-skip`은 생성을 건너뜁니다. 제목에 Jira 키가 있어도 해당 Issue 고유 Jira 레이블이 일치할 때만 기존 업무를 재사용합니다.
- 재실행은 Jira 업무와 링크 댓글을 중복 생성하지 않습니다. Slack 성공은 `jira-notified`로 표시합니다. Task·Bug 유형이 충돌하면 생성하지 않고 실패를 알립니다.
- secret을 사용하는 중앙 helper는 CI를 통과한 integration commit의 전체 SHA로 고정합니다.

## 5. Bug 업무 예시

```text
GitHub Form: Backend Bug
Issue: SCRUM-208 [BE] 만료된 refresh token이 허용되는 오류
branch: fix/SCRUM-208-reject-expired-refresh-token
commit: 🐛 fix(auth): SCRUM-208 [BE] 만료 refresh token 거부
PR: 🐛 fix(auth): SCRUM-208 [BE] 만료 refresh token 거부
```

재현 요청, 기대 응답, 실제 응답, 로그를 Issue에 작성하되 access token,
database credential, 개인정보는 첨부하지 않습니다.

## 6. PR 검증과 병합 규칙

- 일반 작업 branch와 PR 제목에는 같은 `SCRUM-번호`를 정확히 하나 넣습니다.
- `development` 대상 PR 제목에는 `[BE]`만 정확히 하나 넣습니다.
- Gitmoji는 의미에 맞게 자유롭게 선택하고 `feat`, `fix`, `docs`, `test` 등의
  Conventional Commit type을 사용합니다.
- GitHub Issue가 있을 때만 `development` 대상 작업 PR에 같은 저장소 Issue를
  `Resolves #번호`로 연결합니다.
- `backend-quality`와 모든 필수 검사를 통과합니다.
- 마지막 push를 하지 않은 다른 팀원의 승인을 받습니다.
- 모든 review conversation을 해결한 뒤 squash merge합니다.
- `development`와 `main`에는 직접 push 또는 force push하지 않습니다.
- `development` → `main`은 merge commit으로 계보를 유지합니다. 기존 계보가 갈라졌다면 commit을 삭제하지 않고 보호된 동기화 PR로 연결합니다.
- 자동 브랜치 삭제는 사용하지 않으며 작업 브랜치는 sprint 정리 시 수동 삭제합니다.
- PR 제목 형식은 `<gitmoji> <type>(optional-scope): <description>`입니다.
- `main` 대상 PR은 `development`에서만 만들며 긴급 수정도 같은 승격 경로를
  사용합니다.
- `main` 승격 제목은 `🚀 chore(release): [BE] v0.1.0 스프린트 릴리스`처럼
  VERSION과 같은 버전을 사용합니다. Jira 키는 필수가 아니며 Epic 완료와 무관합니다.
  생성물·보호 설정·재실행은 [Backend 릴리스 가이드](RELEASE_GUIDE.md)를 따릅니다.
- `main` 승격 PR에는 `Resolves #번호`, `Closes #번호`, `Fixes #번호`를
  적지 않습니다. GitHub는 기본 브랜치 병합 시 이 키워드로 Issue를 닫습니다.

<a id="review-policy"></a>
### SCRUM PR 리뷰어 조건 시범안

현재 모든 PR은 독립된 팀원 한 명의 승인이 필요합니다. `development`에서 인증·권한, DB·영속화, CI·정책 등 [보호 경로](https://github.com/DodamDodam-Capstone/backend/rules/27601021)를 변경하면 backend 팀원의 승인이 필요합니다. 시범 기간에는 CI가 판정 로직의 회귀 테스트를 실행합니다.

backend의 [Codex 저장소 설정](https://chatgpt.com/settings/code-review/repositories/github-1340757090)은 모든 PR을 열 때 자동 리뷰를 요청합니다. 수정 커밋을 올린 뒤 다시 검토받으려면 해당 PR의 일반 댓글에 `@codex review`를 남깁니다. 이전 커밋의 결과는 새 커밋의 검증 근거로 쓸 수 없습니다.

`#backend-actions`의 PR 알림에는 위험도, 판단 근거, 다음 행동, PR·커밋·검증 링크가 표시됩니다. CI 실행 결과와 공식 Codex 리뷰 완료를 각각 알립니다. 보호 경로 또는 main 릴리스는 높음, 일반 변경에 미해결 지적이 있으면 중간, 완료 조건이 확인된 일반 변경은 낮음입니다. 근거가 부족하면 미판정입니다. Codex가 지적 없이 마쳐도 Jira 완료 조건 판정은 별도로 확인합니다.

고위험 내부 PR은 공식 Codex가 최신 커밋의 리뷰를 완료하면 `sol-max-review` 체크에서 OpenAI Responses API의 `gpt-6.2-sol`을 `max`로 한 번 호출합니다. 보호 경로·main 릴리스 외에도 대화 턴 접수·결과 확정·종료 서비스와 Java 변경 구간에서 기존 메서드들의 호출 순서가 뒤바뀐 경우를 포함합니다. 호출 순서 탐지는 같은 변경 구간에서 식별되는 명시적 호출을 비교합니다. 다른 파일에 걸친 간접 호출은 팀원이 확인합니다.

2차 리뷰에는 PR 제목·본문과 변경 파일의 텍스트 패치를 전달합니다. 체크와 Slack에 위험 근거, 모델, 추론 강도, 커밋, 결과를 표시합니다. 지적이 없어도 고위험 PR은 팀원의 독립 승인이 필요합니다. 추가 push 후에는 PR에 `@codex review`를 남기면 1차 완료 후 같은 기준으로 다시 선별합니다.

설정은 기본 브랜치 `main`에 워크플로를 배포하고, `main`만 허용하는 GitHub Environment `sol-max-review`에 팀 프로젝트의 `OPENAI_API_KEY`를 등록합니다. 같은 PR·커밋의 유료 호출은 체크에 미리 기록하며 타임아웃에도 자동 재호출하지 않습니다. 입력 자료는 160,000바이트, 출력은 추론 포함 16,000토큰까지입니다. 키 누락·패치 생략·한도 초과·불완전한 모델 응답은 팀원 확인 대상으로 표시합니다. 외부 포크에는 유료 호출을 실행하지 않습니다.

운영에서 `review-risk`를 연결할 때 알림용 저장소 변수 `REVIEW_RISK_APP_ID`를 발급 앱 ID로 지정합니다. 체크는 현재 head SHA와 `external_id=pr-<PR 번호>`를 사용합니다. 완료 결과와 제목의 조합은 `success/통과`, `failure/수정 필요`, `action_required/팀원 판단`, `success/팀원 승인`입니다. 연결 전에는 보호 경로와 GitHub CI에서 직접 확인한 정보만 표시합니다.

| 판정 | 다음 행동 |
| --- | --- |
| 통과 | 최신 커밋의 기존 필수 CI가 통과하고, 신뢰된 Codex 결과가 같은 Jira 작업의 각 완료 조건에 코드·테스트 근거를 제시했으며 보호 대상 경로·호출 흐름을 건드리지 않았습니다. 운영 전환 뒤에는 작성자가 병합할 수 있습니다. 일반 기능 수정과 그 동작을 검증하는 테스트 추가도 대상입니다. |
| 수정 필요 | 현재 커밋에 완료 조건 위반 근거가 있거나 CI가 실패했습니다. 수정 후 새 커밋을 검증합니다. |
| 팀원 판단 | 인증·권한·DB·정책 자체, 기존 권한·트랜잭션 호출 순서 변경, 테스트 약화, 분류 불가, Jira 또는 리뷰 근거 누락에 해당합니다. 승인자는 현재 유효한 backend 팀의 사람이며 PR 작성자·마지막 푸시자와 달라야 합니다. |
| CI 대기 | 필수 검사 미완료 또는 결과 미확인입니다. 완료된 결과를 기다립니다. |

고위험 변경에 결함이 있으면 먼저 수정하고 다시 판정합니다. 이전 커밋의 승인, 취소된 승인, 작성자가 붙인 라벨, 댓글 없음은 통과 근거로 사용할 수 없습니다.

**운영 전환 순서**

1. 일반 PR 생성과 Draft의 Ready 전환에서 자동 리뷰 신호를 관찰합니다. 추가 push 후에는 수동 리뷰를 요청하고 실제 리뷰 대상 커밋을 확인합니다.
2. 보호된 실행 환경에서 GitHub의 전체 변경 목록(이동 전 경로 포함), Jira 원문, 최신 리뷰·체크·승인과 푸시 이벤트를 읽습니다. PR 내부 문구를 인증된 리뷰 결과로 사용하지 않습니다. 모델이 완료 조건을 일부만 확인했거나, 호출 관계·테스트 약화를 판단할 수 없으면 팀원에게 보냅니다.
3. 개발 브랜치에 연결하기 전에 시험용 보호 브랜치에서 독립된 필수 검사 `review-risk`를 등록하고 발급 앱을 고정합니다. 현재 CI의 회귀 테스트 성공은 그 PR의 Codex 리뷰 완료 신호와 별개입니다. 이 체크는 변경 요청·사람 승인·승인 취소·새 push 때 다시 평가하며, 일반 PR이 수정할 수 있는 코드로 자기 병합을 승인할 수 없도록 구성합니다.
4. 같은 최신 커밋에서 실제 팀원의 승인, 최종 푸시자 본인의 승인 거부, 이전 승인 거부, Codex 결과 누락 시 팀원 경로를 확인합니다. 기존 CI 3개와 대화 해결은 양쪽 모두에서 필요합니다.
5. 시험 통과 후 `development`에서 추가 검사를 필수화하고 확인한 뒤에만 기본 승인 수를 1에서 0으로 전환합니다. 전역 마지막 푸시 승인 옵션은 조건부 체크에서 고위험 최신 푸시자 분리를 강제한 뒤 조정합니다. `main` 릴리스는 기존 사람 승인 정책을 따릅니다. 신호를 신뢰할 수 없게 되면 기존 승인 1명과 마지막 푸시 승인 옵션을 먼저 복구합니다.

## 7. 완료 확인

- GitHub-first: GitHub Issue `Closed`와 Jira Task `완료`를 모두 확인합니다.
- Jira-first: Jira Task `완료`와 Jira Development의 `MERGED` PR을 확인합니다.
- Jira 업무는 삭제하지 않고 `완료`로 전환하며 Slack 성공 알림도 확인합니다.
- Team Board Gantt에서 업무가 사라지면 `Show completed tickets`를 켭니다.
- Epic은 모든 `[FE]`, `[BE]`, `[AI]`, `[INT]` child Task와 통합 검증이 끝난 뒤
  sprint review에서 수동 완료합니다.

## 8. 문제 발생 시

- `jira-issue-key` 실패: 작업 PR은 Jira 키와 `[BE]`, main 승격은 같은 저장소의 development·제목 버전·VERSION 일치를 확인합니다.
- Jira 자동 생성이 건너뛰어짐: `task` 또는 `bug`를 확인한 뒤 `jira-sync` 레이블을
  추가합니다.
- Jira 자동 생성 실패: `jira-sync`를 제거 후 다시 추가하거나 `GitHub Issue to
  Jira`를 `main`에서 Issue 번호로 재실행합니다.
- 중복 Jira Task가 의심됨: 새 Task를 만들지 말고 `jira-linked` 댓글과 Jira의
  `github-backend-<issue-number>` 레이블을 확인합니다.
- Issue가 닫히지 않음: 작업 PR의 대상이 `development`인지, 본문의
  `Resolves #번호`가 같은 저장소 Issue인지, `Close Linked Issues` 실행 결과를
  확인합니다.
- Jira Task가 완료되지 않음: PR 대상이 `development`인지, PR 제목의 Jira 키가
  해당 Task 키와 일치하는지, Jira Automation 감사 로그를 확인합니다.

Organization 전체 흐름과 다른 저장소 예시는
[integration 팀 가이드](https://github.com/DodamDodam-Capstone/integration/blob/main/docs/TEAM_WORKFLOW_GUIDE.md)를
기준으로 합니다.

## 9. 팀 권한과 전체 프로젝트 기준

GitHub visible Team `backend`에 Backend 담당자를 넣고 저장소 `Write`를 부여합니다. 팀원이 2명 이상일 때 CODEOWNERS와 리뷰 자동 배정을 사용합니다. Jira `Backend` Team을 연결하되 Assignee는 실제 담당 개인으로 유지합니다. 빈 팀이나 Team filter를 미리 만들지 않습니다.

Epic·하위 이슈·Team Board 공통 운영은 [integration 운영 규칙](https://github.com/DodamDodam-Capstone/integration/blob/main/docs/JIRA_GITHUB_INTEGRATION.md)을 따릅니다.
