# 기여 가이드

업무를 시작하기 전에
[`docs/TEAM_WORKFLOW_GUIDE.md`](docs/TEAM_WORKFLOW_GUIDE.md)에서 Jira-first와
GitHub-first 중 한 가지 경로를 선택합니다.

다음 Gitmoji 및 PR 규칙은 프로젝트 전체에서 동일하게 적용합니다.

PR 제목은 `<gitmoji> <type>(optional-scope): <description>` 형식을 사용합니다.

예시:

```text
✨ feat: 회원가입 endpoint 추가
🐛 fix(auth): 만료된 refresh token 거부
✅ test: 중복 이메일 가입 검사 추가
```

`main`과 `development`의 모든 변경은 PR을 사용해야 합니다. 필수 검사를
통과하고 모든 review conversation을 해결합니다. 작업 브랜치 → `development`는
squash merge하고, `development` → `main` 승격은 merge commit을 사용합니다.
자동 브랜치 삭제는 사용하지 않으며 작업 브랜치는 sprint 정리 시 수동으로
삭제합니다.
