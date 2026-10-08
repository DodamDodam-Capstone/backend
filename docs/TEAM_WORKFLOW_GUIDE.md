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

- branch와 PR 제목에는 같은 `SCRUM-번호`를 정확히 하나 넣습니다.
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
- `main` 승격 PR에는 `Resolves #번호`, `Closes #번호`, `Fixes #번호`를
  적지 않습니다. GitHub는 기본 브랜치 병합 시 이 키워드로 Issue를 닫습니다.

## 7. 완료 확인

- GitHub-first: GitHub Issue `Closed`와 Jira Task `완료`를 모두 확인합니다.
- Jira-first: Jira Task `완료`와 Jira Development의 `MERGED` PR을 확인합니다.
- Jira 업무는 삭제하지 않고 `완료`로 전환하며 Slack 성공 알림도 확인합니다.
- Team Board Gantt에서 업무가 사라지면 `Show completed tickets`를 켭니다.
- Epic은 모든 `[FE]`, `[BE]`, `[AI]`, `[INT]` child Task와 통합 검증이 끝난 뒤
  sprint review에서 수동 완료합니다.

## 8. 문제 발생 시

- `jira-issue-key` 실패: branch와 PR의 Jira 키, `[BE]`, target branch를 확인합니다.
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
