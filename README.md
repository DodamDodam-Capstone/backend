# DodamDodam Backend

DodamDodam 캡스톤 프로젝트의 Spring Boot 백엔드 서비스입니다.

## 확정 개발 기준

| 항목 | 선택 |
| --- | --- |
| Group ID | `com.dodamdodam` |
| Artifact | `dodamdodam-backend` |
| Base package | `com.dodamdodam.backend` |
| Spring Boot | `4.1.1` |
| Java | `25` LTS |
| Local JDK | Oracle OpenJDK 25 또는 Eclipse Temurin 25 |
| Build | Gradle `9.7.1` Wrapper, Kotlin DSL |
| Database | PostgreSQL `18.6` |
| Authentication | Google OIDC, Kakao OIDC, 서버 세션 쿠키 |
| Local infrastructure | Docker Compose |

AWS 배포와 RAG·벡터 검색 구성은 현재 범위에 포함하지 않습니다.

## 빠른 시작

필수 도구는 JDK 25와 Docker Desktop입니다. Oracle OpenJDK 25도 사용할 수 있으며,
별도 Gradle 설치는 필요하지 않습니다. IntelliJ의 Project SDK와 Gradle JVM은 모두
JDK 25로 맞춥니다.

```bash
cp .env.example .env
./gradlew bootRun
```

Spring Boot가 `compose.yaml`의 PostgreSQL을 자동으로 시작합니다. 애플리케이션이
기동되면 다음 주소로 상태를 확인합니다.

```bash
curl http://localhost:8080/actuator/health
```

실제 소셜 로그인을 사용하려면 `.env`의 Google·Kakao 값을 발급받은 자격 증명으로
교체해야 합니다. 예시 값으로도 애플리케이션은 실행되지만 공급자 로그인에는
실패합니다.

로그인 시작 주소는 다음과 같습니다.

- Google: `http://localhost:8080/oauth2/authorization/google`
- Kakao: `http://localhost:8080/oauth2/authorization/kakao`
- CSRF 토큰: `GET http://localhost:8080/api/v1/auth/csrf`

## 자주 쓰는 명령

```bash
./gradlew clean check
./gradlew bootRun
docker compose up -d postgres
docker compose --profile full up --build
docker compose stop
```

테스트는 Testcontainers로 PostgreSQL 18.6을 실행하므로 Docker가 켜져 있어야
합니다.

## 문서

- [개발환경 설치·실행 가이드](docs/DEVELOPMENT_SETUP.md)
- [IntelliJ·JDK·Docker 상세 설정](docs/DEVELOPMENT_SETUP.md#5-intellij-idea-설정)
- [Backend 팀 업무 실행 가이드](docs/TEAM_WORKFLOW_GUIDE.md)
- [Backend Jira·GitHub 업무 규칙](docs/JIRA_WORKFLOW.md)
- [기여 가이드](CONTRIBUTING.md)
- [보안 정책](SECURITY.md)

Organization 전체 협업 흐름은
[integration 저장소 문서](https://github.com/DodamDodam-Capstone/integration/blob/main/docs/GITHUB_WORKFLOW.md)에서
관리합니다. 기능 변경은 작업 브랜치에서 `development`로 squash merge하고,
검증된 `development`는 보호된 PR과 승인을 거쳐 merge commit으로 `main`에
반영합니다. `main` 대상 PR의 source branch는 `development`만 허용하며 긴급
수정도 먼저 `development`에 반영합니다.
