# Backend 개발환경 설치·실행 가이드

이 문서는 새 팀원이 IntelliJ IDEA에서 DodamDodam Backend를 실행하고,
PostgreSQL·Google/Kakao 로그인·테스트·Docker 구성을 같은 방식으로 사용할 수
있게 만드는 기준 문서입니다.

기준일은 **2026-08-29**입니다. AWS 배포, 운영 인프라, RAG, 벡터 검색은 이
설정의 범위가 아닙니다.

## 1. 최종 선택과 안정성 판단

| 구분 | 고정 값 | 선택 이유 |
| --- | --- | --- |
| IDE | IntelliJ IDEA `2026.2` 권장, `2025.2` 이상 | Java 25 지원과 Spring·Gradle 분석 안정성 |
| Group ID | `com.dodamdodam` | 팀이 소유한 조직명을 역도메인 형식으로 표현 |
| Artifact | `dodamdodam-backend` | 저장소와 빌드 산출물의 역할을 명확히 표현 |
| Base package | `com.dodamdodam.backend` | 다른 컴포넌트와 충돌하지 않는 Backend 경계 |
| Spring Boot | `4.1.1` | 공식 stable이며 Boot 4.1의 첫 버그 수정 릴리스 |
| Java | `25` LTS | Boot 4.1 지원 범위이고 장기지원 버전 |
| JDK 배포판 | 개발 PC는 Oracle OpenJDK/Temurin 25, CI·Docker는 Temurin 25 | Java major를 통일하고 OS별 배포판 선택은 허용 |
| Build | Gradle `9.7.1` Wrapper + Kotlin DSL | Java 25 실행 지원, 설치 버전 차이 제거 |
| Database | PostgreSQL `18.6` | 지원 중인 최신 major의 최신 patch |
| Schema | Flyway + Hibernate `validate` | 명시적·재현 가능한 스키마 변경 |
| Authentication | OAuth 2.0 Authorization Code + OIDC | Google/Kakao의 검증된 사용자 식별 정보 사용 |
| Browser session | 서버 세션 + HttpOnly 쿠키 | 초기 웹 서비스에서 토큰 노출 면적을 줄임 |
| Local infrastructure | Docker Compose | 팀원별 PostgreSQL 설치 차이를 제거 |
| Integration test | Testcontainers PostgreSQL 18.6 | 실제 DB와 다른 H2 동작 차이를 방지 |

Spring Boot 4.1.1은 Java 17부터 26까지, Gradle 8.14 이상 또는 9.x를 공식
지원합니다. Gradle 9.7.1은 Java 25에서 실행할 수 있습니다. 따라서 이 조합은
각 프로젝트의 공식 호환 범위 안에 있습니다.

Oracle OpenJDK 25는 이 프로젝트에서 사용해도 됩니다. 로컬 JDK vendor가 Oracle이고
CI·Docker가 Eclipse Temurin이어도 모두 표준 Java 25 바이트코드와 API를 사용하므로
문제가 되지 않습니다. 팀의 필수 통일 기준은 **vendor가 아니라 Java major 25**입니다.
vendor 전용 JVM option이나 JDK 내부 API에는 의존하지 않습니다.

PostgreSQL은 `latest` 태그가 아니라 `18.6` patch를 고정했습니다. major를
자동으로 올리면 데이터 디렉터리 형식이 바뀔 수 있기 때문입니다. 보안·버그 수정
patch는 Dependabot PR에서 검증 후 반영합니다.

## 2. 저장소 구조

```text
backend/
├── AGENTS.md                     # Codex 등 공통 에이전트 규칙
├── CLAUDE.md                     # AGENTS.md를 불러오는 Claude Code 진입점
├── build.gradle.kts              # 의존성과 Java toolchain
├── settings.gradle.kts
├── gradlew, gradlew.bat
├── gradle/wrapper/               # Gradle 9.7.1 Wrapper
├── gradle/gradle-daemon-jvm.properties # Gradle Daemon Java 25 기준
├── compose.yaml                  # PostgreSQL, 선택적 app 컨테이너
├── Dockerfile                    # Java 25 멀티 스테이지 이미지
├── .env.example                  # 커밋 가능한 환경변수 템플릿
├── src/main/java/com/dodamdodam/backend/
│   ├── DodamDodamBackendApplication.java
│   └── global/
│       ├── auth/                 # 공통 인증 HTTP 진입점
│       └── config/               # OAuth, Security, CORS 설정
├── src/main/resources/
│   └── application.yml
└── src/test/java/com/dodamdodam/backend/
    └── ...                       # PostgreSQL Testcontainers 통합 테스트
```

기능 코드는 앞으로 `com.dodamdodam.backend.<feature>` 아래에 둡니다. 예를 들어
회원 기능은 `com.dodamdodam.backend.member`이고, 기능이 커질 때만 그 아래를
`api`, `application`, `domain`, `infrastructure`로 나눕니다.

## 3. 필수 도구

### 3.1 JDK 25

JDK 25를 설치합니다. 현재 macOS Apple Silicon 개발 장비의 **Oracle OpenJDK 25
aarch64**는 적합하며 교체할 필요가 없습니다. Windows 팀원은 Temurin 25 또는
Oracle OpenJDK 25의 **Windows x64** 패키지를 설치하면 됩니다.

| 실행 위치 | JDK 기준 | CPU architecture | 판단 |
| --- | --- | --- | --- |
| macOS Apple Silicon | Oracle OpenJDK 25 또는 Temurin 25 | `aarch64`/`arm64` | 정상 |
| Windows Intel/AMD | Oracle OpenJDK 25 또는 Temurin 25 | `x86_64`/`amd64` | 정상 |
| GitHub Actions | Eclipse Temurin 25 | Linux `x86_64` | CI 기준 |
| Docker build/runtime | Eclipse Temurin 25 | host에 맞는 multi-arch image | 컨테이너 기준 |

`aarch64`, `arm64`, `x86_64`, `amd64`는 Java 버전이 아니라 CPU architecture입니다.
각 운영체제에 맞는 설치 파일만 고르면, 소스와 Java 25 class file은 동일하게
공유할 수 있습니다.

```bash
java -version
```

출력의 major가 `25`인지 확인합니다. 저장소는 다음 세 곳에서 25를 고정합니다.

- `.java-version`: CI와 version manager가 읽는 팀 기준
- `build.gradle.kts`의 Java toolchain: compile/test/application용 JDK 기준
- `gradle/gradle-daemon-jvm.properties`: Gradle 자체를 실행하는 Daemon JVM 기준

macOS에서 설치된 JDK 목록은 다음과 같이 확인할 수 있습니다.

```bash
/usr/libexec/java_home -V
/usr/libexec/java_home -v 25
uname -m
```

현재 장비에서는 JDK 이름 `openjdk-25`, vendor `Oracle Corporation`, architecture
`arm64`로 인식되는 것이 정상입니다. Windows PowerShell에서는 다음을 확인합니다.

```powershell
java -version
$env:PROCESSOR_ARCHITECTURE
where.exe java
```

Windows 팀원과 macOS 팀원이 같은 JDK 설치 파일이나 절대 경로를 공유하면 안 됩니다.
각자 OS/CPU에 맞는 JDK를 설치하고, 프로젝트에서는 major 25만 맞춥니다.

### 3.2 Docker Desktop

Docker Engine과 `docker compose` 명령이 모두 필요합니다. Spring Boot의 Compose
연동에 필요한 최소 Compose 버전은 2.2.0이며, 가능하면 Docker Desktop 최신
안정판을 사용합니다.

```bash
docker --version
docker compose version
docker info
```

`docker info`가 daemon 연결 오류를 내면 Docker Desktop을 먼저 실행합니다.

### 3.3 Gradle

Gradle을 시스템에 별도로 설치하지 않습니다. 항상 저장소의 Wrapper를 사용합니다.

```bash
./gradlew --version
```

Windows PowerShell에서는 다음 명령을 사용합니다.

```powershell
.\gradlew.bat --version
```

출력에서 다음을 확인합니다.

- `Gradle 9.7.1`
- `Daemon JVM` 또는 JVM criteria가 Java 25
- OS와 architecture가 현재 장비와 일치

`Launcher JVM`은 wrapper를 시작한 shell의 Java이고, `Daemon JVM`은 실제 Gradle
build를 실행하는 Java입니다. shell 기본 Java가 26이어도 저장소의 Daemon criteria와
Java toolchain이 25를 선택하지만, 혼동을 줄이려면 IntelliJ Gradle JVM과 터미널의
`JAVA_HOME`도 25로 맞추는 것을 권장합니다. `org.gradle.java.home`에 개인별 절대
경로를 커밋하지 않습니다. Windows와 macOS에서 경로가 서로 다르기 때문입니다.

## 4. 최초 설정

저장소 루트에서 다음 순서로 진행합니다.

```bash
cp .env.example .env
./gradlew clean check
./gradlew bootRun
```

`spring-boot-docker-compose`가 `compose.yaml`을 발견해 PostgreSQL을 시작하고,
연결 정보를 애플리케이션에 제공합니다. 설정은 `start-only`이므로 애플리케이션을
종료해도 PostgreSQL은 계속 실행됩니다.

상태를 확인합니다.

```bash
docker compose ps
curl http://localhost:8080/actuator/health
```

기대 응답은 다음과 같습니다.

```json
{"status":"UP"}
```

작업을 마친 뒤 로컬 컨테이너만 멈출 때는 다음 명령을 사용합니다.

```bash
docker compose stop
```

## 5. IntelliJ IDEA 설정

Java 25 언어 기능은 IntelliJ IDEA 2025.2부터 지원됩니다. 현재 설치된 2025.2.2도
빌드에 사용할 수 있지만, 기준일의 최신 안정판인 2026.2로 업데이트를 권장합니다.
특히 Spring Boot 4 설정 분석과 최신 수정 사항은 새 IDE에서 더 안정적입니다.
업데이트 전에도 Gradle build와 애플리케이션 실행 자체는 가능합니다.

### 5.1 프로젝트 열기

1. IntelliJ에서 **Open**을 선택합니다.
2. 이 저장소의 `backend` 디렉터리를 선택합니다.
3. 프로젝트 신뢰 여부가 표시되면 저장소를 확인한 뒤 **Trust Project**를
   선택합니다.
4. `build.gradle.kts`를 Gradle 프로젝트로 불러옵니다.

### 5.2 JDK와 Gradle JVM

아래 값을 모두 확인합니다. 한 항목만 26으로 남아 있어도 IDE 동기화와 터미널
build가 서로 다른 JVM을 사용할 수 있습니다.

| 위치 | 설정 | 값 |
| --- | --- | --- |
| File → Project Structure → Project | SDK | `openjdk-25` (Oracle OpenJDK 25) |
| File → Project Structure → Project | Language level | `25` 또는 `SDK default (25)`, preview 비활성 |
| File → Project Structure → Modules | Module SDK | `Project SDK` 상속 |
| Settings → Build Tools → Gradle | Distribution | `gradle-wrapper.properties`/Wrapper |
| Settings → Build Tools → Gradle | Gradle JVM | `Project SDK (openjdk-25)` |
| Settings → Build Tools → Gradle | Build and run using | `Gradle` |
| Settings → Build Tools → Gradle | Run tests using | `Gradle` |
| Settings → Compiler → Java Compiler | Target bytecode | `25` |
| Settings → Editor → File Encodings | Global/Project encoding | `UTF-8` |

macOS에서는 **IntelliJ IDEA → Settings** 또는 `⌘,`, Windows에서는 **File →
Settings** 또는 `Ctrl+Alt+S`로 설정을 엽니다. 적용 순서는 다음과 같습니다.

1. **File → Project Structure → SDKs**에서 `openjdk-25`의 Home path가 실제 JDK
   25 경로인지 확인합니다. macOS 현재 기준은
   `~/Library/Java/JavaVirtualMachines/openjdk-25/Contents/Home`입니다.
2. **Project**와 **Modules**에서 위 표의 SDK와 Language level을 적용합니다.
3. Gradle 설정에서 JVM을 `Project SDK`로 선택합니다. 시스템 기본 JDK나
   `JAVA_HOME` 자동 선택에 맡기지 않습니다.
4. Wrapper, Gradle build, Gradle test runner를 선택하고 **Apply**합니다.
5. Gradle 도구 창의 **Reload All Gradle Projects**를 실행합니다.
6. IntelliJ Terminal에서 `./gradlew --version`(Windows는
   `.\gradlew.bat --version`)과 `./gradlew javaToolchains`를 실행합니다.

IntelliJ 자체를 실행하는 JetBrains Runtime(JBR)은 Project SDK가 아닙니다.
**Help → Find Action → Choose Boot Java Runtime for the IDE**에서 JBR을 Oracle JDK로
바꾸지 않습니다. 변경해야 하는 것은 Project SDK, Module SDK, Gradle JVM,
Run Configuration JRE입니다.

`.idea/`는 개인 경로와 UI 상태를 포함하므로 Git에서 제외됩니다. 대신 커밋된
Java toolchain과 `gradle-daemon-jvm.properties`가 모든 팀원의 Gradle Daemon을
vendor와 OS에 관계없이 Java 25로 제한합니다.

### 5.3 실행 구성

Spring Boot 실행 구성을 만들 때 다음 값을 사용합니다.

| 설정 | 값 |
| --- | --- |
| Main class | `com.dodamdodam.backend.DodamDodamBackendApplication` |
| Use classpath of module | `dodamdodam-backend.main` 또는 IntelliJ가 생성한 main module |
| Working directory | 저장소의 `backend` 루트 |
| JRE | Project SDK 25 |

`.env`는 `application.yml`의 optional config import로 로드됩니다. 따라서 실제
OAuth 값은 Run Configuration에 중복 입력하지 않아도 됩니다. Working directory가
저장소 루트가 아니면 `.env`를 찾지 못하므로 반드시 위 값을 확인합니다.

애플리케이션 실행 버튼이 여러 개 보이면 Spring Boot main class 실행 구성 또는
Gradle의 `bootRun` 중 하나를 사용합니다. 둘 다 JDK 25여야 하며 동시에 실행해서
8080 port를 중복 점유하지 않습니다.

### 5.4 Docker와 IntelliJ의 관계

`Dockerfile`의 `eclipse-temurin:25-jdk`/`eclipse-temurin:25-jre`에서 Eclipse는
**Eclipse Temurin이라는 JDK 배포판 이름**입니다. Eclipse IDE를 사용한다는 뜻이
아니므로 IntelliJ와 충돌하지 않습니다.

Docker Desktop과 `docker compose` CLI만으로 모든 개발 명령을 실행할 수 있습니다.
IntelliJ의 Docker plugin은 Containers/log/image를 IDE 안에서 보고 싶을 때만
사용하는 선택 기능입니다. 연결한다면 **Settings → Build, Execution, Deployment →
Docker**에서 Docker Desktop socket 연결이 성공하는지만 확인합니다.

Compose와 Temurin image는 multi-architecture를 지원하므로 Apple Silicon에서는
ARM64 image, 일반 Windows PC에서는 AMD64 image가 자동 선택됩니다. `compose.yaml`에
`platform: linux/amd64`를 고정하지 않습니다. PostgreSQL raw volume을 Mac과 Windows
사이에 복사하지 말고 데이터 이동이 필요하면 `pg_dump`/`pg_restore`를 사용합니다.

### 5.5 2026-08-29 macOS Apple Silicon 점검 결과

현재 개발 장비에서 확인한 결과는 다음과 같습니다.

| 점검 항목 | 확인 결과 | 상태 |
| --- | --- | --- |
| IntelliJ edition/version | Ultimate `2025.2.2`, native `aarch64` | Java 25 지원, 2026.2 업데이트 권장 |
| Java SDK 등록 | `openjdk-25`, Oracle OpenJDK 25, `aarch64` | 정상 |
| Project language level | Java 25 | 정상 |
| Compiler bytecode target | 25 | 정상 |
| Java compiler argument | Spring Boot plugin이 `-parameters` 제공 | 명시적 중복 제거 |
| Gradle Wrapper | 9.7.1 | 정상 |
| Gradle JVM | `#PROJECT_JDK (openjdk-25)` + Daemon criteria 25 | 정상 |
| Gradle Java toolchain | Oracle OpenJDK 25 `aarch64` 탐지 | 정상 |
| Spring/Gradle/Java plugin | IntelliJ Ultimate에 포함 | 정상 |
| Docker Engine/Compose | 29.5.3 ARM64 / Compose 5.1.4 | daemon 연결 및 Compose 해석 정상 |

Gradle reload 뒤 `.idea/workspace.xml`의 오래된 Kotlin DSL cache에 Corretto 26 경로가
남아 보이거나 `.idea/compiler.xml`에 `-parameters`가 중복되어 보이면 IDE가 아직
이전 Gradle model을 저장한 상태입니다. IDE를 한 번 재시작하고 Gradle project를
reload합니다. 그래도 26이 사용되면 Gradle JVM을 다시 `Project SDK`로 선택하고
`./gradlew --stop` 후 reload합니다. `.idea/workspace.xml`과 `compiler.xml`은 개인별
cache이므로 팀 문서나 commit에 포함하지 않습니다.

## 6. 환경변수와 비밀값

`.env.example`을 `.env`로 복사하고 로컬 값만 수정합니다. `.env`와 `.env.*`는
Git에서 제외되며 `.env.example`만 커밋됩니다.

| 변수 | 기본값/예시 | 용도 |
| --- | --- | --- |
| `POSTGRES_DB` | `dodamdodam` | 로컬 DB 이름 |
| `POSTGRES_USER` | `dodamdodam` | 로컬 DB 사용자 |
| `POSTGRES_PASSWORD` | `dodamdodam_local` | 로컬 전용 DB 비밀번호 |
| `POSTGRES_PORT` | `5432` | 호스트 공개 포트 |
| `SERVER_PORT` | `8080` | Spring Boot 포트 |
| `GOOGLE_CLIENT_ID` | 발급 값 | Google Web OAuth client ID |
| `GOOGLE_CLIENT_SECRET` | 발급 값 | Google OAuth secret |
| `KAKAO_CLIENT_ID` | REST API key | Kakao OIDC client ID |
| `KAKAO_CLIENT_SECRET` | 발급 값 | 활성화한 Kakao client secret |
| `APP_CORS_ALLOWED_ORIGINS` | `http://localhost:3000,http://localhost:5173` | React 개발 origin 목록 |
| `APP_LOGIN_SUCCESS_URL` | `http://localhost:5173/oauth/callback` | 로그인 성공 후 React 이동 주소 |
| `APP_LOGOUT_SUCCESS_URL` | `http://localhost:5173` | 로그아웃 후 이동 주소 |
| `SESSION_COOKIE_SECURE` | `false` | 로컬 HTTP에서는 false, HTTPS 환경에서는 true |

`application.yml`의 `local-...` OAuth 값은 자격 증명이 없을 때도 context가
기동되도록 만든 비밀이 아닌 placeholder입니다. 실제 공급자 로그인에는 사용할 수
없습니다.

비밀이 노출되었다면 Git 기록만 지우는 것으로 끝내지 말고 해당 공급자 콘솔에서
즉시 폐기하고 재발급합니다.

## 7. PostgreSQL과 Flyway

### 7.1 컨테이너만 직접 실행

애플리케이션과 별도로 DB를 관리하려면 다음 명령을 사용합니다.

```bash
docker compose up -d postgres
docker compose logs -f postgres
```

IntelliJ Database 도구 연결 값은 다음과 같습니다.

| 항목 | 값 |
| --- | --- |
| Host | `localhost` |
| Port | `.env`의 `POSTGRES_PORT` (`5432`) |
| Database | `.env`의 `POSTGRES_DB` (`dodamdodam`) |
| User | `.env`의 `POSTGRES_USER` (`dodamdodam`) |
| Password | `.env`의 `POSTGRES_PASSWORD` |

CLI로 접속할 수도 있습니다.

```bash
docker compose exec postgres psql -U dodamdodam -d dodamdodam
```

### 7.2 PostgreSQL 18 볼륨 주의사항

PostgreSQL 공식 Docker 이미지는 18부터 기본 `PGDATA`와 volume 기준 경로가
바뀌었습니다. 이 저장소는 의도적으로 다음 경로를 사용합니다.

```yaml
volumes:
  - postgres-data:/var/lib/postgresql
```

예전 예제의 `/var/lib/postgresql/data`로 바꾸지 않습니다. major 업그레이드는
이미지 태그만 변경하는 작업이 아니며, 백업과 `pg_upgrade` 또는 dump/restore
계획이 필요합니다.

### 7.3 마이그레이션 규칙

새 스키마 파일은 다음 위치와 이름 규칙을 사용합니다.

```text
src/main/resources/db/migration/V1__create_member_table.sql
src/main/resources/db/migration/V2__add_member_profile.sql
```

- 적용된 migration 파일은 수정하지 않습니다.
- 변경은 항상 다음 version의 새 migration으로 추가합니다.
- 애플리케이션의 `ddl-auto`는 `validate`를 유지합니다.
- migration과 관련 repository 통합 테스트를 같은 PR에 포함합니다.
- PostgreSQL 전용 SQL을 사용했다면 그 의도를 migration 주석이나 PR에 남깁니다.

로컬 데이터를 완전히 버리고 초기화해야 할 때만 다음 명령을 사용합니다.

```bash
docker compose down --volumes
```

이 명령은 `dodamdodam-backend` Compose 프로젝트의 로컬 PostgreSQL 데이터를
복구할 수 없게 삭제합니다. 필요한 데이터가 없는지 먼저 확인합니다.

DB 비밀번호를 `.env`에서 바꿔도 이미 생성된 volume의 기존 계정 비밀번호는
자동으로 바뀌지 않습니다. 로컬 초기화가 가능한 경우 volume을 재생성하거나,
필요한 데이터가 있으면 SQL로 비밀번호를 변경합니다.

## 8. Google 로그인 설정

이 서비스는 브라우저가 Backend의 로그인 URL로 이동하는 서버 측 Authorization
Code 흐름을 사용합니다.

1. Google Cloud Console에서 프로젝트를 선택하거나 생성합니다.
2. OAuth 동의 화면을 구성하고 필요한 테스트 사용자를 등록합니다.
3. OAuth client 유형을 **Web application**으로 생성합니다.
4. Authorized redirect URI에 다음 값을 정확히 등록합니다.

```text
http://localhost:8080/login/oauth2/code/google
```

5. client ID와 secret을 `.env`의 `GOOGLE_CLIENT_ID`,
   `GOOGLE_CLIENT_SECRET`에 넣습니다.

로그인 시작 URL:

```text
http://localhost:8080/oauth2/authorization/google
```

scheme, host, port, path, trailing slash 중 하나라도 콘솔 값과 다르면
`redirect_uri_mismatch`가 발생합니다.

## 9. Kakao 로그인 설정

1. Kakao Developers에서 애플리케이션을 생성합니다.
2. **Kakao Login**을 활성화합니다.
3. **OpenID Connect**를 활성화합니다.
4. Redirect URI에 다음 값을 등록합니다.

```text
http://localhost:8080/login/oauth2/code/kakao
```

5. 앱의 **REST API key**를 `.env`의 `KAKAO_CLIENT_ID`에 넣습니다.
6. Client Secret을 활성화하고 발급 값을 `KAKAO_CLIENT_SECRET`에 넣습니다.
7. 동의 항목에서 닉네임과 이메일 정책을 팀 정책에 맞게 설정합니다.

로그인 시작 URL:

```text
http://localhost:8080/oauth2/authorization/kakao
```

Kakao 이메일은 계정 상태와 동의 정책에 따라 없을 수 있습니다. 사용자 식별자는
이메일 단독이 아니라 `provider + sub` 조합을 기준으로 설계합니다.

Kakao endpoint, JWK endpoint, issuer는 공식 Discovery 문서의 값으로 코드에
명시했습니다. 이 방식은 시작할 때 Discovery 서버에 의존하지 않으면서도 ID token의
서명과 issuer 검증 정보를 유지합니다.

## 10. React 연동 기준

로그인 버튼은 AJAX로 공급자 URL을 호출하기보다 브라우저 navigation을 사용합니다.

```text
GET http://localhost:8080/oauth2/authorization/google
GET http://localhost:8080/oauth2/authorization/kakao
```

로그인 후 Backend는 `APP_LOGIN_SUCCESS_URL`로 이동합니다. React가 세션 기반 API를
호출할 때는 cookie 전달을 켭니다.

```javascript
await fetch("http://localhost:8080/api/v1/example", {
  credentials: "include",
});
```

POST·PUT·PATCH·DELETE 전에 CSRF 토큰을 받습니다.

```javascript
const csrf = await fetch("http://localhost:8080/api/v1/auth/csrf", {
  credentials: "include",
}).then((response) => response.json());

await fetch("http://localhost:8080/api/v1/example", {
  method: "POST",
  credentials: "include",
  headers: {
    "Content-Type": "application/json",
    [csrf.headerName]: csrf.token,
  },
  body: JSON.stringify({}),
});
```

React 개발 서버가 다른 port를 사용하면 `APP_CORS_ALLOWED_ORIGINS`와 로그인 성공
URL을 함께 수정합니다. credential을 사용하는 CORS에서는 `*` origin을 사용하지
않습니다.

## 11. FastAPI·AI 팀 연동 기준

- FastAPI의 일반적인 로컬 port는 `8000`으로 예약하되 실제 AI 저장소 설정을
  계약의 기준으로 삼습니다.
- Backend와 FastAPI는 PostgreSQL table을 공동 소유하지 않습니다.
- 요청·응답 DTO, timeout, retry 가능 여부, 오류 형식을 API 계약으로 기록합니다.
- 장시간 AI 작업은 향후 비동기 job 또는 queue가 필요한지 별도로 결정합니다.
- RAG와 vector 검색은 현재 넣지 않습니다. 필요해지면 AI 팀이 vector 생성과 검색을
  소유할지, Backend가 pgvector를 소유할지 데이터 흐름부터 결정합니다.
- AI 팀이 PostgreSQL을 선택하더라도 서비스별 database/schema와 migration 책임을
  분리합니다.

## 12. Docker 실행 방식

### 방식 A: IntelliJ/Spring Boot + PostgreSQL 컨테이너

일상 개발의 권장 방식입니다. IntelliJ에서 main class를 실행하면 Spring Boot가
PostgreSQL을 자동 시작합니다. 애플리케이션 debug와 hot reload가 쉽습니다.

이미 DB를 직접 실행했다면 Spring Boot는 실행 중인 service를 감지해 연결 정보만
사용합니다.

### 방식 B: PostgreSQL만 Compose로 실행

```bash
docker compose up -d postgres
./gradlew bootRun
```

DB log와 lifecycle을 명시적으로 관리하고 싶을 때 사용합니다.

### 방식 C: 애플리케이션까지 전부 컨테이너로 실행

`app` service는 기본 개발 흐름과 충돌하지 않도록 `full` profile에 있습니다.

```bash
docker compose --profile full up --build
```

종료:

```bash
docker compose --profile full down
```

Dockerfile은 Java 25로 빌드하고 Spring Boot layered jar를 분리한 뒤, JRE image에서
UID/GID `10001`의 non-root 사용자로 실행합니다. 이 구성은 로컬의 production-like
검증용이며 AWS 배포 방식이나 운영 secret 주입 방식을 결정하지 않습니다.

로컬 Oracle OpenJDK와 컨테이너의 Eclipse Temurin이 달라도 괜찮습니다. 두 환경은
Java 25라는 실행 계약을 공유하며, Docker image는 host architecture에 맞는 variant를
자동으로 받습니다.

Compose 문법과 환경변수 치환만 검증하려면 다음 명령을 사용합니다.

```bash
docker compose config --quiet
```

## 13. 빌드와 테스트

| 명령 | 목적 |
| --- | --- |
| `./gradlew compileJava` | main source 컴파일 |
| `./gradlew test` | Testcontainers 통합 테스트 |
| `./gradlew clean check` | CI와 동일한 전체 quality gate |
| `./gradlew bootRun` | 로컬 애플리케이션 실행 |
| `./gradlew bootJar` | 실행 가능한 jar 생성 |
| `./gradlew bootTestRun` | 테스트용 PostgreSQL과 함께 애플리케이션 실행 |

현재 smoke test는 다음을 검증합니다.

- Spring context가 PostgreSQL 18.6에 연결되어 기동되는지
- Flyway와 JPA 설정이 충돌하지 않는지
- health endpoint가 인증 없이 `UP`을 반환하는지
- SPA용 CSRF token endpoint가 token을 발급하는지

### 13.1 2026-08-29 기준 설정 검증 기록

| 검증 | 결과 |
| --- | --- |
| `./gradlew --version` | Gradle 9.7.1, Daemon criteria Java 25, macOS ARM64 확인 |
| `./gradlew javaToolchains` | Oracle OpenJDK 25 `aarch64` JDK 탐지 |
| `./gradlew clean check --no-daemon` | 성공, 테스트 2개/실패 0/오류 0 |
| `docker compose --env-file .env.example config --quiet` | 성공 |
| `docker build -t dodamdodam-backend:local .` | Eclipse Temurin 25 build/JRE image ARM64 빌드 성공 |

현재 shell의 `Launcher JVM`이 Homebrew Java 26이어도 커밋된 Daemon criteria가 실제
Gradle 실행 JVM을 Java 25로 제한하는 것을 확인했습니다. IntelliJ에서는 추가 혼동을
막기 위해 Gradle JVM을 `Project SDK (openjdk-25)`로 명시합니다.

GitHub Actions는 `.java-version`을 읽어 Temurin 25를 설치하고
`./gradlew clean check --no-daemon`을 실행합니다. Daemon criteria는 vendor를
지정하지 않았으므로 CI의 Temurin 25와 로컬의 Oracle OpenJDK 25를 모두 허용합니다.

## 14. Codex와 Claude Code

- Codex와 AGENTS.md를 지원하는 에이전트는 저장소 루트의 `AGENTS.md`를 읽습니다.
- Claude Code는 `CLAUDE.md`를 읽고, 첫 줄의 `@AGENTS.md` import로 같은 규칙을
  불러옵니다.
- 두 에이전트 모두 `backend` 저장소 루트에서 시작합니다.
- 개인별 지시는 커밋하지 않는 `CLAUDE.local.md` 등에 둘 수 있습니다.
- 환경변수, 실제 `.env`, OAuth secret을 에이전트 대화나 commit에 붙이지 않습니다.

에이전트 규칙에는 build/test 명령, package 경계, Flyway, CSRF, CORS, React/FastAPI
계약, 문서 갱신 조건이 들어 있습니다. 규칙을 바꾸면 `AGENTS.md`를 먼저 수정하고
중복 문서를 만들지 않습니다.

## 15. 문제 해결

### Gradle이 다른 Java로 실행됨

```bash
./gradlew --version
./gradlew javaToolchains
```

Launcher JVM, Daemon JVM criteria, toolchain을 구분해서 확인합니다. 실제 build
Daemon과 toolchain은 25여야 합니다. IntelliJ의 Project SDK와 Gradle JVM을 모두
25로 설정하고 다음 순서로 다시 연결합니다.

```bash
./gradlew --stop
./gradlew clean check
```

그 뒤 IntelliJ에서 **Reload All Gradle Projects**를 실행합니다. `org.gradle.java.home`
개인 경로를 저장소 `gradle.properties`에 추가해서 해결하지 않습니다.

### Mac은 ARM64, Windows는 AMD64라서 실행이 다름

Java source와 Gradle 설정은 그대로 공유합니다. 각 팀원이 자기 OS/CPU용 JDK 25와
Docker Desktop을 설치하면 됩니다. 문제가 생기면 `java -version`, `uname -m`
(Windows는 `$env:PROCESSOR_ARCHITECTURE`), `docker info`의 architecture를 확인합니다.
native library를 새로 도입할 때만 해당 library와 Docker image가 두 architecture를
모두 지원하는지 별도로 검증합니다.

### Docker daemon 연결 실패

Docker Desktop을 실행한 뒤 `docker info`가 성공하는지 확인합니다. 회사 VPN이나
보안 소프트웨어가 Docker socket 또는 Docker Hub 연결을 막는지도 확인합니다.

### 5432 또는 8080 port 충돌

`.env`에서 호스트 port를 변경합니다.

```dotenv
POSTGRES_PORT=55432
SERVER_PORT=18080
```

OAuth callback port도 바뀌므로 Google/Kakao 콘솔과 성공 URL을 같은 값으로
수정해야 합니다.

### Testcontainers가 시작되지 않음

`docker info`와 `docker compose ps`를 먼저 확인합니다. Testcontainers는
`postgres:18.6-alpine`을 처음 실행할 때 image를 내려받으므로 네트워크 접근도
필요합니다.

### OAuth redirect URI 오류

공급자 콘솔과 실제 요청의 scheme, host, port, path를 글자 단위로 비교합니다.
기본 callback은 `/login/oauth2/code/{registrationId}`입니다.

### Kakao `invalid_client`

REST API key를 client ID에 넣었는지, client secret 기능을 활성화했는지,
재발급 후 `.env`를 갱신했는지 확인합니다.

### API 요청이 CORS로 차단됨

브라우저 개발 도구의 Origin 값을 확인하고 그 정확한 origin을
`APP_CORS_ALLOWED_ORIGINS`에 추가합니다. Backend를 재시작해야 반영됩니다.

### 변경 요청이 403 CSRF 오류를 반환함

먼저 `/api/v1/auth/csrf`를 `credentials: "include"`로 호출한 뒤 응답의
`headerName`과 `token`을 변경 요청에 넣습니다. 두 요청에서 동일한 session cookie가
전송되는지 확인합니다.

## 16. 업데이트 원칙

- Spring Boot·Gradle patch: Dependabot PR에서 `clean check`와 Docker build를
  확인한 뒤 반영합니다.
- Java patch: CI와 로컬 JDK를 갱신하되 `.java-version` major는 유지합니다.
- Java 또는 Spring Boot minor/major: 지원 범위, plugin, starter 이름, migration
  가이드를 검토하는 별도 작업으로 진행합니다.
- PostgreSQL patch: release note와 Testcontainers를 확인합니다.
- PostgreSQL major: backup·restore rehearsal과 migration 계획 없이 image tag만
  바꾸지 않습니다.
- port, callback, 환경변수, Docker service가 바뀌면 이 문서와 `.env.example`을
  같은 PR에서 갱신합니다.

## 17. 최초 실행 완료 체크리스트

- [ ] IntelliJ Project SDK, Module SDK, Gradle JVM, Run JRE가 모두 JDK 25이다.
- [ ] Language level과 target bytecode가 25이고 preview가 꺼져 있다.
- [ ] Gradle distribution, build runner, test runner가 Wrapper/Gradle로 설정되어 있다.
- [ ] `./gradlew --version`이 Gradle 9.7.1과 Daemon JVM 25를 표시한다.
- [ ] `./gradlew javaToolchains`에서 Java 25 JDK를 찾는다.
- [ ] IntelliJ 자체 JBR은 변경하지 않았다.
- [ ] Docker daemon과 Compose 명령이 정상이다.
- [ ] `.env.example`을 `.env`로 복사했다.
- [ ] 실제 Google/Kakao 값을 `.env`에 넣고 Git 추적 대상이 아님을 확인했다.
- [ ] `./gradlew clean check`가 통과한다.
- [ ] `/actuator/health`가 `UP`이다.
- [ ] Google/Kakao callback URI가 공급자 콘솔과 일치한다.
- [ ] React origin과 CORS 목록이 일치한다.
- [ ] `git status`에 `.env`, `.idea`, build 산출물이 나타나지 않는다.

## 18. 공식 참고 문서

- [Spring Boot 시스템 요구사항](https://docs.spring.io/spring-boot/system-requirements.html)
- [Spring Boot Docker Compose 지원](https://docs.spring.io/spring-boot/reference/features/dev-services.html)
- [Spring Boot Dockerfile 가이드](https://docs.spring.io/spring-boot/reference/packaging/container-images/dockerfiles.html)
- [Spring Security OAuth 2.0 Login](https://docs.spring.io/spring-security/reference/servlet/oauth2/login/)
- [Gradle Java 호환성](https://docs.gradle.org/current/userguide/compatibility.html)
- [Gradle Daemon JVM criteria](https://docs.gradle.org/current/userguide/gradle_daemon.html#sec:daemon_jvm_criteria)
- [IntelliJ IDEA 지원 Java 버전](https://www.jetbrains.com/help/idea/supported-java-versions.html)
- [IntelliJ IDEA Gradle 설정](https://www.jetbrains.com/help/idea/gradle-settings.html)
- [IntelliJ IDEA Gradle JVM 선택 순서](https://www.jetbrains.com/help/idea/gradle-jvm-selection.html)
- [PostgreSQL 버전 정책](https://www.postgresql.org/support/versioning/)
- [PostgreSQL Official Image의 PGDATA 안내](https://hub.docker.com/_/postgres/)
- [Google OpenID Connect](https://developers.google.com/identity/openid-connect/openid-connect)
- [Kakao Login REST API](https://developers.kakao.com/docs/en/kakaologin/rest-api)
- [Codex AGENTS.md 안내](https://learn.chatgpt.com/docs/agent-configuration/agents-md)
- [Claude Code memory와 CLAUDE.md](https://code.claude.com/docs/en/memory)
