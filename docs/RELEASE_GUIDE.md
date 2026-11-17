# Backend 릴리스

## 버전과 PR

루트 `VERSION`이 출시 버전의 기준이며 Gradle도 같은 파일을 읽습니다.
`0.1.0` 같은 정식 숫자 버전을 사용하고 다음 출시 전 작업 PR에서 올립니다.
예: 오류 수정 `0.1.1`, 다음 기능 묶음 `0.2.0`. CI는 버전 증가 commit을 만들지 않습니다.

- 작업: `feature/SCRUM-번호-설명 → development`, 기존 Jira 키·[BE] 검사, squash merge.
- 승격: 같은 저장소의 `development → main`, merge commit. 제목 예: `🚀 chore(release): [BE] v0.1.0 스프린트 릴리스`.
- 승격 PR 제목의 버전은 PR에 포함된 VERSION과 같아야 합니다. 기존 필수 `jira-issue-key` 검사가 이 예외를 검사하며 검사 이름은 유지합니다.
- 승격 본문에는 포함 Task·관련 Epic·검증 결과를 적고 Issue 자동 종료 키워드는 쓰지 않습니다. 매번 출시용 Epic·Task를 새로 만들지 않습니다.
- 최초 자동화 구현만 별도 Task로 등록합니다. GitHub-first 연동이 건너뛰어졌으면 기존 Issue를 검토하고 `jira-sync`를 추가합니다. 같은 Jira Task를 수동으로 다시 만들지 않습니다.

## 스프린트 Task를 Release에 표시

승격 PR 본문에 아래 `## 스프린트 작업` 섹션을 작성합니다. 이전 출시 이후 실제
반영된 Task를 Jira 스프린트 소속과 대조하고, Task 제목·Jira 링크·작업 PR을 적습니다.
이월 작업은 이력을 표시하고 스프린트 미지정 작업은 별도로 적습니다. Jira Task가
없는 의존성 전용 출시라면 해당 섹션에 그 사실과 변경 PR을 적습니다.

```markdown
## 스프린트 작업

### 실제 Jira 스프린트 이름
- [SCRUM-180](https://dodamdodam.atlassian.net/browse/SCRUM-180) 릴리스 자동화 — 작업 PR #56

## 검증
- 실행한 검사와 결과
```

CI는 이 섹션이 비어 있으면 승격을 거부하고, 실제 main merge commit에 연결된
승격 PR의 **전체 본문**을 GitHub Release 상단에 복사합니다. GitHub 자동 변경 내역도
함께 생성합니다. Jira 정보는 PR 준비 시 조회·검토하며 release job에 새 Jira secret은
추가하지 않습니다. 같은 실행을 재시도할 때 기존 Release 본문은 덮어쓰지 않습니다.

스프린트 이름과 저장소 버전은 별개입니다. Jira에서 완료됐더라도 출시 커밋에
포함되지 않은 Task를 넣지 않으며, Epic 전체 완료로 표시하지 않습니다.

## 자동 생성 순서

```text
main push
  → backend-quality (기존 전체 검사 + 릴리스 회귀)
  → backend-release
      release/vX.Y.Z 브랜치 + vX.Y.Z 태그 + GitHub Release/변경 내역
  → notify-integration (같은 SHA 전달)
```

모든 생성물은 품질 검사가 끝난 정확한 main SHA를 가리킵니다. PR·수동 CI는
릴리스를 만들지 않습니다. GitHub Release는 소스 출시 기록이며 실제 서비스 배포와 다릅니다.
Gradle의 bootJar도 VERSION을 사용합니다. 현재 CI는 JAR 업로드나 서비스 배포를 수행하지 않습니다.

GITHUB_TOKEN으로 생성한 태그 push는 별도 workflow를 시작하지 않으므로
ref와 GitHub Release를 같은 job에서 만듭니다. 쓰기 권한은 release job의
`contents: write`로 제한하며 integration 알림은 기존 GitHub App을 재사용합니다.

## GitHub 보호 설정

Settings → Rules → Rulesets에서 아래 두 규칙을 추가합니다.

| 대상 | 적용 패턴 | 활성 규칙 |
| --- | --- | --- |
| Branch | `refs/heads/release/*` | Restrict updates, Restrict deletions, Block force pushes |
| Tag | `refs/tags/v*` | Restrict updates, Restrict deletions, Block force pushes |

새 ref를 생성하는 Restrict creations는 켜지 않습니다. 기존 main·development의
필수 검사·리뷰·병합 방법은 유지합니다. release 브랜치는 수정용이 아닌 확정본 보관용입니다.
잘못된 출시 수정은 새로운 버전으로 진행합니다.

## 실패와 재실행

1. main의 Backend CI 실패 원인을 확인합니다. 품질 실패면 ref·Release·알림은 실행되지 않습니다.
2. 일시적인 API/권한 실패를 해결한 뒤 **동일 main push 실행**을 Re-run합니다.
3. 같은 버전·같은 SHA의 ref/Release는 재사용합니다. 중간에 ref만 만들어졌어도 이어서 생성합니다.
4. 기존 branch/tag가 다른 SHA라면 덮어쓰지 않고 실패합니다. VERSION을 올리는 작업 PR부터 진행합니다.
5. draft/prerelease 또는 tag 없는 기존 Release는 자동 승격하지 않고 운영자가 먼저 원인을 확인합니다.

PR의 오래된 실행은 취소할 수 있지만 서로 다른 main SHA 실행은 취소하지 않습니다.
동일 버전 충돌은 ref의 생성 전용 API와 검증으로 차단합니다.

## Jira와 integration

Jira의 Epic은 원래 기능 목표를 유지합니다. 출시에 포함한 Task는 필요에 따라
Fix Version `backend-0.1.0`처럼 묶습니다. Jira Releases 기능·버전 등록은 프로젝트
관리자와 조율하며 GitHub CI가 Jira 버전을 자동 생성하거나 Released로 변경하지 않습니다.
Jira Released는 실제 통합·배포 완료 기준이고 Epic 완료는 별도 팀 기준입니다.

Backend 알림은 Release 생성 이후 전달되지만 integration의 `Reconcile Components`는
별도 경로로 component main을 읽습니다. 해당 저장소 담당자가 출시 완료 조건을 확인하도록
반영해야 전체 경로가 같아집니다. 기존 `components.lock.json`의 SHA 고정과 Bot PR은 유지합니다.

## 검증

```bash
python3 .github/scripts/test_release.py
python3 .github/scripts/test_ci_contract.py
./gradlew clean check bootJar --no-daemon
```

원격에서는 main CI 성공, release 브랜치·tag의 SHA, 해당 tag의 GitHub Release,
같은 실행의 integration 알림을 확인합니다. 재실행 전후 ref SHA와 Release ID가 같아야 합니다.

참고: [GitHub 이벤트 제한](https://docs.github.com/en/actions/how-tos/write-workflows/choose-when-workflows-run/trigger-a-workflow),
[Rulesets](https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/managing-rulesets/available-rules-for-rulesets).
