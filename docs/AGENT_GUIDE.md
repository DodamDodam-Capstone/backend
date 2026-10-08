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
