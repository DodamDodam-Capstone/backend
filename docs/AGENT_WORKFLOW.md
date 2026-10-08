# Backend Git·Jira 에이전트 실행 규칙

이 문서는 `DodamDodam-Capstone/backend`에서 작업하는 에이전트에만 적용한다.
기존 [Backend 팀 업무 가이드](TEAM_WORKFLOW_GUIDE.md)를 기준으로 계획·상태·추정·일정·업로드를 관리한다.
Jira 사이트는 `https://dodamdodam.atlassian.net`, 프로젝트 키는 `SCRUM`, 작업 접두어는 `[BE]`다.

## 1. 이슈와 작업 단위

- Backend 저장소 변경 작업은 Jira 이슈에 연결한다. 질문, 설명, 단순 조회에는 이슈를 만들지 않는다.
- 사용자가 지정한 키를 우선 사용하고, 생성 전에 관련 이슈와 기존 GitHub 연결을 확인한다.
- Jira Task가 있으면 Jira-first로 진행한다. 없으면 backend의 `Backend Task`/`Backend Bug` Form으로 GitHub-first를 사용하고 생성된 Jira 키를 확인한다. 두 경로를 동시에 실행하지 않는다.
- Task 제목은 `[BE] <구현할 결과>`, Epic 제목은 `[EPIC] <사용자 가치 또는 목표>`다. 여러 저장소가 참여하는 Epic에서도 Backend 작업은 별도의 `[BE]` Task로 둔다.
- 다른 팀 작업이 필요하면 의존 이슈·API 계약·필요 산출물을 기록한다. 다른 저장소의 작업이나 규칙 변경은 명시적으로 요청된 범위에서 수행한다.
- 같은 Task 안에서도 담당자, 산출물 또는 검증을 독립 관리해야 할 때만 Jira Sub-task를 생성한다. 단순 순서는 본문의 체크리스트로 관리한다.
- Sub-task에는 담당자·작업 범위·완료 조건·검증 방법·시작일·목표 종료일을 작성한다. 다른 저장소 작업을 Backend Sub-task로 묶지 않는다.
- Task/Bug 본문에는 목적, 범위, 완료 조건, 검증 방법을 적는다. Bug에는 재현 요청과 기대·실제 응답도 적는다.
- GitHub-first의 `상위 Jira 키`, 유형 레이블, `jira-sync`와 중복 방지는 [팀 업무 가이드](TEAM_WORKFLOW_GUIDE.md)의 GitHub-first 절차를 따른다. Jira 생성이 지연되었다고 별도 Task를 다시 만들지 않는다.

## 2. 착수 전 메타데이터

- 담당자는 실제 Backend 팀원으로 지정하고 우선순위와 해당 sprint를 확인한다. 에이전트 이름을 담당자로 넣지 않는다.
- Story point는 `1, 2, 3, 5, 8` 중 기존 완료 이슈와 비교해 상대적인 규모·복잡도·불확실성으로 산정한다. 포인트를 시간·일수로 환산하지 않는다.
- `1`: 작은 변경, `2`: 익숙한 제한 범위, `3`: 여러 부분의 구현·검증, `5`: 넓은 영향 또는 불확실성, `8`: 큰 작업. 8을 초과하면 분할을 검토한다.
- 착수 전에 Story/Task/Bug의 포인트와 한 줄 추정 근거를 기록한다. Sub-task에는 중복 포인트를 부여하거나 부모에 합산하지 않는다. 기존 팀 추정치와 이미 확정된 포인트를 우선한다.
- 착수 후 소요 시간이 예상과 다르다는 이유로 포인트를 바꾸지 않는다. 범위가 달라지면 분할·재추정 사유를 기록한다.
- 모든 실행 Task/Bug/Sub-task에 계획 시작일과 목표 종료일을 설정한다. 날짜는 `Asia/Seoul`, `YYYY-MM-DD` 기준이며 시작일은 종료일보다 늦을 수 없다.
- Jira `시작 날짜`/`Start date`는 계획 시작일, `기한`/`Due date`는 목표 종료일로 사용한다. 실제 착수·완료는 상태 전환 이력으로 확인하고, 기한을 실제 완료일로 덮어쓰지 않는다.
- Team Board Gantt를 사용하면 `Start Date (Teamboard)`와 `End Date (Teamboard)`에도 같은 계획 일정을 입력한다. 담당자 확정 후 필요한 Resource 연결도 확인한다.
- 사용자·팀이 정한 일정을 우선한다. 일정이 없으면 작업·의존성에 근거한 예상 일정임을 기록하고, 외부에 약속된 마감일을 임의로 확정하거나 변경하지 않는다.
- 일정 변경 시 기존 일정, 새 일정, 이유와 관련 의존 이슈를 기록하고 Jira·Team Board를 함께 갱신한다.
- 실제 이슈 유형·필드·허용 transition을 조회해 사용한다. 필드 이름·ID·지원 여부를 추정하거나 누락된 필드를 프로젝트 설정에 임의 생성하지 않는다. 갱신 불가 항목은 이슈/최종 보고에 남긴다.

## 3. 상태 전환

아래 이름은 Backend의 단계 기준이다. Jira의 실제 한글/영문 상태와 허용 transition에 매핑한다.
이미 단계가 진행된 이슈를 기계적으로 처음으로 되돌리거나 의미 없이 모든 상태를 거치게 하지 않는다.

| 상태 | 진입 조건과 기록 |
| --- | --- |
| 해야할 일 / To Do | 목적·범위·완료 조건이 정리된 대기 작업 |
| 진행 중 / In Progress | 실제 착수. 담당자·포인트·일정 확인 후 현재 상태를 갱신 |
| 검토중 / In Review | 구현·관련 검증을 마치고 리뷰 가능한 PR 또는 산출물을 제출. PR·검증 결과 연결 |
| 완료 / Done | Task/Bug는 모든 완료 조건·필수 Sub-task·CI·리뷰 충족 후 연결 PR 병합과 Jira 자동화 결과 확인 |

- Draft PR 생성이나 코드 작성 종료만으로 `검토중` 또는 `완료`로 처리하지 않는다.
- 수정 요청이나 검증 실패로 추가 작업이 필요하면 `검토중 → 진행 중`으로 되돌리고 이유를 남긴다.
- 막힌 작업은 장애물, 의존 이슈, 필요한 조치와 재개 조건을 기록한다. 기다리는 작업을 완료로 처리하지 않는다.
- branch 생성·PR 생성에 따른 상태 자동화가 설정되어 있다고 가정하지 않는다. 실제 상태를 조회하고 필요한 전환을 수행한다.
- Task 완료는 기존 `PR 병합 시 Task 완료` 자동화를 우선 사용한다. 병합 전에 수동 완료하거나 commit에 `#done`을 넣지 않는다.
- 자동 완료가 실패하면 감사 로그·연결 상태를 확인하고 미반영 상태를 보고한다. 허용된 복구 절차를 따른다.
- 부모 PR을 공유하는 Sub-task는 자기 산출물·검증·리뷰가 끝나면 먼저 완료할 수 있으며 부모 PR을 근거로 연결한다. 독립 PR이 있으면 해당 PR 병합까지 확인한다.
- 모든 필수 Sub-task를 마친 뒤 부모 Task의 PR을 병합한다. Epic은 모든 child Task와 통합 검증 이후 sprint review에서 수동 완료하며, Backend Task 완료만으로 전체 Epic을 완료하지 않는다.
- 작업 시작, 범위·일정 변경, 장애물, 리뷰 요청, 완료 때 유의미한 기록을 남긴다. 도구 호출마다 반복 댓글을 남기지 않는다.

## 4. Git·PR·병합

- backend 저장소의 Git 상태를 확인하고 기존 사용자 변경을 보존한다. 최신 `development`에서 작업 브랜치를 만든다.
- 브랜치는 `feature/<Jira키>-<설명>`, `fix/<Jira키>-<설명>`, `hotfix/<Jira키>-<설명>` 형식이다. hotfix도 먼저 `development`에 반영한다.
- branch와 일반 작업 PR 제목에는 같은 Jira 키를 정확히 하나 넣는다. commit에도 같은 키를 넣는다.
- commit·PR 제목은 `<gitmoji> <type>(optional-scope): SCRUM-번호 [BE] <설명>` 형식이다. 일반 PR에는 `[BE]`를 정확히 하나 넣는다.
- Gitmoji는 의미에 맞게 선택한다. type은 `feat`, `fix`, `refactor`, `docs`, `test`, `ci`, `chore`, `perf`, `security`, `revert`, `style`, `build`, `deps`를 사용한다.
- PR 본문에는 Jira 링크, 변경 내용, 검증 결과를 적는다. backend GitHub Issue가 실제로 있을 때만 `Resolves #번호`를 추가한다.
- `main`·`development`에 직접 push, force push 또는 브랜치 삭제를 하지 않는다. `main` 대상 PR의 source는 항상 `development`다.
- 작업 브랜치 → `development`는 squash merge, `development` → `main` 승격은 merge commit이다. 승격 PR은 Epic 키를 사용한다.
- 모든 필수 CI와 review conversation을 해결한다. 마지막 push를 하지 않은 다른 팀원의 승인을 받으며, 새 commit 이후 승인을 다시 확인한다.
- 필수 검사는 `backend-quality`, `gitmoji-conventional-title`, `jira-issue-key`다. 정확한 검사·승인 요구사항은 실제 Ruleset과 [팀 업무 가이드](TEAM_WORKFLOW_GUIDE.md)로 확인한다.
- Dependabot 등 기존 정책에 명시된 Bot은 Jira 키 검사만 예외다. 에이전트가 자기 작업을 Bot 작업으로 분류해 검사를 우회하지 않는다.
- 작업 브랜치 자동 삭제를 사용하지 않는다. sprint 정리 시 팀의 수동 삭제 절차를 따른다.

## 5. GitHub 업로드와 작성자 표기

- 에이전트가 만드는 commit은 저장소의 기존 사람 Git identity를 사용한다. `user.name`·`user.email`, author·committer를 AI나 Bot identity로 변경하지 않는다. 사람 identity가 없으면 임의로 만들거나 다른 사람을 사칭하지 않고 필요한 정보를 요청한다.
- Claude, Claude Code, Codex, OpenAI, Anthropic 또는 다른 코딩 에이전트를 author, committer, co-author, contributor, signatory로 기재하지 않는다.
- AI를 지칭하는 `Co-authored-by`, `Contributed-by`, `Signed-off-by`, `Generated-by` 및 유사 trailer를 commit 메시지에 추가하지 않는다. 정상적인 사람 공동 작성자와 서명은 이 금지의 대상이 아니다.
- commit·PR 본문에 `generated by Codex`, `generated with Claude` 같은 AI 작성자 표기나 badge를 추가하지 않는다. 변경 내용과 검증 결과를 작성한다.
- push·PR 생성·병합 전에 신규 commit의 author·committer·본문과 PR·squash 메시지를 확인한다. 자동 생성 템플릿이 AI attribution을 삽입하면 제거한 뒤 진행한다.
- Claude·Codex 또는 코딩 에이전트 계정을 GitHub 저장소의 Collaborator로 추가하지 않는다. 기존 팀 자동화의 Bot 정책을 임의 변경하지 않는다.

## 6. 검증과 인계

- Backend 코드 변경은 `./gradlew clean check`, Compose 변경은 `docker compose config --quiet`로 검증한다. 상세 조건은 [Backend 지침](AGENT_GUIDE.md)을 따른다.
- 단순 문서 변경은 링크·내용·서식 확인으로 검증하고, 관계없는 전체 애플리케이션 테스트를 강제하지 않는다.
- 중요 규칙을 추가·변경하면 자동 검증 가능한 부분을 기존 CI에 반영한다. CI 적용 기준과 로컬 명령은 [문서 유지 규칙](AGENT_DOCUMENTATION.md)을 따른다. PR의 필수 CI는 문서 변경에도 충족한다.
- GitHub-first는 PR 병합 후 backend GitHub Issue `Closed`와 Jira Task `완료`를 모두 확인한다. Jira-first는 연결 PR `MERGED`와 Jira Task `완료`를 확인한다.
- Team Board에서 완료 업무가 보이지 않으면 `Show completed tickets`를 확인한다. 완료 이슈를 삭제하지 않는다.
- Jira/GitHub API·권한·필드 또는 검증 환경이 없으면 가능한 로컬 작업을 계속하고, 반영하지 못한 항목과 필요한 조치를 보고한다. 외부 작업·검증을 수행했다고 꾸미지 않는다.
- 최종 보고는 변경 내용, 실행한 검증과 결과, 미해결 사항, Jira 키와 PR 링크를 간결하게 남긴다.

Jira 동기화·연동 설정 수정 시 [팀 업무 가이드](TEAM_WORKFLOW_GUIDE.md)를,
제목·병합 정책 수정 시 [Backend 기여 가이드](../CONTRIBUTING.md)를 함께 확인한다.
