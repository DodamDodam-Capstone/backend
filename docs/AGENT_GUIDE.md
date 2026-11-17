# DodamDodam Backend Detailed Instructions

These instructions apply only to the backend repository. Read the relevant
sections before changing backend code. Commands and the backend
workflow entry points are kept in [AGENTS.md](../AGENTS.md).

## Project baseline

- Group ID: `com.dodamdodam`
- Base package: `com.dodamdodam.backend`
- Java: 25 LTS; CI uses Eclipse Temurin
- Spring Boot: 4.1.1
- Build: Gradle Wrapper version pinned in `gradle/wrapper/gradle-wrapper.properties`, Kotlin DSL
- Database: PostgreSQL 18.6
- Authentication: Google and Kakao OIDC with a server-side HTTP session
- Local infrastructure: Docker Compose
- AWS deployment and vector search are intentionally out of scope until separately decided.

## Code organization

- Keep the application entry point in `com.dodamdodam.backend`.
- Organize business code by feature under `com.dodamdodam.backend.<feature>`.
- Within a feature, separate API, application, domain, and infrastructure concerns only when the feature needs them.
- Keep cross-cutting configuration and shared infrastructure under `com.dodamdodam.backend.global`.
- Do not create generic `util` or `common` dumping grounds. Name shared code after its actual responsibility.
- Keep REST routes under `/api/v1` unless an existing contract says otherwise.

### 기능별 패키지 표준

- 최상위 업무 패키지는 `com.dodamdodam.backend.<feature>`로 구성한다. 전체 기능을 하나의 controller/service/dto 패키지에 모으지 않는다.
- 기능 내부에 역할이 늘어나면 아래 이름으로 분리한다. 같은 역할은 같은 이름을 쓰되, 모든 기능에 모든 하위 패키지를 만들지는 않는다.

| 하위 패키지 | 책임 |
| --- | --- |
| `api` | HTTP 컨트롤러, 요청·응답 연결, HTTP 예외 처리 |
| `application` | 서비스, 업무 처리 순서와 트랜잭션 경계 |
| `dto` | 기능의 요청·응답·내부 전달 객체 |
| `infrastructure` | JDBC 저장소, 외부 시스템 연결 구현 |
| `exception` | 기능에서 공유하는 업무 예외와 오류 정보 |
| `domain` | HTTP·Jackson·DB 구현에 의존하지 않는 업무 모델·상태·규칙이 있을 때 사용 |

- 파일이 적고 책임이 명확한 기능은 기능 패키지에 직접 둘 수 있다. 빈 패키지나 분류만을 위한 Entity·Service 인터페이스·공통 기반 클래스를 추가하지 않는다.
- `auth/session`처럼 수명주기·접근 범위가 함께 움직이는 응집된 묶음과 `global/config`, `global/web`, `global/auth`, `global/error` 같은 공통 책임은 유지할 수 있다. 공통 CSRF 코드를 특정 기능에 종속시키지 않는다.
- API 오류 필드에 의존하는 예외는 `exception`에 두고, 상태 enum·정책 클래스도 실제 업무 책임을 확인해 `domain` 또는 `application`에 둔다. 타입 이름만으로 `domain`에 모으지 않는다.
- 테스트는 가능하면 대상 코드의 패키지를 따른다. 여러 기능을 함께 검증하는 통합 계약 테스트는 루트 test 패키지에 둘 수 있다. 패키지 전용 의존성 주입이 필요한 테스트 설정은 해당 패키지의 test 소스에 두며, 테스트 편의를 위해 운영 생성자를 public으로 넓히지 않는다.
- 패키지 이동 시 호출자·import·Spring 스캔·접근 범위·테스트·문서 링크를 함께 확인한다. 계층 간에 필요한 타입·메서드만 공개하고, 기존 HTTP·JSON·트랜잭션·예외 계약을 보존한다.
- 세션에 저장되는 타입의 패키지 변경은 저장된 직렬화 데이터와 연결될 수 있으므로 별도 호환성 검증 없이 이동하지 않는다. 적용된 DB migration은 패키지 정리를 위해 변경하지 않는다.
- 폴더 이름이나 파일 개수를 고정하는 테스트는 추가하지 않는다. 컴파일과 기존 기능·보안·동시성 테스트로 동작을 검증하고, 패키지 책임은 코드 리뷰로 확인한다.

자녀 기능의 현재 구성 예시:

```text
child/
├── api/             ChildController, ChildExceptionHandler
├── application/     ChildService
├── dto/             CreateChildRequest, ChildView, ChildContext
├── infrastructure/  ChildRepository
└── exception/       ChildException
```

## 주석과 가독성

- 작성·수정하는 운영 코드의 메서드에는 역할과 필요한 호출 조건을 짧은 한국어 JavaDoc으로 남긴다. 내부에는 검증·정규화·저장 단계와 시간대·잠금 순서·예외 처리의 의도를 필요한 위치에 한 줄로 설명한다.
- 코드만 읽어도 명확한 동작을 반복 설명하는 주석은 줄이고 판단 근거와 제약을 기록한다. 구현을 변경하면 관련 주석도 함께 갱신한다.
- 변수와 메서드는 업무 의도가 드러나게 이름 짓는다. 복잡한 조건은 의미 있는 이름이나 작은 메서드로 정리하고, 실패 조건을 먼저 처리해 중첩을 줄인다. 분리는 읽기 부담이나 중복을 실제로 줄일 때만 한다.
- 검증 실패 분기와 반복문은 중괄호와 줄바꿈으로 적용 범위를 명확히 한다. 서로 다른 검증·변환 단계를 한 줄에 몰아 쓰지 않는다.
- 가독성 정리는 요청 범위의 코드에 한정하고 기존 동작·보안·API 계약을 보존한다. 기존 구현을 재사용하며 불필요한 계층·옵션·의존성을 추가하지 않는다.
- 동작 보존은 관련 테스트로 검증하고, 주석의 정확성·이름·구조는 코드 리뷰로 확인한다. 주석 개수나 문서 문구 존재만으로 가독성을 판정하는 검사는 만들지 않는다.

## Database rules

- PostgreSQL is the only application database. Do not add H2 for convenience.
- Manage every schema change with Flyway under `src/main/resources/db/migration`.
- Use names such as `V1__create_member_table.sql` and never edit an already-applied migration.
- Hibernate is configured with `ddl-auto=validate`; do not change it to `create` or `update`.
- Store timestamps as UTC and map them to timezone-aware Java types where appropriate.
- Do not share the backend schema directly with the FastAPI service. Exchange data through a documented API or event contract.

## Authentication and web security

- Never commit OAuth client secrets, database passwords, access tokens, or a real `.env` file.
- Preserve CSRF protection for session-authenticated browser requests.
- React requests that use the login session must send credentials and the `X-XSRF-TOKEN` value issued by `/api/v1/auth/csrf`.
- Keep CORS origins explicit. Never combine credentialed CORS with a wildcard origin.
- Google and Kakao account identity must be keyed by provider plus `sub`, not by mutable email alone.
- Treat provider email as optional until the product policy and consent settings guarantee otherwise.
- A production cookie policy or token architecture change requires a separate security decision.
- When adding, removing, or reordering servlet or Spring Security filters, update [the filter order and rationale](DEVELOPMENT_SETUP.md#106-필터-실행-순서와-변경-기준) in the same change and verify the affected behavior with the relevant contract tests.

## Cross-team contracts

- Read the relevant product/API/database contract from [the P0 documentation index](p0/README.md) before changing behavior. Keep each contract in its designated document rather than duplicating it here.
- React owns browser UI and should call versioned backend endpoints rather than query PostgreSQL.
- FastAPI owns AI workloads. Keep AI request/response schemas explicit and resilient to timeouts and retries.
- Do not introduce pgvector or RAG dependencies in this service unless ownership and data flow are agreed with the AI team.
- Document API or environment-variable changes that affect React, FastAPI, Docker Compose, or CI.

## Quality and delivery

- Add or update tests for behavior changes.
- Enforce important, mechanically verifiable rules in the existing tests and CI. Verify both allowed behavior and a representative violation; do not replace behavior tests with checks for words in source files. See [the CI policy](AGENT_DOCUMENTATION.md).
- Prefer PostgreSQL-backed integration tests for persistence behavior.
- Run `./gradlew clean check` before handing off a code change.
- Run `docker compose config --quiet` after editing Compose.
- Update [DEVELOPMENT_SETUP.md](DEVELOPMENT_SETUP.md) whenever tool versions, ports, environment variables, OAuth callbacks, or setup steps change.
- Follow [TEAM_WORKFLOW_GUIDE.md](TEAM_WORKFLOW_GUIDE.md) for Jira key, branch, commit, PR title, review, and merge rules.
- Preserve unrelated user changes and never rewrite protected branch history.

Commit attribution and upload checks follow the backend execution rules linked from
[AGENTS.md](../AGENTS.md). Never attribute commits to a coding agent.
