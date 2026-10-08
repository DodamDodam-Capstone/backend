# Backend 에이전트 지침의 분리와 유지

이 문서는 backend 저장소 안에서 에이전트 지침을 생성하거나 수정할 때 적용한다.
지침과 상세 문서는 backend checkout 안에서 독립적으로 읽을 수 있게 관리한다.

## 파일 배치

| 위치 | 역할 |
| --- | --- |
| [루트 AGENTS.md](../AGENTS.md) | Backend 원칙, 핵심 금지사항, 명령, 작업별 문서 진입점 |
| [루트 CLAUDE.md](../CLAUDE.md) | 같은 위치의 AGENTS.md를 불러오는 짧은 진입점 |
| [AGENT_WORKFLOW.md](AGENT_WORKFLOW.md) | Jira 단계·Sub-task·포인트·일정, Git·PR·AI attribution 금지 |
| [AGENT_GUIDE.md](AGENT_GUIDE.md) | Backend 아키텍처·DB·인증·서비스 계약 |
| [DEVELOPMENT_SETUP.md](DEVELOPMENT_SETUP.md) | 버전·환경변수·포트·OAuth·설치·실행 |
| [TEAM_WORKFLOW_GUIDE.md](TEAM_WORKFLOW_GUIDE.md) | Backend 팀원이 공유하는 업무 실행 가이드 |
| 필요한 하위 AGENTS.md·CLAUDE.md | 해당 Backend 디렉터리의 추가 책임·명령·제약 |

## 짧게 유지하는 기준

- `AGENTS.md`는 빈 줄을 포함해 60줄 이내, `CLAUDE.md`는 10줄 이내로 유지한다.
- 항상 필요한 책임·명령·금지사항·문서 진입점만 지침에 남긴다.
- 여러 단계의 절차, 긴 예시, 배경 설명, 설정 이력은 backend의 `docs/`로 옮긴다.
- 링크에는 “어떤 작업에서 읽는지”를 함께 적는다. 관련 없는 문서를 매 작업마다 전부 읽게 하지 않는다.
- 기본 지침에 이슈 진행 기록, 일회성 계획, 테스트 로그를 누적하지 않는다. Jira·PR 또는 작업 문서에 기록한다.
- formatter·linter가 강제하는 세부 서식 규칙을 지침에 다시 풀어 쓰지 않는다.

## 디렉터리 지침을 추가할 때

1. 루트 지침과 기존 상세 문서로 처리할 수 있는지 먼저 확인한다.
2. 책임, 실행·검증 명령, 보안 제약 또는 리뷰 방식이 다를 때만 Backend의 해당 디렉터리에 `AGENTS.md`를 생성한다.
3. 하위 파일에는 상위와 다른 추가 규칙만 적고, 필요한 상세 문서는 backend의 `docs/`에 둔다.
4. Claude용 진입점은 같은 위치의 `AGENTS.md`를 `@AGENTS.md`로 불러온다.
5. 부모의 보호 브랜치, 비밀정보, AI attribution 금지 규칙을 하위에서 완화하지 않는다.

문서 폴더라는 이유만으로 `docs/AGENTS.md`를 만들지는 않는다.
해당 문서 작업에 독립적인 작성·검증 규칙이 생기면 추가한다.

## 문서를 이동하거나 수정할 때

- 원래 규칙의 의미를 보존하고, 읽을 조건과 새 링크를 기존 진입점에 남긴다.
- Backend 실행 정책은 `AGENT_WORKFLOW.md`에서 관리하며 기존 팀 가이드와 일치시킨다.
- 다른 팀 저장소에 지침을 생성하거나 적용 범위를 확대하지 않는다.
- 변경 후 파일 길이, 상대 링크, Claude import 경로, 명령의 실제 존재 여부를 확인한다.
- 다른 저장소의 로컬 파일이나 미게시 GitHub 문서에 새 지침을 의존시키지 않는다.

## 중요한 규칙은 CI로 검증

- 보안, 데이터 정합성, API 계약, 반복되는 팀 규칙을 추가·변경하면 자동 검증 가능한 부분을 같은 PR에서 기존 테스트·CI에 연결한다. 기존 검사로 보장되면 중복 검사를 만들지 않는다.
- 인증·인가·CSRF·데이터 무결성은 실제 동작을 테스트한다. 특정 코드 문자열이나 문서 문구의 존재만으로 보장하지 않는다.
- 검사에는 정상 사례와 대표 위반 사례를 남긴다. 실패 시 원인과 수정할 위치를 알 수 있게 출력하고, 검사 제거·무시·항상 성공 처리로 통과시키지 않는다.
- 외부 상태나 사람의 판단이 필요한 Jira 추정·일정·리뷰 승인은 실제 Jira/GitHub 결과로 확인한다. CI에 Jira 비밀값을 추가하거나 조회 실패를 완료로 처리하지 않는다. 자동화하지 못한 이유와 확인 방법은 PR에 적는다.

| 규칙 | 검증 위치 |
| --- | --- |
| 루트·하위 AGENTS 60줄, CLAUDE 10줄, 같은 디렉터리 `@AGENTS.md` 단일 import | `backend-quality`의 `check_agent_policy.py` |
| 에이전트 지침과 `docs/AGENT_*.md`의 내부 파일 링크 | 같은 검사. backend 밖이나 Git에 포함되지 않은 대상은 실패 |
| 신규 commit author·committer, commit·PR의 알려진 AI attribution 표기 | 같은 검사. 일반적인 도구 언급과 사람 공동 작성자는 허용 |
| Gitmoji·Conventional Commit PR 제목, Jira 키·저장소 접두어 | 기존 `gitmoji-conventional-title`, `jira-issue-key` |
| Backend 동작·회귀 | 기존 `backend-quality`의 Gradle `check` |
| 다른 팀원 승인·최신 base·대화 해결·병합 방식 | GitHub Ruleset |

문서 링크는 `[설명](상대/경로.md)` 형식을 쓴다. CI는 파일 존재와 저장소 경계를 검사하며 외부 URL·문서 내부 anchor의 유효성은 확인하지 않는다.
AI 검사는 알려진 Claude·Codex·OpenAI·Anthropic identity와 attribution 패턴을 검사한다. 새로운 표기는 사례와 함께 검사를 보완한다. GitHub Collaborator 권한과 최종 squash 메시지는 업로드 절차에서 별도로 확인한다.

로컬에서도 저장소 루트에서 실행한다.

```bash
python3 .github/scripts/test_agent_policy.py
python3 .github/scripts/check_agent_policy.py --base origin/development
```

PR CI는 base와 head 사이 신규 commit 및 PR 제목·본문을 검사한다. PR 제목·본문 수정 때도 재실행한다. 로컬 `--base` 검사는 commit 범위를 확인하고, `workflow_dispatch`는 문서와 HEAD commit을 검사한다.

Claude의 `@` import는 파일을 시작 시 context에 함께 읽어들인다.
긴 상세 문서를 import로 분리하는 것만으로는 context가 줄지 않으므로,
짧은 AGENTS만 import하고 상세 문서는 일반 링크와 읽을 조건으로 연결한다.
[Claude 공식 문서](https://code.claude.com/docs/en/memory#import-additional-files)

Codex의 지침 탐색 범위와 순서는
[공식 AGENTS.md 문서](https://learn.chatgpt.com/docs/agent-configuration/agents-md)를 따른다.
