# DodamDodam Backend

API, domain, database, 인증·인가, 외부 시스템 연결과 Backend 테스트를 담당한다.
이 지침은 `DodamDodam-Capstone/backend`에만 적용한다. 명령은 backend 저장소 루트에서 실행한다.

## 작업 원칙

- 한국어로 간결하게 설명하고 요청 범위의 최소 변경으로 작업을 완료한다.
- 기존 구현 → 표준 라이브러리 → 설치된 의존성 순서로 재사용한다. 버그는 호출 흐름과 원인을 확인한 뒤 수정한다.
- 작업 전 Git 상태와 관련 소스·테스트를 확인하고 사용자 변경을 보존한다.
- 실행하지 못한 검증과 실패를 성공으로 보고하지 않는다. 비밀값·실제 `.env`·개인정보를 코드·문서·이슈·로그에 기록하지 않는다.
- 기존 사람 Git identity를 사용한다. Claude·Codex·OpenAI·Anthropic 등 AI/Bot을 author·committer·co-author·contributor·signatory로 넣거나 이를 지칭하는 `Co-authored-by` 등 attribution trailer·PR 작성자 표기를 추가하지 않는다.

<!-- codebase-memory-mcp:start -->
## 코드 탐색

- 코드 정의·관계 탐색은 codebase-memory-mcp를 우선 사용한다.
- 순서: `search_graph` → `trace_path` → `get_code_snippet` → `query_graph` → `get_architecture`.
- 코드 탐색 대상이 미인덱싱이면 먼저 `index_repository`를 실행한다. backend 저장소와 실제 소스 경로를 기준으로 조회한다.
- 문자열·설정·문서, MCP 사용 불가·결과 부족 시 `rg`와 파일 읽기로 전환한다. 문서 작업만으로 전체 코드를 인덱싱하지 않는다.
<!-- codebase-memory-mcp:end -->

## 작업별 상세 문서

아래 문서는 해당 작업의 관련 부분만 읽는다. 상세 규칙이 링크만으로 자동 적용된다고 가정하지 않는다.

| 작업 | 먼저 읽을 문서 |
| --- | --- |
| Git·Jira 계획·상태·포인트·일정·업로드 | [Backend 실행 규칙](docs/AGENT_WORKFLOW.md), [팀 업무 가이드](docs/TEAM_WORKFLOW_GUIDE.md) |
| 코드·DB·인증·서비스 계약 변경 | [Backend 상세 지침](docs/AGENT_GUIDE.md) |
| 도구 버전·포트·환경변수·OAuth callback·설치 변경 | [개발 환경 문서](docs/DEVELOPMENT_SETUP.md) |
| AGENTS·CLAUDE 지침·CI 정책 생성·수정 | [문서 분리·유지 규칙](docs/AGENT_DOCUMENTATION.md) |

## Git·Jira 기본 흐름

- Jira 접두어는 `[BE]`다. 기존 Jira Task가 있으면 GitHub Task Form을 중복 생성하지 않는다.
- 착수 전에 담당자·Story point·계획 시작일·목표 종료일을 설정한다. `해야할 일 → 진행 중 → 검토중 → 완료` 기준으로 실제 Jira transition을 사용한다.
- 브랜치는 최신 `development`에서 `feature/<Jira키>-<설명>`, `fix/...`, `hotfix/...`로 만든다. 보호 브랜치 직접 push·force push는 금지한다.
- commit·PR은 Gitmoji + Conventional Commit 형식에 같은 Jira 키와 `[BE]`를 넣는다. 작업은 squash, `development → main`은 merge commit으로 병합한다.
- 필수 검사는 `backend-quality`, `gitmoji-conventional-title`, `jira-issue-key`다. 마지막 push를 하지 않은 다른 팀원의 승인과 모든 review conversation 해결이 필요하다.
- PR 병합 후 Jira 자동 완료를 확인하고, GitHub-first이면 연결 GitHub Issue 종료도 확인한다. Sub-task·Epic 완료 기준은 실행 규칙을 따른다.

## 구현·검증

- PostgreSQL·Flyway·`ddl-auto=validate`, 세션·CSRF·명시적 credentialed CORS 계약을 유지한다. 적용된 migration을 수정하지 않는다.
- 중요한 규칙은 문서에만 두지 않고 자동 검증 가능한 부분을 기존 테스트·CI에 반영한다. 통과·실패 사례를 확인하며 검증을 무력화해 통과시키지 않는다.

| 작업 | 명령 |
| --- | --- |
| 컴파일 | `./gradlew compileJava` |
| 코드 변경 검증 | `./gradlew clean check` |
| 로컬 실행 | `./gradlew bootRun` |
| Testcontainers 실행 | `./gradlew bootTestRun` |
| PostgreSQL 시작 | `docker compose up -d postgres` |
| 전체 컨테이너 실행 | `docker compose --profile full up --build` |
| Compose 변경 검증 | `docker compose config --quiet` |

- 통합 테스트는 Docker daemon과 PostgreSQL Testcontainers를 필요로 한다. 검증 불가 시 원인을 보고한다.
- `AGENTS.md`는 60줄 이내, `CLAUDE.md`는 10줄 이내로 유지한다. 상세 절차는 backend의 `docs/`로 분리하고, 고유 규칙이 필요한 하위 디렉터리에만 개별 지침을 만든다.
- 규칙 자체는 명시적으로 요청받았을 때 수정한다. 최종 보고에는 변경·검증·미해결 사항과 관련 이슈/PR 링크를 남긴다.
